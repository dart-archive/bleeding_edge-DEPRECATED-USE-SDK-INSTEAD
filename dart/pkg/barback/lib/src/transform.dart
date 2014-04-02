// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library barback.transform;

import 'asset.dart';
import 'asset_set.dart';
import 'base_transform.dart';
import 'transform_node.dart';

/// While a [Transformer] represents a *kind* of transformation, this defines
/// one specific usage of it on a set of files.
///
/// This ephemeral object exists only during an actual transform application to
/// facilitate communication between the [Transformer] and the code hosting
/// the transformation. It lets the [Transformer] access inputs and generate
/// outputs.
class Transform extends BaseTransform {
  final _outputs = new AssetSet();

  Transform._(TransformNode node)
    : super(node);

  /// Stores [output] as the output created by this transformation.
  ///
  /// A transformation can output as many assets as it wants.
  void addOutput(Asset output) {
    // TODO(rnystrom): This should immediately throw if an output with that ID
    // has already been created by this transformer.
    _outputs.add(output);
  }
}

/// The controller for [Transform].
class TransformController extends BaseTransformController {
  Transform get transform => super.transform;

  /// The set of assets that the transformer has emitted.
  AssetSet get outputs => transform._outputs;

  TransformController(TransformNode node)
      : super(new Transform._(node));
}
