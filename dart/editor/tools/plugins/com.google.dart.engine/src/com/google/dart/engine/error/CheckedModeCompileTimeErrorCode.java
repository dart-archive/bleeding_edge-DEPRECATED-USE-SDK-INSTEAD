/*
 * Copyright (c) 2014, the Dart project authors.
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
 * The enumeration {@code CompileTimeErrorCode} defines the error codes used for compile time errors
 * caused by constant evaluation that would throw an exception when run in checked mode. The client
 * of the analysis engine is responsible for determining how these errors should be presented to the
 * user (for example, a command-line compiler might elect to treat these errors differently
 * depending whether it is compiling it "checked" mode).
 * 
 * @coverage dart.engine.error
 */
public enum CheckedModeCompileTimeErrorCode implements ErrorCode {
  /**
   * 7.6.1 Generative Constructors: In checked mode, it is a dynamic type error if o is not
   * <b>null</b> and the interface of the class of <i>o</i> is not a subtype of the static type of
   * the field <i>v</i>.
   * <p>
   * 12.11.2 Const: It is a compile-time error if evaluation of a constant object results in an
   * uncaught exception being thrown.
   * 
   * @param initializerType the name of the type of the initializer expression
   * @param fieldType the name of the type of the field
   */
  CONST_FIELD_INITIALIZER_NOT_ASSIGNABLE(
      "The initializer type '%s' cannot be assigned to the field type '%s'"),

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
      "The element type '%s' cannot be assigned to the map value type '%s'");

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
  private CheckedModeCompileTimeErrorCode(String message) {
    this(message, null);
  }

  /**
   * Initialize a newly created error code to have the given message and correction.
   * 
   * @param message the template used to create the message to be displayed for the error
   * @param correction the template used to create the correction to be displayed for the error
   */
  private CheckedModeCompileTimeErrorCode(String message, String correction) {
    this.message = message;
    this.correction = correction;
  }

  @Override
  public String getCorrection() {
    return correction;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return ErrorType.CHECKED_MODE_COMPILE_TIME_ERROR.getSeverity();
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ErrorType getType() {
    return ErrorType.CHECKED_MODE_COMPILE_TIME_ERROR;
  }

  @Override
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
