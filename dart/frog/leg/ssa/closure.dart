// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ClosureData {
  // The globalizedClosureElement will be null for methods that are not local
  // closures.
  final ClassElement globalizedClosureElement;
  // The callElement will be null for methods that are not local closures.
  final FunctionElement callElement;

  ClosureData(this.globalizedClosureElement, this.callElement);
}

Map<Node, ClosureData> _closureDataCache;
Map<Node, ClosureData> get closureDataCache() {
  if (_closureDataCache === null) {
    _closureDataCache = new HashMap<Node, ClosureData>();
  }
  return _closureDataCache;
}

class ClosureTranslator extends AbstractVisitor {
  final Compiler compiler;
  final TreeElements elements;

  FunctionElement currentFunction;
  // The closureData of the currentFunction.
  ClosureData closureData;

  bool insideClosure = null;

  ClosureTranslator(this.compiler, this.elements);

  ClosureData translate(Node node) {
    // Closures have already been analyzed when visiting the surrounding
    // method/function. This also shortcuts for bailout functions.
    ClosureData cached = closureDataCache[node];
    if (cached !== null) return cached;

    visit(node);

    return closureDataCache[node];
  }

  void useLocal(Element element) {
    if (element.enclosingElement != currentFunction) {
      compiler.unimplemented("ClosureTranslator.useLocal captured variable",
                             node: element.parseNode(compiler));
    }
  }

  void declareLocal(Element element) {
    // TODO(floitsch): implement.
  }

  visit(Node node) => node.accept(this);

  visitNode(Node node) => node.visitChildren(this);

  visitVariableDefinitions(VariableDefinitions node) {
    for (Link<Node> link = node.definitions.nodes;
         !link.isEmpty();
         link = link.tail) {
      Node definition = link.head;
      Element element = elements[definition];
      assert(element !== null);
      declareLocal(element);
    }
    // We still need to visit the right-hand sides of the init-assignments.
    // Simply visit all children. We will visit the locals again and make them
    // used, but that should not be a problem.
    node.visitChildren(this);
  }

  visitIdentifier(Identifier node) {
    if (node.isThis() && insideClosure) {
      // TODO(floitsch): handle 'this'.
      compiler.unimplemented("ClosureTranslator.visitIdentifier this-capture",
                             node: node);
    }
    node.visitChildren(this);
  }

  visitSend(Send node) {
    Element element = elements[node];
    if (element !== null &&
        (element.kind == ElementKind.VARIABLE ||
         element.kind == ElementKind.PARAMETER)) {
      useLocal(element);
    } else if (node.receiver === null) {
      if (insideClosure) {
        compiler.unimplemented("ClosureTranslator.visitSend this-capture");
      }
    }
    node.visitChildren(this);    
  }

  ClosureData globalizeClosure(FunctionExpression node) {
    FunctionElement element = elements[node];
    SourceString name = const SourceString("Closure");
    CompilationUnitElement compilationUnit =
        element.getEnclosingCompilationUnit();
    ClassElement globalizedElement = new ClassElement(name, compilationUnit);
    FunctionElement callElement =
        new FunctionElement.from(Namer.CLOSURE_INVOCATION_NAME,
                                 element,
                                 globalizedElement);
    globalizedElement.backendMembers =
        const EmptyLink<Element>().prepend(callElement);
    globalizedElement.isResolved = true;
    return new ClosureData(globalizedElement, callElement);
  }

  visitFunctionExpression(FunctionExpression node) {
    FunctionElement oldFunction = currentFunction;
    ClosureData oldClosureData = closureData;
    bool oldInsideClosure = insideClosure;

    currentFunction = elements[node];
    if (oldInsideClosure !== null) {
      insideClosure = true;
      // A nested closure.
      closureData = globalizeClosure(node);
    } else {
      insideClosure = false;
      closureData = new ClosureData(null, null);
    }
    closureDataCache[node] = closureData;

    node.visitChildren(this);

    // Restore old values.
    insideClosure = oldInsideClosure;
    closureData = oldClosureData;
    currentFunction = oldFunction;

    // If we just visited a closure we declare it. This is not always correct
    // since some closures are used as expressions and don't introduce any
    // name. But in this case the added local is simply not used.
    if (insideClosure !== null) {
      declareLocal(elements[node]);
    }
  }
}
