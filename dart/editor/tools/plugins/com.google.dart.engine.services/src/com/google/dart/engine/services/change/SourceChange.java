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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.source.Source;

import java.util.List;
import java.util.Map;

/**
 * {@link Change} to apply to single {@link Source}.
 */
public class SourceChange extends Change {
  private final Source source;
  private final List<Edit> edits = Lists.newArrayList();
  private final Map<String, List<Edit>> editGroups = Maps.newHashMap();

  /**
   * @param name the name of this change to display in UI
   * @param source the {@link Source} to change
   */
  public SourceChange(String name, Source source) {
    super(name);
    this.source = source;
  }

  /**
   * Adds the {@link Edit} to apply.
   */
  public void addEdit(Edit edit) {
    addEdit(null, edit);
  }

  /**
   * Adds the {@link Edit} to apply.
   */
  public void addEdit(String description, Edit edit) {
    Preconditions.checkNotNull(edit);
    // add to all edits
    edits.add(edit);
    // add to group
    {
      List<Edit> group = editGroups.get(description);
      if (group == null) {
        group = Lists.newArrayList();
        editGroups.put(description, group);
      }
      group.add(edit);
    }
  }

  /**
   * @return the {@link Edit}s grouped by their descriptions.
   */
  public Map<String, List<Edit>> getEditGroups() {
    return editGroups;
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
