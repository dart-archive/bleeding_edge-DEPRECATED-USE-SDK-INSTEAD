/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * Moves the cursor to the end tag if it is in a start tag, and vice versa. Also updates the
 * matching tag annotation in the active editor.
 * 
 * @author nitin
 */
class GoToMatchingTagAction extends TextEditorAction {

  private class UpdateListener implements ISelectionChangedListener {
    public void selectionChanged(SelectionChangedEvent event) {
      updateFor(event.getSelection());
    }
  }

  private static final String ANNOTATION_TYPE = "org.eclipse.wst.xml.ui.matching.tag"; //$NON-NLS-1$
  private ISelectionChangedListener fUpdateListener = null;
  static final boolean DEBUG = false;

  /**
   * @param bundle
   * @param prefix
   * @param editor
   * @param style
   */
  GoToMatchingTagAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
    super(bundle, prefix, editor);
    fUpdateListener = new UpdateListener();
  }

  void removeAnnotation(boolean allMatching) {
    ITextEditor textEditor = getTextEditor();
    if (textEditor == null) {
      if (DEBUG) {
        System.out.println("no editor"); //$NON-NLS-1$
      }
      return;
    }
    IDocumentProvider documentProvider = textEditor.getDocumentProvider();
    if (documentProvider == null) {
      if (DEBUG) {
        System.out.println("no document provider"); //$NON-NLS-1$
      }
      return;
    }
    IAnnotationModel annotationModel = documentProvider.getAnnotationModel(textEditor.getEditorInput());
    if (annotationModel == null) {
      if (DEBUG) {
        System.out.println("no annotation model"); //$NON-NLS-1$
      }
      return;
    }

    Iterator annotationIterator = annotationModel.getAnnotationIterator();
    List oldAnnotations = new ArrayList();
    while (annotationIterator.hasNext()) {
      Annotation annotation = (Annotation) annotationIterator.next();
      if (ANNOTATION_TYPE.equals(annotation.getType())) {
        annotation.markDeleted(true);
        /**
         * Sometimes it is supported, sometime's it is not. Confusing.
         */
        try {
          annotationIterator.remove();
        } catch (UnsupportedOperationException e) {
          oldAnnotations.add(annotation);
        }
        if (DEBUG) {
          System.out.println("removed " + annotation); //$NON-NLS-1$
        }
        if (!allMatching)
          break;
      }
    }
    if (!oldAnnotations.isEmpty()) {
      int size = oldAnnotations.size();
      for (int i = 0; i < size; i++) {
        annotationModel.removeAnnotation((Annotation) oldAnnotations.get(i));
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.Action#runWithEvent(org.eclipse.swt.widgets.Event)
   */
  public void runWithEvent(Event event) {
    super.runWithEvent(event);
    if (getTextEditor() == null)
      return;

    ISelection selection = getTextEditor().getSelectionProvider().getSelection();
    if (!selection.isEmpty() && selection instanceof IStructuredSelection
        && selection instanceof ITextSelection) {
      Object o = ((IStructuredSelection) selection).getFirstElement();
      if (o instanceof IDOMNode) {
        int offset = ((ITextSelection) selection).getOffset();
        IStructuredDocumentRegion matchRegion = null;
        if (((Node) o).getNodeType() == Node.ATTRIBUTE_NODE) {
          o = ((Attr) o).getOwnerElement();
        }

        int targetOffset = -1;
        if (o instanceof IDOMNode) {
          IDOMNode node = (IDOMNode) o;
          IStructuredDocumentRegion startStructuredDocumentRegion = node.getStartStructuredDocumentRegion();
          if (startStructuredDocumentRegion != null
              && startStructuredDocumentRegion.containsOffset(offset)) {
            matchRegion = ((IDOMNode) o).getEndStructuredDocumentRegion();
            if (matchRegion != null)
              targetOffset = matchRegion.getStartOffset() + 2;
          } else {
            IStructuredDocumentRegion endStructuredDocumentRegion = node.getEndStructuredDocumentRegion();
            if (endStructuredDocumentRegion != null
                && endStructuredDocumentRegion.containsOffset(offset)) {
              matchRegion = ((IDOMNode) o).getStartStructuredDocumentRegion();
              if (matchRegion != null)
                targetOffset = matchRegion.getStartOffset() + 1;
            }
          }
        }

        if (targetOffset >= 0) {
          getTextEditor().getSelectionProvider().setSelection(new TextSelection(targetOffset, 0));
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.texteditor.TextEditorAction#setEditor(org.eclipse.ui.texteditor.ITextEditor)
   */
  public void setEditor(ITextEditor editor) {
    ITextEditor textEditor = getTextEditor();
    if (textEditor != null) {
      removeAnnotation(true);

      ISelectionProvider selectionProvider = textEditor.getSelectionProvider();
      if (selectionProvider instanceof IPostSelectionProvider) {
        ((IPostSelectionProvider) selectionProvider).removePostSelectionChangedListener(fUpdateListener);
      }
    }
    super.setEditor(editor);
    if (editor != null) {
      ISelectionProvider selectionProvider = editor.getSelectionProvider();
      if (selectionProvider instanceof IPostSelectionProvider) {
        ((IPostSelectionProvider) selectionProvider).addPostSelectionChangedListener(fUpdateListener);
      }

      updateFor(selectionProvider.getSelection());
    }
  }

  public void update() {
    setEnabled(true);

  }

  void updateFor(ISelection selection) {
    ITextEditor textEditor = getTextEditor();
    if (textEditor == null) {
      if (DEBUG) {
        System.out.println("no editor"); //$NON-NLS-1$
      }
      return;
    }
    IDocumentProvider documentProvider = textEditor.getDocumentProvider();
    if (documentProvider == null) {
      if (DEBUG) {
        System.out.println("no document provider"); //$NON-NLS-1$
      }
      return;
    }
    IAnnotationModel annotationModel = documentProvider.getAnnotationModel(textEditor.getEditorInput());
    if (annotationModel == null || !(annotationModel instanceof IAnnotationModelExtension)) {
      if (DEBUG) {
        System.out.println("no annotation model"); //$NON-NLS-1$
      }
      return;
    }

    List oldAnnotations = new ArrayList(2);
    Iterator annotationIterator = annotationModel.getAnnotationIterator();
    while (annotationIterator.hasNext()) {
      Annotation annotation = (Annotation) annotationIterator.next();
      if (ANNOTATION_TYPE.equals(annotation.getType())) {
        annotation.markDeleted(true);
        if (DEBUG) {
          System.out.println("removing " + annotation); //$NON-NLS-1$
        }
        oldAnnotations.add(annotation);
      }
    }

    Map newAnnotations = new HashMap();
    if (!selection.isEmpty() && selection instanceof IStructuredSelection
        && selection instanceof ITextSelection) {
      Object o = ((IStructuredSelection) selection).getFirstElement();
      if (o instanceof IDOMNode) {
        int offset = ((ITextSelection) selection).getOffset();
        IStructuredDocumentRegion matchRegion = null;
        if (((Node) o).getNodeType() == Node.ATTRIBUTE_NODE) {
          o = ((Attr) o).getOwnerElement();
        }

        Position pStart = null;
        Position pEnd = null;
        String tag = ""; //$NON-NLS-1$
        if (o instanceof IDOMNode) {
          IDOMNode node = (IDOMNode) o;
          IStructuredDocumentRegion startStructuredDocumentRegion = node.getStartStructuredDocumentRegion();
          if (startStructuredDocumentRegion != null
              && startStructuredDocumentRegion.containsOffset(offset)) {
            if (startStructuredDocumentRegion.getNumberOfRegions() > 1) {
              ITextRegion nameRegion = startStructuredDocumentRegion.getRegions().get(1);
              pStart = new Position(startStructuredDocumentRegion.getStartOffset(nameRegion),
                  nameRegion.getTextLength());
              tag = startStructuredDocumentRegion.getText(nameRegion);
            }
            matchRegion = ((IDOMNode) o).getEndStructuredDocumentRegion();
            if (matchRegion != null && matchRegion.getNumberOfRegions() > 1) {
              ITextRegion nameRegion = matchRegion.getRegions().get(1);
              pEnd = new Position(matchRegion.getStartOffset(nameRegion),
                  nameRegion.getTextLength());
            }
          } else {
            IStructuredDocumentRegion endStructuredDocumentRegion = node.getEndStructuredDocumentRegion();
            if (endStructuredDocumentRegion != null
                && endStructuredDocumentRegion.containsOffset(offset)) {
              if (endStructuredDocumentRegion.getNumberOfRegions() > 1) {
                ITextRegion nameRegion = endStructuredDocumentRegion.getRegions().get(1);
                pEnd = new Position(endStructuredDocumentRegion.getStartOffset(nameRegion),
                    nameRegion.getTextLength());
                tag = endStructuredDocumentRegion.getText(nameRegion);
              }
              matchRegion = ((IDOMNode) o).getStartStructuredDocumentRegion();
              if (matchRegion != null && matchRegion.getNumberOfRegions() > 1) {
                ITextRegion nameRegion = matchRegion.getRegions().get(1);
                pStart = new Position(matchRegion.getStartOffset(nameRegion),
                    nameRegion.getTextLength());
              }
            }
          }
        }
        if (pStart != null && pEnd != null) {
          Annotation annotation = new Annotation(ANNOTATION_TYPE, false, NLS.bind(
              XMLUIMessages.gotoMatchingTag_start, tag));
          newAnnotations.put(annotation, pStart);
          if (DEBUG) {
            System.out.println("adding " + annotation); //$NON-NLS-1$
          }
          annotation = new Annotation(ANNOTATION_TYPE, false, NLS.bind(
              XMLUIMessages.gotoMatchingTag_end, tag));
          newAnnotations.put(annotation, pEnd);
          if (DEBUG) {
            System.out.println("adding " + annotation); //$NON-NLS-1$
          }
        }
      }
    }
    ((IAnnotationModelExtension) annotationModel).replaceAnnotations(
        (Annotation[]) oldAnnotations.toArray(new Annotation[oldAnnotations.size()]),
        newAnnotations);
  }
}
