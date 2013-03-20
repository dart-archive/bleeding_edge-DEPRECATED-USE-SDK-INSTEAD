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
package com.google.dart.tools.ui.actions;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.text.editor.LightNodeElement;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;

/**
 * Abstract class selection-based actions.
 */
public abstract class AbstractDartSelectionAction extends InstrumentedSelectionDispatchAction {
  /**
   * @return the {@link Element} covered by the given {@link DartSelection}, may be
   *         <code>null</code>.
   */
  protected static Element getSelectionElement(DartSelection selection) {
    AssistContext context = selection.getContext();
    if (context != null) {
      return context.getCoveredElement();
    }
    return null;
  }

  /**
   * @return the only {@link Element} in the given {@link IStructuredSelection}. May be
   *         <code>null</code>.
   */
  protected static Element getSelectionElement(IStructuredSelection selection) {
    if (selection.size() == 1) {
      Object object = selection.getFirstElement();
      if (object instanceof Element) {
        return (Element) object;
      }
      if (object instanceof LightNodeElement) {
        return ((LightNodeElement) object).getElement();
      }
    }
    return null;
  }

  public AbstractDartSelectionAction(DartEditor editor) {
    super(editor.getEditorSite());
    init();
  }

  public AbstractDartSelectionAction(IWorkbenchSite site) {
    super(site);
    init();
  }

  /**
   * Called once by the constructors to initialize label, tooltip, image. To be overridden by
   * subclasses.
   */
  protected abstract void init();
}
