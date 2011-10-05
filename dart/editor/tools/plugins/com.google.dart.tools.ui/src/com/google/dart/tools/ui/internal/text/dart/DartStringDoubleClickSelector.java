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
package com.google.dart.tools.ui.internal.text.dart;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextUtilities;

/**
 * Double click strategy aware of Java string and character syntax rules.
 */
public class DartStringDoubleClickSelector extends DartDoubleClickSelector {

  private String fPartitioning;

  /**
   * Creates a new Java string double click selector for the given document partitioning.
   * 
   * @param partitioning the document partitioning
   */
  public DartStringDoubleClickSelector(String partitioning) {
    super();
    fPartitioning = partitioning;
  }

  /*
   * @see ITextDoubleClickStrategy#doubleClicked(ITextViewer)
   */
  @Override
  public void doubleClicked(ITextViewer textViewer) {

    int offset = textViewer.getSelectedRange().x;

    if (offset < 0) {
      return;
    }

    IDocument document = textViewer.getDocument();

    IRegion region = match(document, offset);
    if (region != null && region.getLength() >= 2) {
      textViewer.setSelectedRange(region.getOffset() + 1, region.getLength() - 2);
    } else {
      region = selectWord(document, offset);
      textViewer.setSelectedRange(region.getOffset(), region.getLength());
    }
  }

  private IRegion match(IDocument document, int offset) {
    try {
      if ((document.getChar(offset) == '"') || (document.getChar(offset) == '\'')
          || (document.getChar(offset - 1) == '"') || (document.getChar(offset - 1) == '\'')) {
        return TextUtilities.getPartition(document, fPartitioning, offset, true);
      }
    } catch (BadLocationException e) {
    }

    return null;
  }
}
