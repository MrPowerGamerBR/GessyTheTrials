package net.perfectdreams.oldharmony.gl.input

class Keyboard {
    val pressedKeys = mutableSetOf<Key>()

    fun isPressing(key: Key) = pressedKeys.contains(key)
}
