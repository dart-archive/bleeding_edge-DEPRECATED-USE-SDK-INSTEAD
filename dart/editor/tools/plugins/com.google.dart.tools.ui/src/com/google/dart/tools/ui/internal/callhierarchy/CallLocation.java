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
package com.google.dart.tools.ui.internal.callhierarchy;

import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.OpenableElement;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

public class CallLocation implements IAdaptable {
  public static final int UNKNOWN_LINE_NUMBER = -1;
  private CompilationUnitElement fMember;
  private CompilationUnitElement fCalledMember;
  private int fStart;
  private int fEnd;

  private String fCallText;
  private int fLineNumber;

  public CallLocation(CompilationUnitElement member, CompilationUnitElement calledMember,
      int start, int end, int lineNumber) {
    this.fMember = member;
    this.fCalledMember = calledMember;
    this.fStart = start;
    this.fEnd = end;
    this.fLineNumber = lineNumber;
  }

  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
    if (DartElement.class.isAssignableFrom(adapter)) {
      return getMember();
    }
    return null;
  }

  public CompilationUnitElement getCalledMember() {
    return fCalledMember;
  }

  public String getCallText() {
    initCallTextAndLineNumber();
    return fCallText;
  }

  public int getEnd() {
    return fEnd;
  }

  public int getLineNumber() {
    initCallTextAndLineNumber();
    return fLineNumber;
  }

  public DartElement getMember() {
    return fMember;
  }

  public int getStart() {
    return fStart;
  }

  @Override
  public String toString() {
    return getCallText();
  }

  /**
   * Returns the Buffer for the Member represented by this CallLocation.
   * 
   * @return Buffer for the Member or null if the member doesn't have a buffer.
   */
  private Buffer getBufferForMember() {
    Buffer buffer = null;
    try {
      OpenableElement openable = fMember.getOpenable();
      if (openable != null && fMember.exists()) {
        buffer = openable.getBuffer();
      }
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }
    return buffer;
  }

  private void initCallTextAndLineNumber() {
    if (fCallText != null) {
      return;
    }

    Buffer buffer = getBufferForMember();
    if (buffer == null || buffer.getLength() < fEnd) { // buffer contents out of sync
      fCallText = ""; //$NON-NLS-1$
      fLineNumber = UNKNOWN_LINE_NUMBER;
      return;
    }

    fCallText = buffer.getText(fStart, (fEnd - fStart));

    if (fLineNumber == UNKNOWN_LINE_NUMBER) {
      Document document = new Document(buffer.getContents());
      try {
        fLineNumber = document.getLineOfOffset(fStart) + 1;
      } catch (BadLocationException e) {
        DartToolsPlugin.log(e);
      }
    }
  }
}
