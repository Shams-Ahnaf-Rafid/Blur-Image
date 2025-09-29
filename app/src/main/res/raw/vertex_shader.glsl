attribute vec2 a_Position;

uniform float u_AspectRatio;
uniform float u_ImageAspectRatio;
uniform int u_display;

varying vec2 v_TexCoord;

void main() {
    vec2 pos = a_Position;

    if (u_display == 1) {
        if (u_AspectRatio > u_ImageAspectRatio) {
            pos.x *= u_ImageAspectRatio / u_AspectRatio;
        } else {
            pos.y *= u_AspectRatio / u_ImageAspectRatio;
        }
        pos *= 0.9;
    }

    gl_Position = vec4(pos, 0.0, 1.0);
    v_TexCoord = a_Position * 0.5 + 0.5;
}