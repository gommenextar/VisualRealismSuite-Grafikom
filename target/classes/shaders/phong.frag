#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;   // texture unit 0
uniform sampler2D shadowMap;        // texture unit 1
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;

// Shadow Mapping pass 2: bandingkan depth fragment dengan Shadow Map (+ PCF 3x3)
float calcShadow(vec4 fragPosLightSpace, vec3 norm, vec3 lightDir) {
    vec3 proj = fragPosLightSpace.xyz / fragPosLightSpace.w;
    proj = proj * 0.5 + 0.5;                       // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0) return 0.0;                  // di luar far plane cahaya

    // Slope-scaled bias: mencegah Shadow Acne tanpa Peter Panning berlebihan
    float bias = max(0.005 * (1.0 - dot(norm, lightDir)), 0.0005);

    float shadow = 0.0;
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float closest = texture(shadowMap, proj.xy + vec2(x, y) * texel).r;
            shadow += (proj.z - bias > closest) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    vec3 norm = normalize(Normal);
    if (!gl_FrontFacing) norm = -norm;   // sisi dalam tetap benar saat culling dimatikan

    // 1. Ambient
    vec3 ambient = 0.25 * lightColor;

    // 2. Diffuse (Lambertian)
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular (Phong vs Blinn-Phong)
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (diff > 0.0) {
        if (useBlinnPhong) {
            vec3 halfwayDir = normalize(lightDir + viewDir);
            spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);    // Blinn-Phong
        } else {
            vec3 reflectDir = reflect(-lightDir, norm);
            spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Phong
        }
    }
    vec3 specular = 0.8 * spec * lightColor;

    // 4. Shadow
    float shadow = useShadow ? calcShadow(FragPosLightSpace, norm, lightDir) : 0.0;

    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + (1.0 - shadow) * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
