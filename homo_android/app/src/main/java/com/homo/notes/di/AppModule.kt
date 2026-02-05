package com.homo.notes.di

import com.homo.notes.data.GitHubApi
import com.homo.notes.data.GitHubConfig
import com.homo.notes.data.NotesRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppModule {

    private const val GITHUB_API_BASE = "https://api.github.com/"

    fun createGitHubApi(token: String): GitHubApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "token $token")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(GITHUB_API_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(GitHubApi::class.java)
    }

    fun createNotesRepository(config: GitHubConfig): NotesRepository {
        val api = createGitHubApi(config.token)
        return NotesRepository(api, config)
    }
}
