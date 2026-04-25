package com.example.vpnclient.network

import com.example.vpnclient.data.VpnServer
import com.example.vpnclient.parser.ConfigParser
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Загрузка подписки по URL и конвертация в список серверов.
 */
class SubscriptionManager(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    fun fetchAndParse(url: String): Result<List<VpnServer>> {
        return runCatching {
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Ошибка HTTP: ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                ConfigParser.parseSubscription(body)
            }
        }
    }
}
