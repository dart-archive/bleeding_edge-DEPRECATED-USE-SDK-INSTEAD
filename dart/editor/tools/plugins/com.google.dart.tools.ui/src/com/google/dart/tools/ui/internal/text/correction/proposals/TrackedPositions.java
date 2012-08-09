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
package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.dart.compiler.common.HasSourceInfo;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.tools.core.dom.rewrite.TrackedNodePosition;

/**
 * Factory for {@link TrackedNodePosition}.
 */
public class TrackedPositions {
  public static TrackedNodePosition forNode(HasSourceInfo node) {
    SourceInfo sourceInfo = node.getSourceInfo();
    return forStartLength(sourceInfo.getOffset(), sourceInfo.getLength());
  }

  public static TrackedNodePosition forStartEnd(int start, int end) {
    return forStartLength(start, end - start);
  }

  public static TrackedNodePosition forStartLength(final int start, final int length) {
    return new TrackedNodePosition() {

      @Override
      public int getLength() {
        return length;
      }

      @Override
      public int getStartPosition() {
        return start;
      }

      @Override
      public String toString() {
        return "Pos(" + start + "," + length + ")";
      }
    };
  }
}
