package net.perfectdreams.gessythetrials.shaders

import net.perfectdreams.harmony.gl.shaders.GameShader

class SkyboxShader(programId: Int) : GameShader(programId) {
    val uProjection = uniformMatrix4fv("uProjection")
    val uView = uniformMatrix4fv("uView")
    val uSkybox = uniform1i("uSkybox")
}