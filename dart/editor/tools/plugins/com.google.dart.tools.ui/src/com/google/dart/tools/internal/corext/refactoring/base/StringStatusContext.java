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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;

/**
 * {@link StringStatusContext} is the wrapper of the {@link String} to show as a long message.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class StringStatusContext extends RefactoringStatusContext implements IAdaptable {
  private final String title;
  private final String content;

  public StringStatusContext(String title, String content) {
    this.title = title;
    this.content = content;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Object getAdapter(Class adapter) {
    if (adapter == IWorkbenchAdapter.class) {
      return new WorkbenchAdapter() {
        @Override
        public String getLabel(Object object) {
          return title;
        }
      };
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

  public String getTitle() {
    return title;
  }
}
