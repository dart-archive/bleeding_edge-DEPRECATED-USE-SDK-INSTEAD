// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub.validator.sdk_constraint;

import 'dart:async';

import 'package:pub_semver/pub_semver.dart';

import '../entrypoint.dart';
import '../log.dart' as log;
import '../package.dart';
import '../validator.dart';

/// A validator that validates that a package's SDK constraint doesn't use the
/// "^" syntax.
class SdkConstraintValidator extends Validator {
  SdkConstraintValidator(Entrypoint entrypoint)
      : super(entrypoint);

  Future validate() {
    final completer0 = new Completer();
    scheduleMicrotask(() {
      try {
        var constraint = entrypoint.root.pubspec.environment.sdkVersion;
        join0() {
          errors.add(
              "^ version constraints aren't allowed for SDK constraints since "
                  "older versions of pub don't support them.\n" "Expand it manually instead:\n"
                  "\n" "environment:\n" "  sdk: \">=${constraint.min} <${constraint.max}\"");
          completer0.complete();
        }
        if (!constraint.toString().startsWith("^")) {
          completer0.complete(null);
        } else {
          join0();
        }
      } catch (e, s) {
        completer0.completeError(e, s);
      }
    });
    return completer0.future;
  }
}
