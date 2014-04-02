// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// This code was auto-generated, is not intended to be edited, and is subject to
// significant change. Please see the README file for more information.

library engine.element;

import 'dart:collection';
import 'java_core.dart';
import 'java_engine.dart';
import 'utilities_collection.dart';
import 'source.dart';
import 'scanner.dart' show Keyword;
import 'ast.dart';
import 'sdk.dart' show DartSdk;
import 'html.dart' show XmlAttributeNode, XmlTagNode;
import 'engine.dart' show AnalysisContext;
import 'constant.dart' show EvaluationResultImpl;
import 'utilities_dart.dart';

/**
 * The interface `ClassElement` defines the behavior of elements that represent a class.
 */
abstract class ClassElement implements Element {
  /**
   * Return an array containing all of the accessors (getters and setters) declared in this class.
   *
   * @return the accessors declared in this class
   */
  List<PropertyAccessorElement> get accessors;

  /**
   * Return an array containing all the supertypes defined for this class and its supertypes. This
   * includes superclasses, mixins and interfaces.
   *
   * @return all the supertypes of this class, including mixins
   */
  List<InterfaceType> get allSupertypes;

  /**
   * Return an array containing all of the constructors declared in this class.
   *
   * @return the constructors declared in this class
   */
  List<ConstructorElement> get constructors;

  /**
   * Return the field (synthetic or explicit) defined in this class that has the given name, or
   * `null` if this class does not define a field with the given name.
   *
   * @param fieldName the name of the field to be returned
   * @return the field with the given name that is defined in this class
   */
  FieldElement getField(String fieldName);

  /**
   * Return an array containing all of the fields declared in this class.
   *
   * @return the fields declared in this class
   */
  List<FieldElement> get fields;

  /**
   * Return the element representing the getter with the given name that is declared in this class,
   * or `null` if this class does not declare a getter with the given name.
   *
   * @param getterName the name of the getter to be returned
   * @return the getter declared in this class with the given name
   */
  PropertyAccessorElement getGetter(String getterName);

  /**
   * Return an array containing all of the interfaces that are implemented by this class.
   *
   * <b>Note:</b> Because the element model represents the state of the code, it is possible for it
   * to be semantically invalid. In particular, it is not safe to assume that the inheritance
   * structure of a class does not contain a cycle. Clients that traverse the inheritance structure
   * must explicitly guard against infinite loops.
   *
   * @return the interfaces that are implemented by this class
   */
  List<InterfaceType> get interfaces;

  /**
   * Return the element representing the method with the given name that is declared in this class,
   * or `null` if this class does not declare a method with the given name.
   *
   * @param methodName the name of the method to be returned
   * @return the method declared in this class with the given name
   */
  MethodElement getMethod(String methodName);

  /**
   * Return an array containing all of the methods declared in this class.
   *
   * @return the methods declared in this class
   */
  List<MethodElement> get methods;

  /**
   * Return an array containing all of the mixins that are applied to the class being extended in
   * order to derive the superclass of this class.
   *
   * <b>Note:</b> Because the element model represents the state of the code, it is possible for it
   * to be semantically invalid. In particular, it is not safe to assume that the inheritance
   * structure of a class does not contain a cycle. Clients that traverse the inheritance structure
   * must explicitly guard against infinite loops.
   *
   * @return the mixins that are applied to derive the superclass of this class
   */
  List<InterfaceType> get mixins;

  /**
   * Return the named constructor declared in this class with the given name, or `null` if
   * this class does not declare a named constructor with the given name.
   *
   * @param name the name of the constructor to be returned
   * @return the element representing the specified constructor
   */
  ConstructorElement getNamedConstructor(String name);

  /**
   * Return the resolved [ClassDeclaration] node that declares this [ClassElement].
   *
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   *
   * @return the resolved [ClassDeclaration], not `null`.
   */
  @override
  ClassDeclaration get node;

  /**
   * Return the element representing the setter with the given name that is declared in this class,
   * or `null` if this class does not declare a setter with the given name.
   *
   * @param setterName the name of the getter to be returned
   * @return the setter declared in this class with the given name
   */
  PropertyAccessorElement getSetter(String setterName);

  /**
   * Return the superclass of this class, or `null` if the class represents the class
   * 'Object'. All other classes will have a non-`null` superclass. If the superclass was not
   * explicitly declared then the implicit superclass 'Object' will be returned.
   *
   * <b>Note:</b> Because the element model represents the state of the code, it is possible for it
   * to be semantically invalid. In particular, it is not safe to assume that the inheritance
   * structure of a class does not contain a cycle. Clients that traverse the inheritance structure
   * must explicitly guard against infinite loops.
   *
   * @return the superclass of this class
   */
  InterfaceType get supertype;

  /**
   * Return an array containing all of the toolkit specific objects associated with this class. The
   * array will be empty if the class does not have any toolkit specific objects or if the
   * compilation unit containing the class has not yet had toolkit references resolved.
   *
   * @return the toolkit objects associated with this class
   */
  List<ToolkitObjectElement> get toolkitObjects;

  /**
   * Return the type defined by the class.
   *
   * @return the type defined by the class
   */
  InterfaceType get type;

  /**
   * Return an array containing all of the type parameters declared for this class.
   *
   * @return the type parameters declared for this class
   */
  List<TypeParameterElement> get typeParameters;

  /**
   * Return the unnamed constructor declared in this class, or `null` if this class does not
   * declare an unnamed constructor but does declare named constructors. The returned constructor
   * will be synthetic if this class does not declare any constructors, in which case it will
   * represent the default constructor for the class.
   *
   * @return the unnamed constructor defined in this class
   */
  ConstructorElement get unnamedConstructor;

  /**
   * Return `true` if this class or its superclass declares a non-final instance field.
   *
   * @return `true` if this class or its superclass declares a non-final instance field
   */
  bool get hasNonFinalField;

  /**
   * Return `true` if this class has reference to super (so, for example, cannot be used as a
   * mixin).
   *
   * @return `true` if this class has reference to super
   */
  bool get hasReferenceToSuper;

  /**
   * Return `true` if this class declares a static member.
   *
   * @return `true` if this class declares a static member
   */
  bool get hasStaticMember;

  /**
   * Return `true` if this class is abstract. A class is abstract if it has an explicit
   * `abstract` modifier. Note, that this definition of <i>abstract</i> is different from
   * <i>has unimplemented members</i>.
   *
   * @return `true` if this class is abstract
   */
  bool get isAbstract;

  /**
   * Return `true` if this class [isProxy], or if it inherits the proxy annotation
   * from a supertype.
   *
   * @return `true` if this class defines or inherits a proxy
   */
  bool get isOrInheritsProxy;

  /**
   * Return `true` if this element has an annotation of the form '@proxy'.
   *
   * @return `true` if this element defines a proxy
   */
  bool get isProxy;

  /**
   * Return `true` if this class is defined by a typedef construct.
   *
   * @return `true` if this class is defined by a typedef construct
   */
  bool get isTypedef;

  /**
   * Return `true` if this class can validly be used as a mixin when defining another class.
   * The behavior of this method is defined by the Dart Language Specification in section 9:
   * <blockquote>It is a compile-time error if a declared or derived mixin refers to super. It is a
   * compile-time error if a declared or derived mixin explicitly declares a constructor. It is a
   * compile-time error if a mixin is derived from a class whose superclass is not
   * Object.</blockquote>
   *
   * @return `true` if this class can validly be used as a mixin
   */
  bool get isValidMixin;

  /**
   * Return the element representing the getter that results from looking up the given getter in
   * this class with respect to the given library, or `null` if the look up fails. The
   * behavior of this method is defined by the Dart Language Specification in section 12.15.1:
   * <blockquote>The result of looking up getter (respectively setter) <i>m</i> in class <i>C</i>
   * with respect to library <i>L</i> is:
   * * If <i>C</i> declares an instance getter (respectively setter) named <i>m</i> that is
   * accessible to <i>L</i>, then that getter (respectively setter) is the result of the lookup.
   * Otherwise, if <i>C</i> has a superclass <i>S</i>, then the result of the lookup is the result
   * of looking up getter (respectively setter) <i>m</i> in <i>S</i> with respect to <i>L</i>.
   * Otherwise, we say that the lookup has failed.
   * </blockquote>
   *
   * @param getterName the name of the getter being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given getter in this class with respect to the given
   *         library
   */
  PropertyAccessorElement lookUpGetter(String getterName, LibraryElement library);

  /**
   * Return the element representing the method that results from looking up the given method in
   * this class with respect to the given library, or `null` if the look up fails. The
   * behavior of this method is defined by the Dart Language Specification in section 12.15.1:
   * <blockquote> The result of looking up method <i>m</i> in class <i>C</i> with respect to library
   * <i>L</i> is:
   * * If <i>C</i> declares an instance method named <i>m</i> that is accessible to <i>L</i>, then
   * that method is the result of the lookup. Otherwise, if <i>C</i> has a superclass <i>S</i>, then
   * the result of the lookup is the result of looking up method <i>m</i> in <i>S</i> with respect
   * to <i>L</i>. Otherwise, we say that the lookup has failed.
   * </blockquote>
   *
   * @param methodName the name of the method being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given method in this class with respect to the given
   *         library
   */
  MethodElement lookUpMethod(String methodName, LibraryElement library);

  /**
   * Return the element representing the setter that results from looking up the given setter in
   * this class with respect to the given library, or `null` if the look up fails. The
   * behavior of this method is defined by the Dart Language Specification in section 12.16:
   * <blockquote> The result of looking up getter (respectively setter) <i>m</i> in class <i>C</i>
   * with respect to library <i>L</i> is:
   * * If <i>C</i> declares an instance getter (respectively setter) named <i>m</i> that is
   * accessible to <i>L</i>, then that getter (respectively setter) is the result of the lookup.
   * Otherwise, if <i>C</i> has a superclass <i>S</i>, then the result of the lookup is the result
   * of looking up getter (respectively setter) <i>m</i> in <i>S</i> with respect to <i>L</i>.
   * Otherwise, we say that the lookup has failed.
   * </blockquote>
   *
   * @param setterName the name of the setter being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given setter in this class with respect to the given
   *         library
   */
  PropertyAccessorElement lookUpSetter(String setterName, LibraryElement library);
}

/**
 * The interface `ClassMemberElement` defines the behavior of elements that are contained
 * within a [ClassElement].
 */
abstract class ClassMemberElement implements Element {
  /**
   * Return the type in which this member is defined.
   *
   * @return the type in which this member is defined
   */
  @override
  ClassElement get enclosingElement;

  /**
   * Return `true` if this element is a static element. A static element is an element that is
   * not associated with a particular instance, but rather with an entire library or class.
   *
   * @return `true` if this executable element is a static element
   */
  bool get isStatic;
}

/**
 * The interface `CompilationUnitElement` defines the behavior of elements representing a
 * compilation unit.
 */
abstract class CompilationUnitElement implements Element, UriReferencedElement {
  /**
   * Return an array containing all of the top-level accessors (getters and setters) contained in
   * this compilation unit.
   *
   * @return the top-level accessors contained in this compilation unit
   */
  List<PropertyAccessorElement> get accessors;

  /**
   * Return an array containing all of the Angular views defined in this compilation unit. The array
   * will be empty if the element does not have any Angular views or if the compilation unit has not
   * yet had toolkit references resolved.
   *
   * @return the Angular views defined in this compilation unit.
   */
  List<AngularViewElement> get angularViews;

  /**
   * Return the library in which this compilation unit is defined.
   *
   * @return the library in which this compilation unit is defined
   */
  @override
  LibraryElement get enclosingElement;

  /**
   * Return an array containing all of the top-level functions contained in this compilation unit.
   *
   * @return the top-level functions contained in this compilation unit
   */
  List<FunctionElement> get functions;

  /**
   * Return an array containing all of the function type aliases contained in this compilation unit.
   *
   * @return the function type aliases contained in this compilation unit
   */
  List<FunctionTypeAliasElement> get functionTypeAliases;

  /**
   * Return the resolved [CompilationUnit] node that declares this element.
   *
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   *
   * @return the resolved [CompilationUnit], not `null`.
   */
  @override
  CompilationUnit get node;

  /**
   * Return an array containing all of the top-level variables contained in this compilation unit.
   *
   * @return the top-level variables contained in this compilation unit
   */
  List<TopLevelVariableElement> get topLevelVariables;

  /**
   * Return the class defined in this compilation unit that has the given name, or `null` if
   * this compilation unit does not define a class with the given name.
   *
   * @param className the name of the class to be returned
   * @return the class with the given name that is defined in this compilation unit
   */
  ClassElement getType(String className);

  /**
   * Return an array containing all of the classes contained in this compilation unit.
   *
   * @return the classes contained in this compilation unit
   */
  List<ClassElement> get types;
}

/**
 * The interface `ConstructorElement` defines the behavior of elements representing a
 * constructor or a factory method defined within a type.
 */
abstract class ConstructorElement implements ClassMemberElement, ExecutableElement {
  /**
   * Return the resolved [ConstructorDeclaration] node that declares this
   * [ConstructorElement] .
   *
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   *
   * @return the resolved [ConstructorDeclaration], not `null`.
   */
  @override
  ConstructorDeclaration get node;

  /**
   * Return the constructor to which this constructor is redirecting, or `null` if this constructor
   * does not redirect to another constructor or if the library containing this constructor has
   * not yet been resolved.
   *
   * @return the constructor to which this constructor is redirecting
   */
  ConstructorElement get redirectedConstructor;

  /**
   * Return `true` if this constructor is a const constructor.
   *
   * @return `true` if this constructor is a const constructor
   */
  bool get isConst;

  /**
   * Return `true` if this constructor can be used as a default constructor - unnamed and has
   * no required parameters.
   *
   * @return `true` if this constructor can be used as a default constructor.
   */
  bool get isDefaultConstructor;

  /**
   * Return `true` if this constructor represents a factory constructor.
   *
   * @return `true` if this constructor represents a factory constructor
   */
  bool get isFactory;
}

/**
 * The interface `Element` defines the behavior common to all of the elements in the element
 * model. Generally speaking, the element model is a semantic model of the program that represents
 * things that are declared with a name and hence can be referenced elsewhere in the code.
 *
 * There are two exceptions to the general case. First, there are elements in the element model that
 * are created for the convenience of various kinds of analysis but that do not have any
 * corresponding declaration within the source code. Such elements are marked as being
 * <i>synthetic</i>. Examples of synthetic elements include
 * * default constructors in classes that do not define any explicit constructors,
 * * getters and setters that are induced by explicit field declarations,
 * * fields that are induced by explicit declarations of getters and setters, and
 * * functions representing the initialization expression for a variable.
 *
 * Second, there are elements in the element model that do not have a name. These correspond to
 * unnamed functions and exist in order to more accurately represent the semantic structure of the
 * program.
 */
abstract class Element {
  /**
   * An Unicode right arrow.
   */
  static final String RIGHT_ARROW = " \u2192 ";

  /**
   * A comparator that can be used to sort elements by their name offset. Elements with a smaller
   * offset will be sorted to be before elements with a larger name offset.
   */
  static final Comparator<Element> SORT_BY_OFFSET = (Element firstElement, Element secondElement) => firstElement.nameOffset - secondElement.nameOffset;

  /**
   * Use the given visitor to visit this element.
   *
   * @param visitor the visitor that will visit this element
   * @return the value returned by the visitor as a result of visiting this element
   */
  accept(ElementVisitor visitor);

  /**
   * Return the documentation comment for this element as it appears in the original source
   * (complete with the beginning and ending delimiters), or `null` if this element does not
   * have a documentation comment associated with it. This can be a long-running operation if the
   * information needed to access the comment is not cached.
   *
   * @return this element's documentation comment
   * @throws AnalysisException if the documentation comment could not be determined because the
   *           analysis could not be performed
   */
  String computeDocumentationComment();

  /**
   * Return the element of the given class that most immediately encloses this element, or
   * `null` if there is no enclosing element of the given class.
   *
   * @param elementClass the class of the element to be returned
   * @return the element that encloses this element
   */
  Element getAncestor(Predicate<Element> predicate);

  /**
   * Return the analysis context in which this element is defined.
   *
   * @return the analysis context in which this element is defined
   */
  AnalysisContext get context;

  /**
   * Return the display name of this element, or `null` if this element does not have a name.
   *
   * In most cases the name and the display name are the same. Differences though are cases such as
   * setters where the name of some setter `set f(x)` is `f=`, instead of `f`.
   *
   * @return the display name of this element
   */
  String get displayName;

  /**
   * Return the element that either physically or logically encloses this element. This will be
   * `null` if this element is a library because libraries are the top-level elements in the
   * model.
   *
   * @return the element that encloses this element
   */
  Element get enclosingElement;

  /**
   * Return the kind of element that this is.
   *
   * @return the kind of this element
   */
  ElementKind get kind;

  /**
   * Return the library that contains this element. This will be the element itself if it is a
   * library element. This will be `null` if this element is an HTML file because HTML files
   * are not contained in libraries.
   *
   * @return the library that contains this element
   */
  LibraryElement get library;

  /**
   * Return an object representing the location of this element in the element model. The object can
   * be used to locate this element at a later time.
   *
   * @return the location of this element in the element model
   */
  ElementLocation get location;

  /**
   * Return an array containing all of the metadata associated with this element. The array will be
   * empty if the element does not have any metadata or if the library containing this element has
   * not yet been resolved.
   *
   * @return the metadata associated with this element
   */
  List<ElementAnnotation> get metadata;

  /**
   * Return the name of this element, or `null` if this element does not have a name.
   *
   * @return the name of this element
   */
  String get name;

  /**
   * Return the offset of the name of this element in the file that contains the declaration of this
   * element, or `-1` if this element is synthetic, does not have a name, or otherwise does
   * not have an offset.
   *
   * @return the offset of the name of this element
   */
  int get nameOffset;

  /**
   * Return the resolved [AstNode] node that declares this [Element].
   *
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @return the resolved [AstNode], maybe `null` if [Element] is synthetic or
   *         isn't contained in a compilation unit, such as a [LibraryElement].
   */
  AstNode get node;

  /**
   * Return the source that contains this element, or `null` if this element is not contained
   * in a source.
   *
   * @return the source that contains this element
   */
  Source get source;

  /**
   * Return the resolved [CompilationUnit] that declares this [Element].
   *
   * This method is expensive, because resolved AST might have been already evicted from cache, so
   * parsing and resolving will be performed.
   *
   * @return the resolved [CompilationUnit], maybe `null` if synthetic [Element].
   */
  CompilationUnit get unit;

  /**
   * Return `true` if this element, assuming that it is within scope, is accessible to code in
   * the given library. This is defined by the Dart Language Specification in section 3.2:
   * <blockquote> A declaration <i>m</i> is accessible to library <i>L</i> if <i>m</i> is declared
   * in <i>L</i> or if <i>m</i> is public. </blockquote>
   *
   * @param library the library in which a possible reference to this element would occur
   * @return `true` if this element is accessible to code in the given library
   */
  bool isAccessibleIn(LibraryElement library);

  /**
   * Return `true` if this element has an annotation of the form '@deprecated' or
   * '@Deprecated('..')'.
   *
   * @return `true` if this element is deprecated
   */
  bool get isDeprecated;

  /**
   * Return `true` if this element has an annotation of the form '@override'.
   *
   * @return `true` if this element is overridden
   */
  bool get isOverride;

  /**
   * Return `true` if this element is private. Private elements are visible only within the
   * library in which they are declared.
   *
   * @return `true` if this element is private
   */
  bool get isPrivate;

  /**
   * Return `true` if this element is public. Public elements are visible within any library
   * that imports the library in which they are declared.
   *
   * @return `true` if this element is public
   */
  bool get isPublic;

  /**
   * Return `true` if this element is synthetic. A synthetic element is an element that is not
   * represented in the source code explicitly, but is implied by the source code, such as the
   * default constructor for a class that does not explicitly define any constructors.
   *
   * @return `true` if this element is synthetic
   */
  bool get isSynthetic;

  /**
   * Use the given visitor to visit all of the children of this element. There is no guarantee of
   * the order in which the children will be visited.
   *
   * @param visitor the visitor that will be used to visit the children of this element
   */
  void visitChildren(ElementVisitor visitor);
}

/**
 * The interface `ElementAnnotation` defines the behavior of objects representing a single
 * annotation associated with an element.
 */
abstract class ElementAnnotation {
  /**
   * Return the element representing the field, variable, or const constructor being used as an
   * annotation.
   *
   * @return the field, variable, or constructor being used as an annotation
   */
  Element get element;

  /**
   * Return `true` if this annotation marks the associated element as being deprecated.
   *
   * @return `true` if this annotation marks the associated element as being deprecated
   */
  bool get isDeprecated;

  /**
   * Return `true` if this annotation marks the associated method as being expected to
   * override an inherited method.
   *
   * @return `true` if this annotation marks the associated method as overriding another
   *         method
   */
  bool get isOverride;

  /**
   * Return `true` if this annotation marks the associated class as implementing a proxy
   * object.
   *
   * @return `true` if this annotation marks the associated class as implementing a proxy
   *         object
   */
  bool get isProxy;
}

/**
 * The enumeration `ElementKind` defines the various kinds of elements in the element model.
 */
class ElementKind extends Enum<ElementKind> {
  static const ElementKind ANGULAR_FILTER = const ElementKind('ANGULAR_FILTER', 0, "Angular filter");

  static const ElementKind ANGULAR_COMPONENT = const ElementKind('ANGULAR_COMPONENT', 1, "Angular component");

  static const ElementKind ANGULAR_CONTROLLER = const ElementKind('ANGULAR_CONTROLLER', 2, "Angular controller");

  static const ElementKind ANGULAR_DIRECTIVE = const ElementKind('ANGULAR_DIRECTIVE', 3, "Angular directive");

  static const ElementKind ANGULAR_PROPERTY = const ElementKind('ANGULAR_PROPERTY', 4, "Angular property");

  static const ElementKind ANGULAR_SCOPE_PROPERTY = const ElementKind('ANGULAR_SCOPE_PROPERTY', 5, "Angular scope property");

  static const ElementKind ANGULAR_SELECTOR = const ElementKind('ANGULAR_SELECTOR', 6, "Angular selector");

  static const ElementKind ANGULAR_VIEW = const ElementKind('ANGULAR_VIEW', 7, "Angular view");

  static const ElementKind CLASS = const ElementKind('CLASS', 8, "class");

  static const ElementKind COMPILATION_UNIT = const ElementKind('COMPILATION_UNIT', 9, "compilation unit");

  static const ElementKind CONSTRUCTOR = const ElementKind('CONSTRUCTOR', 10, "constructor");

  static const ElementKind DYNAMIC = const ElementKind('DYNAMIC', 11, "<dynamic>");

  static const ElementKind EMBEDDED_HTML_SCRIPT = const ElementKind('EMBEDDED_HTML_SCRIPT', 12, "embedded html script");

  static const ElementKind ERROR = const ElementKind('ERROR', 13, "<error>");

  static const ElementKind EXPORT = const ElementKind('EXPORT', 14, "export directive");

  static const ElementKind EXTERNAL_HTML_SCRIPT = const ElementKind('EXTERNAL_HTML_SCRIPT', 15, "external html script");

  static const ElementKind FIELD = const ElementKind('FIELD', 16, "field");

  static const ElementKind FUNCTION = const ElementKind('FUNCTION', 17, "function");

  static const ElementKind GETTER = const ElementKind('GETTER', 18, "getter");

  static const ElementKind HTML = const ElementKind('HTML', 19, "html");

  static const ElementKind IMPORT = const ElementKind('IMPORT', 20, "import directive");

  static const ElementKind LABEL = const ElementKind('LABEL', 21, "label");

  static const ElementKind LIBRARY = const ElementKind('LIBRARY', 22, "library");

  static const ElementKind LOCAL_VARIABLE = const ElementKind('LOCAL_VARIABLE', 23, "local variable");

  static const ElementKind METHOD = const ElementKind('METHOD', 24, "method");

  static const ElementKind NAME = const ElementKind('NAME', 25, "<name>");

  static const ElementKind PARAMETER = const ElementKind('PARAMETER', 26, "parameter");

  static const ElementKind PREFIX = const ElementKind('PREFIX', 27, "import prefix");

  static const ElementKind SETTER = const ElementKind('SETTER', 28, "setter");

  static const ElementKind TOP_LEVEL_VARIABLE = const ElementKind('TOP_LEVEL_VARIABLE', 29, "top level variable");

  static const ElementKind FUNCTION_TYPE_ALIAS = const ElementKind('FUNCTION_TYPE_ALIAS', 30, "function type alias");

  static const ElementKind TYPE_PARAMETER = const ElementKind('TYPE_PARAMETER', 31, "type parameter");

  static const ElementKind UNIVERSE = const ElementKind('UNIVERSE', 32, "<universe>");

  static const List<ElementKind> values = const [
      ANGULAR_FILTER,
      ANGULAR_COMPONENT,
      ANGULAR_CONTROLLER,
      ANGULAR_DIRECTIVE,
      ANGULAR_PROPERTY,
      ANGULAR_SCOPE_PROPERTY,
      ANGULAR_SELECTOR,
      ANGULAR_VIEW,
      CLASS,
      COMPILATION_UNIT,
      CONSTRUCTOR,
      DYNAMIC,
      EMBEDDED_HTML_SCRIPT,
      ERROR,
      EXPORT,
      EXTERNAL_HTML_SCRIPT,
      FIELD,
      FUNCTION,
      GETTER,
      HTML,
      IMPORT,
      LABEL,
      LIBRARY,
      LOCAL_VARIABLE,
      METHOD,
      NAME,
      PARAMETER,
      PREFIX,
      SETTER,
      TOP_LEVEL_VARIABLE,
      FUNCTION_TYPE_ALIAS,
      TYPE_PARAMETER,
      UNIVERSE];

  /**
   * Return the kind of the given element, or [ERROR] if the element is `null`. This is
   * a utility method that can reduce the need for null checks in other places.
   *
   * @param element the element whose kind is to be returned
   * @return the kind of the given element
   */
  static ElementKind of(Element element) {
    if (element == null) {
      return ERROR;
    }
    return element.kind;
  }

  /**
   * The name displayed in the UI for this kind of element.
   */
  final String displayName;

  /**
   * Initialize a newly created element kind to have the given display name.
   *
   * @param displayName the name displayed in the UI for this kind of element
   */
  const ElementKind(String name, int ordinal, this.displayName) : super(name, ordinal);
}

/**
 * The interface `ElementLocation` defines the behavior of objects that represent the location
 * of an element within the element model.
 */
abstract class ElementLocation {
  /**
   * Return an encoded representation of this location that can be used to create a location that is
   * equal to this location.
   *
   * @return an encoded representation of this location
   */
  String get encoding;
}

/**
 * The interface `ElementVisitor` defines the behavior of objects that can be used to visit an
 * element structure.
 */
abstract class ElementVisitor<R> {
  R visitAngularComponentElement(AngularComponentElement element);

  R visitAngularControllerElement(AngularControllerElement element);

  R visitAngularDirectiveElement(AngularDirectiveElement element);

  R visitAngularFilterElement(AngularFilterElement element);

  R visitAngularPropertyElement(AngularPropertyElement element);

  R visitAngularScopePropertyElement(AngularScopePropertyElement element);

  R visitAngularSelectorElement(AngularSelectorElement element);

  R visitAngularViewElement(AngularViewElement element);

  R visitClassElement(ClassElement element);

  R visitCompilationUnitElement(CompilationUnitElement element);

  R visitConstructorElement(ConstructorElement element);

  R visitEmbeddedHtmlScriptElement(EmbeddedHtmlScriptElement element);

  R visitExportElement(ExportElement element);

  R visitExternalHtmlScriptElement(ExternalHtmlScriptElement element);

  R visitFieldElement(FieldElement element);

  R visitFieldFormalParameterElement(FieldFormalParameterElement element);

  R visitFunctionElement(FunctionElement element);

  R visitFunctionTypeAliasElement(FunctionTypeAliasElement element);

  R visitHtmlElement(HtmlElement element);

  R visitImportElement(ImportElement element);

  R visitLabelElement(LabelElement element);

  R visitLibraryElement(LibraryElement element);

  R visitLocalVariableElement(LocalVariableElement element);

  R visitMethodElement(MethodElement element);

  R visitMultiplyDefinedElement(MultiplyDefinedElement element);

  R visitParameterElement(ParameterElement element);

  R visitPrefixElement(PrefixElement element);

  R visitPropertyAccessorElement(PropertyAccessorElement element);

  R visitTopLevelVariableElement(TopLevelVariableElement element);

  R visitTypeParameterElement(TypeParameterElement element);
}

/**
 * The interface `EmbeddedHtmlScriptElement` defines the behavior of elements representing a
 * script tag in an HTML file having content that defines a Dart library.
 */
abstract class EmbeddedHtmlScriptElement implements HtmlScriptElement {
  /**
   * Return the library element defined by the content of the script tag.
   *
   * @return the library element (not `null`)
   */
  LibraryElement get scriptLibrary;
}

/**
 * The interface `ExecutableElement` defines the behavior of elements representing an
 * executable object, including functions, methods, constructors, getters, and setters.
 */
abstract class ExecutableElement implements Element {
  /**
   * Return an array containing all of the functions defined within this executable element.
   *
   * @return the functions defined within this executable element
   */
  List<FunctionElement> get functions;

  /**
   * Return an array containing all of the labels defined within this executable element.
   *
   * @return the labels defined within this executable element
   */
  List<LabelElement> get labels;

  /**
   * Return an array containing all of the local variables defined within this executable element.
   *
   * @return the local variables defined within this executable element
   */
  List<LocalVariableElement> get localVariables;

  /**
   * Return an array containing all of the parameters defined by this executable element.
   *
   * @return the parameters defined by this executable element
   */
  List<ParameterElement> get parameters;

  /**
   * Return the return type defined by this executable element.
   *
   * @return the return type defined by this executable element
   */
  DartType get returnType;

  /**
   * Return the type of function defined by this executable element.
   *
   * @return the type of function defined by this executable element
   */
  FunctionType get type;

  /**
   * Return `true` if this executable element is an operator. The test may be based on the
   * name of the executable element, in which case the result will be correct when the name is
   * legal.
   *
   * @return `true` if this executable element is an operator
   */
  bool get isOperator;

  /**
   * Return `true` if this element is a static element. A static element is an element that is
   * not associated with a particular instance, but rather with an entire library or class.
   *
   * @return `true` if this executable element is a static element
   */
  bool get isStatic;
}

/**
 * The interface `ExportElement` defines the behavior of objects representing information
 * about a single export directive within a library.
 */
abstract class ExportElement implements Element, UriReferencedElement {
  /**
   * An empty array of export elements.
   */
  static final List<ExportElement> EMPTY_ARRAY = new List<ExportElement>(0);

  /**
   * Return an array containing the combinators that were specified as part of the export directive
   * in the order in which they were specified.
   *
   * @return the combinators specified in the export directive
   */
  List<NamespaceCombinator> get combinators;

  /**
   * Return the library that is exported from this library by this export directive.
   *
   * @return the library that is exported from this library
   */
  LibraryElement get exportedLibrary;
}

/**
 * The interface `ExternalHtmlScriptElement` defines the behavior of elements representing a
 * script tag in an HTML file having a `source` attribute that references a Dart library
 * source file.
 */
abstract class ExternalHtmlScriptElement implements HtmlScriptElement {
  /**
   * Return the source referenced by this element, or `null` if this element does not
   * reference a Dart library source file.
   *
   * @return the source for the external Dart library
   */
  Source get scriptSource;
}

/**
 * The interface `FieldElement` defines the behavior of elements representing a field defined
 * within a type.
 */
abstract class FieldElement implements ClassMemberElement, PropertyInducingElement {
}

/**
 * The interface `FieldFormalParameterElement` defines the behavior of elements representing a
 * field formal parameter defined within a constructor element.
 */
abstract class FieldFormalParameterElement implements ParameterElement {
  /**
   * Return the field element associated with this field formal parameter, or `null` if the
   * parameter references a field that doesn't exist.
   *
   * @return the field element associated with this field formal parameter
   */
  FieldElement get field;
}

/**
 * The interface `FunctionElement` defines the behavior of elements representing a function.
 */
abstract class FunctionElement implements ExecutableElement, LocalElement {
  /**
   * Return the resolved [FunctionDeclaration] node that declares this [FunctionElement]
   * .
   *
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   *
   * @return the resolved [FunctionDeclaration], not `null`.
   */
  @override
  FunctionDeclaration get node;
}

/**
 * The interface `FunctionTypeAliasElement` defines the behavior of elements representing a
 * function type alias (`typedef`).
 */
abstract class FunctionTypeAliasElement implements Element {
  /**
   * Return the compilation unit in which this type alias is defined.
   *
   * @return the compilation unit in which this type alias is defined
   */
  @override
  CompilationUnitElement get enclosingElement;

  /**
   * Return the resolved [FunctionTypeAlias] node that declares this
   * [FunctionTypeAliasElement] .
   *
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   *
   * @return the resolved [FunctionTypeAlias], not `null`.
   */
  @override
  FunctionTypeAlias get node;

  /**
   * Return an array containing all of the parameters defined by this type alias.
   *
   * @return the parameters defined by this type alias
   */
  List<ParameterElement> get parameters;

  /**
   * Return the return type defined by this type alias.
   *
   * @return the return type defined by this type alias
   */
  DartType get returnType;

  /**
   * Return the type of function defined by this type alias.
   *
   * @return the type of function defined by this type alias
   */
  FunctionType get type;

  /**
   * Return an array containing all of the type parameters defined for this type.
   *
   * @return the type parameters defined for this type
   */
  List<TypeParameterElement> get typeParameters;
}

/**
 * The interface `HideElementCombinator` defines the behavior of combinators that cause some
 * of the names in a namespace to be hidden when being imported.
 */
abstract class HideElementCombinator implements NamespaceCombinator {
  /**
   * Return an array containing the names that are not to be made visible in the importing library
   * even if they are defined in the imported library.
   *
   * @return the names from the imported library that are hidden from the importing library
   */
  List<String> get hiddenNames;
}

/**
 * The interface `HtmlElement` defines the behavior of elements representing an HTML file.
 */
abstract class HtmlElement implements Element {
  /**
   * Return the [CompilationUnitElement] associated with this Angular HTML file, maybe
   * `null` if not an Angular file.
   */
  CompilationUnitElement get angularCompilationUnit;

  /**
   * Return an array containing all of the script elements contained in the HTML file. This includes
   * scripts with libraries that are defined by the content of a script tag as well as libraries
   * that are referenced in the {@core source} attribute of a script tag.
   *
   * @return the script elements in the HTML file (not `null`, contains no `null`s)
   */
  List<HtmlScriptElement> get scripts;
}

/**
 * The interface `HtmlScriptElement` defines the behavior of elements representing a script
 * tag in an HTML file.
 *
 * @see EmbeddedHtmlScriptElement
 * @see ExternalHtmlScriptElement
 */
abstract class HtmlScriptElement implements Element {
}

/**
 * The interface `ImportElement` defines the behavior of objects representing information
 * about a single import directive within a library.
 */
abstract class ImportElement implements Element, UriReferencedElement {
  /**
   * An empty array of import elements.
   */
  static final List<ImportElement> EMPTY_ARRAY = new List<ImportElement>(0);

  /**
   * Return an array containing the combinators that were specified as part of the import directive
   * in the order in which they were specified.
   *
   * @return the combinators specified in the import directive
   */
  List<NamespaceCombinator> get combinators;

  /**
   * Return the library that is imported into this library by this import directive.
   *
   * @return the library that is imported into this library
   */
  LibraryElement get importedLibrary;

  /**
   * Return the prefix that was specified as part of the import directive, or `null` if there
   * was no prefix specified.
   *
   * @return the prefix that was specified as part of the import directive
   */
  PrefixElement get prefix;

  /**
   * Return the offset of the prefix of this import in the file that contains this import directive,
   * or `-1` if this import is synthetic, does not have a prefix, or otherwise does not have
   * an offset.
   *
   * @return the offset of the prefix of this import
   */
  int get prefixOffset;
}

/**
 * The interface `LabelElement` defines the behavior of elements representing a label
 * associated with a statement.
 */
abstract class LabelElement implements Element {
  /**
   * Return the executable element in which this label is defined.
   *
   * @return the executable element in which this label is defined
   */
  @override
  ExecutableElement get enclosingElement;
}

/**
 * The interface `LibraryElement` defines the behavior of elements representing a library.
 */
abstract class LibraryElement implements Element {
  /**
   * Return the compilation unit that defines this library.
   *
   * @return the compilation unit that defines this library
   */
  CompilationUnitElement get definingCompilationUnit;

  /**
   * Return the entry point for this library, or `null` if this library does not have an entry
   * point. The entry point is defined to be a zero argument top-level function whose name is
   * `main`.
   *
   * @return the entry point for this library
   */
  FunctionElement get entryPoint;

  /**
   * Return an array containing all of the libraries that are exported from this library.
   *
   * @return an array containing all of the libraries that are exported from this library
   */
  List<LibraryElement> get exportedLibraries;

  /**
   * Return an array containing all of the exports defined in this library.
   *
   * @return the exports defined in this library
   */
  List<ExportElement> get exports;

  /**
   * Return an array containing all of the libraries that are imported into this library. This
   * includes all of the libraries that are imported using a prefix (also available through the
   * prefixes returned by [getPrefixes]) and those that are imported without a prefix.
   *
   * @return an array containing all of the libraries that are imported into this library
   */
  List<LibraryElement> get importedLibraries;

  /**
   * Return an array containing all of the imports defined in this library.
   *
   * @return the imports defined in this library
   */
  List<ImportElement> get imports;

  /**
   * Return an array containing all of the compilation units that are included in this library using
   * a `part` directive. This does not include the defining compilation unit that contains the
   * `part` directives.
   *
   * @return the compilation units that are included in this library
   */
  List<CompilationUnitElement> get parts;

  /**
   * Return an array containing elements for each of the prefixes used to `import` libraries
   * into this library. Each prefix can be used in more than one `import` directive.
   *
   * @return the prefixes used to `import` libraries into this library
   */
  List<PrefixElement> get prefixes;

  /**
   * Return the class defined in this library that has the given name, or `null` if this
   * library does not define a class with the given name.
   *
   * @param className the name of the class to be returned
   * @return the class with the given name that is defined in this library
   */
  ClassElement getType(String className);

  /**
   * Return an array containing all of the compilation units this library consists of. This includes
   * the defining compilation unit and units included using the `part` directive.
   *
   * @return the compilation units this library consists of
   */
  List<CompilationUnitElement> get units;

  /**
   * Return an array containing all directly and indirectly imported libraries.
   *
   * @return all directly and indirectly imported libraries
   */
  List<LibraryElement> get visibleLibraries;

  /**
   * Return `true` if the defining compilation unit of this library contains at least one
   * import directive whose URI uses the "dart-ext" scheme.
   */
  bool get hasExtUri;

  /**
   * Return `true` if this library is created for Angular analysis. If this library has not
   * yet had toolkit references resolved, then `false` will be returned.
   *
   * @return `true` if this library is created for Angular analysis
   */
  bool get isAngularHtml;

  /**
   * Answer `true` if this library is an application that can be run in the browser.
   *
   * @return `true` if this library is an application that can be run in the browser
   */
  bool get isBrowserApplication;

  /**
   * Return `true` if this library is the dart:core library.
   *
   * @return `true` if this library is the dart:core library
   */
  bool get isDartCore;

  /**
   * Return `true` if this library is the dart:core library.
   *
   * @return `true` if this library is the dart:core library
   */
  bool get isInSdk;

  /**
   * Return `true` if this library is up to date with respect to the given time stamp. If any
   * transitively referenced Source is newer than the time stamp, this method returns false.
   *
   * @param timeStamp the time stamp to compare against
   * @return `true` if this library is up to date with respect to the given time stamp
   */
  bool isUpToDate(int timeStamp);
}

/**
 * The interface `LocalElement` defines the behavior of elements that can be (but are not
 * required to be) defined within a method or function (an [ExecutableElement]).
 */
abstract class LocalElement implements Element {
  /**
   * Return a source range that covers the approximate portion of the source in which the name of
   * this element is visible, or `null` if there is no single range of characters within which
   * the element name is visible.
   * * For a local variable, this includes everything from the end of the variable's initializer
   * to the end of the block that encloses the variable declaration.
   * * For a parameter, this includes the body of the method or function that declares the
   * parameter.
   * * For a local function, this includes everything from the beginning of the function's body to
   * the end of the block that encloses the function declaration.
   * * For top-level functions, `null` will be returned because they are potentially visible
   * in multiple sources.
   *
   * @return the range of characters in which the name of this element is visible
   */
  SourceRange get visibleRange;
}

/**
 * The interface `LocalVariableElement` defines the behavior common to elements that represent
 * a local variable.
 */
abstract class LocalVariableElement implements LocalElement, VariableElement {
  /**
   * Return an array containing all of the toolkit specific objects attached to this variable.
   *
   * @return the toolkit objects attached to this variable
   */
  List<ToolkitObjectElement> get toolkitObjects;
}

/**
 * The interface `MethodElement` defines the behavior of elements that represent a method
 * defined within a type.
 */
abstract class MethodElement implements ClassMemberElement, ExecutableElement {
  /**
   * Return the resolved [MethodDeclaration] node that declares this [MethodElement].
   *
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   *
   * @return the resolved [MethodDeclaration], not `null`.
   */
  @override
  MethodDeclaration get node;

  /**
   * Return `true` if this method is abstract. Methods are abstract if they are not external
   * and have no body.
   *
   * @return `true` if this method is abstract
   */
  bool get isAbstract;
}

/**
 * The interface `MultiplyDefinedElement` defines the behavior of pseudo-elements that
 * represent multiple elements defined within a single scope that have the same name. This situation
 * is not allowed by the language, so objects implementing this interface always represent an error.
 * As a result, most of the normal operations on elements do not make sense and will return useless
 * results.
 */
abstract class MultiplyDefinedElement implements Element {
  /**
   * Return an array containing all of the elements that were defined within the scope to have the
   * same name.
   *
   * @return the elements that were defined with the same name
   */
  List<Element> get conflictingElements;

  /**
   * Return the type of this element as the dynamic type.
   *
   * @return the type of this element as the dynamic type
   */
  DartType get type;
}

/**
 * The interface [MultiplyInheritedExecutableElement] defines all of the behavior of an
 * [ExecutableElement], with the additional information of an array of
 * [ExecutableElement]s from which this element was composed.
 */
abstract class MultiplyInheritedExecutableElement implements ExecutableElement {
  /**
   * Return an array containing all of the executable elements defined within this executable
   * element.
   *
   * @return the elements defined within this executable element
   */
  List<ExecutableElement> get inheritedElements;
}

/**
 * The interface `NamespaceCombinator` defines the behavior common to objects that control how
 * namespaces are combined.
 */
abstract class NamespaceCombinator {
  /**
   * An empty array of namespace combinators.
   */
  static final List<NamespaceCombinator> EMPTY_ARRAY = new List<NamespaceCombinator>(0);
}

/**
 * The interface `ParameterElement` defines the behavior of elements representing a parameter
 * defined within an executable element.
 */
abstract class ParameterElement implements LocalElement, VariableElement {
  /**
   * Return a source range that covers the portion of the source in which the default value for this
   * parameter is specified, or `null` if there is no default value.
   *
   * @return the range of characters in which the default value of this parameter is specified
   */
  SourceRange get defaultValueRange;

  /**
   * Return the kind of this parameter.
   *
   * @return the kind of this parameter
   */
  ParameterKind get parameterKind;

  /**
   * Return an array containing all of the parameters defined by this parameter. A parameter will
   * only define other parameters if it is a function typed parameter.
   *
   * @return the parameters defined by this parameter element
   */
  List<ParameterElement> get parameters;

  /**
   * Return `true` if this parameter is an initializing formal parameter.
   *
   * @return `true` if this parameter is an initializing formal parameter
   */
  bool get isInitializingFormal;
}

/**
 * The interface `PrefixElement` defines the behavior common to elements that represent a
 * prefix used to import one or more libraries into another library.
 */
abstract class PrefixElement implements Element {
  /**
   * Return the library into which other libraries are imported using this prefix.
   *
   * @return the library into which other libraries are imported using this prefix
   */
  @override
  LibraryElement get enclosingElement;

  /**
   * Return an array containing all of the libraries that are imported using this prefix.
   *
   * @return the libraries that are imported using this prefix
   */
  List<LibraryElement> get importedLibraries;
}

/**
 * The interface `PropertyAccessorElement` defines the behavior of elements representing a
 * getter or a setter. Note that explicitly defined property accessors implicitly define a synthetic
 * field. Symmetrically, synthetic accessors are implicitly created for explicitly defined fields.
 * The following rules apply:
 * * Every explicit field is represented by a non-synthetic [FieldElement].
 * * Every explicit field induces a getter and possibly a setter, both of which are represented by
 * synthetic [PropertyAccessorElement]s.
 * * Every explicit getter or setter is represented by a non-synthetic
 * [PropertyAccessorElement].
 * * Every explicit getter or setter (or pair thereof if they have the same name) induces a field
 * that is represented by a synthetic [FieldElement].
 */
abstract class PropertyAccessorElement implements ExecutableElement {
  /**
   * Return the accessor representing the getter that corresponds to (has the same name as) this
   * setter, or `null` if this accessor is not a setter or if there is no corresponding
   * getter.
   *
   * @return the getter that corresponds to this setter
   */
  PropertyAccessorElement get correspondingGetter;

  /**
   * Return the accessor representing the setter that corresponds to (has the same name as) this
   * getter, or `null` if this accessor is not a getter or if there is no corresponding
   * setter.
   *
   * @return the setter that corresponds to this getter
   */
  PropertyAccessorElement get correspondingSetter;

  /**
   * Return the field or top-level variable associated with this accessor. If this accessor was
   * explicitly defined (is not synthetic) then the variable associated with it will be synthetic.
   *
   * @return the variable associated with this accessor
   */
  PropertyInducingElement get variable;

  /**
   * Return `true` if this accessor is abstract. Accessors are abstract if they are not
   * external and have no body.
   *
   * @return `true` if this accessor is abstract
   */
  bool get isAbstract;

  /**
   * Return `true` if this accessor represents a getter.
   *
   * @return `true` if this accessor represents a getter
   */
  bool get isGetter;

  /**
   * Return `true` if this accessor represents a setter.
   *
   * @return `true` if this accessor represents a setter
   */
  bool get isSetter;
}

/**
 * The interface `PropertyInducingElement` defines the behavior of elements representing a
 * variable that has an associated getter and possibly a setter. Note that explicitly defined
 * variables implicitly define a synthetic getter and that non-`final` explicitly defined
 * variables implicitly define a synthetic setter. Symmetrically, synthetic fields are implicitly
 * created for explicitly defined getters and setters. The following rules apply:
 * * Every explicit variable is represented by a non-synthetic [PropertyInducingElement].
 * * Every explicit variable induces a getter and possibly a setter, both of which are represented
 * by synthetic [PropertyAccessorElement]s.
 * * Every explicit getter or setter is represented by a non-synthetic
 * [PropertyAccessorElement].
 * * Every explicit getter or setter (or pair thereof if they have the same name) induces a
 * variable that is represented by a synthetic [PropertyInducingElement].
 */
abstract class PropertyInducingElement implements VariableElement {
  /**
   * Return the getter associated with this variable. If this variable was explicitly defined (is
   * not synthetic) then the getter associated with it will be synthetic.
   *
   * @return the getter associated with this variable
   */
  PropertyAccessorElement get getter;

  /**
   * Return the setter associated with this variable, or `null` if the variable is effectively
   * `final` and therefore does not have a setter associated with it. (This can happen either
   * because the variable is explicitly defined as being `final` or because the variable is
   * induced by an explicit getter that does not have a corresponding setter.) If this variable was
   * explicitly defined (is not synthetic) then the setter associated with it will be synthetic.
   *
   * @return the setter associated with this variable
   */
  PropertyAccessorElement get setter;

  /**
   * Return `true` if this element is a static element. A static element is an element that is
   * not associated with a particular instance, but rather with an entire library or class.
   *
   * @return `true` if this executable element is a static element
   */
  bool get isStatic;
}

/**
 * The interface `ShowElementCombinator` defines the behavior of combinators that cause some
 * of the names in a namespace to be visible (and the rest hidden) when being imported.
 */
abstract class ShowElementCombinator implements NamespaceCombinator {
  /**
   * Return the offset of the character immediately following the last character of this node.
   *
   * @return the offset of the character just past this node
   */
  int get end;

  /**
   * Return the offset of the 'show' keyword of this element.
   *
   * @return the offset of the 'show' keyword of this element
   */
  int get offset;

  /**
   * Return an array containing the names that are to be made visible in the importing library if
   * they are defined in the imported library.
   *
   * @return the names from the imported library that are visible in the importing library
   */
  List<String> get shownNames;
}

/**
 * The interface `ToolkitObjectElement` defines the behavior of elements that represent a
 * toolkit specific object, such as Angular controller or component. These elements are not based on
 * the Dart syntax, but on some semantic agreement, such as a special annotation.
 */
abstract class ToolkitObjectElement implements Element {
  /**
   * An empty array of toolkit object elements.
   */
  static final List<ToolkitObjectElement> EMPTY_ARRAY = new List<ToolkitObjectElement>(0);
}

/**
 * The interface `TopLevelVariableElement` defines the behavior of elements representing a
 * top-level variable.
 */
abstract class TopLevelVariableElement implements PropertyInducingElement {
}

/**
 * The interface `TypeParameterElement` defines the behavior of elements representing a type
 * parameter.
 */
abstract class TypeParameterElement implements Element {
  /**
   * Return the type representing the bound associated with this parameter, or `null` if this
   * parameter does not have an explicit bound.
   *
   * @return the type representing the bound associated with this parameter
   */
  DartType get bound;

  /**
   * Return the type defined by this type parameter.
   *
   * @return the type defined by this type parameter
   */
  TypeParameterType get type;
}

/**
 * The interface `UndefinedElement` defines the behavior of pseudo-elements that represent
 * names that are undefined. This situation is not allowed by the language, so objects implementing
 * this interface always represent an error. As a result, most of the normal operations on elements
 * do not make sense and will return useless results.
 */
abstract class UndefinedElement implements Element {
}

/**
 * The interface `UriReferencedElement` defines the behavior of objects included into a
 * library using some URI.
 */
abstract class UriReferencedElement implements Element {
  /**
   * Return the offset of the character immediately following the last character of this node's URI,
   * or `-1` for synthetic import.
   *
   * @return the offset of the character just past the node's URI
   */
  int get uriEnd;

  /**
   * Return the offset of the URI in the file, or `-1` if this element is synthetic.
   *
   * @return the offset of the URI
   */
  int get uriOffset;

  /**
   * Return the URI that is used to include this element into the enclosing library, or `null`
   * if this is the defining compilation unit of a library.
   *
   * @return the URI that is used to include this element into the enclosing library
   */
  String get uri;
}

/**
 * The interface `VariableElement` defines the behavior common to elements that represent a
 * variable.
 */
abstract class VariableElement implements Element {
  /**
   * Return a synthetic function representing this variable's initializer, or `null` if this
   * variable does not have an initializer. The function will have no parameters. The return type of
   * the function will be the compile-time type of the initialization expression.
   *
   * @return a synthetic function representing this variable's initializer
   */
  FunctionElement get initializer;

  /**
   * Return the resolved [VariableDeclaration] node that declares this [VariableElement]
   * .
   *
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   *
   * @return the resolved [VariableDeclaration], not `null`.
   */
  @override
  VariableDeclaration get node;

  /**
   * Return the declared type of this variable, or `null` if the variable did not have a
   * declared type (such as if it was declared using the keyword 'var').
   *
   * @return the declared type of this variable
   */
  DartType get type;

  /**
   * Return `true` if this variable was declared with the 'const' modifier.
   *
   * @return `true` if this variable was declared with the 'const' modifier
   */
  bool get isConst;

  /**
   * Return `true` if this variable was declared with the 'final' modifier. Variables that are
   * declared with the 'const' modifier will return `false` even though they are implicitly
   * final.
   *
   * @return `true` if this variable was declared with the 'final' modifier
   */
  bool get isFinal;
}

/**
 * The interface `AngularControllerElement` defines the Angular component described by
 * <code>NgComponent</code> annotation.
 */
abstract class AngularComponentElement implements AngularHasSelectorElement, AngularHasTemplateElement {
  /**
   * Return an array containing all of the properties declared by this component.
   */
  List<AngularPropertyElement> get properties;

  /**
   * Return an array containing all of the scope properties set in the implementation of this
   * component.
   */
  List<AngularScopePropertyElement> get scopeProperties;

  /**
   * Returns the CSS file URI.
   */
  String get styleUri;

  /**
   * Return the offset of the [getStyleUri] in the [getSource].
   *
   * @return the offset of the style URI
   */
  int get styleUriOffset;
}

/**
 * The interface `AngularControllerElement` defines the Angular controller described by
 * <code>NgController</code> annotation.
 */
abstract class AngularControllerElement implements AngularHasSelectorElement {
}

/**
 * The interface `AngularDirectiveElement` defines the Angular controller described by
 * <code>NgDirective</code> annotation.
 */
abstract class AngularDirectiveElement implements AngularHasSelectorElement {
  /**
   * Return an array containing all of the properties declared by this directive.
   */
  List<AngularPropertyElement> get properties;

  /**
   * Checks if this directive is implemented by the class with given name.
   */
  bool isClass(String name);
}

/**
 * The interface `AngularElement` defines the behavior of objects representing information
 * about an Angular specific element.
 */
abstract class AngularElement implements ToolkitObjectElement {
  /**
   * An empty array of angular elements.
   */
  static final List<AngularElement> EMPTY_ARRAY = new List<AngularElement>(0);

  /**
   * Returns the [AngularApplication] this element is used in.
   *
   * @return the [AngularApplication] this element is used in
   */
  AngularApplication get application;
}

/**
 * The interface `AngularFilterElement` defines the Angular filter described by
 * <code>NgFilter</code> annotation.
 */
abstract class AngularFilterElement implements AngularElement {
}

/**
 * [AngularSelectorElement] based on presence of attribute.
 */
abstract class AngularHasAttributeSelectorElement implements AngularSelectorElement {
}

/**
 * [AngularSelectorElement] based on presence of a class.
 */
abstract class AngularHasClassSelectorElement implements AngularSelectorElement {
}

/**
 * The interface `AngularElement` defines the behavior of objects representing information
 * about an Angular element which is applied conditionally using some [AngularSelectorElement].
 */
abstract class AngularHasSelectorElement implements AngularElement {
  /**
   * Returns the selector specified for this element.
   *
   * @return the [AngularSelectorElement] specified for this element
   */
  AngularSelectorElement get selector;
}

/**
 * The interface `AngularHasTemplateElement` defines common behavior for
 * [AngularElement] that have template URI / [Source].
 */
abstract class AngularHasTemplateElement implements AngularElement {
  /**
   * Returns the HTML template [Source], `null` if not resolved.
   */
  Source get templateSource;

  /**
   * Returns the HTML template URI.
   */
  String get templateUri;

  /**
   * Return the offset of the [getTemplateUri] in the [getSource].
   *
   * @return the offset of the template URI
   */
  int get templateUriOffset;
}

/**
 * The interface `AngularPropertyElement` defines a single property in
 * [AngularComponentElement].
 */
abstract class AngularPropertyElement implements AngularElement {
  /**
   * An empty array of property elements.
   */
  static final List<AngularPropertyElement> EMPTY_ARRAY = [];

  /**
   * Returns the field this property is mapped to.
   *
   * @return the field this property is mapped to.
   */
  FieldElement get field;

  /**
   * Return the offset of the field name of this property in the property map, or `-1` if
   * property was created using annotation on [FieldElement].
   *
   * @return the offset of the field name of this property
   */
  int get fieldNameOffset;

  /**
   * Returns the kind of this property.
   *
   * @return the kind of this property
   */
  AngularPropertyKind get propertyKind;
}

/**
 * The enumeration `AngularPropertyKind` defines the different kinds of property bindings.
 */
class AngularPropertyKind extends Enum<AngularPropertyKind> {
  /**
   * `@` - Map the DOM attribute string. The attribute string will be taken literally or
   * interpolated if it contains binding {{}} syntax and assigned to the expression. (cost: 0
   * watches)
   */
  static const AngularPropertyKind ATTR = const AngularPropertyKind('ATTR', 0);

  /**
   * `&` - Treat the DOM attribute value as an expression. Assign a closure function into the field.
   * This allows the component to control the invocation of the closure. This is useful for passing
   * expressions into controllers which act like callbacks. (cost: 0 watches)
   */
  static const AngularPropertyKind CALLBACK = const AngularPropertyKind('CALLBACK', 1);

  /**
   * `=>` - Treat the DOM attribute value as an expression. Set up a watch, which will read the
   * expression in the attribute and assign the value to destination expression. (cost: 1 watch)
   */
  static const AngularPropertyKind ONE_WAY = const AngularPropertyKind('ONE_WAY', 2);

  /**
   * `=>!` - Treat the DOM attribute value as an expression. Set up a one time watch on expression.
   * Once the expression turns not null it will no longer update. (cost: 1 watches until not null,
   * then 0 watches)
   */
  static const AngularPropertyKind ONE_WAY_ONE_TIME = const AngularPropertyKind('ONE_WAY_ONE_TIME', 3);

  /**
   * `<=>` - Treat the DOM attribute value as an expression. Set up a watch on both outside as well
   * as component scope to keep the source and destination in sync. (cost: 2 watches)
   */
  static const AngularPropertyKind TWO_WAY = const AngularPropertyKind_TWO_WAY('TWO_WAY', 4);

  static const List<AngularPropertyKind> values = const [ATTR, CALLBACK, ONE_WAY, ONE_WAY_ONE_TIME, TWO_WAY];

  /**
   * Returns `true` if property of this kind calls field getter.
   */
  bool callsGetter() => false;

  /**
   * Returns `true` if property of this kind calls field setter.
   */
  bool callsSetter() => true;

  const AngularPropertyKind(String name, int ordinal) : super(name, ordinal);
}

class AngularPropertyKind_TWO_WAY extends AngularPropertyKind {
  const AngularPropertyKind_TWO_WAY(String name, int ordinal) : super(name, ordinal);

  @override
  bool callsGetter() => true;
}

/**
 * The interface `AngularScopeVariableElement` defines the Angular <code>Scope</code>
 * property. They are created for every <code>scope['property'] = value;</code> code snippet.
 */
abstract class AngularScopePropertyElement implements AngularElement {
  /**
   * An empty array of scope property elements.
   */
  static final List<AngularScopePropertyElement> EMPTY_ARRAY = [];

  /**
   * Returns the type of this property, not `null`, maybe <code>dynamic</code>.
   *
   * @return the type of this property.
   */
  DartType get type;
}

/**
 * [AngularSelectorElement] is used to decide when Angular object should be applied.
 *
 * This class is an [Element] to support renaming component tag names, which are identifiers
 * in selectors.
 */
abstract class AngularSelectorElement implements AngularElement {
  /**
   * Checks if the given [XmlTagNode] matches this selector.
   *
   * @param node the [XmlTagNode] to check
   * @return `true` if the given [XmlTagNode] matches, or `false` otherwise
   */
  bool apply(XmlTagNode node);
}

/**
 * [AngularSelectorElement] based on tag name.
 */
abstract class AngularTagSelectorElement implements AngularSelectorElement {
}

/**
 * The interface `AngularViewElement` defines the Angular view defined using invocation like
 * <code>view('views/create.html')</code>.
 */
abstract class AngularViewElement implements AngularHasTemplateElement {
  /**
   * An empty array of view elements.
   */
  static final List<AngularViewElement> EMPTY_ARRAY = new List<AngularViewElement>(0);
}

/**
 * Instances of the class `GeneralizingElementVisitor` implement an element visitor that will
 * recursively visit all of the elements in an element model (like instances of the class
 * [RecursiveElementVisitor]). In addition, when an element of a specific type is visited not
 * only will the visit method for that specific type of element be invoked, but additional methods
 * for the supertypes of that element will also be invoked. For example, using an instance of this
 * class to visit a [MethodElement] will cause the method
 * [visitMethodElement] to be invoked but will also cause the methods
 * [visitExecutableElement] and [visitElement] to be
 * subsequently invoked. This allows visitors to be written that visit all executable elements
 * without needing to override the visit method for each of the specific subclasses of
 * [ExecutableElement].
 *
 * Note, however, that unlike many visitors, element visitors visit objects based on the interfaces
 * implemented by those elements. Because interfaces form a graph structure rather than a tree
 * structure the way classes do, and because it is generally undesirable for an object to be visited
 * more than once, this class flattens the interface graph into a pseudo-tree. In particular, this
 * class treats elements as if the element types were structured in the following way:
 *
 *
 * <pre>
 * Element
 *   ClassElement
 *   CompilationUnitElement
 *   ExecutableElement
 *      ConstructorElement
 *      LocalElement
 *         FunctionElement
 *      MethodElement
 *      PropertyAccessorElement
 *   ExportElement
 *   HtmlElement
 *   ImportElement
 *   LabelElement
 *   LibraryElement
 *   MultiplyDefinedElement
 *   PrefixElement
 *   TypeAliasElement
 *   TypeParameterElement
 *   UndefinedElement
 *   VariableElement
 *      PropertyInducingElement
 *         FieldElement
 *         TopLevelVariableElement
 *      LocalElement
 *         LocalVariableElement
 *         ParameterElement
 *            FieldFormalParameterElement
 * </pre>
 *
 * Subclasses that override a visit method must either invoke the overridden visit method or
 * explicitly invoke the more general visit method. Failure to do so will cause the visit methods
 * for superclasses of the element to not be invoked and will cause the children of the visited node
 * to not be visited.
 */
class GeneralizingElementVisitor<R> implements ElementVisitor<R> {
  @override
  R visitAngularComponentElement(AngularComponentElement element) => visitAngularHasSelectorElement(element);

  @override
  R visitAngularControllerElement(AngularControllerElement element) => visitAngularHasSelectorElement(element);

  @override
  R visitAngularDirectiveElement(AngularDirectiveElement element) => visitAngularHasSelectorElement(element);

  R visitAngularElement(AngularElement element) => visitToolkitObjectElement(element);

  @override
  R visitAngularFilterElement(AngularFilterElement element) => visitAngularElement(element);

  R visitAngularHasSelectorElement(AngularHasSelectorElement element) => visitAngularElement(element);

  @override
  R visitAngularPropertyElement(AngularPropertyElement element) => visitAngularElement(element);

  @override
  R visitAngularScopePropertyElement(AngularScopePropertyElement element) => visitAngularElement(element);

  @override
  R visitAngularSelectorElement(AngularSelectorElement element) => visitAngularElement(element);

  @override
  R visitAngularViewElement(AngularViewElement element) => visitAngularElement(element);

  @override
  R visitClassElement(ClassElement element) => visitElement(element);

  @override
  R visitCompilationUnitElement(CompilationUnitElement element) => visitElement(element);

  @override
  R visitConstructorElement(ConstructorElement element) => visitExecutableElement(element);

  R visitElement(Element element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitEmbeddedHtmlScriptElement(EmbeddedHtmlScriptElement element) => visitHtmlScriptElement(element);

  R visitExecutableElement(ExecutableElement element) => visitElement(element);

  @override
  R visitExportElement(ExportElement element) => visitElement(element);

  @override
  R visitExternalHtmlScriptElement(ExternalHtmlScriptElement element) => visitHtmlScriptElement(element);

  @override
  R visitFieldElement(FieldElement element) => visitPropertyInducingElement(element);

  @override
  R visitFieldFormalParameterElement(FieldFormalParameterElement element) => visitParameterElement(element);

  @override
  R visitFunctionElement(FunctionElement element) => visitLocalElement(element);

  @override
  R visitFunctionTypeAliasElement(FunctionTypeAliasElement element) => visitElement(element);

  @override
  R visitHtmlElement(HtmlElement element) => visitElement(element);

  R visitHtmlScriptElement(HtmlScriptElement element) => visitElement(element);

  @override
  R visitImportElement(ImportElement element) => visitElement(element);

  @override
  R visitLabelElement(LabelElement element) => visitElement(element);

  @override
  R visitLibraryElement(LibraryElement element) => visitElement(element);

  R visitLocalElement(LocalElement element) {
    if (element is LocalVariableElement) {
      return visitVariableElement(element);
    } else if (element is ParameterElement) {
      return visitVariableElement(element);
    } else if (element is FunctionElement) {
      return visitExecutableElement(element);
    }
    return null;
  }

  @override
  R visitLocalVariableElement(LocalVariableElement element) => visitLocalElement(element);

  @override
  R visitMethodElement(MethodElement element) => visitExecutableElement(element);

  @override
  R visitMultiplyDefinedElement(MultiplyDefinedElement element) => visitElement(element);

  @override
  R visitParameterElement(ParameterElement element) => visitLocalElement(element);

  @override
  R visitPrefixElement(PrefixElement element) => visitElement(element);

  @override
  R visitPropertyAccessorElement(PropertyAccessorElement element) => visitExecutableElement(element);

  R visitPropertyInducingElement(PropertyInducingElement element) => visitVariableElement(element);

  R visitToolkitObjectElement(ToolkitObjectElement element) => visitElement(element);

  @override
  R visitTopLevelVariableElement(TopLevelVariableElement element) => visitPropertyInducingElement(element);

  @override
  R visitTypeParameterElement(TypeParameterElement element) => visitElement(element);

  R visitVariableElement(VariableElement element) => visitElement(element);
}

/**
 * Instances of the class `RecursiveElementVisitor` implement an element visitor that will
 * recursively visit all of the element in an element model. For example, using an instance of this
 * class to visit a [CompilationUnitElement] will also cause all of the types in the
 * compilation unit to be visited.
 *
 * Subclasses that override a visit method must either invoke the overridden visit method or must
 * explicitly ask the visited element to visit its children. Failure to do so will cause the
 * children of the visited element to not be visited.
 */
class RecursiveElementVisitor<R> implements ElementVisitor<R> {
  @override
  R visitAngularComponentElement(AngularComponentElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitAngularControllerElement(AngularControllerElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitAngularDirectiveElement(AngularDirectiveElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitAngularFilterElement(AngularFilterElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitAngularPropertyElement(AngularPropertyElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitAngularScopePropertyElement(AngularScopePropertyElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitAngularSelectorElement(AngularSelectorElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitAngularViewElement(AngularViewElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitClassElement(ClassElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitCompilationUnitElement(CompilationUnitElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitConstructorElement(ConstructorElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitEmbeddedHtmlScriptElement(EmbeddedHtmlScriptElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitExportElement(ExportElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitExternalHtmlScriptElement(ExternalHtmlScriptElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitFieldElement(FieldElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitFieldFormalParameterElement(FieldFormalParameterElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitFunctionElement(FunctionElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitFunctionTypeAliasElement(FunctionTypeAliasElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitHtmlElement(HtmlElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitImportElement(ImportElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitLabelElement(LabelElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitLibraryElement(LibraryElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitLocalVariableElement(LocalVariableElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitMethodElement(MethodElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitMultiplyDefinedElement(MultiplyDefinedElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitParameterElement(ParameterElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitPrefixElement(PrefixElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitPropertyAccessorElement(PropertyAccessorElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitTopLevelVariableElement(TopLevelVariableElement element) {
    element.visitChildren(this);
    return null;
  }

  @override
  R visitTypeParameterElement(TypeParameterElement element) {
    element.visitChildren(this);
    return null;
  }
}

/**
 * Instances of the class `SimpleElementVisitor` implement an element visitor that will do
 * nothing when visiting an element. It is intended to be a superclass for classes that use the
 * visitor pattern primarily as a dispatch mechanism (and hence don't need to recursively visit a
 * whole structure) and that only need to visit a small number of element types.
 */
class SimpleElementVisitor<R> implements ElementVisitor<R> {
  @override
  R visitAngularComponentElement(AngularComponentElement element) => null;

  @override
  R visitAngularControllerElement(AngularControllerElement element) => null;

  @override
  R visitAngularDirectiveElement(AngularDirectiveElement element) => null;

  @override
  R visitAngularFilterElement(AngularFilterElement element) => null;

  @override
  R visitAngularPropertyElement(AngularPropertyElement element) => null;

  @override
  R visitAngularScopePropertyElement(AngularScopePropertyElement element) => null;

  @override
  R visitAngularSelectorElement(AngularSelectorElement element) => null;

  @override
  R visitAngularViewElement(AngularViewElement element) => null;

  @override
  R visitClassElement(ClassElement element) => null;

  @override
  R visitCompilationUnitElement(CompilationUnitElement element) => null;

  @override
  R visitConstructorElement(ConstructorElement element) => null;

  @override
  R visitEmbeddedHtmlScriptElement(EmbeddedHtmlScriptElement element) => null;

  @override
  R visitExportElement(ExportElement element) => null;

  @override
  R visitExternalHtmlScriptElement(ExternalHtmlScriptElement element) => null;

  @override
  R visitFieldElement(FieldElement element) => null;

  @override
  R visitFieldFormalParameterElement(FieldFormalParameterElement element) => null;

  @override
  R visitFunctionElement(FunctionElement element) => null;

  @override
  R visitFunctionTypeAliasElement(FunctionTypeAliasElement element) => null;

  @override
  R visitHtmlElement(HtmlElement element) => null;

  @override
  R visitImportElement(ImportElement element) => null;

  @override
  R visitLabelElement(LabelElement element) => null;

  @override
  R visitLibraryElement(LibraryElement element) => null;

  @override
  R visitLocalVariableElement(LocalVariableElement element) => null;

  @override
  R visitMethodElement(MethodElement element) => null;

  @override
  R visitMultiplyDefinedElement(MultiplyDefinedElement element) => null;

  @override
  R visitParameterElement(ParameterElement element) => null;

  @override
  R visitPrefixElement(PrefixElement element) => null;

  @override
  R visitPropertyAccessorElement(PropertyAccessorElement element) => null;

  @override
  R visitTopLevelVariableElement(TopLevelVariableElement element) => null;

  @override
  R visitTypeParameterElement(TypeParameterElement element) => null;
}

/**
 * For AST nodes that could be in both the getter and setter contexts ([IndexExpression]s and
 * [SimpleIdentifier]s), the additional resolved elements are stored in the AST node, in an
 * [AuxiliaryElements]. Since resolved elements are either statically resolved or resolved
 * using propagated type information, this class is a wrapper for a pair of
 * [ExecutableElement]s, not just a single [ExecutableElement].
 */
class AuxiliaryElements {
  /**
   * The element based on propagated type information, or `null` if the AST structure has not
   * been resolved or if this identifier could not be resolved.
   */
  final ExecutableElement propagatedElement;

  /**
   * The element associated with this identifier based on static type information, or `null`
   * if the AST structure has not been resolved or if this identifier could not be resolved.
   */
  final ExecutableElement staticElement;

  /**
   * Create the [AuxiliaryElements] with a static and propagated [ExecutableElement].
   *
   * @param staticElement the static element
   * @param propagatedElement the propagated element
   */
  AuxiliaryElements(this.staticElement, this.propagatedElement);
}

/**
 * Instances of the class `ClassElementImpl` implement a `ClassElement`.
 */
class ClassElementImpl extends ElementImpl implements ClassElement {
  /**
   * An array containing all of the accessors (getters and setters) contained in this class.
   */
  List<PropertyAccessorElement> _accessors = PropertyAccessorElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the constructors contained in this class.
   */
  List<ConstructorElement> _constructors = ConstructorElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the fields contained in this class.
   */
  List<FieldElement> _fields = FieldElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the mixins that are applied to the class being extended in order to
   * derive the superclass of this class.
   */
  List<InterfaceType> mixins = InterfaceTypeImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the interfaces that are implemented by this class.
   */
  List<InterfaceType> interfaces = InterfaceTypeImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the methods contained in this class.
   */
  List<MethodElement> _methods = MethodElementImpl.EMPTY_ARRAY;

  /**
   * The superclass of the class, or `null` if the class does not have an explicit superclass.
   */
  InterfaceType supertype;

  /**
   * An array containing all of the toolkit objects attached to this class.
   */
  List<ToolkitObjectElement> _toolkitObjects = ToolkitObjectElement.EMPTY_ARRAY;

  /**
   * The type defined by the class.
   */
  InterfaceType type;

  /**
   * An array containing all of the type parameters defined for this class.
   */
  List<TypeParameterElement> _typeParameters = TypeParameterElementImpl.EMPTY_ARRAY;

  /**
   * An empty array of class elements.
   */
  static List<ClassElement> EMPTY_ARRAY = new List<ClassElement>(0);

  /**
   * Initialize a newly created class element to have the given name.
   *
   * @param name the name of this element
   */
  ClassElementImpl(Identifier name) : super.forNode(name);

  @override
  accept(ElementVisitor visitor) => visitor.visitClassElement(this);

  @override
  List<PropertyAccessorElement> get accessors => _accessors;

  @override
  List<InterfaceType> get allSupertypes {
    List<InterfaceType> list = new List<InterfaceType>();
    _collectAllSupertypes(list);
    return new List.from(list);
  }

  @override
  ElementImpl getChild(String identifier) {
    //
    // The casts in this method are safe because the set methods would have thrown a CCE if any of
    // the elements in the arrays were not of the expected types.
    //
    for (PropertyAccessorElement accessor in _accessors) {
      if ((accessor as PropertyAccessorElementImpl).identifier == identifier) {
        return accessor as PropertyAccessorElementImpl;
      }
    }
    for (ConstructorElement constructor in _constructors) {
      if ((constructor as ConstructorElementImpl).identifier == identifier) {
        return constructor as ConstructorElementImpl;
      }
    }
    for (FieldElement field in _fields) {
      if ((field as FieldElementImpl).identifier == identifier) {
        return field as FieldElementImpl;
      }
    }
    for (MethodElement method in _methods) {
      if ((method as MethodElementImpl).identifier == identifier) {
        return method as MethodElementImpl;
      }
    }
    for (TypeParameterElement typeParameter in _typeParameters) {
      if ((typeParameter as TypeParameterElementImpl).identifier == identifier) {
        return typeParameter as TypeParameterElementImpl;
      }
    }
    return null;
  }

  @override
  List<ConstructorElement> get constructors => _constructors;

  @override
  FieldElement getField(String name) {
    for (FieldElement fieldElement in _fields) {
      if (name == fieldElement.name) {
        return fieldElement;
      }
    }
    return null;
  }

  @override
  List<FieldElement> get fields => _fields;

  @override
  PropertyAccessorElement getGetter(String getterName) {
    for (PropertyAccessorElement accessor in _accessors) {
      if (accessor.isGetter && accessor.name == getterName) {
        return accessor;
      }
    }
    return null;
  }

  @override
  ElementKind get kind => ElementKind.CLASS;

  @override
  MethodElement getMethod(String methodName) {
    for (MethodElement method in _methods) {
      if (method.name == methodName) {
        return method;
      }
    }
    return null;
  }

  @override
  List<MethodElement> get methods => _methods;

  @override
  ConstructorElement getNamedConstructor(String name) {
    for (ConstructorElement element in constructors) {
      String elementName = element.name;
      if (elementName != null && elementName == name) {
        return element;
      }
    }
    return null;
  }

  @override
  ClassDeclaration get node => getNodeMatching((node) => node is ClassDeclaration);

  @override
  PropertyAccessorElement getSetter(String setterName) {
    // TODO (jwren) revisit- should we append '=' here or require clients to include it?
    // Do we need the check for isSetter below?
    if (!StringUtilities.endsWithChar(setterName, 0x3D)) {
      setterName += '=';
    }
    for (PropertyAccessorElement accessor in _accessors) {
      if (accessor.isSetter && accessor.name == setterName) {
        return accessor;
      }
    }
    return null;
  }

  @override
  List<ToolkitObjectElement> get toolkitObjects => _toolkitObjects;

  @override
  List<TypeParameterElement> get typeParameters => _typeParameters;

  @override
  ConstructorElement get unnamedConstructor {
    for (ConstructorElement element in constructors) {
      String name = element.displayName;
      if (name == null || name.isEmpty) {
        return element;
      }
    }
    return null;
  }

  @override
  bool get hasNonFinalField {
    List<ClassElement> classesToVisit = new List<ClassElement>();
    Set<ClassElement> visitedClasses = new Set<ClassElement>();
    classesToVisit.add(this);
    while (!classesToVisit.isEmpty) {
      ClassElement currentElement = classesToVisit.removeAt(0);
      if (visitedClasses.add(currentElement)) {
        // check fields
        for (FieldElement field in currentElement.fields) {
          if (!field.isFinal && !field.isConst && !field.isStatic && !field.isSynthetic) {
            return true;
          }
        }
        // check mixins
        for (InterfaceType mixinType in currentElement.mixins) {
          ClassElement mixinElement = mixinType.element;
          classesToVisit.add(mixinElement);
        }
        // check super
        InterfaceType supertype = currentElement.supertype;
        if (supertype != null) {
          ClassElement superElement = supertype.element;
          if (superElement != null) {
            classesToVisit.add(superElement);
          }
        }
      }
    }
    // not found
    return false;
  }

  @override
  bool get hasReferenceToSuper => hasModifier(Modifier.REFERENCES_SUPER);

  @override
  bool get hasStaticMember {
    for (MethodElement method in _methods) {
      if (method.isStatic) {
        return true;
      }
    }
    for (PropertyAccessorElement accessor in _accessors) {
      if (accessor.isStatic) {
        return true;
      }
    }
    return false;
  }

  @override
  bool get isAbstract => hasModifier(Modifier.ABSTRACT);

  @override
  bool get isOrInheritsProxy => _safeIsOrInheritsProxy(this, new Set<ClassElement>());

  @override
  bool get isProxy {
    for (ElementAnnotation annotation in metadata) {
      if (annotation.isProxy) {
        return true;
      }
    }
    return false;
  }

  @override
  bool get isTypedef => hasModifier(Modifier.TYPEDEF);

  @override
  bool get isValidMixin => hasModifier(Modifier.MIXIN);

  @override
  PropertyAccessorElement lookUpGetter(String getterName, LibraryElement library) {
    Set<ClassElement> visitedClasses = new Set<ClassElement>();
    ClassElement currentElement = this;
    while (currentElement != null && !visitedClasses.contains(currentElement)) {
      visitedClasses.add(currentElement);
      PropertyAccessorElement element = currentElement.getGetter(getterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
      for (InterfaceType mixin in currentElement.mixins) {
        ClassElement mixinElement = mixin.element;
        if (mixinElement != null) {
          element = mixinElement.getGetter(getterName);
          if (element != null && element.isAccessibleIn(library)) {
            return element;
          }
        }
      }
      InterfaceType supertype = currentElement.supertype;
      if (supertype == null) {
        return null;
      }
      currentElement = supertype.element;
    }
    return null;
  }

  @override
  MethodElement lookUpMethod(String methodName, LibraryElement library) {
    Set<ClassElement> visitedClasses = new Set<ClassElement>();
    ClassElement currentElement = this;
    while (currentElement != null && !visitedClasses.contains(currentElement)) {
      visitedClasses.add(currentElement);
      MethodElement element = currentElement.getMethod(methodName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
      for (InterfaceType mixin in currentElement.mixins) {
        ClassElement mixinElement = mixin.element;
        if (mixinElement != null) {
          element = mixinElement.getMethod(methodName);
          if (element != null && element.isAccessibleIn(library)) {
            return element;
          }
        }
      }
      InterfaceType supertype = currentElement.supertype;
      if (supertype == null) {
        return null;
      }
      currentElement = supertype.element;
    }
    return null;
  }

  @override
  PropertyAccessorElement lookUpSetter(String setterName, LibraryElement library) {
    Set<ClassElement> visitedClasses = new Set<ClassElement>();
    ClassElement currentElement = this;
    while (currentElement != null && !visitedClasses.contains(currentElement)) {
      visitedClasses.add(currentElement);
      PropertyAccessorElement element = currentElement.getSetter(setterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
      for (InterfaceType mixin in currentElement.mixins) {
        ClassElement mixinElement = mixin.element;
        if (mixinElement != null) {
          element = mixinElement.getSetter(setterName);
          if (element != null && element.isAccessibleIn(library)) {
            return element;
          }
        }
      }
      InterfaceType supertype = currentElement.supertype;
      if (supertype == null) {
        return null;
      }
      currentElement = supertype.element;
    }
    return null;
  }

  /**
   * Set whether this class is abstract to correspond to the given value.
   *
   * @param isAbstract `true` if the class is abstract
   */
  void set abstract(bool isAbstract) {
    setModifier(Modifier.ABSTRACT, isAbstract);
  }

  /**
   * Set the accessors contained in this class to the given accessors.
   *
   * @param accessors the accessors contained in this class
   */
  void set accessors(List<PropertyAccessorElement> accessors) {
    for (PropertyAccessorElement accessor in accessors) {
      (accessor as PropertyAccessorElementImpl).enclosingElement = this;
    }
    this._accessors = accessors;
  }

  /**
   * Set the constructors contained in this class to the given constructors.
   *
   * @param constructors the constructors contained in this class
   */
  void set constructors(List<ConstructorElement> constructors) {
    for (ConstructorElement constructor in constructors) {
      (constructor as ConstructorElementImpl).enclosingElement = this;
    }
    this._constructors = constructors;
  }

  /**
   * Set the fields contained in this class to the given fields.
   *
   * @param fields the fields contained in this class
   */
  void set fields(List<FieldElement> fields) {
    for (FieldElement field in fields) {
      (field as FieldElementImpl).enclosingElement = this;
    }
    this._fields = fields;
  }

  /**
   * Set whether this class references 'super' to the given value.
   *
   * @param isReferencedSuper `true` references 'super'
   */
  void set hasReferenceToSuper(bool isReferencedSuper) {
    setModifier(Modifier.REFERENCES_SUPER, isReferencedSuper);
  }

  /**
   * Set the methods contained in this class to the given methods.
   *
   * @param methods the methods contained in this class
   */
  void set methods(List<MethodElement> methods) {
    for (MethodElement method in methods) {
      (method as MethodElementImpl).enclosingElement = this;
    }
    this._methods = methods;
  }

  /**
   * Set the toolkit specific information objects attached to this class.
   *
   * @param toolkitObjects the toolkit objects attached to this class
   */
  void set toolkitObjects(List<ToolkitObjectElement> toolkitObjects) {
    for (ToolkitObjectElement toolkitObject in toolkitObjects) {
      (toolkitObject as ToolkitObjectElementImpl).enclosingElement = this;
    }
    this._toolkitObjects = toolkitObjects;
  }

  /**
   * Set whether this class is defined by a typedef construct to correspond to the given value.
   *
   * @param isTypedef `true` if the class is defined by a typedef construct
   */
  void set typedef(bool isTypedef) {
    setModifier(Modifier.TYPEDEF, isTypedef);
  }

  /**
   * Set the type parameters defined for this class to the given type parameters.
   *
   * @param typeParameters the type parameters defined for this class
   */
  void set typeParameters(List<TypeParameterElement> typeParameters) {
    for (TypeParameterElement typeParameter in typeParameters) {
      (typeParameter as TypeParameterElementImpl).enclosingElement = this;
    }
    this._typeParameters = typeParameters;
  }

  /**
   * Set whether this class is a valid mixin to correspond to the given value.
   *
   * @param isValidMixin `true` if this class can be used as a mixin
   */
  void set validMixin(bool isValidMixin) {
    setModifier(Modifier.MIXIN, isValidMixin);
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(_accessors, visitor);
    safelyVisitChildren(_constructors, visitor);
    safelyVisitChildren(_fields, visitor);
    safelyVisitChildren(_methods, visitor);
    safelyVisitChildren(_toolkitObjects, visitor);
    safelyVisitChildren(_typeParameters, visitor);
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    String name = displayName;
    if (name == null) {
      builder.append("{unnamed class}");
    } else {
      builder.append(name);
    }
    int variableCount = _typeParameters.length;
    if (variableCount > 0) {
      builder.append("<");
      for (int i = 0; i < variableCount; i++) {
        if (i > 0) {
          builder.append(", ");
        }
        (_typeParameters[i] as TypeParameterElementImpl).appendTo(builder);
      }
      builder.append(">");
    }
  }

  void _collectAllSupertypes(List<InterfaceType> supertypes) {
    List<InterfaceType> typesToVisit = new List<InterfaceType>();
    List<ClassElement> visitedClasses = new List<ClassElement>();
    typesToVisit.add(this.type);
    while (!typesToVisit.isEmpty) {
      InterfaceType currentType = typesToVisit.removeAt(0);
      ClassElement currentElement = currentType.element;
      if (!visitedClasses.contains(currentElement)) {
        visitedClasses.add(currentElement);
        if (!identical(currentType, this.type)) {
          supertypes.add(currentType);
        }
        InterfaceType supertype = currentType.superclass;
        if (supertype != null) {
          typesToVisit.add(supertype);
        }
        for (InterfaceType type in currentElement.interfaces) {
          typesToVisit.add(type);
        }
        for (InterfaceType type in currentElement.mixins) {
          ClassElement element = type.element;
          if (!visitedClasses.contains(element)) {
            supertypes.add(type);
          }
        }
      }
    }
  }

  bool _safeIsOrInheritsProxy(ClassElement classElt, Set<ClassElement> visitedClassElts) {
    if (visitedClassElts.contains(classElt)) {
      return false;
    }
    visitedClassElts.add(classElt);
    if (classElt.isProxy) {
      return true;
    } else if (classElt.supertype != null && _safeIsOrInheritsProxy(classElt.supertype.element, visitedClassElts)) {
      return true;
    }
    List<InterfaceType> supertypes = classElt.interfaces;
    for (int i = 0; i < supertypes.length; i++) {
      if (_safeIsOrInheritsProxy(supertypes[i].element, visitedClassElts)) {
        return true;
      }
    }
    supertypes = classElt.mixins;
    for (int i = 0; i < supertypes.length; i++) {
      if (_safeIsOrInheritsProxy(supertypes[i].element, visitedClassElts)) {
        return true;
      }
    }
    return false;
  }
}

/**
 * Instances of the class `CompilationUnitElementImpl` implement a
 * [CompilationUnitElement].
 */
class CompilationUnitElementImpl extends UriReferencedElementImpl implements CompilationUnitElement {
  /**
   * An empty array of compilation unit elements.
   */
  static List<CompilationUnitElement> EMPTY_ARRAY = new List<CompilationUnitElement>(0);

  /**
   * The source that corresponds to this compilation unit.
   */
  Source source;

  /**
   * An array containing all of the top-level accessors (getters and setters) contained in this
   * compilation unit.
   */
  List<PropertyAccessorElement> _accessors = PropertyAccessorElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the top-level functions contained in this compilation unit.
   */
  List<FunctionElement> _functions = FunctionElementImpl.EMPTY_ARRAY;

  /**
   * A table mapping elements to associated toolkit objects.
   */
  Map<Element, List<ToolkitObjectElement>> _toolkitObjects = {};

  /**
   * An array containing all of the function type aliases contained in this compilation unit.
   */
  List<FunctionTypeAliasElement> _typeAliases = FunctionTypeAliasElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the types contained in this compilation unit.
   */
  List<ClassElement> _types = ClassElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the variables contained in this compilation unit.
   */
  List<TopLevelVariableElement> _variables = TopLevelVariableElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the Angular views contained in this compilation unit.
   */
  List<AngularViewElement> _angularViews = AngularViewElement.EMPTY_ARRAY;

  /**
   * Initialize a newly created compilation unit element to have the given name.
   *
   * @param name the name of this element
   */
  CompilationUnitElementImpl(String name) : super(name, -1);

  @override
  accept(ElementVisitor visitor) => visitor.visitCompilationUnitElement(this);

  @override
  bool operator ==(Object object) => object != null && runtimeType == object.runtimeType && source == (object as CompilationUnitElementImpl).source;

  @override
  List<PropertyAccessorElement> get accessors => _accessors;

  @override
  List<AngularViewElement> get angularViews => _angularViews;

  @override
  ElementImpl getChild(String identifier) {
    //
    // The casts in this method are safe because the set methods would have thrown a CCE if any of
    // the elements in the arrays were not of the expected types.
    //
    for (PropertyAccessorElement accessor in _accessors) {
      if ((accessor as PropertyAccessorElementImpl).identifier == identifier) {
        return accessor as PropertyAccessorElementImpl;
      }
    }
    for (VariableElement variable in _variables) {
      if ((variable as VariableElementImpl).identifier == identifier) {
        return variable as VariableElementImpl;
      }
    }
    for (ExecutableElement function in _functions) {
      if ((function as ExecutableElementImpl).identifier == identifier) {
        return function as ExecutableElementImpl;
      }
    }
    for (FunctionTypeAliasElement typeAlias in _typeAliases) {
      if ((typeAlias as FunctionTypeAliasElementImpl).identifier == identifier) {
        return typeAlias as FunctionTypeAliasElementImpl;
      }
    }
    for (ClassElement type in _types) {
      if ((type as ClassElementImpl).identifier == identifier) {
        return type as ClassElementImpl;
      }
    }
    return null;
  }

  @override
  LibraryElement get enclosingElement => super.enclosingElement as LibraryElement;

  @override
  List<FunctionElement> get functions => _functions;

  @override
  List<FunctionTypeAliasElement> get functionTypeAliases => _typeAliases;

  @override
  ElementKind get kind => ElementKind.COMPILATION_UNIT;

  @override
  CompilationUnit get node => unit;

  @override
  List<TopLevelVariableElement> get topLevelVariables => _variables;

  @override
  ClassElement getType(String className) {
    for (ClassElement type in _types) {
      if (type.name == className) {
        return type;
      }
    }
    return null;
  }

  @override
  List<ClassElement> get types => _types;

  @override
  int get hashCode => source.hashCode;

  /**
   * Set the top-level accessors (getters and setters) contained in this compilation unit to the
   * given accessors.
   *
   * @param the top-level accessors (getters and setters) contained in this compilation unit
   */
  void set accessors(List<PropertyAccessorElement> accessors) {
    for (PropertyAccessorElement accessor in accessors) {
      (accessor as PropertyAccessorElementImpl).enclosingElement = this;
    }
    this._accessors = accessors;
  }

  /**
   * Set the Angular views defined in this compilation unit.
   *
   * @param angularViews the Angular views defined in this compilation unit
   */
  void set angularViews(List<AngularViewElement> angularViews) {
    for (AngularViewElement view in angularViews) {
      (view as AngularViewElementImpl).enclosingElement = this;
    }
    this._angularViews = angularViews;
  }

  /**
   * Set the top-level functions contained in this compilation unit to the given functions.
   *
   * @param functions the top-level functions contained in this compilation unit
   */
  void set functions(List<FunctionElement> functions) {
    for (FunctionElement function in functions) {
      (function as FunctionElementImpl).enclosingElement = this;
    }
    this._functions = functions;
  }

  /**
   * Set the top-level variables contained in this compilation unit to the given variables.
   *
   * @param variables the top-level variables contained in this compilation unit
   */
  void set topLevelVariables(List<TopLevelVariableElement> variables) {
    for (TopLevelVariableElement field in variables) {
      (field as TopLevelVariableElementImpl).enclosingElement = this;
    }
    this._variables = variables;
  }

  /**
   * Set the function type aliases contained in this compilation unit to the given type aliases.
   *
   * @param typeAliases the function type aliases contained in this compilation unit
   */
  void set typeAliases(List<FunctionTypeAliasElement> typeAliases) {
    for (FunctionTypeAliasElement typeAlias in typeAliases) {
      (typeAlias as FunctionTypeAliasElementImpl).enclosingElement = this;
    }
    this._typeAliases = typeAliases;
  }

  /**
   * Set the types contained in this compilation unit to the given types.
   *
   * @param types types contained in this compilation unit
   */
  void set types(List<ClassElement> types) {
    for (ClassElement type in types) {
      (type as ClassElementImpl).enclosingElement = this;
    }
    this._types = types;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(_accessors, visitor);
    safelyVisitChildren(_functions, visitor);
    safelyVisitChildren(_typeAliases, visitor);
    safelyVisitChildren(_types, visitor);
    safelyVisitChildren(_variables, visitor);
    safelyVisitChildren(_angularViews, visitor);
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    if (source == null) {
      builder.append("{compilation unit}");
    } else {
      builder.append(source.fullName);
    }
  }

  @override
  String get identifier => source.encoding;

  /**
   * Returns the associated toolkit objects.
   *
   * @param element the [Element] to get toolkit objects for
   * @return the associated toolkit objects, may be empty, but not `null`
   */
  List<ToolkitObjectElement> _getToolkitObjects(Element element) {
    List<ToolkitObjectElement> objects = _toolkitObjects[element];
    if (objects != null) {
      return objects;
    }
    return ToolkitObjectElement.EMPTY_ARRAY;
  }

  /**
   * Sets the toolkit objects that are associated with the given [Element].
   *
   * @param element the [Element] to associate toolkit objects with
   * @param objects the toolkit objects to associate
   */
  void _setToolkitObjects(Element element, List<ToolkitObjectElement> objects) {
    _toolkitObjects[element] = objects;
  }
}

/**
 * Instances of the class `ConstFieldElementImpl` implement a `FieldElement` for a
 * 'const' field that has an initializer.
 */
class ConstFieldElementImpl extends FieldElementImpl {
  /**
   * The result of evaluating this variable's initializer.
   */
  EvaluationResultImpl _result;

  /**
   * Initialize a newly created field element to have the given name.
   *
   * @param name the name of this element
   */
  ConstFieldElementImpl(Identifier name) : super.con1(name);

  @override
  EvaluationResultImpl get evaluationResult => _result;

  @override
  void set evaluationResult(EvaluationResultImpl result) {
    this._result = result;
  }
}

/**
 * Instances of the class `ConstLocalVariableElementImpl` implement a
 * `LocalVariableElement` for a local 'const' variable that has an initializer.
 */
class ConstLocalVariableElementImpl extends LocalVariableElementImpl {
  /**
   * The result of evaluating this variable's initializer.
   */
  EvaluationResultImpl _result;

  /**
   * Initialize a newly created local variable element to have the given name.
   *
   * @param name the name of this element
   */
  ConstLocalVariableElementImpl(Identifier name) : super(name);

  @override
  EvaluationResultImpl get evaluationResult => _result;

  @override
  void set evaluationResult(EvaluationResultImpl result) {
    this._result = result;
  }
}

/**
 * Instances of the class `ConstTopLevelVariableElementImpl` implement a
 * `TopLevelVariableElement` for a top-level 'const' variable that has an initializer.
 */
class ConstTopLevelVariableElementImpl extends TopLevelVariableElementImpl {
  /**
   * The result of evaluating this variable's initializer.
   */
  EvaluationResultImpl _result;

  /**
   * Initialize a newly created top-level variable element to have the given name.
   *
   * @param name the name of this element
   */
  ConstTopLevelVariableElementImpl(Identifier name) : super.con1(name);

  @override
  EvaluationResultImpl get evaluationResult => _result;

  @override
  void set evaluationResult(EvaluationResultImpl result) {
    this._result = result;
  }
}

/**
 * Instances of the class `ConstructorElementImpl` implement a `ConstructorElement`.
 */
class ConstructorElementImpl extends ExecutableElementImpl implements ConstructorElement {
  /**
   * An empty array of constructor elements.
   */
  static List<ConstructorElement> EMPTY_ARRAY = new List<ConstructorElement>(0);

  /**
   * The constructor to which this constructor is redirecting.
   */
  ConstructorElement redirectedConstructor;

  /**
   * Initialize a newly created constructor element to have the given name.
   *
   * @param name the name of this element
   */
  ConstructorElementImpl.con1(Identifier name) : super.con1(name);

  /**
   * Initialize a newly created constructor element to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  ConstructorElementImpl.con2(String name, int nameOffset) : super.con2(name, nameOffset);

  @override
  accept(ElementVisitor visitor) => visitor.visitConstructorElement(this);

  @override
  ClassElement get enclosingElement => super.enclosingElement as ClassElement;

  @override
  ElementKind get kind => ElementKind.CONSTRUCTOR;

  @override
  ConstructorDeclaration get node => getNodeMatching((node) => node is ConstructorDeclaration);

  @override
  bool get isConst => hasModifier(Modifier.CONST);

  @override
  bool get isDefaultConstructor {
    // unnamed
    String name = this.name;
    if (name != null && name.length != 0) {
      return false;
    }
    // no required parameters
    for (ParameterElement parameter in parameters) {
      if (parameter.parameterKind == ParameterKind.REQUIRED) {
        return false;
      }
    }
    // OK, can be used as default constructor
    return true;
  }

  @override
  bool get isFactory => hasModifier(Modifier.FACTORY);

  @override
  bool get isStatic => false;

  /**
   * Set whether this constructor represents a 'const' constructor to the given value.
   *
   * @param isConst `true` if this constructor represents a 'const' constructor
   */
  void set const2(bool isConst) {
    setModifier(Modifier.CONST, isConst);
  }

  /**
   * Set whether this constructor represents a factory method to the given value.
   *
   * @param isFactory `true` if this constructor represents a factory method
   */
  void set factory(bool isFactory) {
    setModifier(Modifier.FACTORY, isFactory);
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append(enclosingElement.displayName);
    String name = displayName;
    if (name != null && !name.isEmpty) {
      builder.append(".");
      builder.append(name);
    }
    super.appendTo(builder);
  }
}

/**
 * Instances of the class `DefaultFieldFormalParameterElementImpl` implement a
 * `FieldFormalParameterElementImpl` for parameters that have an initializer.
 */
class DefaultFieldFormalParameterElementImpl extends FieldFormalParameterElementImpl {
  /**
   * The result of evaluating this variable's initializer.
   */
  EvaluationResultImpl _result;

  /**
   * Initialize a newly created parameter element to have the given name.
   *
   * @param name the name of this element
   */
  DefaultFieldFormalParameterElementImpl(Identifier name) : super(name);

  @override
  EvaluationResultImpl get evaluationResult => _result;

  @override
  void set evaluationResult(EvaluationResultImpl result) {
    this._result = result;
  }
}

/**
 * Instances of the class `DefaultParameterElementImpl` implement a `ParameterElement`
 * for parameters that have an initializer.
 */
class DefaultParameterElementImpl extends ParameterElementImpl {
  /**
   * The result of evaluating this variable's initializer.
   */
  EvaluationResultImpl _result;

  /**
   * Initialize a newly created parameter element to have the given name.
   *
   * @param name the name of this element
   */
  DefaultParameterElementImpl(Identifier name) : super.con1(name);

  @override
  EvaluationResultImpl get evaluationResult => _result;

  @override
  void set evaluationResult(EvaluationResultImpl result) {
    this._result = result;
  }
}

/**
 * Instances of the class `DynamicElementImpl` represent the synthetic element representing
 * the declaration of the type `dynamic`.
 */
class DynamicElementImpl extends ElementImpl {
  /**
   * Return the unique instance of this class.
   *
   * @return the unique instance of this class
   */
  static DynamicElementImpl get instance => DynamicTypeImpl.instance.element as DynamicElementImpl;

  /**
   * The type defined by this element.
   */
  DynamicTypeImpl type;

  /**
   * Initialize a newly created instance of this class. Instances of this class should <b>not</b> be
   * created except as part of creating the type associated with this element. The single instance
   * of this class should be accessed through the method [getInstance].
   */
  DynamicElementImpl() : super(Keyword.DYNAMIC.syntax, -1) {
    setModifier(Modifier.SYNTHETIC, true);
  }

  @override
  accept(ElementVisitor visitor) => null;

  @override
  ElementKind get kind => ElementKind.DYNAMIC;
}

/**
 * Instances of the class `ElementAnnotationImpl` implement an [ElementAnnotation].
 */
class ElementAnnotationImpl implements ElementAnnotation {
  /**
   * The element representing the field, variable, or constructor being used as an annotation.
   */
  final Element element;

  /**
   * An empty array of annotations.
   */
  static List<ElementAnnotationImpl> EMPTY_ARRAY = new List<ElementAnnotationImpl>(0);

  /**
   * The name of the class used to mark an element as being deprecated.
   */
  static String _DEPRECATED_CLASS_NAME = "Deprecated";

  /**
   * The name of the top-level variable used to mark an element as being deprecated.
   */
  static String _DEPRECATED_VARIABLE_NAME = "deprecated";

  /**
   * The name of the top-level variable used to mark a method as being expected to override an
   * inherited method.
   */
  static String _OVERRIDE_VARIABLE_NAME = "override";

  /**
   * The name of the top-level variable used to mark a class as implementing a proxy object.
   */
  static String _PROXY_VARIABLE_NAME = "proxy";

  /**
   * Initialize a newly created annotation.
   *
   * @param element the element representing the field, variable, or constructor being used as an
   *          annotation
   */
  ElementAnnotationImpl(this.element);

  @override
  bool get isDeprecated {
    if (element != null) {
      LibraryElement library = element.library;
      if (library != null && library.isDartCore) {
        if (element is ConstructorElement) {
          ConstructorElement constructorElement = element as ConstructorElement;
          if (constructorElement.enclosingElement.name == _DEPRECATED_CLASS_NAME) {
            return true;
          }
        } else if (element is PropertyAccessorElement && element.name == _DEPRECATED_VARIABLE_NAME) {
          return true;
        }
      }
    }
    return false;
  }

  @override
  bool get isOverride {
    if (element != null) {
      LibraryElement library = element.library;
      if (library != null && library.isDartCore) {
        if (element is PropertyAccessorElement && element.name == _OVERRIDE_VARIABLE_NAME) {
          return true;
        }
      }
    }
    return false;
  }

  @override
  bool get isProxy {
    if (element != null) {
      LibraryElement library = element.library;
      if (library != null && library.isDartCore) {
        if (element is PropertyAccessorElement && element.name == _PROXY_VARIABLE_NAME) {
          return true;
        }
      }
    }
    return false;
  }

  @override
  String toString() => "@${element.toString()}";
}

/**
 * The abstract class `ElementImpl` implements the behavior common to objects that implement
 * an [Element].
 */
abstract class ElementImpl implements Element {
  /**
   * The enclosing element of this element, or `null` if this element is at the root of the
   * element structure.
   */
  ElementImpl _enclosingElement;

  /**
   * The name of this element.
   */
  String _name;

  /**
   * The offset of the name of this element in the file that contains the declaration of this
   * element.
   */
  int nameOffset = 0;

  /**
   * A bit-encoded form of the modifiers associated with this element.
   */
  int _modifiers = 0;

  /**
   * An array containing all of the metadata associated with this element.
   */
  List<ElementAnnotation> metadata = ElementAnnotationImpl.EMPTY_ARRAY;

  /**
   * A cached copy of the calculated hashCode for this element.
   */
  int _cachedHashCode = 0;

  /**
   * Initialize a newly created element to have the given name.
   *
   * @param name the name of this element
   */
  ElementImpl.forNode(Identifier name) : this(name == null ? "" : name.name, name == null ? -1 : name.offset);

  /**
   * Initialize a newly created element to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  ElementImpl(String name, this.nameOffset) {
    this._name = StringUtilities.intern(name);
  }

  @override
  String computeDocumentationComment() {
    AnalysisContext context = this.context;
    if (context == null) {
      return null;
    }
    return context.computeDocumentationComment(this);
  }

  @override
  bool operator ==(Object object) {
    if (identical(this, object)) {
      return true;
    }
    if (object == null || hashCode != object.hashCode) {
      return false;
    }
    return object.runtimeType == runtimeType && (object as Element).location == location;
  }

  @override
  Element getAncestor(Predicate<Element> predicate) {
    Element ancestor = _enclosingElement;
    while (ancestor != null && !predicate(ancestor)) {
      ancestor = ancestor.enclosingElement;
    }
    return ancestor;
  }

  /**
   * Return the child of this element that is uniquely identified by the given identifier, or
   * `null` if there is no such child.
   *
   * @param identifier the identifier used to select a child
   * @return the child of this element with the given identifier
   */
  ElementImpl getChild(String identifier) => null;

  @override
  AnalysisContext get context {
    if (_enclosingElement == null) {
      return null;
    }
    return _enclosingElement.context;
  }

  @override
  String get displayName => _name;

  @override
  Element get enclosingElement => _enclosingElement;

  @override
  LibraryElement get library => getAncestor((element) => element is LibraryElement);

  @override
  ElementLocation get location => new ElementLocationImpl.con1(this);

  @override
  String get name => _name;

  @override
  AstNode get node => getNodeMatching((node) => node is AstNode);

  @override
  Source get source {
    if (_enclosingElement == null) {
      return null;
    }
    return _enclosingElement.source;
  }

  @override
  CompilationUnit get unit => context.resolveCompilationUnit(source, library);

  @override
  int get hashCode {
    // TODO: We might want to re-visit this optimization in the future.
    // We cache the hash code value as this is a very frequently called method.
    if (_cachedHashCode == 0) {
      int hashIdentifier = identifier.hashCode;
      Element enclosing = enclosingElement;
      if (enclosing != null) {
        _cachedHashCode = hashIdentifier + enclosing.hashCode;
      } else {
        _cachedHashCode = hashIdentifier;
      }
    }
    return _cachedHashCode;
  }

  @override
  bool isAccessibleIn(LibraryElement library) {
    if (Identifier.isPrivateName(_name)) {
      return library == this.library;
    }
    return true;
  }

  @override
  bool get isDeprecated {
    for (ElementAnnotation annotation in metadata) {
      if (annotation.isDeprecated) {
        return true;
      }
    }
    return false;
  }

  @override
  bool get isOverride {
    for (ElementAnnotation annotation in metadata) {
      if (annotation.isOverride) {
        return true;
      }
    }
    return false;
  }

  @override
  bool get isPrivate {
    String name = displayName;
    if (name == null) {
      return true;
    }
    return Identifier.isPrivateName(name);
  }

  @override
  bool get isPublic => !isPrivate;

  @override
  bool get isSynthetic => hasModifier(Modifier.SYNTHETIC);

  /**
   * Set whether this element is synthetic to correspond to the given value.
   *
   * @param isSynthetic `true` if the element is synthetic
   */
  void set synthetic(bool isSynthetic) {
    setModifier(Modifier.SYNTHETIC, isSynthetic);
  }

  @override
  String toString() {
    JavaStringBuilder builder = new JavaStringBuilder();
    appendTo(builder);
    return builder.toString();
  }

  @override
  void visitChildren(ElementVisitor visitor) {
  }

  /**
   * Append a textual representation of this type to the given builder.
   *
   * @param builder the builder to which the text is to be appended
   */
  void appendTo(JavaStringBuilder builder) {
    if (_name == null) {
      builder.append("<unnamed ");
      builder.append(runtimeType.toString());
      builder.append(">");
    } else {
      builder.append(_name);
    }
  }

  /**
   * Set this [Element] as an enclosing for given.
   *
   * @param element the element to enclose, must be [ElementImpl]
   */
  void encloseElement(ElementImpl element) {
    element.enclosingElement = this;
  }

  /**
   * Return an identifier that uniquely identifies this element among the children of this element's
   * parent.
   *
   * @return an identifier that uniquely identifies this element relative to its parent
   */
  String get identifier => name;

  /**
   * Return the resolved [AstNode] of the given type enclosing [getNameOffset].
   */
  AstNode getNodeMatching(Predicate<AstNode> predicate) {
    CompilationUnit unit = this.unit;
    if (unit == null) {
      return null;
    }
    int offset = nameOffset;
    AstNode node = new NodeLocator.con1(offset).searchWithin(unit);
    if (node == null) {
      return null;
    }
    return node.getAncestor(predicate);
  }

  /**
   * Return `true` if this element has the given modifier associated with it.
   *
   * @param modifier the modifier being tested for
   * @return `true` if this element has the given modifier associated with it
   */
  bool hasModifier(Modifier modifier) => BooleanArray.getEnum(_modifiers, modifier);

  /**
   * If the given child is not `null`, use the given visitor to visit it.
   *
   * @param child the child to be visited
   * @param visitor the visitor to be used to visit the child
   */
  void safelyVisitChild(Element child, ElementVisitor visitor) {
    if (child != null) {
      child.accept(visitor);
    }
  }

  /**
   * Use the given visitor to visit all of the children in the given array.
   *
   * @param children the children to be visited
   * @param visitor the visitor being used to visit the children
   */
  void safelyVisitChildren(List<Element> children, ElementVisitor visitor) {
    if (children != null) {
      for (Element child in children) {
        child.accept(visitor);
      }
    }
  }

  /**
   * Set the enclosing element of this element to the given element.
   *
   * @param element the enclosing element of this element
   */
  void set enclosingElement(Element element) {
    _enclosingElement = element as ElementImpl;
  }

  /**
   * Set whether the given modifier is associated with this element to correspond to the given
   * value.
   *
   * @param modifier the modifier to be set
   * @param value `true` if the modifier is to be associated with this element
   */
  void setModifier(Modifier modifier, bool value) {
    _modifiers = BooleanArray.setEnum(_modifiers, modifier, value);
  }
}

/**
 * Instances of the class `ElementLocationImpl` implement an [ElementLocation].
 */
class ElementLocationImpl implements ElementLocation {
  /**
   * The path to the element whose location is represented by this object.
   */
  List<String> _components;

  /**
   * The character used to separate components in the encoded form.
   */
  static int _SEPARATOR_CHAR = 0x3B;

  /**
   * Initialize a newly created location to represent the given element.
   *
   * @param element the element whose location is being represented
   */
  ElementLocationImpl.con1(Element element) {
    List<String> components = new List<String>();
    Element ancestor = element;
    while (ancestor != null) {
      components.insert(0, (ancestor as ElementImpl).identifier);
      ancestor = ancestor.enclosingElement;
    }
    this._components = new List.from(components);
  }

  /**
   * Initialize a newly created location from the given encoded form.
   *
   * @param encoding the encoded form of a location
   */
  ElementLocationImpl.con2(String encoding) {
    this._components = _decode(encoding);
  }

  @override
  bool operator ==(Object object) {
    if (identical(this, object)) {
      return true;
    }
    if (object is! ElementLocationImpl) {
      return false;
    }
    ElementLocationImpl location = object as ElementLocationImpl;
    List<String> otherComponents = location._components;
    int length = _components.length;
    if (otherComponents.length != length) {
      return false;
    }
    for (int i = length - 1; i >= 2; i--) {
      if (_components[i] != otherComponents[i]) {
        return false;
      }
    }
    if (length > 1 && !_equalSourceComponents(_components[1], otherComponents[1])) {
      return false;
    }
    if (length > 0 && !_equalSourceComponents(_components[0], otherComponents[0])) {
      return false;
    }
    return true;
  }

  /**
   * Return the path to the element whose location is represented by this object.
   *
   * @return the path to the element whose location is represented by this object
   */
  List<String> get components => _components;

  @override
  String get encoding {
    JavaStringBuilder builder = new JavaStringBuilder();
    int length = _components.length;
    for (int i = 0; i < length; i++) {
      if (i > 0) {
        builder.appendChar(_SEPARATOR_CHAR);
      }
      _encode(builder, _components[i]);
    }
    return builder.toString();
  }

  @override
  int get hashCode {
    int result = 1;
    for (int i = 0; i < _components.length; i++) {
      String component = _components[i];
      int componentHash;
      if (i <= 1) {
        componentHash = _hashSourceComponent(component);
      } else {
        componentHash = component.hashCode;
      }
      result = 31 * result + componentHash;
    }
    return result;
  }

  @override
  String toString() => encoding;

  /**
   * Decode the encoded form of a location into an array of components.
   *
   * @param encoding the encoded form of a location
   * @return the components that were encoded
   */
  List<String> _decode(String encoding) {
    List<String> components = new List<String>();
    JavaStringBuilder builder = new JavaStringBuilder();
    int index = 0;
    int length = encoding.length;
    while (index < length) {
      int currentChar = encoding.codeUnitAt(index);
      if (currentChar == _SEPARATOR_CHAR) {
        if (index + 1 < length && encoding.codeUnitAt(index + 1) == _SEPARATOR_CHAR) {
          builder.appendChar(_SEPARATOR_CHAR);
          index += 2;
        } else {
          components.add(builder.toString());
          builder.length = 0;
          index++;
        }
      } else {
        builder.appendChar(currentChar);
        index++;
      }
    }
    if (builder.length > 0) {
      components.add(builder.toString());
    }
    return new List.from(components);
  }

  /**
   * Append an encoded form of the given component to the given builder.
   *
   * @param builder the builder to which the encoded component is to be appended
   * @param component the component to be appended to the builder
   */
  void _encode(JavaStringBuilder builder, String component) {
    int length = component.length;
    for (int i = 0; i < length; i++) {
      int currentChar = component.codeUnitAt(i);
      if (currentChar == _SEPARATOR_CHAR) {
        builder.appendChar(_SEPARATOR_CHAR);
      }
      builder.appendChar(currentChar);
    }
  }

  /**
   * Return `true` if the given components, when interpreted to be encoded sources with a
   * leading source type indicator, are equal when the source type's are ignored.
   *
   * @param left the left component being compared
   * @param right the right component being compared
   * @return `true` if the given components are equal when the source type's are ignored
   */
  bool _equalSourceComponents(String left, String right) {
    // TODO(brianwilkerson) This method can go away when sources no longer have a URI kind.
    if (left == null) {
      return right == null;
    } else if (right == null) {
      return false;
    }
    int leftLength = left.length;
    int rightLength = right.length;
    if (leftLength != rightLength) {
      return false;
    } else if (leftLength <= 1 || rightLength <= 1) {
      return left == right;
    }
    return javaStringRegionMatches(left, 1, right, 1, leftLength - 1);
  }

  /**
   * Return the hash code of the given encoded source component, ignoring the source type indicator.
   *
   * @param sourceComponent the component to compute a hash code
   * @return the hash code of the given encoded source component
   */
  int _hashSourceComponent(String sourceComponent) {
    // TODO(brianwilkerson) This method can go away when sources no longer have a URI kind.
    if (sourceComponent.length <= 1) {
      return sourceComponent.hashCode;
    }
    return sourceComponent.substring(1).hashCode;
  }
}

/**
 * The class `ElementPair` is a pair of [Element]s. [Object#equals] and
 * [Object#hashCode] so this class can be used in hashed data structures.
 */
class ElementPair {
  /**
   * The first [Element]
   */
  final Element _first;

  /**
   * The second [Element]
   */
  final Element _second;

  /**
   * The sole constructor for this class, taking two [Element]s.
   *
   * @param first the first element
   * @param second the second element
   */
  ElementPair(this._first, this._second);

  @override
  bool operator ==(Object object) {
    if (identical(object, this)) {
      return true;
    }
    if (object is ElementPair) {
      ElementPair elementPair = object;
      return (_first == elementPair._first) && (_second == elementPair._second);
    }
    return false;
  }

  /**
   * Return the first element.
   *
   * @return the first element
   */
  Element get firstElt => _first;

  /**
   * Return the second element
   *
   * @return the second element
   */
  Element get secondElt => _second;

  @override
  int get hashCode => ObjectUtilities.combineHashCodes(_first.hashCode, _second.hashCode);
}

/**
 * Instances of the class `EmbeddedHtmlScriptElementImpl` implement an
 * [EmbeddedHtmlScriptElement].
 */
class EmbeddedHtmlScriptElementImpl extends HtmlScriptElementImpl implements EmbeddedHtmlScriptElement {
  /**
   * The library defined by the script tag's content.
   */
  LibraryElement _scriptLibrary;

  /**
   * Initialize a newly created script element to have the specified tag name and offset.
   *
   * @param node the XML node from which this element is derived (not `null`)
   */
  EmbeddedHtmlScriptElementImpl(XmlTagNode node) : super(node);

  @override
  accept(ElementVisitor visitor) => visitor.visitEmbeddedHtmlScriptElement(this);

  @override
  ElementKind get kind => ElementKind.EMBEDDED_HTML_SCRIPT;

  @override
  LibraryElement get scriptLibrary => _scriptLibrary;

  /**
   * Set the script library defined by the script tag's content.
   *
   * @param scriptLibrary the library or `null` if none
   */
  void set scriptLibrary(LibraryElementImpl scriptLibrary) {
    scriptLibrary.enclosingElement = this;
    this._scriptLibrary = scriptLibrary;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    safelyVisitChild(_scriptLibrary, visitor);
  }
}

/**
 * The abstract class `ExecutableElementImpl` implements the behavior common to
 * `ExecutableElement`s.
 */
abstract class ExecutableElementImpl extends ElementImpl implements ExecutableElement {
  /**
   * An array containing all of the functions defined within this executable element.
   */
  List<FunctionElement> _functions = FunctionElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the labels defined within this executable element.
   */
  List<LabelElement> _labels = LabelElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the local variables defined within this executable element.
   */
  List<LocalVariableElement> _localVariables = LocalVariableElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the parameters defined by this executable element.
   */
  List<ParameterElement> _parameters = ParameterElementImpl.EMPTY_ARRAY;

  /**
   * The return type defined by this executable element.
   */
  DartType returnType;

  /**
   * The type of function defined by this executable element.
   */
  FunctionType type;

  /**
   * An empty array of executable elements.
   */
  static List<ExecutableElement> EMPTY_ARRAY = new List<ExecutableElement>(0);

  /**
   * Initialize a newly created executable element to have the given name.
   *
   * @param name the name of this element
   */
  ExecutableElementImpl.con1(Identifier name) : super.forNode(name);

  /**
   * Initialize a newly created executable element to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  ExecutableElementImpl.con2(String name, int nameOffset) : super(name, nameOffset);

  @override
  ElementImpl getChild(String identifier) {
    for (ExecutableElement function in _functions) {
      if ((function as ExecutableElementImpl).identifier == identifier) {
        return function as ExecutableElementImpl;
      }
    }
    for (LabelElement label in _labels) {
      if ((label as LabelElementImpl).identifier == identifier) {
        return label as LabelElementImpl;
      }
    }
    for (VariableElement variable in _localVariables) {
      if ((variable as VariableElementImpl).identifier == identifier) {
        return variable as VariableElementImpl;
      }
    }
    for (ParameterElement parameter in _parameters) {
      if ((parameter as ParameterElementImpl).identifier == identifier) {
        return parameter as ParameterElementImpl;
      }
    }
    return null;
  }

  @override
  List<FunctionElement> get functions => _functions;

  @override
  List<LabelElement> get labels => _labels;

  @override
  List<LocalVariableElement> get localVariables => _localVariables;

  @override
  List<ParameterElement> get parameters => _parameters;

  @override
  bool get isOperator => false;

  /**
   * Set the functions defined within this executable element to the given functions.
   *
   * @param functions the functions defined within this executable element
   */
  void set functions(List<FunctionElement> functions) {
    for (FunctionElement function in functions) {
      (function as FunctionElementImpl).enclosingElement = this;
    }
    this._functions = functions;
  }

  /**
   * Set the labels defined within this executable element to the given labels.
   *
   * @param labels the labels defined within this executable element
   */
  void set labels(List<LabelElement> labels) {
    for (LabelElement label in labels) {
      (label as LabelElementImpl).enclosingElement = this;
    }
    this._labels = labels;
  }

  /**
   * Set the local variables defined within this executable element to the given variables.
   *
   * @param localVariables the local variables defined within this executable element
   */
  void set localVariables(List<LocalVariableElement> localVariables) {
    for (LocalVariableElement variable in localVariables) {
      (variable as LocalVariableElementImpl).enclosingElement = this;
    }
    this._localVariables = localVariables;
  }

  /**
   * Set the parameters defined by this executable element to the given parameters.
   *
   * @param parameters the parameters defined by this executable element
   */
  void set parameters(List<ParameterElement> parameters) {
    for (ParameterElement parameter in parameters) {
      (parameter as ParameterElementImpl).enclosingElement = this;
    }
    this._parameters = parameters;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(_functions, visitor);
    safelyVisitChildren(_labels, visitor);
    safelyVisitChildren(_localVariables, visitor);
    safelyVisitChildren(_parameters, visitor);
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    if (this.kind != ElementKind.GETTER) {
      builder.append("(");
      String closing = null;
      ParameterKind kind = ParameterKind.REQUIRED;
      int parameterCount = _parameters.length;
      for (int i = 0; i < parameterCount; i++) {
        if (i > 0) {
          builder.append(", ");
        }
        ParameterElementImpl parameter = _parameters[i] as ParameterElementImpl;
        ParameterKind parameterKind = parameter.parameterKind;
        if (parameterKind != kind) {
          if (closing != null) {
            builder.append(closing);
          }
          if (parameterKind == ParameterKind.POSITIONAL) {
            builder.append("[");
            closing = "]";
          } else if (parameterKind == ParameterKind.NAMED) {
            builder.append("{");
            closing = "}";
          } else {
            closing = null;
          }
        }
        kind = parameterKind;
        parameter.appendToWithoutDelimiters(builder);
      }
      if (closing != null) {
        builder.append(closing);
      }
      builder.append(")");
    }
    if (type != null) {
      builder.append(Element.RIGHT_ARROW);
      builder.append(type.returnType);
    }
  }
}

/**
 * Instances of the class `ExportElementImpl` implement an [ExportElement].
 */
class ExportElementImpl extends UriReferencedElementImpl implements ExportElement {
  /**
   * The library that is exported from this library by this export directive.
   */
  LibraryElement exportedLibrary;

  /**
   * The combinators that were specified as part of the export directive in the order in which they
   * were specified.
   */
  List<NamespaceCombinator> combinators = NamespaceCombinator.EMPTY_ARRAY;

  /**
   * Initialize a newly created export element.
   */
  ExportElementImpl() : super(null, -1);

  @override
  accept(ElementVisitor visitor) => visitor.visitExportElement(this);

  @override
  ElementKind get kind => ElementKind.EXPORT;

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append("export ");
    (exportedLibrary as LibraryElementImpl).appendTo(builder);
  }

  @override
  String get identifier => exportedLibrary.name;
}

/**
 * Instances of the class `ExternalHtmlScriptElementImpl` implement an
 * [ExternalHtmlScriptElement].
 */
class ExternalHtmlScriptElementImpl extends HtmlScriptElementImpl implements ExternalHtmlScriptElement {
  /**
   * The source specified in the `source` attribute or `null` if unspecified.
   */
  Source scriptSource;

  /**
   * Initialize a newly created script element to have the specified tag name and offset.
   *
   * @param node the XML node from which this element is derived (not `null`)
   */
  ExternalHtmlScriptElementImpl(XmlTagNode node) : super(node);

  @override
  accept(ElementVisitor visitor) => visitor.visitExternalHtmlScriptElement(this);

  @override
  ElementKind get kind => ElementKind.EXTERNAL_HTML_SCRIPT;
}

/**
 * Instances of the class `FieldElementImpl` implement a `FieldElement`.
 */
class FieldElementImpl extends PropertyInducingElementImpl implements FieldElement {
  /**
   * An empty array of field elements.
   */
  static List<FieldElement> EMPTY_ARRAY = new List<FieldElement>(0);

  /**
   * Initialize a newly created field element to have the given name.
   *
   * @param name the name of this element
   */
  FieldElementImpl.con1(Identifier name) : super.con1(name);

  /**
   * Initialize a newly created synthetic field element to have the given name.
   *
   * @param name the name of this element
   */
  FieldElementImpl.con2(String name) : super.con2(name);

  @override
  accept(ElementVisitor visitor) => visitor.visitFieldElement(this);

  @override
  ClassElement get enclosingElement => super.enclosingElement as ClassElement;

  @override
  ElementKind get kind => ElementKind.FIELD;

  @override
  bool get isStatic => hasModifier(Modifier.STATIC);

  /**
   * Set whether this field is static to correspond to the given value.
   *
   * @param isStatic `true` if the field is static
   */
  void set static(bool isStatic) {
    setModifier(Modifier.STATIC, isStatic);
  }
}

/**
 * Instances of the class `FieldFormalParameterElementImpl` extend
 * [ParameterElementImpl] to provide the additional information of the [FieldElement]
 * associated with the parameter.
 */
class FieldFormalParameterElementImpl extends ParameterElementImpl implements FieldFormalParameterElement {
  /**
   * The field associated with this field formal parameter.
   */
  FieldElement field;

  /**
   * Initialize a newly created parameter element to have the given name.
   *
   * @param name the name of this element
   */
  FieldFormalParameterElementImpl(Identifier name) : super.con1(name);

  @override
  accept(ElementVisitor visitor) => visitor.visitFieldFormalParameterElement(this);

  @override
  bool get isInitializingFormal => true;
}

/**
 * Instances of the class `FunctionElementImpl` implement a `FunctionElement`.
 */
class FunctionElementImpl extends ExecutableElementImpl implements FunctionElement {
  /**
   * The offset to the beginning of the visible range for this element.
   */
  int _visibleRangeOffset = 0;

  /**
   * The length of the visible range for this element, or `-1` if this element does not have a
   * visible range.
   */
  int _visibleRangeLength = -1;

  /**
   * An empty array of function elements.
   */
  static List<FunctionElement> EMPTY_ARRAY = new List<FunctionElement>(0);

  /**
   * Initialize a newly created function element to have the given name.
   *
   * @param name the name of this element
   */
  FunctionElementImpl.con1(Identifier name) : super.con1(name);

  /**
   * Initialize a newly created function element to have no name and the given offset. This is used
   * for function expressions, which have no name.
   *
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  FunctionElementImpl.con2(int nameOffset) : super.con2("", nameOffset);

  @override
  accept(ElementVisitor visitor) => visitor.visitFunctionElement(this);

  @override
  ElementKind get kind => ElementKind.FUNCTION;

  @override
  FunctionDeclaration get node => getNodeMatching((node) => node is FunctionDeclaration);

  @override
  SourceRange get visibleRange {
    if (_visibleRangeLength < 0) {
      return null;
    }
    return new SourceRange(_visibleRangeOffset, _visibleRangeLength);
  }

  @override
  bool get isStatic => enclosingElement is CompilationUnitElement;

  /**
   * Set the visible range for this element to the range starting at the given offset with the given
   * length.
   *
   * @param offset the offset to the beginning of the visible range for this element
   * @param length the length of the visible range for this element, or `-1` if this element
   *          does not have a visible range
   */
  void setVisibleRange(int offset, int length) {
    _visibleRangeOffset = offset;
    _visibleRangeLength = length;
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    String name = displayName;
    if (name != null) {
      builder.append(name);
    }
    super.appendTo(builder);
  }

  @override
  String get identifier => "${name}@${nameOffset}";
}

/**
 * Instances of the class `FunctionTypeAliasElementImpl` implement a
 * `FunctionTypeAliasElement`.
 */
class FunctionTypeAliasElementImpl extends ElementImpl implements FunctionTypeAliasElement {
  /**
   * An array containing all of the parameters defined by this type alias.
   */
  List<ParameterElement> _parameters = ParameterElementImpl.EMPTY_ARRAY;

  /**
   * The return type defined by this type alias.
   */
  DartType returnType;

  /**
   * The type of function defined by this type alias.
   */
  FunctionType type;

  /**
   * An array containing all of the type parameters defined for this type.
   */
  List<TypeParameterElement> _typeParameters = TypeParameterElementImpl.EMPTY_ARRAY;

  /**
   * An empty array of type alias elements.
   */
  static List<FunctionTypeAliasElement> EMPTY_ARRAY = new List<FunctionTypeAliasElement>(0);

  /**
   * Initialize a newly created type alias element to have the given name.
   *
   * @param name the name of this element
   */
  FunctionTypeAliasElementImpl(Identifier name) : super.forNode(name);

  @override
  accept(ElementVisitor visitor) => visitor.visitFunctionTypeAliasElement(this);

  @override
  ElementImpl getChild(String identifier) {
    for (VariableElement parameter in _parameters) {
      if ((parameter as VariableElementImpl).identifier == identifier) {
        return parameter as VariableElementImpl;
      }
    }
    for (TypeParameterElement typeParameter in _typeParameters) {
      if ((typeParameter as TypeParameterElementImpl).identifier == identifier) {
        return typeParameter as TypeParameterElementImpl;
      }
    }
    return null;
  }

  @override
  CompilationUnitElement get enclosingElement => super.enclosingElement as CompilationUnitElement;

  @override
  ElementKind get kind => ElementKind.FUNCTION_TYPE_ALIAS;

  @override
  FunctionTypeAlias get node => getNodeMatching((node) => node is FunctionTypeAlias);

  @override
  List<ParameterElement> get parameters => _parameters;

  @override
  List<TypeParameterElement> get typeParameters => _typeParameters;

  /**
   * Set the parameters defined by this type alias to the given parameters.
   *
   * @param parameters the parameters defined by this type alias
   */
  void set parameters(List<ParameterElement> parameters) {
    if (parameters != null) {
      for (ParameterElement parameter in parameters) {
        (parameter as ParameterElementImpl).enclosingElement = this;
      }
    }
    this._parameters = parameters;
  }

  /**
   * Set the type parameters defined for this type to the given parameters.
   *
   * @param typeParameters the type parameters defined for this type
   */
  void set typeParameters(List<TypeParameterElement> typeParameters) {
    for (TypeParameterElement typeParameter in typeParameters) {
      (typeParameter as TypeParameterElementImpl).enclosingElement = this;
    }
    this._typeParameters = typeParameters;
  }

  /**
   * Set the parameters defined by this type alias to the given parameters without becoming the
   * parent of the parameters. This should only be used by the [TypeResolverVisitor] when
   * creating a synthetic type alias.
   *
   * @param parameters the parameters defined by this type alias
   */
  void shareParameters(List<ParameterElement> parameters) {
    this._parameters = parameters;
  }

  /**
   * Set the type parameters defined for this type to the given parameters without becoming the
   * parent of the parameters. This should only be used by the [TypeResolverVisitor] when
   * creating a synthetic type alias.
   *
   * @param typeParameters the type parameters defined for this type
   */
  void shareTypeParameters(List<TypeParameterElement> typeParameters) {
    this._typeParameters = typeParameters;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(_parameters, visitor);
    safelyVisitChildren(_typeParameters, visitor);
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append("typedef ");
    builder.append(displayName);
    int typeParameterCount = _typeParameters.length;
    if (typeParameterCount > 0) {
      builder.append("<");
      for (int i = 0; i < typeParameterCount; i++) {
        if (i > 0) {
          builder.append(", ");
        }
        (_typeParameters[i] as TypeParameterElementImpl).appendTo(builder);
      }
      builder.append(">");
    }
    builder.append("(");
    int parameterCount = _parameters.length;
    for (int i = 0; i < parameterCount; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      (_parameters[i] as ParameterElementImpl).appendTo(builder);
    }
    builder.append(")");
    if (type != null) {
      builder.append(Element.RIGHT_ARROW);
      builder.append(type.returnType);
    }
  }
}

/**
 * Instances of the class `HideElementCombinatorImpl` implement a
 * [HideElementCombinator].
 */
class HideElementCombinatorImpl implements HideElementCombinator {
  /**
   * The names that are not to be made visible in the importing library even if they are defined in
   * the imported library.
   */
  List<String> hiddenNames = StringUtilities.EMPTY_ARRAY;

  @override
  String toString() {
    JavaStringBuilder builder = new JavaStringBuilder();
    builder.append("show ");
    int count = hiddenNames.length;
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(hiddenNames[i]);
    }
    return builder.toString();
  }
}

/**
 * Instances of the class `HtmlElementImpl` implement an [HtmlElement].
 */
class HtmlElementImpl extends ElementImpl implements HtmlElement {
  /**
   * An empty array of HTML file elements.
   */
  static List<HtmlElement> EMPTY_ARRAY = new List<HtmlElement>(0);

  /**
   * The analysis context in which this library is defined.
   */
  final AnalysisContext context;

  /**
   * The scripts contained in or referenced from script tags in the HTML file.
   */
  List<HtmlScriptElement> _scripts = HtmlScriptElementImpl.EMPTY_ARRAY;

  /**
   * The source that corresponds to this HTML file.
   */
  Source source;

  /**
   * The element associated with Dart pieces in this HTML unit or `null` if the receiver is
   * not resolved.
   */
  CompilationUnitElement angularCompilationUnit;

  /**
   * Initialize a newly created HTML element to have the given name.
   *
   * @param context the analysis context in which the HTML file is defined
   * @param name the name of this element
   */
  HtmlElementImpl(this.context, String name) : super(name, -1);

  @override
  accept(ElementVisitor visitor) => visitor.visitHtmlElement(this);

  @override
  bool operator ==(Object object) {
    if (identical(object, this)) {
      return true;
    }
    if (object == null) {
      return false;
    }
    return runtimeType == object.runtimeType && source == (object as HtmlElementImpl).source;
  }

  @override
  ElementKind get kind => ElementKind.HTML;

  @override
  List<HtmlScriptElement> get scripts => _scripts;

  @override
  int get hashCode => source.hashCode;

  /**
   * Set the scripts contained in the HTML file to the given scripts.
   *
   * @param scripts the scripts
   */
  void set scripts(List<HtmlScriptElement> scripts) {
    if (scripts.length == 0) {
      scripts = HtmlScriptElementImpl.EMPTY_ARRAY;
    }
    for (HtmlScriptElement script in scripts) {
      (script as HtmlScriptElementImpl).enclosingElement = this;
    }
    this._scripts = scripts;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(_scripts, visitor);
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    if (source == null) {
      builder.append("{HTML file}");
    } else {
      builder.append(source.fullName);
    }
  }
}

/**
 * Instances of the class `HtmlScriptElementImpl` implement an [HtmlScriptElement].
 */
abstract class HtmlScriptElementImpl extends ElementImpl implements HtmlScriptElement {
  /**
   * An empty array of HTML script elements.
   */
  static List<HtmlScriptElement> EMPTY_ARRAY = new List<HtmlScriptElement>(0);

  /**
   * Initialize a newly created script element to have the specified tag name and offset.
   *
   * @param node the XML node from which this element is derived (not `null`)
   */
  HtmlScriptElementImpl(XmlTagNode node) : super(node.tag, node.tagToken.offset);
}

/**
 * Instances of the class `ImportElementImpl` implement an [ImportElement].
 */
class ImportElementImpl extends UriReferencedElementImpl implements ImportElement {
  /**
   * The offset of the prefix of this import in the file that contains the this import directive, or
   * `-1` if this import is synthetic.
   */
  int prefixOffset = 0;

  /**
   * The library that is imported into this library by this import directive.
   */
  LibraryElement importedLibrary;

  /**
   * The combinators that were specified as part of the import directive in the order in which they
   * were specified.
   */
  List<NamespaceCombinator> combinators = NamespaceCombinator.EMPTY_ARRAY;

  /**
   * The prefix that was specified as part of the import directive, or `null` if there was no
   * prefix specified.
   */
  PrefixElement prefix;

  /**
   * Initialize a newly created import element.
   *
   * @param offset the directive offset, may be `-1` if synthetic.
   */
  ImportElementImpl(int offset) : super(null, offset);

  @override
  accept(ElementVisitor visitor) => visitor.visitImportElement(this);

  @override
  ElementKind get kind => ElementKind.IMPORT;

  @override
  void visitChildren(ElementVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(prefix, visitor);
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append("import ");
    (importedLibrary as LibraryElementImpl).appendTo(builder);
  }

  @override
  String get identifier => "${(importedLibrary as LibraryElementImpl).identifier}@${nameOffset}";
}

/**
 * Instances of the class `LabelElementImpl` implement a `LabelElement`.
 */
class LabelElementImpl extends ElementImpl implements LabelElement {
  /**
   * A flag indicating whether this label is associated with a `switch` statement.
   */
  final bool _onSwitchStatement;

  /**
   * A flag indicating whether this label is associated with a `switch` member (`case`
   * or `default`).
   */
  final bool _onSwitchMember;

  /**
   * An empty array of label elements.
   */
  static List<LabelElement> EMPTY_ARRAY = new List<LabelElement>(0);

  /**
   * Initialize a newly created label element to have the given name.
   *
   * @param name the name of this element
   * @param onSwitchStatement `true` if this label is associated with a `switch`
   *          statement
   * @param onSwitchMember `true` if this label is associated with a `switch` member
   */
  LabelElementImpl(Identifier name, this._onSwitchStatement, this._onSwitchMember) : super.forNode(name);

  @override
  accept(ElementVisitor visitor) => visitor.visitLabelElement(this);

  @override
  ExecutableElement get enclosingElement => super.enclosingElement as ExecutableElement;

  @override
  ElementKind get kind => ElementKind.LABEL;

  /**
   * Return `true` if this label is associated with a `switch` member (`case` or
   * `default`).
   *
   * @return `true` if this label is associated with a `switch` member
   */
  bool get isOnSwitchMember => _onSwitchMember;

  /**
   * Return `true` if this label is associated with a `switch` statement.
   *
   * @return `true` if this label is associated with a `switch` statement
   */
  bool get isOnSwitchStatement => _onSwitchStatement;
}

/**
 * Instances of the class `LibraryElementImpl` implement a `LibraryElement`.
 */
class LibraryElementImpl extends ElementImpl implements LibraryElement {
  /**
   * An empty array of library elements.
   */
  static List<LibraryElement> EMPTY_ARRAY = new List<LibraryElement>(0);

  /**
   * Determine if the given library is up to date with respect to the given time stamp.
   *
   * @param library the library to process
   * @param timeStamp the time stamp to check against
   * @param visitedLibraries the set of visited libraries
   */
  static bool _safeIsUpToDate(LibraryElement library, int timeStamp, Set<LibraryElement> visitedLibraries) {
    if (!visitedLibraries.contains(library)) {
      visitedLibraries.add(library);
      AnalysisContext context = library.context;
      // Check the defining compilation unit.
      if (timeStamp < context.getModificationStamp(library.definingCompilationUnit.source)) {
        return false;
      }
      // Check the parted compilation units.
      for (CompilationUnitElement element in library.parts) {
        if (timeStamp < context.getModificationStamp(element.source)) {
          return false;
        }
      }
      // Check the imported libraries.
      for (LibraryElement importedLibrary in library.importedLibraries) {
        if (!_safeIsUpToDate(importedLibrary, timeStamp, visitedLibraries)) {
          return false;
        }
      }
      // Check the exported libraries.
      for (LibraryElement exportedLibrary in library.exportedLibraries) {
        if (!_safeIsUpToDate(exportedLibrary, timeStamp, visitedLibraries)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * The analysis context in which this library is defined.
   */
  final AnalysisContext context;

  /**
   * The compilation unit that defines this library.
   */
  CompilationUnitElement _definingCompilationUnit;

  /**
   * The entry point for this library, or `null` if this library does not have an entry point.
   */
  FunctionElement entryPoint;

  /**
   * An array containing specifications of all of the imports defined in this library.
   */
  List<ImportElement> _imports = ImportElement.EMPTY_ARRAY;

  /**
   * An array containing specifications of all of the exports defined in this library.
   */
  List<ExportElement> _exports = ExportElement.EMPTY_ARRAY;

  /**
   * An array containing all of the compilation units that are included in this library using a
   * `part` directive.
   */
  List<CompilationUnitElement> _parts = CompilationUnitElementImpl.EMPTY_ARRAY;

  /**
   * Is `true` if this library is created for Angular analysis.
   */
  bool _isAngularHtml = false;

  /**
   * Initialize a newly created library element to have the given name.
   *
   * @param context the analysis context in which the library is defined
   * @param name the name of this element
   */
  LibraryElementImpl(this.context, LibraryIdentifier name) : super.forNode(name);

  @override
  accept(ElementVisitor visitor) => visitor.visitLibraryElement(this);

  @override
  bool operator ==(Object object) => object != null && runtimeType == object.runtimeType && _definingCompilationUnit == (object as LibraryElementImpl).definingCompilationUnit;

  @override
  ElementImpl getChild(String identifier) {
    if ((_definingCompilationUnit as CompilationUnitElementImpl).identifier == identifier) {
      return _definingCompilationUnit as CompilationUnitElementImpl;
    }
    for (CompilationUnitElement part in _parts) {
      if ((part as CompilationUnitElementImpl).identifier == identifier) {
        return part as CompilationUnitElementImpl;
      }
    }
    for (ImportElement importElement in _imports) {
      if ((importElement as ImportElementImpl).identifier == identifier) {
        return importElement as ImportElementImpl;
      }
    }
    for (ExportElement exportElement in _exports) {
      if ((exportElement as ExportElementImpl).identifier == identifier) {
        return exportElement as ExportElementImpl;
      }
    }
    return null;
  }

  @override
  CompilationUnitElement get definingCompilationUnit => _definingCompilationUnit;

  @override
  List<LibraryElement> get exportedLibraries {
    Set<LibraryElement> libraries = new Set<LibraryElement>();
    for (ExportElement element in _exports) {
      LibraryElement library = element.exportedLibrary;
      if (library != null) {
        libraries.add(library);
      }
    }
    return new List.from(libraries);
  }

  @override
  List<ExportElement> get exports => _exports;

  @override
  List<LibraryElement> get importedLibraries {
    Set<LibraryElement> libraries = new Set<LibraryElement>();
    for (ImportElement element in _imports) {
      LibraryElement library = element.importedLibrary;
      if (library != null) {
        libraries.add(library);
      }
    }
    return new List.from(libraries);
  }

  @override
  List<ImportElement> get imports => _imports;

  @override
  ElementKind get kind => ElementKind.LIBRARY;

  @override
  LibraryElement get library => this;

  @override
  List<CompilationUnitElement> get parts => _parts;

  @override
  List<PrefixElement> get prefixes {
    Set<PrefixElement> prefixes = new Set<PrefixElement>();
    for (ImportElement element in _imports) {
      PrefixElement prefix = element.prefix;
      if (prefix != null) {
        prefixes.add(prefix);
      }
    }
    return new List.from(prefixes);
  }

  @override
  Source get source {
    if (_definingCompilationUnit == null) {
      return null;
    }
    return _definingCompilationUnit.source;
  }

  @override
  ClassElement getType(String className) {
    ClassElement type = _definingCompilationUnit.getType(className);
    if (type != null) {
      return type;
    }
    for (CompilationUnitElement part in _parts) {
      type = part.getType(className);
      if (type != null) {
        return type;
      }
    }
    return null;
  }

  @override
  List<CompilationUnitElement> get units {
    List<CompilationUnitElement> units = new List<CompilationUnitElement>(1 + _parts.length);
    units[0] = _definingCompilationUnit;
    JavaSystem.arraycopy(_parts, 0, units, 1, _parts.length);
    return units;
  }

  @override
  List<LibraryElement> get visibleLibraries {
    Set<LibraryElement> visibleLibraries = new Set();
    _addVisibleLibraries(visibleLibraries, false);
    return new List.from(visibleLibraries);
  }

  @override
  bool get hasExtUri => hasModifier(Modifier.HAS_EXT_URI);

  @override
  int get hashCode => _definingCompilationUnit.hashCode;

  @override
  bool get isAngularHtml => _isAngularHtml;

  @override
  bool get isBrowserApplication => entryPoint != null && isOrImportsBrowserLibrary;

  @override
  bool get isDartCore => name == "dart.core";

  @override
  bool get isInSdk => StringUtilities.startsWith5(name, 0, 0x64, 0x61, 0x72, 0x74, 0x2E);

  @override
  bool isUpToDate(int timeStamp) {
    Set<LibraryElement> visitedLibraries = new Set();
    return _safeIsUpToDate(this, timeStamp, visitedLibraries);
  }

  /**
   * Specifies if this library is created for Angular analysis.
   */
  void set angularHtml(bool isAngularHtml) {
    this._isAngularHtml = isAngularHtml;
  }

  /**
   * Set the compilation unit that defines this library to the given compilation unit.
   *
   * @param definingCompilationUnit the compilation unit that defines this library
   */
  void set definingCompilationUnit(CompilationUnitElement definingCompilationUnit) {
    (definingCompilationUnit as CompilationUnitElementImpl).enclosingElement = this;
    this._definingCompilationUnit = definingCompilationUnit;
  }

  /**
   * Set the specifications of all of the exports defined in this library to the given array.
   *
   * @param exports the specifications of all of the exports defined in this library
   */
  void set exports(List<ExportElement> exports) {
    for (ExportElement exportElement in exports) {
      (exportElement as ExportElementImpl).enclosingElement = this;
    }
    this._exports = exports;
  }

  /**
   * Set whether this library has an import of a "dart-ext" URI to the given value.
   *
   * @param hasExtUri `true` if this library has an import of a "dart-ext" URI
   */
  void set hasExtUri(bool hasExtUri) {
    setModifier(Modifier.HAS_EXT_URI, hasExtUri);
  }

  /**
   * Set the specifications of all of the imports defined in this library to the given array.
   *
   * @param imports the specifications of all of the imports defined in this library
   */
  void set imports(List<ImportElement> imports) {
    for (ImportElement importElement in imports) {
      (importElement as ImportElementImpl).enclosingElement = this;
      PrefixElementImpl prefix = importElement.prefix as PrefixElementImpl;
      if (prefix != null) {
        prefix.enclosingElement = this;
      }
    }
    this._imports = imports;
  }

  /**
   * Set the compilation units that are included in this library using a `part` directive.
   *
   * @param parts the compilation units that are included in this library using a `part`
   *          directive
   */
  void set parts(List<CompilationUnitElement> parts) {
    for (CompilationUnitElement compilationUnit in parts) {
      (compilationUnit as CompilationUnitElementImpl).enclosingElement = this;
    }
    this._parts = parts;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_definingCompilationUnit, visitor);
    safelyVisitChildren(_exports, visitor);
    safelyVisitChildren(_imports, visitor);
    safelyVisitChildren(_parts, visitor);
  }

  @override
  String get identifier => _definingCompilationUnit.source.encoding;

  /**
   * Recursively fills set of visible libraries for [getVisibleElementsLibraries].
   */
  void _addVisibleLibraries(Set<LibraryElement> visibleLibraries, bool includeExports) {
    // maybe already processed
    if (!visibleLibraries.add(this)) {
      return;
    }
    // add imported libraries
    for (ImportElement importElement in _imports) {
      LibraryElement importedLibrary = importElement.importedLibrary;
      if (importedLibrary != null) {
        (importedLibrary as LibraryElementImpl)._addVisibleLibraries(visibleLibraries, true);
      }
    }
    // add exported libraries
    if (includeExports) {
      for (ExportElement exportElement in _exports) {
        LibraryElement exportedLibrary = exportElement.exportedLibrary;
        if (exportedLibrary != null) {
          (exportedLibrary as LibraryElementImpl)._addVisibleLibraries(visibleLibraries, true);
        }
      }
    }
  }

  /**
   * Answer `true` if the receiver directly or indirectly imports the dart:html libraries.
   *
   * @return `true` if the receiver directly or indirectly imports the dart:html libraries
   */
  bool get isOrImportsBrowserLibrary {
    List<LibraryElement> visited = new List<LibraryElement>();
    Source htmlLibSource = context.sourceFactory.forUri(DartSdk.DART_HTML);
    visited.add(this);
    for (int index = 0; index < visited.length; index++) {
      LibraryElement library = visited[index];
      Source source = library.definingCompilationUnit.source;
      if (source == htmlLibSource) {
        return true;
      }
      for (LibraryElement importedLibrary in library.importedLibraries) {
        if (!visited.contains(importedLibrary)) {
          visited.add(importedLibrary);
        }
      }
      for (LibraryElement exportedLibrary in library.exportedLibraries) {
        if (!visited.contains(exportedLibrary)) {
          visited.add(exportedLibrary);
        }
      }
    }
    return false;
  }
}

/**
 * Instances of the class `LocalVariableElementImpl` implement a `LocalVariableElement`.
 */
class LocalVariableElementImpl extends VariableElementImpl implements LocalVariableElement {
  /**
   * Is `true` if this variable is potentially mutated somewhere in its scope.
   */
  bool _potentiallyMutatedInScope = false;

  /**
   * Is `true` if this variable is potentially mutated somewhere in closure.
   */
  bool _potentiallyMutatedInClosure = false;

  /**
   * The offset to the beginning of the visible range for this element.
   */
  int _visibleRangeOffset = 0;

  /**
   * The length of the visible range for this element, or `-1` if this element does not have a
   * visible range.
   */
  int _visibleRangeLength = -1;

  /**
   * An empty array of field elements.
   */
  static List<LocalVariableElement> EMPTY_ARRAY = new List<LocalVariableElement>(0);

  /**
   * Initialize a newly created local variable element to have the given name.
   *
   * @param name the name of this element
   */
  LocalVariableElementImpl(Identifier name) : super.con1(name);

  @override
  accept(ElementVisitor visitor) => visitor.visitLocalVariableElement(this);

  @override
  ElementKind get kind => ElementKind.LOCAL_VARIABLE;

  @override
  List<ToolkitObjectElement> get toolkitObjects {
    CompilationUnitElementImpl unit = getAncestor((element) => element is CompilationUnitElementImpl);
    if (unit == null) {
      return ToolkitObjectElement.EMPTY_ARRAY;
    }
    return unit._getToolkitObjects(this);
  }

  @override
  SourceRange get visibleRange {
    if (_visibleRangeLength < 0) {
      return null;
    }
    return new SourceRange(_visibleRangeOffset, _visibleRangeLength);
  }

  @override
  bool get isPotentiallyMutatedInClosure => _potentiallyMutatedInClosure;

  @override
  bool get isPotentiallyMutatedInScope => _potentiallyMutatedInScope;

  /**
   * Specifies that this variable is potentially mutated somewhere in closure.
   */
  void markPotentiallyMutatedInClosure() {
    _potentiallyMutatedInClosure = true;
  }

  /**
   * Specifies that this variable is potentially mutated somewhere in its scope.
   */
  void markPotentiallyMutatedInScope() {
    _potentiallyMutatedInScope = true;
  }

  /**
   * Set the toolkit specific information objects attached to this variable.
   *
   * @param toolkitObjects the toolkit objects attached to this variable
   */
  void set toolkitObjects(List<ToolkitObjectElement> toolkitObjects) {
    CompilationUnitElementImpl unit = getAncestor((element) => element is CompilationUnitElementImpl);
    if (unit == null) {
      return;
    }
    unit._setToolkitObjects(this, toolkitObjects);
  }

  /**
   * Set the visible range for this element to the range starting at the given offset with the given
   * length.
   *
   * @param offset the offset to the beginning of the visible range for this element
   * @param length the length of the visible range for this element, or `-1` if this element
   *          does not have a visible range
   */
  void setVisibleRange(int offset, int length) {
    _visibleRangeOffset = offset;
    _visibleRangeLength = length;
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append(type);
    builder.append(" ");
    builder.append(displayName);
  }

  @override
  String get identifier => "${super.identifier}@${nameOffset}";
}

/**
 * Instances of the class `MethodElementImpl` implement a `MethodElement`.
 */
class MethodElementImpl extends ExecutableElementImpl implements MethodElement {
  /**
   * An empty array of method elements.
   */
  static List<MethodElement> EMPTY_ARRAY = new List<MethodElement>(0);

  /**
   * Initialize a newly created method element to have the given name.
   *
   * @param name the name of this element
   */
  MethodElementImpl.con1(Identifier name) : super.con1(name);

  /**
   * Initialize a newly created method element to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  MethodElementImpl.con2(String name, int nameOffset) : super.con2(name, nameOffset);

  @override
  accept(ElementVisitor visitor) => visitor.visitMethodElement(this);

  @override
  ClassElement get enclosingElement => super.enclosingElement as ClassElement;

  @override
  ElementKind get kind => ElementKind.METHOD;

  @override
  String get name {
    String name = super.name;
    if (isOperator && name == "-") {
      if (parameters.length == 0) {
        return "unary-";
      }
    }
    return super.name;
  }

  @override
  MethodDeclaration get node => getNodeMatching((node) => node is MethodDeclaration);

  @override
  bool get isAbstract => hasModifier(Modifier.ABSTRACT);

  @override
  bool get isOperator {
    String name = displayName;
    if (name.isEmpty) {
      return false;
    }
    int first = name.codeUnitAt(0);
    return !((0x61 <= first && first <= 0x7A) || (0x41 <= first && first <= 0x5A) || first == 0x5F || first == 0x24);
  }

  @override
  bool get isStatic => hasModifier(Modifier.STATIC);

  /**
   * Set whether this method is abstract to correspond to the given value.
   *
   * @param isAbstract `true` if the method is abstract
   */
  void set abstract(bool isAbstract) {
    setModifier(Modifier.ABSTRACT, isAbstract);
  }

  /**
   * Set whether this method is static to correspond to the given value.
   *
   * @param isStatic `true` if the method is static
   */
  void set static(bool isStatic) {
    setModifier(Modifier.STATIC, isStatic);
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append(enclosingElement.displayName);
    builder.append(".");
    builder.append(displayName);
    super.appendTo(builder);
  }
}

/**
 * The enumeration `Modifier` defines constants for all of the modifiers defined by the Dart
 * language and for a few additional flags that are useful.
 */
class Modifier extends Enum<Modifier> {
  /**
   * Indicates that the modifier 'abstract' was applied to the element.
   */
  static const Modifier ABSTRACT = const Modifier('ABSTRACT', 0);

  /**
   * Indicates that the modifier 'const' was applied to the element.
   */
  static const Modifier CONST = const Modifier('CONST', 1);

  /**
   * Indicates that the modifier 'factory' was applied to the element.
   */
  static const Modifier FACTORY = const Modifier('FACTORY', 2);

  /**
   * Indicates that the modifier 'final' was applied to the element.
   */
  static const Modifier FINAL = const Modifier('FINAL', 3);

  /**
   * Indicates that the pseudo-modifier 'get' was applied to the element.
   */
  static const Modifier GETTER = const Modifier('GETTER', 4);

  /**
   * A flag used for libraries indicating that the defining compilation unit contains at least one
   * import directive whose URI uses the "dart-ext" scheme.
   */
  static const Modifier HAS_EXT_URI = const Modifier('HAS_EXT_URI', 5);

  static const Modifier MIXIN = const Modifier('MIXIN', 6);

  static const Modifier REFERENCES_SUPER = const Modifier('REFERENCES_SUPER', 7);

  /**
   * Indicates that the pseudo-modifier 'set' was applied to the element.
   */
  static const Modifier SETTER = const Modifier('SETTER', 8);

  /**
   * Indicates that the modifier 'static' was applied to the element.
   */
  static const Modifier STATIC = const Modifier('STATIC', 9);

  /**
   * Indicates that the element does not appear in the source code but was implicitly created. For
   * example, if a class does not define any constructors, an implicit zero-argument constructor
   * will be created and it will be marked as being synthetic.
   */
  static const Modifier SYNTHETIC = const Modifier('SYNTHETIC', 10);

  static const Modifier TYPEDEF = const Modifier('TYPEDEF', 11);

  static const List<Modifier> values = const [
      ABSTRACT,
      CONST,
      FACTORY,
      FINAL,
      GETTER,
      HAS_EXT_URI,
      MIXIN,
      REFERENCES_SUPER,
      SETTER,
      STATIC,
      SYNTHETIC,
      TYPEDEF];

  const Modifier(String name, int ordinal) : super(name, ordinal);
}

/**
 * Instances of the class `MultiplyDefinedElementImpl` represent a collection of elements that
 * have the same name within the same scope.
 */
class MultiplyDefinedElementImpl implements MultiplyDefinedElement {
  /**
   * Return an element that represents the given conflicting elements.
   *
   * @param context the analysis context in which the multiply defined elements are defined
   * @param firstElement the first element that conflicts
   * @param secondElement the second element that conflicts
   */
  static Element fromElements(AnalysisContext context, Element firstElement, Element secondElement) {
    List<Element> conflictingElements = _computeConflictingElements(firstElement, secondElement);
    int length = conflictingElements.length;
    if (length == 0) {
      return null;
    } else if (length == 1) {
      return conflictingElements[0];
    }
    return new MultiplyDefinedElementImpl(context, conflictingElements);
  }

  /**
   * Add the given element to the list of elements. If the element is a multiply-defined element,
   * add all of the conflicting elements that it represents.
   *
   * @param elements the list to which the element(s) are to be added
   * @param element the element(s) to be added
   */
  static void _add(Set<Element> elements, Element element) {
    if (element is MultiplyDefinedElementImpl) {
      for (Element conflictingElement in element.conflictingElements) {
        elements.add(conflictingElement);
      }
    } else {
      elements.add(element);
    }
  }

  /**
   * Use the given elements to construct an array of conflicting elements. If either of the given
   * elements are multiply-defined elements then the conflicting elements they represent will be
   * included in the array. Otherwise, the element itself will be included.
   *
   * @param firstElement the first element to be included
   * @param secondElement the second element to be included
   * @return an array containing all of the conflicting elements
   */
  static List<Element> _computeConflictingElements(Element firstElement, Element secondElement) {
    Set<Element> elements = new Set<Element>();
    _add(elements, firstElement);
    _add(elements, secondElement);
    return new List.from(elements);
  }

  /**
   * The analysis context in which the multiply defined elements are defined.
   */
  final AnalysisContext context;

  /**
   * The name of the conflicting elements.
   */
  String _name;

  /**
   * A list containing all of the elements that conflict.
   */
  final List<Element> conflictingElements;

  /**
   * Initialize a newly created element to represent a list of conflicting elements.
   *
   * @param context the analysis context in which the multiply defined elements are defined
   * @param conflictingElements the elements that conflict
   */
  MultiplyDefinedElementImpl(this.context, this.conflictingElements) {
    _name = conflictingElements[0].name;
  }

  @override
  accept(ElementVisitor visitor) => visitor.visitMultiplyDefinedElement(this);

  @override
  String computeDocumentationComment() => null;

  @override
  Element getAncestor(Predicate<Element> predicate) => null;

  @override
  String get displayName => _name;

  @override
  Element get enclosingElement => null;

  @override
  ElementKind get kind => ElementKind.ERROR;

  @override
  LibraryElement get library => null;

  @override
  ElementLocation get location => null;

  @override
  List<ElementAnnotation> get metadata => ElementAnnotationImpl.EMPTY_ARRAY;

  @override
  String get name => _name;

  @override
  int get nameOffset => -1;

  @override
  AstNode get node => null;

  @override
  Source get source => null;

  @override
  DartType get type => DynamicTypeImpl.instance;

  @override
  CompilationUnit get unit => null;

  @override
  bool isAccessibleIn(LibraryElement library) {
    for (Element element in conflictingElements) {
      if (element.isAccessibleIn(library)) {
        return true;
      }
    }
    return false;
  }

  @override
  bool get isDeprecated => false;

  @override
  bool get isOverride => false;

  @override
  bool get isPrivate {
    String name = displayName;
    if (name == null) {
      return false;
    }
    return Identifier.isPrivateName(name);
  }

  @override
  bool get isPublic => !isPrivate;

  @override
  bool get isSynthetic => true;

  @override
  String toString() {
    JavaStringBuilder builder = new JavaStringBuilder();
    builder.append("[");
    int count = conflictingElements.length;
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      (conflictingElements[i] as ElementImpl).appendTo(builder);
    }
    builder.append("]");
    return builder.toString();
  }

  @override
  void visitChildren(ElementVisitor visitor) {
  }
}

/**
 * The interface [MultiplyInheritedMethodElementImpl] defines all of the behavior of an
 * [MethodElementImpl], with the additional information of an array of
 * [ExecutableElement]s from which this element was composed.
 */
class MultiplyInheritedMethodElementImpl extends MethodElementImpl implements MultiplyInheritedExecutableElement {
  /**
   * An array the array of executable elements that were used to compose this element.
   */
  List<ExecutableElement> _elements = MethodElementImpl.EMPTY_ARRAY;

  MultiplyInheritedMethodElementImpl(Identifier name) : super.con1(name) {
    synthetic = true;
  }

  @override
  List<ExecutableElement> get inheritedElements => _elements;

  void set inheritedElements(List<ExecutableElement> elements) {
    this._elements = elements;
  }
}

/**
 * The interface [MultiplyInheritedPropertyAccessorElementImpl] defines all of the behavior of
 * an [PropertyAccessorElementImpl], with the additional information of an array of
 * [ExecutableElement]s from which this element was composed.
 */
class MultiplyInheritedPropertyAccessorElementImpl extends PropertyAccessorElementImpl implements MultiplyInheritedExecutableElement {
  /**
   * An array the array of executable elements that were used to compose this element.
   */
  List<ExecutableElement> _elements = PropertyAccessorElementImpl.EMPTY_ARRAY;

  MultiplyInheritedPropertyAccessorElementImpl(Identifier name) : super.con1(name) {
    synthetic = true;
  }

  @override
  List<ExecutableElement> get inheritedElements => _elements;

  void set inheritedElements(List<ExecutableElement> elements) {
    this._elements = elements;
  }
}

/**
 * Instances of the class `ParameterElementImpl` implement a `ParameterElement`.
 */
class ParameterElementImpl extends VariableElementImpl implements ParameterElement {
  /**
   * Is `true` if this variable is potentially mutated somewhere in its scope.
   */
  bool _potentiallyMutatedInScope = false;

  /**
   * Is `true` if this variable is potentially mutated somewhere in closure.
   */
  bool _potentiallyMutatedInClosure = false;

  /**
   * An array containing all of the parameters defined by this parameter element. There will only be
   * parameters if this parameter is a function typed parameter.
   */
  List<ParameterElement> _parameters = ParameterElementImpl.EMPTY_ARRAY;

  /**
   * The kind of this parameter.
   */
  ParameterKind parameterKind;

  /**
   * The offset to the beginning of the default value range for this element.
   */
  int _defaultValueRangeOffset = 0;

  /**
   * The length of the default value range for this element, or `-1` if this element does not
   * have a default value.
   */
  int _defaultValueRangeLength = -1;

  /**
   * The offset to the beginning of the visible range for this element.
   */
  int _visibleRangeOffset = 0;

  /**
   * The length of the visible range for this element, or `-1` if this element does not have a
   * visible range.
   */
  int _visibleRangeLength = -1;

  /**
   * An empty array of field elements.
   */
  static List<ParameterElement> EMPTY_ARRAY = new List<ParameterElement>(0);

  /**
   * Initialize a newly created parameter element to have the given name.
   *
   * @param name the name of this element
   */
  ParameterElementImpl.con1(Identifier name) : super.con1(name);

  /**
   * Initialize a newly created parameter element to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  ParameterElementImpl.con2(String name, int nameOffset) : super.con2(name, nameOffset);

  @override
  accept(ElementVisitor visitor) => visitor.visitParameterElement(this);

  @override
  SourceRange get defaultValueRange {
    if (_defaultValueRangeLength < 0) {
      return null;
    }
    return new SourceRange(_defaultValueRangeOffset, _defaultValueRangeLength);
  }

  @override
  ElementKind get kind => ElementKind.PARAMETER;

  @override
  List<ParameterElement> get parameters => _parameters;

  @override
  SourceRange get visibleRange {
    if (_visibleRangeLength < 0) {
      return null;
    }
    return new SourceRange(_visibleRangeOffset, _visibleRangeLength);
  }

  @override
  bool get isInitializingFormal => false;

  @override
  bool get isPotentiallyMutatedInClosure => _potentiallyMutatedInClosure;

  @override
  bool get isPotentiallyMutatedInScope => _potentiallyMutatedInScope;

  /**
   * Specifies that this variable is potentially mutated somewhere in closure.
   */
  void markPotentiallyMutatedInClosure() {
    _potentiallyMutatedInClosure = true;
  }

  /**
   * Specifies that this variable is potentially mutated somewhere in its scope.
   */
  void markPotentiallyMutatedInScope() {
    _potentiallyMutatedInScope = true;
  }

  /**
   * Set the range of the default value for this parameter to the range starting at the given offset
   * with the given length.
   *
   * @param offset the offset to the beginning of the default value range for this element
   * @param length the length of the default value range for this element, or `-1` if this
   *          element does not have a default value
   */
  void setDefaultValueRange(int offset, int length) {
    _defaultValueRangeOffset = offset;
    _defaultValueRangeLength = length;
  }

  /**
   * Set the parameters defined by this executable element to the given parameters.
   *
   * @param parameters the parameters defined by this executable element
   */
  void set parameters(List<ParameterElement> parameters) {
    for (ParameterElement parameter in parameters) {
      (parameter as ParameterElementImpl).enclosingElement = this;
    }
    this._parameters = parameters;
  }

  /**
   * Set the visible range for this element to the range starting at the given offset with the given
   * length.
   *
   * @param offset the offset to the beginning of the visible range for this element
   * @param length the length of the visible range for this element, or `-1` if this element
   *          does not have a visible range
   */
  void setVisibleRange(int offset, int length) {
    _visibleRangeOffset = offset;
    _visibleRangeLength = length;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(_parameters, visitor);
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    String left = "";
    String right = "";
    while (true) {
      if (parameterKind == ParameterKind.NAMED) {
        left = "{";
        right = "}";
      } else if (parameterKind == ParameterKind.POSITIONAL) {
        left = "[";
        right = "]";
      } else if (parameterKind == ParameterKind.REQUIRED) {
      }
      break;
    }
    builder.append(left);
    appendToWithoutDelimiters(builder);
    builder.append(right);
  }

  /**
   * Append the type and name of this parameter to the given builder.
   *
   * @param builder the builder to which the type and name are to be appended
   */
  void appendToWithoutDelimiters(JavaStringBuilder builder) {
    builder.append(type);
    builder.append(" ");
    builder.append(displayName);
  }
}

/**
 * Instances of the class `PrefixElementImpl` implement a `PrefixElement`.
 */
class PrefixElementImpl extends ElementImpl implements PrefixElement {
  /**
   * An array containing all of the libraries that are imported using this prefix.
   */
  List<LibraryElement> _importedLibraries = LibraryElementImpl.EMPTY_ARRAY;

  /**
   * An empty array of prefix elements.
   */
  static List<PrefixElement> EMPTY_ARRAY = new List<PrefixElement>(0);

  /**
   * Initialize a newly created prefix element to have the given name.
   *
   * @param name the name of this element
   */
  PrefixElementImpl(Identifier name) : super.forNode(name);

  @override
  accept(ElementVisitor visitor) => visitor.visitPrefixElement(this);

  @override
  LibraryElement get enclosingElement => super.enclosingElement as LibraryElement;

  @override
  List<LibraryElement> get importedLibraries => _importedLibraries;

  @override
  ElementKind get kind => ElementKind.PREFIX;

  /**
   * Set the libraries that are imported using this prefix to the given libraries.
   *
   * @param importedLibraries the libraries that are imported using this prefix
   */
  void set importedLibraries(List<LibraryElement> importedLibraries) {
    for (LibraryElement library in importedLibraries) {
      (library as LibraryElementImpl).enclosingElement = this;
    }
    this._importedLibraries = importedLibraries;
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append("as ");
    super.appendTo(builder);
  }

  @override
  String get identifier => "_${super.identifier}";
}

/**
 * Instances of the class `PropertyAccessorElementImpl` implement a
 * `PropertyAccessorElement`.
 */
class PropertyAccessorElementImpl extends ExecutableElementImpl implements PropertyAccessorElement {
  /**
   * The variable associated with this accessor.
   */
  PropertyInducingElement variable;

  /**
   * An empty array of property accessor elements.
   */
  static List<PropertyAccessorElement> EMPTY_ARRAY = new List<PropertyAccessorElement>(0);

  /**
   * Initialize a newly created property accessor element to have the given name.
   *
   * @param name the name of this element
   */
  PropertyAccessorElementImpl.con1(Identifier name) : super.con1(name);

  /**
   * Initialize a newly created synthetic property accessor element to be associated with the given
   * variable.
   *
   * @param variable the variable with which this access is associated
   */
  PropertyAccessorElementImpl.con2(PropertyInducingElementImpl variable) : super.con2(variable.name, variable.nameOffset) {
    this.variable = variable;
    synthetic = true;
  }

  @override
  accept(ElementVisitor visitor) => visitor.visitPropertyAccessorElement(this);

  @override
  bool operator ==(Object object) => super == object && isGetter == (object as PropertyAccessorElement).isGetter;

  @override
  PropertyAccessorElement get correspondingGetter {
    if (isGetter || variable == null) {
      return null;
    }
    return variable.getter;
  }

  @override
  PropertyAccessorElement get correspondingSetter {
    if (isSetter || variable == null) {
      return null;
    }
    return variable.setter;
  }

  @override
  ElementKind get kind {
    if (isGetter) {
      return ElementKind.GETTER;
    }
    return ElementKind.SETTER;
  }

  @override
  String get name {
    if (isSetter) {
      return "${super.name}=";
    }
    return super.name;
  }

  @override
  AstNode get node {
    if (isSynthetic) {
      return null;
    }
    if (enclosingElement is ClassElement) {
      return getNodeMatching((node) => node is MethodDeclaration);
    }
    if (enclosingElement is CompilationUnitElement) {
      return getNodeMatching((node) => node is FunctionDeclaration);
    }
    return null;
  }

  @override
  int get hashCode => ObjectUtilities.combineHashCodes(super.hashCode, isGetter ? 1 : 2);

  @override
  bool get isAbstract => hasModifier(Modifier.ABSTRACT);

  @override
  bool get isGetter => hasModifier(Modifier.GETTER);

  @override
  bool get isSetter => hasModifier(Modifier.SETTER);

  @override
  bool get isStatic => hasModifier(Modifier.STATIC);

  /**
   * Set whether this accessor is abstract to correspond to the given value.
   *
   * @param isAbstract `true` if the accessor is abstract
   */
  void set abstract(bool isAbstract) {
    setModifier(Modifier.ABSTRACT, isAbstract);
  }

  /**
   * Set whether this accessor is a getter to correspond to the given value.
   *
   * @param isGetter `true` if the accessor is a getter
   */
  void set getter(bool isGetter) {
    setModifier(Modifier.GETTER, isGetter);
  }

  /**
   * Set whether this accessor is a setter to correspond to the given value.
   *
   * @param isSetter `true` if the accessor is a setter
   */
  void set setter(bool isSetter) {
    setModifier(Modifier.SETTER, isSetter);
  }

  /**
   * Set whether this accessor is static to correspond to the given value.
   *
   * @param isStatic `true` if the accessor is static
   */
  void set static(bool isStatic) {
    setModifier(Modifier.STATIC, isStatic);
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append(isGetter ? "get " : "set ");
    builder.append(variable.displayName);
    super.appendTo(builder);
  }
}

/**
 * Instances of the class `PropertyInducingElementImpl` implement a
 * `PropertyInducingElement`.
 */
abstract class PropertyInducingElementImpl extends VariableElementImpl implements PropertyInducingElement {
  /**
   * The getter associated with this element.
   */
  PropertyAccessorElement getter;

  /**
   * The setter associated with this element, or `null` if the element is effectively
   * `final` and therefore does not have a setter associated with it.
   */
  PropertyAccessorElement setter;

  /**
   * An empty array of elements.
   */
  static List<PropertyInducingElement> EMPTY_ARRAY = new List<PropertyInducingElement>(0);

  /**
   * Initialize a newly created element to have the given name.
   *
   * @param name the name of this element
   */
  PropertyInducingElementImpl.con1(Identifier name) : super.con1(name);

  /**
   * Initialize a newly created synthetic element to have the given name.
   *
   * @param name the name of this element
   */
  PropertyInducingElementImpl.con2(String name) : super.con2(name, -1) {
    synthetic = true;
  }
}

/**
 * Instances of the class `ShowElementCombinatorImpl` implement a
 * [ShowElementCombinator].
 */
class ShowElementCombinatorImpl implements ShowElementCombinator {
  /**
   * The names that are to be made visible in the importing library if they are defined in the
   * imported library.
   */
  List<String> shownNames = StringUtilities.EMPTY_ARRAY;

  /**
   * The offset of the character immediately following the last character of this node.
   */
  int end = -1;

  /**
   * The offset of the 'show' keyword of this element.
   */
  int offset = 0;

  @override
  String toString() {
    JavaStringBuilder builder = new JavaStringBuilder();
    builder.append("show ");
    int count = shownNames.length;
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(shownNames[i]);
    }
    return builder.toString();
  }
}

/**
 * Instances of the class `ToolkitObjectElementImpl` implement a `ToolkitObjectElement`.
 */
abstract class ToolkitObjectElementImpl extends ElementImpl implements ToolkitObjectElement {
  /**
   * Initialize a newly created toolkit object element to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  ToolkitObjectElementImpl(String name, int nameOffset) : super(name, nameOffset);
}

/**
 * Instances of the class `TopLevelVariableElementImpl` implement a
 * `TopLevelVariableElement`.
 */
class TopLevelVariableElementImpl extends PropertyInducingElementImpl implements TopLevelVariableElement {
  /**
   * An empty array of top-level variable elements.
   */
  static List<TopLevelVariableElement> EMPTY_ARRAY = new List<TopLevelVariableElement>(0);

  /**
   * Initialize a newly created top-level variable element to have the given name.
   *
   * @param name the name of this element
   */
  TopLevelVariableElementImpl.con1(Identifier name) : super.con1(name);

  /**
   * Initialize a newly created synthetic top-level variable element to have the given name.
   *
   * @param name the name of this element
   */
  TopLevelVariableElementImpl.con2(String name) : super.con2(name);

  @override
  accept(ElementVisitor visitor) => visitor.visitTopLevelVariableElement(this);

  @override
  ElementKind get kind => ElementKind.TOP_LEVEL_VARIABLE;

  @override
  bool get isStatic => true;
}

/**
 * Instances of the class `TypeParameterElementImpl` implement a [TypeParameterElement].
 */
class TypeParameterElementImpl extends ElementImpl implements TypeParameterElement {
  /**
   * The type defined by this type parameter.
   */
  TypeParameterType type;

  /**
   * The type representing the bound associated with this parameter, or `null` if this
   * parameter does not have an explicit bound.
   */
  DartType bound;

  /**
   * An empty array of type parameter elements.
   */
  static List<TypeParameterElement> EMPTY_ARRAY = new List<TypeParameterElement>(0);

  /**
   * Initialize a newly created type parameter element to have the given name.
   *
   * @param name the name of this element
   */
  TypeParameterElementImpl(Identifier name) : super.forNode(name);

  @override
  accept(ElementVisitor visitor) => visitor.visitTypeParameterElement(this);

  @override
  ElementKind get kind => ElementKind.TYPE_PARAMETER;

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append(displayName);
    if (bound != null) {
      builder.append(" extends ");
      builder.append(bound);
    }
  }
}

/**
 * Instances of the class `UriReferencedElementImpl` implement an [UriReferencedElement]
 * .
 */
abstract class UriReferencedElementImpl extends ElementImpl implements UriReferencedElement {
  /**
   * The offset of the URI in the file, may be `-1` if synthetic.
   */
  int uriOffset = -1;

  /**
   * The offset of the character immediately following the last character of this node's URI, may be
   * `-1` if synthetic.
   */
  int uriEnd = -1;

  /**
   * The URI that is specified by this directive.
   */
  String uri;

  /**
   * Initialize a newly created import element.
   *
   * @param name the name of this element
   * @param offset the directive offset, may be `-1` if synthetic.
   */
  UriReferencedElementImpl(String name, int offset) : super(name, offset);
}

/**
 * Instances of the class `VariableElementImpl` implement a `VariableElement`.
 */
abstract class VariableElementImpl extends ElementImpl implements VariableElement {
  /**
   * The declared type of this variable.
   */
  DartType type;

  /**
   * A synthetic function representing this variable's initializer, or `null` if this variable
   * does not have an initializer.
   */
  FunctionElement _initializer;

  /**
   * An empty array of variable elements.
   */
  static List<VariableElement> EMPTY_ARRAY = new List<VariableElement>(0);

  /**
   * Initialize a newly created variable element to have the given name.
   *
   * @param name the name of this element
   */
  VariableElementImpl.con1(Identifier name) : super.forNode(name);

  /**
   * Initialize a newly created variable element to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  VariableElementImpl.con2(String name, int nameOffset) : super(name, nameOffset);

  /**
   * Return the result of evaluating this variable's initializer as a compile-time constant
   * expression, or `null` if this variable is not a 'const' variable, if it does not have an
   * initializer, or if the compilation unit containing the variable has not been resolved.
   *
   * @return the result of evaluating this variable's initializer
   */
  EvaluationResultImpl get evaluationResult => null;

  @override
  FunctionElement get initializer => _initializer;

  @override
  VariableDeclaration get node => getNodeMatching((node) => node is VariableDeclaration);

  @override
  bool get isConst => hasModifier(Modifier.CONST);

  @override
  bool get isFinal => hasModifier(Modifier.FINAL);

  /**
   * Return `true` if this variable is potentially mutated somewhere in a closure. This
   * information is only available for local variables (including parameters) and only after the
   * compilation unit containing the variable has been resolved.
   *
   * @return `true` if this variable is potentially mutated somewhere in closure
   */
  bool get isPotentiallyMutatedInClosure => false;

  /**
   * Return `true` if this variable is potentially mutated somewhere in its scope. This
   * information is only available for local variables (including parameters) and only after the
   * compilation unit containing the variable has been resolved.
   *
   * @return `true` if this variable is potentially mutated somewhere in its scope
   */
  bool get isPotentiallyMutatedInScope => false;

  /**
   * Set whether this variable is const to correspond to the given value.
   *
   * @param isConst `true` if the variable is const
   */
  void set const3(bool isConst) {
    setModifier(Modifier.CONST, isConst);
  }

  /**
   * Set the result of evaluating this variable's initializer as a compile-time constant expression
   * to the given result.
   *
   * @param result the result of evaluating this variable's initializer
   */
  void set evaluationResult(EvaluationResultImpl result) {
    throw new IllegalStateException("Invalid attempt to set a compile-time constant result");
  }

  /**
   * Set whether this variable is final to correspond to the given value.
   *
   * @param isFinal `true` if the variable is final
   */
  void set final2(bool isFinal) {
    setModifier(Modifier.FINAL, isFinal);
  }

  /**
   * Set the function representing this variable's initializer to the given function.
   *
   * @param initializer the function representing this variable's initializer
   */
  void set initializer(FunctionElement initializer) {
    if (initializer != null) {
      (initializer as FunctionElementImpl).enclosingElement = this;
    }
    this._initializer = initializer;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(_initializer, visitor);
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append(type);
    builder.append(" ");
    builder.append(displayName);
  }
}

/**
 * Information about Angular application.
 */
class AngularApplication {
  final Source entryPoint;

  final Set<Source> _librarySources;

  final List<AngularElement> elements;

  final List<Source> elementSources;

  AngularApplication(this.entryPoint, this._librarySources, this.elements, this.elementSources);

  /**
   * Checks if this application depends on the library with the given [Source].
   */
  bool dependsOn(Source librarySource) => _librarySources.contains(librarySource);
}

/**
 * Implementation of `AngularComponentElement`.
 */
class AngularComponentElementImpl extends AngularHasSelectorElementImpl implements AngularComponentElement {
  /**
   * The offset of the defining <code>NgComponent</code> annotation.
   */
  final int _annotationOffset;

  /**
   * The array containing all of the properties declared by this component.
   */
  List<AngularPropertyElement> _properties = AngularPropertyElement.EMPTY_ARRAY;

  /**
   * The array containing all of the scope properties set by this component.
   */
  List<AngularScopePropertyElement> _scopeProperties = AngularScopePropertyElement.EMPTY_ARRAY;

  /**
   * The the CSS file URI.
   */
  String styleUri;

  /**
   * The offset of the [styleUri] in the [getSource].
   */
  int styleUriOffset = 0;

  /**
   * The HTML template URI.
   */
  String templateUri;

  /**
   * The HTML template source.
   */
  Source templateSource;

  /**
   * The offset of the [templateUri] in the [getSource].
   */
  int templateUriOffset = 0;

  /**
   * Initialize a newly created Angular component to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  AngularComponentElementImpl(String name, int nameOffset, this._annotationOffset) : super(name, nameOffset);

  @override
  accept(ElementVisitor visitor) => visitor.visitAngularComponentElement(this);

  @override
  ElementKind get kind => ElementKind.ANGULAR_COMPONENT;

  @override
  List<AngularPropertyElement> get properties => _properties;

  @override
  List<AngularScopePropertyElement> get scopeProperties => _scopeProperties;

  /**
   * Set an array containing all of the properties declared by this component.
   *
   * @param properties the properties to set
   */
  void set properties(List<AngularPropertyElement> properties) {
    for (AngularPropertyElement property in properties) {
      encloseElement(property as AngularPropertyElementImpl);
    }
    this._properties = properties;
  }

  /**
   * Set an array containing all of the scope properties declared by this component.
   *
   * @param properties the properties to set
   */
  void set scopeProperties(List<AngularScopePropertyElement> properties) {
    for (AngularScopePropertyElement property in properties) {
      encloseElement(property as AngularScopePropertyElementImpl);
    }
    this._scopeProperties = properties;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    safelyVisitChildren(_properties, visitor);
    safelyVisitChildren(_scopeProperties, visitor);
    super.visitChildren(visitor);
  }

  @override
  String get identifier => "AngularComponent@${_annotationOffset}";
}

/**
 * Implementation of `AngularControllerElement`.
 */
class AngularControllerElementImpl extends AngularHasSelectorElementImpl implements AngularControllerElement {
  /**
   * Initialize a newly created Angular controller to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  AngularControllerElementImpl(String name, int nameOffset) : super(name, nameOffset);

  @override
  accept(ElementVisitor visitor) => visitor.visitAngularControllerElement(this);

  @override
  ElementKind get kind => ElementKind.ANGULAR_CONTROLLER;
}

/**
 * Implementation of `AngularDirectiveElement`.
 */
class AngularDirectiveElementImpl extends AngularHasSelectorElementImpl implements AngularDirectiveElement {
  /**
   * The offset of the annotation that defines this directive.
   */
  final int _offset;

  /**
   * The array containing all of the properties declared by this directive.
   */
  List<AngularPropertyElement> _properties = AngularPropertyElement.EMPTY_ARRAY;

  /**
   * Initialize a newly created Angular directive to have the given name.
   *
   * @param offset the offset of the annotation that defines this directive
   */
  AngularDirectiveElementImpl(this._offset) : super(null, -1);

  @override
  accept(ElementVisitor visitor) => visitor.visitAngularDirectiveElement(this);

  @override
  String get displayName => selector.displayName;

  @override
  ElementKind get kind => ElementKind.ANGULAR_DIRECTIVE;

  @override
  List<AngularPropertyElement> get properties => _properties;

  @override
  bool isClass(String name) {
    Element enclosing = enclosingElement;
    return enclosing is ClassElement && enclosing.name == name;
  }

  /**
   * Set an array containing all of the properties declared by this directive.
   *
   * @param properties the properties to set
   */
  void set properties(List<AngularPropertyElement> properties) {
    for (AngularPropertyElement property in properties) {
      encloseElement(property as AngularPropertyElementImpl);
    }
    this._properties = properties;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    safelyVisitChildren(_properties, visitor);
    super.visitChildren(visitor);
  }

  @override
  String get identifier => "NgDirective@${_offset}";
}

/**
 * Implementation of `AngularElement`.
 */
abstract class AngularElementImpl extends ToolkitObjectElementImpl implements AngularElement {
  /**
   * The [AngularApplication] this element is used in.
   */
  AngularApplication _application;

  /**
   * Initialize a newly created Angular element to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  AngularElementImpl(String name, int nameOffset) : super(name, nameOffset);

  @override
  AngularApplication get application => _application;

  /**
   * Set the [AngularApplication] this element is used in.
   */
  void set application(AngularApplication application) {
    this._application = application;
  }
}

/**
 * Implementation of `AngularFilterElement`.
 */
class AngularFilterElementImpl extends AngularElementImpl implements AngularFilterElement {
  /**
   * Initialize a newly created Angular filter to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  AngularFilterElementImpl(String name, int nameOffset) : super(name, nameOffset);

  @override
  accept(ElementVisitor visitor) => visitor.visitAngularFilterElement(this);

  @override
  ElementKind get kind => ElementKind.ANGULAR_FILTER;
}

/**
 * Implementation of [AngularSelectorElement] based on presence of a class.
 */
class AngularHasClassSelectorElementImpl extends AngularSelectorElementImpl implements AngularHasClassSelectorElement {
  AngularHasClassSelectorElementImpl(String name, int offset) : super(name, offset);

  @override
  bool apply(XmlTagNode node) {
    XmlAttributeNode attribute = node.getAttribute("class");
    if (attribute != null) {
      String text = attribute.text;
      if (text != null) {
        String name = this.name;
        for (String className in StringUtils.split(text)) {
          if (className == name) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append(".");
    builder.append(name);
  }
}

/**
 * Implementation of `AngularSelectorElement`.
 */
abstract class AngularHasSelectorElementImpl extends AngularElementImpl implements AngularHasSelectorElement {
  /**
   * The selector of this element.
   */
  AngularSelectorElement _selector;

  /**
   * Initialize a newly created Angular element to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  AngularHasSelectorElementImpl(String name, int nameOffset) : super(name, nameOffset);

  @override
  AngularSelectorElement get selector => _selector;

  /**
   * Set the selector of this selector-based element.
   *
   * @param selector the selector to set
   */
  void set selector(AngularSelectorElement selector) {
    encloseElement(selector as AngularSelectorElementImpl);
    this._selector = selector;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    safelyVisitChild(_selector, visitor);
    super.visitChildren(visitor);
  }
}

/**
 * Implementation of `AngularPropertyElement`.
 */
class AngularPropertyElementImpl extends AngularElementImpl implements AngularPropertyElement {
  /**
   * The [FieldElement] to which this property is bound.
   */
  FieldElement field;

  /**
   * The offset of the field name in the property map.
   */
  int fieldNameOffset = -1;

  AngularPropertyKind propertyKind;

  /**
   * Initialize a newly created Angular property to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  AngularPropertyElementImpl(String name, int nameOffset) : super(name, nameOffset);

  @override
  accept(ElementVisitor visitor) => visitor.visitAngularPropertyElement(this);

  @override
  ElementKind get kind => ElementKind.ANGULAR_PROPERTY;
}

/**
 * Implementation of `AngularScopePropertyElement`.
 */
class AngularScopePropertyElementImpl extends AngularElementImpl implements AngularScopePropertyElement {
  /**
   * The type of the property
   */
  final DartType type;

  /**
   * Initialize a newly created Angular scope property to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  AngularScopePropertyElementImpl(String name, int nameOffset, this.type) : super(name, nameOffset);

  @override
  accept(ElementVisitor visitor) => visitor.visitAngularScopePropertyElement(this);

  @override
  ElementKind get kind => ElementKind.ANGULAR_SCOPE_PROPERTY;
}

/**
 * Implementation of `AngularFilterElement`.
 */
abstract class AngularSelectorElementImpl extends AngularElementImpl implements AngularSelectorElement {
  /**
   * Initialize a newly created Angular selector to have the given name.
   *
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  AngularSelectorElementImpl(String name, int nameOffset) : super(name, nameOffset);

  @override
  accept(ElementVisitor visitor) => visitor.visitAngularSelectorElement(this);

  @override
  ElementKind get kind => ElementKind.ANGULAR_SELECTOR;
}

/**
 * Implementation of [AngularSelectorElement] based on tag name.
 */
class AngularTagSelectorElementImpl extends AngularSelectorElementImpl implements AngularTagSelectorElement {
  AngularTagSelectorElementImpl(String name, int offset) : super(name, offset);

  @override
  bool apply(XmlTagNode node) {
    String tagName = name;
    return node.tag == tagName;
  }

  @override
  AngularApplication get application => (enclosingElement as AngularElementImpl).application;
}

/**
 * Implementation of `AngularViewElement`.
 */
class AngularViewElementImpl extends AngularElementImpl implements AngularViewElement {
  /**
   * The HTML template URI.
   */
  final String templateUri;

  /**
   * The offset of the [templateUri] in the [getSource].
   */
  final int templateUriOffset;

  /**
   * The HTML template source.
   */
  Source templateSource;

  /**
   * Initialize a newly created Angular view.
   */
  AngularViewElementImpl(this.templateUri, this.templateUriOffset) : super(null, -1);

  @override
  accept(ElementVisitor visitor) => visitor.visitAngularViewElement(this);

  @override
  ElementKind get kind => ElementKind.ANGULAR_VIEW;

  @override
  String get identifier => "AngularView@${templateUriOffset}";
}

/**
 * Implementation of [AngularSelectorElement] based on presence of attribute.
 */
class HasAttributeSelectorElementImpl extends AngularSelectorElementImpl implements AngularHasAttributeSelectorElement {
  HasAttributeSelectorElementImpl(String attributeName, int offset) : super(attributeName, offset);

  @override
  bool apply(XmlTagNode node) {
    String attributeName = name;
    return node.getAttribute(attributeName) != null;
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append("[");
    builder.append(name);
    builder.append("]");
  }
}

/**
 * Combination of [AngularTagSelectorElementImpl] and [HasAttributeSelectorElementImpl].
 */
class IsTagHasAttributeSelectorElementImpl extends AngularSelectorElementImpl {
  final String tagName;

  final String attributeName;

  IsTagHasAttributeSelectorElementImpl(this.tagName, this.attributeName) : super(null, -1);

  @override
  bool apply(XmlTagNode node) => node.tag == tagName && node.getAttribute(attributeName) != null;
}

/**
 * Instances of the class `ConstructorMember` represent a constructor element defined in a
 * parameterized type where the values of the type parameters are known.
 */
class ConstructorMember extends ExecutableMember implements ConstructorElement {
  /**
   * If the given constructor's type is different when any type parameters from the defining type's
   * declaration are replaced with the actual type arguments from the defining type, create a
   * constructor member representing the given constructor. Return the member that was created, or
   * the base constructor if no member was created.
   *
   * @param baseConstructor the base constructor for which a member might be created
   * @param definingType the type defining the parameters and arguments to be used in the
   *          substitution
   * @return the constructor element that will return the correctly substituted types
   */
  static ConstructorElement from(ConstructorElement baseConstructor, InterfaceType definingType) {
    if (baseConstructor == null || definingType.typeArguments.length == 0) {
      return baseConstructor;
    }
    FunctionType baseType = baseConstructor.type;
    if (baseType == null) {
      // TODO(brianwilkerson) We need to understand when this can happen.
      return baseConstructor;
    }
    List<DartType> argumentTypes = definingType.typeArguments;
    List<DartType> parameterTypes = definingType.element.type.typeArguments;
    FunctionType substitutedType = baseType.substitute2(argumentTypes, parameterTypes);
    if (baseType == substitutedType) {
      return baseConstructor;
    }
    // TODO(brianwilkerson) Consider caching the substituted type in the instance. It would use more
    // memory but speed up some operations. We need to see how often the type is being re-computed.
    return new ConstructorMember(baseConstructor, definingType);
  }

  /**
   * Initialize a newly created element to represent a constructor of the given parameterized type.
   *
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  ConstructorMember(ConstructorElement baseElement, InterfaceType definingType) : super(baseElement, definingType);

  @override
  accept(ElementVisitor visitor) => visitor.visitConstructorElement(this);

  @override
  ConstructorElement get baseElement => super.baseElement as ConstructorElement;

  @override
  ClassElement get enclosingElement => baseElement.enclosingElement;

  @override
  ConstructorDeclaration get node => baseElement.node;

  @override
  ConstructorElement get redirectedConstructor => from(baseElement.redirectedConstructor, definingType);

  @override
  bool get isConst => baseElement.isConst;

  @override
  bool get isDefaultConstructor => baseElement.isDefaultConstructor;

  @override
  bool get isFactory => baseElement.isFactory;

  @override
  String toString() {
    ConstructorElement baseElement = this.baseElement;
    List<ParameterElement> parameters = this.parameters;
    FunctionType type = this.type;
    JavaStringBuilder builder = new JavaStringBuilder();
    builder.append(baseElement.enclosingElement.displayName);
    String name = displayName;
    if (name != null && !name.isEmpty) {
      builder.append(".");
      builder.append(name);
    }
    builder.append("(");
    int parameterCount = parameters.length;
    for (int i = 0; i < parameterCount; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(parameters[i]).toString();
    }
    builder.append(")");
    if (type != null) {
      builder.append(Element.RIGHT_ARROW);
      builder.append(type.returnType);
    }
    return builder.toString();
  }

  @override
  InterfaceType get definingType => super.definingType as InterfaceType;
}

/**
 * The abstract class `ExecutableMember` defines the behavior common to members that represent
 * an executable element defined in a parameterized type where the values of the type parameters are
 * known.
 */
abstract class ExecutableMember extends Member implements ExecutableElement {
  /**
   * Initialize a newly created element to represent an executable element of the given
   * parameterized type.
   *
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  ExecutableMember(ExecutableElement baseElement, InterfaceType definingType) : super(baseElement, definingType);

  @override
  ExecutableElement get baseElement => super.baseElement as ExecutableElement;

  @override
  List<FunctionElement> get functions {
    //
    // Elements within this element should have type parameters substituted, just like this element.
    //
    throw new UnsupportedOperationException();
  }

  @override
  List<LabelElement> get labels => baseElement.labels;

  @override
  List<LocalVariableElement> get localVariables {
    //
    // Elements within this element should have type parameters substituted, just like this element.
    //
    throw new UnsupportedOperationException();
  }

  @override
  List<ParameterElement> get parameters {
    List<ParameterElement> baseParameters = baseElement.parameters;
    int parameterCount = baseParameters.length;
    if (parameterCount == 0) {
      return baseParameters;
    }
    List<ParameterElement> parameterizedParameters = new List<ParameterElement>(parameterCount);
    for (int i = 0; i < parameterCount; i++) {
      parameterizedParameters[i] = ParameterMember.from(baseParameters[i], definingType);
    }
    return parameterizedParameters;
  }

  @override
  DartType get returnType => substituteFor(baseElement.returnType);

  @override
  FunctionType get type => substituteFor(baseElement.type);

  @override
  bool get isOperator => baseElement.isOperator;

  @override
  bool get isStatic => baseElement.isStatic;

  @override
  void visitChildren(ElementVisitor visitor) {
    // TODO(brianwilkerson) We need to finish implementing the accessors used below so that we can
    // safely invoke them.
    super.visitChildren(visitor);
    safelyVisitChildren(baseElement.functions, visitor);
    safelyVisitChildren(labels, visitor);
    safelyVisitChildren(baseElement.localVariables, visitor);
    safelyVisitChildren(parameters, visitor);
  }
}

/**
 * Instances of the class `FieldFormalParameterMember` represent a parameter element defined
 * in a parameterized type where the values of the type parameters are known.
 */
class FieldFormalParameterMember extends ParameterMember implements FieldFormalParameterElement {
  /**
   * Initialize a newly created element to represent a parameter of the given parameterized type.
   *
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  FieldFormalParameterMember(FieldFormalParameterElement baseElement, ParameterizedType definingType) : super(baseElement, definingType);

  @override
  accept(ElementVisitor visitor) => visitor.visitFieldFormalParameterElement(this);

  @override
  FieldElement get field => (baseElement as FieldFormalParameterElement).field;
}

/**
 * Instances of the class `FieldMember` represent a field element defined in a parameterized
 * type where the values of the type parameters are known.
 */
class FieldMember extends VariableMember implements FieldElement {
  /**
   * If the given field's type is different when any type parameters from the defining type's
   * declaration are replaced with the actual type arguments from the defining type, create a field
   * member representing the given field. Return the member that was created, or the base field if
   * no member was created.
   *
   * @param baseField the base field for which a member might be created
   * @param definingType the type defining the parameters and arguments to be used in the
   *          substitution
   * @return the field element that will return the correctly substituted types
   */
  static FieldElement from(FieldElement baseField, InterfaceType definingType) {
    if (baseField == null || definingType.typeArguments.length == 0) {
      return baseField;
    }
    DartType baseType = baseField.type;
    if (baseType == null) {
      return baseField;
    }
    List<DartType> argumentTypes = definingType.typeArguments;
    List<DartType> parameterTypes = definingType.element.type.typeArguments;
    DartType substitutedType = baseType.substitute2(argumentTypes, parameterTypes);
    if (baseType == substitutedType) {
      return baseField;
    }
    // TODO(brianwilkerson) Consider caching the substituted type in the instance. It would use more
    // memory but speed up some operations. We need to see how often the type is being re-computed.
    return new FieldMember(baseField, definingType);
  }

  /**
   * Initialize a newly created element to represent a field of the given parameterized type.
   *
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  FieldMember(FieldElement baseElement, InterfaceType definingType) : super(baseElement, definingType);

  @override
  accept(ElementVisitor visitor) => visitor.visitFieldElement(this);

  @override
  FieldElement get baseElement => super.baseElement as FieldElement;

  @override
  ClassElement get enclosingElement => baseElement.enclosingElement;

  @override
  PropertyAccessorElement get getter => PropertyAccessorMember.from(baseElement.getter, definingType);

  @override
  PropertyAccessorElement get setter => PropertyAccessorMember.from(baseElement.setter, definingType);

  @override
  bool get isStatic => baseElement.isStatic;

  @override
  InterfaceType get definingType => super.definingType as InterfaceType;
}

/**
 * The abstract class `Member` defines the behavior common to elements that represent members
 * of parameterized types.
 */
abstract class Member implements Element {
  /**
   * The element on which the parameterized element was created.
   */
  final Element _baseElement;

  /**
   * The type in which the element is defined.
   */
  final ParameterizedType _definingType;

  /**
   * Initialize a newly created element to represent the member of the given parameterized type.
   *
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  Member(this._baseElement, this._definingType);

  @override
  String computeDocumentationComment() => _baseElement.computeDocumentationComment();

  @override
  Element getAncestor(Predicate<Element> predicate) => baseElement.getAncestor(predicate);

  /**
   * Return the element on which the parameterized element was created.
   *
   * @return the element on which the parameterized element was created
   */
  Element get baseElement => _baseElement;

  @override
  AnalysisContext get context => _baseElement.context;

  @override
  String get displayName => _baseElement.displayName;

  @override
  ElementKind get kind => _baseElement.kind;

  @override
  LibraryElement get library => _baseElement.library;

  @override
  ElementLocation get location => _baseElement.location;

  @override
  List<ElementAnnotation> get metadata => _baseElement.metadata;

  @override
  String get name => _baseElement.name;

  @override
  int get nameOffset => _baseElement.nameOffset;

  @override
  AstNode get node => _baseElement.node;

  @override
  Source get source => _baseElement.source;

  @override
  CompilationUnit get unit => _baseElement.unit;

  @override
  bool isAccessibleIn(LibraryElement library) => _baseElement.isAccessibleIn(library);

  @override
  bool get isDeprecated => _baseElement.isDeprecated;

  @override
  bool get isOverride => _baseElement.isOverride;

  @override
  bool get isPrivate => _baseElement.isPrivate;

  @override
  bool get isPublic => _baseElement.isPublic;

  @override
  bool get isSynthetic => _baseElement.isSynthetic;

  @override
  void visitChildren(ElementVisitor visitor) {
  }

  /**
   * Return the type in which the element is defined.
   *
   * @return the type in which the element is defined
   */
  ParameterizedType get definingType => _definingType;

  /**
   * If the given child is not `null`, use the given visitor to visit it.
   *
   * @param child the child to be visited
   * @param visitor the visitor to be used to visit the child
   */
  void safelyVisitChild(Element child, ElementVisitor visitor) {
    if (child != null) {
      child.accept(visitor);
    }
  }

  /**
   * Use the given visitor to visit all of the children in the given array.
   *
   * @param children the children to be visited
   * @param visitor the visitor being used to visit the children
   */
  void safelyVisitChildren(List<Element> children, ElementVisitor visitor) {
    if (children != null) {
      for (Element child in children) {
        child.accept(visitor);
      }
    }
  }

  /**
   * Return the type that results from replacing the type parameters in the given type with the type
   * arguments.
   *
   * @param type the type to be transformed
   * @return the result of transforming the type
   */
  DartType substituteFor(DartType type) {
    List<DartType> argumentTypes = _definingType.typeArguments;
    List<DartType> parameterTypes = TypeParameterTypeImpl.getTypes(_definingType.typeParameters);
    return type.substitute2(argumentTypes, parameterTypes);
  }

  /**
   * Return the array of types that results from replacing the type parameters in the given types
   * with the type arguments.
   *
   * @param types the types to be transformed
   * @return the result of transforming the types
   */
  List<InterfaceType> substituteFor2(List<InterfaceType> types) {
    int count = types.length;
    List<InterfaceType> substitutedTypes = new List<InterfaceType>(count);
    for (int i = 0; i < count; i++) {
      substitutedTypes[i] = substituteFor(types[i]);
    }
    return substitutedTypes;
  }
}

/**
 * Instances of the class `MethodMember` represent a method element defined in a parameterized
 * type where the values of the type parameters are known.
 */
class MethodMember extends ExecutableMember implements MethodElement {
  /**
   * If the given method's type is different when any type parameters from the defining type's
   * declaration are replaced with the actual type arguments from the defining type, create a method
   * member representing the given method. Return the member that was created, or the base method if
   * no member was created.
   *
   * @param baseMethod the base method for which a member might be created
   * @param definingType the type defining the parameters and arguments to be used in the
   *          substitution
   * @return the method element that will return the correctly substituted types
   */
  static MethodElement from(MethodElement baseMethod, InterfaceType definingType) {
    if (baseMethod == null || definingType.typeArguments.length == 0) {
      return baseMethod;
    }
    FunctionType baseType = baseMethod.type;
    List<DartType> argumentTypes = definingType.typeArguments;
    List<DartType> parameterTypes = definingType.element.type.typeArguments;
    FunctionType substitutedType = baseType.substitute2(argumentTypes, parameterTypes);
    if (baseType == substitutedType) {
      return baseMethod;
    }
    // TODO(brianwilkerson) Consider caching the substituted type in the instance. It would use more
    // memory but speed up some operations. We need to see how often the type is being re-computed.
    return new MethodMember(baseMethod, definingType);
  }

  /**
   * Initialize a newly created element to represent a method of the given parameterized type.
   *
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  MethodMember(MethodElement baseElement, InterfaceType definingType) : super(baseElement, definingType);

  @override
  accept(ElementVisitor visitor) => visitor.visitMethodElement(this);

  @override
  MethodElement get baseElement => super.baseElement as MethodElement;

  @override
  ClassElement get enclosingElement => baseElement.enclosingElement;

  @override
  MethodDeclaration get node => baseElement.node;

  @override
  bool get isAbstract => baseElement.isAbstract;

  @override
  String toString() {
    MethodElement baseElement = this.baseElement;
    List<ParameterElement> parameters = this.parameters;
    FunctionType type = this.type;
    JavaStringBuilder builder = new JavaStringBuilder();
    builder.append(baseElement.enclosingElement.displayName);
    builder.append(".");
    builder.append(baseElement.displayName);
    builder.append("(");
    int parameterCount = parameters.length;
    for (int i = 0; i < parameterCount; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(parameters[i]).toString();
    }
    builder.append(")");
    if (type != null) {
      builder.append(Element.RIGHT_ARROW);
      builder.append(type.returnType);
    }
    return builder.toString();
  }
}

/**
 * Instances of the class `ParameterMember` represent a parameter element defined in a
 * parameterized type where the values of the type parameters are known.
 */
class ParameterMember extends VariableMember implements ParameterElement {
  /**
   * If the given parameter's type is different when any type parameters from the defining type's
   * declaration are replaced with the actual type arguments from the defining type, create a
   * parameter member representing the given parameter. Return the member that was created, or the
   * base parameter if no member was created.
   *
   * @param baseParameter the base parameter for which a member might be created
   * @param definingType the type defining the parameters and arguments to be used in the
   *          substitution
   * @return the parameter element that will return the correctly substituted types
   */
  static ParameterElement from(ParameterElement baseParameter, ParameterizedType definingType) {
    if (baseParameter == null || definingType.typeArguments.length == 0) {
      return baseParameter;
    }
    // Check if parameter type depends on defining type type arguments.
    // It is possible that we did not resolve field formal parameter yet, so skip this check for it.
    bool isFieldFormal = baseParameter is FieldFormalParameterElement;
    if (!isFieldFormal) {
      DartType baseType = baseParameter.type;
      List<DartType> argumentTypes = definingType.typeArguments;
      List<DartType> parameterTypes = TypeParameterTypeImpl.getTypes(definingType.typeParameters);
      DartType substitutedType = baseType.substitute2(argumentTypes, parameterTypes);
      if (baseType == substitutedType) {
        return baseParameter;
      }
    }
    // TODO(brianwilkerson) Consider caching the substituted type in the instance. It would use more
    // memory but speed up some operations. We need to see how often the type is being re-computed.
    if (isFieldFormal) {
      return new FieldFormalParameterMember(baseParameter as FieldFormalParameterElement, definingType);
    }
    return new ParameterMember(baseParameter, definingType);
  }

  /**
   * Initialize a newly created element to represent a parameter of the given parameterized type.
   *
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  ParameterMember(ParameterElement baseElement, ParameterizedType definingType) : super(baseElement, definingType);

  @override
  accept(ElementVisitor visitor) => visitor.visitParameterElement(this);

  @override
  Element getAncestor(Predicate<Element> predicate) {
    Element element = baseElement.getAncestor(predicate);
    ParameterizedType definingType = this.definingType;
    if (definingType is InterfaceType) {
      InterfaceType definingInterfaceType = definingType;
      if (element is ConstructorElement) {
        return ConstructorMember.from(element, definingInterfaceType);
      } else if (element is MethodElement) {
        return MethodMember.from(element, definingInterfaceType);
      } else if (element is PropertyAccessorElement) {
        return PropertyAccessorMember.from(element, definingInterfaceType);
      }
    }
    return element;
  }

  @override
  ParameterElement get baseElement => super.baseElement as ParameterElement;

  @override
  SourceRange get defaultValueRange => baseElement.defaultValueRange;

  @override
  Element get enclosingElement => baseElement.enclosingElement;

  @override
  ParameterKind get parameterKind => baseElement.parameterKind;

  @override
  List<ParameterElement> get parameters {
    List<ParameterElement> baseParameters = baseElement.parameters;
    int parameterCount = baseParameters.length;
    if (parameterCount == 0) {
      return baseParameters;
    }
    List<ParameterElement> parameterizedParameters = new List<ParameterElement>(parameterCount);
    for (int i = 0; i < parameterCount; i++) {
      parameterizedParameters[i] = ParameterMember.from(baseParameters[i], definingType);
    }
    return parameterizedParameters;
  }

  @override
  SourceRange get visibleRange => baseElement.visibleRange;

  @override
  bool get isInitializingFormal => baseElement.isInitializingFormal;

  @override
  String toString() {
    ParameterElement baseElement = this.baseElement;
    String left = "";
    String right = "";
    while (true) {
      if (baseElement.parameterKind == ParameterKind.NAMED) {
        left = "{";
        right = "}";
      } else if (baseElement.parameterKind == ParameterKind.POSITIONAL) {
        left = "[";
        right = "]";
      } else if (baseElement.parameterKind == ParameterKind.REQUIRED) {
      }
      break;
    }
    JavaStringBuilder builder = new JavaStringBuilder();
    builder.append(left);
    builder.append(type);
    builder.append(" ");
    builder.append(baseElement.displayName);
    builder.append(right);
    return builder.toString();
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(parameters, visitor);
  }
}

/**
 * Instances of the class `PropertyAccessorMember` represent a property accessor element
 * defined in a parameterized type where the values of the type parameters are known.
 */
class PropertyAccessorMember extends ExecutableMember implements PropertyAccessorElement {
  /**
   * If the given property accessor's type is different when any type parameters from the defining
   * type's declaration are replaced with the actual type arguments from the defining type, create a
   * property accessor member representing the given property accessor. Return the member that was
   * created, or the base accessor if no member was created.
   *
   * @param baseAccessor the base property accessor for which a member might be created
   * @param definingType the type defining the parameters and arguments to be used in the
   *          substitution
   * @return the property accessor element that will return the correctly substituted types
   */
  static PropertyAccessorElement from(PropertyAccessorElement baseAccessor, InterfaceType definingType) {
    if (baseAccessor == null || definingType.typeArguments.length == 0) {
      return baseAccessor;
    }
    FunctionType baseType = baseAccessor.type;
    List<DartType> argumentTypes = definingType.typeArguments;
    List<DartType> parameterTypes = definingType.element.type.typeArguments;
    FunctionType substitutedType = baseType.substitute2(argumentTypes, parameterTypes);
    if (baseType == substitutedType) {
      return baseAccessor;
    }
    // TODO(brianwilkerson) Consider caching the substituted type in the instance. It would use more
    // memory but speed up some operations. We need to see how often the type is being re-computed.
    return new PropertyAccessorMember(baseAccessor, definingType);
  }

  /**
   * Initialize a newly created element to represent a property accessor of the given parameterized
   * type.
   *
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  PropertyAccessorMember(PropertyAccessorElement baseElement, InterfaceType definingType) : super(baseElement, definingType);

  @override
  accept(ElementVisitor visitor) => visitor.visitPropertyAccessorElement(this);

  @override
  PropertyAccessorElement get baseElement => super.baseElement as PropertyAccessorElement;

  @override
  PropertyAccessorElement get correspondingGetter => from(baseElement.correspondingGetter, definingType);

  @override
  PropertyAccessorElement get correspondingSetter => from(baseElement.correspondingSetter, definingType);

  @override
  Element get enclosingElement => baseElement.enclosingElement;

  @override
  PropertyInducingElement get variable {
    PropertyInducingElement variable = baseElement.variable;
    if (variable is FieldElement) {
      return FieldMember.from(variable, definingType);
    }
    return variable;
  }

  @override
  bool get isAbstract => baseElement.isAbstract;

  @override
  bool get isGetter => baseElement.isGetter;

  @override
  bool get isSetter => baseElement.isSetter;

  @override
  String toString() {
    PropertyAccessorElement baseElement = this.baseElement;
    List<ParameterElement> parameters = this.parameters;
    FunctionType type = this.type;
    JavaStringBuilder builder = new JavaStringBuilder();
    if (isGetter) {
      builder.append("get ");
    } else {
      builder.append("set ");
    }
    builder.append(baseElement.enclosingElement.displayName);
    builder.append(".");
    builder.append(baseElement.displayName);
    builder.append("(");
    int parameterCount = parameters.length;
    for (int i = 0; i < parameterCount; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(parameters[i]).toString();
    }
    builder.append(")");
    if (type != null) {
      builder.append(Element.RIGHT_ARROW);
      builder.append(type.returnType);
    }
    return builder.toString();
  }

  @override
  InterfaceType get definingType => super.definingType as InterfaceType;
}

/**
 * The abstract class `VariableMember` defines the behavior common to members that represent a
 * variable element defined in a parameterized type where the values of the type parameters are
 * known.
 */
abstract class VariableMember extends Member implements VariableElement {
  /**
   * Initialize a newly created element to represent an executable element of the given
   * parameterized type.
   *
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  VariableMember(VariableElement baseElement, ParameterizedType definingType) : super(baseElement, definingType);

  @override
  VariableElement get baseElement => super.baseElement as VariableElement;

  @override
  FunctionElement get initializer {
    //
    // Elements within this element should have type parameters substituted, just like this element.
    //
    throw new UnsupportedOperationException();
  }

  @override
  VariableDeclaration get node => baseElement.node;

  @override
  DartType get type => substituteFor(baseElement.type);

  @override
  bool get isConst => baseElement.isConst;

  @override
  bool get isFinal => baseElement.isFinal;

  @override
  void visitChildren(ElementVisitor visitor) {
    // TODO(brianwilkerson) We need to finish implementing the accessors used below so that we can
    // safely invoke them.
    super.visitChildren(visitor);
    safelyVisitChild(baseElement.initializer, visitor);
  }
}

/**
 * The unique instance of the class `BottomTypeImpl` implements the type `bottom`.
 */
class BottomTypeImpl extends TypeImpl {
  /**
   * The unique instance of this class.
   */
  static BottomTypeImpl _INSTANCE = new BottomTypeImpl();

  /**
   * Return the unique instance of this class.
   *
   * @return the unique instance of this class
   */
  static BottomTypeImpl get instance => _INSTANCE;

  /**
   * Prevent the creation of instances of this class.
   */
  BottomTypeImpl() : super(null, "<bottom>");

  @override
  bool operator ==(Object object) => identical(object, this);

  @override
  int get hashCode => 0;

  @override
  bool get isBottom => true;

  @override
  bool isSupertypeOf(DartType type) => false;

  @override
  BottomTypeImpl substitute2(List<DartType> argumentTypes, List<DartType> parameterTypes) => this;

  @override
  bool internalEquals(Object object, Set<ElementPair> visitedElementPairs) => identical(object, this);

  @override
  bool internalIsMoreSpecificThan(DartType type, bool withDynamic, Set<TypeImpl_TypePair> visitedTypePairs) => true;

  @override
  bool internalIsSubtypeOf(DartType type, Set<TypeImpl_TypePair> visitedTypePairs) => true;
}

/**
 * The unique instance of the class `DynamicTypeImpl` implements the type `dynamic`.
 */
class DynamicTypeImpl extends TypeImpl {
  /**
   * The unique instance of this class.
   */
  static DynamicTypeImpl _INSTANCE = new DynamicTypeImpl();

  /**
   * Return the unique instance of this class.
   *
   * @return the unique instance of this class
   */
  static DynamicTypeImpl get instance => _INSTANCE;

  /**
   * Prevent the creation of instances of this class.
   */
  DynamicTypeImpl() : super(new DynamicElementImpl(), Keyword.DYNAMIC.syntax) {
    (element as DynamicElementImpl).type = this;
  }

  @override
  bool operator ==(Object object) => identical(object, this);

  @override
  int get hashCode => 1;

  @override
  bool get isDynamic => true;

  @override
  bool isSupertypeOf(DartType type) => true;

  @override
  DartType substitute2(List<DartType> argumentTypes, List<DartType> parameterTypes) {
    int length = parameterTypes.length;
    for (int i = 0; i < length; i++) {
      if (parameterTypes[i] == this) {
        return argumentTypes[i];
      }
    }
    return this;
  }

  @override
  bool internalEquals(Object object, Set<ElementPair> visitedElementPairs) => identical(object, this);

  @override
  bool internalIsMoreSpecificThan(DartType type, bool withDynamic, Set<TypeImpl_TypePair> visitedTypePairs) {
    // T is S
    if (identical(this, type)) {
      return true;
    }
    // else
    return withDynamic;
  }

  @override
  bool internalIsSubtypeOf(DartType type, Set<TypeImpl_TypePair> visitedTypePairs) => true;
}

/**
 * Instances of the class `FunctionTypeImpl` defines the behavior common to objects
 * representing the type of a function, method, constructor, getter, or setter.
 */
class FunctionTypeImpl extends TypeImpl implements FunctionType {
  /**
   * Return `true` if all of the name/type pairs in the first map are equal to the
   * corresponding name/type pairs in the second map. The maps are expected to iterate over their
   * entries in the same order in which those entries were added to the map.
   *
   * @param firstTypes the first map of name/type pairs being compared
   * @param secondTypes the second map of name/type pairs being compared
   * @param visitedElementPairs a set of visited element pairs
   * @return `true` if all of the name/type pairs in the first map are equal to the
   *         corresponding name/type pairs in the second map
   */
  static bool _equals(Map<String, DartType> firstTypes, Map<String, DartType> secondTypes, Set<ElementPair> visitedElementPairs) {
    if (secondTypes.length != firstTypes.length) {
      return false;
    }
    JavaIterator<MapEntry<String, DartType>> firstIterator = new JavaIterator(getMapEntrySet(firstTypes));
    JavaIterator<MapEntry<String, DartType>> secondIterator = new JavaIterator(getMapEntrySet(secondTypes));
    while (firstIterator.hasNext) {
      MapEntry<String, DartType> firstEntry = firstIterator.next();
      MapEntry<String, DartType> secondEntry = secondIterator.next();
      if (firstEntry.getKey() != secondEntry.getKey() || !(firstEntry.getValue() as TypeImpl).internalEquals(secondEntry.getValue(), visitedElementPairs)) {
        return false;
      }
    }
    return true;
  }

  /**
   * An array containing the actual types of the type arguments.
   */
  List<DartType> typeArguments = TypeImpl.EMPTY_ARRAY;

  /**
   * Initialize a newly created function type to be declared by the given element and to have the
   * given name.
   *
   * @param element the element representing the declaration of the function type
   */
  FunctionTypeImpl.con1(ExecutableElement element) : super(element, element == null ? null : element.name);

  /**
   * Initialize a newly created function type to be declared by the given element and to have the
   * given name.
   *
   * @param element the element representing the declaration of the function type
   */
  FunctionTypeImpl.con2(FunctionTypeAliasElement element) : super(element, element == null ? null : element.name);

  @override
  bool operator ==(Object object) => internalEquals(object, new Set<ElementPair>());

  @override
  String get displayName {
    String name = this.name;
    if (name == null || name.length == 0) {
      // TODO(brianwilkerson) Determine whether function types should ever have an empty name.
      List<DartType> normalParameterTypes = this.normalParameterTypes;
      List<DartType> optionalParameterTypes = this.optionalParameterTypes;
      Map<String, DartType> namedParameterTypes = this.namedParameterTypes;
      DartType returnType = this.returnType;
      JavaStringBuilder builder = new JavaStringBuilder();
      builder.append("(");
      bool needsComma = false;
      if (normalParameterTypes.length > 0) {
        for (DartType type in normalParameterTypes) {
          if (needsComma) {
            builder.append(", ");
          } else {
            needsComma = true;
          }
          builder.append(type.displayName);
        }
      }
      if (optionalParameterTypes.length > 0) {
        if (needsComma) {
          builder.append(", ");
          needsComma = false;
        }
        builder.append("[");
        for (DartType type in optionalParameterTypes) {
          if (needsComma) {
            builder.append(", ");
          } else {
            needsComma = true;
          }
          builder.append(type.displayName);
        }
        builder.append("]");
        needsComma = true;
      }
      if (namedParameterTypes.length > 0) {
        if (needsComma) {
          builder.append(", ");
          needsComma = false;
        }
        builder.append("{");
        for (MapEntry<String, DartType> entry in getMapEntrySet(namedParameterTypes)) {
          if (needsComma) {
            builder.append(", ");
          } else {
            needsComma = true;
          }
          builder.append(entry.getKey());
          builder.append(": ");
          builder.append(entry.getValue().displayName);
        }
        builder.append("}");
        needsComma = true;
      }
      builder.append(")");
      builder.append(Element.RIGHT_ARROW);
      if (returnType == null) {
        builder.append("null");
      } else {
        builder.append(returnType.displayName);
      }
      name = builder.toString();
    }
    return name;
  }

  @override
  Map<String, DartType> get namedParameterTypes {
    LinkedHashMap<String, DartType> namedParameterTypes = new LinkedHashMap<String, DartType>();
    List<ParameterElement> parameters = baseParameters;
    if (parameters.length == 0) {
      return namedParameterTypes;
    }
    List<DartType> typeParameters = TypeParameterTypeImpl.getTypes(this.typeParameters);
    for (ParameterElement parameter in parameters) {
      if (parameter.parameterKind == ParameterKind.NAMED) {
        namedParameterTypes[parameter.name] = parameter.type.substitute2(typeArguments, typeParameters);
      }
    }
    return namedParameterTypes;
  }

  @override
  List<DartType> get normalParameterTypes {
    List<ParameterElement> parameters = baseParameters;
    if (parameters.length == 0) {
      return TypeImpl.EMPTY_ARRAY;
    }
    List<DartType> typeParameters = TypeParameterTypeImpl.getTypes(this.typeParameters);
    List<DartType> types = new List<DartType>();
    for (ParameterElement parameter in parameters) {
      if (parameter.parameterKind == ParameterKind.REQUIRED) {
        types.add(parameter.type.substitute2(typeArguments, typeParameters));
      }
    }
    return new List.from(types);
  }

  @override
  List<DartType> get optionalParameterTypes {
    List<ParameterElement> parameters = baseParameters;
    if (parameters.length == 0) {
      return TypeImpl.EMPTY_ARRAY;
    }
    List<DartType> typeParameters = TypeParameterTypeImpl.getTypes(this.typeParameters);
    List<DartType> types = new List<DartType>();
    for (ParameterElement parameter in parameters) {
      if (parameter.parameterKind == ParameterKind.POSITIONAL) {
        types.add(parameter.type.substitute2(typeArguments, typeParameters));
      }
    }
    return new List.from(types);
  }

  @override
  List<ParameterElement> get parameters {
    List<ParameterElement> baseParameters = this.baseParameters;
    // no parameters, quick return
    int parameterCount = baseParameters.length;
    if (parameterCount == 0) {
      return baseParameters;
    }
    // create specialized parameters
    List<ParameterElement> specializedParameters = new List<ParameterElement>(parameterCount);
    for (int i = 0; i < parameterCount; i++) {
      specializedParameters[i] = ParameterMember.from(baseParameters[i], this);
    }
    return specializedParameters;
  }

  @override
  DartType get returnType {
    DartType baseReturnType = this.baseReturnType;
    if (baseReturnType == null) {
      // TODO(brianwilkerson) This is a patch. The return type should never be null and we need to
      // understand why it is and fix it.
      return DynamicTypeImpl.instance;
    }
    return baseReturnType.substitute2(typeArguments, TypeParameterTypeImpl.getTypes(typeParameters));
  }

  @override
  List<TypeParameterElement> get typeParameters {
    Element element = this.element;
    if (element is FunctionTypeAliasElement) {
      return element.typeParameters;
    }
    ClassElement definingClass = element.getAncestor((element) => element is ClassElement);
    if (definingClass != null) {
      return definingClass.typeParameters;
    }
    return TypeParameterElementImpl.EMPTY_ARRAY;
  }

  @override
  int get hashCode {
    if (element == null) {
      return 0;
    }
    // Reference the arrays of parameters
    List<DartType> normalParameterTypes = this.normalParameterTypes;
    List<DartType> optionalParameterTypes = this.optionalParameterTypes;
    Iterable<DartType> namedParameterTypes = this.namedParameterTypes.values;
    // Generate the hashCode
    int hashCode = returnType.hashCode;
    for (int i = 0; i < normalParameterTypes.length; i++) {
      hashCode = (hashCode << 1) + normalParameterTypes[i].hashCode;
    }
    for (int i = 0; i < optionalParameterTypes.length; i++) {
      hashCode = (hashCode << 1) + optionalParameterTypes[i].hashCode;
    }
    for (DartType type in namedParameterTypes) {
      hashCode = (hashCode << 1) + type.hashCode;
    }
    return hashCode;
  }

  @override
  bool internalIsMoreSpecificThan(DartType type, bool withDynamic, Set<TypeImpl_TypePair> visitedTypePairs) {
    // trivial base cases
    if (type == null) {
      return false;
    } else if (identical(this, type) || type.isDynamic || type.isDartCoreFunction || type.isObject) {
      return true;
    } else if (type is! FunctionType) {
      return false;
    } else if (this == type) {
      return true;
    }
    FunctionType t = this;
    FunctionType s = type as FunctionType;
    List<DartType> tTypes = t.normalParameterTypes;
    List<DartType> tOpTypes = t.optionalParameterTypes;
    List<DartType> sTypes = s.normalParameterTypes;
    List<DartType> sOpTypes = s.optionalParameterTypes;
    // If one function has positional and the other has named parameters, return false.
    if ((sOpTypes.length > 0 && t.namedParameterTypes.length > 0) || (tOpTypes.length > 0 && s.namedParameterTypes.length > 0)) {
      return false;
    }
    // named parameters case
    if (t.namedParameterTypes.length > 0) {
      // check that the number of required parameters are equal, and check that every t_i is
      // more specific than every s_i
      if (t.normalParameterTypes.length != s.normalParameterTypes.length) {
        return false;
      } else if (t.normalParameterTypes.length > 0) {
        for (int i = 0; i < tTypes.length; i++) {
          if (!(tTypes[i] as TypeImpl).isMoreSpecificThan2(sTypes[i], withDynamic, visitedTypePairs)) {
            return false;
          }
        }
      }
      Map<String, DartType> namedTypesT = t.namedParameterTypes;
      Map<String, DartType> namedTypesS = s.namedParameterTypes;
      // if k >= m is false, return false: the passed function type has more named parameter types than this
      if (namedTypesT.length < namedTypesS.length) {
        return false;
      }
      // Loop through each element in S verifying that T has a matching parameter name and that the
      // corresponding type is more specific then the type in S.
      JavaIterator<MapEntry<String, DartType>> iteratorS = new JavaIterator(getMapEntrySet(namedTypesS));
      while (iteratorS.hasNext) {
        MapEntry<String, DartType> entryS = iteratorS.next();
        DartType typeT = namedTypesT[entryS.getKey()];
        if (typeT == null) {
          return false;
        }
        if (!(typeT as TypeImpl).isMoreSpecificThan2(entryS.getValue(), withDynamic, visitedTypePairs)) {
          return false;
        }
      }
    } else if (s.namedParameterTypes.length > 0) {
      return false;
    } else {
      // positional parameter case
      int tArgLength = tTypes.length + tOpTypes.length;
      int sArgLength = sTypes.length + sOpTypes.length;
      // Check that the total number of parameters in t is greater than or equal to the number of
      // parameters in s and that the number of required parameters in s is greater than or equal to
      // the number of required parameters in t.
      if (tArgLength < sArgLength || sTypes.length < tTypes.length) {
        return false;
      }
      if (tOpTypes.length == 0 && sOpTypes.length == 0) {
        // No positional arguments, don't copy contents to new array
        for (int i = 0; i < sTypes.length; i++) {
          if (!(tTypes[i] as TypeImpl).isMoreSpecificThan2(sTypes[i], withDynamic, visitedTypePairs)) {
            return false;
          }
        }
      } else {
        // Else, we do have positional parameters, copy required and positional parameter types into
        // arrays to do the compare (for loop below).
        List<DartType> tAllTypes = new List<DartType>(sArgLength);
        for (int i = 0; i < tTypes.length; i++) {
          tAllTypes[i] = tTypes[i];
        }
        for (int i = tTypes.length, j = 0; i < sArgLength; i++, j++) {
          tAllTypes[i] = tOpTypes[j];
        }
        List<DartType> sAllTypes = new List<DartType>(sArgLength);
        for (int i = 0; i < sTypes.length; i++) {
          sAllTypes[i] = sTypes[i];
        }
        for (int i = sTypes.length, j = 0; i < sArgLength; i++, j++) {
          sAllTypes[i] = sOpTypes[j];
        }
        for (int i = 0; i < sAllTypes.length; i++) {
          if (!(tAllTypes[i] as TypeImpl).isMoreSpecificThan2(sAllTypes[i], withDynamic, visitedTypePairs)) {
            return false;
          }
        }
      }
    }
    DartType tRetType = t.returnType;
    DartType sRetType = s.returnType;
    return sRetType.isVoid || (tRetType as TypeImpl).isMoreSpecificThan2(sRetType, withDynamic, visitedTypePairs);
  }

  @override
  bool isAssignableTo(DartType type) => isSubtypeOf2(type, new Set<TypeImpl_TypePair>());

  @override
  FunctionTypeImpl substitute3(List<DartType> argumentTypes) => substitute2(argumentTypes, typeArguments);

  @override
  FunctionTypeImpl substitute2(List<DartType> argumentTypes, List<DartType> parameterTypes) {
    if (argumentTypes.length != parameterTypes.length) {
      throw new IllegalArgumentException("argumentTypes.length (${argumentTypes.length}) != parameterTypes.length (${parameterTypes.length})");
    }
    if (argumentTypes.length == 0) {
      return this;
    }
    Element element = this.element;
    FunctionTypeImpl newType = (element is ExecutableElement) ? new FunctionTypeImpl.con1(element) : new FunctionTypeImpl.con2(element as FunctionTypeAliasElement);
    newType.typeArguments = TypeImpl.substitute(typeArguments, argumentTypes, parameterTypes);
    return newType;
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    List<DartType> normalParameterTypes = this.normalParameterTypes;
    List<DartType> optionalParameterTypes = this.optionalParameterTypes;
    Map<String, DartType> namedParameterTypes = this.namedParameterTypes;
    DartType returnType = this.returnType;
    builder.append("(");
    bool needsComma = false;
    if (normalParameterTypes.length > 0) {
      for (DartType type in normalParameterTypes) {
        if (needsComma) {
          builder.append(", ");
        } else {
          needsComma = true;
        }
        (type as TypeImpl).appendTo(builder);
      }
    }
    if (optionalParameterTypes.length > 0) {
      if (needsComma) {
        builder.append(", ");
        needsComma = false;
      }
      builder.append("[");
      for (DartType type in optionalParameterTypes) {
        if (needsComma) {
          builder.append(", ");
        } else {
          needsComma = true;
        }
        (type as TypeImpl).appendTo(builder);
      }
      builder.append("]");
      needsComma = true;
    }
    if (namedParameterTypes.length > 0) {
      if (needsComma) {
        builder.append(", ");
        needsComma = false;
      }
      builder.append("{");
      for (MapEntry<String, DartType> entry in getMapEntrySet(namedParameterTypes)) {
        if (needsComma) {
          builder.append(", ");
        } else {
          needsComma = true;
        }
        builder.append(entry.getKey());
        builder.append(": ");
        (entry.getValue() as TypeImpl).appendTo(builder);
      }
      builder.append("}");
      needsComma = true;
    }
    builder.append(")");
    builder.append(Element.RIGHT_ARROW);
    if (returnType == null) {
      builder.append("null");
    } else {
      (returnType as TypeImpl).appendTo(builder);
    }
  }

  /**
   * @return the base parameter elements of this function element, not `null`.
   */
  List<ParameterElement> get baseParameters {
    Element element = this.element;
    if (element is ExecutableElement) {
      return element.parameters;
    } else {
      return (element as FunctionTypeAliasElement).parameters;
    }
  }

  @override
  bool internalEquals(Object object, Set<ElementPair> visitedElementPairs) {
    if (object is! FunctionTypeImpl) {
      return false;
    }
    FunctionTypeImpl otherType = object as FunctionTypeImpl;
    // If the visitedTypePairs already has the pair (this, type), use the elements to determine equality
    ElementPair elementPair = new ElementPair(element, otherType.element);
    if (!visitedElementPairs.add(elementPair)) {
      return elementPair.firstElt == elementPair.secondElt;
    }
    // Compute the result
    bool result = TypeImpl.equalArrays(normalParameterTypes, otherType.normalParameterTypes, visitedElementPairs) && TypeImpl.equalArrays(optionalParameterTypes, otherType.optionalParameterTypes, visitedElementPairs) && _equals(namedParameterTypes, otherType.namedParameterTypes, visitedElementPairs) && (returnType as TypeImpl).internalEquals(otherType.returnType, visitedElementPairs);
    // Remove the pair from our visited pairs list
    visitedElementPairs.remove(elementPair);
    // Return the result
    return result;
  }

  @override
  bool internalIsSubtypeOf(DartType type, Set<TypeImpl_TypePair> visitedTypePairs) {
    // trivial base cases
    if (type == null) {
      return false;
    } else if (identical(this, type) || type.isDynamic || type.isDartCoreFunction || type.isObject) {
      return true;
    } else if (type is! FunctionType) {
      return false;
    } else if (this == type) {
      return true;
    }
    FunctionType t = this;
    FunctionType s = type as FunctionType;
    List<DartType> tTypes = t.normalParameterTypes;
    List<DartType> tOpTypes = t.optionalParameterTypes;
    List<DartType> sTypes = s.normalParameterTypes;
    List<DartType> sOpTypes = s.optionalParameterTypes;
    // If one function has positional and the other has named parameters, return false.
    if ((sOpTypes.length > 0 && t.namedParameterTypes.length > 0) || (tOpTypes.length > 0 && s.namedParameterTypes.length > 0)) {
      return false;
    }
    // named parameters case
    if (t.namedParameterTypes.length > 0) {
      // check that the number of required parameters are equal, and check that every t_i is
      // assignable to every s_i
      if (t.normalParameterTypes.length != s.normalParameterTypes.length) {
        return false;
      } else if (t.normalParameterTypes.length > 0) {
        for (int i = 0; i < tTypes.length; i++) {
          if (!(tTypes[i] as TypeImpl).isAssignableTo2(sTypes[i], visitedTypePairs)) {
            return false;
          }
        }
      }
      Map<String, DartType> namedTypesT = t.namedParameterTypes;
      Map<String, DartType> namedTypesS = s.namedParameterTypes;
      // if k >= m is false, return false: the passed function type has more named parameter types than this
      if (namedTypesT.length < namedTypesS.length) {
        return false;
      }
      // Loop through each element in S verifying that T has a matching parameter name and that the
      // corresponding type is assignable to the type in S.
      JavaIterator<MapEntry<String, DartType>> iteratorS = new JavaIterator(getMapEntrySet(namedTypesS));
      while (iteratorS.hasNext) {
        MapEntry<String, DartType> entryS = iteratorS.next();
        DartType typeT = namedTypesT[entryS.getKey()];
        if (typeT == null) {
          return false;
        }
        if (!(typeT as TypeImpl).isAssignableTo2(entryS.getValue(), visitedTypePairs)) {
          return false;
        }
      }
    } else if (s.namedParameterTypes.length > 0) {
      return false;
    } else {
      // positional parameter case
      int tArgLength = tTypes.length + tOpTypes.length;
      int sArgLength = sTypes.length + sOpTypes.length;
      // Check that the total number of parameters in t is greater than or equal to the number of
      // parameters in s and that the number of required parameters in s is greater than or equal to
      // the number of required parameters in t.
      if (tArgLength < sArgLength || sTypes.length < tTypes.length) {
        return false;
      }
      if (tOpTypes.length == 0 && sOpTypes.length == 0) {
        // No positional arguments, don't copy contents to new array
        for (int i = 0; i < sTypes.length; i++) {
          if (!(tTypes[i] as TypeImpl).isAssignableTo2(sTypes[i], visitedTypePairs)) {
            return false;
          }
        }
      } else {
        // Else, we do have positional parameters, copy required and positional parameter types into
        // arrays to do the compare (for loop below).
        List<DartType> tAllTypes = new List<DartType>(sArgLength);
        for (int i = 0; i < tTypes.length; i++) {
          tAllTypes[i] = tTypes[i];
        }
        for (int i = tTypes.length, j = 0; i < sArgLength; i++, j++) {
          tAllTypes[i] = tOpTypes[j];
        }
        List<DartType> sAllTypes = new List<DartType>(sArgLength);
        for (int i = 0; i < sTypes.length; i++) {
          sAllTypes[i] = sTypes[i];
        }
        for (int i = sTypes.length, j = 0; i < sArgLength; i++, j++) {
          sAllTypes[i] = sOpTypes[j];
        }
        for (int i = 0; i < sAllTypes.length; i++) {
          if (!(tAllTypes[i] as TypeImpl).isAssignableTo2(sAllTypes[i], visitedTypePairs)) {
            return false;
          }
        }
      }
    }
    DartType tRetType = t.returnType;
    DartType sRetType = s.returnType;
    return sRetType.isVoid || (tRetType as TypeImpl).isAssignableTo2(sRetType, visitedTypePairs);
  }

  /**
   * Return the return type defined by this function's element.
   *
   * @return the return type defined by this function's element
   */
  DartType get baseReturnType {
    Element element = this.element;
    if (element is ExecutableElement) {
      return element.returnType;
    } else {
      return (element as FunctionTypeAliasElement).returnType;
    }
  }
}

/**
 * Instances of the class `InterfaceTypeImpl` defines the behavior common to objects
 * representing the type introduced by either a class or an interface, or a reference to such a
 * type.
 */
class InterfaceTypeImpl extends TypeImpl implements InterfaceType {
  /**
   * An empty array of types.
   */
  static List<InterfaceType> EMPTY_ARRAY = new List<InterfaceType>(0);

  /**
   * This method computes the longest inheritance path from some passed [Type] to Object.
   *
   * @param type the [Type] to compute the longest inheritance path of from the passed
   *          [Type] to Object
   * @return the computed longest inheritance path to Object
   * @see InterfaceType#getLeastUpperBound(Type)
   */
  static int computeLongestInheritancePathToObject(InterfaceType type) => _computeLongestInheritancePathToObject(type, 0, new Set<ClassElement>());

  /**
   * Returns the set of all superinterfaces of the passed [Type].
   *
   * @param type the [Type] to compute the set of superinterfaces of
   * @return the [Set] of superinterfaces of the passed [Type]
   * @see #getLeastUpperBound(Type)
   */
  static Set<InterfaceType> computeSuperinterfaceSet(InterfaceType type) => _computeSuperinterfaceSet(type, new Set<InterfaceType>());

  /**
   * This method computes the longest inheritance path from some passed [Type] to Object. This
   * method calls itself recursively, callers should use the public method
   * [computeLongestInheritancePathToObject].
   *
   * @param type the [Type] to compute the longest inheritance path of from the passed
   *          [Type] to Object
   * @param depth a field used recursively
   * @param visitedClasses the classes that have already been visited
   * @return the computed longest inheritance path to Object
   * @see #computeLongestInheritancePathToObject(Type)
   * @see #getLeastUpperBound(Type)
   */
  static int _computeLongestInheritancePathToObject(InterfaceType type, int depth, Set<ClassElement> visitedClasses) {
    ClassElement classElement = type.element;
    // Object case
    if (classElement.supertype == null || visitedClasses.contains(classElement)) {
      return depth;
    }
    int longestPath = 1;
    try {
      visitedClasses.add(classElement);
      List<InterfaceType> superinterfaces = classElement.interfaces;
      int pathLength;
      if (superinterfaces.length > 0) {
        // loop through each of the superinterfaces recursively calling this method and keeping track
        // of the longest path to return
        for (InterfaceType superinterface in superinterfaces) {
          pathLength = _computeLongestInheritancePathToObject(superinterface, depth + 1, visitedClasses);
          if (pathLength > longestPath) {
            longestPath = pathLength;
          }
        }
      }
      // finally, perform this same check on the super type
      // TODO(brianwilkerson) Does this also need to add in the number of mixin classes?
      InterfaceType supertype = classElement.supertype;
      pathLength = _computeLongestInheritancePathToObject(supertype, depth + 1, visitedClasses);
      if (pathLength > longestPath) {
        longestPath = pathLength;
      }
    } finally {
      visitedClasses.remove(classElement);
    }
    return longestPath;
  }

  /**
   * Returns the set of all superinterfaces of the passed [Type]. This is a recursive method,
   * callers should call the public [computeSuperinterfaceSet].
   *
   * @param type the [Type] to compute the set of superinterfaces of
   * @param set a [HashSet] used recursively by this method
   * @return the [Set] of superinterfaces of the passed [Type]
   * @see #computeSuperinterfaceSet(Type)
   * @see #getLeastUpperBound(Type)
   */
  static Set<InterfaceType> _computeSuperinterfaceSet(InterfaceType type, Set<InterfaceType> set) {
    Element element = type.element;
    if (element != null && element is ClassElement) {
      ClassElement classElement = element;
      List<InterfaceType> superinterfaces = classElement.interfaces;
      for (InterfaceType superinterface in superinterfaces) {
        if (set.add(superinterface)) {
          _computeSuperinterfaceSet(superinterface, set);
        }
      }
      InterfaceType supertype = classElement.supertype;
      if (supertype != null) {
        if (set.add(supertype)) {
          _computeSuperinterfaceSet(supertype, set);
        }
      }
    }
    return set;
  }

  /**
   * Return the intersection of the given sets of types, where intersection is based on the equality
   * of the elements of the types rather than on the equality of the types themselves. In cases
   * where two non-equal types have equal elements, which only happens when the class is
   * parameterized, the type that is added to the intersection is the base type with type arguments
   * that are the least upper bound of the type arguments of the two types.
   *
   * @param first the first set of types to be intersected
   * @param second the second set of types to be intersected
   * @return the intersection of the given sets of types
   */
  static List<InterfaceType> _intersection(Set<InterfaceType> first, Set<InterfaceType> second) {
    Map<ClassElement, InterfaceType> firstMap = new Map<ClassElement, InterfaceType>();
    for (InterfaceType firstType in first) {
      firstMap[firstType.element] = firstType;
    }
    Set<InterfaceType> result = new Set<InterfaceType>();
    for (InterfaceType secondType in second) {
      InterfaceType firstType = firstMap[secondType.element];
      if (firstType != null) {
        result.add(_leastUpperBound(firstType, secondType));
      }
    }
    return new List.from(result);
  }

  /**
   * Return the "least upper bound" of the given types under the assumption that the types have the
   * same element and differ only in terms of the type arguments. The resulting type is composed by
   * comparing the corresponding type arguments, keeping those that are the same, and using
   * 'dynamic' for those that are different.
   *
   * @param firstType the first type
   * @param secondType the second type
   * @return the "least upper bound" of the given types
   */
  static InterfaceType _leastUpperBound(InterfaceType firstType, InterfaceType secondType) {
    if (firstType == secondType) {
      return firstType;
    }
    List<DartType> firstArguments = firstType.typeArguments;
    List<DartType> secondArguments = secondType.typeArguments;
    int argumentCount = firstArguments.length;
    if (argumentCount == 0) {
      return firstType;
    }
    List<DartType> lubArguments = new List<DartType>(argumentCount);
    for (int i = 0; i < argumentCount; i++) {
      //
      // Ideally we would take the least upper bound of the two argument types, but this can cause
      // an infinite recursion (such as when finding the least upper bound of String and num).
      //
      if (firstArguments[i] == secondArguments[i]) {
        lubArguments[i] = firstArguments[i];
      }
      if (lubArguments[i] == null) {
        lubArguments[i] = DynamicTypeImpl.instance;
      }
    }
    InterfaceTypeImpl lub = new InterfaceTypeImpl.con1(firstType.element);
    lub.typeArguments = lubArguments;
    return lub;
  }

  /**
   * An array containing the actual types of the type arguments.
   */
  List<DartType> typeArguments = TypeImpl.EMPTY_ARRAY;

  /**
   * Initialize a newly created type to be declared by the given element.
   *
   * @param element the element representing the declaration of the type
   */
  InterfaceTypeImpl.con1(ClassElement element) : super(element, element.displayName);

  /**
   * Initialize a newly created type to have the given name. This constructor should only be used in
   * cases where there is no declaration of the type.
   *
   * @param name the name of the type
   */
  InterfaceTypeImpl.con2(String name) : super(null, name);

  @override
  bool operator ==(Object object) => internalEquals(object, new Set<ElementPair>());

  @override
  List<PropertyAccessorElement> get accessors {
    List<PropertyAccessorElement> accessors = element.accessors;
    List<PropertyAccessorElement> members = new List<PropertyAccessorElement>(accessors.length);
    for (int i = 0; i < accessors.length; i++) {
      members[i] = PropertyAccessorMember.from(accessors[i], this);
    }
    return members;
  }

  @override
  String get displayName {
    String name = this.name;
    List<DartType> typeArguments = this.typeArguments;
    bool allDynamic = true;
    for (DartType type in typeArguments) {
      if (type != null && !type.isDynamic) {
        allDynamic = false;
        break;
      }
    }
    // If there is at least one non-dynamic type, then list them out
    if (!allDynamic) {
      JavaStringBuilder builder = new JavaStringBuilder();
      builder.append(name);
      builder.append("<");
      for (int i = 0; i < typeArguments.length; i++) {
        if (i != 0) {
          builder.append(", ");
        }
        DartType typeArg = typeArguments[i];
        builder.append(typeArg.displayName);
      }
      builder.append(">");
      name = builder.toString();
    }
    return name;
  }

  @override
  ClassElement get element => super.element as ClassElement;

  @override
  PropertyAccessorElement getGetter(String getterName) => PropertyAccessorMember.from((element as ClassElementImpl).getGetter(getterName), this);

  @override
  List<InterfaceType> get interfaces {
    ClassElement classElement = element;
    List<InterfaceType> interfaces = classElement.interfaces;
    List<TypeParameterElement> typeParameters = classElement.typeParameters;
    List<DartType> parameterTypes = classElement.type.typeArguments;
    if (typeParameters.length == 0) {
      return interfaces;
    }
    int count = interfaces.length;
    List<InterfaceType> typedInterfaces = new List<InterfaceType>(count);
    for (int i = 0; i < count; i++) {
      typedInterfaces[i] = interfaces[i].substitute2(typeArguments, parameterTypes);
    }
    return typedInterfaces;
  }

  @override
  DartType getLeastUpperBound(DartType type) {
    // quick check for self
    if (identical(type, this)) {
      return this;
    }
    // dynamic
    DartType dynamicType = DynamicTypeImpl.instance;
    if (identical(this, dynamicType) || identical(type, dynamicType)) {
      return dynamicType;
    }
    // TODO (jwren) opportunity here for a better, faster algorithm if this turns out to be a bottle-neck
    if (type is! InterfaceType) {
      return null;
    }
    // new names to match up with the spec
    InterfaceType i = this;
    InterfaceType j = type as InterfaceType;
    // compute set of supertypes
    Set<InterfaceType> si = computeSuperinterfaceSet(i);
    Set<InterfaceType> sj = computeSuperinterfaceSet(j);
    // union si with i and sj with j
    si.add(i);
    sj.add(j);
    // compute intersection, reference as set 's'
    List<InterfaceType> s = _intersection(si, sj);
    // for each element in Set s, compute the largest inheritance path to Object
    List<int> depths = new List<int>.filled(s.length, 0);
    int maxDepth = 0;
    for (int n = 0; n < s.length; n++) {
      depths[n] = computeLongestInheritancePathToObject(s[n]);
      if (depths[n] > maxDepth) {
        maxDepth = depths[n];
      }
    }
    // ensure that the currently computed maxDepth is unique,
    // otherwise, decrement and test for uniqueness again
    for (; maxDepth >= 0; maxDepth--) {
      int indexOfLeastUpperBound = -1;
      int numberOfTypesAtMaxDepth = 0;
      for (int m = 0; m < depths.length; m++) {
        if (depths[m] == maxDepth) {
          numberOfTypesAtMaxDepth++;
          indexOfLeastUpperBound = m;
        }
      }
      if (numberOfTypesAtMaxDepth == 1) {
        return s[indexOfLeastUpperBound];
      }
    }
    // illegal state, log and return null- Object at maxDepth == 0 should always return itself as
    // the least upper bound.
    // TODO (jwren) log the error state
    return null;
  }

  @override
  MethodElement getMethod(String methodName) => MethodMember.from((element as ClassElementImpl).getMethod(methodName), this);

  @override
  List<MethodElement> get methods {
    List<MethodElement> methods = element.methods;
    List<MethodElement> members = new List<MethodElement>(methods.length);
    for (int i = 0; i < methods.length; i++) {
      members[i] = MethodMember.from(methods[i], this);
    }
    return members;
  }

  @override
  List<InterfaceType> get mixins {
    ClassElement classElement = element;
    List<InterfaceType> mixins = classElement.mixins;
    List<TypeParameterElement> typeParameters = classElement.typeParameters;
    List<DartType> parameterTypes = classElement.type.typeArguments;
    if (typeParameters.length == 0) {
      return mixins;
    }
    int count = mixins.length;
    List<InterfaceType> typedMixins = new List<InterfaceType>(count);
    for (int i = 0; i < count; i++) {
      typedMixins[i] = mixins[i].substitute2(typeArguments, parameterTypes);
    }
    return typedMixins;
  }

  @override
  PropertyAccessorElement getSetter(String setterName) => PropertyAccessorMember.from((element as ClassElementImpl).getSetter(setterName), this);

  @override
  InterfaceType get superclass {
    ClassElement classElement = element;
    InterfaceType supertype = classElement.supertype;
    if (supertype == null) {
      return null;
    }
    return supertype.substitute2(typeArguments, classElement.type.typeArguments);
  }

  @override
  List<TypeParameterElement> get typeParameters => element.typeParameters;

  @override
  int get hashCode {
    ClassElement element = this.element;
    if (element == null) {
      return 0;
    }
    return element.hashCode;
  }

  @override
  bool get isDartCoreFunction {
    ClassElement element = this.element;
    if (element == null) {
      return false;
    }
    return element.name == "Function" && element.library.isDartCore;
  }

  @override
  bool isDirectSupertypeOf(InterfaceType type) {
    InterfaceType i = this;
    InterfaceType j = type;
    ClassElement jElement = j.element;
    InterfaceType supertype = jElement.supertype;
    //
    // If J has no direct supertype then it is Object, and Object has no direct supertypes.
    //
    if (supertype == null) {
      return false;
    }
    //
    // I is listed in the extends clause of J.
    //
    List<DartType> jArgs = j.typeArguments;
    List<DartType> jVars = jElement.type.typeArguments;
    supertype = supertype.substitute2(jArgs, jVars);
    if (supertype == i) {
      return true;
    }
    //
    // I is listed in the implements clause of J.
    //
    for (InterfaceType interfaceType in jElement.interfaces) {
      interfaceType = interfaceType.substitute2(jArgs, jVars);
      if (interfaceType == i) {
        return true;
      }
    }
    //
    // I is listed in the with clause of J.
    //
    for (InterfaceType mixinType in jElement.mixins) {
      mixinType = mixinType.substitute2(jArgs, jVars);
      if (mixinType == i) {
        return true;
      }
    }
    //
    // J is a mixin application of the mixin of I.
    //
    // TODO(brianwilkerson) Determine whether this needs to be implemented or whether it is covered
    // by the case above.
    return false;
  }

  @override
  bool get isObject => element.supertype == null;

  @override
  ConstructorElement lookUpConstructor(String constructorName, LibraryElement library) {
    // prepare base ConstructorElement
    ConstructorElement constructorElement;
    if (constructorName == null) {
      constructorElement = element.unnamedConstructor;
    } else {
      constructorElement = element.getNamedConstructor(constructorName);
    }
    // not found or not accessible
    if (constructorElement == null || !constructorElement.isAccessibleIn(library)) {
      return null;
    }
    // return member
    return ConstructorMember.from(constructorElement, this);
  }

  @override
  PropertyAccessorElement lookUpGetter(String getterName, LibraryElement library) {
    PropertyAccessorElement element = getGetter(getterName);
    if (element != null && element.isAccessibleIn(library)) {
      return element;
    }
    return lookUpGetterInSuperclass(getterName, library);
  }

  @override
  PropertyAccessorElement lookUpGetterInSuperclass(String getterName, LibraryElement library) {
    for (InterfaceType mixin in mixins) {
      PropertyAccessorElement element = mixin.getGetter(getterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    Set<ClassElement> visitedClasses = new Set<ClassElement>();
    InterfaceType supertype = superclass;
    ClassElement supertypeElement = supertype == null ? null : supertype.element;
    while (supertype != null && !visitedClasses.contains(supertypeElement)) {
      visitedClasses.add(supertypeElement);
      PropertyAccessorElement element = supertype.getGetter(getterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
      for (InterfaceType mixin in supertype.mixins) {
        element = mixin.getGetter(getterName);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
      supertype = supertype.superclass;
      supertypeElement = supertype == null ? null : supertype.element;
    }
    return null;
  }

  @override
  MethodElement lookUpMethod(String methodName, LibraryElement library) {
    MethodElement element = getMethod(methodName);
    if (element != null && element.isAccessibleIn(library)) {
      return element;
    }
    return lookUpMethodInSuperclass(methodName, library);
  }

  @override
  MethodElement lookUpMethodInSuperclass(String methodName, LibraryElement library) {
    for (InterfaceType mixin in mixins) {
      MethodElement element = mixin.getMethod(methodName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    Set<ClassElement> visitedClasses = new Set<ClassElement>();
    InterfaceType supertype = superclass;
    ClassElement supertypeElement = supertype == null ? null : supertype.element;
    while (supertype != null && !visitedClasses.contains(supertypeElement)) {
      visitedClasses.add(supertypeElement);
      MethodElement element = supertype.getMethod(methodName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
      for (InterfaceType mixin in supertype.mixins) {
        element = mixin.getMethod(methodName);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
      supertype = supertype.superclass;
      supertypeElement = supertype == null ? null : supertype.element;
    }
    return null;
  }

  @override
  PropertyAccessorElement lookUpSetter(String setterName, LibraryElement library) {
    PropertyAccessorElement element = getSetter(setterName);
    if (element != null && element.isAccessibleIn(library)) {
      return element;
    }
    return lookUpSetterInSuperclass(setterName, library);
  }

  @override
  PropertyAccessorElement lookUpSetterInSuperclass(String setterName, LibraryElement library) {
    for (InterfaceType mixin in mixins) {
      PropertyAccessorElement element = mixin.getSetter(setterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    Set<ClassElement> visitedClasses = new Set<ClassElement>();
    InterfaceType supertype = superclass;
    ClassElement supertypeElement = supertype == null ? null : supertype.element;
    while (supertype != null && !visitedClasses.contains(supertypeElement)) {
      visitedClasses.add(supertypeElement);
      PropertyAccessorElement element = supertype.getSetter(setterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
      for (InterfaceType mixin in supertype.mixins) {
        element = mixin.getSetter(setterName);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
      supertype = supertype.superclass;
      supertypeElement = supertype == null ? null : supertype.element;
    }
    return null;
  }

  @override
  InterfaceTypeImpl substitute4(List<DartType> argumentTypes) => substitute2(argumentTypes, typeArguments);

  @override
  InterfaceTypeImpl substitute2(List<DartType> argumentTypes, List<DartType> parameterTypes) {
    if (argumentTypes.length != parameterTypes.length) {
      throw new IllegalArgumentException("argumentTypes.length (${argumentTypes.length}) != parameterTypes.length (${parameterTypes.length})");
    }
    if (argumentTypes.length == 0 || typeArguments.length == 0) {
      return this;
    }
    List<DartType> newTypeArguments = TypeImpl.substitute(typeArguments, argumentTypes, parameterTypes);
    if (JavaArrays.equals(newTypeArguments, typeArguments)) {
      return this;
    }
    InterfaceTypeImpl newType = new InterfaceTypeImpl.con1(element);
    newType.typeArguments = newTypeArguments;
    return newType;
  }

  @override
  void appendTo(JavaStringBuilder builder) {
    builder.append(name);
    int argumentCount = typeArguments.length;
    if (argumentCount > 0) {
      builder.append("<");
      for (int i = 0; i < argumentCount; i++) {
        if (i > 0) {
          builder.append(", ");
        }
        (typeArguments[i] as TypeImpl).appendTo(builder);
      }
      builder.append(">");
    }
  }

  @override
  bool internalEquals(Object object, Set<ElementPair> visitedElementPairs) {
    if (object is! InterfaceTypeImpl) {
      return false;
    }
    InterfaceTypeImpl otherType = object as InterfaceTypeImpl;
    return (element == otherType.element) && TypeImpl.equalArrays(typeArguments, otherType.typeArguments, visitedElementPairs);
  }

  @override
  bool internalIsMoreSpecificThan(DartType type, bool withDynamic, Set<TypeImpl_TypePair> visitedTypePairs) {
    //
    // S is dynamic.
    // The test to determine whether S is dynamic is done here because dynamic is not an instance of
    // InterfaceType.
    //
    if (identical(type, DynamicTypeImpl.instance)) {
      return true;
    } else if (type is! InterfaceType) {
      return false;
    }
    return _isMoreSpecificThan(type as InterfaceType, new Set<ClassElement>(), withDynamic, visitedTypePairs);
  }

  @override
  bool internalIsSubtypeOf(DartType type, Set<TypeImpl_TypePair> visitedTypePairs) {
    //
    // T is a subtype of S, written T <: S, iff [bottom/dynamic]T << S
    //
    if (identical(type, DynamicTypeImpl.instance)) {
      return true;
    } else if (type is TypeParameterType) {
      return true;
    } else if (type is FunctionType) {
      ClassElement element = this.element;
      MethodElement callMethod = element.lookUpMethod("call", element.library);
      if (callMethod != null) {
        return callMethod.type.isSubtypeOf(type);
      }
      return false;
    } else if (type is! InterfaceType) {
      return false;
    } else if (this == type) {
      return true;
    }
    return _isSubtypeOf(type as InterfaceType, new Set<ClassElement>(), visitedTypePairs);
  }

  bool _isMoreSpecificThan(InterfaceType s, Set<ClassElement> visitedClasses, bool withDynamic, Set<TypeImpl_TypePair> visitedTypePairs) {
    //
    // A type T is more specific than a type S, written T << S,  if one of the following conditions
    // is met:
    //
    // Reflexivity: T is S.
    //
    if (this == s) {
      return true;
    }
    //
    // T is bottom. (This case is handled by the class BottomTypeImpl.)
    //
    // Direct supertype: S is a direct supertype of T.
    //
    if (s.isDirectSupertypeOf(this)) {
      return true;
    }
    //
    // Covariance: T is of the form I<T1, ..., Tn> and S is of the form I<S1, ..., Sn> and Ti << Si, 1 <= i <= n.
    //
    ClassElement tElement = this.element;
    ClassElement sElement = s.element;
    if (tElement == sElement) {
      List<DartType> tArguments = typeArguments;
      List<DartType> sArguments = s.typeArguments;
      if (tArguments.length != sArguments.length) {
        return false;
      }
      for (int i = 0; i < tArguments.length; i++) {
        if (!(tArguments[i] as TypeImpl).isMoreSpecificThan2(sArguments[i], withDynamic, visitedTypePairs)) {
          return false;
        }
      }
      return true;
    }
    //
    // Transitivity: T << U and U << S.
    //
    // First check for infinite loops
    ClassElement element = this.element;
    if (element == null || visitedClasses.contains(element)) {
      return false;
    }
    visitedClasses.add(element);
    // Iterate over all of the types U that are more specific than T because they are direct
    // supertypes of T and return true if any of them are more specific than S.
    InterfaceType supertype = superclass;
    if (supertype != null && (supertype as InterfaceTypeImpl)._isMoreSpecificThan(s, visitedClasses, withDynamic, visitedTypePairs)) {
      return true;
    }
    for (InterfaceType interfaceType in interfaces) {
      if ((interfaceType as InterfaceTypeImpl)._isMoreSpecificThan(s, visitedClasses, withDynamic, visitedTypePairs)) {
        return true;
      }
    }
    for (InterfaceType mixinType in mixins) {
      if ((mixinType as InterfaceTypeImpl)._isMoreSpecificThan(s, visitedClasses, withDynamic, visitedTypePairs)) {
        return true;
      }
    }
    return false;
  }

  bool _isSubtypeOf(InterfaceType type, Set<ClassElement> visitedClasses, Set<TypeImpl_TypePair> visitedTypePairs) {
    InterfaceType typeT = this;
    InterfaceType typeS = type;
    ClassElement elementT = element;
    if (elementT == null || visitedClasses.contains(elementT)) {
      return false;
    }
    visitedClasses.add(elementT);
    if (typeT == typeS) {
      return true;
    } else if (elementT == typeS.element) {
      // For each of the type arguments return true if all type args from T is a subtype of all
      // types from S.
      List<DartType> typeTArgs = typeT.typeArguments;
      List<DartType> typeSArgs = typeS.typeArguments;
      if (typeTArgs.length != typeSArgs.length) {
        // This case covers the case where two objects are being compared that have a different
        // number of parameterized types.
        return false;
      }
      for (int i = 0; i < typeTArgs.length; i++) {
        // Recursively call isSubtypeOf the type arguments and return false if the T argument is not
        // a subtype of the S argument.
        if (!(typeTArgs[i] as TypeImpl).isSubtypeOf2(typeSArgs[i], visitedTypePairs)) {
          return false;
        }
      }
      return true;
    } else if (typeS.isDartCoreFunction && elementT.getMethod("call") != null) {
      return true;
    }
    InterfaceType supertype = superclass;
    // The type is Object, return false.
    if (supertype != null && (supertype as InterfaceTypeImpl)._isSubtypeOf(typeS, visitedClasses, visitedTypePairs)) {
      return true;
    }
    List<InterfaceType> interfaceTypes = interfaces;
    for (InterfaceType interfaceType in interfaceTypes) {
      if ((interfaceType as InterfaceTypeImpl)._isSubtypeOf(typeS, visitedClasses, visitedTypePairs)) {
        return true;
      }
    }
    List<InterfaceType> mixinTypes = mixins;
    for (InterfaceType mixinType in mixinTypes) {
      if ((mixinType as InterfaceTypeImpl)._isSubtypeOf(typeS, visitedClasses, visitedTypePairs)) {
        return true;
      }
    }
    return false;
  }
}

/**
 * The abstract class `TypeImpl` implements the behavior common to objects representing the
 * declared type of elements in the element model.
 */
abstract class TypeImpl implements DartType {
  static bool equalArrays(List<DartType> typeArgs1, List<DartType> typeArgs2, Set<ElementPair> visitedElementPairs) {
    if (typeArgs1.length != typeArgs2.length) {
      return false;
    }
    for (int i = 0; i < typeArgs1.length; i++) {
      if (!(typeArgs1[i] as TypeImpl).internalEquals(typeArgs2[i], visitedElementPairs)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return an array containing the results of using the given argument types and parameter types to
   * perform a substitution on all of the given types.
   *
   * @param types the types on which a substitution is to be performed
   * @param argumentTypes the argument types for the substitution
   * @param parameterTypes the parameter types for the substitution
   * @return the result of performing the substitution on each of the types
   */
  static List<DartType> substitute(List<DartType> types, List<DartType> argumentTypes, List<DartType> parameterTypes) {
    int length = types.length;
    if (length == 0) {
      return types;
    }
    List<DartType> newTypes = new List<DartType>(length);
    for (int i = 0; i < length; i++) {
      newTypes[i] = types[i].substitute2(argumentTypes, parameterTypes);
    }
    return newTypes;
  }

  /**
   * The element representing the declaration of this type, or `null` if the type has not, or
   * cannot, be associated with an element.
   */
  final Element _element;

  /**
   * The name of this type, or `null` if the type does not have a name.
   */
  final String name;

  /**
   * An empty array of types.
   */
  static List<DartType> EMPTY_ARRAY = new List<DartType>(0);

  /**
   * Initialize a newly created type to be declared by the given element and to have the given name.
   *
   * @param element the element representing the declaration of the type
   * @param name the name of the type
   */
  TypeImpl(this._element, this.name);

  @override
  String get displayName => name;

  @override
  Element get element => _element;

  @override
  DartType getLeastUpperBound(DartType type) => null;

  @override
  bool isAssignableTo(DartType type) => isAssignableTo2(type, new Set<TypeImpl_TypePair>());

  /**
   * Return `true` if this type is assignable to the given type. A type <i>T</i> may be
   * assigned to a type <i>S</i>, written <i>T</i> &hArr; <i>S</i>, iff either <i>T</i> <: <i>S</i>
   * or <i>S</i> <: <i>T</i>.
   *
   * The given set of pairs of types (T1, T2), where each pair indicates that we invoked this method
   * because we are in the process of answering the question of whether T1 is a subtype of T2, is
   * used to prevent infinite loops.
   *
   * @param type the type being compared with this type
   * @param visitedPairs the set of pairs of types used to prevent infinite loops
   * @return `true` if this type is assignable to the given type
   */
  bool isAssignableTo2(DartType type, Set<TypeImpl_TypePair> visitedTypePairs) => isSubtypeOf2(type, visitedTypePairs) || (type as TypeImpl).isSubtypeOf2(this, visitedTypePairs);

  @override
  bool get isBottom => false;

  @override
  bool get isDartCoreFunction => false;

  @override
  bool get isDynamic => false;

  @override
  bool isMoreSpecificThan(DartType type) => isMoreSpecificThan2(type, false, new Set<TypeImpl_TypePair>());

  /**
   * Return `true` if this type is more specific than the given type.
   *
   * The given set of pairs of types (T1, T2), where each pair indicates that we invoked this method
   * because we are in the process of answering the question of whether T1 is a subtype of T2, is
   * used to prevent infinite loops.
   *
   * @param type the type being compared with this type
   * @param withDynamic `true` if "dynamic" should be considered as a subtype of any type
   * @param visitedPairs the set of pairs of types used to prevent infinite loops
   * @return `true` if this type is more specific than the given type
   */
  bool isMoreSpecificThan2(DartType type, bool withDynamic, Set<TypeImpl_TypePair> visitedTypePairs) {
    // If the visitedTypePairs already has the pair (this, type), return false
    TypeImpl_TypePair typePair = new TypeImpl_TypePair(this, type);
    if (!visitedTypePairs.add(typePair)) {
      return false;
    }
    bool result = internalIsMoreSpecificThan(type, withDynamic, visitedTypePairs);
    visitedTypePairs.remove(typePair);
    return result;
  }

  @override
  bool get isObject => false;

  @override
  bool isSubtypeOf(DartType type) => isSubtypeOf2(type, new Set<TypeImpl_TypePair>());

  /**
   * Return `true` if this type is a subtype of the given type.
   *
   * The given set of pairs of types (T1, T2), where each pair indicates that we invoked this method
   * because we are in the process of answering the question of whether T1 is a subtype of T2, is
   * used to prevent infinite loops.
   *
   * @param type the type being compared with this type
   * @param visitedPairs the set of pairs of types used to prevent infinite loops
   * @return `true` if this type is a subtype of the given type
   */
  bool isSubtypeOf2(DartType type, Set<TypeImpl_TypePair> visitedTypePairs) {
    // If the visitedTypePairs already has the pair (this, type), return false
    TypeImpl_TypePair typePair = new TypeImpl_TypePair(this, type);
    if (!visitedTypePairs.add(typePair)) {
      return false;
    }
    bool result = internalIsSubtypeOf(type, visitedTypePairs);
    visitedTypePairs.remove(typePair);
    return result;
  }

  @override
  bool isSupertypeOf(DartType type) => type.isSubtypeOf(this);

  @override
  bool get isVoid => false;

  @override
  String toString() {
    JavaStringBuilder builder = new JavaStringBuilder();
    appendTo(builder);
    return builder.toString();
  }

  /**
   * Append a textual representation of this type to the given builder.
   *
   * @param builder the builder to which the text is to be appended
   */
  void appendTo(JavaStringBuilder builder) {
    if (name == null) {
      builder.append("<unnamed type>");
    } else {
      builder.append(name);
    }
  }

  bool internalEquals(Object object, Set<ElementPair> visitedElementPairs);

  bool internalIsMoreSpecificThan(DartType type, bool withDynamic, Set<TypeImpl_TypePair> visitedTypePairs);

  bool internalIsSubtypeOf(DartType type, Set<TypeImpl_TypePair> visitedTypePairs);
}

class TypeImpl_TypePair {
  final DartType _firstType;

  final DartType _secondType;

  int _cachedHashCode = 0;

  TypeImpl_TypePair(this._firstType, this._secondType);

  @override
  bool operator ==(Object object) {
    if (identical(object, this)) {
      return true;
    }
    if (object is TypeImpl_TypePair) {
      TypeImpl_TypePair typePair = object;
      return _firstType == typePair._firstType && _secondType != null && _secondType == typePair._secondType;
    }
    return false;
  }

  @override
  int get hashCode {
    if (_cachedHashCode == 0) {
      int firstHashCode = 0;
      if (_firstType != null) {
        Element firstElement = _firstType.element;
        firstHashCode = firstElement == null ? 0 : firstElement.hashCode;
      }
      int secondHashCode = 0;
      if (_secondType != null) {
        Element secondElement = _secondType.element;
        secondHashCode = secondElement == null ? 0 : secondElement.hashCode;
      }
      _cachedHashCode = firstHashCode + secondHashCode;
    }
    return _cachedHashCode;
  }
}

/**
 * Instances of the class `TypeParameterTypeImpl` defines the behavior of objects representing
 * the type introduced by a type parameter.
 */
class TypeParameterTypeImpl extends TypeImpl implements TypeParameterType {
  /**
   * An empty array of type parameter types.
   */
  static List<TypeParameterType> EMPTY_ARRAY = new List<TypeParameterType>(0);

  /**
   * Return an array containing the type parameter types defined by the given array of type
   * parameter elements.
   *
   * @param typeParameters the type parameter elements defining the type parameter types to be
   *          returned
   * @return the type parameter types defined by the type parameter elements
   */
  static List<TypeParameterType> getTypes(List<TypeParameterElement> typeParameters) {
    int count = typeParameters.length;
    if (count == 0) {
      return EMPTY_ARRAY;
    }
    List<TypeParameterType> types = new List<TypeParameterType>(count);
    for (int i = 0; i < count; i++) {
      types[i] = typeParameters[i].type;
    }
    return types;
  }

  /**
   * Initialize a newly created type parameter type to be declared by the given element and to have
   * the given name.
   *
   * @param element the element representing the declaration of the type parameter
   */
  TypeParameterTypeImpl(TypeParameterElement element) : super(element, element.name);

  @override
  bool operator ==(Object object) => object is TypeParameterTypeImpl && (element == object.element);

  @override
  TypeParameterElement get element => super.element as TypeParameterElement;

  @override
  int get hashCode => element.hashCode;

  @override
  DartType substitute2(List<DartType> argumentTypes, List<DartType> parameterTypes) {
    int length = parameterTypes.length;
    for (int i = 0; i < length; i++) {
      if (parameterTypes[i] == this) {
        return argumentTypes[i];
      }
    }
    return this;
  }

  @override
  bool internalEquals(Object object, Set<ElementPair> visitedElementPairs) => this == object;

  @override
  bool internalIsMoreSpecificThan(DartType s, bool withDynamic, Set<TypeImpl_TypePair> visitedTypePairs) {
    //
    // A type T is more specific than a type S, written T << S,  if one of the following conditions
    // is met:
    //
    // Reflexivity: T is S.
    //
    if (this == s) {
      return true;
    }
    // S is bottom.
    //
    if (s.isBottom) {
      return true;
    }
    // S is dynamic.
    //
    if (s.isDynamic) {
      return true;
    }
    return _isMoreSpecificThan(s, new Set<DartType>(), withDynamic, visitedTypePairs);
  }

  @override
  bool internalIsSubtypeOf(DartType type, Set<TypeImpl_TypePair> visitedTypePairs) => isMoreSpecificThan2(type, true, new Set<TypeImpl_TypePair>());

  bool _isMoreSpecificThan(DartType s, Set<DartType> visitedTypes, bool withDynamic, Set<TypeImpl_TypePair> visitedTypePairs) {
    // T is a type parameter and S is the upper bound of T.
    //
    DartType bound = element.bound;
    if (s == bound) {
      return true;
    }
    // T is a type parameter and S is Object.
    //
    if (s.isObject) {
      return true;
    }
    // We need upper bound to continue.
    if (bound == null) {
      return false;
    }
    //
    // Transitivity: T << U and U << S.
    //
    if (bound is TypeParameterTypeImpl) {
      TypeParameterTypeImpl boundTypeParameter = bound;
      // First check for infinite loops
      if (visitedTypes.contains(bound)) {
        return false;
      }
      visitedTypes.add(bound);
      // Then check upper bound.
      return boundTypeParameter._isMoreSpecificThan(s, visitedTypes, withDynamic, visitedTypePairs);
    }
    // Check interface type.
    return (bound as TypeImpl).isMoreSpecificThan2(s, withDynamic, visitedTypePairs);
  }
}

/**
 * The unique instance of the class `VoidTypeImpl` implements the type `void`.
 */
class VoidTypeImpl extends TypeImpl implements VoidType {
  /**
   * The unique instance of this class.
   */
  static VoidTypeImpl _INSTANCE = new VoidTypeImpl();

  /**
   * Return the unique instance of this class.
   *
   * @return the unique instance of this class
   */
  static VoidTypeImpl get instance => _INSTANCE;

  /**
   * Prevent the creation of instances of this class.
   */
  VoidTypeImpl() : super(null, Keyword.VOID.syntax);

  @override
  bool operator ==(Object object) => identical(object, this);

  @override
  int get hashCode => 2;

  @override
  bool get isVoid => true;

  @override
  VoidTypeImpl substitute2(List<DartType> argumentTypes, List<DartType> parameterTypes) => this;

  @override
  bool internalEquals(Object object, Set<ElementPair> visitedElementPairs) => identical(object, this);

  @override
  bool internalIsMoreSpecificThan(DartType type, bool withDynamic, Set<TypeImpl_TypePair> visitedTypePairs) => isSubtypeOf(type);

  @override
  bool internalIsSubtypeOf(DartType type, Set<TypeImpl_TypePair> visitedTypePairs) => identical(type, this) || identical(type, DynamicTypeImpl.instance);
}

/**
 * The interface `FunctionType` defines the behavior common to objects representing the type
 * of a function, method, constructor, getter, or setter. Function types come in three variations:
 * <ol>
 * * The types of functions that only have required parameters. These have the general form
 * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>) &rarr; T</i>.
 * * The types of functions with optional positional parameters. These have the general form
 * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, [T<sub>n+1</sub>, &hellip;, T<sub>n+k</sub>]) &rarr;
 * T</i>.
 * * The types of functions with named parameters. These have the general form <i>(T<sub>1</sub>,
 * &hellip;, T<sub>n</sub>, {T<sub>x1</sub> x1, &hellip;, T<sub>xk</sub> xk}) &rarr; T</i>.
 * </ol>
 */
abstract class FunctionType implements ParameterizedType {
  /**
   * Return a map from the names of named parameters to the types of the named parameters of this
   * type of function. The entries in the map will be iterated in the same order as the order in
   * which the named parameters were defined. If there were no named parameters declared then the
   * map will be empty.
   *
   * @return a map from the name to the types of the named parameters of this type of function
   */
  Map<String, DartType> get namedParameterTypes;

  /**
   * Return an array containing the types of the normal parameters of this type of function. The
   * parameter types are in the same order as they appear in the declaration of the function.
   *
   * @return the types of the normal parameters of this type of function
   */
  List<DartType> get normalParameterTypes;

  /**
   * Return a map from the names of optional (positional) parameters to the types of the optional
   * parameters of this type of function. The entries in the map will be iterated in the same order
   * as the order in which the optional parameters were defined. If there were no optional
   * parameters declared then the map will be empty.
   *
   * @return a map from the name to the types of the optional parameters of this type of function
   */
  List<DartType> get optionalParameterTypes;

  /**
   * Return an array containing the parameters elements of this type of function. The parameter
   * types are in the same order as they appear in the declaration of the function.
   *
   * @return the parameters elements of this type of function
   */
  List<ParameterElement> get parameters;

  /**
   * Return the type of object returned by this type of function.
   *
   * @return the type of object returned by this type of function
   */
  DartType get returnType;

  /**
   * Return `true` if this type is a subtype of the given type.
   *
   * A function type <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>) &rarr; T</i> is a subtype of the
   * function type <i>(S<sub>1</sub>, &hellip;, S<sub>n</sub>) &rarr; S</i>, if all of the following
   * conditions are met:
   * * Either
   * * <i>S</i> is void, or
   * * <i>T &hArr; S</i>.
   *
   * * For all <i>i</i>, 1 <= <i>i</i> <= <i>n</i>, <i>T<sub>i</sub> &hArr; S<sub>i</sub></i>.
   * A function type <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, [T<sub>n+1</sub>, &hellip;,
   * T<sub>n+k</sub>]) &rarr; T</i> is a subtype of the function type <i>(S<sub>1</sub>, &hellip;,
   * S<sub>n</sub>, [S<sub>n+1</sub>, &hellip;, S<sub>n+m</sub>]) &rarr; S</i>, if all of the
   * following conditions are met:
   * * Either
   * * <i>S</i> is void, or
   * * <i>T &hArr; S</i>.
   *
   * * <i>k</i> >= <i>m</i> and for all <i>i</i>, 1 <= <i>i</i> <= <i>n+m</i>, <i>T<sub>i</sub>
   * &hArr; S<sub>i</sub></i>.
   * A function type <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, {T<sub>x1</sub> x1, &hellip;,
   * T<sub>xk</sub> xk}) &rarr; T</i> is a subtype of the function type <i>(S<sub>1</sub>, &hellip;,
   * S<sub>n</sub>, {S<sub>y1</sub> y1, &hellip;, S<sub>ym</sub> ym}) &rarr; S</i>, if all of the
   * following conditions are met:
   * * Either
   * * <i>S</i> is void,
   * * or <i>T &hArr; S</i>.
   *
   * * For all <i>i</i>, 1 <= <i>i</i> <= <i>n</i>, <i>T<sub>i</sub> &hArr; S<sub>i</sub></i>.
   * * <i>k</i> >= <i>m</i> and <i>y<sub>i</sub></i> in <i>{x<sub>1</sub>, &hellip;,
   * x<sub>k</sub>}</i>, 1 <= <i>i</i> <= <i>m</i>.
   * * For all <i>y<sub>i</sub></i> in <i>{y<sub>1</sub>, &hellip;, y<sub>m</sub>}</i>,
   * <i>y<sub>i</sub> = x<sub>j</sub> => Tj &hArr; Si</i>.
   * In addition, the following subtype rules apply:
   *
   * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, []) &rarr; T <: (T<sub>1</sub>, &hellip;,
   * T<sub>n</sub>) &rarr; T.</i><br>
   * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>) &rarr; T <: (T<sub>1</sub>, &hellip;,
   * T<sub>n</sub>, {}) &rarr; T.</i><br>
   * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>, {}) &rarr; T <: (T<sub>1</sub>, &hellip;,
   * T<sub>n</sub>) &rarr; T.</i><br>
   * <i>(T<sub>1</sub>, &hellip;, T<sub>n</sub>) &rarr; T <: (T<sub>1</sub>, &hellip;,
   * T<sub>n</sub>, []) &rarr; T.</i>
   *
   * All functions implement the class `Function`. However not all function types are a
   * subtype of `Function`. If an interface type <i>I</i> includes a method named
   * `call()`, and the type of `call()` is the function type <i>F</i>, then <i>I</i> is
   * considered to be a subtype of <i>F</i>.
   *
   * @param type the type being compared with this type
   * @return `true` if this type is a subtype of the given type
   */
  @override
  bool isSubtypeOf(DartType type);

  /**
   * Return the type resulting from substituting the given arguments for this type's parameters.
   * This is fully equivalent to `substitute(argumentTypes, getTypeArguments())`.
   *
   * @param argumentTypes the actual type arguments being substituted for the type parameters
   * @return the result of performing the substitution
   */
  FunctionType substitute3(List<DartType> argumentTypes);

  @override
  FunctionType substitute2(List<DartType> argumentTypes, List<DartType> parameterTypes);
}

/**
 * The interface `InterfaceType` defines the behavior common to objects representing the type
 * introduced by either a class or an interface, or a reference to such a type.
 */
abstract class InterfaceType implements ParameterizedType {
  /**
   * Return an array containing all of the accessors (getters and setters) declared in this type.
   *
   * @return the accessors declared in this type
   */
  List<PropertyAccessorElement> get accessors;

  @override
  ClassElement get element;

  /**
   * Return the element representing the getter with the given name that is declared in this class,
   * or `null` if this class does not declare a getter with the given name.
   *
   * @param getterName the name of the getter to be returned
   * @return the getter declared in this class with the given name
   */
  PropertyAccessorElement getGetter(String getterName);

  /**
   * Return an array containing all of the interfaces that are implemented by this interface. Note
   * that this is <b>not</b>, in general, equivalent to getting the interfaces from this type's
   * element because the types returned by this method will have had their type parameters replaced.
   *
   * @return the interfaces that are implemented by this type
   */
  List<InterfaceType> get interfaces;

  /**
   * Return the least upper bound of this type and the given type, or `null` if there is no
   * least upper bound.
   *
   * Given two interfaces <i>I</i> and <i>J</i>, let <i>S<sub>I</sub></i> be the set of
   * superinterfaces of <i>I<i>, let <i>S<sub>J</sub></i> be the set of superinterfaces of <i>J</i>
   * and let <i>S = (I &cup; S<sub>I</sub>) &cap; (J &cup; S<sub>J</sub>)</i>. Furthermore, we
   * define <i>S<sub>n</sub> = {T | T &isin; S &and; depth(T) = n}</i> for any finite <i>n</i>,
   * where <i>depth(T)</i> is the number of steps in the longest inheritance path from <i>T</i> to
   * <i>Object</i>. Let <i>q</i> be the largest number such that <i>S<sub>q</sub></i> has
   * cardinality one. The least upper bound of <i>I</i> and <i>J</i> is the sole element of
   * <i>S<sub>q</sub></i>.
   *
   * @param type the other type used to compute the least upper bound
   * @return the least upper bound of this type and the given type
   */
  @override
  DartType getLeastUpperBound(DartType type);

  /**
   * Return the element representing the method with the given name that is declared in this class,
   * or `null` if this class does not declare a method with the given name.
   *
   * @param methodName the name of the method to be returned
   * @return the method declared in this class with the given name
   */
  MethodElement getMethod(String methodName);

  /**
   * Return an array containing all of the methods declared in this type.
   *
   * @return the methods declared in this type
   */
  List<MethodElement> get methods;

  /**
   * Return an array containing all of the mixins that are applied to the class being extended in
   * order to derive the superclass of this class. Note that this is <b>not</b>, in general,
   * equivalent to getting the mixins from this type's element because the types returned by this
   * method will have had their type parameters replaced.
   *
   * @return the mixins that are applied to derive the superclass of this class
   */
  List<InterfaceType> get mixins;

  /**
   * Return the element representing the setter with the given name that is declared in this class,
   * or `null` if this class does not declare a setter with the given name.
   *
   * @param setterName the name of the setter to be returned
   * @return the setter declared in this class with the given name
   */
  PropertyAccessorElement getSetter(String setterName);

  /**
   * Return the type representing the superclass of this type, or null if this type represents the
   * class 'Object'. Note that this is <b>not</b>, in general, equivalent to getting the superclass
   * from this type's element because the type returned by this method will have had it's type
   * parameters replaced.
   *
   * @return the superclass of this type
   */
  InterfaceType get superclass;

  /**
   * Return `true` if this type is a direct supertype of the given type. The implicit
   * interface of class <i>I</i> is a direct supertype of the implicit interface of class <i>J</i>
   * iff:
   * * <i>I</i> is Object, and <i>J</i> has no extends clause.
   * * <i>I</i> is listed in the extends clause of <i>J</i>.
   * * <i>I</i> is listed in the implements clause of <i>J</i>.
   * * <i>I</i> is listed in the with clause of <i>J</i>.
   * * <i>J</i> is a mixin application of the mixin of <i>I</i>.
   *
   * @param type the type being compared with this type
   * @return `true` if this type is a direct supertype of the given type
   */
  bool isDirectSupertypeOf(InterfaceType type);

  /**
   * Return `true` if this type is more specific than the given type. An interface type
   * <i>T</i> is more specific than an interface type <i>S</i>, written <i>T &laquo; S</i>, if one
   * of the following conditions is met:
   * * Reflexivity: <i>T</i> is <i>S</i>.
   * * <i>T</i> is bottom.
   * * <i>S</i> is dynamic.
   * * Direct supertype: <i>S</i> is a direct supertype of <i>T</i>.
   * * <i>T</i> is a type parameter and <i>S</i> is the upper bound of <i>T</i>.
   * * Covariance: <i>T</i> is of the form <i>I&lt;T<sub>1</sub>, &hellip;, T<sub>n</sub>&gt;</i>
   * and S</i> is of the form <i>I&lt;S<sub>1</sub>, &hellip;, S<sub>n</sub>&gt;</i> and
   * <i>T<sub>i</sub> &laquo; S<sub>i</sub></i>, <i>1 <= i <= n</i>.
   * * Transitivity: <i>T &laquo; U</i> and <i>U &laquo; S</i>.
   *
   * @param type the type being compared with this type
   * @return `true` if this type is more specific than the given type
   */
  @override
  bool isMoreSpecificThan(DartType type);

  /**
   * Return `true` if this type is a subtype of the given type. An interface type <i>T</i> is
   * a subtype of an interface type <i>S</i>, written <i>T</i> <: <i>S</i>, iff
   * <i>[bottom/dynamic]T</i> &laquo; <i>S</i> (<i>T</i> is more specific than <i>S</i>). If an
   * interface type <i>I</i> includes a method named <i>call()</i>, and the type of <i>call()</i> is
   * the function type <i>F</i>, then <i>I</i> is considered to be a subtype of <i>F</i>.
   *
   * @param type the type being compared with this type
   * @return `true` if this type is a subtype of the given type
   */
  @override
  bool isSubtypeOf(DartType type);

  /**
   * Return the element representing the constructor that results from looking up the given
   * constructor in this class with respect to the given library, or `null` if the look up
   * fails. The behavior of this method is defined by the Dart Language Specification in section
   * 12.11.1: <blockquote>If <i>e</i> is of the form <b>new</b> <i>T.id()</i> then let <i>q<i> be
   * the constructor <i>T.id</i>, otherwise let <i>q<i> be the constructor <i>T<i>. Otherwise, if
   * <i>q</i> is not defined or not accessible, a NoSuchMethodException is thrown. </blockquote>
   *
   * @param constructorName the name of the constructor being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given constructor in this class with respect to the given
   *         library
   */
  ConstructorElement lookUpConstructor(String constructorName, LibraryElement library);

  /**
   * Return the element representing the getter that results from looking up the given getter in
   * this class with respect to the given library, or `null` if the look up fails. The
   * behavior of this method is defined by the Dart Language Specification in section 12.15.1:
   * <blockquote>The result of looking up getter (respectively setter) <i>m</i> in class <i>C</i>
   * with respect to library <i>L</i> is:
   * * If <i>C</i> declares an instance getter (respectively setter) named <i>m</i> that is
   * accessible to <i>L</i>, then that getter (respectively setter) is the result of the lookup.
   * Otherwise, if <i>C</i> has a superclass <i>S</i>, then the result of the lookup is the result
   * of looking up getter (respectively setter) <i>m</i> in <i>S</i> with respect to <i>L</i>.
   * Otherwise, we say that the lookup has failed.
   * </blockquote>
   *
   * @param getterName the name of the getter being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given getter in this class with respect to the given
   *         library
   */
  PropertyAccessorElement lookUpGetter(String getterName, LibraryElement library);

  /**
   * Return the element representing the getter that results from looking up the given getter in the
   * superclass of this class with respect to the given library, or `null` if the look up
   * fails. The behavior of this method is defined by the Dart Language Specification in section
   * 12.15.1: <blockquote>The result of looking up getter (respectively setter) <i>m</i> in class
   * <i>C</i> with respect to library <i>L</i> is:
   * * If <i>C</i> declares an instance getter (respectively setter) named <i>m</i> that is
   * accessible to <i>L</i>, then that getter (respectively setter) is the result of the lookup.
   * Otherwise, if <i>C</i> has a superclass <i>S</i>, then the result of the lookup is the result
   * of looking up getter (respectively setter) <i>m</i> in <i>S</i> with respect to <i>L</i>.
   * Otherwise, we say that the lookup has failed.
   * </blockquote>
   *
   * @param getterName the name of the getter being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given getter in this class with respect to the given
   *         library
   */
  PropertyAccessorElement lookUpGetterInSuperclass(String getterName, LibraryElement library);

  /**
   * Return the element representing the method that results from looking up the given method in
   * this class with respect to the given library, or `null` if the look up fails. The
   * behavior of this method is defined by the Dart Language Specification in section 12.15.1:
   * <blockquote> The result of looking up method <i>m</i> in class <i>C</i> with respect to library
   * <i>L</i> is:
   * * If <i>C</i> declares an instance method named <i>m</i> that is accessible to <i>L</i>, then
   * that method is the result of the lookup. Otherwise, if <i>C</i> has a superclass <i>S</i>, then
   * the result of the lookup is the result of looking up method <i>m</i> in <i>S</i> with respect
   * to <i>L</i>. Otherwise, we say that the lookup has failed.
   * </blockquote>
   *
   * @param methodName the name of the method being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given method in this class with respect to the given
   *         library
   */
  MethodElement lookUpMethod(String methodName, LibraryElement library);

  /**
   * Return the element representing the method that results from looking up the given method in the
   * superclass of this class with respect to the given library, or `null` if the look up
   * fails. The behavior of this method is defined by the Dart Language Specification in section
   * 12.15.1: <blockquote> The result of looking up method <i>m</i> in class <i>C</i> with respect
   * to library <i>L</i> is:
   * * If <i>C</i> declares an instance method named <i>m</i> that is accessible to <i>L</i>, then
   * that method is the result of the lookup. Otherwise, if <i>C</i> has a superclass <i>S</i>, then
   * the result of the lookup is the result of looking up method <i>m</i> in <i>S</i> with respect
   * to <i>L</i>. Otherwise, we say that the lookup has failed.
   * </blockquote>
   *
   * @param methodName the name of the method being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given method in this class with respect to the given
   *         library
   */
  MethodElement lookUpMethodInSuperclass(String methodName, LibraryElement library);

  /**
   * Return the element representing the setter that results from looking up the given setter in
   * this class with respect to the given library, or `null` if the look up fails. The
   * behavior of this method is defined by the Dart Language Specification in section 12.16:
   * <blockquote> The result of looking up getter (respectively setter) <i>m</i> in class <i>C</i>
   * with respect to library <i>L</i> is:
   * * If <i>C</i> declares an instance getter (respectively setter) named <i>m</i> that is
   * accessible to <i>L</i>, then that getter (respectively setter) is the result of the lookup.
   * Otherwise, if <i>C</i> has a superclass <i>S</i>, then the result of the lookup is the result
   * of looking up getter (respectively setter) <i>m</i> in <i>S</i> with respect to <i>L</i>.
   * Otherwise, we say that the lookup has failed.
   * </blockquote>
   *
   * @param setterName the name of the setter being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given setter in this class with respect to the given
   *         library
   */
  PropertyAccessorElement lookUpSetter(String setterName, LibraryElement library);

  /**
   * Return the element representing the setter that results from looking up the given setter in the
   * superclass of this class with respect to the given library, or `null` if the look up
   * fails. The behavior of this method is defined by the Dart Language Specification in section
   * 12.16: <blockquote> The result of looking up getter (respectively setter) <i>m</i> in class
   * <i>C</i> with respect to library <i>L</i> is:
   * * If <i>C</i> declares an instance getter (respectively setter) named <i>m</i> that is
   * accessible to <i>L</i>, then that getter (respectively setter) is the result of the lookup.
   * Otherwise, if <i>C</i> has a superclass <i>S</i>, then the result of the lookup is the result
   * of looking up getter (respectively setter) <i>m</i> in <i>S</i> with respect to <i>L</i>.
   * Otherwise, we say that the lookup has failed.
   * </blockquote>
   *
   * @param setterName the name of the setter being looked up
   * @param library the library with respect to which the lookup is being performed
   * @return the result of looking up the given setter in this class with respect to the given
   *         library
   */
  PropertyAccessorElement lookUpSetterInSuperclass(String setterName, LibraryElement library);

  /**
   * Return the type resulting from substituting the given arguments for this type's parameters.
   * This is fully equivalent to `substitute(argumentTypes, getTypeArguments())`.
   *
   * @param argumentTypes the actual type arguments being substituted for the type parameters
   * @return the result of performing the substitution
   */
  InterfaceType substitute4(List<DartType> argumentTypes);

  @override
  InterfaceType substitute2(List<DartType> argumentTypes, List<DartType> parameterTypes);
}

/**
 * The interface `ParameterizedType` defines the behavior common to objects representing a
 * type with type parameters, such as a class or function type alias.
 */
abstract class ParameterizedType implements DartType {
  /**
   * Return an array containing the actual types of the type arguments. If this type's element does
   * not have type parameters, then the array should be empty (although it is possible for type
   * arguments to be erroneously declared). If the element has type parameters and the actual type
   * does not explicitly include argument values, then the type "dynamic" will be automatically
   * provided.
   *
   * @return the actual types of the type arguments
   */
  List<DartType> get typeArguments;

  /**
   * Return an array containing all of the type parameters declared for this type.
   *
   * @return the type parameters declared for this type
   */
  List<TypeParameterElement> get typeParameters;
}

/**
 * The interface `Type` defines the behavior of objects representing the declared type of
 * elements in the element model.
 */
abstract class DartType {
  /**
   * Return the name of this type as it should appear when presented to users in contexts such as
   * error messages.
   *
   * @return the name of this type
   */
  String get displayName;

  /**
   * Return the element representing the declaration of this type, or `null` if the type has
   * not, or cannot, be associated with an element. The former case will occur if the element model
   * is not yet complete; the latter case will occur if this object represents an undefined type.
   *
   * @return the element representing the declaration of this type
   */
  Element get element;

  /**
   * Return the least upper bound of this type and the given type, or `null` if there is no
   * least upper bound.
   *
   * @param type the other type used to compute the least upper bound
   * @return the least upper bound of this type and the given type
   */
  DartType getLeastUpperBound(DartType type);

  /**
   * Return the name of this type, or `null` if the type does not have a name, such as when
   * the type represents the type of an unnamed function.
   *
   * @return the name of this type
   */
  String get name;

  /**
   * Return `true` if this type is assignable to the given type. A type <i>T</i> may be
   * assigned to a type <i>S</i>, written <i>T</i> &hArr; <i>S</i>, iff either <i>T</i> <: <i>S</i>
   * or <i>S</i> <: <i>T</i>.
   *
   * @param type the type being compared with this type
   * @return `true` if this type is assignable to the given type
   */
  bool isAssignableTo(DartType type);

  /**
   * Return `true` if this type represents the bottom type.
   *
   * @return `true` if this type represents the bottom type
   */
  bool get isBottom;

  /**
   * Return `true` if this type represents the type 'Function' defined in the dart:core
   * library.
   *
   * @return `true` if this type represents the type 'Function' defined in the dart:core
   *         library
   */
  bool get isDartCoreFunction;

  /**
   * Return `true` if this type represents the type 'dynamic'.
   *
   * @return `true` if this type represents the type 'dynamic'
   */
  bool get isDynamic;

  /**
   * Return `true` if this type is more specific than the given type.
   *
   * @param type the type being compared with this type
   * @return `true` if this type is more specific than the given type
   */
  bool isMoreSpecificThan(DartType type);

  /**
   * Return `true` if this type represents the type 'Object'.
   *
   * @return `true` if this type represents the type 'Object'
   */
  bool get isObject;

  /**
   * Return `true` if this type is a subtype of the given type.
   *
   * @param type the type being compared with this type
   * @return `true` if this type is a subtype of the given type
   */
  bool isSubtypeOf(DartType type);

  /**
   * Return `true` if this type is a supertype of the given type. A type <i>S</i> is a
   * supertype of <i>T</i>, written <i>S</i> :> <i>T</i>, iff <i>T</i> is a subtype of <i>S</i>.
   *
   * @param type the type being compared with this type
   * @return `true` if this type is a supertype of the given type
   */
  bool isSupertypeOf(DartType type);

  /**
   * Return `true` if this type represents the type 'void'.
   *
   * @return `true` if this type represents the type 'void'
   */
  bool get isVoid;

  /**
   * Return the type resulting from substituting the given arguments for the given parameters in
   * this type. The specification defines this operation in section 2: <blockquote> The notation
   * <i>[x<sub>1</sub>, ..., x<sub>n</sub>/y<sub>1</sub>, ..., y<sub>n</sub>]E</i> denotes a copy of
   * <i>E</i> in which all occurrences of <i>y<sub>i</sub>, 1 <= i <= n</i> have been replaced with
   * <i>x<sub>i</sub></i>.</blockquote> Note that, contrary to the specification, this method will
   * not create a copy of this type if no substitutions were required, but will return this type
   * directly.
   *
   * @param argumentTypes the actual type arguments being substituted for the parameters
   * @param parameterTypes the parameters to be replaced
   * @return the result of performing the substitution
   */
  DartType substitute2(List<DartType> argumentTypes, List<DartType> parameterTypes);
}

/**
 * The interface `TypeParameterType` defines the behavior of objects representing the type
 * introduced by a type parameter.
 */
abstract class TypeParameterType implements DartType {
  @override
  TypeParameterElement get element;
}

/**
 * The interface `VoidType` defines the behavior of the unique object representing the type
 * `void`.
 */
abstract class VoidType implements DartType {
  @override
  VoidType substitute2(List<DartType> argumentTypes, List<DartType> parameterTypes);
}