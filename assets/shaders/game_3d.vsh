#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aUVCoords;
layout (location = 2) in int aVertexBoneId;
layout (location = 3) in vec3 aNormal;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
uniform mat4 uBoneMatrices[19];

out vec2 vUVCoords;
out vec3 vNormal;
out vec3 vPos;

flat out int vVertexBoneId;

void main() {
    vec3 TransposedNormal = mat3(transpose(inverse(uModel))) * aNormal;
    vUVCoords = aUVCoords;
    vVertexBoneId = aVertexBoneId;
    vNormal = TransposedNormal;

    // Get bone transformation for this vertex
    mat4 boneTransform = uBoneMatrices[aVertexBoneId];

    // Initialize transformed position
    vec4 transformedPosition = boneTransform * vec4(aPos, 1.0);

    // Because we are transforming the bones, we need to pass the transformed coordinate
    vPos = (uModel * transformedPosition).xyz; // Yes, this is like that

    mat4 mvp = uProjection * uView * uModel;

    if (false) {
        gl_Position = mvp * vec4(aPos, 1.0);
        return;
    }

    gl_Position = mvp * transformedPosition;
}