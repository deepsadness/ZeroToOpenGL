precision mediump float;

varying vec2 vTextureCoordinate;
uniform sampler2D uTexture;
void main() {
    gl_FragColor = texture2D(uTexture,vTextureCoordinate);
}