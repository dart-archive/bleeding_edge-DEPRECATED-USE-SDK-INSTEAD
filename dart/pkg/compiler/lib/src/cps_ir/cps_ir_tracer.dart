// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library dart2js.ir_tracer;

import 'dart:async' show EventSink;

import 'cps_ir_nodes.dart' as cps_ir hide Function;
import '../tracer.dart';

/**
 * If true, show LetCont expressions in output.
 */
const bool IR_TRACE_LET_CONT = false;

class IRTracer extends TracerUtil implements cps_ir.Visitor {
  EventSink<String> output;

  IRTracer(this.output);

  visit(cps_ir.Node node) => node.accept(this);

  void traceGraph(String name, cps_ir.ExecutableDefinition graph) {
    tag("cfg", () {
      printProperty("name", name);
      visit(graph);
    });
  }

  // Temporary field used during tree walk
  Names names;

  visitExecutableDefinition(cps_ir.ExecutableDefinition node) {
    names = new Names();
    BlockCollector builder = new BlockCollector(names);
    builder.visit(node);

    for (Block block in builder.entries) {
      printNode(block);
    }
    for (Block block in builder.cont2block.values) {
      printNode(block);
    }
    names = null;
  }

  visitFieldDefinition(cps_ir.FieldDefinition node) {
    if (node.hasInitializer) {
      visitExecutableDefinition(node);
    }
  }

  visitFunctionDefinition(cps_ir.FunctionDefinition node) {
    if (node.isAbstract) return;
    visitExecutableDefinition(node);
  }

  visitConstructorDefinition(cps_ir.ConstructorDefinition node) {
    if (node.isAbstract) return;
    visitExecutableDefinition(node);
  }

  int countUses(cps_ir.Definition definition) {
    int count = 0;
    cps_ir.Reference ref = definition.firstRef;
    while (ref != null) {
      ++count;
      ref = ref.next;
    }
    return count;
  }

  printNode(Block block) {
    tag("block", () {
      printProperty("name", block.name);
      printProperty("from_bci", -1);
      printProperty("to_bci", -1);
      printProperty("predecessors", block.pred.map((n) => n.name));
      printProperty("successors", block.succ.map((n) => n.name));
      printEmptyProperty("xhandlers");
      printEmptyProperty("flags");
      tag("states", () {
        tag("locals", () {
          printProperty("size", 0);
          printProperty("method", "None");
        });
      });
      tag("HIR", () {
        for (cps_ir.Parameter param in block.parameters) {
          String name = names.name(param);
          printStmt(name, "Parameter $name [useCount=${countUses(param)}]");
        }
        visit(block.body);
      });
    });
  }

  void printStmt(String resultVar, String contents) {
    int bci = 0;
    int uses = 0;
    addIndent();
    add("$bci $uses $resultVar $contents <|@\n");
  }

  visitLetPrim(cps_ir.LetPrim node) {
    String id = names.name(node.primitive);
    printStmt(id, "LetPrim $id = ${formatPrimitive(node.primitive)}");
    visit(node.body);
  }

  visitLetCont(cps_ir.LetCont node) {
    if (IR_TRACE_LET_CONT) {
      String dummy = names.name(node);
      for (cps_ir.Continuation continuation in node.continuations) {
        String id = names.name(continuation);
        printStmt(dummy, "LetCont $id = <$id>");
      }
    }
    visit(node.body);
  }

  visitLetMutable(cps_ir.LetMutable node) {
    String id = names.name(node.variable);
    printStmt(id, "${node.runtimeType} $id = ${formatReference(node.value)}");
    visit(node.body);
  }

  visitInvokeStatic(cps_ir.InvokeStatic node) {
    String dummy = names.name(node);
    String callName = node.selector.name;
    String args = node.arguments.map(formatReference).join(', ');
    String kont = formatReference(node.continuation);
    printStmt(dummy, "InvokeStatic $callName ($args) $kont");
  }

  visitInvokeMethod(cps_ir.InvokeMethod node) {
    String dummy = names.name(node);
    String receiver = formatReference(node.receiver);
    String callName = node.selector.name;
    String args = node.arguments.map(formatReference).join(', ');
    String kont = formatReference(node.continuation);
    printStmt(dummy,
        "InvokeMethod $receiver $callName ($args) $kont");
  }

  visitInvokeMethodDirectly(cps_ir.InvokeMethodDirectly node) {
    String dummy = names.name(node);
    String receiver = formatReference(node.receiver);
    String callName = node.selector.name;
    String args = node.arguments.map(formatReference).join(', ');
    String kont = formatReference(node.continuation);
    printStmt(dummy,
        "InvokeMethodDirectly $receiver $callName ($args) $kont");
  }

  visitInvokeConstructor(cps_ir.InvokeConstructor node) {
    String dummy = names.name(node);
    String callName;
    if (node.target.name.isEmpty) {
      callName = '${node.type}';
    } else {
      callName = '${node.type}.${node.target.name}';
    }
    String args = node.arguments.map(formatReference).join(', ');
    String kont = formatReference(node.continuation);
    printStmt(dummy, "InvokeConstructor $callName ($args) $kont");
  }

  visitConcatenateStrings(cps_ir.ConcatenateStrings node) {
    String dummy = names.name(node);
    String args = node.arguments.map(formatReference).join(', ');
    String kont = formatReference(node.continuation);
    printStmt(dummy, "ConcatenateStrings ($args) $kont");
  }

  visitLiteralList(cps_ir.LiteralList node) {
    String dummy = names.name(node);
    String values = node.values.map(formatReference).join(', ');
    printStmt(dummy, "LiteralList ($values)");
  }

  visitLiteralMap(cps_ir.LiteralMap node) {
    String dummy = names.name(node);
    List<String> entries = new List<String>();
    for (cps_ir.LiteralMapEntry entry in node.entries) {
      String key = formatReference(entry.key);
      String value = formatReference(entry.value);
      entries.add("$key: $value");
    }
    printStmt(dummy, "LiteralMap (${entries.join(', ')})");
  }

  visitTypeOperator(cps_ir.TypeOperator node) {
    String dummy = names.name(node);
    String operator = node.isTypeTest ? 'is' : 'as';
    List<String> entries = new List<String>();
    String receiver = formatReference(node.receiver);
    printStmt(dummy, "TypeOperator ($operator $receiver ${node.type})");
  }

  visitInvokeContinuation(cps_ir.InvokeContinuation node) {
    String dummy = names.name(node);
    String kont = formatReference(node.continuation);
    String args = node.arguments.map(formatReference).join(', ');
    printStmt(dummy, "InvokeContinuation $kont ($args)");
  }

  visitBranch(cps_ir.Branch node) {
    String dummy = names.name(node);
    String condition = visit(node.condition);
    String trueCont = formatReference(node.trueContinuation);
    String falseCont = formatReference(node.falseContinuation);
    printStmt(dummy, "Branch $condition ($trueCont, $falseCont)");
  }

  visitSetMutableVariable(cps_ir.SetMutableVariable node) {
    String dummy = names.name(node);
    String variable = names.name(node.variable.definition);
    String value = formatReference(node.value);
    printStmt(dummy, '${node.runtimeType} $variable := $value');
    visit(node.body);
  }

  visitDeclareFunction(cps_ir.DeclareFunction node) {
    String dummy = names.name(node);
    String variable = names.name(node.variable);
    printStmt(dummy, 'DeclareFunction $variable');
    visit(node.body);
  }

  String formatReference(cps_ir.Reference ref) {
    cps_ir.Definition target = ref.definition;
    if (target is cps_ir.Continuation && target.isReturnContinuation) {
      return "return"; // Do not generate a name for the return continuation
    } else {
      return names.name(ref.definition);
    }
  }

  String formatPrimitive(cps_ir.Primitive p) => visit(p);

  visitConstant(cps_ir.Constant node) {
    return "Constant ${node.expression.value.toStructuredString()}";
  }

  visitParameter(cps_ir.Parameter node) {
    return "Parameter ${names.name(node)}";
  }

  visitMutableVariable(cps_ir.MutableVariable node) {
    return "${node.runtimeType} ${names.name(node)}";
  }

  visitContinuation(cps_ir.Continuation node) {
    return "Continuation ${names.name(node)}";
  }

  visitIsTrue(cps_ir.IsTrue node) {
    return "IsTrue(${names.name(node.value.definition)})";
  }

  visitSetField(cps_ir.SetField node) {
    String dummy = names.name(node);
    String object = formatReference(node.object);
    String field = node.field.name;
    String value = formatReference(node.value);
    printStmt(dummy, 'SetField $object.$field = $value');
    visit(node.body);
  }

  visitGetField(cps_ir.GetField node) {
    String object = formatReference(node.object);
    String field = node.field.name;
    return 'GetField($object.$field)';
  }

  visitCreateBox(cps_ir.CreateBox node) {
    return 'CreateBox';
  }

  visitCreateInstance(cps_ir.CreateInstance node) {
    String className = node.classElement.name;
    String arguments = node.arguments.map(formatReference).join(', ');
    return 'CreateInstance $className ($arguments)';
  }

  visitIdentical(cps_ir.Identical node) {
    String left = formatReference(node.left);
    String right = formatReference(node.right);
    return "Identical($left, $right)";
  }

  visitInterceptor(cps_ir.Interceptor node) {
    return "Interceptor(${formatReference(node.input)})";
  }

  visitThis(cps_ir.This node) {
    return "This";
  }

  visitReifyTypeVar(cps_ir.ReifyTypeVar node) {
    return "ReifyTypeVar ${node.typeVariable.name}";
  }

  visitCreateFunction(cps_ir.CreateFunction node) {
    return "CreateFunction ${node.definition.element.name}";
  }

  visitGetMutableVariable(cps_ir.GetMutableVariable node) {
    String variable = names.name(node.variable.definition);
    return '${node.runtimeType} $variable';
  }

  visitRunnableBody(cps_ir.RunnableBody node) {}
  visitFieldInitializer(cps_ir.FieldInitializer node) {}
  visitSuperInitializer(cps_ir.SuperInitializer node) {}
  visitCondition(cps_ir.Condition c) {}
  visitExpression(cps_ir.Expression e) {}
  visitPrimitive(cps_ir.Primitive p) {}
  visitDefinition(cps_ir.Definition d) {}
  visitInitializer(cps_ir.Initializer i) {}
  visitNode(cps_ir.Node n) {}
}

/**
 * Invents (and remembers) names for Continuations, Parameters, etc.
 * The names must match the conventions used by IR Hydra, e.g.
 * Continuations and Functions must have names of form B### since they
 * are visualized as basic blocks.
 */
class Names {
  final Map<Object, String> names = {};
  final Map<String, int> counters = {
    'r': 0,
    'B': 0,
    'v': 0,
    'x': 0,
    'c': 0
  };

  String prefix(x) {
    if (x is cps_ir.Parameter) return 'r';
    if (x is cps_ir.Continuation || x is cps_ir.FunctionDefinition) return 'B';
    if (x is cps_ir.Primitive) return 'v';
    if (x is cps_ir.MutableVariable) return 'c';
    return 'x';
  }

  String name(x) {
    String nam = names[x];
    if (nam == null) {
      String pref = prefix(x);
      int id = counters[pref]++;
      nam = names[x] = '${pref}${id}';
    }
    return nam;
  }
}

/**
 * A vertex in the graph visualization, used in place of basic blocks.
 */
class Block {
  String name;
  final List<cps_ir.Parameter> parameters;
  final cps_ir.Expression body;
  final List<Block> succ = <Block>[];
  final List<Block> pred = <Block>[];

  Block(this.name, this.parameters, this.body);

  void addEdgeTo(Block successor) {
    succ.add(successor);
    successor.pred.add(this);
  }
}

class BlockCollector extends cps_ir.Visitor {
  final Map<cps_ir.Continuation, Block> cont2block =
      <cps_ir.Continuation, Block>{};
  final Set<Block> entries = new Set<Block>();
  Block current_block;

  Names names;
  BlockCollector(this.names);

  Block getBlock(cps_ir.Continuation c) {
    Block block = cont2block[c];
    if (block == null) {
      block = new Block(names.name(c), c.parameters, c.body);
      cont2block[c] = block;
    }
    return block;
  }

  visitRunnableBody(cps_ir.RunnableBody node) {
    current_block = new Block(names.name(node), [], node.body);
    entries.add(current_block);
    visit(node.body);
  }

  visitFieldInitializer(cps_ir.FieldInitializer node) {
    visit(node.body);
  }

  visitSuperInitializer(cps_ir.SuperInitializer node) {
    node.arguments.forEach(visit);
  }

  visitExecutableDefinition(cps_ir.ExecutableDefinition node) {
    visit(node.body);
  }

  visitFieldDefinition(cps_ir.FieldDefinition node) {
    if (node.hasInitializer) {
      visitExecutableDefinition(node);
    }
  }

  visitFunctionDefinition(cps_ir.FunctionDefinition node) {
    visitExecutableDefinition(node);
  }

  visitConstructorDefinition(cps_ir.ConstructorDefinition node) {
    visitExecutableDefinition(node);
  }

  visitLetPrim(cps_ir.LetPrim exp) {
    visit(exp.body);
  }

  visitLetCont(cps_ir.LetCont exp) {
    exp.continuations.forEach(visit);
    visit(exp.body);
  }

  void addEdgeToContinuation(cps_ir.Reference continuation) {
    cps_ir.Definition target = continuation.definition;
    if (target is cps_ir.Continuation && !target.isReturnContinuation) {
      current_block.addEdgeTo(getBlock(target));
    }
  }

  visitInvokeStatic(cps_ir.InvokeStatic exp) {
    addEdgeToContinuation(exp.continuation);
  }

  visitInvokeMethod(cps_ir.InvokeMethod exp) {
    addEdgeToContinuation(exp.continuation);
  }

  visitInvokeConstructor(cps_ir.InvokeConstructor exp) {
    addEdgeToContinuation(exp.continuation);
  }

  visitConcatenateStrings(cps_ir.ConcatenateStrings exp) {
    addEdgeToContinuation(exp.continuation);
  }

  visitInvokeContinuation(cps_ir.InvokeContinuation exp) {
    addEdgeToContinuation(exp.continuation);
  }

  visitSetMutableVariable(cps_ir.SetMutableVariable exp) {
    visit(exp.body);
  }

  visitLetMutable(cps_ir.LetMutable exp) {
    visit(exp.body);
  }

  visitSetField(cps_ir.SetField exp) {
    visit(exp.body);
  }

  visitDeclareFunction(cps_ir.DeclareFunction exp) {
    visit(exp.body);
  }

  visitBranch(cps_ir.Branch exp) {
    cps_ir.Continuation trueTarget = exp.trueContinuation.definition;
    if (!trueTarget.isReturnContinuation) {
      current_block.addEdgeTo(getBlock(trueTarget));
    }
    cps_ir.Continuation falseTarget = exp.falseContinuation.definition;
    if (!falseTarget.isReturnContinuation) {
      current_block.addEdgeTo(getBlock(falseTarget));
    }
  }

  visitContinuation(cps_ir.Continuation c) {
    var old_node = current_block;
    current_block = getBlock(c);
    visit(c.body);
    current_block = old_node;
  }
}
