/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.error;

/**
 * The enumeration {@code StaticWarningCode} defines the error codes used for static warnings. The
 * convention for this class is for the name of the error code to indicate the problem that caused
 * the error to be generated and for the error message to explain what is wrong and, when
 * appropriate, how the problem can be corrected.
 * 
 * @coverage dart.engine.error
 */
public enum StaticWarningCode implements ErrorCode {
  /**
   * 12.11.1 New: It is a static warning if the static type of <i>a<sub>i</sub>, 1 &lt;= i &lt;= n+
   * k</i> may not be assigned to the type of the corresponding formal parameter of the constructor
   * <i>T.id</i> (respectively <i>T</i>).
   * <p>
   * 12.11.2 Const: It is a static warning if the static type of <i>a<sub>i</sub>, 1 &lt;= i &lt;=
   * n+ k</i> may not be assigned to the type of the corresponding formal parameter of the
   * constructor <i>T.id</i> (respectively <i>T</i>).
   * <p>
   * 12.14.2 Binding Actuals to Formals: Let <i>T<sub>i</sub></i> be the static type of
   * <i>a<sub>i</sub></i>, let <i>S<sub>i</sub></i> be the type of <i>p<sub>i</sub>, 1 &lt;= i &lt;=
   * n+k</i> and let <i>S<sub>q</sub></i> be the type of the named parameter <i>q</i> of <i>f</i>.
   * It is a static warning if <i>T<sub>j</sub></i> may not be assigned to <i>S<sub>j</sub>, 1 &lt;=
   * j &lt;= m</i>.
   * <p>
   * 12.14.2 Binding Actuals to Formals: Furthermore, each <i>q<sub>i</sub>, 1 &lt;= i &lt;= l</i>,
   * must have a corresponding named parameter in the set <i>{p<sub>n+1</sub>, &hellip;
   * p<sub>n+k</sub>}</i> or a static warning occurs. It is a static warning if
   * <i>T<sub>m+j</sub></i> may not be assigned to <i>S<sub>r</sub></i>, where <i>r = q<sub>j</sub>,
   * 1 &lt;= j &lt;= l</i>.
   */
  ARGUMENT_TYPE_NOT_ASSIGNABLE(""),

  /**
   * 5 Variables: Attempting to assign to a final variable elsewhere will cause a NoSuchMethodError
   * to be thrown, because no setter is defined for it. The assignment will also give rise to a
   * static warning for the same reason.
   */
  ASSIGNMENT_TO_FINAL(""),

  /**
   * 13.9 Switch: It is a static warning if the last statement of the statement sequence
   * <i>s<sub>k</sub></i> is not a break, continue, return or throw statement.
   */
  CASE_BLOCK_NOT_TERMINATED(""),

  /**
   * 12.32 Type Cast: It is a static warning if <i>T</i> does not denote a type available in the
   * current lexical scope.
   */
  CAST_TO_NON_TYPE(""),

  /**
   * 16.1.2 Comments: A token of the form <i>[new c](uri)</i> will be replaced by a link in the
   * formatted output. The link will point at the constructor named <i>c</i> in <i>L</i>. The title
   * of the link will be <i>c</i>. It is a static warning if uri is not the URI of a dart library
   * <i>L</i>, or if <i>c</i> is not the name of a constructor of a class declared in the exported
   * namespace of <i>L</i>.
   */
  COMMENT_REFERENCE_CONSTRUCTOR_NOT_VISIBLE(""),

  /**
   * 16.1.2 Comments: A token of the form <i>[id](uri)</i> will be replaced by a link in the
   * formatted output. The link will point at the declaration named <i>id</i> in <i>L</i>. The title
   * of the link will be <i>id</i>. It is a static warning if uri is not the URI of a dart library
   * <i>L</i>, or if <i>id</i> is not a name declared in the exported namespace of <i>L</i>.
   */
  COMMENT_REFERENCE_IDENTIFIER_NOT_VISIBLE(""),

  /**
   * 16.1.2 Comments: It is a static warning if <i>c</i> does not denote a constructor that
   * available in the scope of the documentation comment.
   */
  COMMENT_REFERENCE_UNDECLARED_CONSTRUCTOR(""),

  /**
   * 16.1.2 Comments: It is a static warning if <i>id</i> does not denote a declaration that
   * available in the scope of the documentation comment.
   */
  COMMENT_REFERENCE_UNDECLARED_IDENTIFIER(""),

  /**
   * 16.1.2 Comments: A token of the form <i>[id](uri)</i> will be replaced by a link in the
   * formatted output. The link will point at the declaration named <i>id</i> in <i>L</i>. The title
   * of the link will be <i>id</i>. It is a static warning if uri is not the URI of a dart library
   * <i>L</i>, or if <i>id</i> is not a name declared in the exported namespace of <i>L</i>.
   */
  COMMENT_REFERENCE_URI_NOT_LIBRARY(""),

  /**
   * 7.4 Abstract Instance Members: It is a static warning if an abstract member is declared or
   * inherited in a concrete class.
   */
  CONCRETE_CLASS_WITH_ABSTRACT_MEMBER(""),

  /**
   * 7.2 Getters: It is a static warning if a class <i>C</i> declares an instance getter named
   * <i>v</i> and an accessible static member named <i>v</i> or <i>v=</i> is declared in a
   * superclass of <i>C</i>.
   */
  CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER(""),

  /**
   * 7.3 Setters: It is a static warning if a class <i>C</i> declares an instance setter named
   * <i>v=</i> and an accessible static member named <i>v=</i> or <i>v</i> is declared in a
   * superclass of <i>C</i>.
   */
  CONFLICTING_INSTANCE_SETTER_AND_SUPERCLASS_MEMBER(""),

  /**
   * 7.2 Getters: It is a static warning if a class declares a static getter named <i>v</i> and also
   * has a non-static setter named <i>v=</i>.
   */
  CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER(""),

  /**
   * 7.3 Setters: It is a static warning if a class declares a static setter named <i>v=</i> and
   * also has a non-static member named <i>v</i>.
   */
  CONFLICTING_STATIC_SETTER_AND_INSTANCE_GETTER(""),

  /**
   * 12.11.2 Const: Given an instance creation expression of the form <i>const q(a<sub>1</sub>,
   * &hellip; a<sub>n</sub>)</i> it is a static warning if <i>q</i> is the constructor of an
   * abstract class but <i>q</i> is not a factory constructor.
   */
  CONST_WITH_ABSTRACT_CLASS("Abstract classes cannot be created with a 'const' expression"),

  /**
   * 12.7 Maps: It is a static warning if the values of any two keys in a map literal are equal.
   */
  EQUAL_KEYS_IN_MAP("Keys in a map cannot be equal"),

  /**
   * 7.6.1 Generative Constructors: An initializing formal has the form <i>this.id</i>. It is a
   * static warning if the static type of <i>id</i> is not assignable to <i>T<sub>id</sub></i>.
   */
  FIELD_INITIALIZER_WITH_INVALID_TYPE(""),

  /**
   * 12.14.2 Binding Actuals to Formals: It is a static warning if <i>m &lt; h</i> or if <i>m &gt;
   * n</i>.
   */
  INCORRECT_NUMBER_OF_ARGUMENTS(""),

  /**
   * 7.1 Instance Methods: It is a static warning if a class <i>C</i> declares an instance method
   * named <i>n</i> and an accessible static member named <i>n</i> is declared in a superclass of
   * <i>C</i>.
   */
  INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC(""),

  /**
   * 7.6.2 Factories: It is a static warning if <i>M.id</i> is not a constructor name.
   */
  INVALID_FACTORY_NAME(""),

  /**
   * 7.2 Getters: It is a static warning if a getter <i>m1</i> overrides a getter <i>m2</i> and the
   * type of <i>m1</i> is not a subtype of the type of <i>m2</i>.
   */
  INVALID_OVERRIDE_GETTER_TYPE(""),

  /**
   * 7.1 Instance Methods: It is a static warning if an instance method <i>m1</i> overrides an
   * instance method <i>m2</i> and the type of <i>m1</i> is not a subtype of the type of <i>m2</i>.
   */
  INVALID_OVERRIDE_RETURN_TYPE(""),

  /**
   * 7.3 Setters: It is a static warning if a setter <i>m1</i> overrides a setter <i>m2</i> and the
   * type of <i>m1</i> is not a subtype of the type of <i>m2</i>.
   */
  INVALID_OVERRIDE_SETTER_RETURN_TYPE(""),

  /**
   * 12.15.4 Super Invocation: A super method invocation <i>i</i> has the form
   * <i>super.m(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;
   * x<sub>n+k</sub>: a<sub>n+k</sub>)</i>. If <i>S.m</i> exists, it is a static warning if the type
   * <i>F</i> of <i>S.m</i> may not be assigned to a function type.
   */
  INVOCATION_OF_NON_FUNCTION(""),

  /**
   * 7.3 Setters: It is a static warning if a class has a setter named <i>v=</i> with argument type
   * <i>T</i> and a getter named <i>v</i> with return type <i>S</i>, and <i>T</i> may not be
   * assigned to <i>S</i>.
   */
  MISMATCHED_GETTER_AND_SETTER_TYPES(""),

  /**
   * 12.11.1 New: It is a static warning if <i>q</i> is a constructor of an abstract class and
   * <i>q</i> is not a factory constructor.
   */
  NEW_WITH_ABSTRACT_CLASS("Abstract classes cannot be created with a 'new' expression"),

  /**
   * 12.11.1 New: It is a static warning if <i>T</i> is not a class accessible in the current scope,
   * optionally followed by type arguments.
   */
  NEW_WITH_NON_TYPE(""),

  /**
   * 12.11.1 New: If <i>T</i> is a class or parameterized type accessible in the current scope then:
   * 1. If <i>e</i> is of the form <i>new T.id(a<sub>1</sub>, &hellip;, a<sub>n</sub>,
   * x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;, x<sub>n+k</sub>: a<sub>n+k</sub>)</i> it is a
   * static warning if <i>T.id</i> is not the name of a constructor declared by the type <i>T</i>.
   * If <i>e</i> of the form <i>new T(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>:
   * a<sub>n+1</sub>, &hellip; x<sub>n+k</sub>: a<sub>n+kM/sub>)</i> it is a static warning if the
   * type <i>T</i> does not declare a constructor with the same name as the declaration of <i>T</i>.
   */
  NEW_WITH_UNDEFINED_CONSTRUCTOR(""),

  /**
   * 7.10 Superinterfaces: It is a static warning if the implicit interface of a non-abstract class
   * <i>C</i> includes an instance member <i>m</i> and <i>C</i> does not declare or inherit a
   * corresponding instance member <i>m</i>.
   */
  NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER(""),

  /**
   * 7.9.1 Inheritance and Overriding: It is a static warning if a non-abstract class inherits an
   * abstract method.
   */
  NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_METHOD(""),

  /**
   * 12.31 Type Test: It is a static warning if <i>T</i> does not denote a type available in the
   * current lexical scope.
   */
  NON_TYPE(""),

  /**
   * 13.10 Try: An on-catch clause of the form <i>on T catch (p<sub>1</sub>, p<sub>2</sub>) s</i> or
   * <i>on T s</i> matches an object <i>o</i> if the type of <i>o</i> is a subtype of <i>T</i>. It
   * is a static warning if <i>T</i> does not denote a type available in the lexical scope of the
   * catch clause.
   */
  NON_TYPE_IN_CATCH_CLAUSE(""),

  /**
   * 7.1.1 Operators: It is a static warning if the return type of the user-declared operator []= is
   * explicitly declared and not void.
   */
  NON_VOID_RETURN_FOR_OPERATOR(""),

  /**
   * 7.3 Setters: It is a static warning if a setter declares a return type other than void.
   */
  NON_VOID_RETURN_FOR_SETTER(""),

  /**
   * 8 Interfaces: It is a static warning if an interface member <i>m1</i> overrides an interface
   * member <i>m2</i> and the type of <i>m1</i> is not a subtype of the type of <i>m2</i>.
   */
  OVERRIDE_NOT_SUBTYPE(""),

  /**
   * 8 Interfaces: It is a static warning if an interface method <i>m1</i> overrides an interface
   * method <i>m2</i>, the signature of <i>m2</i> explicitly specifies a default value for a formal
   * parameter <i>p</i> and the signature of <i>m1</i> specifies a different default value for
   * <i>p</i>.
   */
  OVERRIDE_WITH_DIFFERENT_DEFAULT(""),

  /**
   * 14.3 Parts: It is a static warning if the referenced part declaration <i>p</i> names a library
   * other than the current library as the library to which <i>p</i> belongs.
   * 
   * @param expectedLibraryName the name of expected library name
   * @param actualLibraryName the non-matching actual library name from the "part of" declaration
   */
  PART_OF_DIFFERENT_LIBRARY("Expected this library to be part of '%s', not '%s'"),

  /**
   * 7.6.2 Factories: It is a static warning if the function type of <i>k’</i> is not a subtype of
   * the type of <i>k</i>.
   */
  REDIRECT_TO_INVALID_RETURN_TYPE(""),

  /**
   * 7.6.2 Factories: It is a static warning if type does not denote a class accessible in the
   * current scope; if type does denote such a class <i>C</i> it is a static warning if the
   * referenced constructor (be it <i>type</i> or <i>type.id</i>) is not a constructor of <i>C</i>.
   */
  REDIRECT_TO_MISSING_CONSTRUCTOR(""),

  /**
   * 7.6.2 Factories: It is a static warning if type does not denote a class accessible in the
   * current scope; if type does denote such a class <i>C</i> it is a static warning if the
   * referenced constructor (be it <i>type</i> or <i>type.id</i>) is not a constructor of <i>C</i>.
   */
  REDIRECT_TO_NON_CLASS(""),

  /**
   * 13.11 Return: Let <i>f</i> be the function immediately enclosing a return statement of the form
   * <i>return;</i> It is a static warning if both of the following conditions hold:
   * <ol>
   * <li><i>f</i> is not a generative constructor.
   * <li>The return type of <i>f</i> may not be assigned to void.
   * </ol>
   */
  RETURN_WITHOUT_VALUE(""),

  /**
   * 13.9 Switch: It is a static warning if the type of <i>e</i> may not be assigned to the type of
   * <i>e<sub>k</sub></i>.
   */
  SWITCH_EXPRESSION_NOT_ASSIGNABLE(""),

  /**
   * 12.15.3 Static Invocation: A static method invocation <i>i</i> has the form
   * <i>C.m(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;
   * x<sub>n+k</sub>: a<sub>n+k</sub>)</i>. It is a static warning if <i>C</i> does not denote a
   * class in the current scope.
   */
  UNDEFINED_CLASS("Undefined class '%s'"),

  /**
   * 12.17 Getter Invocation: It is a static warning if there is no class <i>C</i> in the enclosing
   * lexical scope of <i>i</i>, or if <i>C</i> does not declare, implicitly or explicitly, a getter
   * named <i>m</i>.
   * 
   * @param getterName the name of the getter
   * @param enclosingType the name of the enclosing type where the getter is being looked for
   */
  // TODO(jwren) tests needed for this error code
  UNDEFINED_GETTER("There is no such getter '%s' in '%s'"),

  /**
   * 12.30 Identifier Reference: It is as static warning if an identifier expression of the form
   * <i>id</i> occurs inside a top level or static function (be it function, method, getter, or
   * setter) or variable initializer and there is no declaration <i>d</i> with name <i>id</i> in the
   * lexical scope enclosing the expression.
   */
  // TODO(jwren) Should we include the " in '%s'" in this message as well?
  UNDEFINED_IDENTIFIER("Undefined name '%s'"),

  /**
   * 12.30 Identifier Reference: It is as static warning if an identifier expression of the form
   * <i>id</i> occurs inside a top level or static function (be it function, method, getter, or
   * setter) or variable initializer and there is no declaration <i>d</i> with name <i>id</i> in the
   * lexical scope enclosing the expression.
   * 
   * @param operator the name of the operator
   * @param enclosingType the name of the enclosing type where the operator is being looked for
   */
  // TODO(jwren) tests needed for this error code
  UNDEFINED_OPERATOR("There is no such operator '%s' in '%s'"),

  /**
   * 12.18 Assignment: It is as static warning if an assignment of the form <i>v = e</i> occurs
   * inside a top level or static function (be it function, method, getter, or setter) or variable
   * initializer and there is no declaration <i>d</i> with name <i>v=</i> in the lexical scope
   * enclosing the assignment.
   * <p>
   * 12.18 Assignment: It is a static warning if there is no class <i>C</i> in the enclosing lexical
   * scope of the assignment, or if <i>C</i> does not declare, implicitly or explicitly, a setter
   * <i>v=</i>.
   * 
   * @param setterName the name of the getter
   * @param enclosingType the name of the enclosing type where the setter is being looked for
   */
  // TODO(jwren) tests needed for this error code
  UNDEFINED_SETTER("There is no such setter '%s' in '%s'"),

  /**
   * 12.15.3 Static Invocation: It is a static warning if <i>C</i> does not declare a static method
   * or getter <i>m</i>.
   * 
   * @param methodName the name of the method
   * @param enclosingType the name of the enclosing type where the method is being looked for
   */
  // TODO(jwren) Even though we have a message here, this warning code is not being generated.
  UNDEFINED_STATIC_METHOD_OR_GETTER("There is no such static method '%s' in '%s'");

  /**
   * The message template used to create the message to be displayed for this error.
   */
  private final String message;

  /**
   * Initialize a newly created error code to have the given type and message.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private StaticWarningCode(String message) {
    this.message = message;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return ErrorType.STATIC_WARNING.getSeverity();
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ErrorType getType() {
    return ErrorType.STATIC_WARNING;
  }

  @Override
  public boolean needsRecompilation() {
    return true;
  }
}
