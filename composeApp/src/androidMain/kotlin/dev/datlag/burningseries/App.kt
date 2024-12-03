package dev.datlag.burningseries

import android.content.Context
import android.os.StrictMode
import androidx.multidex.MultiDexApplication
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import dev.datlag.burningseries.module.NetworkModule
import dev.datlag.burningseries.other.StateSaver
import dev.datlag.sekret.NativeLoader
import dev.datlag.tooling.scopeCatching
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bindSingleton
import org.kodein.di.instance

class App : MultiDexApplication(), DIAware {

    override val di: DI = DI {
        bindSingleton<Context> {
            applicationContext
        }

        import(NetworkModule.di)
    }

    @OptIn(DelicateCoilApi::class)
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())

            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .permitDiskReads()
                    .permitDiskWrites()
                    .permitCustomSlowCalls()
                    .penaltyLog()
                    .penaltyDialog()
                    .build()
            )
        }
        StateSaver.sekretLibraryLoaded = NativeLoader.loadLibrary("sekret") || scopeCatching {
            System.loadLibrary("sekret")
        }.onFailure {
            Napier.e("Sekret loading error", it)
        }.isSuccess

        val imageLoader by di.instance<ImageLoader>()
        SingletonImageLoader.setUnsafe(imageLoader)
    }
}