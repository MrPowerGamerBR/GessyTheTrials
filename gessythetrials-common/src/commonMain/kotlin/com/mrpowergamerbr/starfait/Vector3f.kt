package com.mrpowergamerbr.starfait

import kotlin.math.sqrt

// From JOML
// Like JOML, methods without a "dest" parameter modify (and return) this vector, while methods with a "dest" parameter store the result in "dest"
class Vector3f(var x: Float, var y: Float, var z: Float) {
    constructor() : this(0f, 0f, 0f)
    constructor(d: Float) : this(d, d, d)
    constructor(v: Vector3f) : this(v.x, v.y, v.z)

    fun set(x: Float, y: Float, z: Float): Vector3f {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(v: Vector3f): Vector3f = set(v.x, v.y, v.z)

    fun add(v: Vector3f): Vector3f = add(v, this)

    fun add(v: Vector3f, dest: Vector3f): Vector3f {
        dest.x = x + v.x
        dest.y = y + v.y
        dest.z = z + v.z
        return dest
    }

    fun add(x: Float, y: Float, z: Float): Vector3f = add(x, y, z, this)

    fun add(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        dest.x = this.x + x
        dest.y = this.y + y
        dest.z = this.z + z
        return dest
    }

    fun sub(v: Vector3f): Vector3f = sub(v, this)

    fun sub(v: Vector3f, dest: Vector3f): Vector3f {
        dest.x = x - v.x
        dest.y = y - v.y
        dest.z = z - v.z
        return dest
    }

    fun mul(scalar: Float): Vector3f = mul(scalar, this)

    fun mul(scalar: Float, dest: Vector3f): Vector3f {
        dest.x = x * scalar
        dest.y = y * scalar
        dest.z = z * scalar
        return dest
    }

    fun dot(v: Vector3f): Float = x * v.x + y * v.y + z * v.z

    fun cross(v: Vector3f): Vector3f = cross(v, this)

    fun cross(v: Vector3f, dest: Vector3f): Vector3f {
        val rx = y * v.z - z * v.y
        val ry = z * v.x - x * v.z
        val rz = x * v.y - y * v.x
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun lengthSquared(): Float = x * x + y * y + z * z

    fun length(): Float = sqrt(lengthSquared())

    fun distanceSquared(v: Vector3f): Float {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return dx * dx + dy * dy + dz * dz
    }

    fun distance(v: Vector3f): Float = sqrt(distanceSquared(v))

    fun normalize(): Vector3f = normalize(this)

    fun normalize(dest: Vector3f): Vector3f {
        val scalar = 1.0f / length()
        dest.x = x * scalar
        dest.y = y * scalar
        dest.z = z * scalar
        return dest
    }

    // Like JOML's Kotlin extensions, these create a new vector
    operator fun plus(v: Vector3f): Vector3f = add(v, Vector3f())
    operator fun minus(v: Vector3f): Vector3f = sub(v, Vector3f())

    override fun toString() = "($x $y $z)"
}
