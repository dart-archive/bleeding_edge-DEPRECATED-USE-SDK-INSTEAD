/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.model;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IStatus;

import java.lang.reflect.Method;

/**
 * Test for {@link DartConventions}.
 */
public class DartConventionsTest extends TestCase {
  private static String elementName;

  /**
   * Asserts that given name has {@link IStatus#ERROR} severity.
   */
  private static String validateError(String name) throws Exception {
    return validateSeverity(name, IStatus.ERROR);
  }

  /**
   * @return the {@link IStatus} of {@link #elementName} name validation.
   */
  private static IStatus validateName(String name) throws Exception {
    Method method = DartConventions.class.getMethod("validate" + elementName, String.class);
    return (IStatus) method.invoke(null, name);
  }

  /**
   * Asserts that given name has {@link IStatus#OK} severity.
   */
  private static String validateOK(String name) throws Exception {
    return validateSeverity(name, IStatus.OK);
  }

  private static String validateSeverity(String name, int severity) throws Exception {
    IStatus status = validateName(name);
    assertTrue(name + " " + status, status.getSeverity() == severity);
    return status.getMessage();
  }

  /**
   * Asserts that given name has {@link IStatus#WARNING} severity.
   */
  private static String validateWarning(String name) throws Exception {
    return validateSeverity(name, IStatus.WARNING);
  }

  /**
   * Test for {@link DartConventions#validateFieldName(String)}.
   */
  public void test_validateFieldName() throws Exception {
    elementName = "FieldName";
    // OK
    validateOK("field");
    validateOK("field2");
    validateOK("fieldName");
    validateOK("_field");
    // warning: first should be lower case
    validateWarning("Field");
    // null
    validateError(null);
    // spaces
    validateError(" field");
    validateError("field ");
    // identifier
    validateError("");
    validateError("f ield");
    validateError("2field");
    validateError("field-");
  }

  /**
   * Test for {@link DartConventions#validateFunctionName(String)}.
   */
  public void test_validateFunctionName() throws Exception {
    elementName = "FunctionName";
    // OK
    validateOK("function");
    validateOK("function2");
    validateOK("functionName");
    validateOK("_function");
    // warning: first should be lower case
    validateWarning("Function");
  }

  /**
   * Test for {@link DartConventions#validateFunctionTypeAliasName(String)}.
   */
  public void test_validateFunctionTypeAliasName() throws Exception {
    elementName = "FunctionTypeAliasName";
    // OK
    validateOK("TypeAlias");
    validateOK("TypeAlias2");
    validateOK("_TypeAlias");
    // warning: first should be upper case
    validateWarning("typeAlias");
    // null
    validateError(null);
    // spaces
    validateError(" TypeAlias");
    validateError("TypeAlias ");
    // identifier
    validateError("");
    validateError("Type Alias");
    validateError("2TypeAlias");
    validateError("TypeAlias-");
  }

  /**
   * Test for {@link DartConventions#validateMethodName(String)}.
   */
  public void test_validateMethodName() throws Exception {
    elementName = "MethodName";
    // OK
    validateOK("method");
    validateOK("method2");
    validateOK("methodName");
    validateOK("_method");
    // warning: first should be lower case
    validateWarning("Method");
  }

  /**
   * Test for {@link DartConventions#validateParameterName(String)}.
   */
  public void test_validateParameterName() throws Exception {
    elementName = "ParameterName";
    // OK
    validateOK("parameter");
    validateOK("parameter2");
    validateOK("parameterName");
    validateOK("_parameter");
    // warning: first should be lower case
    validateWarning("Parameter");
  }

  /**
   * Test for {@link DartConventions#validatePrefix(String)}.
   */
  public void test_validatePrefix() throws Exception {
    elementName = "Prefix";
    // OK
    validateOK("prefix");
    validateOK("Prefix");
    validateOK("prefix2");
    validateOK("prefixName");
    validateOK("_variable");
    // null
    validateError(null);
    // spaces
    validateError(" prefix");
    validateError("prefix ");
    // identifier
    validateError("pre fix");
  }

  /**
   * Test for {@link DartConventions#validateTypeName(String)}.
   */
  public void test_validateTypeName() throws Exception {
    elementName = "TypeName";
    // OK
    validateOK("Type");
    validateOK("Type2");
    validateOK("TypeName");
    validateOK("_Type");
    // warning: first should be upper case
    validateWarning("type");
    // null
    validateError(null);
    // spaces
    validateError(" Type");
    validateError("Type ");
    // identifier
    validateError("");
    validateError("T ype");
    validateError("2Type");
    validateError("Type-");
  }

  /**
   * Test for {@link DartConventions#validateTypeParameterName(String)}.
   */
  public void test_validateTypeParameterName() throws Exception {
    elementName = "TypeParameterName";
    // OK
    validateOK("Type");
    validateOK("Type2");
    validateOK("TypeName");
    validateOK("_Type");
    // warning: first should be upper case
    validateWarning("type");
  }

  /**
   * Test for {@link DartConventions#validateVariableName(String)}.
   */
  public void test_validateVariableName() throws Exception {
    elementName = "VariableName";
    // OK
    validateOK("variable");
    validateOK("variable2");
    validateOK("variableName");
    validateOK("_variable");
    // warning: first should be lower case
    validateWarning("Variable");
  }
}
