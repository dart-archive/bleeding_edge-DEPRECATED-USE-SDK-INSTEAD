// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ResolverTask extends CompilerTask {
  ResolverTask(Compiler compiler) : super(compiler);
  String get name() => 'Resolver';

  Map<Node, Element> resolve(Node tree) {
    return measure(() {
      ResolverVisitor visitor = new ResolverVisitor(compiler);
      visitor.visit(tree);
      return visitor.mapping;
    });
  }
}

class ResolverVisitor implements Visitor<Element> {
  final Compiler compiler;
  final Map<Node, Element> mapping;

  Context context;

  ResolverVisitor(Compiler compiler)
    : context = new Context(null, compiler.universe.scope,
                            compiler.universe.elements),
      mapping = new Map<Node, Element>(),
      this.compiler = compiler;

  fail(Node node) {
    compiler.cancel('cannot resolve ${node}');
  }

  visit(Node node) {
    Element element = node.accept(this);
    if (element !== null) {
      mapping[node] = element;
    }
    return element;
  }

  visitIn(Node node, Context ctx) {
    Context parent = context;
    context = ctx;
    Element element = visit(node);
    context = parent;
  }

  visitBlock(Block node) {
    visit(node.statements);
  }

  visitExpressionStatement(ExpressionStatement node) {
    visit(node.expression);
  }

  visitFunctionExpression(FunctionExpression node) {
    var parameterElements = {};
    for (var link = node.parameters.nodes; !link.isEmpty();
         link = link.tail) {
      var parameter = link.head;
      Element parameterElement = visit(parameter);
      parameterElements[parameter.name] = parameterElement;
    }
    var element = null;
    Identifier name = node.name;
    if (name !== null) {
      element = context.lookup(name.source);
    }
    visitIn(node.body, new Context(context, element, parameterElements));
    return element;
  }

  visitIdentifier(Identifier node) {
    var element = context.lookup(node.source);
    if (element == null) fail(node);
    return element;
  }

  visitIf(If node) {
    visit(node.condition);
    visit(node.thenPart);
    if (node.elsePart !== null) visit(node.elsePart);
  }

  visitSend(Send node) {
    Identifier selector = node.selector;
    Element target = compiler.universe.find(selector.source);
    if (target == null) {
      SourceString name = selector.source;
      if (name == const SourceString('print') ||
          name == const SourceString('+') ||
          name == const SourceString('-') ||
          name == const SourceString('*') ||
          name == const SourceString('/') ||
          name == const SourceString('~/')) {
        // Do nothing.
      } else {
        // Complain: we could not resolve the method.
        fail(node);
      }
    } else {
      // Add the source of the method to the work list.
      compiler.worklist.add(selector.source);
    }
    visit(node.argumentsNode);
    return target;
  }

  visitLiteralInt(LiteralInt node) {
  }

  visitLiteralDouble(LiteralDouble node) {
  }

  visitLiteralBool(LiteralBool node) {
  }

  visitLiteralString(LiteralString node) {
  }

  visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      visit(link.head);
    }
  }

  visitOperator(Operator node) {
    fail(node);
  }

  visitParameter(Parameter node) {
    return new Element(node.name.source, context.element);
  }

  visitReturn(Return node) {
    visit(node.expression);
    return null;
  }

  visitTypeAnnotation(TypeAnnotation node) {
  }

  visitVariableDefinitions(VariableDefinitions node) {
    Visitor visitor = new VariableDefinitionsVisitor(node, this);
    visitor.visit(node.definitions);
  }
}

class VariableDefinitionsVisitor implements Visitor<Element> {
  VariableDefinitions definitions;
  ResolverVisitor resolver;

  VariableDefinitionsVisitor(this.definitions, this.resolver);

  visitSend(Send node) {
    assert(node.arguments.tail.isEmpty()); // Sanity check
    Identifier selector = node.selector;
    SourceString name = selector.source;
    if (name != const SourceString('=')) resolver.fail(node);
    resolver.visit(node.arguments.head);

    // Visit the receiver after visiting the initializer, to not put
    // the receiver in the scope.
    visit(node.receiver);
  }

  visitIdentifier(Identifier node) {
    Element variableElement =
        new Element(node.source, resolver.context.element);
    resolver.context.elements[node.token.value] = variableElement;
    resolver.mapping[node] = variableElement;
  }

  visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      visit(link.head);
    }
  }

  visit(Node node) => node.accept(this);
}

class Context {
  final Context parent;
  final Map<SourceString, Element> elements;
  final Element element;

  // TODO(karlklose): add currentClass, currentLibrary.

  Context(this.parent, this.element, [elements])
    : this.elements = (elements === null) ? {} : elements;

  Element lookup(SourceString name) {
    // TODO(karlklose): add parameter 'Library inLibrary' for library
    // private lookup.
    if (elements != null && elements[name] !== null) {
      return elements[name];
    } else if (parent !== null) {
      return parent.lookup(name);
    } else {
     return null;
    }
  }
}
