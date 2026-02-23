package com.example.livinglifemmo

import androidx.datastore.preferences.core.edit
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MigrationInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() = runBlocking {
        context.dataStore.edit { it.clear() }
    }

    @After
    fun teardown() = runBlocking {
        context.dataStore.edit { it.clear() }
    }

    @Test
    fun runDataMigrations_upgradesAndClampsLegacyValues() = runBlocking {
        context.dataStore.edit { p ->
            p[Keys.DATA_VERSION] = 1
            p[Keys.DAILY_RESET_HOUR] = 90
            p[Keys.FONT_SCALE_PERCENT] = 20
        }

        runDataMigrations(context)
        val prefs = context.dataStore.data.first()

        assertEquals(CURRENT_DATA_VERSION, prefs[Keys.DATA_VERSION])
        assertEquals(23, prefs[Keys.DAILY_RESET_HOUR])
        assertEquals(80, prefs[Keys.FONT_SCALE_PERCENT])
        assertTrue(prefs[Keys.DAILY_REMINDERS_ENABLED] ?: false)
    }
}
