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

import com.google.common.collect.Maps;
import com.google.dart.engine.source.Source;

import java.util.Collection;
import java.util.Map;

/**
 * Manages multiple {@link SourceChange} objects.
 */
public class SourceChangeManager {
  private final Map<Source, SourceChange> changeMap = Maps.newHashMap();

  /**
   * @return the {@link SourceChange} to record modifications for given {@link Source}.
   */
  public SourceChange get(Source source) {
    SourceChange change = changeMap.get(source);
    if (change == null) {
      change = new SourceChange(source.getShortName(), source);
      changeMap.put(source, change);
    }
    return change;
  }

  /**
   * @return all {@link SourceChange} in this manager.
   */
  public SourceChange[] getChanges() {
    Collection<SourceChange> changes = changeMap.values();
    return changes.toArray(new SourceChange[changes.size()]);
  }
}
