package com.zackwhye.secondbrain.core.data

import com.zackwhye.secondbrain.core.model.AskCitation
import com.zackwhye.secondbrain.core.model.AskResult
import com.zackwhye.secondbrain.core.network.FakeAuthSessionManager
import com.zackwhye.secondbrain.core.network.api.FakeSupabaseAskApi
import com.zackwhye.secondbrain.core.network.dto.AskCitationDto
import com.zackwhye.secondbrain.core.network.dto.AskResponseDto
import com.zackwhye.secondbrain.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import java.net.UnknownHostException

/** Covers the real AskRepositoryImpl: the only repository that surfaces failures to its caller. */
class AskRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthSessionManager()
    private val api = FakeSupabaseAskApi()
    private val repository = AskRepositoryImpl(auth, api)

    @Test
    fun `answered response maps answer and citations`() = runTest {
        api.response = AskResponseDto(
            hasResults = true,
            answer = "Sarah is based in Singapore.",
            citations = listOf(AskCitationDto(itemId = "item-1", title = "Notes with Sarah")),
        )

        val result = repository.ask("Where is Sarah?")

        assertEquals("Where is Sarah?", api.lastQuestion)
        assertEquals(
            AskResult.Answered("Sarah is based in Singapore.", listOf(AskCitation("item-1", "Notes with Sarah"))),
            result,
        )
    }

    @Test
    fun `no retrieval maps to NoResults, never to an empty answer`() = runTest {
        api.response = AskResponseDto(hasResults = false, answer = "should be ignored")

        assertEquals(AskResult.NoResults, repository.ask("Anything?"))
    }

    @Test
    fun `non-2xx surfaces as HttpException to the caller`() = runTest {
        api.codesQueue.clear(); api.codesQueue.add(502)

        val thrown = runCatching { repository.ask("q") }.exceptionOrNull()
        assertEquals(502, (thrown as HttpException).code())
    }

    @Test
    fun `a 401 refreshes the token once and retries transparently`() = runTest {
        api.response = AskResponseDto(hasResults = true, answer = "ok")
        api.codesQueue.clear(); api.codesQueue.add(401); api.codesQueue.add(200)

        val result = repository.ask("q")

        assertEquals(AskResult.Answered("ok", emptyList()), result)
        assertEquals(1, auth.refreshCallCount)
        assertEquals(2, api.callCount)
    }

    @Test
    fun `network failure propagates instead of being swallowed`() = runTest {
        auth.shouldFail = true

        val thrown = runCatching { repository.ask("q") }.exceptionOrNull()
        assertEquals(UnknownHostException::class.java, thrown?.javaClass)
        assertEquals(0, api.callCount)
    }
}
