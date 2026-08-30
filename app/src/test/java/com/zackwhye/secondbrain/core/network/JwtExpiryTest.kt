package com.zackwhye.secondbrain.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class JwtExpiryTest {

    private fun fakeJwt(payloadJson: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("{\"alg\":\"none\"}".toByteArray())
        val payload = encoder.encodeToString(payloadJson.toByteArray())
        return "$header.$payload.signature"
    }

    @Test
    fun `decodes the exp claim from a well-formed JWT`() {
        val jwt = fakeJwt("""{"exp":1893456000,"sub":"user-id"}""")

        assertEquals(1893456000L, JwtExpiry.decodeExpiryEpochSeconds(jwt))
    }

    @Test
    fun `returns null when the exp claim is missing`() {
        val jwt = fakeJwt("""{"sub":"user-id"}""")

        assertNull(JwtExpiry.decodeExpiryEpochSeconds(jwt))
    }

    @Test
    fun `returns null for a malformed token instead of throwing`() {
        assertNull(JwtExpiry.decodeExpiryEpochSeconds("not-a-jwt"))
        assertNull(JwtExpiry.decodeExpiryEpochSeconds(""))
    }
}
