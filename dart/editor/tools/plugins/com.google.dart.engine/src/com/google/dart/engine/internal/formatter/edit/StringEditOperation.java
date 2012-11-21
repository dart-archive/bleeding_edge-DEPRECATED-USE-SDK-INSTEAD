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
package com.google.dart.engine.internal.formatter.edit;

import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.formatter.edit.EditOperation;

import java.util.List;

/**
 * A simple un-optimized string edit operation implementation.
 */
public class StringEditOperation implements EditOperation<String, String> {

  private final List<Edit> edits;

  /**
   * Create an edit operation for the given sequence of edits.
   * 
   * @param edits the sequence of edits to apply
   */
  public StringEditOperation(List<Edit> edits) {
    this.edits = edits;
  }

  @Override
  public String applyTo(String document) {

    StringBuilder builder = new StringBuilder(document);

    Edit edit;

    for (int i = edits.size() - 1; i >= 0; --i) {
      edit = edits.get(i);
      builder.replace(edit.offset, edit.offset + edit.length, edit.replacement);
    }

    return builder.toString();
  }

}
