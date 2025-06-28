package net.perfectdreams.gessythetrials.shaders

import net.perfectdreams.harmony.gl.shaders.GameShader

class Level3DWaterShader(programId: Int) : GameShader(programId) {
    val uProjection = uniformMatrix4fv("uProjection")
    val uView = uniformMatrix4fv("uView")
    val uModel = uniformMatrix4fv("uModel")
    val uTexture = uniform1Sampler2D("uTexture")
    val uTime = uniform1f("uTime")
}