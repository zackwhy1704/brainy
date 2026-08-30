package com.zackwhye.secondbrain.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.Base64

/**
 * Pure JVM — deliberately not android.util.Base64, which is stubbed to
 * throw in plain unit tests. java.util.Base64 is real on both the JVM and
 * on-device (available since API 26, matching this project's minSdk).
 */
object JwtExpiry {

    /** Returns the `exp` claim (epoch seconds) from a JWT, or null if it's missing or the token is malformed. */
    fun decodeExpiryEpochSeconds(jwt: String): Long? = runCatching {
        val payloadSegment = jwt.split(".").getOrNull(1) ?: return null
        val padded = payloadSegment.padEnd((payloadSegment.length + 3) / 4 * 4, '=')
        val decoded = Base64.getUrlDecoder().decode(padded)
        Json.parseToJsonElement(String(decoded, Charsets.UTF_8)).jsonObject["exp"]?.jsonPrimitive?.longOrNull
    }.getOrNull()
}
