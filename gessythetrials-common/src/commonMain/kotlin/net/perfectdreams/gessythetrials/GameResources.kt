package net.perfectdreams.gessythetrials

import com.mrpowergamerbr.starfait.Vector3f
import kotlinx.serialization.json.Json
import net.perfectdreams.gessythetrials.collision.StoredAABB
import net.perfectdreams.gessythetrials.meshes.JsonMesh
import net.perfectdreams.gessythetrials.meshes.JsonMeshLoader
import net.perfectdreams.gessythetrials.meshes.JsonMeshManager
import net.perfectdreams.gessythetrials.shaders.Default2DShader
import net.perfectdreams.gessythetrials.shaders.Default3DShader
import net.perfectdreams.gessythetrials.shaders.Level3DShader
import net.perfectdreams.gessythetrials.shaders.Level3DWaterShader
import net.perfectdreams.gessythetrials.shaders.SkyboxShader
import net.perfectdreams.gessythetrials.skeletons.AnimationBone
import net.perfectdreams.gessythetrials.skeletons.Skeleton
import net.perfectdreams.harmony.gl.GLConstants.GL_ARRAY_BUFFER
import net.perfectdreams.harmony.gl.GLConstants.GL_CLAMP_TO_EDGE
import net.perfectdreams.harmony.gl.GLConstants.GL_FLOAT
import net.perfectdreams.harmony.gl.GLConstants.GL_LINEAR
import net.perfectdreams.harmony.gl.GLConstants.GL_NEAREST
import net.perfectdreams.harmony.gl.GLConstants.GL_RGBA
import net.perfectdreams.harmony.gl.GLConstants.GL_STATIC_DRAW
import net.perfectdreams.harmony.gl.GLConstants.GL_TEXTURE_CUBE_MAP
import net.perfectdreams.harmony.gl.GLConstants.GL_TEXTURE_CUBE_MAP_POSITIVE_X
import net.perfectdreams.harmony.gl.GLConstants.GL_TEXTURE_MAG_FILTER
import net.perfectdreams.harmony.gl.GLConstants.GL_TEXTURE_MIN_FILTER
import net.perfectdreams.harmony.gl.GLConstants.GL_TEXTURE_WRAP_R
import net.perfectdreams.harmony.gl.GLConstants.GL_TEXTURE_WRAP_S
import net.perfectdreams.harmony.gl.GLConstants.GL_TEXTURE_WRAP_T
import net.perfectdreams.harmony.gl.GLConstants.GL_UNSIGNED_BYTE
import net.perfectdreams.harmony.gl.GLContext
import net.perfectdreams.harmony.gl.GPUResources
import net.perfectdreams.harmony.gl.textures.Texture
import net.perfectdreams.oldharmony.gl.meshes.MeshRenderingState

class GameResources(
    val gessyMesh: Skeleton,
    val testUvCubeMesh: MeshRenderingState,
    val sphereMesh: MeshRenderingState,
    val levelCollisions: List<AABB>,
    val gessyTexture: Texture,
    val levelTexture: Texture,
    val wallTexture: Texture,
    val loriTexture: Texture,
    val collisionTexture: Texture,
    val waterTexture: Texture,
    val fontTexture: Texture,
    val skyboxTextureId: Int,
    val skyboxCubeVAO: Int,
    val fontCharacters: Map<Char, FontCharacter>,
    val shader3d: Default3DShader,
    val shader2d: Default2DShader,
    val shader3dLevel: Level3DShader,
    val shader3dLevelWater: Level3DWaterShader,
    val shaderSkybox: SkyboxShader,
    val gessySkeleton: Skeleton,
    val levelJsonMesh: JsonMesh,
    val multiMaterialLevelMesh: Map<String, MeshRenderingState>,
    val gessyIdlePose: List<Map<String, AnimationBone>>,
    val gessyRunPose: List<Map<String, AnimationBone>>,
    val gessyAirPose: List<Map<String, AnimationBone>>
) {
    companion object {
        suspend fun load(
            gl: GLContext,
            gpu: GPUResources,
            textureManager: TextureManager,
            vfs: VirtualFileSystem
        ): GameResources {
            val meshManager = JsonMeshManager(gl)

            val gessyMesh = meshManager.createJsonMeshSkeleton(
                JsonMeshLoader.load(vfs.readText("meshes/gessy.json")),
                vfs.readText("armatures/gessy_skeleton.json")
            )
            val testUvCubeMesh = meshManager.createJsonMeshVAO(
                JsonMeshLoader.load(vfs.readText("meshes/test_uv_cube.json"))
            )
            val sphereMesh = meshManager.createJsonMeshVAO(JsonMeshLoader.load(vfs.readText("meshes/sphere.json")))
            /* val levelMesh = meshManager.createJsonMeshVAO(
                JsonMeshLoader.load(vfs.readText("meshes/test_uv_cube_lvl.json"))
            ) */
            // val skyboxMesh = meshManager.createSkyboxVAO()
            val levelStoredCollisions = Json.decodeFromString<List<StoredAABB>>(vfs.readText("collisions/test_uv_cube_lvl.json"))
            val levelCollisions = levelStoredCollisions.map {
                AABB.fromPoints(
                    Vector3f(it.min[0], it.min[1], it.min[2]),
                    Vector3f(it.max[0], it.max[1], it.max[2])
                )
            }

            val gessyTexture = textureManager.loadTexture(
                "textures/gessy_texture.png",
                GL_NEAREST
            )
            val levelTexture = textureManager.loadTexture(
                "textures/grass.png",
                GL_NEAREST
            )
            val wallTexture = textureManager.loadTexture(
                "textures/wall.png",
                GL_NEAREST
            )
            val loriTexture = textureManager.loadTexture(
                "textures/lori_solo_map_big.png",
                GL_LINEAR
            )
            val collisionTexture = textureManager.loadTexture(
                "textures/collision.png",
                GL_NEAREST
            )
            val waterTexture = textureManager.loadTexture(
                "textures/water.png",
                GL_NEAREST
            )
            val fontTexture = textureManager.loadTexture(
                "textures/font.png",
                GL_NEAREST
            )
            val skyboxTextureId = loadCubemap(gl, textureManager)
            val skyboxCubeVAO = createCubeVAO(gl)

            val fontCharacters = meshManager.createFontAtlas()

            val shader3d = gpu.createShader(
                vfs.readText("shaders/game_3d.vsh"),
                vfs.readText("shaders/game_3d.fsh"),
                ::Default3DShader
            )

            val shader2d = gpu.createShader(
                vfs.readText("shaders/game_2d.vsh"),
                vfs.readText("shaders/game_2d.fsh"),
                ::Default2DShader
            )

            val shader3dLevel = gpu.createShader(
                vfs.readText("shaders/game_3d_level.vsh"),
                vfs.readText("shaders/game_3d_level.fsh"),
                ::Level3DShader
            )
            val shader3dLevelWater = gpu.createShader(
                vfs.readText("shaders/game_3d_level.vsh"),
                vfs.readText("shaders/game_3d_level_water.fsh"),
                ::Level3DWaterShader
            )

            val shaderSkybox = gpu.createShader(
                vfs.readText("shaders/skybox.vsh"),
                vfs.readText("shaders/skybox.fsh"),
                ::SkyboxShader
            )

            val gessySkeleton = meshManager.createJsonMeshSkeleton(
                JsonMeshLoader.load(vfs.readText("meshes/gessy.json")),
                vfs.readText("armatures/gessy_skeleton.json")
            )

            val levelJsonMesh = JsonMeshLoader.load(vfs.readText("meshes/level.json"))
            val multiMaterialLevelMesh = meshManager.createJsonMeshMultiMaterialMesh(levelJsonMesh)

            val gessyIdlePose = Json.decodeFromString<List<Map<String, AnimationBone>>>(vfs.readText("animations/idle.json"))
            val gessyRunPose = Json.decodeFromString<List<Map<String, AnimationBone>>>(vfs.readText("animations/run.json"))
            val gessyAirPose = Json.decodeFromString<List<Map<String, AnimationBone>>>(vfs.readText("animations/air.json"))

            return GameResources(
                gessyMesh,
                testUvCubeMesh,
                sphereMesh,
                levelCollisions,
                gessyTexture,
                levelTexture,
                wallTexture,
                loriTexture,
                collisionTexture,
                waterTexture,
                fontTexture,
                skyboxTextureId,
                skyboxCubeVAO,
                fontCharacters,
                shader3d,
                shader2d,
                shader3dLevel,
                shader3dLevelWater,
                shaderSkybox,
                gessySkeleton,
                levelJsonMesh,
                multiMaterialLevelMesh,
                gessyIdlePose,
                gessyRunPose,
                gessyAirPose
            )
        }

        private fun createCubeVAO(gl: GLContext): Int {
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
            val quadVAO = gl.genVertexArrays()
            gl.bindVertexArray(quadVAO)

            // Generate four VBOs
            val vboId = gl.genBuffers()

            // Position VBO
            gl.enableVertexAttribArray(0)
            gl.bindBuffer(GL_ARRAY_BUFFER, vboId)
            gl.bufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW)
            gl.vertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
            // glDisableVertexAttribArray(0)

            return quadVAO
        }

        private suspend fun loadCubemap(gl: GLContext, textureManager: TextureManager): Int {
            val faces = arrayOf(
                "right.jpg",
                "left.jpg",
                "top.jpg",
                "bottom.jpg",
                "front.jpg",
                "back.jpg"
            )

            // Decode all faces before binding the texture, because decoding is async on the browser
            val faceSources = faces.map { textureManager.loadTextureSource("textures/$it") }

            val textureId = gl.genTextures()
            gl.bindTexture(GL_TEXTURE_CUBE_MAP, textureId)

            for ((index, faceSource) in faceSources.withIndex()) {
                // The texture sources are always RGBA
                gl.texImage2D(
                    GL_TEXTURE_CUBE_MAP_POSITIVE_X + index,
                    0,
                    GL_RGBA,
                    0,
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    faceSource
                )
            }

            gl.texParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            gl.texParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            gl.texParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            gl.texParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            gl.texParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)

            return textureId
        }
    }
}
