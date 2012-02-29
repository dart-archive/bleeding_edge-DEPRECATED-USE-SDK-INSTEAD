// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
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
  static final ElementKind LIBRARY = const ElementKind('library');
  static final ElementKind PREFIX = const ElementKind('prefix');

  static final ElementKind STATEMENT = const ElementKind('statement');
  static final ElementKind LABEL = const ElementKind('label');

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

  bool isFunction() => kind === ElementKind.FUNCTION;
  bool isMember() =>
      enclosingElement !== null && enclosingElement.kind === ElementKind.CLASS;
  bool isInstanceMember() => false;
  bool isFactoryConstructor() => modifiers !== null && modifiers.isFactory();
  bool isGenerativeConstructor() => kind === ElementKind.GENERATIVE_CONSTRUCTOR;
  bool isCompilationUnit() {
    return kind === ElementKind.COMPILATION_UNIT ||
           kind === ElementKind.LIBRARY;
  }
  bool isVariable() => kind === ElementKind.VARIABLE;
  bool isParameter() => kind === ElementKind.PARAMETER;
  bool isStatement() => kind === ElementKind.STATEMENT;

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

  CompilationUnitElement getCompilationUnit() {
    Element element = this;
    while (element !== null && !element.isCompilationUnit()) {
      element = element.enclosingElement;
    }
    return element;
  }

  LibraryElement getLibrary() {
    Element element = this;
    while (element.kind !== ElementKind.LIBRARY) {
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

  CompilationUnitElement(Script script, Element enclosing)
    : this.script = script,
      super(new SourceString(script.name),
            ElementKind.COMPILATION_UNIT,
            enclosing);

  CompilationUnitElement.library(Script script)
    : this.script = script,
      super(new SourceString(script.name), ElementKind.LIBRARY, null);

  void addMember(Element element, DiagnosticListener listener) {
    LibraryElement library = enclosingElement;
    library.addMember(element, listener);
    topLevelElements = topLevelElements.prepend(element);
  }

  void define(Element element, DiagnosticListener listener) {
    LibraryElement library = enclosingElement;
    library.define(element, listener);
  }

  void addTag(ScriptTag tag, DiagnosticListener listener) {
    listener.cancel("script tags not allowed here", node: tag);
  }
}

class LibraryElement extends CompilationUnitElement {
  // TODO(ahe): Library element should not be a subclass of
  // CompilationUnitElement.

  Link<CompilationUnitElement> compilationUnits =
    const EmptyLink<CompilationUnitElement>();
  Link<ScriptTag> tags = const EmptyLink<ScriptTag>();
  ScriptTag libraryTag;
  Map<SourceString, Element> elements;

  LibraryElement(Script script)
      : elements = new Map<SourceString, Element>(),
        super.library(script);

  void addCompilationUnit(CompilationUnitElement element) {
    compilationUnits = compilationUnits.prepend(element);
  }

  void addTag(ScriptTag tag, DiagnosticListener listener) {
    tags = tags.prepend(tag);
  }

  void addMember(Element element, DiagnosticListener listener) {
    topLevelElements = topLevelElements.prepend(element);
    define(element, listener);
  }

  void define(Element element, DiagnosticListener listener) {
    Element existing = elements.putIfAbsent(element.name, () => element);
    if (existing !== element) {
      listener.cancel('duplicate definition', token: element.position());
      listener.cancel('existing definition', token: existing.position());
    }
  }

  Element find(SourceString name) {
    return elements[name];
  }

  Element lookupLocalMember(SourceString name) {
    Element element = find(name);
    if (element === null) return null;
    return (this === element.getLibrary()) ? element : null;
  }
}

class PrefixElement extends Element {
  final LiteralString prefix;
  final LibraryElement library;

  PrefixElement(LiteralString prefix,
                LibraryElement this.library,
                Element enclosing)
    : this.prefix = prefix,
      super(prefix.dartString.source, ElementKind.PREFIX, enclosing) {
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
    type = getType(parseNode(compiler).type, compiler, getLibrary());
    return type;
  }

  Token position() => cachedNode.getBeginToken();
}

class ForeignElement extends Element {
  ForeignElement(SourceString name, ContainerElement enclosingElement)
    : super(name, ElementKind.FOREIGN, enclosingElement);

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

/** DEPRECATED. */
Type getType(TypeAnnotation typeAnnotation,
             Compiler compiler,
             LibraryElement library) {
  // TODO(karlklose,ngeoffray): This method should be removed and the
  // information should be computed by the resolver.

  if (typeAnnotation == null || typeAnnotation.typeName == null) {
    return compiler.types.dynamicType;
  }
  Identifier identifier = typeAnnotation.typeName.asIdentifier();
  if (identifier === null) {
    compiler.cancel('library prefixes not handled',
                    node: typeAnnotation.typeName);
  }
  final SourceString name = identifier.source;
  Element element = library.find(name);
  if (element !== null && element.kind === ElementKind.CLASS) {
    // TODO(karlklose): substitute type parameters.
    return element.computeType(compiler);
  }
  Type type = compiler.types.lookup(name);
  if (type === null) {
    type = compiler.types.dynamicType;
  }
  return type;
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

  /**
   * If this is an interface constructor, [defaultImplementation] will
   * changed by the resolver to point to the default
   * implementation. Otherwise, [:defaultImplementation === this:].
   */
  FunctionElement defaultImplementation;

  FunctionElement(SourceString name,
                  ElementKind kind,
                  Modifiers modifiers,
                  Element enclosing)
    : this.tooMuchOverloading(name, null, kind, modifiers, enclosing, null);

  FunctionElement.node(SourceString name,
                       FunctionExpression node,
                       ElementKind kind,
                       Modifiers modifiers,
                       Element enclosing)
    : this.tooMuchOverloading(name, node, kind, modifiers, enclosing, null);

  FunctionElement.from(SourceString name,
                       FunctionElement other,
                       Element enclosing)
    : this.tooMuchOverloading(name, other.cachedNode, other.kind,
                              other.modifiers, enclosing,
                              other.functionParameters);

  FunctionElement.tooMuchOverloading(SourceString name,
                                     FunctionExpression this.cachedNode,
                                     ElementKind kind,
                                     Modifiers this.modifiers,
                                     Element enclosing,
                                     FunctionParameters this.functionParameters)
    : super(name, kind, enclosing)
  {
    defaultImplementation = this;
  }

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
    Type returnType = getType(node.returnType, compiler, getLibrary());
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
  Type defaultClass;
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
          listener.cancel('duplicate definition of ${name.slowToString()}',
                          element: element);
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

  Element lookupLocalMember(SourceString memberName) {
    return localMembers[memberName];
  }

  Element lookupConstructor(SourceString className,
                            [SourceString constructorName =
                                 const SourceString(''),
                            Element noMatch(Element)]) {
    // TODO(karlklose): have a map from class names to a map of constructors
    //                  instead of creating the name here?
    SourceString normalizedName;
    if (constructorName !== const SourceString('')) {
      normalizedName = Elements.constructConstructorName(className,
                                                         constructorName);
    } else {
      normalizedName = className;
    }
    Element result = constructors[normalizedName];
    if (result === null && noMatch !== null) {
      result = noMatch(lookupLocalMember(constructorName));
    }
    return result;
  }

  bool canHaveDefaultConstructor() => constructors.length == 0;

  SynthesizedConstructorElement getSynthesizedConstructor() {
    // TODO(ahe): Get rid of this method. Add the
    // SynthesizedConstructorElement to [constructors] if empty when
    // [resolve] is called.
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

  bool isInterface() => false;
  bool isNative() => nativeName != null;
  SourceString nativeName;
}

class Elements {
  static bool isLocal(Element element) {
    return ((element !== null)
            && !element.isInstanceMember()
            && !isStaticOrTopLevelField(element)
            && !isStaticOrTopLevelFunction(element)
            && (element.kind === ElementKind.VARIABLE ||
                element.kind === ElementKind.PARAMETER ||
                element.kind === ElementKind.FUNCTION));
  }

  static bool isInstanceField(Element element) {
    return (element !== null)
           && element.isInstanceMember()
           && (element.kind === ElementKind.FIELD
               || element.kind === ElementKind.GETTER
               || element.kind === ElementKind.SETTER);
  }

  static bool isStaticOrTopLevel(Element element) {
    return (element != null)
           && !element.isInstanceMember()
           && element.enclosingElement !== null
           && (element.enclosingElement.kind == ElementKind.CLASS ||
               element.enclosingElement.kind == ElementKind.COMPILATION_UNIT ||
               element.enclosingElement.kind == ElementKind.LIBRARY);
  }

  static bool isStaticOrTopLevelField(Element element) {
    return isStaticOrTopLevel(element)
           && (element.kind === ElementKind.FIELD
               || element.kind === ElementKind.GETTER
               || element.kind === ElementKind.SETTER);
  }

  static bool isStaticOrTopLevelFunction(Element element) {
    return isStaticOrTopLevel(element)
           && (element.kind === ElementKind.FUNCTION);
  }

  static bool isInstanceMethod(Element element) {
    return (element != null)
           && element.isInstanceMember()
           && (element.kind === ElementKind.FUNCTION);
  }

  static bool isInstanceSend(Send send, TreeElements elements) {
    Element element = elements[send];
    if (element === null) return !isClosureSend(send, elements);
    return isInstanceMethod(element) || isInstanceField(element);
  }

  static bool isClosureSend(Send send, TreeElements elements) {
    if (send.isPropertyAccess) return false;
    if (send.receiver !== null) return false;
    Element element = elements[send];
    // (o)() or foo()().
    if (element === null && send.selector.asIdentifier() === null) return true;
    if (element === null) return false;
    // foo() with foo a local or a parameter.
    return isLocal(element);
  }

  static SourceString constructConstructorName(SourceString receiver,
                                               SourceString selector) {
    String r = receiver.slowToString();
    String s = selector.slowToString();
    return new SourceString('$r\$$s');
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
    else {
      throw new Exception('Unhandled selector: ${selector.slowToString()}');
    }
    return new SourceString('$receiver\$$str');
  }
}


class LabelElement extends Element {
  final Identifier label;
  final String labelName;
  final StatementElement target;
  bool isBreakTarget = false;
  bool isContinueTarget = false;
  LabelElement(Identifier label, this.labelName, this.target,
               Element enclosingElement)
      : this.label = label,
        super(label.source, ElementKind.LABEL, enclosingElement);

  void setBreakTarget() {
    isBreakTarget = true;
    target.isBreakTarget = true;
  }
  void setContinueTarget() {
    isContinueTarget = true;
    target.isContinueTarget = true;
  }

  bool get isTarget() => isBreakTarget || isContinueTarget;
  Node parseNode(DiagnosticListener l) => label;
}

// Represents a reference to a statement, either a label or the
// default target of a break or continue.
class StatementElement extends Element {
  final Statement statement;
  Link<LabelElement> labels = const EmptyLink<LabelElement>();
  bool isBreakTarget = false;
  bool isContinueTarget = false;

  StatementElement(this.statement, Element enclosingElement)
      : super(const SourceString(""), ElementKind.STATEMENT, enclosingElement);
  bool get isTarget() => isBreakTarget || isContinueTarget;

  LabelElement addLabel(Identifier label, String labelName) {
    LabelElement result = new LabelElement(label, labelName, this,
                                           enclosingElement);
    labels = labels.prepend(result);
    return result;
  }

  Node parseNode(DiagnosticListener l) => statement;
}
