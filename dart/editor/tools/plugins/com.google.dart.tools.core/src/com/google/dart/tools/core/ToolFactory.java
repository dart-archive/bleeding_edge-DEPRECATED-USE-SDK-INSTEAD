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
package com.google.dart.tools.core;

import com.google.dart.tools.core.formatter.CodeFormatter;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.core.internal.formatter.DefaultCodeFormatter;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating various compiler tools, such as scanners, parsers and compilers.
 * <p>
 * This class provides static methods only; it is not intended to be instantiated or subclassed by
 * clients.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class ToolFactory {
  /**
   * This mode is used for formatting new code when some formatter options should not be used. In
   * particular, options that preserve the indentation of comments are not used. In the future,
   * newly added options may be ignored as well.
   * <p>
   * Clients that are formatting new code are recommended to use this mode.
   * </p>
   * 
   * @see DefaultCodeFormatterConstants#FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN
   * @see DefaultCodeFormatterConstants#FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN
   * @see #createCodeFormatter(Map, int)
   */
  public static final int M_FORMAT_NEW = 0;

  /**
   * This mode is used for formatting existing code when all formatter options should be used. In
   * particular, options that preserve the indentation of comments are used.
   * <p>
   * Clients that are formatting existing code are recommended to use this mode.
   * </p>
   * 
   * @see DefaultCodeFormatterConstants#FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN
   * @see DefaultCodeFormatterConstants#FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN
   * @see #createCodeFormatter(Map, int)
   */
  public static final int M_FORMAT_EXISTING = 1;

  /**
   * Create an instance of the built-in code formatter.
   * <p>
   * The given options should at least provide the source level (
   * {@link JavaScriptCore#COMPILER_SOURCE}), the compiler compliance level (
   * {@link JavaScriptCore#COMPILER_COMPLIANCE}) and the target platform (
   * {@link JavaScriptCore#COMPILER_CODEGEN_TARGET_PLATFORM}). Without these options, it is not
   * possible for the code formatter to know what kind of source it needs to format.
   * </p>
   * <p>
   * Note this is equivalent to <code>createCodeFormatter(options, M_FORMAT_NEW)</code>. Thus some
   * code formatter options may be ignored. See @{link {@link #M_FORMAT_NEW} for more details.
   * </p>
   * 
   * @param options - the options map to use for formatting with the default code formatter.
   *          Recognized options are documented on <code>JavaScriptCore#getDefaultOptions()</code>.
   *          If set to <code>null</code>, then use the current settings from
   *          <code>JavaScriptCore#getOptions</code>.
   * @return an instance of the built-in code formatter
   * @see CodeFormatter
   * @see JavaScriptCore#getOptions()
   */
  public static CodeFormatter createCodeFormatter(Map<String, String> options) {
    return createCodeFormatter(options, M_FORMAT_NEW);
  }

  /**
   * Create an instance of the built-in code formatter.
   * <p>
   * The given options should at least provide the source level (
   * {@link JavaScriptCore#COMPILER_SOURCE}), the compiler compliance level (
   * {@link JavaScriptCore#COMPILER_COMPLIANCE}) and the target platform (
   * {@link JavaScriptCore#COMPILER_CODEGEN_TARGET_PLATFORM}). Without these options, it is not
   * possible for the code formatter to know what kind of source it needs to format.
   * </p>
   * <p>
   * The given mode determines what options should be enabled when formatting the code. It can have
   * the following values: {@link #M_FORMAT_NEW}, {@link #M_FORMAT_EXISTING}, but other values may
   * be added in the future.
   * </p>
   * 
   * @param options the options map to use for formatting with the default code formatter.
   *          Recognized options are documented on <code>JavaScriptCore#getDefaultOptions()</code>.
   *          If set to <code>null</code>, then use the current settings from
   *          <code>JavaScriptCore#getOptions</code>.
   * @param mode the given mode to modify the given options.
   * @return an instance of the built-in code formatter
   * @see CodeFormatter
   * @see JavaScriptCore#getOptions()
   */
  public static CodeFormatter createCodeFormatter(Map<String, String> options, int mode) {
    if (options == null) {
      options = DartCore.getOptions();
    }
    Map<String, String> currentOptions = new HashMap<String, String>(options);
    if (mode == M_FORMAT_NEW) {
      // disable the option for not indenting comments starting on first column
      currentOptions.put(
          DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN,
          DefaultCodeFormatterConstants.FALSE);
      currentOptions.put(
          DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN,
          DefaultCodeFormatterConstants.FALSE);
    }
    return new DefaultCodeFormatter(currentOptions);
  }
}
