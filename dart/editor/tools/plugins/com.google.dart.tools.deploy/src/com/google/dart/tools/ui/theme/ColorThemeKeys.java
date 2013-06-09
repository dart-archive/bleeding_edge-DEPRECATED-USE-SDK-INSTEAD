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
  ABSTRACT_METHOD("abstractMethod"),
  ANNOTATION("annotation"),
  BACKGROUND("background"),
  BRACKET("bracket"),
  CLASS("class"),
  COMMENT_TASK_TAG("commentTaskTag"),
  CURRENT_LINE("currentLine"),
  DARTDOC("javadoc"),
  DARTDOC_KEYWORD("javadocKeyword"),
  DARTDOC_LINK("javadocLink"),
  DARTDOC_TAG("javadocTag"),
  DEBUG_CURRENT_INSTRUCTION_POINTER("debugCurrentInstructionPointer"),
  DEBUG_SECONDARY_INSTRUCTION_POINTER("debugSecondaryInstructionPointer"),
  DELETION_INDICATION("deletionIndication"),
  DEPRECATED_MEMBER("deprecatedMember"),
  ENUM("enum"),
  FIELD("field"),
  FILTERED_SEARCH_RESULT_INDICATION("filteredSearchResultIndication"),
  FIND_SCOPE("findScope"),
  FOREGROUND("foreground"),
  INHERITED_METHOD("inheritedMethod"),
  INTERFACE("interface"),
  KEYWORD("keyword"),
  LINE_NUMBER("lineNumber"),
  LOCAL_VARIABLE("localVariable"),
  LOCAL_VARIABLE_DECLARATION("localVariableDeclaration"),
  METHOD("method"),
  METHOD_DECLARATION("methodDeclaration"),
  MULTI_LINE_COMMENT("multiLineComment"),
  NUMBER("number"),
  OCCURRENCE_INDICATION("occurrenceIndication"),
  OPERATOR("operator"),
  PARAMETER_VARIABLE("parameterVariable"), // Allow re-use of Java or Javascript themes
  SEARCH_RESULT_INDICATION("searchResultIndication"),
  SELECTION_BACKGROUND("selectionBackground"),
  SELECTION_FOREGROUND("selectionForeground"),
  SINGLE_LINE_COMMENT("singleLineComment"),
  SOURCE_HOVER_BACKGROUND("sourceHoverBackground"),
  STATIC_FIELD("staticField"),
  STATIC_FINAL_FIELD("staticFinalField"),
  STATIC_METHOD("staticMethod"),
  STRING("string"),
  TYPE_ARGUMENT("typeArgument"),
  TYPE_PARAMETER("typeParameter"),
  WRITE_OCCURRENCE_INDICATION("writeOccurrenceIndication");
  // TODO(messick): Add additional semantic highlighting keys for Dart elements

  String name;

  ColorThemeKeys(String name) {
    this.name = name;
  }
}
