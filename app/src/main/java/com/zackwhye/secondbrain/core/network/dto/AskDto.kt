package com.zackwhye.secondbrain.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AskRequestDto(val question: String)

@Serializable
data class AskResponseDto(
    val hasResults: Boolean,
    val answer: String? = null,
    val citations: List<AskCitationDto> = emptyList(),
)

@Serializable
data class AskCitationDto(val itemId: String, val title: String)
