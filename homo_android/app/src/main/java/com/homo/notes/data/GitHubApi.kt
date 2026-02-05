package com.homo.notes.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * GitHub REST API (api.github.com) 用于笔记的读写。
 * 笔记存放在仓库的 notes/ 目录，每个笔记为 {毫秒时间戳}.json。
 */
interface GitHubApi {

    /** 列出 notes 目录下的文件（用于分页时取文件名列表） */
    @GET("repos/{owner}/{repo}/contents/notes")
    suspend fun listNotes(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("ref") ref: String
    ): Response<List<GitHubContentItem>>

    /** 获取单个文件内容（content 为 base64） */
    @GET("repos/{owner}/{repo}/contents/notes/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String
    ): Response<GitHubFileContent>

    /** 创建或更新文件 */
    @PUT("repos/{owner}/{repo}/contents/notes/{path}")
    suspend fun putFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body body: GitHubPutBody
    ): Response<GitHubPutResponse>

    /** 删除文件（需在 body 中传 message、sha、branch；DELETE 带 body 需用 @HTTP） */
    @HTTP(method = "DELETE", path = "repos/{owner}/{repo}/contents/notes/{path}", hasBody = true)
    suspend fun deleteFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body body: GitHubDeleteBody
    ): Response<Unit>
}

/** 目录列表项 */
data class GitHubContentItem(
    val name: String,
    val path: String,
    val sha: String?,
    val type: String?
)

/** 文件内容（GET contents 返回） */
data class GitHubFileContent(
    val content: String?,
    val encoding: String?,
    val sha: String?,
    val name: String?
)

/** 创建/更新文件请求体 */
data class GitHubPutBody(
    val message: String,
    val content: String,
    val branch: String
)

/** 删除文件请求体 */
data class GitHubDeleteBody(
    val message: String,
    val sha: String,
    val branch: String
)

/** PUT 成功响应（仅需 content.sha 等，此处简化） */
data class GitHubPutResponse(
    val content: GitHubPutResponseContent?
)

data class GitHubPutResponseContent(
    val sha: String?,
    val name: String?
)
