package net.perfectdreams.gessythetrials

import net.perfectdreams.harmony.gl.GLConstants
import net.perfectdreams.harmony.gl.GPUResources
import net.perfectdreams.harmony.gl.TextureSource
import net.perfectdreams.harmony.gl.textures.Texture

abstract class TextureManager(val gpu: GPUResources) {
    /**
     * Decodes the image at [filePath] into a RGBA [TextureSource], without uploading it to the GPU
     */
    abstract suspend fun loadTextureSource(filePath: String): TextureSource

    suspend fun loadTexture(
        filePath: String,
        textureInterpolationType: Int
    ): Texture {
        return gpu.createTexture(
            loadTextureSource(filePath),
            GLConstants.GL_REPEAT,
            GLConstants.GL_REPEAT,
            textureInterpolationType,
            textureInterpolationType
        )
    }
}
