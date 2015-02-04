// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library js_tree_ir_builder;

import '../../tree_ir/tree_ir_builder.dart' show Builder;
import 'glue.dart' show Glue;
import '../../dart2jslib.dart' show Selector, InternalErrorFunction;
import '../../elements/elements.dart';
import '../../cps_ir/cps_ir_nodes.dart' as cps_ir;
import '../../tree_ir/tree_ir_nodes.dart';

/// Subclass of [Builder] that can translate nodes which are specific to the
/// JavaScript backend.
class JsTreeBuilder extends Builder {
  final Glue _glue;
  final Element identicalFunction;

  JsTreeBuilder(
      InternalErrorFunction internalError,
      this.identicalFunction,
      this._glue,
      [Builder parent])
    : super(internalError, parent);

  JsTreeBuilder createInnerBuilder() {
    return new JsTreeBuilder(internalError, identicalFunction, _glue, this);
  }

  Selector get identicalSelector {
    return new Selector.call('identical', null, 2);
  }

  Expression visitIdentical(cps_ir.Identical node) {
    return new InvokeStatic(
        identicalFunction,
        identicalSelector,
        <Expression>[getVariableReference(node.left),
                     getVariableReference(node.right)]);
  }

  Expression visitInterceptor(cps_ir.Interceptor node) {
    Element getInterceptor = _glue.getInterceptorMethod;
    String name = _glue.getInterceptorName(node.interceptedClasses);
    Selector selector = new Selector.call(name, null, 1);
    _glue.registerUseInterceptorInCodegen();
    return new InvokeStatic(
        getInterceptor,
        selector,
        <Expression>[getVariableReference(node.input)]);
  }

  Expression visitGetField(cps_ir.GetField node) {
    return new GetField(getVariableReference(node.object), node.field);
  }

  Statement visitSetField(cps_ir.SetField node) {
    return new SetField(getVariableReference(node.object),
                        node.field,
                        getVariableReference(node.value),
                        visit(node.body));
  }

  Expression visitCreateBox(cps_ir.CreateBox node) {
    return new CreateBox();
  }

  Expression visitCreateInstance(cps_ir.CreateInstance node) {
    return new CreateInstance(
        node.classElement,
        node.arguments.map(getVariableReference).toList());
  }
}
