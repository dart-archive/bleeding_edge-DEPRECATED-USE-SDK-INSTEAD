// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library engine.incremental_resolver;

import 'dart:collection';
import 'dart:math' as math;

import 'package:analyzer/src/services/lint.dart';

import 'ast.dart';
import 'element.dart';
import 'engine.dart';
import 'error.dart';
import 'error_verifier.dart';
import 'incremental_logger.dart' show logger, LoggingTimer;
import 'java_engine.dart';
import 'parser.dart';
import 'resolver.dart';
import 'scanner.dart';
import 'source.dart';
import 'utilities_dart.dart';


/**
 * If `true`, an attempt to resolve API-changing modifications is made.
 */
bool _resolveApiChanges = false;


/**
 * This method is used to enable/disable API-changing modifications resolution.
 */
void set test_resolveApiChanges(bool value) {
  _resolveApiChanges = value;
}


/**
 * Instances of the class [DeclarationMatcher] determine whether the element
 * model defined by a given AST structure matches an existing element model.
 */
class DeclarationMatcher extends RecursiveAstVisitor {
  /**
   * The libary containing the AST nodes being visited.
   */
  LibraryElement _enclosingLibrary;

  /**
   * The compilation unit containing the AST nodes being visited.
   */
  CompilationUnitElement _enclosingUnit;

  /**
   * The function type alias containing the AST nodes being visited, or `null` if we are not
   * in the scope of a function type alias.
   */
  FunctionTypeAliasElement _enclosingAlias;

  /**
   * The class containing the AST nodes being visited, or `null` if we are not
   * in the scope of a class.
   */
  ClassElementImpl _enclosingClass;

  /**
   * The parameter containing the AST nodes being visited, or `null` if we are not in the
   * scope of a parameter.
   */
  ParameterElement _enclosingParameter;

  FieldDeclaration _enclosingFieldNode = null;
  bool _inTopLevelVariableDeclaration = false;

  /**
   * Is `true` if the current class declaration has a constructor.
   */
  bool _hasConstructor = false;

  /**
   * A set containing all of the elements in the element model that were defined by the old AST node
   * corresponding to the AST node being visited.
   */
  HashSet<Element> _allElements = new HashSet<Element>();

  /**
   * A set containing all of the elements were defined in the old element model,
   * but are not defined in the new element model.
   */
  HashSet<Element> _removedElements = new HashSet<Element>();

  /**
   * A set containing all of the elements are defined in the new element model,
   * but were not defined in the old element model.
   */
  HashSet<Element> _addedElements = new HashSet<Element>();

  /**
   * Determines how elements model corresponding to the given [node] differs
   * from the [element].
   */
  DeclarationMatchKind matches(AstNode node, Element element) {
    logger.enter('match $element @ ${element.nameOffset}');
    try {
      _captureEnclosingElements(element);
      _gatherElements(element);
      node.accept(this);
    } on _DeclarationMismatchException catch (exception) {
      return DeclarationMatchKind.MISMATCH;
    } finally {
      logger.exit();
    }
    // no API changes
    if (_removedElements.isEmpty && _addedElements.isEmpty) {
      return DeclarationMatchKind.MATCH;
    }
    // simple API change
    logger.log('_removedElements: $_removedElements');
    logger.log('_addedElements: $_addedElements');
    _removedElements.forEach(_removeElement);
    if (_removedElements.length <= 1 && _addedElements.length == 1) {
      return DeclarationMatchKind.MISMATCH_OK;
    }
    // something more complex
    return DeclarationMatchKind.MISMATCH;
  }

  @override
  visitBlockFunctionBody(BlockFunctionBody node) {
    // ignore bodies
  }

  @override
  visitClassDeclaration(ClassDeclaration node) {
    String name = node.name.name;
    ClassElement element = _findElement(_enclosingUnit.types, name);
    _enclosingClass = element;
    _processElement(element);
    _assertSameAnnotations(node, element);
    _assertSameTypeParameters(node.typeParameters, element.typeParameters);
    // check for missing clauses
    if (node.extendsClause == null) {
      _assertTrue(element.supertype.name == 'Object');
    }
    if (node.implementsClause == null) {
      _assertTrue(element.interfaces.isEmpty);
    }
    if (node.withClause == null) {
      _assertTrue(element.mixins.isEmpty);
    }
    // process clauses and members
    _hasConstructor = false;
    super.visitClassDeclaration(node);
    // process default constructor
    if (!_hasConstructor) {
      ConstructorElement constructor = element.unnamedConstructor;
      _processElement(constructor);
      if (!constructor.isSynthetic) {
        _assertEquals(constructor.parameters.length, 0);
      }
    }
  }

  @override
  visitClassTypeAlias(ClassTypeAlias node) {
    String name = node.name.name;
    ClassElement element = _findElement(_enclosingUnit.types, name);
    _enclosingClass = element;
    _processElement(element);
    _assertSameTypeParameters(node.typeParameters, element.typeParameters);
    _processElement(element.unnamedConstructor);
    super.visitClassTypeAlias(node);
  }

  @override
  visitCompilationUnit(CompilationUnit node) {
    _processElement(_enclosingUnit);
    super.visitCompilationUnit(node);
  }

  @override
  visitConstructorDeclaration(ConstructorDeclaration node) {
    _hasConstructor = true;
    SimpleIdentifier constructorName = node.name;
    ConstructorElementImpl element = constructorName == null ?
        _enclosingClass.unnamedConstructor :
        _enclosingClass.getNamedConstructor(constructorName.name);
    _processElement(element);
    _assertCompatibleParameters(node.parameters, element.parameters);
    // TODO(scheglov) debug null Location
    if (element != null) {
      if (element.context == null || element.source == null) {
        logger.log(
            'Bad constructor element $element for $node in ${node.parent}');
      }
    }
    // matches, update the existing element
    ExecutableElement newElement = node.element;
    node.element = element;
    _setLocalElements(element, newElement);
  }

  @override
  visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    String name = node.name.name;
    FieldElement element = _findElement(_enclosingClass.fields, name);
    _processElement(element);
  }

  @override
  visitEnumDeclaration(EnumDeclaration node) {
    String name = node.name.name;
    ClassElement element = _findElement(_enclosingUnit.enums, name);
    _enclosingClass = element;
    _processElement(element);
    _assertTrue(element.isEnum);
    super.visitEnumDeclaration(node);
  }

  @override
  visitExportDirective(ExportDirective node) {
    String uri = _getStringValue(node.uri);
    if (uri != null) {
      ExportElement element =
          _findUriReferencedElement(_enclosingLibrary.exports, uri);
      _processElement(element);
      _assertCombinators(node.combinators, element.combinators);
    }
  }

  @override
  visitExpressionFunctionBody(ExpressionFunctionBody node) {
    // ignore bodies
  }

  @override
  visitExtendsClause(ExtendsClause node) {
    _assertSameType(node.superclass, _enclosingClass.supertype);
  }

  @override
  visitFieldDeclaration(FieldDeclaration node) {
    _enclosingFieldNode = node;
    try {
      super.visitFieldDeclaration(node);
    } finally {
      _enclosingFieldNode = null;
    }
  }

  @override
  visitFunctionDeclaration(FunctionDeclaration node) {
    // prepare element name
    String name = node.name.name;
    if (node.isSetter) {
      name += '=';
    }
    // prepare element
    Token property = node.propertyKeyword;
    ExecutableElementImpl element;
    if (property == null) {
      element = _findElement(_enclosingUnit.functions, name);
    } else {
      element = _findElement(_enclosingUnit.accessors, name);
    }
    // process element
    _processElement(element);
    _assertSameAnnotations(node, element);
    _assertFalse(element.isSynthetic);
    _assertSameType(node.returnType, element.returnType);
    _assertCompatibleParameters(
        node.functionExpression.parameters,
        element.parameters);
    _assertBodyModifiers(node.functionExpression.body, element);
    // matches, update the existing element
    ExecutableElement newElement = node.element;
    node.name.staticElement = element;
    node.functionExpression.element = element;
    _setLocalElements(element, newElement);
  }

  @override
  visitFunctionTypeAlias(FunctionTypeAlias node) {
    String name = node.name.name;
    FunctionTypeAliasElement element =
        _findElement(_enclosingUnit.functionTypeAliases, name);
    _processElement(element);
    _assertSameTypeParameters(node.typeParameters, element.typeParameters);
    _assertSameType(node.returnType, element.returnType);
    _assertCompatibleParameters(node.parameters, element.parameters);
  }

  @override
  visitImplementsClause(ImplementsClause node) {
    List<TypeName> nodes = node.interfaces;
    List<InterfaceType> types = _enclosingClass.interfaces;
    _assertSameTypes(nodes, types);
  }

  @override
  visitImportDirective(ImportDirective node) {
    String uri = _getStringValue(node.uri);
    if (uri != null) {
      ImportElement element =
          _findUriReferencedElement(_enclosingLibrary.imports, uri);
      _processElement(element);
      // match the prefix
      SimpleIdentifier prefixNode = node.prefix;
      PrefixElement prefixElement = element.prefix;
      if (prefixNode == null) {
        _assertNull(prefixElement);
      } else {
        _assertNotNull(prefixElement);
        _assertEquals(prefixNode.name, prefixElement.name);
      }
      // match combinators
      _assertCombinators(node.combinators, element.combinators);
    }
  }

  @override
  visitMethodDeclaration(MethodDeclaration node) {
    // prepare element name
    String name = node.name.name;
    if (name == TokenType.MINUS.lexeme &&
        node.parameters.parameters.length == 0) {
      name = "unary-";
    }
    if (node.isSetter) {
      name += '=';
    }
    // prepare element
    Token property = node.propertyKeyword;
    ExecutableElementImpl element;
    if (property == null) {
      element = _findElement(_enclosingClass.methods, name);
    } else {
      element = _findElement(_enclosingClass.accessors, name);
    }
    // process element
    ExecutableElement newElement = node.element;
    try {
      _assertNotNull(element);
      _assertSameAnnotations(node, element);
      _assertEquals(node.isStatic, element.isStatic);
      _assertSameType(node.returnType, element.returnType);
      _assertCompatibleParameters(node.parameters, element.parameters);
      _assertBodyModifiers(node.body, element);
      _removedElements.remove(element);
      // matches, update the existing element
      node.name.staticElement = element;
      _setLocalElements(element, newElement);
    } on _DeclarationMismatchException catch (e) {
      _addedElements.add(newElement);
      _removeElement(element);
      // add new element
      if (newElement is MethodElement) {
        List<MethodElement> methods = _enclosingClass.methods;
        methods.add(newElement);
        _enclosingClass.methods = methods;
      } else {
        List<PropertyAccessorElement> accessors = _enclosingClass.accessors;
        accessors.add(newElement);
        _enclosingClass.accessors = accessors;
      }
    }
  }

  @override
  visitPartDirective(PartDirective node) {
    String uri = _getStringValue(node.uri);
    if (uri != null) {
      CompilationUnitElement element =
          _findUriReferencedElement(_enclosingLibrary.parts, uri);
      _processElement(element);
    }
    super.visitPartDirective(node);
  }

  @override
  visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    _inTopLevelVariableDeclaration = true;
    try {
      super.visitTopLevelVariableDeclaration(node);
    } finally {
      _inTopLevelVariableDeclaration = false;
    }
  }

  @override
  visitVariableDeclaration(VariableDeclaration node) {
    // prepare variable
    String name = node.name.name;
    PropertyInducingElement element;
    if (_inTopLevelVariableDeclaration) {
      element = _findElement(_enclosingUnit.topLevelVariables, name);
    } else {
      element = _findElement(_enclosingClass.fields, name);
    }
    // verify
    PropertyInducingElement newElement = node.name.staticElement;
    _processElement(element);
    _assertSameAnnotations(node, element);
    _assertEquals(node.isConst, element.isConst);
    _assertEquals(node.isFinal, element.isFinal);
    if (_enclosingFieldNode != null) {
      _assertEquals(_enclosingFieldNode.isStatic, element.isStatic);
    }
    _assertSameType(
        (node.parent as VariableDeclarationList).type,
        element.type);
    // matches, restore the existing element
    node.name.staticElement = element;
    if (element is VariableElementImpl) {
      (element as VariableElementImpl).initializer = newElement.initializer;
    }
  }

  @override
  visitWithClause(WithClause node) {
    List<TypeName> nodes = node.mixinTypes;
    List<InterfaceType> types = _enclosingClass.mixins;
    _assertSameTypes(nodes, types);
  }

  /**
   * Asserts that [body] has async / generator modifiers compatible with the
   * given [element].
   */
  void _assertBodyModifiers(FunctionBody body, ExecutableElementImpl element) {
    _assertEquals(body.isSynchronous, element.isSynchronous);
    _assertEquals(body.isGenerator, element.isGenerator);
  }

  void _assertCombinators(List<Combinator> nodeCombinators,
      List<NamespaceCombinator> elementCombinators) {
    // prepare shown/hidden names in the element
    Set<String> showNames = new Set<String>();
    Set<String> hideNames = new Set<String>();
    for (NamespaceCombinator combinator in elementCombinators) {
      if (combinator is ShowElementCombinator) {
        showNames.addAll(combinator.shownNames);
      } else if (combinator is HideElementCombinator) {
        hideNames.addAll(combinator.hiddenNames);
      }
    }
    // match combinators with the node
    for (Combinator combinator in nodeCombinators) {
      if (combinator is ShowCombinator) {
        for (SimpleIdentifier nameNode in combinator.shownNames) {
          String name = nameNode.name;
          _assertTrue(showNames.remove(name));
        }
      } else if (combinator is HideCombinator) {
        for (SimpleIdentifier nameNode in combinator.hiddenNames) {
          String name = nameNode.name;
          _assertTrue(hideNames.remove(name));
        }
      }
    }
    _assertTrue(showNames.isEmpty);
    _assertTrue(hideNames.isEmpty);
  }

  void _assertCompatibleParameter(FormalParameter node,
      ParameterElement element) {
    _assertEquals(node.kind, element.parameterKind);
    if (node.kind == ParameterKind.NAMED) {
      _assertEquals(node.identifier.name, element.name);
    }
    // check parameter type specific properties
    if (node is DefaultFormalParameter) {
      Expression nodeDefault = node.defaultValue;
      if (nodeDefault == null) {
        _assertNull(element.defaultValueCode);
      } else {
        _assertEquals(nodeDefault.toSource(), element.defaultValueCode);
      }
    } else if (node is FieldFormalParameter) {
      _assertTrue(element.isInitializingFormal);
    } else if (node is FunctionTypedFormalParameter) {
      _assertTrue(element.type is FunctionType);
      FunctionType elementType = element.type;
      _assertCompatibleParameters(node.parameters, element.parameters);
      _assertSameType(node.returnType, elementType.returnType);
    } else if (node is SimpleFormalParameter) {
      _assertSameType(node.type, element.type);
    }
  }

  void _assertCompatibleParameters(FormalParameterList nodes,
      List<ParameterElement> elements) {
    if (nodes == null) {
      return _assertEquals(elements.length, 0);
    }
    List<FormalParameter> parameters = nodes.parameters;
    int length = parameters.length;
    _assertEquals(length, elements.length);
    for (int i = 0; i < length; i++) {
      _assertCompatibleParameter(parameters[i], elements[i]);
    }
  }

  void _assertEquals(Object a, Object b) {
    if (a != b) {
      throw new _DeclarationMismatchException();
    }
  }

  void _assertFalse(bool condition) {
    if (condition) {
      throw new _DeclarationMismatchException();
    }
  }

  void _assertNotNull(Object object) {
    if (object == null) {
      throw new _DeclarationMismatchException();
    }
  }

  void _assertNull(Object object) {
    if (object != null) {
      throw new _DeclarationMismatchException();
    }
  }

  void _assertSameAnnotation(Annotation node, ElementAnnotation annotation) {
    Element element = annotation.element;
    if (element is ConstructorElement) {
      _assertTrue(node.name is SimpleIdentifier);
      _assertNull(node.constructorName);
      TypeName nodeType = new TypeName(node.name, null);
      _assertSameType(nodeType, element.returnType);
      // TODO(scheglov) validate arguments
    }
    if (element is PropertyAccessorElement) {
      _assertTrue(node.name is SimpleIdentifier);
      String nodeName = node.name.name;
      String elementName = element.displayName;
      _assertEquals(nodeName, elementName);
    }
  }

  void _assertSameAnnotations(AnnotatedNode node, Element element) {
    List<Annotation> nodeAnnotaitons = node.metadata;
    List<ElementAnnotation> elementAnnotations = element.metadata;
    int length = nodeAnnotaitons.length;
    _assertEquals(elementAnnotations.length, length);
    for (int i = 0; i < length; i++) {
      _assertSameAnnotation(nodeAnnotaitons[i], elementAnnotations[i]);
    }
  }

  void _assertSameType(TypeName node, DartType type) {
    // no return type == dynamic
    if (node == null) {
      return _assertTrue(type == null || type.isDynamic);
    }
    if (type == null) {
      return _assertTrue(false);
    }
    // prepare name
    Identifier nameIdentifier = node.name;
    if (nameIdentifier is PrefixedIdentifier) {
      nameIdentifier = (nameIdentifier as PrefixedIdentifier).identifier;
    }
    String nodeName = nameIdentifier.name;
    // check specific type kinds
    if (type is ParameterizedType) {
      _assertEquals(nodeName, type.name);
      // check arguments
      TypeArgumentList nodeArgumentList = node.typeArguments;
      List<DartType> typeArguments = type.typeArguments;
      if (nodeArgumentList == null) {
        // Node doesn't have type arguments, so all type arguments of the
        // element must be "dynamic".
        for (DartType typeArgument in typeArguments) {
          _assertTrue(typeArgument.isDynamic);
        }
      } else {
        List<TypeName> nodeArguments = nodeArgumentList.arguments;
        _assertSameTypes(nodeArguments, typeArguments);
      }
    } else if (type is TypeParameterType) {
      _assertEquals(nodeName, type.name);
      // TODO(scheglov) it should be possible to rename type parameters
    } else if (type.isVoid) {
      _assertEquals(nodeName, 'void');
    } else if (type.isDynamic) {
      _assertEquals(nodeName, 'dynamic');
    } else {
      // TODO(scheglov) support other types
      logger.log('node: $node type: $type  type.type: ${type.runtimeType}');
      _assertTrue(false);
    }
  }

  void _assertSameTypeParameter(TypeParameter node,
      TypeParameterElement element) {
    _assertSameType(node.bound, element.bound);
  }

  void _assertSameTypeParameters(TypeParameterList nodesList,
      List<TypeParameterElement> elements) {
    if (nodesList == null) {
      return _assertEquals(elements.length, 0);
    }
    List<TypeParameter> nodes = nodesList.typeParameters;
    int length = nodes.length;
    _assertEquals(length, elements.length);
    for (int i = 0; i < length; i++) {
      _assertSameTypeParameter(nodes[i], elements[i]);
    }
  }

  void _assertSameTypes(List<TypeName> nodes, List<DartType> types) {
    int length = nodes.length;
    _assertEquals(length, types.length);
    for (int i = 0; i < length; i++) {
      _assertSameType(nodes[i], types[i]);
    }
  }

  void _assertTrue(bool condition) {
    if (!condition) {
      throw new _DeclarationMismatchException();
    }
  }

  /**
   * Given that the comparison is to begin with the given [element], capture
   * the enclosing elements that might be used while performing the comparison.
   */
  void _captureEnclosingElements(Element element) {
    Element parent =
        element is CompilationUnitElement ? element : element.enclosingElement;
    while (parent != null) {
      if (parent is CompilationUnitElement) {
        _enclosingUnit = parent as CompilationUnitElement;
        _enclosingLibrary = element.library;
      } else if (parent is ClassElement) {
        if (_enclosingClass == null) {
          _enclosingClass = parent as ClassElement;
        }
      } else if (parent is FunctionTypeAliasElement) {
        if (_enclosingAlias == null) {
          _enclosingAlias = parent as FunctionTypeAliasElement;
        }
      } else if (parent is ParameterElement) {
        if (_enclosingParameter == null) {
          _enclosingParameter = parent as ParameterElement;
        }
      }
      parent = parent.enclosingElement;
    }
  }

  void _gatherElements(Element element) {
    _ElementsGatherer gatherer = new _ElementsGatherer(this);
    element.accept(gatherer);
    // TODO(scheglov) what if a change in a directive?
    if (identical(element, _enclosingLibrary.definingCompilationUnit)) {
      gatherer.addElements(_enclosingLibrary.imports);
      gatherer.addElements(_enclosingLibrary.exports);
      gatherer.addElements(_enclosingLibrary.parts);
    }
  }

  void _processElement(Element element) {
    _assertNotNull(element);
    if (!_allElements.contains(element)) {
      throw new _DeclarationMismatchException();
    }
    _removedElements.remove(element);
  }

  void _removeElement(Element element) {
    if (element != null) {
      Element enclosingElement = element.enclosingElement;
      if (element is MethodElement) {
        ClassElement classElement = enclosingElement;
        _removeIdenticalElement(classElement.methods, element);
      } else if (element is PropertyAccessorElement) {
        if (enclosingElement is ClassElement) {
          _removeIdenticalElement(enclosingElement.accessors, element);
        }
        if (enclosingElement is CompilationUnitElement) {
          _removeIdenticalElement(enclosingElement.accessors, element);
        }
      }
    }
  }

  /**
   * Return the [Element] in [elements] with the given [name].
   */
  static Element _findElement(List<Element> elements, String name) {
    for (Element element in elements) {
      if (element.name == name) {
        return element;
      }
    }
    return null;
  }

  /**
   * Return the [UriReferencedElement] from [elements] with the given [uri], or
   * `null` if there is no such element.
   */
  static UriReferencedElement
      _findUriReferencedElement(List<UriReferencedElement> elements, String uri) {
    for (UriReferencedElement element in elements) {
      if (element.uri == uri) {
        return element;
      }
    }
    return null;
  }

  /**
   * Return the value of [literal], or `null` if the string is not a constant
   * string without any string interpolation.
   */
  static String _getStringValue(StringLiteral literal) {
    if (literal is StringInterpolation) {
      return null;
    }
    return literal.stringValue;
  }

  /**
   * Removes the first element identical to the given [element] from [elements].
   */
  static void _removeIdenticalElement(List elements, Object element) {
    int length = elements.length;
    for (int i = 0; i < length; i++) {
      if (identical(elements[i], element)) {
        elements.removeAt(i);
        return;
      }
    }
  }

  static void _setLocalElements(ExecutableElementImpl to,
      ExecutableElement from) {
    to.functions = from.functions;
    to.labels = from.labels;
    to.localVariables = from.localVariables;
    to.parameters = from.parameters;
  }
}


/**
 * Describes how declarations match an existing elements model.
 */
class DeclarationMatchKind {
  /**
   * Complete match, no API changes.
   */
  static const MATCH = const DeclarationMatchKind('MATCH');

  /**
   * Has API changes that we might be able to resolve incrementally.
   */
  static const MISMATCH_OK = const DeclarationMatchKind('MISMATCH_OK');

  /**
   * Has API changes that we cannot resolve incrementally.
   */
  static const MISMATCH = const DeclarationMatchKind('MISMATCH');

  final String name;

  const DeclarationMatchKind(this.name);

  @override
  String toString() => name;
}


/**
 * Instances of the class [IncrementalResolver] resolve the smallest portion of
 * an AST structure that we currently know how to resolve.
 */
class IncrementalResolver {
  /**
   * The element of the compilation unit being resolved.
   */
  final CompilationUnitElement _definingUnit;

  /**
   * The context the compilation unit being resolved in.
   */
  AnalysisContextImpl _context;

  /**
   * The object used to access the types from the core library.
   */
  TypeProvider _typeProvider;

  /**
   * The element for the library containing the compilation unit being resolved.
   */
  LibraryElement _definingLibrary;

  /**
   * The [DartEntry] corresponding to the source being resolved.
   */
  DartEntry entry;

  /**
   * The source representing the compilation unit being visited.
   */
  Source _source;

  /**
   * The source representing the library of the compilation unit being visited.
   */
  Source _librarySource;

  /**
   * The offset of the changed contents.
   */
  final int _updateOffset;

  /**
   * The end of the changed contents in the old unit.
   */
  final int _updateEndOld;

  /**
   * The end of the changed contents in the new unit.
   */
  final int _updateEndNew;

  int _updateDelta;

  RecordingErrorListener errorListener = new RecordingErrorListener();
  ResolutionContext _resolutionContext;

  List<AnalysisError> _resolveErrors = AnalysisError.NO_ERRORS;
  List<AnalysisError> _verifyErrors = AnalysisError.NO_ERRORS;
  List<AnalysisError> _lints = AnalysisError.NO_ERRORS;

  /**
   * Initialize a newly created incremental resolver to resolve a node in the
   * given source in the given library.
   */
  IncrementalResolver(this._definingUnit, this._updateOffset,
      this._updateEndOld, this._updateEndNew) {
    _updateDelta = _updateEndNew - _updateEndOld;
    _definingLibrary = _definingUnit.library;
    _librarySource = _definingLibrary.source;
    _source = _definingUnit.source;
    _context = _definingUnit.context;
    _typeProvider = _context.typeProvider;
    entry = _context.getReadableSourceEntryOrNull(_source);
  }

  /**
   * Resolve [node], reporting any errors or warnings to the given listener.
   *
   * [node] - the root of the AST structure to be resolved.
   *
   * Returns `true` if resolution was successful.
   */
  bool resolve(AstNode node) {
    logger.enter('resolve: $_definingUnit');
    try {
      AstNode rootNode = _findResolutionRoot(node);
      _prepareResolutionContext(rootNode);
      // update elements
      _updateElementNameOffsets();
      _buildElements(rootNode);
      if (!_canBeIncrementallyResolved(rootNode)) {
        return false;
      }
      // resolve
      _resolveReferences(rootNode);
      // verify
      _verify(rootNode);
      _context.invalidateLibraryHints(_librarySource);
      _generateLints(rootNode);
      // update entry errors
      _updateEntry();
      // OK
      return true;
    } finally {
      logger.exit();
    }
  }

  void _buildElements(AstNode node) {
    LoggingTimer timer = logger.startTimer();
    try {
      ElementHolder holder = new ElementHolder();
      ElementBuilder builder = new ElementBuilder(holder);
      if (_resolutionContext.enclosingClassDeclaration != null) {
        builder.visitClassDeclarationIncrementally(
            _resolutionContext.enclosingClassDeclaration);
      }
      node.accept(builder);
    } finally {
      timer.stop('build elements');
    }
  }

  /**
   * Return `true` if [node] does not have element model changes, or these
   * changes can be incrementally propagated.
   */
  bool _canBeIncrementallyResolved(AstNode node) {
    // If we are replacing the whole declaration, this means that its signature
    // is changed. It might be an API change, or not.
    //
    // If, for example, a required parameter is changed, it is not an API
    // change, but we want to find the existing corresponding Element in the
    // enclosing one, set it for the node and update as needed.
    //
    // If, for example, the name of a method is changed, it is an API change,
    // we need to know the old Element and the new Element. Again, we need to
    // check the whole enclosing Element.
    if (node is Declaration) {
      node = node.parent;
    }
    Element element = _getElement(node);
    DeclarationMatcher matcher = new DeclarationMatcher();
    DeclarationMatchKind matchKind = matcher.matches(node, element);
    if (matchKind == DeclarationMatchKind.MATCH) {
      return true;
    }
    // mismatch that cannot be incrementally fixed
    return false;
  }

  /**
   * Return `true` if the given node can be resolved independently of any other
   * nodes.
   *
   * *Note*: This method needs to be kept in sync with
   * [ScopeBuilder.ContextBuilder].
   *
   * [node] - the node being tested.
   */
  bool _canBeResolved(AstNode node) =>
      node is ClassDeclaration ||
          node is ClassTypeAlias ||
          node is CompilationUnit ||
          node is ConstructorDeclaration ||
          node is FunctionDeclaration ||
          node is FunctionTypeAlias ||
          node is MethodDeclaration ||
          node is TopLevelVariableDeclaration;

  /**
   * Starting at [node], find the smallest AST node that can be resolved
   * independently of any other nodes. Return the node that was found.
   *
   * [node] - the node at which the search is to begin
   *
   * Throws [AnalysisException] if there is no such node.
   */
  AstNode _findResolutionRoot(AstNode node) {
    while (node != null) {
      if (_canBeResolved(node)) {
        return node;
      }
      node = node.parent;
    }
    throw new AnalysisException("Cannot resolve node: no resolvable node");
  }

  void _generateLints(AstNode node) {
    LoggingTimer timer = logger.startTimer();
    try {
      RecordingErrorListener errorListener = new RecordingErrorListener();
      CompilationUnit unit = node.getAncestor((n) => n is CompilationUnit);
      LintGenerator lintGenerator =
          new LintGenerator(<CompilationUnit>[unit], errorListener);
      lintGenerator.generate();
      _lints = errorListener.getErrorsForSource(_source);
    } finally {
      timer.stop('generate lints');
    }
  }

  /**
   * Return the element defined by [node], or `null` if the node does not
   * define an element.
   */
  Element _getElement(AstNode node) {
    if (node is Declaration) {
      return node.element;
    } else if (node is CompilationUnit) {
      return node.element;
    }
    return null;
  }

  void _prepareResolutionContext(AstNode node) {
    if (_resolutionContext == null) {
      _resolutionContext =
          ResolutionContextBuilder.contextFor(node, errorListener);
    }
  }

  _resolveReferences(AstNode node) {
    LoggingTimer timer = logger.startTimer();
    try {
      _prepareResolutionContext(node);
      Scope scope = _resolutionContext.scope;
      // resolve types
      {
        TypeResolverVisitor visitor = new TypeResolverVisitor.con3(
            _definingLibrary,
            _source,
            _typeProvider,
            scope,
            errorListener);
        node.accept(visitor);
      }
      // resolve variables
      {
        VariableResolverVisitor visitor = new VariableResolverVisitor.con2(
            _definingLibrary,
            _source,
            _typeProvider,
            scope,
            errorListener);
        node.accept(visitor);
      }
      // resolve references
      {
        ResolverVisitor visitor = new ResolverVisitor.con3(
            _definingLibrary,
            _source,
            _typeProvider,
            scope,
            errorListener);
        if (_resolutionContext.enclosingClassDeclaration != null) {
          visitor.visitClassDeclarationIncrementally(
              _resolutionContext.enclosingClassDeclaration);
        }
        if (node is Comment) {
          visitor.resolveOnlyCommentInFunctionBody = true;
          node = node.parent;
        }
        visitor.initForIncrementalResolution();
        node.accept(visitor);
      }
      // remember errors
      _resolveErrors = errorListener.getErrorsForSource(_source);
    } finally {
      timer.stop('resolve references');
    }
  }

  void _shiftEntryErrors() {
    _shiftErrors(DartEntry.RESOLUTION_ERRORS);
    _shiftErrors(DartEntry.VERIFICATION_ERRORS);
    _shiftErrors(DartEntry.HINTS);
    _shiftErrors(DartEntry.LINTS);
  }

  void _shiftErrors(DataDescriptor<List<AnalysisError>> descriptor) {
    List<AnalysisError> errors =
        entry.getValueInLibrary(descriptor, _librarySource);
    for (AnalysisError error in errors) {
      int errorOffset = error.offset;
      if (errorOffset > _updateOffset) {
        error.offset += _updateDelta;
      }
    }
  }

  void _updateElementNameOffsets() {
    LoggingTimer timer = logger.startTimer();
    try {
      _definingUnit.accept(
          new _ElementNameOffsetUpdater(_updateOffset, _updateDelta));
    } finally {
      timer.stop('update element offsets');
    }
  }

  void _updateEntry() {
    {
      List<AnalysisError> oldErrors =
          entry.getValueInLibrary(DartEntry.RESOLUTION_ERRORS, _librarySource);
      List<AnalysisError> errors = _updateErrors(oldErrors, _resolveErrors);
      entry.setValueInLibrary(
          DartEntry.RESOLUTION_ERRORS,
          _librarySource,
          errors);
    }
    {
      List<AnalysisError> oldErrors =
          entry.getValueInLibrary(DartEntry.VERIFICATION_ERRORS, _librarySource);
      List<AnalysisError> errors = _updateErrors(oldErrors, _verifyErrors);
      entry.setValueInLibrary(
          DartEntry.VERIFICATION_ERRORS,
          _librarySource,
          errors);
    }
    entry.setValueInLibrary(DartEntry.LINTS, _librarySource, _lints);
  }

  List<AnalysisError> _updateErrors(List<AnalysisError> oldErrors,
      List<AnalysisError> newErrors) {
    List<AnalysisError> errors = new List<AnalysisError>();
    // add updated old errors
    for (AnalysisError error in oldErrors) {
      int errorOffset = error.offset;
      if (errorOffset < _updateOffset) {
        errors.add(error);
      } else if (errorOffset > _updateEndOld) {
        error.offset += _updateDelta;
        errors.add(error);
      }
    }
    // add new errors
    for (AnalysisError error in newErrors) {
      int errorOffset = error.offset;
      if (errorOffset > _updateOffset && errorOffset < _updateEndNew) {
        errors.add(error);
      }
    }
    // done
    return errors;
  }

  void _verify(AstNode node) {
    LoggingTimer timer = logger.startTimer();
    try {
      RecordingErrorListener errorListener = new RecordingErrorListener();
      ErrorReporter errorReporter = new ErrorReporter(errorListener, _source);
      ErrorVerifier errorVerifier = new ErrorVerifier(
          errorReporter,
          _definingLibrary,
          _typeProvider,
          new InheritanceManager(_definingLibrary));
      if (_resolutionContext.enclosingClassDeclaration != null) {
        errorVerifier.visitClassDeclarationIncrementally(
            _resolutionContext.enclosingClassDeclaration);
      }
      node.accept(errorVerifier);
      _verifyErrors = errorListener.getErrorsForSource(_source);
    } finally {
      timer.stop('verify');
    }
  }
}


class PoorMansIncrementalResolver {
  final TypeProvider _typeProvider;
  final Source _unitSource;
  final DartEntry _entry;
  final CompilationUnit _oldUnit;
  CompilationUnitElement _unitElement;

  int _updateOffset;
  int _updateDelta;
  int _updateEndOld;
  int _updateEndNew;

  List<AnalysisError> _newScanErrors = <AnalysisError>[];
  List<AnalysisError> _newParseErrors = <AnalysisError>[];

  PoorMansIncrementalResolver(this._typeProvider, this._unitSource, this._entry,
      this._oldUnit, bool resolveApiChanges) {
    _resolveApiChanges = resolveApiChanges;
  }

  /**
   * Attempts to update [_oldUnit] to the state corresponding to [newCode].
   * Returns `true` if success, or `false` otherwise.
   * The [_oldUnit] might be damaged.
   */
  bool resolve(String newCode) {
    logger.enter('diff/resolve $_unitSource');
    try {
      // prepare old unit
      if (!_areCurlyBracketsBalanced(_oldUnit.beginToken)) {
        logger.log('Unbalanced number of curly brackets in the old unit.');
        return false;
      }
      _unitElement = _oldUnit.element;
      // prepare new unit
      CompilationUnit newUnit = _parseUnit(newCode);
      if (!_areCurlyBracketsBalanced(newUnit.beginToken)) {
        logger.log('Unbalanced number of curly brackets in the new unit.');
        return false;
      }
      // find difference
      _TokenPair firstPair =
          _findFirstDifferentToken(_oldUnit.beginToken, newUnit.beginToken);
      _TokenPair lastPair =
          _findLastDifferentToken(_oldUnit.endToken, newUnit.endToken);
      if (firstPair != null && lastPair != null) {
        int firstOffsetOld = firstPair.oldToken.offset;
        int firstOffsetNew = firstPair.newToken.offset;
        int lastOffsetOld = lastPair.oldToken.end;
        int lastOffsetNew = lastPair.newToken.end;
        int beginOffsetOld = math.min(firstOffsetOld, lastOffsetOld);
        int endOffsetOld = math.max(firstOffsetOld, lastOffsetOld);
        int beginOffsetNew = math.min(firstOffsetNew, lastOffsetNew);
        int endOffsetNew = math.max(firstOffsetNew, lastOffsetNew);
        // check for a whitespace only change
        if (identical(lastPair.oldToken, firstPair.oldToken) &&
            identical(lastPair.newToken, firstPair.newToken)) {
          _updateOffset = beginOffsetOld - 1;
          _updateEndOld = endOffsetOld;
          _updateEndNew = endOffsetNew;
          _updateDelta = newUnit.length - _oldUnit.length;
          // A Dart documentation comment change.
          if (firstPair.kind == _TokenDifferenceKind.COMMENT_DOC) {
            bool success = _resolveComment(_oldUnit, newUnit, firstPair);
            logger.log('Documentation comment resolved: $success');
            return success;
          }
          // A pure whitespace change.
          if (firstPair.kind == _TokenDifferenceKind.OFFSET) {
            logger.log('Whitespace change.');
            _shiftTokens(firstPair.oldToken);
            {
              IncrementalResolver incrementalResolver = new IncrementalResolver(
                  _unitElement,
                  _updateOffset,
                  _updateEndOld,
                  _updateEndNew);
              incrementalResolver._updateElementNameOffsets();
              incrementalResolver._shiftEntryErrors();
            }
            _updateEntry();
            logger.log('Success.');
            return true;
          }
          // fall-through, end-of-line comment
        }
        // Find nodes covering the "old" and "new" token ranges.
        AstNode oldNode =
            _findNodeCovering(_oldUnit, beginOffsetOld, endOffsetOld);
        AstNode newNode =
            _findNodeCovering(newUnit, beginOffsetNew, endOffsetNew);
        logger.log(() => 'oldNode: $oldNode');
        logger.log(() => 'newNode: $newNode');
        // Try to find the smallest common node, a FunctionBody currently.
        {
          List<AstNode> oldParents = _getParents(oldNode);
          List<AstNode> newParents = _getParents(newNode);
          int length = math.min(oldParents.length, newParents.length);
          bool found = false;
          for (int i = 0; i < length; i++) {
            AstNode oldParent = oldParents[i];
            AstNode newParent = newParents[i];
            if (oldParent is FunctionDeclaration &&
                newParent is FunctionDeclaration ||
                oldParent is MethodDeclaration && newParent is MethodDeclaration ||
                oldParent is ConstructorDeclaration && newParent is ConstructorDeclaration) {
              oldNode = oldParent;
              newNode = newParent;
              found = true;
            }
            if (oldParent is FunctionBody && newParent is FunctionBody) {
              oldNode = oldParent;
              newNode = newParent;
              found = true;
              break;
            }
          }
          if (!found) {
            logger.log('Failure: no enclosing function body or executable.');
            return false;
          }
        }
        logger.log(() => 'oldNode: $oldNode');
        logger.log(() => 'newNode: $newNode');
        // prepare update range
        _updateOffset = oldNode.offset;
        _updateEndOld = oldNode.end;
        _updateEndNew = newNode.end;
        _updateDelta = _updateEndNew - _updateEndOld;
        // replace node
        NodeReplacer.replace(oldNode, newNode);
        // update token references
        {
          Token oldBeginToken = _getBeginTokenNotComment(oldNode);
          Token newBeginToken = _getBeginTokenNotComment(newNode);
          if (oldBeginToken.previous.type == TokenType.EOF) {
            _oldUnit.beginToken = newBeginToken;
          } else {
            oldBeginToken.previous.setNext(newBeginToken);
          }
          newNode.endToken.setNext(oldNode.endToken.next);
          _shiftTokens(oldNode.endToken.next);
        }
        // perform incremental resolution
        IncrementalResolver incrementalResolver = new IncrementalResolver(
            _unitElement,
            _updateOffset,
            _updateEndOld,
            _updateEndNew);
        bool success = incrementalResolver.resolve(newNode);
        // check if success
        if (!success) {
          logger.log('Failure: element model changed.');
          return false;
        }
        // update DartEntry
        _updateEntry();
        logger.log('Success.');
        return true;
      }
    } catch (e, st) {
      logger.log(e);
      logger.log(st);
      logger.log('Failure: exception.');
    } finally {
      logger.exit();
    }
    return false;
  }

  CompilationUnit _parseUnit(String code) {
    LoggingTimer timer = logger.startTimer();
    try {
      Token token = _scan(code);
      RecordingErrorListener errorListener = new RecordingErrorListener();
      Parser parser = new Parser(_unitSource, errorListener);
      CompilationUnit unit = parser.parseCompilationUnit(token);
      _newParseErrors = errorListener.errors;
      return unit;
    } finally {
      timer.stop('parse');
    }
  }

  /**
   * Attempts to resolve a documentation comment change.
   * Returns `true` if success.
   */
  bool _resolveComment(CompilationUnit oldUnit, CompilationUnit newUnit,
      _TokenPair firstPair) {
    Token oldToken = firstPair.oldToken;
    Token newToken = firstPair.newToken;
    CommentToken oldComments = oldToken.precedingComments;
    CommentToken newComments = newToken.precedingComments;
    if (oldComments == null || newComments == null) {
      return false;
    }
    // find nodes
    int offset = oldComments.offset;
    logger.log('offset: $offset');
    Comment oldComment = _findNodeCovering(oldUnit, offset, offset);
    Comment newComment = _findNodeCovering(newUnit, offset, offset);
    logger.log('oldComment.beginToken: ${oldComment.beginToken}');
    logger.log('newComment.beginToken: ${newComment.beginToken}');
    _updateOffset = oldToken.offset - 1;
    // update token references
    _shiftTokens(firstPair.oldToken);
    _setPrecedingComments(oldToken, newComment.tokens.first);
    // replace node
    NodeReplacer.replace(oldComment, newComment);
    // update elements
    IncrementalResolver incrementalResolver = new IncrementalResolver(
        _unitElement,
        _updateOffset,
        _updateEndOld,
        _updateEndNew);
    incrementalResolver._updateElementNameOffsets();
    incrementalResolver._shiftEntryErrors();
    _updateEntry();
    // resolve references in the comment
    incrementalResolver._resolveReferences(newComment);
    // OK
    return true;
  }

  Token _scan(String code) {
    RecordingErrorListener errorListener = new RecordingErrorListener();
    CharSequenceReader reader = new CharSequenceReader(code);
    Scanner scanner = new Scanner(_unitSource, reader, errorListener);
    Token token = scanner.tokenize();
    _newScanErrors = errorListener.errors;
    return token;
  }

  void _shiftTokens(Token token) {
    while (token != null) {
      if (token.offset > _updateOffset) {
        token.offset += _updateDelta;
      }
      // comments
      _shiftTokens(token.precedingComments);
      if (token is DocumentationCommentToken) {
        for (Token reference in token.references) {
          _shiftTokens(reference);
        }
      }
      // next
      if (token.type == TokenType.EOF) {
        break;
      }
      token = token.next;
    }
  }

  void _updateEntry() {
    _entry.setValue(DartEntry.SCAN_ERRORS, _newScanErrors);
    _entry.setValue(DartEntry.PARSE_ERRORS, _newParseErrors);
  }

  /**
   * Checks if [token] has a balanced number of open and closed curly brackets.
   */
  static bool _areCurlyBracketsBalanced(Token token) {
    int numOpen = _getTokenCount(token, TokenType.OPEN_CURLY_BRACKET);
    int numOpen2 =
        _getTokenCount(token, TokenType.STRING_INTERPOLATION_EXPRESSION);
    int numClosed = _getTokenCount(token, TokenType.CLOSE_CURLY_BRACKET);
    return numOpen + numOpen2 == numClosed;
  }

  static _TokenDifferenceKind _compareToken(Token oldToken, Token newToken,
      int delta, bool forComment) {
    while (true) {
      if (oldToken == null && newToken == null) {
        return null;
      }
      if (oldToken == null || newToken == null) {
        return _TokenDifferenceKind.CONTENT;
      }
      if (oldToken.type != newToken.type) {
        return _TokenDifferenceKind.CONTENT;
      }
      if (oldToken.lexeme != newToken.lexeme) {
        return _TokenDifferenceKind.CONTENT;
      }
      if (newToken.offset - oldToken.offset != delta) {
        return _TokenDifferenceKind.OFFSET;
      }
      // continue if comment tokens are being checked
      if (!forComment) {
        break;
      }
      oldToken = oldToken.next;
      newToken = newToken.next;
    }
    return null;
  }

  static _TokenPair _findFirstDifferentToken(Token oldToken, Token newToken) {
    while (true) {
      if (oldToken.type == TokenType.EOF && newToken.type == TokenType.EOF) {
        return null;
      }
      if (oldToken.type == TokenType.EOF || newToken.type == TokenType.EOF) {
        return new _TokenPair(_TokenDifferenceKind.CONTENT, oldToken, newToken);
      }
      // compare comments
      {
        Token oldComment = oldToken.precedingComments;
        Token newComment = newToken.precedingComments;
        if (_compareToken(oldComment, newComment, 0, true) != null) {
          _TokenDifferenceKind diffKind = _TokenDifferenceKind.COMMENT;
          if (oldComment is DocumentationCommentToken ||
              newComment is DocumentationCommentToken) {
            diffKind = _TokenDifferenceKind.COMMENT_DOC;
          }
          return new _TokenPair(diffKind, oldToken, newToken);
        }
      }
      // compare tokens
      _TokenDifferenceKind diffKind =
          _compareToken(oldToken, newToken, 0, false);
      if (diffKind != null) {
        return new _TokenPair(diffKind, oldToken, newToken);
      }
      // next tokens
      oldToken = oldToken.next;
      newToken = newToken.next;
    }
    // no difference
    return null;
  }

  static _TokenPair _findLastDifferentToken(Token oldToken, Token newToken) {
    int delta = newToken.offset - oldToken.offset;
    while (oldToken.previous != oldToken && newToken.previous != newToken) {
      // compare tokens
      _TokenDifferenceKind diffKind =
          _compareToken(oldToken, newToken, delta, false);
      if (diffKind != null) {
        return new _TokenPair(diffKind, oldToken.next, newToken.next);
      }
      // compare comments
      {
        Token oldComment = oldToken.precedingComments;
        Token newComment = newToken.precedingComments;
        if (_compareToken(oldComment, newComment, delta, true) != null) {
          _TokenDifferenceKind diffKind = _TokenDifferenceKind.COMMENT;
          if (oldComment is DocumentationCommentToken ||
              newComment is DocumentationCommentToken) {
            diffKind = _TokenDifferenceKind.COMMENT_DOC;
          }
          return new _TokenPair(diffKind, oldToken, newToken);
        }
      }
      // next tokens
      oldToken = oldToken.previous;
      newToken = newToken.previous;
    }
    return null;
  }

  static AstNode _findNodeCovering(AstNode root, int offset, int end) {
    NodeLocator nodeLocator = new NodeLocator.con2(offset, end);
    return nodeLocator.searchWithin(root);
  }

  static Token _getBeginTokenNotComment(AstNode node) {
    Token oldBeginToken = node.beginToken;
    if (oldBeginToken is CommentToken) {
      oldBeginToken = (oldBeginToken as CommentToken).parent;
    }
    return oldBeginToken;
  }

  static List<AstNode> _getParents(AstNode node) {
    List<AstNode> parents = <AstNode>[];
    while (node != null) {
      parents.insert(0, node);
      node = node.parent;
    }
    return parents;
  }


  /**
   * Returns number of tokens with the given [type].
   */
  static int _getTokenCount(Token token, TokenType type) {
    int count = 0;
    while (token.type != TokenType.EOF) {
      if (token.type == type) {
        count++;
      }
      token = token.next;
    }
    return count;
  }

  /**
   * Set the given [comment] as a "precedingComments" for [parent].
   */
  static void _setPrecedingComments(Token parent, CommentToken comment) {
    if (parent is BeginTokenWithComment) {
      parent.precedingComments = comment;
    } else if (parent is KeywordTokenWithComment) {
      parent.precedingComments = comment;
    } else if (parent is StringTokenWithComment) {
      parent.precedingComments = comment;
    } else if (parent is TokenWithComment) {
      parent.precedingComments = comment;
    } else {
      Type parentType = parent != null ? parent.runtimeType : null;
      throw new AnalysisException('Uknown parent token type: $parentType');
    }
  }
}


/**
 * The context to resolve an [AstNode] in.
 */
class ResolutionContext {
  CompilationUnitElement enclosingUnit;
  ClassDeclaration enclosingClassDeclaration;
  ClassElement enclosingClass;
  Scope scope;
}


/**
 * Instances of the class [ResolutionContextBuilder] build the context for a
 * given node in an AST structure. At the moment, this class only handles
 * top-level and class-level declarations.
 */
class ResolutionContextBuilder {
  /**
   * The listener to which analysis errors will be reported.
   */
  final AnalysisErrorListener _errorListener;

  /**
   * The class containing the enclosing [CompilationUnitElement].
   */
  CompilationUnitElement _enclosingUnit;

  /**
   * The class containing the enclosing [ClassDeclaration], or `null` if we are
   * not in the scope of a class.
   */
  ClassDeclaration _enclosingClassDeclaration;

  /**
   * The class containing the enclosing [ClassElement], or `null` if we are not
   * in the scope of a class.
   */
  ClassElement _enclosingClass;

  /**
   * Initialize a newly created scope builder to generate a scope that will
   * report errors to the given listener.
   */
  ResolutionContextBuilder(this._errorListener);

  Scope _scopeFor(AstNode node) {
    if (node is CompilationUnit) {
      return _scopeForAstNode(node);
    }
    AstNode parent = node.parent;
    if (parent == null) {
      throw new AnalysisException(
          "Cannot create scope: node is not part of a CompilationUnit");
    }
    return _scopeForAstNode(parent);
  }

  /**
   * Return the scope in which the given AST structure should be resolved.
   *
   * *Note:* This method needs to be kept in sync with
   * [IncrementalResolver.canBeResolved].
   *
   * [node] - the root of the AST structure to be resolved.
   *
   * Throws [AnalysisException] if the AST structure has not been resolved or
   * is not part of a [CompilationUnit]
   */
  Scope _scopeForAstNode(AstNode node) {
    if (node is CompilationUnit) {
      return _scopeForCompilationUnit(node);
    }
    AstNode parent = node.parent;
    if (parent == null) {
      throw new AnalysisException(
          "Cannot create scope: node is not part of a CompilationUnit");
    }
    Scope scope = _scopeForAstNode(parent);
    if (node is ClassDeclaration) {
      _enclosingClassDeclaration = node;
      _enclosingClass = node.element;
      if (_enclosingClass == null) {
        throw new AnalysisException(
            "Cannot build a scope for an unresolved class");
      }
      scope = new ClassScope(
          new TypeParameterScope(scope, _enclosingClass),
          _enclosingClass);
    } else if (node is ClassTypeAlias) {
      ClassElement element = node.element;
      if (element == null) {
        throw new AnalysisException(
            "Cannot build a scope for an unresolved class type alias");
      }
      scope = new ClassScope(new TypeParameterScope(scope, element), element);
    } else if (node is ConstructorDeclaration) {
      ConstructorElement element = node.element;
      if (element == null) {
        throw new AnalysisException(
            "Cannot build a scope for an unresolved constructor");
      }
      FunctionScope functionScope = new FunctionScope(scope, element);
      functionScope.defineParameters();
      scope = functionScope;
    } else if (node is FunctionDeclaration) {
      ExecutableElement element = node.element;
      if (element == null) {
        throw new AnalysisException(
            "Cannot build a scope for an unresolved function");
      }
      FunctionScope functionScope = new FunctionScope(scope, element);
      functionScope.defineParameters();
      scope = functionScope;
    } else if (node is FunctionTypeAlias) {
      scope = new FunctionTypeScope(scope, node.element);
    } else if (node is MethodDeclaration) {
      ExecutableElement element = node.element;
      if (element == null) {
        throw new AnalysisException(
            "Cannot build a scope for an unresolved method");
      }
      FunctionScope functionScope = new FunctionScope(scope, element);
      functionScope.defineParameters();
      scope = functionScope;
    }
    return scope;
  }

  Scope _scopeForCompilationUnit(CompilationUnit node) {
    _enclosingUnit = node.element;
    if (_enclosingUnit == null) {
      throw new AnalysisException(
          "Cannot create scope: compilation unit is not resolved");
    }
    LibraryElement libraryElement = _enclosingUnit.library;
    if (libraryElement == null) {
      throw new AnalysisException(
          "Cannot create scope: compilation unit is not part of a library");
    }
    return new LibraryScope(libraryElement, _errorListener);
  }

  /**
   * Return the context in which the given AST structure should be resolved.
   *
   * [node] - the root of the AST structure to be resolved.
   * [errorListener] - the listener to which analysis errors will be reported.
   *
   * Throws [AnalysisException] if the AST structure has not been resolved or
   * is not part of a [CompilationUnit]
   */
  static ResolutionContext contextFor(AstNode node,
      AnalysisErrorListener errorListener) {
    if (node == null) {
      throw new AnalysisException("Cannot create context: node is null");
    }
    // build scope
    ResolutionContextBuilder builder =
        new ResolutionContextBuilder(errorListener);
    Scope scope = builder._scopeFor(node);
    // prepare context
    ResolutionContext context = new ResolutionContext();
    context.scope = scope;
    context.enclosingUnit = builder._enclosingUnit;
    context.enclosingClassDeclaration = builder._enclosingClassDeclaration;
    context.enclosingClass = builder._enclosingClass;
    return context;
  }
}


/**
 * Instances of the class [_DeclarationMismatchException] represent an exception
 * that is thrown when the element model defined by a given AST structure does
 * not match an existing element model.
 */
class _DeclarationMismatchException {
}


class _ElementNameOffsetUpdater extends GeneralizingElementVisitor {
  final int updateOffset;
  final int updateDelta;

  _ElementNameOffsetUpdater(this.updateOffset, this.updateDelta);

  @override
  visitElement(Element element) {
    int nameOffset = element.nameOffset;
    if (nameOffset > updateOffset) {
      (element as ElementImpl).nameOffset = nameOffset + updateDelta;
    }
    super.visitElement(element);
  }
}


class _ElementsGatherer extends GeneralizingElementVisitor {
  final DeclarationMatcher matcher;

  _ElementsGatherer(this.matcher);

  void addElements(List<Element> elements) {
    for (Element element in elements) {
      if (!element.isSynthetic) {
        _addElement(element);
      }
    }
  }

  @override
  visitElement(Element element) {
    _addElement(element);
    super.visitElement(element);
  }

  @override
  visitExecutableElement(ExecutableElement element) {
    _addElement(element);
  }

  @override
  visitParameterElement(ParameterElement element) {
  }

  @override
  visitPropertyAccessorElement(PropertyAccessorElement element) {
    if (!element.isSynthetic) {
      _addElement(element);
    }
    // Don't visit children (such as synthetic setter parameters).
  }

  @override
  visitPropertyInducingElement(PropertyInducingElement element) {
    if (!element.isSynthetic) {
      _addElement(element);
    }
    // Don't visit children (such as property accessors).
  }

  @override
  visitTypeParameterElement(TypeParameterElement element) {
  }

  void _addElement(Element element) {
    if (element != null) {
      matcher._allElements.add(element);
      matcher._removedElements.add(element);
    }
  }
}


/**
 * Describes how two [Token]s are different.
 */
class _TokenDifferenceKind {
  static const COMMENT = const _TokenDifferenceKind('COMMENT');
  static const COMMENT_DOC = const _TokenDifferenceKind('COMMENT_DOC');
  static const CONTENT = const _TokenDifferenceKind('CONTENT');
  static const OFFSET = const _TokenDifferenceKind('OFFSET');

  final String name;

  const _TokenDifferenceKind(this.name);

  @override
  String toString() => name;
}


class _TokenPair {
  final _TokenDifferenceKind kind;
  final Token oldToken;
  final Token newToken;
  _TokenPair(this.kind, this.oldToken, this.newToken);
}
