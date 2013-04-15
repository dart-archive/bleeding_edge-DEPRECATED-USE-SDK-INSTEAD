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
 * The enumeration {@code CompileTimeErrorCode} defines the error codes used for compile time
 * errors. The convention for this class is for the name of the error code to indicate the problem
 * that caused the error to be generated and for the error message to explain what is wrong and,
 * when appropriate, how the problem can be corrected.
 * 
 * @coverage dart.engine.error
 */
public enum CompileTimeErrorCode implements ErrorCode {
  /**
   * 14.2 Exports: It is a compile-time error if a name <i>N</i> is re-exported by a library
   * <i>L</i> and <i>N</i> is introduced into the export namespace of <i>L</i> by more than one
   * export.
   */
  AMBIGUOUS_EXPORT(""),

  /**
   * 14.1 Imports: If a name <i>N</i> is referenced by a library <i>L</i> and <i>N</i> is introduced
   * into the top level scope <i>L</i> by more than one import then:
   * <ol>
   * <li>It is a static warning if <i>N</i> is used as a type annotation.
   * <li>In checked mode, it is a dynamic error if <i>N</i> is used as a type annotation and
   * referenced during a subtype test.
   * <li>Otherwise, it is a compile-time error.
   * </ol>
   */
  AMBIGUOUS_IMPORT(""),

  /**
   * 12.33 Argument Definition Test: It is a compile time error if <i>v</i> does not denote a formal
   * parameter.
   * 
   * @param the name of the identifier in the argument definition test that is not a parameter
   */
  ARGUMENT_DEFINITION_TEST_NON_PARAMETER("'%s' is not a parameter"),

  /**
   * 12.30 Identifier Reference: It is a compile-time error to use a built-in identifier other than
   * dynamic as a type annotation.
   */
  BUILT_IN_IDENTIFIER_AS_TYPE("The built-in identifier '%s' cannot be as a type"),

  /**
   * 12.30 Identifier Reference: It is a compile-time error if a built-in identifier is used as the
   * declared name of a class, type parameter or type alias.
   */
  BUILT_IN_IDENTIFIER_AS_TYPE_NAME("The built-in identifier '%s' cannot be used as a type name"),

  /**
   * 12.30 Identifier Reference: It is a compile-time error if a built-in identifier is used as the
   * declared name of a class, type parameter or type alias.
   */
  BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME(
      "The built-in identifier '%s' cannot be used as a type alias name"),

  /**
   * 12.30 Identifier Reference: It is a compile-time error if a built-in identifier is used as the
   * declared name of a class, type parameter or type alias.
   */
  BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME(
      "The built-in identifier '%s' cannot be used as a type variable name"),

  /**
   * 13.9 Switch: It is a compile-time error if the class <i>C</i> implements the operator
   * <i>==</i>.
   */
  CASE_EXPRESSION_TYPE_IMPLEMENTS_EQUALS(
      "The switch case expression type '%s' cannot override the == operator"),

  /**
   * 12.1 Constants: It is a compile-time error if evaluation of a compile-time constant would raise
   * an exception.
   */
  COMPILE_TIME_CONSTANT_RAISES_EXCEPTION(""),

  /**
   * 7.6 Constructors: A constructor name always begins with the name of its immediately enclosing
   * class, and may optionally be followed by a dot and an identifier <i>id</i>. It is a
   * compile-time error if <i>id</i> is the name of a member declared in the immediately enclosing
   * class.
   */
  CONFLICTING_CONSTRUCTOR_NAME_AND_FIELD(
      "'%s' cannot be used to name a constructor and a method in this class"),

  /**
   * 7.6 Constructors: A constructor name always begins with the name of its immediately enclosing
   * class, and may optionally be followed by a dot and an identifier <i>id</i>. It is a
   * compile-time error if <i>id</i> is the name of a member declared in the immediately enclosing
   * class.
   */
  CONFLICTING_CONSTRUCTOR_NAME_AND_METHOD(
      "'%s' cannot be used to name a constructor and a field in this class"),

  /**
   * 7.6.3 Constant Constructors: It is a compile-time error if a constant constructor is declared
   * by a class that has a non-final instance variable.
   */
  CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD(
      "Classes with non-final fields cannot define 'const' constructors"),

  /**
   * 6.2 Formal Parameters: It is a compile-time error if a formal parameter is declared as a
   * constant variable.
   */
  CONST_FORMAL_PARAMETER("Parameters cannot be 'const'"),

  /**
   * 5 Variables: A constant variable must be initialized to a compile-time constant or a
   * compile-time error occurs.
   */
  CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE("'const' variables must be constant value"),

  /**
   * 12.11.2 Const: It is a compile-time error if evaluation of a constant object results in an
   * uncaught exception being thrown.
   */
  CONST_EVAL_THROWS_EXCEPTION("'const' constructors cannot throw exceptions"),

  /**
   * 12.11.2 Const: If <i>T</i> is a parameterized type <i>S&lt;U<sub>1</sub>, &hellip;,
   * U<sub>m</sub>&gt;</i>, let <i>R = S</i>; It is a compile time error if <i>S</i> is not a
   * generic type with <i>m</i> type parameters.
   * 
   * @param typeName the name of the type being referenced (<i>S</i>)
   * @param parameterCount the number of type parameters that were declared
   * @param argumentCount the number of type arguments provided
   */
  CONST_WITH_INVALID_TYPE_PARAMETERS(
      "The type '%s' is declared with %d type parameters, but %d type arguments were given"),

  /**
   * 12.11.2 Const: If <i>e</i> is of the form <i>const T(a<sub>1</sub>, &hellip;, a<sub>n</sub>,
   * x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;, x<sub>n+k</sub>: a<sub>n+k</sub>)</i> it is a
   * compile-time error if the type <i>T</i> does not declare a constant constructor with the same
   * name as the declaration of <i>T</i>.
   */
  CONST_WITH_NON_CONST("The constructor being called is not a 'const' constructor"),

  /**
   * 12.11.2 Const: In all of the above cases, it is a compile-time error if <i>a<sub>i</sub>, 1
   * &lt;= i &lt;= n + k</i>, is not a compile-time constant expression.
   */
  CONST_WITH_NON_CONSTANT_ARGUMENT(""),

  /**
   * 12.11.2 Const: It is a compile-time error if <i>T</i> is not a class accessible in the current
   * scope, optionally followed by type arguments.
   * <p>
   * 12.11.2 Const: If <i>e</i> is of the form <i>const T.id(a<sub>1</sub>, &hellip;, a<sub>n</sub>,
   * x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip; x<sub>n+k</sub>: a<sub>n+k</sub>)</i> it is a
   * compile-time error if <i>T</i> is not a class accessible in the current scope, optionally
   * followed by type arguments.
   */
  CONST_WITH_NON_TYPE(""),

  /**
   * 12.11.2 Const: It is a compile-time error if <i>T</i> includes any type parameters.
   */
  CONST_WITH_TYPE_PARAMETERS(""),

  /**
   * 12.11.2 Const: It is a compile-time error if <i>T.id</i> is not the name of a constant
   * constructor declared by the type <i>T</i>.
   */
  CONST_WITH_UNDEFINED_CONSTRUCTOR(""),

  /**
   * 15.3.1 Typedef: It is a compile-time error if any default values are specified in the signature
   * of a function type alias.
   */
  DEFAULT_VALUE_IN_FUNCTION_TYPE_ALIAS("Default values aren't allowed in typedefs"),

  /**
   * 3.1 Scoping: It is a compile-time error if there is more than one entity with the same name
   * declared in the same scope.
   * 
   * @param duplicateName the name of the duplicate entity
   */
  DUPLICATE_DEFINITION("The name '%s' is already defined"),

  /**
   * 7 Classes: It is a compile-time error if a class declares two members of the same name.
   */
  DUPLICATE_MEMBER_NAME(""),

  /**
   * 7 Classes: It is a compile-time error if a class has an instance member and a static member
   * with the same name.
   */
  DUPLICATE_MEMBER_NAME_INSTANCE_STATIC(""),

  /**
   * 12.14.2 Binding Actuals to Formals: It is a compile-time error if <i>q<sub>i</sub> =
   * q<sub>j</sub></i> for any <i>i != j</i> [where <i>q<sub>i</sub></i> is the label for a named
   * argument].
   */
  DUPLICATE_NAMED_ARGUMENT(""),

  /**
   * 14.2 Exports: It is a compile-time error if the compilation unit found at the specified URI is
   * not a library declaration.
   * 
   * @param uri the uri pointing to a non-library declaration
   */
  EXPORT_OF_NON_LIBRARY("The exported library '%s' must not have a part-of directive"),

  /**
   * 7.9 Superclasses: It is a compile-time error if the extends clause of a class <i>C</i> includes
   * a type expression that does not denote a class available in the lexical scope of <i>C</i>.
   * 
   * @param typeName the name of the superclass that was not found
   */
  EXTENDS_NON_CLASS("Classes can only extend other classes"),

  /**
   * 12.2 Null: It is a compile-time error for a class to attempt to extend or implement Null.
   * <p>
   * 12.3 Numbers: It is a compile-time error for a class to attempt to extend or implement int.
   * <p>
   * 12.3 Numbers: It is a compile-time error for a class to attempt to extend or implement double.
   * <p>
   * 12.3 Numbers: It is a compile-time error for any type other than the types int and double to
   * attempt to extend or implement num.
   * <p>
   * 12.4 Booleans: It is a compile-time error for a class to attempt to extend or implement bool.
   * <p>
   * 12.5 Strings: It is a compile-time error for a class to attempt to extend or implement String.
   * 
   * @param typeName the name of the type that cannot be extended
   * @see #IMPLEMENTS_DISALLOWED_CLASS
   */
  EXTENDS_DISALLOWED_CLASS("Classes cannot extend '%s'"),

  /**
   * 12.2 Null: It is a compile-time error for a class to attempt to extend or implement Null.
   * <p>
   * 12.3 Numbers: It is a compile-time error for a class to attempt to extend or implement int.
   * <p>
   * 12.3 Numbers: It is a compile-time error for a class to attempt to extend or implement double.
   * <p>
   * 12.3 Numbers: It is a compile-time error for any type other than the types int and double to
   * attempt to extend or implement num.
   * <p>
   * 12.4 Booleans: It is a compile-time error for a class to attempt to extend or implement bool.
   * <p>
   * 12.5 Strings: It is a compile-time error for a class to attempt to extend or implement String.
   * 
   * @param typeName the name of the type that cannot be implemented
   * @see #EXTENDS_DISALLOWED_CLASS
   */
  IMPLEMENTS_DISALLOWED_CLASS("Classes cannot implement '%s'"),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. It is a compile time
   * error if more than one initializer corresponding to a given instance variable appears in
   * <i>k</i>’s list.
   */
  FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS(
      "The field '%s' cannot be initialized twice in the same constructor"),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. It is a compile time
   * error if <i>k</i>’s initializer list contains an initializer for a final variable <i>f</i>
   * whose declaration includes an initialization expression.
   */
  FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION(
      "Values cannot be set in the constructor if they are final, and have already been set"),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. It is a compile time
   * error if <i>k</i>’s initializer list contains an initializer for a variable that is initialized
   * by means of an initializing formal of <i>k</i>.
   */
  FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER(
      "Fields cannot be initialized in both the parameter list and the initializers"),

  /**
   * 7.6.1 Generative Constructors: It is a compile-time error if an initializing formal is used by
   * a function other than a non-redirecting generative constructor.
   */
  FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR(
      "Initializing formal fields can only be used in constructors"),

  /**
   * 5 Variables: It is a compile-time error if a final instance variable that has been initialized
   * at its point of declaration is also initialized in a constructor.
   * 
   * @param name the name of the field in question
   */
  // TODO (jwren) only a subset of these are being caught
  FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR(
      "'%s' is final and was given a value when it was declared, so it cannot be set to a new value"),

  /**
   * 5 Variables: It is a compile-time error if a final instance variable that has is initialized by
   * means of an initializing formal of a constructor is also initialized elsewhere in the same
   * constructor.
   * 
   * @param name the name of the field in question
   */
  // TODO (jwren) only a subset of these are being caught
  FINAL_INITIALIZED_MULTIPLE_TIMES("'%s' is a final field and so can only be set once"),

  /**
   * 5 Variables: It is a compile-time error if a library, static or local variable <i>v</i> is
   * final and <i>v</i> is not initialized at its point of declaration.
   * 
   * @param name the name of the variable in question
   */
  // TODO(jwren) not yet being created,
  // see ErrorVerifier.checkForAllFinalInitializedErrorCodes(ConstructorDeclaration)
  FINAL_NOT_INITIALIZED("The final variable '%s' must be initialized"),

  /**
   * 7.2 Getters: It is a compile-time error if a class has both a getter and a method with the same
   * name.
   */
  GETTER_AND_METHOD_WITH_SAME_NAME(""),

  /**
   * 7.10 Superinterfaces: It is a compile-time error if the implements clause of a class includes
   * type dynamic.
   */
  IMPLEMENTS_DYNAMIC("Classes cannot implement 'dynamic'"),

  /**
   * 7.10 Superinterfaces: It is a compile-time error if the implements clause of a class <i>C</i>
   * includes a type expression that does not denote a class available in the lexical scope of
   * <i>C</i>.
   * 
   * @param typeName the name of the interface that was not found
   */
  IMPLEMENTS_NON_CLASS("Classes can only implement other classes"),

  /**
   * 7.10 Superinterfaces: It is a compile-time error if a type <i>T</i> appears more than once in
   * the implements clause of a class.
   * 
   * @param name the name of the type in question
   */
  IMPLEMENTS_REPEATED("'%s' can only be implemented once"),

  /**
   * 7.10 Superinterfaces: It is a compile-time error if the interface of a class <i>C</i> is a
   * superinterface of itself.
   * 
   * @param name the name of the type in question
   */
  IMPLEMENTS_SELF("'%s' cannot implement itself"),

  /**
   * 14.1 Imports: It is a compile-time error to import two different libraries with the same name.
   */
  IMPORT_DUPLICATED_LIBRARY_NAME(""),

  /**
   * 14.1 Imports: It is a compile-time error if the compilation unit found at the specified URI is
   * not a library declaration.
   * 
   * @param uri the uri pointing to a non-library declaration
   */
  IMPORT_OF_NON_LIBRARY("The imported library '%s' must not have a part-of directive"),

  /**
   * 13.9 Switch: It is a compile-time error if values of the expressions <i>e<sub>k</sub></i> are
   * not instances of the same class <i>C</i>, for all <i>1 &lt;= k &lt;= n</i>.
   */
  INCONSITENT_CASE_EXPRESSION_TYPES(""),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. It is a compile-time
   * error if <i>k</i>'s initializer list contains an initializer for a variable that is not an
   * instance variable declared in the immediately surrounding class.
   * 
   * @see #INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD
   */
  INITIALIZER_FOR_NON_EXISTANT_FIELD(""),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. It is a compile-time
   * error if <i>k</i>'s initializer list contains an initializer for a variable that is not an
   * instance variable declared in the immediately surrounding class.
   */
  INITIALIZER_FOR_STATIC_FIELD(""),

  /**
   * 7.6.1 Generative Constructors: An initializing formal has the form <i>this.id</i>. It is a
   * compile-time error if <i>id</i> is not the name of an instance variable of the immediately
   * enclosing class.
   * 
   * @param id the name of the initializing formal that is not an instance variable in the
   *          immediately enclosing class
   * @see #INITIALIZING_FORMAL_FOR_STATIC_FIELD
   * @see #INITIALIZER_FOR_NON_EXISTANT_FIELD
   */
  INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD("'%s' is not a variable in the enclosing class"),

  /**
   * 7.6.1 Generative Constructors: An initializing formal has the form <i>this.id</i>. It is a
   * compile-time error if <i>id</i> is not the name of an instance variable of the immediately
   * enclosing class.
   * 
   * @param id the name of the initializing formal that is a static variable in the immediately
   *          enclosing class
   * @see #INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD
   */
  INITIALIZING_FORMAL_FOR_STATIC_FIELD(
      "'%s' is a static variable in the enclosing class, variable initialized in a constructor cannot be static"),

  /**
   * TODO(brianwilkerson) Remove this when we have decided on how to report errors in compile-time
   * constants. Until then, this acts as a placeholder for more informative errors.
   */
  INVALID_CONSTANT(""),

  /**
   * 7.6 Constructors: It is a compile-time error if the name of a constructor is not a constructor
   * name.
   */
  INVALID_CONSTRUCTOR_NAME(""),

  /**
   * 7.6.2 Factories: It is a compile-time error if <i>M</i> is not the name of the immediately
   * enclosing class.
   */
  INVALID_FACTORY_NAME_NOT_A_CLASS(""),

  /**
   * 7.1 Instance Methods: It is a static warning if an instance method <i>m1</i> overrides an
   * instance member <i>m2</i>, the signature of <i>m2</i> explicitly specifies a default value for
   * a formal parameter <i>p</i> and the signature of <i>m1</i> specifies a different default value
   * for <i>p</i>.
   */
  INVALID_OVERRIDE_DEFAULT_VALUE(""),

  /**
   * 7.1: It is a compile-time error if an instance method <i>m1</i> overrides an instance member
   * <i>m2</i> and <i>m1</i> does not declare all the named parameters declared by <i>m2</i>.
   */
  INVALID_OVERRIDE_NAMED(""),

  /**
   * 7.1 Instance Methods: It is a compile-time error if an instance method m1 overrides an instance
   * member <i>m2</i> and <i>m1</i> has fewer optional positional parameters than <i>m2</i>.
   */
  INVALID_OVERRIDE_POSITIONAL(""),

  /**
   * 7.1 Instance Methods: It is a compile-time error if an instance method <i>m1</i> overrides an
   * instance member <i>m2</i> and <i>m1</i> has a different number of required parameters than
   * <i>m2</i>.
   */
  INVALID_OVERRIDE_REQUIRED(""),

  /**
   * 12.10 This: It is a compile-time error if this appears in a top-level function or variable
   * initializer, in a factory constructor, or in a static method or variable initializer, or in the
   * initializer of an instance variable.
   */
  INVALID_REFERENCE_TO_THIS(""),

  /**
   * 12.7 Maps: It is a compile-time error if the first type argument to a map literal is not
   * String.
   */
  INVALID_TYPE_ARGUMENT_FOR_KEY(""),

  /**
   * 12.6 Lists: It is a compile time error if the type argument of a constant list literal includes
   * a type parameter.
   */
  INVALID_TYPE_ARGUMENT_IN_CONST_LIST(""),

  /**
   * 12.7 Maps: It is a compile time error if the type arguments of a constant map literal include a
   * type parameter.
   */
  INVALID_TYPE_ARGUMENT_IN_CONST_MAP(""),

  /**
   * 14.2 Exports: It is a compile-time error if the compilation unit found at the specified URI is
   * not a library declaration.
   * <p>
   * 14.1 Imports: It is a compile-time error if the compilation unit found at the specified URI is
   * not a library declaration.
   * <p>
   * 14.3 Parts: It is a compile time error if the contents of the URI are not a valid part
   * declaration.
   * 
   * @param uri the uri pointing to a non-library declaration
   */
  INVALID_URI("'%s' is not a valid uri"),

  /**
   * 13.13 Break: It is a compile-time error if no such statement <i>s<sub>E</sub></i> exists within
   * the innermost function in which <i>s<sub>b</sub></i> occurs.
   * <p>
   * 13.14 Continue: It is a compile-time error if no such statement or case clause
   * <i>s<sub>E</sub></i> exists within the innermost function in which <i>s<sub>c</sub></i> occurs.
   * 
   * @param labelName the name of the unresolvable label
   */
  LABEL_IN_OUTER_SCOPE("Cannot reference label '%s' declared in an outer method"),

  /**
   * 13.13 Break: It is a compile-time error if no such statement <i>s<sub>E</sub></i> exists within
   * the innermost function in which <i>s<sub>b</sub></i> occurs.
   * <p>
   * 13.14 Continue: It is a compile-time error if no such statement or case clause
   * <i>s<sub>E</sub></i> exists within the innermost function in which <i>s<sub>c</sub></i> occurs.
   * 
   * @param labelName the name of the unresolvable label
   */
  LABEL_UNDEFINED("Cannot reference undefined label '%s'"),

  /**
   * 7 Classes: It is a compile time error if a class <i>C</i> declares a member with the same name
   * as <i>C</i>.
   */
  MEMBER_WITH_CLASS_NAME(""),

  /**
   * 9 Mixins: It is a compile-time error if a declared or derived mixin explicitly declares a
   * constructor.
   */
  MIXIN_DECLARES_CONSTRUCTOR(""),

  /**
   * 9 Mixins: It is a compile-time error if a mixin is derived from a class whose superclass is not
   * Object.
   */
  MIXIN_INHERITS_FROM_NOT_OBJECT(""),

  /**
   * 9.1 Mixin Application: It is a compile-time error if <i>M</i> does not denote a class or mixin
   * available in the immediately enclosing scope.
   * 
   * @param typeName the name of the mixin that was not found
   */
  MIXIN_OF_NON_CLASS("Classes can only mixin other classes"),

  /**
   * 9.1 Mixin Application: If <i>M</i> is a class, it is a compile time error if a well formed
   * mixin cannot be derived from <i>M</i>.
   */
  MIXIN_OF_NON_MIXIN(""), // TODO(brianwilkerson) This might be covered by more specific errors.

  /**
   * 9 Mixins: It is a compile-time error if a declared or derived mixin refers to super.
   */
  MIXIN_REFERENCES_SUPER(""),

  /**
   * 9.1 Mixin Application: It is a compile-time error if <i>S</i> does not denote a class available
   * in the immediately enclosing scope.
   */
  MIXIN_WITH_NON_CLASS_SUPERCLASS(""),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. Then <i>k</i> may
   * include at most one superinitializer in its initializer list or a compile time error occurs.
   */
  MULTIPLE_SUPER_INITIALIZERS(""),

  /**
   * 12.11.1 New: It is a compile time error if <i>S</i> is not a generic type with <i>m</i> type
   * parameters.
   * 
   * @param typeName the name of the type being referenced (<i>S</i>)
   * @param parameterCount the number of type parameters that were declared
   * @param argumentCount the number of type arguments provided
   */
  NEW_WITH_INVALID_TYPE_PARAMETERS(
      "The type '%s' is declared with %d type parameters, but %d type arguments were given"),

  /**
   * 13.2 Expression Statements: It is a compile-time error if a non-constant map literal that has
   * no explicit type arguments appears in a place where a statement is expected.
   */
  NON_CONST_MAP_AS_EXPRESSION_STATEMENT(""),

  /**
   * 13.9 Switch: Given a switch statement of the form <i>switch (e) { label<sub>11</sub> &hellip;
   * label<sub>1j1</sub> case e<sub>1</sub>: s<sub>1</sub> &hellip; label<sub>n1</sub> &hellip;
   * label<sub>njn</sub> case e<sub>n</sub>: s<sub>n</sub> default: s<sub>n+1</sub>}</i> or the form
   * <i>switch (e) { label<sub>11</sub> &hellip; label<sub>1j1</sub> case e<sub>1</sub>:
   * s<sub>1</sub> &hellip; label<sub>n1</sub> &hellip; label<sub>njn</sub> case e<sub>n</sub>:
   * s<sub>n</sub>}</i>, it is a compile-time error if the expressions <i>e<sub>k</sub></i> are not
   * compile-time constants, for all <i>1 &lt;= k &lt;= n</i>.
   */
  NON_CONSTANT_CASE_EXPRESSION("Case expressions must be constant"),

  /**
   * 6.2.2 Optional Formals: It is a compile-time error if the default value of an optional
   * parameter is not a compile-time constant.
   */
  NON_CONSTANT_DEFAULT_VALUE("Default values of an optional parameter must be constant"),

  /**
   * 12.6 Lists: It is a compile time error if an element of a constant list literal is not a
   * compile-time constant.
   */
  NON_CONSTANT_LIST_ELEMENT("'const' lists must have all constant values"),

  /**
   * 12.7 Maps: It is a compile time error if either a key or a value of an entry in a constant map
   * literal is not a compile-time constant.
   */
  NON_CONSTANT_MAP_KEY("The keys in a map must be constant"),

  /**
   * 12.7 Maps: It is a compile time error if either a key or a value of an entry in a constant map
   * literal is not a compile-time constant.
   */
  NON_CONSTANT_MAP_VALUE("The values in a 'const' map must be constant"),

  /**
   * 7.6.3 Constant Constructors: Any expression that appears within the initializer list of a
   * constant constructor must be a potentially constant expression, or a compile-time error occurs.
   */
  NON_CONSTANT_VALUE_IN_INITIALIZER(""),

  /**
   * 7.9 Superclasses: It is a compile-time error to specify an extends clause for class Object.
   */
  OBJECT_CANNOT_EXTEND_ANOTHER_CLASS(""),

  /**
   * 7.1.1 Operators: It is a compile-time error to declare an optional parameter in an operator.
   */
  OPTIONAL_PARAMETER_IN_OPERATOR(""),

  /**
   * 8 Interfaces: It is a compile-time error if an interface member <i>m1</i> overrides an
   * interface member <i>m2</i> and <i>m1</i> does not declare all the named parameters declared by
   * <i>m2</i> in the same order.
   */
  OVERRIDE_MISSING_NAMED_PARAMETERS(""),

  /**
   * 8 Interfaces: It is a compile-time error if an interface member <i>m1</i> overrides an
   * interface member <i>m2</i> and <i>m1</i> has a different number of required parameters than
   * <i>m2</i>.
   */
  OVERRIDE_MISSING_REQUIRED_PARAMETERS(""),

  /**
   * 14.3 Parts: It is a compile time error if the contents of the URI are not a valid part
   * declaration.
   * 
   * @param uri the uri pointing to a non-library declaration
   */
  PART_OF_NON_PART("The included part '%s' must have a part-of directive"),

  /**
   * 14.1 Imports: It is a compile-time error if the current library declares a top-level member
   * named <i>p</i>.
   */
  PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER(""),

  /**
   * 6.2.2 Optional Formals: It is a compile-time error if the name of a named optional parameter
   * begins with an ‘_’ character.
   */
  PRIVATE_OPTIONAL_PARAMETER(""),

  /**
   * 12.1 Constants: It is a compile-time error if the value of a compile-time constant expression
   * depends on itself.
   */
  RECURSIVE_COMPILE_TIME_CONSTANT(""),

  /**
   * 7.6.2 Factories: It is a compile-time error if a redirecting factory constructor redirects to
   * itself, either directly or indirectly via a sequence of redirections.
   */
  RECURSIVE_FACTORY_REDIRECT(""),

  /**
   * 15.3.1 Typedef: It is a compile-time error if a typedef refers to itself via a chain of
   * references that does not include a class type.
   */
  RECURSIVE_FUNCTION_TYPE_ALIAS(""),

  /**
   * 8.1 Superinterfaces: It is a compile-time error if an interface is a superinterface of itself.
   */
  RECURSIVE_INTERFACE_INHERITANCE(""),

  /**
   * 7.6.2 Factories: It is a compile-time error if <i>k</i> is prefixed with the const modifier but
   * <i>k’</i> is not a constant constructor.
   */
  REDIRECT_TO_NON_CONST_CONSTRUCTOR(""),

  /**
   * 13.3 Local Variable Declaration: It is a compile-time error if <i>e</i> refers to the name
   * <i>v</i> or the name <i>v=</i>.
   */
  REFERENCE_TO_DECLARED_VARIABLE_IN_INITIALIZER(""),

  /**
   * 16.1.1 Reserved Words: A reserved word may not be used as an identifier; it is a compile-time
   * error if a reserved word is used where an identifier is expected.
   */
  RESERVED_WORD_AS_IDENTIFIER(""),

  /**
   * 12.8.1 Rethrow: It is a compile-time error if an expression of the form <i>rethrow;</i> is not
   * enclosed within a on-catch clause.
   */
  RETHROW_OUTSIDE_CATCH("rethrow must be inside of a catch clause"),

  /**
   * 13.11 Return: It is a compile-time error if a return statement of the form <i>return e;</i>
   * appears in a generative constructor.
   */
  RETURN_IN_GENERATIVE_CONSTRUCTOR(""),

  /**
   * 6.1 Function Declarations: It is a compile-time error to preface a function declaration with
   * the built-in identifier static.
   */
  STATIC_TOP_LEVEL_FUNCTION(""),

  /**
   * 5 Variables: It is a compile-time error to preface a top level variable declaration with the
   * built-in identifier static.
   */
  STATIC_TOP_LEVEL_VARIABLE(""),

  /**
   * 12.15.4 Super Invocation: A super method invocation <i>i</i> has the form
   * <i>super.m(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;
   * x<sub>n+k</sub>: a<sub>n+k</sub>)</i>. It is a compile-time error if a super method invocation
   * occurs in a top-level function or variable initializer, in an instance variable initializer or
   * initializer list, in class Object, in a factory constructor, or in a static method or variable
   * initializer.
   */
  SUPER_IN_INVALID_CONTEXT(""),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. It is a compile-time
   * error if a generative constructor of class Object includes a superinitializer.
   */
  SUPER_INITIALIZER_IN_OBJECT(""),

  /**
   * 12.11 Instance Creation: It is a compile-time error if a constructor of a non-generic type
   * invoked by a new expression or a constant object expression is passed any type arguments.
   * <p>
   * 12.32 Type Cast: It is a compile-time error if <i>T</i> is a parameterized type of the form
   * <i>G&lt;T<sub>1</sub>, &hellip;, T<sub>n</sub>&gt;</i> and <i>G</i> is not a generic type with
   * <i>n</i> type parameters.
   */
  TYPE_ARGUMENTS_FOR_NON_GENERIC_CLASS(""),

  /**
   * 7.6.1 Generative Constructors: Let <i>C</i> be the class in which the superinitializer appears
   * and let <i>S</i> be the superclass of <i>C</i>. Let <i>k</i> be a generative constructor. It is
   * a compile-time error if class <i>S</i> does not declare a generative constructor named <i>S</i>
   * (respectively <i>S.id</i>)
   */
  UNDEFINED_CONSTRUCTOR_IN_INITIALIZER(""),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. Each final instance
   * variable <i>f</i> declared in the immediately enclosing class must have an initializer in
   * <i>k</i>'s initializer list unless it has already been initialized by one of the following
   * means:
   * <ol>
   * <li>Initialization at the declaration of <i>f</i>.
   * <li>Initialization by means of an initializing formal of <i>k</i>.
   * </ol>
   * or a compile-time error occurs.
   */
  UNINITIALIZED_FINAL_FIELD(""),

  /**
   * 14.1 Imports: It is a compile-time error if <i>x</i> is not a compile-time constant, or if
   * <i>x</i> involves string interpolation.
   * <p>
   * 14.3 Parts: It is a compile-time error if <i>s</i> is not a compile-time constant, or if
   * <i>s</i> involves string interpolation.
   * <p>
   * 14.5 URIs: It is a compile-time error if the string literal <i>x</i> that describes a URI is
   * not a compile-time constant, or if <i>x</i> involves string interpolation.
   */
  URI_WITH_INTERPOLATION("URIs cannot use string interpolation"),

  /**
   * 7.1.1 Operators: It is a compile-time error if the arity of the user-declared operator []= is
   * not 2. It is a compile time error if the arity of a user-declared operator with one of the
   * names: &lt;, &gt;, &lt;=, &gt;=, ==, +, /, ~/, *, %, |, ^, &, &lt;&lt;, &gt;&gt;, [] is not 1.
   * It is a compile time error if the arity of the user-declared operator - is not 0 or 1. It is a
   * compile time error if the arity of the user-declared operator ~ is not 0.
   */
  WRONG_NUMBER_OF_PARAMETERS_FOR_OPERATOR(""),

  /**
   * 7.3 Setters: It is a compile-time error if a setter’s formal parameter list does not include
   * exactly one required formal parameter <i>p</i>.
   */
  WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER(""),

  /**
   * 12.11 Instance Creation: It is a compile-time error if a constructor of a generic type with
   * <i>n</i> type parameters invoked by a new expression or a constant object expression is passed
   * <i>m</i> type arguments where <i>m != n</i>.
   * <p>
   * 12.31 Type Test: It is a compile-time error if <i>T</i> is a parameterized type of the form
   * <i>G&lt;T<sub>1</sub>, &hellip;, T<sub>n</sub>&gt;</i> and <i>G</i> is not a generic type with
   * <i>n</i> type parameters.
   */
  WRONG_NUMBER_OF_TYPE_ARGUMENTS("");

  /**
   * The message template used to create the message to be displayed for this error.
   */
  private final String message;

  /**
   * Initialize a newly created error code to have the given message.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private CompileTimeErrorCode(String message) {
    this.message = message;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return ErrorType.COMPILE_TIME_ERROR.getSeverity();
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ErrorType getType() {
    return ErrorType.COMPILE_TIME_ERROR;
  }

  @Override
  public boolean needsRecompilation() {
    return true;
  }
}
