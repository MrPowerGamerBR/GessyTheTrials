package net.perfectdreams.gessythetrials

import net.perfectdreams.harmony.gl.GLConstants
import net.perfectdreams.harmony.render.RenderDevice
import net.perfectdreams.harmony.gl.TextureSource
import net.perfectdreams.harmony.render.textures.Texture

abstract class TextureManager(val device: RenderDevice) {
    /**
     * Decodes the image at [filePath] into a RGBA [TextureSource], without uploading it to the GPU
     */
    abstract suspend fun loadTextureSource(filePath: String): TextureSource

    suspend fun loadTexture(
        filePath: String,
        textureInterpolationType: Int
    ): Texture {
        return device.createTexture(
            loadTextureSource(filePath),
            GLConstants.GL_REPEAT,
            GLConstants.GL_REPEAT,
            textureInterpolationType,
            textureInterpolationType
        )
    }
}
