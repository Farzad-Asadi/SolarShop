package com.example.solarShop.data.modules



import com.example.solarShop.BuildConfig
import com.example.solarShop.data.dataStore.SessionDataStore
import com.example.solarShop.data.network.AuthPlugin
import com.example.solarShop.data.network.RefreshResult
import com.example.solarShop.data.network.ServerConnectionState
import com.example.solarShop.data.network.mock.mockPlainEngine
import com.example.solarShop.data.network.remote.AuthApi
import com.example.solarShop.data.network.remote.EntitlementApi
import com.example.solarShop.data.network.remote.FileApi
import com.example.solarShop.data.network.remote.SyncApi
import com.example.solarShop.data.network.remote.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.errors.IOException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @OptIn(ExperimentalSerializationApi::class)
    @Provides @Singleton @Named("networkJson")
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = false
    }

    @Provides @Singleton @Named("useMock")
    fun provideUseMock(): Boolean = BuildConfig.USE_MOCK

    @Provides @Singleton @Named("plain")
    fun providePlainClient(
        @Named("networkJson")json: Json,
        @Named("useMock") useMock: Boolean
    ): HttpClient {
        val engine: HttpClientEngine =
            if (useMock) mockPlainEngine(json) else OkHttp.create {}

        return HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(Logging) { logger = Logger.DEFAULT; level = LogLevel.BODY }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis  = 15_000
            }
            defaultRequest {
                url(BuildConfig.BASE_URL)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }
        }
    }

    // اگر Authed جدا می‌خواهی، فعلاً مثل plain؛ Bearer را داخل APIها می‌گذاریم
    @Provides
    @Singleton
    @Named("authed")
    fun provideAuthedClient(
        @Named("networkJson") json: Json,
        @Named("useMock") useMock: Boolean,
        session: SessionDataStore,
        serverState: ServerConnectionState
    ): HttpClient {
        val engine: HttpClientEngine =
            if (useMock) mockPlainEngine(json) else OkHttp.create {}

        return HttpClient(engine) {
            expectSuccess = false

            install(ContentNegotiation) {
                json(json)
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.BODY
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }

            install(AuthPlugin) {
                getAccessToken = {
                    session.snapshot().accessToken
                }

                getRefreshToken = {
                    session.snapshot().refreshToken
                }

                refreshTokens = { refreshToken ->
                    try {
                        val authApi = AuthApi(providePlainClient(json, useMock))
                        val response = authApi.refreshRaw(refreshToken)

                        when (response.status) {
                            HttpStatusCode.OK -> {
                                val tokens = response.body<com.example.solarShop.data.network.dto.TokenResponse>()

                                session.setTokens(
                                    access = tokens.access,
                                    refresh = tokens.refresh
                                )

                                RefreshResult.Success
                            }

                            HttpStatusCode.Unauthorized,
                            HttpStatusCode.Forbidden -> {
                                session.clearSession()
                                RefreshResult.InvalidRefreshToken
                            }

                            else -> {
                                RefreshResult.NetworkError
                            }
                        }

                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: IOException) {
                        RefreshResult.NetworkError
                    } catch (e: Exception) {
                        RefreshResult.NetworkError
                    }
                }

                onConnected = {
                    serverState.connected()
                }

                onServerUnreachable = {
                    serverState.unreachable()
                }

                onAuthExpired = {
                    serverState.authExpired()
                }
            }

            defaultRequest {
                url(BuildConfig.BASE_URL)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }
        }
    }

    // APIها
    @Provides @Singleton
    fun provideAuthApi(@Named("plain") client: HttpClient): AuthApi = AuthApi(client)

    @Provides @Singleton
    fun provideUserApi(@Named("plain") client: HttpClient, session: SessionDataStore): UserApi =
        UserApi(client, session)

    @Provides @Singleton
    fun provideEntitlementApi(@Named("plain") client: HttpClient, session: SessionDataStore): EntitlementApi =
        EntitlementApi(client, session)

    @Provides
    @Singleton
    @Named("external")
    fun provideExternalClient(
        @Named("networkJson") json: Json
    ): HttpClient {
        return HttpClient(OkHttp.create {}) {
            expectSuccess = false

            install(ContentNegotiation) {
                json(json)
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.BODY
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }

            defaultRequest {
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }
        }
    }

    @Provides
    @Singleton
    fun provideSyncApi(
        @Named("authed") client: HttpClient
    ): SyncApi = SyncApi(client)

    @Provides
    @Singleton
    fun provideFileApi(
        @Named("authed") client: HttpClient
    ): FileApi = FileApi(client)
}



