// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('js_evaluator_node');

#import('lib/node/node.dart');
#import('evaluator.dart');


class NodeJsEvaluator implements JsEvaluator {
  Context _context;

  NodeJsEvaluator() : this._context = vm.createContext(createSandbox());

  eval(String js) => vm.runInContext(js, this._context);
}
