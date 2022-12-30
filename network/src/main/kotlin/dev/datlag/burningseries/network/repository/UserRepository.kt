package dev.datlag.burningseries.network.repository

import androidx.datastore.core.DataStore
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import dev.datlag.burningseries.datastore.common.updateBSAccount
import dev.datlag.burningseries.datastore.preferences.UserSettings
import dev.datlag.burningseries.network.BurningSeries
import dev.datlag.burningseries.network.model.UserLogin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

class UserRepository(
    private val api: BurningSeries,
    private val settings: DataStore<UserSettings>
) {

    fun bsLogin(username: String, password: String, header: String) = flow {
        val response = api.login(header)
        emit(when (response) {
            is ApiSuccessResponse -> {
                val info = response.body

                settings.updateBSAccount(
                    username,
                    password,
                    info?.loginCookie?.name,
                    info?.loginCookie?.value,
                    info?.loginCookie?.maxAge,
                    info?.loginCookie?.expires,
                    info?.uidCookie?.name,
                    info?.uidCookie?.value,
                    info?.uidCookie?.maxAge,
                    info?.uidCookie?.expires
                ) to true
            }
            else -> settings.updateBSAccount(
                username,
                password
            ) to false
        })
    }.flowOn(Dispatchers.IO)
}