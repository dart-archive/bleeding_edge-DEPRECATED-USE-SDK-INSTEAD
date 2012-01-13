// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("compiler_helper.dart");
#import("../../../leg/ssa/ssa.dart");

final String TEST_ONE = @"""
foo(param0) {
  return param0.intValue - 2;
}
""";

final String TEST_TWO = @"""
sum(start) {
  for (var i = 0; i < 10; i += 1) start = start + i;
  return start;
}
""";

final String TEST_THREE = @"""
sum(param0) {
  var start = param0.intValue;
  for (var i = 0; i < 10; i += 1) start = start + i;
  return start;
}
""";

final String TEST_FOUR = @"""
sum(param0) {
  if (param0) {
    var start = param0.intValue;
    return start - 1;
  } else {
    var start = param0.intValue;
    return start - 1;
  }
}
""";


testGraphs(HGraphPair graphs) {
  Expect.isNotNull(graphs);
  Expect.isNotNull(graphs.optimized);
  Expect.isNotNull(graphs.unoptimized);
  OptimizedGraphVisitor opt = new OptimizedGraphVisitor();
  UnoptimizedGraphVisitor unopt = new UnoptimizedGraphVisitor();
  opt.visitGraph(graphs.optimized);
  unopt.visitGraph(graphs.unoptimized);
  Expect.equals(opt.guards.length, unopt.bailouts.length);

  for (int i = 0; i < opt.guards.length; i++) {
    HTypeGuard guard = opt.guards[i];
    HBailoutTarget bailout = unopt.bailouts[i];
    int guardingStatic = (guard.guarded is HStatic) ? 1 : 0;
    Expect.equals(guard.inputs.length,
                  bailout.inputs.length + guardingStatic);
  }
}

main() {
  testGraphs(getGraphs(TEST_ONE, 'foo'));
  testGraphs(getGraphs(TEST_TWO, 'sum'));
  testGraphs(getGraphs(TEST_THREE, 'sum'));
  testGraphs(getGraphs(TEST_FOUR, 'sum'));
}

class OptimizedGraphVisitor extends HBaseVisitor {
  List<HTypeGuard> guards;
  List<HTypeGuard> parameterGuards;
  OptimizedGraphVisitor();

  void visitGraph(HGraph graph) {
    guards = [];
    parameterGuards = [];
    visitDominatorTree(graph);
  }

  void visitTypeGuard(HTypeGuard guard) {
    if (guard.guarded is HParameterValue) {
      parameterGuards.add(guard);
    } else {
      guards.add(guard);
    }
  }

  void visitBailoutTarget(HBailoutTarget target) {
    Expect.fail('bailout in optimized');
  }
}

class UnoptimizedGraphVisitor extends HBaseVisitor {
  List<HBailoutTarget> bailouts;
  UnoptimizedGraphVisitor();

  void visitGraph(HGraph graph) {
    bailouts = [];
    visitDominatorTree(graph);
  }

  void visitTypeGuard(HTypeGuard guard) {
    Expect.fail('guard in unoptimized');
  }

  void visitBailoutTarget(HBailoutTarget target) {
    bailouts.add(target);
  }
}
