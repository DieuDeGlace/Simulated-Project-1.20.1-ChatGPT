#include veil:fog
#include veil:light
#include aeronautics:levitite_utils

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
layout(location = 4) in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform int FogShape;

uniform float SableEnableNormalLighting;
uniform float SableSkyLightScale;
uniform mat3 SableInverseNormalMat;
uniform float SableShadeDown;
uniform float SableShadeUp;
uniform float SableShadeNorth;
uniform float SableShadeSouth;
uniform float SableShadeWest;
uniform float SableShadeEast;

uniform float time;
uniform int onSublevel;
uniform int layerIndex;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

// Minecraft 1.20.1's light include does not provide the 1.21
// block_brightness helper. These values are supplied by Sable from the
// active level's directional shade table.
float sable_block_brightness(vec3 worldNormal) {
    float fromDown = pow(clamp(-worldNormal.y, 0.0, 1.0), 3.0) * SableShadeDown;
    float fromUp = pow(clamp(worldNormal.y, 0.0, 1.0), 3.0) * SableShadeUp;
    float fromNorth = pow(clamp(-worldNormal.z, 0.0, 1.0), 2.0) * SableShadeNorth;
    float fromSouth = pow(clamp(worldNormal.z, 0.0, 1.0), 2.0) * SableShadeSouth;
    float fromWest = pow(clamp(-worldNormal.x, 0.0, 1.0), 2.0) * SableShadeWest;
    float fromEast = pow(clamp(worldNormal.x, 0.0, 1.0), 2.0) * SableShadeEast;
    return fromDown + fromUp + fromNorth + fromSouth + fromWest + fromEast;
}

void main() {

    vec3 pos = Position + ChunkOffset;
    if(onSublevel > 0)
    {
        vec3 localVelocity = getVelocity(pos - offset);
        float l = length(localVelocity);
        localVelocity *= (1-exp(-l/materialTransitionSpeed))/(l+0.001);
        pos += localVelocity * onSublevel * layerIndex * 0.15;
    }
    gl_Position = vec4(pos, 1.0);

    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, ivec2(UV2 * vec2(1.0, SableSkyLightScale)));
    vec3 sableWorldNormal = SableInverseNormalMat * (mat3(ModelViewMat) * Normal);
    vertexColor.rgb *= mix(vec3(1.0), vec3(sable_block_brightness(sableWorldNormal)), SableEnableNormalLighting);

    texCoord0 = UV0;
}
