package com.erns.alertauni.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.erns.alertauni.data.model.DraftEntity
import com.erns.alertauni.data.model.NotificacionEntity

// v4: agrega la tabla `borradores` del Muro de Anuncios
@Database(entities = [NotificacionEntity::class, DraftEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificacionDao(): NotificacionDao
    abstract fun draftDao(): DraftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v3: cada aviso guarda el anuncio al que pertenece, su título y si ya se leyó
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notificaciones ADD COLUMN postId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notificaciones ADD COLUMN titulo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE notificaciones ADD COLUMN leido INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v4: borradores del Muro de Anuncios; igual al esquema que genera Room para DraftEntity
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `borradores` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`ownerId` TEXT NOT NULL, " +
                        "`courseCatalogId` TEXT, " +
                        "`isPrivate` INTEGER NOT NULL, " +
                        "`recipientId` TEXT, " +
                        "`recipientName` TEXT, " +
                        "`priority` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_borradores_ownerId` ON `borradores` (`ownerId`)")
            }
        }

        @JvmStatic
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notificaciones-db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
