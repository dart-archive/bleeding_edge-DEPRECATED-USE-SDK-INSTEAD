/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.hyperlink;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * The is almost an exact copy of DefaultHyperlinkPresenter. However this hyperlink presenter works
 * with the StructuredTextEditor's Highlighter instead of TextPresentation. The main difference is
 * <code>text.redrawRange(offset, length, true);</code> is called instead of passing false for
 * clearBackground. Also all mention of TextPresentation was removed since it does not really apply.
 * 
 * @see org.eclipse.jface.text.hyperlink.DefaultHyperlinkPresenter
 */
public class HighlighterHyperlinkPresenter implements IHyperlinkPresenter, PaintListener,
    ITextInputListener, IDocumentListener, IPropertyChangeListener {

  /**
   * A named preference that holds the color used for hyperlinks.
   * <p>
   * Value is of type <code>String</code>. A RGB color value encoded as a string using class
   * <code>PreferenceConverter</code>
   * </p>
   * 
   * @see org.eclipse.jface.resource.StringConverter
   * @see org.eclipse.jface.preference.PreferenceConverter
   */
  public final static String HYPERLINK_COLOR = "hyperlinkColor"; //$NON-NLS-1$

  /** The text viewer. */
  private ITextViewer fTextViewer;
  /** The hand cursor. */
  private Cursor fCursor;
  /** The link color. */
  private Color fColor;
  /** Tells whether to dispose the color on uninstall. */
  private boolean fDisposeColor;
  /** The currently active region. */
  private IRegion fActiveRegion;
  /** The currently active style range as position. */
  private Position fRememberedPosition;
  /** The optional preference store */
  private IPreferenceStore fPreferenceStore;

  /**
   * Creates a new default hyperlink presenter which uses {@link #HYPERLINK_COLOR}to read the color
   * from the given preference store.
   * 
   * @param store the preference store
   */
  public HighlighterHyperlinkPresenter(IPreferenceStore store) {
    fPreferenceStore = store;
    fDisposeColor = true;
  }

  /**
   * Creates a new default hyperlink presenter.
   * 
   * @param color the hyperlink color, to be disposed by the caller
   */
  public HighlighterHyperlinkPresenter(Color color) {
    fDisposeColor = false;
    fColor = color;
  }

  public boolean canShowMultipleHyperlinks() {
    return false;
  }

  public void showHyperlinks(IHyperlink[] hyperlinks) {
    Assert.isLegal(hyperlinks != null && hyperlinks.length == 1);
    highlightRegion(hyperlinks[0].getHyperlinkRegion());
    activateCursor();
  }

  public void hideHyperlinks() {
    repairRepresentation();
    fRememberedPosition = null;
  }

  public void install(ITextViewer textViewer) {
    Assert.isNotNull(textViewer);
    fTextViewer = textViewer;
    fTextViewer.addTextInputListener(this);

    StyledText text = fTextViewer.getTextWidget();
    if (text != null && !text.isDisposed()) {
      text.addPaintListener(this);
      if (fPreferenceStore != null)
        fColor = createColor(fPreferenceStore, HYPERLINK_COLOR, text.getDisplay());
    }

    if (fPreferenceStore != null)
      fPreferenceStore.addPropertyChangeListener(this);
  }

  public void uninstall() {
    fTextViewer.removeTextInputListener(this);

    if (fColor != null) {
      if (fDisposeColor)
        fColor.dispose();
      fColor = null;
    }

    if (fCursor != null) {
      fCursor.dispose();
      fCursor = null;
    }

    StyledText text = fTextViewer.getTextWidget();
    if (text != null && !text.isDisposed())
      text.removePaintListener(this);

    fTextViewer = null;

    if (fPreferenceStore != null)
      fPreferenceStore.removePropertyChangeListener(this);
  }

  public void setColor(Color color) {
    Assert.isNotNull(fTextViewer);
    fColor = color;
  }

  private void highlightRegion(IRegion region) {

    if (region.equals(fActiveRegion))
      return;

    repairRepresentation();

    StyledText text = fTextViewer.getTextWidget();
    if (text == null || text.isDisposed())
      return;

    // Underline
    int offset = 0;
    int length = 0;
    if (fTextViewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) fTextViewer;
      IRegion widgetRange = extension.modelRange2WidgetRange(region);
      if (widgetRange == null)
        return;

      offset = widgetRange.getOffset();
      length = widgetRange.getLength();

    } else {
      offset = region.getOffset() - fTextViewer.getVisibleRegion().getOffset();
      length = region.getLength();
    }

    // needs to clean background due to StructuredTextEditor's highlighter
    text.redrawRange(offset, length, true);

    // Invalidate region ==> apply text presentation
    fActiveRegion = region;

    if (fTextViewer instanceof ITextViewerExtension2)
      ((ITextViewerExtension2) fTextViewer).invalidateTextPresentation(region.getOffset(),
          region.getLength());
    else
      fTextViewer.invalidateTextPresentation();
  }

  private void activateCursor() {
    StyledText text = fTextViewer.getTextWidget();
    if (text == null || text.isDisposed())
      return;
    Display display = text.getDisplay();
    if (fCursor == null)
      fCursor = new Cursor(display, SWT.CURSOR_HAND);
    text.setCursor(fCursor);
  }

  private void resetCursor() {
    StyledText text = fTextViewer.getTextWidget();
    if (text != null && !text.isDisposed())
      text.setCursor(null);

    if (fCursor != null) {
      fCursor.dispose();
      fCursor = null;
    }
  }

  private void repairRepresentation() {

    if (fActiveRegion == null)
      return;

    int offset = fActiveRegion.getOffset();
    int length = fActiveRegion.getLength();
    fActiveRegion = null;

    resetCursor();

    // Invalidate ==> remove applied text presentation
    if (fTextViewer instanceof ITextViewerExtension2)
      ((ITextViewerExtension2) fTextViewer).invalidateTextPresentation(offset, length);
    else
      fTextViewer.invalidateTextPresentation();

    // Remove underline
    if (fTextViewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) fTextViewer;
      offset = extension.modelOffset2WidgetOffset(offset);
    } else {
      offset -= fTextViewer.getVisibleRegion().getOffset();
    }
    try {
      StyledText text = fTextViewer.getTextWidget();

      // needs to clean background due to StructuredTextEditor's
      // highlighter
      text.redrawRange(offset, length, true);

    } catch (IllegalArgumentException x) {
      // ignore - do not log
    }
  }

  /*
   * @see PaintListener#paintControl(PaintEvent)
   */
  public void paintControl(PaintEvent event) {
    if (fActiveRegion == null)
      return;

    StyledText text = fTextViewer.getTextWidget();
    if (text == null || text.isDisposed())
      return;

    int offset = 0;
    int length = 0;

    if (fTextViewer instanceof ITextViewerExtension5) {

      ITextViewerExtension5 extension = (ITextViewerExtension5) fTextViewer;
      IRegion widgetRange = extension.modelRange2WidgetRange(fActiveRegion);
      if (widgetRange == null)
        return;

      offset = widgetRange.getOffset();
      length = widgetRange.getLength();

    } else {

      IRegion region = fTextViewer.getVisibleRegion();
      if (!includes(region, fActiveRegion))
        return;

      offset = fActiveRegion.getOffset() - region.getOffset();
      length = fActiveRegion.getLength();
    }

    // support for BIDI
    Point minLocation = getMinimumLocation(text, offset, length);
    Point maxLocation = getMaximumLocation(text, offset, length);

    int x1 = minLocation.x;
    int x2 = maxLocation.x - 1;
    int y = minLocation.y + text.getLineHeight() - 1;

    GC gc = event.gc;
    if (fColor != null && !fColor.isDisposed())
      gc.setForeground(fColor);
    else if (fColor == null && !(offset < 0 && offset >= text.getCharCount())) {
      StyleRange style = text.getStyleRangeAtOffset(offset);
      if (style != null)
        gc.setForeground(style.foreground);
    }
    gc.drawLine(x1, y, x2, y);
  }

  private Point getMinimumLocation(StyledText text, int offset, int length) {
    int max = text.getCharCount();
    Rectangle bounds = text.getBounds();
    Point minLocation = new Point(bounds.width, bounds.height);
    for (int i = 0; i <= length; i++) {
      int k = offset + i;
      if (k < 0 || k > max)
        break;

      Point location = text.getLocationAtOffset(k);
      if (location.x < minLocation.x)
        minLocation.x = location.x;
      if (location.y < minLocation.y)
        minLocation.y = location.y;
    }

    return minLocation;
  }

  private Point getMaximumLocation(StyledText text, int offset, int length) {
    Point maxLocation = new Point(0, 0);

    for (int i = 0; i <= length; i++) {
      int k = offset + i;
      if (k < 0 || k > text.getCharCount())
        break;

      Point location = text.getLocationAtOffset(k);
      if (location.x > maxLocation.x)
        maxLocation.x = location.x;
      if (location.y > maxLocation.y)
        maxLocation.y = location.y;
    }

    return maxLocation;
  }

  private boolean includes(IRegion region, IRegion position) {
    return position.getOffset() >= region.getOffset()
        && position.getOffset() + position.getLength() <= region.getOffset() + region.getLength();
  }

  /*
   * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.
   * DocumentEvent)
   */
  public void documentAboutToBeChanged(DocumentEvent event) {
    if (fActiveRegion != null) {
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

        StyledText widget = fTextViewer.getTextWidget();
        if (widget != null && !widget.isDisposed()) {
          widget.getDisplay().asyncExec(new Runnable() {
            public void run() {
              hideHyperlinks();
            }
          });
        }

      } else {
        fActiveRegion = null;
        fRememberedPosition = null;
        hideHyperlinks();
      }
    }
  }

  /*
   * @see
   * org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text
   * .IDocument, org.eclipse.jface.text.IDocument)
   */
  public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
    if (oldInput == null)
      return;
    hideHyperlinks();
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

  /**
   * Creates a color from the information stored in the given preference store.
   * 
   * @param store the preference store
   * @param key the key
   * @param display the display
   * @return the color or <code>null</code> if there is no such information available
   */
  private Color createColor(IPreferenceStore store, String key, Display display) {

    RGB rgb = null;

    if (store.contains(key)) {

      if (store.isDefault(key))
        rgb = PreferenceConverter.getDefaultColor(store, key);
      else
        rgb = PreferenceConverter.getColor(store, key);

      if (rgb != null)
        return new Color(display, rgb);
    }

    return null;
  }

  /*
   * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.
   * PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent event) {
    if (!HYPERLINK_COLOR.equals(event.getProperty()))
      return;

    if (fDisposeColor && fColor != null && !fColor.isDisposed())
      fColor.dispose();
    fColor = null;

    StyledText textWidget = fTextViewer.getTextWidget();
    if (textWidget != null && !textWidget.isDisposed())
      fColor = createColor(fPreferenceStore, HYPERLINK_COLOR, textWidget.getDisplay());
  }
}
