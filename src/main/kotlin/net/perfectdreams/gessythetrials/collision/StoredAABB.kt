package net.perfectdreams.gessythetrials.collision

import kotlinx.serialization.Serializable

@Serializable
data class StoredAABB(
    val name: String,
    val min: List<Float>,
    val max: List<Float>
)