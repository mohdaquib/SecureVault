package com.securevault.demo.domain.usecase

import com.securevault.demo.domain.model.Note
import com.securevault.demo.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow

class GetNotesUseCase(
    private val repository: NotesRepository,
) {
    operator fun invoke(): Flow<List<Note>> = repository.observeNotes()
}
