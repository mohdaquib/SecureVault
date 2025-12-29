package com.securevault.domain.usecase

import com.securevault.domain.model.Note
import com.securevault.domain.repository.NotesRepository
import java.time.Instant

class CreateNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(title: String, content: String) {
        require(title.isNotBlank()) { "Title cannot be blank" }
        val now = Instant.now()
        val note = Note(title = title.trim(),
            content = content, createdAt = now, updatedAt = now)
        repository.createNote(note)
    }
}