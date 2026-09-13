package com.securevault.demo.domain.usecase

import com.securevault.demo.domain.repository.NotesRepository

class DeleteNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(noteId: String) {
        repository.deleteNote(noteId = noteId)
    }
}