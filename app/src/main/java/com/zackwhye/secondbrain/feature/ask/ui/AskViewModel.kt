package com.zackwhye.secondbrain.feature.ask.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackwhye.secondbrain.core.data.AskRepository
import com.zackwhye.secondbrain.core.model.AskResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AskViewModel @Inject constructor(
    private val askRepository: AskRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AskUiState>(AskUiState.Idle)
    val uiState: StateFlow<AskUiState> = _uiState.asStateFlow()

    private var lastQuestion: String? = null

    fun ask(question: String) {
        if (question.isBlank()) return
        lastQuestion = question
        _uiState.value = AskUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                when (val result = askRepository.ask(question)) {
                    is AskResult.Answered -> AskUiState.Ready(
                        answer = result.answer,
                        citations = result.citations.map { AskCitationUiModel(it.itemId, it.title) },
                    )
                    AskResult.NoResults -> AskUiState.EmptyRetrieval
                }
            } catch (e: Exception) {
                AskUiState.Error(message = "Couldn't get an answer. Check your connection.", retryable = true)
            }
        }
    }

    fun retry() {
        lastQuestion?.let(::ask)
    }
}
