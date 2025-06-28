package net.perfectdreams.gessythetrials

import org.lwjgl.opengl.ARBInternalformatQuery2.GL_TEXTURE_CUBE_MAP
import org.lwjgl.opengl.GL11.GL_LINEAR
import org.lwjgl.opengl.GL11.GL_RGB
import org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glGenTextures
import org.lwjgl.opengl.GL11.glTexImage2D
import org.lwjgl.opengl.GL11.glTexParameteri
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R
import org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X
import org.lwjgl.stb.STBImage.stbi_image_free
import org.lwjgl.stb.STBImage.stbi_load

fun main() {
    val textureId = glGenTextures()
    glBindTexture(GL_TEXTURE_CUBE_MAP, textureId)

    repeat(6) {
        val data = stbi_load("assets/images/top.jpg", IntArray(1) { 2048 }, IntArray(1) { 2048 }, IntArray(1) { 3 }, 3)!!

        glTexImage2D(
            GL_TEXTURE_CUBE_MAP_POSITIVE_X + it,
            0,
            GL_RGB,
            2048,
            2048,
            0,
            GL_RGB,
            GL_UNSIGNED_BYTE,
            data
        )

        stbi_image_free(data)
    }

    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)
}