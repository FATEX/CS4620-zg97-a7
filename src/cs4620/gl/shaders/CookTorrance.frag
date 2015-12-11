#version 120
#define M_PI 3.1415926535897932384626433832795

// You May Use The Following Functions As RenderMaterial Input
// vec4 getDiffuseColor(vec2 uv)
// vec4 getNormalColor(vec2 uv)
// vec4 getSpecularColor(vec2 uv)

// Lighting Information
const int MAX_LIGHTS = 16;
uniform int numLights;
uniform vec3 lightIntensity[MAX_LIGHTS];
uniform vec3 lightPosition[MAX_LIGHTS];
uniform vec3 ambientLightIntensity;

// Camera Information
uniform vec3 worldCam;
uniform float exposure;


// Shading Information

varying vec2 fUV;
varying vec3 fN; // normal at the vertex
varying vec4 worldPos; // vertex position in world coordinates

// 0 : smooth, 1: rough
uniform float roughness;

void main() {
    // TODO A4
    vec3 N = normalize(fN);
    vec3 V = normalize(worldCam - worldPos.xyz);
    
    gl_FragColor = vec4(1,1,1,1);
    
    vec4 finalColor = vec4(0.0, 0.0, 0.0, 0.0);
    
    for (int i = 0; i < numLights; i++) {
        float r = length(lightPosition[i] - worldPos.xyz);
        vec3 L = normalize(lightPosition[i] - worldPos.xyz);
        vec3 H = normalize(L + V);
        
        float F0 = 0.04;
        
        float Fbeta = F0 + (1 - F0) * pow(1.0 - dot(V, H), 5);
        
        float D = pow(1.0 / (pow(roughness, 2) * pow(dot(N, H), 4)),
                      exp((pow(dot(N, H), 2) - 1.0) / (pow(roughness, 2) * pow(dot(N, H), 2))));
        
        float G = min(min(1, (2 * dot(N, H) * dot(N, V)) / dot(V, H)), (2 * dot(N, H) * dot(N, L)) / dot(V, H));
        
        vec4 firstTerm = getSpecularColor(fUV) * (Fbeta / M_PI);
        
        float secondTerm = dot(D, G) / (dot(N, V) * dot(N, L));
        
        vec4 kd = getDiffuseColor(fUV);
        
        vec4 thirdTerm = max(dot(N, L), 0) * (vec4(lightIntensity[i], 0) / (r * r));
        
        vec4 fourthTerm = vec4(ambientLightIntensity, 0.0) * getDiffuseColor(fUV);
        
        finalColor += (firstTerm * secondTerm + kd) * thirdTerm + fourthTerm;
    }
    
    gl_FragColor = (finalColor) * exposure;
}