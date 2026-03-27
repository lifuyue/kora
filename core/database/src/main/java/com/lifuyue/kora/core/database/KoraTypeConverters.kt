package com.lifuyue.kora.core.database

import androidx.room.TypeConverter

class KoraTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.joinToString(separator = "\u001F")

    @TypeConverter
    fun toStringList(value: String?): List<String>? = value?.split("\u001F")
}
