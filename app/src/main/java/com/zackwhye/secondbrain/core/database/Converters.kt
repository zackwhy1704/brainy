package com.zackwhye.secondbrain.core.database

import androidx.room.TypeConverter
import com.zackwhye.secondbrain.core.database.entity.BriefStatus
import com.zackwhye.secondbrain.core.database.entity.ItemLinkType
import com.zackwhye.secondbrain.core.database.entity.ItemSourceType
import com.zackwhye.secondbrain.core.database.entity.ItemSyncState
import com.zackwhye.secondbrain.core.database.entity.SourceDoor

private const val LIST_DELIMITER = ""

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(LIST_DELIMITER)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(LIST_DELIMITER)

    @TypeConverter
    fun fromItemSourceType(value: ItemSourceType): String = value.name

    @TypeConverter
    fun toItemSourceType(value: String): ItemSourceType = ItemSourceType.valueOf(value)

    @TypeConverter
    fun fromItemSyncState(value: ItemSyncState): String = value.name

    @TypeConverter
    fun toItemSyncState(value: String): ItemSyncState = ItemSyncState.valueOf(value)

    @TypeConverter
    fun fromBriefStatus(value: BriefStatus): String = value.name

    @TypeConverter
    fun toBriefStatus(value: String): BriefStatus = BriefStatus.valueOf(value)

    @TypeConverter
    fun fromItemLinkType(value: ItemLinkType): String = value.name

    @TypeConverter
    fun toItemLinkType(value: String): ItemLinkType = ItemLinkType.valueOf(value)

    @TypeConverter
    fun fromSourceDoor(value: SourceDoor): String = value.name

    @TypeConverter
    fun toSourceDoor(value: String): SourceDoor = SourceDoor.valueOf(value)
}
