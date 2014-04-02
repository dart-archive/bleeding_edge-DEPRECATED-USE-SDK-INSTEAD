// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub.asset.transformer_isolate;

import 'dart:convert';
import 'dart:isolate';
import 'dart:mirrors';

import 'package:barback/barback.dart';

import 'serialize.dart';

/// Sets up the initial communication with the host isolate.
void loadTransformers(SendPort replyTo) {
  var port = new ReceivePort();
  replyTo.send(port.sendPort);
  port.first.then((wrappedMessage) {
    respond(wrappedMessage, (message) {
      var library = Uri.parse(message['library']);
      var configuration = JSON.decode(message['configuration']);
      var mode = new BarbackMode(message['mode']);
      return _initialize(library, configuration, mode).
          map(serializeTransformerOrGroup).toList();
    });
  });
}

/// Loads all the transformers and groups defined in [uri].
///
/// Loads the library, finds any Transformer or TransformerGroup subclasses in
/// it, instantiates them with [configuration] and [mode], and returns them.
Iterable _initialize(Uri uri, Map configuration, BarbackMode mode) {
  var mirrors = currentMirrorSystem();
  var transformerClass = reflectClass(Transformer);
  var groupClass = reflectClass(TransformerGroup);

  // TODO(nweiz): if no valid transformers are found, throw an error message
  // describing candidates and why they were rejected.
  return mirrors.libraries[uri].declarations.values.map((declaration) {
    if (declaration is! ClassMirror) return null;
    var classMirror = declaration;
    if (classMirror.isPrivate) return null;
    if (classMirror.isAbstract) return null;
    if (!classMirror.isSubtypeOf(transformerClass) &&
        !classMirror.isSubtypeOf(groupClass)) {
      return null;
    }

    var constructor = _getConstructor(classMirror, 'asPlugin');
    if (constructor == null) return null;
    if (constructor.parameters.isEmpty) {
      if (configuration.isNotEmpty) return null;
      return classMirror.newInstance(const Symbol('asPlugin'), []).reflectee;
    }
    if (constructor.parameters.length != 1) return null;

    return classMirror.newInstance(const Symbol('asPlugin'),
        [new BarbackSettings(configuration, mode)]).reflectee;
  }).where((classMirror) => classMirror != null);
}

// TODO(nweiz): clean this up when issue 13248 is fixed.
MethodMirror _getConstructor(ClassMirror classMirror, String constructor) {
  var name = new Symbol("${MirrorSystem.getName(classMirror.simpleName)}"
      ".$constructor");
  var candidate = classMirror.declarations[name];
  if (candidate is MethodMirror && candidate.isConstructor) return candidate;
  return null;
}
