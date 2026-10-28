package net.perfectdreams.gessythetrials

class VirtualFileSystem {
    val files = mutableMapOf<String, ByteArray>()

    fun readText(path: String) = (files[path] ?: error("File $path does not exist!")).decodeToString()
}
