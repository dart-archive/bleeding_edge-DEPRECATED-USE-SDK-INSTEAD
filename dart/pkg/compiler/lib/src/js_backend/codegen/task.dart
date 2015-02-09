// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/// Generate code using the cps-based IR pipeline.
library code_generator_task;

import 'glue.dart';
import 'codegen.dart';
import 'unsugar.dart';

import '../js_backend.dart';
import '../../dart2jslib.dart';
import '../../cps_ir/cps_ir_nodes.dart' as cps;
import '../../cps_ir/cps_ir_builder.dart';
import '../../tree_ir/tree_ir_nodes.dart' as tree_ir;
import '../../types/types.dart' show TypeMask, UnionTypeMask, FlatTypeMask,
    ForwardingTypeMask;
import '../../elements/elements.dart';
import '../../js/js.dart' as js;
import '../../io/source_information.dart' show StartEndSourceInformation;
import '../../tree_ir/tree_ir_builder.dart' as tree_builder;
import '../../dart_backend/backend_ast_emitter.dart' as backend_ast_emitter;
import '../../cps_ir/optimizers.dart';
import '../../tracer.dart';
import '../../js_backend/codegen/codegen.dart';
import '../../ssa/ssa.dart' as ssa;
import '../../tree_ir/optimization/optimization.dart';
import '../../cps_ir/cps_ir_nodes_sexpr.dart';
import 'js_tree_builder.dart';

class CpsFunctionCompiler implements FunctionCompiler {
  final IrBuilderTask irBuilderTask;
  final ConstantSystem constantSystem;
  final Compiler compiler;
  final Glue glue;

  TypeSystem types;

  // TODO(karlklose,sigurm): remove and update dart-doc of [compile].
  final FunctionCompiler fallbackCompiler;

  Tracer get tracer => compiler.tracer;

  CpsFunctionCompiler(Compiler compiler, JavaScriptBackend backend)
      : irBuilderTask = new IrBuilderTask(compiler),
        fallbackCompiler = new ssa.SsaFunctionCompiler(backend, true),
        constantSystem = backend.constantSystem,
        compiler = compiler,
        glue = new Glue(compiler);

  String get name => 'CPS Ir pipeline';

  /// Generates JavaScript code for `work.element`. First tries to use the
  /// Cps Ir -> tree ir -> js pipeline, and if that fails due to language
  /// features not implemented it will fall back to the ssa pipeline (for
  /// platform code) or will cancel compilation (for user code).
  js.Fun compile(CodegenWorkItem work) {
    types = new TypeMaskSystem(compiler);
    AstElement element = work.element;
    JavaScriptBackend backend = compiler.backend;
    return compiler.withCurrentElement(element, () {
      if (element.library.isPlatformLibrary ||
          element.library == backend.interceptorsLibrary) {
        compiler.log('Using SSA compiler for platform element $element');
        return fallbackCompiler.compile(work);
      }
      try {
        if (tracer != null) {
          tracer.traceCompilation(element.name, null);
        }
        cps.FunctionDefinition cpsFunction = compileToCpsIR(element);
        cpsFunction = optimizeCpsIR(cpsFunction);
        tree_ir.FunctionDefinition treeFunction = compileToTreeIR(cpsFunction);
        treeFunction = optimizeTreeIR(treeFunction);
        return compileToJavaScript(work, treeFunction);
      } on CodegenBailout catch (e) {
        String message = "Unable to compile $element with the new compiler.\n"
            "  Reason: ${e.message}";
        compiler.internalError(element, message);
      }
    });
  }

  void giveUp(String reason) {
    throw new CodegenBailout(null, reason);
  }

  void traceGraph(String title, var irObject) {
    if (tracer != null) {
      tracer.traceGraph(title, irObject);
    }
  }

  cps.FunctionDefinition compileToCpsIR(AstElement element) {
    // TODO(sigurdm): Support these constructs.
    if (element.isNative ||
        element.isField) {
      giveUp('unsupported element kind: ${element.name}:${element.kind}');
    }

    cps.FunctionDefinition cpsNode = irBuilderTask.buildNode(element);
    if (cpsNode == null) {
      giveUp('unable to build cps definition of $element');
    }
    if (element.isInstanceMember && !element.isGenerativeConstructorBody) {
      Selector selector = new Selector.fromElement(cpsNode.element);
      if (glue.isInterceptedSelector(selector)) {
        giveUp('cannot compile methods that need interceptor calling '
            'convention.');
      }
    }
    traceGraph("IR Builder", cpsNode);
    new UnsugarVisitor(glue).rewrite(cpsNode);
    traceGraph("Unsugaring", cpsNode);
    return cpsNode;
  }

  static const Pattern PRINT_TYPED_IR_FILTER = null;

  String formatTypeMask(TypeMask type) {
    if (type is UnionTypeMask) {
      return '[${type.disjointMasks.map(formatTypeMask).join(', ')}]';
    } else if (type is FlatTypeMask) {
      if (type.isEmpty) {
        return "null";
      }
      String suffix = (type.isExact ? "" : "+") + (type.isNullable ? "?" : "!");
      return '${type.base.name}$suffix';
    } else if (type is ForwardingTypeMask) {
      return formatTypeMask(type.forwardTo);
    }
    throw 'unsupported: $type';
  }

  cps.FunctionDefinition optimizeCpsIR(cps.FunctionDefinition cpsNode) {
    // Transformations on the CPS IR.

    TypePropagator typePropagator = new TypePropagator<TypeMask>(
        compiler.types,
        constantSystem,
        new TypeMaskSystem(compiler),
        compiler.internalError);
    typePropagator.rewrite(cpsNode);
    traceGraph("Sparse constant propagation", cpsNode);

    if (PRINT_TYPED_IR_FILTER != null &&
        PRINT_TYPED_IR_FILTER.matchAsPrefix(cpsNode.element.name) != null) {
      String printType(cps.Node node, String s) {
        var type = typePropagator.getType(node);
        return type == null ? s : "$s:${formatTypeMask(type.type)}";
      }
      DEBUG_MODE = true;
      print(new SExpressionStringifier(printType).visit(cpsNode));
    }

    new RedundantPhiEliminator().rewrite(cpsNode);
    traceGraph("Redundant phi elimination", cpsNode);
    new ShrinkingReducer().rewrite(cpsNode);
    traceGraph("Shrinking reductions", cpsNode);

    // Do not rewrite the IR after variable allocation.  Allocation
    // makes decisions based on an approximation of IR variable live
    // ranges that can be invalidated by transforming the IR.
    new cps.RegisterAllocator().visit(cpsNode);
    return cpsNode;
  }

  tree_ir.FunctionDefinition compileToTreeIR(cps.FunctionDefinition cpsNode) {
    tree_builder.Builder builder = new JsTreeBuilder(
        compiler.internalError, compiler.identicalFunction, glue);
    tree_ir.FunctionDefinition treeNode = builder.buildFunction(cpsNode);
    assert(treeNode != null);
    traceGraph('Tree builder', treeNode);
    return treeNode;
  }

  tree_ir.FunctionDefinition optimizeTreeIR(
      tree_ir.FunctionDefinition treeNode) {
    // Transformations on the Tree IR.
    new StatementRewriter().rewrite(treeNode);
    traceGraph('Statement rewriter', treeNode);
    new CopyPropagator().rewrite(treeNode);
    traceGraph('Copy propagation', treeNode);
    new LoopRewriter().rewrite(treeNode);
    traceGraph('Loop rewriter', treeNode);
    new LogicalRewriter().rewrite(treeNode);
    traceGraph('Logical rewriter', treeNode);
    new backend_ast_emitter.UnshadowParameters().unshadow(treeNode);
    traceGraph('Unshadow parameters', treeNode);
    return treeNode;
  }

  js.Fun compileToJavaScript(CodegenWorkItem work,
                             tree_ir.FunctionDefinition definition) {
    CodeGenerator codeGen = new CodeGenerator(glue, work.registry);

    return attachPosition(codeGen.buildFunction(definition), work.element);
  }

  Iterable<CompilerTask> get tasks {
    // TODO(sigurdm): Make a better list of tasks.
    return <CompilerTask>[irBuilderTask]..addAll(fallbackCompiler.tasks);
  }

  js.Node attachPosition(js.Node node, AstElement element) {
    return node.withSourceInformation(
        StartEndSourceInformation.computeSourceInformation(element));
  }
}
