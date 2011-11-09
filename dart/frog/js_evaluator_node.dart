// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('js_evaluator_node');

#import('lib/node/node.dart');
#import('evaluator.dart');

class NodeJsEvaluator implements JsEvaluator {
  var _sandbox;

  NodeJsEvaluator(): this._sandbox = createSandbox();

  var eval(String js) => vm.runInNewContext(js, _sandbox);
}
