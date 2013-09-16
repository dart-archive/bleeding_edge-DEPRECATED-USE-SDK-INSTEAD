/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.style;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.sse.ui.internal.preferences.EditorPreferenceNames;
import org.eclipse.wst.sse.ui.internal.provisional.style.StructuredPresentationReconciler;
import org.eclipse.wst.sse.ui.internal.style.SemanticHighlightingManager.HighlightedPosition;
import org.eclipse.wst.sse.ui.internal.style.SemanticHighlightingManager.HighlightingStyle;
import org.eclipse.wst.sse.ui.internal.util.EditorUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Semantic highlighting presenter - UI thread implementation. Based on
 * org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingPresenter
 * 
 * @since 3.1
 */
public class SemanticHighlightingPresenter implements ITextPresentationListener,
    ITextInputListener, IDocumentListener {

  /**
   * Semantic highlighting position updater.
   */
  private class HighlightingPositionUpdater implements IPositionUpdater {

    /** The position category. */
    private final String fCategory;

    /**
     * Creates a new updater for the given <code>category</code>.
     * 
     * @param category the new category.
     */
    public HighlightingPositionUpdater(String category) {
      fCategory = category;
    }

    /*
     * @see org.eclipse.jface.text.IPositionUpdater#update(org.eclipse.jface.text.DocumentEvent)
     */
    public void update(DocumentEvent event) {

      int eventOffset = event.getOffset();
      int eventOldLength = event.getLength();
      int eventEnd = eventOffset + eventOldLength;

      try {
        Position[] positions = event.getDocument().getPositions(fCategory);

        for (int i = 0; i != positions.length; i++) {

          HighlightedPosition position = (HighlightedPosition) positions[i];

          // Also update deleted positions because they get deleted by the background thread and removed/invalidated only in the UI runnable
//					if (position.isDeleted())
//						continue;

          int offset = position.getOffset();
          int length = position.getLength();
          int end = offset + length;

          if (offset > eventEnd)
            updateWithPrecedingEvent(position, event);
          else if (end < eventOffset) {
            // do nothing
            // updateWithSucceedingEvent(position, event);
          } else if (offset <= eventOffset && end >= eventEnd) {
            // Previous region updated to overlap the beginning of this one; just bump the start.
            if (i > 0 && positions[i - 1].offset + positions[i - 1].length > offset)
              updateWithPrecedingEvent(position, event);
            else
              updateWithIncludedEvent(position, event);
          } else if (offset <= eventOffset)
            updateWithOverEndEvent(position, event);
          else if (end >= eventEnd)
            updateWithOverStartEvent(position, event);
          else
            updateWithIncludingEvent(position, event);
        }
      } catch (BadPositionCategoryException e) {
        // ignore and return
      }
    }

    /**
     * Update the given position with the given event. The event precedes the position.
     * 
     * @param position The position
     * @param event The event
     */
    private void updateWithPrecedingEvent(HighlightedPosition position, DocumentEvent event) {
      String newText = event.getText();
      int eventNewLength = newText != null ? newText.length() : 0;
      int deltaLength = eventNewLength - event.getLength();

      position.setOffset(position.getOffset() + deltaLength);
    }

    /**
     * Update the given position with the given event. The event succeeds the position.
     * 
     * @param position The position
     * @param event The event
     */
//		private void updateWithSucceedingEvent(HighlightedPosition position, DocumentEvent event) {
//		}

    /**
     * Update the given position with the given event. The event is included by the position.
     * 
     * @param position The position
     * @param event The event
     */
    private void updateWithIncludedEvent(HighlightedPosition position, DocumentEvent event) {
      int eventOffset = event.getOffset();
      String newText = event.getText();
      if (newText == null)
        newText = ""; //$NON-NLS-1$
      int eventNewLength = newText.length();

      int deltaLength = eventNewLength - event.getLength();

      int offset = position.getOffset();
      int length = position.getLength();
      int end = offset + length;

      int includedLength = 0;
      while (includedLength < eventNewLength
          && !Character.isWhitespace(newText.charAt(includedLength)))
        includedLength++;
      if (includedLength == eventNewLength)
        position.setLength(length + deltaLength);
      else {
        int newLeftLength = eventOffset - offset + includedLength;

        int excludedLength = eventNewLength;
        while (excludedLength > 0 && !Character.isWhitespace(newText.charAt(excludedLength - 1)))
          excludedLength--;
        int newRightOffset = eventOffset + excludedLength;
        int newRightLength = end + deltaLength - newRightOffset;

        if (newRightLength == 0) {
          position.setLength(newLeftLength);
        } else {
          if (newLeftLength == 0) {
            position.update(newRightOffset, newRightLength);
          } else {
            position.setLength(newLeftLength);
            addPositionFromUI(position);
          }
        }
      }
    }

    /**
     * Update the given position with the given event. The event overlaps with the end of the
     * position.
     * 
     * @param position The position
     * @param event The event
     */
    private void updateWithOverEndEvent(HighlightedPosition position, DocumentEvent event) {
      String newText = event.getText();
      if (newText == null)
        newText = ""; //$NON-NLS-1$
      int eventNewLength = newText.length();

      int includedLength = 0;
      while (includedLength < eventNewLength
          && !Character.isWhitespace(newText.charAt(includedLength)))
        includedLength++;
      position.setLength(event.getOffset() - position.getOffset() + includedLength);
    }

    /**
     * Update the given position with the given event. The event overlaps with the start of the
     * position.
     * 
     * @param position The position
     * @param event The event
     */
    private void updateWithOverStartEvent(HighlightedPosition position, DocumentEvent event) {
      int eventOffset = event.getOffset();
      int eventEnd = eventOffset + event.getLength();

      String newText = event.getText();
      if (newText == null)
        newText = ""; //$NON-NLS-1$
      int eventNewLength = newText.length();

      int excludedLength = eventNewLength;
      while (excludedLength > 0 && !Character.isWhitespace(newText.charAt(excludedLength - 1)))
        excludedLength--;
      int deleted = eventEnd - position.getOffset();
      int inserted = eventNewLength - excludedLength;
      position.update(eventOffset + excludedLength, position.getLength() - deleted + inserted);
    }

    /**
     * Update the given position with the given event. The event includes the position.
     * 
     * @param position The position
     * @param event The event
     */
    private void updateWithIncludingEvent(HighlightedPosition position, DocumentEvent event) {
      position.delete();
      position.update(event.getOffset(), 0);
    }
  }

  /** Position updater */
  private IPositionUpdater fPositionUpdater = new HighlightingPositionUpdater(getPositionCategory());

  /** The source viewer this semantic highlighting reconciler is installed on */
  private ISourceViewer fSourceViewer;
  /** The background presentation reconciler */
  private StructuredPresentationReconciler fPresentationReconciler;

  /** UI's current highlighted positions - can contain <code>null</code> elements */
  private List fPositions = new ArrayList();
  /** UI position lock */
  private Object fPositionLock = new Object();

  /** <code>true</code> iff the current reconcile is canceled. */
  private boolean fIsCanceled = false;

  /** Read-only color support */
  private YUV_RGBConverter rgbConverter;
  private Map readOnlyColorTable;
  double readOnlyForegroundScaleFactor = 30;

  public SemanticHighlightingPresenter() {
    // no listener for now since there's no UI to change the value
    IPreferenceStore editorStore = SSEUIPlugin.getDefault().getPreferenceStore();
    readOnlyForegroundScaleFactor = editorStore.getInt(EditorPreferenceNames.READ_ONLY_FOREGROUND_SCALE);
  }

  /**
   * Creates and returns a new highlighted position with the given offset, length and highlighting.
   * <p>
   * NOTE: Also called from background thread.
   * </p>
   * 
   * @param offset The offset
   * @param length The length
   * @param highlighting The highlighting
   * @return The new highlighted position
   */
  public HighlightedPosition createHighlightedPosition(int offset, int length,
      HighlightingStyle highlighting) {
    // TODO: reuse deleted positions
    return new HighlightedPosition(offset, length, highlighting, fPositionUpdater);
  }

  /**
   * Creates and returns a new highlighted position from the given position and highlighting.
   * <p>
   * NOTE: Also called from background thread.
   * </p>
   * 
   * @param position The position
   * @param highlighting The highlighting
   * @return The new highlighted position
   */
  public HighlightedPosition createHighlightedPosition(Position position,
      HighlightingStyle highlighting) {
    // TODO: reuse deleted positions
    return new HighlightedPosition(position, highlighting, fPositionUpdater);
  }

  /**
   * Creates and returns a new highlighted position from the given position and highlighting.
   * <p>
   * NOTE: Also called from background thread.
   * </p>
   * 
   * @param position The position
   * @param highlighting The highlighting
   * @param isReadOnly Is this a read-only position
   * @return The new highlighted position
   */
  public HighlightedPosition createHighlightedPosition(Position position,
      HighlightingStyle highlighting, boolean isReadOnly) {
    // TODO: reuse deleted positions
    return new HighlightedPosition(position, highlighting, fPositionUpdater, isReadOnly);
  }

  /**
   * Adds all current positions to the given list.
   * <p>
   * NOTE: Called from background thread.
   * </p>
   * 
   * @param list The list
   */
  public void addAllPositions(List list) {
    synchronized (fPositionLock) {
      list.addAll(fPositions);
    }
  }

  /**
   * Create a text presentation in the background.
   * <p>
   * NOTE: Called from background thread.
   * </p>
   * 
   * @param addedPositions the added positions
   * @param removedPositions the removed positions
   * @return the text presentation or <code>null</code>, if reconciliation should be canceled
   */
  public TextPresentation createPresentation(List addedPositions, List removedPositions) {
    ISourceViewer sourceViewer = fSourceViewer;
    StructuredPresentationReconciler presentationReconciler = fPresentationReconciler;
    if (sourceViewer == null || presentationReconciler == null)
      return null;

    if (isCanceled())
      return null;

    IDocument document = sourceViewer.getDocument();
    if (document == null)
      return null;

    int minStart = Integer.MAX_VALUE;
    int maxEnd = Integer.MIN_VALUE;
    for (int i = 0, n = removedPositions.size(); i < n; i++) {
      Position position = (Position) removedPositions.get(i);
      int offset = position.getOffset();
      minStart = Math.min(minStart, offset);
      maxEnd = Math.max(maxEnd, offset + position.getLength());
    }
    for (int i = 0, n = addedPositions.size(); i < n; i++) {
      Position position = (Position) addedPositions.get(i);
      int offset = position.getOffset();
      minStart = Math.min(minStart, offset);
      maxEnd = Math.max(maxEnd, offset + position.getLength());
    }

    if (minStart < maxEnd)
      try {
        return presentationReconciler.createRepairDescription(new Region(minStart, maxEnd
            - minStart), document);
      } catch (RuntimeException e) {
        // Assume concurrent modification from UI thread
      }

    return null;
  }

  /**
   * Create a runnable for updating the presentation.
   * <p>
   * NOTE: Called from background thread.
   * </p>
   * 
   * @param textPresentation the text presentation
   * @param addedPositions the added positions
   * @param removedPositions the removed positions
   * @return the runnable or <code>null</code>, if reconciliation should be canceled
   */
  public Runnable createUpdateRunnable(final TextPresentation textPresentation,
      List addedPositions, List removedPositions) {
    if (fSourceViewer == null || textPresentation == null)
      return null;

    // TODO: do clustering of positions and post multiple fast runnables
    final HighlightedPosition[] added = new SemanticHighlightingManager.HighlightedPosition[addedPositions.size()];
    addedPositions.toArray(added);
    final SemanticHighlightingManager.HighlightedPosition[] removed = new SemanticHighlightingManager.HighlightedPosition[removedPositions.size()];
    removedPositions.toArray(removed);

    if (isCanceled())
      return null;

    Runnable runnable = new Runnable() {
      public void run() {
        updatePresentation(textPresentation, added, removed);
      }
    };
    return runnable;
  }

  /**
   * Invalidate the presentation of the positions based on the given added positions and the
   * existing deleted positions. Also unregisters the deleted positions from the document and
   * patches the positions of this presenter.
   * <p>
   * NOTE: Indirectly called from background thread by UI runnable.
   * </p>
   * 
   * @param textPresentation the text presentation or <code>null</code>, if the presentation should
   *          computed in the UI thread
   * @param addedPositions the added positions
   * @param removedPositions the removed positions
   */
  public void updatePresentation(TextPresentation textPresentation,
      HighlightedPosition[] addedPositions, HighlightedPosition[] removedPositions) {
    if (fSourceViewer == null)
      return;

//		checkOrdering("added positions: ", Arrays.asList(addedPositions)); //$NON-NLS-1$
//		checkOrdering("removed positions: ", Arrays.asList(removedPositions)); //$NON-NLS-1$
//		checkOrdering("old positions: ", fPositions); //$NON-NLS-1$

    // TODO: double-check consistency with document.getPositions(...)
    // TODO: reuse removed positions
    if (isCanceled())
      return;

    IDocument document = fSourceViewer.getDocument();
    if (document == null)
      return;

    String positionCategory = getPositionCategory();

    List removedPositionsList = Arrays.asList(removedPositions);

    try {
      synchronized (fPositionLock) {
        List oldPositions = fPositions;
        int newSize = Math.max(fPositions.size() + addedPositions.length - removedPositions.length,
            10);

        /*
         * The following loop is a kind of merge sort: it merges two List<Position>, each sorted by
         * position.offset, into one new list. The first of the two is the previous list of
         * positions (oldPositions), from which any deleted positions get removed on the fly. The
         * second of two is the list of added positions. The result is stored in newPositions.
         */
        List newPositions = new ArrayList(newSize);
        Position position = null;
        Position addedPosition = null;
        for (int i = 0, j = 0, n = oldPositions.size(), m = addedPositions.length; i < n
            || position != null || j < m || addedPosition != null;) {
          // loop variant: i + j < old(i + j)

          // a) find the next non-deleted Position from the old list
          while (position == null && i < n) {
            position = (Position) oldPositions.get(i++);
            if (position.isDeleted() || contain(removedPositionsList, position)) {
              document.removePosition(positionCategory, position);
              position = null;
            }
          }

          // b) find the next Position from the added list
          if (addedPosition == null && j < m) {
            addedPosition = addedPositions[j++];
            document.addPosition(positionCategory, addedPosition);
          }

          // c) merge: add the next of position/addedPosition with the lower offset
          if (position != null) {
            if (addedPosition != null)
              if (position.getOffset() <= addedPosition.getOffset()) {
                newPositions.add(position);
                position = null;
              } else {
                newPositions.add(addedPosition);
                addedPosition = null;
              }
            else {
              newPositions.add(position);
              position = null;
            }
          } else if (addedPosition != null) {
            newPositions.add(addedPosition);
            addedPosition = null;
          }
        }
        fPositions = newPositions;
        Collections.sort(fPositions, new Comparator() {

          public int compare(Object arg0, Object arg1) {
            Position p1 = (Position) arg0;
            Position p2 = (Position) arg1;
            return p1.offset - p2.offset;
          }
        });
      }
    } catch (BadPositionCategoryException e) {
      // Should not happen
      Logger.logException(e);
    } catch (BadLocationException e) {
      // Should not happen
      Logger.logException(e);
    }
//		checkOrdering("new positions: ", fPositions); //$NON-NLS-1$

    if (textPresentation != null)
      fSourceViewer.changeTextPresentation(textPresentation, false);
    else
      fSourceViewer.invalidateTextPresentation();
  }

//	private void checkOrdering(String s, List positions) {
//		Position previous= null;
//		for (int i= 0, n= positions.size(); i < n; i++) {
//			Position current= (Position) positions.get(i);
//			if (previous != null && previous.getOffset() + previous.getLength() > current.getOffset())
//				return;
//		}
//	}

  /**
   * Returns <code>true</code> iff the positions contain the position.
   * 
   * @param positions the positions, must be ordered by offset but may overlap
   * @param position the position
   * @return <code>true</code> iff the positions contain the position
   */
  private boolean contain(List positions, Position position) {
    return indexOf(positions, position) != -1;
  }

  /**
   * Returns index of the position in the positions, <code>-1</code> if not found.
   * 
   * @param positions the positions, must be ordered by offset but may overlap
   * @param position the position
   * @return the index
   */
  private int indexOf(List positions, Position position) {
    int index = computeIndexAtOffset(positions, position.getOffset());
    int size = positions.size();
    while (index < size) {
      if (positions.get(index) == position)
        return index;
      index++;
    }
    return -1;
  }

  /**
   * Insert the given position in <code>fPositions</code>, s.t. the offsets remain in linear order.
   * 
   * @param position The position for insertion
   */
  private void insertPosition(Position position) {
    int i = computeIndexAfterOffset(fPositions, position.getOffset());
    fPositions.add(i, position);
  }

  /**
   * Returns the index of the first position with an offset greater than the given offset.
   * 
   * @param positions the positions, must be ordered by offset and must not overlap
   * @param offset the offset
   * @return the index of the last position with an offset greater than the given offset
   */
  private int computeIndexAfterOffset(List positions, int offset) {
    int i = -1;
    int j = positions.size();
    while (j - i > 1) {
      int k = (i + j) >> 1;
      Position position = (Position) positions.get(k);
      if (position.getOffset() > offset)
        j = k;
      else
        i = k;
    }
    return j;
  }

  /**
   * Returns the index of the first position with an offset equal or greater than the given offset.
   * 
   * @param positions the positions, must be ordered by offset and must not overlap
   * @param offset the offset
   * @return the index of the last position with an offset equal or greater than the given offset
   */
  private int computeIndexAtOffset(List positions, int offset) {
    int i = -1;
    int j = positions.size();
    while (j - i > 1) {
      int k = (i + j) >> 1;
      Position position = (Position) positions.get(k);
      if (position.getOffset() >= offset)
        j = k;
      else
        i = k;
    }
    return j;
  }

  /*
   * @see
   * org.eclipse.jface.text.ITextPresentationListener#applyTextPresentation(org.eclipse.jface.text
   * .TextPresentation)
   */
  public void applyTextPresentation(TextPresentation textPresentation) {
    IRegion region = textPresentation.getExtent();
    int minStart = Integer.MAX_VALUE;
    int maxEnd = Integer.MIN_VALUE;
    int i = computeIndexAtOffset(fPositions, region.getOffset()), n = computeIndexAtOffset(
        fPositions, region.getOffset() + region.getLength());
    if (n - i > 2) {
      List ranges = new ArrayList(n - i);
      for (; i < n; i++) {
        HighlightedPosition position = (HighlightedPosition) fPositions.get(i);
        if (!position.isDeleted()) {
          if (!position.isReadOnly())
            ranges.add(position.createStyleRange());
          else {
            int offset = position.getOffset();
            minStart = Math.min(minStart, offset);
            maxEnd = Math.max(maxEnd, offset + position.getLength());
          }

        }
      }
      StyleRange[] array = new StyleRange[ranges.size()];
      array = (StyleRange[]) ranges.toArray(array);
      textPresentation.replaceStyleRanges(array);
    } else {
      for (; i < n; i++) {
        HighlightedPosition position = (HighlightedPosition) fPositions.get(i);
        if (!position.isDeleted()) {
          if (!position.isReadOnly())
            textPresentation.replaceStyleRange(position.createStyleRange());
          else {
            int offset = position.getOffset();
            minStart = Math.min(minStart, offset);
            maxEnd = Math.max(maxEnd, offset + position.getLength());
          }
        }
      }
    }
    if (minStart < maxEnd) {
      IStructuredDocument document = (IStructuredDocument) fSourceViewer.getDocument();
      if (document.containsReadOnly(minStart, maxEnd)) {
        Iterator nonDefaultStyleRangeIterator = textPresentation.getNonDefaultStyleRangeIterator();
        while (nonDefaultStyleRangeIterator.hasNext()) {
          StyleRange styleRange = (StyleRange) nonDefaultStyleRangeIterator.next();
          if (document.containsReadOnly(styleRange.start, styleRange.length)) {
            adjustForeground(styleRange);
          }
        }
      }
    }
  }

  /*
   * @see
   * org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text
   * .IDocument, org.eclipse.jface.text.IDocument)
   */
  public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
    setCanceled(true);
    releaseDocument(oldInput);
    resetState();
  }

  /*
   * @see
   * org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument
   * , org.eclipse.jface.text.IDocument)
   */
  public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
    manageDocument(newInput);
  }

  /*
   * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.
   * DocumentEvent)
   */
  public void documentAboutToBeChanged(DocumentEvent event) {
    setCanceled(true);
  }

  /*
   * @see
   * org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
   */
  public void documentChanged(DocumentEvent event) {
  }

  /**
   * @return Returns <code>true</code> iff the current reconcile is canceled.
   *         <p>
   *         NOTE: Also called from background thread.
   *         </p>
   */
  public boolean isCanceled() {
    IDocument document = fSourceViewer != null ? fSourceViewer.getDocument() : null;
    if (document == null)
      return fIsCanceled;

    synchronized (getLockObject(document)) {
      return fIsCanceled;
    }
  }

  /**
   * Set whether or not the current reconcile is canceled.
   * 
   * @param isCanceled <code>true</code> iff the current reconcile is canceled
   */
  public void setCanceled(boolean isCanceled) {
    IDocument document = fSourceViewer != null ? fSourceViewer.getDocument() : null;
    if (document == null) {
      fIsCanceled = isCanceled;
      return;
    }

    synchronized (getLockObject(document)) {
      fIsCanceled = isCanceled;
    }
  }

  /**
   * @param document the document
   * @return the document's lock object
   */
  private Object getLockObject(IDocument document) {
    if (document instanceof ISynchronizable) {
      Object lock = ((ISynchronizable) document).getLockObject();
      if (lock != null)
        return lock;
    }
    return document;
  }

  /**
   * Install this presenter on the given source viewer and background presentation reconciler.
   * 
   * @param sourceViewer the source viewer
   * @param backgroundPresentationReconciler the background presentation reconciler, can be
   *          <code>null</code>, in that case
   *          {@link SemanticHighlightingPresenter#createPresentation(List, List)} should not be
   *          called
   */
  public void install(ISourceViewer sourceViewer,
      StructuredPresentationReconciler backgroundPresentationReconciler) {
    fSourceViewer = sourceViewer;
    fPresentationReconciler = backgroundPresentationReconciler;

    if (fSourceViewer instanceof StructuredTextViewer)
      ((StructuredTextViewer) fSourceViewer).prependTextPresentationListener(this);
    else if (fSourceViewer instanceof ITextViewerExtension4)
      ((ITextViewerExtension4) fSourceViewer).addTextPresentationListener(this);

    fSourceViewer.addTextInputListener(this);
    manageDocument(fSourceViewer.getDocument());
  }

  /**
   * Uninstall this presenter.
   */
  public void uninstall() {
    setCanceled(true);

    if (fSourceViewer != null) {
      if (fSourceViewer instanceof ITextViewerExtension4)
        ((ITextViewerExtension4) fSourceViewer).addTextPresentationListener(this);
      releaseDocument(fSourceViewer.getDocument());
      invalidateTextPresentation();
      resetState();

      fSourceViewer.removeTextInputListener(this);
      fSourceViewer = null;
    }
  }

  /**
   * Invalidate text presentation of positions with the given highlighting.
   * 
   * @param highlighting The highlighting
   */
  public void highlightingStyleChanged(HighlightingStyle highlighting) {
    if (fSourceViewer instanceof ITextViewerExtension2) {
      final ITextViewerExtension2 viewer = (ITextViewerExtension2) fSourceViewer;
      for (int i = 0, n = fPositions.size(); i < n; i++) {
        HighlightedPosition position = (HighlightedPosition) fPositions.get(i);
        if (position.getHighlighting() == highlighting)
          viewer.invalidateTextPresentation(position.getOffset(), position.getLength());
      }
    } else {
      fSourceViewer.invalidateTextPresentation();
    }
  }

  /**
   * Invalidate text presentation of all positions.
   */
  private void invalidateTextPresentation() {
    if (fSourceViewer instanceof ITextViewerExtension2) {
      final ITextViewerExtension2 viewer = (ITextViewerExtension2) fSourceViewer;
      for (int i = 0, n = fPositions.size(); i < n; i++) {
        Position position = (Position) fPositions.get(i);
        viewer.invalidateTextPresentation(position.getOffset(), position.getLength());
      }
    } else {
      fSourceViewer.invalidateTextPresentation();
    }
  }

  /**
   * Add a position with the given range and highlighting unconditionally, only from UI thread. The
   * position will also be registered on the document. The text presentation is not invalidated.
   * 
   * @param uiPosition the highlighted position to add from the UI
   */
  private void addPositionFromUI(HighlightedPosition uiPosition) {
    Position position = createHighlightedPosition(uiPosition, uiPosition.getHighlighting(),
        uiPosition.isReadOnly());
    synchronized (fPositionLock) {
      insertPosition(position);
    }

    IDocument document = fSourceViewer.getDocument();
    if (document == null)
      return;
    String positionCategory = getPositionCategory();
    try {
      document.addPosition(positionCategory, position);
    } catch (BadLocationException e) {
      // Should not happen
      Logger.logException(e);
    } catch (BadPositionCategoryException e) {
      // Should not happen
      Logger.logException(e);
    }
  }

  /**
   * Reset to initial state.
   */
  private void resetState() {
    synchronized (fPositionLock) {
      fPositions.clear();
    }
  }

  /**
   * Start managing the given document.
   * 
   * @param document The document
   */
  private void manageDocument(IDocument document) {
    if (document != null) {
      document.addPositionCategory(getPositionCategory());
      document.addPositionUpdater(fPositionUpdater);
      document.addDocumentListener(this);
    }
  }

  /**
   * Stop managing the given document.
   * 
   * @param document The document
   */
  private void releaseDocument(IDocument document) {
    if (document != null) {
      document.removeDocumentListener(this);
      document.removePositionUpdater(fPositionUpdater);
      try {
        document.removePositionCategory(getPositionCategory());
      } catch (BadPositionCategoryException e) {
        // Should not happen
        Logger.logException(e);
      }
    }
  }

  /**
   * @return The semantic reconciler position's category.
   */
  private String getPositionCategory() {
    return toString();
  }

  private void adjustForeground(StyleRange styleRange) {
    RGB oldRGB = null;
    // Color oldColor = styleRange.foreground;
    Color oldColor = styleRange.background;
    if (oldColor == null) {
      // oldRGB = getTextWidget().getForeground().getRGB();
      oldColor = fSourceViewer.getTextWidget().getBackground();
      oldRGB = oldColor.getRGB();
    } else {
      oldRGB = oldColor.getRGB();
    }
    Color newColor = getCachedColorFor(oldRGB);
    if (newColor == null) {
      // make text "closer to" background lumanence
      double target = getRGBConverter().calculateYComponent(oldColor);
      RGB newRGB = getRGBConverter().transformRGBToGrey(oldRGB,
          readOnlyForegroundScaleFactor / 100.0, target);

      // save conversion, so calculations only need to be done once
      cacheColor(oldRGB, newRGB);
      newColor = getCachedColorFor(oldRGB);
    }
    styleRange.foreground = newColor;
  }

  private YUV_RGBConverter getRGBConverter() {
    if (rgbConverter == null) {
      rgbConverter = new YUV_RGBConverter();
    }
    return rgbConverter;
  }

  /**
   * Cache read-only color.
   * 
   * @param oldRGB
   * @param newColor
   */
  private void cacheColor(RGB oldRGB, RGB newColor) {
    if (readOnlyColorTable == null) {
      readOnlyColorTable = new HashMap();
    }
    readOnlyColorTable.put(oldRGB, newColor);
  }

  /**
   * This method is just to get existing read-only colors.
   */
  private Color getCachedColorFor(RGB oldRGB) {
    Color result = null;

    if (readOnlyColorTable != null) {
      RGB readOnlyRGB = (RGB) readOnlyColorTable.get(oldRGB);
      result = EditorUtility.getColor(readOnlyRGB);
    }

    return result;
  }

  /**
   * A utility class to do various color manipulations
   */
  private class YUV_RGBConverter {
    /**
     * This class "holds" the YUV values corresponding to RGB color
     */
    private class YUV {

      class NormalizedRGB {
        double blue;
        double green;
        private final double maxRGB = 256.0;
        double red;

        public NormalizedRGB(RGB rgb) {
          // first normalize to between 0 - 1
          red = rgb.red / maxRGB;
          green = rgb.green / maxRGB;
          blue = rgb.blue / maxRGB;

          red = gammaNormalized(red);
          green = gammaNormalized(green);
          blue = gammaNormalized(blue);

        }
      }

      private NormalizedRGB normalizedRGB;

      private double u = -1;
      private double v = -1;
      private double y = -1;

      private YUV() {
        super();
      }

      public YUV(RGB rgb) {
        this();
        normalizedRGB = new NormalizedRGB(rgb);
        // force calculations
        getY();
        getV();
        getU();
      }

      /**
       * normalize to "average" gamma 2.2222 or 1/0.45
       */
      double gammaNormalized(double colorComponent) {
        if (colorComponent < 0.018) {
          return colorComponent * 0.45;
        } else {
          return 1.099 * Math.pow(colorComponent, 0.45) - 0.099;
        }
      }

      /**
       * @return RGB based on original RGB and current YUV values;
       */

      public double getU() {
        if (u == -1) {
          u = 0.4949 * (normalizedRGB.blue - getY());
        }
        return u;

      }

      public double getV() {
        if (v == -1) {
          v = 0.877 * (normalizedRGB.red - getY());
        }
        return v;
      }

      public double getY() {
        if (y == -1) {
          y = 0.299 * normalizedRGB.red + 0.587 * normalizedRGB.green + 0.114 * normalizedRGB.blue;
        }
        return y;
      }

    }

    public YUV_RGBConverter() {
      super();
    }

    public double calculateYComponent(Color targetColor) {
      return new YUV(targetColor.getRGB()).getY();
    }

    public RGB transformRGBToGrey(RGB originalRGB, double scaleFactor, double target) {
      RGB transformedRGB = null;
      // we left the "full" API method signature, but this
      // version does not take into account originalRGB, though
      // it might someday.
      // for now, we'll simply make the new RGB grey, either a little
      // lighter, or a little darker than background.
      double y = 0;
      double mid = 0.5;
      // zero is black, one is white
      if (target < mid) {
        // is "dark" make lighter
        y = target + scaleFactor;
      } else {
        // is "light" make darker
        y = target - scaleFactor;
      }
      int c = (int) Math.round(y * 255);
      // just to gaurd against mis-use, or scale's values greater
      // than mid point (and possibly rounding error)
      if (c > 255)
        c = 255;
      if (c < 0)
        c = 0;
      transformedRGB = new RGB(c, c, c);
      return transformedRGB;
    }
  }
}
