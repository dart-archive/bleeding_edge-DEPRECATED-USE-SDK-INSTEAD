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

package com.google.dart.tools.ui.web.utils;

import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;

/**
 * An abstract implementation for auto-indent strategies.
 */
public abstract class WebEditorAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {

  public WebEditorAutoIndentStrategy() {

  }

  @Override
  public final void customizeDocumentCommand(IDocument d, DocumentCommand c) {
    if (c.length == 0 && c.text != null
        && TextUtilities.endsWith(d.getLegalLineDelimiters(), c.text) != -1) {
      doAutoIndentAfterNewLine(d, c);
    }
  }

  protected abstract void doAutoIndentAfterNewLine(IDocument d, DocumentCommand c);

}
