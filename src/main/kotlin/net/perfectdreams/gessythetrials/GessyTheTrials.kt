package net.perfectdreams.gessythetrials

import kotlinx.serialization.json.Json
import net.perfectdreams.gessythetrials.Player.Companion.SPHERE_RADIUS
import net.perfectdreams.gessythetrials.collision.StoredAABB
import net.perfectdreams.gessythetrials.meshes.JsonMeshLoader
import net.perfectdreams.gessythetrials.meshes.JsonMeshManager
import net.perfectdreams.gessythetrials.shaders.Default2DShader
import net.perfectdreams.gessythetrials.shaders.Default3DShader
import net.perfectdreams.gessythetrials.shaders.Level3DShader
import net.perfectdreams.gessythetrials.shaders.Level3DWaterShader
import net.perfectdreams.gessythetrials.shaders.SkyboxShader
import net.perfectdreams.gessythetrials.skeletons.AnimationBone
import net.perfectdreams.harmony.gl.debug.OpenGLDebugging
import net.perfectdreams.harmony.gl.input.Keyboard
import net.perfectdreams.harmony.gl.shaders.ShaderManager
import net.perfectdreams.harmony.gl.shaders.bind
import net.perfectdreams.harmony.gl.textures.TextureManager
import net.perfectdreams.harmony.glfw.misc.glfwGetTimeFloat
import net.perfectdreams.harmony.logging.HarmonyLoggerFactory
import net.perfectdreams.harmony.logging.slf4j.HarmonyLoggerCreatorSLF4J
import org.joml.Intersectionf
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.glfw.GLFWWindowSizeCallback
import org.lwjgl.glfw.GLFWWindowSizeCallbackI
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.NULL
import java.io.File
import java.lang.Math
import java.lang.System
import java.nio.IntBuffer
import kotlin.math.cos
import kotlin.math.sin


class GessyTheTrials {
    // The window handle
    private var window: Long = 0
    private val shaderManager = ShaderManager()
    private val meshManager = JsonMeshManager()
    private val textureManager = TextureManager()
    private val player = Player()
    private val keyboard = Keyboard()

    // Timekeeping
    private var lastFrameTime = 0.0f
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
            if (action == GLFW_PRESS) {
                keyboard.pressedKeys.add(key)
            }
            if (action == GLFW_RELEASE) {
                keyboard.pressedKeys.remove(key)
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
                glViewport(0, 0, width, height)
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

        // Set the clear color
        glClearColor(0.2f, 0.2f, 0.2f, 1.0f)

        val gessyMesh = meshManager.createJsonMeshSkeleton(
            JsonMeshLoader.load(File("assets/meshes/gessy.json").readText()),
            File("assets/armatures/gessy_skeleton.json").readText()
        )
        val testUvCubeMesh = meshManager.createJsonMeshVAO(
            JsonMeshLoader.load(File("assets/meshes/test_uv_cube.json").readText())
        )
        val sphereMesh = meshManager.createJsonMeshVAO(JsonMeshLoader.load(File("assets/meshes/sphere.json").readText()))
        /* val levelMesh = meshManager.createJsonMeshVAO(
            JsonMeshLoader.load(File("assets/meshes/test_uv_cube_lvl.json").readText())
        ) */
        // val skyboxMesh = meshManager.createSkyboxVAO()
        val levelStoredCollisions = Json.decodeFromString<List<StoredAABB>>(File("assets/collisions/test_uv_cube_lvl.json").readText())
        val levelCollisions = levelStoredCollisions.map {
            AABB.fromPoints(
                Vector3f(it.min[0], it.min[1], it.min[2]),
                Vector3f(it.max[0], it.max[1], it.max[2])
            )
        }

        val gessyTexture = textureManager.loadTexture(
            "assets/textures/gessy_texture.png",
            GL_NEAREST
        )
        val levelTexture = textureManager.loadTexture(
            "assets/textures/grass.png",
            GL_NEAREST
        )
        val wallTexture = textureManager.loadTexture(
            "assets/textures/wall.png",
            GL_NEAREST
        )
        val loriTexture = textureManager.loadTexture(
            "assets/textures/lori_solo_map_big.png",
            GL_LINEAR
        )
        val collisionTexture = textureManager.loadTexture(
            "assets/textures/collision.png",
            GL_NEAREST
        )
        val waterTexture = textureManager.loadTexture(
            "assets/textures/water.png",
            GL_NEAREST
        )
        val fontTexture = textureManager.loadTexture(
            "assets/textures/font.png",
            GL_NEAREST
        )
        val skyboxTextureId = loadCubemap()
        val skyboxCubeVAO = createCubeVAO()

        val fontCharactersMeshes = meshManager.createFontAtlas()

        val shader3d = shaderManager.loadShader(
            File("assets/shaders/game_3d.vsh").readText(),
            File("assets/shaders/game_3d.fsh").readText(),
            ::Default3DShader
        )

        val shader2d = shaderManager.loadShader(
            File("assets/shaders/game_2d.vsh").readText(),
            File("assets/shaders/game_2d.fsh").readText(),
            ::Default2DShader
        )

        val shader3dLevel = shaderManager.loadShader(
            File("assets/shaders/game_3d_level.vsh").readText(),
            File("assets/shaders/game_3d_level.fsh").readText(),
            ::Level3DShader
        )
        val shader3dLevelWater = shaderManager.loadShader(
            File("assets/shaders/game_3d_level.vsh").readText(),
            File("assets/shaders/game_3d_level_water.fsh").readText(),
            ::Level3DWaterShader
        )

        val shaderSkybox = shaderManager.loadShader(
            File("assets/shaders/skybox.vsh").readText(),
            File("assets/shaders/skybox.fsh").readText(),
            ::SkyboxShader
        )

        val gessySkeleton = meshManager.createJsonMeshSkeleton(
            JsonMeshLoader.load(File("assets/meshes/gessy.json").readText()),
            File("assets/armatures/gessy_skeleton.json").readText()
        )

        val levelJsonMesh = JsonMeshLoader.load(File("assets/meshes/level.json").readText())
        val multiMaterialLevelMesh = meshManager.createJsonMeshMultiMaterialMesh(levelJsonMesh)

        val screenProjectionMatrix = Matrix4f()
            .ortho(0.0f, 1280f, 720f, 0.0f, -1.0f, 1.0f)

        val gessyIdlePose = Json.decodeFromString<List<Map<String, AnimationBone>>>(File("assets/animations/idle.json").readText())
        val gessyRunPose = Json.decodeFromString<List<Map<String, AnimationBone>>>(File("assets/animations/run.json").readText())
        val gessyAirPose = Json.decodeFromString<List<Map<String, AnimationBone>>>(File("assets/animations/air.json").readText())

        fun renderText(
            position: Vector2f,
            size: Float,
            input: String,
        ) {
            var x = 0f

            shader2d.bind {
                this.uProjection.set(screenProjectionMatrix.get(FloatArray(16)))
                this.uView.set(Matrix4f().get(FloatArray(16)))
                this.uTexture.set(GL_TEXTURE0, fontTexture)

                for (char in input) {
                    this.uModel.set(Matrix4f()
                        .translate(x + position.x, position.y, 0f)
                        .scale(size, size, 1f)
                        .get(FloatArray(16))
                    )

                    val mesh = fontCharactersMeshes[char]
                    if (mesh != null) {
                        fontCharactersMeshes[char]!!.render()
                    }

                    x += size
                }
            }
        }

        lastFrameTime = glfwGetTimeFloat()

        var lastFrame = glfwGetTimeFloat()
        var accumulatedTime = 0f

        // Run the rendering loop until the user has attempted to close
        while (!glfwWindowShouldClose(window)) {
            // Calculate delta time
            val currentFrameTime = glfwGetTimeFloat()
            val deltaTime = currentFrameTime - lastFrameTime
            lastFrameTime = currentFrameTime

            val now = glfwGetTimeFloat()
            accumulatedTime += (now - lastFrame)
            lastFrame = now

            glEnableVertexAttribArray(3)
            glEnableVertexAttribArray(2)
            glEnableVertexAttribArray(1)
            glEnableVertexAttribArray(0)

            glEnable(GL_DEPTH_TEST)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glClear(GL43.GL_COLOR_BUFFER_BIT or GL43.GL_DEPTH_BUFFER_BIT)

            while (accumulatedTime >= 0.016666668f) {
                if (keyboard.pressedKeys.contains(GLFW_KEY_W)) {
                    player.accumulatedNewHorizontalSpeed += 0.02f // (0.07f * deltaTime)
                }

                if (keyboard.pressedKeys.contains(GLFW_KEY_S)) {
                    player.accumulatedNewHorizontalSpeed -= 0.02f // (0.07f * deltaTime)
                }

                if (keyboard.pressedKeys.contains(GLFW_KEY_D)) {
                    player.direction -= 3f
                }

                if (keyboard.pressedKeys.contains(GLFW_KEY_A)) {
                    player.direction += 3f
                }

                if (keyboard.pressedKeys.contains(GLFW_KEY_UP)) {
                    player.position.y += 4f
                }

                if (keyboard.pressedKeys.contains(GLFW_KEY_DOWN)) {
                    player.position.y -= 4f
                }

                if (keyboard.pressedKeys.contains(GLFW_KEY_R)) {
                    player.position.x = 0.0f
                    player.position.y = 0.0f
                    player.position.z = 0.0f
                }

                if (player.isOnGround && keyboard.pressedKeys.contains(GLFW_KEY_SPACE)) {
                    // player.verticalSpeed -= (11f * deltaTime)
                    player.verticalSpeed -= 0.25f
                }

                player.update(deltaTime, levelCollisions, levelJsonMesh)
                accumulatedTime -= 0.016666668f
            }

            val directionRadians = Math.toRadians(player.direction.toDouble()).toFloat()
            val xMove = sin(directionRadians) * 3f
            val zMove = cos(directionRadians) * 3f

            val cameraRayOrigin = Vector3f(player.position.x, player.position.y + 2f, player.position.z)
            val cameraDirection = Vector3f(-xMove, 1f, -zMove).normalize()

            var nearestDistance = 3f
            for (face in levelJsonMesh.faces) {
                val triangle = Triangle(
                    levelJsonMesh.vertices[face.v0].let { Vector3f(it.x, it.y, it.z) },
                    levelJsonMesh.vertices[face.v1].let { Vector3f(it.x, it.y, it.z) },
                    levelJsonMesh.vertices[face.v2].let { Vector3f(it.x, it.y, it.z) },
                    face.normal.let { Vector3f(it.x, it.y, it.z) },
                )

                val hit = CollisionUtils.rayIntersectsTriangle(
                    cameraRayOrigin,
                    cameraDirection,
                    triangle.v0,
                    triangle.v1,
                    triangle.v2
                )

                if (hit != null) {
                    val distance = cameraRayOrigin.distance(hit)

                    if (nearestDistance > distance) {
                        nearestDistance = distance
                    }
                }
            }

            val viewMatrix = Matrix4f()
                .lookAt(
                    // Vector3f(0f, 8f, 0.001f),
                    cameraRayOrigin.add(
                        cameraDirection.normalize(Vector3f()).mul(nearestDistance),
                        Vector3f()
                    ),
                    // Vector3f(0f, 4f, 10f),
                    cameraRayOrigin,
                    Vector3f(0f, 1f, 0f)
                )

            val fov = Math.toRadians(90.0 + (player.horizontalSpeed * 60)).toFloat() // Field of View in radians
            val aspectRatio = 16.0f / 9.0f // Aspect ratio of the viewport
            val nearPlane = 0.1f // Near clipping plane
            val farPlane = 100.0f // Far clipping plane

            // Create the projection matrix
            val projectionMatrix = Matrix4f().perspective(fov, aspectRatio, nearPlane, farPlane)

            // The best way would be by rendering the skybox AFTER the 3d objects, to avoid unnecessarily rendering the skybox where objects are in front
            // However, this does cause issues with transparent objects (like gessy's whiskers)
            glDepthFunc(GL_LEQUAL)
            shaderSkybox.bind {
                // Create a 3x3 matrix from the upper-left portion of the 4x4 matrix
                val rotationOnly = Matrix3f().set(viewMatrix)

                // Create a new 4x4 matrix from the 3x3 matrix (no translation)
                val viewWithoutTranslation = Matrix4f().set(rotationOnly)

                this.uSkybox.set(0)
                this.uView.set(viewWithoutTranslation.get(FloatArray(16)))
                this.uProjection.set(projectionMatrix.get(FloatArray(16)))

                testUvCubeMesh.render()
            }
            glDepthFunc(GL_LESS)

            for ((materialName, mesh) in multiMaterialLevelMesh) {
                when (materialName) {
                    "Water" -> {
                        shader3dLevelWater.bind {
                            this.uTexture.set(GL_TEXTURE0, waterTexture)
                            this.uModel.set(Matrix4f().get(FloatArray(16)))
                            this.uProjection.set(projectionMatrix.get(FloatArray(16)))
                            this.uView.set(viewMatrix.get(FloatArray(16)))
                            this.uTime.set(glfwGetTimeFloat())

                            mesh.render()
                        }
                    }
                    "Material", "Material.002", "Material.003" -> {
                        shader3dLevel.bind {
                            when (materialName) {
                                "Material" -> this.uTexture.set(GL_TEXTURE0, levelTexture)
                                "Material.002" -> this.uTexture.set(GL_TEXTURE0, wallTexture)
                                "Material.003" -> this.uTexture.set(GL_TEXTURE0, loriTexture)
                                else -> error("Unsupported material $materialName!")
                            }

                            this.uModel.set(Matrix4f().get(FloatArray(16)))
                            this.uProjection.set(projectionMatrix.get(FloatArray(16)))
                            this.uView.set(viewMatrix.get(FloatArray(16)))

                            mesh.render()
                        }
                    }
                    else -> error("Unsupported material $materialName!")
                }

            }

            glDisable(GL_DEPTH_TEST)
            shader3dLevel.bind {
                this.uModel.set(
                    Matrix4f()
                        .translate(player.position.x, player.position.y + Player.SPHERE_RADIUS, player.position.z)
                        .scale(Player.SPHERE_RADIUS, Player.SPHERE_RADIUS, Player.SPHERE_RADIUS)
                        .get(FloatArray(16))
                )
                this.uProjection.set(projectionMatrix.get(FloatArray(16)))
                this.uView.set(viewMatrix.get(FloatArray(16)))
                this.uTexture.set(GL_TEXTURE0, collisionTexture)

                sphereMesh.render()
            }
            glEnable(GL_DEPTH_TEST)

            /* shader3d.bind {
                this.uTexture.set(GL_TEXTURE0, levelTexture)
                this.uModel.set(Matrix4f().get(FloatArray(16)))
                this.uUseNormalsAsColor.set(false)
                this.uIsObjectIdPass.set(false)
                this.uProjection.set(projectionMatrix.get(FloatArray(16)))
                this.uView.set(viewMatrix.get(FloatArray(16)))

                val floatArray = FloatArray(16 * 19)
                repeat(19) {
                    Matrix4f().get(floatArray, it * 16)
                }

                this.uBoneMatrices.set(floatArray)

                levelMesh.render()
            } */

            shader3d.bind {
                this.uTexture.set(GL_TEXTURE0, gessyTexture)
                this.uModel.set(
                    Matrix4f()
                        .translate(player.position.x, player.position.y, player.position.z)
                        .rotateY(Math.toRadians(player.direction.toDouble()).toFloat())
                        .get(FloatArray(16))
                )
                this.uUseNormalsAsColor.set(false)
                this.uIsObjectIdPass.set(false)
                this.uProjection.set(projectionMatrix.get(FloatArray(16)))
                this.uView.set(viewMatrix.get(FloatArray(16)))

                // Transformed matrices for the pose
                val globalAnimatedTransformsMatrices = mutableMapOf<String, Matrix4f>()

                for ((name, bone) in gessySkeleton.bones) {
                    val parentMatrix = if (bone.parent == null) {
                        Matrix4f()
                    } else {
                        globalAnimatedTransformsMatrices[bone.parent]
                            ?: error("Parent matrix not found for bone $name")
                    }

                    val thisMatrix = Matrix4f()
                        .m00(bone.localBindTransformMatrix[0])
                        .m01(bone.localBindTransformMatrix[1])
                        .m02(bone.localBindTransformMatrix[2])
                        .m03(bone.localBindTransformMatrix[3])

                        .m10(bone.localBindTransformMatrix[4])
                        .m11(bone.localBindTransformMatrix[5])
                        .m12(bone.localBindTransformMatrix[6])
                        .m13(bone.localBindTransformMatrix[7])

                        .m20(bone.localBindTransformMatrix[8])
                        .m21(bone.localBindTransformMatrix[9])
                        .m22(bone.localBindTransformMatrix[10])
                        .m23(bone.localBindTransformMatrix[11])

                        .m30(bone.localBindTransformMatrix[12])
                        .m31(bone.localBindTransformMatrix[13])
                        .m32(bone.localBindTransformMatrix[14])
                        .m33(bone.localBindTransformMatrix[15])

                    val modelMatrix = Matrix4f(parentMatrix).mul(thisMatrix)

                    val animBones = if (!player.isOnGround) {
                        gessyAirPose[((glfwGetTime() * 20).toInt() % 1)][name]!!
                    } else if (player.horizontalSpeed != 0f) {
                        gessyRunPose[(player.playerAnimationSpeedFrame.toInt() % 50)][name]!!
                    } else {
                        gessyIdlePose[((glfwGetTime() * 80).toInt() % 39)][name]!!
                    }

                    modelMatrix.rotate(
                        Quaternionf(
                            animBones.rotation.x,
                            animBones.rotation.y,
                            animBones.rotation.z,
                            animBones.rotation.w
                        )
                    )

                    globalAnimatedTransformsMatrices[name] = modelMatrix
                }

                val boneTransformsMatrixArray = FloatArray(19 * 16)

                for ((groupName, index) in gessySkeleton.groupNamesToGroupIds) {
                    Matrix4f(globalAnimatedTransformsMatrices[groupName]!!)
                        .mul(gessySkeleton.inverseBindPoseMatrices[groupName]!!)
                        .get(boneTransformsMatrixArray, index * 16)
                }

                this.uBoneMatrices.set(boneTransformsMatrixArray)

                gessyMesh.meshRenderingState.render()
            }
            // the skybox should always be last

            glDisable(GL_DEPTH_TEST)
            glClear(GL43.GL_DEPTH_BUFFER_BIT)
            renderText(Vector2f(0f, 0f), 7f * 3f, "x: ${player.position.x}")
            renderText(Vector2f(0f, 7f * 3f), 7f * 3f, "y: ${player.position.y}")
            renderText(Vector2f(0f, (7f * 3f) * 2f), 7f * 3f, "z: ${player.position.z}")
            renderText(Vector2f(0f, (7f * 3f) * 3f), 7f * 3f, "hspeed: ${player.horizontalSpeed}")
            renderText(Vector2f(0f, (7f * 3f) * 4f), 7f * 3f, "vspeed: ${player.verticalSpeed}")
            renderText(Vector2f(0f, (7f * 3f) * 5f), 7f * 3f, "last hit: ${player.lastCollisionResult?.isColliding}, depth: ${player.lastCollisionResult?.penetrationDepth}")
            renderText(Vector2f(0f, (7f * 3f) * 6f), 7f * 3f, "collision normal: ${player.lastCollisionResult?.collisionNormal?.x}, ${player.lastCollisionResult?.collisionNormal?.y}, ${player.lastCollisionResult?.collisionNormal?.z}")
            renderText(Vector2f(0f, (7f * 3f) * 7f), 7f * 3f, "plane normal: ${player.lastCollisionResult?.planeNormal?.x}, ${player.lastCollisionResult?.planeNormal?.y}, ${player.lastCollisionResult?.planeNormal?.z}")

            /* if (isColliding) {
                renderText(Vector2f(0f, (7f * 5f) * 5f), 7f * 5f, "colliding")
            } else {
                renderText(Vector2f(0f, (7f * 5f) * 5f), 7f * 5f, "not colliding")
            }
            renderText(Vector2f(0f, (7f * 5f) * 6f), 7f * 5f, "is on ground: ${player.isOnGround}") */
            renderText(Vector2f(0f, (7f * 3f) * 8f), 7f * 3f, "is on ground: ${player.isOnGround}")
            renderText(Vector2f(0f, (7f * 3f) * 9f), 7f * 3f, "ticks: ${player.ticks}")

            glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()
        }
    }

    fun createCubeVAO(): Int {
        // positions: vec3
        val quadVertices = floatArrayOf(
            // positions
            -1.0f,  1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,

            -1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,

            -1.0f, -1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

            -1.0f,  1.0f, -1.0f,
            1.0f,  1.0f, -1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f, -1.0f,

            -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f,  1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f,  1.0f,
            1.0f, -1.0f,  1.0f
        )

        // Create and bind VAO
        val quadVAO = GL32.glGenVertexArrays()
        GL30.glBindVertexArray(quadVAO)

        // Generate four VBOs
        val vboId = glGenBuffers()

        // Position VBO
        glEnableVertexAttribArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
        // glDisableVertexAttribArray(0)

        return quadVAO
    }

    fun loadCubemap(): Int {
        val textureId = glGenTextures()
        glBindTexture(ARBInternalformatQuery2.GL_TEXTURE_CUBE_MAP, textureId)

        val faces = arrayOf(
            "right.jpg",
            "left.jpg",
            "top.jpg",
            "bottom.jpg",
            "front.jpg",
            "back.jpg"
        )

        stbi_set_flip_vertically_on_load(false)
        for ((index, face) in faces.withIndex()) {
            val widthArray = IntArray(1) { 0 }
            val heightArray = IntArray(1) { 0 }
            val channelsInFileArray = IntArray(1) { 0 }

            val data = stbi_load("assets/textures/$face", widthArray, heightArray, channelsInFileArray, 0)!!

            glTexImage2D(
                GL_TEXTURE_CUBE_MAP_POSITIVE_X + index,
                0,
                GL_RGB,
                widthArray[0],
                heightArray[0],
                0,
                GL_RGB,
                GL_UNSIGNED_BYTE,
                data
            )

            stbi_image_free(data)
        }

        glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)

        return textureId
    }
}