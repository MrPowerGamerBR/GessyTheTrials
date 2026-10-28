package net.perfectdreams.gessythetrials

import com.mrpowergamerbr.starfait.Vector3f

class AABB(val min: Vector3f, val max: Vector3f) {
    companion object {
        /**
         * Creates an AABB by automatically determining min and max values from two points.
         * @param a First point
         * @param b Second point
         * @return AABB with correctly calculated min and max vectors
         */
        fun fromPoints(a: Vector3f, b: Vector3f): AABB {
            val min = Vector3f(
                minOf(a.x, b.x),
                minOf(a.y, b.y),
                minOf(a.z, b.z)
            )

            val max = Vector3f(
                maxOf(a.x, b.x),
                maxOf(a.y, b.y),
                maxOf(a.z, b.z)
            )

            return AABB(min, max)
        }
    }

    fun intersects(other: AABB): Boolean {
        // Check if boxes overlap in all three dimensions
        return (this.min.x <= other.max.x && this.max.x >= other.min.x) &&
                (this.min.y <= other.max.y && this.max.y >= other.min.y) &&
                (this.min.z <= other.max.z && this.max.z >= other.min.z)
    }

    fun getCollisionDepth(other: AABB): Vector3f {
        val depth = Vector3f()

        if (this.intersects(other)) {
            // Calculate overlap in each dimension
            depth.x = minOf(this.max.x - other.min.x, other.max.x - this.min.x)
            depth.y = minOf(this.max.y - other.min.y, other.max.y - this.min.y)
            depth.z = minOf(this.max.z - other.min.z, other.max.z - this.min.z)
        }

        return depth
    }

    fun getCollisionNormal(other: AABB): Vector3f {
        val normal = Vector3f()

        if (this.intersects(other)) {
            val depth = getCollisionDepth(other)

            // Find the minimum penetration axis
            if (depth.x <= depth.y && depth.x <= depth.z) {
                normal.x = if (this.min.x < other.min.x) -1f else 1f
            } else if (depth.y <= depth.x && depth.y <= depth.z) {
                normal.y = if (this.min.y < other.min.y) -1f else 1f
            } else {
                normal.z = if (this.min.z < other.min.z) -1f else 1f
            }
        }

        return normal
    }

    fun resolveCollision(other: AABB): Vector3f {
        if (!this.intersects(other)) {
            return Vector3f(0f)
        }

        val depth = getCollisionDepth(other)
        val normal = getCollisionNormal(other)

        // Calculate the minimum translation vector
        return Vector3f(normal).mul(
            when {
                normal.x != 0f -> depth.x
                normal.y != 0f -> depth.y
                else -> depth.z
            }
        )
    }
}
