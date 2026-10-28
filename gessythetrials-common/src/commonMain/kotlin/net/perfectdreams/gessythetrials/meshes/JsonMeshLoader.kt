package net.perfectdreams.gessythetrials.meshes

import kotlinx.serialization.json.Json

object JsonMeshLoader {
    fun load(content: String): JsonMesh {
        return Json.decodeFromString<JsonMesh>(content)
    }
}