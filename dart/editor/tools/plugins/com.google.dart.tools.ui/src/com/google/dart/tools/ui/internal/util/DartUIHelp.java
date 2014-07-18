/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.ui.IWorkbenchPart;

public class DartUIHelp {

  private static class JavaUIHelpContextProvider implements IContextProvider {
    private String fId;
    private Object[] fSelected;

    public JavaUIHelpContextProvider(String id, Object[] selected) {
      fId = id;
      fSelected = selected;
    }

    @Override
    public IContext getContext(Object target) {
      IContext context = HelpSystem.getContext(fId);
      if (fSelected != null && fSelected.length > 0) {
        try {
          context = new DartDocHelpContext(context, fSelected);
        } catch (DartModelException e) {
          // since we are updating the UI with async exec it
          // can happen that the element doesn't exist anymore
          // but we are still showing it in the user interface
          if (!e.isDoesNotExist()) {
            DartToolsPlugin.log(e);
          }
        }
      }
      return context;
    }

    @Override
    public int getContextChangeMask() {
      return SELECTION;
    }

    @Override
    public String getSearchExpression(Object target) {
      return null;
    }
  }

  private static class JavaUIHelpListener implements HelpListener {

    private StructuredViewer fViewer;
    private String fContextId;
    private DartEditor fEditor;

    public JavaUIHelpListener(DartEditor editor, String contextId) {
      fContextId = contextId;
      fEditor = editor;
    }

    public JavaUIHelpListener(StructuredViewer viewer, String contextId) {
      fViewer = viewer;
      fContextId = contextId;
    }

    /*
     * @see HelpListener#helpRequested(HelpEvent)
     */
    @Override
    public void helpRequested(HelpEvent e) {
      try {
        Object[] selected = null;
        if (fViewer != null) {
          ISelection selection = fViewer.getSelection();
          if (selection instanceof IStructuredSelection) {
            selected = ((IStructuredSelection) selection).toArray();
          }
        }
        DartDocHelpContext.displayHelp(fContextId, selected);
      } catch (CoreException x) {
        DartToolsPlugin.log(x);
      }
    }
  }

  /**
   * Creates and returns a help context provider for the given part.
   * 
   * @param part the part for which to create the help context provider
   * @param contextId the optional context ID used to retrieve static help
   * @return the help context provider
   */
  public static IContextProvider getHelpContextProvider(IWorkbenchPart part, String contextId) {
    IStructuredSelection selection;
    try {
      selection = SelectionConverter.getStructuredSelection(part);
    } catch (DartModelException ex) {
      DartToolsPlugin.log(ex);
      selection = StructuredSelection.EMPTY;
    }
    Object[] elements = selection.toArray();
    return new JavaUIHelpContextProvider(contextId, elements);
  }

  public static void setHelp(DartEditor editor, StyledText text, String contextId) {
    JavaUIHelpListener listener = new JavaUIHelpListener(editor, contextId);
    text.addHelpListener(listener);
  }

  public static void setHelp(StructuredViewer viewer, String contextId) {
    JavaUIHelpListener listener = new JavaUIHelpListener(viewer, contextId);
    viewer.getControl().addHelpListener(listener);
  }
}
