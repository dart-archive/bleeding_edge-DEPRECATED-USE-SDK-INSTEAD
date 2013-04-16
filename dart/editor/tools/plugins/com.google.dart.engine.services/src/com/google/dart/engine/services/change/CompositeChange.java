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

import java.util.Collections;
import java.util.List;

/**
 * Composition of several {@link Change}s.
 */
public class CompositeChange extends Change {
  private final List<Change> children = Lists.newArrayList();

  public CompositeChange(String name) {
    super(name);
  }

  public CompositeChange(String name, Change... changes) {
    super(name);
    add(changes);
  }

  /**
   * Adds given {@link Change}s.
   */
  public void add(Change... changes) {
    Collections.addAll(children, changes);
  }

  /**
   * @return the children {@link Change}s.
   */
  public List<Change> getChildren() {
    return children;
  }
}
