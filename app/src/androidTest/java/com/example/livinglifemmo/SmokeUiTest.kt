package com.example.livinglifemmo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SmokeUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsDailyQuestHeaderOnLaunch() {
        rule.onNodeWithText("DAILY QUESTS").assertIsDisplayed()
    }

    @Test
    fun opensSettingsFromDrawer() {
        rule.onNodeWithText("Drawer").performClick()
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Gameplay").assertIsDisplayed()
    }

    @Test
    fun opensCommunityFromDrawer() {
        rule.onNodeWithText("Drawer").performClick()
        rule.onNodeWithText("Community").performClick()
        rule.onNodeWithText("Discover").assertIsDisplayed()
    }

    @Test
    fun opensShopAndCalendarFromDrawer() {
        rule.onNodeWithText("Drawer").performClick()
        rule.onNodeWithText("Shop").performClick()
        rule.onNodeWithText("Shop Catalog").assertIsDisplayed()
        rule.onNodeWithText("Drawer").performClick()
        rule.onNodeWithText("Calendar").performClick()
        rule.onNodeWithText("Plans for", substring = true).assertIsDisplayed()
    }
}
