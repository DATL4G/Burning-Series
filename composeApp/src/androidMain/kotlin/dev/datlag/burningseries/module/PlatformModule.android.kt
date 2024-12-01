package dev.datlag.burningseries.module

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import coil3.ImageLoader
import coil3.request.allowHardware
import dev.datlag.burningseries.BuildConfig
import dev.datlag.burningseries.BuildKonfig
import dev.datlag.burningseries.Sekret
import dev.datlag.burningseries.common.deviceName
import dev.datlag.burningseries.database.DriverFactory
import dev.datlag.burningseries.firebase.FirebaseFactory
import dev.datlag.burningseries.firebase.initialize
import dev.datlag.burningseries.other.StateSaver
import dev.datlag.burningseries.settings.DataStoreAppSettings
import dev.datlag.burningseries.settings.DataStoreUserSettings
import dev.datlag.burningseries.settings.Settings
import dev.datlag.burningseries.settings.model.AppSettings
import dev.datlag.burningseries.settings.model.UserSettings
import dev.datlag.tooling.Platform
import dev.datlag.tooling.createAsFileSafely
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import java.net.InetAddress
import java.util.concurrent.TimeUnit

actual object PlatformModule {

    private const val NAME = "AndroidPlatformModule"
    private const val APP_VERSION = "APP_VERSION"
    private const val DEVICE_NAME = "DEVICE_NAME"

    actual val di: DI.Module = DI.Module(NAME) {
        bindSingleton<OkHttpClient> {
            OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(3, TimeUnit.MINUTES)
                .readTimeout(3, TimeUnit.MINUTES)
                .writeTimeout(3, TimeUnit.MINUTES)
                .build()
        }
        bindSingleton<Dns> {
            DnsOverHttps.Builder()
                .client(instance())
                .url("https://dns.google/dns-query".toHttpUrl())
                .bootstrapDnsHosts(InetAddress.getByName("8.8.4.4"), InetAddress.getByName("8.8.8.8"))
                .build()
        }
        bindSingleton<HttpClient> {
            HttpClient(OkHttp) {
                followRedirects = true
                engine {
                    config {
                        followRedirects(true)
                        followSslRedirects(true)
                        connectTimeout(3, TimeUnit.MINUTES)
                        readTimeout(3, TimeUnit.MINUTES)
                        writeTimeout(3, TimeUnit.MINUTES)
                        dns(instance())
                    }
                }
                install(ContentNegotiation) {
                    json(instance(), ContentType.Application.Json)
                    json(instance(), ContentType.Text.Plain)
                }
            }
        }
        bindSingleton<HttpClient>("STREAM_CLIENT") {
            HttpClient(OkHttp) {
                followRedirects = true
            }
        }
        bindSingleton<DataStore<AppSettings>> {
            val app: Context = instance()

            DataStoreFactory.create(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = AppSettings.SettingsSerializer,
                    producePath = {
                        val path = app.filesDir.toOkioPath()
                            .resolve("v6")
                            .resolve("app.settings").also {
                                it.toFile().createAsFileSafely()
                            }

                        path
                    }
                )
            )
        }
        bindSingleton<Settings.PlatformAppSettings> {
            DataStoreAppSettings(instance())
        }
        bindSingleton<DataStore<UserSettings>> {
            val app: Context = instance()

            DataStoreFactory.create(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = UserSettings.SettingsSerializer,
                    producePath = {
                        val path = app.filesDir.toOkioPath()
                            .resolve("v6")
                            .resolve("user.settings").also {
                                it.toFile().createAsFileSafely()
                            }

                        path
                    }
                )
            )
        }
        bindSingleton<Settings.PlatformUserSettings> {
            DataStoreUserSettings(instance())
        }
        bindSingleton<FirebaseFactory> {
            if (StateSaver.sekretLibraryLoaded) {
                FirebaseFactory.initialize(
                    context = instance(),
                    projectId = Sekret.firebaseProject(BuildKonfig.packageName),
                    applicationId = Sekret.firebaseApplication(BuildKonfig.packageName)!!,
                    apiKey = Sekret.firebaseApiKey(BuildKonfig.packageName)!!,
                    localLogger = object : FirebaseFactory.Crashlytics.LocalLogger {
                        override fun warn(message: String?) {
                            message?.let { Napier.w(it) }
                        }

                        override fun error(message: String?) {
                            message?.let { Napier.e(it) }
                        }

                        override fun error(throwable: Throwable?) {
                            throwable?.let { Napier.e("", it) }
                        }
                    }
                )
            } else {
                FirebaseFactory.Empty
            }
        }
        bindSingleton<DriverFactory> {
            DriverFactory(
                context = instance()
            )
        }
        bindSingleton<AndroidCodeAuthFlowFactory> {
            AndroidCodeAuthFlowFactory()
        }
        bindSingleton<String>(APP_VERSION) {
            BuildConfig.VERSION_NAME
        }
        bindSingleton<String>(DEVICE_NAME) {
            Platform.deviceName(instance())
        }
    }
}

actual fun ImageLoader.Builder.extendImageLoader(): ImageLoader.Builder {
    return this.allowHardware(false)
}