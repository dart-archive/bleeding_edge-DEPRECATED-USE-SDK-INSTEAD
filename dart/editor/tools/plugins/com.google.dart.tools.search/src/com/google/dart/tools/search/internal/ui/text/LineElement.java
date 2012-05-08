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
package com.google.dart.tools.search.internal.ui.text;

import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.Match;

import java.util.ArrayList;

/**
 * Element representing a line in a file
 */
public class LineElement {

  private final FileResource<?> fParent;

  private final int fLineNumber;
  private final int fLineStartOffset;
  private final String fLineContents;

  public LineElement(FileResource<?> parent, int lineNumber, int lineStartOffset,
      String lineContents) {
    fParent = parent;
    fLineNumber = lineNumber;
    fLineStartOffset = lineStartOffset;
    fLineContents = lineContents;
  }

  public boolean contains(int offset) {
    return fLineStartOffset <= offset && offset < fLineStartOffset + fLineContents.length();
  }

  public String getContents() {
    return fLineContents;
  }

  public int getLength() {
    return fLineContents.length();
  }

  public int getLine() {
    return fLineNumber;
  }

  public FileMatch[] getMatches(AbstractTextSearchResult result) {
    ArrayList<FileMatch> res = new ArrayList<FileMatch>();
    Match[] matches = result.getMatches(fParent);
    for (int i = 0; i < matches.length; i++) {
      FileMatch curr = (FileMatch) matches[i];
      if (curr.getLineElement() == this) {
        res.add(curr);
      }
    }
    return res.toArray(new FileMatch[res.size()]);
  }

  public int getNumberOfMatches(AbstractTextSearchResult result) {
    int count = 0;
    Match[] matches = result.getMatches(fParent);
    for (int i = 0; i < matches.length; i++) {
      FileMatch curr = (FileMatch) matches[i];
      if (curr.getLineElement() == this) {
        count++;
      }
    }
    return count;
  }

  public int getOffset() {
    return fLineStartOffset;
  }

  public FileResource<?> getParent() {
    return fParent;
  }

}
