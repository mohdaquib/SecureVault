package com.securevault.domain.usecase

import com.securevault.domain.repository.NotesRepository

class DeleteNoteUseCase(private val notesRepository: NotesRepository) {
    suspend operator fun invoke(noteId: String) {
        notesRepository.deleteNote(noteId = noteId)
    }
}