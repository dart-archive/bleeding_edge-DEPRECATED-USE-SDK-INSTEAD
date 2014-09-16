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
package com.google.dart.tools.internal.corext.refactoring.base;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.internal.refactoring.WorkbenchSourceAdapter_OLD;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * {@link DartStatusContext_OLD} is the wrapper of the {@link Source} and {@link SourceRange} in it
 * where some problem was detected.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class DartStatusContext_OLD extends RefactoringStatusContext implements IAdaptable {
  /**
   * @return the {@link String} content of the given {@link Source}.
   */
  private static String getSourceContent(final AnalysisContext context, final Source source) {
    final String result[] = {""};
    ExecutionUtils.runIgnore(new RunnableEx() {
      @Override
      public void run() throws Exception {
        result[0] = context.getContents(source).getData().toString();
      }
    });
    return result[0];
  }

  private final Source source;
  private final String content;
  private final SourceRange sourceRange;

  public DartStatusContext_OLD(AnalysisContext context, Source source, SourceRange range) {
    this.source = source;
    Assert.isNotNull(source);
    this.content = getSourceContent(context, source);
    this.sourceRange = range;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Object getAdapter(Class adapter) {
    if (adapter == IWorkbenchAdapter.class) {
      return new WorkbenchSourceAdapter_OLD(source);
    }
    return null;
  }

  public String getContent() {
    return content;
  }

  @Override
  public Object getCorrespondingElement() {
    return null;
  }

  public SourceRange getSourceRange() {
    return sourceRange;
  }
}
