package net.perfectdreams.gessythetrials

import org.joml.Vector3f

data class Triangle(
    val v0: Vector3f,
    val v1: Vector3f,
    val v2: Vector3f,
    val normal: Vector3f
)