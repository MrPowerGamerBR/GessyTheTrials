package net.perfectdreams.harmony.gl.input

class Keyboard {
    val pressedKeys = mutableSetOf<Int>()

    fun isPressing(key: Int) = pressedKeys.contains(key)
}