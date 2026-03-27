package com.lifuyue.kora.core.testing

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider

object RoomTestFactory {
    inline fun <reified T : RoomDatabase> inMemoryDatabase(
        context: Context = ApplicationProvider.getApplicationContext(),
        noinline configure: androidx.room.RoomDatabase.Builder<T>.() -> Unit = {},
    ): T =
        Room
            .inMemoryDatabaseBuilder(context, T::class.java)
            .allowMainThreadQueries()
            .apply(configure)
            .build()
}
