/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.style;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.util.Debug;
import org.eclipse.wst.sse.ui.internal.ExtendedConfigurationBuilder;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.preferences.EditorPreferenceNames;
import org.eclipse.wst.sse.ui.internal.util.EditorUtility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class is to directly mediate between the Structured Document data structure and the text
 * widget's text and events. It assumes there only the model is interested in text events, and all
 * other views will work from that model. Changes to the text widgets input can cause changes in the
 * model, which in turn cause changes to the widget's display.
 */
public class Highlighter implements IHighlighter {

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

      public YUV(double y, double u, double v) {
        this();
        this.y = y;
        this.u = u;
        this.v = v;
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
      public RGB getRGB() {
        RGB result = null;
        double r = getY() + 1.14 * getV();
        double g = getY() - 0.395 * getU() - 0.58 * getV();
        double b = getY() + 2.032 * getU();

        int red = (int) (inverseGammaNormalized(r) * 256);
        int green = (int) (inverseGammaNormalized(g) * 256);
        int blue = (int) (inverseGammaNormalized(b) * 256);
        if (red < 0)
          red = 0;
        else if (red > 255)
          red = 255;
        if (green < 0)
          green = 0;
        else if (green > 255)
          green = 255;
        if (blue < 0)
          blue = 0;
        else if (blue > 255)
          blue = 255;

        result = new RGB(red, green, blue);
        return result;
      }

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

      double inverseGammaNormalized(double colorComponent) {
        if (colorComponent < 0.018) {
          return colorComponent * .222;
        } else {
          return Math.pow(((.9099 * colorComponent + 0.09)), 2.22);
        }
      }

    }

    public YUV_RGBConverter() {
      super();
    }

    public double calculateYComponent(Color targetColor) {
      return new YUV(targetColor.getRGB()).getY();
    }

    public RGB transformRGB(RGB originalRGB, double scaleFactor, double target) {
      RGB transformedRGB = null;
      // CCIR601 yuv = new CCIR601(originalRGB);
      YUV yuv = new YUV(originalRGB);
      double y = yuv.getY();
      // zero is black, one is white
      if (y < target) {
        // is "dark" make lighter
        y = y + ((target - y) * scaleFactor);
      } else {
        // is "light" make darker
        y = y - ((y - target) * scaleFactor);
      }
      // yuv.setY(y);
      YUV newYUV = new YUV(y, yuv.getU(), yuv.getV());
      // CCIR601 newYUV = new CCIR601(y, yuv.getCb601(),
      // yuv.getCr601());
      transformedRGB = newYUV.getRGB();
      return transformedRGB;
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

  private final boolean DEBUG = false;
  private final StyleRange[] EMPTY_STYLE_RANGE = new StyleRange[0];
  static final String LINE_STYLE_PROVIDER_EXTENDED_ID = "linestyleprovider"; //$NON-NLS-1$
  private static final int MAX_NUMBER_STYLES = 500;
  private static final int LEFT_STYLES_SIZE = 200;
  private static final int RIGHT_STYLES_SIZE = 200;
  private static final int MIDDLE_STYLES_SIZE = 1;

  private IPropertyChangeListener fForegroundScaleListener = new IPropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent event) {
      if (EditorPreferenceNames.READ_ONLY_FOREGROUND_SCALE.equals(event.getProperty())) {
        IPreferenceStore editorStore = SSEUIPlugin.getDefault().getPreferenceStore();
        readOnlyForegroundScaleFactor = editorStore.getInt(EditorPreferenceNames.READ_ONLY_FOREGROUND_SCALE);
        disposeColorTable();
        refreshDisplay();
      }
    }
  };
  private List fHoldStyleResults;
  private String fPartitioning = IDocumentExtension3.DEFAULT_PARTITIONING;

  private int fSavedLength = -1;
  private int fSavedOffset = -1;
  private StyleRange[] fSavedRanges = null;

  private IStructuredDocument fStructuredDocument;
  private Map fTableOfProviders;

  private Map fExtendedProviders;

  protected final LineStyleProvider NOOP_PROVIDER = new LineStyleProviderForNoOp();

  private double readOnlyBackgroundScaleFactor = 10;
  private Hashtable readOnlyColorTable;
  double readOnlyForegroundScaleFactor = 30;

  private YUV_RGBConverter rgbConverter;
  private ITextViewer textViewer;
  private StyledText textWidget;

  public Highlighter() {
    super();

    // in the 'limitSize' method, we make this strong assumption, so, will check here, 
    // so if tweaked in future, we'll get a quick reminder. 
    if (LEFT_STYLES_SIZE + MIDDLE_STYLES_SIZE + RIGHT_STYLES_SIZE > MAX_NUMBER_STYLES) {
      throw new IllegalStateException("Highligher constants are not defined correctly"); //$NON-NLS-1$
    }
  }

  protected void addEmptyRange(int start, int length, Collection holdResults) {
    StyleRange result = new StyleRange();
    result.start = start;
    result.length = length;
    holdResults.add(result);
  }

  /**
   * Registers a given line style provider for a particular partition type. If there is already a
   * line style provider registered for this type, the new line style provider is registered instead
   * of the old one.
   * 
   * @param partitionType the partition type under which to register
   * @param the line style provider to register, or <code>null</code> to remove an existing one
   */
  public void addProvider(String partitionType, LineStyleProvider provider) {
    getTableOfProviders().put(partitionType, provider);
  }

  /**
   * Adjust the style ranges' start and length so that they refer to the textviewer widget's range
   * instead of the textviewer's document range.
   * 
   * @param ranges
   * @param adjustment
   */
  protected void adjust(StyleRange[] ranges, int adjustment) {
    ITextViewer viewer = getTextViewer();

    if (adjustment != 0) {
      // just use the adjustment value
      // convert document regions back to widget regions
      for (int i = 0; i < ranges.length; i++) {
        // just adjust the range using the given adjustment
        ranges[i].start += adjustment;
      }
    } else if (viewer instanceof ITextViewerExtension5) {
      // use ITextViewerExtension5
      ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;

      // convert document regions back to widget regions
      for (int i = 0; i < ranges.length; i++) {
        // get document range, taking into account folding
        // regions in viewer
        IRegion region = extension.modelRange2WidgetRange(new Region(ranges[i].start,
            ranges[i].length));
        if (region != null) {
          ranges[i].start = region.getOffset();
          ranges[i].length = region.getLength();
        } // else what happens if region is not found?!
      }
    }
  }

  /**
   * @deprecated - Read Only areas have unchanged background colors
   */
  void adjustBackground(StyleRange styleRange) {
    RGB oldRGB = null;
    Color oldColor = styleRange.background;
    if (oldColor == null) {
      oldColor = getTextWidget().getBackground();
    }
    oldRGB = oldColor.getRGB();
    Color newColor = getCachedColorFor(oldRGB);
    if (newColor == null) {
      double target = getRGBConverter().calculateYComponent(oldColor);
      // if background is "light" make it darker, and vice versa
      if (target < 0.5)
        target = 1.0;
      else
        target = 0.0;
      RGB newRGB = getRGBConverter().transformRGB(oldRGB, readOnlyBackgroundScaleFactor / 100.0,
          target);

      cacheColor(oldRGB, newRGB);
      newColor = getCachedColorFor(oldRGB);
    }
    styleRange.background = newColor;
  }

  private void adjustForeground(StyleRange styleRange) {
    RGB oldRGB = null;
    // Color oldColor = styleRange.foreground;
    Color oldColor = styleRange.background;
    if (oldColor == null) {
      // oldRGB = getTextWidget().getForeground().getRGB();
      oldColor = getTextWidget().getBackground();
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

  /**
   * Cache read-only color.
   * 
   * @param oldRGB
   * @param newColor
   */
  private void cacheColor(RGB oldRGB, RGB newColor) {
    if (readOnlyColorTable == null) {
      readOnlyColorTable = new Hashtable();
    }
    readOnlyColorTable.put(oldRGB, newColor);
  }

  /**
   * @param result
   * @return
   */
  private StyleRange[] convertReadOnlyRegions(StyleRange[] result, int start, int length) {
    IStructuredDocument structuredDocument = getDocument();

    /**
     * (dmw) For client/provider simplicity (and consistent look and feel) we'll handle readonly
     * regions in one spot, here in the Highlighter. Currently it is a fair assumption that each
     * readonly region will be on an ITextRegion boundary, so we combine consecutive styles when
     * found to be equivalent. Plus, for now, we'll just adjust foreground. Eventually will use a
     * "dimming" algrorithm to adjust color's satuation/brightness.
     */
    if (structuredDocument.containsReadOnly(start, length)) {
      // something is read-only in the line, so go through each style,
      // and adjust
      for (int i = 0; i < result.length; i++) {
        StyleRange styleRange = result[i];
        if (structuredDocument.containsReadOnly(styleRange.start, styleRange.length)) {
          adjustForeground(styleRange);
        }
      }
    }
    return result;
  }

  /**
   * Clear out the readOnlyColorTable
   */
  void disposeColorTable() {
    if (readOnlyColorTable != null) {
      readOnlyColorTable.clear();
    }
    readOnlyColorTable = null;
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

  protected IStructuredDocument getDocument() {
    return fStructuredDocument;
  }

  /**
   * Adjust the given widget offset and length so that they are the textviewer document's offset and
   * length, taking into account what is actually visible in the document.
   * 
   * @param offset
   * @param length
   * @return a region containing the offset and length within the textviewer's document or null if
   *         the offset is not within the document
   */
  private IRegion getDocumentRangeFromWidgetRange(int offset, int length) {
    IRegion styleRegion = null;
    ITextViewer viewer = getTextViewer();
    if (viewer instanceof ITextViewerExtension5) {
      // get document range, taking into account folding regions in
      // viewer
      ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
      styleRegion = extension.widgetRange2ModelRange(new Region(offset, length));
    } else {
      // get document range, taking into account viewer visible region
      // get visible region in viewer
      IRegion vr = null;
      if (viewer != null)
        vr = viewer.getVisibleRegion();
      else
        vr = new Region(0, getDocument().getLength());

      // if offset is not within visible region, then we don't really
      // care
      if (offset <= vr.getLength()) {
        // Adjust the offset to be within visible region
        styleRegion = new Region(offset + vr.getOffset(), length);
      }
    }
    return styleRegion;
  }

  private Map getExtendedProviders() {
    if (fExtendedProviders == null) {
      fExtendedProviders = new HashMap(3);
    }
    return fExtendedProviders;
  }

  /**
   * Method getProviderFor.
   * 
   * @param typedRegion
   * @return LineStyleProvider
   */
  private LineStyleProvider getProviderFor(ITypedRegion typedRegion) {
    String type = typedRegion.getType();
    LineStyleProvider result = (LineStyleProvider) fTableOfProviders.get(type);
    if (result == null) {
      // NOT YET FINALIZED - DO NOT CONSIDER AS API
      synchronized (getExtendedProviders()) {
        if (!getExtendedProviders().containsKey(type)) {
          LineStyleProvider provider = (LineStyleProvider) ExtendedConfigurationBuilder.getInstance().getConfiguration(
              LINE_STYLE_PROVIDER_EXTENDED_ID, type);
          getExtendedProviders().put(type, provider);
          if (provider != null) {
            provider.init(getDocument(), this);
          }
          result = provider;
        } else {
          result = (LineStyleProvider) getExtendedProviders().get(type);
        }
      }
    }
    if (result == null) {
      result = NOOP_PROVIDER;
    }
    return result;
  }

  private YUV_RGBConverter getRGBConverter() {
    if (rgbConverter == null) {
      rgbConverter = new YUV_RGBConverter();
    }
    return rgbConverter;
  }

  private Map getTableOfProviders() {
    if (fTableOfProviders == null) {
      fTableOfProviders = new HashMap();
    }
    return fTableOfProviders;
  }

  /**
   * Returns the textViewer.
   * 
   * @return ITextViewer
   */
  public ITextViewer getTextViewer() {
    return textViewer;
  }

  /**
   * @return
   */
  protected StyledText getTextWidget() {
    return textWidget;
  }

  /**
   * Installs highlighter support on the given text viewer.
   * 
   * @param textViewer the text viewer on which content assist will work
   */
  public void install(ITextViewer newTextViewer) {
    this.textViewer = newTextViewer;

    IPreferenceStore editorStore = SSEUIPlugin.getDefault().getPreferenceStore();
    editorStore.addPropertyChangeListener(fForegroundScaleListener);
    readOnlyForegroundScaleFactor = editorStore.getInt(EditorPreferenceNames.READ_ONLY_FOREGROUND_SCALE);

    if (textWidget != null) {
      textWidget.removeLineStyleListener(this);
    }
    textWidget = newTextViewer.getTextWidget();
    if (textWidget != null) {
      textWidget.addLineStyleListener(this);
    }

    refreshDisplay();
  }

  public StyleRange[] lineGetStyle(int eventLineOffset, int eventLineLength) {
    StyleRange[] eventStyles = EMPTY_STYLE_RANGE;
    try {
      if (getDocument() == null || eventLineLength == 0) {
        // getDocument() == null
        // during initialization, this is sometimes called before our
        // structured
        // is set, in which case we set styles to be the empty style
        // range
        // (event.styles can not be null)

        // eventLineLength == 0
        // we sometimes get odd requests from the very last CRLF in
        // the
        // document
        // it has no length, and there is no node for it!
        eventStyles = EMPTY_STYLE_RANGE;
      } else {
        /*
         * LineStyleProviders work using absolute document offsets. To support visible regions,
         * adjust the requested range up to the full document offsets.
         */
        IRegion styleRegion = getDocumentRangeFromWidgetRange(eventLineOffset, eventLineLength);
        if (styleRegion != null) {
          int start = styleRegion.getOffset();
          int length = styleRegion.getLength();

          ITypedRegion[] partitions = TextUtilities.computePartitioning(getDocument(),
              fPartitioning, start, length, false);
          eventStyles = prepareStyleRangesArray(partitions, start, length);

          /*
           * If there is a subtext offset, the style ranges must be adjusted to the expected offsets
           * just check if eventLineOffset is different than start then adjust, otherwise u can
           * leave it alone unless there is special handling for itextviewerextension5?
           */
          if (start != eventLineOffset) {
            int offset = 0;
            // figure out visible region to use for adjustment
            // only adjust if need to
            if (!(getTextViewer() instanceof ITextViewerExtension5)) {
              IRegion vr = getTextViewer().getVisibleRegion();
              if (vr != null) {
                offset = vr.getOffset();
              }
            }
            adjust(eventStyles, -offset);
          }

          eventStyles = limitSize(eventStyles);

          // for debugging only
          if (DEBUG) {
            if (!valid(eventStyles, eventLineOffset, eventLineLength)) {
              Logger.log(Logger.WARNING,
                  "Highlighter::lineGetStyle found invalid styles at offset " + eventLineOffset); //$NON-NLS-1$
            }
          }
        }

      }

    } catch (Exception e) {
      // if ANY exception occurs during highlighting,
      // just return "no highlighting"
      eventStyles = EMPTY_STYLE_RANGE;
      if (Debug.syntaxHighlighting) {
        System.out.println("Exception during highlighting!"); //$NON-NLS-1$
      }
    }

    return eventStyles;
  }

  /**
   * This method is to centralize the logic in limiting the overall number of style ranges that make
   * it to the styled text widget. Too many styles sent to StyledText results in apparent, but not
   * real, hangs of Eclipse Display thread. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=108806
   * 
   * @param eventStyles
   * @return
   */
  private StyleRange[] limitSize(StyleRange[] eventStyles) {

    // quick return with same object if not modification needed
    if (eventStyles.length < MAX_NUMBER_STYLES) {
      return eventStyles;
    } else {
      // we could just take the easy way out and truncate, but will 
      // be much better appearing if both the start of the line and the 
      // end of the line are displayed with styles. Since these are both
      // the parts of the line a user is likely to look at. The middle of the 
      // line will still be "plain". Presumably, the user would re-format the 
      // file to avoid long lines, so unlikely to see the middle. 
      StyleRange[] newRanges = new StyleRange[LEFT_STYLES_SIZE + RIGHT_STYLES_SIZE
          + MIDDLE_STYLES_SIZE];
      System.arraycopy(eventStyles, 0, newRanges, 0, LEFT_STYLES_SIZE);
      //
      // do end, before we do middle
      System.arraycopy(eventStyles, eventStyles.length - RIGHT_STYLES_SIZE, newRanges,
          LEFT_STYLES_SIZE + MIDDLE_STYLES_SIZE, RIGHT_STYLES_SIZE);
      //
      // technically, we should compute the exact middle as one big style range, 
      // with default colors and styles, so if someone does actually type or work with 
      // documnet as is, will still be correct. 
      //
      StyleRange allBlank = new StyleRange();
      StyleRange lastKnown = newRanges[LEFT_STYLES_SIZE - 1];
      allBlank.start = lastKnown.start + lastKnown.length;
      StyleRange nextKnown = newRanges[LEFT_STYLES_SIZE + MIDDLE_STYLES_SIZE + 1];
      allBlank.length = nextKnown.start - allBlank.start;
      newRanges[LEFT_STYLES_SIZE] = allBlank;
      return newRanges;
    }
  }

  /**
   * A passthrough method that extracts relevant data from the LineStyleEvent and passes it along.
   * This method was separated for performance testing purposes.
   * 
   * @see org.eclipse.swt.custom.LineStyleListener#lineGetStyle(LineStyleEvent)
   */
  public void lineGetStyle(LineStyleEvent event) {
    int offset = event.lineOffset;
    int length = event.lineText.length();

    /*
     * For some reason, we are sometimes asked for the same style range over and over again. This
     * was found to happen during 'revert' of a file with one line in it that is 40K long! So, while
     * we don't know root cause, caching the styled ranges in case the exact same request is made
     * multiple times seems like cheap insurance.
     */
    if (offset == fSavedOffset && length == fSavedLength && fSavedRanges != null) {
      event.styles = fSavedRanges;
    } else {
      // need to assign this array here, or else the field won't get
      // updated
      event.styles = lineGetStyle(offset, length);
      // now saved "cached data" for repeated requests which are exaclty
      // same
      fSavedOffset = offset;
      fSavedLength = length;
      fSavedRanges = event.styles;
    }
  }

  /**
   * Note: its very important this method never return null, which is why the final null check is in
   * a finally clause
   */

  protected StyleRange[] prepareStyleRangesArray(ITypedRegion[] partitions, int start, int length) {

    StyleRange[] result = EMPTY_STYLE_RANGE;

    if (fHoldStyleResults == null) {
      fHoldStyleResults = new ArrayList(partitions.length);
    } else {
      fHoldStyleResults.clear();
    }

    // TODO: make some of these instance variables to prevent creation on
    // stack
    LineStyleProvider currentLineStyleProvider = null;
    boolean handled = false;
    for (int i = 0; i < partitions.length; i++) {
      ITypedRegion currentPartition = partitions[i];
      currentLineStyleProvider = getProviderFor(currentPartition);
      currentLineStyleProvider.init(getDocument(), this);
      handled = currentLineStyleProvider.prepareRegions(currentPartition,
          currentPartition.getOffset(), currentPartition.getLength(), fHoldStyleResults);
      if (Debug.syntaxHighlighting && !handled) {
        System.out.println("Did not handle highlighting in Highlighter inner while"); //$NON-NLS-1$
      }
    }

    int resultSize = fHoldStyleResults.size();
    if (resultSize > 0) {
      result = (StyleRange[]) fHoldStyleResults.toArray(new StyleRange[fHoldStyleResults.size()]);
    } else {
      result = EMPTY_STYLE_RANGE;
    }
    result = convertReadOnlyRegions(result, start, length);
    return result;
  }

  public void refreshDisplay() {
    if (textWidget != null && !textWidget.isDisposed())
      textWidget.redraw();
  }

  /**
	 */
  public void refreshDisplay(int start, int length) {
    if (textWidget != null && !textWidget.isDisposed())
      textWidget.redrawRange(start, length, true);
  }

  public void removeProvider(String partitionType) {
    getTableOfProviders().remove(partitionType);
  }

  public void setDocument(IStructuredDocument structuredDocument) {
    fStructuredDocument = structuredDocument;
  }

  public void setDocumentPartitioning(String partitioning) {
    if (partitioning != null) {
      fPartitioning = partitioning;
    } else {
      fPartitioning = IDocumentExtension3.DEFAULT_PARTITIONING;
    }
  }

  /**
   * Uninstalls highlighter support from the text viewer it has previously be installed on.
   */
  public void uninstall() {
    if (textWidget != null && !textWidget.isDisposed()) {
      textWidget.removeLineStyleListener(this);
    }
    textWidget = null;

    Collection providers = getTableOfProviders().values();
    Iterator iterator = providers.iterator();
    while (iterator.hasNext()) {
      LineStyleProvider lineStyleProvider = (LineStyleProvider) iterator.next();
      lineStyleProvider.release();
      // this remove probably isn't strictly needed, since
      // typically highlighter instance as a whole will go
      // away ... but in case that ever changes, this seems like
      // a better style.
      iterator.remove();
    }

    synchronized (getExtendedProviders()) {
      providers = new ArrayList(getExtendedProviders().values());
      getExtendedProviders().clear();
    }
    iterator = providers.iterator();
    while (iterator.hasNext()) {
      LineStyleProvider lineStyleProvider = (LineStyleProvider) iterator.next();
      if (lineStyleProvider != null) {
        lineStyleProvider.release();
        iterator.remove();
      }
    }

    IPreferenceStore editorStore = SSEUIPlugin.getDefault().getPreferenceStore();
    editorStore.removePropertyChangeListener(fForegroundScaleListener);
    disposeColorTable();

    // clear out cached variables (d282894)
    fSavedOffset = -1;
    fSavedLength = -1;
    fSavedRanges = null;
  }

  /**
   * Purely a debugging aide.
   */
  private boolean valid(StyleRange[] eventStyles, int startOffset, int lineLength) {
    boolean result = false;
    if (eventStyles != null) {
      if (eventStyles.length > 0) {
        StyleRange first = eventStyles[0];
        StyleRange last = eventStyles[eventStyles.length - 1];
        if (startOffset > first.start) {
          result = false;
        } else {
          int lineEndOffset = startOffset + lineLength;
          int lastOffset = last.start + last.length;
          if (lastOffset > lineEndOffset) {
            result = false;
          } else {
            result = true;
          }
        }
      } else {
        // a zero length array is ok
        result = true;
      }
    }
    return result;
  }
}
