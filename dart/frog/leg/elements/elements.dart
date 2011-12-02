// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('elements');

#import('../tree/tree.dart');
#import('../scanner/scannerlib.dart');
#import('../leg.dart');  // TODO(karlklose): we only need type.
#import('../util/util.dart');

// TODO(ahe): Better name, better abstraction...
interface Canceler {
  void cancel([String reason, node, token, instruction]);
}

// TODO(ahe): Better name, better abstraction...
interface Logger {
  void log(message);
}

class ElementKind {
  final String id;

  const ElementKind(String this.id);

  static final ElementKind VARIABLE = const ElementKind('variable');
  static final ElementKind PARAMETER = const ElementKind('parameter');
  static final ElementKind FUNCTION = const ElementKind('function');
  static final ElementKind CLASS = const ElementKind('class');
  static final ElementKind FOREIGN = const ElementKind('foreign');
  static final ElementKind CONSTRUCTOR = const ElementKind('constructor');

  toString() => id;
}

class Element implements Hashable {
  final SourceString name;
  final ElementKind kind;
  final Element enclosingElement;
  abstract Node parseNode(Canceler canceler, Logger logger);
  abstract Type computeType(Compiler compiler, Types types);
  bool isClassMember() =>
      enclosingElement !== null && enclosingElement.kind == ElementKind.CLASS;

  const Element(this.name, this.kind, this.enclosingElement);

  // TODO(kasperl): This is a very bad hash code for the element and
  // there's no reason why two elements with the same name should have
  // the same hash code. Replace this with a simple id in the element?
  int hashCode() => name.hashCode();
}

class VariableElement extends Element {
  final Node node;
  final TypeAnnotation typeAnnotation;
  Type type;

  VariableElement(Node this.node, TypeAnnotation this.typeAnnotation,
                  ElementKind kind, SourceString name, Element enclosingElement)
    : super(name, kind, enclosingElement);

  Node parseNode(Canceler canceler, Logger logger) {
    return node;
  }

  Type computeType(Compiler compiler, Types types) {
    if (type !== null) return type;
    type = getType(typeAnnotation, types);
    return type;
  }
}

class ForeignElement extends Element {
  ForeignElement(SourceString name) : super(name, ElementKind.FOREIGN, null);

  Type computeType(Compiler compiler, Types types) {
    return types.dynamicType;
  }
}

/**
 * TODO(ngeoffray): Remove this method in favor of using the universe.
 *
 * Return the type referred to by the type annotation. This method
 * accepts annotations with 'typeName == null' to indicate a missing
 * annotation.
 */
Type getType(TypeAnnotation annotation, types) {
  if (annotation == null || annotation.typeName == null) {
    return types.dynamicType;
  }
  return types.lookup(annotation.typeName.source);
}

class FunctionElement extends Element {
  Link<Element> parameters;
  FunctionExpression node;
  Type type;

  FunctionElement(SourceString name,
                  ElementKind kind,
                  Element enclosing)
    : super(name, kind, enclosing);
  FunctionElement.node(FunctionExpression node,
                       ElementKind kind,
                       Element enclosing)
    : super(node.name.asIdentifier().source, kind, enclosing),
      this.node = node;

  FunctionType computeType(Compiler compiler, types) {
    if (type != null) return type;
    if (parameters == null) compiler.resolveSignature(this);

    FunctionExpression node = parseNode(compiler, compiler);
    Type returnType = getType(node.returnType, types);
    if (returnType === null) compiler.cancel('unknown type ${node.returnType}');

    LinkBuilder<Type> parameterTypes = new LinkBuilder<Type>();
    for (Link<Element> link = parameters; !link.isEmpty(); link = link.tail) {
      parameterTypes.addLast(link.head.computeType(compiler, types));
    }
    type = new FunctionType(returnType, parameterTypes.toLink());
    return type;
  }

  Node parseNode(Canceler canceler, Logger logger) => node;
}

class SynthesizedConstructorElement extends FunctionElement {
  SynthesizedConstructorElement(Element enclosing)
    : super(const SourceString(''), ElementKind.CONSTRUCTOR, enclosing) {
    parameters = const EmptyLink<Element>();
  }

  FunctionType computeType(Compiler compiler, types) {
    if (type != null) return type;
    type = new FunctionType(types.voidType, const EmptyLink<Type>());
    return type;
  }

  Node parseNode(Canceler canceler, Logger logger) {
    if (node != null) return node;
    node = new FunctionExpression(
        new Identifier.synthetic(''),
        new NodeList.empty(),
        new Block(new NodeList.empty()));
    return node;
  }
}

class ClassElement extends Element {
  Type type;
  Type supertype;
  Link<Type> interfaces = const EmptyLink<Type>();
  bool isResolved = false;
  ClassNode node;

  ClassElement(SourceString name) : super(name, ElementKind.CLASS, null);

  Type computeType(compiler, types) {
    if (type === null) {
      type = new SimpleType(name, this);
    }
    return type;
  }

  void resolve(Compiler compiler) {
    if (isResolved) return;
    compiler.resolveType(this);
    isResolved = true;
  }

  // TODO(ngeoffray): Implement these.
  Element lookupLocalElement(SourceString name) => null;
  bool canHaveDefaultConstructor() => true;
  void addConstructor(Element element) {}
}
