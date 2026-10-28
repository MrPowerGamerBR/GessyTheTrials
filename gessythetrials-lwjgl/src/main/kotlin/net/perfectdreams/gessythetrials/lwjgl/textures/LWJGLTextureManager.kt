package net.perfectdreams.gessythetrials.lwjgl.textures

import net.perfectdreams.gessythetrials.TextureManager
import net.perfectdreams.gessythetrials.VirtualFileSystem
import net.perfectdreams.harmony.gl.GPUResources
import net.perfectdreams.harmony.gl.TextureSource

class LWJGLTextureManager(gpu: GPUResources, val vfs: VirtualFileSystem) : TextureManager(gpu) {
    override suspend fun loadTextureSource(filePath: String): TextureSource {
        val data = vfs.files[filePath] ?: error("File $filePath does not exist!")
        return STBImageDecoder.decode(data)
    }
}
