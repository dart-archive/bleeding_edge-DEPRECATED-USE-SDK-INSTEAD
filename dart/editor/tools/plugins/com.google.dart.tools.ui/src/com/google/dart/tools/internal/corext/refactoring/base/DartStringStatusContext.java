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
package com.google.dart.tools.internal.corext.refactoring.base;

import com.google.dart.tools.core.model.SourceRange;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

/**
 * A Dart string context can be used to annotate a </code>RefactoringStatusEntry<code>
 * with detailed information about an error detected in Dart source code represented
 * by a string.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class DartStringStatusContext extends RefactoringStatusContext {

  private final String fSource;
  private final SourceRange fSourceRange;

  /**
   * @param source the source code containing the error
   * @param range a source range inside <code>source</code> or <code>null</code> if no special
   *          source range is known.
   */
  public DartStringStatusContext(String source, SourceRange range) {
    Assert.isNotNull(source);
    fSource = source;
    fSourceRange = range;
  }

  @Override
  public Object getCorrespondingElement() {
    return null;
  }

  public String getSource() {
    return fSource;
  }

  public SourceRange getSourceRange() {
    return fSourceRange;
  }
}
