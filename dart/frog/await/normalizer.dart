// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** Normalizes the AST to make the translation in [AwaitProcessor] simpler. */
class AwaitNormalizer implements TreeVisitor {
  // TODO(sigmund): implement normalization. [AwaitProcessor] assumes that:
  //
  // - await only occurs in top-level assignments. For example:
  //      if (await t) return;
  //   after normalization should become:
  //      final $t = await t;
  //      if ($t) return;
  //
  // - await in declarations are split in multiple declarations:
  //      int x = 1, y = await t, z = 3, w = y;
  //   becomes:
  //      int x = 1;
  //      int y = await t;
  //      int z = 3, w = y;
  //
  // - await cannot occur on complex assignments:
  //      x += await t
  //   becomes:
  //      $t = await t
  //      x += $t
  //
  // - await cannot occur outside statement blocks:
  //      if (...) x = await t
  //   becomes:
  //      if (...) { x = await t }
}
