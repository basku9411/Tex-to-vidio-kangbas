package com.example.data.remote

import androidx.annotation.Keep
import com.example.data.local.VideoHistoryEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("uid", uid)
        put("email", email.take(320))
        put("displayName", displayName.take(120))
        photoUrl?.take(1024)?.takeIf { it.isNotBlank() }?.let { put("photoUrl", it) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }

    fun toUpdateMap(): Map<String, Any> = buildMap {
        put("email", email.take(320))
        put("displayName", displayName.take(120))
        photoUrl?.take(1024)?.takeIf { it.isNotBlank() }?.let { put("photoUrl", it) }
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class FirestoreVideoHistory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val modelLabel: String = "",
    val mode: String = "text",
    val status: String = "queued",
    val progressText: String = "",
    val thumbPath: String? = null,
    val taskId: String? = null,
    val videoUrl: String? = null,
    val error: String? = null,
    val createdAtMs: Long = 0L,
    val completedAtMs: Long? = null,
    val estimatedCredits: Int = 0,
    val draftJson: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toEntity(): VideoHistoryEntity = VideoHistoryEntity(
        id = id,
        userId = userId,
        title = title,
        modelLabel = modelLabel,
        mode = mode,
        status = status,
        progressText = progressText,
        thumbPath = thumbPath,
        taskId = taskId,
        videoUrl = videoUrl,
        error = error,
        createdAt = createdAtMs,
        completedAt = completedAtMs,
        estimatedCredits = estimatedCredits,
        draftJson = draftJson,
    )

    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("title", title.take(200))
        put("modelLabel", modelLabel.take(200))
        put("mode", sanitizeMode(mode))
        put("status", sanitizeStatus(status))
        put("progressText", progressText.take(200))
        put("createdAtMs", createdAtMs.coerceAtLeast(0L))
        put("estimatedCredits", estimatedCredits.coerceIn(0, 100_000))
        thumbPath?.take(1024)?.takeIf { it.isNotBlank() }?.let { put("thumbPath", it) }
        taskId?.take(256)?.takeIf { it.isNotBlank() }?.let { put("taskId", it) }
        videoUrl?.take(2048)?.takeIf { it.isNotBlank() }?.let { put("videoUrl", it) }
        error?.take(1000)?.takeIf { it.isNotBlank() }?.let { put("error", it) }
        completedAtMs?.takeIf { it >= 0L }?.let { put("completedAtMs", it) }
        draftJson?.take(32_000)?.takeIf { it.isNotBlank() }?.let { put("draftJson", it) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }

    fun toUpdateMap(): Map<String, Any> = buildMap {
        put("title", title.take(200))
        put("modelLabel", modelLabel.take(200))
        put("mode", sanitizeMode(mode))
        put("status", sanitizeStatus(status))
        put("progressText", progressText.take(200))
        put("estimatedCredits", estimatedCredits.coerceIn(0, 100_000))
        thumbPath?.take(1024)?.takeIf { it.isNotBlank() }?.let { put("thumbPath", it) }
        taskId?.take(256)?.takeIf { it.isNotBlank() }?.let { put("taskId", it) }
        videoUrl?.take(2048)?.takeIf { it.isNotBlank() }?.let { put("videoUrl", it) }
        error?.take(1000)?.takeIf { it.isNotBlank() }?.let { put("error", it) }
        completedAtMs?.takeIf { it >= 0L }?.let { put("completedAtMs", it) }
        draftJson?.take(32_000)?.takeIf { it.isNotBlank() }?.let { put("draftJson", it) }
        put("updatedAt", FieldValue.serverTimestamp())
    }

    companion object {
        fun fromEntity(entity: VideoHistoryEntity, ownerUid: String): FirestoreVideoHistory =
            FirestoreVideoHistory(
                id = entity.id,
                userId = ownerUid,
                title = entity.title,
                modelLabel = entity.modelLabel,
                mode = sanitizeMode(entity.mode),
                status = sanitizeStatus(entity.status),
                progressText = entity.progressText,
                thumbPath = entity.thumbPath,
                taskId = entity.taskId,
                videoUrl = entity.videoUrl,
                error = entity.error,
                createdAtMs = entity.createdAt,
                completedAtMs = entity.completedAt,
                estimatedCredits = entity.estimatedCredits,
                draftJson = entity.draftJson,
            )

        private fun sanitizeMode(raw: String): String =
            if (raw in setOf("text", "image", "caption")) raw else "text"

        private fun sanitizeStatus(raw: String): String =
            if (raw in setOf("queued", "processing", "completed", "failed")) raw else "queued"
    }
}
