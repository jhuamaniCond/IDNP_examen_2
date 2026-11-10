package com.erns.alertauni.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Datos del Muro de Anuncios que el backend todavia no guarda: anuncios archivados y
 * anuncios con los comentarios cerrados.
 *
 * Son operaciones SIMULADAS: se guardan solo en el dispositivo del docente mientras
 * no exista la columna post.archived ni el endpoint para cambiar allow_comments.
 * Cada cuenta tiene su propio archivo, asi otra cuenta en el mismo telefono no ve lo
 * que archivo la anterior. La prioridad ya se guarda en el backend (post.priority).
 */
@Singleton
class AnnounceLocalStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun forUser(ownerId: String): UserStore {
        val safe = ownerId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return UserStore(context.getSharedPreferences("muro_anuncios_$safe", Context.MODE_PRIVATE))
    }

    class UserStore(private val prefs: SharedPreferences) {
        fun archivedIds(): Set<Int> = readIds(KEY_ARCHIVED)
        fun closedCommentIds(): Set<Int> = readIds(KEY_CLOSED_COMMENTS)


        fun setArchived(postId: Int, archived: Boolean) = toggle(KEY_ARCHIVED, postId, archived)

        fun setCommentsClosed(postId: Int, closed: Boolean) =
            toggle(KEY_CLOSED_COMMENTS, postId, closed)


        private fun readIds(key: String): Set<Int> =
            prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

        private fun toggle(key: String, postId: Int, add: Boolean) {
            val ids = readIds(key).toMutableSet()
            if (add) ids += postId else ids -= postId
            prefs.edit().putStringSet(key, ids.map { it.toString() }.toSet()).apply()
        }
    }

    private companion object {
        const val KEY_ARCHIVED = "archived"
        const val KEY_CLOSED_COMMENTS = "closed_comments"
    }
}
