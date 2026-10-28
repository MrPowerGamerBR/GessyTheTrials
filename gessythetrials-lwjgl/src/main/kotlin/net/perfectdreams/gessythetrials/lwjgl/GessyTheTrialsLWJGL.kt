package net.perfectdreams.gessythetrials.lwjgl

import kotlinx.coroutines.runBlocking
import net.perfectdreams.gessythetrials.GameResources
import net.perfectdreams.gessythetrials.GessyTheTrials
import net.perfectdreams.gessythetrials.VirtualFileSystem
import net.perfectdreams.gessythetrials.lwjgl.textures.LWJGLTextureManager
import net.perfectdreams.harmony.gl.GLConstants.GL_FILL
import net.perfectdreams.harmony.gl.GLConstants.GL_FRONT_AND_BACK
import net.perfectdreams.harmony.gl.GLConstants.GL_LINE
import net.perfectdreams.harmony.gl.GPUResources
import net.perfectdreams.harmony.gl.opengl.OpenGLContext
import net.perfectdreams.harmony.gl.opengl.OpenGLDebugging
import net.perfectdreams.harmony.logging.HarmonyLoggerFactory
import net.perfectdreams.harmony.logging.slf4j.HarmonyLoggerCreatorSLF4J
import net.perfectdreams.oldharmony.gl.input.Key
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.glfw.GLFWWindowSizeCallbackI
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.glPolygonMode
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.io.File
import java.nio.IntBuffer

class GessyTheTrialsLWJGL {
    // The window handle
    private var window: Long = 0
    private val gl = OpenGLContext()
    lateinit var game: GessyTheTrials

    private var wireframeMode = false

    fun start() {
        HarmonyLoggerFactory.setLoggerCreator(HarmonyLoggerCreatorSLF4J())
        println("Hello LWJGL " + Version.getVersion() + "!")

        init()

        loop()

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
    }

    private fun init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        // Create the window
        window = glfwCreateWindow(1280, 720, "Gessy: The Trials", NULL, NULL)
        if (window == NULL) throw RuntimeException("Failed to create the GLFW window")

        glfwSetKeyCallback(window) { window, key, scancode, action, mods ->
            val gameKey = toGameKey(key)
            if (gameKey != null) {
                if (action == GLFW_PRESS) {
                    game.keyboard.pressedKeys.add(gameKey)
                }
                if (action == GLFW_RELEASE) {
                    game.keyboard.pressedKeys.remove(gameKey)
                }
            }

            if (action == GLFW_RELEASE && key == GLFW_KEY_M) {
                this.wireframeMode = !this.wireframeMode

                if (wireframeMode)
                    glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                else
                    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
            }
        }

        stackPush().use { stack ->
            val pWidth: IntBuffer = stack.mallocInt(1) // int*
            val pHeight: IntBuffer = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode: GLFWVidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            )
        }

        glfwSetWindowSizeCallback(window, object : GLFWWindowSizeCallbackI {
            override fun invoke(window: Long, width: Int, height: Int) {
                gl.viewport(0, 0, width, height)
            }
        })

        // Make the OpenGL context current
        glfwMakeContextCurrent(window)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)
    }

    private fun loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()
        OpenGLDebugging.enableReportGLErrors()

        val gpu = GPUResources(gl, 1280, 720)
        val vfs = loadVirtualFileSystem(File("assets"))
        val textureManager = LWJGLTextureManager(gpu, vfs)

        // Set the clear color
        gl.clearColor(0.2f, 0.2f, 0.2f, 1.0f)

        val resources = runBlocking { GameResources.load(gl, gpu, textureManager, vfs) }
        this.game = GessyTheTrials(gl, gpu, resources)

        // Run the rendering loop until the user has attempted to close
        while (!glfwWindowShouldClose(window)) {
            game.render()

            glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()
        }
    }

    private fun toGameKey(key: Int) = when (key) {
        GLFW_KEY_W -> Key.W
        GLFW_KEY_A -> Key.A
        GLFW_KEY_S -> Key.S
        GLFW_KEY_D -> Key.D
        GLFW_KEY_R -> Key.R
        GLFW_KEY_UP -> Key.UP
        GLFW_KEY_DOWN -> Key.DOWN
        GLFW_KEY_SPACE -> Key.SPACE
        else -> null
    }

    /**
     * Loads all files in the [assetsFolder] into a [VirtualFileSystem], keyed by their path relative to the [assetsFolder]
     */
    private fun loadVirtualFileSystem(assetsFolder: File): VirtualFileSystem {
        check(assetsFolder.isDirectory) { "Assets folder ${assetsFolder.absolutePath} does not exist!" }

        val vfs = VirtualFileSystem()
        assetsFolder.walkTopDown()
            .filter { it.isFile }
            .forEach {
                vfs.files[it.relativeTo(assetsFolder).invariantSeparatorsPath] = it.readBytes()
            }
        return vfs
    }
}
