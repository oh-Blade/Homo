package com.homo.notes.data

import com.google.gson.annotations.SerializedName

/**
 * 单条笔记（与 Web 端 API 返回格式一致）
 */
data class Note(
    val id: Long,
    val content: String,
    val timestamp: String,
    val created: String,
    @SerializedName("filename") val filename: String? = null
)

data class NotesResponse(
    val notes: List<Note>,
    val pagination: Pagination
)

data class Pagination(
    val page: Int,
    val limit: Int,
    val total: Int,
    val hasMore: Boolean,
    val totalPages: Int
)

data class SaveNoteRequest(
    val content: String
)

data class SaveNoteResponse(
    val success: Boolean,
    val note: Note
)
