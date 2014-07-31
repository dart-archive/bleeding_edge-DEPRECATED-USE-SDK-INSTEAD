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
package com.google.dart.server;

/**
 * The various kinds of completion proposals. Each specifies the kind of completion to be created,
 * corresponding to different syntactical elements.
 * 
 * @coverage dart.server
 */
public enum CompletionSuggestionKind {
  NONE,
  ARGUMENT_LIST,
  CLASS,
  CLASS_ALIAS,
  CONSTRUCTOR,
  FIELD,
  FUNCTION,
  FUNCTION_TYPE_ALIAS,
  GETTER,
  IMPORT,
  LIBRARY_PREFIX,
  METHOD,
  METHOD_NAME,
  NAMED_ARGUMENT,
  OPTIONAL_ARGUMENT,
  PARAMETER,
  SETTER,
  TOP_LEVEL_VARIABLE,
  TYPE_PARAMETER,
  VARIABLE
}
