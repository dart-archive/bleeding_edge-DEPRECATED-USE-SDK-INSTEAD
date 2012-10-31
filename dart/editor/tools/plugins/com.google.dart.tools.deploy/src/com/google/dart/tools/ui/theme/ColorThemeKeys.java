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
package com.google.dart.tools.ui.theme;

/**
 * Keys that can be used in a color theme.
 */
public enum ColorThemeKeys {
  FOREGROUND("foreground"),
  BACKGROUND("background"),
  SELECTION_FOREGROUND("selectionForeground"),
  SELECTION_BACKGROUND("selectionBackground"),
  CURRENT_LINE("currentLine"),
  LINE_NUMBER("lineNumber"),
  SEARCH_RESULT_INDICATION("searchResultIndication"),
  FILTERED_SEARCH_RESULT_INDICATION("filteredSearchResultIndication"),
  OCCURRENCE_INDICATION("occurrenceIndication"),
  WRITE_OCCURRENCE_INDICATION("writeOccurrenceIndication"),
  DELETION_INDICATION("deletionIndication"),
  FIND_SCOPE("findScope"),
  SINGLE_LINE_COMMENT("singleLineComment"),
  MULTI_LINE_COMMENT("multiLineComment"),
  COMMENT_TASK_TAG("commentTaskTag"),
  SOURCE_HOVER_BACKGROUND("sourceHoverBackground"),
  NUMBER("number"),
  STRING("string"),
  BRACKET("bracket"),
  OPERATOR("operator"),
  KEYWORD("keyword"),
  CLASS("class"),
  INTERFACE("interface"),
  ENUM("enum"),
  METHOD("method"),
  METHOD_DECLARATION("methodDeclaration"),
  ANNOTATION("annotation"),
  LOCAL_VARIABLE("localVariable"),
  LOCAL_VARIABLE_DECLARATION("localVariableDeclaration"),
  INHERITED_METHOD("inheritedMethod"),
  ABSTRACT_METHOD("abstractMethod"),
  STATIC_METHOD("staticMethod"),
  DARTDOC("javadoc"), // Allow re-use of Java or Javascript themes
  DARTDOC_TAG("javadocTag"),
  DARTDOC_KEYWORD("javadocKeyword"),
  DARTDOC_LINK("javadocLink"),
  FIELD("field"),
  STATIC_FIELD("staticField"),
  STATIC_FINAL_FIELD("staticFinalField"),
  PARAMETER_VARIABLE("parameterVariable"),
  TYPE_ARGUMENT("typeArgument"),
  TYPE_PARAMETER("typeParameter"),
  DEPRECATED_MEMBER("deprecatedMember"),
  DEBUG_CURRENT_INSTRUCTION_POINTER("debugCurrentInstructionPointer"),
  DEBUG_SECONDARY_INSTRUCTION_POINTER("debugSecondaryInstructionPointer");
  // TODO(messick): Add additional semantic highlighting keys for Dart elements

  String name;

  ColorThemeKeys(String name) {
    this.name = name;
  }
}
