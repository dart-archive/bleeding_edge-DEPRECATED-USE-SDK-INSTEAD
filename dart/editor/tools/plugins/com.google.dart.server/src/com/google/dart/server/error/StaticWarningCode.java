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
package com.google.dart.server.error;

/**
 * The enumeration {@code StaticWarningCode} defines the error codes used for static warnings. The
 * convention for this class is for the name of the error code to indicate the problem that caused
 * the error to be generated and for the error message to explain what is wrong and, when
 * appropriate, how the problem can be corrected.
 * 
 * @coverage dart.server.error
 */
public enum StaticWarningCode implements ErrorCode {
  /**
   * 14.1 Imports: If a name <i>N</i> is referenced by a library <i>L</i> and <i>N</i> is introduced
   * into the top level scope <i>L</i> by more than one import then:
   * <ol>
   * <li>A static warning occurs.
   * <li>If <i>N</i> is referenced as a function, getter or setter, a <i>NoSuchMethodError</i> is
   * raised.
   * <li>If <i>N</i> is referenced as a type, it is treated as a malformed type.
   * </ol>
   * 
   * @param ambiguousTypeName the name of the ambiguous type
   * @param firstLibraryName the name of the first library that the type is found
   * @param secondLibraryName the name of the second library that the type is found
   */
  AMBIGUOUS_IMPORT("The name '%s' is defined in the libraries %s"),

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
   * 
   * @param actualType the name of the actual argument type
   * @param expectedType the name of the expected type
   */
  ARGUMENT_TYPE_NOT_ASSIGNABLE(
      "The argument type '%s' cannot be assigned to the parameter type '%s'"),

  /**
   * 5 Variables: Attempting to assign to a final variable elsewhere will cause a NoSuchMethodError
   * to be thrown, because no setter is defined for it. The assignment will also give rise to a
   * static warning for the same reason.
   * <p>
   * A constant variable is always implicitly final.
   */
  ASSIGNMENT_TO_CONST("Constant variables cannot be assigned a value"),

  /**
   * 5 Variables: Attempting to assign to a final variable elsewhere will cause a NoSuchMethodError
   * to be thrown, because no setter is defined for it. The assignment will also give rise to a
   * static warning for the same reason.
   */
  ASSIGNMENT_TO_FINAL("'%s' cannot be used as a setter, it is final"),

  /**
   * 5 Variables: Attempting to assign to a final variable elsewhere will cause a NoSuchMethodError
   * to be thrown, because no setter is defined for it. The assignment will also give rise to a
   * static warning for the same reason.
   */
  ASSIGNMENT_TO_FINAL_NO_SETTER("No setter named '%s' in class '%s'"),

  /**
   * 12.18 Assignment: It is as static warning if an assignment of the form <i>v = e</i> occurs
   * inside a top level or static function (be it function, method, getter, or setter) or variable
   * initializer and there is neither a local variable declaration with name <i>v</i> nor setter
   * declaration with name <i>v=</i> in the lexical scope enclosing the assignment.
   */
  ASSIGNMENT_TO_FUNCTION("Functions cannot be assigned a value"),

  /**
   * 12.18 Assignment: Let <i>T</i> be the static type of <i>e<sub>1</sub></i>. It is a static type
   * warning if <i>T</i> does not have an accessible instance setter named <i>v=</i>.
   */
  ASSIGNMENT_TO_METHOD("Methods cannot be assigned a value"),

  /**
   * 13.9 Switch: It is a static warning if the last statement of the statement sequence
   * <i>s<sub>k</sub></i> is not a break, continue, return or throw statement.
   */
  CASE_BLOCK_NOT_TERMINATED(
      "The last statement of the 'case' should be 'break', 'continue', 'return' or 'throw'"),

  /**
   * 12.32 Type Cast: It is a static warning if <i>T</i> does not denote a type available in the
   * current lexical scope.
   */
  CAST_TO_NON_TYPE("The name '%s' is not a type and cannot be used in an 'as' expression"),

  /**
   * 7.4 Abstract Instance Members: It is a static warning if an abstract member is declared or
   * inherited in a concrete class.
   */
  CONCRETE_CLASS_WITH_ABSTRACT_MEMBER("'%s' must have a method body because '%s' is not abstract"),

  /**
   * 14.1 Imports: If a name <i>N</i> is referenced by a library <i>L</i> and <i>N</i> would be
   * introduced into the top level scope of <i>L</i> by an import from a library whose URI begins
   * with <i>dart:</i> and an import from a library whose URI does not begin with <i>dart:</i>:
   * <ul>
   * <li>The import from <i>dart:</i> is implicitly extended by a hide N clause.</li>
   * <li>A static warning is issued.</li>
   * </ul>
   * 
   * @param ambiguousName the ambiguous name
   * @param sdkLibraryName the name of the dart: library that the element is found
   * @param otherLibraryName the name of the non-dart: library that the element is found
   */
  CONFLICTING_DART_IMPORT("Element '%s' from SDK library '%s' is implicitly hidden by '%s'"),

  /**
   * 7.2 Getters: It is a static warning if a class <i>C</i> declares an instance getter named
   * <i>v</i> and an accessible static member named <i>v</i> or <i>v=</i> is declared in a
   * superclass of <i>C</i>.
   * 
   * @param superName the name of the super class declaring a static member
   */
  CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER(
      "Superclass '%s' declares static member with the same name"),

  /**
   * 7.1 Instance Methods: It is a static warning if a class <i>C</i> declares an instance method
   * named <i>n</i> and has a setter named <i>n=</i>.
   */
  CONFLICTING_INSTANCE_METHOD_SETTER(
      "Class '%s' declares instance method '%s', but also has a setter with the same name from '%s'"),

  /**
   * 7.1 Instance Methods: It is a static warning if a class <i>C</i> declares an instance method
   * named <i>n</i> and has a setter named <i>n=</i>.
   */
  CONFLICTING_INSTANCE_METHOD_SETTER2(
      "Class '%s' declares the setter '%s', but also has an instance method in the same class"),

  /**
   * 7.3 Setters: It is a static warning if a class <i>C</i> declares an instance setter named
   * <i>v=</i> and an accessible static member named <i>v=</i> or <i>v</i> is declared in a
   * superclass of <i>C</i>.
   * 
   * @param superName the name of the super class declaring a static member
   */
  CONFLICTING_INSTANCE_SETTER_AND_SUPERCLASS_MEMBER(
      "Superclass '%s' declares static member with the same name"),

  /**
   * 7.2 Getters: It is a static warning if a class declares a static getter named <i>v</i> and also
   * has a non-static setter named <i>v=</i>.
   */
  CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER(
      "Class '%s' declares non-static setter with the same name"),

  /**
   * 7.3 Setters: It is a static warning if a class declares a static setter named <i>v=</i> and
   * also has a non-static member named <i>v</i>.
   */
  CONFLICTING_STATIC_SETTER_AND_INSTANCE_MEMBER(
      "Class '%s' declares non-static member with the same name"),

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
   * 14.2 Exports: It is a static warning to export two different libraries with the same name.
   * 
   * @param uri1 the uri pointing to a first library
   * @param uri2 the uri pointing to a second library
   * @param name the shared name of the exported libraries
   */
  EXPORT_DUPLICATED_LIBRARY_NAME(
      "The exported libraries '%s' and '%s' should not have the same name '%s'"),

  /**
   * 12.14.2 Binding Actuals to Formals: It is a static warning if <i>m &lt; h</i> or if <i>m &gt;
   * n</i>.
   * 
   * @param requiredCount the maximum number of positional arguments
   * @param argumentCount the actual number of positional arguments given
   * @see #NOT_ENOUGH_REQUIRED_ARGUMENTS
   */
  EXTRA_POSITIONAL_ARGUMENTS("%d positional arguments expected, but %d found"),

  /**
   * 5. Variables: It is a static warning if a final instance variable that has been initialized at
   * its point of declaration is also initialized in a constructor.
   */
  FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION(
      "Values cannot be set in the constructor if they are final, and have already been set"),

  /**
   * 5. Variables: It is a static warning if a final instance variable that has been initialized at
   * its point of declaration is also initialized in a constructor.
   * 
   * @param name the name of the field in question
   */
  // TODO (jwren) only a subset of these are being caught
  FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR(
      "'%s' is final and was given a value when it was declared, so it cannot be set to a new value"),

  /**
   * 7.6.1 Generative Constructors: Execution of an initializer of the form <b>this</b>.<i>v</i> =
   * <i>e</i> proceeds as follows: First, the expression <i>e</i> is evaluated to an object
   * <i>o</i>. Then, the instance variable <i>v</i> of the object denoted by this is bound to
   * <i>o</i>.
   * <p>
   * 12.14.2 Binding Actuals to Formals: Let <i>T<sub>i</sub></i> be the static type of
   * <i>a<sub>i</sub></i>, let <i>S<sub>i</sub></i> be the type of <i>p<sub>i</sub>, 1 &lt;= i &lt;=
   * n+k</i> and let <i>S<sub>q</sub></i> be the type of the named parameter <i>q</i> of <i>f</i>.
   * It is a static warning if <i>T<sub>j</sub></i> may not be assigned to <i>S<sub>j</sub>, 1 &lt;=
   * j &lt;= m</i>.
   * 
   * @param initializerType the name of the type of the initializer expression
   * @param fieldType the name of the type of the field
   */
  FIELD_INITIALIZER_NOT_ASSIGNABLE(
      "The initializer type '%s' cannot be assigned to the field type '%s'"),

  /**
   * 7.6.1 Generative Constructors: An initializing formal has the form <i>this.id</i>. It is a
   * static warning if the static type of <i>id</i> is not assignable to <i>T<sub>id</sub></i>.
   * 
   * @param parameterType the name of the type of the field formal parameter
   * @param fieldType the name of the type of the field
   */
  FIELD_INITIALIZING_FORMAL_NOT_ASSIGNABLE(
      "The parameter type '%s' is incompatable with the field type '%s'"),

  /**
   * 5 Variables: It is a static warning if a library, static or local variable <i>v</i> is final
   * and <i>v</i> is not initialized at its point of declaration.
   * <p>
   * 7.6.1 Generative Constructors: Each final instance variable <i>f</i> declared in the
   * immediately enclosing class must have an initializer in <i>k</i>'s initializer list unless it
   * has already been initialized by one of the following means:
   * <ul>
   * <li>Initialization at the declaration of <i>f</i>.</li>
   * <li>Initialization by means of an initializing formal of <i>k</i>.</li>
   * </ul>
   * or a static warning occurs.
   * 
   * @param name the name of the uninitialized final variable
   */
  FINAL_NOT_INITIALIZED("The final variable '%s' must be initialized"),

  /**
   * 15.5 Function Types: It is a static warning if a concrete class implements Function and does
   * not have a concrete method named call().
   */
  FUNCTION_WITHOUT_CALL("Concrete classes that implement Function must implement the method call()"),

  /**
   * 14.1 Imports: It is a static warning to import two different libraries with the same name.
   * 
   * @param uri1 the uri pointing to a first library
   * @param uri2 the uri pointing to a second library
   * @param name the shared name of the imported libraries
   */
  IMPORT_DUPLICATED_LIBRARY_NAME(
      "The imported libraries '%s' and '%s' should not have the same name '%s'"),

  /**
   * 14.1 Imports: It is a static warning if the specified URI of a deferred import does not refer
   * to a library declaration.
   * 
   * @param uri the uri pointing to a non-library declaration
   * @see CompileTimeErrorCode#IMPORT_OF_NON_LIBRARY
   */
  IMPORT_OF_NON_LIBRARY("The imported library '%s' must not have a part-of directive"),

  /**
   * 8.1.1 Inheritance and Overriding: However, if the above rules would cause multiple members
   * <i>m<sub>1</sub>, &hellip;, m<sub>k</sub></i> with the same name <i>n</i> that would be
   * inherited (because identically named members existed in several superinterfaces) then at most
   * one member is inherited.
   * <p>
   * If some but not all of the <i>m<sub>i</sub>, 1 &lt;= i &lt;= k</i> are getters none of the
   * <i>m<sub>i</sub></i> are inherited, and a static warning is issued.
   */
  INCONSISTENT_METHOD_INHERITANCE_GETTER_AND_METHOD(
      "'%s' is inherited as a getter and also a method"),

  /**
   * 7.1 Instance Methods: It is a static warning if a class <i>C</i> declares an instance method
   * named <i>n</i> and an accessible static member named <i>n</i> is declared in a superclass of
   * <i>C</i>.
   * 
   * @param memberName the name of the member with the name conflict
   * @param superclassName the name of the enclosing class that has the static member
   */
  INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC(
      "'%s' collides with a static member in the superclass '%s'"),

  /**
   * 7.2 Getters: It is a static warning if a getter <i>m1</i> overrides a getter <i>m2</i> and the
   * type of <i>m1</i> is not a subtype of the type of <i>m2</i>.
   * 
   * @param actualReturnTypeName the name of the expected return type
   * @param expectedReturnType the name of the actual return type, not assignable to the
   *          actualReturnTypeName
   * @param className the name of the class where the overridden getter is declared
   * @see #INVALID_METHOD_OVERRIDE_RETURN_TYPE
   */
  INVALID_GETTER_OVERRIDE_RETURN_TYPE(
      "The return type '%s' is not assignable to '%s' as required by the getter it is overriding from '%s'"),

  /**
   * 7.1 Instance Methods: It is a static warning if an instance method <i>m1</i> overrides an
   * instance method <i>m2</i> and the type of <i>m1</i> is not a subtype of the type of <i>m2</i>.
   * 
   * @param actualParamTypeName the name of the expected parameter type
   * @param expectedParamType the name of the actual parameter type, not assignable to the
   *          actualParamTypeName
   * @param className the name of the class where the overridden method is declared
   */
  INVALID_METHOD_OVERRIDE_NAMED_PARAM_TYPE(
      "The parameter type '%s' is not assignable to '%s' as required by the method it is overriding from '%s'"),

  /**
   * 7.1 Instance Methods: It is a static warning if an instance method <i>m1</i> overrides an
   * instance method <i>m2</i> and the type of <i>m1</i> is not a subtype of the type of <i>m2</i>.
   * 
   * @param actualParamTypeName the name of the expected parameter type
   * @param expectedParamType the name of the actual parameter type, not assignable to the
   *          actualParamTypeName
   * @param className the name of the class where the overridden method is declared
   * @see #INVALID_SETTER_OVERRIDE_NORMAL_PARAM_TYPE
   */
  INVALID_METHOD_OVERRIDE_NORMAL_PARAM_TYPE(
      "The parameter type '%s' is not assignable to '%s' as required by the method it is overriding from '%s'"),

  /**
   * 7.1 Instance Methods: It is a static warning if an instance method <i>m1</i> overrides an
   * instance method <i>m2</i> and the type of <i>m1</i> is not a subtype of the type of <i>m2</i>.
   * 
   * @param actualParamTypeName the name of the expected parameter type
   * @param expectedParamType the name of the actual parameter type, not assignable to the
   *          actualParamTypeName
   * @param className the name of the class where the overridden method is declared
   */
  INVALID_METHOD_OVERRIDE_OPTIONAL_PARAM_TYPE(
      "The parameter type '%s' is not assignable to '%s' as required by the method it is overriding from '%s'"),

  /**
   * 7.1 Instance Methods: It is a static warning if an instance method <i>m1</i> overrides an
   * instance method <i>m2</i> and the type of <i>m1</i> is not a subtype of the type of <i>m2</i>.
   * 
   * @param actualReturnTypeName the name of the expected return type
   * @param expectedReturnType the name of the actual return type, not assignable to the
   *          actualReturnTypeName
   * @param className the name of the class where the overridden method is declared
   * @see #INVALID_GETTER_OVERRIDE_RETURN_TYPE
   */
  INVALID_METHOD_OVERRIDE_RETURN_TYPE(
      "The return type '%s' is not assignable to '%s' as required by the method it is overriding from '%s'"),

  /**
   * 7.1 Instance Methods: It is a static warning if an instance method <i>m1</i> overrides an
   * instance member <i>m2</i>, the signature of <i>m2</i> explicitly specifies a default value for
   * a formal parameter <i>p</i> and the signature of <i>m1</i> specifies a different default value
   * for <i>p</i>.
   */
  INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES_NAMED(
      "Parameters cannot override default values, this method overrides '%s.%s' where '%s' has a different value"),

  /**
   * 7.1 Instance Methods: It is a static warning if an instance method <i>m1</i> overrides an
   * instance member <i>m2</i>, the signature of <i>m2</i> explicitly specifies a default value for
   * a formal parameter <i>p</i> and the signature of <i>m1</i> specifies a different default value
   * for <i>p</i>.
   */
  INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES_POSITIONAL(
      "Parameters cannot override default values, this method overrides '%s.%s' where this positional parameter has a different value"),

  /**
   * 7.1 Instance Methods: It is a static warning if an instance method <i>m1</i> overrides an
   * instance member <i>m2</i> and <i>m1</i> does not declare all the named parameters declared by
   * <i>m2</i>.
   * 
   * @param paramCount the number of named parameters in the overridden member
   * @param className the name of the class from the overridden method
   */
  INVALID_OVERRIDE_NAMED(
      "Missing the named parameter '%s' to match the overridden method from '%s'"),

  /**
   * 7.1 Instance Methods: It is a static warning if an instance method <i>m1</i> overrides an
   * instance member <i>m2</i> and <i>m1</i> has fewer positional parameters than <i>m2</i>.
   * 
   * @param paramCount the number of positional parameters in the overridden member
   * @param className the name of the class from the overridden method
   */
  INVALID_OVERRIDE_POSITIONAL(
      "Must have at least %d parameters to match the overridden method from '%s'"),

  /**
   * 7.1 Instance Methods: It is a static warning if an instance method <i>m1</i> overrides an
   * instance member <i>m2</i> and <i>m1</i> has a greater number of required parameters than
   * <i>m2</i>.
   * 
   * @param paramCount the number of required parameters in the overridden member
   * @param className the name of the class from the overridden method
   */
  INVALID_OVERRIDE_REQUIRED(
      "Must have %d required parameters or less to match the overridden method from '%s'"),

  /**
   * 7.3 Setters: It is a static warning if a setter <i>m1</i> overrides a setter <i>m2</i> and the
   * type of <i>m1</i> is not a subtype of the type of <i>m2</i>.
   * 
   * @param actualParamTypeName the name of the expected parameter type
   * @param expectedParamType the name of the actual parameter type, not assignable to the
   *          actualParamTypeName
   * @param className the name of the class where the overridden setter is declared
   * @see #INVALID_METHOD_OVERRIDE_NORMAL_PARAM_TYPE
   */
  INVALID_SETTER_OVERRIDE_NORMAL_PARAM_TYPE(
      "The parameter type '%s' is not assignable to '%s' as required by the setter it is overriding from '%s'"),

  /**
   * 12.6 Lists: A run-time list literal &lt;<i>E</i>&gt; [<i>e<sub>1</sub></i> &hellip;
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
   * <i>e<sub>1</sub></i> &hellip; <i>k<sub>n</sub></i> : <i>e<sub>n</sub></i>] is evaluated as
   * follows:
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
   * <i>e<sub>1</sub></i> &hellip; <i>k<sub>n</sub></i> : <i>e<sub>n</sub></i>] is evaluated as
   * follows:
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
   * 7.3 Setters: It is a static warning if a class has a setter named <i>v=</i> with argument type
   * <i>T</i> and a getter named <i>v</i> with return type <i>S</i>, and <i>T</i> may not be
   * assigned to <i>S</i>.
   */
  MISMATCHED_GETTER_AND_SETTER_TYPES(
      "The parameter type for setter '%s' is '%s' which is not assignable to its getter (of type '%s')"),

  /**
   * 7.3 Setters: It is a static warning if a class has a setter named <i>v=</i> with argument type
   * <i>T</i> and a getter named <i>v</i> with return type <i>S</i>, and <i>T</i> may not be
   * assigned to <i>S</i>.
   */
  MISMATCHED_GETTER_AND_SETTER_TYPES_FROM_SUPERTYPE(
      "The parameter type for setter '%s' is '%s' which is not assignable to its getter (of type '%s'), from superclass '%s'"),

  /**
   * 13.12 Return: It is a static warning if a function contains both one or more return statements
   * of the form <i>return;</i> and one or more return statements of the form <i>return e;</i>.
   */
  MIXED_RETURN_TYPES("Methods and functions cannot use return both with and without values"),

  /**
   * 12.11.1 New: It is a static warning if <i>q</i> is a constructor of an abstract class and
   * <i>q</i> is not a factory constructor.
   */
  NEW_WITH_ABSTRACT_CLASS("Abstract classes cannot be created with a 'new' expression"),

  /**
   * 15.8 Parameterized Types: Any use of a malbounded type gives rise to a static warning.
   * 
   * @param typeName the name of the type being referenced (<i>S</i>)
   * @param parameterCount the number of type parameters that were declared
   * @param argumentCount the number of type arguments provided
   * @see CompileTimeErrorCode#CONST_WITH_INVALID_TYPE_PARAMETERS
   * @see StaticTypeWarningCode#WRONG_NUMBER_OF_TYPE_ARGUMENTS
   */
  NEW_WITH_INVALID_TYPE_PARAMETERS(
      "The type '%s' is declared with %d type parameters, but %d type arguments were given"),

  /**
   * 12.11.1 New: It is a static warning if <i>T</i> is not a class accessible in the current scope,
   * optionally followed by type arguments.
   * 
   * @param name the name of the non-type element
   */
  NEW_WITH_NON_TYPE("The name '%s' is not a class"),

  /**
   * 12.11.1 New: If <i>T</i> is a class or parameterized type accessible in the current scope then:
   * 1. If <i>e</i> is of the form <i>new T.id(a<sub>1</sub>, &hellip;, a<sub>n</sub>,
   * x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;, x<sub>n+k</sub>: a<sub>n+k</sub>)</i> it is a
   * static warning if <i>T.id</i> is not the name of a constructor declared by the type <i>T</i>.
   * If <i>e</i> of the form <i>new T(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>:
   * a<sub>n+1</sub>, &hellip;, x<sub>n+k</sub>: a<sub>n+kM/sub>)</i> it is a static warning if the
   * type <i>T</i> does not declare a constructor with the same name as the declaration of <i>T</i>.
   */
  NEW_WITH_UNDEFINED_CONSTRUCTOR("The class '%s' does not have a constructor '%s'"),

  /**
   * 12.11.1 New: If <i>T</i> is a class or parameterized type accessible in the current scope then:
   * 1. If <i>e</i> is of the form <i>new T.id(a<sub>1</sub>, &hellip;, a<sub>n</sub>,
   * x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;, x<sub>n+k</sub>: a<sub>n+k</sub>)</i> it is a
   * static warning if <i>T.id</i> is not the name of a constructor declared by the type <i>T</i>.
   * If <i>e</i> of the form <i>new T(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>:
   * a<sub>n+1</sub>, &hellip;, x<sub>n+k</sub>: a<sub>n+kM/sub>)</i> it is a static warning if the
   * type <i>T</i> does not declare a constructor with the same name as the declaration of <i>T</i>.
   */
  NEW_WITH_UNDEFINED_CONSTRUCTOR_DEFAULT("The class '%s' does not have a default constructor"),

  /**
   * 7.9.1 Inheritance and Overriding: It is a static warning if a non-abstract class inherits an
   * abstract method.
   * <p>
   * 7.10 Superinterfaces: Let <i>C</i> be a concrete class that does not declare its own
   * <i>noSuchMethod()</i> method. It is a static warning if the implicit interface of <i>C</i>
   * includes an instance member <i>m</i> of type <i>F</i> and <i>C</i> does not declare or inherit
   * a corresponding instance member <i>m</i> of type <i>F'</i> such that <i>F' <: F</i>.
   * <p>
   * 7.4 Abstract Instance Members: It is a static warning if an abstract member is declared or
   * inherited in a concrete class unless that member overrides a concrete one.
   * 
   * @param memberName the name of the first member
   * @param memberName the name of the second member
   * @param memberName the name of the third member
   * @param memberName the name of the fourth member
   * @param additionalCount the number of additional missing members that aren't listed
   */
  NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FIVE_PLUS(
      "Missing concrete implementation of %s, %s, %s, %s and %d more"),

  /**
   * 7.9.1 Inheritance and Overriding: It is a static warning if a non-abstract class inherits an
   * abstract method.
   * <p>
   * 7.10 Superinterfaces: Let <i>C</i> be a concrete class that does not declare its own
   * <i>noSuchMethod()</i> method. It is a static warning if the implicit interface of <i>C</i>
   * includes an instance member <i>m</i> of type <i>F</i> and <i>C</i> does not declare or inherit
   * a corresponding instance member <i>m</i> of type <i>F'</i> such that <i>F' <: F</i>.
   * <p>
   * 7.4 Abstract Instance Members: It is a static warning if an abstract member is declared or
   * inherited in a concrete class unless that member overrides a concrete one.
   * 
   * @param memberName the name of the first member
   * @param memberName the name of the second member
   * @param memberName the name of the third member
   * @param memberName the name of the fourth member
   */
  NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FOUR(
      "Missing concrete implementation of %s, %s, %s and %s"),

  /**
   * 7.9.1 Inheritance and Overriding: It is a static warning if a non-abstract class inherits an
   * abstract method.
   * <p>
   * 7.10 Superinterfaces: Let <i>C</i> be a concrete class that does not declare its own
   * <i>noSuchMethod()</i> method. It is a static warning if the implicit interface of <i>C</i>
   * includes an instance member <i>m</i> of type <i>F</i> and <i>C</i> does not declare or inherit
   * a corresponding instance member <i>m</i> of type <i>F'</i> such that <i>F' <: F</i>.
   * <p>
   * 7.4 Abstract Instance Members: It is a static warning if an abstract member is declared or
   * inherited in a concrete class unless that member overrides a concrete one.
   * 
   * @param memberName the name of the member
   */
  NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE("Missing concrete implementation of %s"),

  /**
   * 7.9.1 Inheritance and Overriding: It is a static warning if a non-abstract class inherits an
   * abstract method.
   * <p>
   * 7.10 Superinterfaces: Let <i>C</i> be a concrete class that does not declare its own
   * <i>noSuchMethod()</i> method. It is a static warning if the implicit interface of <i>C</i>
   * includes an instance member <i>m</i> of type <i>F</i> and <i>C</i> does not declare or inherit
   * a corresponding instance member <i>m</i> of type <i>F'</i> such that <i>F' <: F</i>.
   * <p>
   * 7.4 Abstract Instance Members: It is a static warning if an abstract member is declared or
   * inherited in a concrete class unless that member overrides a concrete one.
   * 
   * @param memberName the name of the first member
   * @param memberName the name of the second member
   * @param memberName the name of the third member
   */
  NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_THREE(
      "Missing concrete implementation of %s, %s and %s"),

  /**
   * 7.9.1 Inheritance and Overriding: It is a static warning if a non-abstract class inherits an
   * abstract method.
   * <p>
   * 7.10 Superinterfaces: Let <i>C</i> be a concrete class that does not declare its own
   * <i>noSuchMethod()</i> method. It is a static warning if the implicit interface of <i>C</i>
   * includes an instance member <i>m</i> of type <i>F</i> and <i>C</i> does not declare or inherit
   * a corresponding instance member <i>m</i> of type <i>F'</i> such that <i>F' <: F</i>.
   * <p>
   * 7.4 Abstract Instance Members: It is a static warning if an abstract member is declared or
   * inherited in a concrete class unless that member overrides a concrete one.
   * 
   * @param memberName the name of the first member
   * @param memberName the name of the second member
   */
  NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_TWO("Missing concrete implementation of %s and %s"),

  /**
   * 13.11 Try: An on-catch clause of the form <i>on T catch (p<sub>1</sub>, p<sub>2</sub>) s</i> or
   * <i>on T s</i> matches an object <i>o</i> if the type of <i>o</i> is a subtype of <i>T</i>. It
   * is a static warning if <i>T</i> does not denote a type available in the lexical scope of the
   * catch clause.
   * 
   * @param name the name of the non-type element
   */
  NON_TYPE_IN_CATCH_CLAUSE("The name '%s' is not a type and cannot be used in an on-catch clause"),

  /**
   * 7.1.1 Operators: It is a static warning if the return type of the user-declared operator []= is
   * explicitly declared and not void.
   */
  NON_VOID_RETURN_FOR_OPERATOR("The return type of the operator []= must be 'void'"),

  /**
   * 7.3 Setters: It is a static warning if a setter declares a return type other than void.
   */
  NON_VOID_RETURN_FOR_SETTER("The return type of the setter must be 'void'"),

  /**
   * 15.1 Static Types: A type <i>T</i> is malformed iff: <li><i>T</i> has the form <i>id</i> or the
   * form <i>prefix.id</i>, and in the enclosing lexical scope, the name <i>id</i> (respectively
   * <i>prefix.id</i>) does not denote a type.</li> <li><i>T</i> denotes a type parameter in the
   * enclosing lexical scope, but occurs in the signature or body of a static member.</li> <li>
   * <i>T</i> is a parameterized type of the form <i>G&lt;S<sub>1</sub>, .., S<sub>n</sub>&gt;</i>,
   * and <i>G</i> is malformed.</li></ul>
   * <p>
   * Any use of a malformed type gives rise to a static warning.
   * 
   * @param nonTypeName the name that is not a type
   */
  NOT_A_TYPE("%s is not a type"),

  /**
   * 12.14.2 Binding Actuals to Formals: It is a static warning if <i>m &lt; h</i> or if <i>m &gt;
   * n</i>.
   * 
   * @param requiredCount the expected number of required arguments
   * @param argumentCount the actual number of positional arguments given
   * @see #EXTRA_POSITIONAL_ARGUMENTS
   */
  NOT_ENOUGH_REQUIRED_ARGUMENTS("%d required argument(s) expected, but %d found"),

  /**
   * 14.3 Parts: It is a static warning if the referenced part declaration <i>p</i> names a library
   * other than the current library as the library to which <i>p</i> belongs.
   * 
   * @param expectedLibraryName the name of expected library name
   * @param actualLibraryName the non-matching actual library name from the "part of" declaration
   */
  PART_OF_DIFFERENT_LIBRARY("Expected this library to be part of '%s', not '%s'"),

  /**
   * 7.6.2 Factories: It is a static warning if the function type of <i>k'</i> is not a subtype of
   * the type of <i>k</i>.
   * 
   * @param redirectedName the name of the redirected constructor
   * @param redirectingName the name of the redirecting constructor
   */
  REDIRECT_TO_INVALID_FUNCTION_TYPE(
      "The redirected constructor '%s' has incompatible parameters with '%s'"),

  /**
   * 7.6.2 Factories: It is a static warning if the function type of <i>k'</i> is not a subtype of
   * the type of <i>k</i>.
   * 
   * @param redirectedName the name of the redirected constructor return type
   * @param redirectingName the name of the redirecting constructor return type
   */
  REDIRECT_TO_INVALID_RETURN_TYPE(
      "The return type '%s' of the redirected constructor is not assignable to '%s'"),

  /**
   * 7.6.2 Factories: It is a static warning if type does not denote a class accessible in the
   * current scope; if type does denote such a class <i>C</i> it is a static warning if the
   * referenced constructor (be it <i>type</i> or <i>type.id</i>) is not a constructor of <i>C</i>.
   */
  REDIRECT_TO_MISSING_CONSTRUCTOR("The constructor '%s' could not be found in '%s'"),

  /**
   * 7.6.2 Factories: It is a static warning if type does not denote a class accessible in the
   * current scope; if type does denote such a class <i>C</i> it is a static warning if the
   * referenced constructor (be it <i>type</i> or <i>type.id</i>) is not a constructor of <i>C</i>.
   */
  REDIRECT_TO_NON_CLASS(
      "The name '%s' is not a type and cannot be used in a redirected constructor"),

  /**
   * 13.12 Return: Let <i>f</i> be the function immediately enclosing a return statement of the form
   * <i>return;</i> It is a static warning if both of the following conditions hold:
   * <ol>
   * <li><i>f</i> is not a generative constructor.
   * <li>The return type of <i>f</i> may not be assigned to void.
   * </ol>
   */
  RETURN_WITHOUT_VALUE("Missing return value after 'return'"),

  /**
   * 12.16.3 Static Invocation: It is a static warning if <i>C</i> does not declare a static method
   * or getter <i>m</i>.
   * 
   * @param memberName the name of the instance member
   */
  STATIC_ACCESS_TO_INSTANCE_MEMBER("Instance member '%s' cannot be accessed using static access"),

  /**
   * 13.9 Switch: It is a static warning if the type of <i>e</i> may not be assigned to the type of
   * <i>e<sub>k</sub></i>.
   */
  SWITCH_EXPRESSION_NOT_ASSIGNABLE(
      "Type '%s' of the switch expression is not assignable to the type '%s' of case expressions"),

  /**
   * 15.1 Static Types: It is a static warning to use a deferred type in a type annotation.
   * 
   * @param name the name of the type that is deferred and being used in a type annotation
   */
  TYPE_ANNOTATION_DEFERRED_CLASS(
      "The deferred type '%s' cannot be used in a declaration, cast or type test"),

  /**
   * 12.31 Type Test: It is a static warning if <i>T</i> does not denote a type available in the
   * current lexical scope.
   */
  TYPE_TEST_NON_TYPE("The name '%s' is not a type and cannot be used in an 'is' expression"),

  /**
   * 10 Generics: However, a type parameter is considered to be a malformed type when referenced by
   * a static member.
   * <p>
   * 15.1 Static Types: Any use of a malformed type gives rise to a static warning. A malformed type
   * is then interpreted as dynamic by the static type checker and the runtime.
   */
  TYPE_PARAMETER_REFERENCED_BY_STATIC("Static members cannot reference type parameters"),

  /**
   * 12.16.3 Static Invocation: A static method invocation <i>i</i> has the form
   * <i>C.m(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;
   * x<sub>n+k</sub>: a<sub>n+k</sub>)</i>. It is a static warning if <i>C</i> does not denote a
   * class in the current scope.
   * 
   * @param undefinedClassName the name of the undefined class
   */
  UNDEFINED_CLASS("Undefined class '%s'"),

  /**
   * Same as {@link #UNDEFINED_CLASS}, but to catch using "boolean" instead of "bool".
   */
  UNDEFINED_CLASS_BOOLEAN("Undefined class 'boolean'; did you mean 'bool'?"),

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
   * 
   * @param name the name of the identifier
   */
  UNDEFINED_IDENTIFIER("Undefined name '%s'"),

  /**
   * 12.14.2 Binding Actuals to Formals: Furthermore, each <i>q<sub>i</sub></i>, <i>1<=i<=l</i>,
   * must have a corresponding named parameter in the set {<i>p<sub>n+1</sub></i> &hellip;
   * <i>p<sub>n+k</sub></i>} or a static warning occurs.
   * 
   * @param name the name of the requested named parameter
   */
  UNDEFINED_NAMED_PARAMETER("The named parameter '%s' is not defined"),

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
   * 12.16.3 Static Invocation: It is a static warning if <i>C</i> does not declare a static method
   * or getter <i>m</i>.
   * 
   * @param methodName the name of the method
   * @param enclosingType the name of the enclosing type where the method is being looked for
   */
  // Note: all cases of this method are covered by the StaticWarningCode.UNDEFINED_METHOD/UNDEFINED_GETTER and UNDEFINED_SETTER codes
  UNDEFINED_STATIC_METHOD_OR_GETTER("There is no such static method, getter or setter '%s' in '%s'"),

  /**
   * 7.2 Getters: It is a static warning if the return type of a getter is void.
   */
  VOID_RETURN_FOR_GETTER("The return type of the getter must not be 'void'");

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
  private StaticWarningCode(String message) {
    this(message, null);
  }

  /**
   * Initialize a newly created error code to have the given message and correction.
   * 
   * @param message the template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private StaticWarningCode(String message, String correction) {
  }

  @Override
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
