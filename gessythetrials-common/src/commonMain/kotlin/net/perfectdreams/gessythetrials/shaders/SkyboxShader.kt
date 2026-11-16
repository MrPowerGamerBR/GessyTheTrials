package net.perfectdreams.gessythetrials.shaders

import net.perfectdreams.harmony.gl.GLContext
import net.perfectdreams.harmony.gl.GLProgram
import net.perfectdreams.harmony.render.shaders.ShaderProgram

class SkyboxShader(gl: GLContext, programId: GLProgram) : ShaderProgram(gl, programId) {
    val uProjection = uniformMatrix4fv("uProjection")
    val uView = uniformMatrix4fv("uView")
    val uSkybox = uniform1i("uSkybox")
}