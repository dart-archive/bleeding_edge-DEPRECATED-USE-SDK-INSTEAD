// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of dart2js.js_emitter;

class TypeTestEmitter extends CodeEmitterHelper {
  void emitIsTests(ClassElement classElement, ClassBuilder builder) {
    new TypeTestGenerator(compiler, emitter.task, namer)
        .emitIsTests(classElement, builder);
  }
}