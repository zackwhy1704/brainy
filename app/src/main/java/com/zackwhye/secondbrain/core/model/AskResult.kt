package com.zackwhye.secondbrain.core.model

data class AskCitation(val itemId: String, val title: String)

/** Phase 3 Ask: never answers from model knowledge without a citation — NoResults is an explicit
 * outcome, not an error, when retrieval finds nothing relevant. */
sealed interface AskResult {
    data class Answered(val answer: String, val citations: List<AskCitation>) : AskResult
    data object NoResults : AskResult
}
