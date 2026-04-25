package com.example.vpnclient.parser

import android.util.Base64
import com.example.vpnclient.data.VpnServer
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Унифицированный парсер конфигов:
 * 1) vless:// URI
 * 2) JSON-конфиг Xray/V2Ray
 * 3) base64-подписка со списком строк
 */
object ConfigParser {

    fun parseSingleConfig(raw: String): Result<VpnServer> {
        val content = raw.trim()
        return runCatching {
            when {
                content.startsWith("vless://", ignoreCase = true) -> parseVlessUri(content)
                content.startsWith("{") -> parseJsonConfig(content)
                else -> throw IllegalArgumentException("Неподдерживаемый формат конфига")
            }
        }
    }

    fun parseSubscription(raw: String): List<VpnServer> {
        val decoded = decodeMaybeBase64(raw).trim()
        if (decoded.isEmpty()) return emptyList()

        return decoded
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line -> parseSingleConfig(line).getOrNull() }
            .toList()
    }

    private fun parseVlessUri(uriString: String): VpnServer {
        val uri = URI(uriString)
        val host = uri.host ?: throw IllegalArgumentException("Не найден host в vless URI")
        val port = if (uri.port == -1) 443 else uri.port
        val fragment = uri.fragment?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
        val remark = if (!fragment.isNullOrBlank()) fragment else "VLESS $host:$port"
        return VpnServer(
            remark = remark,
            rawConfig = uriString,
            host = host,
            port = port
        )
    }

    private fun parseJsonConfig(json: String): VpnServer {
        val jsonObj = JSONObject(json)
        val outbounds = jsonObj.optJSONArray("outbounds")
            ?: throw IllegalArgumentException("JSON-конфиг не содержит outbounds")

        val firstOutbound = outbounds.getJSONObject(0)
        val settings = firstOutbound.optJSONObject("settings")
        val vnextArray = settings?.optJSONArray("vnext")
            ?: throw IllegalArgumentException("JSON-конфиг не содержит vnext")

        val firstNode = vnextArray.getJSONObject(0)
        val host = firstNode.optString("address", "unknown")
        val port = firstNode.optInt("port", 443)

        return VpnServer(
            remark = "JSON $host:$port",
            rawConfig = json,
            host = host,
            port = port
        )
    }

    private fun decodeMaybeBase64(input: String): String {
        return try {
            val normalized = input.replace("\n", "").trim()
            val decoded = Base64.decode(normalized, Base64.DEFAULT)
            val result = decoded.toString(StandardCharsets.UTF_8)
            if (result.contains("vless://") || result.contains("{")) result else input
        } catch (_: Exception) {
            input
        }
    }
}
