package com.zackwhye.secondbrain.feature.capture.domain

import com.zackwhye.secondbrain.core.data.ItemRepository
import com.zackwhye.secondbrain.core.model.CapturedContext
import javax.inject.Inject

/** The one funnel every door writes through (ARCHITECTURE.md → "The four doors"). */
class SaveCapturedItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
) {
    suspend operator fun invoke(context: CapturedContext): String = itemRepository.saveCapturedItem(context)
}
