package com.zackwhye.secondbrain.navigation

import kotlinx.serialization.Serializable

sealed interface Destinations {
    @Serializable data object Home : Destinations
    @Serializable data class ItemDetail(val itemId: String) : Destinations
    @Serializable data object Ask : Destinations
}
