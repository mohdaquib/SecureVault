package com.securevault.domain.usecase

import com.securevault.domain.model.Note
import com.securevault.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow

class GetNotesUseCase(
    private val repository: NotesRepository,
) {
    operator fun invoke(): Flow<List<Note>> = repository.observeNotes()
}
