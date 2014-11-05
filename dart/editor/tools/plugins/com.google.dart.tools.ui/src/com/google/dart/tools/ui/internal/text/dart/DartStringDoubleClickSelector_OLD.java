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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.InterpolationString;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

/**
 * Double click strategy aware of Java string and character syntax rules.
 * 
 * @coverage dart.editor.ui.text
 */
public class DartStringDoubleClickSelector_OLD extends DartDoubleClickSelector_OLD {
  @Override
  public void doubleClicked(ITextViewer textViewer) {
    // prepare offset
    int offset = textViewer.getSelectedRange().x;
    if (offset < 0) {
      return;
    }
    // prepare document
    IDocument document = textViewer.getDocument();
    // try to get string region
    IRegion region = match(document, offset);
    if (region != null && region.getLength() > 0) {
      textViewer.setSelectedRange(region.getOffset(), region.getLength());
    } else {
      region = selectWord(document, offset);
      try {
        String selected = document.get(region.getOffset(), region.getLength());
        if (selected.indexOf('$') >= 0) {
          CompilationUnitEditor editor = ((CompilationUnitEditor.AdaptedSourceViewer) textViewer).getEditor();
          NodeLocator locator = new NodeLocator(offset);
          AstNode node = locator.searchWithin(editor.getInputUnit());
          if (node instanceof InterpolationString) {
            IRegion strRegion = computeStringRegion(node);
            if (strRegion != null) {
              region = strRegion;
            }
          }
        }
      } catch (BadLocationException ex) {
        // ignore it
      }
      textViewer.setSelectedRange(region.getOffset(), region.getLength());
    }
  }

  private IRegion match(IDocument document, int offset) {
    try {
      // previous is quote, search forward
      {
        char c = document.getChar(offset - 1);
        if (c == '"' || c == '\'') {
          int end = match(document, offset, 1, c);
          return new Region(offset, end - offset);
        }
      }
      // next is quote, search backward
      {
        char c = document.getChar(offset);
        if (c == '"' || c == '\'') {
          int end = match(document, offset - 1, -1, c) + 1;
          return new Region(end, offset - end);
        }
      }
    } catch (BadLocationException e) {
    }
    return null;
  }

  private int match(IDocument document, int offset, int delta, char charToFind)
      throws BadLocationException {
    for (;; offset += delta) {
      char c = document.getChar(offset);
      if (c == charToFind) {
        return offset;
      }
      if (c == '\\') {
      }
    }
  }
}
