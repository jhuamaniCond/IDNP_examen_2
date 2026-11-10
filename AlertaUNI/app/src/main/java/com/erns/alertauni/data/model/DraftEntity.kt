package com.erns.alertauni.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Anuncio que el docente todavia no quiere publicar. Vive solo en el dispositivo (Room)
 * y pertenece a una cuenta: [ownerId] es el id del usuario que inicio sesion, para que
 * otra cuenta en el mismo telefono no vea estos borradores.
 */
@Entity(tableName = "borradores", indices = [Index("ownerId")])
data class DraftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String,
    val courseCatalogId: String?,
    val isPrivate: Boolean,
    val recipientId: String?,
    val recipientName: String?,
    val priority: String,
    val title: String,
    val content: String,
    val updatedAt: Long
)
