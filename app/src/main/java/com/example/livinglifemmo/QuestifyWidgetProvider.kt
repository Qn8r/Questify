package com.example.livinglifemmo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class QuestifyWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_QUICK_COMPLETE) {
            val changed = runBlocking { completeNextQuestFromExternalAction(context) }
            if (changed) updateAll(context)
        }
    }

    companion object {
        private const val ACTION_QUICK_COMPLETE = "com.example.livinglifemmo.widget.COMPLETE_ONE"

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, QuestifyWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) updateWidgets(context, appWidgetManager, ids)
        }

        private fun updateWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val prefs = runBlocking { context.dataStore.data.first() }
            val base = deserializeQuests(prefs[Keys.QUESTS].orEmpty())
            val done = parseIds(prefs[Keys.COMPLETED]).size
            val total = base.size.coerceAtLeast(1)
            val percent = ((done * 100f) / total.toFloat()).toInt().coerceIn(0, 100)
            val quickIntent = Intent(context, QuestifyWidgetProvider::class.java).apply { action = ACTION_QUICK_COMPLETE }
            val quickPending = PendingIntent.getBroadcast(
                context,
                1002,
                quickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val launchIntent = Intent(context, MainActivity::class.java)
            val launchPending = PendingIntent.getActivity(
                context,
                1003,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            ids.forEach { id ->
                val rv = RemoteViews(context.packageName, R.layout.questify_widget)
                rv.setTextViewText(R.id.widgetTitle, "Questify Today")
                rv.setTextViewText(R.id.widgetProgress, "$done/$total done ($percent%)")
                rv.setOnClickPendingIntent(R.id.widgetQuickComplete, quickPending)
                rv.setOnClickPendingIntent(R.id.widgetRoot, launchPending)
                manager.updateAppWidget(id, rv)
            }
        }
    }
}
