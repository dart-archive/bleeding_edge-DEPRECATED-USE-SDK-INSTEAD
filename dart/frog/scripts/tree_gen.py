#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

'''Generates the many subtypes of Node as well as a NodeVisitor into
   tree.g.dart.'''

from codegen import CodeWriter

class Node:
  def __init__(self, name, properties=None):
    if properties is None:
      self.properties = []
    else:
      self.properties = [p.strip().split() for p in properties.split(',')]
    self.kind = self.__class__.__name__

    self.name = name
    if self.name.endswith('!'):
      self.name = self.name[:-1]
      self.fullname = self.name
    else:
      self.fullname = self.name + self.kind

  def write(self, cw):
    cw.enterBlock('class %s extends %s {' % (self.fullname, self.kind))

    for typ, name in self.properties:
      cw.writeln('%s %s;', typ, name)

    args = ['this.%s' % name for typ, name in self.properties]
    args.append('SourceSpan span')

    cw.writeln('')

    cw.writeln('%s(%s): super(span) {}', self.fullname, ', '.join(args));

    cw.writeln('')

    cw.writeln('visit(TreeVisitor visitor) => visitor.visit%s(this);'
        % self.fullname)

    cw.exitBlock('}')

  def writeVisitInterfaceMethod(self, cw):
    cw.writeln('visit%s(%s node);', self.fullname, self.fullname)

  def writePrettyPrintMethod(self, cw):
    cw.enterBlock('void visit%s(%s node) {' % (self.fullname, self.fullname))
    if oneLineProperties(self.properties):
      cw.writeln(
        'output.heading(%r + output.toValue(node.%s) + ")", node.span);' %
        (self.fullname + '(', self.properties[0][1]))
    else:
      cw.writeln('output.heading(%r, node.span);' % self.fullname)
      for typ, name in self.properties:
        if isNodeType(typ):
          cw.writeln('output.writeNode(%r, node.%s);', name, name)
        elif isListType(typ):
          innerType = typ[len('List<'):-1]
          if isNodeType(innerType):
            cw.writeln('output.writeNodeList(%r, node.%s);', name, name)
          else:
            cw.writeln('output.writeList(%r, node.%s);', name, name)
        else:
          cw.writeln('output.writeValue(%r, node.%s);', name, name)
    cw.exitBlock('}')

def oneLineProperties(properties):
  if len(properties) != 1: return False

  propType = properties[0][0]

  if propType == 'Identifier': return True
  if isNodeType(propType): return False
  if isListType(propType): return False

  return True

def isNodeType(typ):
  return (typ.endswith('Expression') or typ.endswith('Statement') or
          typ.endswith('Node') or typ.endswith('TypeReference') or
          typ.endswith('Identifier') or typ.endswith('Definition'))

def isListType(typ):
  return typ.startswith('List<')

class Expression(Node): pass

class Statement(Node): pass

class Definition(Node): pass

class TypeReference(Node): pass

nodes = [
  Definition('Directive', 'Identifier name, List<ArgumentNode> arguments'),

  Definition('Type',
    'bool isClass, Identifier name, List<ParameterType> typeParameters, '+
    'List<TypeReference> extendsTypes, List<TypeReference> implementsTypes, '+
    'NativeType nativeType, DefaultTypeReference defaultType, '+
    'List<Statement> body'),

  Definition('FunctionType',
    'FunctionDefinition func, List<ParameterType> typeParameters'),

  Definition('Variable',
    'List<Token> modifiers, TypeReference type, List<Identifier> names,' +
    'List<Expression> values'),

  Definition('Function',
    'List<Token> modifiers, TypeReference returnType, Identifier name,' +
    'List<FormalNode> formals, List<Expression> initializers, ' +
    'String nativeBody, Statement body'),

  Statement('Return', 'Expression value'),
  Statement('Throw', 'Expression value'),
  Statement('Assert', 'Expression test'),

  Statement('Break', 'Identifier label'),
  Statement('Continue', 'Identifier label'),

  Statement('If',
    'Expression test, Statement trueBranch, Statement falseBranch'),
  Statement('While', 'Expression test, Statement body'),
  Statement('Do', 'Statement body, Expression test'),
  Statement('For',
    'Statement init, Expression test, List<Expression> step, Statement body'),
  Statement('ForIn',
    'DeclaredIdentifier item, Expression list, Statement body'),

  Statement('Try',
    'Statement body, List<CatchNode> catches, Statement finallyBlock'),
  Statement('Switch', 'Expression test, List<CaseNode> cases'),

  Statement('Block', 'List<Statement> body'),

  Statement('Labeled', 'Identifier name, Statement body'),
  Statement('Expression', 'Expression body'),
  Statement('Empty'),
  Statement('Diet'),

  Expression('Lambda', 'FunctionDefinition func'),
  Expression('Call', 'Expression target, List<ArgumentNode> arguments'),

  # These three desugar into Call, but that is handled in gen, not parser.
  Expression('Index', 'Expression target, Expression index'),
  Expression('Binary', 'Token op, Expression x, Expression y'),
  Expression('Unary', 'Token op, Expression self'),

  Expression('Postfix', 'Expression body, Token op'),

  Expression('New',
    'bool isConst, TypeReference type, Identifier name,' +
    'List<ArgumentNode> arguments'),

  Expression('List',
    'bool isConst, TypeReference itemType, List<Expression> values'),
  Expression('Map',
    'bool isConst, TypeReference keyType, TypeReference valueType,' +
    'List<Expression> items'),

  Expression('Conditional',
    'Expression test, Expression trueBranch, Expression falseBranch'),

  Expression('Is', 'bool isTrue, Expression x, TypeReference type'),
  Expression('Paren', 'Expression body'),
  Expression('Await', 'Expression body'),

  Expression('Dot', 'Expression self, Identifier name'),
  Expression('Var', 'Identifier name'),

  Expression('This'),
  Expression('Super'),

  Expression('Literal', 'Value value'),

  Expression('StringInterp', 'List<Expression> pieces'),

  # TODO(jimhug): Split into Simple and Qualified names
  TypeReference('Simple', 'Type type'),
  TypeReference('Name',
    'bool isFinal, Identifier name, List<Identifier> names'),

  TypeReference('Generic',
    'TypeReference baseType, List<TypeReference> typeArguments, int depth'),
  TypeReference('Function',
    'bool isFinal, FunctionDefinition func'),
  # TODO(jimhug): This shouldn't be a subtype of TypeReference.
  TypeReference('Default', 'bool oldFactory, NameTypeReference baseType, '+
    'List<ParameterType> typeParameters'),

  Node('Argument', 'Identifier label, Expression value'),
  Node('Formal',
    'bool isThis,  bool isRest, TypeReference type, Identifier name,'+
    'Expression value'),

  Node('Catch',
    'DeclaredIdentifier exception, DeclaredIdentifier trace, Statement body'),
  Node('Case',
    'Identifier label, List<Expression> cases, List<Statement> statements'),

  # Don't want to add Node to these names, use ! as convention to say so.
  Node('TypeParameter!', 'Identifier name, TypeReference extendsType'),

  # TODO(jimhug): Consider removing this node and just using String.
  Node('Identifier!', 'String name'),

  # Pseudo Expression for cover grammar approach
  Expression('DeclaredIdentifier!',
             'TypeReference type, Identifier name, bool isFinal'),
]

def main():
  cw = CodeWriter(__file__)

  for node in nodes:
    node.write(cw)
    cw.writeln()

  cw.writeln()
  cw.enterBlock('interface TreeVisitor {')
  for node in nodes:
    node.writeVisitInterfaceMethod(cw)
    cw.writeln()

  cw.exitBlock('}')

  cw.writeln()
  cw.enterBlock('class TreePrinter implements TreeVisitor {')

  cw.writeln('var output;')
  cw.writeln('TreePrinter(this.output) { output.printer = this; }')
  for node in nodes:
    node.writePrettyPrintMethod(cw)
    cw.writeln()
  cw.exitBlock('}')

  cw.writeToFile('tree')

if __name__ == '__main__': main()
