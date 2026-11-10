package com.erns.alertauni.data.local

import com.erns.alertauni.domain.manager.DataStoreHelper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identifica la cuenta con sesion iniciada para separar los datos locales por usuario.
 * Usa el id de Supabase Auth y, si no esta disponible, el correo guardado al iniciar sesion.
 */
@Singleton
class SessionOwner @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val dataStoreHelper: DataStoreHelper
) {
    suspend fun id(): String {
        val supabaseId = runCatching { supabaseClient.auth.currentUserOrNull()?.id }.getOrNull()
        if (!supabaseId.isNullOrBlank()) return supabaseId
        return dataStoreHelper.getEmail().ifBlank { "sin-sesion" }
    }
}
