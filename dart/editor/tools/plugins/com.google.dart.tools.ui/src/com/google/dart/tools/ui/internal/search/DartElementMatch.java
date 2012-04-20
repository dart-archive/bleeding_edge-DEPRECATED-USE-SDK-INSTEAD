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
package com.google.dart.tools.ui.internal.search;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.search.ui.text.Match;

/**
 * A search match with additional dart-specific info.
 */
public class DartElementMatch extends Match {

  private final int accuracy;
  private final boolean isDartdoc;
  private final boolean isReadAccess;
  private final boolean isSuperInvocation;
  private final boolean isWriteAccess;
  private final int matchRule;

  //TODO (pquitslund): revisit matchRule and accuracy args
  DartElementMatch(DartElement element, int matchRule, int offset, int length, int accuracy,
      boolean isReadAccess, boolean isWriteAccess, boolean isDartdoc, boolean isSuperInvocation) {
    super(element, offset, length);
    this.accuracy = accuracy;
    this.matchRule = matchRule;
    this.isWriteAccess = isWriteAccess;
    this.isReadAccess = isReadAccess;
    this.isDartdoc = isDartdoc;
    this.isSuperInvocation = isSuperInvocation;
  }

  public int getAccuracy() {
    return accuracy;
  }

  public int getMatchRule() {
    return matchRule;
  }

  public boolean isDartDoc() {
    return isDartdoc;
  }

  public boolean isReadAccess() {
    return isReadAccess;
  }

  public boolean isSuperInvocation() {
    return isSuperInvocation;
  }

  public boolean isWriteAccess() {
    return isWriteAccess;
  }

}
