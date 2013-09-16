/*****************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 ****************************************************************************/
package org.eclipse.wst.xml.ui.internal.tabletree;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.NavigationLocation;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextSelectionNavigationLocation;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

/**
 * {@link NavigationLocation} that is loosely based on {@link TextSelectionNavigation} but operates
 * on the {@link XMLMultiPageEditorPart}'s design page. History is preserved as positions, but the
 * selection set on the viewer is the indexed region
 */
class DesignPageNavigationLocation extends NavigationLocation {

  // Memento tags and values
  private static final String TAG_X = "x"; //$NON-NLS-1$
  private static final String TAG_Y = "y"; //$NON-NLS-1$
  private static final String TAG_INFO = "info"; //$NON-NLS-1$
  private static final String INFO_DELETED = "deleted"; //$NON-NLS-1$
  private static final String INFO_NOT_DELETED = "not_deleted"; //$NON-NLS-1$

  private static final String CATEGORY = "__navigation_" + TextSelectionNavigationLocation.class.hashCode(); //$NON-NLS-1$
  private static final IPositionUpdater fgPositionUpdater = new DefaultPositionUpdater(CATEGORY);

  private Position fPosition;
  private IDocument fDocument;
  private Position fSavedPosition;
  private IDesignViewer fViewer;

  DesignPageNavigationLocation(IEditorPart part, IDesignViewer viewer, boolean initialize) {
    super(part);

    fViewer = viewer;
    if (initialize) {
      final ISelection selection = fViewer.getSelectionProvider().getSelection();

      IEditorPart textPart = getTextEditorPart();
      if (textPart != null) {
        IDocument document = getDocument((ITextEditor) textPart);
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
          Object first = ((IStructuredSelection) selection).getFirstElement();
          if (first instanceof IDOMNode) {
            IDOMNode node = (IDOMNode) first;
            Position position = new Position(node.getStartOffset(), node.getLength());
            if (installOnDocument(document, position)) {
              fDocument = document;
              fPosition = position;
              if (!part.isDirty())
                fSavedPosition = new Position(fPosition.offset, fPosition.length);
            }
          }
        } else { // The editor may not necessarily have a selection when opened
          Position position = new Position(0, 0);
          if (installOnDocument(document, position)) {
            fDocument = document;
            fPosition = position;
            if (!part.isDirty())
              fSavedPosition = new Position(fPosition.offset, fPosition.length);
          }
        }
      }
    }

  }

  /**
   * Returns the {@link ITextEditor} associated with this editor part
   * 
   * @return {@link IEditorPart} that is o
   */
  protected ITextEditor getTextEditorPart() {
    IEditorPart part = super.getEditorPart();
    if (part != null) {
      return (ITextEditor) part.getAdapter(ITextEditor.class);
    }
    return null;
  }

  /**
   * Returns the text editor's document.
   * 
   * @param part the text editor
   * @return the document of the given text editor
   */
  private IDocument getDocument(ITextEditor part) {
    IDocumentProvider provider = part.getDocumentProvider();
    return provider.getDocument(part.getEditorInput());
  }

  /**
   * Installs the given position on the given document.
   * 
   * @param document the document
   * @param position the position
   * @return <code>true</code> if the position could be installed
   */
  private boolean installOnDocument(IDocument document, Position position) {

    if (document != null && position != null) {

      if (!document.containsPositionCategory(CATEGORY)) {
        document.addPositionCategory(CATEGORY);
        document.addPositionUpdater(fgPositionUpdater);
      }

      try {
        document.addPosition(CATEGORY, position);
        return true;
      } catch (BadLocationException e) {
      } catch (BadPositionCategoryException e) {
      }
    }

    return false;
  }

  /**
   * Uninstalls the given position from the given document.
   * 
   * @param document the document
   * @param position the position
   * @return <code>true</code> if the position could be uninstalled
   */
  private boolean uninstallFromDocument(IDocument document, Position position) {

    if (document != null && position != null) {
      try {

        document.removePosition(CATEGORY, position);

        Position[] category = document.getPositions(CATEGORY);
        if (category == null || category.length == 0) {
          document.removePositionCategory(CATEGORY);
          document.removePositionUpdater(fgPositionUpdater);
        }
        return true;

      } catch (BadPositionCategoryException e) {
      }
    }

    return false;
  }

  /*
   * @see Object#toString()
   */
  public String toString() {
    return "Selection<" + fPosition + ">"; //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Tells whether this location is equal to the current location in the given text editor.
   * 
   * @param part the text editor
   * @return <code>true</code> if the locations are equal
   */
  private boolean equalsLocationOf() {

    if (fPosition == null)
      return true;

    if (fPosition.isDeleted)
      return false;

    final ISelection selection = fViewer.getSelectionProvider().getSelection();
    if (selection instanceof IStructuredSelection) {
      final Object first = ((IStructuredSelection) selection).getFirstElement();
      if (first instanceof IDOMNode) {
        final IDOMNode node = (IDOMNode) first;
        return fPosition.offset == node.getStartOffset() && fPosition.length == node.getLength();
      }
    }

    return false;
  }

  public void dispose() {
    uninstallFromDocument(fDocument, fPosition);
    fDocument = null;
    fPosition = null;
    fSavedPosition = null;
    super.dispose();
  }

  /**
   * Releases the state of this location.
   */
  public void releaseState() {
    // deactivate
    uninstallFromDocument(fDocument, fPosition);
    fDocument = null;
    fPosition = null;
    fSavedPosition = null;
    super.releaseState();
  }

  /**
   * Merges the given location into this one.
   * 
   * @param location the location to merge into this one
   * @return <code>true<code> if merging was successful
   */
  public boolean mergeInto(INavigationLocation location) {

    if (location == null)
      return false;

    if (getClass() != location.getClass())
      return false;

    if (fPosition == null || fPosition.isDeleted)
      return true;

    DesignPageNavigationLocation s = (DesignPageNavigationLocation) location;
    if (s.fPosition == null || s.fPosition.isDeleted) {
      uninstallFromDocument(fDocument, fPosition);
      s.fDocument = fDocument;
      s.fPosition = fPosition;
      s.fSavedPosition = fSavedPosition;
      return true;
    }

    if (s.fDocument == fDocument) {
      if (s.fPosition.overlapsWith(fPosition.offset, fPosition.length)
          || fPosition.offset + fPosition.length == s.fPosition.offset
          || s.fPosition.offset + s.fPosition.length == fPosition.offset) {
        s.fPosition.offset = fPosition.offset;
        s.fPosition.length = fPosition.length;
        return true;
      }
    }

    return false;
  }

  /**
   * Restores this location.
   */
  public void restoreLocation() {
    if (fPosition == null || fPosition.isDeleted)
      return;

    if (fViewer instanceof Viewer) {
      ((Viewer) fViewer).setSelection(getSelection(), true);
    }
  }

  private ISelection getSelection() {
    ISelection selection = null;
    IStructuredModel model = null;
    final ITextEditor editor = getTextEditorPart();
    if (editor != null) {
      try {
        final IDocument document = getDocument(editor);
        if (document instanceof IStructuredDocument) {
          model = StructuredModelManager.getModelManager().getModelForRead(
              (IStructuredDocument) document);
          if (model != null) {
            final IndexedRegion region = model.getIndexedRegion(fPosition.offset);
            if (region != null) {
              selection = new StructuredSelection(region);
            }
          }
        }
      } finally {
        if (model != null) {
          model.releaseFromRead();
        }
      }
    }
    return selection;
  }

  /**
   * Restores the object state from the given memento.
   * 
   * @param memento the memento
   */
  public void restoreState(IMemento memento) {

    final ITextEditor part = getTextEditorPart();
    if (part != null) {

      // restore
      fDocument = getDocument(part);

      Integer offset = memento.getInteger(TAG_X);
      Integer length = memento.getInteger(TAG_Y);
      String deleted = memento.getString(TAG_INFO);

      if (offset != null && length != null) {
        Position p = new Position(offset.intValue(), length.intValue());
        if (deleted != null)
          p.isDeleted = INFO_DELETED.equals(deleted) ? true : false;

        // activate
        if (installOnDocument(fDocument, p)) {
          fPosition = p;
          if (!part.isDirty())
            fSavedPosition = new Position(fPosition.offset, fPosition.length);
        }
      }
    }
  }

  /**
   * Stores the object state into the given memento.
   * 
   * @param memento the memento
   */
  public void saveState(IMemento memento) {
    if (fSavedPosition != null) {
      memento.putInteger(TAG_X, fSavedPosition.offset);
      memento.putInteger(TAG_Y, fSavedPosition.length);
      memento.putString(TAG_INFO, (fSavedPosition.isDeleted ? INFO_DELETED : INFO_NOT_DELETED));
    }
  }

  /**
   * Hook method which is called when the given editor has been saved.
   * 
   * @param part the editor part
   */
  public void partSaved(IEditorPart part) {
    // http://dev.eclipse.org/bugs/show_bug.cgi?id=25440
    if (fPosition == null || fPosition.isDeleted())
      fSavedPosition = null;
    else
      fSavedPosition = new Position(fPosition.offset, fPosition.length);
  }

  /**
   * Updates the this location.
   */
  public void update() {
    final IEditorPart part = getEditorPart();

    if (equalsLocationOf())
      return;

    final ISelection selection = fViewer.getSelectionProvider().getSelection();
    if (selection == null || selection.isEmpty())
      return;

    if (selection instanceof IStructuredSelection) {
      Object first = ((IStructuredSelection) selection).getFirstElement();
      if (first instanceof IDOMNode) {
        IDOMNode node = (IDOMNode) first;
        fPosition.offset = node.getStartOffset();
        fPosition.length = node.getLength();
        fPosition.isDeleted = false;
        if (!part.isDirty())
          fSavedPosition = new Position(fPosition.offset, fPosition.length);
      }
    }
  }
}
