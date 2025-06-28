plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    application
}

group = "net.perfectdreams.gessythetrials"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.perfectdreams.net")
}

val lwjglVersion = "3.3.6"
val lwjglNatives = "natives-windows"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1")

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

    implementation("org.joml:joml:1.10.8")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "net.perfectdreams.gessythetrials.GessyTheTrialsLauncher"
}