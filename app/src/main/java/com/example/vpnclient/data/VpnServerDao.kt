package com.example.vpnclient.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnServerDao {
    @Query("SELECT * FROM vpn_servers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VpnServer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: VpnServer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(servers: List<VpnServer>)

    @Delete
    suspend fun delete(server: VpnServer)

    @Query("DELETE FROM vpn_servers WHERE id = :serverId")
    suspend fun deleteById(serverId: Long)
}
