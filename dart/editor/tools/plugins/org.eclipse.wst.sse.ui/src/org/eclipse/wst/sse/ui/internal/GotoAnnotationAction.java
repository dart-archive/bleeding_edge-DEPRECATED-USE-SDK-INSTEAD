/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;
import org.eclipse.ui.texteditor.TextEditorAction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Based on org.eclipse.jdt.internal.ui.javaeditor.GotoAnnotationAction and the
 * org.eclipse.jdt.internal.ui.JavaEditor's gotoError() method. Rewritten based on 3.0M7 version to
 * operate generically.
 * 
 * @deprecated - use org.eclipse.ui.texteditor.GotoAnnotationAction
 */
public class GotoAnnotationAction extends TextEditorAction {

  private static final boolean _debug = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.ui/gotoNextAnnotation")); //$NON-NLS-1$  //$NON-NLS-2$

  /**
   * Clears the status line on selection changed.
   */
  protected class StatusLineClearer implements ISelectionChangedListener {
    IStatusLineManager fStatusLineManager = null;

    protected StatusLineClearer(IStatusLineManager mgr) {
      super();
      fStatusLineManager = mgr;
    }

    /*
     * @see
     * ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
     */
    public void selectionChanged(SelectionChangedEvent event) {
      getTextEditor().getSelectionProvider().removeSelectionChangedListener(StatusLineClearer.this);

      fStatusLineManager.setErrorMessage(null, null);
      fStatusLineManager.setMessage(null, null);
    }
  }

  private boolean fForward;
  private String fLabel;

  private String fPrefix;

  /**
   * @param prefix
   * @param editor
   */
  public GotoAnnotationAction(String prefix, boolean forward) {
    super(SSEUIMessages.getResourceBundle(), prefix, null);
    fForward = forward;
    fPrefix = prefix;
    fLabel = SSEUIMessages.getResourceBundle().getString(fPrefix);
  }

  /*
   * This is the default label used for description
   */
  public String getDefaultLabel() {
    return fLabel;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.Action#getDescription()
   */
  public String getDescription() {
    return getDefaultLabel();
  }

  /**
   * Returns the annotation closest to the given range respecting the given direction. If an
   * annotation is found, the annotations current position is copied into the provided annotation
   * position.
   * 
   * @param offset the region offset
   * @param length the region length
   * @param forward <code>true</code> for forwards, <code>false</code> for backward
   * @param annotationPosition the position of the found annotation
   * @return the found annotation
   */
  protected Annotation getNextAnnotation(final int offset, final int length, boolean forward,
      Position annotationPosition) {
    Annotation nextAnnotation = null;
    Position nextAnnotationPosition = null;
    Annotation containingAnnotation = null;
    Position containingAnnotationPosition = null;
    boolean currentAnnotation = false;

    IDocument document = getTextEditor().getDocumentProvider().getDocument(
        getTextEditor().getEditorInput());
    int endOfDocument = document.getLength();
    int distance = Integer.MAX_VALUE;

    IAnnotationModel model = getTextEditor().getDocumentProvider().getAnnotationModel(
        getTextEditor().getEditorInput());
    // external files may not have an annotation model
    if (model != null) {
      Iterator e = model.getAnnotationIterator();
      while (e.hasNext()) {
        Annotation a = (Annotation) e.next();
        if (!isNavigationTarget(a))
          continue;

        Position p = model.getPosition(a);
        if (p == null)
          continue;

        if (forward && p.offset == offset || !forward
            && p.offset + p.getLength() == offset + length) {
          if (containingAnnotation == null
              || (forward && p.length >= containingAnnotationPosition.length || !forward
                  && p.length >= containingAnnotationPosition.length)) {
            containingAnnotation = a;
            containingAnnotationPosition = p;
            currentAnnotation = p.length == length;
          }
        } else {
          int currentDistance = 0;

          if (forward) {
            currentDistance = p.getOffset() - offset;
            if (currentDistance < 0) {
              currentDistance = endOfDocument + currentDistance;
            }

            if (currentDistance < distance || currentDistance == distance
                && p.length < nextAnnotationPosition.length) {
              distance = currentDistance;
              nextAnnotation = a;
              nextAnnotationPosition = p;
            }
          } else {
            currentDistance = offset + length - (p.getOffset() + p.length);
            if (currentDistance < 0)
              currentDistance = endOfDocument + currentDistance;

            if (currentDistance < distance || currentDistance == distance
                && p.length < nextAnnotationPosition.length) {
              distance = currentDistance;
              nextAnnotation = a;
              nextAnnotationPosition = p;
            }
          }
        }
      }
    }
    if (containingAnnotationPosition != null && (!currentAnnotation || nextAnnotation == null)) {
      annotationPosition.setOffset(containingAnnotationPosition.getOffset());
      annotationPosition.setLength(containingAnnotationPosition.getLength());
      return containingAnnotation;
    }
    if (nextAnnotationPosition != null) {
      annotationPosition.setOffset(nextAnnotationPosition.getOffset());
      annotationPosition.setLength(nextAnnotationPosition.getLength());
    }

    return nextAnnotation;
  }

  private IStatusLineManager getStatusLineManager() {
    // The original JavaEditor M7 implementation made use of an adapter,
    // but that approach
    // fails with a MultiPageEditorSite

    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null)
      return null;
    IWorkbenchPage page = window.getActivePage();
    if (page == null)
      return null;
    IEditorPart editor = page.getActiveEditor();
    if (editor == null)
      return null;
    IEditorActionBarContributor contributor = editor.getEditorSite().getActionBarContributor();
    if (contributor instanceof EditorActionBarContributor) {
      return ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
    }
    return null;
  }

  public String getText() {
    return getDefaultLabel();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.Action#getToolTipText()
   */
  public String getToolTipText() {
    return getDefaultLabel();
  }

  /**
   * Jumps to the error next according to the given direction based off JavaEditor#gotoAnnotation()
   * 
   * @param forward is the direction
   */
  public void gotoAnnotation(boolean forward) {
    ITextSelection selection = (ITextSelection) getTextEditor().getSelectionProvider().getSelection();
    Position position = new Position(0, 0);
    if (false /* delayed - see bug 18316 */) {
      getNextAnnotation(selection.getOffset(), selection.getLength(), forward, position);
      getTextEditor().selectAndReveal(position.getOffset(), position.getLength());
    } else /* no delay - see bug 18316 */{
      Annotation annotation = getNextAnnotation(selection.getOffset(), selection.getLength(),
          forward, position);
      IEditorStatusLine editorStatusLine = (IEditorStatusLine) getTextEditor().getAdapter(
          IEditorStatusLine.class);
      if (editorStatusLine != null) {
        editorStatusLine.setMessage(true, null, null);
        editorStatusLine.setMessage(false, null, null);
      } else {
        IStatusLineManager mgr = getStatusLineManager();
        if (mgr != null) {
          mgr.setErrorMessage(null);
          mgr.setMessage(null, null);
        }
      }
      if (annotation != null) {
        updateAnnotationViews(annotation);
        if (_debug) {
          System.out.println("select and reveal " + annotation.getType() + "@" + position.getOffset() + ":" + position.getLength()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        getTextEditor().selectAndReveal(position.getOffset(), position.getLength());
        if (editorStatusLine != null) {
          editorStatusLine.setMessage(true, null, null);
          editorStatusLine.setMessage(false, annotation.getText(), null);
        } else {
          IStatusLineManager mgr = getStatusLineManager();
          if (mgr != null) {
            mgr.setErrorMessage(null);
            mgr.setMessage(null, annotation.getText());
          }
          getTextEditor().getSelectionProvider().addSelectionChangedListener(
              new StatusLineClearer(mgr));
        }
      }
    }
  }

  /**
   * Returns whether the given annotation is configured as a target for the
   * "Go to Next/Previous Annotation" actions
   * 
   * @param annotation the annotation
   * @return <code>true</code> if this is a target, <code>false</code> otherwise
   * @see Eclipse 3.0
   */
  protected boolean isNavigationTarget(Annotation annotation) {
    Preferences preferences = EditorsUI.getPluginPreferences();
    AnnotationPreference preference = EditorsUI.getAnnotationPreferenceLookup().getAnnotationPreference(
        annotation);
    // See bug 41689
    // String key= forward ? preference.getIsGoToNextNavigationTargetKey()
    // : preference.getIsGoToPreviousNavigationTargetKey();
    String key = preference == null ? null : preference.getIsGoToNextNavigationTargetKey();
    return (key != null && preferences.getBoolean(key));
  }

  public void run() {
    gotoAnnotation(fForward);
  }

  public void setEditor(ITextEditor editor) {
    super.setEditor(editor);
    update();
  }

  /**
   * Updates the annotation views that show the given annotation.
   * 
   * @param annotation the annotation
   */
  protected void updateAnnotationViews(Annotation annotation) {
    IMarker marker = null;
    if (annotation instanceof SimpleMarkerAnnotation)
      marker = ((SimpleMarkerAnnotation) annotation).getMarker();

    if (marker != null) {
      try {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
          IWorkbenchPage page = window.getActivePage();
          if (page != null) {
            IViewPart view = null;
            if (marker.isSubtypeOf(IMarker.PROBLEM)) {
              view = page.findView(IPageLayout.ID_PROBLEM_VIEW);
            } else if (marker.isSubtypeOf(IMarker.TASK)) {
              view = page.findView(IPageLayout.ID_TASK_LIST);
            } else if (marker.isSubtypeOf(IMarker.BOOKMARK)) {
              view = page.findView(IPageLayout.ID_BOOKMARKS);
            }
//						else if (marker.isSubtypeOf(IBreakpoint.BREAKPOINT_MARKER)) {
//							view = page.findView(IDebugUIConstants.ID_BREAKPOINT_VIEW);
//						}

            if (view == null) {
              view = page.findView("org.eclipse.ui.views.AllMarkersView");
            }
            // If the view isn't open on this perspective, don't
            // interact with it
            if (view != null) {
              Method method = view.getClass().getMethod(
                  "setSelection", new Class[] {IStructuredSelection.class, boolean.class}); //$NON-NLS-1$
              if (method != null) {
                method.invoke(view, new Object[] {new StructuredSelection(marker), Boolean.TRUE});
                page.bringToTop(view);
              }
            }
          }
        }
      }
      // ignore exceptions, don't update any of the lists, just set
      // statusline
      catch (CoreException x) {
        //
      } catch (NoSuchMethodException x) {
        //
      } catch (IllegalAccessException x) {
        //
      } catch (InvocationTargetException x) {
        //
      }
    }
  }
}
