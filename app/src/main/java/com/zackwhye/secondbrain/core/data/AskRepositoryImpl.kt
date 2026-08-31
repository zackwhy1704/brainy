package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.BuildConfig
import com.zackwhye.secondbrain.core.model.AskCitation
import com.zackwhye.secondbrain.core.model.AskResult
import com.zackwhye.secondbrain.core.network.AuthSessionManager
import com.zackwhye.secondbrain.core.network.api.SupabaseAskApi
import com.zackwhye.secondbrain.core.network.dto.AskRequestDto
import com.zackwhye.secondbrain.core.network.withAuthRetry
import retrofit2.HttpException
import javax.inject.Inject

class AskRepositoryImpl @Inject constructor(
    private val authSessionManager: AuthSessionManager,
    private val askApi: SupabaseAskApi,
) : AskRepository {

    override suspend fun ask(question: String): AskResult {
        val response = authSessionManager.withAuthRetry { bearer ->
            askApi.ask(authorization = bearer, apiKey = BuildConfig.SUPABASE_ANON_KEY, body = AskRequestDto(question))
        }
        if (!response.isSuccessful) throw HttpException(response)
        val body = checkNotNull(response.body()) { "ask() returned 2xx with an empty body" }

        return if (body.hasResults) {
            AskResult.Answered(
                answer = body.answer.orEmpty(),
                citations = body.citations.map { AskCitation(itemId = it.itemId, title = it.title) },
            )
        } else {
            AskResult.NoResults
        }
    }
}
