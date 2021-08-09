#use "flywheel:context/fog.glsl"

varying vec3 BoxCoord;

uniform sampler3D uLightVolume;

uniform sampler2D uBlockAtlas;
uniform sampler2D uLightMap;

uniform vec3 uLightBoxSize;
uniform vec3 uLightBoxMin;
uniform mat4 uModel;

uniform float uTime;
uniform mat4 uViewProjection;
uniform vec3 uCameraPos;

uniform vec2 uWindowSize;

void FLWFinalizeNormal(inout vec3 normal) {
    mat3 m;
    m[0] = uModel[0].xyz;
    m[1] = uModel[1].xyz;
    m[2] = uModel[2].xyz;
    normal = m * normal;
}

#if defined(VERTEX_SHADER)

void FLWFinalizeWorldPos(inout vec4 worldPos) {
    worldPos = uModel * worldPos;

    BoxCoord = (worldPos.xyz - uLightBoxMin) / uLightBoxSize;

    #if defined(USE_FOG)
    FragDistance = length(worldPos.xyz);
    #endif

    gl_Position = uViewProjection * worldPos;
}

#elif defined(FRAGMENT_SHADER)
#use "flywheel:core/lightutil.glsl"

vec4 FLWBlockTexture(vec2 texCoords) {
    return texture2D(uBlockAtlas, texCoords);
}

void FLWFinalizeColor(vec4 color) {
    #if defined(USE_FOG)
    float a = color.a;
    float fog = clamp(FLWFogFactor(), 0., 1.);

    color = mix(uFogColor, color, fog);
    color.a = a;
    #endif

    gl_FragColor = color;
}

vec4 FLWLight(vec2 lightCoords) {
    lightCoords = max(lightCoords, texture3D(uLightVolume, BoxCoord).rg);

    return texture2D(uLightMap, shiftLight(lightCoords));
}

#endif
