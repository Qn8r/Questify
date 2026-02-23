package com.example.livinglifemmo

import android.util.Log

object AppLog {
    private const val TAG = "Questify"
    private const val MAX_LOGS = 200
    private val logs = ArrayDeque<String>()

    private fun push(level: String, message: String, throwable: Throwable?) {
        val line = buildString {
            append(System.currentTimeMillis())
            append(" [")
            append(level)
            append("] ")
            append(message)
            if (throwable != null) {
                append(" :: ")
                append(throwable::class.java.simpleName)
                append(": ")
                append(throwable.message)
            }
        }
        if (logs.size >= MAX_LOGS) logs.removeFirst()
        logs.addLast(line)
    }

    fun d(message: String) {
        Log.d(TAG, message)
        push("D", message, null)
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
        push("W", message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        push("E", message, throwable)
    }

    fun exportRecentLogs(): String = logs.joinToString("\n")

    fun event(name: String, details: String = "") {
        val msg = if (details.isBlank()) "event:$name" else "event:$name | $details"
        d(msg)
    }
}
