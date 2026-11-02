package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cleaned_links")
data class CleanedLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalUrl: String,
    val cleanedUrl: String,
    val domain: String,
    val strippedCount: Int,
    val strippedParamsSummary: String,
    val bytesSaved: Int,
    val timestamp: Long = System.currentTimeMillis()
)
