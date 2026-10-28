package net.perfectdreams.gessythetrials

import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.Matrix4f
import com.mrpowergamerbr.starfait.Quaternionf
import com.mrpowergamerbr.starfait.Vector2f
import com.mrpowergamerbr.starfait.Vector3f
import net.perfectdreams.gessythetrials.Player.Companion.SPHERE_RADIUS
import net.perfectdreams.gessythetrials.shaders.Default3DShader
import net.perfectdreams.harmony.gl.GLConstants.GL_BLEND
import net.perfectdreams.harmony.gl.GLConstants.GL_COLOR_BUFFER_BIT
import net.perfectdreams.harmony.gl.GLConstants.GL_DEPTH_BUFFER_BIT
import net.perfectdreams.harmony.gl.GLConstants.GL_DEPTH_TEST
import net.perfectdreams.harmony.gl.GLConstants.GL_ONE_MINUS_SRC_ALPHA
import net.perfectdreams.harmony.gl.GLConstants.GL_SRC_ALPHA
import net.perfectdreams.harmony.gl.GLConstants.GL_TEXTURE0
import net.perfectdreams.harmony.gl.GLConstants.GL_TEXTURE_CUBE_MAP
import net.perfectdreams.harmony.gl.GLContext
import net.perfectdreams.harmony.gl.GPUResources
import net.perfectdreams.harmony.gl.pipelines.CompareFunc
import net.perfectdreams.harmony.gl.pipelines.DepthState
import net.perfectdreams.harmony.gl.pipelines.Pipeline
import net.perfectdreams.harmony.gl.pipelines.PipelineScope
import net.perfectdreams.harmony.gl.textures.Texture
import net.perfectdreams.harmony.graphics.batcher.SpriteBatcher
import net.perfectdreams.harmony.graphics.batcher.withSpritePipeline
import net.perfectdreams.oldharmony.gl.input.Key
import net.perfectdreams.oldharmony.gl.input.Keyboard
import kotlin.math.cos
import kotlin.math.sin

class GessyTheTrials(
    val gl: GLContext,
    val gpu: GPUResources,
    val resources: GameResources
) {
    companion object {
        private const val TICK_DURATION = 0.016666668f

        // If the game falls too far behind (for example, when the browser tab is in the background), we skip the missing ticks instead of running all of them at once
        private const val MAX_ACCUMULATED_TIME = 0.25f
    }

    val keyboard = Keyboard()
    private val player = Player()
    private val batcher = SpriteBatcher(gl)
    private val startedAt = currentTimeMillis()

    // Timekeeping
    private var lastFrameTime = getTimeFloat()
    private var lastFrame = getTimeFloat()
    private var accumulatedTime = 0f

    /**
     * Gets the time elapsed since the game has started, in seconds (like GLFW's `glfwGetTime()`)
     */
    fun getTime() = (currentTimeMillis() - startedAt) / 1000.0

    fun getTimeFloat() = getTime().toFloat()

    fun render() {
        // Calculate delta time
        val currentFrameTime = getTimeFloat()
        val deltaTime = currentFrameTime - lastFrameTime
        lastFrameTime = currentFrameTime

        val now = getTimeFloat()
        accumulatedTime += (now - lastFrame)
        lastFrame = now

        if (accumulatedTime > MAX_ACCUMULATED_TIME)
            accumulatedTime = MAX_ACCUMULATED_TIME

        gl.enable(GL_DEPTH_TEST)
        gl.enable(GL_BLEND)
        gl.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        gl.clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        while (accumulatedTime >= TICK_DURATION) {
            if (keyboard.isPressing(Key.W)) {
                player.accumulatedNewHorizontalSpeed += 0.02f // (0.07f * deltaTime)
            }

            if (keyboard.isPressing(Key.S)) {
                player.accumulatedNewHorizontalSpeed -= 0.02f // (0.07f * deltaTime)
            }

            if (keyboard.isPressing(Key.D)) {
                player.direction -= 3f
            }

            if (keyboard.isPressing(Key.A)) {
                player.direction += 3f
            }

            if (keyboard.isPressing(Key.UP)) {
                player.position.y += 4f
            }

            if (keyboard.isPressing(Key.DOWN)) {
                player.position.y -= 4f
            }

            if (keyboard.isPressing(Key.R)) {
                player.position.x = 0.0f
                player.position.y = 0.0f
                player.position.z = 0.0f
            }

            if (player.isOnGround && keyboard.isPressing(Key.SPACE)) {
                // player.verticalSpeed -= (11f * deltaTime)
                player.verticalSpeed -= 0.25f
            }

            player.update(deltaTime, resources.levelCollisions, resources.levelJsonMesh)
            accumulatedTime -= TICK_DURATION
        }

        val levelJsonMesh = resources.levelJsonMesh

        val directionRadians = MathUtils.toRadians(player.direction.toDouble()).toFloat()
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

        val fov = MathUtils.toRadians(90.0 + (player.horizontalSpeed * 60)).toFloat() // Field of View in radians
        val aspectRatio = 16.0f / 9.0f // Aspect ratio of the viewport
        val nearPlane = 0.1f // Near clipping plane
        val farPlane = 100.0f // Far clipping plane

        // Create the projection matrix
        val projectionMatrix = Matrix4f().perspective(fov, aspectRatio, nearPlane, farPlane)

        val screenProjectionMatrix = Matrix4f()
            .ortho(0.0f, 1280f, 720f, 0.0f, -1.0f, 1.0f)

        gpu.onRenderTarget(gpu.windowRenderTarget) {
            // The best way would be by rendering the skybox AFTER the 3d objects, to avoid unnecessarily rendering the skybox where objects are in front
            // However, this does cause issues with transparent objects (like gessy's whiskers)
            withPipeline(Pipeline(resources.shaderSkybox, depth = DepthState(write = true, compare = CompareFunc.LESS_THAN_OR_EQUAL))) {
                // Keep only the rotation of the view matrix (no translation)
                val viewWithoutTranslation = Matrix4f(viewMatrix).set3x3Only()

                gl.activeTexture(GL_TEXTURE0)
                gl.bindTexture(GL_TEXTURE_CUBE_MAP, resources.skyboxTextureId)
                this.shader.uSkybox.set(0)
                this.shader.uView.set(viewWithoutTranslation.get(FloatArray(16)))
                this.shader.uProjection.set(projectionMatrix.get(FloatArray(16)))

                resources.testUvCubeMesh.render()
            }

            for ((materialName, mesh) in resources.multiMaterialLevelMesh) {
                when (materialName) {
                    "Water" -> {
                        withPipeline(Pipeline(resources.shader3dLevelWater, depth = DepthState(write = true))) {
                            this.shader.uTexture.set(0, resources.waterTexture)
                            this.shader.uModel.set(Matrix4f().get(FloatArray(16)))
                            this.shader.uProjection.set(projectionMatrix.get(FloatArray(16)))
                            this.shader.uView.set(viewMatrix.get(FloatArray(16)))
                            this.shader.uTime.set(getTimeFloat())

                            mesh.render()
                        }
                    }
                    "Material", "Material.002", "Material.003" -> {
                        withPipeline(Pipeline(resources.shader3dLevel, depth = DepthState(write = true))) {
                            when (materialName) {
                                "Material" -> this.shader.uTexture.set(0, resources.levelTexture)
                                "Material.002" -> this.shader.uTexture.set(0, resources.wallTexture)
                                "Material.003" -> this.shader.uTexture.set(0, resources.loriTexture)
                                else -> error("Unsupported material $materialName!")
                            }

                            this.shader.uModel.set(Matrix4f().get(FloatArray(16)))
                            this.shader.uProjection.set(projectionMatrix.get(FloatArray(16)))
                            this.shader.uView.set(viewMatrix.get(FloatArray(16)))

                            mesh.render()
                        }
                    }
                    else -> error("Unsupported material $materialName!")
                }
            }

            // Debug collision sphere
            if (false) {
                withPipeline(Pipeline(resources.shader3d, depth = null)) {
                    this.shader.uModel.set(
                        Matrix4f()
                            .translate(player.position.x, player.position.y + SPHERE_RADIUS, player.position.z)
                            .scale(SPHERE_RADIUS, SPHERE_RADIUS, SPHERE_RADIUS)
                            .get(FloatArray(16))
                    )
                    this.shader.uProjection.set(projectionMatrix.get(FloatArray(16)))
                    this.shader.uView.set(viewMatrix.get(FloatArray(16)))
                    this.shader.uTexture.set(0, resources.collisionTexture)

                    resources.sphereMesh.render()
                }
            }

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

            fun PipelineScope<Default3DShader>.renderGessy() {
                val gessySkeleton = resources.gessySkeleton

                this.shader.uTexture.set(0, resources.gessyTexture)
                this.shader.uModel.set(
                    Matrix4f()
                        .translate(player.position.x, player.position.y, player.position.z)
                        .rotateY(MathUtils.toRadians(player.direction.toDouble()).toFloat())
                        .get(FloatArray(16))
                )
                this.shader.uUseNormalsAsColor.set(false)
                this.shader.uIsObjectIdPass.set(false)
                this.shader.uProjection.set(projectionMatrix.get(FloatArray(16)))
                this.shader.uView.set(viewMatrix.get(FloatArray(16)))

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
                        resources.gessyAirPose[((getTime() * 20).toInt() % 1)][name]!!
                    } else if (player.horizontalSpeed != 0f) {
                        resources.gessyRunPose[(player.playerAnimationSpeedFrame.toInt() % 50)][name]!!
                    } else {
                        resources.gessyIdlePose[((getTime() * 80).toInt() % 39)][name]!!
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

                this.shader.uBoneMatrices.set(boneTransformsMatrixArray)

                resources.gessyMesh.meshRenderingState.render()
            }

            // Render behind walls
            withPipeline(Pipeline(resources.shader3d, depth = DepthState(write = false, compare = CompareFunc.GREATER_THAN))) {
                this.shader.uFlatColor.set(0.2f, 0.2f, 0.8f, 1.0f)
                renderGessy()
            }

            withPipeline(Pipeline(resources.shader3d, depth = DepthState(write = true))) {
                this.shader.uFlatColor.set(0.0f, 0.0f, 0.0f, 0.0f)
                renderGessy()
            }

            withSpritePipeline(batcher, resources.shader2d) {
                gl.clear(GL_DEPTH_BUFFER_BIT)

                this.shader.uProjection.set(screenProjectionMatrix.get(FloatArray(16)))
                this.shader.uView.set(Matrix4f().get(FloatArray(16)))
                this.shader.uTexture.set(0, resources.fontTexture)

                val fontCharacters = resources.fontCharacters
                val fontTexture = resources.fontTexture

                it.drawBatch(this) {
                    renderText(this, Vector2f(0f, 0f), fontCharacters, fontTexture, 7f * 3f, "x: ${player.position.x}")
                    renderText(this, Vector2f(0f, 7f * 3f), fontCharacters, fontTexture, 7f * 3f, "y: ${player.position.y}")
                    renderText(this, Vector2f(0f, (7f * 3f) * 2f), fontCharacters, fontTexture, 7f * 3f, "z: ${player.position.z}")
                    renderText(this, Vector2f(0f, (7f * 3f) * 3f), fontCharacters, fontTexture, 7f * 3f, "hspeed: ${player.horizontalSpeed}")
                    renderText(this, Vector2f(0f, (7f * 3f) * 4f), fontCharacters, fontTexture, 7f * 3f, "vspeed: ${player.verticalSpeed}")
                    renderText(this, Vector2f(0f, (7f * 3f) * 5f), fontCharacters, fontTexture, 7f * 3f, "last hit: ${player.lastCollisionResult?.isColliding}, depth: ${player.lastCollisionResult?.penetrationDepth}")
                    renderText(
                        this,
                        Vector2f(0f, (7f * 3f) * 6f),
                        fontCharacters,
                        fontTexture,
                        7f * 3f,
                        "collision normal: ${player.lastCollisionResult?.collisionNormal?.x}, ${player.lastCollisionResult?.collisionNormal?.y}, ${player.lastCollisionResult?.collisionNormal?.z}"
                    )
                    renderText(
                        this,
                        Vector2f(0f, (7f * 3f) * 7f),
                        fontCharacters,
                        fontTexture,
                        7f * 3f,
                        "plane normal: ${player.lastCollisionResult?.planeNormal?.x}, ${player.lastCollisionResult?.planeNormal?.y}, ${player.lastCollisionResult?.planeNormal?.z}"
                    )

                    /* if (isColliding) {
                        renderText(Vector2f(0f, (7f * 5f) * 5f), 7f * 5f, "colliding")
                    } else {
                        renderText(Vector2f(0f, (7f * 5f) * 5f), 7f * 5f, "not colliding")
                    }
                    renderText(Vector2f(0f, (7f * 5f) * 6f), 7f * 5f, "is on ground: ${player.isOnGround}") */
                    renderText(this, Vector2f(0f, (7f * 3f) * 8f), fontCharacters, fontTexture, 7f * 3f, "is on ground: ${player.isOnGround}")
                    renderText(this, Vector2f(0f, (7f * 3f) * 9f), fontCharacters, fontTexture, 7f * 3f, "ticks: ${player.ticks}")
                }
            }
        }
    }

    fun renderText(
        batcherScope: SpriteBatcher.SpriteBatcherScope,
        position: Vector2f,
        fontCharacters: Map<Char, FontCharacter>,
        fontTexture: Texture,
        size: Float,
        input: String,
    ) {
        var x = 0f

        for (char in input) {
            val character = fontCharacters[char]
            if (character != null) {
                batcherScope.drawQuad(
                    position.x + x,
                    position.y,
                    size,
                    size,
                    character.uvTopLeft,
                    0f,
                    character.uvTopRight,
                    1f,
                    fontTexture,
                    1f,
                    1f,
                    1f,
                    1f
                )
            }

            x += size
        }
    }
}
