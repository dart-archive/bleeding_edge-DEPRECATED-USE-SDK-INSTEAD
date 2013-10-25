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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.internal.corext.refactoring.base.StringStatusContext;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.ui.refactoring.TextStatusContextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class StringStatusContextViewer extends TextStatusContextViewer {
  @Override
  public void setInput(RefactoringStatusContext context) {
    if (context instanceof StringStatusContext) {
      StringStatusContext sc = (StringStatusContext) context;
      setInput(new Document(sc.getContent()), null);
      updateTitle(sc);
    }
  }

  @Override
  protected SourceViewer createSourceViewer(Composite parent) {
    return new SourceViewer(parent, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI
        | SWT.FULL_SELECTION);
  }
}
