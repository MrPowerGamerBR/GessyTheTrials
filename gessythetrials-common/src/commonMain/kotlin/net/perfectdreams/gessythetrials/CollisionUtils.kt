package net.perfectdreams.gessythetrials

import com.mrpowergamerbr.starfait.Vector3f
import kotlin.math.sqrt

object CollisionUtils {
    fun rayIntersectsTriangle(
        rayOrigin: Vector3f,
        rayDirection: Vector3f,
        vertex0: Vector3f,
        vertex1: Vector3f,
        vertex2: Vector3f
    ): Vector3f? {
        val epsilon = 0.0000001f

        val edge1 = vertex1 - vertex0
        val edge2 = vertex2 - vertex0
        val h = rayDirection.cross(edge2, Vector3f())
        val a = edge1.dot(h)

        if (a > -epsilon && a < epsilon) {
            return null // This ray is parallel to this triangle.
        }

        val f = 1.0f / a
        val s = rayOrigin.sub(vertex0, Vector3f())
        val u = f * s.dot(h)

        if (u < 0.0f || u > 1.0f) {
            return null
        }

        val q = s.cross(edge1, Vector3f())
        val v = f * rayDirection.dot(q)

        if (v < 0.0f || u + v > 1.0f) {
            return null
        }

        // At this stage we can compute t to find out where the intersection point is on the line.
        val t = f * edge2.dot(q)
        return if (t > epsilon) {
            // ray intersection
            rayOrigin.add(rayDirection.mul(t, Vector3f()), Vector3f())
        } else {
            // This means that there is a line intersection but not a ray intersection.
            null
        }
    }


    /**
     * Finds the point on a line segment (from A to B) that is closest to a given point P.
     */
    fun closestPointOnLineSegment(p: Vector3f, a: Vector3f, b: Vector3f): Vector3f {
        val ab = b.sub(a, Vector3f())
        // Project p onto the line of ab, but clamp t to the segment [A, B]
        val t = (p.sub(a, Vector3f()).dot(ab) / ab.lengthSquared()).coerceIn(0f, 1f)
        return a.add(ab.mul(t, Vector3f()), Vector3f())
    }

    /**
     * Checks if a point P, which is assumed to be on the same plane as the triangle,
     * is inside the triangle's boundaries.
     */
    fun isPointInTriangle(p: Vector3f, tri: Triangle): Boolean {
        // Using the Barycentric coordinate method
        val v0 = tri.v2.sub(tri.v0, Vector3f())
        val v1 = tri.v1.sub(tri.v0, Vector3f())
        val v2 = p.sub(tri.v0, Vector3f())

        val dot00 = v0.dot(v0)
        val dot01 = v0.dot(v1)
        val dot02 = v0.dot(v2)
        val dot11 = v1.dot(v1)
        val dot12 = v1.dot(v2)

        val invDenom = 1 / (dot00 * dot11 - dot01 * dot01)
        val u = (dot11 * dot02 - dot01 * dot12) * invDenom
        val v = (dot00 * dot12 - dot01 * dot02) * invDenom

        return (u >= 0) && (v >= 0) && (u + v <= 1)
    }

    /**
     * Performs a collision test between a sphere and a triangle.
     *
     * @return A CollisionResult object containing detailed information about the collision.
     */
    fun checkSphereTriangleIntersection(sphere: Sphere, triangle: Triangle): CollisionResult {
        // 1. Find the point on the triangle's plane that is closest to the sphere's center.
        // val edgeAB = triangle.v1.sub(triangle.v0, Vector3f())
        // val edgeAC = triangle.v2.sub(triangle.v0, Vector3f())
        // val planeNormal = edgeAB.cross(edgeAC).normalize(Vector3f())
        val planeNormal = Vector3f(triangle.normal)

        val vecAtoCenter = sphere.center.sub(triangle.v0, Vector3f())
        val distCenterToPlane = vecAtoCenter.dot(planeNormal)

        // Project the sphere's center onto the triangle's plane.
        val planeProj = planeNormal.mul(distCenterToPlane, Vector3f())
        val closestPointOnPlane = sphere.center.sub(planeProj, Vector3f())

        // 2. Determine the actual closest point on the triangle mesh (face, edge, or vertex).
        val closestPointOnTriangle = if (isPointInTriangle(closestPointOnPlane, triangle)) {
            // The projected point is inside the triangle, so it's our closest point.
            closestPointOnPlane
        } else {
            // The projected point is outside. The closest point must be on an edge or vertex.
            // Find the closest point on each of the three edges to the sphere's center.
            val p_ab = closestPointOnLineSegment(sphere.center, triangle.v0, triangle.v1)
            val p_bc = closestPointOnLineSegment(sphere.center, triangle.v1, triangle.v2)
            val p_ca = closestPointOnLineSegment(sphere.center, triangle.v2, triangle.v0)

            // Find which of these three edge points is actually closest to the sphere center.
            val distSq_ab = sphere.center.distanceSquared(p_ab)
            val distSq_bc = sphere.center.distanceSquared(p_bc)
            val distSq_ca = sphere.center.distanceSquared(p_ca)

            when {
                distSq_ab < distSq_bc && distSq_ab < distSq_ca -> p_ab
                distSq_bc < distSq_ca -> p_bc
                else -> p_ca
            }
        }

        // 3. Perform the final collision check.
        val vecClosestPointToCenter = sphere.center.sub(closestPointOnTriangle, Vector3f())
        val distSq = vecClosestPointToCenter.lengthSquared()
        val radiusSq = sphere.radius * sphere.radius

        if (distSq < radiusSq) {
            val distance = sqrt(distSq)
            // Avoid division by zero if sphere center is exactly on the closest point
            val normal = if (distance > 0.0001f) vecClosestPointToCenter.normalize(Vector3f()) else planeNormal
            return CollisionResult(
                isColliding = true,
                distance = distance,
                penetrationDepth = sphere.radius - distance,
                planeNormal = planeNormal,
                collisionNormal = normal,
                closestPoint = closestPointOnTriangle
            )
        }

        return CollisionResult(isColliding = false)
    }

    data class CollisionResult(
        val isColliding: Boolean = false,
        val distance: Float = 0f,
        val penetrationDepth: Float = 0f,
        val planeNormal: Vector3f = Vector3f(0f, 0f, 0f),
        val collisionNormal: Vector3f = Vector3f(0f, 0f, 0f),
        val closestPoint: Vector3f = Vector3f(0f, 0f, 0f)
    )

    /**
     * Projects a vector onto a plane defined by a normal orthogonal to the plane.
     * The plane normal does not need to be normalized.
     *
     * @param planeNormal The normal of the plane.
     * @return The projected vector.
     */
    fun projectOnPlane(vectorToProject: Vector3f, planeNormal: Vector3f, dest: Vector3f = vectorToProject): Vector3f {
        val dotProduct = vectorToProject.dot(planeNormal)
        val projectionOntoNormal = planeNormal.mul(dotProduct, Vector3f())
        return vectorToProject.sub(projectionOntoNormal, dest)
    }
}