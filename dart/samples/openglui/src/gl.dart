// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * An HTML-based prototype implementation for GL.
 */

// TODO(vsm): Move this out of the common directory.
library gl_html;

import 'dart:html';
export 'dart:html' show WebGLRenderingContext;
export 'dart:html' show Float32Array;

WebGLRenderingContext gl;

log(message) => window.console.log(message);

