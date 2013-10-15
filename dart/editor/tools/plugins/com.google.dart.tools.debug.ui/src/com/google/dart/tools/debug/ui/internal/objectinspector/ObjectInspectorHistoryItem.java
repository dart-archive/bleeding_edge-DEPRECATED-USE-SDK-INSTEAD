/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.debug.ui.internal.objectinspector;

import org.eclipse.debug.core.model.IValue;
import org.eclipse.jface.viewers.ISelection;

/**
 * An object to track the object inspector navigation history.
 */
public class ObjectInspectorHistoryItem {
  private IValue value;
  private String text = "";
  private ISelection selection;
  private int topIndex;

  ObjectInspectorHistoryItem(IValue value) {
    this.value = value;
  }

  public ISelection getSelection() {
    return selection;
  }

  public String getText() {
    return text;
  }

  public int getTopIndex() {
    return topIndex;
  }

  public IValue getValue() {
    return value;
  }

  public void setSelection(ISelection selection) {
    this.selection = selection;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setTopIndex(int topIndex) {
    this.topIndex = topIndex;
  }
}
