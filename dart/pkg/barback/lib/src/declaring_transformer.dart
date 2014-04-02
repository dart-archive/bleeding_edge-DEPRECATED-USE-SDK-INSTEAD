// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library barback.declaring_transformer;

import 'dart:async';

import 'declaring_transform.dart';

/// An interface for [Transformer]s that can cheaply figure out which assets
/// they'll emit without doing the work of actually creating those assets.
///
/// If a transformer implements this interface, that allows barback to perform
/// optimizations to make the asset graph work more smoothly.
abstract class DeclaringTransformer {
  /// Declare which assets would be emitted for the primary input specified by
  /// [transform].
  ///
  /// For the most part, this works like [Transformer.apply]. The difference is
  /// that instead of emitting [Asset]s, it just emits [AssetId]s through
  /// [transform.addOutputId].
  Future declareOutputs(DeclaringTransform transform);
}
