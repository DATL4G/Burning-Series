package dev.datlag.burningseries.other

import dev.datlag.burningseries.model.Series
import dev.datlag.burningseries.model.SeriesData
import dev.datlag.burningseries.model.serializer.SerializableImmutableSet
import dev.datlag.k2k.Host
import dev.datlag.k2k.connect.Connection
import dev.datlag.k2k.connect.connection
import dev.datlag.k2k.discover.Discovery
import dev.datlag.k2k.discover.discovery
import dev.datlag.nanoid.NanoIdUtils
import dev.datlag.skeo.DirectLink
import dev.datlag.tooling.async.suspendCatching
import dev.datlag.tooling.compose.ioDispatcher
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

data object K2Kast : AutoCloseable {

    private lateinit var showClient: Discovery
    private lateinit var searchClient: Discovery

    private lateinit var connection: Connection

    private var scope: CoroutineScope? = null
    private val connectedHost = MutableStateFlow<Host?>(null)

    private const val DISCOVER_PORT = 7330
    private const val CONNECTION_PORT = 7332

    val code: String by lazy {
        NanoIdUtils.randomNanoId(
            alphabet = "0123456789".toCharArray(),
            size = 6
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    val devices: StateFlow<ImmutableSet<Device>>
        get() = if (::searchClient.isInitialized) {
            combine(
                connectedHost,
                searchClient.peers
            ) { selected, all ->
                all.map {
                    Device(
                        host = it,
                        selected = selected == it
                    )
                }.toImmutableSet()
            }.stateIn(
                scope = scope ?: (GlobalScope + ioDispatcher()),
                started = SharingStarted.WhileSubscribed(),
                initialValue = searchClient.peers.value.map(::Device).toImmutableSet()
            )
        } else {
            MutableStateFlow(persistentSetOf())
        }

    val connectedDevice: Device?
        get() = devices.value.firstOrNull { it.selected }

    fun initialize(scope: CoroutineScope) {
        this.scope = scope

        initializeShow(scope)
        initializeSearch(scope)
        initializeConnection(scope)
    }

    private fun initializeShow(scope: CoroutineScope) {
        if (::showClient.isInitialized) {
            return
        }

        showClient = scope.discovery {
            setShowTimeout(0L)
            setPort(DISCOVER_PORT)
        }
    }

    private fun initializeSearch(scope: CoroutineScope) {
        if (::searchClient.isInitialized) {
            return
        }

        searchClient = scope.discovery {
            setSearchTimeout(0L)
            setPort(DISCOVER_PORT)
        }
    }

    private fun initializeConnection(scope: CoroutineScope) {
        if (::connection.isInitialized) {
            return
        }

        connection = scope.connection {
            setPort(CONNECTION_PORT)
        }
    }

    fun show(name: String) {
        showClient.show(name = name, filterMatch = code)
    }

    fun hide() {
        showClient.hide()
    }

    fun search(code: String?) {
        if (code.isNullOrBlank() || code.length != 6) {
            searchClient.lose()
            return
        }

        searchClient.search(
            filter = "^$code$".toRegex()
        )
    }

    fun lose() {
        searchClient.lose()
    }

    fun connect(host: Host) {
        connectedHost.update { host }
    }

    fun connect(device: Device) = connect(device.host)

    fun disconnect() {
        connectedHost.update { null }
    }

    fun receive(listener: suspend (ByteArray) -> Unit) = connection.receive(listener)

    suspend fun send(byteArray: ByteArray, host: Host? = connectedDevice?.host) {
        if (host == null) {
            return
        }

        suspendCatching {
            connection.sendNow(byteArray, host)
        }
    }
    suspend fun send(byteArray: ByteArray, device: Device? = connectedDevice) = send(byteArray, device?.host)

    suspend fun watch(
        episode: Series.Episode,
        host: Host? = connectedDevice?.host
    ) = send(
        byteArray = episode.href.encodeToByteArray(),
        host = host
    )

    suspend fun watch(
        episode: Series.Episode,
        device: Device? = connectedDevice
    ) = watch(
        episode = episode,
        host = device?.host
    )

    override fun close() {
        this.scope?.cancel()
        this.scope = null

        closeShow()
        closeSearch()
    }

    private fun closeShow() {
        if (!::showClient.isInitialized) {
            return
        }

        showClient.close()
    }

    private fun closeSearch() {
        if (!::searchClient.isInitialized) {
            return
        }

        searchClient.close()
    }

    @Serializable
    data class Device(
        val host: Host,
        val name: String = host.name,
        val selected: Boolean = connectedHost.value == host
    )
}