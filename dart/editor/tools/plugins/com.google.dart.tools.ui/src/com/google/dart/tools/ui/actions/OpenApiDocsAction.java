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

package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * Opens the API reference.
 */
public class OpenApiDocsAction extends AbstractInstrumentedAction implements IWorkbenchAction,
    ISelectionChangedListener {

  public OpenApiDocsAction() {
    setText(ActionMessages.OpenApiDocsAction_text);
    setDescription(ActionMessages.OpenApiDocsAction_description);
    setToolTipText(ActionMessages.OpenApiDocsAction_tooltip);
    setId("com.google.dart.tools.ui.apiref.open"); //$NON-NLS-N$
  }

  @Override
  public void dispose() {
    //nothing to do
  }

  @Override
  public void run() {
    EmitInstrumentationCommand();

    ExternalBrowserUtil.openInExternalBrowser(ActionMessages.OpenApiDocsAction_href);
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {

  }

}
