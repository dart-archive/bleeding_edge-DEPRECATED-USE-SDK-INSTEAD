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
  static final ElementKind GENERATIVE_CONSTRUCTOR =
      const ElementKind('generative_constructor');
  static final ElementKind FIELD = const ElementKind('field');
  static final ElementKind VARIABLE_LIST = const ElementKind('variable_list');
  static final ElementKind FIELD_LIST = const ElementKind('field_list');
  static final ElementKind GENERATIVE_CONSTRUCTOR_BODY =
      const ElementKind('generative_constructor_body');
  static final ElementKind COMPILATION_UNIT =
      const ElementKind('compilation_unit');

  toString() => id;
}

class Element implements Hashable {
  final SourceString name;
  final ElementKind kind;
  final Element enclosingElement;
  Modifiers get modifiers() => null;


  Node parseNode(Canceler canceler, Logger logger) {
    canceler.cancel("Internal Error: Element.parseNode");
  }

  Type computeType(Compiler compiler) {
    compiler.internalError("Element.computeType.");
  }

  bool isFunction() => kind == ElementKind.FUNCTION;
  bool isMember() =>
      enclosingElement !== null && enclosingElement.kind == ElementKind.CLASS;
  bool isInstanceMember() => false;
  bool isFactoryConstructor() => modifiers != null && modifiers.isFactory();
  bool isGenerativeConstructor() => kind == ElementKind.GENERATIVE_CONSTRUCTOR;
  bool isCompilationUnit() => kind == ElementKind.COMPILATION_UNIT;
  bool isVariable() => kind == ElementKind.VARIABLE;
  bool isParameter() => kind == ElementKind.PARAMETER;

  bool isAssignable() {
    if (modifiers != null && modifiers.isFinal()) return false;
    if (isFunction() || isGenerativeConstructor()) return false;
    return true;
  }

  const Element(this.name, this.kind, this.enclosingElement);

  // TODO(kasperl): This is a very bad hash code for the element and
  // there's no reason why two elements with the same name should have
  // the same hash code. Replace this with a simple id in the element?
  int hashCode() => name.hashCode();

  CompilationUnitElement getEnclosingCompilationUnit() {
    Element element = this;
    while (element !== null && !element.isCompilationUnit()) {
      element = element.enclosingElement;
    }
    return element;
  }

  toString() => '$kind($name)';
}

class CompilationUnitElement extends Element {
  final Script script;
  CompilationUnitElement(Script script, Element enclosing)
    : super(new SourceString(script.name),
            ElementKind.COMPILATION_UNIT,
            enclosing),
      this.script = script;
}

class VariableElement extends Element {
  final VariableListElement variables;
  Expression cachedNode; // The send or the identifier in the variables list.

  Modifiers get modifiers() => variables.modifiers;

  VariableElement(SourceString name,
                  VariableListElement this.variables,
                  ElementKind kind,
                  Element enclosing,
                  [Node node])
    : super(name, kind, enclosing), cachedNode = node;

  Node parseNode(Canceler canceler, Logger logger) {
    if (cachedNode !== null) return cachedNode;
    VariableDefinitions definitions = variables.parseNode(canceler, logger);
    for (Link<Node> link = definitions.definitions.nodes;
         !link.isEmpty(); link = link.tail) {
      Expression initializedIdentifier = link.head;
      Identifier identifier = initializedIdentifier.asIdentifier();
      if (identifier === null) {
        identifier = initializedIdentifier.asSendSet().selector.asIdentifier();
      }
      if (name === identifier.source) {
        cachedNode = initializedIdentifier;
        return cachedNode;
      }
    }
    canceler.cancel('internal error: could not find $name', node: variables);
  }

  Type computeType(Compiler compiler) {
    return variables.computeType(compiler);
  }

  Type get type() => variables.type;

  bool isInstanceMember() {
    return isMember() && !modifiers.isStatic();
  }
}

// This element represents a list of variable or field declaration.
// It contains the node, and the type. A [VariableElement] always
// references its [VariableListElement]. It forwards its
// [computeType] and [parseNode] methods to this element.
class VariableListElement extends Element {
  VariableDefinitions cachedNode;
  Type type;
  final Modifiers modifiers;

  VariableListElement(ElementKind kind,
                      Modifiers this.modifiers,
                      Element enclosing)
    : super(null, kind, enclosing);

  VariableListElement.node(VariableDefinitions node,
                           ElementKind kind,
                           Element enclosing)
    : super(null, kind, enclosing),
      this.cachedNode = node,
      this.modifiers = node.modifiers;

  VariableDefinitions parseNode(Canceler canceler, Logger logger) {
    return cachedNode;
  }

  Type computeType(Compiler compiler) {
    if (type != null) return type;
    type = getType(parseNode(compiler, compiler).type, compiler,
                   compiler.types);
    return type;
  }
}

class ForeignElement extends Element {
  ForeignElement(SourceString name) : super(name, ElementKind.FOREIGN, null);

  Type computeType(Compiler compiler) {
    return compiler.types.dynamicType;
  }

  parseNode(Canceler canceler, Logger logger) {
    throw "internal error: ForeignElement has no node";
  }
}

/**
 * TODO(ngeoffray): Remove this method in favor of using the universe.
 *
 * Return the type referred to by the type annotation. This method
 * accepts annotations with 'typeName == null' to indicate a missing
 * annotation.
 */
Type getType(TypeAnnotation typeAnnotation, compiler, types) {
  if (typeAnnotation == null || typeAnnotation.typeName == null) {
    return types.dynamicType;
  }
  Identifier identifier = typeAnnotation.typeName.asIdentifier();
  if (identifier === null) {
    compiler.cancel('library prefixes not handled',
                    node: typeAnnotation.typeName);
  }
  final SourceString name = identifier.source;
  Element element = compiler.universe.find(name);
  if (element !== null && element.kind === ElementKind.CLASS) {
    // TODO(karlklose): substitute type parameters.
    return element.computeType(compiler);
  }
  return types.lookup(name);
}

class FunctionElement extends Element {
  Link<Element> parameters;
  FunctionExpression cachedNode;
  Type type;
  final Modifiers modifiers;
  int cachedParameterCount;

  FunctionElement(SourceString name,
                  ElementKind kind,
                  Modifiers this.modifiers,
                  Element enclosing,
                  [Node node])
    : super(name, kind, enclosing), cachedNode = node;
  FunctionElement.node(SourceString name,
                       FunctionExpression node,
                       ElementKind kind,
                       Modifiers this.modifiers,
                       Element enclosing)
    : super(name, kind, enclosing),
      this.cachedNode = node;

  bool isInstanceMember() {
    return isMember()
           && kind != ElementKind.GENERATIVE_CONSTRUCTOR
           && !modifiers.isFactory()
           && !modifiers.isStatic();
  }

  int parameterCount(Compiler compiler) {
    if (cachedParameterCount === null) {
      cachedParameterCount = 0;
      if (parameters == null) compiler.resolveSignature(this);
      for (Link l = parameters; !l.isEmpty(); l = l.tail) {
        cachedParameterCount++;
      }
    }
    return cachedParameterCount;
  }

  FunctionType computeType(Compiler compiler) {
    if (type != null) return type;
    if (parameters == null) compiler.resolveSignature(this);
    Types types = compiler.types;
    FunctionExpression node =
        compiler.parser.measure(() => parseNode(compiler, compiler));
    Type returnType = getType(node.returnType, compiler, types);
    if (returnType === null) returnType = types.dynamicType;

    LinkBuilder<Type> parameterTypes = new LinkBuilder<Type>();
    for (Link<Element> link = parameters; !link.isEmpty(); link = link.tail) {
      parameterTypes.addLast(link.head.computeType(compiler));
    }
    type = new FunctionType(returnType, parameterTypes.toLink(), this);
    return type;
  }

  Node parseNode(Canceler canceler, Logger logger) => cachedNode;
}

class ConstructorBodyElement extends FunctionElement {
  FunctionElement constructor;

  ConstructorBodyElement(FunctionElement constructor)
      : this.constructor = constructor,
        super(constructor.name,
              ElementKind.GENERATIVE_CONSTRUCTOR_BODY,
              null,
              constructor.enclosingElement) {
    this.parameters = constructor.parameters;
  }

  bool isInstanceMember() => true;

  FunctionType computeType(Compiler compiler) { unreachable(); }

  Node parseNode(Canceler canceler, Logger logger) {
    if (cachedNode !== null) return cachedNode;
    cachedNode = constructor.parseNode(canceler, logger);
    assert(cachedNode !== null);
    return cachedNode;
  }
}

class SynthesizedConstructorElement extends FunctionElement {
  SynthesizedConstructorElement(Element enclosing)
    : super(enclosing.name, ElementKind.GENERATIVE_CONSTRUCTOR,
            null, enclosing) {
    parameters = const EmptyLink<Element>();
  }

  FunctionType computeType(Compiler compiler) {
    if (type != null) return type;
    Type returnType = compiler.types.voidType;
    type = new FunctionType(returnType, const EmptyLink<Type>(), this);
    return type;
  }

  Node parseNode(Canceler canceler, Logger logger) {
    if (cachedNode != null) return cachedNode;
    cachedNode = new FunctionExpression(
        new Identifier.synthetic(''),
        new NodeList.empty(),
        new Block(new NodeList.empty()),
        null, null, null);
    return cachedNode;
  }
}

class ClassElement extends Element {
  Type type;
  Type supertype;
  Link<Element> members = const EmptyLink<Element>();
  Map<SourceString, Element> localMembers;
  Map<SourceString, Element> constructors;
  Link<Type> interfaces = const EmptyLink<Type>();
  bool isResolved = false;
  // backendMembers are members that have been added by the backend to simplify
  // compilation. They don't have any user-side counter-part.
  Link<Element> backendMembers = const EmptyLink<Element>();
  SynthesizedConstructorElement synthesizedConstructor;

  ClassElement(SourceString name, CompilationUnitElement enclosing)
    : localMembers = new Map<SourceString, Element>(),
      constructors = new Map<SourceString, Element>(),
      super(name, ElementKind.CLASS, enclosing);

  void addMember(Element element) {
    members = members.prepend(element);
    if (element.kind == ElementKind.GENERATIVE_CONSTRUCTOR ||
        element.modifiers.isFactory()) {
      constructors[element.name] = element;
    } else {
      localMembers[element.name] = element;
    }
  }

  Type computeType(compiler) {
    if (type === null) {
      type = new SimpleType(name, this);
    }
    return type;
  }

  ClassElement resolve(Compiler compiler) {
    if (!isResolved) {
      compiler.resolveType(this);
      isResolved = true;
    }
    return this;
  }

  Element lookupLocalMember(SourceString name) {
    return localMembers[name];
  }

  Element lookupConstructor(SourceString className,
                            [SourceString constructorName =
                                 const SourceString(''),
                            Element noMatch(Element)]) {
    // TODO(karlklose): have a map from class names to a map of constructors
    //                  instead of creating the name here?
    SourceString name;
    if (constructorName !== const SourceString('')) {
      name = new SourceString('$className.$constructorName');
    } else {
      name = className;
    }
    Element result = constructors[name];
    if (result === null && noMatch !== null) {
      result = noMatch(lookupLocalMember(constructorName));
    }
    return result;
  }

  bool canHaveDefaultConstructor() => constructors.length == 0;

  SynthesizedConstructorElement getSynthesizedConstructor() {
    if (synthesizedConstructor === null && canHaveDefaultConstructor()) {
      synthesizedConstructor = new SynthesizedConstructorElement(this);
    }
    return synthesizedConstructor;
  }

  /**
   * Returns the super class, if any.
   *
   * The returned element may not be resolved yet.
   */
  ClassElement get superClass() {
    assert(isResolved);
    return supertype === null ? null : supertype.element;
  }
}

class Elements {
  static bool isInstanceField(Element element) {
    return (element !== null)
           && element.isInstanceMember()
           && (element.kind === ElementKind.FIELD);
  }

  static bool isStaticOrTopLevelField(Element element) {
    return (element != null)
           && !element.isInstanceMember()
           && (element.kind === ElementKind.FIELD);
  }

  static bool isInstanceMethod(Element element) {
    return (element != null)
           && element.isInstanceMember()
           && (element.kind === ElementKind.FUNCTION);
  }

  static bool isClosureSend(Send send, TreeElements elements) {
    if (send.isPropertyAccess) return false;
    if (send.receiver !== null) return false;
    Element element = elements[send];
    // (o)() or foo()().
    if (element === null && send.selector.asIdentifier() === null) return true;
    if (element === null) return false;
    // foo() with foo a local or a parameter.
    return element.isVariable() || element.isParameter();
  }
}
