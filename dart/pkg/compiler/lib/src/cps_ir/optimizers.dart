// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library dart2js.cps_ir.optimizers;

import '../constants/expressions.dart' show
    ConstantExpression,
    PrimitiveConstantExpression;
import '../constants/values.dart';
import '../dart_types.dart' as types;
import '../dart2jslib.dart' as dart2js;
import '../tree/tree.dart' show LiteralDartString;
import '../util/util.dart';
import 'cps_ir_nodes.dart';
import '../types/types.dart' show TypeMask, TypesTask;
import '../core_types.dart' show CoreTypes;
import '../types/constants.dart' show computeTypeMask;
import '../elements/elements.dart' show ClassElement, Element, Entity,
    FieldElement, FunctionElement, ParameterElement;
import '../dart2jslib.dart' show ClassWorld;

part 'type_propagation.dart';
part 'redundant_phi.dart';
part 'shrinking_reductions.dart';

/// An optimization pass over the CPS IR.
abstract class Pass {
  /// Applies optimizations to root, rewriting it in the process.
  void rewrite(ExecutableDefinition root) => root.applyPass(this);

  void rewriteConstructorDefinition(ConstructorDefinition root);
  void rewriteFunctionDefinition(FunctionDefinition root);
  void rewriteFieldDefinition(FieldDefinition root);
}

abstract class PassMixin implements Pass {
  void rewrite(ExecutableDefinition root) => root.applyPass(this);

  void rewriteExecutableDefinition(ExecutableDefinition root);

  void rewriteFunctionDefinition(FunctionDefinition root) {
    if (root.isAbstract) return;
    rewriteExecutableDefinition(root);
  }

  void rewriteConstructorDefinition(ConstructorDefinition root) {
    if (root.isAbstract) return;
    rewriteExecutableDefinition(root);
  }
  void rewriteFieldDefinition(FieldDefinition root) {
    if (!root.hasInitializer) return;
    rewriteExecutableDefinition(root);
  }
}
