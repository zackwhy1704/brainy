package com.zackwhye.secondbrain.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for the `delete_item_cascade` RPC — PostgREST maps fields to the function's named arguments. */
@Serializable
data class DeleteItemCascadeRequest(
    @SerialName("p_item_id") val itemId: String,
)
