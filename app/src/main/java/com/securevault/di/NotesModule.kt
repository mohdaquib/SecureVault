package com.securevault.di

import com.securevault.demo.data.local.NoteDao
import com.securevault.demo.data.repository.NotesRepositoryImpl
import com.securevault.demo.domain.repository.NotesRepository
import com.securevault.demo.domain.usecase.CreateNoteUseCase
import com.securevault.demo.domain.usecase.DeleteNoteUseCase
import com.securevault.demo.domain.usecase.GetNotesUseCase
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.securevault.data.SecureVaultDataFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NotesModule {
    @Provides
    @Singleton
    fun provideNotesRepository(@ApplicationContext context: Context): NotesRepository =
        SecureVaultDataFactory.createNotesRepository(context)

    @Provides
    fun provideGetNotesUseCase(repo: NotesRepository) = GetNotesUseCase(repository = repo)

    @Provides
    fun provideCreateNotesUseCase(repo: NotesRepository) = CreateNoteUseCase(repository = repo)

    @Provides
    fun provideDeleteNotesUseCase(repo: NotesRepository) = DeleteNoteUseCase(repository = repo)
}
