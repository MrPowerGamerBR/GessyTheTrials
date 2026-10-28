#version 430 core

in vec3 vNormal;
in vec2 vUVCoords;
in vec3 vPos;

flat in int vVertexBoneId;
uniform sampler2D uTexture;
uniform bool uIsObjectIdPass;
uniform vec3 uObjectIdRGB;
uniform bool uUseNormalsAsColor;
uniform vec4 uFlatColor;

out vec4 FragColor;

void main() {
    FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    if (false && vVertexBoneId == 2) {
        FragColor = vec4(0.0, 0.0, 1.0, 1.0);
        return;
    }

    if (uIsObjectIdPass) {
        FragColor = vec4(uObjectIdRGB, 1.0);
        return;
    }

    if (uUseNormalsAsColor) {
        FragColor = vec4((vNormal.r + 1.0) / 2.0, (vNormal.g + 1.0) / 2.0, (vNormal.b + 1.0) / 2.0, 1.0);
        return;
    }

    vec4 color = vec4(texture(uTexture, vUVCoords));

    if (color.a == 0.0)
        discard;

    if (true) {
        if (uFlatColor.a != 0.0) {
            // Checkboard
            if ((int(gl_FragCoord.x) + int(gl_FragCoord.y)) % 2 == 0)
                discard;
            FragColor = uFlatColor;
            return;
        }

        FragColor = color;
        return;
    }

    vec3 lightPos = vec3(0.0, 100.0, 0.0);
    vec3 lightColor = vec3(1.0, 1.0, 1.0);
    float ambientStrength = 0.7;
    vec3 ambient = ambientStrength * lightColor;

    vec3 norm = normalize(vNormal);
    vec3 lightDir = normalize(lightPos - vPos.xyz);

    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    vec3 resultVec3 = (ambient + diffuse) * color.rgb;

    vec4 result = vec4(resultVec3, 1.0);

    FragColor = result;
    // FragColor = vec4(1.0, 1.0, 1.0, 1.0);
}