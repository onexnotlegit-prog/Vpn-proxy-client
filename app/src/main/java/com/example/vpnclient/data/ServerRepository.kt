package com.example.vpnclient.data

import kotlinx.coroutines.flow.Flow

class ServerRepository(
    private val dao: VpnServerDao
) {
    fun observeServers(): Flow<List<VpnServer>> = dao.observeAll()

    suspend fun addServer(server: VpnServer) = dao.insert(server)

    suspend fun addServers(servers: List<VpnServer>) = dao.insertAll(servers)

    suspend fun removeServer(server: VpnServer) = dao.delete(server)
}
