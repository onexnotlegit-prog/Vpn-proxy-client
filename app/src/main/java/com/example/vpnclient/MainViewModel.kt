package com.example.vpnclient

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vpnclient.data.AppDatabase
import com.example.vpnclient.data.ServerRepository
import com.example.vpnclient.data.VpnServer
import com.example.vpnclient.network.SubscriptionManager
import com.example.vpnclient.parser.ConfigParser
import com.example.vpnclient.vpn.VpnConnectionState
import com.example.vpnclient.vpn.VpnEngineController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServerRepository(AppDatabase.getInstance(application).vpnServerDao())
    private val subscriptionManager = SubscriptionManager()

    val servers = repository.observeServers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val vpnState: StateFlow<VpnConnectionState> = VpnEngineController.state
    val logs = VpnEngineController.logs

    fun addFromClipboard() {
        viewModelScope.launch {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(getApplication())?.toString().orEmpty()

            ConfigParser.parseSingleConfig(text)
                .onSuccess { repository.addServer(it) }
                .onFailure { VpnEngineController.pushLog("[clipboard] ${it.message}") }
        }
    }

    fun addSubscription(url: String) {
        viewModelScope.launch {
            subscriptionManager.fetchAndParse(url)
                .onSuccess { list ->
                    if (list.isNotEmpty()) {
                        repository.addServers(list)
                        VpnEngineController.pushLog("[subscription] Добавлено серверов: ${list.size}")
                    } else {
                        VpnEngineController.pushLog("[subscription] Серверов не найдено")
                    }
                }
                .onFailure {
                    VpnEngineController.pushLog("[subscription] Ошибка: ${it.message}")
                }
        }
    }

    fun deleteServer(server: VpnServer) {
        viewModelScope.launch { repository.removeServer(server) }
    }

    fun connect(server: VpnServer) = VpnEngineController.connect(getApplication(), server)

    fun disconnect() = VpnEngineController.disconnect(getApplication())

    fun switch(server: VpnServer) = VpnEngineController.switchServer(getApplication(), server)
}
