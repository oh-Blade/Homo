package com.homo.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homo.notes.data.Note
import com.homo.notes.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    settingsRepository: SettingsRepository,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar by viewModel.snackbar.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(snackbar) {
        snackbar?.let { msg ->
            val text = when (msg) {
                is SnackbarMessage.Success -> msg.text
                is SnackbarMessage.Error -> msg.text
            }
            snackbarHostState.showSnackbar(text)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("心路历程", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadNotes(reset = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Editor
            EditorSection(
                content = uiState.editorContent,
                wordCount = uiState.wordCount,
                saveInProgress = uiState.saveInProgress,
                onContentChange = viewModel::setEditorContent,
                onSave = viewModel::saveNote,
                onClear = viewModel::clearEditor
            )

            // Notes list
            Text(
                "历史笔记",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading && uiState.notes.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.error != null && uiState.notes.isEmpty() -> {
                        val errorMessage = uiState.error
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadNotes(reset = true) }) {
                                Text("重试")
                            }
                        }
                    }
                    uiState.notes.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "还没有笔记，快写下第一篇吧！",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.notes) { note ->
                                NoteItem(
                                    note = note,
                                    onDelete = { noteToDelete = note }
                                )
                            }
                            if (uiState.pagination?.hasMore == true) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.isLoadingMore) {
                                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                        } else {
                                            TextButton(onClick = { viewModel.loadNotes(reset = false) }) {
                                                Text("加载更多笔记")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            settingsRepository = settingsRepository,
            viewModel = viewModel
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("删除笔记") },
            text = { Text("确定要删除这条笔记吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        note.filename?.let { viewModel.deleteNote(it) }
                        noteToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun EditorSection(
    content: String,
    wordCount: Int,
    saveInProgress: Boolean,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .verticalScroll(rememberScrollState()),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        if (content.isEmpty()) {
                            Text(
                                "在这里写下你的想法…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        }
                        inner()
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "字数: $wordCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    TextButton(onClick = onClear) {
                        Text("清空")
                    }
                    Button(
                        onClick = onSave,
                        enabled = wordCount > 0 && !saveInProgress
                    ) {
                        if (saveInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("保存笔记")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteItem(
    note: Note,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatDate(note.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = android.text.Html.fromHtml(
                        note.content,
                        android.text.Html.FROM_HTML_MODE_LEGACY
                    ).toString().ifBlank { "(无内容)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatDate(timestamp: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(timestamp)
            ?: return timestamp
        val now = Date()
        val diff = now.time - date.time
        val diffDays = (diff / (1000 * 60 * 60 * 24)).toInt()
        when {
            diffDays == 0 -> "今天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            diffDays == 1 -> "昨天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            diffDays < 7 -> "${diffDays}天前"
            else -> SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(date)
        }
    } catch (_: Exception) {
        timestamp
    }
}

@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit,
    settingsRepository: SettingsRepository,
    viewModel: NotesViewModel
) {
    var githubToken by remember { mutableStateOf("") }
    var githubUsername by remember { mutableStateOf("") }
    var githubRepo by remember { mutableStateOf("") }
    var githubBranch by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val config = settingsRepository.githubConfig.first()
        githubToken = config.token
        githubUsername = config.username
        githubRepo = config.repo
        githubBranch = config.branch.ifBlank { SettingsRepository.DEFAULT_BRANCH }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = githubToken,
                    onValueChange = { githubToken = it },
                    label = { Text("GITHUB_TOKEN") },
                    placeholder = { Text("GitHub 个人访问令牌") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = githubUsername,
                    onValueChange = { githubUsername = it },
                    label = { Text("GITHUB_USERNAME") },
                    placeholder = { Text("GitHub 用户名") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = githubRepo,
                    onValueChange = { githubRepo = it },
                    label = { Text("GITHUB_REPO") },
                    placeholder = { Text("仓库名") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = githubBranch,
                    onValueChange = { githubBranch = it },
                    label = { Text("GITHUB_BRANCH") },
                    placeholder = { Text("分支，默认 main") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    settingsRepository.setGitHubConfig(
                        token = githubToken,
                        username = githubUsername,
                        repo = githubRepo,
                        branch = githubBranch.ifBlank { SettingsRepository.DEFAULT_BRANCH }
                    )
                    viewModel.loadNotes(reset = true)
                }
                onDismiss()
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
