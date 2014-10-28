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

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.services.internal.correction.AbstractDartTest;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;

import static com.google.dart.engine.services.refactoring.NamingConventions.validateAngularAttributeName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateAngularComponentName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateAngularControllerName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateAngularFormatterName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateAngularPropertyName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateAngularScopePropertyName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateAngularTagSelectorName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateClassName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateConstructorName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateFieldName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateFunctionName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateFunctionTypeAliasName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateImportPrefixName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateLibraryName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateMethodName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateParameterName;
import static com.google.dart.engine.services.refactoring.NamingConventions.validateVariableName;

/**
 * Test for {@link NamingConventions}.
 */
public class NamingConventionsTest extends AbstractDartTest {
  public void test_validateAngularAttribute_blank() throws Exception {
    assertRefactoringStatus(
        validateAngularAttributeName(" "),
        RefactoringStatusSeverity.ERROR,
        "Attribute name must not be blank.");
    assertRefactoringStatus(
        validateAngularAttributeName(" "),
        RefactoringStatusSeverity.ERROR,
        "Attribute name must not be blank.");
  }

  public void test_validateAngularAttribute_hasBlank() throws Exception {
    assertRefactoringStatus(
        validateAngularAttributeName("my- bad-name"),
        RefactoringStatusSeverity.ERROR,
        "Attribute name must not contain ' '.");
  }

  public void test_validateAngularAttribute_hasDot() throws Exception {
    assertRefactoringStatus(
        validateAngularAttributeName("my.bad.name"),
        RefactoringStatusSeverity.ERROR,
        "Attribute name must not contain '.'.");
  }

  public void test_validateAngularAttribute_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateAngularAttributeName("2my-bad-name"),
        RefactoringStatusSeverity.ERROR,
        "Attribute name must not start with '2'.");
  }

  public void test_validateAngularAttribute_null() throws Exception {
    assertRefactoringStatus(
        validateAngularAttributeName(null),
        RefactoringStatusSeverity.ERROR,
        "Attribute name must not be null.");
  }

  public void test_validateAngularAttribute_OK_oneIdentifier() throws Exception {
    assertRefactoringStatusOK(validateAngularAttributeName("name"));
  }

  public void test_validateAngularAttribute_OK_severalIdentifiers() throws Exception {
    assertRefactoringStatusOK(validateAngularAttributeName("my-property-name"));
  }

  public void test_validateAngularComponentName_blank() throws Exception {
    assertRefactoringStatus(
        validateAngularComponentName(" name"),
        RefactoringStatusSeverity.ERROR,
        "Component name must not start or end with a blank.");
    assertRefactoringStatus(
        validateAngularComponentName("name "),
        RefactoringStatusSeverity.ERROR,
        "Component name must not start or end with a blank.");
  }

  public void test_validateAngularComponentName_hasBlank() throws Exception {
    assertRefactoringStatus(
        validateAngularComponentName("my name"),
        RefactoringStatusSeverity.ERROR,
        "Component name must not contain ' '.");
  }

  public void test_validateAngularComponentName_hasDot() throws Exception {
    assertRefactoringStatus(
        validateAngularComponentName("my.bad.name"),
        RefactoringStatusSeverity.ERROR,
        "Component name must not contain '.'.");
  }

  public void test_validateAngularComponentName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateAngularComponentName("2my-bad-name"),
        RefactoringStatusSeverity.ERROR,
        "Component name must not start with '2'.");
  }

  public void test_validateAngularComponentName_null() throws Exception {
    assertRefactoringStatus(
        validateAngularComponentName(null),
        RefactoringStatusSeverity.ERROR,
        "Component name must not be null.");
  }

  public void test_validateAngularComponentName_OK() throws Exception {
    assertRefactoringStatusOK(validateAngularComponentName("name"));
  }

  public void test_validateAngularControllerName_blank() throws Exception {
    assertRefactoringStatus(
        validateAngularControllerName(" name"),
        RefactoringStatusSeverity.ERROR,
        "Controller name must not start or end with a blank.");
    assertRefactoringStatus(
        validateAngularControllerName("name "),
        RefactoringStatusSeverity.ERROR,
        "Controller name must not start or end with a blank.");
  }

  public void test_validateAngularControllerName_hasBlank() throws Exception {
    assertRefactoringStatus(
        validateAngularControllerName("my name"),
        RefactoringStatusSeverity.ERROR,
        "Controller name must not contain ' '.");
  }

  public void test_validateAngularControllerName_hasDot() throws Exception {
    assertRefactoringStatus(
        validateAngularControllerName("my.bad.name"),
        RefactoringStatusSeverity.ERROR,
        "Controller name must not contain '.'.");
  }

  public void test_validateAngularControllerName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateAngularControllerName("2my-bad-name"),
        RefactoringStatusSeverity.ERROR,
        "Controller name must not start with '2'.");
  }

  public void test_validateAngularControllerName_null() throws Exception {
    assertRefactoringStatus(
        validateAngularControllerName(null),
        RefactoringStatusSeverity.ERROR,
        "Controller name must not be null.");
  }

  public void test_validateAngularControllerName_OK() throws Exception {
    assertRefactoringStatusOK(validateAngularControllerName("name"));
  }

  public void test_validateAngularFormatterName_blank() throws Exception {
    assertRefactoringStatus(
        validateAngularFormatterName(" name"),
        RefactoringStatusSeverity.ERROR,
        "Formatter name must not start or end with a blank.");
    assertRefactoringStatus(
        validateAngularFormatterName("name "),
        RefactoringStatusSeverity.ERROR,
        "Formatter name must not start or end with a blank.");
  }

  public void test_validateAngularFormatterName_hasBlank() throws Exception {
    assertRefactoringStatus(
        validateAngularFormatterName("my name"),
        RefactoringStatusSeverity.ERROR,
        "Formatter name must not contain ' '.");
  }

  public void test_validateAngularFormatterName_hasDot() throws Exception {
    assertRefactoringStatus(
        validateAngularFormatterName("my.bad.name"),
        RefactoringStatusSeverity.ERROR,
        "Formatter name must not contain '.'.");
  }

  public void test_validateAngularFormatterName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateAngularFormatterName("2my-bad-name"),
        RefactoringStatusSeverity.ERROR,
        "Formatter name must not start with '2'.");
  }

  public void test_validateAngularFormatterName_null() throws Exception {
    assertRefactoringStatus(
        validateAngularFormatterName(null),
        RefactoringStatusSeverity.ERROR,
        "Formatter name must not be null.");
  }

  public void test_validateAngularFormatterName_OK() throws Exception {
    assertRefactoringStatusOK(validateAngularFormatterName("name"));
  }

  public void test_validateAngularPropertyName_blank() throws Exception {
    assertRefactoringStatus(
        validateAngularPropertyName(" "),
        RefactoringStatusSeverity.ERROR,
        "Property name must not be blank.");
    assertRefactoringStatus(
        validateAngularPropertyName(" "),
        RefactoringStatusSeverity.ERROR,
        "Property name must not be blank.");
  }

  public void test_validateAngularPropertyName_hasBlank() throws Exception {
    assertRefactoringStatus(
        validateAngularPropertyName("my- bad-name"),
        RefactoringStatusSeverity.ERROR,
        "Property name must not contain ' '.");
  }

  public void test_validateAngularPropertyName_hasDot() throws Exception {
    assertRefactoringStatus(
        validateAngularPropertyName("my.bad.name"),
        RefactoringStatusSeverity.ERROR,
        "Property name must not contain '.'.");
  }

  public void test_validateAngularPropertyName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateAngularPropertyName("2my-bad-name"),
        RefactoringStatusSeverity.ERROR,
        "Property name must not start with '2'.");
  }

  public void test_validateAngularPropertyName_null() throws Exception {
    assertRefactoringStatus(
        validateAngularPropertyName(null),
        RefactoringStatusSeverity.ERROR,
        "Property name must not be null.");
  }

  public void test_validateAngularPropertyName_OK_oneIdentifier() throws Exception {
    assertRefactoringStatusOK(validateAngularPropertyName("name"));
  }

  public void test_validateAngularPropertyName_OK_severalIdentifiers() throws Exception {
    assertRefactoringStatusOK(validateAngularPropertyName("my-property-name"));
  }

  public void test_validateAngularScopePropertyName_blank() throws Exception {
    assertRefactoringStatus(
        validateAngularScopePropertyName(" name"),
        RefactoringStatusSeverity.ERROR,
        "Scope property name must not start or end with a blank.");
    assertRefactoringStatus(
        validateAngularScopePropertyName("name "),
        RefactoringStatusSeverity.ERROR,
        "Scope property name must not start or end with a blank.");
  }

  public void test_validateAngularScopePropertyName_hasBlank() throws Exception {
    assertRefactoringStatus(
        validateAngularScopePropertyName("my name"),
        RefactoringStatusSeverity.ERROR,
        "Scope property name must not contain ' '.");
  }

  public void test_validateAngularScopePropertyName_hasDot() throws Exception {
    assertRefactoringStatus(
        validateAngularScopePropertyName("my.bad.name"),
        RefactoringStatusSeverity.ERROR,
        "Scope property name must not contain '.'.");
  }

  public void test_validateAngularScopePropertyName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateAngularScopePropertyName("2my-bad-name"),
        RefactoringStatusSeverity.ERROR,
        "Scope property name must not start with '2'.");
  }

  public void test_validateAngularScopePropertyName_null() throws Exception {
    assertRefactoringStatus(
        validateAngularScopePropertyName(null),
        RefactoringStatusSeverity.ERROR,
        "Scope property name must not be null.");
  }

  public void test_validateAngularScopePropertyName_OK() throws Exception {
    assertRefactoringStatusOK(validateAngularScopePropertyName("name"));
  }

  public void test_validateAngularTagSelector_blank() throws Exception {
    assertRefactoringStatus(
        validateAngularTagSelectorName(" "),
        RefactoringStatusSeverity.ERROR,
        "Tag selector name must not be blank.");
    assertRefactoringStatus(
        validateAngularTagSelectorName(" "),
        RefactoringStatusSeverity.ERROR,
        "Tag selector name must not be blank.");
  }

  public void test_validateAngularTagSelector_hasBlank() throws Exception {
    assertRefactoringStatus(
        validateAngularTagSelectorName("my- bad-name"),
        RefactoringStatusSeverity.ERROR,
        "Tag selector name must not contain ' '.");
  }

  public void test_validateAngularTagSelector_hasDot() throws Exception {
    assertRefactoringStatus(
        validateAngularTagSelectorName("my.bad.name"),
        RefactoringStatusSeverity.ERROR,
        "Tag selector name must not contain '.'.");
  }

  public void test_validateAngularTagSelector_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateAngularTagSelectorName("2my-bad-name"),
        RefactoringStatusSeverity.ERROR,
        "Tag selector name must not start with '2'.");
  }

  public void test_validateAngularTagSelector_null() throws Exception {
    assertRefactoringStatus(
        validateAngularTagSelectorName(null),
        RefactoringStatusSeverity.ERROR,
        "Tag selector name must not be null.");
  }

  public void test_validateAngularTagSelector_OK_oneIdentifier() throws Exception {
    assertRefactoringStatusOK(validateAngularTagSelectorName("name"));
  }

  public void test_validateAngularTagSelector_OK_severalIdentifiers() throws Exception {
    assertRefactoringStatusOK(validateAngularTagSelectorName("my-property-name"));
  }

  public void test_validateClassName_doesNotStartWithLowerCase() throws Exception {
    assertRefactoringStatus(
        validateClassName("newName"),
        RefactoringStatusSeverity.WARNING,
        "Class name should start with an uppercase letter.");
  }

  public void test_validateClassName_empty() throws Exception {
    assertRefactoringStatus(
        validateClassName(""),
        RefactoringStatusSeverity.ERROR,
        "Class name must not be empty.");
  }

  public void test_validateClassName_leadingBlanks() throws Exception {
    assertRefactoringStatus(
        validateClassName("  newName"),
        RefactoringStatusSeverity.ERROR,
        "Class name must not start or end with a blank.");
  }

  public void test_validateClassName_notIdentifierMiddle() throws Exception {
    assertRefactoringStatus(
        validateClassName("na-me"),
        RefactoringStatusSeverity.ERROR,
        "Class name must not contain '-'.");
  }

  public void test_validateClassName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateClassName("2name"),
        RefactoringStatusSeverity.ERROR,
        "Class name must not start with '2'.");
  }

  public void test_validateClassName_null() throws Exception {
    assertRefactoringStatus(
        validateClassName(null),
        RefactoringStatusSeverity.ERROR,
        "Class name must not be null.");
  }

  public void test_validateClassName_OK() throws Exception {
    assertRefactoringStatusOK(validateClassName("NewName"));
  }

  public void test_validateClassName_OK_leadingDollar() throws Exception {
    assertRefactoringStatusOK(validateClassName("$NewName"));
  }

  public void test_validateClassName_OK_leadingUnderscore() throws Exception {
    assertRefactoringStatusOK(validateClassName("_NewName"));
  }

  public void test_validateClassName_OK_middleDollar() throws Exception {
    assertRefactoringStatusOK(validateClassName("New$Name"));
  }

  public void test_validateClassName_trailingBlanks() throws Exception {
    assertRefactoringStatus(
        validateClassName("newName  "),
        RefactoringStatusSeverity.ERROR,
        "Class name must not start or end with a blank.");
  }

  public void test_validateConstructorName_doesNotStartWithLowerCase() throws Exception {
    assertRefactoringStatus(
        validateConstructorName("NewName"),
        RefactoringStatusSeverity.WARNING,
        "Constructor name should start with a lowercase letter.");
  }

  public void test_validateConstructorName_empty() throws Exception {
    assertRefactoringStatusOK(validateConstructorName(""));
  }

  public void test_validateConstructorName_leadingBlanks() throws Exception {
    assertRefactoringStatus(
        validateConstructorName("  newName"),
        RefactoringStatusSeverity.ERROR,
        "Constructor name must not start or end with a blank.");
  }

  public void test_validateConstructorName_notIdentifierMiddle() throws Exception {
    assertRefactoringStatus(
        validateConstructorName("na-me"),
        RefactoringStatusSeverity.ERROR,
        "Constructor name must not contain '-'.");
  }

  public void test_validateConstructorName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateConstructorName("2name"),
        RefactoringStatusSeverity.ERROR,
        "Constructor name must not start with '2'.");
  }

  public void test_validateConstructorName_null() throws Exception {
    assertRefactoringStatus(
        validateConstructorName(null),
        RefactoringStatusSeverity.ERROR,
        "Constructor name must not be null.");
  }

  public void test_validateConstructorName_OK() throws Exception {
    assertRefactoringStatusOK(validateConstructorName("newName"));
  }

  public void test_validateConstructorName_OK_leadingUnderscore() throws Exception {
    assertRefactoringStatusOK(validateConstructorName("_newName"));
  }

  public void test_validateConstructorName_trailingBlanks() throws Exception {
    assertRefactoringStatus(
        validateConstructorName("newName  "),
        RefactoringStatusSeverity.ERROR,
        "Constructor name must not start or end with a blank.");
  }

  public void test_validateFieldName_doesNotStartWithLowerCase() throws Exception {
    assertRefactoringStatus(
        validateFieldName("NewName"),
        RefactoringStatusSeverity.WARNING,
        "Field name should start with a lowercase letter.");
  }

  public void test_validateFieldName_empty() throws Exception {
    assertRefactoringStatus(
        validateFieldName(""),
        RefactoringStatusSeverity.ERROR,
        "Field name must not be empty.");
  }

  public void test_validateFieldName_leadingBlanks() throws Exception {
    assertRefactoringStatus(
        validateFieldName("  newName"),
        RefactoringStatusSeverity.ERROR,
        "Field name must not start or end with a blank.");
  }

  public void test_validateFieldName_notIdentifierMiddle() throws Exception {
    assertRefactoringStatus(
        validateFieldName("na-me"),
        RefactoringStatusSeverity.ERROR,
        "Field name must not contain '-'.");
  }

  public void test_validateFieldName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateFieldName("2name"),
        RefactoringStatusSeverity.ERROR,
        "Field name must not start with '2'.");
  }

  public void test_validateFieldName_null() throws Exception {
    assertRefactoringStatus(
        validateFieldName(null),
        RefactoringStatusSeverity.ERROR,
        "Field name must not be null.");
  }

  public void test_validateFieldName_OK() throws Exception {
    assertRefactoringStatusOK(validateFieldName("newName"));
  }

  public void test_validateFieldName_OK_leadingUnderscore() throws Exception {
    assertRefactoringStatusOK(validateFieldName("_newName"));
  }

  public void test_validateFieldName_trailingBlanks() throws Exception {
    assertRefactoringStatus(
        validateFieldName("newName  "),
        RefactoringStatusSeverity.ERROR,
        "Field name must not start or end with a blank.");
  }

  public void test_validateFunctionName_doesNotStartWithLowerCase() throws Exception {
    assertRefactoringStatus(
        validateFunctionName("NewName"),
        RefactoringStatusSeverity.WARNING,
        "Function name should start with a lowercase letter.");
  }

  public void test_validateFunctionName_empty() throws Exception {
    assertRefactoringStatus(
        validateFunctionName(""),
        RefactoringStatusSeverity.ERROR,
        "Function name must not be empty.");
  }

  public void test_validateFunctionName_leadingBlanks() throws Exception {
    assertRefactoringStatus(
        validateFunctionName("  newName"),
        RefactoringStatusSeverity.ERROR,
        "Function name must not start or end with a blank.");
  }

  public void test_validateFunctionName_notIdentifierMiddle() throws Exception {
    assertRefactoringStatus(
        validateFunctionName("na-me"),
        RefactoringStatusSeverity.ERROR,
        "Function name must not contain '-'.");
  }

  public void test_validateFunctionName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateFunctionName("2name"),
        RefactoringStatusSeverity.ERROR,
        "Function name must not start with '2'.");
  }

  public void test_validateFunctionName_null() throws Exception {
    assertRefactoringStatus(
        validateFunctionName(null),
        RefactoringStatusSeverity.ERROR,
        "Function name must not be null.");
  }

  public void test_validateFunctionName_OK() throws Exception {
    assertRefactoringStatusOK(validateFunctionName("newName"));
  }

  public void test_validateFunctionName_OK_leadingDollar() throws Exception {
    assertRefactoringStatusOK(validateFunctionName("$newName"));
  }

  public void test_validateFunctionName_OK_leadingUnderscore() throws Exception {
    assertRefactoringStatusOK(validateFunctionName("_newName"));
  }

  public void test_validateFunctionName_OK_middleDollar() throws Exception {
    assertRefactoringStatusOK(validateFunctionName("new$Name"));
  }

  public void test_validateFunctionName_trailingBlanks() throws Exception {
    assertRefactoringStatus(
        validateFunctionName("newName  "),
        RefactoringStatusSeverity.ERROR,
        "Function name must not start or end with a blank.");
  }

  public void test_validateFunctionTypeAliasName_doesNotStartWithLowerCase() throws Exception {
    assertRefactoringStatus(
        validateFunctionTypeAliasName("newName"),
        RefactoringStatusSeverity.WARNING,
        "Function type alias name should start with an uppercase letter.");
  }

  public void test_validateFunctionTypeAliasName_empty() throws Exception {
    assertRefactoringStatus(
        validateFunctionTypeAliasName(""),
        RefactoringStatusSeverity.ERROR,
        "Function type alias name must not be empty.");
  }

  public void test_validateFunctionTypeAliasName_leadingBlanks() throws Exception {
    assertRefactoringStatus(
        validateFunctionTypeAliasName("  newName"),
        RefactoringStatusSeverity.ERROR,
        "Function type alias name must not start or end with a blank.");
  }

  public void test_validateFunctionTypeAliasName_notIdentifierMiddle() throws Exception {
    assertRefactoringStatus(
        validateFunctionTypeAliasName("na-me"),
        RefactoringStatusSeverity.ERROR,
        "Function type alias name must not contain '-'.");
  }

  public void test_validateFunctionTypeAliasName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateFunctionTypeAliasName("2name"),
        RefactoringStatusSeverity.ERROR,
        "Function type alias name must not start with '2'.");
  }

  public void test_validateFunctionTypeAliasName_null() throws Exception {
    assertRefactoringStatus(
        validateFunctionTypeAliasName(null),
        RefactoringStatusSeverity.ERROR,
        "Function type alias name must not be null.");
  }

  public void test_validateFunctionTypeAliasName_OK() throws Exception {
    assertRefactoringStatusOK(validateFunctionTypeAliasName("NewName"));
  }

  public void test_validateFunctionTypeAliasName_OK_leadingUnderscore() throws Exception {
    assertRefactoringStatusOK(validateFunctionTypeAliasName("_NewName"));
  }

  public void test_validateFunctionTypeAliasName_trailingBlanks() throws Exception {
    assertRefactoringStatus(
        validateFunctionTypeAliasName("newName  "),
        RefactoringStatusSeverity.ERROR,
        "Function type alias name must not start or end with a blank.");
  }

  public void test_validateImportPrefixName_doesNotStartWithLowerCase() throws Exception {
    assertRefactoringStatus(
        validateImportPrefixName("NewName"),
        RefactoringStatusSeverity.WARNING,
        "Import prefix name should start with a lowercase letter.");
  }

  public void test_validateImportPrefixName_leadingBlanks() throws Exception {
    assertRefactoringStatus(
        validateImportPrefixName("  newName"),
        RefactoringStatusSeverity.ERROR,
        "Import prefix name must not start or end with a blank.");
  }

  public void test_validateImportPrefixName_notIdentifierMiddle() throws Exception {
    assertRefactoringStatus(
        validateImportPrefixName("na-me"),
        RefactoringStatusSeverity.ERROR,
        "Import prefix name must not contain '-'.");
  }

  public void test_validateImportPrefixName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateImportPrefixName("2name"),
        RefactoringStatusSeverity.ERROR,
        "Import prefix name must not start with '2'.");
  }

  public void test_validateImportPrefixName_null() throws Exception {
    assertRefactoringStatus(
        validateImportPrefixName(null),
        RefactoringStatusSeverity.ERROR,
        "Import prefix name must not be null.");
  }

  public void test_validateImportPrefixName_OK() throws Exception {
    assertRefactoringStatusOK(validateImportPrefixName("newName"));
  }

  public void test_validateImportPrefixName_OK_empty() throws Exception {
    assertRefactoringStatusOK(validateImportPrefixName(""));
  }

  public void test_validateImportPrefixName_OK_leadingDollar() throws Exception {
    assertRefactoringStatusOK(validateImportPrefixName("$newName"));
  }

  public void test_validateImportPrefixName_OK_leadingUnderscore() throws Exception {
    assertRefactoringStatusOK(validateImportPrefixName("_newName"));
  }

  public void test_validateImportPrefixName_OK_middleDollar() throws Exception {
    assertRefactoringStatusOK(validateImportPrefixName("new$Name"));
  }

  public void test_validateImportPrefixName_trailingBlanks() throws Exception {
    assertRefactoringStatus(
        validateImportPrefixName("newName  "),
        RefactoringStatusSeverity.ERROR,
        "Import prefix name must not start or end with a blank.");
  }

  public void test_validateLibraryName_blank() throws Exception {
    assertRefactoringStatus(
        validateLibraryName(" "),
        RefactoringStatusSeverity.ERROR,
        "Library name must not be blank.");
    assertRefactoringStatus(
        validateLibraryName(" "),
        RefactoringStatusSeverity.ERROR,
        "Library name must not be blank.");
  }

  public void test_validateLibraryName_blank_identifier() throws Exception {
    assertRefactoringStatus(
        validateLibraryName("my..name"),
        RefactoringStatusSeverity.ERROR,
        "Library name identifier must not be empty.");
    assertRefactoringStatus(
        validateLibraryName("my. .name"),
        RefactoringStatusSeverity.ERROR,
        "Library name identifier must not start or end with a blank.");
  }

  public void test_validateLibraryName_hasUpperCase() throws Exception {
    assertRefactoringStatus(
        validateLibraryName("newName"),
        RefactoringStatusSeverity.WARNING,
        "Library name should consist of lower-case identifier separated by dots.");
  }

  public void test_validateLibraryName_leadingBlanks() throws Exception {
    assertRefactoringStatus(
        validateLibraryName("my. bad.name"),
        RefactoringStatusSeverity.ERROR,
        "Library name identifier must not start or end with a blank.");
  }

  public void test_validateLibraryName_notIdentifierMiddle() throws Exception {
    assertRefactoringStatus(
        validateLibraryName("my.ba-d.name"),
        RefactoringStatusSeverity.ERROR,
        "Library name identifier must not contain '-'.");
  }

  public void test_validateLibraryName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateLibraryName("my.2bad.name"),
        RefactoringStatusSeverity.ERROR,
        "Library name identifier must not start with '2'.");
  }

  public void test_validateLibraryName_null() throws Exception {
    assertRefactoringStatus(
        validateLibraryName(null),
        RefactoringStatusSeverity.ERROR,
        "Library name must not be null.");
  }

  public void test_validateLibraryName_OK_oneIdentifier() throws Exception {
    assertRefactoringStatusOK(validateLibraryName("name"));
  }

  public void test_validateLibraryName_OK_severalIdentifiers() throws Exception {
    assertRefactoringStatusOK(validateLibraryName("my.library.name"));
  }

  public void test_validateLibraryName_trailingBlanks() throws Exception {
    assertRefactoringStatus(
        validateLibraryName("my.bad .name"),
        RefactoringStatusSeverity.ERROR,
        "Library name identifier must not start or end with a blank.");
  }

  public void test_validateMethodName_doesNotStartWithLowerCase() throws Exception {
    assertRefactoringStatus(
        validateMethodName("NewName"),
        RefactoringStatusSeverity.WARNING,
        "Method name should start with a lowercase letter.");
  }

  public void test_validateMethodName_empty() throws Exception {
    assertRefactoringStatus(
        validateMethodName(""),
        RefactoringStatusSeverity.ERROR,
        "Method name must not be empty.");
  }

  public void test_validateMethodName_leadingBlanks() throws Exception {
    assertRefactoringStatus(
        validateMethodName("  newName"),
        RefactoringStatusSeverity.ERROR,
        "Method name must not start or end with a blank.");
  }

  public void test_validateMethodName_notIdentifierMiddle() throws Exception {
    assertRefactoringStatus(
        validateMethodName("na-me"),
        RefactoringStatusSeverity.ERROR,
        "Method name must not contain '-'.");
  }

  public void test_validateMethodName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateMethodName("2name"),
        RefactoringStatusSeverity.ERROR,
        "Method name must not start with '2'.");
  }

  public void test_validateMethodName_null() throws Exception {
    assertRefactoringStatus(
        validateMethodName(null),
        RefactoringStatusSeverity.ERROR,
        "Method name must not be null.");
  }

  public void test_validateMethodName_OK() throws Exception {
    assertRefactoringStatusOK(validateMethodName("newName"));
  }

  public void test_validateMethodName_OK_leadingUnderscore() throws Exception {
    assertRefactoringStatusOK(validateMethodName("_newName"));
  }

  public void test_validateMethodName_trailingBlanks() throws Exception {
    assertRefactoringStatus(
        validateMethodName("newName  "),
        RefactoringStatusSeverity.ERROR,
        "Method name must not start or end with a blank.");
  }

  public void test_validateParameterName_doesNotStartWithLowerCase() throws Exception {
    assertRefactoringStatus(
        validateParameterName("NewName"),
        RefactoringStatusSeverity.WARNING,
        "Parameter name should start with a lowercase letter.");
  }

  public void test_validateParameterName_empty() throws Exception {
    assertRefactoringStatus(
        validateParameterName(""),
        RefactoringStatusSeverity.ERROR,
        "Parameter name must not be empty.");
  }

  public void test_validateParameterName_leadingBlanks() throws Exception {
    assertRefactoringStatus(
        validateParameterName("  newName"),
        RefactoringStatusSeverity.ERROR,
        "Parameter name must not start or end with a blank.");
  }

  public void test_validateParameterName_notIdentifierMiddle() throws Exception {
    assertRefactoringStatus(
        validateParameterName("na-me"),
        RefactoringStatusSeverity.ERROR,
        "Parameter name must not contain '-'.");
  }

  public void test_validateParameterName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateParameterName("2name"),
        RefactoringStatusSeverity.ERROR,
        "Parameter name must not start with '2'.");
  }

  public void test_validateParameterName_null() throws Exception {
    assertRefactoringStatus(
        validateParameterName(null),
        RefactoringStatusSeverity.ERROR,
        "Parameter name must not be null.");
  }

  public void test_validateParameterName_OK() throws Exception {
    assertRefactoringStatusOK(validateParameterName("newName"));
  }

  public void test_validateParameterName_OK_leadingUnderscore() throws Exception {
    assertRefactoringStatusOK(validateParameterName("_newName"));
  }

  public void test_validateParameterName_trailingBlanks() throws Exception {
    assertRefactoringStatus(
        validateParameterName("newName  "),
        RefactoringStatusSeverity.ERROR,
        "Parameter name must not start or end with a blank.");
  }

  public void test_validateVariableName_doesNotStartWithLowerCase() throws Exception {
    assertRefactoringStatus(
        validateVariableName("NewName"),
        RefactoringStatusSeverity.WARNING,
        "Variable name should start with a lowercase letter.");
  }

  public void test_validateVariableName_empty() throws Exception {
    assertRefactoringStatus(
        validateVariableName(""),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not be empty.");
  }

  public void test_validateVariableName_leadingBlanks() throws Exception {
    assertRefactoringStatus(
        validateVariableName("  newName"),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not start or end with a blank.");
  }

  public void test_validateVariableName_notIdentifierMiddle() throws Exception {
    assertRefactoringStatus(
        validateVariableName("na-me"),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not contain '-'.");
  }

  public void test_validateVariableName_notIdentifierStart() throws Exception {
    assertRefactoringStatus(
        validateVariableName("2name"),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not start with '2'.");
  }

  public void test_validateVariableName_null() throws Exception {
    assertRefactoringStatus(
        validateVariableName(null),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not be null.");
  }

  public void test_validateVariableName_OK() throws Exception {
    assertRefactoringStatusOK(validateVariableName("newName"));
  }

  public void test_validateVariableName_OK_leadingUnderscore() throws Exception {
    assertRefactoringStatusOK(validateVariableName("_newName"));
  }

  public void test_validateVariableName_trailingBlanks() throws Exception {
    assertRefactoringStatus(
        validateVariableName("newName  "),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not start or end with a blank.");
  }

}
