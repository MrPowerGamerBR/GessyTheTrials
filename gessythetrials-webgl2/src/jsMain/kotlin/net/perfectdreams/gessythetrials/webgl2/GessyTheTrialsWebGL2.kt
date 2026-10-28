package net.perfectdreams.gessythetrials.webgl2

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.perfectdreams.gessythetrials.GameResources
import net.perfectdreams.gessythetrials.GessyTheTrials
import net.perfectdreams.gessythetrials.VirtualFileSystem
import net.perfectdreams.gessythetrials.webgl2.textures.WebTextureManager
import net.perfectdreams.harmony.gl.GPUResources
import net.perfectdreams.harmony.gl.webgl2.WebGL2Context
import net.perfectdreams.oldharmony.gl.input.Key
import web.animations.requestAnimationFrame
import web.dom.document
import web.events.addHandler
import web.gl.ID
import web.gl.WebGL2RenderingContext
import web.gl.WebGLContextAttributes
import web.html.HTMLCanvasElement
import web.keyboard.ArrowDown
import web.keyboard.ArrowUp
import web.keyboard.KeyA
import web.keyboard.KeyCode
import web.keyboard.KeyD
import web.keyboard.KeyR
import web.keyboard.KeyS
import web.keyboard.KeyW
import web.keyboard.Space
import web.keyboard.KeyboardEvent
import web.window.keyDownEvent
import web.window.keyUpEvent
import web.window.window

class GessyTheTrialsWebGL2(val vfs: VirtualFileSystem) {
    lateinit var game: GessyTheTrials

    fun start() {
        // Get the Canvas from the DOM
        val canvas = document.querySelector("#glCanvas") as HTMLCanvasElement

        // We disable the alpha channel because the game renders things with alpha < 1.0 to the screen, which would cause the page to bleed through the canvas
        val webgl2 = canvas.getContext(WebGL2RenderingContext.ID, WebGLContextAttributes(alpha = false)) ?: error("WebGL2 is not supported!")

        window.keyDownEvent.addHandler {
            val key = toGameKey(it) ?: return@addHandler
            // Avoid scrolling the page with the arrow keys and space
            it.preventDefault()

            if (::game.isInitialized)
                game.keyboard.pressedKeys.add(key)
        }

        window.keyUpEvent.addHandler {
            val key = toGameKey(it) ?: return@addHandler
            it.preventDefault()

            if (::game.isInitialized)
                game.keyboard.pressedKeys.remove(key)
        }

        val gl = WebGL2Context(webgl2)
        val gpu = GPUResources(gl, webgl2.drawingBufferWidth, webgl2.drawingBufferHeight)
        val textureManager = WebTextureManager(gpu, vfs)

        GlobalScope.launch {
            val resources = GameResources.load(gl, gpu, textureManager, vfs)
            this@GessyTheTrialsWebGL2.game = GessyTheTrials(gl, gpu, resources)

            // Set the clear color
            gl.clearColor(0.2f, 0.2f, 0.2f, 1.0f)

            fun render() {
                game.render()

                requestAnimationFrame { render() }
            }

            requestAnimationFrame { render() }
        }
    }

    private fun toGameKey(event: KeyboardEvent) = when (event.code) {
        KeyCode.KeyW -> Key.W
        KeyCode.KeyA -> Key.A
        KeyCode.KeyS -> Key.S
        KeyCode.KeyD -> Key.D
        KeyCode.KeyR -> Key.R
        KeyCode.ArrowUp -> Key.UP
        KeyCode.ArrowDown -> Key.DOWN
        KeyCode.Space -> Key.SPACE
        else -> null
    }
}
