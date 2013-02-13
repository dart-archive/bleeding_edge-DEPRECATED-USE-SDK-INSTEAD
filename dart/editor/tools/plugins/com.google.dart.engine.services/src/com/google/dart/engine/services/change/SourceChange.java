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

package com.google.dart.engine.services.change;

import com.google.common.collect.Lists;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.source.Source;

import java.util.List;

/**
 * {@link Change} to apply to single {@link Source}.
 */
public class SourceChange extends Change {
  private final Source source;
  private final List<Edit> edits = Lists.newArrayList();

  public SourceChange(String name, Source source) {
    super(name);
    this.source = source;
  }

  /**
   * Adds the {@link Edit} to apply.
   */
  public void addEdit(Edit edit) {
    edits.add(edit);
  }

  /**
   * @return the {@link Edit}s to apply.
   */
  public List<Edit> getEdits() {
    return edits;
  }

  /**
   * @return the {@link Source} to apply changes to.
   */
  public Source getSource() {
    return source;
  }
}
