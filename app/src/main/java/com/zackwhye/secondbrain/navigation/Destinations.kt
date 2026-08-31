package com.zackwhye.secondbrain.navigation

import kotlinx.serialization.Serializable

sealed interface Destinations {
    /** Shown once, before Home, until acknowledged (FirstRunStore). */
    @Serializable data object FirstRun : Destinations
    @Serializable data object Home : Destinations
    @Serializable data class ItemDetail(val itemId: String) : Destinations
    @Serializable data object Ask : Destinations
    /** [subject] is the person's name as written — the only identity facts carry in this build. */
    @Serializable data class Person(val subject: String) : Destinations
}
