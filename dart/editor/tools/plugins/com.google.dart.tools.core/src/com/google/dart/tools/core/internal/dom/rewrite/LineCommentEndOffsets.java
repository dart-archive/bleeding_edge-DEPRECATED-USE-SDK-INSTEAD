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

import com.google.dart.compiler.ast.DartComment;
import com.google.dart.tools.core.formatter.IndentManipulation;
import com.google.dart.tools.core.internal.compiler.Util;

import java.util.Arrays;
import java.util.List;

public class LineCommentEndOffsets {
  private int[] offsets;
  private final List<DartComment> commentList;

  public LineCommentEndOffsets(List<DartComment> commentList) {
    this.commentList = commentList;
    offsets = null; // create on demand
  }

  public boolean isEndOfLineComment(int offset) {
    return offset >= 0 && Arrays.binarySearch(getOffsets(), offset) >= 0;
  }

  public boolean isEndOfLineComment(int offset, char[] content) {
    if (offset < 0
        || (offset < content.length && !IndentManipulation.isLineDelimiterChar(content[offset]))) {
      return false;
    }
    return Arrays.binarySearch(getOffsets(), offset) >= 0;
  }

  public boolean remove(int offset) {
    int[] offsetArray = getOffsets(); // returns the shared array
    int index = Arrays.binarySearch(offsetArray, offset);
    if (index >= 0) {
      if (index > 0) {
        // shift from the beginning and insert -1 (smallest number) at the
        // beginning
        // 1, 2, 3, x, 4, 5 -> -1, 1, 2, 3, 4, 5
        System.arraycopy(offsetArray, 0, offsetArray, 1, index);
      }
      offsetArray[0] = -1;
      return true;
    }
    return false;
  }

  private int[] getOffsets() {
    if (offsets == null) {
      if (commentList != null) {
        int nComments = commentList.size();
        // count the number of line comments
        int count = 0;
        for (int i = 0; i < nComments; i++) {
          DartComment comment = commentList.get(i);
          if (comment.isEndOfLine()) {
            count++;
          }
        }
        // fill the offset table
        offsets = new int[count];
        for (int i = 0, k = 0; i < nComments; i++) {
          DartComment comment = commentList.get(i);
          if (comment.isEndOfLine()) {
            offsets[k++] = comment.getSourceStart() + comment.getSourceLength();
          }
        }
      } else {
        offsets = Util.EMPTY_INT_ARRAY;
      }
    }
    return offsets;
  }
}
