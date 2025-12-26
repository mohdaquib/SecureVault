package com.securevault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SecureDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        fun create(
            context: Context,
            passphrase: ByteArray,
        ): SecureDatabase {
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
