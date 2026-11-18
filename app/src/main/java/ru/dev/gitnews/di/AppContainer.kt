package ru.dev.gitnews.di

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import ru.dev.gitnews.data.local.GitNewsDatabase
import ru.dev.gitnews.data.local.SecureTokenStorage
import ru.dev.gitnews.data.remote.AuthInterceptor
import ru.dev.gitnews.data.remote.GitHubApiService
import ru.dev.gitnews.data.repository.AuthRepositoryImpl
import ru.dev.gitnews.data.repository.BookmarkRepositoryImpl
import ru.dev.gitnews.data.repository.GitHubRepositoryImpl
import ru.dev.gitnews.data.repository.PresetRepositoryImpl
import ru.dev.gitnews.data.repository.ReleaseTrackerRepositoryImpl
import ru.dev.gitnews.domain.repository.AuthRepository
import ru.dev.gitnews.domain.repository.BookmarkRepository
import ru.dev.gitnews.domain.repository.GitHubRepository
import ru.dev.gitnews.domain.repository.PresetRepository
import ru.dev.gitnews.domain.repository.ReleaseTrackerRepository
import ru.dev.gitnews.ml.MlKitTranslationManager
import java.util.concurrent.TimeUnit

interface AppContainer {
    val secureTokenStorage: SecureTokenStorage
    val gitNewsDatabase: GitNewsDatabase
    val authRepository: AuthRepository
    val presetRepository: PresetRepository
    val bookmarkRepository: BookmarkRepository
    val gitHubRepository: GitHubRepository
    val releaseTrackerRepository: ReleaseTrackerRepository
    val translationManager: MlKitTranslationManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val secureTokenStorage: SecureTokenStorage by lazy {
        SecureTokenStorage(context)
    }

    override val gitNewsDatabase: GitNewsDatabase by lazy {
        GitNewsDatabase.getInstance(context)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(secureTokenStorage))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    private val gitHubApiService: GitHubApiService by lazy {
        retrofit.create(GitHubApiService::class.java)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(gitHubApiService, secureTokenStorage)
    }

    override val presetRepository: PresetRepository by lazy {
        PresetRepositoryImpl(gitNewsDatabase.presetDao())
    }

    override val bookmarkRepository: BookmarkRepository by lazy {
        BookmarkRepositoryImpl(gitNewsDatabase.bookmarkDao())
    }

    override val gitHubRepository: GitHubRepository by lazy {
        GitHubRepositoryImpl(gitHubApiService, gitNewsDatabase.bookmarkDao(), okHttpClient)
    }

    override val releaseTrackerRepository: ReleaseTrackerRepository by lazy {
        ReleaseTrackerRepositoryImpl(
            context = context,
            gitHubApiService = gitHubApiService,
            bookmarkDao = gitNewsDatabase.bookmarkDao(),
            secureTokenStorage = secureTokenStorage
        )
    }

    override val translationManager: MlKitTranslationManager by lazy {
        MlKitTranslationManager(gitNewsDatabase.translationCacheDao())
    }
}
