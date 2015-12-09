#version 120

// You May Use The Following Functions As RenderMaterial Input
// vec4 getDiffuseColor(vec2 uv) // samples fire.png
// vec4 getNormalColor(vec2 uv)  // samples noise.png

uniform float time;

const vec3 texture_scales = vec3(1.0, 2.0, 3.0);
const vec3 scroll_speeds = vec3(0.1, 0.1, 0.1);

varying vec2 fUV;
varying vec3 fPos;



vec2 coord1;
vec2 coord2;
vec2 coord3;

void main() {
    // TODO#PPA2 SOLUTION START
    coord1.x = fUV.x * texture_scales.x; //u
    coord1.y = fUV.y * texture_scales.x; //v
    
    coord2.x = fUV.x * texture_scales.y; //u
    coord2.y = fUV.y * texture_scales.y; //v
    
    coord3.x = fUV.x * texture_scales.z; //u
    coord3.y = fUV.y * texture_scales.z; //v
    
    
    coord1.y = coord1.y + time * scroll_speeds.x;
    coord2.y = coord2.y + time * scroll_speeds.y;
    coord3.y = coord3.y + time * scroll_speeds.z;
    
    //coord1.x = coord1.x * time * scroll_speeds.x;
    //coord2.x = coord2.x * time * scroll_speeds.y;
    //coord3.x = coord3.x * time * scroll_speeds.z;
    
    
    coord1.x = mod(coord1.x, 1.0);
    coord2.x = mod(coord2.x, 1.0);
    coord3.x = mod(coord3.x, 1.0);
    
    coord1.y = mod(coord1.y, 1.0);
    coord2.y = mod(coord2.y, 1.0);
    coord3.y = mod(coord3.y, 1.0);
    ///if (coord2.y > 1) coord2.y = coord2.y - int(coord2.y) / 1;
    //if (coord3.y > 1) coord3.y = coord3.y - int(coord3.y) / 1;
    
    //if (coord1.x > 1) coord1.x = coord1.x - int(coord1.x) / 1;
    //if (coord2.x > 1) coord2.x = coord2.x - int(coord2.x) / 1;
    //if (coord3.x > 1) coord3.x = coord3.x - int(coord3.x) / 1;
    
    //UV.x = (coord1.x + coord2.x + coord3.x) / 3;
    //UV.y = (coord1.y + coord2.y + coord3.y) / 3;
    vec4 sample_color1 = getNormalColor(coord1);
    vec4 sample_color2 = getNormalColor(coord2);
    vec4 sample_color3 = getNormalColor(coord3);
    vec2 UV = vec2((sample_color1.x + sample_color2.x + sample_color3.x) / 3, (sample_color1.y + sample_color2.y + sample_color3.y) / 3);
    gl_FragColor = getDiffuseColor(UV);
    // SOLUTION END
}