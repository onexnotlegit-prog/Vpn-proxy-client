package com.example.vpnclient.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность VPN-сервера для хранения в локальной БД.
 */
@Entity(tableName = "vpn_servers")
data class VpnServer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remark: String,
    val rawConfig: String,
    val host: String,
    val port: Int,
    val createdAt: Long = System.currentTimeMillis()
)
