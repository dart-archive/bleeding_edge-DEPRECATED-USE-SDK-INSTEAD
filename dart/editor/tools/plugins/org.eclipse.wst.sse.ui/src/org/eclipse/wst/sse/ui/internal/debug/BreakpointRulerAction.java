/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring Pete Carapetyan/Genuitec
 * - 244835 - Enable/Disable breakpoint action does not refresh its label
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.debug;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.extension.BreakpointProviderBuilder;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.breakpoint.IBreakpointProvider;

import java.util.ArrayList;
import java.util.List;

public abstract class BreakpointRulerAction extends Action implements IUpdate {

  protected class MouseUpdater implements MouseListener {
    public void mouseDoubleClick(MouseEvent e) {
      // do nothing (here)
    }

    public void mouseDown(MouseEvent e) {
      update();
    }

    public void mouseUp(MouseEvent e) {
      // do nothing
    }
  }

  public static final String getFileExtension(IEditorInput input) {
    IPath path = null;
    if (input instanceof IStorageEditorInput) {
      try {
        path = ((IStorageEditorInput) input).getStorage().getFullPath();
      } catch (CoreException e) {
        Logger.logException(e);
      }
    }
    if (path != null) {
      return path.getFileExtension();
    }
    String name = input.getName();
    int index = name.lastIndexOf('.');
    if (index == -1)
      return null;
    if (index == (name.length() - 1))
      return ""; //$NON-NLS-1$
    return name.substring(index + 1);
  }

  public static final IResource getResource(IEditorInput input) {
    IResource resource = null;

    if (input instanceof IFileEditorInput)
      resource = ((IFileEditorInput) input).getFile();
    if (resource == null)
      resource = (IResource) input.getAdapter(IFile.class);
    if (resource == null)
      resource = (IResource) input.getAdapter(IResource.class);

    IEditorPart editorPart = null;
    if (resource == null) {
      IWorkbench workbench = SSEUIPlugin.getDefault().getWorkbench();
      if (workbench != null) {
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window != null) {
          IPartService service = window.getPartService();
          if (service != null) {
            Object part = service.getActivePart();
            if (part != null && part instanceof IEditorPart) {
              editorPart = (IEditorPart) part;
              if (editorPart != null) {
                IStructuredModel model = null;
                ITextEditor textEditor = null;
                try {
                  if (editorPart instanceof ITextEditor) {
                    textEditor = (ITextEditor) editorPart;
                  }
                  if (textEditor == null) {
                    textEditor = (ITextEditor) editorPart.getAdapter(ITextEditor.class);
                  }
                  if (textEditor != null) {
                    IDocument textDocument = textEditor.getDocumentProvider().getDocument(input);
                    model = StructuredModelManager.getModelManager().getExistingModelForRead(
                        textDocument);
                    if (model != null) {
                      resource = BreakpointProviderBuilder.getInstance().getResource(input,
                          model.getContentTypeIdentifier(), getFileExtension(input));
                    }
                  }
                  if (resource == null) {
                    IBreakpointProvider[] providers = BreakpointProviderBuilder.getInstance().getBreakpointProviders(
                        editorPart, null, getFileExtension(input));
                    for (int i = 0; i < providers.length && resource == null; i++) {
                      resource = providers[i].getResource(input);
                    }
                  }
                } catch (Exception e) {
                  Logger.logException(e);
                } finally {
                  if (model != null) {
                    model.releaseFromRead();
                  }
                }
              }

            }
          }
        }

      }
    }
    return resource;
  }

  protected MouseListener fMouseListener = null;

  protected IVerticalRulerInfo fRulerInfo = null;
  protected ITextEditor fTextEditor = null;

  private IMenuListener menuListener;

  public BreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
    super();
    fTextEditor = editor;
    if (rulerInfo != null) {
      fRulerInfo = rulerInfo;
      fMouseListener = new MouseUpdater();
      rulerInfo.getControl().addMouseListener(fMouseListener);
    }
    if (editor instanceof ITextEditorExtension) {
      ITextEditorExtension extension = (ITextEditorExtension) editor;
      menuListener = new IMenuListener() {
        public void menuAboutToShow(IMenuManager manager) {
          update();
        }
      };
      extension.addRulerContextMenuListener(menuListener);
    }
  }

  /**
   * Returns the <code>AbstractMarkerAnnotationModel</code> of the editor's input.
   * 
   * @return the marker annotation model
   */
  protected AbstractMarkerAnnotationModel getAnnotationModel() {
    IDocumentProvider provider = fTextEditor.getDocumentProvider();
    IAnnotationModel model = provider.getAnnotationModel(fTextEditor.getEditorInput());
    if (model instanceof AbstractMarkerAnnotationModel)
      return (AbstractMarkerAnnotationModel) model;
    return null;
  }

  protected IBreakpoint[] getBreakpoints(IMarker[] markers) {
    IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
    List breakpoints = new ArrayList(markers.length);
    for (int i = 0; i < markers.length; i++) {
      IBreakpoint breakpoint = manager.getBreakpoint(markers[i]);
      if (breakpoint != null) {
        breakpoints.add(breakpoint);
      }
    }
    return (IBreakpoint[]) breakpoints.toArray(new IBreakpoint[0]);
  }

  /**
   * Returns the <code>IDocument</code> of the editor's input.
   * 
   * @return the document of the editor's input
   */
  protected IDocument getDocument() {
    IDocumentProvider provider = fTextEditor.getDocumentProvider();
    return provider.getDocument(fTextEditor.getEditorInput());
  }

  /**
   * Returns all breakpoint markers which include the ruler's line of activity.
   * 
   * @return an array of markers which include the ruler's line of activity
   */
  protected IMarker[] getMarkers() {
    List markers = new ArrayList();

    IResource resource = getResource();
    IDocument document = getDocument();
    AbstractMarkerAnnotationModel annotationModel = getAnnotationModel();

    if (resource != null && annotationModel != null && resource.exists()) {
      try {
        IMarker[] allMarkers = resource.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true,
            IResource.DEPTH_ZERO);
        if (allMarkers != null) {
          for (int i = 0; i < allMarkers.length; i++) {
            if (includesRulerLine(annotationModel.getMarkerPosition(allMarkers[i]), document)) {
              markers.add(allMarkers[i]);
            }
          }
        }
      } catch (CoreException x) {
        //
      }
    }

    return (IMarker[]) markers.toArray(new IMarker[0]);
  }

  protected IResource getResource() {
    IEditorInput input = getTextEditor().getEditorInput();
    IResource resource = getResource(input);
    return resource;
  }

  /**
   * @return Returns the rulerInfo.
   */
  public IVerticalRulerInfo getRulerInfo() {
    return fRulerInfo;
  }

  /**
   * @return Returns the textEditor.
   */
  public ITextEditor getTextEditor() {
    return fTextEditor;
  }

  protected boolean hasMarkers() {
    IResource resource = getResource();
    IDocument document = getDocument();
    AbstractMarkerAnnotationModel model = getAnnotationModel();

    if (resource != null && model != null && resource.exists()) {
      try {
        IMarker[] allMarkers = resource.findMarkers(IBreakpoint.LINE_BREAKPOINT_MARKER, true,
            IResource.DEPTH_ZERO);
        if (allMarkers != null) {
          for (int i = 0; i < allMarkers.length; i++) {
            if (includesRulerLine(model.getMarkerPosition(allMarkers[i]), document)) {
              return true;
            }
          }
        }
      } catch (CoreException x) {
        //
      }
    }
    return false;
  }

  /**
   * Checks whether a position includes the ruler's line of activity.
   * 
   * @param position the position to be checked
   * @param document the document the position refers to
   * @return <code>true</code> if the line is included by the given position
   */
  protected boolean includesRulerLine(Position position, IDocument document) {
    if (position != null && fRulerInfo != null) {
      try {
        int markerLine = document.getLineOfOffset(position.getOffset());
        int line = getRulerInfo().getLineOfLastMouseButtonActivity();
        if (line == markerLine)
          return true;
        // commented because of "1GEUOZ9: ITPJUI:ALL - Confusing UI
        // for
        // multiline Bookmarks and Tasks"
        // return (markerLine <= line && line <=
        // document.getLineOfOffset(position.getOffset() +
        // position.getLength()));
      } catch (BadLocationException x) {
        //
      }
    }
    return false;
  }
}
