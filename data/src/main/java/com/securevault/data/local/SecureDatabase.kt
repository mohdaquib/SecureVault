package com.securevault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.securevault.core.crypto.SecurePassphraseStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(InstantConverters::class)
abstract class SecureDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        fun create(context: Context, passphraseStore: SecurePassphraseStore): SecureDatabase {
            val passphrase = passphraseStore.getOrCreatePassphrase()
            val factory = SupportOpenHelperFactory(passphrase)
            return Room
                .databaseBuilder(
                    context,
                    SecureDatabase::class.java,
                    "securevault.db",
                ).openHelperFactory(factory)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
        }
    }
}
