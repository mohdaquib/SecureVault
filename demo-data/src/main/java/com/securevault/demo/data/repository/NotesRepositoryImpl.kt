package com.securevault.demo.data.repository

import com.securevault.demo.data.local.NoteDao
import com.securevault.demo.data.local.toDomain
import com.securevault.demo.data.local.toEntity
import com.securevault.demo.domain.model.Note
import com.securevault.demo.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotesRepositoryImpl(private val noteDao: NoteDao) : NotesRepository {
    override fun observeNotes(): Flow<List<Note>> =
        noteDao.observeNotes().map { entities ->  entities.map { it.toDomain() }}

    override suspend fun createNote(note: Note) {
        noteDao.insert(noteEntity = note.toEntity())
    }

    override suspend fun deleteNote(noteId: String) {
        noteDao.delete(noteId)
    }
}