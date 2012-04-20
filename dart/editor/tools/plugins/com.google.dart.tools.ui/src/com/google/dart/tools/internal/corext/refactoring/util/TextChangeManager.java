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
package com.google.dart.tools.internal.corext.refactoring.util;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;

import org.eclipse.ltk.core.refactoring.TextChange;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A <code>TextChangeManager</code> manages associations between <code>CompilationUnit</code> or
 * <code>IFile</code> and <code>TextChange</code> objects.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class TextChangeManager {

  private Map<CompilationUnit, TextChange> fMap = new HashMap<CompilationUnit, TextChange>(10);

  private final boolean fKeepExecutedTextEdits;

  public TextChangeManager() {
    this(false);
  }

  public TextChangeManager(boolean keepExecutedTextEdits) {
    fKeepExecutedTextEdits = keepExecutedTextEdits;
  }

  /**
   * Clears all associations between resources and text changes.
   */
  public void clear() {
    fMap.clear();
  }

  /**
   * Returns if any text changes are managed for the specified compilation unit.
   * 
   * @param cu the compilation unit
   * @return <code>true</code> if any text changes are managed for the specified compilation unit
   *         and <code>false</code> otherwise
   */
  public boolean containsChangesIn(CompilationUnit cu) {
    return fMap.containsKey(cu);
  }

  /**
   * Returns the <code>TextChange</code> associated with the given compilation unit. If the manager
   * does not already manage an association it creates a one.
   * 
   * @param cu the compilation unit for which the text buffer change is requested
   * @return the text change associated with the given compilation unit.
   */
  public TextChange get(CompilationUnit cu) {
    TextChange result = fMap.get(cu);
    if (result == null) {
      result = new CompilationUnitChange(cu.getElementName(), cu);
      result.setKeepPreviewEdits(fKeepExecutedTextEdits);
      fMap.put(cu, result);
    }
    return result;
  }

  /**
   * Returns all text changes managed by this instance.
   * 
   * @return all text changes managed by this instance
   */
  public TextChange[] getAllChanges() {
    Set<CompilationUnit> cuSet = fMap.keySet();
    CompilationUnit[] cus = cuSet.toArray(new CompilationUnit[cuSet.size()]);
    // sort by cu name:
    Arrays.sort(cus, new Comparator<CompilationUnit>() {
      @Override
      public int compare(CompilationUnit o1, CompilationUnit o2) {
        String name1 = o1.getElementName();
        String name2 = o2.getElementName();
        return name1.compareTo(name2);
      }
    });

    TextChange[] textChanges = new TextChange[cus.length];
    for (int i = 0; i < cus.length; i++) {
      textChanges[i] = fMap.get(cus[i]);
    }
    return textChanges;
  }

  /**
   * Returns all compilation units managed by this instance.
   * 
   * @return all compilation units managed by this instance
   */
  public CompilationUnit[] getAllCompilationUnits() {
    return fMap.keySet().toArray(new CompilationUnit[fMap.keySet().size()]);
  }

  /**
   * Adds an association between the given compilation unit and the passed change to this manager.
   * 
   * @param cu the compilation unit (key)
   * @param change the change associated with the compilation unit
   */
  public void manage(CompilationUnit cu, TextChange change) {
    fMap.put(cu, change);
  }

  /**
   * Removes the <tt>TextChange</tt> managed under the given key <code>unit<code>.
   * 
   * @param unit the key determining the <tt>TextChange</tt> to be removed.
   * @return the removed <tt>TextChange</tt>.
   */
  public TextChange remove(CompilationUnit unit) {
    return fMap.remove(unit);
  }
}
