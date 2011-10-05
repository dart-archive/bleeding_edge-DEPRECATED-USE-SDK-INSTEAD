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
package com.google.dart.tools.core.internal.compiler.parser;

/**
 * Instances of the class <code>RecoveryScannerData</code>
 */
public class RecoveryScannerData {
  public int insertedTokensPtr = -1;
  public int[][] insertedTokens;
  public int[] insertedTokensPosition;
  public boolean[] insertedTokenUsed;

  public int replacedTokensPtr = -1;
  public int[][] replacedTokens;
  public int[] replacedTokensStart;
  public int[] replacedTokensEnd;
  public boolean[] replacedTokenUsed;

  public int removedTokensPtr = -1;
  public int[] removedTokensStart;
  public int[] removedTokensEnd;
  public boolean[] removedTokenUsed;

  public RecoveryScannerData removeUnused() {
    if (insertedTokens != null) {
      int newInsertedTokensPtr = -1;
      for (int i = 0; i <= insertedTokensPtr; i++) {
        if (insertedTokenUsed[i]) {
          newInsertedTokensPtr++;
          insertedTokens[newInsertedTokensPtr] = insertedTokens[i];
          insertedTokensPosition[newInsertedTokensPtr] = insertedTokensPosition[i];
          insertedTokenUsed[newInsertedTokensPtr] = insertedTokenUsed[i];
        }
      }
      insertedTokensPtr = newInsertedTokensPtr;
    }

    if (replacedTokens != null) {
      int newReplacedTokensPtr = -1;
      for (int i = 0; i <= replacedTokensPtr; i++) {
        if (replacedTokenUsed[i]) {
          newReplacedTokensPtr++;
          replacedTokens[newReplacedTokensPtr] = replacedTokens[i];
          replacedTokensStart[newReplacedTokensPtr] = replacedTokensStart[i];
          replacedTokensEnd[newReplacedTokensPtr] = replacedTokensEnd[i];
          replacedTokenUsed[newReplacedTokensPtr] = replacedTokenUsed[i];
        }
      }
      replacedTokensPtr = newReplacedTokensPtr;
    }
    if (removedTokensStart != null) {
      int newRemovedTokensPtr = -1;
      for (int i = 0; i <= removedTokensPtr; i++) {
        if (removedTokenUsed[i]) {
          newRemovedTokensPtr++;
          removedTokensStart[newRemovedTokensPtr] = removedTokensStart[i];
          removedTokensEnd[newRemovedTokensPtr] = removedTokensEnd[i];
          removedTokenUsed[newRemovedTokensPtr] = removedTokenUsed[i];
        }
      }
      removedTokensPtr = newRemovedTokensPtr;
    }

    return this;
  }
}
