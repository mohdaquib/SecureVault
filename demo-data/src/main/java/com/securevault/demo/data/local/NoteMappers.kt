package com.securevault.demo.data.local

import com.securevault.demo.domain.model.Note

internal fun NoteEntity.toDomain(): Note =
    Note(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Note.toEntity(): NoteEntity =
    NoteEntity(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
