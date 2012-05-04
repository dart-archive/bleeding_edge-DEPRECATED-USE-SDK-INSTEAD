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
package com.google.dart.tools.core.internal.dom.rewrite;

import com.google.dart.tools.core.formatter.IndentManipulation;

import org.eclipse.text.edits.ISourceModifier;
import org.eclipse.text.edits.ReplaceEdit;

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of the class <code>SourceModifier</code>
 */
public class SourceModifier implements ISourceModifier {
  private final String destinationIndent;
  private final int sourceIndentLevel;
  private final int tabWidth;
  private final int indentWidth;

  public SourceModifier(int sourceIndentLevel, String destinationIndent, int tabWidth,
      int indentWidth) {
    this.destinationIndent = destinationIndent;
    this.sourceIndentLevel = sourceIndentLevel;
    this.tabWidth = tabWidth;
    this.indentWidth = indentWidth;
  }

  @Override
  public ISourceModifier copy() {
    // We are state less
    return this;
  }

  @Override
  public ReplaceEdit[] getModifications(String source) {
    List<ReplaceEdit> result = new ArrayList<ReplaceEdit>();
    int destIndentLevel = IndentManipulation.measureIndentUnits(
        destinationIndent,
        tabWidth,
        indentWidth);
    if (destIndentLevel == sourceIndentLevel) {
      return result.toArray(new ReplaceEdit[result.size()]);
    }
    return IndentManipulation.getChangeIndentEdits(
        source,
        sourceIndentLevel,
        tabWidth,
        indentWidth,
        destinationIndent);
  }
}
