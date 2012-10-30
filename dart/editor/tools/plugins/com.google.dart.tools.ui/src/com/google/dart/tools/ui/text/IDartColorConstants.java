/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.text;

/**
 * Color keys used for syntax highlighting Dart code and Dartdoc compliant comments. A
 * <code>IColorManager</code> is responsible for mapping concrete colors to these keys.
 * <p>
 * This interface declares static final fields only; it is not intended to be implemented.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves. *
 * 
 * @see com.google.dart.tools.ui.text.config.IColorManager
 * @see com.google.dart.tools.ui.text.config.IColorManagerExtension
 */
public interface IDartColorConstants {

  /**
   * Note: This constant is for internal use only. Clients should not use this constant. The prefix
   * all color constants start with (value <code>"dart_"</code>).
   */
  String PREFIX = "dart_"; //$NON-NLS-1$

  /**
   * The color key for multi-line comments in Dart code (value
   * <code>"dart_multi_line_comment"</code>).
   */
  String JAVA_MULTI_LINE_COMMENT = "dart_multi_line_comment"; //$NON-NLS-1$

  /**
   * The color key for single-line comments in Dart code (value
   * <code>"dart_single_line_comment"</code>).
   */
  String JAVA_SINGLE_LINE_COMMENT = "dart_single_line_comment"; //$NON-NLS-1$

  /**
   * The color key for Dart keywords in Dart code (value <code>"dart_keyword"</code>).
   */
  String JAVA_KEYWORD = "dart_keyword"; //$NON-NLS-1$

  /**
   * The color key for strings in Dart code (value <code>"dart_string"</code>).
   */
  String JAVA_STRING = "dart_string"; //$NON-NLS-1$

  /**
   * The color key for strings in Dart code (value <code>"dart_string"</code>).
   */
  String DART_MULTI_LINE_STRING = "dart_multiline_string"; //$NON-NLS-1$

  /**
   * The color key for raw strings in Dart code (value <code>"dart_raw_string"</code>).
   */
  String DART_RAW_STRING = "dart_raw_string"; //$NON-NLS-1$

  /**
   * The color key for method names in JavaScript code (value <code>"java_method_name"</code>).
   * 
   * @deprecated replaced as of 3.1 by an equivalent semantic highlighting, see
   *             {@link org.eclipse.wst.jsdt.internal.ui.javaeditor.SemanticHighlightings#METHOD}
   */
  @Deprecated
  String JAVA_METHOD_NAME = "java_method_name"; //$NON-NLS-1$

  /**
   * The color key for keyword 'return' in Dart code (value <code>"dart_keyword_return"</code>).
   */
  String JAVA_KEYWORD_RETURN = "dart_keyword_return"; //$NON-NLS-1$

  /**
   * The color key for operators in Dart code (value <code>"dart_operator"</code>).
   */
  String JAVA_OPERATOR = "dart_operator"; //$NON-NLS-1$

  /**
   * The color key for brackets in Dart code (value <code>"dart_bracket"</code> ).
   */
  String JAVA_BRACKET = "dart_bracket"; //$NON-NLS-1$

  /**
   * The color key for everything in Dart code for which no other color is specified (value
   * <code>"dart_default"</code>).
   */
  String JAVA_DEFAULT = "dart_default"; //$NON-NLS-1$

  /**
   * The color key for annotations (value <code>"java_annotation"</code>).
   * 
   * @deprecated replaced as of 3.2 by an equivalent semantic highlighting, see
   *             {@link org.eclipse.wst.jsdt.internal.ui.javaeditor.SemanticHighlightings#ANNOTATION}
   */
  @Deprecated
  String JAVA_ANNOTATION = "java_annotation"; //$NON-NLS-1$

  /**
   * The color key for task tags in Dart comments (value <code>"dart_comment_task_tag"</code>).
   */
  String TASK_TAG = "dart_comment_task_tag"; //$NON-NLS-1$

  /**
   * The color key for JavaDoc keywords (<code>@foo</code>) in JavaDoc comments (value
   * <code>"dart_doc_keyword"</code>).
   */
  String JAVADOC_KEYWORD = "dart_doc_keyword"; //$NON-NLS-1$

  /**
   * The color key for HTML tags (<code>&lt;foo&gt;</code>) in JavaDoc comments (value
   * <code>"java_doc_tag"</code>).
   */
  String JAVADOC_TAG = "dart_doc_tag"; //$NON-NLS-1$

  /**
   * The color key for JavaDoc links (<code>{foo}</code>) in JavaDoc comments (value
   * <code>"java_doc_link"</code>).
   */
  String JAVADOC_LINK = "dart_doc_link"; //$NON-NLS-1$

  /**
   * The color key for everything in DartDoc comments for which no other color is specified (value
   * <code>"dart_doc_default"</code>).
   */
  String JAVADOC_DEFAULT = "dart_doc_default"; //$NON-NLS-1$

  // ---------- Properties File Editor ----------

  /**
   * The color key for keys in a properties file (value <code>"pf_coloring_key"</code>).
   */
  String PROPERTIES_FILE_COLORING_KEY = "pf_coloring_key"; //$NON-NLS-1$

  /**
   * The color key for comments in a properties file (value <code>"pf_coloring_comment"</code>).
   */

  String PROPERTIES_FILE_COLORING_COMMENT = "pf_coloring_comment"; //$NON-NLS-1$

  /**
   * The color key for values in a properties file (value <code>"pf_coloring_value"</code>).
   */
  String PROPERTIES_FILE_COLORING_VALUE = "pf_coloring_value"; //$NON-NLS-1$

  /**
   * The color key for assignment in a properties file. (value <code>"pf_coloring_assignment"</code>
   * ).
   */
  String PROPERTIES_FILE_COLORING_ASSIGNMENT = "pf_coloring_assignment"; //$NON-NLS-1$

  /**
   * The color key for arguments in values in a properties file. (value
   * <code>"pf_coloring_argument"</code>).
   */
  String PROPERTIES_FILE_COLORING_ARGUMENT = "pf_coloring_argument"; //$NON-NLS-1$
}
