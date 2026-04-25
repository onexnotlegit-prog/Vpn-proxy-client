package com.example.vpnclient.vpn

import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

/**
 * Базовый VpnService.
 * Реальный проксирующий трафик выполняется ядром Xray через библиотеку.
 */
class AppVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (tunInterface == null) {
            tunInterface = Builder()
                .setSession("VLESS VPN")
                .addAddress("10.10.0.2", 32)
                .addDnsServer("1.1.1.1")
                .addRoute("0.0.0.0", 0)
                .establish()
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        tunInterface?.close()
        tunInterface = null
        super.onDestroy()
    }
}
