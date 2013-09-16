/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.openon;

import com.ibm.icu.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.util.EditorUtility;

/**
 * @deprecated Use org.eclipse.jface.text.hyperlink.HyperlinkManager
 */
public class OpenFileHyperlinkTracker implements KeyListener, MouseListener, MouseMoveListener,
    FocusListener, PaintListener, IPropertyChangeListener, IDocumentListener, ITextInputListener {

  /** The session is active. */
  private boolean fActive;

  /** The currently active style range. */
  private IRegion fActiveRegion;
  /** Preference key for browser-like links to be enabled */
  private String fBrowserLikeLinksKeyModifierKey;

  /** The link color. */
  private Color fColor;
  /** The hand cursor. */
  private Cursor fCursor;
  /** The key modifier mask. */
  private int fKeyModifierMask;
  /** Preference key for hyperlink underline color */
  private String fLinkColorKey;
  /** The preference store */
  private IPreferenceStore fPreferenceStore;
  /** The currently active style range as position. */
  private Position fRememberedPosition;

  /** The text viewer this hyperlink tracker is associated with */
  private ITextViewer fTextViewer;

  /**
	 *  
	 */
  public OpenFileHyperlinkTracker(ITextViewer textViewer) {
    fTextViewer = textViewer;
  }

  private void activateCursor(ITextViewer viewer) {
    StyledText text = viewer.getTextWidget();
    if (text == null || text.isDisposed())
      return;
    Display display = text.getDisplay();
    if (fCursor == null)
      fCursor = new Cursor(display, SWT.CURSOR_HAND);
    text.setCursor(fCursor);
  }

  private int computeStateMask(String modifiers) {
    if (modifiers == null)
      return -1;

    if (modifiers.length() == 0)
      return SWT.NONE;

    int stateMask = 0;
    StringTokenizer modifierTokenizer = new StringTokenizer(modifiers, ",;.:+-* "); //$NON-NLS-1$
    while (modifierTokenizer.hasMoreTokens()) {
      int modifier = EditorUtility.findLocalizedModifier(modifierTokenizer.nextToken());
      if (modifier == 0 || (stateMask & modifier) == modifier)
        return -1;
      stateMask = stateMask | modifier;
    }
    return stateMask;
  }

  /**
   * Creates a color from the information stored in the given preference store. Returns
   * <code>null</code> if there is no such information available.
   */
  private Color createColor(IPreferenceStore store, String key, Display display) {

    RGB rgb = null;

    if (store.contains(key)) {

      if (store.isDefault(key))
        rgb = PreferenceConverter.getDefaultColor(store, key);
      else
        rgb = PreferenceConverter.getColor(store, key);
    }

    return EditorUtility.getColor(rgb);
  }

  public void deactivate() {
    deactivate(false);
  }

  public void deactivate(boolean redrawAll) {
    if (!fActive)
      return;

    repairRepresentation(redrawAll);
    fActive = false;
  }

  /*
   * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.
   * DocumentEvent)
   */
  public void documentAboutToBeChanged(DocumentEvent event) {
    if (fActive && fActiveRegion != null) {
      fRememberedPosition = new Position(fActiveRegion.getOffset(), fActiveRegion.getLength());
      try {
        event.getDocument().addPosition(fRememberedPosition);
      } catch (BadLocationException x) {
        fRememberedPosition = null;
      }
    }
  }

  /*
   * @see
   * org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
   */
  public void documentChanged(DocumentEvent event) {
    if (fRememberedPosition != null) {
      if (!fRememberedPosition.isDeleted()) {

        event.getDocument().removePosition(fRememberedPosition);
        fActiveRegion = new Region(fRememberedPosition.getOffset(), fRememberedPosition.getLength());
        fRememberedPosition = null;

        ITextViewer viewer = getTextViewer();
        if (viewer != null) {
          StyledText widget = viewer.getTextWidget();
          if (widget != null && !widget.isDisposed()) {
            widget.getDisplay().asyncExec(new Runnable() {
              public void run() {
                deactivate();
              }
            });
          }
        }

      } else {
        fActiveRegion = null;
        fRememberedPosition = null;
        deactivate();
      }
    }
  }

  /*
   * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
   */
  public void focusGained(FocusEvent e) {
  }

  /*
   * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
   */
  public void focusLost(FocusEvent event) {
    deactivate();
  }

  private int getCurrentTextOffset() {
    try {
      StyledText text = getTextViewer().getTextWidget();
      if (text == null || text.isDisposed())
        return -1;

      Display display = text.getDisplay();
      Point absolutePosition = display.getCursorLocation();
      Point relativePosition = text.toControl(absolutePosition);

      int widgetOffset = text.getOffsetAtLocation(relativePosition);
      if (getTextViewer() instanceof ITextViewerExtension5) {
        ITextViewerExtension5 extension = (ITextViewerExtension5) getTextViewer();
        return extension.widgetOffset2ModelOffset(widgetOffset);
      } else {
        return widgetOffset + getTextViewer().getVisibleRegion().getOffset();
      }

    } catch (IllegalArgumentException e) {
      return -1;
    }
  }

  private Point getMaximumLocation(StyledText text, int offset, int length) {
    Point maxLocation = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

    for (int i = 0; i <= length; i++) {
      Point location = text.getLocationAtOffset(offset + i);

      if (location.x > maxLocation.x)
        maxLocation.x = location.x;
      if (location.y > maxLocation.y)
        maxLocation.y = location.y;
    }

    return maxLocation;
  }

  private Point getMinimumLocation(StyledText text, int offset, int length) {
    Point minLocation = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);

    for (int i = 0; i <= length; i++) {
      Point location = text.getLocationAtOffset(offset + i);

      if (location.x < minLocation.x)
        minLocation.x = location.x;
      if (location.y < minLocation.y)
        minLocation.y = location.y;
    }

    return minLocation;
  }

  private IPreferenceStore getNewPreferenceStore() {
    return fPreferenceStore;
  }

  private ITextViewer getTextViewer() {
    return fTextViewer;
  }

  private void highlightRegion(ITextViewer viewer, IRegion region) {

    if (region.equals(fActiveRegion))
      return;

    repairRepresentation();

    StyledText text = viewer.getTextWidget();
    if (text == null || text.isDisposed())
      return;

    // Underline
    int offset = 0;
    int length = 0;
    if (viewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
      IRegion widgetRange = extension.modelRange2WidgetRange(new Region(region.getOffset(),
          region.getLength()));
      if (widgetRange == null)
        return;

      offset = widgetRange.getOffset();
      length = widgetRange.getLength();

    } else {
      offset = region.getOffset() - viewer.getVisibleRegion().getOffset();
      length = region.getLength();
    }
    // need clearBackground to be true for paint event to be fired
    text.redrawRange(offset, length, true);

    fActiveRegion = region;
  }

  private boolean includes(IRegion region, IRegion position) {
    return position.getOffset() >= region.getOffset()
        && position.getOffset() + position.getLength() <= region.getOffset() + region.getLength();
  }

  /*
   * @see
   * org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text
   * .IDocument, org.eclipse.jface.text.IDocument)
   */
  public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
    if (oldInput == null)
      return;
    deactivate();
    oldInput.removeDocumentListener(this);
  }

  /*
   * @see
   * org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument
   * , org.eclipse.jface.text.IDocument)
   */
  public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
    if (newInput == null)
      return;
    newInput.addDocumentListener(this);
  }

  public void install(IPreferenceStore store) {
    fPreferenceStore = store;
    ITextViewer textViewer = getTextViewer();
    if (textViewer == null)
      return;

    StyledText text = textViewer.getTextWidget();
    if (text == null || text.isDisposed())
      return;

    updateColor(textViewer);

    textViewer.addTextInputListener(this);

    IDocument document = textViewer.getDocument();
    if (document != null)
      document.addDocumentListener(this);

    text.addKeyListener(this);
    text.addMouseListener(this);
    text.addMouseMoveListener(this);
    text.addFocusListener(this);
    text.addPaintListener(this);

    updateKeyModifierMask();

    fPreferenceStore.addPropertyChangeListener(this);
  }

  /*
   * @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
   */
  public void keyPressed(KeyEvent event) {

    if (fActive) {
      deactivate();
      return;
    }

    if (event.keyCode != fKeyModifierMask) {
      deactivate();
      return;
    }

    fActive = true;

    //			removed for #25871
    //
    //			ISourceViewer viewer= getSourceViewer();
    //			if (viewer == null)
    //				return;
    //			
    //			IRegion region= getCurrentTextRegion(viewer);
    //			if (region == null)
    //				return;
    //			
    //			highlightRegion(viewer, region);
    //			activateCursor(viewer);
  }

  /*
   * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
   */
  public void keyReleased(KeyEvent event) {

    if (!fActive)
      return;

    deactivate();
  }

  /*
   * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
   */
  public void mouseDoubleClick(MouseEvent e) {
  }

  /*
   * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
   */
  public void mouseDown(MouseEvent event) {

    if (!fActive)
      return;

    if (event.stateMask != fKeyModifierMask) {
      deactivate();
      return;
    }

    if (event.button != 1) {
      deactivate();
      return;
    }
  }

  /*
   * @see org.eclipse.swt.events.MouseMoveListener#mouseMove(org.eclipse.swt.events.MouseEvent)
   */
  public void mouseMove(MouseEvent event) {

    if (event.widget instanceof Control && !((Control) event.widget).isFocusControl()) {
      deactivate();
      return;
    }

    if (!fActive) {
      if (event.stateMask != fKeyModifierMask)
        return;
      // modifier was already pressed
      fActive = true;
    }

    ITextViewer viewer = getTextViewer();
    if (viewer == null) {
      deactivate();
      return;
    }

    StyledText text = viewer.getTextWidget();
    if (text == null || text.isDisposed()) {
      deactivate();
      return;
    }

    if ((event.stateMask & SWT.BUTTON1) != 0 && text.getSelectionCount() != 0) {
      deactivate();
      return;
    }

    IRegion region = null;
    int offset = getCurrentTextOffset();
    IOpenOn openOn = OpenOnProvider.getInstance().getOpenOn(getTextViewer().getDocument(), offset);
    if (openOn != null) {
      region = openOn.getOpenOnRegion(getTextViewer().getDocument(), offset);
    }
    if (region == null || region.getLength() == 0) {
      repairRepresentation();
      return;
    }

    highlightRegion(viewer, region);
    activateCursor(viewer);
  }

  /*
   * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
   */
  public void mouseUp(MouseEvent e) {

    if (!fActive)
      return;

    if (e.button != 1) {
      deactivate();
      return;
    }

    boolean wasActive = fCursor != null;
    IRegion previousRegion = fActiveRegion;

    deactivate();

    if (wasActive) {
      IOpenOn openOn = OpenOnProvider.getInstance().getOpenOn(getTextViewer().getDocument(),
          previousRegion.getOffset());
      if (openOn != null) {
        openOn.openOn(getTextViewer().getDocument(), previousRegion);
      }
    }
  }

  /*
   * @see PaintListener#paintControl(PaintEvent)
   */
  public void paintControl(PaintEvent event) {
    if (fActiveRegion == null)
      return;

    ITextViewer viewer = getTextViewer();
    if (viewer == null)
      return;

    StyledText text = viewer.getTextWidget();
    if (text == null || text.isDisposed())
      return;

    int offset = 0;
    int length = 0;

    if (viewer instanceof ITextViewerExtension5) {

      ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
      IRegion widgetRange = extension.modelRange2WidgetRange(fActiveRegion);
      if (widgetRange == null)
        return;

      offset = widgetRange.getOffset();
      length = widgetRange.getLength();

    } else {

      IRegion region = viewer.getVisibleRegion();
      if (!includes(region, fActiveRegion))
        return;

      offset = fActiveRegion.getOffset() - region.getOffset();
      length = fActiveRegion.getLength();
    }

    // support for bidi
    Point minLocation = getMinimumLocation(text, offset, length);
    Point maxLocation = getMaximumLocation(text, offset, length);

    int x1 = minLocation.x;
    int x2 = minLocation.x + maxLocation.x - minLocation.x - 1;
    int y = minLocation.y + text.getLineHeight() - 1;

    GC gc = event.gc;
    if (fColor != null && !fColor.isDisposed())
      gc.setForeground(fColor);
    gc.drawLine(x1, y, x2, y);
  }

  /*
   * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getProperty().equals(fLinkColorKey)) {
      ITextViewer viewer = getTextViewer();
      if (viewer != null)
        updateColor(viewer);
    } else if (event.getProperty().equals(fBrowserLikeLinksKeyModifierKey)) {
      updateKeyModifierMask();
    }
  }

  private void repairRepresentation() {
    repairRepresentation(false);
  }

  private void repairRepresentation(boolean redrawAll) {

    if (fActiveRegion == null)
      return;

    int offset = fActiveRegion.getOffset();
    int length = fActiveRegion.getLength();
    fActiveRegion = null;

    ITextViewer viewer = getTextViewer();
    if (viewer != null) {

      resetCursor(viewer);

      // Remove underline
      if (viewer instanceof ITextViewerExtension5) {
        ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
        offset = extension.modelOffset2WidgetOffset(offset);
      } else {
        offset -= viewer.getVisibleRegion().getOffset();
      }
      try {
        StyledText text = viewer.getTextWidget();

        // need clearBackground to be true for paint event to be fired
        text.redrawRange(offset, length, true);
      } catch (IllegalArgumentException x) {
        Logger.logException(x);
      }
    }
  }

  private void resetCursor(ITextViewer viewer) {
    StyledText text = viewer.getTextWidget();
    if (text != null && !text.isDisposed())
      text.setCursor(null);

    if (fCursor != null) {
      fCursor.dispose();
      fCursor = null;
    }
  }

  public void setHyperlinkPreferenceKeys(String linkColorKey, String browserLikeLinksKeyModifierKey) {
    fLinkColorKey = linkColorKey;
    fBrowserLikeLinksKeyModifierKey = browserLikeLinksKeyModifierKey;
  }

  public void uninstall() {
    if (fCursor != null) {
      fCursor.dispose();
      fCursor = null;
    }

    ITextViewer textViewer = getTextViewer();
    if (textViewer == null)
      return;

    textViewer.removeTextInputListener(this);

    IDocument document = textViewer.getDocument();
    if (document != null)
      document.removeDocumentListener(this);

    IPreferenceStore preferenceStore = getNewPreferenceStore();
    if (preferenceStore != null)
      preferenceStore.removePropertyChangeListener(this);

    StyledText text = textViewer.getTextWidget();
    if (text == null || text.isDisposed())
      return;

    text.removeKeyListener(this);
    text.removeMouseListener(this);
    text.removeMouseMoveListener(this);
    text.removeFocusListener(this);
    text.removePaintListener(this);
  }

  private void updateColor(ITextViewer viewer) {
    StyledText text = viewer.getTextWidget();
    if (text == null || text.isDisposed())
      return;

    Display display = text.getDisplay();
    fColor = createColor(getNewPreferenceStore(), fLinkColorKey, display);
  }

  private void updateKeyModifierMask() {
    String modifiers = getNewPreferenceStore().getString(fBrowserLikeLinksKeyModifierKey);
    fKeyModifierMask = computeStateMask(modifiers);
  }
}
