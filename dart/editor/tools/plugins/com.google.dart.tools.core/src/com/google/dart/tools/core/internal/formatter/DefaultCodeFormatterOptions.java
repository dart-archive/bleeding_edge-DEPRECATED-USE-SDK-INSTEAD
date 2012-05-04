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
package com.google.dart.tools.core.internal.formatter;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartPreferenceConstants;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.core.internal.formatter.align.Alignment;
import com.google.dart.tools.core.internal.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class <code>DefaultCodeFormatterOptions</code>
 */
public class DefaultCodeFormatterOptions {
  public static final int TAB = 1;
  public static final int SPACE = 2;
  public static final int MIXED = 4;

  public static DefaultCodeFormatterOptions getDartConventionsSettings() {
    DefaultCodeFormatterOptions options = new DefaultCodeFormatterOptions();
    options.setDartConventionsSettings();
    return options;
  }

  public static DefaultCodeFormatterOptions getDefaultSettings() {
    DefaultCodeFormatterOptions options = new DefaultCodeFormatterOptions();
    options.setDefaultSettings();
    return options;
  }

  public static DefaultCodeFormatterOptions getEclipseDefaultSettings() {
    DefaultCodeFormatterOptions options = new DefaultCodeFormatterOptions();
    options.setEclipseDefaultSettings();
    return options;
  }

  public int alignment_for_arguments_in_allocation_expression;
  public int alignment_for_arguments_in_annotation;
  public int alignment_for_arguments_in_enum_constant;
  public int alignment_for_arguments_in_explicit_constructor_call;
  public int alignment_for_arguments_in_method_invocation;
  public int alignment_for_arguments_in_qualified_allocation_expression;
  public int alignment_for_assignment;
  public int alignment_for_binary_expression;
  public int alignment_for_compact_if;
  public int alignment_for_conditional_expression;
  public int alignment_for_enum_constants;
  public int alignment_for_expressions_in_array_initializer;
  public int alignment_for_method_declaration;
  public int alignment_for_function_declaration;
  public int alignment_for_multiple_fields;
  public int alignment_for_parameters_in_constructor_declaration;
  public int alignment_for_initializers_in_constructor_declaration;
  public int alignment_for_parameters_in_method_declaration;
  public int alignment_for_parameters_in_function_declaration;
  public int alignment_for_selector_in_method_invocation;
  public int alignment_for_superclass_in_type_declaration;
  public int alignment_for_superinterfaces_in_enum_declaration;
  public int alignment_for_superinterfaces_in_type_declaration;
  public int alignment_for_throws_clause_in_constructor_declaration;
  public int alignment_for_throws_clause_in_method_declaration;

  public boolean align_type_members_on_columns;

  public String brace_position_for_annotation_type_declaration;
  public String brace_position_for_anonymous_type_declaration;
  public String brace_position_for_array_initializer;
  public String brace_position_for_block;
  public String brace_position_for_block_in_case;
  public String brace_position_for_constructor_declaration;
  public String brace_position_for_enum_constant;
  public String brace_position_for_function_declaration;
  public String brace_position_for_method_declaration;
  public String brace_position_for_type_declaration;
  public String brace_position_for_switch;

  public int continuation_indentation;
  public int continuation_indentation_for_array_initializer;

  public int blank_lines_after_imports;
  public int blank_lines_after_package;
  public int blank_lines_before_field;
  public int blank_lines_before_first_class_body_declaration;
  public int blank_lines_before_imports;
  public int blank_lines_before_member_type;
  public int blank_lines_before_method;
  public int blank_lines_before_new_chunk;
  public int blank_lines_before_package;
  public int blank_lines_between_import_groups;
  public int blank_lines_between_type_declarations;
  public int blank_lines_at_beginning_of_method_body;

  public boolean comment_clear_blank_lines_in_javadoc_comment;
  public boolean comment_clear_blank_lines_in_block_comment;
  public boolean comment_new_lines_at_block_boundaries;
  public boolean comment_new_lines_at_javadoc_boundaries;
  public boolean comment_format_javadoc_comment;
  public boolean comment_format_line_comment;
  public boolean comment_format_line_comment_starting_on_first_column;
  public boolean comment_format_block_comment;
  public boolean comment_format_header;
  public boolean comment_format_html;
  public boolean comment_format_source;
  public boolean comment_indent_parameter_description;
  public boolean comment_indent_root_tags;
  public boolean comment_insert_empty_line_before_root_tags;
  public boolean comment_insert_new_line_for_parameter;
  public int comment_line_length;

  public boolean use_tags;
  public char[] disabling_tag;
  public char[] enabling_tag;
  private final static char[] DEFAULT_DISABLING_TAG = "@formatter:off".toCharArray(); //$NON-NLS-1$
  private final static char[] DEFAULT_ENABLING_TAG = "@formatter:on".toCharArray(); //$NON-NLS-1$

  public boolean indent_statements_compare_to_block;
  public boolean indent_statements_compare_to_body;
  public boolean indent_body_declarations_compare_to_annotation_declaration_header;
  public boolean indent_body_declarations_compare_to_enum_constant_header;
  public boolean indent_body_declarations_compare_to_enum_declaration_header;
  public boolean indent_body_declarations_compare_to_type_header;
  public boolean indent_breaks_compare_to_cases;
  public boolean indent_empty_lines;
  public boolean indent_switchstatements_compare_to_cases;
  public boolean indent_switchstatements_compare_to_switch;
  public int indentation_size;

  public boolean insert_new_line_after_annotation_on_type;
  public boolean insert_new_line_after_annotation_on_field;
  public boolean insert_new_line_after_annotation_on_method;
  public boolean insert_new_line_after_annotation_on_package;
  public boolean insert_new_line_after_annotation_on_parameter;
  public boolean insert_new_line_after_annotation_on_local_variable;
  public boolean insert_new_line_after_label;
  public boolean insert_new_line_after_opening_brace_in_array_initializer;
  public boolean insert_new_line_at_end_of_file_if_missing;
  public boolean insert_new_line_before_catch_in_try_statement;
  public boolean insert_new_line_before_closing_brace_in_array_initializer;
  public boolean insert_new_line_before_else_in_if_statement;
  public boolean insert_new_line_before_finally_in_try_statement;
  public boolean insert_new_line_before_while_in_do_statement;
  public boolean insert_new_line_in_empty_anonymous_type_declaration;
  public boolean insert_new_line_in_empty_block;
  public boolean insert_new_line_in_empty_annotation_declaration;
  public boolean insert_new_line_in_empty_enum_constant;
  public boolean insert_new_line_in_empty_enum_declaration;
  public boolean insert_new_line_in_empty_method_body;
  public boolean insert_new_line_in_empty_type_declaration;
  public boolean insert_space_after_and_in_type_parameter;
  public boolean insert_space_after_assignment_operator;
  public boolean insert_space_after_at_in_annotation;
  public boolean insert_space_after_at_in_annotation_type_declaration;
  public boolean insert_space_after_binary_operator;
  public boolean insert_space_after_closing_angle_bracket_in_type_arguments;
  public boolean insert_space_after_closing_angle_bracket_in_type_parameters;
  public boolean insert_space_after_closing_paren_in_cast;
  public boolean insert_space_after_closing_brace_in_block;
  public boolean insert_space_after_colon_in_assert;
  public boolean insert_space_after_colon_in_case;
  public boolean insert_space_after_colon_in_conditional;
  public boolean insert_space_after_colon_in_for;
  public boolean insert_space_after_colon_in_labeled_statement;
  public boolean insert_space_after_comma_in_allocation_expression;
  public boolean insert_space_after_comma_in_annotation;
  public boolean insert_space_after_comma_in_array_initializer;
  public boolean insert_space_after_comma_in_constructor_declaration_parameters;
  public boolean insert_space_after_comma_in_constructor_declaration_throws;
  public boolean insert_space_after_comma_in_enum_constant_arguments;
  public boolean insert_space_after_comma_in_enum_declarations;
  public boolean insert_space_after_comma_in_explicit_constructor_call_arguments;
  public boolean insert_space_after_comma_in_for_increments;
  public boolean insert_space_after_comma_in_for_inits;
  public boolean insert_space_after_comma_in_method_invocation_arguments;
  public boolean insert_space_after_comma_in_method_declaration_parameters;
  public boolean insert_space_after_comma_in_method_declaration_throws;
  public boolean insert_space_after_comma_in_multiple_field_declarations;
  public boolean insert_space_after_comma_in_multiple_local_declarations;
  public boolean insert_space_after_comma_in_parameterized_type_reference;
  public boolean insert_space_after_comma_in_superinterfaces;
  public boolean insert_space_after_comma_in_type_arguments;
  public boolean insert_space_after_comma_in_type_parameters;
  public boolean insert_space_after_ellipsis;
  public boolean insert_space_after_opening_angle_bracket_in_parameterized_type_reference;
  public boolean insert_space_after_opening_angle_bracket_in_type_arguments;
  public boolean insert_space_after_opening_angle_bracket_in_type_parameters;
  public boolean insert_space_after_opening_bracket_in_array_allocation_expression;
  public boolean insert_space_after_opening_bracket_in_array_reference;
  public boolean insert_space_after_opening_brace_in_array_initializer;
  public boolean insert_space_after_opening_paren_in_annotation;
  public boolean insert_space_after_opening_paren_in_cast;
  public boolean insert_space_after_opening_paren_in_catch;
  public boolean insert_space_after_opening_paren_in_constructor_declaration;
  public boolean insert_space_after_opening_paren_in_enum_constant;
  public boolean insert_space_after_opening_paren_in_for;
  public boolean insert_space_after_opening_paren_in_if;
  public boolean insert_space_after_opening_paren_in_method_declaration;
  public boolean insert_space_after_opening_paren_in_method_invocation;
  public boolean insert_space_after_opening_paren_in_parenthesized_expression;
  public boolean insert_space_after_opening_paren_in_switch;
  public boolean insert_space_after_opening_paren_in_synchronized;
  public boolean insert_space_after_opening_paren_in_while;
  public boolean insert_space_after_postfix_operator;
  public boolean insert_space_after_prefix_operator;
  public boolean insert_space_after_question_in_conditional;
  public boolean insert_space_after_question_in_wilcard;
  public boolean insert_space_after_semicolon_in_for;
  public boolean insert_space_after_unary_operator;
  public boolean insert_space_before_and_in_type_parameter;
  public boolean insert_space_before_at_in_annotation_type_declaration;
  public boolean insert_space_before_assignment_operator;
  public boolean insert_space_before_binary_operator;
  public boolean insert_space_before_closing_angle_bracket_in_parameterized_type_reference;
  public boolean insert_space_before_closing_angle_bracket_in_type_arguments;
  public boolean insert_space_before_closing_angle_bracket_in_type_parameters;
  public boolean insert_space_before_closing_brace_in_array_initializer;
  public boolean insert_space_before_closing_bracket_in_array_allocation_expression;
  public boolean insert_space_before_closing_bracket_in_array_reference;
  public boolean insert_space_before_closing_paren_in_annotation;
  public boolean insert_space_before_closing_paren_in_cast;
  public boolean insert_space_before_closing_paren_in_catch;
  public boolean insert_space_before_closing_paren_in_constructor_declaration;
  public boolean insert_space_before_closing_paren_in_enum_constant;
  public boolean insert_space_before_closing_paren_in_for;
  public boolean insert_space_before_closing_paren_in_if;
  public boolean insert_space_before_closing_paren_in_method_declaration;
  public boolean insert_space_before_closing_paren_in_method_invocation;
  public boolean insert_space_before_closing_paren_in_parenthesized_expression;
  public boolean insert_space_before_closing_paren_in_switch;
  public boolean insert_space_before_closing_paren_in_synchronized;
  public boolean insert_space_before_closing_paren_in_while;
  public boolean insert_space_before_colon_in_assert;
  public boolean insert_space_before_colon_in_case;
  public boolean insert_space_before_colon_in_conditional;
  public boolean insert_space_before_colon_in_default;
  public boolean insert_space_before_colon_in_for;
  public boolean insert_space_before_colon_in_labeled_statement;
  public boolean insert_space_before_comma_in_allocation_expression;
  public boolean insert_space_before_comma_in_annotation;
  public boolean insert_space_before_comma_in_array_initializer;
  public boolean insert_space_before_comma_in_constructor_declaration_parameters;
  public boolean insert_space_before_comma_in_constructor_declaration_throws;
  public boolean insert_space_before_comma_in_enum_constant_arguments;
  public boolean insert_space_before_comma_in_enum_declarations;
  public boolean insert_space_before_comma_in_explicit_constructor_call_arguments;
  public boolean insert_space_before_comma_in_for_increments;
  public boolean insert_space_before_comma_in_for_inits;
  public boolean insert_space_before_comma_in_method_invocation_arguments;
  public boolean insert_space_before_comma_in_method_declaration_parameters;
  public boolean insert_space_before_comma_in_method_declaration_throws;
  public boolean insert_space_before_comma_in_multiple_field_declarations;
  public boolean insert_space_before_comma_in_multiple_local_declarations;
  public boolean insert_space_before_comma_in_parameterized_type_reference;
  public boolean insert_space_before_comma_in_superinterfaces;
  public boolean insert_space_before_comma_in_type_arguments;
  public boolean insert_space_before_comma_in_type_parameters;
  public boolean insert_space_before_ellipsis;
  public boolean insert_space_before_parenthesized_expression_in_return;
  public boolean insert_space_before_parenthesized_expression_in_throw;
  public boolean insert_space_before_question_in_wilcard;
  public boolean insert_space_before_opening_angle_bracket_in_parameterized_type_reference;
  public boolean insert_space_before_opening_angle_bracket_in_type_arguments;
  public boolean insert_space_before_opening_angle_bracket_in_type_parameters;
  public boolean insert_space_before_opening_brace_in_annotation_type_declaration;
  public boolean insert_space_before_opening_brace_in_anonymous_type_declaration;
  public boolean insert_space_before_opening_brace_in_array_initializer;
  public boolean insert_space_before_opening_brace_in_block;
  public boolean insert_space_before_opening_brace_in_constructor_declaration;
  public boolean insert_space_before_opening_brace_in_enum_constant;
  public boolean insert_space_before_opening_brace_in_enum_declaration;
  public boolean insert_space_before_opening_brace_in_method_declaration;
  public boolean insert_space_before_opening_brace_in_type_declaration;
  public boolean insert_space_before_opening_bracket_in_array_allocation_expression;
  public boolean insert_space_before_opening_bracket_in_array_reference;
  public boolean insert_space_before_opening_bracket_in_array_type_reference;
  public boolean insert_space_before_opening_paren_in_annotation;
  public boolean insert_space_before_opening_paren_in_annotation_type_member_declaration;
  public boolean insert_space_before_opening_paren_in_catch;
  public boolean insert_space_before_opening_paren_in_constructor_declaration;
  public boolean insert_space_before_opening_paren_in_enum_constant;
  public boolean insert_space_before_opening_paren_in_for;
  public boolean insert_space_before_opening_paren_in_if;
  public boolean insert_space_before_opening_paren_in_method_invocation;
  public boolean insert_space_before_opening_paren_in_method_declaration;
  public boolean insert_space_before_opening_paren_in_switch;
  public boolean insert_space_before_opening_brace_in_switch;
  public boolean insert_space_before_opening_paren_in_synchronized;
  public boolean insert_space_before_opening_paren_in_parenthesized_expression;
  public boolean insert_space_before_opening_paren_in_while;
  public boolean insert_space_before_postfix_operator;
  public boolean insert_space_before_prefix_operator;
  public boolean insert_space_before_question_in_conditional;
  public boolean insert_space_before_semicolon;
  public boolean insert_space_before_semicolon_in_for;
  public boolean insert_space_before_unary_operator;
  public boolean insert_space_between_brackets_in_array_type_reference;
  public boolean insert_space_between_empty_braces_in_array_initializer;
  public boolean insert_space_between_empty_brackets_in_array_allocation_expression;
  public boolean insert_space_between_empty_parens_in_annotation_type_member_declaration;
  public boolean insert_space_between_empty_parens_in_constructor_declaration;
  public boolean insert_space_between_empty_parens_in_enum_constant;
  public boolean insert_space_between_empty_parens_in_method_declaration;
  public boolean insert_space_between_empty_parens_in_method_invocation;
  public boolean compact_else_if;
  public boolean keep_guardian_clause_on_one_line;
  public boolean keep_else_statement_on_same_line;
  public boolean keep_empty_array_initializer_on_one_line;
  public boolean keep_simple_if_on_one_line;
  public boolean keep_then_statement_on_same_line;
  public boolean never_indent_block_comments_on_first_column;
  public boolean never_indent_line_comments_on_first_column;
  public int number_of_empty_lines_to_preserve;
  public boolean join_wrapped_lines;
  public boolean join_lines_in_comments;
  public boolean put_empty_statement_on_new_line;
  public int tab_size;
  public final char filling_space = ' ';
  public int page_width;
  public int tab_char;
  public boolean use_tabs_only_for_leading_indentations;
  public boolean wrap_before_binary_operator;
  public boolean wrap_outer_expressions_when_nested;

  public int initial_indentation_level;
  public String line_separator;

  public DefaultCodeFormatterOptions(Map<String, String> settings) {
    setDefaultSettings();
    if (settings == null) {
      return;
    }
    set(settings);
  }

  private DefaultCodeFormatterOptions() {
    // cannot be instantiated
  }

  public Map<String, String> getMap() {
    Map<String, String> options = new HashMap<String, String>();
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION,
        getAlignment(alignment_for_arguments_in_allocation_expression));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ANNOTATION,
        getAlignment(alignment_for_arguments_in_annotation));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ENUM_CONSTANT,
        getAlignment(alignment_for_arguments_in_enum_constant));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL,
        getAlignment(alignment_for_arguments_in_explicit_constructor_call));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION,
        getAlignment(alignment_for_arguments_in_method_invocation));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION,
        getAlignment(alignment_for_arguments_in_qualified_allocation_expression));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSIGNMENT,
        getAlignment(alignment_for_assignment));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION,
        getAlignment(alignment_for_binary_expression));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF,
        getAlignment(alignment_for_compact_if));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION,
        getAlignment(alignment_for_conditional_expression));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS,
        getAlignment(alignment_for_enum_constants));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER,
        getAlignment(alignment_for_expressions_in_array_initializer));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_METHOD_DECLARATION,
        getAlignment(alignment_for_method_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_METHOD_DECLARATION,
        getAlignment(alignment_for_function_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLE_FIELDS,
        getAlignment(alignment_for_multiple_fields));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_INITIZAIZERS_IN_CONSTRUCTOR_DECLARATION,
        getAlignment(alignment_for_initializers_in_constructor_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION,
        getAlignment(alignment_for_parameters_in_constructor_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION,
        getAlignment(alignment_for_parameters_in_method_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_FUNCTION_DECLARATION,
        getAlignment(alignment_for_parameters_in_function_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION,
        getAlignment(alignment_for_selector_in_method_invocation));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION,
        getAlignment(alignment_for_superclass_in_type_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_ENUM_DECLARATION,
        getAlignment(alignment_for_superinterfaces_in_enum_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION,
        getAlignment(alignment_for_superinterfaces_in_type_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION,
        getAlignment(alignment_for_throws_clause_in_constructor_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION,
        getAlignment(alignment_for_throws_clause_in_method_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS,
        align_type_members_on_columns ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANNOTATION_TYPE_DECLARATION,
        brace_position_for_annotation_type_declaration);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION,
        brace_position_for_anonymous_type_declaration);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER,
        brace_position_for_array_initializer);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK,
        brace_position_for_block);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK_IN_CASE,
        brace_position_for_block_in_case);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION,
        brace_position_for_constructor_declaration);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_CONSTANT,
        brace_position_for_enum_constant);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_DECLARATION,
        brace_position_for_function_declaration);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION,
        brace_position_for_method_declaration);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION,
        brace_position_for_type_declaration);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH,
        brace_position_for_switch);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT,
        comment_clear_blank_lines_in_block_comment ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT,
        comment_clear_blank_lines_in_javadoc_comment ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_NEW_LINES_AT_BLOCK_BOUNDARIES,
        comment_new_lines_at_block_boundaries ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_NEW_LINES_AT_JAVADOC_BOUNDARIES,
        comment_new_lines_at_javadoc_boundaries ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT,
        comment_format_block_comment ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER,
        comment_format_header ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HTML, comment_format_html
        ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT,
        comment_format_javadoc_comment ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT,
        comment_format_line_comment ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT_STARTING_ON_FIRST_COLUMN,
        comment_format_line_comment_starting_on_first_column ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_SOURCE,
        comment_format_source ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION,
        comment_indent_parameter_description ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_ROOT_TAGS,
        comment_indent_root_tags ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS,
        comment_insert_empty_line_before_root_tags ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER,
        comment_insert_new_line_for_parameter ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH,
        Integer.toString(comment_line_length));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION,
        Integer.toString(continuation_indentation));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER,
        Integer.toString(continuation_indentation_for_array_initializer));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS,
        Integer.toString(blank_lines_after_imports));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_PACKAGE,
        Integer.toString(blank_lines_after_package));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD,
        Integer.toString(blank_lines_before_field));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION,
        Integer.toString(blank_lines_before_first_class_body_declaration));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS,
        Integer.toString(blank_lines_before_imports));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_MEMBER_TYPE,
        Integer.toString(blank_lines_before_member_type));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD,
        Integer.toString(blank_lines_before_method));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK,
        Integer.toString(blank_lines_before_new_chunk));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_PACKAGE,
        Integer.toString(blank_lines_before_package));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_IMPORT_GROUPS,
        Integer.toString(blank_lines_between_import_groups));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_TYPE_DECLARATIONS,
        Integer.toString(blank_lines_between_type_declarations));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_METHOD_BODY,
        Integer.toString(blank_lines_at_beginning_of_method_body));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BLOCK,
        indent_statements_compare_to_block ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BODY,
        indent_statements_compare_to_body ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ANNOTATION_DECLARATION_HEADER,
        indent_body_declarations_compare_to_annotation_declaration_header
            ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_CONSTANT_HEADER,
        indent_body_declarations_compare_to_enum_constant_header
            ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_DECLARATION_HEADER,
        indent_body_declarations_compare_to_enum_declaration_header
            ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER,
        indent_body_declarations_compare_to_type_header ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INDENT_BREAKS_COMPARE_TO_CASES,
        indent_breaks_compare_to_cases ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_EMPTY_LINES, indent_empty_lines
        ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES,
        indent_switchstatements_compare_to_cases ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH,
        indent_switchstatements_compare_to_switch ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE,
        Integer.toString(indentation_size));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_TYPE,
        insert_new_line_after_annotation_on_type ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_FIELD,
        insert_new_line_after_annotation_on_field ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_METHOD,
        insert_new_line_after_annotation_on_method ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PACKAGE,
        insert_new_line_after_annotation_on_package ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PARAMETER,
        insert_new_line_after_annotation_on_parameter ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE,
        insert_new_line_after_annotation_on_local_variable ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER,
        insert_new_line_after_opening_brace_in_array_initializer ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AT_END_OF_FILE_IF_MISSING,
        insert_new_line_at_end_of_file_if_missing ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT,
        insert_new_line_before_catch_in_try_statement ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER,
        insert_new_line_before_closing_brace_in_array_initializer ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT,
        insert_new_line_before_else_in_if_statement ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT,
        insert_new_line_before_finally_in_try_statement ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT,
        insert_new_line_before_while_in_do_statement ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION,
        insert_new_line_in_empty_anonymous_type_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK,
        insert_new_line_in_empty_block ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANNOTATION_DECLARATION,
        insert_new_line_in_empty_annotation_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_CONSTANT,
        insert_new_line_in_empty_enum_constant ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_DECLARATION,
        insert_new_line_in_empty_enum_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_METHOD_BODY,
        insert_new_line_in_empty_method_body ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION,
        insert_new_line_in_empty_type_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_LABEL,
        insert_new_line_after_label ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AND_IN_TYPE_PARAMETER,
        insert_space_after_and_in_type_parameter ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR,
        insert_space_after_assignment_operator ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION,
        insert_space_after_at_in_annotation ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION_TYPE_DECLARATION,
        insert_space_after_at_in_annotation_type_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR,
        insert_space_after_binary_operator ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS,
        insert_space_after_closing_angle_bracket_in_type_arguments ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS,
        insert_space_after_closing_angle_bracket_in_type_parameters
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST,
        insert_space_after_closing_paren_in_cast ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK,
        insert_space_after_closing_brace_in_block ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_ASSERT,
        insert_space_after_colon_in_assert ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CASE,
        insert_space_after_colon_in_case ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL,
        insert_space_after_colon_in_conditional ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_FOR,
        insert_space_after_colon_in_for ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_LABELED_STATEMENT,
        insert_space_after_colon_in_labeled_statement ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION,
        insert_space_after_comma_in_allocation_expression ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ANNOTATION,
        insert_space_after_comma_in_annotation ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER,
        insert_space_after_comma_in_array_initializer ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS,
        insert_space_after_comma_in_constructor_declaration_parameters
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS,
        insert_space_after_comma_in_constructor_declaration_throws ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_CONSTANT_ARGUMENTS,
        insert_space_after_comma_in_enum_constant_arguments ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_DECLARATIONS,
        insert_space_after_comma_in_enum_declarations ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS,
        insert_space_after_comma_in_explicit_constructor_call_arguments
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS,
        insert_space_after_comma_in_for_increments ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS,
        insert_space_after_comma_in_for_inits ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS,
        insert_space_after_comma_in_method_invocation_arguments ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS,
        insert_space_after_comma_in_method_declaration_parameters ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS,
        insert_space_after_comma_in_method_declaration_throws ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS,
        insert_space_after_comma_in_multiple_field_declarations ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS,
        insert_space_after_comma_in_multiple_local_declarations ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE,
        insert_space_after_comma_in_parameterized_type_reference ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES,
        insert_space_after_comma_in_superinterfaces ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS,
        insert_space_after_comma_in_type_arguments ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_PARAMETERS,
        insert_space_after_comma_in_type_parameters ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION,
        insert_space_after_opening_bracket_in_array_allocation_expression
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ELLIPSIS,
        insert_space_after_ellipsis ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE,
        insert_space_after_opening_angle_bracket_in_parameterized_type_reference
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS,
        insert_space_after_opening_angle_bracket_in_type_arguments ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS,
        insert_space_after_opening_angle_bracket_in_type_parameters
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE,
        insert_space_after_opening_bracket_in_array_reference ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER,
        insert_space_after_opening_brace_in_array_initializer ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ANNOTATION,
        insert_space_after_opening_paren_in_annotation ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CAST,
        insert_space_after_opening_paren_in_cast ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH,
        insert_space_after_opening_paren_in_catch ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION,
        insert_space_after_opening_paren_in_constructor_declaration
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ENUM_CONSTANT,
        insert_space_after_opening_paren_in_enum_constant ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR,
        insert_space_after_opening_paren_in_for ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF,
        insert_space_after_opening_paren_in_if ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION,
        insert_space_after_opening_paren_in_method_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION,
        insert_space_after_opening_paren_in_method_invocation ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION,
        insert_space_after_opening_paren_in_parenthesized_expression
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH,
        insert_space_after_opening_paren_in_switch ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED,
        insert_space_after_opening_paren_in_synchronized ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE,
        insert_space_after_opening_paren_in_while ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_POSTFIX_OPERATOR,
        insert_space_after_postfix_operator ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_PREFIX_OPERATOR,
        insert_space_after_prefix_operator ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL,
        insert_space_after_question_in_conditional ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_WILDCARD,
        insert_space_after_question_in_wilcard ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR,
        insert_space_after_semicolon_in_for ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR,
        insert_space_after_unary_operator ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_AND_IN_TYPE_PARAMETER,
        insert_space_before_and_in_type_parameter ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_AT_IN_ANNOTATION_TYPE_DECLARATION,
        insert_space_before_at_in_annotation_type_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR,
        insert_space_before_assignment_operator ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR,
        insert_space_before_binary_operator ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE,
        insert_space_before_closing_angle_bracket_in_parameterized_type_reference
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS,
        insert_space_before_closing_angle_bracket_in_type_arguments
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS,
        insert_space_before_closing_angle_bracket_in_type_parameters
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER,
        insert_space_before_closing_brace_in_array_initializer ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION,
        insert_space_before_closing_bracket_in_array_allocation_expression
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE,
        insert_space_before_closing_bracket_in_array_reference ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ANNOTATION,
        insert_space_before_closing_paren_in_annotation ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CAST,
        insert_space_before_closing_paren_in_cast ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH,
        insert_space_before_closing_paren_in_catch ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION,
        insert_space_before_closing_paren_in_constructor_declaration
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ENUM_CONSTANT,
        insert_space_before_closing_paren_in_enum_constant ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR,
        insert_space_before_closing_paren_in_for ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF,
        insert_space_before_closing_paren_in_if ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION,
        insert_space_before_closing_paren_in_method_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION,
        insert_space_before_closing_paren_in_method_invocation ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_PARENTHESIZED_EXPRESSION,
        insert_space_before_closing_paren_in_parenthesized_expression
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH,
        insert_space_before_closing_paren_in_switch ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED,
        insert_space_before_closing_paren_in_synchronized ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE,
        insert_space_before_closing_paren_in_while ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_ASSERT,
        insert_space_before_colon_in_assert ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE,
        insert_space_before_colon_in_case ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL,
        insert_space_before_colon_in_conditional ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT,
        insert_space_before_colon_in_default ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_FOR,
        insert_space_before_colon_in_for ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_LABELED_STATEMENT,
        insert_space_before_colon_in_labeled_statement ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ALLOCATION_EXPRESSION,
        insert_space_before_comma_in_allocation_expression ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ANNOTATION,
        insert_space_before_comma_in_annotation ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ARRAY_INITIALIZER,
        insert_space_before_comma_in_array_initializer ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS,
        insert_space_before_comma_in_constructor_declaration_parameters
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS,
        insert_space_before_comma_in_constructor_declaration_throws
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_CONSTANT_ARGUMENTS,
        insert_space_before_comma_in_enum_constant_arguments ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_DECLARATIONS,
        insert_space_before_comma_in_enum_declarations ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS,
        insert_space_before_comma_in_explicit_constructor_call_arguments
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INCREMENTS,
        insert_space_before_comma_in_for_increments ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INITS,
        insert_space_before_comma_in_for_inits ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS,
        insert_space_before_comma_in_method_invocation_arguments ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS,
        insert_space_before_comma_in_method_declaration_parameters ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS,
        insert_space_before_comma_in_method_declaration_throws ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS,
        insert_space_before_comma_in_multiple_field_declarations ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS,
        insert_space_before_comma_in_multiple_local_declarations ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES,
        insert_space_before_comma_in_superinterfaces ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_ARGUMENTS,
        insert_space_before_comma_in_type_arguments ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_PARAMETERS,
        insert_space_before_comma_in_type_parameters ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE,
        insert_space_before_comma_in_parameterized_type_reference ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ELLIPSIS,
        insert_space_before_ellipsis ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE,
        insert_space_before_opening_angle_bracket_in_parameterized_type_reference
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS,
        insert_space_before_opening_angle_bracket_in_type_arguments
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS,
        insert_space_before_opening_angle_bracket_in_type_parameters
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANNOTATION_TYPE_DECLARATION,
        insert_space_before_opening_brace_in_annotation_type_declaration
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION,
        insert_space_before_opening_brace_in_anonymous_type_declaration
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER,
        insert_space_before_opening_brace_in_array_initializer ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK,
        insert_space_before_opening_brace_in_block ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION,
        insert_space_before_opening_brace_in_constructor_declaration
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_CONSTANT,
        insert_space_before_opening_brace_in_enum_constant ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_DECLARATION,
        insert_space_before_opening_brace_in_enum_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION,
        insert_space_before_opening_brace_in_method_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION,
        insert_space_before_opening_brace_in_type_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION,
        insert_space_before_opening_bracket_in_array_allocation_expression
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE,
        insert_space_before_opening_bracket_in_array_reference ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE,
        insert_space_before_opening_bracket_in_array_type_reference
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION,
        insert_space_before_opening_paren_in_annotation ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION_TYPE_MEMBER_DECLARATION,
        insert_space_before_opening_paren_in_annotation_type_member_declaration
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH,
        insert_space_before_opening_paren_in_catch ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION,
        insert_space_before_opening_paren_in_constructor_declaration
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ENUM_CONSTANT,
        insert_space_before_opening_paren_in_enum_constant ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR,
        insert_space_before_opening_paren_in_for ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF,
        insert_space_before_opening_paren_in_if ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION,
        insert_space_before_opening_paren_in_method_invocation ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION,
        insert_space_before_opening_paren_in_method_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH,
        insert_space_before_opening_paren_in_switch ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH,
        insert_space_before_opening_brace_in_switch ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED,
        insert_space_before_opening_paren_in_synchronized ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION,
        insert_space_before_opening_paren_in_parenthesized_expression
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE,
        insert_space_before_opening_paren_in_while ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_RETURN,
        insert_space_before_parenthesized_expression_in_return ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_THROW,
        insert_space_before_parenthesized_expression_in_throw ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_POSTFIX_OPERATOR,
        insert_space_before_postfix_operator ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PREFIX_OPERATOR,
        insert_space_before_prefix_operator ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL,
        insert_space_before_question_in_conditional ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_WILDCARD,
        insert_space_before_question_in_wilcard ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON,
        insert_space_before_semicolon ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_FOR,
        insert_space_before_semicolon_in_for ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_UNARY_OPERATOR,
        insert_space_before_unary_operator ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_TYPE_REFERENCE,
        insert_space_between_brackets_in_array_type_reference ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER,
        insert_space_between_empty_braces_in_array_initializer ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACKETS_IN_ARRAY_ALLOCATION_EXPRESSION,
        insert_space_between_empty_brackets_in_array_allocation_expression
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ANNOTATION_TYPE_MEMBER_DECLARATION,
        insert_space_between_empty_parens_in_annotation_type_member_declaration
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION,
        insert_space_between_empty_parens_in_constructor_declaration
            ? DartPreferenceConstants.INSERT : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ENUM_CONSTANT,
        insert_space_between_empty_parens_in_enum_constant ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION,
        insert_space_between_empty_parens_in_method_declaration ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION,
        insert_space_between_empty_parens_in_method_invocation ? DartPreferenceConstants.INSERT
            : DartPreferenceConstants.DO_NOT_INSERT);
    options.put(DefaultCodeFormatterConstants.FORMATTER_COMPACT_ELSE_IF, compact_else_if
        ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_KEEP_GUARDIAN_CLAUSE_ON_ONE_LINE,
        keep_guardian_clause_on_one_line ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_KEEP_ELSE_STATEMENT_ON_SAME_LINE,
        keep_else_statement_on_same_line ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_KEEP_EMPTY_ARRAY_INITIALIZER_ON_ONE_LINE,
        keep_empty_array_initializer_on_one_line ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE,
        keep_simple_if_on_one_line ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_KEEP_THEN_STATEMENT_ON_SAME_LINE,
        keep_then_statement_on_same_line ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN,
        never_indent_block_comments_on_first_column ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN,
        never_indent_line_comments_on_first_column ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE,
        Integer.toString(number_of_empty_lines_to_preserve));
    options.put(DefaultCodeFormatterConstants.FORMATTER_JOIN_WRAPPED_LINES, join_wrapped_lines
        ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_JOIN_LINES_IN_COMMENTS,
        join_lines_in_comments ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE,
        put_empty_statement_on_new_line ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, Integer.toString(page_width));
    switch (tab_char) {
      case SPACE:
        options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, DartPreferenceConstants.SPACE);
        break;
      case TAB:
        options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, DartPreferenceConstants.TAB);
        break;
      case MIXED:
        options.put(
            DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR,
            DefaultCodeFormatterConstants.MIXED);
        break;
    }
    options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, Integer.toString(tab_size));
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_USE_TABS_ONLY_FOR_LEADING_INDENTATIONS,
        use_tabs_only_for_leading_indentations ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BINARY_OPERATOR,
        wrap_before_binary_operator ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    options.put(DefaultCodeFormatterConstants.FORMATTER_DISABLING_TAG, disabling_tag == null
        ? Util.EMPTY_STRING : new String(disabling_tag));
    options.put(DefaultCodeFormatterConstants.FORMATTER_ENABLING_TAG, enabling_tag == null
        ? Util.EMPTY_STRING : new String(enabling_tag));
    options.put(DefaultCodeFormatterConstants.FORMATTER_USE_ON_OFF_TAGS, use_tags
        ? DefaultCodeFormatterConstants.TRUE : DefaultCodeFormatterConstants.FALSE);
    options.put(
        DefaultCodeFormatterConstants.FORMATTER_WRAP_OUTER_EXPRESSIONS_WHEN_NESTED,
        wrap_outer_expressions_when_nested ? DefaultCodeFormatterConstants.TRUE
            : DefaultCodeFormatterConstants.FALSE);
    return options;
  }

  public void set(Map<String, String> settings) {
    final Object alignmentForArgumentsInAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION);
    if (alignmentForArgumentsInAllocationExpressionOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_arguments_in_allocation_expression = Integer.parseInt((String) alignmentForArgumentsInAllocationExpressionOption);
      } catch (NumberFormatException e) {
        // alignment_for_arguments_in_allocation_expression =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_arguments_in_allocation_expression =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForArgumentsInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ANNOTATION);
    if (alignmentForArgumentsInAnnotationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_arguments_in_annotation = Integer.parseInt((String) alignmentForArgumentsInAnnotationOption);
      } catch (NumberFormatException e) {
        // alignment_for_arguments_in_annotation =
        // Alignment.M_NO_ALIGNMENT;
      } catch (ClassCastException e) {
        // alignment_for_arguments_in_annotation =
        // Alignment.M_NO_ALIGNMENT;
      }
    }
    final Object alignmentForArgumentsInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ENUM_CONSTANT);
    if (alignmentForArgumentsInEnumConstantOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_arguments_in_enum_constant = Integer.parseInt((String) alignmentForArgumentsInEnumConstantOption);
      } catch (NumberFormatException e) {
        // alignment_for_arguments_in_enum_constant =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_arguments_in_enum_constant =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForArgumentsInExplicitConstructorCallOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL);
    if (alignmentForArgumentsInExplicitConstructorCallOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_arguments_in_explicit_constructor_call = Integer.parseInt((String) alignmentForArgumentsInExplicitConstructorCallOption);
      } catch (NumberFormatException e) {
        // alignment_for_arguments_in_explicit_constructor_call =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_arguments_in_explicit_constructor_call =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForArgumentsInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION);
    if (alignmentForArgumentsInMethodInvocationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_arguments_in_method_invocation = Integer.parseInt((String) alignmentForArgumentsInMethodInvocationOption);
      } catch (NumberFormatException e) {
        // alignment_for_arguments_in_method_invocation =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_arguments_in_method_invocation =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForArgumentsInQualifiedAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION);
    if (alignmentForArgumentsInQualifiedAllocationExpressionOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_arguments_in_qualified_allocation_expression = Integer.parseInt((String) alignmentForArgumentsInQualifiedAllocationExpressionOption);
      } catch (NumberFormatException e) {
        // alignment_for_arguments_in_qualified_allocation_expression =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_arguments_in_qualified_allocation_expression =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForAssignmentOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSIGNMENT);
    if (alignmentForAssignmentOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_assignment = Integer.parseInt((String) alignmentForAssignmentOption);
      } catch (NumberFormatException e) {
        // alignment_for_assignment = Alignment.M_ONE_PER_LINE_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_assignment = Alignment.M_ONE_PER_LINE_SPLIT;
      }
    }
    final Object alignmentForBinaryExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BINARY_EXPRESSION);
    if (alignmentForBinaryExpressionOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_binary_expression = Integer.parseInt((String) alignmentForBinaryExpressionOption);
      } catch (NumberFormatException e) {
        // alignment_for_binary_expression = Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_binary_expression = Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForCompactIfOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF);
    if (alignmentForCompactIfOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_compact_if = Integer.parseInt((String) alignmentForCompactIfOption);
      } catch (NumberFormatException e) {
        // alignment_for_compact_if = Alignment.M_ONE_PER_LINE_SPLIT
        // | Alignment.M_INDENT_BY_ONE;
      } catch (ClassCastException e) {
        // alignment_for_compact_if = Alignment.M_ONE_PER_LINE_SPLIT
        // | Alignment.M_INDENT_BY_ONE;
      }
    }
    final Object alignmentForConditionalExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION);
    if (alignmentForConditionalExpressionOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_conditional_expression = Integer.parseInt((String) alignmentForConditionalExpressionOption);
      } catch (NumberFormatException e) {
        // alignment_for_conditional_expression =
        // Alignment.M_ONE_PER_LINE_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_conditional_expression =
        // Alignment.M_ONE_PER_LINE_SPLIT;
      }
    }
    final Object alignmentForEnumConstantsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS);
    if (alignmentForEnumConstantsOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_enum_constants = Integer.parseInt((String) alignmentForEnumConstantsOption);
      } catch (NumberFormatException e) {
        // alignment_for_enum_constants = Alignment.NONE;
      } catch (ClassCastException e) {
        // alignment_for_enum_constants = Alignment.NONE;
      }
    }
    final Object alignmentForExpressionsInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER);
    if (alignmentForExpressionsInArrayInitializerOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_expressions_in_array_initializer = Integer.parseInt((String) alignmentForExpressionsInArrayInitializerOption);
      } catch (NumberFormatException e) {
        // alignment_for_expressions_in_array_initializer =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_expressions_in_array_initializer =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_METHOD_DECLARATION);
    if (alignmentForMethodDeclarationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_method_declaration = Integer.parseInt((String) alignmentForMethodDeclarationOption);
      } catch (NumberFormatException e) {
        // alignment_for_method_declaration = Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_method_declaration = Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForFunctionDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_FUNCTION_DECLARATION);
    if (alignmentForFunctionDeclarationOption != null) {
      DartCore.notYetImplemented();
      try {
        // TODO make a new option for function definition alignment
        alignment_for_function_declaration = Integer.parseInt((String) alignmentForFunctionDeclarationOption);
      } catch (NumberFormatException e) {
        // alignment_for_function_declaration = Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_function_declaration = Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForMultipleFieldsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLE_FIELDS);
    if (alignmentForMultipleFieldsOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_multiple_fields = Integer.parseInt((String) alignmentForMultipleFieldsOption);
      } catch (NumberFormatException e) {
        // alignment_for_multiple_fields = Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_multiple_fields = Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForInitializersInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_INITIZAIZERS_IN_CONSTRUCTOR_DECLARATION);
    if (alignmentForInitializersInConstructorDeclarationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_initializers_in_constructor_declaration = Integer.parseInt((String) alignmentForInitializersInConstructorDeclarationOption);
      } catch (NumberFormatException e) {
        // alignment_for_initializers_in_constructor_declaration =
        // Alignment.M_ONE_PER_LINE_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_initializers_in_constructor_declaration =
        // Alignment.M_ONE_PER_LINE_SPLIT;
      }
    }
    final Object alignmentForParametersInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION);
    if (alignmentForParametersInConstructorDeclarationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_parameters_in_constructor_declaration = Integer.parseInt((String) alignmentForParametersInConstructorDeclarationOption);
      } catch (NumberFormatException e) {
        // alignment_for_parameters_in_constructor_declaration =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_parameters_in_constructor_declaration =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForParametersInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION);
    if (alignmentForParametersInMethodDeclarationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_parameters_in_method_declaration = Integer.parseInt((String) alignmentForParametersInMethodDeclarationOption);
      } catch (NumberFormatException e) {
        // alignment_for_parameters_in_method_declaration =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_parameters_in_method_declaration =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForParametersInFunctionDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_FUNCTION_DECLARATION);
    if (alignmentForParametersInFunctionDeclarationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_parameters_in_function_declaration = Integer.parseInt((String) alignmentForParametersInFunctionDeclarationOption);
      } catch (NumberFormatException e) {
        // alignment_for_parameters_in_function_declaration =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_parameters_in_function_declaration =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForSelectorInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION);
    if (alignmentForSelectorInMethodInvocationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_selector_in_method_invocation = Integer.parseInt((String) alignmentForSelectorInMethodInvocationOption);
      } catch (NumberFormatException e) {
        // alignment_for_selector_in_method_invocation =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_selector_in_method_invocation =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForSuperclassInTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION);
    if (alignmentForSuperclassInTypeDeclarationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_superclass_in_type_declaration = Integer.parseInt((String) alignmentForSuperclassInTypeDeclarationOption);
      } catch (NumberFormatException e) {
        // alignment_for_superclass_in_type_declaration =
        // Alignment.M_NEXT_SHIFTED_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_superclass_in_type_declaration =
        // Alignment.M_NEXT_SHIFTED_SPLIT;
      }
    }
    final Object alignmentForSuperinterfacesInEnumDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_ENUM_DECLARATION);
    if (alignmentForSuperinterfacesInEnumDeclarationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_superinterfaces_in_enum_declaration = Integer.parseInt((String) alignmentForSuperinterfacesInEnumDeclarationOption);
      } catch (NumberFormatException e) {
        // alignment_for_superinterfaces_in_enum_declaration =
        // Alignment.M_NEXT_SHIFTED_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_superinterfaces_in_enum_declaration =
        // Alignment.M_NEXT_SHIFTED_SPLIT;
      }
    }
    final Object alignmentForSuperinterfacesInTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION);
    if (alignmentForSuperinterfacesInTypeDeclarationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_superinterfaces_in_type_declaration = Integer.parseInt((String) alignmentForSuperinterfacesInTypeDeclarationOption);
      } catch (NumberFormatException e) {
        // alignment_for_superinterfaces_in_type_declaration =
        // Alignment.M_NEXT_SHIFTED_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_superinterfaces_in_type_declaration =
        // Alignment.M_NEXT_SHIFTED_SPLIT;
      }
    }
    final Object alignmentForThrowsClauseInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION);
    if (alignmentForThrowsClauseInConstructorDeclarationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_throws_clause_in_constructor_declaration = Integer.parseInt((String) alignmentForThrowsClauseInConstructorDeclarationOption);
      } catch (NumberFormatException e) {
        // alignment_for_throws_clause_in_constructor_declaration =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_throws_clause_in_constructor_declaration =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignmentForThrowsClauseInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION);
    if (alignmentForThrowsClauseInMethodDeclarationOption != null) {
      DartCore.notYetImplemented();
      try {
        alignment_for_throws_clause_in_method_declaration = Integer.parseInt((String) alignmentForThrowsClauseInMethodDeclarationOption);
      } catch (NumberFormatException e) {
        // alignment_for_throws_clause_in_method_declaration =
        // Alignment.M_COMPACT_SPLIT;
      } catch (ClassCastException e) {
        // alignment_for_throws_clause_in_method_declaration =
        // Alignment.M_COMPACT_SPLIT;
      }
    }
    final Object alignTypeMembersOnColumnsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS);
    if (alignTypeMembersOnColumnsOption != null) {
      align_type_members_on_columns = DefaultCodeFormatterConstants.TRUE.equals(alignTypeMembersOnColumnsOption);
    }
    final Object bracePositionForAnnotationTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANNOTATION_TYPE_DECLARATION);
    if (bracePositionForAnnotationTypeDeclarationOption != null) {
      try {
        brace_position_for_annotation_type_declaration = (String) bracePositionForAnnotationTypeDeclarationOption;
      } catch (ClassCastException e) {
        brace_position_for_annotation_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
      }
    }
    final Object bracePositionForAnonymousTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION);
    if (bracePositionForAnonymousTypeDeclarationOption != null) {
      try {
        brace_position_for_anonymous_type_declaration = (String) bracePositionForAnonymousTypeDeclarationOption;
      } catch (ClassCastException e) {
        brace_position_for_anonymous_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
      }
    }
    final Object bracePositionForArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER);
    if (bracePositionForArrayInitializerOption != null) {
      try {
        brace_position_for_array_initializer = (String) bracePositionForArrayInitializerOption;
      } catch (ClassCastException e) {
        brace_position_for_array_initializer = DefaultCodeFormatterConstants.END_OF_LINE;
      }
    }
    final Object bracePositionForBlockOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK);
    if (bracePositionForBlockOption != null) {
      try {
        brace_position_for_block = (String) bracePositionForBlockOption;
      } catch (ClassCastException e) {
        brace_position_for_block = DefaultCodeFormatterConstants.END_OF_LINE;
      }
    }
    final Object bracePositionForBlockInCaseOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK_IN_CASE);
    if (bracePositionForBlockInCaseOption != null) {
      try {
        brace_position_for_block_in_case = (String) bracePositionForBlockInCaseOption;
      } catch (ClassCastException e) {
        brace_position_for_block_in_case = DefaultCodeFormatterConstants.END_OF_LINE;
      }
    }
    final Object bracePositionForConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION);
    if (bracePositionForConstructorDeclarationOption != null) {
      try {
        brace_position_for_constructor_declaration = (String) bracePositionForConstructorDeclarationOption;
      } catch (ClassCastException e) {
        brace_position_for_constructor_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
      }
    }
    final Object bracePositionForEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_CONSTANT);
    if (bracePositionForEnumConstantOption != null) {
      try {
        brace_position_for_enum_constant = (String) bracePositionForEnumConstantOption;
      } catch (ClassCastException e) {
        brace_position_for_enum_constant = DefaultCodeFormatterConstants.END_OF_LINE;
      }
    }
    final Object bracePositionForEnumDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_DECLARATION);
    if (bracePositionForEnumDeclarationOption != null) {
      try {
        brace_position_for_function_declaration = (String) bracePositionForEnumDeclarationOption;
      } catch (ClassCastException e) {
        brace_position_for_function_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
      }
    }
    final Object bracePositionForMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION);
    if (bracePositionForMethodDeclarationOption != null) {
      try {
        brace_position_for_method_declaration = (String) bracePositionForMethodDeclarationOption;
      } catch (ClassCastException e) {
        brace_position_for_method_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
      }
    }
    final Object bracePositionForSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH);
    if (bracePositionForSwitchOption != null) {
      try {
        brace_position_for_switch = (String) bracePositionForSwitchOption;
      } catch (ClassCastException e) {
        brace_position_for_switch = DefaultCodeFormatterConstants.END_OF_LINE;
      }
    }
    final Object bracePositionForTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION);
    if (bracePositionForTypeDeclarationOption != null) {
      try {
        brace_position_for_type_declaration = (String) bracePositionForTypeDeclarationOption;
      } catch (ClassCastException e) {
        brace_position_for_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
      }
    }
    final Object continuationIndentationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION);
    if (continuationIndentationOption != null) {
      try {
        continuation_indentation = Integer.parseInt((String) continuationIndentationOption);
      } catch (NumberFormatException e) {
        continuation_indentation = 2;
      } catch (ClassCastException e) {
        continuation_indentation = 2;
      }
    }
    final Object continuationIndentationForArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER);
    if (continuationIndentationForArrayInitializerOption != null) {
      try {
        continuation_indentation_for_array_initializer = Integer.parseInt((String) continuationIndentationForArrayInitializerOption);
      } catch (NumberFormatException e) {
        continuation_indentation_for_array_initializer = 2;
      } catch (ClassCastException e) {
        continuation_indentation_for_array_initializer = 2;
      }
    }
    final Object blankLinesAfterImportsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS);
    if (blankLinesAfterImportsOption != null) {
      try {
        blank_lines_after_imports = Integer.parseInt((String) blankLinesAfterImportsOption);
      } catch (NumberFormatException e) {
        blank_lines_after_imports = 0;
      } catch (ClassCastException e) {
        blank_lines_after_imports = 0;
      }
    }
    final Object blankLinesAfterPackageOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_PACKAGE);
    if (blankLinesAfterPackageOption != null) {
      try {
        blank_lines_after_package = Integer.parseInt((String) blankLinesAfterPackageOption);
      } catch (NumberFormatException e) {
        blank_lines_after_package = 0;
      } catch (ClassCastException e) {
        blank_lines_after_package = 0;
      }
    }
    final Object blankLinesBeforeFieldOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD);
    if (blankLinesBeforeFieldOption != null) {
      try {
        blank_lines_before_field = Integer.parseInt((String) blankLinesBeforeFieldOption);
      } catch (NumberFormatException e) {
        blank_lines_before_field = 0;
      } catch (ClassCastException e) {
        blank_lines_before_field = 0;
      }
    }
    final Object blankLinesBeforeFirstClassBodyDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION);
    if (blankLinesBeforeFirstClassBodyDeclarationOption != null) {
      try {
        blank_lines_before_first_class_body_declaration = Integer.parseInt((String) blankLinesBeforeFirstClassBodyDeclarationOption);
      } catch (NumberFormatException e) {
        blank_lines_before_first_class_body_declaration = 0;
      } catch (ClassCastException e) {
        blank_lines_before_first_class_body_declaration = 0;
      }
    }
    final Object blankLinesBeforeImportsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS);
    if (blankLinesBeforeImportsOption != null) {
      try {
        blank_lines_before_imports = Integer.parseInt((String) blankLinesBeforeImportsOption);
      } catch (NumberFormatException e) {
        blank_lines_before_imports = 0;
      } catch (ClassCastException e) {
        blank_lines_before_imports = 0;
      }
    }
    final Object blankLinesBeforeMemberTypeOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_MEMBER_TYPE);
    if (blankLinesBeforeMemberTypeOption != null) {
      try {
        blank_lines_before_member_type = Integer.parseInt((String) blankLinesBeforeMemberTypeOption);
      } catch (NumberFormatException e) {
        blank_lines_before_member_type = 0;
      } catch (ClassCastException e) {
        blank_lines_before_member_type = 0;
      }
    }
    final Object blankLinesBeforeMethodOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD);
    if (blankLinesBeforeMethodOption != null) {
      try {
        blank_lines_before_method = Integer.parseInt((String) blankLinesBeforeMethodOption);
      } catch (NumberFormatException e) {
        blank_lines_before_method = 0;
      } catch (ClassCastException e) {
        blank_lines_before_method = 0;
      }
    }
    final Object blankLinesBeforeNewChunkOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK);
    if (blankLinesBeforeNewChunkOption != null) {
      try {
        blank_lines_before_new_chunk = Integer.parseInt((String) blankLinesBeforeNewChunkOption);
      } catch (NumberFormatException e) {
        blank_lines_before_new_chunk = 0;
      } catch (ClassCastException e) {
        blank_lines_before_new_chunk = 0;
      }
    }
    final Object blankLinesBeforePackageOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_PACKAGE);
    if (blankLinesBeforePackageOption != null) {
      try {
        blank_lines_before_package = Integer.parseInt((String) blankLinesBeforePackageOption);
      } catch (NumberFormatException e) {
        blank_lines_before_package = 0;
      } catch (ClassCastException e) {
        blank_lines_before_package = 0;
      }
    }
    final Object blankLinesBetweenImportGroupsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_IMPORT_GROUPS);
    if (blankLinesBetweenImportGroupsOption != null) {
      try {
        blank_lines_between_import_groups = Integer.parseInt((String) blankLinesBetweenImportGroupsOption);
      } catch (NumberFormatException e) {
        blank_lines_between_import_groups = 1;
      } catch (ClassCastException e) {
        blank_lines_between_import_groups = 1;
      }
    }
    final Object blankLinesBetweenTypeDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_TYPE_DECLARATIONS);
    if (blankLinesBetweenTypeDeclarationsOption != null) {
      try {
        blank_lines_between_type_declarations = Integer.parseInt((String) blankLinesBetweenTypeDeclarationsOption);
      } catch (NumberFormatException e) {
        blank_lines_between_type_declarations = 0;
      } catch (ClassCastException e) {
        blank_lines_between_type_declarations = 0;
      }
    }
    final Object blankLinesAtBeginningOfMethodBodyOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_METHOD_BODY);
    if (blankLinesAtBeginningOfMethodBodyOption != null) {
      try {
        blank_lines_at_beginning_of_method_body = Integer.parseInt((String) blankLinesAtBeginningOfMethodBodyOption);
      } catch (NumberFormatException e) {
        blank_lines_at_beginning_of_method_body = 0;
      } catch (ClassCastException e) {
        blank_lines_at_beginning_of_method_body = 0;
      }
    }
    final Object commentFormatJavadocCommentOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT);
    if (commentFormatJavadocCommentOption != null) {
      comment_format_javadoc_comment = DefaultCodeFormatterConstants.TRUE.equals(commentFormatJavadocCommentOption);
    }
    final Object commentFormatBlockCommentOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT);
    if (commentFormatBlockCommentOption != null) {
      comment_format_block_comment = DefaultCodeFormatterConstants.TRUE.equals(commentFormatBlockCommentOption);
    }
    final Object commentFormatLineCommentOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT);
    if (commentFormatLineCommentOption != null) {
      comment_format_line_comment = DefaultCodeFormatterConstants.TRUE.equals(commentFormatLineCommentOption);
    }
    final Object formatLineCommentStartingOnFirstColumnOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT_STARTING_ON_FIRST_COLUMN);
    if (formatLineCommentStartingOnFirstColumnOption != null) {
      comment_format_line_comment_starting_on_first_column = DefaultCodeFormatterConstants.TRUE.equals(formatLineCommentStartingOnFirstColumnOption);
    }
    final Object commentFormatHeaderOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER);
    if (commentFormatHeaderOption != null) {
      comment_format_header = DefaultCodeFormatterConstants.TRUE.equals(commentFormatHeaderOption);
    }
    final Object commentFormatHtmlOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HTML);
    if (commentFormatHtmlOption != null) {
      comment_format_html = DefaultCodeFormatterConstants.TRUE.equals(commentFormatHtmlOption);
    }
    final Object commentFormatSourceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_SOURCE);
    if (commentFormatSourceOption != null) {
      comment_format_source = DefaultCodeFormatterConstants.TRUE.equals(commentFormatSourceOption);
    }
    final Object commentIndentParameterDescriptionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION);
    if (commentIndentParameterDescriptionOption != null) {
      comment_indent_parameter_description = DefaultCodeFormatterConstants.TRUE.equals(commentIndentParameterDescriptionOption);
    }
    final Object commentIndentRootTagsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_ROOT_TAGS);
    if (commentIndentRootTagsOption != null) {
      comment_indent_root_tags = DefaultCodeFormatterConstants.TRUE.equals(commentIndentRootTagsOption);
    }
    final Object commentInsertEmptyLineBeforeRootTagsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS);
    if (commentInsertEmptyLineBeforeRootTagsOption != null) {
      comment_insert_empty_line_before_root_tags = DartPreferenceConstants.INSERT.equals(commentInsertEmptyLineBeforeRootTagsOption);
    }
    final Object commentInsertNewLineForParameterOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER);
    if (commentInsertNewLineForParameterOption != null) {
      comment_insert_new_line_for_parameter = DartPreferenceConstants.INSERT.equals(commentInsertNewLineForParameterOption);
    }
    final Object commentLineLengthOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH);
    if (commentLineLengthOption != null) {
      try {
        comment_line_length = Integer.parseInt((String) commentLineLengthOption);
      } catch (NumberFormatException e) {
        comment_line_length = 80;
      } catch (ClassCastException e) {
        comment_line_length = 80;
      }
    }
    final Object commentNewLinesAtBlockBoundariesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_NEW_LINES_AT_BLOCK_BOUNDARIES);
    if (commentNewLinesAtBlockBoundariesOption != null) {
      comment_new_lines_at_block_boundaries = DefaultCodeFormatterConstants.TRUE.equals(commentNewLinesAtBlockBoundariesOption);
    }
    final Object commentNewLinesAtJavadocBoundariesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_NEW_LINES_AT_JAVADOC_BOUNDARIES);
    if (commentNewLinesAtJavadocBoundariesOption != null) {
      comment_new_lines_at_javadoc_boundaries = DefaultCodeFormatterConstants.TRUE.equals(commentNewLinesAtJavadocBoundariesOption);
    }
    final Object indentStatementsCompareToBlockOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BLOCK);
    if (indentStatementsCompareToBlockOption != null) {
      indent_statements_compare_to_block = DefaultCodeFormatterConstants.TRUE.equals(indentStatementsCompareToBlockOption);
    }
    final Object indentStatementsCompareToBodyOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BODY);
    if (indentStatementsCompareToBodyOption != null) {
      indent_statements_compare_to_body = DefaultCodeFormatterConstants.TRUE.equals(indentStatementsCompareToBodyOption);
    }
    final Object indentBodyDeclarationsCompareToAnnotationDeclarationHeaderOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ANNOTATION_DECLARATION_HEADER);
    if (indentBodyDeclarationsCompareToAnnotationDeclarationHeaderOption != null) {
      indent_body_declarations_compare_to_annotation_declaration_header = DefaultCodeFormatterConstants.TRUE.equals(indentBodyDeclarationsCompareToAnnotationDeclarationHeaderOption);
    }
    final Object indentBodyDeclarationsCompareToEnumConstantHeaderOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_CONSTANT_HEADER);
    if (indentBodyDeclarationsCompareToEnumConstantHeaderOption != null) {
      indent_body_declarations_compare_to_enum_constant_header = DefaultCodeFormatterConstants.TRUE.equals(indentBodyDeclarationsCompareToEnumConstantHeaderOption);
    }
    final Object indentBodyDeclarationsCompareToEnumDeclarationHeaderOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_DECLARATION_HEADER);
    if (indentBodyDeclarationsCompareToEnumDeclarationHeaderOption != null) {
      indent_body_declarations_compare_to_enum_declaration_header = DefaultCodeFormatterConstants.TRUE.equals(indentBodyDeclarationsCompareToEnumDeclarationHeaderOption);
    }
    final Object indentBodyDeclarationsCompareToTypeHeaderOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER);
    if (indentBodyDeclarationsCompareToTypeHeaderOption != null) {
      indent_body_declarations_compare_to_type_header = DefaultCodeFormatterConstants.TRUE.equals(indentBodyDeclarationsCompareToTypeHeaderOption);
    }
    final Object indentBreaksCompareToCasesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_BREAKS_COMPARE_TO_CASES);
    if (indentBreaksCompareToCasesOption != null) {
      indent_breaks_compare_to_cases = DefaultCodeFormatterConstants.TRUE.equals(indentBreaksCompareToCasesOption);
    }
    final Object indentEmptyLinesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_EMPTY_LINES);
    if (indentEmptyLinesOption != null) {
      indent_empty_lines = DefaultCodeFormatterConstants.TRUE.equals(indentEmptyLinesOption);
    }
    final Object indentSwitchstatementsCompareToCasesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES);
    if (indentSwitchstatementsCompareToCasesOption != null) {
      indent_switchstatements_compare_to_cases = DefaultCodeFormatterConstants.TRUE.equals(indentSwitchstatementsCompareToCasesOption);
    }
    final Object indentSwitchstatementsCompareToSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH);
    if (indentSwitchstatementsCompareToSwitchOption != null) {
      indent_switchstatements_compare_to_switch = DefaultCodeFormatterConstants.TRUE.equals(indentSwitchstatementsCompareToSwitchOption);
    }
    final Object indentationSizeOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE);
    if (indentationSizeOption != null) {
      try {
        indentation_size = Integer.parseInt((String) indentationSizeOption);
      } catch (NumberFormatException e) {
        indentation_size = 4;
      } catch (ClassCastException e) {
        indentation_size = 4;
      }
    }
    final Object insertNewLineAfterOpeningBraceInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER);
    if (insertNewLineAfterOpeningBraceInArrayInitializerOption != null) {
      insert_new_line_after_opening_brace_in_array_initializer = DartPreferenceConstants.INSERT.equals(insertNewLineAfterOpeningBraceInArrayInitializerOption);
    }
    final Object insertNewLineAtEndOfFileIfMissingOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AT_END_OF_FILE_IF_MISSING);
    if (insertNewLineAtEndOfFileIfMissingOption != null) {
      insert_new_line_at_end_of_file_if_missing = DartPreferenceConstants.INSERT.equals(insertNewLineAtEndOfFileIfMissingOption);
    }
    final Object insertNewLineBeforeCatchInTryStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT);
    if (insertNewLineBeforeCatchInTryStatementOption != null) {
      insert_new_line_before_catch_in_try_statement = DartPreferenceConstants.INSERT.equals(insertNewLineBeforeCatchInTryStatementOption);
    }
    final Object insertNewLineBeforeClosingBraceInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER);
    if (insertNewLineBeforeClosingBraceInArrayInitializerOption != null) {
      insert_new_line_before_closing_brace_in_array_initializer = DartPreferenceConstants.INSERT.equals(insertNewLineBeforeClosingBraceInArrayInitializerOption);
    }
    final Object insertNewLineBeforeElseInIfStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT);
    if (insertNewLineBeforeElseInIfStatementOption != null) {
      insert_new_line_before_else_in_if_statement = DartPreferenceConstants.INSERT.equals(insertNewLineBeforeElseInIfStatementOption);
    }
    final Object insertNewLineBeforeFinallyInTryStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT);
    if (insertNewLineBeforeFinallyInTryStatementOption != null) {
      insert_new_line_before_finally_in_try_statement = DartPreferenceConstants.INSERT.equals(insertNewLineBeforeFinallyInTryStatementOption);
    }
    final Object insertNewLineBeforeWhileInDoStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT);
    if (insertNewLineBeforeWhileInDoStatementOption != null) {
      insert_new_line_before_while_in_do_statement = DartPreferenceConstants.INSERT.equals(insertNewLineBeforeWhileInDoStatementOption);
    }
    final Object insertNewLineInEmptyAnonymousTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION);
    if (insertNewLineInEmptyAnonymousTypeDeclarationOption != null) {
      insert_new_line_in_empty_anonymous_type_declaration = DartPreferenceConstants.INSERT.equals(insertNewLineInEmptyAnonymousTypeDeclarationOption);
    }
    final Object insertNewLineInEmptyBlockOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_BLOCK);
    if (insertNewLineInEmptyBlockOption != null) {
      insert_new_line_in_empty_block = DartPreferenceConstants.INSERT.equals(insertNewLineInEmptyBlockOption);
    }
    final Object insertNewLineInEmptyAnnotationDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANNOTATION_DECLARATION);
    if (insertNewLineInEmptyAnnotationDeclarationOption != null) {
      insert_new_line_in_empty_annotation_declaration = DartPreferenceConstants.INSERT.equals(insertNewLineInEmptyAnnotationDeclarationOption);
    }
    final Object insertNewLineInEmptyEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_CONSTANT);
    if (insertNewLineInEmptyEnumConstantOption != null) {
      insert_new_line_in_empty_enum_constant = DartPreferenceConstants.INSERT.equals(insertNewLineInEmptyEnumConstantOption);
    }
    final Object insertNewLineInEmptyEnumDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ENUM_DECLARATION);
    if (insertNewLineInEmptyEnumDeclarationOption != null) {
      insert_new_line_in_empty_enum_declaration = DartPreferenceConstants.INSERT.equals(insertNewLineInEmptyEnumDeclarationOption);
    }
    final Object insertNewLineInEmptyMethodBodyOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_METHOD_BODY);
    if (insertNewLineInEmptyMethodBodyOption != null) {
      insert_new_line_in_empty_method_body = DartPreferenceConstants.INSERT.equals(insertNewLineInEmptyMethodBodyOption);
    }
    final Object insertNewLineInEmptyTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_TYPE_DECLARATION);
    if (insertNewLineInEmptyTypeDeclarationOption != null) {
      insert_new_line_in_empty_type_declaration = DartPreferenceConstants.INSERT.equals(insertNewLineInEmptyTypeDeclarationOption);
    }
    final Object insertNewLineAfterLabelOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_LABEL);
    if (insertNewLineAfterLabelOption != null) {
      insert_new_line_after_label = DartPreferenceConstants.INSERT.equals(insertNewLineAfterLabelOption);
    }
    final Object insertSpaceAfterAndInWildcardOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AND_IN_TYPE_PARAMETER);
    if (insertSpaceAfterAndInWildcardOption != null) {
      insert_space_after_and_in_type_parameter = DartPreferenceConstants.INSERT.equals(insertSpaceAfterAndInWildcardOption);
    }
    final Object insertSpaceAfterAssignmentOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR);
    if (insertSpaceAfterAssignmentOperatorOption != null) {
      insert_space_after_assignment_operator = DartPreferenceConstants.INSERT.equals(insertSpaceAfterAssignmentOperatorOption);
    }
    final Object insertSpaceAfterAtInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION);
    if (insertSpaceAfterAtInAnnotationOption != null) {
      insert_space_after_at_in_annotation = DartPreferenceConstants.INSERT.equals(insertSpaceAfterAtInAnnotationOption);
    }
    final Object insertSpaceAfterAtInAnnotationTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION_TYPE_DECLARATION);
    if (insertSpaceAfterAtInAnnotationTypeDeclarationOption != null) {
      insert_space_after_at_in_annotation_type_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceAfterAtInAnnotationTypeDeclarationOption);
    }
    final Object insertSpaceAfterBinaryOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR);
    if (insertSpaceAfterBinaryOperatorOption != null) {
      insert_space_after_binary_operator = DartPreferenceConstants.INSERT.equals(insertSpaceAfterBinaryOperatorOption);
    }
    final Object insertSpaceAfterClosingAngleBracketInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS);
    if (insertSpaceAfterClosingAngleBracketInTypeArgumentsOption != null) {
      insert_space_after_closing_angle_bracket_in_type_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceAfterClosingAngleBracketInTypeArgumentsOption);
    }
    final Object insertSpaceAfterClosingAngleBracketInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS);
    if (insertSpaceAfterClosingAngleBracketInTypeParametersOption != null) {
      insert_space_after_closing_angle_bracket_in_type_parameters = DartPreferenceConstants.INSERT.equals(insertSpaceAfterClosingAngleBracketInTypeParametersOption);
    }
    final Object insertSpaceAfterClosingParenInCastOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST);
    if (insertSpaceAfterClosingParenInCastOption != null) {
      insert_space_after_closing_paren_in_cast = DartPreferenceConstants.INSERT.equals(insertSpaceAfterClosingParenInCastOption);
    }
    final Object insertSpaceAfterClosingBraceInBlockOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK);
    if (insertSpaceAfterClosingBraceInBlockOption != null) {
      insert_space_after_closing_brace_in_block = DartPreferenceConstants.INSERT.equals(insertSpaceAfterClosingBraceInBlockOption);
    }
    final Object insertSpaceAfterColonInAssertOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_ASSERT);
    if (insertSpaceAfterColonInAssertOption != null) {
      insert_space_after_colon_in_assert = DartPreferenceConstants.INSERT.equals(insertSpaceAfterColonInAssertOption);
    }
    final Object insertSpaceAfterColonInCaseOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CASE);
    if (insertSpaceAfterColonInCaseOption != null) {
      insert_space_after_colon_in_case = DartPreferenceConstants.INSERT.equals(insertSpaceAfterColonInCaseOption);
    }
    final Object insertSpaceAfterColonInConditionalOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL);
    if (insertSpaceAfterColonInConditionalOption != null) {
      insert_space_after_colon_in_conditional = DartPreferenceConstants.INSERT.equals(insertSpaceAfterColonInConditionalOption);
    }
    final Object insertSpaceAfterColonInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_FOR);
    if (insertSpaceAfterColonInForOption != null) {
      insert_space_after_colon_in_for = DartPreferenceConstants.INSERT.equals(insertSpaceAfterColonInForOption);
    }
    final Object insertSpaceAfterColonInLabeledStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_LABELED_STATEMENT);
    if (insertSpaceAfterColonInLabeledStatementOption != null) {
      insert_space_after_colon_in_labeled_statement = DartPreferenceConstants.INSERT.equals(insertSpaceAfterColonInLabeledStatementOption);
    }
    final Object insertSpaceAfterCommaInAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION);
    if (insertSpaceAfterCommaInAllocationExpressionOption != null) {
      insert_space_after_comma_in_allocation_expression = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInAllocationExpressionOption);
    }
    final Object insertSpaceAfterCommaInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ANNOTATION);
    if (insertSpaceAfterCommaInAnnotationOption != null) {
      insert_space_after_comma_in_annotation = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInAnnotationOption);
    }
    final Object insertSpaceAfterCommaInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER);
    if (insertSpaceAfterCommaInArrayInitializerOption != null) {
      insert_space_after_comma_in_array_initializer = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInArrayInitializerOption);
    }
    final Object insertSpaceAfterCommaInConstructorDeclarationParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS);
    if (insertSpaceAfterCommaInConstructorDeclarationParametersOption != null) {
      insert_space_after_comma_in_constructor_declaration_parameters = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInConstructorDeclarationParametersOption);
    }
    final Object insertSpaceAfterCommaInConstructorDeclarationThrowsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS);
    if (insertSpaceAfterCommaInConstructorDeclarationThrowsOption != null) {
      insert_space_after_comma_in_constructor_declaration_throws = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInConstructorDeclarationThrowsOption);
    }
    final Object insertSpaceAfterCommaInEnumConstantArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_CONSTANT_ARGUMENTS);
    if (insertSpaceAfterCommaInEnumConstantArgumentsOption != null) {
      insert_space_after_comma_in_enum_constant_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInEnumConstantArgumentsOption);
    }
    final Object insertSpaceAfterCommaInEnumDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_DECLARATIONS);
    if (insertSpaceAfterCommaInEnumDeclarationsOption != null) {
      insert_space_after_comma_in_enum_declarations = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInEnumDeclarationsOption);
    }
    final Object insertSpaceAfterCommaInExplicitConstructorCallArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS);
    if (insertSpaceAfterCommaInExplicitConstructorCallArgumentsOption != null) {
      insert_space_after_comma_in_explicit_constructor_call_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInExplicitConstructorCallArgumentsOption);
    }
    final Object insertSpaceAfterCommaInForIncrementsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS);
    if (insertSpaceAfterCommaInForIncrementsOption != null) {
      insert_space_after_comma_in_for_increments = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInForIncrementsOption);
    }
    final Object insertSpaceAfterCommaInForInitsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS);
    if (insertSpaceAfterCommaInForInitsOption != null) {
      insert_space_after_comma_in_for_inits = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInForInitsOption);
    }
    final Object insertSpaceAfterCommaInMethodInvocationArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS);
    if (insertSpaceAfterCommaInMethodInvocationArgumentsOption != null) {
      insert_space_after_comma_in_method_invocation_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInMethodInvocationArgumentsOption);
    }
    final Object insertSpaceAfterCommaInMethodDeclarationParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS);
    if (insertSpaceAfterCommaInMethodDeclarationParametersOption != null) {
      insert_space_after_comma_in_method_declaration_parameters = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInMethodDeclarationParametersOption);
    }
    final Object insertSpaceAfterCommaInMethodDeclarationThrowsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS);
    if (insertSpaceAfterCommaInMethodDeclarationThrowsOption != null) {
      insert_space_after_comma_in_method_declaration_throws = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInMethodDeclarationThrowsOption);
    }
    final Object insertSpaceAfterCommaInMultipleFieldDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS);
    if (insertSpaceAfterCommaInMultipleFieldDeclarationsOption != null) {
      insert_space_after_comma_in_multiple_field_declarations = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInMultipleFieldDeclarationsOption);
    }
    final Object insertSpaceAfterCommaInMultipleLocalDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS);
    if (insertSpaceAfterCommaInMultipleLocalDeclarationsOption != null) {
      insert_space_after_comma_in_multiple_local_declarations = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInMultipleLocalDeclarationsOption);
    }
    final Object insertSpaceAfterCommaInParameterizedTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE);
    if (insertSpaceAfterCommaInParameterizedTypeReferenceOption != null) {
      insert_space_after_comma_in_parameterized_type_reference = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInParameterizedTypeReferenceOption);
    }
    final Object insertSpaceAfterCommaInSuperinterfacesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES);
    if (insertSpaceAfterCommaInSuperinterfacesOption != null) {
      insert_space_after_comma_in_superinterfaces = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInSuperinterfacesOption);
    }
    final Object insertSpaceAfterCommaInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS);
    if (insertSpaceAfterCommaInTypeArgumentsOption != null) {
      insert_space_after_comma_in_type_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInTypeArgumentsOption);
    }
    final Object insertSpaceAfterCommaInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_PARAMETERS);
    if (insertSpaceAfterCommaInTypeParametersOption != null) {
      insert_space_after_comma_in_type_parameters = DartPreferenceConstants.INSERT.equals(insertSpaceAfterCommaInTypeParametersOption);
    }
    final Object insertSpaceAfterEllipsisOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ELLIPSIS);
    if (insertSpaceAfterEllipsisOption != null) {
      insert_space_after_ellipsis = DartPreferenceConstants.INSERT.equals(insertSpaceAfterEllipsisOption);
    }
    final Object insertSpaceAfterOpeningAngleBracketInParameterizedTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE);
    if (insertSpaceAfterOpeningAngleBracketInParameterizedTypeReferenceOption != null) {
      insert_space_after_opening_angle_bracket_in_parameterized_type_reference = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningAngleBracketInParameterizedTypeReferenceOption);
    }
    final Object insertSpaceAfterOpeningAngleBracketInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS);
    if (insertSpaceAfterOpeningAngleBracketInTypeArgumentsOption != null) {
      insert_space_after_opening_angle_bracket_in_type_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningAngleBracketInTypeArgumentsOption);
    }
    final Object insertSpaceAfterOpeningAngleBracketInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS);
    if (insertSpaceAfterOpeningAngleBracketInTypeParametersOption != null) {
      insert_space_after_opening_angle_bracket_in_type_parameters = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningAngleBracketInTypeParametersOption);
    }
    final Object insertSpaceAfterOpeningBracketInArrayAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION);
    if (insertSpaceAfterOpeningBracketInArrayAllocationExpressionOption != null) {
      insert_space_after_opening_bracket_in_array_allocation_expression = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningBracketInArrayAllocationExpressionOption);
    }
    final Object insertSpaceAfterOpeningBracketInArrayReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE);
    if (insertSpaceAfterOpeningBracketInArrayReferenceOption != null) {
      insert_space_after_opening_bracket_in_array_reference = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningBracketInArrayReferenceOption);
    }
    final Object insertSpaceAfterOpeningBraceInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER);
    if (insertSpaceAfterOpeningBraceInArrayInitializerOption != null) {
      insert_space_after_opening_brace_in_array_initializer = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningBraceInArrayInitializerOption);
    }
    final Object insertSpaceAfterOpeningParenInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ANNOTATION);
    if (insertSpaceAfterOpeningParenInAnnotationOption != null) {
      insert_space_after_opening_paren_in_annotation = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInAnnotationOption);
    }
    final Object insertSpaceAfterOpeningParenInCastOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CAST);
    if (insertSpaceAfterOpeningParenInCastOption != null) {
      insert_space_after_opening_paren_in_cast = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInCastOption);
    }
    final Object insertSpaceAfterOpeningParenInCatchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH);
    if (insertSpaceAfterOpeningParenInCatchOption != null) {
      insert_space_after_opening_paren_in_catch = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInCatchOption);
    }
    final Object insertSpaceAfterOpeningParenInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION);
    if (insertSpaceAfterOpeningParenInConstructorDeclarationOption != null) {
      insert_space_after_opening_paren_in_constructor_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInConstructorDeclarationOption);
    }
    final Object insertSpaceAfterOpeningParenInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ENUM_CONSTANT);
    if (insertSpaceAfterOpeningParenInEnumConstantOption != null) {
      insert_space_after_opening_paren_in_enum_constant = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInEnumConstantOption);
    }
    final Object insertSpaceAfterOpeningParenInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR);
    if (insertSpaceAfterOpeningParenInForOption != null) {
      insert_space_after_opening_paren_in_for = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInForOption);
    }
    final Object insertSpaceAfterOpeningParenInIfOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF);
    if (insertSpaceAfterOpeningParenInIfOption != null) {
      insert_space_after_opening_paren_in_if = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInIfOption);
    }
    final Object insertSpaceAfterOpeningParenInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION);
    if (insertSpaceAfterOpeningParenInMethodDeclarationOption != null) {
      insert_space_after_opening_paren_in_method_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInMethodDeclarationOption);
    }
    final Object insertSpaceAfterOpeningParenInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION);
    if (insertSpaceAfterOpeningParenInMethodInvocationOption != null) {
      insert_space_after_opening_paren_in_method_invocation = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInMethodInvocationOption);
    }
    final Object insertSpaceAfterOpeningParenInParenthesizedExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
    if (insertSpaceAfterOpeningParenInParenthesizedExpressionOption != null) {
      insert_space_after_opening_paren_in_parenthesized_expression = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInParenthesizedExpressionOption);
    }
    final Object insertSpaceAfterOpeningParenInSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH);
    if (insertSpaceAfterOpeningParenInSwitchOption != null) {
      insert_space_after_opening_paren_in_switch = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInSwitchOption);
    }
    final Object insertSpaceAfterOpeningParenInSynchronizedOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED);
    if (insertSpaceAfterOpeningParenInSynchronizedOption != null) {
      insert_space_after_opening_paren_in_synchronized = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInSynchronizedOption);
    }
    final Object insertSpaceAfterOpeningParenInWhileOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE);
    if (insertSpaceAfterOpeningParenInWhileOption != null) {
      insert_space_after_opening_paren_in_while = DartPreferenceConstants.INSERT.equals(insertSpaceAfterOpeningParenInWhileOption);
    }
    final Object insertSpaceAfterPostfixOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_POSTFIX_OPERATOR);
    if (insertSpaceAfterPostfixOperatorOption != null) {
      insert_space_after_postfix_operator = DartPreferenceConstants.INSERT.equals(insertSpaceAfterPostfixOperatorOption);
    }
    final Object insertSpaceAfterPrefixOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_PREFIX_OPERATOR);
    if (insertSpaceAfterPrefixOperatorOption != null) {
      insert_space_after_prefix_operator = DartPreferenceConstants.INSERT.equals(insertSpaceAfterPrefixOperatorOption);
    }
    final Object insertSpaceAfterQuestionInConditionalOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL);
    if (insertSpaceAfterQuestionInConditionalOption != null) {
      insert_space_after_question_in_conditional = DartPreferenceConstants.INSERT.equals(insertSpaceAfterQuestionInConditionalOption);
    }
    final Object insertSpaceAfterQuestionInWildcardOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_WILDCARD);
    if (insertSpaceAfterQuestionInWildcardOption != null) {
      insert_space_after_question_in_wilcard = DartPreferenceConstants.INSERT.equals(insertSpaceAfterQuestionInWildcardOption);
    }
    final Object insertSpaceAfterSemicolonInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR);
    if (insertSpaceAfterSemicolonInForOption != null) {
      insert_space_after_semicolon_in_for = DartPreferenceConstants.INSERT.equals(insertSpaceAfterSemicolonInForOption);
    }
    final Object insertSpaceAfterUnaryOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR);
    if (insertSpaceAfterUnaryOperatorOption != null) {
      insert_space_after_unary_operator = DartPreferenceConstants.INSERT.equals(insertSpaceAfterUnaryOperatorOption);
    }
    final Object insertSpaceBeforeAndInWildcardOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_AND_IN_TYPE_PARAMETER);
    if (insertSpaceBeforeAndInWildcardOption != null) {
      insert_space_before_and_in_type_parameter = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeAndInWildcardOption);
    }
    final Object insertSpaceBeforeAtInAnnotationTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_AT_IN_ANNOTATION_TYPE_DECLARATION);
    if (insertSpaceBeforeAtInAnnotationTypeDeclarationOption != null) {
      insert_space_before_at_in_annotation_type_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeAtInAnnotationTypeDeclarationOption);
    }
    final Object insertSpaceBeforeAssignmentOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR);
    if (insertSpaceBeforeAssignmentOperatorOption != null) {
      insert_space_before_assignment_operator = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeAssignmentOperatorOption);
    }
    final Object insertSpaceBeforeBinaryOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR);
    if (insertSpaceBeforeBinaryOperatorOption != null) {
      insert_space_before_binary_operator = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeBinaryOperatorOption);
    }
    final Object insertSpaceBeforeClosingAngleBracketInParameterizedTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE);
    if (insertSpaceBeforeClosingAngleBracketInParameterizedTypeReferenceOption != null) {
      insert_space_before_closing_angle_bracket_in_parameterized_type_reference = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingAngleBracketInParameterizedTypeReferenceOption);
    }
    final Object insertSpaceBeforeClosingAngleBracketInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS);
    if (insertSpaceBeforeClosingAngleBracketInTypeArgumentsOption != null) {
      insert_space_before_closing_angle_bracket_in_type_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingAngleBracketInTypeArgumentsOption);
    }
    final Object insertSpaceBeforeClosingAngleBracketInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS);
    if (insertSpaceBeforeClosingAngleBracketInTypeParametersOption != null) {
      insert_space_before_closing_angle_bracket_in_type_parameters = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingAngleBracketInTypeParametersOption);
    }
    final Object insertSpaceBeforeClosingBraceInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER);
    if (insertSpaceBeforeClosingBraceInArrayInitializerOption != null) {
      insert_space_before_closing_brace_in_array_initializer = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingBraceInArrayInitializerOption);
    }
    final Object insertSpaceBeforeClosingBracketInArrayAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION);
    if (insertSpaceBeforeClosingBracketInArrayAllocationExpressionOption != null) {
      insert_space_before_closing_bracket_in_array_allocation_expression = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingBracketInArrayAllocationExpressionOption);
    }
    final Object insertSpaceBeforeClosingBracketInArrayReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE);
    if (insertSpaceBeforeClosingBracketInArrayReferenceOption != null) {
      insert_space_before_closing_bracket_in_array_reference = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingBracketInArrayReferenceOption);
    }
    final Object insertSpaceBeforeClosingParenInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ANNOTATION);
    if (insertSpaceBeforeClosingParenInAnnotationOption != null) {
      insert_space_before_closing_paren_in_annotation = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInAnnotationOption);
    }
    final Object insertSpaceBeforeClosingParenInCastOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CAST);
    if (insertSpaceBeforeClosingParenInCastOption != null) {
      insert_space_before_closing_paren_in_cast = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInCastOption);
    }
    final Object insertSpaceBeforeClosingParenInCatchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH);
    if (insertSpaceBeforeClosingParenInCatchOption != null) {
      insert_space_before_closing_paren_in_catch = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInCatchOption);
    }
    final Object insertSpaceBeforeClosingParenInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION);
    if (insertSpaceBeforeClosingParenInConstructorDeclarationOption != null) {
      insert_space_before_closing_paren_in_constructor_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInConstructorDeclarationOption);
    }
    final Object insertSpaceBeforeClosingParenInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ENUM_CONSTANT);
    if (insertSpaceBeforeClosingParenInEnumConstantOption != null) {
      insert_space_before_closing_paren_in_enum_constant = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInEnumConstantOption);
    }
    final Object insertSpaceBeforeClosingParenInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR);
    if (insertSpaceBeforeClosingParenInForOption != null) {
      insert_space_before_closing_paren_in_for = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInForOption);
    }
    final Object insertSpaceBeforeClosingParenInIfOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF);
    if (insertSpaceBeforeClosingParenInIfOption != null) {
      insert_space_before_closing_paren_in_if = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInIfOption);
    }
    final Object insertSpaceBeforeClosingParenInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION);
    if (insertSpaceBeforeClosingParenInMethodDeclarationOption != null) {
      insert_space_before_closing_paren_in_method_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInMethodDeclarationOption);
    }
    final Object insertSpaceBeforeClosingParenInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION);
    if (insertSpaceBeforeClosingParenInMethodInvocationOption != null) {
      insert_space_before_closing_paren_in_method_invocation = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInMethodInvocationOption);
    }
    final Object insertSpaceBeforeClosingParenInParenthesizedExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_PARENTHESIZED_EXPRESSION);
    if (insertSpaceBeforeClosingParenInParenthesizedExpressionOption != null) {
      insert_space_before_closing_paren_in_parenthesized_expression = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInParenthesizedExpressionOption);
    }
    final Object insertSpaceBeforeClosingParenInSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH);
    if (insertSpaceBeforeClosingParenInSwitchOption != null) {
      insert_space_before_closing_paren_in_switch = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInSwitchOption);
    }
    final Object insertSpaceBeforeClosingParenInSynchronizedOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED);
    if (insertSpaceBeforeClosingParenInSynchronizedOption != null) {
      insert_space_before_closing_paren_in_synchronized = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInSynchronizedOption);
    }
    final Object insertSpaceBeforeClosingParenInWhileOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE);
    if (insertSpaceBeforeClosingParenInWhileOption != null) {
      insert_space_before_closing_paren_in_while = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeClosingParenInWhileOption);
    }
    final Object insertSpaceBeforeColonInAssertOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_ASSERT);
    if (insertSpaceBeforeColonInAssertOption != null) {
      insert_space_before_colon_in_assert = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeColonInAssertOption);
    }
    final Object insertSpaceBeforeColonInCaseOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE);
    if (insertSpaceBeforeColonInCaseOption != null) {
      insert_space_before_colon_in_case = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeColonInCaseOption);
    }
    final Object insertSpaceBeforeColonInConditionalOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL);
    if (insertSpaceBeforeColonInConditionalOption != null) {
      insert_space_before_colon_in_conditional = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeColonInConditionalOption);
    }
    final Object insertSpaceBeforeColonInDefaultOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT);
    if (insertSpaceBeforeColonInDefaultOption != null) {
      insert_space_before_colon_in_default = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeColonInDefaultOption);
    }
    final Object insertSpaceBeforeColonInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_FOR);
    if (insertSpaceBeforeColonInForOption != null) {
      insert_space_before_colon_in_for = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeColonInForOption);
    }
    final Object insertSpaceBeforeColonInLabeledStatementOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_LABELED_STATEMENT);
    if (insertSpaceBeforeColonInLabeledStatementOption != null) {
      insert_space_before_colon_in_labeled_statement = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeColonInLabeledStatementOption);
    }
    final Object insertSpaceBeforeCommaInAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ALLOCATION_EXPRESSION);
    if (insertSpaceBeforeCommaInAllocationExpressionOption != null) {
      insert_space_before_comma_in_allocation_expression = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInAllocationExpressionOption);
    }
    final Object insertSpaceBeforeCommaInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ANNOTATION);
    if (insertSpaceBeforeCommaInAnnotationOption != null) {
      insert_space_before_comma_in_annotation = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInAnnotationOption);
    }
    final Object insertSpaceBeforeCommaInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ARRAY_INITIALIZER);
    if (insertSpaceBeforeCommaInArrayInitializerOption != null) {
      insert_space_before_comma_in_array_initializer = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInArrayInitializerOption);
    }
    final Object insertSpaceBeforeCommaInConstructorDeclarationParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS);
    if (insertSpaceBeforeCommaInConstructorDeclarationParametersOption != null) {
      insert_space_before_comma_in_constructor_declaration_parameters = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInConstructorDeclarationParametersOption);
    }
    final Object insertSpaceBeforeCommaInConstructorDeclarationThrowsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS);
    if (insertSpaceBeforeCommaInConstructorDeclarationThrowsOption != null) {
      insert_space_before_comma_in_constructor_declaration_throws = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInConstructorDeclarationThrowsOption);
    }
    final Object insertSpaceBeforeCommaInEnumConstantArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_CONSTANT_ARGUMENTS);
    if (insertSpaceBeforeCommaInEnumConstantArgumentsOption != null) {
      insert_space_before_comma_in_enum_constant_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInEnumConstantArgumentsOption);
    }
    final Object insertSpaceBeforeCommaInEnumDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_DECLARATIONS);
    if (insertSpaceBeforeCommaInEnumDeclarationsOption != null) {
      insert_space_before_comma_in_enum_declarations = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInEnumDeclarationsOption);
    }
    final Object insertSpaceBeforeCommaInExplicitConstructorCallArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS);
    if (insertSpaceBeforeCommaInExplicitConstructorCallArgumentsOption != null) {
      insert_space_before_comma_in_explicit_constructor_call_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInExplicitConstructorCallArgumentsOption);
    }
    final Object insertSpaceBeforeCommaInForIncrementsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INCREMENTS);
    if (insertSpaceBeforeCommaInForIncrementsOption != null) {
      insert_space_before_comma_in_for_increments = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInForIncrementsOption);
    }
    final Object insertSpaceBeforeCommaInForInitsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INITS);
    if (insertSpaceBeforeCommaInForInitsOption != null) {
      insert_space_before_comma_in_for_inits = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInForInitsOption);
    }
    final Object insertSpaceBeforeCommaInMethodInvocationArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS);
    if (insertSpaceBeforeCommaInMethodInvocationArgumentsOption != null) {
      insert_space_before_comma_in_method_invocation_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInMethodInvocationArgumentsOption);
    }
    final Object insertSpaceBeforeCommaInMethodDeclarationParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS);
    if (insertSpaceBeforeCommaInMethodDeclarationParametersOption != null) {
      insert_space_before_comma_in_method_declaration_parameters = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInMethodDeclarationParametersOption);
    }
    final Object insertSpaceBeforeCommaInMethodDeclarationThrowsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS);
    if (insertSpaceBeforeCommaInMethodDeclarationThrowsOption != null) {
      insert_space_before_comma_in_method_declaration_throws = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInMethodDeclarationThrowsOption);
    }
    final Object insertSpaceBeforeCommaInMultipleFieldDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS);
    if (insertSpaceBeforeCommaInMultipleFieldDeclarationsOption != null) {
      insert_space_before_comma_in_multiple_field_declarations = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInMultipleFieldDeclarationsOption);
    }
    final Object insertSpaceBeforeCommaInMultipleLocalDeclarationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS);
    if (insertSpaceBeforeCommaInMultipleLocalDeclarationsOption != null) {
      insert_space_before_comma_in_multiple_local_declarations = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInMultipleLocalDeclarationsOption);
    }
    final Object insertSpaceBeforeCommaInParameterizedTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE);
    if (insertSpaceBeforeCommaInParameterizedTypeReferenceOption != null) {
      insert_space_before_comma_in_parameterized_type_reference = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInParameterizedTypeReferenceOption);
    }
    final Object insertSpaceBeforeCommaInSuperinterfacesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES);
    if (insertSpaceBeforeCommaInSuperinterfacesOption != null) {
      insert_space_before_comma_in_superinterfaces = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInSuperinterfacesOption);
    }
    final Object insertSpaceBeforeCommaInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_ARGUMENTS);
    if (insertSpaceBeforeCommaInTypeArgumentsOption != null) {
      insert_space_before_comma_in_type_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInTypeArgumentsOption);
    }
    final Object insertSpaceBeforeCommaInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_PARAMETERS);
    if (insertSpaceBeforeCommaInTypeParametersOption != null) {
      insert_space_before_comma_in_type_parameters = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeCommaInTypeParametersOption);
    }
    final Object insertSpaceBeforeEllipsisOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ELLIPSIS);
    if (insertSpaceBeforeEllipsisOption != null) {
      insert_space_before_ellipsis = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeEllipsisOption);
    }
    final Object insertSpaceBeforeOpeningAngleBrackerInParameterizedTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE);
    if (insertSpaceBeforeOpeningAngleBrackerInParameterizedTypeReferenceOption != null) {
      insert_space_before_opening_angle_bracket_in_parameterized_type_reference = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningAngleBrackerInParameterizedTypeReferenceOption);
    }
    final Object insertSpaceBeforeOpeningAngleBrackerInTypeArgumentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS);
    if (insertSpaceBeforeOpeningAngleBrackerInTypeArgumentsOption != null) {
      insert_space_before_opening_angle_bracket_in_type_arguments = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningAngleBrackerInTypeArgumentsOption);
    }
    final Object insertSpaceBeforeOpeningAngleBrackerInTypeParametersOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS);
    if (insertSpaceBeforeOpeningAngleBrackerInTypeParametersOption != null) {
      insert_space_before_opening_angle_bracket_in_type_parameters = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningAngleBrackerInTypeParametersOption);
    }
    final Object insertSpaceBeforeOpeningBraceInAnnotationTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANNOTATION_TYPE_DECLARATION);
    if (insertSpaceBeforeOpeningBraceInAnnotationTypeDeclarationOption != null) {
      insert_space_before_opening_brace_in_annotation_type_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBraceInAnnotationTypeDeclarationOption);
    }
    final Object insertSpaceBeforeOpeningBraceInAnonymousTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION);
    if (insertSpaceBeforeOpeningBraceInAnonymousTypeDeclarationOption != null) {
      insert_space_before_opening_brace_in_anonymous_type_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBraceInAnonymousTypeDeclarationOption);
    }
    final Object insertSpaceBeforeOpeningBraceInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER);
    if (insertSpaceBeforeOpeningBraceInArrayInitializerOption != null) {
      insert_space_before_opening_brace_in_array_initializer = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBraceInArrayInitializerOption);
    }
    final Object insertSpaceBeforeOpeningBraceInBlockOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK);
    if (insertSpaceBeforeOpeningBraceInBlockOption != null) {
      insert_space_before_opening_brace_in_block = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBraceInBlockOption);
    }
    final Object insertSpaceBeforeOpeningBraceInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION);
    if (insertSpaceBeforeOpeningBraceInConstructorDeclarationOption != null) {
      insert_space_before_opening_brace_in_constructor_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBraceInConstructorDeclarationOption);
    }
    final Object insertSpaceBeforeOpeningBraceInEnumDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_DECLARATION);
    if (insertSpaceBeforeOpeningBraceInEnumDeclarationOption != null) {
      insert_space_before_opening_brace_in_enum_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBraceInEnumDeclarationOption);
    }
    final Object insertSpaceBeforeOpeningBraceInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_CONSTANT);
    if (insertSpaceBeforeOpeningBraceInEnumConstantOption != null) {
      insert_space_before_opening_brace_in_enum_constant = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBraceInEnumConstantOption);
    }
    final Object insertSpaceBeforeOpeningBraceInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION);
    if (insertSpaceBeforeOpeningBraceInMethodDeclarationOption != null) {
      insert_space_before_opening_brace_in_method_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBraceInMethodDeclarationOption);
    }
    final Object insertSpaceBeforeOpeningBraceInTypeDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION);
    if (insertSpaceBeforeOpeningBraceInTypeDeclarationOption != null) {
      insert_space_before_opening_brace_in_type_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBraceInTypeDeclarationOption);
    }
    final Object insertSpaceBeforeOpeningBracketInArrayAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION);
    if (insertSpaceBeforeOpeningBracketInArrayAllocationExpressionOption != null) {
      insert_space_before_opening_bracket_in_array_allocation_expression = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBracketInArrayAllocationExpressionOption);
    }
    final Object insertSpaceBeforeOpeningBracketInArrayReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE);
    if (insertSpaceBeforeOpeningBracketInArrayReferenceOption != null) {
      insert_space_before_opening_bracket_in_array_reference = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBracketInArrayReferenceOption);
    }
    final Object insertSpaceBeforeOpeningBracketInArrayTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE);
    if (insertSpaceBeforeOpeningBracketInArrayTypeReferenceOption != null) {
      insert_space_before_opening_bracket_in_array_type_reference = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBracketInArrayTypeReferenceOption);
    }
    final Object insertSpaceBeforeOpeningParenInAnnotationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION);
    if (insertSpaceBeforeOpeningParenInAnnotationOption != null) {
      insert_space_before_opening_paren_in_annotation = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInAnnotationOption);
    }
    final Object insertSpaceBeforeOpeningParenInAnnotationTypeMemberDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION_TYPE_MEMBER_DECLARATION);
    if (insertSpaceBeforeOpeningParenInAnnotationTypeMemberDeclarationOption != null) {
      insert_space_before_opening_paren_in_annotation_type_member_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInAnnotationTypeMemberDeclarationOption);
    }
    final Object insertSpaceBeforeOpeningParenInCatchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH);
    if (insertSpaceBeforeOpeningParenInCatchOption != null) {
      insert_space_before_opening_paren_in_catch = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInCatchOption);
    }
    final Object insertSpaceBeforeOpeningParenInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION);
    if (insertSpaceBeforeOpeningParenInConstructorDeclarationOption != null) {
      insert_space_before_opening_paren_in_constructor_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInConstructorDeclarationOption);
    }
    final Object insertSpaceBeforeOpeningParenInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ENUM_CONSTANT);
    if (insertSpaceBeforeOpeningParenInEnumConstantOption != null) {
      insert_space_before_opening_paren_in_enum_constant = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInEnumConstantOption);
    }
    final Object insertSpaceBeforeOpeningParenInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR);
    if (insertSpaceBeforeOpeningParenInForOption != null) {
      insert_space_before_opening_paren_in_for = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInForOption);
    }
    final Object insertSpaceBeforeOpeningParenInIfOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF);
    if (insertSpaceBeforeOpeningParenInIfOption != null) {
      insert_space_before_opening_paren_in_if = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInIfOption);
    }
    final Object insertSpaceBeforeOpeningParenInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION);
    if (insertSpaceBeforeOpeningParenInMethodInvocationOption != null) {
      insert_space_before_opening_paren_in_method_invocation = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInMethodInvocationOption);
    }
    final Object insertSpaceBeforeOpeningParenInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION);
    if (insertSpaceBeforeOpeningParenInMethodDeclarationOption != null) {
      insert_space_before_opening_paren_in_method_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInMethodDeclarationOption);
    }
    final Object insertSpaceBeforeOpeningParenInSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH);
    if (insertSpaceBeforeOpeningParenInSwitchOption != null) {
      insert_space_before_opening_paren_in_switch = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInSwitchOption);
    }
    final Object insertSpaceBeforeOpeningBraceInSwitchOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH);
    if (insertSpaceBeforeOpeningBraceInSwitchOption != null) {
      insert_space_before_opening_brace_in_switch = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningBraceInSwitchOption);
    }
    final Object insertSpaceBeforeOpeningParenInSynchronizedOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED);
    if (insertSpaceBeforeOpeningParenInSynchronizedOption != null) {
      insert_space_before_opening_paren_in_synchronized = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInSynchronizedOption);
    }
    final Object insertSpaceBeforeOpeningParenInParenthesizedExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
    if (insertSpaceBeforeOpeningParenInParenthesizedExpressionOption != null) {
      insert_space_before_opening_paren_in_parenthesized_expression = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInParenthesizedExpressionOption);
    }
    final Object insertSpaceBeforeOpeningParenInWhileOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE);
    if (insertSpaceBeforeOpeningParenInWhileOption != null) {
      insert_space_before_opening_paren_in_while = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeOpeningParenInWhileOption);
    }
    final Object insertSpaceBeforeParenthesizedExpressionInReturnOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_RETURN);
    if (insertSpaceBeforeParenthesizedExpressionInReturnOption != null) {
      insert_space_before_parenthesized_expression_in_return = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeParenthesizedExpressionInReturnOption);
    }
    final Object insertSpaceBeforeParenthesizedExpressionInThrowOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_THROW);
    if (insertSpaceBeforeParenthesizedExpressionInThrowOption != null) {
      insert_space_before_parenthesized_expression_in_throw = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeParenthesizedExpressionInThrowOption);
    }
    final Object insertSpaceBeforePostfixOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_POSTFIX_OPERATOR);
    if (insertSpaceBeforePostfixOperatorOption != null) {
      insert_space_before_postfix_operator = DartPreferenceConstants.INSERT.equals(insertSpaceBeforePostfixOperatorOption);
    }
    final Object insertSpaceBeforePrefixOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PREFIX_OPERATOR);
    if (insertSpaceBeforePrefixOperatorOption != null) {
      insert_space_before_prefix_operator = DartPreferenceConstants.INSERT.equals(insertSpaceBeforePrefixOperatorOption);
    }
    final Object insertSpaceBeforeQuestionInConditionalOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL);
    if (insertSpaceBeforeQuestionInConditionalOption != null) {
      insert_space_before_question_in_conditional = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeQuestionInConditionalOption);
    }
    final Object insertSpaceBeforeQuestionInWildcardOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_WILDCARD);
    if (insertSpaceBeforeQuestionInWildcardOption != null) {
      insert_space_before_question_in_wilcard = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeQuestionInWildcardOption);
    }
    final Object insertSpaceBeforeSemicolonOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON);
    if (insertSpaceBeforeSemicolonOption != null) {
      insert_space_before_semicolon = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeSemicolonOption);
    }
    final Object insertSpaceBeforeSemicolonInForOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_FOR);
    if (insertSpaceBeforeSemicolonInForOption != null) {
      insert_space_before_semicolon_in_for = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeSemicolonInForOption);
    }
    final Object insertSpaceBeforeUnaryOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_UNARY_OPERATOR);
    if (insertSpaceBeforeUnaryOperatorOption != null) {
      insert_space_before_unary_operator = DartPreferenceConstants.INSERT.equals(insertSpaceBeforeUnaryOperatorOption);
    }
    final Object insertSpaceBetweenBracketsInArrayTypeReferenceOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_TYPE_REFERENCE);
    if (insertSpaceBetweenBracketsInArrayTypeReferenceOption != null) {
      insert_space_between_brackets_in_array_type_reference = DartPreferenceConstants.INSERT.equals(insertSpaceBetweenBracketsInArrayTypeReferenceOption);
    }
    final Object insertSpaceBetweenEmptyBracesInArrayInitializerOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER);
    if (insertSpaceBetweenEmptyBracesInArrayInitializerOption != null) {
      insert_space_between_empty_braces_in_array_initializer = DartPreferenceConstants.INSERT.equals(insertSpaceBetweenEmptyBracesInArrayInitializerOption);
    }
    final Object insertSpaceBetweenEmptyBracketsInArrayAllocationExpressionOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACKETS_IN_ARRAY_ALLOCATION_EXPRESSION);
    if (insertSpaceBetweenEmptyBracketsInArrayAllocationExpressionOption != null) {
      insert_space_between_empty_brackets_in_array_allocation_expression = DartPreferenceConstants.INSERT.equals(insertSpaceBetweenEmptyBracketsInArrayAllocationExpressionOption);
    }
    final Object insertSpaceBetweenEmptyParensInConstructorDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION);
    if (insertSpaceBetweenEmptyParensInConstructorDeclarationOption != null) {
      insert_space_between_empty_parens_in_constructor_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBetweenEmptyParensInConstructorDeclarationOption);
    }
    final Object insertSpaceBetweenEmptyParensInAnnotationTypeMemberDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ANNOTATION_TYPE_MEMBER_DECLARATION);
    if (insertSpaceBetweenEmptyParensInAnnotationTypeMemberDeclarationOption != null) {
      insert_space_between_empty_parens_in_annotation_type_member_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBetweenEmptyParensInAnnotationTypeMemberDeclarationOption);
    }
    final Object insertSpaceBetweenEmptyParensInEnumConstantOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ENUM_CONSTANT);
    if (insertSpaceBetweenEmptyParensInEnumConstantOption != null) {
      insert_space_between_empty_parens_in_enum_constant = DartPreferenceConstants.INSERT.equals(insertSpaceBetweenEmptyParensInEnumConstantOption);
    }
    final Object insertSpaceBetweenEmptyParensInMethodDeclarationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION);
    if (insertSpaceBetweenEmptyParensInMethodDeclarationOption != null) {
      insert_space_between_empty_parens_in_method_declaration = DartPreferenceConstants.INSERT.equals(insertSpaceBetweenEmptyParensInMethodDeclarationOption);
    }
    final Object insertSpaceBetweenEmptyParensInMethodInvocationOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION);
    if (insertSpaceBetweenEmptyParensInMethodInvocationOption != null) {
      insert_space_between_empty_parens_in_method_invocation = DartPreferenceConstants.INSERT.equals(insertSpaceBetweenEmptyParensInMethodInvocationOption);
    }
    final Object compactElseIfOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_COMPACT_ELSE_IF);
    if (compactElseIfOption != null) {
      compact_else_if = DefaultCodeFormatterConstants.TRUE.equals(compactElseIfOption);
    }
    final Object keepGuardianClauseOnOneLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_GUARDIAN_CLAUSE_ON_ONE_LINE);
    if (keepGuardianClauseOnOneLineOption != null) {
      keep_guardian_clause_on_one_line = DefaultCodeFormatterConstants.TRUE.equals(keepGuardianClauseOnOneLineOption);
    }
    final Object keepElseStatementOnSameLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_ELSE_STATEMENT_ON_SAME_LINE);
    if (keepElseStatementOnSameLineOption != null) {
      keep_else_statement_on_same_line = DefaultCodeFormatterConstants.TRUE.equals(keepElseStatementOnSameLineOption);
    }
    final Object keepEmptyArrayInitializerOnOneLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_EMPTY_ARRAY_INITIALIZER_ON_ONE_LINE);
    if (keepEmptyArrayInitializerOnOneLineOption != null) {
      keep_empty_array_initializer_on_one_line = DefaultCodeFormatterConstants.TRUE.equals(keepEmptyArrayInitializerOnOneLineOption);
    }
    final Object keepSimpleIfOnOneLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE);
    if (keepSimpleIfOnOneLineOption != null) {
      keep_simple_if_on_one_line = DefaultCodeFormatterConstants.TRUE.equals(keepSimpleIfOnOneLineOption);
    }
    final Object keepThenStatementOnSameLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_KEEP_THEN_STATEMENT_ON_SAME_LINE);
    if (keepThenStatementOnSameLineOption != null) {
      keep_then_statement_on_same_line = DefaultCodeFormatterConstants.TRUE.equals(keepThenStatementOnSameLineOption);
    }
    final Object neverIndentBlockCommentOnFirstColumnOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN);
    if (neverIndentBlockCommentOnFirstColumnOption != null) {
      never_indent_block_comments_on_first_column = DefaultCodeFormatterConstants.TRUE.equals(neverIndentBlockCommentOnFirstColumnOption);
    }
    final Object neverIndentLineCommentOnFirstColumnOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN);
    if (neverIndentLineCommentOnFirstColumnOption != null) {
      never_indent_line_comments_on_first_column = DefaultCodeFormatterConstants.TRUE.equals(neverIndentLineCommentOnFirstColumnOption);
    }
    final Object numberOfEmptyLinesToPreserveOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE);
    if (numberOfEmptyLinesToPreserveOption != null) {
      try {
        number_of_empty_lines_to_preserve = Integer.parseInt((String) numberOfEmptyLinesToPreserveOption);
      } catch (NumberFormatException e) {
        number_of_empty_lines_to_preserve = 0;
      } catch (ClassCastException e) {
        number_of_empty_lines_to_preserve = 0;
      }
    }
    final Object joinLinesInCommentsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_JOIN_LINES_IN_COMMENTS);
    if (joinLinesInCommentsOption != null) {
      join_lines_in_comments = DefaultCodeFormatterConstants.TRUE.equals(joinLinesInCommentsOption);
    }
    final Object joinWrappedLinesOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_JOIN_WRAPPED_LINES);
    if (joinWrappedLinesOption != null) {
      join_wrapped_lines = DefaultCodeFormatterConstants.TRUE.equals(joinWrappedLinesOption);
    }
    final Object putEmptyStatementOnNewLineOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE);
    if (putEmptyStatementOnNewLineOption != null) {
      put_empty_statement_on_new_line = DefaultCodeFormatterConstants.TRUE.equals(putEmptyStatementOnNewLineOption);
    }
    final Object tabSizeOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE);
    if (tabSizeOption != null) {
      try {
        tab_size = Integer.parseInt((String) tabSizeOption);
      } catch (NumberFormatException e) {
        tab_size = 4;
      } catch (ClassCastException e) {
        tab_size = 4;
      }
    }
    final Object useTabsOnlyForLeadingIndentationsOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_USE_TABS_ONLY_FOR_LEADING_INDENTATIONS);
    if (useTabsOnlyForLeadingIndentationsOption != null) {
      use_tabs_only_for_leading_indentations = DefaultCodeFormatterConstants.TRUE.equals(useTabsOnlyForLeadingIndentationsOption);
    }
    final Object pageWidthOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT);
    if (pageWidthOption != null) {
      try {
        page_width = Integer.parseInt((String) pageWidthOption);
      } catch (NumberFormatException e) {
        page_width = 80;
      } catch (ClassCastException e) {
        page_width = 80;
      }
    }
    final Object useTabOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR);
    if (useTabOption != null) {
      if (DartPreferenceConstants.TAB.equals(useTabOption)) {
        tab_char = TAB;
      } else if (DartPreferenceConstants.SPACE.equals(useTabOption)) {
        tab_char = SPACE;
      } else {
        tab_char = MIXED;
      }
    }
    final Object wrapBeforeBinaryOperatorOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BINARY_OPERATOR);
    if (wrapBeforeBinaryOperatorOption != null) {
      wrap_before_binary_operator = DefaultCodeFormatterConstants.TRUE.equals(wrapBeforeBinaryOperatorOption);
    }
    final Object useTags = settings.get(DefaultCodeFormatterConstants.FORMATTER_USE_ON_OFF_TAGS);
    if (useTags != null) {
      use_tags = DefaultCodeFormatterConstants.TRUE.equals(useTags);
    }
    final Object disableTagOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_DISABLING_TAG);
    if (disableTagOption != null) {
      if (disableTagOption instanceof String) {
        String stringValue = (String) disableTagOption;
        int idx = stringValue.indexOf('\n');
        if (idx == 0) {
          disabling_tag = null;
        } else {
          String tag = idx < 0 ? stringValue.trim() : stringValue.substring(0, idx).trim();
          if (tag.length() == 0) {
            disabling_tag = null;
          } else {
            disabling_tag = tag.toCharArray();
          }
        }
      }
    }
    final Object enableTagOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_ENABLING_TAG);
    if (enableTagOption != null) {
      if (enableTagOption instanceof String) {
        String stringValue = (String) enableTagOption;
        int idx = stringValue.indexOf('\n');
        if (idx == 0) {
          enabling_tag = null;
        } else {
          String tag = idx < 0 ? stringValue.trim() : stringValue.substring(0, idx).trim();
          if (tag.length() == 0) {
            enabling_tag = null;
          } else {
            enabling_tag = tag.toCharArray();
          }
        }
      }
    }
    final Object wrapWrapOuterExpressionsWhenNestedOption = settings.get(DefaultCodeFormatterConstants.FORMATTER_WRAP_OUTER_EXPRESSIONS_WHEN_NESTED);
    if (wrapWrapOuterExpressionsWhenNestedOption != null) {
      wrap_outer_expressions_when_nested = DefaultCodeFormatterConstants.TRUE.equals(wrapWrapOuterExpressionsWhenNestedOption);
    }
  }

  public void setDartConventionsSettings() {
    alignment_for_arguments_in_allocation_expression = Alignment.M_COMPACT_SPLIT;
    alignment_for_arguments_in_annotation = Alignment.M_NO_ALIGNMENT;
    alignment_for_arguments_in_enum_constant = Alignment.M_COMPACT_SPLIT;
    alignment_for_arguments_in_explicit_constructor_call = Alignment.M_COMPACT_SPLIT;
    alignment_for_arguments_in_method_invocation = Alignment.M_COMPACT_SPLIT;
    alignment_for_arguments_in_qualified_allocation_expression = Alignment.M_COMPACT_SPLIT;
    alignment_for_assignment = Alignment.M_NO_ALIGNMENT;
    alignment_for_binary_expression = Alignment.M_COMPACT_SPLIT;
    alignment_for_compact_if = Alignment.M_COMPACT_SPLIT;
    alignment_for_conditional_expression = Alignment.M_NEXT_PER_LINE_SPLIT;
    alignment_for_enum_constants = Alignment.NONE;
    alignment_for_expressions_in_array_initializer = Alignment.M_COMPACT_SPLIT;
    alignment_for_method_declaration = Alignment.M_NO_ALIGNMENT;
    alignment_for_multiple_fields = Alignment.M_COMPACT_SPLIT;
    alignment_for_initializers_in_constructor_declaration = Alignment.M_ONE_PER_LINE_SPLIT;
    alignment_for_parameters_in_constructor_declaration = Alignment.M_COMPACT_SPLIT;
    alignment_for_parameters_in_method_declaration = Alignment.M_COMPACT_SPLIT;
    alignment_for_selector_in_method_invocation = Alignment.M_COMPACT_SPLIT;
    alignment_for_superclass_in_type_declaration = Alignment.M_COMPACT_SPLIT;
    alignment_for_superinterfaces_in_enum_declaration = Alignment.M_COMPACT_SPLIT;
    alignment_for_superinterfaces_in_type_declaration = Alignment.M_COMPACT_SPLIT;
    alignment_for_throws_clause_in_constructor_declaration = Alignment.M_COMPACT_SPLIT;
    alignment_for_throws_clause_in_method_declaration = Alignment.M_COMPACT_SPLIT;
    align_type_members_on_columns = false;
    brace_position_for_annotation_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_anonymous_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_array_initializer = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_block = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_block_in_case = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_constructor_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_enum_constant = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_function_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_method_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_switch = DefaultCodeFormatterConstants.END_OF_LINE;
    comment_clear_blank_lines_in_block_comment = false;
    comment_clear_blank_lines_in_javadoc_comment = false;
    comment_format_block_comment = true;
    comment_format_javadoc_comment = true;
    comment_format_line_comment = true;
    comment_format_line_comment_starting_on_first_column = true;
    comment_format_header = false;
    comment_format_html = true;
    comment_format_source = true;
    comment_indent_parameter_description = true;
    comment_indent_root_tags = true;
    comment_insert_empty_line_before_root_tags = true;
    comment_insert_new_line_for_parameter = true;
    comment_new_lines_at_block_boundaries = true;
    comment_new_lines_at_javadoc_boundaries = true;
    comment_line_length = 80;
    continuation_indentation = 2;
    continuation_indentation_for_array_initializer = 2;
    blank_lines_after_imports = 1;
    blank_lines_after_package = 1;
    blank_lines_before_field = 0;
    blank_lines_before_first_class_body_declaration = 0;
    blank_lines_before_imports = 1;
    blank_lines_before_member_type = 1;
    blank_lines_before_method = 1;
    blank_lines_before_new_chunk = 1;
    blank_lines_before_package = 0;
    blank_lines_between_import_groups = 1;
    blank_lines_between_type_declarations = 1;
    blank_lines_at_beginning_of_method_body = 0;
    indent_statements_compare_to_block = true;
    indent_statements_compare_to_body = true;
    indent_body_declarations_compare_to_annotation_declaration_header = true;
    indent_body_declarations_compare_to_enum_constant_header = true;
    indent_body_declarations_compare_to_enum_declaration_header = true;
    indent_body_declarations_compare_to_type_header = true;
    indent_breaks_compare_to_cases = true;
    indent_empty_lines = false;
    indent_switchstatements_compare_to_cases = true;
    indent_switchstatements_compare_to_switch = false;
    indentation_size = 4;
    insert_new_line_after_annotation_on_type = true;
    insert_new_line_after_annotation_on_field = true;
    insert_new_line_after_annotation_on_method = true;
    insert_new_line_after_annotation_on_package = true;
    insert_new_line_after_annotation_on_parameter = false;
    insert_new_line_after_annotation_on_local_variable = true;
    insert_new_line_after_opening_brace_in_array_initializer = false;
    insert_new_line_at_end_of_file_if_missing = true;
    insert_new_line_before_catch_in_try_statement = false;
    insert_new_line_before_closing_brace_in_array_initializer = false;
    insert_new_line_before_else_in_if_statement = false;
    insert_new_line_before_finally_in_try_statement = false;
    insert_new_line_before_while_in_do_statement = false;
    insert_new_line_in_empty_anonymous_type_declaration = true;
    insert_new_line_in_empty_block = true;
    insert_new_line_in_empty_annotation_declaration = true;
    insert_new_line_in_empty_enum_constant = true;
    insert_new_line_in_empty_enum_declaration = true;
    insert_new_line_in_empty_method_body = true;
    insert_new_line_in_empty_type_declaration = true;
    insert_space_after_and_in_type_parameter = true;
    insert_space_after_assignment_operator = true;
    insert_space_after_at_in_annotation = false;
    insert_space_after_at_in_annotation_type_declaration = false;
    insert_space_after_binary_operator = true;
    insert_space_after_closing_angle_bracket_in_type_arguments = true;
    insert_space_after_closing_angle_bracket_in_type_parameters = true;
    insert_space_after_closing_paren_in_cast = true;
    insert_space_after_closing_brace_in_block = true;
    insert_space_after_colon_in_assert = true;
    insert_space_after_colon_in_case = true;
    insert_space_after_colon_in_conditional = true;
    insert_space_after_colon_in_for = true;
    insert_space_after_colon_in_labeled_statement = true;
    insert_space_after_comma_in_allocation_expression = true;
    insert_space_after_comma_in_annotation = true;
    insert_space_after_comma_in_array_initializer = true;
    insert_space_after_comma_in_constructor_declaration_parameters = true;
    insert_space_after_comma_in_constructor_declaration_throws = true;
    insert_space_after_comma_in_enum_constant_arguments = true;
    insert_space_after_comma_in_enum_declarations = true;
    insert_space_after_comma_in_explicit_constructor_call_arguments = true;
    insert_space_after_comma_in_for_increments = true;
    insert_space_after_comma_in_for_inits = true;
    insert_space_after_comma_in_method_invocation_arguments = true;
    insert_space_after_comma_in_method_declaration_parameters = true;
    insert_space_after_comma_in_method_declaration_throws = true;
    insert_space_after_comma_in_multiple_field_declarations = true;
    insert_space_after_comma_in_multiple_local_declarations = true;
    insert_space_after_comma_in_parameterized_type_reference = true;
    insert_space_after_comma_in_superinterfaces = true;
    insert_space_after_comma_in_type_arguments = true;
    insert_space_after_comma_in_type_parameters = true;
    insert_space_after_ellipsis = false;
    insert_space_after_opening_angle_bracket_in_parameterized_type_reference = false;
    insert_space_after_opening_angle_bracket_in_type_arguments = false;
    insert_space_after_opening_angle_bracket_in_type_parameters = false;
    insert_space_after_opening_bracket_in_array_allocation_expression = false;
    insert_space_after_opening_bracket_in_array_reference = false;
    insert_space_after_opening_brace_in_array_initializer = true;
    insert_space_after_opening_paren_in_annotation = false;
    insert_space_after_opening_paren_in_cast = false;
    insert_space_after_opening_paren_in_catch = false;
    insert_space_after_opening_paren_in_constructor_declaration = false;
    insert_space_after_opening_paren_in_enum_constant = false;
    insert_space_after_opening_paren_in_for = false;
    insert_space_after_opening_paren_in_if = false;
    insert_space_after_opening_paren_in_method_declaration = false;
    insert_space_after_opening_paren_in_method_invocation = false;
    insert_space_after_opening_paren_in_parenthesized_expression = false;
    insert_space_after_opening_paren_in_switch = false;
    insert_space_after_opening_paren_in_synchronized = false;
    insert_space_after_opening_paren_in_while = false;
    insert_space_after_postfix_operator = false;
    insert_space_after_prefix_operator = false;
    insert_space_after_question_in_conditional = true;
    insert_space_after_question_in_wilcard = false;
    insert_space_after_semicolon_in_for = true;
    insert_space_after_unary_operator = false;
    insert_space_before_and_in_type_parameter = true;
    insert_space_before_at_in_annotation_type_declaration = true;
    insert_space_before_assignment_operator = true;
    insert_space_before_binary_operator = true;
    insert_space_before_closing_angle_bracket_in_parameterized_type_reference = false;
    insert_space_before_closing_angle_bracket_in_type_arguments = false;
    insert_space_before_closing_angle_bracket_in_type_parameters = false;
    insert_space_before_closing_brace_in_array_initializer = true;
    insert_space_before_closing_bracket_in_array_allocation_expression = false;
    insert_space_before_closing_bracket_in_array_reference = false;
    insert_space_before_closing_paren_in_annotation = false;
    insert_space_before_closing_paren_in_cast = false;
    insert_space_before_closing_paren_in_catch = false;
    insert_space_before_closing_paren_in_constructor_declaration = false;
    insert_space_before_closing_paren_in_enum_constant = false;
    insert_space_before_closing_paren_in_for = false;
    insert_space_before_closing_paren_in_if = false;
    insert_space_before_closing_paren_in_method_declaration = false;
    insert_space_before_closing_paren_in_method_invocation = false;
    insert_space_before_closing_paren_in_parenthesized_expression = false;
    insert_space_before_closing_paren_in_switch = false;
    insert_space_before_closing_paren_in_synchronized = false;
    insert_space_before_closing_paren_in_while = false;
    insert_space_before_colon_in_assert = true;
    insert_space_before_colon_in_case = false;
    insert_space_before_colon_in_conditional = true;
    insert_space_before_colon_in_default = false;
    insert_space_before_colon_in_for = true;
    insert_space_before_colon_in_labeled_statement = false;
    insert_space_before_comma_in_allocation_expression = false;
    insert_space_before_comma_in_array_initializer = false;
    insert_space_before_comma_in_constructor_declaration_parameters = false;
    insert_space_before_comma_in_constructor_declaration_throws = false;
    insert_space_before_comma_in_enum_constant_arguments = false;
    insert_space_before_comma_in_enum_declarations = false;
    insert_space_before_comma_in_explicit_constructor_call_arguments = false;
    insert_space_before_comma_in_for_increments = false;
    insert_space_before_comma_in_for_inits = false;
    insert_space_before_comma_in_method_invocation_arguments = false;
    insert_space_before_comma_in_method_declaration_parameters = false;
    insert_space_before_comma_in_method_declaration_throws = false;
    insert_space_before_comma_in_multiple_field_declarations = false;
    insert_space_before_comma_in_multiple_local_declarations = false;
    insert_space_before_comma_in_parameterized_type_reference = false;
    insert_space_before_comma_in_superinterfaces = false;
    insert_space_before_comma_in_type_arguments = false;
    insert_space_before_comma_in_type_parameters = false;
    insert_space_before_ellipsis = false;
    insert_space_before_parenthesized_expression_in_return = true;
    insert_space_before_parenthesized_expression_in_throw = true;
    insert_space_before_opening_angle_bracket_in_parameterized_type_reference = false;
    insert_space_before_opening_angle_bracket_in_type_arguments = false;
    insert_space_before_opening_angle_bracket_in_type_parameters = false;
    insert_space_before_opening_brace_in_annotation_type_declaration = true;
    insert_space_before_opening_brace_in_anonymous_type_declaration = true;
    insert_space_before_opening_brace_in_array_initializer = true;
    insert_space_before_opening_brace_in_block = true;
    insert_space_before_opening_brace_in_constructor_declaration = true;
    insert_space_before_opening_brace_in_enum_constant = true;
    insert_space_before_opening_brace_in_enum_declaration = true;
    insert_space_before_opening_brace_in_method_declaration = true;
    insert_space_before_opening_brace_in_switch = true;
    insert_space_before_opening_brace_in_type_declaration = true;
    insert_space_before_opening_bracket_in_array_allocation_expression = false;
    insert_space_before_opening_bracket_in_array_reference = false;
    insert_space_before_opening_bracket_in_array_type_reference = false;
    insert_space_before_opening_paren_in_annotation = false;
    insert_space_before_opening_paren_in_annotation_type_member_declaration = false;
    insert_space_before_opening_paren_in_catch = true;
    insert_space_before_opening_paren_in_constructor_declaration = false;
    insert_space_before_opening_paren_in_enum_constant = false;
    insert_space_before_opening_paren_in_for = true;
    insert_space_before_opening_paren_in_if = true;
    insert_space_before_opening_paren_in_method_invocation = false;
    insert_space_before_opening_paren_in_method_declaration = false;
    insert_space_before_opening_paren_in_switch = true;
    insert_space_before_opening_paren_in_synchronized = true;
    insert_space_before_opening_paren_in_parenthesized_expression = false;
    insert_space_before_opening_paren_in_while = true;
    insert_space_before_postfix_operator = false;
    insert_space_before_prefix_operator = false;
    insert_space_before_question_in_conditional = true;
    insert_space_before_question_in_wilcard = false;
    insert_space_before_semicolon = false;
    insert_space_before_semicolon_in_for = false;
    insert_space_before_unary_operator = false;
    insert_space_between_brackets_in_array_type_reference = false;
    insert_space_between_empty_braces_in_array_initializer = false;
    insert_space_between_empty_brackets_in_array_allocation_expression = false;
    insert_space_between_empty_parens_in_annotation_type_member_declaration = false;
    insert_space_between_empty_parens_in_constructor_declaration = false;
    insert_space_between_empty_parens_in_enum_constant = false;
    insert_space_between_empty_parens_in_method_declaration = false;
    insert_space_between_empty_parens_in_method_invocation = false;
    compact_else_if = true;
    keep_guardian_clause_on_one_line = false;
    keep_else_statement_on_same_line = false;
    keep_empty_array_initializer_on_one_line = false;
    keep_simple_if_on_one_line = false;
    keep_then_statement_on_same_line = false;
    never_indent_block_comments_on_first_column = false;
    never_indent_line_comments_on_first_column = true;
    number_of_empty_lines_to_preserve = 1;
    join_lines_in_comments = true;
    join_wrapped_lines = true;
    put_empty_statement_on_new_line = true;
    tab_size = 2;
    page_width = 80;
    tab_char = SPACE;
    use_tabs_only_for_leading_indentations = false;
    wrap_before_binary_operator = true;
    use_tags = false;
    disabling_tag = DEFAULT_DISABLING_TAG;
    enabling_tag = DEFAULT_ENABLING_TAG;
    wrap_outer_expressions_when_nested = true;
  }

  public void setDefaultSettings() {
    alignment_for_arguments_in_allocation_expression = Alignment.M_COMPACT_SPLIT;
    alignment_for_arguments_in_annotation = Alignment.M_NO_ALIGNMENT;
    alignment_for_arguments_in_enum_constant = Alignment.M_COMPACT_SPLIT;
    alignment_for_arguments_in_explicit_constructor_call = Alignment.M_COMPACT_SPLIT;
    alignment_for_arguments_in_method_invocation = Alignment.M_COMPACT_SPLIT;
    alignment_for_arguments_in_qualified_allocation_expression = Alignment.M_COMPACT_SPLIT;
    alignment_for_assignment = Alignment.M_NO_ALIGNMENT;
    alignment_for_binary_expression = Alignment.M_COMPACT_SPLIT;
    alignment_for_compact_if = Alignment.M_ONE_PER_LINE_SPLIT | Alignment.M_INDENT_BY_ONE;
    alignment_for_conditional_expression = Alignment.M_ONE_PER_LINE_SPLIT;
    alignment_for_enum_constants = Alignment.NONE;
    alignment_for_expressions_in_array_initializer = Alignment.M_COMPACT_SPLIT;
    alignment_for_method_declaration = Alignment.M_NO_ALIGNMENT;
    alignment_for_multiple_fields = Alignment.M_COMPACT_SPLIT;
    alignment_for_initializers_in_constructor_declaration = Alignment.M_ONE_PER_LINE_SPLIT;
    alignment_for_parameters_in_constructor_declaration = Alignment.M_COMPACT_SPLIT;
    alignment_for_parameters_in_method_declaration = Alignment.M_COMPACT_SPLIT;
    alignment_for_selector_in_method_invocation = Alignment.M_COMPACT_SPLIT;
    alignment_for_superclass_in_type_declaration = Alignment.M_NEXT_SHIFTED_SPLIT;
    alignment_for_superinterfaces_in_enum_declaration = Alignment.M_NEXT_SHIFTED_SPLIT;
    alignment_for_superinterfaces_in_type_declaration = Alignment.M_NEXT_SHIFTED_SPLIT;
    alignment_for_throws_clause_in_constructor_declaration = Alignment.M_COMPACT_SPLIT;
    alignment_for_throws_clause_in_method_declaration = Alignment.M_COMPACT_SPLIT;
    align_type_members_on_columns = false;
    brace_position_for_annotation_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_anonymous_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_array_initializer = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_block = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_block_in_case = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_constructor_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_enum_constant = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_function_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_method_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_type_declaration = DefaultCodeFormatterConstants.END_OF_LINE;
    brace_position_for_switch = DefaultCodeFormatterConstants.END_OF_LINE;
    comment_clear_blank_lines_in_block_comment = false;
    comment_clear_blank_lines_in_javadoc_comment = false;
    comment_format_block_comment = true;
    comment_format_javadoc_comment = true;
    comment_format_line_comment = true;
    comment_format_line_comment_starting_on_first_column = true;
    comment_format_header = false;
    comment_format_html = true;
    comment_format_source = true;
    comment_indent_parameter_description = true;
    comment_indent_root_tags = true;
    comment_insert_empty_line_before_root_tags = true;
    comment_insert_new_line_for_parameter = true;
    comment_new_lines_at_block_boundaries = true;
    comment_new_lines_at_javadoc_boundaries = true;
    comment_line_length = 80;
    continuation_indentation = 2;
    continuation_indentation_for_array_initializer = 2;
    blank_lines_after_imports = 0;
    blank_lines_after_package = 0;
    blank_lines_before_field = 0;
    blank_lines_before_first_class_body_declaration = 0;
    blank_lines_before_imports = 0;
    blank_lines_before_member_type = 0;
    blank_lines_before_method = 0;
    blank_lines_before_new_chunk = 0;
    blank_lines_before_package = 0;
    blank_lines_between_import_groups = 1;
    blank_lines_between_type_declarations = 0;
    blank_lines_at_beginning_of_method_body = 0;
    indent_statements_compare_to_block = true;
    indent_statements_compare_to_body = true;
    indent_body_declarations_compare_to_annotation_declaration_header = true;
    indent_body_declarations_compare_to_enum_constant_header = true;
    indent_body_declarations_compare_to_enum_declaration_header = true;
    indent_body_declarations_compare_to_type_header = true;
    indent_breaks_compare_to_cases = true;
    indent_empty_lines = false;
    indent_switchstatements_compare_to_cases = true;
    indent_switchstatements_compare_to_switch = true;
    indentation_size = 4;
    insert_new_line_after_annotation_on_type = true;
    insert_new_line_after_annotation_on_field = true;
    insert_new_line_after_annotation_on_method = true;
    insert_new_line_after_annotation_on_package = true;
    insert_new_line_after_annotation_on_parameter = false;
    insert_new_line_after_annotation_on_local_variable = true;
    insert_new_line_after_opening_brace_in_array_initializer = false;
    insert_new_line_at_end_of_file_if_missing = true;
    insert_new_line_before_catch_in_try_statement = false;
    insert_new_line_before_closing_brace_in_array_initializer = false;
    insert_new_line_before_else_in_if_statement = false;
    insert_new_line_before_finally_in_try_statement = false;
    insert_new_line_before_while_in_do_statement = false;
    insert_new_line_in_empty_anonymous_type_declaration = true;
    insert_new_line_in_empty_block = true;
    insert_new_line_in_empty_annotation_declaration = true;
    insert_new_line_in_empty_enum_constant = true;
    insert_new_line_in_empty_enum_declaration = true;
    insert_new_line_in_empty_method_body = true;
    insert_new_line_in_empty_type_declaration = true;
    insert_space_after_and_in_type_parameter = true;
    insert_space_after_assignment_operator = true;
    insert_space_after_at_in_annotation = false;
    insert_space_after_at_in_annotation_type_declaration = false;
    insert_space_after_binary_operator = true;
    insert_space_after_closing_angle_bracket_in_type_arguments = true;
    insert_space_after_closing_angle_bracket_in_type_parameters = true;
    insert_space_after_closing_paren_in_cast = true;
    insert_space_after_closing_brace_in_block = true;
    insert_space_after_colon_in_assert = true;
    insert_space_after_colon_in_case = true;
    insert_space_after_colon_in_conditional = true;
    insert_space_after_colon_in_for = true;
    insert_space_after_colon_in_labeled_statement = true;
    insert_space_after_comma_in_allocation_expression = true;
    insert_space_after_comma_in_annotation = true;
    insert_space_after_comma_in_array_initializer = true;
    insert_space_after_comma_in_constructor_declaration_parameters = true;
    insert_space_after_comma_in_constructor_declaration_throws = true;
    insert_space_after_comma_in_enum_constant_arguments = true;
    insert_space_after_comma_in_enum_declarations = true;
    insert_space_after_comma_in_explicit_constructor_call_arguments = true;
    insert_space_after_comma_in_for_increments = true;
    insert_space_after_comma_in_for_inits = true;
    insert_space_after_comma_in_method_invocation_arguments = true;
    insert_space_after_comma_in_method_declaration_parameters = true;
    insert_space_after_comma_in_method_declaration_throws = true;
    insert_space_after_comma_in_multiple_field_declarations = true;
    insert_space_after_comma_in_multiple_local_declarations = true;
    insert_space_after_comma_in_parameterized_type_reference = true;
    insert_space_after_comma_in_superinterfaces = true;
    insert_space_after_comma_in_type_arguments = true;
    insert_space_after_comma_in_type_parameters = true;
    insert_space_after_ellipsis = false;
    insert_space_after_opening_angle_bracket_in_parameterized_type_reference = false;
    insert_space_after_opening_angle_bracket_in_type_arguments = false;
    insert_space_after_opening_angle_bracket_in_type_parameters = false;
    insert_space_after_opening_bracket_in_array_allocation_expression = false;
    insert_space_after_opening_bracket_in_array_reference = false;
    insert_space_after_opening_brace_in_array_initializer = false;
    insert_space_after_opening_paren_in_annotation = false;
    insert_space_after_opening_paren_in_cast = false;
    insert_space_after_opening_paren_in_catch = false;
    insert_space_after_opening_paren_in_constructor_declaration = false;
    insert_space_after_opening_paren_in_enum_constant = false;
    insert_space_after_opening_paren_in_for = false;
    insert_space_after_opening_paren_in_if = false;
    insert_space_after_opening_paren_in_method_declaration = false;
    insert_space_after_opening_paren_in_method_invocation = false;
    insert_space_after_opening_paren_in_parenthesized_expression = false;
    insert_space_after_opening_paren_in_switch = false;
    insert_space_after_opening_paren_in_synchronized = false;
    insert_space_after_opening_paren_in_while = false;
    insert_space_after_postfix_operator = false;
    insert_space_after_prefix_operator = false;
    insert_space_after_question_in_conditional = true;
    insert_space_after_question_in_wilcard = false;
    insert_space_after_semicolon_in_for = true;
    insert_space_after_unary_operator = false;
    insert_space_before_and_in_type_parameter = true;
    insert_space_before_at_in_annotation_type_declaration = true;
    insert_space_before_assignment_operator = true;
    insert_space_before_binary_operator = true;
    insert_space_before_closing_angle_bracket_in_parameterized_type_reference = false;
    insert_space_before_closing_angle_bracket_in_type_arguments = false;
    insert_space_before_closing_angle_bracket_in_type_parameters = false;
    insert_space_before_closing_brace_in_array_initializer = false;
    insert_space_before_closing_bracket_in_array_allocation_expression = false;
    insert_space_before_closing_bracket_in_array_reference = false;
    insert_space_before_closing_paren_in_annotation = false;
    insert_space_before_closing_paren_in_cast = false;
    insert_space_before_closing_paren_in_catch = false;
    insert_space_before_closing_paren_in_constructor_declaration = false;
    insert_space_before_closing_paren_in_enum_constant = false;
    insert_space_before_closing_paren_in_for = false;
    insert_space_before_closing_paren_in_if = false;
    insert_space_before_closing_paren_in_method_declaration = false;
    insert_space_before_closing_paren_in_method_invocation = false;
    insert_space_before_closing_paren_in_parenthesized_expression = false;
    insert_space_before_closing_paren_in_switch = false;
    insert_space_before_closing_paren_in_synchronized = false;
    insert_space_before_closing_paren_in_while = false;
    insert_space_before_colon_in_assert = true;
    insert_space_before_colon_in_case = true;
    insert_space_before_colon_in_conditional = true;
    insert_space_before_colon_in_default = true;
    insert_space_before_colon_in_for = true;
    insert_space_before_colon_in_labeled_statement = true;
    insert_space_before_comma_in_allocation_expression = false;
    insert_space_before_comma_in_array_initializer = false;
    insert_space_before_comma_in_constructor_declaration_parameters = false;
    insert_space_before_comma_in_constructor_declaration_throws = false;
    insert_space_before_comma_in_enum_constant_arguments = false;
    insert_space_before_comma_in_enum_declarations = false;
    insert_space_before_comma_in_explicit_constructor_call_arguments = false;
    insert_space_before_comma_in_for_increments = false;
    insert_space_before_comma_in_for_inits = false;
    insert_space_before_comma_in_method_invocation_arguments = false;
    insert_space_before_comma_in_method_declaration_parameters = false;
    insert_space_before_comma_in_method_declaration_throws = false;
    insert_space_before_comma_in_multiple_field_declarations = false;
    insert_space_before_comma_in_multiple_local_declarations = false;
    insert_space_before_comma_in_parameterized_type_reference = false;
    insert_space_before_comma_in_superinterfaces = false;
    insert_space_before_comma_in_type_arguments = false;
    insert_space_before_comma_in_type_parameters = false;
    insert_space_before_ellipsis = false;
    insert_space_before_parenthesized_expression_in_return = true;
    insert_space_before_parenthesized_expression_in_throw = true;
    insert_space_before_opening_angle_bracket_in_parameterized_type_reference = false;
    insert_space_before_opening_angle_bracket_in_type_arguments = false;
    insert_space_before_opening_angle_bracket_in_type_parameters = false;
    insert_space_before_opening_brace_in_annotation_type_declaration = true;
    insert_space_before_opening_brace_in_anonymous_type_declaration = true;
    insert_space_before_opening_brace_in_array_initializer = false;
    insert_space_before_opening_brace_in_block = true;
    insert_space_before_opening_brace_in_constructor_declaration = true;
    insert_space_before_opening_brace_in_enum_constant = true;
    insert_space_before_opening_brace_in_enum_declaration = true;
    insert_space_before_opening_brace_in_method_declaration = true;
    insert_space_before_opening_brace_in_switch = true;
    insert_space_before_opening_brace_in_type_declaration = true;
    insert_space_before_opening_bracket_in_array_allocation_expression = false;
    insert_space_before_opening_bracket_in_array_reference = false;
    insert_space_before_opening_bracket_in_array_type_reference = false;
    insert_space_before_opening_paren_in_annotation = false;
    insert_space_before_opening_paren_in_annotation_type_member_declaration = false;
    insert_space_before_opening_paren_in_catch = true;
    insert_space_before_opening_paren_in_constructor_declaration = false;
    insert_space_before_opening_paren_in_enum_constant = false;
    insert_space_before_opening_paren_in_for = true;
    insert_space_before_opening_paren_in_if = true;
    insert_space_before_opening_paren_in_method_invocation = false;
    insert_space_before_opening_paren_in_method_declaration = false;
    insert_space_before_opening_paren_in_switch = true;
    insert_space_before_opening_paren_in_synchronized = true;
    insert_space_before_opening_paren_in_parenthesized_expression = false;
    insert_space_before_opening_paren_in_while = true;
    insert_space_before_postfix_operator = false;
    insert_space_before_prefix_operator = false;
    insert_space_before_question_in_conditional = true;
    insert_space_before_question_in_wilcard = false;
    insert_space_before_semicolon = false;
    insert_space_before_semicolon_in_for = false;
    insert_space_before_unary_operator = false;
    insert_space_between_brackets_in_array_type_reference = false;
    insert_space_between_empty_braces_in_array_initializer = false;
    insert_space_between_empty_brackets_in_array_allocation_expression = false;
    insert_space_between_empty_parens_in_annotation_type_member_declaration = false;
    insert_space_between_empty_parens_in_constructor_declaration = false;
    insert_space_between_empty_parens_in_enum_constant = false;
    insert_space_between_empty_parens_in_method_declaration = false;
    insert_space_between_empty_parens_in_method_invocation = false;
    compact_else_if = true;
    keep_guardian_clause_on_one_line = false;
    keep_else_statement_on_same_line = false;
    keep_empty_array_initializer_on_one_line = false;
    keep_simple_if_on_one_line = false;
    keep_then_statement_on_same_line = false;
    never_indent_block_comments_on_first_column = false;
    never_indent_line_comments_on_first_column = true;
    number_of_empty_lines_to_preserve = 1;
    join_lines_in_comments = true;
    join_wrapped_lines = true;
    put_empty_statement_on_new_line = false;
    tab_size = 2;
    page_width = 80;
    tab_char = SPACE; // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=49081
    use_tabs_only_for_leading_indentations = false;
    wrap_before_binary_operator = true;
    use_tags = false;
    disabling_tag = DEFAULT_DISABLING_TAG;
    enabling_tag = DEFAULT_ENABLING_TAG;
    wrap_outer_expressions_when_nested = true;
  }

  public void setEclipseDefaultSettings() {
    setDartConventionsSettings();
    tab_char = SPACE;
    tab_size = 2;
  }

  private String getAlignment(int alignment) {
    return Integer.toString(alignment);
  }
}
