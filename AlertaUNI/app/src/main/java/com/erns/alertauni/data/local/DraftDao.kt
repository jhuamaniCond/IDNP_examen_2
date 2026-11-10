package com.erns.alertauni.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erns.alertauni.data.model.DraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    /** Borradores de una sola cuenta, el ultimo editado primero. */
    @Query("SELECT * FROM borradores WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    fun observe(ownerId: String): Flow<List<DraftEntity>>

    /** Inserta uno nuevo (id = 0) o reemplaza el existente; devuelve el id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: DraftEntity): Long

    @Query("DELETE FROM borradores WHERE id = :id AND ownerId = :ownerId")
    suspend fun delete(id: Long, ownerId: String)
}
