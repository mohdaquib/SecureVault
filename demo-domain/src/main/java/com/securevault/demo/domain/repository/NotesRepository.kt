package com.securevault.demo.domain.repository

import com.securevault.demo.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun createNote(note: Note)
    suspend fun deleteNote(noteId: String)
}