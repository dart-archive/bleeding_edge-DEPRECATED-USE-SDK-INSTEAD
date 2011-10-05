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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Instances of the class <code>SourceReferenceInfo</code> implement element info for
 * SourceReference elements.
 */
public class SourceReferenceInfo extends DartElementInfo {
  private int sourceRangeStart;

  private int sourceRangeEnd;

  public int getDeclarationSourceEnd() {
    return sourceRangeEnd;
  }

  public int getDeclarationSourceStart() {
    return sourceRangeStart;
  }

  public SourceRange getSourceRange() {
    return new SourceRangeImpl(sourceRangeStart, sourceRangeEnd - sourceRangeStart + 1);
  }

  public void setSourceRangeEnd(int end) {
    sourceRangeEnd = end;
  }

  public void setSourceRangeStart(int start) {
    sourceRangeStart = start;
  }
}
