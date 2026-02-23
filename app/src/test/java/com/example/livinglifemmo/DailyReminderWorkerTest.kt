package com.example.livinglifemmo

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class DailyReminderWorkerTest {

    @Test
    fun calculateInitialDelayMillis_isAtLeastFiveMinutes() {
        val delay = DailyReminderWorker.calculateInitialDelayMillis(
            dailyResetHour = 7,
            nowMillis = 1_700_000_000_000L
        )
        assertTrue(delay >= TimeUnit.MINUTES.toMillis(5))
    }

    @Test
    fun calculateInitialDelayMillis_isLessThanOrEqualToOneDay() {
        val delay = DailyReminderWorker.calculateInitialDelayMillis(
            dailyResetHour = 21,
            nowMillis = 1_700_000_000_000L
        )
        assertTrue(delay <= TimeUnit.DAYS.toMillis(1))
    }
}
