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

class Element implements Hashable {
  final Element enclosingElement;
  final SourceString name;

  abstract Node parseNode(Canceler canceler, Logger logger);
  abstract Type computeType(Compiler compiler, Types types);

  Element(this.name, [this.enclosingElement]);

  int hashCode() => name.hashCode();
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
    return types.dynamic;
  }
  return types.lookup(annotation.typeName.source);
}

class FunctionElement extends Element {
  Type type;

  FunctionElement(SourceString name) : super(name);

  FunctionType computeType(Compiler compiler, types) {
    if (type != null) return type;

    FunctionExpression node = parseNode(compiler, compiler);
    Type returnType = getType(node.returnType, types);
    if (returnType === null) compiler.cancel('unknown type $returnType');
    LinkBuilder<Type> parameterTypes = new LinkBuilder<Type>();
    for (var link = node.parameters.nodes; !link.isEmpty(); link = link.tail) {
      compiler.cancel('parameters not supported.');
      VariableDefinitions parameter = link.head;
      parameterTypes.addLast(getType(parameter.type, types));
    }
    type = new FunctionType(returnType, parameterTypes.toLink());
    return type;
  }
}
