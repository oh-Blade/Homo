package com.homo.notes.data

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class NotesRepository(
    private val api: GitHubApi,
    private val config: GitHubConfig
) {
    private val gson = Gson()

    suspend fun getNotes(page: Int = 1, limit: Int = 10): Result<NotesResponse> = withContext(Dispatchers.IO) {
        try {
            val owner = config.username
            val repo = config.repo
            val branch = config.branch
            val listResponse = api.listNotes(owner, repo, branch)
            if (!listResponse.isSuccessful) {
                if ( listResponse.code() == 404 ) {
                    // notes 目录不存在，返回空列表
                    return@withContext Result.success(
                        NotesResponse(notes = emptyList(), pagination = Pagination(page, limit, 0, false, 0))
                    )
                }
                return@withContext Result.failure(HttpException(listResponse))
            }
            val items = listResponse.body() ?: emptyList()
            val jsonFiles = items
                .filter { it.name.endsWith(".json") && it.type == "file" }
                .sortedByDescending { parseTimestampFromFilename(it.name) ?: 0L } // 按时间戳降序，新在前
            val total = jsonFiles.size
            val totalPages = if (limit > 0) (total + limit - 1) / limit else 0
            val start = (page - 1) * limit
            val end = minOf(start + limit, total)
            val slice = jsonFiles.subList(start, end)
            val notes = slice.mapNotNull { item ->
                fetchNoteContent(owner, repo, branch, item.name)
            }
            val hasMore = end < total
            Result.success(
                NotesResponse(
                    notes = notes,
                    pagination = Pagination(page, limit, total, hasMore, totalPages)
                )
            )
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "getNotes error", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchNoteContent(owner: String, repo: String, branch: String, filename: String): Note? {
        val resp = api.getFileContent(owner, repo, filename, branch)
        if (!resp.isSuccessful) return null
        val body = resp.body() ?: return null
        val contentBase64 = body.content ?: return null
        return try {
            val decoded = Base64.decode(contentBase64, Base64.DEFAULT)
            val json = String(decoded, Charsets.UTF_8)
            val note = gson.fromJson(json, Note::class.java)
            note.copy(filename = filename)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveNote(content: String): Result<Note> = withContext(Dispatchers.IO) {
        try {
            val owner = config.username
            val repo = config.repo
            val branch = config.branch
            val id = System.currentTimeMillis()
            val filename = "$id.json"
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .format(java.util.Date())
            val noteData = mapOf(
                "id" to id,
                "content" to content,
                "timestamp" to timestamp,
                "created" to timestamp
            )
            val json = gson.toJson(noteData)
            val contentBase64 = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val message = "添加笔记: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA).format(java.util.Date())}"
            val putResponse = api.putFile(owner, repo, filename, GitHubPutBody(message, contentBase64, branch))
            if (!putResponse.isSuccessful) {
                val errBody = putResponse.errorBody()?.string()
                return@withContext Result.failure(Exception(errBody ?: "保存失败"))
            }
            val note = Note(id = id, content = content, timestamp = timestamp, created = timestamp, filename = filename)
            Result.success(note)
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "saveNote error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(filename: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!filename.matches(Regex("^\\d+\\.json$"))) {
                return@withContext Result.failure(Exception("无效的文件名格式"))
            }
            val owner = config.username
            val repo = config.repo
            val branch = config.branch
            val fileResp = api.getFileContent(owner, repo, filename, branch)
            if (!fileResp.isSuccessful) {
                return@withContext Result.failure(Exception("无法获取文件信息"))
            }
            val sha = fileResp.body()?.sha ?: return@withContext Result.failure(Exception("无法获取文件 SHA"))
            val message = "删除笔记: $filename"
            val delResponse = api.deleteFile(owner, repo, filename, GitHubDeleteBody(message, sha, branch))
            if (!delResponse.isSuccessful) {
                val errBody = delResponse.errorBody()?.string()
                return@withContext Result.failure(Exception(errBody ?: "删除失败"))
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "deleteNote error", e)
            Result.failure(e)
        }
    }

    /**
     * 从文件名解析毫秒时间戳，如 1738567890123.json -> 1738567890123L，不符合 \d+.json 返回 null。
     */
    private fun parseTimestampFromFilename(name: String): Long? {
        val match = NOTE_FILENAME_REGEX.find(name) ?: return null
        return match.groupValues.getOrNull(1)?.toLongOrNull()
    }

    companion object {
        private const val TAG = "NotesRepository"
        private val NOTE_FILENAME_REGEX = Regex("^(\\d+)\\.json$")
    }
}
