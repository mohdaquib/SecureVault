package com.securevault.domain.repository

import com.securevault.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun createNote(note: Note)
    suspend fun deleteNote(noteId: String)
}