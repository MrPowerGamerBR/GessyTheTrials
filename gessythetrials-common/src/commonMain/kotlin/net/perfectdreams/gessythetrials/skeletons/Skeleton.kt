package net.perfectdreams.gessythetrials.skeletons

import net.perfectdreams.oldharmony.gl.meshes.MeshRenderingState
import com.mrpowergamerbr.starfait.Matrix4f

class Skeleton(
    val bones: Map<String, NewBone>,
    val meshRenderingState: MeshRenderingState,
    val groupNamesToGroupIds: Map<String, Int>,
    val globalBindTransformsMatrices: MutableMap<String, Matrix4f>,
    val inverseBindPoseMatrices: MutableMap<String, Matrix4f>
)