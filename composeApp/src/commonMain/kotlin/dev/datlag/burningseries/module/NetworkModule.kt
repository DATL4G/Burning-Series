package dev.datlag.burningseries.module

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import de.jensklingenberg.ktorfit.ktorfit
import dev.datlag.burningseries.BuildKonfig
import dev.datlag.burningseries.Sekret
import dev.datlag.burningseries.common.nullableFirebaseInstance
import dev.datlag.burningseries.github.GitHub
import dev.datlag.burningseries.github.createGitHub
import dev.datlag.burningseries.network.EpisodeStateMachine
import dev.datlag.burningseries.network.HomeStateMachine
import dev.datlag.burningseries.network.SaveStateMachine
import dev.datlag.burningseries.network.SearchStateMachine
import dev.datlag.burningseries.network.SeriesStateMachine
import dev.datlag.burningseries.other.SyncHelper
import dev.datlag.burningseries.other.UserHelper
import dev.datlag.tooling.compose.ioDispatcher
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import okio.FileSystem
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.kodein.di.instanceOrNull
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlow

data object NetworkModule {

    const val NAME = "NetworkModule"

    @OptIn(ExperimentalOpenIdConnect::class)
    val di = DI.Module(NAME) {
        import(DatabaseModule.di)

        bindSingleton<Json> {
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        }
        bindSingleton<ImageLoader> {
            ImageLoader.Builder(instance<PlatformContext>())
                .components {
                    add(KtorNetworkFetcherFactory(instance<HttpClient>()))
                    add(SvgDecoder.Factory())
                }
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(instance<PlatformContext>())
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                        .maxSizeBytes(512L * 1024 * 1024) // 512 MB
                        .build()
                }
                .crossfade(true)
                .extendImageLoader()
                .build()
        }
        bindProvider<HomeStateMachine> {
            HomeStateMachine(
                client = instance(),
                crashlytics = nullableFirebaseInstance()?.crashlytics
            )
        }
        bindProvider<SearchStateMachine> {
            SearchStateMachine(
                client = instance(),
                crashlytics = nullableFirebaseInstance()?.crashlytics
            )
        }
        bindProvider<SeriesStateMachine> {
            SeriesStateMachine(
                client = instance(),
                crashlytics = nullableFirebaseInstance()?.crashlytics
            )
        }
        bindProvider<EpisodeStateMachine> {
            EpisodeStateMachine(
                client = instanceOrNull<HttpClient>("STREAM_CLIENT") ?: instance(),
                firebaseAuth = nullableFirebaseInstance()?.auth,
                fireStore = nullableFirebaseInstance()?.store,
                crashlytics = nullableFirebaseInstance()?.crashlytics
            )
        }
        bindProvider<SaveStateMachine> {
            SaveStateMachine(
                client = instance(),
                streamClient = instanceOrNull("STREAM_CLIENT"),
                firebaseAuth = nullableFirebaseInstance()?.auth,
                fireStore = nullableFirebaseInstance()?.store,
                crashlytics = nullableFirebaseInstance()?.crashlytics
            )
        }
        bindSingleton<GitHub> {
            val ktorfit = ktorfit {
                httpClient(instance<HttpClient>())
                baseUrl("https://api.github.com/")
            }
            ktorfit.createGitHub()
        }
        bindSingleton<OpenIdConnectClient> {
            OpenIdConnectClient {
                endpoints {
                    authorizationEndpoint = "https://github.com/login/oauth/authorize"
                    tokenEndpoint = "https://github.com/login/oauth/access_token"
                }

                clientId = Sekret.githubClientId(BuildKonfig.packageName)
                clientSecret = Sekret.githubClientSecret(BuildKonfig.packageName)
                scope = "read:user"
            }
        }
        bindProvider<CodeAuthFlow> {
            instance<CodeAuthFlowFactory>().createAuthFlow(instance())
        }
        bindSingleton<UserHelper> {
            UserHelper(
                github = instance(),
                appVersion = instanceOrNull("APP_VERSION"),
                oidcClient = instance(),
                tokenStore = instance(),
                appSettings = instance(),
                database = instance()
            )
        }
        bindSingleton<SyncHelper> {
            SyncHelper(
                appSettings = instance(),
                userSettings = instance()
            )
        }
    }
}