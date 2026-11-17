package com.example

import com.example.model.formatTemperature
import com.example.model.formatTimerSeconds
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun temperatureFormatting_supportsCelsiusAndFahrenheit() {
        assertEquals("43.0°C", formatTemperature(43.0f, useFahrenheit = false))
        assertEquals("109.4°F", formatTemperature(43.0f, useFahrenheit = true))
    }

    @Test
    fun timerFormatting_formatsMinutesAndSeconds() {
        assertEquals("10:00", formatTimerSeconds(600))
        assertEquals("01:05", formatTimerSeconds(65))
        assertEquals("00:00", formatTimerSeconds(-3))
    }
}
