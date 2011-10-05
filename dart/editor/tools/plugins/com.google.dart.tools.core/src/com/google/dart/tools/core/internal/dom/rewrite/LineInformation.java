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
package com.google.dart.tools.core.internal.dom.rewrite;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Instances of the class <code>LineInformation</code>
 */
public abstract class LineInformation {
  public static LineInformation create(final DartUnit astRoot) {
    return new LineInformation() {
      @Override
      public int getLineOffset(int line) {
        DartCore.notYetImplemented();
        return 0;
        // return astRoot.getPosition(line + 1, 0);
      }

      @Override
      public int getLineOfOffset(int offset) {
        DartCore.notYetImplemented();
        return 0;
        // return astRoot.getLineNumber(offset) - 1;
      }
    };
  }

  public static LineInformation create(final IDocument doc) {
    return new LineInformation() {
      @Override
      public int getLineOffset(int line) {
        try {
          return doc.getLineOffset(line);
        } catch (BadLocationException e) {
          return -1;
        }
      }

      @Override
      public int getLineOfOffset(int offset) {
        try {
          return doc.getLineOfOffset(offset);
        } catch (BadLocationException e) {
          return -1;
        }
      }
    };
  }

  public abstract int getLineOffset(int line);

  public abstract int getLineOfOffset(int offset);
}
