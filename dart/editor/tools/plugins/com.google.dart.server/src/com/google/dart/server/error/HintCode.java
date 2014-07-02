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
 * The enumeration {@code HintCode} defines the hints and coding recommendations for best practices
 * which are not mentioned in the Dart Language Specification.
 * 
 * @coverage dart.server.error
 */
public enum HintCode implements ErrorCode {
  /**
   * This hint is generated anywhere where the
   * {@link StaticWarningCode#ARGUMENT_TYPE_NOT_ASSIGNABLE} would have been generated, if we used
   * propagated information for the warnings.
   * 
   * @param actualType the name of the actual argument type
   * @param expectedType the name of the expected type
   * @see StaticWarningCode#ARGUMENT_TYPE_NOT_ASSIGNABLE
   */
  ARGUMENT_TYPE_NOT_ASSIGNABLE(
      "The argument type '%s' cannot be assigned to the parameter type '%s'"),

  /**
   * Dead code is code that is never reached, this can happen for instance if a statement follows a
   * return statement.
   */
  DEAD_CODE("Dead code"),

  /**
   * Dead code is code that is never reached. This case covers cases where the user has catch
   * clauses after {@code catch (e)} or {@code on Object catch (e)}.
   */
  DEAD_CODE_CATCH_FOLLOWING_CATCH(
      "Dead code, catch clauses after a 'catch (e)' or an 'on Object catch (e)' are never reached"),

  /**
   * Dead code is code that is never reached. This case covers cases where the user has an on-catch
   * clause such as {@code on A catch (e)}, where a supertype of {@code A} was already caught.
   * 
   * @param subtypeName name of the subtype
   * @param supertypeName name of the supertype
   */
  DEAD_CODE_ON_CATCH_SUBTYPE(
      "Dead code, this on-catch block will never be executed since '%s' is a subtype of '%s'"),

  /**
   * Deprecated members should not be invoked or used.
   * 
   * @param memberName the name of the member
   */
  DEPRECATED_MEMBER_USE("'%s' is deprecated"),

  /**
   * Duplicate imports.
   */
  DUPLICATE_IMPORT("Duplicate import"),

  /**
   * Hint to use the ~/ operator.
   */
  DIVISION_OPTIMIZATION("The operator x ~/ y is more efficient than (x / y).toInt()"),

  /**
   * Hint for the {@code x is double} type checks.
   */
  IS_DOUBLE("When compiled to JS, this test might return true when the left hand side is an int"),

  /**
   * Hint for the {@code x is int} type checks.
   */
  IS_INT("When compiled to JS, this test might return true when the left hand side is a double"),

  /**
   * Hint for the {@code x is! double} type checks.
   */
  IS_NOT_DOUBLE(
      "When compiled to JS, this test might return false when the left hand side is an int"),

  /**
   * Hint for the {@code x is! int} type checks.
   */
  IS_NOT_INT(
      "When compiled to JS, this test might return false when the left hand side is a double"),

  /**
   * Deferred libraries shouldn't define a top level function 'loadLibrary'.
   */
  IMPORT_DEFERRED_LIBRARY_WITH_LOAD_FUNCTION(
      "The library '%s' defines a top-level function named 'loadLibrary' which is hidden by deferring this library"),

  /**
   * This hint is generated anywhere where the {@link StaticTypeWarningCode#INVALID_ASSIGNMENT}
   * would have been generated, if we used propagated information for the warnings.
   * 
   * @param rhsTypeName the name of the right hand side type
   * @param lhsTypeName the name of the left hand side type
   * @see StaticTypeWarningCode#INVALID_ASSIGNMENT
   */
  INVALID_ASSIGNMENT("A value of type '%s' cannot be assigned to a variable of type '%s'"),

  /**
   * Generate a hint for methods or functions that have a return type, but do not have a non-void
   * return statement on all branches. At the end of methods or functions with no return, Dart
   * implicitly returns {@code null}, avoiding these implicit returns is considered a best practice.
   * 
   * @param returnType the name of the declared return type
   */
  MISSING_RETURN(
      "This function declares a return type of '%s', but does not end with a return statement",
      "Either add a return statement or change the return type to 'void'"),

  /**
   * A getter with the override annotation does not override an existing getter.
   */
  OVERRIDE_ON_NON_OVERRIDING_GETTER("Getter does not override an inherited getter"),

  /**
   * A method with the override annotation does not override an existing method.
   */
  OVERRIDE_ON_NON_OVERRIDING_METHOD("Method does not override an inherited method"),

  /**
   * A setter with the override annotation does not override an existing setter.
   */
  OVERRIDE_ON_NON_OVERRIDING_SETTER("Setter does not override an inherited setter"),

  /**
   * Hint for classes that override equals, but not hashCode.
   * 
   * @param className the name of the current class
   */
  OVERRIDE_EQUALS_BUT_NOT_HASH_CODE("The class '%s' overrides 'operator==', but not 'get hashCode'"),

  /**
   * Type checks of the type {@code x is! Null} should be done with {@code x != null}.
   */
  TYPE_CHECK_IS_NOT_NULL("Tests for non-null should be done with '!= null'"),

  /**
   * Type checks of the type {@code x is Null} should be done with {@code x == null}.
   */
  TYPE_CHECK_IS_NULL("Tests for null should be done with '== null'"),

  /**
   * This hint is generated anywhere where the {@link StaticTypeWarningCode#UNDEFINED_GETTER} or
   * {@link StaticWarningCode#UNDEFINED_GETTER} would have been generated, if we used propagated
   * information for the warnings.
   * 
   * @param getterName the name of the getter
   * @param enclosingType the name of the enclosing type where the getter is being looked for
   * @see StaticTypeWarningCode#UNDEFINED_GETTER
   * @see StaticWarningCode#UNDEFINED_GETTER
   */
  UNDEFINED_GETTER("There is no such getter '%s' in '%s'"),

  /**
   * This hint is generated anywhere where the {@link StaticTypeWarningCode#UNDEFINED_METHOD} would
   * have been generated, if we used propagated information for the warnings.
   * 
   * @param methodName the name of the method that is undefined
   * @param typeName the resolved type name that the method lookup is happening on
   * @see StaticTypeWarningCode#UNDEFINED_METHOD
   */
  UNDEFINED_METHOD("The method '%s' is not defined for the class '%s'"),

  /**
   * This hint is generated anywhere where the {@link StaticTypeWarningCode#UNDEFINED_OPERATOR}
   * would have been generated, if we used propagated information for the warnings.
   * 
   * @param operator the name of the operator
   * @param enclosingType the name of the enclosing type where the operator is being looked for
   * @see StaticTypeWarningCode#UNDEFINED_OPERATOR
   */
  UNDEFINED_OPERATOR("There is no such operator '%s' in '%s'"),

  /**
   * This hint is generated anywhere where the {@link StaticTypeWarningCode#UNDEFINED_SETTER} or
   * {@link StaticWarningCode#UNDEFINED_SETTER} would have been generated, if we used propagated
   * information for the warnings.
   * 
   * @param setterName the name of the setter
   * @param enclosingType the name of the enclosing type where the setter is being looked for
   * @see StaticTypeWarningCode#UNDEFINED_SETTER
   * @see StaticWarningCode#UNDEFINED_SETTER
   */
  UNDEFINED_SETTER("There is no such setter '%s' in '%s'"),

  /**
   * Unnecessary cast.
   */
  UNNECESSARY_CAST("Unnecessary cast"),

  /**
   * Unnecessary type checks, the result is always true.
   */
  UNNECESSARY_TYPE_CHECK_FALSE("Unnecessary type check, the result is always false"),

  /**
   * Unnecessary type checks, the result is always false.
   */
  UNNECESSARY_TYPE_CHECK_TRUE("Unnecessary type check, the result is always true"),

  /**
   * Unused imports are imports which are never not used.
   */
  UNUSED_IMPORT("Unused import"),

  /**
   * Hint for cases where the source expects a method or function to return a non-void result, but
   * the method or function signature returns void.
   * 
   * @param name the name of the method or function that returns void
   */
  USE_OF_VOID_RESULT("The result of '%s' is being used, even though it is declared to be 'void'");

  /**
   * Initialize a newly created error code to have the given message.
   * 
   * @param message the message template used to create the message to be displayed for the error
   */
  private HintCode(String message) {
    this(message, null);
  }

  /**
   * Initialize a newly created error code to have the given message and correction.
   * 
   * @param message the template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private HintCode(String message, String correction) {
  }

  @Override
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
