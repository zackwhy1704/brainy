package com.zackwhye.secondbrain.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Empty body — GoTrue's /auth/v1/signup treats a body with no email/password as an anonymous sign-in. */
@Serializable
class AnonymousSignInRequest

@Serializable
data class AuthSessionDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    val user: AuthUserDto,
)

@Serializable
data class AuthUserDto(val id: String)

@Serializable
data class RefreshTokenRequest(@SerialName("refresh_token") val refreshToken: String)
