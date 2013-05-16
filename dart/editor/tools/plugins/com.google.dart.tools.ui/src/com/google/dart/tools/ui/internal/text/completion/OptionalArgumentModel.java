/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.completion;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;

import java.util.List;

/**
 * Adds support to the linked mode editor that removes unneeded optional arguments from a method
 * invocation completion proposal after it has been inserted and edited. The superclass is marked
 * 
 * @noextend, but we need to ignore that.
 */
class OptionalArgumentModel extends LinkedModeModel {

  private boolean hasNamed;
  private LinkedModeUI ui;
  private ITextViewer viewer;
  private int exitLocation;
  private int delta;
  private List<OptionalArgumentPosition> positions;
  private IDocument document;
  private boolean isStopped;

  public void exitPositionUpdater(LinkedModeUI ui, ITextViewer viewer, int loc) {
    this.ui = ui;
    this.viewer = viewer;
    this.exitLocation = loc;
    this.isStopped = false;
  }

  public void setHasNamed(boolean hasNamed) {
    this.hasNamed = hasNamed;
  }

  public void setPositions(List<OptionalArgumentPosition> positions, IDocument document) {
    this.positions = positions;
    this.document = document;
  }

  @Override
  public void stopForwarding(int flags) {
    try {
      if (isStopped) {
        // This is a hack. Don't do it twice, if the framework somehow re-enters this method.
        return;
      }
      isStopped = true;
      if ((flags & ILinkedModeListener.EXTERNAL_MODIFICATION) != 0) {
        return;
      }

      // Clean up unnecessary optional arguments.
      MultiTextEdit textEdit = updateUneditedPositions();
      if (textEdit.getChildrenSize() == 0) {
        return;
      }
      positions.clear();
      // Move the exit position so the text cursor appears in the right place.
      ui.setExitPosition(viewer, exitLocation - delta, 0, Integer.MAX_VALUE);
      // NOW we can update the document. This allows each argument to be restored via undo.
      textEdit.apply(document);
    } catch (BadLocationException ex) {
      // ignore it
    } finally {
      super.stopForwarding(flags);
    }
  }

  private void deleteText(OptionalArgumentPosition pos, MultiTextEdit textEdit,
      boolean preserveContent) throws BadLocationException {
    IDocument doc = pos.getDocument();
    int len = doc.getLength();
    int offset = pos.getNameOffset();
    int end = pos.getNameLength() + offset;
    char ch = doc.getChar(end);
    if (ch == ',') {
      ch = doc.getChar(++end);
      while (Character.isWhitespace(ch)) {
        if (++end == len) {
          return;
        }
        ch = doc.getChar(end);
      }
    }
    if (preserveContent) {
      offset = pos.getNameLength() + offset;
    }
    len = end - offset;
    delta += len;
    textEdit.addChild(new DeleteEdit(offset, len));
  }

  private MultiTextEdit updateUneditedPositions() throws BadLocationException {
    if (!hasNamed) {
      // Preserve unedited optional arguments that appear to the left of an edited optional.
      boolean required = false;
      for (int i = positions.size() - 1; i >= 0; i--) {
        OptionalArgumentPosition pos = positions.get(i);
        if (pos.isModified()) {
          required = true;
        } else {
          pos.setIsRequired(required);
        }
      }
    }
    MultiTextEdit textEdit = new MultiTextEdit();
    for (OptionalArgumentPosition pos : positions) {
      if (!pos.isModified()) {
        // Remove unedited argument.
        deleteText(pos, textEdit, false);
      }
    }
    for (int i = positions.size() - 1; i >= 0; i--) {
      OptionalArgumentPosition pos = positions.get(i);
      if (pos.isModified()) {
        // Remove comma after last argument, if any.
        deleteText(pos, textEdit, true);
        break;
      }
    }
    return textEdit;
  }

}
