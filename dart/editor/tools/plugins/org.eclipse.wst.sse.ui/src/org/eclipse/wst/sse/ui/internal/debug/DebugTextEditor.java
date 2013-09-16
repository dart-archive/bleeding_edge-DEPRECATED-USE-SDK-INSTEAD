/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.debug;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.editors.text.StorageDocumentProvider;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.MarkerRulerAction;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.internal.ExtendedConfigurationBuilder;
import org.eclipse.wst.sse.ui.internal.ExtendedEditorActionBuilder;
import org.eclipse.wst.sse.ui.internal.IExtendedContributor;
import org.eclipse.wst.sse.ui.internal.IPopupMenuContributor;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.StructuredResourceMarkerAnnotationModel;
import org.eclipse.wst.sse.ui.internal.actions.ActionDefinitionIds;
import org.eclipse.wst.sse.ui.internal.extension.BreakpointProviderBuilder;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ConfigurationPointCalculator;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.breakpoint.IExtendedStorageEditorInput;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nitin A text editor capable of using the IBreakpointProvider framework. This class is
 *         intended to be used by clients who wish to use the IBreakpointProvider framework but not
 *         the StructuredTextEditor, e.g. VBS source editors. It is provided AS-IS and marked
 *         internal as it is unsupported and subject to change at any time.
 */
public class DebugTextEditor extends TextEditor {

  private class ShowInTargetLister implements IShowInTargetList {
    public String[] getShowInTargetIds() {
      return fShowInTargetIds;
    }
  }

  /**
   * DocumentProvider for IStorageEditorInputs - supports IExtendedStorageEditorInput notifications
   * and assigning breakpoint markers.
   */
  class StorageInputDocumentProvider extends StorageDocumentProvider implements
      IElementStateListener {
    protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
      IAnnotationModel model = null;
      IStorageEditorInput storageInput = (IStorageEditorInput) element;
      String ext = BreakpointRulerAction.getFileExtension(storageInput);
      IContentType[] types = getEditorInputContentTypes(storageInput);
      IResource res = null;
      for (int i = 0; res == null && i < types.length; i++) {
        res = BreakpointProviderBuilder.getInstance().getResource(storageInput, types[i].getId(),
            ext);
      }
      String id = storageInput.getName();
      if (storageInput.getStorage() != null) {
        IPath fullPath = storageInput.getStorage().getFullPath();
        if (fullPath != null)
          id = fullPath.toString();
        else
          id = storageInput.getName();
      }
      if (res != null)
        model = new StructuredResourceMarkerAnnotationModel(res, id);
      else
        model = new StructuredResourceMarkerAnnotationModel(
            ResourcesPlugin.getWorkspace().getRoot(), id);

      return model;
    }

    protected AbstractDocumentProvider.ElementInfo createElementInfo(Object element)
        throws CoreException {
      if (element instanceof IExtendedStorageEditorInput) {
        ((IExtendedStorageEditorInput) element).addElementStateListener(this);
      }
      return super.createElementInfo(element);
    }

    protected void disposeElementInfo(Object element, ElementInfo info) {
      if (element instanceof IExtendedStorageEditorInput) {
        ((IExtendedStorageEditorInput) element).removeElementStateListener(this);
      }
      super.disposeElementInfo(element, info);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.editors.text.StorageDocumentProvider#doSaveDocument(org.eclipse.core.runtime
     * .IProgressMonitor, java.lang.Object, org.eclipse.jface.text.IDocument, boolean)
     */
    protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document,
        boolean overwrite) throws CoreException {
      // untested
      new FileDocumentProvider().saveDocument(monitor, element, document, overwrite);
    }

    public void elementContentAboutToBeReplaced(Object element) {
      fireElementContentAboutToBeReplaced(element);
    }

    public void elementContentReplaced(Object element) {
      fireElementContentReplaced(element);
    }

    public void elementDeleted(Object element) {
      fireElementDeleted(element);
    }

    public void elementDirtyStateChanged(Object element, boolean isDirty) {
      fireElementDirtyStateChanged(element, isDirty);
    }

    public void elementMoved(Object originalElement, Object movedElement) {
      fireElementMoved(originalElement, movedElement);
    }
  }

  String[] fShowInTargetIds = new String[] {IPageLayout.ID_RES_NAV};
  private IShowInTargetList fShowInTargetListAdapter = new ShowInTargetLister();

  IDocumentProvider fStorageInputDocumentProvider = null;

  public DebugTextEditor() {
    super();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.editors.text.TextEditor#createActions()
   */
  protected void createActions() {
    super.createActions();

    // StructuredTextEditor Action - toggle breakpoints
    IAction action = new ToggleBreakpointAction(this, getVerticalRuler()) {
      protected String getContentType(IDocument document) {
        ILocationProvider provider = (ILocationProvider) getEditorInput().getAdapter(
            ILocationProvider.class);
        if (provider != null) {
          IPath location = provider.getPath(getEditorInput());
          return detectContentType(location).getId();
        } else if (getEditorInput() instanceof IPathEditorInput) {
          IPath location = ((IPathEditorInput) getEditorInput()).getPath();
          return detectContentType(location).getId();
        }
        return IContentTypeManager.CT_TEXT;
      }
    };
    setAction(ActionDefinitionIds.TOGGLE_BREAKPOINTS, action);
    // StructuredTextEditor Action - manage breakpoints
    action = new ManageBreakpointAction(this, getVerticalRuler());
    setAction(ActionDefinitionIds.MANAGE_BREAKPOINTS, action);
    // StructuredTextEditor Action - edit breakpoints
    action = new EditBreakpointAction(this, getVerticalRuler());
    setAction(ActionDefinitionIds.EDIT_BREAKPOINTS, action);
  }

  /**
   * Loads the Show In Target IDs from the Extended Configuration extension point.
   * 
   * @return
   */
  protected String[] createShowInTargetIds() {
    List allIds = new ArrayList(0);
    ExtendedConfigurationBuilder builder = ExtendedConfigurationBuilder.getInstance();
    String[] configurationIds = getConfigurationPoints();
    for (int i = 0; i < configurationIds.length; i++) {
      String[] definitions = builder.getDefinitions("showintarget", configurationIds[i]); //$NON-NLS-1$
      for (int j = 0; j < definitions.length; j++) {
        String someIds = definitions[j];
        if (someIds != null && someIds.length() > 0) {
          String[] ids = StringUtils.unpack(someIds);
          for (int k = 0; k < ids.length; k++) {
            // trim, just to keep things clean
            String id = ids[k].trim();
            if (!allIds.contains(id)) {
              allIds.add(id);
            }
          }
        }
      }
    }

    if (!allIds.contains(IPageLayout.ID_RES_NAV)) {
      allIds.add(IPageLayout.ID_RES_NAV);
    }
    return (String[]) allIds.toArray(new String[0]);
  }

  IContentType detectContentType(IPath location) {
    IContentType type = null;

    IResource resource = FileBuffers.getWorkspaceFileAtLocation(location);
    if (resource != null) {
      if (resource.getType() == IResource.FILE && resource.isAccessible()) {
        IContentDescription d = null;
        try {
          // Optimized description lookup, might not succeed
          d = ((IFile) resource).getContentDescription();
          if (d != null) {
            type = d.getContentType();
          }
        } catch (CoreException e) {
          // Should not be possible given the accessible and file
          // type check above
        }
        if (type == null) {
          type = Platform.getContentTypeManager().findContentTypeFor(resource.getName());
        }
      }
    } else {
      File file = FileBuffers.getSystemFileAtLocation(location);
      if (file != null) {
        InputStream input = null;
        try {
          input = new FileInputStream(file);
          type = Platform.getContentTypeManager().findContentTypeFor(input, location.toOSString());
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
          if (input != null) {
            try {
              input.close();
            } catch (IOException e1) {
            }
          }
        }
        if (type == null) {
          type = Platform.getContentTypeManager().findContentTypeFor(file.getName());
        }
      }
    }
    if (type == null) {
      type = Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);
    }
    return type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.texteditor.AbstractTextEditor#editorContextMenuAboutToShow(org.eclipse.jface
   * .action.IMenuManager)
   */
  protected void editorContextMenuAboutToShow(IMenuManager menu) {
    super.editorContextMenuAboutToShow(menu);

    IEditorActionBarContributor c = getEditorSite().getActionBarContributor();
    if (c instanceof IPopupMenuContributor) {
      ((IPopupMenuContributor) c).contributeToPopupMenu(menu);
    } else {
      ExtendedEditorActionBuilder builder = new ExtendedEditorActionBuilder();
      IExtendedContributor pmc = builder.readActionExtensions(getConfigurationPoints());
      if (pmc != null) {
        pmc.setActiveEditor(this);
        pmc.contributeToPopupMenu(menu);
      }
    }
  }

  /**
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  public Object getAdapter(Class required) {
    // Navigate action set menu
    if (IShowInTargetList.class.equals(required))
      return fShowInTargetListAdapter;
    return super.getAdapter(required);
  }

  protected String[] getConfigurationPoints() {
    return ConfigurationPointCalculator.getConfigurationPoints(this,
        getInputContentType(getEditorInput()), ConfigurationPointCalculator.SOURCE,
        DebugTextEditor.class);
  }

  IContentType[] getEditorInputContentTypes(IEditorInput input) {
    IContentType[] types = null;
    IResource resource = null;

    if (input.getAdapter(IFile.class) != null) {
      resource = (IFile) input.getAdapter(IFile.class);
    } else if (input.getAdapter(IFile.class) != null) {
      resource = (IResource) input.getAdapter(IResource.class);
    }
    if (resource.getType() == IResource.FILE && resource.isAccessible()) {
      IContentDescription d = null;
      try {
        // optimized description lookup, might not succeed
        d = ((IFile) resource).getContentDescription();
        if (d != null) {
          types = new IContentType[] {d.getContentType()};
        }
      } catch (CoreException e) {
        // should not be possible given the accessible and file type
        // check above
      }
    }
    if (types == null) {
      types = Platform.getContentTypeManager().findContentTypesFor(input.getName());
    }
    return types;
  }

  /**
   * @param editorInput
   * @return
   */
  private String getInputContentType(IEditorInput editorInput) {
    IContentType[] types = getEditorInputContentTypes(editorInput);
    if (types != null) {
      return types[0].getId();
    }
    return null;
  }

  /**
   * @return
   */
  private boolean isDebuggingAvailable() {
    boolean debuggingAvailable = false;
    IContentType[] types = getEditorInputContentTypes(getEditorInput());
    for (int i = 0; !debuggingAvailable && i < types.length; i++) {
      debuggingAvailable = debuggingAvailable
          || BreakpointProviderBuilder.getInstance().isAvailable(types[i].getId(),
              BreakpointRulerAction.getFileExtension(getEditorInput()));
    }
    return debuggingAvailable;
  }

  protected void rulerContextMenuAboutToShow(IMenuManager menu) {
    if (isDebuggingAvailable()) {
      menu.add(getAction(ActionDefinitionIds.TOGGLE_BREAKPOINTS));
      menu.add(getAction(ActionDefinitionIds.MANAGE_BREAKPOINTS));
      menu.add(getAction(ActionDefinitionIds.EDIT_BREAKPOINTS));
      menu.add(new Separator());
    } else {
      Logger.log(Logger.INFO, getClass().getName() + " could not enable debugging actions"); //$NON-NLS-1$
    }
    super.rulerContextMenuAboutToShow(menu);
  }

  /**
   * Ensure that the correct IDocumentProvider is used. For IFile and Files, the default provider
   * with a specified AnnotationModelFactory is used. For StorageEditorInputs, use a custom provider
   * that creates a usable ResourceAnnotationModel
   * 
   * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#setDocumentProvider(org.eclipse.ui.IEditorInput)
   */
  protected void setDocumentProvider(IEditorInput input) {
    if (input instanceof IStorageEditorInput && !(input instanceof IFileEditorInput)) {
      if (fStorageInputDocumentProvider == null) {
        fStorageInputDocumentProvider = new StorageInputDocumentProvider();
      }
      setDocumentProvider(fStorageInputDocumentProvider);
    } else {
      super.setDocumentProvider(input);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#updateContentDependentActions()
   */
  protected void updateContentDependentActions() {
    super.updateContentDependentActions();
    if (isDebuggingAvailable()) {
      setAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK,
          getAction(ActionDefinitionIds.TOGGLE_BREAKPOINTS));
    } else {
      // The Default Text Editor uses editorContribution to perform this
      // mapping, but since it relies on the IEditorSite ID, it can't be
      // relied on for MultiPageEditorParts. Instead, force the action
      // registration manually.
      setAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK,
          new MarkerRulerAction(SSEUIMessages.getResourceBundle(),
              "Editor.ManageBookmarks.", this, getVerticalRuler(), IMarker.BOOKMARK, true)); //$NON-NLS-1$
    }
    fShowInTargetIds = createShowInTargetIds();
  }
}
