package com.example.livinglifemmo

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SupabaseApi {
    private val gson = Gson()
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private const val DEFAULT_FEED_PAGE_SIZE = 30
    private const val MAX_FEED_PAGE_SIZE = 200

    enum class FeedSortMode { LATEST, OLDEST, POPULAR }

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private fun parseUtc(value: String, pattern: String): Long? {
        val fmt = SimpleDateFormat(pattern, Locale.US).apply { timeZone = utc }
        return runCatching { fmt.parse(value)?.time }.getOrNull()
    }

    private fun toEpochMillis(ts: String?): Long {
        if (ts.isNullOrBlank()) return System.currentTimeMillis()
        return parseUtc(ts, "yyyy-MM-dd'T'HH:mm:ss.SSSX")
            ?: parseUtc(ts, "yyyy-MM-dd'T'HH:mm:ssX")
            ?: System.currentTimeMillis()
    }

    private fun toIso(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = utc }
        return fmt.format(Date(millis))
    }

    private fun openConnection(
        method: String,
        path: String,
        query: Map<String, String> = emptyMap(),
        bearerToken: String? = null
    ): HttpURLConnection {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        val queryPart = if (query.isEmpty()) "" else query.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
        val full = if (queryPart.isBlank()) "$base$path" else "$base$path?$queryPart"
        val conn = URL(full).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
        val token = bearerToken ?: BuildConfig.SUPABASE_ANON_KEY
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Content-Type", "application/json")
        return conn
    }

    private suspend fun request(
        method: String,
        path: String,
        query: Map<String, String> = emptyMap(),
        body: String? = null,
        prefer: String? = null,
        bearerToken: String? = null
    ): Pair<Int, String> {
        var backoffMs = 300L
        var lastCode = 0
        var lastRaw = ""
        repeat(3) { attempt ->
            val call = runCatching {
                val conn = openConnection(method, path, query, bearerToken)
                if (!prefer.isNullOrBlank()) conn.setRequestProperty("Prefer", prefer)
                if (body != null) {
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                }
                val code = conn.responseCode
                val raw = runCatching {
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }.getOrDefault("")
                conn.disconnect()
                code to raw
            }
            if (call.isSuccess) {
                val (code, raw) = call.getOrThrow()
                lastCode = code
                lastRaw = raw
                if (code in 200..299 || (code in 400..499 && code != 429)) {
                    return code to raw
                }
            } else {
                val ex = call.exceptionOrNull()
                if (ex !is IOException) return 0 to ""
            }
            if (attempt < 2) {
                delay(backoffMs)
                backoffMs *= 2L
            }
        }
        return lastCode to lastRaw
    }

    private fun orderForFeed(sortMode: FeedSortMode): String {
        return when (sortMode) {
            FeedSortMode.LATEST -> "created_at.desc"
            FeedSortMode.OLDEST -> "created_at.asc"
            FeedSortMode.POPULAR -> "rating_average.desc,remix_count.desc,created_at.desc"
        }
    }

    private fun normalizeLimit(limit: Int): Int = limit.coerceIn(1, MAX_FEED_PAGE_SIZE)
    private fun normalizeOffset(offset: Int): Int = offset.coerceAtLeast(0)

    private fun returnedRowCount(raw: String): Int {
        if (raw.isBlank()) return 0
        return runCatching { gson.fromJson(raw, Array<IdRow>::class.java)?.size ?: 0 }.getOrDefault(0)
    }

    private suspend fun fetchRatingValuesForPost(postId: String): List<Int> = withContext(Dispatchers.IO) {
        val (code, raw) = request(
            method = "GET",
            path = "/rest/v1/community_ratings",
            query = mapOf("select" to "stars", "post_id" to "eq.$postId")
        )
        if (code !in 200..299 || raw.isBlank()) return@withContext emptyList()
        runCatching {
            gson.fromJson(raw, Array<PostRatingValue>::class.java)?.map { it.stars }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private suspend fun fetchPostAggregateSnapshot(postId: String): PostAggregateSnapshot? = withContext(Dispatchers.IO) {
        val (code, raw) = request(
            method = "GET",
            path = "/rest/v1/community_posts",
            query = mapOf(
                "select" to "rating_average,rating_count",
                "id" to "eq.$postId",
                "limit" to "1"
            )
        )
        if (code !in 200..299 || raw.isBlank()) return@withContext null
        runCatching {
            val row = gson.fromJson(raw, Array<JsonObject>::class.java)?.firstOrNull() ?: return@runCatching null
            val count = row.get("rating_count")?.takeUnless { it.isJsonNull }?.asInt ?: 0
            val avgElement = row.get("rating_average")
            val avgLiteral = avgElement?.takeUnless { it.isJsonNull }?.toString() ?: "0"
            val avg = avgElement?.takeUnless { it.isJsonNull }?.asDouble ?: 0.0
            PostAggregateSnapshot(ratingCount = count, ratingAverage = avg, ratingAverageLiteral = avgLiteral)
        }.getOrNull()
    }

    private suspend fun compareAndSetPostAggregate(
        postId: String,
        expected: PostAggregateSnapshot,
        nextCount: Int,
        nextAverage: Double
    ): Boolean = withContext(Dispatchers.IO) {
        val body = gson.toJson(mapOf("rating_average" to nextAverage, "rating_count" to nextCount))
        val (code, raw) = request(
            method = "PATCH",
            path = "/rest/v1/community_posts",
            query = mapOf(
                "id" to "eq.$postId",
                "rating_count" to "eq.${expected.ratingCount}",
                "rating_average" to "eq.${expected.ratingAverageLiteral}",
                "select" to "id"
            ),
            body = body,
            prefer = "return=representation"
        )
        code in 200..299 && returnedRowCount(raw) > 0
    }

    private suspend fun fetchRemixCount(postId: String): Int? = withContext(Dispatchers.IO) {
        val (code, raw) = request(
            method = "GET",
            path = "/rest/v1/community_posts",
            query = mapOf(
                "select" to "remix_count",
                "id" to "eq.$postId",
                "limit" to "1"
            )
        )
        if (code !in 200..299 || raw.isBlank()) return@withContext null
        runCatching {
            gson.fromJson(raw, Array<RemixCountRow>::class.java)?.firstOrNull()?.remixCount
        }.getOrNull()
    }

    private suspend fun compareAndSetRemixCount(postId: String, expected: Int, next: Int): Boolean = withContext(Dispatchers.IO) {
        val body = gson.toJson(mapOf("remix_count" to next))
        val (code, raw) = request(
            method = "PATCH",
            path = "/rest/v1/community_posts",
            query = mapOf(
                "id" to "eq.$postId",
                "remix_count" to "eq.$expected",
                "select" to "id"
            ),
            body = body,
            prefer = "return=representation"
        )
        code in 200..299 && returnedRowCount(raw) > 0
    }

    suspend fun signInWithEmail(email: String, password: String): String? = withContext(Dispatchers.IO) {
        if (!isConfigured || email.isBlank() || password.isBlank()) return@withContext null
        val body = gson.toJson(mapOf("email" to email.trim(), "password" to password))
        val (code, raw) = request(
            method = "POST",
            path = "/auth/v1/token",
            query = mapOf("grant_type" to "password"),
            body = body
        )
        if (code !in 200..299 || raw.isBlank()) return@withContext null
        runCatching {
            gson.fromJson(raw, AuthSessionResponse::class.java)?.accessToken
        }.getOrNull()
    }

    suspend fun signUpWithEmail(email: String, password: String): String? = withContext(Dispatchers.IO) {
        if (!isConfigured || email.isBlank() || password.isBlank()) return@withContext null
        val body = gson.toJson(mapOf("email" to email.trim(), "password" to password))
        val (code, raw) = request(
            method = "POST",
            path = "/auth/v1/signup",
            body = body
        )
        if (code !in 200..299 || raw.isBlank()) return@withContext null
        runCatching {
            gson.fromJson(raw, AuthSessionResponse::class.java)?.accessToken
        }.getOrNull()
    }

    suspend fun upsertCloudBackup(userEmail: String, payload: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || userEmail.isBlank() || payload.isBlank() || accessToken.isBlank()) return@withContext false
        val body = gson.toJson(
            listOf(
                mapOf(
                    "user_email" to userEmail.trim().lowercase(Locale.getDefault()),
                    "payload" to payload,
                    "updated_at" to toIso(System.currentTimeMillis())
                )
            )
        )
        val (code, raw) = request(
            method = "POST",
            path = "/rest/v1/cloud_backups",
            query = mapOf("on_conflict" to "user_email"),
            body = body,
            prefer = "resolution=merge-duplicates",
            bearerToken = accessToken
        )
        if (code !in 200..299) AppLog.w("Cloud backup upsert failed code=$code raw=$raw")
        code in 200..299
    }

    suspend fun fetchCloudBackup(userEmail: String, accessToken: String): String? = withContext(Dispatchers.IO) {
        if (!isConfigured || userEmail.isBlank() || accessToken.isBlank()) return@withContext null
        val (code, raw) = request(
            method = "GET",
            path = "/rest/v1/cloud_backups",
            query = mapOf(
                "select" to "payload",
                "user_email" to "eq.${userEmail.trim().lowercase(Locale.getDefault())}",
                "order" to "updated_at.desc",
                "limit" to "1"
            ),
            bearerToken = accessToken
        )
        if (code !in 200..299 || raw.isBlank()) return@withContext null
        runCatching {
            gson.fromJson(raw, Array<CloudBackupRow>::class.java)?.firstOrNull()?.payload
        }.getOrNull()
    }

    suspend fun fetchPostsPaged(
        sortMode: FeedSortMode = FeedSortMode.POPULAR,
        limit: Int = DEFAULT_FEED_PAGE_SIZE,
        offset: Int = 0
    ): List<CommunityPost> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext emptyList()
        val safeLimit = normalizeLimit(limit)
        val safeOffset = normalizeOffset(offset)
        val (code, raw) = request(
            method = "GET",
            path = "/rest/v1/community_posts",
            query = mapOf(
                "select" to "*",
                "order" to orderForFeed(sortMode),
                "limit" to safeLimit.toString(),
                "offset" to safeOffset.toString()
            )
        )
        if (code !in 200..299 || raw.isBlank()) return@withContext emptyList()
        val rows = runCatching {
            gson.fromJson(raw, Array<CommunityPostRow>::class.java)?.toList().orEmpty()
        }.getOrDefault(emptyList())
        rows.map { it.toDomain() }
    }

    // Backward-compatible default behavior.
    suspend fun fetchPosts(): List<CommunityPost> {
        return fetchPostsPaged(
            sortMode = FeedSortMode.POPULAR,
            limit = MAX_FEED_PAGE_SIZE,
            offset = 0
        )
    }

    suspend fun fetchLatestPosts(limit: Int = DEFAULT_FEED_PAGE_SIZE, offset: Int = 0): List<CommunityPost> {
        return fetchPostsPaged(sortMode = FeedSortMode.LATEST, limit = limit, offset = offset)
    }

    suspend fun fetchOldestPosts(limit: Int = DEFAULT_FEED_PAGE_SIZE, offset: Int = 0): List<CommunityPost> {
        return fetchPostsPaged(sortMode = FeedSortMode.OLDEST, limit = limit, offset = offset)
    }

    suspend fun fetchPopularPosts(limit: Int = DEFAULT_FEED_PAGE_SIZE, offset: Int = 0): List<CommunityPost> {
        return fetchPostsPaged(sortMode = FeedSortMode.POPULAR, limit = limit, offset = offset)
    }

    suspend fun fetchFollows(userId: String): Set<String> = withContext(Dispatchers.IO) {
        if (!isConfigured || userId.isBlank()) return@withContext emptySet()
        val (code, raw) = request(
            method = "GET",
            path = "/rest/v1/community_follows",
            query = mapOf("select" to "author_id", "follower_id" to "eq.$userId")
        )
        if (code !in 200..299 || raw.isBlank()) return@withContext emptySet()
        runCatching {
            gson.fromJson(raw, Array<FollowRow>::class.java)?.map { it.authorId }?.toSet().orEmpty()
        }.getOrDefault(emptySet())
    }

    suspend fun fetchRatings(userId: String): Map<String, Int> = withContext(Dispatchers.IO) {
        if (!isConfigured || userId.isBlank()) return@withContext emptyMap()
        val (code, raw) = request(
            method = "GET",
            path = "/rest/v1/community_ratings",
            query = mapOf("select" to "post_id,stars", "user_id" to "eq.$userId")
        )
        if (code !in 200..299 || raw.isBlank()) return@withContext emptyMap()
        runCatching {
            gson.fromJson(raw, Array<RatingRow>::class.java)?.associate { it.postId to it.stars }.orEmpty()
        }.getOrDefault(emptyMap())
    }

    suspend fun publishPost(post: CommunityPost): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        val row = CommunityPostRow.fromDomain(post)
        val body = gson.toJson(listOf(row))
        val (code, _) = request(
            method = "POST",
            path = "/rest/v1/community_posts",
            body = body
        )
        code in 200..299
    }

    suspend fun submitFeedbackInbox(
        userId: String,
        userName: String,
        category: String,
        message: String,
        appTheme: String,
        level: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || userId.isBlank() || message.isBlank()) return@withContext false
        val payload = mapOf(
            "user_id" to userId.trim(),
            "user_name" to userName.trim().ifBlank { "Player" },
            "category" to category.trim().ifBlank { "General" },
            "message" to message.trim(),
            "app_theme" to appTheme.trim(),
            "level" to level,
            "created_at" to toIso(System.currentTimeMillis())
        )
        val body = gson.toJson(listOf(payload))
        val (code, raw) = request(
            method = "POST",
            path = "/rest/v1/app_feedback_inbox",
            body = body
        )
        if (code !in 200..299) AppLog.w("Feedback submit failed code=$code raw=$raw")
        code in 200..299
    }

    suspend fun upsertPlanState(
        userId: String,
        userName: String,
        plans: Map<Long, List<String>>
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || userId.isBlank()) return@withContext false
        val payload = mapOf(
            "user_id" to userId.trim(),
            "user_name" to userName.trim().ifBlank { "Player" },
            "plans_json" to gson.toJson(plans),
            "updated_at" to toIso(System.currentTimeMillis())
        )
        val (code, raw) = request(
            method = "POST",
            path = "/rest/v1/app_plan_states",
            query = mapOf("on_conflict" to "user_id"),
            body = gson.toJson(listOf(payload)),
            prefer = "resolution=merge-duplicates"
        )
        if (code !in 200..299) AppLog.w("Plan sync failed code=$code raw=$raw")
        code in 200..299
    }

    suspend fun upsertHistoryState(
        userId: String,
        userName: String,
        day: Long,
        done: Int,
        total: Int,
        allDone: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || userId.isBlank()) return@withContext false
        val payload = mapOf(
            "user_id" to userId.trim(),
            "user_name" to userName.trim().ifBlank { "Player" },
            "epoch_day" to day,
            "done_count" to done.coerceAtLeast(0),
            "total_count" to total.coerceAtLeast(0),
            "all_done" to allDone,
            "updated_at" to toIso(System.currentTimeMillis())
        )
        val (code, raw) = request(
            method = "POST",
            path = "/rest/v1/app_history_states",
            query = mapOf("on_conflict" to "user_id,epoch_day"),
            body = gson.toJson(listOf(payload)),
            prefer = "resolution=merge-duplicates"
        )
        if (code !in 200..299) AppLog.w("History sync failed code=$code raw=$raw")
        code in 200..299
    }

    suspend fun upsertFollow(userId: String, authorId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || userId.isBlank() || authorId.isBlank()) return@withContext false
        val body = gson.toJson(listOf(FollowRow(userId, authorId)))
        val (code, _) = request(
            method = "POST",
            path = "/rest/v1/community_follows",
            body = body,
            prefer = "resolution=merge-duplicates"
        )
        code in 200..299
    }

    suspend fun deleteFollow(userId: String, authorId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || userId.isBlank() || authorId.isBlank()) return@withContext false
        val (code, _) = request(
            method = "DELETE",
            path = "/rest/v1/community_follows",
            query = mapOf("follower_id" to "eq.$userId", "author_id" to "eq.$authorId")
        )
        code in 200..299
    }

    suspend fun upsertRatingAndRefreshAggregate(userId: String, postId: String, stars: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || userId.isBlank() || postId.isBlank()) return@withContext false
        val s = stars.coerceIn(1, 5)
        val body = gson.toJson(listOf(RatingRow(userId = userId, postId = postId, stars = s)))
        val (upsertCode, _) = request(
            method = "POST",
            path = "/rest/v1/community_ratings",
            query = mapOf("on_conflict" to "user_id,post_id"),
            body = body,
            prefer = "resolution=merge-duplicates"
        )
        if (upsertCode !in 200..299) return@withContext false

        repeat(5) { attempt ->
            val expected = fetchPostAggregateSnapshot(postId) ?: return@withContext false
            val values = fetchRatingValuesForPost(postId)
            if (values.isEmpty()) return@withContext false
            val nextCount = values.size
            val nextAverage = values.average()
            if (compareAndSetPostAggregate(postId, expected, nextCount, nextAverage)) {
                return@withContext true
            }
            if (attempt < 4) delay(90L * (attempt + 1))
        }
        false
    }

    suspend fun incrementRemix(postId: String, current: Int? = null): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || postId.isBlank()) return@withContext false
        var expected = current
        repeat(5) { attempt ->
            val observed = expected ?: fetchRemixCount(postId) ?: return@withContext false
            if (compareAndSetRemixCount(postId, observed, observed + 1)) {
                return@withContext true
            }
            expected = null
            if (attempt < 4) delay(90L * (attempt + 1))
        }
        false
    }

    private data class CommunityPostRow(
        val id: String,
        @SerializedName("author_id") val authorId: String,
        @SerializedName("author_name") val authorName: String,
        val title: String,
        val description: String,
        val tags: List<String>,
        @SerializedName("template_json") val templateJson: JsonElement,
        @SerializedName("created_at") val createdAt: String,
        @SerializedName("rating_average") val ratingAverage: Double,
        @SerializedName("rating_count") val ratingCount: Int,
        @SerializedName("remix_count") val remixCount: Int,
        @SerializedName("source_post_id") val sourcePostId: String?
    ) {
        fun toDomain(): CommunityPost {
            val template = runCatching {
                gson.fromJson(templateJson, GameTemplate::class.java)
            }.getOrElse { GameTemplate(templateName = title) }
            return CommunityPost(
                id = id,
                authorId = authorId,
                authorName = authorName,
                title = title,
                description = description,
                tags = tags,
                template = template,
                createdAtMillis = toEpochMillis(createdAt),
                ratingAverage = ratingAverage,
                ratingCount = ratingCount,
                remixCount = remixCount,
                sourcePostId = sourcePostId
            )
        }

        companion object {
            fun fromDomain(post: CommunityPost): CommunityPostRow {
                val tags = post.tags.map { it.trim().lowercase(Locale.getDefault()) }.filter { it.isNotBlank() }
                return CommunityPostRow(
                    id = post.id,
                    authorId = post.authorId,
                    authorName = post.authorName,
                    title = post.title,
                    description = post.description,
                    tags = tags,
                    templateJson = gson.toJsonTree(post.template),
                    createdAt = toIso(post.createdAtMillis),
                    ratingAverage = post.ratingAverage,
                    ratingCount = post.ratingCount,
                    remixCount = post.remixCount,
                    sourcePostId = post.sourcePostId
                )
            }
        }
    }

    private data class FollowRow(
        @SerializedName("follower_id") val followerId: String,
        @SerializedName("author_id") val authorId: String
    )

    private data class RatingRow(
        @SerializedName("user_id") val userId: String,
        @SerializedName("post_id") val postId: String,
        val stars: Int
    )

    private data class PostRatingValue(val stars: Int)

    private data class IdRow(val id: String?)

    private data class RemixCountRow(@SerializedName("remix_count") val remixCount: Int)

    private data class PostAggregateSnapshot(
        val ratingCount: Int,
        val ratingAverage: Double,
        val ratingAverageLiteral: String
    )

    private data class AuthSessionResponse(
        @SerializedName("access_token") val accessToken: String?
    )

    private data class CloudBackupRow(val payload: String)
}
