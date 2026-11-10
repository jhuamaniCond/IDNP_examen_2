package com.erns.alertauni.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://aflvmovgtxrizxifzijd.supabase.co",
            supabaseKey = "sb_publishable_5d6H81_nSg19LZiYNWxwvw_-wUItHMy"
        ) {
//            install(Postgrest)
//            install(Auth)
//            install(Realtime)
            httpEngine = io.ktor.client.engine.okhttp.OkHttp.create()
            install(Functions) // Add this for Edge Functions
            install(Auth)
            install(Postgrest) // Para la funcion set_post_priority
        }
    }

//    @Provides
//    @Singleton
//    fun providePostgrest(client: SupabaseClient): Postgrest = client.postgrest
}