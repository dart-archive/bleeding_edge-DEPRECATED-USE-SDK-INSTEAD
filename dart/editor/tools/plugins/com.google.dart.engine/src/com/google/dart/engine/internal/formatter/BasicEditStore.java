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
package com.google.dart.engine.internal.formatter;

import com.google.common.collect.Lists;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.formatter.edit.EditStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic un-optimized edit store suitable for subclassing.
 */
public class BasicEditStore implements EditStore {

  private final ArrayList<Edit> edits = Lists.newArrayList();

  @Override
  public void addEdit(int offset, int length, String replacement) {
    add(new Edit(offset, length, replacement));
  }

  @Override
  public int getCurrentEditIndex() {
    //TODO(pquitslund): verify that this should be "last" vs. "next"
    return edits.size() - 1;
  }

  @Override
  public List<Edit> getEdits() {
    return edits;
  }

  @Override
  public Edit getLastEdit() {
    if (edits.isEmpty()) {
      return null;
    }
    return edits.get(edits.size() - 1);
  }

  @Override
  public void insert(int offset, String insertedString) {
    addEdit(offset, 0, insertedString);
  }

  @Override
  public void reset() {
    edits.clear();
  }

  @Override
  public String toString() {
    return "EditStore(" + edits.toString() + ")";
  }

  /**
   * Add the given edit to the end of the edit sequence.
   * 
   * @param edit the edit to add
   */
  protected void add(Edit edit) {
    edits.add(edit);
  }

}
