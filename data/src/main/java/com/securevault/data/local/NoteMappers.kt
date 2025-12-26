package com.securevault.data.local

import com.securevault.domain.model.Note

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
