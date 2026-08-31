package com.zackwhye.secondbrain.feature.person.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackwhye.secondbrain.core.data.FactRepository
import com.zackwhye.secondbrain.core.model.Fact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 5_000L

@HiltViewModel
class PersonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val factRepository: FactRepository,
) : ViewModel() {

    private val subject = checkNotNull(savedStateHandle.get<String>("subject")) { "Person route requires subject" }

    val uiState: StateFlow<PersonUiState> = factRepository.observeFactsForSubject(subject)
        .map<List<Fact>, PersonUiState> { facts ->
            if (facts.isEmpty()) PersonUiState.Empty else PersonUiState.Ready(facts.toPersonUiModel(subject))
        }
        .catch { emit(PersonUiState.Error(message = "Couldn't load this person.", retryable = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonUiState.Loading)

    /** Driven by the Route's LaunchedEffect, never self-started — see ItemDetailViewModel for why. */
    suspend fun pollFactsWhileActive() {
        while (true) {
            factRepository.pollFacts()
            delay(POLL_INTERVAL_MS)
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

/**
 * Current facts are the non-superseded ones. For each, its immediate predecessor is the fact that
 * names it in `supersededBy` — that pair is "what changed", with both dates and both quotes.
 */
internal fun List<Fact>.toPersonUiModel(subject: String): PersonUiModel {
    val current = filter { it.supersededBy == null }.sortedWith(compareBy({ it.category }, { it.value }))
    return PersonUiModel(
        subject = firstOrNull()?.subject ?: subject,
        facts = current.map { fact ->
            val predecessor = firstOrNull { it.supersededBy == fact.id }
            CurrentFactUiModel(
                category = fact.category,
                value = fact.value,
                quote = fact.quote,
                validFromLabel = dateFormatter.format(fact.validFrom),
                sourceItemId = fact.sourceItemId,
                previous = predecessor?.let {
                    PreviousFactUiModel(
                        value = it.value,
                        quote = it.quote,
                        validFromLabel = dateFormatter.format(it.validFrom),
                        sourceItemId = it.sourceItemId,
                    )
                },
            )
        },
    )
}
