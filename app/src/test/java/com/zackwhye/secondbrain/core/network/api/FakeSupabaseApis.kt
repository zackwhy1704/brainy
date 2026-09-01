package com.zackwhye.secondbrain.core.network.api

import com.zackwhye.secondbrain.core.network.dto.AskRequestDto
import com.zackwhye.secondbrain.core.network.dto.AskResponseDto
import com.zackwhye.secondbrain.core.network.dto.BriefDto
import com.zackwhye.secondbrain.core.network.dto.DeleteItemCascadeRequest
import com.zackwhye.secondbrain.core.network.dto.FactDto
import com.zackwhye.secondbrain.core.network.dto.ItemDto
import com.zackwhye.secondbrain.core.network.dto.RetryExtractionRequest
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/** Shared helper: a queue consumed one code per call, holding at the last entry. */
private fun MutableList<Int>.nextCode(): Int = if (size > 1) removeAt(0) else first()

private fun <T> respond(code: Int, body: T): Response<T> =
    if (code in 200..299) Response.success(body) else Response.error(code, "".toResponseBody())

class FakeSupabaseBriefsApi : SupabaseBriefsApi {
    var briefs: List<BriefDto> = emptyList()
    val getCodesQueue: MutableList<Int> = mutableListOf(200)
    var getCallCount: Int = 0
        private set
    var lastItemIdFilter: String? = null
        private set

    var triggerCode: Int = 200
    val triggeredItemIds: MutableList<String> = mutableListOf()

    override suspend fun getBriefs(authorization: String, apiKey: String, itemIdFilter: String): Response<List<BriefDto>> {
        getCallCount++
        lastItemIdFilter = itemIdFilter
        return respond(getCodesQueue.nextCode(), briefs)
    }

    override suspend fun triggerExtraction(authorization: String, apiKey: String, body: RetryExtractionRequest): Response<Unit> {
        triggeredItemIds.add(body.itemId)
        return respond(triggerCode, Unit)
    }
}

class FakeSupabaseFactsApi : SupabaseFactsApi {
    var facts: List<FactDto> = emptyList()
    val codesQueue: MutableList<Int> = mutableListOf(200)
    var callCount: Int = 0
        private set

    override suspend fun getFacts(authorization: String, apiKey: String, select: String, order: String): Response<List<FactDto>> {
        callCount++
        return respond(codesQueue.nextCode(), facts)
    }
}

class FakeSupabaseAskApi : SupabaseAskApi {
    var response: AskResponseDto = AskResponseDto(hasResults = false)
    val codesQueue: MutableList<Int> = mutableListOf(200)
    var callCount: Int = 0
        private set
    var lastQuestion: String? = null
        private set

    override suspend fun ask(authorization: String, apiKey: String, body: AskRequestDto): Response<AskResponseDto> {
        callCount++
        lastQuestion = body.question
        return respond(codesQueue.nextCode(), response)
    }
}

/** [responseCodesQueue] is consumed one code per call, then holds at the last entry (default: always succeed). */
class FakeSupabaseItemsApi : SupabaseItemsApi {
    var lastInserted: List<ItemDto>? = null
    val responseCodesQueue: MutableList<Int> = mutableListOf(201)
    var callCount: Int = 0
        private set

    var deleteResponseCode: Int = 204
    var deleteCallCount: Int = 0
        private set
    var lastDeletedItemId: String? = null
        private set

    override suspend fun insertItem(authorization: String, apiKey: String, prefer: String, item: List<ItemDto>): Response<Unit> {
        callCount++
        lastInserted = item
        val code = if (responseCodesQueue.size > 1) responseCodesQueue.removeAt(0) else responseCodesQueue.first()
        return if (code in 200..299) Response.success(Unit) else Response.error(code, "".toResponseBody())
    }

    override suspend fun deleteItemCascade(authorization: String, apiKey: String, body: DeleteItemCascadeRequest): Response<Unit> {
        deleteCallCount++
        lastDeletedItemId = body.itemId
        return if (deleteResponseCode in 200..299) Response.success(Unit) else Response.error(deleteResponseCode, "".toResponseBody())
    }
}

class FakeSupabaseStorageApi : SupabaseStorageApi {
    val deletedPaths: MutableList<String> = mutableListOf()

    override suspend fun upload(bucket: String, path: String, authorization: String, apiKey: String, contentType: String, file: RequestBody): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun delete(bucket: String, path: String, authorization: String, apiKey: String): Response<Unit> {
        deletedPaths.add("$bucket/$path")
        return Response.success(Unit)
    }
}
