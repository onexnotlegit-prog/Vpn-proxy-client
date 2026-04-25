package com.example.vpnclient.vpn

import android.content.Context
import com.example.vpnclient.data.VpnServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Контроллер для запуска/остановки Xray через внешнюю библиотеку.
 * Используется reflection, чтобы избежать жёсткой привязки к package name конкретной реализации.
 */
object VpnEngineController {

    private val _state = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    val state: StateFlow<VpnConnectionState> = _state

    private val _logs = MutableStateFlow(listOf("[init] Готово к подключению"))
    val logs: StateFlow<List<String>> = _logs

    private var currentServer: VpnServer? = null

    fun connect(context: Context, server: VpnServer) {
        pushLog("[connect] Подключение к ${server.remark}")
        _state.value = VpnConnectionState.CONNECTING
        currentServer = server

        val started = runCatching {
            tryStartThroughKnownControllers(context, server)
        }.getOrElse {
            pushLog("[error] ${it.message}")
            false
        }

        _state.value = if (started) VpnConnectionState.CONNECTED else VpnConnectionState.ERROR
    }

    fun disconnect(context: Context) {
        runCatching {
            stopThroughKnownControllers(context)
            pushLog("[disconnect] VPN отключен")
        }.onFailure {
            pushLog("[error] Ошибка остановки: ${it.message}")
        }
        _state.value = VpnConnectionState.DISCONNECTED
        currentServer = null
    }

    fun switchServer(context: Context, server: VpnServer) {
        pushLog("[switch] Переключение на ${server.remark}")
        disconnect(context)
        connect(context, server)
    }

    private fun tryStartThroughKnownControllers(context: Context, server: VpnServer): Boolean {
        val candidates = listOf(
            "com.dabut.lib.v2ray.V2rayController",
            "com.dabut.lib.v2ray.core.V2rayController"
        )

        for (className in candidates) {
            val clazz = runCatching { Class.forName(className) }.getOrNull() ?: continue

            // init(Context, Int, String)
            runCatching {
                val initMethod = clazz.getDeclaredMethod(
                    "init",
                    Context::class.java,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                initMethod.invoke(null, context, android.R.drawable.ic_dialog_info, "VLESS VPN Client")
                pushLog("[core] init via $className")
            }

            // startV2ray(Context, String, String, ArrayList<String>?)
            val startResult = runCatching {
                val startMethod = clazz.getDeclaredMethod(
                    "startV2ray",
                    Context::class.java,
                    String::class.java,
                    String::class.java,
                    ArrayList::class.java
                )
                startMethod.invoke(null, context, server.remark, server.rawConfig, null)
                true
            }.getOrDefault(false)

            if (startResult) {
                pushLog("[core] startV2ray OK via $className")
                return true
            }
        }

        pushLog("[core] Не удалось вызвать V2Ray библиотеку. Проверьте зависимость.")
        return false
    }

    private fun stopThroughKnownControllers(context: Context) {
        val candidates = listOf(
            "com.dabut.lib.v2ray.V2rayController",
            "com.dabut.lib.v2ray.core.V2rayController"
        )

        for (className in candidates) {
            val clazz = runCatching { Class.forName(className) }.getOrNull() ?: continue
            runCatching {
                val stopMethod = clazz.getDeclaredMethod("stopV2ray", Context::class.java)
                stopMethod.invoke(null, context)
                pushLog("[core] stopV2ray OK via $className")
                return
            }
        }
    }

    fun pushLog(log: String) {
        _logs.value = (_logs.value + "${System.currentTimeMillis()} $log").takeLast(200)
    }
}
