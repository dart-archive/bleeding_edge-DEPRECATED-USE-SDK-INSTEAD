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
package com.google.dart.tools.ui.internal.text.editor.saveactions;

import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import java.lang.reflect.InvocationTargetException;

/**
 * An action to remove trailing whitespace from a document.
 */
public class RemoveTrailingWhitespaceAction {
  /**
   * Applying big {@link TextEdit} may be pretty expensive if document itself is also big. Currently
   * in Dart this causes running "FastDocumentPartitionScanner" on each change and it looks like
   * Editor is hanging.
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=10443
   */
  private static void applyTextEdit(IDocument document, TextEdit textEdit) throws Exception {
    if (document instanceof IDocumentExtension4) {
      IDocumentExtension4 extension4 = (IDocumentExtension4) document;
      DocumentRewriteSession session = extension4.startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED);
      textEdit.apply(document);
      extension4.stopRewriteSession(session);
    } else {
      textEdit.apply(document);
    }
  }

  private final ISourceViewer viewer;

  /**
   * Create an instance.
   * 
   * @param viewer the viewer whose backing document will be modified
   */
  public RemoveTrailingWhitespaceAction(ISourceViewer viewer) {
    this.viewer = viewer;
  }

  public void run() throws InvocationTargetException {

    IDocument document = viewer.getDocument();
    if (document == null) {
      return;
    }

    try {
      MultiTextEdit edit = CodeFormatEditFactory.removeTrailingWhitespace(document);
      if (edit.hasChildren()) {
        applyTextEdit(document, edit);
        // using rewrite session causes horizontal scroll bar reset, so we need to show selection
        // https://code.google.com/p/dart/issues/detail?id=11769
        viewer.getTextWidget().showSelection();
      }
    } catch (Throwable e) {
      throw new InvocationTargetException(e);
    }
  }
}
