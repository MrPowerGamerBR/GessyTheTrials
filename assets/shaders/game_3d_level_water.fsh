#version 430 core

in vec2 vUVCoords;
in vec3 vNormal;
in vec3 vPos;

uniform sampler2D uTexture;
uniform float uTime;

out vec4 FragColor;

void main() {
    vec4 color = vec4(texture(uTexture, vUVCoords + vec2(0.0, uTime * 2.0)));

    if (color.a == 0.0)
        discard;

    if (true) {
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
}