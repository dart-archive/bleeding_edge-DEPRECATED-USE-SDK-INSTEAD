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
package com.google.dart.tools.ui.internal.text.functions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class DartWordFinder {

  public static IRegion findWord(IDocument document, int offset) {

    int start = -2;
    int end = -1;

    try {
      int pos = offset;
      char c;

      while (pos >= 0) {
        c = document.getChar(pos);
        if (!Character.isJavaIdentifierPart(c)) {
          break;
        }
        --pos;
      }
      start = pos;

      pos = offset;
      int length = document.getLength();

      while (pos < length) {
        c = document.getChar(pos);
        if (!Character.isJavaIdentifierPart(c)) {
          break;
        }
        ++pos;
      }
      end = pos;

    } catch (BadLocationException x) {
    }

    if (start >= -1 && end > -1) {
      if (start == offset && end == offset) {
        return new Region(offset, 0);
      } else if (start == offset) {
        return new Region(start, end - start);
      } else {
        return new Region(start + 1, end - start - 1);
      }
    }

    return null;
  }
}
