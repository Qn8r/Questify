package com.example.livinglifemmo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class CommunitySyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!SupabaseApi.isConfigured) return Result.success()
        val prefs = applicationContext.dataStore.data.first()
        val userId = prefs[Keys.COMMUNITY_USER_ID].orEmpty()
        if (userId.isBlank()) return Result.success()

        val queue = normalizeCommunitySyncQueue(
            deserializeCommunitySyncQueue(prefs[Keys.COMMUNITY_SYNC_QUEUE])
        )
        if (queue.isEmpty()) return Result.success()
        AppLog.d("CommunitySyncWorker started with ${queue.size} queued tasks.")

        val remaining = mutableListOf<CommunitySyncTask>()
        queue.forEach { task ->
            val ok = when (task.type) {
                CommunitySyncTaskType.PUBLISH_POST -> task.post?.let { SupabaseApi.publishPost(it) } ?: true
                CommunitySyncTaskType.FOLLOW_AUTHOR -> task.authorId?.let { SupabaseApi.upsertFollow(userId, it) } ?: true
                CommunitySyncTaskType.UNFOLLOW_AUTHOR -> task.authorId?.let { SupabaseApi.deleteFollow(userId, it) } ?: true
                CommunitySyncTaskType.RATE_POST -> {
                    val postId = task.postId
                    val stars = task.stars
                    if (postId != null && stars != null) {
                        SupabaseApi.upsertRatingAndRefreshAggregate(userId, postId, stars)
                    } else {
                        true
                    }
                }
                CommunitySyncTaskType.INCREMENT_REMIX -> {
                    val postId = task.postId
                    val remixCount = task.currentRemixCount
                    if (postId != null && remixCount != null) {
                        SupabaseApi.incrementRemix(postId, remixCount)
                    } else {
                        true
                    }
                }
            }
            if (!ok) {
                val nextAttempt = task.attemptCount + 1
                if (nextAttempt <= 5) remaining += task.copy(attemptCount = nextAttempt)
            }
        }

        val normalizedRemaining = normalizeCommunitySyncQueue(remaining)
        applicationContext.dataStore.edit { p ->
            p[Keys.COMMUNITY_SYNC_QUEUE] = serializeCommunitySyncQueue(normalizedRemaining)
        }
        if (normalizedRemaining.isNotEmpty()) {
            AppLog.w("CommunitySyncWorker leaving ${normalizedRemaining.size} tasks for retry.")
        }
        return if (normalizedRemaining.isEmpty()) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "questify_community_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val req = OneTimeWorkRequestBuilder<CommunitySyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                req
            )
        }
    }
}
