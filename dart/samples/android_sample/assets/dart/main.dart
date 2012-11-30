// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import "dart:math" as Math;
import "android_extension.dart";

const FRAGMENT_PROGRAM = """
  precision mediump float;

  const vec3 lightDir = vec3(0.577350269, 0.577350269, -0.577350269);
  varying vec3 vPosition;
  uniform vec3 cameraPos;
  uniform vec3 sphere1Center;
  uniform vec3 sphere2Center;
  uniform vec3 sphere3Center;

  bool intersectSphere(vec3 center, vec3 lStart, vec3 lDir,
                       out float dist) {
    vec3 c = center - lStart;
    float b = dot(lDir, c);
    float d = b*b - dot(c, c) + 1.0;
    if (d < 0.0) {
      dist = 10000.0;
      return false;
    }

    dist = b - sqrt(d);
    if (dist < 0.0) {
      dist = 10000.0;
      return false;
    }

    return true;
  }

  vec3 lightAt(vec3 N, vec3 V, vec3 color) {
    vec3 L = lightDir;
    vec3 R = reflect(-L, N);

    float c = 0.3 + 0.4 * pow(max(dot(R, V), 0.0), 30.0) + 0.7 * dot(L, N);

    if (c > 1.0) {
      return mix(color, vec3(1.6, 1.6, 1.6), c - 1.0);
    }

    return c * color;
  }

  bool intersectWorld(vec3 lStart, vec3 lDir, out vec3 pos,
                      out vec3 normal, out vec3 color) {
    float d1, d2, d3;
    bool h1, h2, h3;

    h1 = intersectSphere(sphere1Center, lStart, lDir, d1);
    h2 = intersectSphere(sphere2Center, lStart, lDir, d2);
    h3 = intersectSphere(sphere3Center, lStart, lDir, d3);

    if (h1 && d1 < d2 && d1 < d3) {
      pos = lStart + d1 * lDir;
      normal = pos - sphere1Center;
      color = vec3(0.0, 0.0, 0.9);
      if (fract(pos.x / 1.5) > 0.5 ^^
          fract(pos.y / 1.5) > 0.5 ^^
          fract(pos.z / 1.5) > 0.5) {
        color = vec3(1.0, 0.0, 1.0);
      }
      else {
        color = vec3(1.0, 1.0, 0.0);
      }
    }
    else if (h2 && d2 < d3) {
      pos = lStart + d2 * lDir;
      normal = pos - sphere2Center;
      color = vec3(0.9, mod(normal.y * 2.5, 1.0), 0.0);
    }
    else if (h3) {
      pos = lStart + d3 * lDir;
      normal = pos - sphere3Center;
      color = vec3(0.0, clamp(sphere3Center.y/1.5, 0.0, 0.9),
                   clamp(0.9 - sphere3Center.y/1.5, 0.0, 0.9));
    }
    else if (lDir.y < -0.01) {
      pos = lStart + ((lStart.y + 2.7) / -lDir.y) * lDir;
      if (pos.x*pos.x + pos.z*pos.z > 30.0) {
        return false;
      }
      normal = vec3(0.0, 1.0, 0.0);
      if (fract(pos.x / 5.0) > 0.5 == fract(pos.z / 5.0) > 0.5) {
        color = vec3(1.0);
      }
      else {
        color = vec3(0.0);
      }
    }
    else {
     return false;
    }

    return true;
  }

  void main(void)
  {
    vec3 cameraDir = normalize(vPosition - cameraPos);

    vec3 p1, norm, p2;
    vec3 col, colT, colM, col3;
    if (intersectWorld(cameraPos, cameraDir, p1,
                       norm, colT)) {
      col = lightAt(norm, -cameraDir, colT);
      colM = (colT + vec3(0.2)) / 1.2;
      cameraDir = reflect(cameraDir, norm);
      if (intersectWorld(p1, cameraDir, p2, norm, colT)) {
        col += lightAt(norm, -cameraDir, colT) * colM;
        colM *= (colT + vec3(0.2)) / 1.2;
        cameraDir = reflect(cameraDir, norm);
        if (intersectWorld(p2, cameraDir, p1, norm, colT)) {
          col += lightAt(norm, -cameraDir, colT) * colM;
        }
      }

      gl_FragColor = vec4(col, 1.0);
    }
    else {
      gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
  }
""";

const VERTEX_PROGRAM = """
  attribute vec2 aVertexPosition;
  attribute vec3 aPlotPosition;

  varying vec3 vPosition;

  void main(void)
  {
    gl_Position = vec4(aVertexPosition, 1.0, 1.0);
    vPosition = aPlotPosition;
 }
""";

loadShader(final type, final program) {
  final shader = gl.createShader(type);
  gl.shaderSource(shader, program);
  gl.compileShader(shader);
  if (gl.getShaderParameter(shader, WebGLRenderingContext.COMPILE_STATUS)
      != WebGLRenderingContext.TRUE) {
    log("Could not compile shader: ${gl.getShaderInfoLog(shader)}");
    throw new Exception("Could not compile shader");
  }
  
  return shader;
}

var shaderProgram;
var aVertexPosition;
var aPlotPosition;
var cameraPos;
var sphere1Center;
var sphere2Center;
var sphere3Center;
var ratio;

initShaders() {
  var vertexShader = loadShader(WebGLRenderingContext.VERTEX_SHADER,
      VERTEX_PROGRAM);
  var fragmentShader = loadShader(WebGLRenderingContext.FRAGMENT_SHADER,
      FRAGMENT_PROGRAM);

  shaderProgram = gl.createProgram();
  if (shaderProgram == 0) {
    throw new Exception("Could not create program.");
  }

  gl.attachShader(shaderProgram, vertexShader);
  gl.attachShader(shaderProgram, fragmentShader);
  gl.linkProgram(shaderProgram);

  if (gl.getProgramParameter(shaderProgram, WebGLRenderingContext.LINK_STATUS)
      != WebGLRenderingContext.TRUE) {
    log("Could not link program: ${gl.getProgramInfoLog(shaderProgram)}");
    throw new Exception("Could not initialize shaders");
  }
  
  gl.useProgram(shaderProgram);

  aVertexPosition = gl.getAttribLocation(shaderProgram, "aVertexPosition");
  gl.enableVertexAttribArray(aVertexPosition);

  aPlotPosition = gl.getAttribLocation(shaderProgram, "aPlotPosition");
  gl.enableVertexAttribArray(aPlotPosition);

  cameraPos = gl.getUniformLocation(shaderProgram, "cameraPos");
  sphere1Center = gl.getUniformLocation(shaderProgram, "sphere1Center");
  sphere2Center = gl.getUniformLocation(shaderProgram, "sphere2Center");
  sphere3Center = gl.getUniformLocation(shaderProgram, "sphere3Center");
}

initBuffers() {
  var vertexPositionBuffer = gl.createBuffer();
  gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexPositionBuffer);
  // TODO(gram): vertices should be a Float32Array to be consistent with WebGL.
  var vertices = [
                  1.0,  1.0,
                  -1.0,  1.0,
                  1.0, -1.0,
                  -1.0, -1.0,
                  ];

  gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertices,
      WebGLRenderingContext.STATIC_DRAW);
  gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexPositionBuffer);
  gl.vertexAttribPointer(aVertexPosition, 2, WebGLRenderingContext.FLOAT,
      false,0, 0);

  var plotPositionBuffer = gl.createBuffer();
  gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, plotPositionBuffer);
  gl.vertexAttribPointer(aPlotPosition, 3, WebGLRenderingContext.FLOAT,
      false, 0, 0);
}

class Vector {
  var x;
  var y;
  var z;
  Vector(this.x, this.y, this.z);
}

// TODO(gram): This should be using vector_math.
crossProd(v1, v2) {
  return
    new Vector(
               v1.y*v2.z - v2.y*v1.z,
               v1.z*v2.x - v2.z*v1.x,
               v1.x*v2.y - v2.x*v1.y);
}

normalize(v) {
  var l = Math.sqrt(v.x*v.x + v.y*v.y + v.z*v.z);
  return new Vector( v.x/l, v.y/l, v.z/l );
}

vectAdd(v1, v2) {
  return new Vector( v1.x + v2.x, v1.y + v2.y, v1.z + v2.z );
}

vectSub(v1, v2) {
  return new Vector( v1.x - v2.x, v1.y - v2.y, v1.z - v2.z );
}

vectMul(v, l) {
  return new Vector( v.x*l, v.y*l, v.z*l );
}

pushVec(v, arr) {
  arr.addAll([v.x, v.y, v.z]);
}

var t = 0;
int width, height;

drawScene() {
  var x1 = Math.sin(t * 1.1) * 1.5;
  var y1 = Math.cos(t * 1.3) * 1.5;
  var z1 = Math.sin(t + Math.PI/3) * 1.5;
  var x2 = Math.cos(t * 1.2) * 1.5;
  var y2 = Math.sin(t * 1.4) * 1.5;
  var z2 = Math.sin(t*1.25 - Math.PI/3) * 1.5;
  var x3 = Math.cos(t * 1.15) * 1.5;
  var y3 = Math.sin(t * 1.37) * 1.5;
  var z3 = Math.sin(t*1.27) * 1.5;

  var cameraFrom = new Vector(
                          Math.sin(t * 0.4) * 18,
                          Math.sin(t * 0.13) * 5 + 5,
                          Math.cos(t * 0.4) * 18 );
  var cameraTo = new Vector(0.0, 0.0, 0.0);
  var cameraPersp = 6.0;
  var up = new Vector(0.0, 1.0, 0.0);
  var cameraDir = normalize(vectSub(cameraTo, cameraFrom));

  var cameraLeft = normalize(crossProd(cameraDir, up));
  var cameraUp = normalize(crossProd(cameraLeft, cameraDir));
  // cameraFrom + cameraDir * cameraPersp
  var cameraCenter = vectAdd(cameraFrom, vectMul(cameraDir, cameraPersp));

  // cameraCenter + cameraUp + cameraLeft * ratio
  var cameraTopLeft  = vectAdd(vectAdd(cameraCenter, cameraUp),
                               vectMul(cameraLeft, ratio));
  var cameraBotLeft  = vectAdd(vectSub(cameraCenter, cameraUp),
                               vectMul(cameraLeft, ratio));
  var cameraTopRight = vectSub(vectAdd(cameraCenter, cameraUp),
                               vectMul(cameraLeft, ratio));
  var cameraBotRight = vectSub(vectSub(cameraCenter, cameraUp),
                               vectMul(cameraLeft, ratio));

  // TODO(gram): corners should be a Float32Array to be consistent with WebGL.
  var corners = [];
  pushVec(cameraTopRight, corners);
  pushVec(cameraTopLeft, corners);
  pushVec(cameraBotRight, corners);
  pushVec(cameraBotLeft, corners);

  gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, corners,
      WebGLRenderingContext.STATIC_DRAW);

  gl.uniform3f(cameraPos, cameraFrom.x, cameraFrom.y, cameraFrom.z);
  gl.uniform3f(sphere1Center, x1, y1, z1);
  gl.uniform3f(sphere2Center, x2, y2, z2);
  gl.uniform3f(sphere3Center, x3, y3, z3);

  gl.drawArrays(WebGLRenderingContext.TRIANGLE_STRIP, 0, 4);

  t += 0.03;
  if (t > Math.PI * 200) {
    t -= Math.PI * 200;
  }
}

setup(int w, int h) {
  initShaders();
  gl.clearColor(0.0, 0.0, 0.0, 1.0);
  gl.clearDepth(1.0);
  initBuffers();
  resize(w, h);
}

resize(int w, int h) {
  width = w;
  height = h;
  ratio = width / height;
  gl.viewport(0, 0, width, height);
  t -= 0.03;
  drawScene();
}

update() {
  drawScene();
  eglSwapBuffers();
}

onMotionDown(num when, num x, num y) {
  t = (x / width) * Math.PI * 200;
}

onMotionUp(num when, num x, num y) {}
onMotionMove(num when, num x, num y) {}
onMotionCancel(num when, num x, num y) {}
onMotionOutside(num when, num x, num y) {}
onMotionPointerDown(num when, num x, num y) {}
onMotionPointerUp(num when, num x, num y) {}
onKeyDown(num when, int flags, int keycode, int metastate, int repeat) {}
onKeyUp(num when, int flags, int keycode, int metastate, int repeat) {}
onKeyMultiple(num when, int flags, int keycode, int metastate, int repeat) {
}

