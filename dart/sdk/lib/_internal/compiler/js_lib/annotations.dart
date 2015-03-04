// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of _js_helper;

/// Tells the optimizing compiler that the annotated method has no
/// side-effects.
/// Requires @NoInline() to function correctly.
class NoSideEffects {
  const NoSideEffects();
}

/// Tells the optimizing compiler that the annotated method cannot throw.
/// Requires @NoInline() to function correctly.
class NoThrows {
  const NoThrows();
}

/// Tells the optimizing compiler to not inline the annotated method.
class NoInline {
  const NoInline();
}

/// Tells the optimizing compiler to always inline the annotated method.
class ForceInline {
  const ForceInline();
}

// Ensures that the annotated method is represented internally using
// IR nodes ([:value == true:]) or AST nodes ([:value == false:]).
class IrRepresentation {
  final bool value;
  const IrRepresentation(this.value);
}

/// Marks a class as native and defines its JavaScript name(s).
class Native {
  final String name;
  const Native(this.name);
}

class _Patch {
  final String version;

  const _Patch(this.version);
}

/// Annotation that marks the declaration as a patch.
const _Patch patch = const _Patch(null);

/// Annotation that marks the declaration as a patch for the old emitter.
const _Patch patch_old = const _Patch('old');

/// Annotation that marks the declaration as a patch for the new emitter.
const _Patch patch_new = const _Patch('new');
