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
package com.google.dart.tools.internal.corext.refactoring.util;

import com.google.common.collect.Maps;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * Manages association between {@link Source} and {@link TextChange} objects.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class TextChangeManager {
  private Map<Source, TextChange> fMap = Maps.newHashMap();

  /**
   * Clears all associations between resources and text changes.
   */
  public void clear() {
    fMap.clear();
  }

  /**
   * @return {@code true} if any text changes are managed for the given {@link Source}.
   */
  public boolean containsChangesIn(Source source) {
    return fMap.containsKey(source);
  }

  /**
   * @return the {@link TextChange} associated with the given {@link Source}, existing or new.
   */
  public TextChange get(Source source) {
    TextChange result = fMap.get(source);
    if (result == null) {
      IFile file = (IFile) DartCore.getProjectManager().getResource(source);
      if (file != null) {
        result = new TextFileChange(source.getShortName(), file);
      } else {
        result = new DocumentChange(source.getShortName(), new Document());
      }
      fMap.put(source, result);
    }
    return result;
  }

  /**
   * @return all {@link TextChange}s managed by this instance.
   */
  public TextChange[] getAllChanges() {
    Set<Source> sourceSet = fMap.keySet();
    Source[] sources = sourceSet.toArray(new Source[sourceSet.size()]);
    // sort by Source name:
    Arrays.sort(sources, new Comparator<Source>() {
      @Override
      public int compare(Source o1, Source o2) {
        String name1 = o1.getShortName();
        String name2 = o2.getShortName();
        return name1.compareTo(name2);
      }
    });

    TextChange[] textChanges = new TextChange[sources.length];
    for (int i = 0; i < sources.length; i++) {
      textChanges[i] = fMap.get(sources[i]);
    }
    return textChanges;
  }

  /**
   * @return all {@link Source}s managed by this instance.
   */
  public Source[] getAllSources() {
    return fMap.keySet().toArray(new Source[fMap.keySet().size()]);
  }

  /**
   * @return {@code true} if there are no actual {@link TextChange}s.
   */
  public boolean isEmpty() {
    return fMap.isEmpty();
  }
}
