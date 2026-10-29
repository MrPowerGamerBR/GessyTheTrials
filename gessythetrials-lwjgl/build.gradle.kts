plugins {
    kotlin("jvm")
    application
}

group = "net.perfectdreams.gessythetrials"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.3.6"
val lwjglNatives = "natives-linux"

dependencies {
    implementation(project(":gessythetrials-common"))
    implementation(libs.harmony.gl)
    implementation(libs.harmony.openglLwjgl)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-assimp")
    implementation("org.lwjgl", "lwjgl-bgfx")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-stb")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-bgfx", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("net.perfectdreams.harmony.logging:harmonylogging-slf4j:1.0.3")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "net.perfectdreams.gessythetrials.lwjgl.GessyTheTrialsLWJGLLauncher"
}

tasks.named<JavaExec>("run") {
    // The game loads its assets from the "assets" folder in the root project
    workingDir = rootProject.projectDir
}
