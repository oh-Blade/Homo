package com.homo.notes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 应用内设置存储。
 * 保存 GitHub 配置：GITHUB_TOKEN、GITHUB_USERNAME、GITHUB_REPO、GITHUB_BRANCH，
 * 用于通过 api.github.com 直接提交/拉取笔记。
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class GitHubConfig(
    val token: String,
    val username: String,
    val repo: String,
    val branch: String
) {
    val isConfigured: Boolean
        get() = token.isNotBlank() && username.isNotBlank() && repo.isNotBlank() && branch.isNotBlank()
}

class SettingsRepository(private val context: Context) {

    val githubConfig: Flow<GitHubConfig> = context.dataStore.data.map { prefs ->
        GitHubConfig(
            token = prefs[KEY_GITHUB_TOKEN] ?: "",
            username = prefs[KEY_GITHUB_USERNAME] ?: "",
            repo = prefs[KEY_GITHUB_REPO] ?: "",
            branch = prefs[KEY_GITHUB_BRANCH] ?: DEFAULT_BRANCH
        )
    }

    suspend fun setGitHubConfig(
        token: String,
        username: String,
        repo: String,
        branch: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GITHUB_TOKEN] = token.trim()
            prefs[KEY_GITHUB_USERNAME] = username.trim()
            prefs[KEY_GITHUB_REPO] = repo.trim()
            prefs[KEY_GITHUB_BRANCH] = branch.trim().ifBlank { DEFAULT_BRANCH }
        }
    }

    companion object {
        private val KEY_GITHUB_TOKEN = stringPreferencesKey("github_token")
        private val KEY_GITHUB_USERNAME = stringPreferencesKey("github_username")
        private val KEY_GITHUB_REPO = stringPreferencesKey("github_repo")
        private val KEY_GITHUB_BRANCH = stringPreferencesKey("github_branch")
        const val DEFAULT_BRANCH = "main"
    }
}
