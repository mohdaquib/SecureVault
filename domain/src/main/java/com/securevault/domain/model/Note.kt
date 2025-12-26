package com.securevault.domain.model

import java.time.Instant
import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
