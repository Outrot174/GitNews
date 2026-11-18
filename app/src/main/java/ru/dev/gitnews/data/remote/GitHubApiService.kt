package ru.dev.gitnews.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import ru.dev.gitnews.data.remote.dto.DeviceCodeResponseDto
import ru.dev.gitnews.data.remote.dto.GitHubRepoDto
import ru.dev.gitnews.data.remote.dto.GitHubSearchResponseDto
import ru.dev.gitnews.data.remote.dto.GitHubUserDto
import ru.dev.gitnews.data.remote.dto.OAuthTokenResponseDto
import ru.dev.gitnews.data.remote.dto.RateLimitResponseDto
import ru.dev.gitnews.data.remote.dto.ReleaseDto

interface GitHubApiService {

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q", encoded = true) query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): GitHubSearchResponseDto

    @GET("user")
    suspend fun getCurrentUser(): GitHubUserDto

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRepoDto

    @Headers(
        "Accept: application/vnd.github.raw+json, application/vnd.github.v3.raw, text/plain; charset=utf-8"
    )
    @GET("repos/{owner}/{repo}/readme")
    suspend fun getRawReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ResponseBody

    @GET("user/starred/{owner}/{repo}")
    suspend fun checkIsStarred(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @PUT("user/starred/{owner}/{repo}")
    suspend fun starRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @DELETE("user/starred/{owner}/{repo}")
    suspend fun unstarRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<ReleaseDto>

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ReleaseDto

    @GET("rate_limit")
    suspend fun getRateLimit(): RateLimitResponseDto

    // OAuth Device Flow Endpoints
    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST
    suspend fun requestDeviceCode(
        @Url url: String = "https://github.com/login/device/code",
        @Field("client_id") clientId: String,
        @Field("scope") scope: String = "read:user,public_repo"
    ): DeviceCodeResponseDto

    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST
    suspend fun pollDeviceToken(
        @Url url: String = "https://github.com/login/oauth/access_token",
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
    ): OAuthTokenResponseDto
}
