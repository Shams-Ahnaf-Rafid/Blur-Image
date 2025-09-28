precision mediump float;

uniform float u_Thickness;
uniform vec2 u_Points[2];
uniform vec2 u_resolution;
uniform sampler2D u_Mask;
uniform sampler2D u_Forest;
uniform sampler2D u_Blur;
uniform bool u_Remove;
uniform bool u_Horizontal;
uniform int u_Display;
uniform float u_Intensity;

varying vec2 v_TexCoord;

vec4 gaussianBlur(sampler2D image, vec2 texCoord, vec2 direction, vec2 resolution) {
    vec2 texelSize = u_Intensity / resolution / 20.0;
    vec2 offset = direction * texelSize;

    float w0 = 0.153388;
    float w1 = 0.144893;
    float w2 = 0.122649;
    float w3 = 0.092902;
    float w4 = 0.062084;

    vec4 result = texture2D(image, texCoord) * w0;

    result += texture2D(image, texCoord + offset * 1.0) * w1;
    result += texture2D(image, texCoord - offset * 1.0) * w1;

    result += texture2D(image, texCoord + offset * 2.0) * w2;
    result += texture2D(image, texCoord - offset * 2.0) * w2;

    result += texture2D(image, texCoord + offset * 3.0) * w3;
    result += texture2D(image, texCoord - offset * 3.0) * w3;

    result += texture2D(image, texCoord + offset * 4.0) * w4;
    result += texture2D(image, texCoord - offset * 4.0) * w4;

//    result = (2.0 * texture2D(image, texCoord)) - result;

    return result;
}

void main() {
    if (u_Display == 2) {
        if (u_Horizontal) {
            gl_FragColor = gaussianBlur(u_Forest, v_TexCoord, vec2(1.0, 0.0), u_resolution);
        }
        else {
            gl_FragColor = gaussianBlur(u_Blur, v_TexCoord, vec2(0.0, 1.0), u_resolution);
        }
    }
    else if (u_Display == 1) {
        float a = texture2D(u_Mask, v_TexCoord).r;
        if (a == 0.0) gl_FragColor = texture2D(u_Forest, v_TexCoord);
        else gl_FragColor = texture2D(u_Blur, v_TexCoord);
    }
    else {
        vec2 fragPos = gl_FragCoord.xy;

        vec2 a = u_Points[0] * 0.5 + 0.5;
        vec2 b = u_Points[1] * 0.5 + 0.5;

        a *= u_resolution;
        b *= u_resolution;

        vec2 ab = b - a;
        vec2 ap = fragPos - a;

        float t = clamp(dot(ap, ab) / dot(ab, ab), 0.0, 1.0);
        vec2 closest = a + t * ab;

        float dist = length(fragPos - closest);

        if (dist <= u_Thickness * 0.5) {
            if (u_Remove) {
                gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
            }
            else {
                gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
            }
        }
        else {
            gl_FragColor = texture2D(u_Mask, v_TexCoord);
        }
    }
}