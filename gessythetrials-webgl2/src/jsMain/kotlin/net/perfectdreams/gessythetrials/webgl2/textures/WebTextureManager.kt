package net.perfectdreams.gessythetrials.webgl2.textures

import js.array.toJsArray
import js.buffer.toArrayBuffer
import js.objects.unsafeJso
import net.perfectdreams.gessythetrials.TextureManager
import net.perfectdreams.gessythetrials.VirtualFileSystem
import net.perfectdreams.harmony.gl.GPUResources
import net.perfectdreams.harmony.gl.TextureSource
import net.perfectdreams.harmony.gl.webgl2.BrowserImageBitmapTextureSource
import web.blob.Blob
import web.images.createImageBitmap

/**
 * A [TextureManager] that loads textures from images that were already decoded by the browser
 */
class WebTextureManager(gpu: GPUResources, val vfs: VirtualFileSystem) : TextureManager(gpu) {
    override suspend fun loadTextureSource(filePath: String): TextureSource {
        val data = vfs.files[filePath] ?: error("File $filePath does not exist!")

        val blob = Blob(
            listOf(data.toArrayBuffer()).toJsArray(),
            unsafeJso {
                type = "application/octet-stream"
            }
        )

        val image = createImageBitmap(blob)

        return BrowserImageBitmapTextureSource(image)
    }
}
