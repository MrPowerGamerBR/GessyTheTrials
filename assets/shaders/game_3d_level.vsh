#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aUVCoords;
layout (location = 2) in vec3 aNormal;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

out vec2 vUVCoords;
out vec3 vNormal;
out vec3 vPos;

void main() {
    vec3 TransposedNormal = mat3(transpose(inverse(uModel))) * aNormal;
    vUVCoords = aUVCoords;
    vNormal = TransposedNormal;
    vPos = (uModel * vec4(aPos, 1.0)).xyz; // Yes, this is like that

    mat4 mvp = uProjection * uView * uModel;
    vec4 position = mvp * vec4(aPos, 1.0);

    gl_Position = position;
}