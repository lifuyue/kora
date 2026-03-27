package com.lifuyue.kora.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Test
    fun swapsMainDispatcherForTests() =
        runTest(testDispatcher) {
            val deferred = async(Dispatchers.Main) { "main-dispatcher" }

            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("main-dispatcher", deferred.await())
        }
}
