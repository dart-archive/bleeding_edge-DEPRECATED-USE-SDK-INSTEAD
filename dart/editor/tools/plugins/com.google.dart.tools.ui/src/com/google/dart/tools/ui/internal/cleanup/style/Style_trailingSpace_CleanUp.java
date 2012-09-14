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
package com.google.dart.tools.ui.internal.cleanup.style;

import com.google.common.base.CharMatcher;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.ui.internal.cleanup.migration.AbstractMigrateCleanUp;

/**
 * Removes trailing whitespace.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Style_trailingSpace_CleanUp extends AbstractMigrateCleanUp {
  @Override
  protected void createFix() throws Exception {
    String eol = utils.getEndOfLine();
    String source = unit.getSource();
    String[] lines = StringUtils.splitPreserveAllTokens(source, eol);
    int offset = 0;
    for (String line : lines) {
      int lastWhitespace = CharMatcher.WHITESPACE.negate().lastIndexIn(line) + 1;
      int spaceEnd = offset + line.length();
      int spaceStart = offset + lastWhitespace;
      if (spaceEnd != spaceStart) {
        addReplaceEdit(SourceRangeFactory.forStartEnd(spaceStart, spaceEnd), "");
      }
      offset += line.length() + eol.length();
    }
  }
}
