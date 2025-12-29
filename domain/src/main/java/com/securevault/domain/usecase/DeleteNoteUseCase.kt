package com.securevault.domain.usecase

import com.securevault.domain.repository.NotesRepository

class DeleteNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(noteId: String) {
        repository.deleteNote(noteId = noteId)
    }
}