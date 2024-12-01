package dev.datlag.burningseries.other

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import de.jensklingenberg.ktorfit.ktorfit
import dev.datlag.burningseries.github.GitHub
import dev.datlag.burningseries.github.model.UserAndRelease
import dev.datlag.mimasu.core.update.IUpdateCheckCallback
import dev.datlag.mimasu.core.update.IUpdateInfo
import dev.datlag.mimasu.core.update.IUpdateService
import dev.datlag.tooling.async.suspendCatching
import dev.datlag.tooling.safeCast
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.instanceOrNull

class MimasuUpdateService : LifecycleService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        val di = applicationContext.safeCast<DIAware>()?.di ?: (application as? DIAware)?.di ?: return null

        return Binder(lifecycleScope, di)
    }

    data class UpdateInfo(
        val available: Boolean,
        val required: Boolean,
        val storeURL: String? = null,
        val directDownload: String? = null
    ) : IUpdateInfo.Stub() {
        override fun available(): Boolean {
            return available
        }

        override fun required(): Boolean {
            return required
        }

        override fun storeURL(): String? {
            return storeURL
        }

        override fun directDownload(): String? {
            return directDownload
        }
    }

    class Binder(
        val scope: CoroutineScope,
        override val di: DI
    ) : IUpdateService.Stub(), DIAware {

        private val github by instance<GitHub>()
        private val appVersion by instanceOrNull<String>("APP_VERSION")

        override fun hasUpdate(callback: IUpdateCheckCallback?) {
            scope.launch(Dispatchers.IO) {

                val installed = appVersion?.ifBlank { null } ?: return@launch withContext(Dispatchers.Main) {
                    callback?.onUpdateInfo(
                        UpdateInfo(
                            available = false,
                            required = false
                        )
                    )
                }

                val release = suspendCatching {
                    github.getLatestRelease(
                        owner = Constants.GITHUB_OWNER_NAME,
                        repo = Constants.GITHUB_REPO_NAME
                    )
                }.getOrNull()
                    ?.let(::UserAndRelease)
                    ?.release
                    ?.asUpdateOrNull(installed)

                withContext(Dispatchers.Main) {
                    callback?.onUpdateInfo(
                        UpdateInfo(
                            available = release?.androidAsset != null,
                            required = true,
                            storeURL = release?.url?.ifBlank { null },
                            directDownload = release?.androidAsset?.downloadUrl?.toString()?.ifBlank { null }
                        )
                    )
                }
            }
        }
    }
}