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
package com.google.dart.server;

/**
 * The enumeration {@code HighlightType} defines the kinds of highlighting that can be associated
 * with a region of text.
 * 
 * @coverage dart.server
 */
public enum HighlightType {
  ANNOTATION,
  BUILT_IN,
  CLASS,
  COMMENT_BLOCK,
  COMMENT_DOCUMENTATION,
  COMMENT_END_OF_LINE,
  CONSTRUCTOR,
  DIRECTIVE,
  DYNAMIC_TYPE,
  FIELD,
  FIELD_STATIC,
  FUNCTION_DECLARATION,
  FUNCTION,
  FUNCTION_TYPE_ALIAS,
  GETTER_DECLARATION,
  KEYWORD,
  IDENTIFIER_DEFAULT,
  IMPORT_PREFIX,
  LITERAL_BOOLEAN,
  LITERAL_DOUBLE,
  LITERAL_INTEGER,
  LITERAL_LIST,
  LITERAL_MAP,
  LITERAL_STRING,
  LOCAL_VARIABLE_DECLARATION,
  LOCAL_VARIABLE,
  METHOD_DECLARATION,
  METHOD_DECLARATION_STATIC,
  METHOD,
  METHOD_STATIC,
  PARAMETER,
  SETTER_DECLARATION,
  TOP_LEVEL_VARIABLE,
  TYPE_NAME_DYNAMIC,
  TYPE_PARAMETER;
}
