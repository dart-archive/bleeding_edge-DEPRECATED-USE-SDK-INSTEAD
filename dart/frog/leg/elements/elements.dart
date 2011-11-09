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
  void cancel([String reason]);
}

// TODO(ahe): Better name, better abstraction...
interface Logger {
  void log(message);
}

class ElementKind {
  final String id;
  const ElementKind(String this.id);
  static final ElementKind VARIABLE = const ElementKind('variable');
  static final ElementKind FUNCTION = const ElementKind('function');
  static final ElementKind CLASS = const ElementKind('class');
  static final ElementKind FOREIGN = const ElementKind('foreign');
}

class Element implements Hashable {
  final SourceString name;
  final ElementKind kind;
  final Element enclosingElement;

  abstract Node parseNode(Canceler canceler, Logger logger);
  abstract Type computeType(Compiler compiler, Types types);

  Element(this.name, this.kind, this.enclosingElement);

  int hashCode() => name.hashCode();
}

class VariableElement extends Element {
  final Node node;
  final TypeAnnotation typeAnnotation;
  Type type;

  VariableElement(Node this.node, TypeAnnotation this.typeAnnotation,
                  SourceString name, Element enclosingElement)
    : super(name, ElementKind.VARIABLE, enclosingElement);

  Node parseNode(Canceler canceler, Logger logger) {
    return node;
  }

  Type computeType(Compiler compiler, Types types) {
    return getType(typeAnnotation, types);
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
  Type type;

  // TODO(nogeoffray): set the enclosingElement.
  FunctionElement(SourceString name) : super(name, ElementKind.FUNCTION, null);

  FunctionType computeType(Compiler compiler, types) {
    if (type != null) return type;

    FunctionExpression node = parseNode(compiler, compiler);
    Type returnType = getType(node.returnType, types);
    if (returnType === null) compiler.cancel('unknown type $returnType');
    LinkBuilder<Type> parameterTypes = new LinkBuilder<Type>();
    for (var link = node.parameters.nodes; !link.isEmpty(); link = link.tail) {
      VariableDefinitions parameter = link.head;
      parameterTypes.addLast(getType(parameter.type, types));
    }
    type = new FunctionType(returnType, parameterTypes.toLink());
    return type;
  }
}

class ClassElement extends Element {
  ClassElement(SourceString name) : super(name, ElementKind.CLASS, null);

  Type computeType(compiler, types) {
    compiler.unimplemented('ClassElement.computeType');
  }
}
