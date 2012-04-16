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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.text.edits.MultiTextEdit;

import java.lang.reflect.InvocationTargetException;

/**
 * An action to remove trailing whitespace from a document.
 */
public class RemoveTrailingWhitespaceAction {

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
      edit.apply(document);
    } catch (Throwable e) {
      throw new InvocationTargetException(e);
    }
  }

}
