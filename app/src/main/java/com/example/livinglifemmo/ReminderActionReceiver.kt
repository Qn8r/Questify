package com.example.livinglifemmo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE_ONE) return
        val changed = runBlocking { completeNextQuestFromExternalAction(context) }
        if (changed) QuestifyWidgetProvider.updateAll(context)
    }

    companion object {
        const val ACTION_COMPLETE_ONE = "com.example.livinglifemmo.action.COMPLETE_ONE_FROM_NOTIFICATION"
    }
}
