/*******************************************************************************
 * Copyright (c) 2008, 2012 Standards for Technology in Automotive Retail and others. All rights
 * reserved. This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver - initial API and
 * implementation, bug 212330 IBM Corporation - http://bugs.eclipse.org/373701
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.handlers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.services.IServiceScopes;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLEditorMessages;
import org.eclipse.wst.xml.ui.internal.util.SharedXMLEditorPluginImageHelper;

public class ToggleEditModeHandler extends AbstractHandler implements IElementUpdater {
  protected ImageDescriptor onImage = SharedXMLEditorPluginImageHelper.getImageDescriptor(SharedXMLEditorPluginImageHelper.IMG_ETOOL_CONSTRAINON);
  protected ImageDescriptor offImage = SharedXMLEditorPluginImageHelper.getImageDescriptor(SharedXMLEditorPluginImageHelper.IMG_ETOOL_CONSTRAINOFF);

  public ToggleEditModeHandler() {
    super();
  }

  public Object execute(ExecutionEvent event) throws ExecutionException {
    IEditorPart editor = HandlerUtil.getActiveEditor(event);
    ITextEditor textEditor = null;
    if (editor instanceof ITextEditor)
      textEditor = (ITextEditor) editor;
    else {
      Object o = editor.getAdapter(ITextEditor.class);
      if (o != null)
        textEditor = (ITextEditor) o;
    }
    if (textEditor != null) {
      IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
      IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(
          document);
      if (model != null) {
        ModelQuery modelQuery;
        try {
          modelQuery = ModelQueryUtil.getModelQuery(model);
        } finally {
          model.releaseFromRead();
        }
        if (modelQuery != null) {
          int newState = getNextState(modelQuery.getEditMode());
          modelQuery.setEditMode(newState);

          // Force a Refresh on this command so that the image can
          // be
          // updated.
          ICommandService commandService = (ICommandService) HandlerUtil.getActiveWorkbenchWindow(
              event).getService(ICommandService.class);
          Map filter = new HashMap();
          filter.put(IServiceScopes.WINDOW_SCOPE, HandlerUtil.getActiveWorkbenchWindow(event));
          commandService.refreshElements(event.getCommand().getId(), filter);
        }
      }
    }

    return null;
  }

  public int getNextState(int editMode) {
    int result = -1;
    if (editMode == ModelQuery.EDIT_MODE_CONSTRAINED_STRICT) {
      result = ModelQuery.EDIT_MODE_UNCONSTRAINED;
    } else {
      result = ModelQuery.EDIT_MODE_CONSTRAINED_STRICT;
    }
    return result;
  }

  // Handlers that need to interact with the ui that the command came from
  // need to use implement this method.
  public void updateElement(UIElement element, Map parameters) {
    XMLUIPlugin xmluiPlugin = XMLUIPlugin.getDefault();
    if (xmluiPlugin == null)
      return; // probably shutting down
    IWorkbench workbench = xmluiPlugin.getWorkbench();
    if (workbench == null)
      return;
    IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
    if (activeWorkbenchWindow == null)
      return;
    IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
    if (activePage == null)
      return;

    IEditorPart editor = activePage.getActiveEditor();
    ITextEditor textEditor = null;
    if (editor instanceof ITextEditor)
      textEditor = (ITextEditor) editor;
    else if (editor != null) {
      Object o = editor.getAdapter(ITextEditor.class);
      if (o != null)
        textEditor = (ITextEditor) o;
    }
    if (textEditor != null) {
      IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
      IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(
          document);
      if (model != null) {
        ModelQuery modelQuery;
        try {
          modelQuery = ModelQueryUtil.getModelQuery(model);
        } finally {
          model.releaseFromRead();
        }
        if (modelQuery != null) {
          setAppearanceForEditMode(modelQuery.getEditMode(), element);
        }
      }
    }
  }

  public void setAppearanceForEditMode(int editMode, UIElement element) {
    if (editMode == ModelQuery.EDIT_MODE_CONSTRAINED_STRICT) {
      element.setTooltip(XMLEditorMessages.XMLTableTreeActionBarContributor_3);
      element.setIcon(onImage);
    } else {
      element.setTooltip(XMLEditorMessages.XMLTableTreeActionBarContributor_5);
      element.setIcon(offImage);
    }
  }
}
