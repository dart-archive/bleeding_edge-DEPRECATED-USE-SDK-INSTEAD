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
package com.google.dart.tools.search.internal.core.text;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Adapting a document to a CharSequence
 */
public class DocumentCharSequence implements CharSequence {

  private final IDocument fDocument;

  /**
   * @param document The document to wrap
   */
  public DocumentCharSequence(IDocument document) {
    fDocument = document;
  }

  public int length() {
    return fDocument.getLength();
  }

  public char charAt(int index) {
    try {
      return fDocument.getChar(index);
    } catch (BadLocationException e) {
      throw new IndexOutOfBoundsException();
    }
  }

  public CharSequence subSequence(int start, int end) {
    try {
      return fDocument.get(start, end - start);
    } catch (BadLocationException e) {
      throw new IndexOutOfBoundsException();
    }
  }

}
