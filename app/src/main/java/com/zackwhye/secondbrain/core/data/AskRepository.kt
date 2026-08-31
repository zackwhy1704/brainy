package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.model.AskResult

interface AskRepository {
    suspend fun ask(question: String): AskResult
}
