// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of dart2js.cps_ir.optimizers;

/**
 * [ShrinkingReducer] applies shrinking reductions to CPS terms as described
 * in 'Compiling with Continuations, Continued' by Andrew Kennedy.
 */
class ShrinkingReducer extends PassMixin {
  Set<_ReductionTask> _worklist;

  static final _DeletedNode _DELETED = new _DeletedNode();

  /// Applies shrinking reductions to root, mutating root in the process.
  @override
  void rewriteExecutableDefinition(ExecutableDefinition root) {
    _worklist = new Set<_ReductionTask>();
    _RedexVisitor redexVisitor = new _RedexVisitor(_worklist);

    // Set all parent pointers.
    new ParentVisitor().visit(root);

    // Sweep over the term, collecting redexes into the worklist.
    redexVisitor.visit(root);

    // Process the worklist.
    while (_worklist.isNotEmpty) {
      _ReductionTask task = _worklist.first;
      _worklist.remove(task);
      _processTask(task);
    }
  }

  /// Removes the given node from the CPS graph, replacing it with its body
  /// and marking it as deleted. The node's parent must be a [[InteriorNode]].
  void _removeNode(InteriorNode node) {
    Node body           = node.body;
    InteriorNode parent = node.parent;
    assert(parent.body == node);

    body.parent = parent;
    parent.body = body;
    node.parent = _DELETED;
  }

  /// Remove a given continuation from the CPS graph.  The LetCont itself is
  /// removed if the given continuation is the only binding.
  void _removeContinuation(Continuation cont) {
    LetCont parent = cont.parent;
    if (parent.continuations.length == 1) {
      assert(cont.parent_index == 0);
      _removeNode(parent);
    } else {
      List<Continuation> continuations = parent.continuations;
      for (int i = cont.parent_index; i < continuations.length - 1; ++i) {
        Continuation current = continuations[i + 1];
        continuations[i] = current;
        current.parent_index = i;
      }
      continuations.removeLast();
    }
    cont.parent = _DELETED;
  }

  void _processTask(_ReductionTask task) {
    // Skip tasks for deleted nodes.
    if (task.node.parent == _DELETED) {
      return;
    }

    switch (task.kind) {
      case _ReductionKind.DEAD_VAL:
        _reduceDeadVal(task);
        break;
      case _ReductionKind.DEAD_CONT:
        _reduceDeadCont(task);
        break;
      case _ReductionKind.BETA_CONT_LIN:
        _reduceBetaContLin(task);
        break;
      case _ReductionKind.ETA_CONT:
        _reduceEtaCont(task);
        break;
      case _ReductionKind.DEAD_PARAMETER:
        _reduceDeadParameter(task);
        break;
      default:
        assert(false);
    }
  }

  /// Applies the dead-val reduction:
  ///   letprim x = V in E -> E (x not free in E).
  void _reduceDeadVal(_ReductionTask task) {
    assert(_isDeadVal(task.node));

    // Remove dead primitive.
    LetPrim letPrim = task.node;;
    _removeNode(letPrim);

    // Perform bookkeeping on removed body and scan for new redexes.
    new _RemovalVisitor(_worklist).visit(letPrim.primitive);
  }

  /// Applies the dead-cont reduction:
  ///   letcont k x = E0 in E1 -> E1 (k not free in E1).
  void _reduceDeadCont(_ReductionTask task) {
    assert(_isDeadCont(task.node));

    // Remove dead continuation.
    Continuation cont = task.node;
    _removeContinuation(cont);

    // Perform bookkeeping on removed body and scan for new redexes.
    new _RemovalVisitor(_worklist).visit(cont);
  }

  /// Applies the beta-cont-lin reduction:
  ///   letcont k x = E0 in E1[k y] -> E1[E0[y/x]] (k not free in E1).
  void _reduceBetaContLin(_ReductionTask task) {
    // Might have been mutated, recheck if reduction is still valid.
    // In the following example, the beta-cont-lin reduction of k0 could have
    // been invalidated by removal of the dead continuation k1:
    //
    //  letcont k0 x0 = E0 in
    //    letcont k1 x1 = k0 x1 in
    //      return x2
    if (!_isBetaContLin(task.node)) {
      return;
    }

    // Remove the continuation.
    Continuation cont = task.node;
    _removeContinuation(cont);

    // Replace its invocation with the continuation body.
    InvokeContinuation invoke = cont.firstRef.parent;
    InteriorNode invokeParent = invoke.parent;

    cont.body.parent = invokeParent;
    invokeParent.body = cont.body;

    // Substitute the invocation argument for the continuation parameter.
    for (int i = 0; i < invoke.arguments.length; i++) {
      Reference argRef = invoke.arguments[i];
      argRef.definition.substituteFor(cont.parameters[i]);
    }

    // Perform bookkeeping on substituted body and scan for new redexes.
    new _RemovalVisitor(_worklist).visit(invoke);
  }

  /// Applies the eta-cont reduction:
  ///   letcont k x = j x in E -> E[j/k].
  /// If k is unused, degenerates to dead-cont.
  void _reduceEtaCont(_ReductionTask task) {
    // Might have been mutated, recheck if reduction is still valid.
    // In the following example, the eta-cont reduction of k1 could have been
    // invalidated by an earlier beta-cont-lin reduction of k0.
    //
    //  letcont k0 x0 = E0 in
    //    letcont k1 x1 = k0 x1 in E1
    if (!_isEtaCont(task.node)) {
      return;
    }

    // Remove the continuation.
    Continuation cont = task.node;
    _removeContinuation(cont);

    InvokeContinuation invoke = cont.body;
    Continuation wrappedCont = invoke.continuation.definition;

    // Replace all occurrences with the wrapped continuation.
    wrappedCont.substituteFor(cont);

    // Perform bookkeeping on removed body and scan for new redexes.
    new _RemovalVisitor(_worklist).visit(cont);
  }

  void _reduceDeadParameter(_ReductionTask task) {
    assert(_isDeadParameter(task.node));

    Parameter parameter = task.node;
    Continuation continuation = parameter.parent;
    int index = parameter.parent_index;

    // Remove the index'th argument from each invocation.
    Reference<Continuation> current = continuation.firstRef;
    while (current != null) {
      InvokeContinuation invoke = current.parent;
      Reference<Primitive> argument = invoke.arguments[index];
      argument.unlink();
      // Removing an argument can create a dead parameter or dead value redex.
      if (argument.definition is Parameter) {
        if (_isDeadParameter(argument.definition)) {
          _worklist.add(new _ReductionTask(_ReductionKind.DEAD_PARAMETER,
                                           argument.definition));
        }
      } else {
        Node parent = argument.definition.parent;
        if (parent is LetPrim) {
          if (_isDeadVal(parent)) {
            _worklist.add(new _ReductionTask(_ReductionKind.DEAD_VAL, parent));
          }
        }
      }
      invoke.arguments.removeAt(index);
      current = current.next;
    }
    // Copy the parameters above index down.
    List<Parameter> parameters = continuation.parameters;
    for (int i = index; i < parameters.length - 1; ++i) {
      Parameter p = parameters[i + 1];
      parameters[i] = p;
      p.parent_index = i;
    }
    parameters.removeLast();

    // Removing an unused parameter can create an eta-redex.
    if (_isEtaCont(continuation)) {
      _worklist.add(new _ReductionTask(_ReductionKind.ETA_CONT, continuation));
    }
  }
}

/// Returns true iff the bound primitive is unused.
bool _isDeadVal(LetPrim node) => !node.primitive.hasAtLeastOneUse;

/// Returns true iff the continuation is unused.
bool _isDeadCont(Continuation cont) {
  return !cont.isReturnContinuation && !cont.hasAtLeastOneUse;
}

/// Returns true iff the continuation has a body (i.e., it is not the return
/// continuation), it is used exactly once, and that use is as the continuation
/// of a continuation invocation.
bool _isBetaContLin(Continuation cont) {
  // There is a restriction on continuation eta-redexes that the body is not an
  // invocation of the return continuation, because that leads to worse code
  // when translating back to direct style (it duplicates returns).  There is no
  // such restriction here because continuation beta-reduction is only performed
  // for singly referenced continuations. Thus, there is no possibility of code
  // duplication.
  if (cont.isReturnContinuation || !cont.hasExactlyOneUse) {
    return false;
  }

  if (cont.firstRef.parent is InvokeContinuation) {
    InvokeContinuation invoke = cont.firstRef.parent;
    return (cont == invoke.continuation.definition);
  }

  return false;
}

/// Returns true iff the continuation consists of a continuation
/// invocation, passing on all parameters. Special cases exist (see below).
bool _isEtaCont(Continuation cont) {
  if (cont.isReturnContinuation || cont.body is! InvokeContinuation) {
    return false;
  }

  InvokeContinuation invoke = cont.body;
  Continuation invokedCont = invoke.continuation.definition;

  // Do not eta-reduce return join-points since the direct-style code is worse
  // in the common case (i.e. returns are moved inside `if` branches).
  if (invokedCont.isReturnContinuation) {
    return false;
  }

  // Translation to direct style generates different statements for recursive
  // and non-recursive invokes. It should still be possible to apply eta-cont if
  // this is not a self-invocation.
  //
  // TODO(kmillikin): Remove this restriction if it makes sense to do so.
  if (invoke.isRecursive) {
    return false;
  }

  // If cont has more parameters than the invocation has arguments, the extra
  // parameters will be dead and dead-parameter will eventually create the
  // eta-redex if possible.
  //
  // If the invocation's arguments are simply a permutation of cont's
  // parameters, then there is likewise a possible reduction that involves
  // rewriting the invocations of cont.  We are missing that reduction here.
  //
  // If cont has fewer parameters than the invocation has arguments then a
  // reduction would still possible, since the extra invocation arguments must
  // be in scope at all the invocations of cont.  For example:
  //
  // let cont k1(x1) = k0(x0, x1) in E -eta-> E'
  // where E' has k0(x0, v) substituted for each k1(v).
  //
  // HOWEVER, adding continuation parameters is unlikely to be an optimization
  // since it duplicates assignments used in direct-style to implement parameter
  // passing.
  //
  // TODO(kmillikin): find real occurrences of these patterns, and see if they
  // can be optimized.
  if (cont.parameters.length != invoke.arguments.length) {
    return false;
  }

  // TODO(jgruber): Linear in the parameter count. Can be improved to near
  // constant time by using union-find data structure.
  for (int i = 0; i < cont.parameters.length; i++) {
    if (invoke.arguments[i].definition != cont.parameters[i]) {
      return false;
    }
  }

  return true;
}

bool _isDeadParameter(Parameter parameter) {
  // We cannot remove function parameters as an intraprocedural optimization.
  if (parameter.parent is! Continuation || parameter.hasAtLeastOneUse) {
    return false;
  }

  // We cannot remove the parameter to a call continuation, because the
  // resulting expression will not be well-formed (call continuations have
  // exactly one argument).  The return continuation is a call continuation, so
  // we cannot remove its dummy parameter.
  Continuation continuation = parameter.parent;
  if (continuation.isReturnContinuation) return false;
  Reference<Continuation> current = continuation.firstRef;
  while (current != null) {
    if (current.parent is! InvokeContinuation) return false;
    InvokeContinuation invoke = current.parent;
    if (invoke.continuation.definition != continuation) return false;
    current = current.next;
  }
  return true;
}

/// Traverses a term and adds any found redexes to the worklist.
class _RedexVisitor extends RecursiveVisitor {
  final Set<_ReductionTask> worklist;

  _RedexVisitor(this.worklist);

  void processLetPrim(LetPrim node) {
    if (_isDeadVal(node)) {
      worklist.add(new _ReductionTask(_ReductionKind.DEAD_VAL, node));
    }
  }

  void processContinuation(Continuation node) {
    // Continuation beta- and eta-redexes can overlap, namely when an eta-redex
    // is invoked exactly once.  We prioritize continuation beta-redexes over
    // eta-redexes because some reductions (e.g., dead parameter elimination)
    // can destroy a continuation eta-redex.  If we prioritized eta- over
    // beta-redexes, this would implicitly "create" the corresponding beta-redex
    // (in the sense that it would still apply) and the algorithm would not
    // detect it.
    if (_isDeadCont(node)) {
      worklist.add(new _ReductionTask(_ReductionKind.DEAD_CONT, node));
    } else if (_isBetaContLin(node)){
      worklist.add(new _ReductionTask(_ReductionKind.BETA_CONT_LIN, node));
    } else if (_isEtaCont(node)) {
      worklist.add(new _ReductionTask(_ReductionKind.ETA_CONT, node));
    }
  }

  void processParameter(Parameter node) {
    if (_isDeadParameter(node)) {
      worklist.add(new _ReductionTask(_ReductionKind.DEAD_PARAMETER, node));
    }
  }
}

/// Traverses a deleted CPS term, marking nodes that might participate in a
/// redex as deleted and adding newly created redexes to the worklist.
///
/// Deleted nodes that might participate in a reduction task are marked so that
/// any corresponding tasks can be skipped.  Nodes are marked so by setting
/// their parent to the deleted sentinel.
class _RemovalVisitor extends RecursiveVisitor {
  final Set<_ReductionTask> worklist;

  _RemovalVisitor(this.worklist);

  void processLetPrim(LetPrim node) {
    node.parent = ShrinkingReducer._DELETED;
  }

  void processContinuation(Continuation node) {
    node.parent = ShrinkingReducer._DELETED;
  }

  void processReference(Reference reference) {
    reference.unlink();

    if (reference.definition is Primitive) {
      Primitive primitive = reference.definition;
      Node parent = primitive.parent;
      // The parent might be the deleted sentinel, or it might be a
      // Continuation or FunctionDefinition if the primitive is an argument.
      if (parent is LetPrim && _isDeadVal(parent)) {
        worklist.add(new _ReductionTask(_ReductionKind.DEAD_VAL, parent));
      }
    } else if (reference.definition is Continuation) {
      Continuation cont = reference.definition;
      Node parent = cont.parent;
      // The parent might be the deleted sentinel, or it might be a
      // RunnableBody if the continuation is the return continuation.
      if (parent is LetCont) {
        if (cont.isRecursive && cont.hasAtMostOneUse) {
          // Convert recursive to nonrecursive continuations.  If the
          // continuation is still in use, it is either dead and will be
          // removed, or it is called nonrecursively outside its body.
          cont.isRecursive = false;
        }
        if (_isDeadCont(cont)) {
          worklist.add(new _ReductionTask(_ReductionKind.DEAD_CONT, cont));
        } else if (_isBetaContLin(cont)) {
          worklist.add(new _ReductionTask(_ReductionKind.BETA_CONT_LIN, cont));
        }
      }
    }
  }
}

/// Traverses the CPS term and sets node.parent for each visited node.
class ParentVisitor extends RecursiveVisitor {
  processFunctionDefinition(FunctionDefinition node) {
    node.body.parent = node;
    int index = 0;
    node.parameters.forEach((Parameter parameter) {
      parameter.parent = node;
      parameter.parent_index = index++;
    });
  }

  processRunnableBody(RunnableBody node) {
    node.returnContinuation.parent = node;
    node.body.parent = node;
  }

  processConstructorDefinition(ConstructorDefinition node) {
    node.body.parent = node;
    int index = 0;
    node.parameters.forEach((Parameter parameter) {
      parameter.parent = node;
      parameter.parent_index = index++;
    });
    node.initializers.forEach((Initializer i) => i.parent = node);
  }

  // Expressions.

  processFieldInitializer(FieldInitializer node) {
    node.body.body.parent = node;
  }

  processSuperInitializer(SuperInitializer node) {
    node.arguments.forEach(
        (RunnableBody argument) => argument.body.parent = node);
  }

  processLetPrim(LetPrim node) {
    node.primitive.parent = node;
    node.body.parent = node;
  }

  processLetCont(LetCont node) {
    int index = 0;
    node.continuations.forEach((Continuation continuation) {
      continuation.parent = node;
      continuation.parent_index = index++;
    });
    node.body.parent = node;
  }

  processInvokeStatic(InvokeStatic node) {
    node.arguments.forEach((Reference ref) => ref.parent = node);
    node.continuation.parent = node;
  }

  processInvokeContinuation(InvokeContinuation node) {
    node.continuation.parent = node;
    node.arguments.forEach((Reference ref) => ref.parent = node);
  }

  processInvokeMethod(InvokeMethod node) {
    node.receiver.parent = node;
    node.continuation.parent = node;
    node.arguments.forEach((Reference ref) => ref.parent = node);
  }

  processInvokeMethodDirectly(InvokeMethodDirectly node) {
    node.receiver.parent = node;
    node.continuation.parent = node;
    node.arguments.forEach((Reference ref) => ref.parent = node);
  }

  processInvokeConstructor(InvokeConstructor node) {
    node.continuation.parent = node;
    node.arguments.forEach((Reference ref) => ref.parent = node);
  }

  processConcatenateStrings(ConcatenateStrings node) {
    node.continuation.parent = node;
    node.arguments.forEach((Reference ref) => ref.parent = node);
  }

  processBranch(Branch node) {
    node.condition.parent = node;
    node.trueContinuation.parent = node;
    node.falseContinuation.parent = node;
  }

  processTypeOperator(TypeOperator node) {
    node.continuation.parent = node;
    node.receiver.parent = node;
  }

  processSetClosureVariable(SetClosureVariable node) {
    node.body.parent = node;
    node.value.parent = node;
  }

  processDeclareFunction(DeclareFunction node) {
    node.definition.parent = node;
    node.body.parent = node;
  }

  // Definitions.

  processLiteralList(LiteralList node) {
    node.values.forEach((Reference ref) => ref.parent = node);
  }

  processLiteralMap(LiteralMap node) {
    node.entries.forEach((LiteralMapEntry entry) {
      entry.key.parent = node;
      entry.value.parent = node;
    });
  }

  processCreateFunction(CreateFunction node) {
    node.definition.parent = node;
  }

  processContinuation(Continuation node) {
    if (node.body != null) node.body.parent = node;
    int index = 0;
    node.parameters.forEach((Parameter parameter) {
      parameter.parent = node;
      parameter.parent_index = index++;
    });
  }

  // Conditions.

  processIsTrue(IsTrue node) {
    node.value.parent = node;
  }

  // JavaScript specific nodes.

  processIdentical(Identical node) {
    node.left.parent = node;
    node.right.parent = node;
  }

  processInterceptor(Interceptor node) {
    node.input.parent = node;
  }

  processSetField(SetField node) {
    node.object.parent = node;
    node.value.parent = node;
    node.body.parent = node;
  }

  processGetField(GetField node) {
    node.object.parent = node;
  }

  processCreateInstance(CreateInstance node) {
    node.arguments.forEach((Reference ref) => ref.parent = node);
  }

  processCreateBox(CreateBox node) {
  }
}

class _ReductionKind {
  final String name;
  final int hashCode;

  const _ReductionKind(this.name, this.hashCode);

  static const _ReductionKind DEAD_VAL = const _ReductionKind('dead-val', 0);
  static const _ReductionKind DEAD_CONT = const _ReductionKind('dead-cont', 1);
  static const _ReductionKind BETA_CONT_LIN =
      const _ReductionKind('beta-cont-lin', 2);
  static const _ReductionKind ETA_CONT = const _ReductionKind('eta-cont', 3);
  static const _ReductionKind DEAD_PARAMETER =
      const _ReductionKind('dead-parameter', 4);

  String toString() => name;
}

/// Represents a reduction task on the worklist. Implements both hashCode and
/// operator== since instantiations are used as Set elements.
class _ReductionTask {
  final _ReductionKind kind;
  final Node node;

  int get hashCode {
    assert(kind.hashCode < (1 << 3));
    return (node.hashCode << 3) | kind.hashCode;
  }

  _ReductionTask(this.kind, this.node) {
    assert(node is Continuation || node is LetPrim || node is Parameter);
  }

  bool operator==(_ReductionTask that) {
    return (that.kind == this.kind && that.node == this.node);
  }

  String toString() => "$kind: $node";
}

/// A dummy class used solely to mark nodes as deleted once they are removed
/// from a term.
class _DeletedNode extends Node {
  accept(_) => null;
}
