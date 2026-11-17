package com.example

import android.content.Context
import android.os.Vibrator
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.engine.HapticFeedbackHelper
import com.example.model.HeatingStatus
import com.example.model.HeatingStrength
import com.example.model.SafetySettings
import com.example.model.ThermalLevel
import com.example.model.WarmerUiState
import com.example.ui.components.HeroThermalDashboardCard
import com.example.ui.components.TemperatureArcGaugeCard
import com.example.ui.theme.EmberCoreTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun readAppNameAndRenderTemperatureGauge() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kelvin", appName)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            EmberCoreTheme(useOledBlack = true) {
                TemperatureArcGaugeCard(
                    tempC = 36.5f,
                    maxTempCutoffC = 43.0f,
                    useFahrenheit = false,
                    thermalLevel = ThermalLevel.LIGHT,
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(300L)
        composeTestRule.onNodeWithText("36.5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cutoff 43.0°C").assertIsDisplayed()

        val state = WarmerUiState(
            status = HeatingStatus.WARMING,
            safetySettings = SafetySettings(useOledBlackTheme = true),
        )
        assertEquals(true, state.isHeating)
        assertEquals(true, state.safetySettings.useOledBlackTheme)
    }

    @Test
    fun hapticFeedbackTriggersOnIntensityAndSafetyCutoff() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val shadowVibrator = Shadows.shadowOf(vibrator)
        shadowVibrator.setHasVibrator(true)

        val helper = HapticFeedbackHelper(context)
        helper.vibrateIntensityToggle(HeatingStrength.HIGH)
        assertTrue(shadowVibrator.isVibrating)

        vibrator.cancel()
        helper.vibrateThermalSafetyCutoff()
        assertTrue(shadowVibrator.isVibrating)
    }
}
