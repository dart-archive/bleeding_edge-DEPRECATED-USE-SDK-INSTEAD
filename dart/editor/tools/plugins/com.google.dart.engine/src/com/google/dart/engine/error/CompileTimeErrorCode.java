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
   * Enum proposal: It is also a compile-time error to explicitly instantiate an enum via 'new' or
   * 'const' or to access its private fields.
   */
  ACCESS_PRIVATE_ENUM_FIELD(
      "The private fields of an enum cannot be accessed, even within the same library"),

  /**
   * 14.2 Exports: It is a compile-time error if a name <i>N</i> is re-exported by a library
   * <i>L</i> and <i>N</i> is introduced into the export namespace of <i>L</i> by more than one
   * export, unless each all exports refer to same declaration for the name N.
   * 
   * @param ambiguousElementName the name of the ambiguous element
   * @param firstLibraryName the name of the first library that the type is found
   * @param secondLibraryName the name of the second library that the type is found
   */
  AMBIGUOUS_EXPORT("The name '%s' is defined in the libraries '%s' and '%s'"),

  /**
   * 12.33 Argument Definition Test: It is a compile time error if <i>v</i> does not denote a formal
   * parameter.
   * 
   * @param the name of the identifier in the argument definition test that is not a parameter
   */
  ARGUMENT_DEFINITION_TEST_NON_PARAMETER("'%s' is not a parameter"),

  /**
   * ?? Asynchronous For-in: It is a compile-time error if an asynchronous for-in statement appears
   * inside a synchronous function.
   */
  ASYNC_FOR_IN_WRONG_CONTEXT(
      "The asynchronous for-in can only be used in a function marked with async or async*"),

  /**
   * ??: It is a compile-time error if the function immediately enclosing a is not declared
   * asynchronous.
   */
  AWAIT_IN_WRONG_CONTEXT(
      "The await expression can only be used in a function marked as async or async*"),

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
  BUILT_IN_IDENTIFIER_AS_TYPE_PARAMETER_NAME(
      "The built-in identifier '%s' cannot be used as a type parameter name"),

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
  // Low priority- no priority until we can come up with an example of such constant
  COMPILE_TIME_CONSTANT_RAISES_EXCEPTION(""),

  /**
   * 7.2 Getters: It is a compile-time error if a class has both a getter and a method with the same
   * name. This restriction holds regardless of whether the getter is defined explicitly or
   * implicitly, or whether the getter or the method are inherited or not.
   */
  CONFLICTING_GETTER_AND_METHOD(
      "Class '%s' cannot have both getter '%s.%s' and method with the same name"),

  /**
   * 7.2 Getters: It is a compile-time error if a class has both a getter and a method with the same
   * name. This restriction holds regardless of whether the getter is defined explicitly or
   * implicitly, or whether the getter or the method are inherited or not.
   */
  CONFLICTING_METHOD_AND_GETTER(
      "Class '%s' cannot have both method '%s.%s' and getter with the same name"),

  /**
   * 7.6 Constructors: A constructor name always begins with the name of its immediately enclosing
   * class, and may optionally be followed by a dot and an identifier <i>id</i>. It is a
   * compile-time error if <i>id</i> is the name of a member declared in the immediately enclosing
   * class.
   */
  CONFLICTING_CONSTRUCTOR_NAME_AND_FIELD(
      "'%s' cannot be used to name a constructor and a field in this class"),

  /**
   * 7.6 Constructors: A constructor name always begins with the name of its immediately enclosing
   * class, and may optionally be followed by a dot and an identifier <i>id</i>. It is a
   * compile-time error if <i>id</i> is the name of a member declared in the immediately enclosing
   * class.
   */
  CONFLICTING_CONSTRUCTOR_NAME_AND_METHOD(
      "'%s' cannot be used to name a constructor and a method in this class"),

  /**
   * 7. Classes: It is a compile time error if a generic class declares a type variable with the
   * same name as the class or any of its members or constructors.
   */
  CONFLICTING_TYPE_VARIABLE_AND_CLASS(
      "'%s' cannot be used to name a type varaible in a class with the same name"),

  /**
   * 7. Classes: It is a compile time error if a generic class declares a type variable with the
   * same name as the class or any of its members or constructors.
   */
  CONFLICTING_TYPE_VARIABLE_AND_MEMBER(
      "'%s' cannot be used to name a type varaible and member in this class"),

  /**
   * 12.11.2 Const: It is a compile-time error if evaluation of a constant object results in an
   * uncaught exception being thrown.
   */
  CONST_CONSTRUCTOR_THROWS_EXCEPTION("'const' constructors cannot throw exceptions"),

  /**
   * 10.6.3 Constant Constructors: It is a compile-time error if a constant constructor is declared
   * by a class C if any instance variable declared in C is initialized with an expression that is
   * not a constant expression.
   */
  CONST_CONSTRUCTOR_WITH_FIELD_INITIALIZED_BY_NON_CONST(
      "Can't define the 'const' constructor because the field '%s' is initialized with a non-constant value"),

  /**
   * 7.6.3 Constant Constructors: The superinitializer that appears, explicitly or implicitly, in
   * the initializer list of a constant constructor must specify a constant constructor of the
   * superclass of the immediately enclosing class or a compile-time error occurs.
   * <p>
   * 9 Mixins: For each generative constructor named ... an implicitly declared constructor named
   * ... is declared.
   */
  CONST_CONSTRUCTOR_WITH_MIXIN("Constant constructor cannot be declared for a class with a mixin"),

  /**
   * 7.6.3 Constant Constructors: The superinitializer that appears, explicitly or implicitly, in
   * the initializer list of a constant constructor must specify a constant constructor of the
   * superclass of the immediately enclosing class or a compile-time error occurs.
   */
  CONST_CONSTRUCTOR_WITH_NON_CONST_SUPER(
      "Constant constructor cannot call non-constant super constructor of '%s'"),

  /**
   * 7.6.3 Constant Constructors: It is a compile-time error if a constant constructor is declared
   * by a class that has a non-final instance variable.
   * <p>
   * The above refers to both locally declared and inherited instance variables.
   */
  CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD(
      "Cannot define the 'const' constructor for a class with non-final fields"),

  /**
   * 12.12.2 Const: It is a compile-time error if <i>T</i> is a deferred type.
   */
  CONST_DEFERRED_CLASS("Deferred classes cannot be created with 'const'"),

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
   * 5 Variables: A constant variable must be initialized to a compile-time constant or a
   * compile-time error occurs.
   * <p>
   * 12.1 Constants: A qualified reference to a static constant variable that is not qualified by a
   * deferred prefix.
   */
  CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE_FROM_DEFERRED_LIBRARY(
      "Constant values from a deferred library cannot be used to initialized a 'const' variable"),

  /**
   * 7.5 Instance Variables: It is a compile-time error if an instance variable is declared to be
   * constant.
   */
  CONST_INSTANCE_FIELD("Only static fields can be declared as 'const'"),

  /**
   * 12.8 Maps: It is a compile-time error if the key of an entry in a constant map literal is an
   * instance of a class that implements the operator <i>==</i> unless the key is a string or
   * integer.
   */
  CONST_MAP_KEY_EXPRESSION_TYPE_IMPLEMENTS_EQUALS(
      "The constant map entry key expression type '%s' cannot override the == operator"),

  /**
   * 5 Variables: A constant variable must be initialized to a compile-time constant (12.1) or a
   * compile-time error occurs.
   * 
   * @param name the name of the uninitialized final variable
   */
  CONST_NOT_INITIALIZED("The const variable '%s' must be initialized"),

  /**
   * 12.11.2 Const: An expression of one of the forms !e, e1 && e2 or e1 || e2, where e, e1 and e2
   * are constant expressions that evaluate to a boolean value.
   */
  CONST_EVAL_TYPE_BOOL("An expression of type 'bool' was expected"),

  /**
   * 12.11.2 Const: An expression of one of the forms e1 == e2 or e1 != e2 where e1 and e2 are
   * constant expressions that evaluate to a numeric, string or boolean value or to null.
   */
  CONST_EVAL_TYPE_BOOL_NUM_STRING(
      "An expression of type 'bool', 'num', 'String' or 'null' was expected"),

  /**
   * 12.11.2 Const: An expression of one of the forms ~e, e1 ^ e2, e1 & e2, e1 | e2, e1 >> e2 or e1
   * << e2, where e, e1 and e2 are constant expressions that evaluate to an integer value or to
   * null.
   */
  CONST_EVAL_TYPE_INT("An expression of type 'int' was expected"),

  /**
   * 12.11.2 Const: An expression of one of the forms e, e1 + e2, e1 - e2, e1 * e2, e1 / e2, e1 ~/
   * e2, e1 > e2, e1 < e2, e1 >= e2, e1 <= e2 or e1 % e2, where e, e1 and e2 are constant
   * expressions that evaluate to a numeric value or to null..
   */
  CONST_EVAL_TYPE_NUM("An expression of type 'num' was expected"),

  /**
   * 12.11.2 Const: It is a compile-time error if evaluation of a constant object results in an
   * uncaught exception being thrown.
   */
  CONST_EVAL_THROWS_EXCEPTION("Evaluation of this constant expression causes exception"),

  /**
   * 12.11.2 Const: It is a compile-time error if evaluation of a constant object results in an
   * uncaught exception being thrown.
   */
  CONST_EVAL_THROWS_IDBZE(
      "Evaluation of this constant expression throws IntegerDivisionByZeroException"),

  /**
   * 12.11.2 Const: If <i>T</i> is a parameterized type <i>S&lt;U<sub>1</sub>, &hellip;,
   * U<sub>m</sub>&gt;</i>, let <i>R = S</i>; It is a compile time error if <i>S</i> is not a
   * generic type with <i>m</i> type parameters.
   * 
   * @param typeName the name of the type being referenced (<i>S</i>)
   * @param parameterCount the number of type parameters that were declared
   * @param argumentCount the number of type arguments provided
   * @see CompileTimeErrorCode#NEW_WITH_INVALID_TYPE_PARAMETERS
   * @see StaticTypeWarningCode#WRONG_NUMBER_OF_TYPE_ARGUMENTS
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
  CONST_WITH_NON_CONSTANT_ARGUMENT("Arguments of a constant creation must be constant expressions"),

  /**
   * 12.11.2 Const: It is a compile-time error if <i>T</i> is not a class accessible in the current
   * scope, optionally followed by type arguments.
   * <p>
   * 12.11.2 Const: If <i>e</i> is of the form <i>const T.id(a<sub>1</sub>, &hellip;, a<sub>n</sub>,
   * x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip; x<sub>n+k</sub>: a<sub>n+k</sub>)</i> it is a
   * compile-time error if <i>T</i> is not a class accessible in the current scope, optionally
   * followed by type arguments.
   * 
   * @param name the name of the non-type element
   */
  CONST_WITH_NON_TYPE("The name '%s' is not a class"),

  /**
   * 12.11.2 Const: It is a compile-time error if <i>T</i> includes any type parameters.
   */
  CONST_WITH_TYPE_PARAMETERS("The constant creation cannot use a type parameter"),

  /**
   * 12.11.2 Const: It is a compile-time error if <i>T.id</i> is not the name of a constant
   * constructor declared by the type <i>T</i>.
   * 
   * @param typeName the name of the type
   * @param constructorName the name of the requested constant constructor
   */
  CONST_WITH_UNDEFINED_CONSTRUCTOR("The class '%s' does not have a constant constructor '%s'"),

  /**
   * 12.11.2 Const: It is a compile-time error if <i>T.id</i> is not the name of a constant
   * constructor declared by the type <i>T</i>.
   * 
   * @param typeName the name of the type
   */
  CONST_WITH_UNDEFINED_CONSTRUCTOR_DEFAULT(
      "The class '%s' does not have a default constant constructor"),

  /**
   * 15.3.1 Typedef: It is a compile-time error if any default values are specified in the signature
   * of a function type alias.
   */
  DEFAULT_VALUE_IN_FUNCTION_TYPE_ALIAS("Default values aren't allowed in typedefs"),

  /**
   * 6.2.1 Required Formals: By means of a function signature that names the parameter and describes
   * its type as a function type. It is a compile-time error if any default values are specified in
   * the signature of such a function type.
   */
  DEFAULT_VALUE_IN_FUNCTION_TYPED_PARAMETER(
      "Default values aren't allowed in function type parameters"),

  /**
   * 7.6.2 Factories: It is a compile-time error if <i>k</i> explicitly specifies a default value
   * for an optional parameter.
   */
  DEFAULT_VALUE_IN_REDIRECTING_FACTORY_CONSTRUCTOR(
      "Default values aren't allowed in factory constructors that redirect to another constructor"),

  /**
   * 3.1 Scoping: It is a compile-time error if there is more than one entity with the same name
   * declared in the same scope.
   */
  DUPLICATE_CONSTRUCTOR_DEFAULT("The default constructor is already defined"),

  /**
   * 3.1 Scoping: It is a compile-time error if there is more than one entity with the same name
   * declared in the same scope.
   * 
   * @param duplicateName the name of the duplicate entity
   */
  DUPLICATE_CONSTRUCTOR_NAME("The constructor with name '%s' is already defined"),

  /**
   * 3.1 Scoping: It is a compile-time error if there is more than one entity with the same name
   * declared in the same scope.
   * <p>
   * 7 Classes: It is a compile-time error if a class declares two members of the same name.
   * <p>
   * 7 Classes: It is a compile-time error if a class has an instance member and a static member
   * with the same name.
   * 
   * @param duplicateName the name of the duplicate entity
   */
  DUPLICATE_DEFINITION("The name '%s' is already defined"),

  /**
   * 7. Classes: It is a compile-time error if a class has an instance member and a static member
   * with the same name.
   * <p>
   * This covers the additional duplicate definition cases where inheritance has to be considered.
   * 
   * @param className the name of the class that has conflicting instance/static members
   * @param name the name of the conflicting members
   * @see #DUPLICATE_DEFINITION
   */
  DUPLICATE_DEFINITION_INHERITANCE("The name '%s' is already defined in '%s'"),

  /**
   * 12.14.2 Binding Actuals to Formals: It is a compile-time error if <i>q<sub>i</sub> =
   * q<sub>j</sub></i> for any <i>i != j</i> [where <i>q<sub>i</sub></i> is the label for a named
   * argument].
   */
  DUPLICATE_NAMED_ARGUMENT("The argument for the named parameter '%s' was already specified"),

  /**
   * SDK implementation libraries can be exported only by other SDK libraries.
   * 
   * @param uri the uri pointing to a library
   */
  EXPORT_INTERNAL_LIBRARY("The library '%s' is internal and cannot be exported"),

  /**
   * 14.2 Exports: It is a compile-time error if the compilation unit found at the specified URI is
   * not a library declaration.
   * 
   * @param uri the uri pointing to a non-library declaration
   */
  EXPORT_OF_NON_LIBRARY("The exported library '%s' must not have a part-of directive"),

  /**
   * Enum proposal: It is a compile-time error to subclass, mix-in or implement an enum.
   */
  EXTENDS_ENUM("Classes cannot extend an enum"),

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
   * 7.9 Superclasses: It is a compile-time error if the extends clause of a class <i>C</i> includes
   * a deferred type expression.
   * 
   * @param typeName the name of the type that cannot be extended
   * @see #IMPLEMENTS_DEFERRED_CLASS
   * @see #MIXIN_DEFERRED_CLASS
   */
  EXTENDS_DEFERRED_CLASS("This class cannot extend the deferred class '%s'"),

  /**
   * 12.14.2 Binding Actuals to Formals: It is a static warning if <i>m &lt; h</i> or if <i>m &gt;
   * n</i>.
   * <p>
   * 12.11.2 Const: It is a compile-time error if evaluation of a constant object results in an
   * uncaught exception being thrown.
   * 
   * @param requiredCount the maximum number of positional arguments
   * @param argumentCount the actual number of positional arguments given
   */
  EXTRA_POSITIONAL_ARGUMENTS("%d positional arguments expected, but %d found"),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. It is a compile time
   * error if more than one initializer corresponding to a given instance variable appears in
   * <i>k</i>'s list.
   */
  FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS(
      "The field '%s' cannot be initialized twice in the same constructor"),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. It is a compile time
   * error if <i>k</i>'s initializer list contains an initializer for a variable that is initialized
   * by means of an initializing formal of <i>k</i>.
   */
  FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER(
      "Fields cannot be initialized in both the parameter list and the initializers"),

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
   * 7.6.1 Generative Constructors: It is a compile-time error if an initializing formal is used by
   * a function other than a non-redirecting generative constructor.
   */
  FIELD_INITIALIZER_FACTORY_CONSTRUCTOR(
      "Initializing formal fields cannot be used in factory constructors"),

  /**
   * 7.6.1 Generative Constructors: It is a compile-time error if an initializing formal is used by
   * a function other than a non-redirecting generative constructor.
   */
  FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR(
      "Initializing formal fields can only be used in constructors"),

  /**
   * 7.6.1 Generative Constructors: A generative constructor may be redirecting, in which case its
   * only action is to invoke another generative constructor.
   * <p>
   * 7.6.1 Generative Constructors: It is a compile-time error if an initializing formal is used by
   * a function other than a non-redirecting generative constructor.
   */
  FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR(
      "The redirecting constructor cannot have a field initializer"),

  /**
   * 7.2 Getters: It is a compile-time error if a class has both a getter and a method with the same
   * name.
   * 
   * @param name the conflicting name of the getter and method
   */
  GETTER_AND_METHOD_WITH_SAME_NAME(
      "'%s' cannot be used to name a getter, there is already a method with the same name"),

  /**
   * 7.10 Superinterfaces: It is a compile-time error if the implements clause of a class <i>C</i>
   * specifies a malformed type or deferred type as a superinterface.
   * 
   * @param typeName the name of the type that cannot be extended
   * @see #EXTENDS_DEFERRED_CLASS
   * @see #MIXIN_DEFERRED_CLASS
   */
  IMPLEMENTS_DEFERRED_CLASS("This class cannot implement the deferred class '%s'"),

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
   * 7.10 Superinterfaces: It is a compile-time error if the implements clause of a class includes
   * type dynamic.
   */
  IMPLEMENTS_DYNAMIC("Classes cannot implement 'dynamic'"),

  /**
   * Enum proposal: It is a compile-time error to subclass, mix-in or implement an enum.
   */
  IMPLEMENTS_ENUM("Classes cannot implement an enum"),

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
   * @param className the name of the class that is implemented more than once
   */
  IMPLEMENTS_REPEATED("'%s' can only be implemented once"),

  /**
   * 7.10 Superinterfaces: It is a compile-time error if the superclass of a class <i>C</i> appears
   * in the implements clause of <i>C</i>.
   * 
   * @param className the name of the class that appears in both "extends" and "implements" clauses
   */
  IMPLEMENTS_SUPER_CLASS("'%s' cannot be used in both 'extends' and 'implements' clauses"),

  /**
   * 7.6.1 Generative Constructors: Note that <b>this</b> is not in scope on the right hand side of
   * an initializer.
   * <p>
   * 12.10 This: It is a compile-time error if this appears in a top-level function or variable
   * initializer, in a factory constructor, or in a static method or variable initializer, or in the
   * initializer of an instance variable.
   * 
   * @param name the name of the type in question
   */
  IMPLICIT_THIS_REFERENCE_IN_INITIALIZER("Only static members can be accessed in initializers"),

  /**
   * SDK implementation libraries can be imported only by other SDK libraries.
   * 
   * @param uri the uri pointing to a library
   */
  IMPORT_INTERNAL_LIBRARY("The library '%s' is internal and cannot be imported"),

  /**
   * 14.1 Imports: It is a compile-time error if the specified URI of an immediate import does not
   * refer to a library declaration.
   * 
   * @param uri the uri pointing to a non-library declaration
   * @see StaticWarningCode#IMPORT_OF_NON_LIBRARY
   */
  IMPORT_OF_NON_LIBRARY("The imported library '%s' must not have a part-of directive"),

  /**
   * 13.9 Switch: It is a compile-time error if values of the expressions <i>e<sub>k</sub></i> are
   * not instances of the same class <i>C</i>, for all <i>1 &lt;= k &lt;= n</i>.
   * 
   * @param expressionSource the expression source code that is the unexpected type
   * @param expectedType the name of the expected type
   */
  INCONSISTENT_CASE_EXPRESSION_TYPES(
      "Case expressions must have the same types, '%s' is not a '%s'"),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. It is a compile-time
   * error if <i>k</i>'s initializer list contains an initializer for a variable that is not an
   * instance variable declared in the immediately surrounding class.
   * 
   * @param id the name of the initializing formal that is not an instance variable in the
   *          immediately enclosing class
   * @see #INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD
   */
  INITIALIZER_FOR_NON_EXISTANT_FIELD("'%s' is not a variable in the enclosing class"),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. It is a compile-time
   * error if <i>k</i>'s initializer list contains an initializer for a variable that is not an
   * instance variable declared in the immediately surrounding class.
   * 
   * @param id the name of the initializing formal that is a static variable in the immediately
   *          enclosing class
   * @see #INITIALIZING_FORMAL_FOR_STATIC_FIELD
   */
  INITIALIZER_FOR_STATIC_FIELD(
      "'%s' is a static variable in the enclosing class, variables initialized in a constructor cannot be static"),

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
   * @see #INITIALIZER_FOR_STATIC_FIELD
   */
  INITIALIZING_FORMAL_FOR_STATIC_FIELD(
      "'%s' is a static field in the enclosing class, fields initialized in a constructor cannot be static"),

  /**
   * 12.30 Identifier Reference: Otherwise, e is equivalent to the property extraction
   * <b>this</b>.<i>id</i>.
   */
  INSTANCE_MEMBER_ACCESS_FROM_FACTORY(
      "Instance members cannot be accessed from a factory constructor"),

  /**
   * 12.30 Identifier Reference: Otherwise, e is equivalent to the property extraction
   * <b>this</b>.<i>id</i>.
   */
  INSTANCE_MEMBER_ACCESS_FROM_STATIC("Instance members cannot be accessed from a static method"),

  /**
   * Enum proposal: It is also a compile-time error to explicitly instantiate an enum via 'new' or
   * 'const' or to access its private fields.
   */
  INSTANTIATE_ENUM("Enums cannot be instantiated"),

  /**
   * 11 Metadata: Metadata consists of a series of annotations, each of which begin with the
   * character @, followed by a constant expression that must be either a reference to a
   * compile-time constant variable, or a call to a constant constructor.
   */
  INVALID_ANNOTATION("Annotation can be only constant variable or constant constructor invocation"),

  /**
   * 11 Metadata: Metadata consists of a series of annotations, each of which begin with the
   * character @, followed by a constant expression that must be either a reference to a
   * compile-time constant variable, or a call to a constant constructor.
   * <p>
   * 12.1 Constants: A qualified reference to a static constant variable that is not qualified by a
   * deferred prefix.
   */
  INVALID_ANNOTATION_FROM_DEFERRED_LIBRARY(
      "Constant values from a deferred library cannot be used as annotations"),

  /**
   * 15.31 Identifier Reference: It is a compile-time error if any of the identifiers async, await
   * or yield is used as an identifier in a function body marked with either async, async* or sync*.
   */
  INVALID_IDENTIFIER_IN_ASYNC(
      "The identifier '%s' cannot be used in a function marked with async, async* or sync*"),

  /**
   * 9. Functions: It is a compile-time error if an async, async* or sync* modifier is attached to
   * the body of a setter or constructor.
   */
  INVALID_MODIFIER_ON_CONSTRUCTOR(
      "The modifier '%s' cannot be applied to the body of a constructor"),

  /**
   * 9. Functions: It is a compile-time error if an async, async* or sync* modifier is attached to
   * the body of a setter or constructor.
   */
  INVALID_MODIFIER_ON_SETTER("The modifier '%s' cannot be applied to the body of a setter"),

  /**
   * TODO(brianwilkerson) Remove this when we have decided on how to report errors in compile-time
   * constants. Until then, this acts as a placeholder for more informative errors.
   * <p>
   * See TODOs in ConstantVisitor
   */
  INVALID_CONSTANT("Invalid constant value"),

  /**
   * 7.6 Constructors: It is a compile-time error if the name of a constructor is not a constructor
   * name.
   */
  INVALID_CONSTRUCTOR_NAME("Invalid constructor name"),

  /**
   * 7.6.2 Factories: It is a compile-time error if <i>M</i> is not the name of the immediately
   * enclosing class.
   */
  INVALID_FACTORY_NAME_NOT_A_CLASS("The name of the immediately enclosing class expected"),

  /**
   * 12.10 This: It is a compile-time error if this appears in a top-level function or variable
   * initializer, in a factory constructor, or in a static method or variable initializer, or in the
   * initializer of an instance variable.
   */
  INVALID_REFERENCE_TO_THIS("Invalid reference to 'this' expression"),

  /**
   * 12.6 Lists: It is a compile time error if the type argument of a constant list literal includes
   * a type parameter.
   * 
   * @name the name of the type parameter
   */
  INVALID_TYPE_ARGUMENT_IN_CONST_LIST(
      "Constant list literals cannot include a type parameter as a type argument, such as '%s'"),

  /**
   * 12.7 Maps: It is a compile time error if the type arguments of a constant map literal include a
   * type parameter.
   * 
   * @name the name of the type parameter
   */
  INVALID_TYPE_ARGUMENT_IN_CONST_MAP(
      "Constant map literals cannot include a type parameter as a type argument, such as '%s'"),

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
   * @param uri the URI that is invalid
   * @see #URI_DOES_NOT_EXIST
   */
  INVALID_URI("Invalid URI syntax: '%s'"),

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
   * 12.6 Lists: A run-time list literal &lt;<i>E</i>&gt; [<i>e<sub>1</sub></i> ...
   * <i>e<sub>n</sub></i>] is evaluated as follows:
   * <ul>
   * <li>The operator []= is invoked on <i>a</i> with first argument <i>i</i> and second argument
   * <i>o<sub>i+1</sub></i><i>, 1 &lt;= i &lt;= n</i></li>
   * </ul>
   * <p>
   * 12.14.2 Binding Actuals to Formals: Let <i>T<sub>i</sub></i> be the static type of
   * <i>a<sub>i</sub></i>, let <i>S<sub>i</sub></i> be the type of <i>p<sub>i</sub>, 1 &lt;= i &lt;=
   * n+k</i> and let <i>S<sub>q</sub></i> be the type of the named parameter <i>q</i> of <i>f</i>.
   * It is a static warning if <i>T<sub>j</sub></i> may not be assigned to <i>S<sub>j</sub>, 1 &lt;=
   * j &lt;= m</i>.
   */
  LIST_ELEMENT_TYPE_NOT_ASSIGNABLE("The element type '%s' cannot be assigned to the list type '%s'"),

  /**
   * 12.7 Map: A run-time map literal &lt;<i>K</i>, <i>V</i>&gt; [<i>k<sub>1</sub></i> :
   * <i>e<sub>1</sub></i> ... <i>k<sub>n</sub></i> : <i>e<sub>n</sub></i>] is evaluated as follows:
   * <ul>
   * <li>The operator []= is invoked on <i>m</i> with first argument <i>k<sub>i</sub></i> and second
   * argument <i>e<sub>i</sub></i><i>, 1 &lt;= i &lt;= n</i></li>
   * </ul>
   * <p>
   * 12.14.2 Binding Actuals to Formals: Let <i>T<sub>i</sub></i> be the static type of
   * <i>a<sub>i</sub></i>, let <i>S<sub>i</sub></i> be the type of <i>p<sub>i</sub>, 1 &lt;= i &lt;=
   * n+k</i> and let <i>S<sub>q</sub></i> be the type of the named parameter <i>q</i> of <i>f</i>.
   * It is a static warning if <i>T<sub>j</sub></i> may not be assigned to <i>S<sub>j</sub>, 1 &lt;=
   * j &lt;= m</i>.
   */
  MAP_KEY_TYPE_NOT_ASSIGNABLE("The element type '%s' cannot be assigned to the map key type '%s'"),

  /**
   * 12.7 Map: A run-time map literal &lt;<i>K</i>, <i>V</i>&gt; [<i>k<sub>1</sub></i> :
   * <i>e<sub>1</sub></i> ... <i>k<sub>n</sub></i> : <i>e<sub>n</sub></i>] is evaluated as follows:
   * <ul>
   * <li>The operator []= is invoked on <i>m</i> with first argument <i>k<sub>i</sub></i> and second
   * argument <i>e<sub>i</sub></i><i>, 1 &lt;= i &lt;= n</i></li>
   * </ul>
   * <p>
   * 12.14.2 Binding Actuals to Formals: Let <i>T<sub>i</sub></i> be the static type of
   * <i>a<sub>i</sub></i>, let <i>S<sub>i</sub></i> be the type of <i>p<sub>i</sub>, 1 &lt;= i &lt;=
   * n+k</i> and let <i>S<sub>q</sub></i> be the type of the named parameter <i>q</i> of <i>f</i>.
   * It is a static warning if <i>T<sub>j</sub></i> may not be assigned to <i>S<sub>j</sub>, 1 &lt;=
   * j &lt;= m</i>.
   */
  MAP_VALUE_TYPE_NOT_ASSIGNABLE(
      "The element type '%s' cannot be assigned to the map value type '%s'"),

  /**
   * 7 Classes: It is a compile time error if a class <i>C</i> declares a member with the same name
   * as <i>C</i>.
   */
  MEMBER_WITH_CLASS_NAME("Class members cannot have the same name as the enclosing class"),

  /**
   * 7.2 Getters: It is a compile-time error if a class has both a getter and a method with the same
   * name.
   * 
   * @param name the conflicting name of the getter and method
   */
  METHOD_AND_GETTER_WITH_SAME_NAME(
      "'%s' cannot be used to name a method, there is already a getter with the same name"),

  /**
   * 12.1 Constants: A constant expression is ... a constant list literal.
   */
  MISSING_CONST_IN_LIST_LITERAL(
      "List literals must be prefixed with 'const' when used as a constant expression"),

  /**
   * 12.1 Constants: A constant expression is ... a constant map literal.
   */
  MISSING_CONST_IN_MAP_LITERAL(
      "Map literals must be prefixed with 'const' when used as a constant expression"),

  /**
   * Enum proposal: It is a static warning if all of the following conditions hold:
   * <ul>
   * <li>The switch statement does not have a 'default' clause.</li>
   * <li>The static type of <i>e</i> is an enumerated typed with elements <i>id<sub>1</sub></i>,
   * &hellip;, <i>id<sub>n</sub></i>.</li>
   * <li>The sets {<i>e<sub>1</sub></i>, &hellip;, <i>e<sub>k</sub></i>} and {<i>id<sub>1</sub></i>,
   * &hellip;, <i>id<sub>n</sub></i>} are not the same.</li>
   * </ul>
   * 
   * @param constantName the name of the constant that is missing
   */
  MISSING_ENUM_CONSTANT_IN_SWITCH("Missing case clause for '%s'",
      "Add a case clause for the missing constant or add a default clause."),

  /**
   * 9 Mixins: It is a compile-time error if a declared or derived mixin explicitly declares a
   * constructor.
   * 
   * @param typeName the name of the mixin that is invalid
   */
  MIXIN_DECLARES_CONSTRUCTOR(
      "The class '%s' cannot be used as a mixin because it declares a constructor"),

  /**
   * 9.1 Mixin Application: It is a compile-time error if the with clause of a mixin application
   * <i>C</i> includes a deferred type expression.
   * 
   * @param typeName the name of the type that cannot be extended
   * @see #EXTENDS_DEFERRED_CLASS
   * @see #IMPLEMENTS_DEFERRED_CLASS
   */
  MIXIN_DEFERRED_CLASS("This class cannot mixin the deferred class '%s'"),

  /**
   * 9 Mixins: It is a compile-time error if a mixin is derived from a class whose superclass is not
   * Object.
   * 
   * @param typeName the name of the mixin that is invalid
   */
  MIXIN_INHERITS_FROM_NOT_OBJECT(
      "The class '%s' cannot be used as a mixin because it extends a class other than Object"),

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
  MIXIN_OF_DISALLOWED_CLASS("Classes cannot mixin '%s'"),

  /**
   * Enum proposal: It is a compile-time error to subclass, mix-in or implement an enum.
   */
  MIXIN_OF_ENUM("Classes cannot mixin an enum"),

  /**
   * 9.1 Mixin Application: It is a compile-time error if <i>M</i> does not denote a class or mixin
   * available in the immediately enclosing scope.
   */
  MIXIN_OF_NON_CLASS("Classes can only mixin other classes"),

  /**
   * 9 Mixins: It is a compile-time error if a declared or derived mixin refers to super.
   */
  MIXIN_REFERENCES_SUPER("The class '%s' cannot be used as a mixin because it references 'super'"),

  /**
   * 9.1 Mixin Application: It is a compile-time error if <i>S</i> does not denote a class available
   * in the immediately enclosing scope.
   */
  MIXIN_WITH_NON_CLASS_SUPERCLASS("Mixin can only be applied to class"),

  /**
   * 7.6.1 Generative Constructors: A generative constructor may be redirecting, in which case its
   * only action is to invoke another generative constructor.
   */
  MULTIPLE_REDIRECTING_CONSTRUCTOR_INVOCATIONS(
      "Constructor may have at most one 'this' redirection"),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. Then <i>k</i> may
   * include at most one superinitializer in its initializer list or a compile time error occurs.
   */
  MULTIPLE_SUPER_INITIALIZERS("Constructor may have at most one 'super' initializer"),

  /**
   * 11 Metadata: Metadata consists of a series of annotations, each of which begin with the
   * character @, followed by a constant expression that must be either a reference to a
   * compile-time constant variable, or a call to a constant constructor.
   */
  NO_ANNOTATION_CONSTRUCTOR_ARGUMENTS("Annotation creation must have arguments"),

  /**
   * 7.6.1 Generative Constructors: If no superinitializer is provided, an implicit superinitializer
   * of the form <b>super</b>() is added at the end of <i>k</i>'s initializer list, unless the
   * enclosing class is class <i>Object</i>.
   * <p>
   * 7.6.1 Generative constructors. It is a compile-time error if class <i>S</i> does not declare a
   * generative constructor named <i>S</i> (respectively <i>S.id</i>)
   */
  NO_DEFAULT_SUPER_CONSTRUCTOR_EXPLICIT("The class '%s' does not have a default constructor"),

  /**
   * 7.6 Constructors: Iff no constructor is specified for a class <i>C</i>, it implicitly has a
   * default constructor C() : <b>super<b>() {}, unless <i>C</i> is class <i>Object</i>.
   * <p>
   * 7.6.1 Generative constructors. It is a compile-time error if class <i>S</i> does not declare a
   * generative constructor named <i>S</i> (respectively <i>S.id</i>)
   */
  NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT("The class '%s' does not have a default constructor"),

  /**
   * 13.2 Expression Statements: It is a compile-time error if a non-constant map literal that has
   * no explicit type arguments appears in a place where a statement is expected.
   */
  NON_CONST_MAP_AS_EXPRESSION_STATEMENT(
      "A non-constant map literal without type arguments cannot be used as an expression statement"),

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
   * 13.9 Switch: Given a switch statement of the form <i>switch (e) { label<sub>11</sub> &hellip;
   * label<sub>1j1</sub> case e<sub>1</sub>: s<sub>1</sub> &hellip; label<sub>n1</sub> &hellip;
   * label<sub>njn</sub> case e<sub>n</sub>: s<sub>n</sub> default: s<sub>n+1</sub>}</i> or the form
   * <i>switch (e) { label<sub>11</sub> &hellip; label<sub>1j1</sub> case e<sub>1</sub>:
   * s<sub>1</sub> &hellip; label<sub>n1</sub> &hellip; label<sub>njn</sub> case e<sub>n</sub>:
   * s<sub>n</sub>}</i>, it is a compile-time error if the expressions <i>e<sub>k</sub></i> are not
   * compile-time constants, for all <i>1 &lt;= k &lt;= n</i>.
   * <p>
   * 12.1 Constants: A qualified reference to a static constant variable that is not qualified by a
   * deferred prefix.
   */
  NON_CONSTANT_CASE_EXPRESSION_FROM_DEFERRED_LIBRARY(
      "Constant values from a deferred library cannot be used as a case expression"),

  /**
   * 6.2.2 Optional Formals: It is a compile-time error if the default value of an optional
   * parameter is not a compile-time constant.
   */
  NON_CONSTANT_DEFAULT_VALUE("Default values of an optional parameter must be constant"),

  /**
   * 6.2.2 Optional Formals: It is a compile-time error if the default value of an optional
   * parameter is not a compile-time constant.
   * <p>
   * 12.1 Constants: A qualified reference to a static constant variable that is not qualified by a
   * deferred prefix.
   */
  NON_CONSTANT_DEFAULT_VALUE_FROM_DEFERRED_LIBRARY(
      "Constant values from a deferred library cannot be used as a default parameter value"),

  /**
   * 12.6 Lists: It is a compile time error if an element of a constant list literal is not a
   * compile-time constant.
   */
  NON_CONSTANT_LIST_ELEMENT("'const' lists must have all constant values"),

  /**
   * 12.6 Lists: It is a compile time error if an element of a constant list literal is not a
   * compile-time constant.
   * <p>
   * 12.1 Constants: A qualified reference to a static constant variable that is not qualified by a
   * deferred prefix.
   */
  NON_CONSTANT_LIST_ELEMENT_FROM_DEFERRED_LIBRARY(
      "Constant values from a deferred library cannot be used as values in a 'const' list"),

  /**
   * 12.7 Maps: It is a compile time error if either a key or a value of an entry in a constant map
   * literal is not a compile-time constant.
   */
  NON_CONSTANT_MAP_KEY("The keys in a map must be constant"),

  /**
   * 12.7 Maps: It is a compile time error if either a key or a value of an entry in a constant map
   * literal is not a compile-time constant.
   * <p>
   * 12.1 Constants: A qualified reference to a static constant variable that is not qualified by a
   * deferred prefix.
   */
  NON_CONSTANT_MAP_KEY_FROM_DEFERRED_LIBRARY(
      "Constant values from a deferred library cannot be used as keys in a map"),

  /**
   * 12.7 Maps: It is a compile time error if either a key or a value of an entry in a constant map
   * literal is not a compile-time constant.
   */
  NON_CONSTANT_MAP_VALUE("The values in a 'const' map must be constant"),

  /**
   * 12.7 Maps: It is a compile time error if either a key or a value of an entry in a constant map
   * literal is not a compile-time constant.
   * <p>
   * 12.1 Constants: A qualified reference to a static constant variable that is not qualified by a
   * deferred prefix.
   */
  NON_CONSTANT_MAP_VALUE_FROM_DEFERRED_LIBRARY(
      "Constant values from a deferred library cannot be used as values in a 'const' map"),

  /**
   * 11 Metadata: Metadata consists of a series of annotations, each of which begin with the
   * character @, followed by a constant expression that must be either a reference to a
   * compile-time constant variable, or a call to a constant constructor.
   * <p>
   * "From deferred library" case is covered by
   * {@link CompileTimeErrorCode#INVALID_ANNOTATION_FROM_DEFERRED_LIBRARY}.
   */
  NON_CONSTANT_ANNOTATION_CONSTRUCTOR("Annotation creation can use only 'const' constructor"),

  /**
   * 7.6.3 Constant Constructors: Any expression that appears within the initializer list of a
   * constant constructor must be a potentially constant expression, or a compile-time error occurs.
   */
  NON_CONSTANT_VALUE_IN_INITIALIZER(
      "Initializer expressions in constant constructors must be constants"),

  /**
   * 7.6.3 Constant Constructors: Any expression that appears within the initializer list of a
   * constant constructor must be a potentially constant expression, or a compile-time error occurs.
   * <p>
   * 12.1 Constants: A qualified reference to a static constant variable that is not qualified by a
   * deferred prefix.
   */
  NON_CONSTANT_VALUE_IN_INITIALIZER_FROM_DEFERRED_LIBRARY(
      "Constant values from a deferred library cannot be used as constant initializers"),

  /**
   * 12.14.2 Binding Actuals to Formals: It is a static warning if <i>m < h</i> or if <i>m > n</i>.
   * <p>
   * 12.11.2 Const: It is a compile-time error if evaluation of a constant object results in an
   * uncaught exception being thrown.
   * 
   * @param requiredCount the expected number of required arguments
   * @param argumentCount the actual number of positional arguments given
   */
  NOT_ENOUGH_REQUIRED_ARGUMENTS("%d required argument(s) expected, but %d found"),

  /**
   * 7.6.1 Generative Constructors: Let <i>C</i> be the class in which the superinitializer appears
   * and let <i>S</i> be the superclass of <i>C</i>. Let <i>k</i> be a generative constructor. It is
   * a compile-time error if class <i>S</i> does not declare a generative constructor named <i>S</i>
   * (respectively <i>S.id</i>)
   */
  NON_GENERATIVE_CONSTRUCTOR("The generative constructor '%s' expected, but factory found"),

  /**
   * 7.9 Superclasses: It is a compile-time error to specify an extends clause for class Object.
   */
  // Low priority- Object is provided by the SDK
  OBJECT_CANNOT_EXTEND_ANOTHER_CLASS(""),

  /**
   * 7.1.1 Operators: It is a compile-time error to declare an optional parameter in an operator.
   */
  OPTIONAL_PARAMETER_IN_OPERATOR("Optional parameters are not allowed when defining an operator"),

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
  PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER(
      "The name '%s' is already used as an import prefix and cannot be used to name a top-level element"),

  /**
   * 6.2.2 Optional Formals: It is a compile-time error if the name of a named optional parameter
   * begins with an '_' character.
   */
  PRIVATE_OPTIONAL_PARAMETER("Named optional parameters cannot start with an underscore"),

  /**
   * 12.1 Constants: It is a compile-time error if the value of a compile-time constant expression
   * depends on itself.
   */
  // TODO (jwren) Implementation of this code relies on the cycle detection in DirectedGraph.
  // See Dart issue 8423
  RECURSIVE_COMPILE_TIME_CONSTANT(""),

  /**
   * 7.6.1 Generative Constructors: A generative constructor may be redirecting, in which case its
   * only action is to invoke another generative constructor.
   * <p>
   * TODO(scheglov) review this later, there are no explicit "it is a compile-time error" in
   * specification. But it was added to the co19 and there is same error for factories.
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=954
   */
  RECURSIVE_CONSTRUCTOR_REDIRECT("Cycle in redirecting generative constructors"),

  /**
   * 7.6.2 Factories: It is a compile-time error if a redirecting factory constructor redirects to
   * itself, either directly or indirectly via a sequence of redirections.
   */
  RECURSIVE_FACTORY_REDIRECT("Cycle in redirecting factory constructors"),

  /**
   * 7.10 Superinterfaces: It is a compile-time error if the interface of a class <i>C</i> is a
   * superinterface of itself.
   * <p>
   * 8.1 Superinterfaces: It is a compile-time error if an interface is a superinterface of itself.
   * <p>
   * 7.9 Superclasses: It is a compile-time error if a class <i>C</i> is a superclass of itself.
   * 
   * @param className the name of the class that implements itself recursively
   * @param strImplementsPath a string representation of the implements loop
   */
  RECURSIVE_INTERFACE_INHERITANCE("'%s' cannot be a superinterface of itself: %s"),

  /**
   * 7.10 Superinterfaces: It is a compile-time error if the interface of a class <i>C</i> is a
   * superinterface of itself.
   * <p>
   * 8.1 Superinterfaces: It is a compile-time error if an interface is a superinterface of itself.
   * <p>
   * 7.9 Superclasses: It is a compile-time error if a class <i>C</i> is a superclass of itself.
   * 
   * @param className the name of the class that implements itself recursively
   */
  RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_EXTENDS("'%s' cannot extend itself"),

  /**
   * 7.10 Superinterfaces: It is a compile-time error if the interface of a class <i>C</i> is a
   * superinterface of itself.
   * <p>
   * 8.1 Superinterfaces: It is a compile-time error if an interface is a superinterface of itself.
   * <p>
   * 7.9 Superclasses: It is a compile-time error if a class <i>C</i> is a superclass of itself.
   * 
   * @param className the name of the class that implements itself recursively
   */
  RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_IMPLEMENTS("'%s' cannot implement itself"),

  /**
   * 7.10 Superinterfaces: It is a compile-time error if the interface of a class <i>C</i> is a
   * superinterface of itself.
   * <p>
   * 8.1 Superinterfaces: It is a compile-time error if an interface is a superinterface of itself.
   * <p>
   * 7.9 Superclasses: It is a compile-time error if a class <i>C</i> is a superclass of itself.
   * 
   * @param className the name of the class that implements itself recursively
   */
  RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_WITH("'%s' cannot use itself as a mixin"),

  /**
   * 7.6.2 Factories: It is a compile-time error if <i>k</i> is prefixed with the const modifier but
   * <i>k'</i> is not a constant constructor.
   */
  REDIRECT_TO_MISSING_CONSTRUCTOR("The constructor '%s' could not be found in '%s'"),

  /**
   * 7.6.2 Factories: It is a compile-time error if <i>k</i> is prefixed with the const modifier but
   * <i>k'</i> is not a constant constructor.
   */
  REDIRECT_TO_NON_CLASS(
      "The name '%s' is not a type and cannot be used in a redirected constructor"),

  /**
   * 7.6.2 Factories: It is a compile-time error if <i>k</i> is prefixed with the const modifier but
   * <i>k'</i> is not a constant constructor.
   */
  REDIRECT_TO_NON_CONST_CONSTRUCTOR(
      "Constant factory constructor cannot delegate to a non-constant constructor"),

  /**
   * 7.6.1 Generative constructors: A generative constructor may be <i>redirecting</i>, in which
   * case its only action is to invoke another generative constructor.
   */
  REDIRECT_GENERATIVE_TO_MISSING_CONSTRUCTOR("The constructor '%s' could not be found in '%s'"),

  /**
   * 7.6.1 Generative constructors: A generative constructor may be <i>redirecting</i>, in which
   * case its only action is to invoke another generative constructor.
   */
  REDIRECT_GENERATIVE_TO_NON_GENERATIVE_CONSTRUCTOR(
      "Generative constructor cannot redirect to a factory constructor"),

  /**
   * 5 Variables: A local variable may only be referenced at a source code location that is after
   * its initializer, if any, is complete, or a compile-time error occurs.
   */
  REFERENCED_BEFORE_DECLARATION("Local variables cannot be referenced before they are declared"),

  /**
   * 12.8.1 Rethrow: It is a compile-time error if an expression of the form <i>rethrow;</i> is not
   * enclosed within a on-catch clause.
   */
  RETHROW_OUTSIDE_CATCH("rethrow must be inside of a catch clause"),

  /**
   * 13.12 Return: It is a compile-time error if a return statement of the form <i>return e;</i>
   * appears in a generative constructor.
   */
  RETURN_IN_GENERATIVE_CONSTRUCTOR("Constructors cannot return a value"),

  /**
   * 13.12 Return: It is a compile-time error if a return statement of the form <i>return e;</i>
   * appears in a generator function.
   */
  RETURN_IN_GENERATOR(
      "Cannot return a value from a generator function (one marked with either 'async*' or 'sync*')"),

  /**
   * 14.1 Imports: It is a compile-time error if a prefix used in a deferred import is used in
   * another import clause.
   */
  SHARED_DEFERRED_PREFIX(
      "The prefix of a deferred import cannot be used in other import directives"),

  /**
   * 12.15.4 Super Invocation: A super method invocation <i>i</i> has the form
   * <i>super.m(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;
   * x<sub>n+k</sub>: a<sub>n+k</sub>)</i>. It is a compile-time error if a super method invocation
   * occurs in a top-level function or variable initializer, in an instance variable initializer or
   * initializer list, in class Object, in a factory constructor, or in a static method or variable
   * initializer.
   */
  SUPER_IN_INVALID_CONTEXT("Invalid context for 'super' invocation"),

  /**
   * 7.6.1 Generative Constructors: A generative constructor may be redirecting, in which case its
   * only action is to invoke another generative constructor.
   */
  SUPER_IN_REDIRECTING_CONSTRUCTOR("The redirecting constructor cannot have a 'super' initializer"),

  /**
   * 7.6.1 Generative Constructors: Let <i>k</i> be a generative constructor. It is a compile-time
   * error if a generative constructor of class Object includes a superinitializer.
   */
  // Low priority- Object is provided by the SDK
  SUPER_INITIALIZER_IN_OBJECT(""),

  /**
   * 12.11 Instance Creation: It is a static type warning if any of the type arguments to a
   * constructor of a generic type <i>G</i> invoked by a new expression or a constant object
   * expression are not subtypes of the bounds of the corresponding formal type parameters of
   * <i>G</i>.
   * <p>
   * 12.11.1 New: If T is malformed a dynamic error occurs. In checked mode, if T is mal-bounded a
   * dynamic error occurs.
   * <p>
   * 12.1 Constants: It is a compile-time error if evaluation of a compile-time constant would raise
   * an exception.
   * 
   * @param boundedTypeName the name of the type used in the instance creation that should be
   *          limited by the bound as specified in the class declaration
   * @param boundingTypeName the name of the bounding type
   * @see StaticTypeWarningCode#TYPE_ARGUMENT_NOT_MATCHING_BOUNDS
   */
  TYPE_ARGUMENT_NOT_MATCHING_BOUNDS("'%s' does not extend '%s'"),

  /**
   * 15.3.1 Typedef: Any self reference, either directly, or recursively via another typedef, is a
   * compile time error.
   */
  TYPE_ALIAS_CANNOT_REFERENCE_ITSELF(
      "Type alias cannot reference itself directly or recursively via another typedef"),

  /**
   * 12.11.2 Const: It is a compile-time error if <i>T</i> is not a class accessible in the current
   * scope, optionally followed by type arguments.
   */
  UNDEFINED_CLASS("Undefined class '%s'"),

  /**
   * 7.6.1 Generative Constructors: Let <i>C</i> be the class in which the superinitializer appears
   * and let <i>S</i> be the superclass of <i>C</i>. Let <i>k</i> be a generative constructor. It is
   * a compile-time error if class <i>S</i> does not declare a generative constructor named <i>S</i>
   * (respectively <i>S.id</i>)
   */
  UNDEFINED_CONSTRUCTOR_IN_INITIALIZER("The class '%s' does not have a generative constructor '%s'"),

  /**
   * 7.6.1 Generative Constructors: Let <i>C</i> be the class in which the superinitializer appears
   * and let <i>S</i> be the superclass of <i>C</i>. Let <i>k</i> be a generative constructor. It is
   * a compile-time error if class <i>S</i> does not declare a generative constructor named <i>S</i>
   * (respectively <i>S.id</i>)
   */
  UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT(
      "The class '%s' does not have a default generative constructor"),

  /**
   * 12.14.2 Binding Actuals to Formals: Furthermore, each <i>q<sub>i</sub></i>, <i>1<=i<=l</i>,
   * must have a corresponding named parameter in the set {<i>p<sub>n+1</sub></i> ...
   * <i>p<sub>n+k</sub></i>} or a static warning occurs.
   * <p>
   * 12.11.2 Const: It is a compile-time error if evaluation of a constant object results in an
   * uncaught exception being thrown.
   * 
   * @param name the name of the requested named parameter
   */
  UNDEFINED_NAMED_PARAMETER("The named parameter '%s' is not defined"),

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
   * @param uri the URI pointing to a non-existent file
   * @see #INVALID_URI
   */
  URI_DOES_NOT_EXIST("Target of URI does not exist: '%s'"),

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
   * 
   * @param operatorName the name of the declared operator
   * @param expectedNumberOfParameters the number of parameters expected
   * @param actualNumberOfParameters the number of parameters found in the operator declaration
   */
  WRONG_NUMBER_OF_PARAMETERS_FOR_OPERATOR(
      "Operator '%s' should declare exactly %d parameter(s), but %d found"),

  /**
   * 7.1.1 Operators: It is a compile time error if the arity of the user-declared operator - is not
   * 0 or 1.
   * 
   * @param actualNumberOfParameters the number of parameters found in the operator declaration
   */
  WRONG_NUMBER_OF_PARAMETERS_FOR_OPERATOR_MINUS(
      "Operator '-' should declare 0 or 1 parameter, but %d found"),

  /**
   * 7.3 Setters: It is a compile-time error if a setter's formal parameter list does not include
   * exactly one required formal parameter <i>p</i>.
   */
  WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER("Setters should declare exactly one required parameter"),

  /**
   * ?? Yield: It is a compile-time error if a yield statement appears in a function that is not a
   * generator function.
   */
  YIELD_EACH_IN_NON_GENERATOR(
      "Yield-each statements must be in a generator function (one marked with either 'async*' or 'sync*')"),

  /**
   * ?? Yield: It is a compile-time error if a yield statement appears in a function that is not a
   * generator function.
   */
  YIELD_IN_NON_GENERATOR(
      "Yield statements must be in a generator function (one marked with either 'async*' or 'sync*')");

  /**
   * The template used to create the message to be displayed for this error.
   */
  private final String message;

  /**
   * The template used to create the correction to be displayed for this error, or {@code null} if
   * there is no correction information for this error.
   */
  public String correction;

  /**
   * Initialize a newly created error code to have the given message.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private CompileTimeErrorCode(String message) {
    this(message, null);
  }

  /**
   * Initialize a newly created error code to have the given message and correction.
   * 
   * @param message the template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private CompileTimeErrorCode(String message, String correction) {
    this.message = message;
    this.correction = correction;
  }

  @Override
  public String getCorrection() {
    return correction;
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
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
