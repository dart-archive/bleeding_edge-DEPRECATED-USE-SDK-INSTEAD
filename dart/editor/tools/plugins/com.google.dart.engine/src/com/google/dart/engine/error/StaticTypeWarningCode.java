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
   * 12.7 Lists: A fresh instance (7.6.1) <i>a</i>, of size <i>n</i>, whose class implements the
   * built-in class <i>List&lt;E></i> is allocated.
   * 
   * @param numTypeArgument the number of provided type arguments
   */
  EXPECTED_ONE_LIST_TYPE_ARGUMENTS(
      "List literal requires exactly one type arguments or none, but %d found"),

  /**
   * 12.8 Maps: A fresh instance (7.6.1) <i>m</i>, of size <i>n</i>, whose class implements the
   * built-in class <i>Map&lt;K, V></i> is allocated.
   * 
   * @param numTypeArgument the number of provided type arguments
   */
  EXPECTED_TWO_MAP_TYPE_ARGUMENTS(
      "Map literal requires exactly two type arguments or none, but %d found"),

  /**
   * 12.18 Assignment: Let <i>T</i> be the static type of <i>e<sub>1</sub></i>. It is a static type
   * warning if <i>T</i> does not have an accessible instance setter named <i>v=</i>.
   * 
   * @see #UNDEFINED_SETTER
   */
  // Low priority- This is currently being caught by StaticWarningCode.UNDEFINED_SETTER. In order to
  // identify situations where the setter is actually inaccessible, we would need to convert the
  // lookups in the resolver (ElementResolver) to use the InheritanceManager. After this, we would
  // need to enhance the InheritanceManager to be able to make the distinction.
  INACCESSIBLE_SETTER(""),

  /**
   * 8.1.1 Inheritance and Overriding: However, if the above rules would cause multiple members
   * <i>m<sub>1</sub>, &hellip;, m<sub>k</sub></i> with the same name <i>n</i> that would be
   * inherited (because identically named members existed in several superinterfaces) then at most
   * one member is inherited.
   * <p>
   * If the static types <i>T<sub>1</sub>, &hellip;, T<sub>k</sub></i> of the members
   * <i>m<sub>1</sub>, &hellip;, m<sub>k</sub></i> are not identical, then there must be a member
   * <i>m<sub>x</sub></i> such that <i>T<sub>x</sub> &lt;: T<sub>i</sub>, 1 &lt;= x &lt;= k</i> for
   * all <i>i, 1 &lt;= i &lt;= k</i>, or a static type warning occurs. The member that is inherited
   * is <i>m<sub>x</sub></i>, if it exists; otherwise:
   * <ul>
   * <li>Let <i>numberOfPositionals</i>(<i>f</i>) denote the number of positional parameters of a
   * function <i>f</i>, and let <i>numberOfRequiredParams</i>(<i>f</i>) denote the number of
   * required parameters of a function <i>f</i>. Furthermore, let <i>s</i> denote the set of all
   * named parameters of the <i>m<sub>1</sub>, &hellip;, m<sub>k</sub></i>. Then let
   * <ul>
   * <li><i>h = max(numberOfPositionals(m<sub>i</sub>)),</i></li>
   * <li><i>r = min(numberOfRequiredParams(m<sub>i</sub>)), for all <i>i</i>, 1 <= i <= k.</i></li>
   * </ul>
   * If <i>r <= h</i> then <i>I</i> has a method named <i>n</i>, with <i>r</i> required parameters
   * of type <b>dynamic</b>, <i>h</i> positional parameters of type <b>dynamic</b>, named parameters
   * <i>s</i> of type <b>dynamic</b> and return type <b>dynamic</b>.</li>
   * <li>Otherwise none of the members <i>m<sub>1</sub>, &hellip;, m<sub>k</sub></i> is inherited.
   * </ul>
   */
  INCONSISTENT_METHOD_INHERITANCE(
      "'%s' is inherited by at least two interfaces inconsistently, from %s"),

  /**
   * 12.15.1 Ordinary Invocation: It is a static type warning if <i>T</i> does not have an
   * accessible (3.2) instance member named <i>m</i>.
   * 
   * @param memberName the name of the static member
   * @see UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER
   */
  INSTANCE_ACCESS_TO_STATIC_MEMBER("Static member '%s' cannot be accessed using instance access"),

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
   * <p>
   * 12.15.4 Super Invocation: A super method invocation <i>i</i> has the form
   * <i>super.m(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>, &hellip;
   * x<sub>n+k</sub>: a<sub>n+k</sub>)</i>. If <i>S.m</i> exists, it is a static warning if the type
   * <i>F</i> of <i>S.m</i> may not be assigned to a function type.
   * 
   * @param nonFunctionIdentifier the name of the identifier that is not a function type
   */
  INVOCATION_OF_NON_FUNCTION("'%s' is not a method"),

  /**
   * 12.14.4 Function Expression Invocation: A function expression invocation <i>i</i> has the form
   * <i>e<sub>f</sub>(a<sub>1</sub>, &hellip;, a<sub>n</sub>, x<sub>n+1</sub>: a<sub>n+1</sub>,
   * &hellip;, x<sub>n+k</sub>: a<sub>n+k</sub>)</i>, where <i>e<sub>f</sub></i> is an expression.
   * <p>
   * It is a static type warning if the static type <i>F</i> of <i>e<sub>f</sub></i> may not be
   * assigned to a function type.
   */
  INVOCATION_OF_NON_FUNCTION_EXPRESSION("Cannot invoke a non-function"),

  /**
   * 12.20 Conditional: It is a static type warning if the type of <i>e<sub>1</sub></i> may not be
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
   * 12.28 Unary Expressions: The expression !<i>e</i> is equivalent to the expression
   * <i>e</i>?<b>false<b> : <b>true</b>.
   * <p>
   * 12.20 Conditional: It is a static type warning if the type of <i>e<sub>1</sub></i> may not be
   * assigned to bool.
   */
  NON_BOOL_NEGATION_EXPRESSION("Negation argument must have a static type of 'bool'"),

  /**
   * 12.21 Logical Boolean Expressions: It is a static type warning if the static types of both of
   * <i>e<sub>1</sub></i> and <i>e<sub>2</sub></i> may not be assigned to bool.
   * 
   * @param operator the lexeme of the logical operator
   */
  NON_BOOL_OPERAND("The operands of the '%s' operator must be assignable to 'bool'"),

  /**
   * 15.8 Parameterized Types: It is a static type warning if <i>A<sub>i</sub>, 1 &lt;= i &lt;=
   * n</i> does not denote a type in the enclosing lexical scope.
   */
  NON_TYPE_AS_TYPE_ARGUMENT(
      "The name '%s' is not a type and cannot be used as a parameterized type"),

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
   * <p>
   * 15.8 Parameterized Types: If <i>S</i> is the static type of a member <i>m</i> of <i>G</i>, then
   * the static type of the member <i>m</i> of <i>G&lt;A<sub>1</sub>, &hellip;,
   * A<sub>n</sub>&gt;</i> is <i>[A<sub>1</sub>, &hellip;, A<sub>n</sub>/T<sub>1</sub>, &hellip;,
   * T<sub>n</sub>]S</i> where <i>T<sub>1</sub>, &hellip;, T<sub>n</sub></i> are the formal type
   * parameters of <i>G</i>. Let <i>B<sub>i</sub></i> be the bounds of <i>T<sub>i</sub>, 1 &lt;= i
   * &lt;= n</i>. It is a static type warning if <i>A<sub>i</sub></i> is not a subtype of
   * <i>[A<sub>1</sub>, &hellip;, A<sub>n</sub>/T<sub>1</sub>, &hellip;,
   * T<sub>n</sub>]B<sub>i</sub>, 1 &lt;= i &lt;= n</i>.
   * <p>
   * 7.6.2 Factories: It is a static type warning if any of the type arguments to <i>k'</i> are not
   * subtypes of the bounds of the corresponding formal type parameters of type.
   * 
   * @param boundedTypeName the name of the type used in the instance creation that should be
   *          limited by the bound as specified in the class declaration
   * @param boundingTypeName the name of the bounding type
   * @see #TYPE_PARAMETER_SUPERTYPE_OF_ITS_BOUND
   */
  TYPE_ARGUMENT_NOT_MATCHING_BOUNDS("'%s' does not extend '%s'"),

  /**
   * 10 Generics: It is a static type warning if a type parameter is a supertype of its upper bound.
   * 
   * @param typeParameterName the name of the type parameter
   * @see #TYPE_ARGUMENT_NOT_MATCHING_BOUNDS
   */
  TYPE_PARAMETER_SUPERTYPE_OF_ITS_BOUND("'%s' cannot be a supertype of its upper bound"),

  /**
   * 12.17 Getter Invocation: It is a static warning if there is no class <i>C</i> in the enclosing
   * lexical scope of <i>i</i>, or if <i>C</i> does not declare, implicitly or explicitly, a getter
   * named <i>m</i>.
   * 
   * @param constantName the name of the enumeration constant that is not defined
   * @param enumName the name of the enumeration used to access the constant
   */
  UNDEFINED_ENUM_CONSTANT("There is no constant named '%s' in '%s'"),

  /**
   * 12.15.3 Unqualified Invocation: If there exists a lexically visible declaration named
   * <i>id</i>, let <i>f<sub>id</sub></i> be the innermost such declaration. Then: [skip].
   * Otherwise, <i>f<sub>id</sub></i> is considered equivalent to the ordinary method invocation
   * <b>this</b>.<i>id</i>(<i>a<sub>1</sub></i>, ..., <i>a<sub>n</sub></i>, <i>x<sub>n+1</sub></i> :
   * <i>a<sub>n+1</sub></i>, ..., <i>x<sub>n+k</sub></i> : <i>a<sub>n+k</sub></i>).
   * 
   * @param methodName the name of the method that is undefined
   */
  UNDEFINED_FUNCTION("The function '%s' is not defined"),

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
   * 12.18 Assignment: Evaluation of an assignment of the form
   * <i>e<sub>1</sub></i>[<i>e<sub>2</sub></i>] = <i>e<sub>3</sub></i> is equivalent to the
   * evaluation of the expression (a, i, e){a.[]=(i, e); return e;} (<i>e<sub>1</sub></i>,
   * <i>e<sub>2</sub></i>, <i>e<sub>2</sub></i>).
   * <p>
   * 12.29 Assignable Expressions: An assignable expression of the form
   * <i>e<sub>1</sub></i>[<i>e<sub>2</sub></i>] is evaluated as a method invocation of the operator
   * method [] on <i>e<sub>1</sub></i> with argument <i>e<sub>2</sub></i>.
   * <p>
   * 12.15.1 Ordinary Invocation: Let <i>T</i> be the static type of <i>o</i>. It is a static type
   * warning if <i>T</i> does not have an accessible instance member named <i>m</i>.
   * 
   * @param operator the name of the operator
   * @param enclosingType the name of the enclosing type where the operator is being looked for
   */
  UNDEFINED_OPERATOR("There is no such operator '%s' in '%s'"),

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
   * 12.15.1 Ordinary Invocation: It is a static type warning if <i>T</i> does not have an
   * accessible (3.2) instance member named <i>m</i>.
   * <p>
   * This is a specialization of {@link #INSTANCE_ACCESS_TO_STATIC_MEMBER} that is used when we are
   * able to find the name defined in a supertype. It exists to provide a more informative error
   * message.
   */
  UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER(
      "Static members from supertypes must be qualified by the name of the defining type"),

  /**
   * 15.8 Parameterized Types: It is a static type warning if <i>G</i> is not a generic type with
   * exactly <i>n</i> type parameters.
   * 
   * @param typeName the name of the type being referenced (<i>G</i>)
   * @param parameterCount the number of type parameters that were declared
   * @param argumentCount the number of type arguments provided
   * @see CompileTimeErrorCode#CONST_WITH_INVALID_TYPE_PARAMETERS
   * @see CompileTimeErrorCode#NEW_WITH_INVALID_TYPE_PARAMETERS
   */
  WRONG_NUMBER_OF_TYPE_ARGUMENTS(
      "The type '%s' is declared with %d type parameters, but %d type arguments were given");

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
  private StaticTypeWarningCode(String message) {
    this(message, null);
  }

  /**
   * Initialize a newly created error code to have the given message and correction.
   * 
   * @param message the template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private StaticTypeWarningCode(String message, String correction) {
    this.message = message;
    this.correction = correction;
  }

  @Override
  public String getCorrection() {
    return correction;
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
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
