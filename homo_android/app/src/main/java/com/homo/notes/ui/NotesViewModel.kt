package com.homo.notes.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homo.notes.data.NotesRepository
import com.homo.notes.data.SettingsRepository
import com.homo.notes.di.AppModule
import com.homo.notes.data.GitHubConfig
import com.homo.notes.data.Note
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val pagination: com.homo.notes.data.Pagination? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val saveInProgress: Boolean = false,
    val deleteInProgress: Boolean = false,
    val editorContent: String = "",
    val wordCount: Int = 0
)

sealed class SnackbarMessage {
    data class Success(val text: String) : SnackbarMessage()
    data class Error(val text: String) : SnackbarMessage()
}

class NotesViewModel(
    private val settingsRepository: SettingsRepository,
    private val notesRepositoryProvider: (GitHubConfig) -> NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val _snackbar = MutableStateFlow<SnackbarMessage?>(null)
    val snackbar: StateFlow<SnackbarMessage?> = _snackbar.asStateFlow()

    private var loadJob: Job? = null
    private var currentPage = 1
    private var hasMore = true

    init {
        viewModelScope.launch {
            settingsRepository.githubConfig.collect { config ->
                if (config.isConfigured) {
                    loadNotes(reset = true)
                }
            }
        }
    }

    fun setEditorContent(plainText: String) {
        _uiState.update {
            it.copy(
                editorContent = plainText,
                wordCount = plainText.length
            )
        }
    }

    fun loadNotes(reset: Boolean = true) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val config = settingsRepository.githubConfig.first()
            if (!config.isConfigured) {
                _uiState.update { it.copy(error = "请先在设置中配置 GitHub 信息") }
                return@launch
            }
            val repo = notesRepositoryProvider(config)
            if (reset) {
                currentPage = 1
                hasMore = true
                _uiState.update { it.copy(isLoading = true, error = null) }
            } else {
                if (!hasMore) return@launch
                _uiState.update { it.copy(isLoadingMore = true, error = null) }
            }
            // 加载更多时请求下一页（currentPage + 1），避免重复请求已加载的页
            val pageToLoad = if (reset) 1 else currentPage + 1
            Log.d(TAG, "loadNotes: reset=$reset, currentPage=$currentPage, pageToLoad=$pageToLoad, hasMore=$hasMore, alreadyLoaded=${_uiState.value.notes.size}")
            val result = repo.getNotes(page = pageToLoad, limit = 10)
            result.fold(
                onSuccess = { response ->
                    val newList = if (reset) response.notes else _uiState.value.notes + response.notes
                    hasMore = response.pagination.hasMore
                    currentPage = pageToLoad
                    Log.d(TAG, "loadNotes success: page=${response.pagination.page}, total=${response.pagination.total}, hasMore=$hasMore, newListSize=${newList.size}, currentPage=$currentPage")
                    _uiState.update {
                        it.copy(
                            notes = newList,
                            pagination = response.pagination,
                            isLoading = false,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = e.message ?: "加载失败"
                        )
                    }
                    _snackbar.value = SnackbarMessage.Error(e.message ?: "加载失败")
                }
            )
        }
    }

    fun saveNote() {
        viewModelScope.launch {
            val plainText = _uiState.value.editorContent.trim()
            if (plainText.isBlank()) {
                _snackbar.value = SnackbarMessage.Error("请输入笔记内容")
                return@launch
            }
            val config = settingsRepository.githubConfig.first()
            if (!config.isConfigured) {
                _snackbar.value = SnackbarMessage.Error("请先在设置中配置 GitHub 信息")
                return@launch
            }
            _uiState.update { it.copy(saveInProgress = true) }
            val repo = notesRepositoryProvider(config)
            val htmlContent = "<p>${plainText.replace("\n", "<br>")}</p>"
            val result = repo.saveNote(htmlContent)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            saveInProgress = false,
                            editorContent = "",
                            wordCount = 0
                        )
                    }
                    _snackbar.value = SnackbarMessage.Success("笔记保存成功")
                    loadNotes(reset = true)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(saveInProgress = false) }
                    _snackbar.value = SnackbarMessage.Error(e.message ?: "保存失败")
                }
            )
        }
    }

    fun deleteNote(filename: String) {
        viewModelScope.launch {
            val config = settingsRepository.githubConfig.first()
            if (!config.isConfigured) {
                _snackbar.value = SnackbarMessage.Error("请先在设置中配置 GitHub 信息")
                return@launch
            }
            _uiState.update { it.copy(deleteInProgress = true) }
            val repo = notesRepositoryProvider(config)
            val result = repo.deleteNote(filename)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(deleteInProgress = false) }
                    _snackbar.value = SnackbarMessage.Success("笔记已删除")
                    loadNotes(reset = true)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(deleteInProgress = false) }
                    _snackbar.value = SnackbarMessage.Error(e.message ?: "删除失败")
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissSnackbar() {
        _snackbar.value = null
    }

    fun clearEditor() {
        _uiState.update {
            it.copy(editorContent = "", wordCount = 0)
        }
    }

    companion object {
        private const val TAG = "NotesViewModel"
    }
}

/** 用于在配置变更（如旋转）后保留 ViewModel 状态 */
class NotesViewModelFactory(
    private val application: Application,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            return NotesViewModel(
                settingsRepository = settingsRepository,
                notesRepositoryProvider = { config -> AppModule.createNotesRepository(config) }
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
