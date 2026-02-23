package com.example.livinglifemmo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageSerializationTest {

    @Test
    fun communitySyncQueue_roundTrips() {
        val post = CommunityPost(
            authorId = "u1",
            authorName = "Player",
            title = "Test",
            description = "Desc",
            template = getDefaultGameTemplate()
        )
        val queue = listOf(
            CommunitySyncTask(type = CommunitySyncTaskType.PUBLISH_POST, post = post),
            CommunitySyncTask(type = CommunitySyncTaskType.FOLLOW_AUTHOR, authorId = "author_1"),
            CommunitySyncTask(type = CommunitySyncTaskType.RATE_POST, postId = "post_1", stars = 4)
        )

        val encoded = serializeCommunitySyncQueue(queue)
        val decoded = deserializeCommunitySyncQueue(encoded)

        assertEquals(queue.size, decoded.size)
        assertEquals(queue[0].type, decoded[0].type)
        assertEquals(queue[1].authorId, decoded[1].authorId)
        assertEquals(queue[2].stars, decoded[2].stars)
    }

    @Test
    fun deserializeCommunitySyncQueue_handlesBadJson() {
        val decoded = deserializeCommunitySyncQueue("{bad")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun normalizeCommunitySyncQueue_dedupsAndCaps() {
        val now = System.currentTimeMillis()
        val raw = listOf(
            CommunitySyncTask(type = CommunitySyncTaskType.FOLLOW_AUTHOR, authorId = "a1", createdAtMillis = now),
            CommunitySyncTask(type = CommunitySyncTaskType.FOLLOW_AUTHOR, authorId = "a1", createdAtMillis = now + 1),
            CommunitySyncTask(type = CommunitySyncTaskType.RATE_POST, postId = "p1", stars = 7, createdAtMillis = now + 2)
        )

        val normalized = normalizeCommunitySyncQueue(
            tasks = raw,
            nowMillis = now + 10,
            maxQueueSize = 10
        )

        assertEquals(2, normalized.size)
        assertEquals(5, normalized.last().stars)
    }

    @Test
    fun importGameTemplate_rejectsOversizedPayload() {
        val oversized = "x".repeat(300_000)
        val parsed = importGameTemplate(oversized)
        assertTrue(parsed == null)
    }
}
