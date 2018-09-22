attribute vec4 a_Position;
//添加了一个 a_TextureCoordinates ,因为他有两个分量。S坐标和T坐标，所以定义为vec2.
attribute vec2 a_TextureCoordinates;
uniform mat4 u_Matrix;
//然后把坐标传递给被插值的varying
varying vec2 v_TextureCoordinates;

void main(){
    gl_Position=u_Matrix*a_Position;
    v_TextureCoordinates=a_TextureCoordinates;
}