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
package com.google.dart.engine.services.refactoring;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;

import java.text.MessageFormat;

/**
 * Provides methods for checking {@link Element} naming conventions.
 */
public final class NamingConventions {
  /**
   * @return the {@link RefactoringStatus} with {@link RefactoringStatusSeverity#OK} if the name is
   *         valid, {@link RefactoringStatusSeverity#WARNING} if the name is discouraged, or
   *         {@link RefactoringStatusSeverity#ERROR} if the name is illegal.
   */
  public static RefactoringStatus validateClassName(String name) {
    return validateUpperCamelCase(name, "Class");
  }

  /**
   * @return the {@link RefactoringStatus} with {@link RefactoringStatusSeverity#OK} if the name is
   *         valid, {@link RefactoringStatusSeverity#WARNING} if the name is discouraged, or
   *         {@link RefactoringStatusSeverity#ERROR} if the name is illegal.
   */
  public static RefactoringStatus validateConstantName(String name) {
    // null
    if (name == null) {
      return RefactoringStatus.createErrorStatus("Constant name must not be null.");
    }
    // has leading/trailing spaces
    String trimmed = name.trim();
    if (!name.equals(trimmed)) {
      return RefactoringStatus.createErrorStatus("Constant name must not start or end with a blank.");
    }
    // is not identifier
    RefactoringStatus status = validateIdentifier(name, "Constant");
    if (!status.isOK()) {
      return status;
    }
    // is private, OK
    int startIndex = 0;
    if (name.charAt(0) == '_') {
      startIndex++;
    }
    // does not start with lower case
    for (int i = startIndex; i < name.length(); i++) {
      char c = name.charAt(i);
      if (!Character.isUpperCase(c) && !Character.isDigit(c) && c != '_') {
        return RefactoringStatus.createWarningStatus("Constant name should be all uppercase with underscores.");
      }
    }
    // OK
    return new RefactoringStatus();
  }

  /**
   * @return the {@link RefactoringStatus} with {@link RefactoringStatusSeverity#OK} if the name is
   *         valid, {@link RefactoringStatusSeverity#WARNING} if the name is discouraged, or
   *         {@link RefactoringStatusSeverity#ERROR} if the name is illegal.
   */
  public static RefactoringStatus validateConstructorName(String name) {
    if (name != null && name.isEmpty()) {
      return new RefactoringStatus();
    }
    return validateLowerCamelCase(name, "Constructor");
  }

  /**
   * @return the {@link RefactoringStatus} with {@link RefactoringStatusSeverity#OK} if the name is
   *         valid, {@link RefactoringStatusSeverity#WARNING} if the name is discouraged, or
   *         {@link RefactoringStatusSeverity#ERROR} if the name is illegal.
   */
  public static RefactoringStatus validateFieldName(String name) {
    return validateLowerCamelCase(name, "Field");
  }

  /**
   * @return the {@link RefactoringStatus} with {@link RefactoringStatusSeverity#OK} if the name is
   *         valid, {@link RefactoringStatusSeverity#WARNING} if the name is discouraged, or
   *         {@link RefactoringStatusSeverity#ERROR} if the name is illegal.
   */
  public static RefactoringStatus validateFunctionName(String name) {
    return validateLowerCamelCase(name, "Function");
  }

  /**
   * @return the {@link RefactoringStatus} with {@link RefactoringStatusSeverity#OK} if the name is
   *         valid, {@link RefactoringStatusSeverity#WARNING} if the name is discouraged, or
   *         {@link RefactoringStatusSeverity#ERROR} if the name is illegal.
   */
  public static RefactoringStatus validateFunctionTypeAliasName(String name) {
    return validateUpperCamelCase(name, "Function type alias");
  }

  /**
   * @return the {@link RefactoringStatus} with {@link RefactoringStatusSeverity#OK} if the name is
   *         valid, {@link RefactoringStatusSeverity#WARNING} if the name is discouraged, or
   *         {@link RefactoringStatusSeverity#ERROR} if the name is illegal.
   */
  public static RefactoringStatus validateMethodName(String name) {
    return validateLowerCamelCase(name, "Method");
  }

  /**
   * @return the {@link RefactoringStatus} with {@link RefactoringStatusSeverity#OK} if the name is
   *         valid, {@link RefactoringStatusSeverity#WARNING} if the name is discouraged, or
   *         {@link RefactoringStatusSeverity#ERROR} if the name is illegal.
   */
  public static RefactoringStatus validateParameterName(String name) {
    return validateLowerCamelCase(name, "Parameter");
  }

  /**
   * @return the {@link RefactoringStatus} with {@link RefactoringStatusSeverity#OK} if the name is
   *         valid, {@link RefactoringStatusSeverity#WARNING} if the name is discouraged, or
   *         {@link RefactoringStatusSeverity#ERROR} if the name is illegal.
   */
  public static RefactoringStatus validateVariableName(String name) {
    return validateLowerCamelCase(name, "Variable");
  }

  private static RefactoringStatus validateIdentifier(String identifier, String elementName) {
    int length = identifier.length();
    if (length == 0) {
      String message = MessageFormat.format("{0} name must not be empty.", elementName);
      return RefactoringStatus.createErrorStatus(message);
    }
    char currentChar = identifier.charAt(0);
    if (!Character.isLetter(currentChar) && currentChar != '_') {
      String message = MessageFormat.format(
          "{0} name must not start with ''{1}''.",
          elementName,
          currentChar);
      return RefactoringStatus.createErrorStatus(message);
    }
    for (int i = 1; i < length; i++) {
      currentChar = identifier.charAt(i);
      if (!Character.isLetterOrDigit(currentChar) && currentChar != '_') {
        String message = MessageFormat.format(
            "{0} name must not contain ''{1}''.",
            elementName,
            currentChar);
        return RefactoringStatus.createErrorStatus(message);
      }
    }
    return new RefactoringStatus();
  }

  /**
   * Validate the given identifier, which should be lower camel case.
   */
  private static RefactoringStatus validateLowerCamelCase(String identifier, String elementName) {
    // null
    if (identifier == null) {
      String message = MessageFormat.format("{0} name must not be null.", elementName);
      return RefactoringStatus.createErrorStatus(message);
    }
    // has leading/trailing spaces
    String trimmed = identifier.trim();
    if (!identifier.equals(trimmed)) {
      String message = MessageFormat.format(
          "{0} name must not start or end with a blank.",
          elementName);
      return RefactoringStatus.createErrorStatus(message);
    }
    // is not identifier
    RefactoringStatus status = validateIdentifier(identifier, elementName);
    if (!status.isOK()) {
      return status;
    }
    // is private, OK
    if (identifier.charAt(0) == '_') {
      return new RefactoringStatus();
    }
    // does not start with lower case
    if (!Character.isLowerCase(identifier.charAt(0))) {
      String message = MessageFormat.format(
          "{0} name should start with a lowercase letter.",
          elementName);
      return RefactoringStatus.createWarningStatus(message);
    }
    // OK
    return new RefactoringStatus();
  }

  /**
   * Validate the given identifier, which should be upper camel case.
   */
  private static RefactoringStatus validateUpperCamelCase(String identifier, String elementName) {
    // null
    if (identifier == null) {
      String message = MessageFormat.format("{0} name must not be null.", elementName);
      return RefactoringStatus.createErrorStatus(message);
    }
    // leading or trailing spaces
    String trimmed = identifier.trim();
    if (!identifier.equals(trimmed)) {
      String message = MessageFormat.format(
          "{0} name must not start or end with a blank.",
          elementName);
      return RefactoringStatus.createErrorStatus(message);
    }
    // is not identifier
    RefactoringStatus status = validateIdentifier(identifier, elementName);
    if (!status.isOK()) {
      return status;
    }
    // is private, OK
    if (identifier.charAt(0) == '_') {
      return new RefactoringStatus();
    }
    // does not start with upper case
    if (!Character.isUpperCase(identifier.charAt(0))) {
      // By convention, class names usually start with an uppercase letter
      String message = MessageFormat.format(
          "{0} name should start with an uppercase letter.",
          elementName);
      return RefactoringStatus.createWarningStatus(message);
    }
    // OK
    return new RefactoringStatus();
  }

  private NamingConventions() {
  }
}
