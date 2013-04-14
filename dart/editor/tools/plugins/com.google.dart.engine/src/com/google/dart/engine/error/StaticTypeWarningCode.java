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
 * The enumeration {@code StaticTypeWarningCode} defines the error codes used for static type
 * warnings. The convention for this class is for the name of the error code to indicate the problem
 * that caused the error to be generated and for the error message to explain what is wrong and,
 * when appropriate, how the problem can be corrected.
 * 
 * @coverage dart.engine.error
 */
public enum StaticTypeWarningCode implements ErrorCode {
  /**
   * 12.18 Assignment: Let <i>T</i> be the static type of <i>e<sub>1</sub></i>. It is a static type
   * warning if <i>T</i> does not have an accessible instance setter named <i>v=</i>.
   * 
   * @see #UNDEFINED_SETTER
   */
  INACCESSIBLE_SETTER(""),

  /**
   * 8.1.1 Inheritance and Overriding: However, if there are multiple members <i>m<sub>1</sub>,
   * &hellip; m<sub>k</sub></i> with the same name <i>n</i> that would be inherited (because
   * identically named members existed in several superinterfaces) then at most one member is
   * inherited. If the static types <i>T<sub>1</sub>, &hellip;, T<sub>k</sub></i> of the members
   * <i>m<sub>1</sub>, &hellip;, m<sub>k</sub></i> are not identical, then there must be a member
   * <i>m<sub>x</sub></i> such that <i>T<sub>x</sub> &lt; T<sub>i</sub>, 1 &lt;= x &lt;= k</i> for
   * all <i>i, 1 &lt;= i &lt; k</i>, or a static type warning occurs. The member that is inherited
   * is <i>m<sub>x</sub></i>, if it exists; otherwise:
   * <ol>
   * <li>If all of <i>m<sub>1</sub>, &hellip; m<sub>k</sub></i> have the same number <i>r</i> of
   * required parameters and the same set of named parameters <i>s</i>, then let <i>h = max(
   * numberOfOptionalPositionals( m<sub>i</sub> ) ), 1 &lt;= i &lt;= k</i>. <i>I</i> has a method
   * named <i>n</i>, with <i>r</i> required parameters of type dynamic, <i>h</i> optional positional
   * parameters of type dynamic, named parameters <i>s</i> of type dynamic and return type dynamic.
   * <li>Otherwise none of the members <i>m<sub>1</sub>, &hellip;, m<sub>k</sub></i> is inherited.
   * </ol>
   */
  INCONSISTENT_METHOD_INHERITANCE(""), // This probably wants to be multiple messages.

  /**
   * 12.18 Assignment: It is a static type warning if the static type of <i>e</i> may not be
   * assigned to the static type of <i>v</i>. The static type of the expression <i>v = e</i> is the
   * static type of <i>e</i>.
   * <p>
   * 12.18 Assignment: It is a static type warning if the static type of <i>e</i> may not be
   * assigned to the static type of <i>C.v</i>. The static type of the expression <i>C.v = e</i> is
   * the static type of <i>e</i>.
   * <p>
   * 12.18 Assignment: Let <i>T</i> be the static type of <i>e<sub>1</sub></i>. It is a static type
   * warning if the static type of <i>e<sub>2</sub></i> may not be assigned to <i>T</i>.
   * 
   * @param rhsTypeName the name of the right hand side type
   * @param lhsTypeName the name of the left hand side type
   */
  INVALID_ASSIGNMENT("A value of type '%s' cannot be assigned to a variable of type '%s'"),

  /**
   * 12.14.4 Function Expression Invocation: A function expression invocation <i>i</i> has the form
   * <i>e<sub>f</sub>(a<sub>1</sub>, &hellip; a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>,
   * &hellip;, x<sub>n+k</sub>: a<sub>n+k</sub>)</i>, where <i>e<sub>f</sub></i> is an expression.
   * <p>
   * It is a static type warning if the static type <i>F</i> of <i>e<sub>f</sub></i> may not be
   * assigned to a function type.
   * <p>
   * 12.15.1 Ordinary Invocation: An ordinary method invocation <i>i</i> has the form
   * <i>o.m(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;
   * x<sub>n+k</sub>: a<sub>n+k</sub>)</i>.
   * <p>
   * Let <i>T</i> be the static type of <i>o</i>. It is a static type warning if <i>T</i> does not
   * have an accessible instance member named <i>m</i>. If <i>T.m</i> exists, it is a static warning
   * if the type <i>F</i> of <i>T.m</i> may not be assigned to a function type. If <i>T.m</i> does
   * not exist, or if <i>F</i> is not a function type, the static type of <i>i</i> is dynamic.
   * <p>
   * 12.15.3 Static Invocation: It is a static type warning if the type <i>F</i> of <i>C.m</i> may
   * not be assigned to a function type.
   * 
   * @param nonFunctionIdentifier the name of the identifier that is not a function type
   */
  INVOCATION_OF_NON_FUNCTION("'%s' is not a method"),

  /**
   * 12.19 Conditional: It is a static type warning if the type of <i>e<sub>1</sub></i> may not be
   * assigned to bool.
   * <p>
   * 13.5 If: It is a static type warning if the type of the expression <i>b</i> may not be assigned
   * to bool.
   * <p>
   * 13.7 While: It is a static type warning if the type of <i>e</i> may not be assigned to bool.
   * <p>
   * 13.8 Do: It is a static type warning if the type of <i>e</i> cannot be assigned to bool.
   */
  NON_BOOL_CONDITION("Conditions must have a static type of 'bool'"),

  /**
   * 13.15 Assert: It is a static type warning if the type of <i>e</i> may not be assigned to either
   * bool or () &rarr; bool
   */
  NON_BOOL_EXPRESSION("Assertions must be on either a 'bool' or '() -> bool'"),

  /**
   * 15.8 Parameterized Types: It is a static type warning if <i>A<sub>i</sub>, 1 &lt;= i &lt;=
   * n</i> does not denote a type in the enclosing lexical scope.
   */
  NON_TYPE_AS_TYPE_ARGUMENT(""),

  /**
   * 7.6.2 Factories: It is a static type warning if any of the type arguments to <i>k’</i> are not
   * subtypes of the bounds of the corresponding formal type parameters of type.
   */
  REDIRECT_WITH_INVALID_TYPE_PARAMETERS(""),

  /**
   * 13.11 Return: It is a static type warning if the type of <i>e</i> may not be assigned to the
   * declared return type of the immediately enclosing function.
   * 
   * @param actualReturnType the return type as declared in the return statement
   * @param expectedReturnType the expected return type as defined by the method
   * @param methodName the name of the method
   */
  RETURN_OF_INVALID_TYPE("The return type '%s' is not a '%s', as defined by the method '%s'"),

  /**
   * 12.11 Instance Creation: It is a static type warning if any of the type arguments to a
   * constructor of a generic type <i>G</i> invoked by a new expression or a constant object
   * expression are not subtypes of the bounds of the corresponding formal type parameters of
   * <i>G</i>.
   * 
   * @param boundedTypeName the name of the type used in the instance creation that should be
   *          limited by the bound as specified in the class declaration
   * @param boundingTypeName the name of the bounding type
   */
  TYPE_ARGUMENT_NOT_MATCHING_BOUNDS("'%s' does not extend '%s'"),

  /**
   * 10 Generics: It is a static type warning if a type parameter is a supertype of its upper bound.
   * <p>
   * 15.8 Parameterized Types: If <i>S</i> is the static type of a member <i>m</i> of <i>G</i>, then
   * the static type of the member <i>m</i> of <i>G&lt;A<sub>1</sub>, &hellip; A<sub>n</sub>&gt;</i>
   * is <i>[A<sub>1</sub>, &hellip;, A<sub>n</sub>/T<sub>1</sub>, &hellip;, T<sub>n</sub>]S</i>
   * where <i>T<sub>1</sub>, &hellip; T<sub>n</sub></i> are the formal type parameters of <i>G</i>.
   * Let <i>B<sub>i</sub></i> be the bounds of <i>T<sub>i</sub>, 1 &lt;= i &lt;= n</i>. It is a
   * static type warning if <i>A<sub>i</sub></i> is not a subtype of <i>[A<sub>1</sub>, &hellip;,
   * A<sub>n</sub>/T<sub>1</sub>, &hellip;, T<sub>n</sub>]B<sub>i</sub>, 1 &lt;= i &lt;= n</i>.
   */
  TYPE_ARGUMENT_VIOLATES_BOUNDS(""),

  /**
   * Specification reference needed. This is equivalent to {@link #UNDEFINED_METHOD}, but for
   * top-level functions.
   * 
   * @param methodName the name of the method that is undefined
   */
  UNDEFINED_FUNCTION("The FUNCTION '%s' is not defined"),

  /**
   * 12.17 Getter Invocation: Let <i>T</i> be the static type of <i>e</i>. It is a static type
   * warning if <i>T</i> does not have a getter named <i>m</i>.
   * 
   * @param getterName the name of the getter
   * @param enclosingType the name of the enclosing type where the getter is being looked for
   */
  UNDEFINED_GETTER("There is no such getter '%s' in '%s'"),

  /**
   * 12.15.1 Ordinary Invocation: Let <i>T</i> be the static type of <i>o</i>. It is a static type
   * warning if <i>T</i> does not have an accessible instance member named <i>m</i>.
   * 
   * @param methodName the name of the method that is undefined
   * @param typeName the resolved type name that the method lookup is happening on
   */
  UNDEFINED_METHOD("The method '%s' is not defined for the class '%s'"),

  /**
   * 12.18 Assignment: Let <i>T</i> be the static type of <i>e<sub>1</sub></i>. It is a static type
   * warning if <i>T</i> does not have an accessible instance setter named <i>v=</i>.
   * 
   * @param setterName the name of the setter
   * @param enclosingType the name of the enclosing type where the setter is being looked for
   * @see #INACCESSIBLE_SETTER
   */
  UNDEFINED_SETTER("There is no such setter '%s' in '%s'"),

  /**
   * 12.15.4 Super Invocation: A super method invocation <i>i</i> has the form
   * <i>super.m(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;
   * x<sub>n+k</sub>: a<sub>n+k</sub>)</i>. It is a static type warning if <i>S</i> does not have an
   * accessible instance member named <i>m</i>.
   * 
   * @param methodName the name of the method that is undefined
   * @param typeName the resolved type name that the method lookup is happening on
   */
  UNDEFINED_SUPER_METHOD("There is no such method '%s' in '%s'"),

  /**
   * 15.8 Parameterized Types: It is a static type warning if <i>G</i> is not an accessible generic
   * type declaration with <i>n</i> type parameters.
   * 
   * @param typeName the name of the type being referenced (<i>G</i>)
   * @param parameterCount the number of type parameters that were declared
   * @param argumentCount the number of type arguments provided
   */
  WRONG_NUMBER_OF_TYPE_ARGUMENTS(
      "The type '%s' is declared with %d type parameters, but %d type arguments were given");

  /**
   * The message template used to create the message to be displayed for this error.
   */
  private final String message;

  /**
   * Initialize a newly created error code to have the given type and message.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private StaticTypeWarningCode(String message) {
    this.message = message;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return ErrorType.STATIC_TYPE_WARNING.getSeverity();
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ErrorType getType() {
    return ErrorType.STATIC_TYPE_WARNING;
  }

  @Override
  public boolean needsRecompilation() {
    return true;
  }
}
