package net.perfectdreams.gessythetrials

import net.perfectdreams.Raycaster
import net.perfectdreams.gessythetrials.CollisionUtils.projectOnPlane
import net.perfectdreams.gessythetrials.meshes.JsonMesh
import net.perfectdreams.gessythetrials.meshes.JsonMeshManager
import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.Vector3f
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Player {
    companion object {
        private const val MAX_BOUNCES = 15
        private const val SKIN_WIDTH = 0.015f
        const val SPHERE_RADIUS = 0.5f
    }

    var ticks = 0
    var position = Vector3f(0f, 5f, 0f)

    val dimensions = Vector3f(1f, 1f, 1f)
    var direction = 0f
    var horizontalSpeed = 0f
    var verticalSpeed = 0f
    var isOnGround = false
    var lastCollisionResult: CollisionUtils.CollisionResult? = null
    var accumulatedNewHorizontalSpeed = 0f
    var playerAnimationSpeedFrame = 0f

    fun update(deltaTime: Float, levelCollisions: List<AABB>, levelJsonMesh: JsonMesh) {
        ticks++

        val wasOnGround = this.isOnGround

        val originalHorizontalVel = this.horizontalSpeed

        this.verticalSpeed += 0.01f

        this.playerAnimationSpeedFrame += (this.horizontalSpeed * 3.5f).absoluteValue

        this.horizontalSpeed += this.accumulatedNewHorizontalSpeed
        this.accumulatedNewHorizontalSpeed = 0f

        println("f: ${this.playerAnimationSpeedFrame}")

        if (this.horizontalSpeed > 0f) {
            this.horizontalSpeed -= 0.005f
        } else if (0f > this.horizontalSpeed) {
            this.horizontalSpeed += 0.005f
        }

        if (0.005f > abs(this.horizontalSpeed)) {
            this.horizontalSpeed = 0f
        }

        this.horizontalSpeed = this.horizontalSpeed.coerceIn(-0.52f, 0.52f)

        val directionRadians = MathUtils.toRadians(direction.toDouble()).toFloat()
        val xMove = sin(directionRadians) * horizontalSpeed
        val zMove = cos(directionRadians) * horizontalSpeed

        // val newPosition = Vector3f(position.x, position.y, position.z)
        // newPosition.x += xMove
        // newPosition.y -= this.verticalSpeed
        // newPosition.z += zMove
        val velocity = Vector3f(
            xMove,
            -this.verticalSpeed,
            zMove
        )

        // position.y -= 2f * deltaTime
        val hits = mutableListOf<CollisionUtils.CollisionResult>()

        for (face in levelJsonMesh.faces) {
            val hit = CollisionUtils.checkSphereTriangleIntersection(
                Sphere(
                    Vector3f(this.position).add(0f, SPHERE_RADIUS, 0f).add(velocity),
                    SPHERE_RADIUS
                ),
                Triangle(
                    levelJsonMesh.vertices[face.v0].let { Vector3f(it.x, it.y, it.z) },
                    levelJsonMesh.vertices[face.v1].let { Vector3f(it.x, it.y, it.z) },
                    levelJsonMesh.vertices[face.v2].let { Vector3f(it.x, it.y, it.z) },
                    face.normal.let { Vector3f(it.x, it.y, it.z) },
                )
            )

            if (hit.isColliding) {
                hits.add(hit)
            }
        }

        this.lastCollisionResult = hits.firstOrNull()
        this.isOnGround = false

        for (hit in hits) {
            if (hit.collisionNormal.y >= 0.6f) {
                // floor
                this.isOnGround = true
                this.verticalSpeed = 0f
                // velocity.setComponent(1, 0f)
            } else {
                // wall
                this.horizontalSpeed = (originalHorizontalVel * 0.9f)
            }

            velocity.add(hit.collisionNormal.mul(hit.penetrationDepth, Vector3f()))
        }

        if (this.isOnGround && 0.00005f >= velocity.lengthSquared()) {
            velocity.set(0f, 0f, 0f)
        } else {
            this.position.add(velocity)
        }
    }
}