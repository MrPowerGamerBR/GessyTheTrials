package net.perfectdreams.gessythetrials.meshes

import kotlinx.serialization.json.Json
import net.perfectdreams.gessythetrials.FontCharacter
import net.perfectdreams.gessythetrials.skeletons.NewBone
import net.perfectdreams.gessythetrials.skeletons.Skeleton
import net.perfectdreams.harmony.gl.GLConstants.GL_ARRAY_BUFFER
import net.perfectdreams.harmony.gl.GLConstants.GL_STATIC_DRAW
import net.perfectdreams.harmony.gl.GLContext
import net.perfectdreams.oldharmony.gl.meshes.MeshRenderingState
import com.mrpowergamerbr.starfait.Matrix4f
import net.perfectdreams.harmony.gl.GLBuffer
import net.perfectdreams.harmony.gl.GLVertexArrayObject
import net.perfectdreams.harmony.render.vertex.buildVertexAttributes

class JsonMeshManager(val gl: GLContext) {
    // This variation loads a mesh, but won't assign any bones for any of the vertices
    // Use a skeleton for animation!
    fun createJsonMeshVAO(meshModel: JsonMesh): MeshRenderingState {
        // Initial validation
        for (face in meshModel.faces) {
            meshModel.vertices.getOrNull(face.v0) ?: error("Missing vertex!")
            meshModel.vertices.getOrNull(face.v1) ?: error("Missing vertex!")
            meshModel.vertices.getOrNull(face.v2) ?: error("Missing vertex!")
        }

        if (false) {
            for (vertex in meshModel.vertices) {
                if (vertex.vertexGroups.size >= 2)
                    error("Too many vertex groups for vertex $vertex!")
            }
        }

        println("Faces: ${meshModel.faces.size}")
        println("Vertices: ${meshModel.vertices.size}")

        // We can't use EBOs here because the same "vertex" may have different normals
        // All values are initialized to zero

        // Each face has 3 vertices, each vertex has 3 coordinates (x, y, z)
        val vertices = FloatArray(meshModel.faces.size * 9)

        // Each face has 3 vertices, each vertex has 2 UV coordinates
        val textureCoordinates = FloatArray(meshModel.faces.size * 6)

        // Each vertex has 1 bone index ID
        val vertexBoneIds = IntArray(meshModel.faces.size * 3)

        // Each face has 3 vertices, each vertex has 3 normals
        val normals = FloatArray(meshModel.faces.size * 9)

        var arrayVertexIdx = 0
        var arrayUVIdx = 0
        var arrayVertexBonesIdx = 0
        var arrayNormalsIdx = 0

        for (face in meshModel.faces) {
            val vertex0 = meshModel.vertices[face.v0]
            val vertex1 = meshModel.vertices[face.v1]
            val vertex2 = meshModel.vertices[face.v2]

            vertices[arrayVertexIdx++] = vertex0.x
            vertices[arrayVertexIdx++] = vertex0.y
            vertices[arrayVertexIdx++] = vertex0.z

            vertices[arrayVertexIdx++] = vertex1.x
            vertices[arrayVertexIdx++] = vertex1.y
            vertices[arrayVertexIdx++] = vertex1.z

            vertices[arrayVertexIdx++] = vertex2.x
            vertices[arrayVertexIdx++] = vertex2.y
            vertices[arrayVertexIdx++] = vertex2.z

            normals[arrayNormalsIdx++] = face.normal.x
            normals[arrayNormalsIdx++] = face.normal.y
            normals[arrayNormalsIdx++] = face.normal.z

            normals[arrayNormalsIdx++] = face.normal.x
            normals[arrayNormalsIdx++] = face.normal.y
            normals[arrayNormalsIdx++] = face.normal.z

            normals[arrayNormalsIdx++] = face.normal.x
            normals[arrayNormalsIdx++] = face.normal.y
            normals[arrayNormalsIdx++] = face.normal.z

            val textureCoordinates0 = face.uv0
            val textureCoordinates1 = face.uv1
            val textureCoordinates2 = face.uv2

            textureCoordinates[arrayUVIdx++] = textureCoordinates0.x
            textureCoordinates[arrayUVIdx++] = 1f - textureCoordinates0.y

            textureCoordinates[arrayUVIdx++] = textureCoordinates1.x
            textureCoordinates[arrayUVIdx++] = 1f - textureCoordinates1.y

            textureCoordinates[arrayUVIdx++] = textureCoordinates2.x
            textureCoordinates[arrayUVIdx++] = 1f - textureCoordinates2.y

            vertexBoneIds[arrayVertexBonesIdx++] = 0
            vertexBoneIds[arrayVertexBonesIdx++] = 0
            vertexBoneIds[arrayVertexBonesIdx++] = 0
        }

        for (v in vertexBoneIds) {
            println(v)
        }

        // Create and bind VAO
        val quadVAO = gl.genVertexArrays()
        gl.bindVertexArray(quadVAO)

        // Generate four VBOs
        val vbosArray = Array<GLBuffer>(4) { gl.genBuffers() }

        // Each attribute has its own VBO, so each VBO gets its own vertex attributes
        // The attributes MUST be applied while their VBO is bound!

        // Position VBO
        gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[0])
        gl.bufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        buildVertexAttributes { vec3(0) }.apply(gl)

        // Texture Coordinates VBO
        gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[1])
        gl.bufferData(GL_ARRAY_BUFFER, textureCoordinates, GL_STATIC_DRAW)
        buildVertexAttributes { vec2(1) }.apply(gl)

        // Vertex Bones VBO
        gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[2])
        gl.bufferData(GL_ARRAY_BUFFER, vertexBoneIds, GL_STATIC_DRAW)
        buildVertexAttributes { int(2) }.apply(gl)

        // Normals VBO
        gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[3])
        gl.bufferData(GL_ARRAY_BUFFER, normals, GL_STATIC_DRAW)
        buildVertexAttributes { vec3(3) }.apply(gl)

        // Unbind
        gl.bindBuffer(GL_ARRAY_BUFFER, null)
        gl.bindVertexArray(null)

        return MeshRenderingState(gl, quadVAO, meshModel.faces.size * 3)
    }

    // This creates a mesh for the 2D shader
    fun create2DQuad(): MeshRenderingState {
        // We don't use JSON meshes for this because a 2D quad is so simple that we can hardcode it in here
        // Also because the 2D shader works differently from the 3D shader, so exporting a plane to be used in the 2D shader isn't straightforward

        // positions: vec2, uv: vec2
        val quadVertices = floatArrayOf(
            // The descriptions are as a full quad
            // If we do a triangle from 0f to 1f, everything will be positioned around the quad's top left corner

            // First triangle
            0.0f, 0.0f, 0.0f, 1.0f, // top left
            1.0f, 0.0f, 1.0f, 1.0f, // top right
            1.0f, 1.0f, 1.0f, 0.0f, // bottom right

            // Second triangle
            1.0f, 1.0f, 1.0f, 0.0f, // bottom right
            0.0f, 1.0f, 0.0f, 0.0f, // bottom left
            0.0f, 0.0f, 0.0f, 1.0f, // top left
        )

        val screenQuadVao = gl.genVertexArrays()
        val vbo = gl.genBuffers()

        gl.bindVertexArray(screenQuadVao)
        gl.bindBuffer(GL_ARRAY_BUFFER, vbo)
        gl.bufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW)

        buildVertexAttributes {
            vec2(0) // Position
            vec2(1) // UV
        }.apply(gl)

        // Unbind the VBO and VAO to prevent accidental modification
        gl.bindBuffer(GL_ARRAY_BUFFER, null)
        gl.bindVertexArray(null)

        return MeshRenderingState(gl, screenQuadVao, 6)
    }

    // This creates a mesh for the 2D shader
    fun createMesh2D(meshModel: JsonMesh): MeshRenderingState {
        val quadData = FloatArray(
            // Each face has 3 vertices, each vertex has 3 coordinates (x, y, z)
            // Each face has 3 vertices, each vertex has 2 UV coordinates
            (meshModel.faces.size * 9) + (meshModel.faces.size * 6)
        )

        var arrayIdx = 0

        for (face in meshModel.faces) {
            val vertex0 = meshModel.vertices[face.v0]
            val vertex1 = meshModel.vertices[face.v1]
            val vertex2 = meshModel.vertices[face.v2]

            val textureCoordinates0 = face.uv0
            val textureCoordinates1 = face.uv1
            val textureCoordinates2 = face.uv2

            quadData[arrayIdx++] = vertex0.x
            quadData[arrayIdx++] = vertex0.y

            quadData[arrayIdx++] = textureCoordinates0.x
            quadData[arrayIdx++] = 1f - textureCoordinates0.y

            quadData[arrayIdx++] = vertex1.x
            quadData[arrayIdx++] = vertex1.y

            quadData[arrayIdx++] = textureCoordinates1.x
            quadData[arrayIdx++] = 1f - textureCoordinates1.y

            quadData[arrayIdx++] = vertex2.x
            quadData[arrayIdx++] = vertex2.y

            quadData[arrayIdx++] = textureCoordinates2.x
            quadData[arrayIdx++] = textureCoordinates2.y
        }

        // Create and bind VAO
        val quadVAO = gl.genVertexArrays()
        gl.bindVertexArray(quadVAO)

        // Generate one VBO
        val vbosArray = Array<GLBuffer>(1) { gl.genBuffers() }

        gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[0])
        gl.bufferData(GL_ARRAY_BUFFER, quadData, GL_STATIC_DRAW)
        buildVertexAttributes {
            vec2(0) // Position
            vec2(1) // UV
        }.apply(gl)

        // Unbind
        gl.bindBuffer(GL_ARRAY_BUFFER, null)
        gl.bindVertexArray(null)

        return MeshRenderingState(gl, quadVAO, meshModel.faces.size * 3)
    }

    fun createJsonMeshSkeleton(
        meshModel: JsonMesh,
        content: String
    ): Skeleton {
        val skeleton = Json.decodeFromString<Map<String, NewBone>>(content)

        // Initial validation
        for (face in meshModel.faces) {
            meshModel.vertices.getOrNull(face.v0) ?: error("Missing vertex!")
            meshModel.vertices.getOrNull(face.v1) ?: error("Missing vertex!")
            meshModel.vertices.getOrNull(face.v2) ?: error("Missing vertex!")
        }

        // Reenable this later!
        if (false) {
            for (vertex in meshModel.vertices) {
                if (vertex.vertexGroups.size >= 2)
                    error("Too many vertex groups for vertex $vertex!")
            }
        }

        // Reconciliation!
        // Before we start, we need to map the correct GLSL Group ID to each vertex group
        // Because an skeleton may have different IDs than the mesh
        val skeletonGroupNamesToSkeletonGroupIds = mutableMapOf<String, Int>()

        run {
            var idx = 0
            for (bone in skeleton) {
                skeletonGroupNamesToSkeletonGroupIds[bone.key] = idx++
            }
        }

        println("Faces: ${meshModel.faces.size}")
        println("Vertices: ${meshModel.vertices.size}")
        for ((groupName, groupId) in skeletonGroupNamesToSkeletonGroupIds) {
            println("Group $groupName -> $groupId")
        }

        // We can't use EBOs here because the same "vertex" may have different normals
        // All values are initialized to zero

        // Each face has 3 vertices, each vertex has 3 coordinates (x, y, z)
        val vertices = FloatArray(meshModel.faces.size * 9)

        // Each face has 3 vertices, each vertex has 2 UV coordinates
        val textureCoordinates = FloatArray(meshModel.faces.size * 6)

        // Each vertex has 1 bone index ID
        val vertexBoneIds = IntArray(meshModel.faces.size * 3)

        // Each face has 3 vertices, each vertex has 3 normals
        val normals = FloatArray(meshModel.faces.size * 9)

        var arrayVertexIdx = 0
        var arrayUVIdx = 0
        var arrayVertexBonesIdx = 0
        var arrayNormalsIdx = 0

        for (face in meshModel.faces) {
            val vertex0 = meshModel.vertices[face.v0]
            val vertex1 = meshModel.vertices[face.v1]
            val vertex2 = meshModel.vertices[face.v2]

            vertices[arrayVertexIdx++] = vertex0.x
            vertices[arrayVertexIdx++] = vertex0.y
            vertices[arrayVertexIdx++] = vertex0.z

            vertices[arrayVertexIdx++] = vertex1.x
            vertices[arrayVertexIdx++] = vertex1.y
            vertices[arrayVertexIdx++] = vertex1.z

            vertices[arrayVertexIdx++] = vertex2.x
            vertices[arrayVertexIdx++] = vertex2.y
            vertices[arrayVertexIdx++] = vertex2.z

            normals[arrayNormalsIdx++] = face.normal.x
            normals[arrayNormalsIdx++] = face.normal.y
            normals[arrayNormalsIdx++] = face.normal.z

            normals[arrayNormalsIdx++] = face.normal.x
            normals[arrayNormalsIdx++] = face.normal.y
            normals[arrayNormalsIdx++] = face.normal.z

            normals[arrayNormalsIdx++] = face.normal.x
            normals[arrayNormalsIdx++] = face.normal.y
            normals[arrayNormalsIdx++] = face.normal.z

            val textureCoordinates0 = face.uv0
            val textureCoordinates1 = face.uv1
            val textureCoordinates2 = face.uv2

            textureCoordinates[arrayUVIdx++] = textureCoordinates0.x
            textureCoordinates[arrayUVIdx++] = 1f - textureCoordinates0.y

            textureCoordinates[arrayUVIdx++] = textureCoordinates1.x
            textureCoordinates[arrayUVIdx++] = 1f - textureCoordinates1.y

            textureCoordinates[arrayUVIdx++] = textureCoordinates2.x
            textureCoordinates[arrayUVIdx++] = 1f - textureCoordinates2.y

            vertexBoneIds[arrayVertexBonesIdx++] = skeletonGroupNamesToSkeletonGroupIds[vertex0.vertexGroups.first()] ?: error("Could not find group ${vertex0.vertexGroups.first()}")
            vertexBoneIds[arrayVertexBonesIdx++] = skeletonGroupNamesToSkeletonGroupIds[vertex1.vertexGroups.first()] ?: error("Could not find group ${vertex0.vertexGroups.first()}")
            vertexBoneIds[arrayVertexBonesIdx++] = skeletonGroupNamesToSkeletonGroupIds[vertex2.vertexGroups.first()] ?: error("Could not find group ${vertex0.vertexGroups.first()}")
        }

        for (v in vertexBoneIds) {
            println(v)
        }

        // Create and bind VAO
        val quadVAO = gl.genVertexArrays()
        gl.bindVertexArray(quadVAO)

        // Generate four VBOs
        val vbosArray = Array<GLBuffer>(4) { gl.genBuffers() }

        // Each attribute has its own VBO, so each VBO gets its own vertex attributes
        // The attributes MUST be applied while their VBO is bound!

        // Position VBO
        gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[0])
        gl.bufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        buildVertexAttributes { vec3(0) }.apply(gl)

        // Texture Coordinates VBO
        gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[1])
        gl.bufferData(GL_ARRAY_BUFFER, textureCoordinates, GL_STATIC_DRAW)
        buildVertexAttributes { vec2(1) }.apply(gl)

        // Vertex Bones VBO
        gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[2])
        gl.bufferData(GL_ARRAY_BUFFER, vertexBoneIds, GL_STATIC_DRAW)
        buildVertexAttributes { int(2) }.apply(gl)

        // Normals VBO
        gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[3])
        gl.bufferData(GL_ARRAY_BUFFER, normals, GL_STATIC_DRAW)
        buildVertexAttributes { vec3(3) }.apply(gl)

        // Unbind
        gl.bindBuffer(GL_ARRAY_BUFFER, null)
        gl.bindVertexArray(null)

        // Skeleton tidbits
        val globalBindTransformsMatrices = mutableMapOf<String, Matrix4f>()

        for ((name, bone) in skeleton) {
            val parentMatrix = if (bone.parent == null) {
                Matrix4f()
            } else {
                globalBindTransformsMatrices[bone.parent] ?: error("Parent matrix not found for bone $name")
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

            globalBindTransformsMatrices[name] = modelMatrix
        }

        val inverseBindPoseMatrices = mutableMapOf<String, Matrix4f>()
        for ((name, matrix) in globalBindTransformsMatrices) {
            inverseBindPoseMatrices[name] = Matrix4f(matrix).invert()
        }

        return Skeleton(
            skeleton,
            MeshRenderingState(gl, quadVAO, meshModel.faces.size * 3),
            skeletonGroupNamesToSkeletonGroupIds,
            globalBindTransformsMatrices,
            inverseBindPoseMatrices
        )
    }

    fun createFontAtlas(): MutableMap<Char, FontCharacter> {
        // positions: vec2, uv: vec2
        val input = "abcdefghijklmnopqrstuvwxyz0123456789:.-+"
        val characters = mutableMapOf<Char, FontCharacter>()

        for ((index, char) in input.withIndex()) {
            val widthRelativeToSize = (7 / 512f)
            val heightRelativeToSize = (7 / 512f)

            val uvTopLeft = index * widthRelativeToSize
            val uvTopRight = (index + 1) * widthRelativeToSize

            characters[char] = FontCharacter(uvTopLeft, uvTopRight)
        }

        return characters
    }

    fun createSkyboxVAO(): MeshRenderingState {
        val vertices = floatArrayOf(
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
        val vbosArray = Array<GLBuffer>(1) { gl.genBuffers() }

        // Position VBO
        gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[0])
        gl.bufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        buildVertexAttributes { vec3(0) }.apply(gl)

        return MeshRenderingState(gl, quadVAO, 36)
    }

    // For the level rendering, splits up into multiple different meshes based on material
    fun createJsonMeshMultiMaterialMesh(meshModel: JsonMesh): Map<String, MeshRenderingState> {
        // Initial validation
        for (face in meshModel.faces) {
            meshModel.vertices.getOrNull(face.v0) ?: error("Missing vertex!")
            meshModel.vertices.getOrNull(face.v1) ?: error("Missing vertex!")
            meshModel.vertices.getOrNull(face.v2) ?: error("Missing vertex!")
        }

        println("Faces: ${meshModel.faces.size}")
        println("Vertices: ${meshModel.vertices.size}")

        val meshMaterialMap = mutableMapOf<String, MeshRenderingState>()

        for ((materialIndex, material) in meshModel.materials.withIndex()) {
            val filteredMeshFaces = meshModel.faces.filter { it.material == materialIndex }

            if (filteredMeshFaces.isNotEmpty()) {
                // We can't use EBOs here because the same "vertex" may have different normals
                // All values are initialized to zero

                // Each face has 3 vertices, each vertex has 3 coordinates (x, y, z)
                val vertices = FloatArray(filteredMeshFaces.size * 9)

                // Each face has 3 vertices, each vertex has 2 UV coordinates
                val textureCoordinates = FloatArray(filteredMeshFaces.size * 6)

                // Each face has 3 vertices, each vertex has 3 normals
                val normals = FloatArray(filteredMeshFaces.size * 9)

                var arrayVertexIdx = 0
                var arrayUVIdx = 0
                var arrayNormalsIdx = 0

                for (face in filteredMeshFaces) {
                    val vertex0 = meshModel.vertices[face.v0]
                    val vertex1 = meshModel.vertices[face.v1]
                    val vertex2 = meshModel.vertices[face.v2]

                    vertices[arrayVertexIdx++] = vertex0.x
                    vertices[arrayVertexIdx++] = vertex0.y
                    vertices[arrayVertexIdx++] = vertex0.z

                    vertices[arrayVertexIdx++] = vertex1.x
                    vertices[arrayVertexIdx++] = vertex1.y
                    vertices[arrayVertexIdx++] = vertex1.z

                    vertices[arrayVertexIdx++] = vertex2.x
                    vertices[arrayVertexIdx++] = vertex2.y
                    vertices[arrayVertexIdx++] = vertex2.z

                    normals[arrayNormalsIdx++] = face.normal.x
                    normals[arrayNormalsIdx++] = face.normal.y
                    normals[arrayNormalsIdx++] = face.normal.z

                    normals[arrayNormalsIdx++] = face.normal.x
                    normals[arrayNormalsIdx++] = face.normal.y
                    normals[arrayNormalsIdx++] = face.normal.z

                    normals[arrayNormalsIdx++] = face.normal.x
                    normals[arrayNormalsIdx++] = face.normal.y
                    normals[arrayNormalsIdx++] = face.normal.z

                    val textureCoordinates0 = face.uv0
                    val textureCoordinates1 = face.uv1
                    val textureCoordinates2 = face.uv2

                    textureCoordinates[arrayUVIdx++] = textureCoordinates0.x
                    textureCoordinates[arrayUVIdx++] = 1f - textureCoordinates0.y

                    textureCoordinates[arrayUVIdx++] = textureCoordinates1.x
                    textureCoordinates[arrayUVIdx++] = 1f - textureCoordinates1.y

                    textureCoordinates[arrayUVIdx++] = textureCoordinates2.x
                    textureCoordinates[arrayUVIdx++] = 1f - textureCoordinates2.y
                }

                // Create and bind VAO
                val quadVAO = gl.genVertexArrays()
                gl.bindVertexArray(quadVAO)

                // Generate three VBOs
                val vbosArray = Array<GLBuffer>(3) { gl.genBuffers() }

                // Each attribute has its own VBO, so each VBO gets its own vertex attributes
                // The attributes MUST be applied while their VBO is bound!

                // Position VBO
                gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[0])
                gl.bufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
                buildVertexAttributes { vec3(0) }.apply(gl)

                // Texture Coordinates VBO
                gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[1])
                gl.bufferData(GL_ARRAY_BUFFER, textureCoordinates, GL_STATIC_DRAW)
                buildVertexAttributes { vec2(1) }.apply(gl)

                // Normals VBO
                gl.bindBuffer(GL_ARRAY_BUFFER, vbosArray[2])
                gl.bufferData(GL_ARRAY_BUFFER, normals, GL_STATIC_DRAW)
                buildVertexAttributes { vec3(2) }.apply(gl)

                // Unbind
                gl.bindBuffer(GL_ARRAY_BUFFER, null)
                gl.bindVertexArray(null)

                meshMaterialMap[material] = MeshRenderingState(gl, quadVAO, filteredMeshFaces.size * 3)
            }
        }

        return meshMaterialMap
    }
}