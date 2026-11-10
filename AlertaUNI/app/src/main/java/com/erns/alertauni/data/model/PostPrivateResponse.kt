package com.erns.alertauni.data.model

import kotlinx.serialization.Serializable

// service-post-private-add responde {success, data: {post_id, created_at}}
@Serializable
data class PostPrivateResponse(
    val post_id: Int = 0,
    val created_at: String = ""
)
