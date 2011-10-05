/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jface.text.formatter.FormattingContext;

/**
 * Formatting context for the comment formatter.
 * 
 * @since 3.0
 */
public class DartFormattingContext extends FormattingContext {

  /*
   * @see org.eclipse.jface.text.formatter.IFormattingContext#getPreferenceKeys()
   */
  @Override
  public String[] getPreferenceKeys() {
    return new String[] {
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_SOURCE,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_ROOT_TAGS,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT,
        DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HTML};
  }

  /*
   * @see org.eclipse.jface.text.formatter.IFormattingContext#isBooleanPreference(java.lang.String)
   */
  @Override
  public boolean isBooleanPreference(String key) {
    return !key.equals(DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH);
  }

  /*
   * @see org.eclipse.jface.text.formatter.IFormattingContext#isIntegerPreference(java.lang.String)
   */
  @Override
  public boolean isIntegerPreference(String key) {
    return key.equals(DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH);
  }
}
