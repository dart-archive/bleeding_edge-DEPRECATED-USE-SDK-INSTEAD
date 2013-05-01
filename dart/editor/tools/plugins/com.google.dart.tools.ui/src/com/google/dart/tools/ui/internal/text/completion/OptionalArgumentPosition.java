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
import org.eclipse.jface.text.link.LinkedPosition;

/**
 * Allows for deletion of named and optional arguments plus allows for editing parameter of named
 * argument without editing name.
 */
class OptionalArgumentPosition extends LinkedPosition {

  private String originalString;
  private boolean isRequired;
  private int deltaOffset;

  public OptionalArgumentPosition(IDocument document, int offset, int length, int sequence) {
    super(document, offset, length, sequence);
    isRequired = false;
    deltaOffset = 0;
    try {
      originalString = document.get(offset, length);
    } catch (BadLocationException ex) {
      // ignore it
    }
  }

  public int getNameLength() {
    return length + deltaOffset;
  }

  public int getNameOffset() {
    return offset - deltaOffset;
  }

  public boolean isModified() {
    if (isRequired) {
      return true;
    }
    try {
      return !originalString.equals(getDocument().get(getNameOffset(), getNameLength()));
    } catch (BadLocationException ex) {
      // ignore it
    }
    return false;
  }

  public void resetNameStart() {
    deltaOffset = length;
    super.setOffset(offset + length);
    super.setLength(0);
  }

  public void setIsRequired(boolean isRequired) {
    this.isRequired = isRequired;
  }
}
