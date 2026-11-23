package com.example.data.local

import androidx.room.Entity

@Entity(
    tableName = "video_history",
    primaryKeys = ["userId", "id"],
)
data class VideoHistoryEntity(
    val id: String,
    val userId: String = "",
    val title: String,
    val modelLabel: String,
    val mode: String,
    val status: String,
    val progressText: String,
    val thumbPath: String?,
    val taskId: String?,
    val videoUrl: String?,
    val error: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val estimatedCredits: Int,
    val draftJson: String? = null,
)
