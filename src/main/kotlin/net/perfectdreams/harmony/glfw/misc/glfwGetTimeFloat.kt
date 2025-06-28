package net.perfectdreams.harmony.glfw.misc

import org.lwjgl.glfw.GLFW.glfwGetTime

fun glfwGetTimeFloat() = glfwGetTime().toFloat()