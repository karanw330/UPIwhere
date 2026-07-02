package com.example.hello_kotlin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.hello_kotlin.data.db.dao.AutoTagRuleDao
import com.example.hello_kotlin.data.db.dao.TagDao
import com.example.hello_kotlin.data.db.dao.TransactionDao
import com.example.hello_kotlin.data.db.entity.AutoTagRuleEntity
import com.example.hello_kotlin.data.db.entity.TagEntity
import com.example.hello_kotlin.data.db.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        TagEntity::class,
        AutoTagRuleEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun tagDao(): TagDao
    abstract fun autoTagRuleDao(): AutoTagRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isIgnored INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "expense_tracker.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate default tags
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).tagDao().let { dao ->
                                val defaultTags = listOf(
                                    TagEntity(name = "Food", colorHex = "#FF6B6B", emoji = "🍔"),
                                    TagEntity(name = "Transport", colorHex = "#4ECDC4", emoji = "🚗"),
                                    TagEntity(name = "Shopping", colorHex = "#45B7D1", emoji = "🛒"),
                                    TagEntity(name = "Health", colorHex = "#96CEB4", emoji = "💊"),
                                    TagEntity(name = "Entertainment", colorHex = "#FFEAA7", emoji = "🎮"),
                                    TagEntity(name = "Subscriptions", colorHex = "#DDA0DD", emoji = "📱"),
                                    TagEntity(name = "Bills", colorHex = "#F39C12", emoji = "💡"),
                                    TagEntity(name = "Other", colorHex = "#95A5A6", emoji = "📦")
                                )
                                defaultTags.forEach { dao.insertTag(it) }
                            }
                        }
                    }
                })
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
