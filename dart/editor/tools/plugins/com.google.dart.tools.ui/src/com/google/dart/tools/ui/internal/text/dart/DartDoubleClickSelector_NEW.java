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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.ui.internal.text.functions.DartPairMatcher;
import com.google.dart.tools.ui.internal.text.functions.ISourceVersionDependent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

/**
 * Double click strategy aware of Java identifier syntax rules.
 * 
 * @coverage dart.editor.ui.text
 */
public class DartDoubleClickSelector_NEW implements ITextDoubleClickStrategy,
    ISourceVersionDependent {
  protected static final char[] BRACKETS = {'{', '}', '(', ')', '[', ']', '<', '>'};
  protected DartPairMatcher pairMatcher = new DartPairMatcher(BRACKETS);

  @Override
  public void doubleClicked(ITextViewer textViewer) {
    IDocument document = textViewer.getDocument();
    // prepare offset
    int offset = textViewer.getSelectedRange().x;
    if (offset < 0) {
      return;
    }
    // try pair matching
    {
      IRegion region = pairMatcher.match(document, offset);
      if (region != null && region.getLength() >= 2) {
        textViewer.setSelectedRange(region.getOffset() + 1, region.getLength() - 2);
        return;
      }
    }
    // try word
    IRegion region = selectWord(document, offset);
    textViewer.setSelectedRange(region.getOffset(), region.getLength());
  }

  @Override
  public void setSourceVersion(String version) {
  }

  private IRegion selectWord(IDocument document, int anchor) {
    try {
      int length = document.getLength();
      // forward
      int end = anchor;
      while (end < length) {
        char c = document.getChar(end);
        if (!Character.isJavaIdentifierPart(c)) {
          break;
        }
        end++;
      }
      // backward
      int start = anchor;
      while (start > 0) {
        char c = document.getChar(start - 1);
        if (!Character.isJavaIdentifierPart(c)) {
          break;
        }
        if (c == '$' && start > 1 && !Character.isJavaIdentifierPart(document.getChar(start - 2))) {
          break;
        }
        start--;
      }
      // done
      return new Region(start, end - start);
    } catch (BadLocationException e) {
      return new Region(anchor, 0);
    }
  }
}
