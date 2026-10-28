package net.perfectdreams

import com.mrpowergamerbr.starfait.Vector3f
import kotlin.math.sqrt

object Raycaster {
    data class RaycastHit(
        var point: Vector3f = Vector3f(),
        var normal: Vector3f = Vector3f(),
        var distance: Float = 0f
    )

    fun sphereCast(
        origin: Vector3f,
        radius: Float,
        direction: Vector3f,
        maxDistance: Float,
        triangle: Array<Vector3f>
    ): RaycastHit? {
        val v0 = triangle[0]
        val v1 = triangle[1]
        val v2 = triangle[2]

        val edge1 = Vector3f()
        v1.sub(v0, edge1)
        val edge2 = Vector3f()
        v2.sub(v0, edge2)

        val normal = Vector3f()
        edge1.cross(edge2, normal).normalize()

        // Check if the sphere is moving parallel to the triangle plane
        val dot = normal.dot(direction)
        if (dot == 0.0f) {
            return null
        }

        // 1. Intersection with the triangle plane
        var t0 = (normal.dot(v0) - normal.dot(origin) - radius) / dot
        var t1 = (normal.dot(v0) - normal.dot(origin) + radius) / dot

        if (t0 > t1) {
            // Swap t0 and t1
            val temp = t0
            t0 = t1
            t1 = temp
        }

        if (t0 > maxDistance || t1 < 0) {
            return null
        }

        val hit = RaycastHit()
        var found = false
        var t = Float.MAX_VALUE

        // Check plane intersection
        if (t0 >= 0 && t0 < t) {
            val pointOnSphere = Vector3f(direction).mul(t0).add(origin)
            val pointOnPlane = Vector3f(pointOnSphere).sub(Vector3f(normal).mul(radius))

            if (isPointInTriangle(pointOnPlane, v0, v1, v2)) {
                t = t0
                hit.point.set(pointOnPlane)
                hit.normal.set(normal)
                hit.distance = t0
                found = true
            }
        }


        // 2. Intersection with vertices
        for (vertex in triangle) {
            val (t_vertex, point) = intersectRaySphere(origin, direction, vertex, radius)
            if (t_vertex >= 0 && t_vertex < t) {
                t = t_vertex
                hit.point.set(point)
                vertex.sub(point, hit.normal).normalize()
                hit.distance = t
                found = true
            }
        }

        // 3. Intersection with edges
        val edges = arrayOf(
            Pair(v0, v1),
            Pair(v1, v2),
            Pair(v2, v0)
        )

        for ((start, end) in edges) {
            val edgeDir = Vector3f()
            end.sub(start, edgeDir)
            val edgeLen = edgeDir.length()
            edgeDir.normalize()

            val (t_edge, point) = intersectRayCylinder(origin, direction, start, edgeDir, radius)
            if (t_edge >= 0 && t_edge < t) {
                val distOnEdge = point.sub(start, Vector3f()).dot(edgeDir)
                if (distOnEdge >= 0 && distOnEdge <= edgeLen) {
                    t = t_edge
                    hit.point.set(point)
                    val pointOnEdge = Vector3f(edgeDir).mul(distOnEdge).add(start)
                    pointOnEdge.sub(point, hit.normal).normalize()
                    hit.distance = t
                    found = true
                }
            }
        }

        return if (found) hit else null
    }

    fun isPointInTriangle(p: Vector3f, a: Vector3f, b: Vector3f, c: Vector3f): Boolean {
        val v0 = c.sub(a, Vector3f())
        val v1 = b.sub(a, Vector3f())
        val v2 = p.sub(a, Vector3f())

        val dot00 = v0.dot(v0)
        val dot01 = v0.dot(v1)
        val dot02 = v0.dot(v2)
        val dot11 = v1.dot(v1)
        val dot12 = v1.dot(v2)

        val invDenom = 1 / (dot00 * dot11 - dot01 * dot01)
        val u = (dot11 * dot02 - dot01 * dot12) * invDenom
        val v = (dot00 * dot12 - dot01 * dot02) * invDenom

        return (u >= 0) && (v >= 0) && (u + v < 1)
    }


    fun intersectRaySphere(rayOrigin: Vector3f, rayDir: Vector3f, sphereCenter: Vector3f, sphereRadius: Float): Pair<Float, Vector3f> {
        val oc = rayOrigin.sub(sphereCenter, Vector3f())
        val a = rayDir.dot(rayDir)
        val b = 2.0f * oc.dot(rayDir)
        val c = oc.dot(oc) - sphereRadius * sphereRadius
        val discriminant = b * b - 4 * a * c
        if (discriminant < 0) {
            return Pair(-1f, Vector3f())
        }

        val t = (-b - sqrt(discriminant)) / (2.0f * a)
        val point = Vector3f(rayDir).mul(t).add(rayOrigin)
        return Pair(t, point)
    }

    fun intersectRayCylinder(rayOrigin: Vector3f, rayDir: Vector3f, cylStart: Vector3f, cylDir: Vector3f, cylRadius: Float): Pair<Float, Vector3f> {
        val oc = rayOrigin.sub(cylStart, Vector3f())

        val card = cylDir.dot(rayDir)
        val caoc = cylDir.dot(oc)

        val a = 1 - card * card
        val b = oc.dot(rayDir) - caoc * card
        val c = oc.dot(oc) - caoc * caoc - cylRadius * cylRadius

        val discriminant = b * b - a * c
        if (discriminant < 0) {
            return Pair(-1f, Vector3f())
        }

        val t = (-b - sqrt(discriminant)) / a
        val point = Vector3f(rayDir).mul(t).add(rayOrigin)
        return Pair(t, point)
    }
}