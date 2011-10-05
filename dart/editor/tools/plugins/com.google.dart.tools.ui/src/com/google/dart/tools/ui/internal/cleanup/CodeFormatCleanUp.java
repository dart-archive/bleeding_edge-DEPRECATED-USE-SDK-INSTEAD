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
package com.google.dart.tools.ui.internal.cleanup;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.cleanup.CleanUpContext;
import com.google.dart.tools.ui.cleanup.CleanUpRequirements;
import com.google.dart.tools.ui.cleanup.ICleanUpFix;
import com.google.dart.tools.ui.internal.cleanup.IMultiLineCleanUp.MultiLineCleanUpContext;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;

import java.util.ArrayList;
import java.util.Map;

public class CodeFormatCleanUp extends AbstractCleanUp {

  public CodeFormatCleanUp() {
    super();
  }

  public CodeFormatCleanUp(Map<String, String> options) {
    super(options);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
    CompilationUnit compilationUnit = context.getCompilationUnit();
    if (compilationUnit == null) {
      return null;
    }

    IRegion[] regions;
    if (context instanceof MultiLineCleanUpContext) {
      regions = ((MultiLineCleanUpContext) context).getRegions();
    } else {
      regions = null;
    }
    boolean removeWhitespaces = isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
    return CodeFormatFix.createCleanUp(compilationUnit, regions,
        isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE), removeWhitespaces
            && isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL),
        removeWhitespaces
            && isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY),
        isEnabled(CleanUpConstants.FORMAT_CORRECT_INDENTATION));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPreview() {
    StringBuffer buf = new StringBuffer();
    buf.append("/**\n"); //$NON-NLS-1$
    buf.append(" *A Javadoc comment\n"); //$NON-NLS-1$
    buf.append("* @since 2007\n"); //$NON-NLS-1$
    buf.append(" */\n"); //$NON-NLS-1$
    buf.append("public class Engine {\n"); //$NON-NLS-1$
    buf.append("  public void start() {}\n"); //$NON-NLS-1$
    if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES)
        && isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL)) {
      buf.append("\n"); //$NON-NLS-1$
    } else {
      buf.append("    \n"); //$NON-NLS-1$
    }
    if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES)) {
      buf.append("    public\n"); //$NON-NLS-1$
    } else {
      buf.append("    public \n"); //$NON-NLS-1$
    }
    buf.append("        void stop() {\n"); //$NON-NLS-1$
    buf.append("    }\n"); //$NON-NLS-1$
    buf.append("}\n"); //$NON-NLS-1$

    return buf.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CleanUpRequirements getRequirements() {
    boolean requiresChangedRegions = isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE)
        && isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
    return new CleanUpRequirements(false, false, requiresChangedRegions, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getStepDescriptions() {
    ArrayList<String> result = new ArrayList<String>();
    if (isEnabled(CleanUpConstants.FORMAT_SOURCE_CODE)) {
      result.add(MultiFixMessages.CodeFormatCleanUp_description);
    }

    if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES)) {
      if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL)) {
        result.add(MultiFixMessages.CodeFormatCleanUp_RemoveTrailingAll_description);
      } else if (isEnabled(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY)) {
        result.add(MultiFixMessages.CodeFormatCleanUp_RemoveTrailingNoEmpty_description);
      }
    }

    if (isEnabled(CleanUpConstants.FORMAT_CORRECT_INDENTATION)) {
      result.add(MultiFixMessages.CodeFormatCleanUp_correctIndentation_description);
    }

    return result.toArray(new String[result.size()]);
  }
}
