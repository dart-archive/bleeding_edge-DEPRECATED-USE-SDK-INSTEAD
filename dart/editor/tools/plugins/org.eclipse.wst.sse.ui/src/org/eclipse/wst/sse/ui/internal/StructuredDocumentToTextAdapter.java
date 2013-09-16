/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ConfigurableLineTracker;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentAdapter;
import org.eclipse.jface.text.IDocumentAdapterExtension;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRepairableDocument;
import org.eclipse.jface.text.ITextStore;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.projection.ProjectionDocument;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.TextChangeListener;
import org.eclipse.swt.custom.TextChangedEvent;
import org.eclipse.swt.custom.TextChangingEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.ILockable;
import org.eclipse.wst.sse.core.internal.provisional.events.IStructuredDocumentListener;
import org.eclipse.wst.sse.core.internal.provisional.events.NewDocumentEvent;
import org.eclipse.wst.sse.core.internal.provisional.events.NoChangeEvent;
import org.eclipse.wst.sse.core.internal.provisional.events.RegionChangedEvent;
import org.eclipse.wst.sse.core.internal.provisional.events.RegionsReplacedEvent;
import org.eclipse.wst.sse.core.internal.provisional.events.StructuredDocumentEvent;
import org.eclipse.wst.sse.core.internal.provisional.events.StructuredDocumentRegionsReplacedEvent;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegionList;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.internal.util.Debug;
import org.eclipse.wst.sse.core.internal.util.Utilities;
import org.eclipse.wst.sse.ui.internal.util.Assert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Adapts IStructuredDocument events and methods to StyledTextContent events and methods
 */
public class StructuredDocumentToTextAdapter implements IDocumentAdapter, IDocumentAdapterExtension {

  private class DocumentClone extends AbstractDocument {

    /**
     * Creates a new document clone with the given content.
     * 
     * @param content the content
     * @param lineDelimiters the line delimiters
     */
    public DocumentClone(String content, String[] lineDelimiters) {
      super();
      setTextStore(new StringTextStore(content));
      ConfigurableLineTracker tracker = new ConfigurableLineTracker(lineDelimiters);
      setLineTracker(tracker);
      getTracker().set(content);
      completeInitialization();
    }
  }

  // A pre-notification listener for the viewer's Document
  class DocumentListener implements IDocumentListener {
    protected boolean allTextChanged = false;

    protected DocumentEvent currentEvent;

    synchronized public void documentAboutToBeChanged(DocumentEvent event) {
      if (isStoppedForwardingChanges())
        return;

      pendingDocumentChangedEvent = true;
      allTextChanged = event.getOffset() <= 0
          && event.getLength() >= StructuredDocumentToTextAdapter.this.getDocument().getLength();
      currentEvent = event;

      StructuredDocumentToTextAdapter.this.relayTextChanging(event.getOffset(), event.getLength(),
          event.getText());
    }

    synchronized public void documentChanged(DocumentEvent event) {
      if (isStoppedForwardingChanges())
        return;

      if (currentEvent != null && event == currentEvent) {
        if (allTextChanged) {
          StructuredDocumentToTextAdapter.this.relayTextSet();
        } else {
          // temp work around for immediate thread
          // problem.
          // should have more general solution
          // soon. 'syncExec' are rumored to be
          // prone to hang.
          StructuredDocumentToTextAdapter.this.relayTextChanged();
        }
      }

      currentEvent = null;
      pendingDocumentChangedEvent = false;
      handlePendingEvents();
      lastEvent = null;

    }
  }

  private static class StringTextStore implements ITextStore {

    private String fContent;

    /**
     * Creates a new string text store with the given content.
     * 
     * @param content the content
     */
    public StringTextStore(String content) {
      Assert.isNotNull(content, "content can not be null when setting text store"); //$NON-NLS-1$
      fContent = content;
    }

    /*
     * @see org.eclipse.jface.text.ITextStore#get(int)
     */
    public char get(int offset) {
      return fContent.charAt(offset);
    }

    /*
     * @see org.eclipse.jface.text.ITextStore#get(int, int)
     */
    public String get(int offset, int length) {
      return fContent.substring(offset, offset + length);
    }

    /*
     * @see org.eclipse.jface.text.ITextStore#getLength()
     */
    public int getLength() {
      return fContent.length();
    }

    /*
     * @see org.eclipse.jface.text.ITextStore#replace(int, int, java.lang.String)
     */
    public void replace(int offset, int length, String text) {
    }

    /*
     * @see org.eclipse.jface.text.ITextStore#set(java.lang.String)
     */
    public void set(String text) {
    }

  }

  /**
   * Changes to the Document/IStructuredDocument can extend beyond the text change area and require
   * more redrawing to keep the hilighting correct. The event must be saved so that the redraw is
   * only sent after a textChanged event is received.
   */
  class StructuredDocumentListener implements IStructuredDocumentListener {

    public void newModel(NewDocumentEvent structuredDocumentEvent) {

      if (isStoppedForwardingChanges()) {
        // if
        // (StructuredDocumentToTextAdapter.this.fStopRelayingChanges)
        // {
        if (Debug.debugStructuredDocument) {
          System.out.println("skipped relaying StructuredDocumentEvent " + structuredDocumentEvent.getClass().getName()); //$NON-NLS-1$
        }
        return;
      }
      // should use textSet when all contents have
      // changed
      // otherwise need to use the pair of
      // textChanging and
      // textChanged.
      StructuredDocumentToTextAdapter.this.lastEvent = structuredDocumentEvent;
    }

    public void noChange(final NoChangeEvent structuredDocumentEvent) {

      if (Debug.debugStructuredDocument) {
        System.out.println("skipped relaying StructuredDocumentEvent " + structuredDocumentEvent.getClass().getName()); //$NON-NLS-1$
      }
      if (structuredDocumentEvent.reason == NoChangeEvent.READ_ONLY_STATE_CHANGE) {
        if (pendingDocumentChangedEvent) {
          if (lastEventQueue == null) {
            lastEventQueue = new ArrayList();
          }
          lastEventQueue.add(structuredDocumentEvent);
        } else {
          StructuredDocumentToTextAdapter.this.lastEvent = structuredDocumentEvent;
        }
      }
    }

    public void nodesReplaced(StructuredDocumentRegionsReplacedEvent structuredDocumentEvent) {

      if (isStoppedForwardingChanges()) {
        // if
        // (StructuredDocumentToTextAdapter.this.fStopRelayingChanges)
        // {
        if (Debug.debugStructuredDocument) {
          System.out.println("not relaying StructuredDocumentEvent " + structuredDocumentEvent.getClass().getName()); //$NON-NLS-1$
        }
        return;
      }
      if (Debug.debugStructuredDocument) {
        System.out.println("saving StructuredDocumentEvent " + structuredDocumentEvent.getClass().getName()); //$NON-NLS-1$
      }
      StructuredDocumentToTextAdapter.this.lastEvent = structuredDocumentEvent;
    }

    public void regionChanged(RegionChangedEvent structuredDocumentEvent) {

      if (isStoppedForwardingChanges()) {
        // if
        // (StructuredDocumentToTextAdapter.this.fStopRelayingChanges)
        // {
        if (Debug.debugStructuredDocument) {
          System.out.println("not relaying StructuredDocumentEvent " + structuredDocumentEvent.getClass().getName()); //$NON-NLS-1$
        }
        return;
      }
      if (Debug.debugStructuredDocument) {
        System.out.println("saving StructuredDocumentEvent " + structuredDocumentEvent.getClass().getName()); //$NON-NLS-1$
      }
      StructuredDocumentToTextAdapter.this.lastEvent = structuredDocumentEvent;
    }

    public void regionsReplaced(RegionsReplacedEvent structuredDocumentEvent) {

      if (isStoppedForwardingChanges()) {
        // if
        // (StructuredDocumentToTextAdapter.this.fStopRelayingChanges)
        // {
        if (Debug.debugStructuredDocument) {
          System.out.println("not relaying StructuredDocumentEvent " + structuredDocumentEvent.getClass().getName()); //$NON-NLS-1$
        }
        return;
      }
      if (Debug.debugStructuredDocument) {
        System.out.println("saving StructuredDocumentEvent " + structuredDocumentEvent.getClass().getName()); //$NON-NLS-1$
      }
      StructuredDocumentToTextAdapter.this.lastEvent = structuredDocumentEvent;
    }
  }

  private static final String EMPTY_STRING = ""; //$NON-NLS-1$

  private final static boolean redrawBackground = true;

  /** The visible child document. */
  private ProjectionDocument fChildDocument;

  /** The master document */
  private IDocument fDocument;
  /** The document clone for the non-forwarding case. */
  private IDocument fDocumentClone;

  // only use this temp work around if on GTK
  // it causes funny "cursor blinking" if used on windows
  private final boolean forceRedrawOnRegionChanged = Platform.getWS().equals("gtk"); //$NON-NLS-1$
  /** The original content */
  private String fOriginalContent;
  /** The original line delimiters */
  private String[] fOriginalLineDelimiters;

  private int fStopRelayingChangesRequests = 0;

  private StyledText fStyledTextWidget;

  /** The registered text changed listeners */
  TextChangeListener[] fTextChangeListeners;
  protected DocumentListener internalDocumentListener;

  // The listeners for relaying DocumentEvents and
  // requesting repaints
  // after modification
  private IStructuredDocumentListener internalStructuredDocumentListener;

  protected StructuredDocumentEvent lastEvent = null;
  List lastEventQueue;
  boolean pendingDocumentChangedEvent;

  private static final boolean DEBUG = false;

  /**
   * TEST ONLY - TEST ONLY - TEST ONLY NOT API use this constructor only for tests. Creates a new
   * document adapter which is initiallly not connected to any document.
   */
  public StructuredDocumentToTextAdapter() {

    internalStructuredDocumentListener = new StructuredDocumentListener();
    internalDocumentListener = new DocumentListener();
    // for testing only
    // setDocument(getModelManager().createStructuredDocumentFor(ContentTypeIdentifierForXML.ContentTypeID_XML));
  }

  /**
   * Creates a new document adapter which is initiallly not connected to any document.
   */
  public StructuredDocumentToTextAdapter(StyledText styledTextWidget) {

    // do not use 'this()' in this case
    super();
    internalStructuredDocumentListener = new StructuredDocumentListener();
    internalDocumentListener = new DocumentListener();
    fStyledTextWidget = styledTextWidget;
  }

  private void _setDocument(IDocument newDoc) {
    if (fDocument instanceof IStructuredDocument) {
      ((IStructuredDocument) fDocument).removeDocumentChangedListener(internalStructuredDocumentListener);
    }
    fDocument = newDoc;
    if (!isStoppedForwardingChanges()) {
      fDocumentClone = null;
      fOriginalContent = getDocument() != null ? getDocument().get() : null;
      fOriginalLineDelimiters = getDocument() != null ? getDocument().getLegalLineDelimiters()
          : null;
    }

    if (DEBUG && fDocument != null && !(fDocument instanceof ILockable)) {

      System.out.println("Warning: non ILockable document used in StructuredDocumentToTextAdapter"); //$NON-NLS-1$
      System.out.println("         document updates on non-display thread will not be safe if editor open"); //$NON-NLS-1$
    }
    if (fDocument instanceof IStructuredDocument) {
      ((IStructuredDocument) fDocument).addDocumentChangedListener(internalStructuredDocumentListener);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.custom.StyledTextContent#addTextChangeListener(org.eclipse.swt.custom.
   * TextChangeListener)
   */
  public synchronized void addTextChangeListener(TextChangeListener listener) {

    // make sure listener is not already in listening
    // (and if it is, print a warning to aid debugging,
    // if needed)

    if (Utilities.contains(fTextChangeListeners, listener)) {
      if (Debug.displayWarnings) {
        System.out.println("StructuredDocumentToTextAdapter::addTextChangedListeners. listener " + listener + " was added more than once. "); //$NON-NLS-2$//$NON-NLS-1$
      }
    } else {
      if (Debug.debugStructuredDocument) {
        System.out.println("StructuredDocumentToTextAdapter::addTextChangedListeners. Adding an instance of " + listener.getClass() + " as a listener on text adapter."); //$NON-NLS-2$//$NON-NLS-1$
      }
      int oldSize = 0;
      if (fTextChangeListeners != null) {
        // normally won't be null, but we need to be
        // sure, for first time through
        oldSize = fTextChangeListeners.length;
      }
      int newSize = oldSize + 1;
      TextChangeListener[] newListeners = new TextChangeListener[newSize];
      if (fTextChangeListeners != null) {
        System.arraycopy(fTextChangeListeners, 0, newListeners, 0, oldSize);
      }
      // add listener to last position
      newListeners[newSize - 1] = listener;
      //
      // now switch new for old
      fTextChangeListeners = newListeners;
      //
    }
  }

  /*
   * @see org.eclipse.swt.custom.StyledTextContent#getCharCount()
   */
  public int getCharCount() {

    // getDocument can sometimes be null during startup
    // and dispose
    int result = 0;
    IDocument doc = getDocument();
    if (doc != null) {
      result = getSafeDocument().getLength();
    }
    return result;
  }

  private IDocument getClonedDocument() {
    if (fDocumentClone == null) {
      String content = fOriginalContent == null ? "" : fOriginalContent; //$NON-NLS-1$
      String[] delims = fOriginalLineDelimiters == null ? DefaultLineTracker.DELIMITERS
          : fOriginalLineDelimiters;
      fDocumentClone = new DocumentClone(content, delims);
    }
    return fDocumentClone;
  }

  Display getDisplay() {

    // Note: the workbench should always have a display
    // (unless running headless), whereas Display.getCurrent()
    // only returns the display if the currently executing thread
    // has one.
    if (PlatformUI.isWorkbenchRunning())
      return PlatformUI.getWorkbench().getDisplay();
    else
      return null;
  }

  /**
   * Returns the visible document.
   * 
   * @return IDocument
   */
  protected IDocument getDocument() {

    if (fChildDocument == null)
      return fDocument;
    return fChildDocument;
  }

  /**
   * Returns region in master document of given region (should be region in projection document)
   * 
   * @return region if no projection document exists, region of master document if possible, null
   *         otherwise
   */
  private IRegion getProjectionToMasterRegion(IRegion region) {
    IRegion originalRegion = region;
    if (fChildDocument != null) {
      try {
        originalRegion = fChildDocument.getProjectionMapping().toOriginRegion(region);
      } catch (BadLocationException e) {
        Logger.logException(e);
      }
    }

    return originalRegion;
  }

  /**
   * Returns offset in projection document of given offset (should be offset in master document)
   * 
   * @return offset if no projection document exists, offset of projection document if possible, -1
   *         otherwise
   */
  private int getMasterToProjectionOffset(int offset) {
    int originalOffset = offset;
    if (fChildDocument != null) {
      try {
        originalOffset = fChildDocument.getProjectionMapping().toImageOffset(offset);
      } catch (BadLocationException e) {
        Logger.logException(e);
      }
    }

    return originalOffset;
  }

  /**
   * Return the line at the given character offset without delimiters.
   * <p>
   * 
   * @param offset offset of the line to return. Does not include delimiters of preceeding lines.
   *          Offset 0 is the first character of the document.
   * @return the line text without delimiters
   */
  public java.lang.String getLine(int lineNumber) {

    String result = null;
    if (lineNumber >= getLineCount()) {
      if (Debug.displayWarnings) {
        System.out.println("Development Debug: IStructuredDocument:getLine() error. lineNumber requested (" + lineNumber + ") was greater than number of lines(" + getLineCount() + "). EmptyString returned"); //$NON-NLS-1$//$NON-NLS-3$//$NON-NLS-2$
      }
      result = EMPTY_STRING;
    } else {
      IDocument doc = getSafeDocument();
      if (doc == null) {
        result = EMPTY_STRING;
      } else {
        try {
          IRegion r = doc.getLineInformation(lineNumber);
          if (r.getLength() > 0) {
            result = doc.get(r.getOffset(), r.getLength());
          } else {
            result = EMPTY_STRING;
          }
        } catch (BadLocationException e) {
          result = EMPTY_STRING;
        }
      }
    }
    return result;
  }

  /**
   * Tries to repair the line information.
   * 
   * @param document the document
   * @see IRepairableDocument#repairLineInformation()
   * @see Eclipse 3.0
   */
  private void repairLineInformation(IDocument document) {
    if (document instanceof IRepairableDocument) {
      IRepairableDocument repairable = (IRepairableDocument) document;
      repairable.repairLineInformation();
    }
  }

  /**
   * Return the line index at the given character offset.
   * <p>
   * 
   * @param offset offset of the line to return. The first character of the document is at offset 0.
   *          An offset of getLength() is valid and should answer the number of lines.
   * @return the line index. The first line is at index 0. If the character at offset is a delimiter
   *         character, answer the line index of the line that is delimited. For example, text =
   *         "\r\n\r\n", delimiter = "\r\n", then: getLineAtOffset(0) == 0 getLineAtOffset(1) == 0
   *         getLineAtOffset(2) == 1 getLineAtOffset(3) == 1 getLineAtOffset(4) == 2
   */
  public int getLineAtOffset(int offset) {

    int result = 0;
    IDocument doc = getSafeDocument();
    if (doc != null) {
      try {
        result = doc.getLineOfOffset(offset);
      } catch (BadLocationException x) {
        repairLineInformation(doc);
        try {
          result = doc.getLineOfOffset(offset);
        } catch (BadLocationException x2) {
          // should not occur, but seems to for projection
          // documents, related to repainting overview ruler
          result = 0;
        }
      }
    }
    return result;
  }

  public int getLineCount() {
    int result = 0;
    IDocument doc = getSafeDocument();
    if (doc != null) {
      result = doc.getNumberOfLines();
    }
    return result;
  }

  /*
   * @see org.eclipse.swt.custom.StyledTextContent#getLineDelimiter
   */
  public String getLineDelimiter() {
    String result = null;
    if (getParentDocument() instanceof IStructuredDocument) {
      result = ((IStructuredDocument) getParentDocument()).getLineDelimiter();
    } else {
      IDocument doc = getSafeDocument();
      result = TextUtilities.getDefaultLineDelimiter(doc);
    }
    return result;
  }

  /**
   * Return the character offset of the first character of the given line.
   * <p>
   * 
   * @param lineIndex index of the line. The first line is at index 0.
   * @return offset offset of the first character of the line. The first character of the document
   *         is at offset 0. The return value should include line delimiters. For example, text =
   *         "\r\ntest\r\n", delimiter = "\r\n", then: getOffsetAtLine(0) == 0 getOffsetAtLine(1) ==
   *         2 getOffsetAtLine(2) == 8 NOTE: When there is no text (i.e., no lines),
   *         getOffsetAtLine(0) is a valid call that should return 0.
   */
  public int getOffsetAtLine(int lineIndex) {

    int result = 0;
    IDocument doc = getSafeDocument();
    if (doc != null) {
      try {
        result = doc.getLineOffset(lineIndex);
      } catch (BadLocationException e) {
        result = 0;
      }
    }
    return result;
  }

  /**
   * Returns the parent document
   * 
   * @return the parent document
   */
  private IDocument getParentDocument() {
    return fDocument;
  }

  /**
   * This is the document to use for request from the StyledText widget. Its either the live
   * documnet or a clone of it, depending on stop/resume state.
   */
  private IDocument getSafeDocument() {
    IDocument result = null;
    if (isStoppedForwardingChanges()) {
      result = getClonedDocument();
    } else {
      // note, this document can be normal structured text document,
      // or the projection/child document
      result = getDocument();
    }
    return result;
  }

  /**
   * @return org.eclipse.swt.custom.StyledText
   */
  StyledText getStyledTextWidget() {
    return fStyledTextWidget;
  }

  /**
   * Returns a string representing the content at the given range.
   * <p>
   * 
   * @param start the start offset of the text to return. Offset 0 is the first character of the
   *          document.
   * @param length the length of the text to return
   * @return the text at the given range
   */
  public String getTextRange(int start, int length) {
    String result = null;
    try {
      IDocument doc = getSafeDocument();
      result = doc.get(start, length);
    } catch (BadLocationException e) {
      result = EMPTY_STRING;
    }
    return result;
  }

  /**
   * assume only for "no change" events, for now
   */
  protected void handlePendingEvents() {

    if (lastEventQueue == null)
      return;

    Iterator iterator = lastEventQueue.iterator();
    while (iterator.hasNext()) {
      NoChangeEvent noChangeEvent = (NoChangeEvent) iterator.next();
      redrawNoChange(noChangeEvent);
    }

    lastEventQueue = null;
    lastEvent = null;
  }

  boolean isStoppedForwardingChanges() {
    return fStopRelayingChangesRequests > 0;
  }

  /**
   * this method is assumed to be called only for read only region changes.
   */
  protected void redrawNoChange(NoChangeEvent structuredDocumentEvent) {

    if (isStoppedForwardingChanges())
      return;
    if (Debug.debugStructuredDocument) {
      System.out.println("maybe redraw stuff"); //$NON-NLS-1$
    }

    int startOffset = structuredDocumentEvent.getOffset();
    int length = structuredDocumentEvent.getLength();
    redrawRangeWithLength(startOffset, length);

  }

  /**
   * Request a redraw of the text range occupied by the given StructuredDocumentRegionsReplacedEvent
   * 
   * @param structuredDocumentEvent
   */
  protected void redrawNodesReplaced(StructuredDocumentRegionsReplacedEvent structuredDocumentEvent) {

    if (isStoppedForwardingChanges())
      return;
    if (Debug.debugStructuredDocument) {
      System.out.println("maybe redraw stuff"); //$NON-NLS-1$
    }
    // just the new stuff
    IStructuredDocumentRegionList newStructuredDocumentRegions = structuredDocumentEvent.getNewStructuredDocumentRegions();

    int nNewNodes = newStructuredDocumentRegions.getLength();
    if (nNewNodes > 0) {
      IStructuredDocumentRegion firstNode = newStructuredDocumentRegions.item(0);
      IStructuredDocumentRegion lastNode = newStructuredDocumentRegions.item(nNewNodes - 1);
      redrawRange(firstNode.getStartOffset(), lastNode.getEndOffset());
    }
  }

  /**
   * Redraws the give offsets in terms of the StructuredDocument. If only part of the model is
   * visible, ensures that only the visible portion of the given range is redrawn.
   * 
   * @param startModelOffset
   * @param endModelOffset
   */
  private void redrawRange(final int startModelOffset, final int endModelOffset) {

    if (getDocument() == null)
      return;
    if (Debug.debugStructuredDocument) {
      System.out.println("redraw stuff: " + startModelOffset + "-" + endModelOffset); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (fChildDocument == null) {
      Runnable runnable = new Runnable() {
        public void run() {
          getStyledTextWidget().redrawRange(startModelOffset, endModelOffset - startModelOffset,
              redrawBackground);
        }
      };
      runOnDisplayThreadIfNeedede(runnable);

    } else {
      int high = getDocument().getLength();
      int startOffset = getMasterToProjectionOffset(startModelOffset);

      int endOffset = getMasterToProjectionOffset(endModelOffset);

      // if offsets were not visible, just try to redraw everything in
      // the child document
      // // not visible
      // if (endOffset < 0 || startOffset > high)
      // return;
      // restrict lower bound
      if (startOffset < 0) {
        startOffset = 0;
      }
      // restrict upper bound
      // if (endOffset > high) {
      // endOffset = high;
      // }
      if (endOffset < 0) {
        endOffset = high;
      }

      int length = endOffset - startOffset;
      // redrawBackground with false would be faster
      // but assumes background (or font) is not
      // changing
      final int finalStartOffset = startOffset;
      final int finallength = length;

      Runnable runnable = new Runnable() {
        public void run() {
          getStyledTextWidget().redrawRange(finalStartOffset, finallength, redrawBackground);
        }
      };
      runOnDisplayThreadIfNeedede(runnable);

    }
  }

  /**
   * Redraws the give offsets in terms of the Flat Node model. If only part of the model is visible,
   * ensures that only the visible portion of the given range is redrawn.
   * 
   * @param startModelOffset
   * @param endModelOffset
   */
  private void redrawRangeWithLength(final int startModelOffset, final int length) {

    if (getDocument() == null)
      return;
    if (Debug.debugStructuredDocument) {
      System.out.println("redraw stuff: " + startModelOffset + "-" + length); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (fChildDocument == null) {
      Runnable runnable = new Runnable() {
        public void run() {
          getStyledTextWidget().redrawRange(startModelOffset, length, redrawBackground);
        }
      };
      runOnDisplayThreadIfNeedede(runnable);
    } else {
      int high = getDocument().getLength();
      // TODO need to take into account segmented visible regions
      int startOffset = getMasterToProjectionOffset(startModelOffset);
      // not visible
      if (startOffset > high || length < 1)
        return;
      // restrict lower bound
      if (startOffset < 0) {
        startOffset = 0;
      }
      int endOffset = startOffset + length - 1;
      // restrict upper bound
      if (endOffset > high) {
        endOffset = high;
      }

      // note: length of the child documnet should be
      // updated,
      // need to investigate why its not at this
      // point, but is
      // probably just because the document event
      // handling is not
      // completely finished.
      int newLength = endOffset - startOffset; // d283007

      // redrawBackground with false would be faster
      // but assumes background (or font) is not
      // changing
      final int finalStartOffset = startOffset;
      final int finalNewLength = newLength;
      Runnable runnable = new Runnable() {
        public void run() {
          getStyledTextWidget().redrawRange(finalStartOffset, finalNewLength, redrawBackground);
        }
      };
      runOnDisplayThreadIfNeedede(runnable);
    }
  }

  /**
   * Request a redraw of the text range occupied by the given RegionChangedEvent for certain (not
   * all) ITextRegion contexts
   * 
   * @param structuredDocumentEvent
   */
  protected void redrawRegionChanged(RegionChangedEvent structuredDocumentEvent) {

    if (isStoppedForwardingChanges()) {
      return;
    }
    if (Debug.debugStructuredDocument) {
      System.out.println("maybe redraw stuff"); //$NON-NLS-1$
    }

    // (nsd) TODO: try to make this reliable somehow
    // without being directly content dependent
    // if ((region instanceof ITextRegionContainer) ||
    // (type == XMLJSPRegionContexts.BLOCK_TEXT) ||
    // (type == XMLJSPRegionContexts.JSP_CONTENT)) {
    // IStructuredDocumentRegion flatNode =
    // structuredDocumentEvent.getStructuredDocumentRegion();
    // // redraw background of false is faster,
    // // but assumes background (or font) is not
    // changing
    // redrawRange(flatNode.getStartOffset(region),
    // flatNode.getEndOffset(region));
    // }
    if (forceRedrawOnRegionChanged) {
      // workaround for redrawing problems on Linux-GTK
      int startOffset = structuredDocumentEvent.getOffset();
      int endOffset = structuredDocumentEvent.getOffset() + structuredDocumentEvent.getLength();
      try {
        IRegion startLine = structuredDocumentEvent.fDocument.getLineInformationOfOffset(startOffset);
        IRegion endLine = structuredDocumentEvent.fDocument.getLineInformationOfOffset(endOffset);
        if (startLine != null && endLine != null) {
          redrawRange(startLine.getOffset(), endLine.getOffset() + endLine.getLength());
        }
      } catch (BadLocationException e) {
        // nothing for now
      }
    }
  }

  /**
   * Request a redraw of the text range occupied by the given RegionsReplacedEvent
   * 
   * @param structuredDocumentEvent
   */
  protected void redrawRegionsReplaced(RegionsReplacedEvent structuredDocumentEvent) {

    if (isStoppedForwardingChanges())
      return;
    if (Debug.debugStructuredDocument) {
      System.out.println("maybe redraw stuff"); //$NON-NLS-1$
    }
    ITextRegionList newRegions = structuredDocumentEvent.getNewRegions();
    int nRegions = newRegions.size();
    if (nRegions > 0) {
      ITextRegion firstRegion = newRegions.get(0);
      ITextRegion lastRegion = newRegions.get(nRegions - 1);
      IStructuredDocumentRegion flatNode = structuredDocumentEvent.getStructuredDocumentRegion();
      redrawRange(flatNode.getStartOffset(firstRegion), flatNode.getEndOffset(lastRegion));
    }
  }

  protected void redrawTextChanged() {

    if (lastEvent != null) {
      // update display, since some cases can effect
      // highlighting beyond the changed text area.
      if (lastEvent instanceof StructuredDocumentRegionsReplacedEvent)
        redrawNodesReplaced((StructuredDocumentRegionsReplacedEvent) lastEvent);
      if (lastEvent instanceof RegionsReplacedEvent)
        redrawRegionsReplaced((RegionsReplacedEvent) lastEvent);
      if (lastEvent instanceof RegionChangedEvent)
        redrawRegionChanged((RegionChangedEvent) lastEvent);
      // moved following line to 'document changed' so
      // the "last event" can be
      // re-drawn after pending re-draws
      // lastEvent = null;
    }
  }

  /**
   * Sends a text replace event to all registered listeners.
   */
  protected void relayTextChanged() {

    if (isStoppedForwardingChanges()) {
      if (Debug.debugStructuredDocument && getDocument() != null) {
        System.out.println("NOT relaying text changed (" + getDocument().getLength() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
      }
      return;
    }
    if (Debug.debugStructuredDocument && getDocument() != null) {
      System.out.println("relaying text changed (" + getDocument().getLength() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    final TextChangedEvent textChangedEvent = new TextChangedEvent(this);

    // we must assign listeners to local variable, since
    // the add and remove listener
    // methods can change the actual instance of the
    // listener array from another thread

    Runnable runnable = new Runnable() {
      public void run() {
        if (fTextChangeListeners != null) {
          Object[] holdListeners = fTextChangeListeners;
          for (int i = 0; i < holdListeners.length; i++) {
            // this is a safe cast, since addListeners
            // requires a IStructuredDocumentListener
            ((TextChangeListener) holdListeners[i]).textChanged(textChangedEvent);
          }
        }
      }
    };
    runOnDisplayThreadIfNeedede(runnable);
    redrawTextChanged();
  }

  /**
   * Sends a text change to all registered listeners
   */
  protected void relayTextChanging(int requestedStart, int requestedLength, String requestedChange) {

    if (getDocument() == null)
      return;
    if (isStoppedForwardingChanges()) {
      if (Debug.debugStructuredDocument && getDocument() != null) {
        System.out.println("NOT relaying text changing: " + requestedStart + ":" + getDocument().getLength()); //$NON-NLS-1$ //$NON-NLS-2$
      }
      return;
    }
    if (Debug.debugStructuredDocument && getDocument() != null) {
      System.out.println("relaying text changing: " + requestedStart + ":" + getDocument().getLength()); //$NON-NLS-1$ //$NON-NLS-2$
    }
    lastEvent = null;
    try {
      final TextChangingEvent textChangingEvent = new TextChangingEvent(this);

      textChangingEvent.start = requestedStart;
      textChangingEvent.replaceCharCount = requestedLength;
      textChangingEvent.newCharCount = (requestedChange == null ? 0 : requestedChange.length());
      textChangingEvent.replaceLineCount = getDocument().getNumberOfLines(requestedStart,
          requestedLength) - 1;
      textChangingEvent.newText = requestedChange;
      textChangingEvent.newLineCount = (requestedChange == null ? 0
          : getDocument().computeNumberOfLines(requestedChange));

      // we must assign listeners to local variable,
      // since the add and remove listner
      // methods can change the actual instance of the
      // listener array from another thread
      Runnable runnable = new Runnable() {
        public void run() {
          if (fTextChangeListeners != null) {
            TextChangeListener[] holdListeners = fTextChangeListeners;
            for (int i = 0; i < holdListeners.length; i++) {
              // this is a safe cast, since
              // addListeners requires a
              // IStructuredDocumentListener
              holdListeners[i].textChanging(textChangingEvent);
            }
          }
        }
      };
      runOnDisplayThreadIfNeedede(runnable);
    } catch (BadLocationException e) {
      // log for now, unless we find reason not to
      Logger.log(Logger.INFO, e.getMessage());
    }
  }

  /**
   * Sends a text set event to all registered listeners. Widget should redraw itself automatically.
   */
  protected void relayTextSet() {

    if (isStoppedForwardingChanges()) {
      if (Debug.debugStructuredDocument && getDocument() != null) {
        System.out.println("NOT relaying text set (" + getDocument().getLength() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
      }
      return;
    }
    if (Debug.debugStructuredDocument && getDocument() != null) {
      System.out.println("relaying text set (" + getDocument().getLength() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    lastEvent = null;
    final TextChangedEvent textChangedEvent = new TextChangedEvent(this);

    // we must assign listeners to local variable, since
    // the add and remove listner
    // methods can change the actual instance of the
    // listener array from another thread
    Runnable runnable = new Runnable() {
      public void run() {
        if (fTextChangeListeners != null) {
          TextChangeListener[] holdListeners = fTextChangeListeners;
          for (int i = 0; i < holdListeners.length; i++) {
            holdListeners[i].textSet(textChangedEvent);
          }
        }
      }
    };
    runOnDisplayThreadIfNeedede(runnable);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.custom.StyledTextContent#removeTextChangeListener(org.eclipse.swt.custom.
   * TextChangeListener)
   */
  public synchronized void removeTextChangeListener(final TextChangeListener listener) {

    if ((fTextChangeListeners != null) && (listener != null)) {
      // if its not in the listeners, we'll ignore the
      // request
      if (!Utilities.contains(fTextChangeListeners, listener)) {
        if (Debug.displayWarnings) {
          System.out.println("StructuredDocumentToTextAdapter::removeTextChangedListeners. listener " + listener + " was not present. "); //$NON-NLS-2$//$NON-NLS-1$
        }
      } else {
        if (Debug.debugStructuredDocument) {
          System.out.println("StructuredDocumentToTextAdapter::addTextChangedListeners. Removing an instance of " + listener.getClass() + " as a listener on text adapter."); //$NON-NLS-2$//$NON-NLS-1$
        }
        final int oldSize = fTextChangeListeners.length;
        int newSize = oldSize - 1;
        final TextChangeListener[] newListeners = new TextChangeListener[newSize];

        Runnable runnable = new Runnable() {
          public void run() {
            int index = 0;
            for (int i = 0; i < oldSize; i++) {
              if (fTextChangeListeners[i] != listener) {
                // copy old to new if its not the
                // one we are removing
                newListeners[index++] = fTextChangeListeners[i];
              }
            }
          }
        };
        runOnDisplayThreadIfNeedede(runnable);
        // now that we have a new array, let's
        // switch it for the old one
        fTextChangeListeners = newListeners;
      }
    }
  }

  /**
   * Replace the text with "newText" starting at position "start" for a length of "replaceLength".
   * <p>
   * Implementors have to notify TextChanged listeners after the content has been updated. The
   * TextChangedEvent should be set as follows:
   * <ul>
   * <li>event.type = SWT.TextReplaced
   * <li>event.start = start of the replaced text
   * <li>event.numReplacedLines = number of replaced lines
   * <li>event.numNewLines = number of new lines
   * <li>event.replacedLength = length of the replaced text
   * <li>event.newLength = length of the new text
   * </ul>
   * <b>NOTE: </b> numNewLines is the number of inserted lines and numReplacedLines is the number of
   * deleted lines based on the change that occurs visually. For example:
   * <ul>
   * <li>(replacedText, newText) ==> (numReplacedLines, numNewLines)
   * <li>("", "\n") ==> (0, 1)
   * <li>("\n\n", "a") ==> (2, 0)
   * <li>("a", "\n\n") ==> (0, 2)
   * <li>("\n", "") ==> (1, 0)
   * </ul>
   * </p>
   * 
   * @param start start offset of text to replace, none of the offsets include delimiters of
   *          preceeding lines, offset 0 is the first character of the document
   * @param replaceLength start offset of text to replace
   * @param newText start offset of text to replace
   */
  public void replaceTextRange(int start, int replaceLength, String text) {

    if (getParentDocument() instanceof IStructuredDocument) {
      // the structuredDocument initiates the "changing"
      // and "changed" events.
      // they are both fired by the time this method
      // returns.
      IRegion region = getProjectionToMasterRegion(new Region(start, replaceLength));
      if (region != null) {
        ((IStructuredDocument) getParentDocument()).replaceText(this, region.getOffset(),
            region.getLength(), text);
        return;
      }
    }
    // default is to just try and replace text range in current document
    try {
      getDocument().replace(start, replaceLength, text);
    } catch (BadLocationException e) {
      // log for now, unless we find reason not to
      Logger.log(Logger.INFO, e.getMessage());
    }
  }

  /**
   * @see org.eclipse.jface.text.IDocumentAdapterExtension#resumeForwardingDocumentChanges()
   */
  public void resumeForwardingDocumentChanges() {

    // from re-reading the textSet API in StyledText, we
    // must call
    // textSet if all the contents changed. If all the
    // contents did
    // not change, we need to call the pair of APIs,
    // textChanging and
    // textChanged. So, if we ever keep careful track of
    // changes
    // during stop forwarding and resume forwarding, we
    // can
    // investigate change make use of the pair of APIs.
    fStopRelayingChangesRequests--;
    if (fStopRelayingChangesRequests == 0) {
      // fIsForwarding= true;
      fDocumentClone = null;
      fOriginalContent = null;
      fOriginalLineDelimiters = null;
      // fireTextSet();
      relayTextSet();
    }
  }

  /**
   * This 'Runnable' should be very brief, and should not "call out" to other code which itself
   * might call syncExec, or deadlock might occur.
   * 
   * @param r
   */
  private void runOnDisplayThreadIfNeedede(Runnable r) {
    // if there is no Display at all (that is, running headless),
    // or if we are already running on the display thread, then
    // simply execute the runnable.
    if (getDisplay() == null || (Thread.currentThread() == getDisplay().getThread())) {
      r.run();
    } else {
      // otherwise force the runnable to run on the display thread.
      //
      // Its unclear if we need this at all, once
      // we "force" document update to always take place on display
      // thread.
      IDocument doc = getDocument();
      if (doc instanceof ILockable) {

        ILock lock = null;
        try {
          lock = ((ILockable) doc).getLockObject();
          lock.acquire();
          getDisplay().syncExec(r);
        } finally {
          if (lock != null) {
            lock.release();
          }
        }
      } else {
        // else, ignore!, since risk of deadlock
        throw new IllegalStateException(
            "non lockable document used for structuredDocumentToTextAdapter"); //$NON-NLS-1$
      }
    }
  }

  /**
   * @param newModel
   */
  public void setDocument(IDocument document) {

    if (getDocument() != null) {
      getDocument().removePrenotifiedDocumentListener(internalDocumentListener);
    }
    lastEvent = null;
    if (document instanceof ProjectionDocument) {
      fChildDocument = (ProjectionDocument) document;
      _setDocument(fChildDocument.getMasterDocument());
    } else {
      fChildDocument = null;
      _setDocument(document);
    }
    if (getDocument() != null) {
      getDocument().addPrenotifiedDocumentListener(internalDocumentListener);
    }
  }

  /**
   * @see IDocument#setText
   */
  public void setText(String string) {

    if (isStoppedForwardingChanges()) {
      fDocumentClone = null;
      fOriginalContent = getDocument().get();
      fOriginalLineDelimiters = getDocument().getLegalLineDelimiters();
    } else if (getParentDocument() instanceof IStructuredDocument) {
      ((IStructuredDocument) getDocument()).setText(this, string);
    } else {
      getDocument().set(string);
    }
    relayTextSet();
  }

  /**
   * This method was added to make testing easier. Normally, the widget is specified on the
   * constructor.
   */
  public void setWidget(StyledText widget) {

    fStyledTextWidget = widget;
  }

  /**
   * @see org.eclipse.jface.text.IDocumentAdapterExtension#stopForwardingDocumentChanges()
   */
  public void stopForwardingDocumentChanges() {

    fStopRelayingChangesRequests++;
    // only need to take snapshot on first request
    if (fStopRelayingChangesRequests == 1) {
      fDocumentClone = null;
      fOriginalContent = getDocument().get();
      fOriginalLineDelimiters = getDocument().getLegalLineDelimiters();
    }
  }
}
