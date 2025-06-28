#version 330 core
out vec4 FragColor;

in vec3 vUVCoords;

uniform samplerCube uSkybox;

void main() {
    FragColor = texture(uSkybox, vUVCoords);
}