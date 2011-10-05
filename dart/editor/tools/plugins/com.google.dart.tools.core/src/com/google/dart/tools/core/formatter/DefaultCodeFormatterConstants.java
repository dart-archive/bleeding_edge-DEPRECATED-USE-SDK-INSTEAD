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
package com.google.dart.tools.core.formatter;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartPreferenceConstants;
import com.google.dart.tools.core.ToolFactory;
import com.google.dart.tools.core.internal.formatter.DefaultCodeFormatterOptions;

import java.util.Map;

/**
 * The class <code>DefaultCodeFormatterConstants</code> defines utility methods and constants used
 * to set up the options of the code formatter.
 */
public final class DefaultCodeFormatterConstants {
  /**
   * <pre>
   * FORMATTER / Value to set a brace location at the end of a line.
   * </pre>
   * 
   * @see #FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER
   * @see #FORMATTER_BRACE_POSITION_FOR_BLOCK
   * @see #FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_SWITCH
   * @see #FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION
   */
  public static final String END_OF_LINE = "end_of_line"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Value to set an option to false.
   * </pre>
   */
  public static final String FALSE = "false"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to align type members of a type declaration on column
   *     - option id:         "com.google.dart.tools.core.formatter.formatter.align_type_members_on_columns"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS = DartCore.PLUGIN_ID
      + ".formatter.align_type_members_on_columns"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option for alignment of arguments in allocation expression
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_arguments_in_allocation_expression"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_arguments_in_allocation_expression"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of arguments in enum constant
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_arguments_in_enum_constant"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ENUM_CONSTANT = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_arguments_in_enum_constant"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of arguments in annotation
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_arguments_in_annotation"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_NO_SPLIT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ANNOTATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_arguments_in_annotation"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of arguments in explicit constructor call
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_arguments_in_explicit_constructor_call"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_arguments_in_explicit_constructor_call"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of arguments in method invocation
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_arguments_in_method_invocation"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_arguments_in_method_invocation"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of arguments in qualified allocation expression
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_arguments_in_qualified_allocation_expression"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_arguments_in_qualified_allocation_expression"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of assignment
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_assignment"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_NO_SPLIT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_ASSIGNMENT = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_assignment"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of binary expression
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_binary_expression"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_binary_expression"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of compact if
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_compact_if"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_ONE_PER_LINE, INDENT_BY_ONE)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_COMPACT_IF = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_compact_if"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of conditional expression
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_conditional_expression"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_ONE_PER_LINE, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_conditional_expression"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of enum constants
   *     - option id:        "com.google.dart.tools.core.formatter.alignment_for_enum_constants"
   *     - possible values:  values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:          createAlignmentValue(false, WRAP_NO_SPLIT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_enum_constants"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of expressions in array initializer
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_expressions_in_array_initializer"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_expressions_in_array_initializer"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_method_declaration"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_NO_SPLIT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_METHOD_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_method_declaration"; //$NON-NLS-1$
  public static final String FORMATTER_ALIGNMENT_FOR_FUNCTION_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_function_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of multiple fields
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_multiple_fields"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_MULTIPLE_FIELDS = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_multiple_fields";//$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of parameters in constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_parameters_in_constructor_declaration"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_parameters_in_constructor_declaration"; //$NON-NLS-1$
  public static final String FORMATTER_ALIGNMENT_FOR_INITIZAIZERS_IN_CONSTRUCTOR_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_initializaer_in_constructor_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of parameters in method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_parameters_in_method_declaration"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_parameters_in_method_declaration"; //$NON-NLS-1$
  public static final String FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_FUNCTION_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_parameters_in_function_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of selector in method invocation
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_selector_in_method_invocation"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_selector_in_method_invocation"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of superclass in type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_superclass_in_type_declaration"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_NEXT_SHIFTED, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_superclass_in_type_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of superinterfaces in enum declaration
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_superinterfaces_in_enum_declaration"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_ENUM_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_superinterfaces_in_enum_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of superinterfaces in type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_superinterfaces_in_type_declaration"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_superinterfaces_in_type_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of throws clause in constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_throws_clause_in_constructor_declaration"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_throws_clause_in_constructor_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option for alignment of throws clause in method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.alignment_for_throws_clause_in_method_declaration"
   *     - possible values:   values returned by <code>createAlignmentValue(boolean, int, int)</code> call
   *     - default:           createAlignmentValue(false, WRAP_COMPACT, INDENT_DEFAULT)
   * </pre>
   * 
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static final String FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.alignment_for_throws_clause_in_method_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to add blank lines after the imports declaration
   *     - option id:         "com.google.dart.tools.core.formatter.blank_lines_after_imports"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_BLANK_LINES_AFTER_IMPORTS = DartCore.PLUGIN_ID
      + ".formatter.blank_lines_after_imports"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to add blank lines after the package declaration
   *     - option id:         "com.google.dart.tools.core.formatter.blank_lines_after_package"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_BLANK_LINES_AFTER_PACKAGE = DartCore.PLUGIN_ID
      + ".formatter.blank_lines_after_package"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to add blank lines at the beginning of the method body
   *     - option id:         "com.google.dart.tools.core.formatter.number_of_blank_lines_at_beginning_of_method_body"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_BLANK_LINES_AT_BEGINNING_OF_METHOD_BODY = DartCore.PLUGIN_ID
      + ".formatter.number_of_blank_lines_at_beginning_of_method_body"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to add blank lines before a field declaration
   *     - option id:         "com.google.dart.tools.core.formatter.blank_lines_before_field"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_BLANK_LINES_BEFORE_FIELD = DartCore.PLUGIN_ID
      + ".formatter.blank_lines_before_field"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to add blank lines before the first class body declaration
   *     - option id:         "com.google.dart.tools.core.formatter.blank_lines_before_first_class_body_declaration"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.blank_lines_before_first_class_body_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to add blank lines before the imports declaration
   *     - option id:         "com.google.dart.tools.core.formatter.blank_lines_before_imports"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_BLANK_LINES_BEFORE_IMPORTS = DartCore.PLUGIN_ID
      + ".formatter.blank_lines_before_imports"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to add blank lines before a member type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.blank_lines_before_member_type"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_BLANK_LINES_BEFORE_MEMBER_TYPE = DartCore.PLUGIN_ID
      + ".formatter.blank_lines_before_member_type"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to add blank lines before a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.blank_lines_before_method"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_BLANK_LINES_BEFORE_METHOD = DartCore.PLUGIN_ID
      + ".formatter.blank_lines_before_method"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to add blank lines before a new chunk
   *     - option id:         "com.google.dart.tools.core.formatter.blank_lines_before_new_chunk"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK = DartCore.PLUGIN_ID
      + ".formatter.blank_lines_before_new_chunk"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to add blank lines before the package declaration
   *     - option id:         "com.google.dart.tools.core.formatter.blank_lines_before_package"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_BLANK_LINES_BEFORE_PACKAGE = DartCore.PLUGIN_ID
      + ".formatter.blank_lines_before_package"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to add blank lines between import groups
   *     - option id:         "com.google.dart.tools.core.formatter.blank_lines_between_import_groups"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "1"
   * </pre>
   * 
   * Note: Import groups are defined once "Organize Import" operation has been executed. The code
   * formatter itself doesn't define the import groups.
   */
  public static final String FORMATTER_BLANK_LINES_BETWEEN_IMPORT_GROUPS = DartCore.PLUGIN_ID
      + ".formatter.blank_lines_between_import_groups"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to add blank lines between type declarations
   *     - option id:         "com.google.dart.tools.core.formatter.blank_lines_between_type_declarations"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_BLANK_LINES_BETWEEN_TYPE_DECLARATIONS = DartCore.PLUGIN_ID
      + ".formatter.blank_lines_between_type_declarations"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to position the braces of an annotation type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.brace_position_for_annotation_type_declaration"
   *     - possible values:   { END_OF_LINE, NEXT_LINE, NEXT_LINE_SHIFTED, NEXT_LINE_ON_WRAP }
   *     - default:           END_OF_LINE
   * </pre>
   * 
   * @see #END_OF_LINE
   * @see #NEXT_LINE
   * @see #NEXT_LINE_SHIFTED
   * @see #NEXT_LINE_ON_WRAP
   */
  public static final String FORMATTER_BRACE_POSITION_FOR_ANNOTATION_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.brace_position_for_annotation_type_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to position the braces of an anonymous type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.brace_position_for_anonymous_type_declaration"
   *     - possible values:   { END_OF_LINE, NEXT_LINE, NEXT_LINE_SHIFTED, NEXT_LINE_ON_WRAP }
   *     - default:           END_OF_LINE
   * </pre>
   * 
   * @see #END_OF_LINE
   * @see #NEXT_LINE
   * @see #NEXT_LINE_SHIFTED
   * @see #NEXT_LINE_ON_WRAP
   */
  public static final String FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.brace_position_for_anonymous_type_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to position the braces of an array initializer
   *     - option id:         "com.google.dart.tools.core.formatter.brace_position_for_array_initializer"
   *     - possible values:   { END_OF_LINE, NEXT_LINE, NEXT_LINE_SHIFTED, NEXT_LINE_ON_WRAP }
   *     - default:           END_OF_LINE
   * </pre>
   * 
   * @see #END_OF_LINE
   * @see #NEXT_LINE
   * @see #NEXT_LINE_SHIFTED
   * @see #NEXT_LINE_ON_WRAP
   */
  public static final String FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER = DartCore.PLUGIN_ID
      + ".formatter.brace_position_for_array_initializer"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to position the braces of a block
   *     - option id:         "com.google.dart.tools.core.formatter.brace_position_for_block"
   *     - possible values:   { END_OF_LINE, NEXT_LINE, NEXT_LINE_SHIFTED, NEXT_LINE_ON_WRAP }
   *     - default:           END_OF_LINE
   * </pre>
   * 
   * @see #END_OF_LINE
   * @see #NEXT_LINE
   * @see #NEXT_LINE_SHIFTED
   * @see #NEXT_LINE_ON_WRAP
   */
  public static final String FORMATTER_BRACE_POSITION_FOR_BLOCK = DartCore.PLUGIN_ID
      + ".formatter.brace_position_for_block"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to position the braces of a block in a case statement when the block is the first statement following
   *             the case
   *     - option id:         "com.google.dart.tools.core.formatter.brace_position_for_block_in_case"
   *     - possible values:   { END_OF_LINE, NEXT_LINE, NEXT_LINE_SHIFTED, NEXT_LINE_ON_WRAP }
   *     - default:           END_OF_LINE
   * </pre>
   * 
   * @see #END_OF_LINE
   * @see #NEXT_LINE
   * @see #NEXT_LINE_SHIFTED
   * @see #NEXT_LINE_ON_WRAP
   */
  public static final String FORMATTER_BRACE_POSITION_FOR_BLOCK_IN_CASE = DartCore.PLUGIN_ID
      + ".formatter.brace_position_for_block_in_case"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to position the braces of a constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.brace_position_for_constructor_declaration"
   *     - possible values:   { END_OF_LINE, NEXT_LINE, NEXT_LINE_SHIFTED, NEXT_LINE_ON_WRAP }
   *     - default:           END_OF_LINE
   * </pre>
   * 
   * @see #END_OF_LINE
   * @see #NEXT_LINE
   * @see #NEXT_LINE_SHIFTED
   * @see #NEXT_LINE_ON_WRAP
   */
  public static final String FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.brace_position_for_constructor_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to position the braces of an enum constant
   *     - option id:         "com.google.dart.tools.core.formatter.brace_position_for_enum_constant"
   *     - possible values:   { END_OF_LINE, NEXT_LINE, NEXT_LINE_SHIFTED, NEXT_LINE_ON_WRAP }
   *     - default:           END_OF_LINE
   * </pre>
   * 
   * @see #END_OF_LINE
   * @see #NEXT_LINE
   * @see #NEXT_LINE_SHIFTED
   * @see #NEXT_LINE_ON_WRAP
   */
  public static final String FORMATTER_BRACE_POSITION_FOR_ENUM_CONSTANT = DartCore.PLUGIN_ID
      + ".formatter.brace_position_for_enum_constant"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to position the braces of an enum declaration
   *     - option id:         "com.google.dart.tools.core.formatter.brace_position_for_enum_declaration"
   *     - possible values:   { END_OF_LINE, NEXT_LINE, NEXT_LINE_SHIFTED, NEXT_LINE_ON_WRAP }
   *     - default:           END_OF_LINE
   * </pre>
   * 
   * @see #END_OF_LINE
   * @see #NEXT_LINE
   * @see #NEXT_LINE_SHIFTED
   * @see #NEXT_LINE_ON_WRAP
   */
  public static final String FORMATTER_BRACE_POSITION_FOR_ENUM_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.brace_position_for_enum_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to position the braces of a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.brace_position_for_method_declaration"
   *     - possible values:   { END_OF_LINE, NEXT_LINE, NEXT_LINE_SHIFTED, NEXT_LINE_ON_WRAP }
   *     - default:           END_OF_LINE
   * </pre>
   * 
   * @see #END_OF_LINE
   * @see #NEXT_LINE
   * @see #NEXT_LINE_SHIFTED
   * @see #NEXT_LINE_ON_WRAP
   */
  public static final String FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.brace_position_for_method_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to position the braces of a switch statement
   *     - option id:         "com.google.dart.tools.core.formatter.brace_position_for_switch"
   *     - possible values:   { END_OF_LINE, NEXT_LINE, NEXT_LINE_SHIFTED, NEXT_LINE_ON_WRAP }
   *     - default:           END_OF_LINE
   * </pre>
   * 
   * @see #END_OF_LINE
   * @see #NEXT_LINE
   * @see #NEXT_LINE_SHIFTED
   * @see #NEXT_LINE_ON_WRAP
   */
  public static final String FORMATTER_BRACE_POSITION_FOR_SWITCH = DartCore.PLUGIN_ID
      + ".formatter.brace_position_for_switch"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to position the braces of a type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.brace_position_for_type_declaration"
   *     - possible values:   { END_OF_LINE, NEXT_LINE, NEXT_LINE_SHIFTED, NEXT_LINE_ON_WRAP }
   *     - default:           END_OF_LINE
   * </pre>
   * 
   * @see #END_OF_LINE
   * @see #NEXT_LINE
   * @see #NEXT_LINE_SHIFTED
   * @see #NEXT_LINE_ON_WRAP
   */
  public static final String FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.brace_position_for_type_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether blank lines are cleared inside javadoc comments
   *     - option id:         "com.google.dart.tools.core.formatter.comment.clear_blank_lines_in_javadoc_comment"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT = DartCore.PLUGIN_ID
      + ".formatter.comment.clear_blank_lines_in_javadoc_comment"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether blank lines are cleared inside block comments
   *     - option id:         "com.google.dart.tools.core.formatter.comment.clear_blank_lines_in_block_comment"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT = DartCore.PLUGIN_ID
      + ".formatter.comment.clear_blank_lines_in_block_comment"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether single line comments are formatted
   *     - option id:         "com.google.dart.tools.core.formatter.comment.format_line_comments"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_FORMAT_LINE_COMMENT = DartCore.PLUGIN_ID
      + ".formatter.comment.format_line_comments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to format line comments that start on the first column
   *     - option id:         "com.google.dart.tools.core.formatter.format_line_comment_starting_on_first_column"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * Note that this option is ignored if either the {@link #FORMATTER_COMMENT_FORMAT_LINE_COMMENT}
   * option has been set to {@link #FALSE} or the formatter is created with the mode
   * {@link ToolFactory#M_FORMAT_NEW}.
   * 
   * @see #TRUE
   * @see #FALSE
   * @see ToolFactory#createCodeFormatter(Map, int)
   */
  public static final String FORMATTER_COMMENT_FORMAT_LINE_COMMENT_STARTING_ON_FIRST_COLUMN = DartCore.PLUGIN_ID
      + ".formatter.format_line_comment_starting_on_first_column"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether multiple lines comments are formatted
   *     - option id:         "com.google.dart.tools.core.formatter.comment.format_block_comments"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT = DartCore.PLUGIN_ID
      + ".formatter.comment.format_block_comments"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether javadoc comments are formatted
   *     - option id:         "com.google.dart.tools.core.formatter.comment.format_javadoc_comments"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT = DartCore.PLUGIN_ID
      + ".formatter.comment.format_javadoc_comments"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether the header comment of a Dart source file is formatted
   *     - option id:         "com.google.dart.tools.core.formatter.comment.format_header"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_FORMAT_HEADER = DartCore.PLUGIN_ID
      + ".formatter.comment.format_header"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether HTML tags are formatted.
   *     - option id:         "com.google.dart.tools.core.formatter.comment.format_html"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_FORMAT_HTML = DartCore.PLUGIN_ID
      + ".formatter.comment.format_html"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether code snippets are formatted in comments
   *     - option id:         "com.google.dart.tools.core.formatter.comment.format_source_code"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_FORMAT_SOURCE = DartCore.PLUGIN_ID
      + ".formatter.comment.format_source_code"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether description of Javadoc parameters are indented
   *     - option id:         "com.google.dart.tools.core.formatter.comment.indent_parameter_description"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION = DartCore.PLUGIN_ID
      + ".formatter.comment.indent_parameter_description"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether Javadoc root tags are indented.
   *     - option id:         "com.google.dart.tools.core.formatter.comment.indent_root_tags"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_INDENT_ROOT_TAGS = DartCore.PLUGIN_ID
      + ".formatter.comment.indent_root_tags"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert an empty line before the Javadoc root tag block
   *     - option id:         "com.google.dart.tools.core.formatter.comment.insert_new_line_before_root_tags"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public final static String FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS = DartCore.PLUGIN_ID
      + ".formatter.comment.insert_new_line_before_root_tags"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a new line after Javadoc root tag parameters
   *     - option id:         "com.google.dart.tools.core.formatter.comment.insert_new_line_for_parameter"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public final static String FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER = DartCore.PLUGIN_ID
      + ".formatter.comment.insert_new_line_for_parameter"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to specify the line length for comments.
   *     - option id:         "com.google.dart.tools.core.formatter.comment.line_length"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "80"
   * </pre>
   */
  public final static String FORMATTER_COMMENT_LINE_LENGTH = DartCore.PLUGIN_ID
      + ".formatter.comment.line_length"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether block comments will have new lines at boundaries
   *     - option id:         "com.google.dart.tools.core.formatter.comment.new_lines_at_block_boundaries"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_NEW_LINES_AT_BLOCK_BOUNDARIES = DartCore.PLUGIN_ID
      + ".formatter.comment.new_lines_at_block_boundaries"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to control whether javadoc comments will have new lines at boundaries
   *     - option id:         "com.google.dart.tools.core.formatter.comment.new_lines_at_javadoc_boundaries"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public final static String FORMATTER_COMMENT_NEW_LINES_AT_JAVADOC_BOUNDARIES = DartCore.PLUGIN_ID
      + ".formatter.comment.new_lines_at_javadoc_boundaries"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to compact else/if
   *     - option id:         "com.google.dart.tools.core.formatter.compact_else_if"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_COMPACT_ELSE_IF = DartCore.PLUGIN_ID
      + ".formatter.compact_else_if"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to set the continuation indentation
   *     - option id:         "com.google.dart.tools.core.formatter.continuation_indentation"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "2"
   * </pre>
   */
  public static final String FORMATTER_CONTINUATION_INDENTATION = DartCore.PLUGIN_ID
      + ".formatter.continuation_indentation"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to set the continuation indentation inside array initializer
   *     - option id:         "com.google.dart.tools.core.formatter.continuation_indentation_for_array_initializer"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "2"
   * </pre>
   */
  public static final String FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER = DartCore.PLUGIN_ID
      + ".formatter.continuation_indentation_for_array_initializer"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to use the disabling and enabling tags defined respectively by the {@link #FORMATTER_DISABLING_TAG} and the {@link #FORMATTER_ENABLING_TAG} options.
   *     - option id:         "com.google.dart.tools.core.formatter.use_on_off_tags"
   *     - possible values:   TRUE / FALSE
   *     - default:           FALSE
   * </pre>
   */
  public static final String FORMATTER_USE_ON_OFF_TAGS = DartCore.PLUGIN_ID
      + ".formatter.use_on_off_tags"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to define the tag to put in a comment to disable the formatting.
   *     - option id:         "com.google.dart.tools.core.formatter.disabling_tag"
   *     - possible values:   String, with constraints mentioned below
   *     - default:           "@formatter:off"
   * 
   * See the {@link #FORMATTER_ENABLING_TAG} option to re-enable it.
   * </pre>
   * <p>
   * Note that:
   * <ol>
   * <li>This tag is used by the formatter only if the {@link #FORMATTER_USE_ON_OFF_TAGS} option is
   * set to {@link #TRUE}.</li>
   * <li>The tag name will be trimmed. Hence if it does contain white spaces at the beginning or at
   * the end, they will not be taken into account while searching for the tag in the comments</li>
   * <li>If a tag is starting with a letter or digit, then it cannot be leaded by another letter or
   * digit to be recognized ( <code>"ToDisableFormatter"</code> will not be recognized as a
   * disabling tag <code>"DisableFormatter"</code>, but <code>"To:DisableFormatter"</code> will be
   * detected for either tag <code>"DisableFormatter"</code> or <code>":DisableFormatter"</code>).<br>
   * Respectively, a tag ending with a letter or digit cannot be followed by a letter or digit to be
   * recognized (<code>"DisableFormatter1"</code> will not be recognized as a disabling tag
   * <code>"DisableFormatter"</code>, but <code>"DisableFormatter:1"</code> will be detected either
   * for tag <code>"DisableFormatter"</code> or <code>"DisableFormatter:"</code>)</li>
   * <li>As soon as the formatter encounters the defined disabling tag, it stops to format the code
   * from the beginning of the comment including this tag. If it was already disabled, the tag has
   * no special effect.
   * <p>
   * For example, the second default enabling tag &quot;<b>@formatter:off</b>&quot; in the following
   * snippet is not necessary as the formatter was already disabled since the first one:
   * 
   * <pre>
   * class X {
   *   // @formatter:off
   *   void foo1() {
   *   }
   * 
   *   // @formatter:off
   *   void foo2() {
   *   }
   * 
   *   void bar1() {
   *   }
   * 
   *   void bar2() {
   *   }
   * }
   * </pre>
   * </p>
   * </li>
   * <li>If no enabling tag is found by the formatter after the disabling tag, then the end of the
   * snippet won't be formatted.<br>
   * For example, when a disabling tag is put at the beginning of the code, then the entire content
   * of a compilation unit is not formatted:
   * 
   * <pre>
   * // @formatter:off
   * class X {
   *   void foo1() {
   *   }
   * 
   *   void foo2() {
   *   }
   * 
   *   void bar1() {
   *   }
   * 
   *   void bar2() {
   *   }
   * }
   * </pre>
   * </li>
   * <li>If a mix of disabling and enabling tags is done in the same comment, then the formatter
   * will only take into account the last encountered tag in the comment.
   * <p>
   * For example, in the following snippet, the formatter will be disabled after the comment:
   * </p>
   * 
   * <pre>
   * class X {
   *   /*
   *    * This is a comment with a mix of disabling and enabling tags: -
   *    * &lt;b&gt;@formatter:off&lt;/b&gt; - &lt;b&gt;@formatter:on&lt;/b&gt; - &lt;b&gt;@formatter:off&lt;/b&gt; The
   *    * formatter will stop to format from the beginning of this comment...
   *    &#42;/
   *   void foo() {
   *   }
   * 
   *   void bar() {
   *   }
   * }
   * </pre>
   * </li>
   * <li>The tag cannot include newline character (i.e. '\n') but it can have white spaces.<br>
   * E.g. "<b>format: off</b>" is a valid disabling tag.<br>
   * In the future, newlines may be used to support multiple disabling tags.</li>
   * <li>The tag can include line or block comments start/end tokens.
   * <p>
   * If such tags are used, e.g. "<b>//J-</b>", then the single comment can also stop the formatting
   * as shown in the following snippet:
   * </p>
   * 
   * <pre>
   * //J-
   * // Formatting was stopped from comment above...
   * public class X {
   * //J+
   * // Formatting is restarted from here...
   * void foo() {}
   * </pre>
   * <p>
   * As any disabling tags, as soon as a comment includes it, the formatting stops from this
   * comment:
   * </p>
   * 
   * <pre>
   * public class X {
   *   // Line comment including the disabling tag: //J-
   *   // Formatting was stopped from comment above...
   *   void foo1() {
   *   }
   * 
   *   // J+
   *   // Formatting restarts from here...
   *   void bar1() {
   *   }
   * 
   *   /*
   *    * Block comment including the disabling tag: //J+ The formatter stops from
   *    * this comment...
   *    &#42;/
   *   void foo2() {
   *   }
   * 
   *   // J+
   *   // Formatting restarts from here...
   *   void bar2() {
   *   }
   * 
   *   /**
   *    * Javadoc comment including the enabling tag: //J+ The formatter stops from
   *    * this comment...
   *    &#42;/
   *   void foo3() {
   *   }
   * }
   * </pre>
   * </li>
   * </ol>
   * </p>
   */
  public static final String FORMATTER_DISABLING_TAG = DartCore.PLUGIN_ID
      + ".formatter.disabling_tag"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to define the tag to put in a comment to re-enable the formatting after it has been disabled (see {@link #FORMATTER_DISABLING_TAG})
   *     - option id:         "com.google.dart.tools.core.formatter.enabling_tag"
   *     - possible values:   String, with constraints mentioned below
   *     - default:           "@formatter:on"
   * </pre>
   * <p>
   * Note that:
   * <ol>
   * <li>This tag is used by the formatter only if the {@link #FORMATTER_USE_ON_OFF_TAGS} option is
   * set to {@link #TRUE}.</li>
   * <li>The tag name will be trimmed. Hence if it does contain white spaces at the beginning or at
   * the end, they will not be taken into account while searching for the tag in the comments</li>
   * <li>If a tag is starting with a letter or digit, then it cannot be leaded by another letter or
   * digit to be recognized ( <code>"ReEnableFormatter"</code> will not be recognized as an enabling
   * tag <code>"EnableFormatter"</code>, but <code>"Re:EnableFormatter"</code> will be detected for
   * either tag <code>"EnableFormatter"</code> or <code>":EnableFormatter"</code>).<br>
   * Respectively, a tag ending with a letter or digit cannot be followed by a letter or digit to be
   * recognized (<code>"EnableFormatter1"</code> will not be recognized as an enabling tag
   * <code>"EnableFormatter"</code>, but <code>"EnableFormatter:1"</code> will be detected either
   * for tag <code>"EnableFormatter"</code> or <code>"EnableFormatter:"</code>)</li>
   * <li>As soon as the formatter encounters the defined enabling tag, it re-starts to format the
   * code just after the comment including this tag. If it was already active, i.e. already
   * re-enabled or never disabled, the tag has no special effect.
   * <p>
   * For example, the default enabling tag &quot;<b>@formatter:on</b>&quot; in the following snippet
   * is not necessary as the formatter has never been disabled:
   * 
   * <pre>
   * class X {
   *   void foo1() {
   *   }
   * 
   *   void foo2() {
   *   }
   * 
   *   // @formatter:on
   *   void bar1() {
   *   }
   * 
   *   void bar2() {
   *   }
   * }
   * </pre>
   * Or, in the following other snippet, the second enabling tag is not necessary as the formatting
   * will have been re-enabled by the first one:
   * 
   * <pre>
   * class X {
   *   // @formatter:off
   *   void foo1() {
   *   }
   * 
   *   void foo2() {
   *   }
   * 
   *   // @formatter:on
   *   void bar1() {
   *   }
   * 
   *   // @formatter:on
   *   void bar2() {
   *   }
   * }
   * </pre>
   * </p>
   * </li>
   * <li>If a mix of disabling and enabling tags is done in the same comment, then the formatter
   * will only take into account the last encountered tag in the comment.
   * <p>
   * For example, in the following snippet, the formatter will be re-enabled after the comment:
   * </p>
   * 
   * <pre>
   * // @formatter:off
   * class X {
   *   /*
   *    * This is a comment with a mix of disabling and enabling tags: -
   *    * &lt;b&gt;@formatter:on&lt;/b&gt; - &lt;b&gt;@formatter:off&lt;/b&gt; - &lt;b&gt;@formatter:on&lt;/b&gt; The
   *    * formatter will restart to format after this comment...
   *    &#42;/
   *   void foo() {
   *   }
   * 
   *   void bar() {
   *   }
   * }
   * </pre>
   * </li>
   * <li>The tag cannot include newline character (i.e. '\n') but it can have white spaces.<br>
   * E.g. "<b>format: on</b>" is a valid enabling tag<br>
   * In the future, newlines may be used to support multiple enabling tags.</li>
   * <li>The tag can include line or block comments start/end tokens. Javadoc tokens are not
   * considered as valid tags.
   * <p>
   * If such tags are used, e.g. "<b>//J+</b>", then the single comment can also start the
   * formatting as shown in the following snippet:
   * </p>
   * 
   * <pre>
   * // J-
   * // Formatting was stopped from comment above...
   * public class X {
   *   // J+
   *   // Formatting restarts from here...
   *   void foo() {
   *   }
   * }
   * </pre>
   * <p>
   * As any enabling tags, as soon as a comment includes it, the formatting restarts just after the
   * comment:
   * </p>
   * 
   * <pre>
   * public class X {
   *   // J-
   *   // Formatting was stopped from comment above...
   *   void foo1() {
   *   }
   * 
   *   // Line comment including the enabling tag: //J+
   *   // Formatting restarts from here...
   *   void bar1() {
   *   }
   * 
   *   // J-
   *   // Formatting was stopped from comment above...
   *   void foo2() {
   *   }
   * 
   *   /*
   *    * Block comment including the enabling tag: //J+ The formatter restarts after
   *    * this comment...
   *    &#42;/
   *   // Formatting restarts from here...
   *   void bar2() {
   *   }
   * 
   *   // J-
   *   // Formatting was stopped from comment above...
   *   void foo3() {
   *   }
   * 
   *   /**
   *    * Javadoc comment including the enabling tag: //J+ The formatter restarts
   *    * after this comment...
   *    &#42;/
   *   void bar3() {
   *   }
   * }
   * </pre>
   * </li>
   * </ol>
   * </p>
   */
  public static final String FORMATTER_ENABLING_TAG = DartCore.PLUGIN_ID
      + ".formatter.enabling_tag"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to indent body declarations compare to its enclosing annotation declaration header
   *     - option id:         "com.google.dart.tools.core.formatter.indent_body_declarations_compare_to_annotation_declaration_header"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ANNOTATION_DECLARATION_HEADER = DartCore.PLUGIN_ID
      + ".formatter.indent_body_declarations_compare_to_annotation_declaration_header"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to indent body declarations compare to its enclosing enum constant header
   *     - option id:         "com.google.dart.tools.core.formatter.indent_body_declarations_compare_to_enum_constant_header"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_CONSTANT_HEADER = DartCore.PLUGIN_ID
      + ".formatter.indent_body_declarations_compare_to_enum_constant_header"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to indent body declarations compare to its enclosing enum declaration header
   *     - option id:         "com.google.dart.tools.core.formatter.indent_body_declarations_compare_to_enum_declaration_header"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_DECLARATION_HEADER = DartCore.PLUGIN_ID
      + ".formatter.indent_body_declarations_compare_to_enum_declaration_header"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to indent body declarations compare to its enclosing type header
   *     - option id:         "com.google.dart.tools.core.formatter.indent_body_declarations_compare_to_type_header"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER = DartCore.PLUGIN_ID
      + ".formatter.indent_body_declarations_compare_to_type_header"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to indent breaks compare to cases
   *     - option id:         "com.google.dart.tools.core.formatter.indent_breaks_compare_to_cases"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_INDENT_BREAKS_COMPARE_TO_CASES = DartCore.PLUGIN_ID
      + ".formatter.indent_breaks_compare_to_cases"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to indent empty lines
   *     - option id:         "com.google.dart.tools.core.formatter.indent_empty_lines"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_INDENT_EMPTY_LINES = DartCore.PLUGIN_ID
      + ".formatter.indent_empty_lines"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to indent statements inside a block
   *     - option id:         "com.google.dart.tools.core.formatter.indent_statements_compare_to_block"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BLOCK = DartCore.PLUGIN_ID
      + ".formatter.indent_statements_compare_to_block"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to indent statements inside the body of a method or a constructor
   *     - option id:         "com.google.dart.tools.core.formatter.indent_statements_compare_to_body"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BODY = DartCore.PLUGIN_ID
      + ".formatter.indent_statements_compare_to_body"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to indent switch statements compare to cases
   *     - option id:         "com.google.dart.tools.core.formatter.indent_switchstatements_compare_to_cases"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES = DartCore.PLUGIN_ID
      + ".formatter.indent_switchstatements_compare_to_cases"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to indent switch statements compare to switch
   *     - option id:         "com.google.dart.tools.core.formatter.indent_switchstatements_compare_to_switch"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH = DartCore.PLUGIN_ID
      + ".formatter.indent_switchstatements_compare_to_switch"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to specify the equivalent number of spaces that represents one indentation
   *     - option id:         "com.google.dart.tools.core.formatter.indentation.size"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "4"
   * </pre>
   * <p>
   * This option is used only if the tab char is set to MIXED.
   * </p>
   * 
   * @see #FORMATTER_TAB_CHAR
   */
  public static final String FORMATTER_INDENTATION_SIZE = DartCore.PLUGIN_ID
      + ".formatter.indentation.size"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a new line after an annotation on a field declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_after_annotation_on_field"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_FIELD = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_after_annotation_on_field";//$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a new line after an annotation on a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_after_annotation_on_method"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_METHOD = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_after_annotation_on_method";//$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a new line after an annotation on a package declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_after_annotation_on_package"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PACKAGE = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_after_annotation_on_package";//$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a new line after an annotation on a type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_after_annotation_on_type"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_TYPE = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_after_annotation_on_type";//$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a new line after an annotation on a parameter
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_after_annotation_on_parameter"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PARAMETER = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_after_annotation_on_parameter";//$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a new line after an annotation on a local variable
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_after_annotation_on_local_variable"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_after_annotation_on_local_variable";//$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a new line after a label
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_after_label"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_AFTER_LABEL = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_after_label"; //$NON-NLS-1$ 

  /**
   * <pre>
   * FORMATTER / Option to insert a new line after the opening brace in an array initializer
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_after_opening_brace_in_array_initializer"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_after_opening_brace_in_array_initializer";//$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a new line at the end of the current file if missing
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_at_end_of_file_if_missing"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_AT_END_OF_FILE_IF_MISSING = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_at_end_of_file_if_missing";//$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line before the catch keyword in try statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_before_catch_in_try_statement"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_before_catch_in_try_statement"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line before the closing brace in an array initializer
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_before_closing_brace_in_array_initializer"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_before_closing_brace_in_array_initializer";//$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line before the else keyword in if statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_before_else_in_if_statement"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_before_else_in_if_statement"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line before the finally keyword in try statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_before_finally_in_try_statement"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_before_finally_in_try_statement"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line before while in do statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_before_while_in_do_statement"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_before_while_in_do_statement"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line in an empty annotation declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_in_empty_annotation_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANNOTATION_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_in_empty_annotation_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line in an empty anonymous type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_in_empty_anonymous_type_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_in_empty_anonymous_type_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line in an empty block
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_in_empty_block"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_in_empty_block"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line in an empty enum constant
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_in_empty_enum_constant"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_CONSTANT = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_in_empty_enum_constant"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line in an empty enum declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_in_empty_enum_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_in_empty_enum_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line in an empty method body
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_in_empty_method_body"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_METHOD_BODY = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_in_empty_method_body"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a new line in an empty type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_new_line_in_empty_type_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_new_line_in_empty_type_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after and in wilcard
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_and_in_type_parameter"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_AND_IN_TYPE_PARAMETER = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_and_in_type_parameter"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after an assignment operator
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_assignment_operator"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_assignment_operator"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after at in annotation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_at_in_annotation"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_at_in_annotation"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after at in annotation type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_at_in_annotation_type_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_at_in_annotation_type_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after a binary operator
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_binary_operator"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_binary_operator"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the closing angle bracket in type arguments
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_closing_angle_bracket_in_type_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_closing_angle_bracket_in_type_arguments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the closing angle bracket in type parameters
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_closing_angle_bracket_in_type_parameters"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_closing_angle_bracket_in_type_parameters"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the closing brace of a block
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_closing_brace_in_block"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_closing_brace_in_block"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the closing parenthesis of a cast expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_closing_paren_in_cast"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_closing_paren_in_cast"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the colon in an assert statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_colon_in_assert"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COLON_IN_ASSERT = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_colon_in_assert"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after colon in a case statement when a opening brace follows the colon
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_colon_in_case"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CASE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_colon_in_case"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the colon in a conditional expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_colon_in_conditional"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_colon_in_conditional"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after colon in a for statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_colon_in_for"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COLON_IN_FOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_colon_in_for"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the colon in a labeled statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_colon_in_labeled_statement"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COLON_IN_LABELED_STATEMENT = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_colon_in_labeled_statement"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in an allocation expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_allocation_expression"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_allocation_expression"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in annotation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_annotation"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ANNOTATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_annotation"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in an array initializer
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_array_initializer"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_array_initializer"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in the parameters of a constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_constructor_declaration_parameters"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_constructor_declaration_parameters"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in the exception names in a throws clause of a constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_constructor_declaration_throws"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_constructor_declaration_throws"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in the arguments of an enum constant
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_enum_constant_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_CONSTANT_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_enum_constant_arguments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in enum declarations
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_enum_declarations"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_DECLARATIONS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_enum_declarations"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in the arguments of an explicit constructor call
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_explicitconstructorcall_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_explicitconstructorcall_arguments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in the increments of a for statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_for_increments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_for_increments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in the initializations of a for statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_for_inits"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_for_inits"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in the parameters of a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_method_declaration_parameters"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_method_declaration_parameters"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in the exception names in a throws clause of a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_method_declaration_throws"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_method_declaration_throws"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in the arguments of a method invocation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_method_invocation_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_method_invocation_arguments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in multiple field declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_multiple_field_declarations"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_multiple_field_declarations"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in multiple local declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_multiple_local_declarations"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_multiple_local_declarations"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in parameterized type reference
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_parameterized_type_reference"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_parameterized_type_reference"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in superinterfaces names of a type header
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_superinterfaces"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_superinterfaces"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in type arguments
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_type_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_type_arguments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the comma in type parameters
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_comma_in_type_parameters"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_PARAMETERS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_comma_in_type_parameters"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after ellipsis
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_ellipsis"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_ELLIPSIS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_ellipsis"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening angle bracket in parameterized type reference
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_angle_bracket_in_parameterized_type_reference"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_angle_bracket_in_parameterized_type_reference";//$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening angle bracket in type arguments
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_angle_bracket_in_type_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_angle_bracket_in_type_arguments";//$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening angle bracket in type parameters
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_angle_bracket_in_type_parameters"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_angle_bracket_in_type_parameters";//$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening brace in an array initializer
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_brace_in_array_initializer"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_brace_in_array_initializer"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening bracket inside an array allocation expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_bracket_in_array_allocation_expression"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_bracket_in_array_allocation_expression";//$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening bracket inside an array reference
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_bracket_in_array_reference"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_bracket_in_array_reference";//$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in annotation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_annotation"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ANNOTATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_annotation"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in a cast expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_cast"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CAST = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_cast"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in a catch
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_catch"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_catch"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in a constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_constructor_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_constructor_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in enum constant
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_enum_constant"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ENUM_CONSTANT = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_enum_constant"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in a for statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_for"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_for"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in an if statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_if"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_if"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_method_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_method_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in a method invocation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_method_invocation"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_method_invocation"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in a parenthesized expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_parenthesized_expression"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_parenthesized_expression"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in a switch statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_switch"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_switch"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in a synchronized statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_synchronized"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_synchronized"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after the opening parenthesis in a while statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_opening_paren_in_while"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_opening_paren_in_while"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after a postfix operator
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_postfix_operator"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_POSTFIX_OPERATOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_postfix_operator"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after a prefix operator
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_prefix_operator"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_PREFIX_OPERATOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_prefix_operator"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after question mark in a conditional expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_question_in_conditional"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_question_in_conditional"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after question mark in a wildcard
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_question_in_wildcard"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_WILDCARD = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_question_in_wildcard"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after semicolon in a for statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_semicolon_in_for"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_semicolon_in_for"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space after an unary operator
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_after_unary_operator"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_after_unary_operator"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before and in wildcard
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_and_in_type_parameter"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_AND_IN_TYPE_PARAMETER = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_and_in_type_parameter"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before an assignment operator
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_assignment_operator"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_assignment_operator"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before at in annotation type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_at_in_annotation_type_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_AT_IN_ANNOTATION_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_at_in_annotation_type_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before an binary operator
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_binary_operator"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_binary_operator"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing angle bracket in parameterized type reference
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_angle_bracket_in_parameterized_type_reference"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_angle_bracket_in_parameterized_type_reference"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing angle bracket in type arguments
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_angle_bracket_in_type_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_angle_bracket_in_type_arguments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing angle bracket in type parameters
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_angle_bracket_in_type_parameters"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_angle_bracket_in_type_parameters"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing brace in an array initializer
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_brace_in_array_initializer"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_brace_in_array_initializer"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing bracket in an array allocation expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_bracket_in_array_allocation_expression"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_bracket_in_array_allocation_expression";//$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing bracket in an array reference
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_bracket_in_array_reference"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_bracket_in_array_reference";//$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in annotation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_annotation"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ANNOTATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_annotation"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in a cast expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_cast"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CAST = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_cast"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in a catch
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_catch"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_catch"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in a constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_constructor_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_constructor_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in enum constant
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_enum_constant"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ENUM_CONSTANT = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_enum_constant"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in a for statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_for"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_for"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in an if statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_if"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_if"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_method_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_method_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in a method invocation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_method_invocation"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_method_invocation"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in a parenthesized expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_parenthesized_expression"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_PARENTHESIZED_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_parenthesized_expression"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in a switch statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_switch"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_switch"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in a synchronized statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_synchronized"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_synchronized"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the closing parenthesis in a while statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_closing_paren_in_while"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_closing_paren_in_while"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before colon in an assert statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_colon_in_assert"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_ASSERT = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_colon_in_assert"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before colon in a case statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_colon_in_case"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_colon_in_case"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before colon in a conditional expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_colon_in_conditional"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_colon_in_conditional"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before colon in a default statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_colon_in_default"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_colon_in_default"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before colon in a for statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_colon_in_for"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_FOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_colon_in_for"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before colon in a labeled statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_colon_in_labeled_statement"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_LABELED_STATEMENT = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_colon_in_labeled_statement"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in an allocation expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_allocation_expression"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ALLOCATION_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_allocation_expression"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in annotation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_annotation"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ANNOTATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_annotation"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in an array initializer
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_array_initializer"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ARRAY_INITIALIZER = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_array_initializer"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in the parameters of a constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_constructor_declaration_parameters"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_constructor_declaration_parameters"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in the exception names of the throws clause of a constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_constructor_declaration_throws"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_constructor_declaration_throws"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in the arguments of enum constant
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_enum_constant_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_CONSTANT_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_enum_constant_arguments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in enum declarations
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_enum_declarations"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_DECLARATIONS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_enum_declarations"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in the arguments of an explicit constructor call
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_explicitconstructorcall_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_explicitconstructorcall_arguments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in the increments of a for statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_for_increments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INCREMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_for_increments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in the initializations of a for statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_for_inits"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INITS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_for_inits"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in the parameters of a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_method_declaration_parameters"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_method_declaration_parameters"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in the exception names of the throws clause of a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_method_declaration_throws"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_method_declaration_throws"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in the arguments of a method invocation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_method_invocation_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_method_invocation_arguments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in a multiple field declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_multiple_field_declarations"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_multiple_field_declarations"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in a multiple local declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_multiple_local_declarations"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_multiple_local_declarations"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in parameterized type reference
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_parameterized_type_reference"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_parameterized_type_reference"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in the superinterfaces names in a type header
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_superinterfaces"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_superinterfaces"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in type arguments
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_type_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_type_arguments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before comma in type parameters
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_comma_in_type_parameters"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_PARAMETERS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_comma_in_type_parameters"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before ellipsis
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_ellipsis"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_ELLIPSIS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_ellipsis"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening angle bracket in parameterized type reference
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_angle_bracket_in_parameterized_type_reference"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_angle_bracket_in_parameterized_type_reference"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening angle bracket in type arguments
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_angle_bracket_in_type_arguments"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_angle_bracket_in_type_arguments"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening angle bracket in type parameters
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_angle_bracket_in_type_parameters"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_angle_bracket_in_type_parameters"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening brace in an annotation type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_brace_in_annotation_type_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANNOTATION_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_brace_in_annotation_type_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening brace in an anonymous type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_brace_in_anonymous_type_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_brace_in_anonymous_type_declaration"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening brace in an array initializer
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_brace_in_array_initializer"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_brace_in_array_initializer"; //$NON-NLS-1$
  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening brace in a block
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_brace_in_block"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_brace_in_block"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening brace in a constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_brace_in_constructor_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_brace_in_constructor_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening brace in an enum constant
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_brace_in_enum_constant"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_CONSTANT = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_brace_in_enum_constant"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening brace in an enum declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_brace_in_enum_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_brace_in_enum_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening brace in a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_brace_in_method_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_brace_in_method_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening brace in a switch statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_brace_in_switch"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_brace_in_switch"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening brace in a type declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_brace_in_type_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_brace_in_type_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening bracket in an array allocation expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_bracket_in_array_allocation_expression"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_bracket_in_array_allocation_expression";//$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening bracket in an array reference
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_bracket_in_array_reference"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_bracket_in_array_reference";//$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening bracket in an array type reference
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_bracket_in_array_type_reference"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_bracket_in_array_type_reference"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in annotation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_annotation"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_annotation"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in annotation type member declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_annotation_type_member_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION_TYPE_MEMBER_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_annotation_type_member_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in a catch
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_catch"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_catch"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in a constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_constructor_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_constructor_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in enum constant
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_enum_constant"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ENUM_CONSTANT = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_enum_constant"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in a for statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_for"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_for"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in an if statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_if"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_if"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_method_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_method_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in a method invocation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_method_invocation"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_method_invocation"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in a parenthesized expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_parenthesized_expression"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_parenthesized_expression"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in a switch statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_switch"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_switch"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in a synchronized statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_synchronized"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_synchronized"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before the opening parenthesis in a while statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_opening_paren_in_while"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_opening_paren_in_while"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before parenthesized expression in return statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_parenthesized_expression_in_return"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_RETURN = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_parenthesized_expression_in_return"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before parenthesized expression in throw statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_parenthesized_expression_in_throw"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_THROW = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_parenthesized_expression_in_throw"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before a postfix operator
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_postfix_operator"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT\
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_POSTFIX_OPERATOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_postfix_operator"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before a prefix operator
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_prefix_operator"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_PREFIX_OPERATOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_prefix_operator"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before question mark in a conditional expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_question_in_conditional"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_question_in_conditional"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before question mark in a wildcard
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_question_in_wildcard"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_WILDCARD = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_question_in_wildcard"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before semicolon
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_semicolon"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_semicolon"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before semicolon in for statement
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_semicolon_in_for"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_FOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_semicolon_in_for"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space before unary operator
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_before_unary_operator"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BEFORE_UNARY_OPERATOR = DartCore.PLUGIN_ID
      + ".formatter.insert_space_before_unary_operator"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space between brackets in an array type reference
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_between_brackets_in_array_type_reference"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_TYPE_REFERENCE = DartCore.PLUGIN_ID
      + ".formatter.insert_space_between_brackets_in_array_type_reference"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space between empty braces in an array initializer
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_between_empty_braces_in_array_initializer"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER = DartCore.PLUGIN_ID
      + ".formatter.insert_space_between_empty_braces_in_array_initializer"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space between empty brackets in an array allocation expression
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_between_empty_brackets_in_array_allocation_expression"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACKETS_IN_ARRAY_ALLOCATION_EXPRESSION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_between_empty_brackets_in_array_allocation_expression"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space between empty parenthesis in an annotation type member declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_between_empty_parens_in_annotation_type_member_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ANNOTATION_TYPE_MEMBER_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_between_empty_parens_in_annotation_type_member_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space between empty parenthesis in a constructor declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_between_empty_parens_in_constructor_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_between_empty_parens_in_constructor_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space between empty parenthesis in enum constant
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_between_empty_parens_in_enum_constant"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ENUM_CONSTANT = DartCore.PLUGIN_ID
      + ".formatter.insert_space_between_empty_parens_in_enum_constant"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space between empty parenthesis in a method declaration
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_between_empty_parens_in_method_declaration"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_between_empty_parens_in_method_declaration"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to insert a space between empty parenthesis in a method invocation
   *     - option id:         "com.google.dart.tools.core.formatter.insert_space_between_empty_parens_in_method_invocation"
   *     - possible values:   { INSERT, DO_NOT_INSERT }
   *     - default:           DO_NOT_INSERT
   * </pre>
   * 
   * @see DartPreferenceConstants#INSERT
   * @see DartPreferenceConstants#DO_NOT_INSERT
   */
  public static final String FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION = DartCore.PLUGIN_ID
      + ".formatter.insert_space_between_empty_parens_in_method_invocation"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to keep else statement on the same line
   *     - option id:         "com.google.dart.tools.core.formatter.keep_else_statement_on_same_line"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_KEEP_ELSE_STATEMENT_ON_SAME_LINE = DartCore.PLUGIN_ID
      + ".formatter.keep_else_statement_on_same_line"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to keep empty array initializer one one line
   *     - option id:         "com.google.dart.tools.core.formatter.keep_empty_array_initializer_on_one_line"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_KEEP_EMPTY_ARRAY_INITIALIZER_ON_ONE_LINE = DartCore.PLUGIN_ID
      + ".formatter.keep_empty_array_initializer_on_one_line"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to keep guardian clause on one line
   *     - option id:         "com.google.dart.tools.core.formatter.format_guardian_clause_on_one_line"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_KEEP_GUARDIAN_CLAUSE_ON_ONE_LINE = DartCore.PLUGIN_ID
      + ".formatter.format_guardian_clause_on_one_line"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to keep simple if statement on the one line
   *     - option id:         "com.google.dart.tools.core.formatter.keep_imple_if_on_one_line"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE = DartCore.PLUGIN_ID
      + ".formatter.keep_imple_if_on_one_line"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to keep then statement on the same line
   *     - option id:         "com.google.dart.tools.core.formatter.keep_then_statement_on_same_line"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_KEEP_THEN_STATEMENT_ON_SAME_LINE = DartCore.PLUGIN_ID
      + ".formatter.keep_then_statement_on_same_line";//$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to specify the length of the page. Beyond this length, the formatter will try to split the code
   *     - option id:         "com.google.dart.tools.core.formatter.lineSplit"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "80"
   * </pre>
   */
  public static final String FORMATTER_LINE_SPLIT = DartCore.PLUGIN_ID + ".formatter.lineSplit"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to indent block comments that start on the first column
   *     - option id:         "com.google.dart.tools.core.formatter.formatter.never_indent_block_comments_on_first_column"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * Note that this option is ignored if the formatter is created with the mode
   * {@link ToolFactory#M_FORMAT_NEW}.
   * 
   * @see #TRUE
   * @see #FALSE
   * @see ToolFactory#createCodeFormatter(Map, int)
   */
  public static final String FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN = DartCore.PLUGIN_ID
      + ".formatter.never_indent_block_comments_on_first_column"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to indent line comments that start on the first column
   *     - option id:         "com.google.dart.tools.core.formatter.formatter.never_indent_line_comments_on_first_column"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * Note that:
   * <ul>
   * <li>this option is ignored if the formatter is created with the mode
   * {@link ToolFactory#M_FORMAT_NEW}</li>
   * <li>even with this option activated, the formatter still can ignore line comments starting at
   * first column if the option
   * {@link #FORMATTER_COMMENT_FORMAT_LINE_COMMENT_STARTING_ON_FIRST_COLUMN} is set to
   * {@value #FALSE}</li>
   * </ul>
   * 
   * @see #TRUE
   * @see #FALSE
   * @see ToolFactory#createCodeFormatter(Map, int)
   */
  public static final String FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN = DartCore.PLUGIN_ID
      + ".formatter.never_indent_line_comments_on_first_column"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to specify the number of empty lines to preserve
   *     - option id:         "com.google.dart.tools.core.formatter.number_of_empty_lines_to_preserve"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "0"
   * </pre>
   */
  public static final String FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE = DartCore.PLUGIN_ID
      + ".formatter.number_of_empty_lines_to_preserve"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to specify whether the formatter can join wrapped lines or not
   * 
   *    For example, the wrapped lines of method foo return statement in following test case:
   *      class X {
   *      String foo() {
   *      return "select x "
   *             + "from y "
   *             + "where z=a";
   *      }
   *      }
   * 
   *    will be preserved by the formatter when the new preference is used
   *    even if the maximum line width would give it enough space to join the lines.
   *    Hence produces the following output:
   *      class X {
   *          String foo() {
   *              return "select x "
   *                      + "from y "
   *                      + "where z=a";
   *          }
   *      }
   * 
   *     - option id:         "com.google.dart.tools.core.formatter.join_wrapped_lines"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   */
  public static final String FORMATTER_JOIN_WRAPPED_LINES = DartCore.PLUGIN_ID
      + ".formatter.join_wrapped_lines"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to specify whether the formatter can join text lines in comments or not
   * 
   *    For example, the following comment:
   *      /**
   *       * The foo method.
   *       * foo is a substitute for bar.
   *       *&#0047;
   *      public class X {
   *      }
   * 
   *    will be unchanged by the formatter when this new preference is used,
   *    even if the maximum line width would give it enough space to join the lines.
   * 
   *     - option id:         "com.google.dart.tools.core.formatter.join_lines_in_comments"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   */
  public static final String FORMATTER_JOIN_LINES_IN_COMMENTS = DartCore.PLUGIN_ID
      + ".formatter.join_lines_in_comments"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to specify whether or not empty statement should be on a new line
   *     - option id:         "com.google.dart.tools.core.formatter.put_empty_statement_on_new_line"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE = DartCore.PLUGIN_ID
      + ".formatter.put_empty_statement_on_new_line"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to specify the tabulation size
   *     - option id:         "com.google.dart.tools.core.formatter.tabulation.char"
   *     - possible values:   { TAB, SPACE, MIXED }
   *     - default:           TAB
   * </pre>
   * 
   * More values may be added in the future.
   * 
   * @see DartPreferenceConstants#TAB
   * @see DartPreferenceConstants#SPACE
   * @see #MIXED
   */
  public static final String FORMATTER_TAB_CHAR = DartCore.PLUGIN_ID + ".formatter.tabulation.char"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to specify the equivalent number of spaces that represents one tabulation
   *     - option id:         "com.google.dart.tools.core.formatter.tabulation.size"
   *     - possible values:   "&lt;n&gt;", where n is zero or a positive integer
   *     - default:           "4"
   * </pre>
   */
  public static final String FORMATTER_TAB_SIZE = DartCore.PLUGIN_ID + ".formatter.tabulation.size"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to use tabulations for indentation and spaces for line wrapping
   *     - option id:         "com.google.dart.tools.core.formatter.use_tabs_only_for_leading_indentations"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           FALSE
   * </pre>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_USE_TABS_ONLY_FOR_LEADING_INDENTATIONS = DartCore.PLUGIN_ID
      + ".formatter.use_tabs_only_for_leading_indentations"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to wrap before the binary operator
   *     - option id:         "com.google.dart.tools.core.formatter.wrap_before_binary_operator"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * 
   * This option is used only if the option {@link #FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION} is
   * set.
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_WRAP_BEFORE_BINARY_OPERATOR = DartCore.PLUGIN_ID
      + ".formatter.wrap_before_binary_operator"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Option to wrap outer expressions in nested expressions
   *     - option id:         "com.google.dart.tools.core.formatter.wrap_outer_expressions_when_nested"
   *     - possible values:   { TRUE, FALSE }
   *     - default:           TRUE
   * </pre>
   * <p>
   * This option changes the formatter behavior when nested method calls are encountered. Since 3.6,
   * the formatter tries to wrap outermost method calls first to have a better output.
   * </p>
   * <p>
   * For example, let's say we are using the Eclipse built-in profile with a max line width=40+space
   * for tab policy. Then consider the following snippet:
   * </p>
   * 
   * <pre>
   * public class X01 {
   *   void test() {
   *     foo(bar(1, 2, 3, 4), bar(5, 6, 7, 8));
   *   }
   * }
   * </pre>
   * <p>
   * With this new strategy, the formatter will wrap the line earlier, between the arguments of the
   * message call for this example, and then it will allow to keep each nested call on a single
   * line.
   * </p>
   * <p>
   * Hence, the output will be:
   * </p>
   * 
   * <pre>
   * public class X01 {
   *   void test() {
   *     foo(bar(1, 2, 3, 4), bar(5, 6, 7, 8));
   *   }
   * }
   * </pre>
   * <p>
   * </p>
   * <p>
   * <b><u>Important notes</u></b>:
   * </p>
   * <ol>
   * <li>This new behavior is automatically activated (i.e. the default value for this preference is
   * {@link #TRUE}). If the backward compatibility regarding previous versions' formatter behavior
   * (i.e. before 3.6 version) is necessary, then the preference needs to be set to {@link #FALSE}
   * to retrieve the previous formatter behavior.</li>
   * <li>The new strategy currently only applies to nested method calls, but might be extended to
   * other nested expressions in future versions</li>
   * </ol>
   * 
   * @see #TRUE
   * @see #FALSE
   */
  public static final String FORMATTER_WRAP_OUTER_EXPRESSIONS_WHEN_NESTED = DartCore.PLUGIN_ID
      + ".formatter.wrap_outer_expressions_when_nested"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / The wrapping is done by indenting by one compare to the current indentation.
   * </pre>
   */
  public static final int INDENT_BY_ONE = 2;

  /**
   * <pre>
   * FORMATTER / The wrapping is done by using the current indentation.
   * </pre>
   */
  public static final int INDENT_DEFAULT = 0;

  /**
   * <pre>
   * FORMATTER / The wrapping is done by indenting on column under the splitting location.
   * </pre>
   */
  public static final int INDENT_ON_COLUMN = 1;

  /**
   * <pre>
   * FORMATTER / Possible value for the option FORMATTER_TAB_CHAR
   * </pre>
   * 
   * @see DartPreferenceConstants#TAB
   * @see DartPreferenceConstants#SPACE
   * @see #FORMATTER_TAB_CHAR
   */
  public static final String MIXED = "mixed"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Value to set a brace location at the start of the next line with
   *             the right indentation.
   * </pre>
   * 
   * @see #FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER
   * @see #FORMATTER_BRACE_POSITION_FOR_BLOCK
   * @see #FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_SWITCH
   * @see #FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION
   */
  public static final String NEXT_LINE = "next_line"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Value to set a brace location at the start of the next line if a wrapping
   *             occured.
   * </pre>
   * 
   * @see #FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER
   * @see #FORMATTER_BRACE_POSITION_FOR_BLOCK
   * @see #FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_SWITCH
   * @see #FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION
   */
  public static final String NEXT_LINE_ON_WRAP = "next_line_on_wrap"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Value to set a brace location at the start of the next line with
   *             an extra indentation.
   * </pre>
   * 
   * @see #FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER
   * @see #FORMATTER_BRACE_POSITION_FOR_BLOCK
   * @see #FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION
   * @see #FORMATTER_BRACE_POSITION_FOR_SWITCH
   * @see #FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION
   */
  public static final String NEXT_LINE_SHIFTED = "next_line_shifted"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / Value to set an option to true.
   * </pre>
   */
  public static final String TRUE = "true"; //$NON-NLS-1$

  /**
   * <pre>
   * FORMATTER / The wrapping is done using as few lines as possible.
   * </pre>
   */
  public static final int WRAP_COMPACT = 1;

  /**
   * <pre>
   * FORMATTER / The wrapping is done putting the first element on a new
   *             line and then wrapping next elements using as few lines as possible.
   * </pre>
   */
  public static final int WRAP_COMPACT_FIRST_BREAK = 2;

  /**
   * <pre>
   * FORMATTER / The wrapping is done by putting each element on its own line
   *             except the first element.
   * </pre>
   */
  public static final int WRAP_NEXT_PER_LINE = 5;

  /**
   * <pre>
   * FORMATTER / The wrapping is done by putting each element on its own line.
   *             All elements are indented by one except the first element.
   * </pre>
   */
  public static final int WRAP_NEXT_SHIFTED = 4;

  /**
   * <pre>
   * FORMATTER / Value to disable alignment.
   * </pre>
   */
  public static final int WRAP_NO_SPLIT = 0;

  /**
   * <pre>
   * FORMATTER / The wrapping is done by putting each element on its own line.
   * </pre>
   */
  public static final int WRAP_ONE_PER_LINE = 3;

  /*
   * Private constants. Not in javadoc
   */
  private static final IllegalArgumentException WRONG_ARGUMENT = new IllegalArgumentException();

  /**
   * Create a new alignment value according to the given values. This must be used to set up the
   * alignment options.
   * 
   * @param forceSplit the given force value
   * @param wrapStyle the given wrapping style
   * @param indentStyle the given indent style
   * @return the new alignment value
   */
  public static String createAlignmentValue(boolean forceSplit, int wrapStyle, int indentStyle) {
    int alignmentValue = 0;
    DartCore.notYetImplemented();
    // switch (wrapStyle) {
    // case WRAP_COMPACT:
    // alignmentValue |= Alignment.M_COMPACT_SPLIT;
    // break;
    // case WRAP_COMPACT_FIRST_BREAK:
    // alignmentValue |= Alignment.M_COMPACT_FIRST_BREAK_SPLIT;
    // break;
    // case WRAP_NEXT_PER_LINE:
    // alignmentValue |= Alignment.M_NEXT_PER_LINE_SPLIT;
    // break;
    // case WRAP_NEXT_SHIFTED:
    // alignmentValue |= Alignment.M_NEXT_SHIFTED_SPLIT;
    // break;
    // case WRAP_ONE_PER_LINE:
    // alignmentValue |= Alignment.M_ONE_PER_LINE_SPLIT;
    // break;
    // }
    // if (forceSplit) {
    // alignmentValue |= Alignment.M_FORCE;
    // }
    // switch (indentStyle) {
    // case INDENT_BY_ONE:
    // alignmentValue |= Alignment.M_INDENT_BY_ONE;
    // break;
    // case INDENT_ON_COLUMN:
    // alignmentValue |= Alignment.M_INDENT_ON_COLUMN;
    // }
    return String.valueOf(alignmentValue);
  }

  /**
   * Return the settings according to the Dart conventions.
   * 
   * @return the settings according to the Dart conventions
   */
  public static Map<String, String> getDartConventionsSettings() {
    return DefaultCodeFormatterOptions.getDartConventionsSettings().getMap();
  }

  /**
   * Returns the default Eclipse formatter settings
   * 
   * @return the Eclipse default settings
   */
  public static Map<String, String> getEclipseDefaultSettings() {
    return DefaultCodeFormatterOptions.getEclipseDefaultSettings().getMap();
  }

  /**
   * Return the force value of the given alignment value. The given alignment value should be
   * created using the <code>createAlignmentValue(boolean, int, int)</code> API.
   * 
   * @param value the given alignment value
   * @return the force value of the given alignment value
   * @throws IllegalArgumentException if the given alignment value is null, or if it doesn't have a
   *           valid format.
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static boolean getForceWrapping(String value) {
    if (value == null) {
      throw WRONG_ARGUMENT;
    }
    DartCore.notYetImplemented();
    return false;
    // try {
    // int existingValue = Integer.parseInt(value);
    // return (existingValue & Alignment.M_FORCE) != 0;
    // } catch (NumberFormatException e) {
    // throw WRONG_ARGUMENT;
    // }
  }

  /**
   * Return the indentation style of the given alignment value. The given alignment value should be
   * created using the <code>createAlignmentValue(boolean, int, int)</code> API.
   * 
   * @param value the given alignment value
   * @return the indentation style of the given alignment value
   * @throws IllegalArgumentException if the given alignment value is null, or if it doesn't have a
   *           valid format.
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static int getIndentStyle(String value) {
    if (value == null) {
      throw WRONG_ARGUMENT;
    }
    DartCore.notYetImplemented();
    return INDENT_DEFAULT;
    // try {
    // int existingValue = Integer.parseInt(value);
    // if ((existingValue & Alignment.M_INDENT_BY_ONE) != 0) {
    // return INDENT_BY_ONE;
    // } else if ((existingValue & Alignment.M_INDENT_ON_COLUMN) != 0) {
    // return INDENT_ON_COLUMN;
    // } else {
    // return INDENT_DEFAULT;
    // }
    // } catch (NumberFormatException e) {
    // throw WRONG_ARGUMENT;
    // }
  }

  /**
   * Return the wrapping style of the given alignment value. The given alignment value should be
   * created using the <code>createAlignmentValue(boolean, int, int)</code> API.
   * 
   * @param value the given alignment value
   * @return the wrapping style of the given alignment value
   * @throws IllegalArgumentException if the given alignment value is null, or if it doesn't have a
   *           valid format.
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static int getWrappingStyle(String value) {
    if (value == null) {
      throw WRONG_ARGUMENT;
    }
    DartCore.notYetImplemented();
    return WRAP_NO_SPLIT;
    // try {
    // int existingValue = Integer.parseInt(value) & Alignment.SPLIT_MASK;
    // switch (existingValue) {
    // case Alignment.M_COMPACT_SPLIT:
    // return WRAP_COMPACT;
    // case Alignment.M_COMPACT_FIRST_BREAK_SPLIT:
    // return WRAP_COMPACT_FIRST_BREAK;
    // case Alignment.M_NEXT_PER_LINE_SPLIT:
    // return WRAP_NEXT_PER_LINE;
    // case Alignment.M_NEXT_SHIFTED_SPLIT:
    // return WRAP_NEXT_SHIFTED;
    // case Alignment.M_ONE_PER_LINE_SPLIT:
    // return WRAP_ONE_PER_LINE;
    // default:
    // return WRAP_NO_SPLIT;
    // }
    // } catch (NumberFormatException e) {
    // throw WRONG_ARGUMENT;
    // }
  }

  /**
   * Set the force value of the given alignment value and return the new value. The given alignment
   * value should be created using the <code>createAlignmentValue(boolean, int, int)</code> API.
   * 
   * @param value the given alignment value
   * @param force the given force value
   * @return the new alignment value
   * @throws IllegalArgumentException if the given alignment value is null, or if it doesn't have a
   *           valid format.
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static String setForceWrapping(String value, boolean force) {
    if (value == null) {
      throw WRONG_ARGUMENT;
    }
    try {
      int existingValue = Integer.parseInt(value);
      // clear existing force bit
      DartCore.notYetImplemented();
      // existingValue &= ~Alignment.M_FORCE;
      // if (force) {
      // existingValue |= Alignment.M_FORCE;
      // }
      return String.valueOf(existingValue);
    } catch (NumberFormatException e) {
      throw WRONG_ARGUMENT;
    }
  }

  /**
   * Set the indentation style of the given alignment value and return the new value. The given
   * value should be created using the <code>createAlignmentValue(boolean, int, int)</code> API.
   * 
   * @param value the given alignment value
   * @param indentStyle the given indentation style
   * @return the new alignment value
   * @throws IllegalArgumentException if the given alignment value is null, if the given indentation
   *           style is not one of the possible indentation styles, or if the given alignment value
   *           doesn't have a valid format.
   * @see #INDENT_BY_ONE
   * @see #INDENT_DEFAULT
   * @see #INDENT_ON_COLUMN
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static String setIndentStyle(String value, int indentStyle) {
    if (value == null) {
      throw WRONG_ARGUMENT;
    }
    switch (indentStyle) {
      case INDENT_BY_ONE:
      case INDENT_DEFAULT:
      case INDENT_ON_COLUMN:
        break;
      default:
        throw WRONG_ARGUMENT;
    }
    try {
      int existingValue = Integer.parseInt(value);
      // clear existing indent bits
      DartCore.notYetImplemented();
      // existingValue &= ~(Alignment.M_INDENT_BY_ONE |
      // Alignment.M_INDENT_ON_COLUMN);
      // switch (indentStyle) {
      // case INDENT_BY_ONE:
      // existingValue |= Alignment.M_INDENT_BY_ONE;
      // break;
      // case INDENT_ON_COLUMN:
      // existingValue |= Alignment.M_INDENT_ON_COLUMN;
      // }
      return String.valueOf(existingValue);
    } catch (NumberFormatException e) {
      throw WRONG_ARGUMENT;
    }
  }

  /**
   * Set the wrapping style of the given alignment value and return the new value. The given value
   * should be created using the <code>createAlignmentValue(boolean, int, int)</code> API.
   * 
   * @param value the given alignment value
   * @param wrappingStyle the given wrapping style
   * @return the new alignment value
   * @throws IllegalArgumentException if the given alignment value is null, if the given wrapping
   *           style is not one of the possible wrapping styles, or if the given alignment value
   *           doesn't have a valid format.
   * @see #WRAP_COMPACT
   * @see #WRAP_COMPACT_FIRST_BREAK
   * @see #WRAP_NEXT_PER_LINE
   * @see #WRAP_NEXT_SHIFTED
   * @see #WRAP_NO_SPLIT
   * @see #WRAP_ONE_PER_LINE
   * @see #createAlignmentValue(boolean, int, int)
   */
  public static String setWrappingStyle(String value, int wrappingStyle) {
    if (value == null) {
      throw WRONG_ARGUMENT;
    }
    switch (wrappingStyle) {
      case WRAP_COMPACT:
      case WRAP_COMPACT_FIRST_BREAK:
      case WRAP_NEXT_PER_LINE:
      case WRAP_NEXT_SHIFTED:
      case WRAP_NO_SPLIT:
      case WRAP_ONE_PER_LINE:
        break;
      default:
        throw WRONG_ARGUMENT;
    }
    try {
      int existingValue = Integer.parseInt(value);
      // clear existing split bits
      DartCore.notYetImplemented();
      // existingValue &= ~(Alignment.SPLIT_MASK);
      // switch (wrappingStyle) {
      // case WRAP_COMPACT:
      // existingValue |= Alignment.M_COMPACT_SPLIT;
      // break;
      // case WRAP_COMPACT_FIRST_BREAK:
      // existingValue |= Alignment.M_COMPACT_FIRST_BREAK_SPLIT;
      // break;
      // case WRAP_NEXT_PER_LINE:
      // existingValue |= Alignment.M_NEXT_PER_LINE_SPLIT;
      // break;
      // case WRAP_NEXT_SHIFTED:
      // existingValue |= Alignment.M_NEXT_SHIFTED_SPLIT;
      // break;
      // case WRAP_ONE_PER_LINE:
      // existingValue |= Alignment.M_ONE_PER_LINE_SPLIT;
      // break;
      // }
      return String.valueOf(existingValue);
    } catch (NumberFormatException e) {
      throw WRONG_ARGUMENT;
    }
  }
}
