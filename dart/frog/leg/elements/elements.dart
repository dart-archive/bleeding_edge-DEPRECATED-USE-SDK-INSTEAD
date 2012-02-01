// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('elements');

#import('../tree/tree.dart');
#import('../scanner/scannerlib.dart');
#import('../leg.dart');  // TODO(karlklose): we only need type.
#import('../util/util.dart');

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
  static final ElementKind GETTER = const ElementKind('getter');
  static final ElementKind SETTER = const ElementKind('setter');
  static final ElementKind ABSTRACT_FIELD = const ElementKind('abstract_field');

  toString() => id;
}

class Element implements Hashable {
  final SourceString name;
  final ElementKind kind;
  final Element enclosingElement;
  Modifiers get modifiers() => null;

  Node parseNode(DiagnosticListener listener) {
    listener.cancel("Internal Error: Element.parseNode");
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

  Token position() => null;

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

class ContainerElement extends Element {
  ContainerElement(name, kind, enclosingElement) :
    super(name, kind, enclosingElement);

  abstract void addMember(Element element, DiagnosticListener listener);
}

class CompilationUnitElement extends ContainerElement {
  final Script script;
  Link<Element> topLevelElements = const EmptyLink<Element>();
  Link<ScriptTag> tags = const EmptyLink<ScriptTag>();

  CompilationUnitElement(Script script, Element enclosing)
    : super(new SourceString(script.name),
            ElementKind.COMPILATION_UNIT,
            enclosing),
      this.script = script;

  void addMember(Element element, DiagnosticListener listener) {
    topLevelElements = topLevelElements.prepend(element);
  }

  void addTag(ScriptTag tag) {
    tags = tags.prepend(tag);
  }
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

  Node parseNode(DiagnosticListener listener) {
    if (cachedNode !== null) return cachedNode;
    VariableDefinitions definitions = variables.parseNode(listener);
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
    listener.cancel('internal error: could not find $name', node: variables);
  }

  Type computeType(Compiler compiler) {
    return variables.computeType(compiler);
  }

  Type get type() => variables.type;

  bool isInstanceMember() {
    return isMember() && !modifiers.isStatic();
  }

  Token position() {
    // TODO(ahe): Record the token corresponding to name instead of
    // returning different values at different points in time.
    return (cachedNode !== null)
        ? cachedNode.getBeginToken() : variables.position();
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

  VariableDefinitions parseNode(DiagnosticListener listener) {
    return cachedNode;
  }

  Type computeType(Compiler compiler) {
    if (type != null) return type;
    type = getType(parseNode(compiler).type, compiler,
                   compiler.types);
    return type;
  }

  Token position() => cachedNode.getBeginToken();
}

class ForeignElement extends Element {
  ForeignElement(SourceString name) : super(name, ElementKind.FOREIGN, null);

  Type computeType(Compiler compiler) {
    return compiler.types.dynamicType;
  }

  parseNode(DiagnosticListener listener) {
    throw "internal error: ForeignElement has no node";
  }
}

class AbstractFieldElement extends Element {
  FunctionElement getter;
  FunctionElement setter;
  AbstractFieldElement(SourceString name, Element enclosing)
      : super(name, ElementKind.ABSTRACT_FIELD, enclosing);

  Type computeType(Compiler compiler) {
    throw "internal error: AbstractFieldElement has no type";
  }

  Node parseNode(DiagnosticListener listener) {
    throw "internal error: AbstractFieldElement has no node";
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

class FunctionParameters {
  Link<Element> requiredParameters;
  Link<Element> optionalParameters;
  int requiredParameterCount;
  int optionalParameterCount;
  FunctionParameters(this.requiredParameters,
                     this.optionalParameters,
                     this.requiredParameterCount,
                     this.optionalParameterCount);

  void forEachParameter(void function(Element parameter)) {
    for (Link<Element> link = requiredParameters;
         !link.isEmpty();
         link = link.tail) {
      function(link.head);
    }
    for (Link<Element> link = optionalParameters;
         !link.isEmpty();
         link = link.tail) {
      function(link.head);
    }
  }

  int get parameterCount() => requiredParameterCount + optionalParameterCount;
}

class FunctionElement extends Element {
  FunctionExpression cachedNode;
  Type type;
  final Modifiers modifiers;
  FunctionParameters functionParameters;

  FunctionElement(SourceString name,
                  ElementKind kind,
                  Modifiers this.modifiers,
                  Element enclosing)
    : super(name, kind, enclosing);

  FunctionElement.node(SourceString name,
                       FunctionExpression node,
                       ElementKind kind,
                       Modifiers this.modifiers,
                       Element enclosing)
    : super(name, kind, enclosing),
      cachedNode = node;

  FunctionElement.from(SourceString name,
                       FunctionElement other,
                       Element enclosing)
    : super(name, other.kind, enclosing),
      cachedNode = other.cachedNode,
      modifiers = other.modifiers,
      functionParameters = other.functionParameters;

  bool isInstanceMember() {
    return isMember()
           && kind != ElementKind.GENERATIVE_CONSTRUCTOR
           && !modifiers.isFactory()
           && !modifiers.isStatic();
  }

  FunctionParameters computeParameters(Compiler compiler) {
    if (functionParameters !== null) return functionParameters;
    functionParameters = compiler.resolveSignature(this);
    return functionParameters;
  }

  int requiredParameterCount(Compiler compiler) {
    return computeParameters(compiler).requiredParameterCount;
  }

  int optionalParameterCount(Compiler compiler) {
    return computeParameters(compiler).optionalParameterCount;
  }

  int parameterCount(Compiler compiler) {
    return computeParameters(compiler).parameterCount;
  }

  FunctionType computeType(Compiler compiler) {
    if (type != null) return type;
    FunctionParameters parameters = computeParameters(compiler);
    Types types = compiler.types;
    FunctionExpression node =
        compiler.parser.measure(() => parseNode(compiler));
    Type returnType = getType(node.returnType, compiler, types);
    if (returnType === null) returnType = types.dynamicType;

    LinkBuilder<Type> parameterTypes = new LinkBuilder<Type>();
    for (Link<Element> link = parameters.requiredParameters;
         !link.isEmpty();
         link = link.tail) {
      parameterTypes.addLast(link.head.computeType(compiler));
    }
    type = new FunctionType(returnType, parameterTypes.toLink(), this);
    return type;
  }

  Node parseNode(DiagnosticListener listener) => cachedNode;

  Token position() => cachedNode.getBeginToken();
}

class ConstructorBodyElement extends FunctionElement {
  FunctionElement constructor;

  ConstructorBodyElement(FunctionElement constructor)
      : this.constructor = constructor,
        super(constructor.name,
              ElementKind.GENERATIVE_CONSTRUCTOR_BODY,
              null,
              constructor.enclosingElement) {
    functionParameters = constructor.functionParameters;
  }

  bool isInstanceMember() => true;

  FunctionType computeType(Compiler compiler) { unreachable(); }

  Node parseNode(DiagnosticListener listener) {
    if (cachedNode !== null) return cachedNode;
    cachedNode = constructor.parseNode(listener);
    assert(cachedNode !== null);
    return cachedNode;
  }

  Token position() => constructor.position();
}

class SynthesizedConstructorElement extends FunctionElement {
  SynthesizedConstructorElement(Element enclosing)
    : super(enclosing.name, ElementKind.GENERATIVE_CONSTRUCTOR,
            null, enclosing);

  FunctionType computeType(Compiler compiler) {
    if (type != null) return type;
    Type returnType = compiler.types.voidType;
    type = new FunctionType(returnType, const EmptyLink<Type>(), this);
    return type;
  }

  Node parseNode(DiagnosticListener listener) {
    if (cachedNode != null) return cachedNode;
    cachedNode = new FunctionExpression(
        new Identifier.synthetic(''),
        new NodeList.empty(),
        new Block(new NodeList.empty()),
        null, null, null, null);
    return cachedNode;
  }

  Token position() => null;
}

class ClassElement extends ContainerElement {
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

  Link<Type> allSupertypes;

  ClassElement(SourceString name, CompilationUnitElement enclosing)
    : localMembers = new Map<SourceString, Element>(),
      constructors = new Map<SourceString, Element>(),
      super(name, ElementKind.CLASS, enclosing);

  void addMember(Element element, DiagnosticListener listener) {
    members = members.prepend(element);
    if (element.kind == ElementKind.GENERATIVE_CONSTRUCTOR ||
        element.modifiers.isFactory()) {
      constructors[element.name] = element;
    } else if (element.kind == ElementKind.GETTER
               || element.kind == ElementKind.SETTER) {
      Element existing = localMembers[element.name];
      if (existing != null) {
        if (existing.kind !== ElementKind.ABSTRACT_FIELD) {
          listener.cancel('duplicate definition of $name', element: element);
          listener.cancel('existing definition', element: existing);
        } else {
          AbstractFieldElement field = existing;
          if (element.kind == ElementKind.GETTER) {
            if (field.getter != null) {
              listener.cancel('duplicate definition of getter ${element.name}',
                              element: element);
              listener.cancel('existing definition', element: field.getter);
            } else {
              field.getter = element;
            }
          } else {
            if (field.setter != null) {
              listener.cancel('duplicate definition of setter ${element.name}',
                              element: element);
              listener.cancel('existing definition', element: field.setter);
            } else {
              field.setter = element;
            }
          }
        }
      } else {
        AbstractFieldElement field =
            new AbstractFieldElement(element.name, this);
        localMembers[element.name] = field;
        if (element.kind == ElementKind.GETTER) {
          field.getter = element;
        } else {
          field.setter = element;
        }
      }
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
      name = Elements.constructConstructorName(className, constructorName);
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
  ClassElement get superclass() {
    assert(isResolved);
    return supertype === null ? null : supertype.element;
  }
}

class Elements {
  static bool isInstanceField(Element element) {
    return (element !== null)
           && element.isInstanceMember()
           && (element.kind === ElementKind.FIELD
               || element.kind === ElementKind.GETTER
               || element.kind === ElementKind.SETTER);
  }

  static bool isStaticOrTopLevelField(Element element) {
    return (element != null)
           && !element.isInstanceMember()
           && (element.kind === ElementKind.FIELD
               || element.kind === ElementKind.GETTER
               || element.kind === ElementKind.SETTER);
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

  static SourceString constructConstructorName(SourceString receiver,
                                               SourceString selector) {
    return new SourceString('$receiver\$$selector');
  }

  static SourceString constructOperatorName(SourceString receiver,
                                            SourceString selector,
                                            [bool isPrefix = false]) {
    String str = selector.stringValue;
    if (str === '==' || str === '!=') return Namer.OPERATOR_EQUALS;

    if (str === '~') str = 'not';
    else if (str === 'negate' || (str === '-' && isPrefix)) str = 'negate';
    else if (str === '[]') str = 'index';
    else if (str === '[]=') str = 'indexSet';
    else if (str === '*' || str === '*=') str = 'mul';
    else if (str === '/' || str === '/=') str = 'div';
    else if (str === '%' || str === '%=') str = 'mod';
    else if (str === '~/' || str === '~/=') str = 'tdiv';
    else if (str === '+' || str === '+=') str = 'add';
    else if (str === '-' || str === '-=') str = 'sub';
    else if (str === '<<' || str === '<<=') str = 'shl';
    else if (str === '>>' || str === '>>=') str = 'shr';
    else if (str === '>=') str = 'ge';
    else if (str === '>') str = 'gt';
    else if (str === '<=') str = 'le';
    else if (str === '<') str = 'lt';
    else if (str === '&' || str === '&=') str = 'and';
    else if (str === '^' || str === '^=') str = 'xor';
    else if (str === '|' || str === '|=') str = 'or';
    return new SourceString('$receiver\$$str');
  }
}
