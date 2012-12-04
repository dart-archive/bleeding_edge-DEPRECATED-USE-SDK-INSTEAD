package com.google.dart.tools.ui.presentation;

import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderRenderer;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

import java.util.Hashtable;

/**
 * Copied from superclass and changed to display custom colors. A new compiler preference page was
 * enabled to stop complaining about methods that do not override the superclass method due to being
 * in different packages. That should get cleaned up after the offending methods are renamed, but
 * the rename won't happen until most of the bugs have been squashed.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TabFolderRenderer extends CTabFolderRenderer {

  protected CTabFolder parent;

  int[] curve;
  int[] topCurveHighlightStart;
  int[] topCurveHighlightEnd;
  int curveWidth = 0;
  int curveIndent = 0;
  int lastTabHeight = -1;

  Color fillColor;
  /* Selected item appearance */
  Color selectionHighlightGradientBegin = null; //null == no highlight
  //Although we are given new colours all the time to show different states (active, etc),
  //some of which may have a highlight and some not, we'd like to retain the highlight colours
  //as a cache so that we can reuse them if we're again told to show the highlight.
  //We are relying on the fact that only one tab state usually gets a highlight, so only
  //a single cache is required. If that happens to not be true, cache simply becomes less effective,
  //but we don't leak colours.
  Color[] selectionHighlightGradientColorsCache = null; //null is a legal value, check on access
  /* Colors for anti-aliasing */
  Color selectedOuterColor = null;
  Color selectedInnerColor = null;
  Color tabAreaColor = null;
  /* 
   * Border color that was used in computing the cached anti-alias Colors.
   * We have to recompute the colors if the border color changes
   */
  Color lastBorderColor = null;

  private int parentfixedTabHeight = SWT.DEFAULT;
  private int parentminChars = 20;
  private int[] parentgradientPercents;
  private int[] parentselectionGradientPercents;
  private boolean parentshowUnselectedClose = true;
  private boolean parentshowClose = true;
  private boolean parentshowUnselectedImage = true;
  private boolean parentborderVisible = true;
  private boolean parentonBottom = false;
  private boolean parentsimple = true;
  private boolean parentsingle = false;
  private boolean parentselectionGradientVertical = true;
  private boolean parentgradientVertical = true;
  private Color parentselectionBackground;
  private Color parentselectionForeground;
  private Color[] parentselectionGradientColors;
  private Color[] parentgradientColors;
  private Image parentselectionBgImage;

  static final String COLOR_CACHE = "org.eclipse.swt.custom.CTabFolderRenderer.ColorCache"; //$NON-NLS-1$
  //TOP_LEFT_CORNER_HILITE is laid out in reverse (ie. top to bottom)
  //so can fade in same direction as right swoop curve
  static final int[] TOP_LEFT_CORNER_HILITE = new int[] {5, 2, 4, 2, 3, 3, 2, 4, 2, 5, 1, 6};

  static final int[] TOP_LEFT_CORNER = new int[] {0, 6, 1, 5, 1, 4, 4, 1, 5, 1, 6, 0};
  static final int[] TOP_RIGHT_CORNER = new int[] {-6, 0, -5, 1, -4, 1, -1, 4, -1, 5, 0, 6};

  static final int[] BOTTOM_LEFT_CORNER = new int[] {0, -6, 1, -5, 1, -4, 4, -1, 5, -1, 6, 0};
  static final int[] BOTTOM_RIGHT_CORNER = new int[] {-6, 0, -5, -1, -4, -1, -1, -4, -1, -5, 0, -6};

  static final int[] SIMPLE_TOP_LEFT_CORNER = new int[] {0, 2, 1, 1, 2, 0};

  static final int[] SIMPLE_TOP_RIGHT_CORNER = new int[] {-2, 0, -1, 1, 0, 2};
  static final int[] SIMPLE_BOTTOM_LEFT_CORNER = new int[] {0, -2, 1, -1, 2, 0};
  static final int[] SIMPLE_BOTTOM_RIGHT_CORNER = new int[] {-2, 0, -1, -1, 0, -2};
  static final int[] SIMPLE_UNSELECTED_INNER_CORNER = new int[] {0, 0};

  static final int[] TOP_LEFT_CORNER_BORDERLESS = new int[] {0, 6, 1, 5, 1, 4, 4, 1, 5, 1, 6, 0};
  static final int[] TOP_RIGHT_CORNER_BORDERLESS = new int[] {
      -7, 0, -6, 1, -5, 1, -2, 4, -2, 5, -1, 6};
  static final int[] BOTTOM_LEFT_CORNER_BORDERLESS = new int[] {
      0, -6, 1, -6, 1, -5, 2, -4, 4, -2, 5, -1, 6, -1, 6, 0};
  static final int[] BOTTOM_RIGHT_CORNER_BORDERLESS = new int[] {
      -7, 0, -7, -1, -6, -1, -5, -2, -3, -4, -2, -5, -2, -6, -1, -6};
  static final int[] SIMPLE_TOP_LEFT_CORNER_BORDERLESS = new int[] {0, 2, 1, 1, 2, 0};

  static final int[] SIMPLE_TOP_RIGHT_CORNER_BORDERLESS = new int[] {-3, 0, -2, 1, -1, 2};
  static final int[] SIMPLE_BOTTOM_LEFT_CORNER_BORDERLESS = new int[] {0, -3, 1, -2, 2, -1, 3, 0};
  static final int[] SIMPLE_BOTTOM_RIGHT_CORNER_BORDERLESS = new int[] {
      -4, 0, -3, -1, -2, -2, -1, -3};
  static final RGB CLOSE_FILL = new RGB(252, 160, 160);

  static final int BUTTON_SIZE = 18;
  static final int BUTTON_BORDER = SWT.COLOR_WIDGET_DARK_SHADOW;
  static final int BUTTON_FILL = SWT.COLOR_LIST_BACKGROUND;
  static final int BORDER1_COLOR = SWT.COLOR_WIDGET_NORMAL_SHADOW;

  static final int ITEM_TOP_MARGIN = 2;

  static final int ITEM_BOTTOM_MARGIN = 2;

  static final int ITEM_LEFT_MARGIN = 4;
  static final int ITEM_RIGHT_MARGIN = 4;
  static final int INTERNAL_SPACING = 4;

  static final int FLAGS = SWT.DRAW_TRANSPARENT | SWT.DRAW_MNEMONIC;
  static final String ELLIPSIS = "..."; //$NON-NLS-1$ 
  //Part constants
  public static final int PART_BODY = -1;
  public static final int PART_HEADER = -2;
  public static final int PART_BORDER = -3;
  public static final int PART_BACKGROUND = -4;
  public static final int PART_MAX_BUTTON = -5;

  public static final int PART_MIN_BUTTON = -6;
  public static final int PART_CHEVRON_BUTTON = -7;
  public static final int PART_CLOSE_BUTTON = -8;
  public static final int MINIMUM_SIZE = 1 << 24; //TODO: Should this be a state?

  static Color getColor(Display display, RGB rgb) {
    Color color;
    Hashtable ColorCache = (Hashtable) display.getData(COLOR_CACHE);
    if (ColorCache == null) {
      ColorCache = new Hashtable();
      display.setData(COLOR_CACHE, ColorCache);
    }
    Object[] colorData = (Object[]) ColorCache.get(rgb);
    if (colorData != null) {
      color = (Color) colorData[0];
      int refcount = ((Integer) colorData[1]).intValue();
      colorData[1] = new Integer(refcount + 1);
    } else {
      color = new Color(display, rgb);
      ColorCache.put(rgb, new Object[] {color, new Integer(1)});
    }
    return color;
  }

  static void releaseColor(Display display, Color color) {
    RGB rgb = color.getRGB();
    Hashtable ColorCache = (Hashtable) display.getData(COLOR_CACHE);
    if (ColorCache == null) {
      ColorCache = new Hashtable();
      display.setData(COLOR_CACHE, ColorCache);
    }
    Object[] colorData = (Object[]) ColorCache.get(rgb);
    if (colorData != null) {
      int refcount = ((Integer) colorData[1]).intValue();
      refcount--;
      if (refcount == 0) {
        ColorCache.remove(rgb);
        color.dispose();
      } else {
        colorData[1] = new Integer(refcount);
      }
    }
  }

  /**
   * Constructs a new instance of this class given its parent.
   * 
   * @param parent CTabFolder
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_INVALID_ARGUMENT - if the parent is disposed</li>
   *              </ul>
   * @see Widget#getStyle
   */
  protected TabFolderRenderer(CTabFolder parent) {
    super(parent);
    if (parent == null) {
      return;
    }
    if (parent.isDisposed()) {
      SWT.error(SWT.ERROR_INVALID_ARGUMENT);
    }
    this.parent = parent;
    initialize();
  }

  /**
   * Returns the preferred size of a part.
   * <p>
   * The <em>preferred size</em> of a part is the size that it would best be displayed at. The width
   * hint and height hint arguments allow the caller to ask a control questions such as "Given a
   * particular width, how high does the part need to be to show all of the contents?" To indicate
   * that the caller does not wish to constrain a particular dimension, the constant
   * <code>SWT.DEFAULT</code> is passed for the hint.
   * </p>
   * <p>
   * The <code>part</code> value indicated what component the preferred size is to be calculated
   * for. Valid values are any of the part constants:
   * <ul>
   * <li>PART_BODY</li>
   * <li>PART_HEADER</li>
   * <li>PART_BORDER</li>
   * <li>PART_BACKGROUND</li>
   * <li>PART_MAX_BUTTON</li>
   * <li>PART_MIN_BUTTON</li>
   * <li>PART_CHEVRON_BUTTON</li>
   * <li>PART_CLOSE_BUTTON</li>
   * <li>A positive integer which is the index of an item in the CTabFolder.</li>
   * </ul>
   * </p>
   * <p>
   * The <code>state</code> parameter may be one of the following:
   * <ul>
   * <li>SWT.NONE</li>
   * <li>SWT.SELECTED - whether the part is selected</li>
   * </ul>
   * </p>
   * 
   * @param part a part constant
   * @param state current state
   * @param gc the gc to use for measuring
   * @param wHint the width hint (can be <code>SWT.DEFAULT</code>)
   * @param hHint the height hint (can be <code>SWT.DEFAULT</code>)
   * @return the preferred size of the part
   * @since 3.6
   */
  @Override
  protected Point computeSize(int part, int state, GC gc, int wHint, int hHint) {
    int width = 0, height = 0;
    switch (part) {
      case PART_HEADER:
        if (parentfixedTabHeight != SWT.DEFAULT) {
          height = parentfixedTabHeight == 0 ? 0 : parentfixedTabHeight + 1; // +1 for line drawn across top of tab
        } else {
          CTabItem[] items = parentitems();
          if (items.length == 0) {
            height = gc.textExtent("Default", FLAGS).y + ITEM_TOP_MARGIN + ITEM_BOTTOM_MARGIN; //$NON-NLS-1$
          } else {
            for (int i = 0; i < items.length; i++) {
              height = Math.max(height, computeSize(i, SWT.NONE, gc, wHint, hHint).y);
            }
          }
          gc.dispose();
        }
        break;
      case PART_MAX_BUTTON:
      case PART_MIN_BUTTON:
      case PART_CLOSE_BUTTON:
        width = height = BUTTON_SIZE;
        break;
      case PART_CHEVRON_BUTTON:
        width = 3 * BUTTON_SIZE / 2;
        height = BUTTON_SIZE;
        break;
      default:
        if (0 <= part && part < parent.getItemCount()) {
          updateCurves();
          CTabItem item = parentitems()[part];
          if (item.isDisposed()) {
            return new Point(0, 0);
          }
          Image image = item.getImage();
          if (image != null) {
            Rectangle bounds = image.getBounds();
            if ((state & SWT.SELECTED) != 0 || parentshowUnselectedImage) {
              width += bounds.width;
            }
            height = bounds.height;
          }
          String text = null;
          if ((state & MINIMUM_SIZE) != 0) {
            int minChars = parentminChars;
            text = minChars == 0 ? null : item.getText();
            if (text != null && text.length() > minChars) {
              if (useEllipses()) {
                int end = minChars < ELLIPSIS.length() + 1 ? minChars : minChars
                    - ELLIPSIS.length();
                text = text.substring(0, end);
                if (minChars > ELLIPSIS.length() + 1) {
                  text += ELLIPSIS;
                }
              } else {
                int end = minChars;
                text = text.substring(0, end);
              }
            }
          } else {
            text = item.getText();
          }
          if (text != null) {
            if (width > 0) {
              width += INTERNAL_SPACING;
            }
            if (itemfont(item) == null) {
              Point size = gc.textExtent(text, FLAGS);
              width += size.x;
              height = Math.max(height, size.y);
            } else {
              Font gcFont = gc.getFont();
              gc.setFont(itemfont(item));
              Point size = gc.textExtent(text, FLAGS);
              width += size.x;
              height = Math.max(height, size.y);
              gc.setFont(gcFont);
            }
          }
          if (parentshowClose || itemshowClose(item)) {
            if ((state & SWT.SELECTED) != 0 || parentshowUnselectedClose) {
              if (width > 0) {
                width += INTERNAL_SPACING;
              }
              width += computeSize(PART_CLOSE_BUTTON, SWT.NONE, gc, SWT.DEFAULT, SWT.DEFAULT).x;
            }
          }
        }
        break;
    }
    Rectangle trim = computeTrim(part, state, 0, 0, width, height);
    width = trim.width;
    height = trim.height;
    return new Point(width, height);
  }

  /**
   * Given a desired <em>client area</em> for the part (as described by the arguments), returns the
   * bounding rectangle which would be required to produce that client area.
   * <p>
   * In other words, it returns a rectangle such that, if the part's bounds were set to that
   * rectangle, the area of the part which is capable of displaying data (that is, not covered by
   * the "trimmings") would be the rectangle described by the arguments (relative to the receiver's
   * parent).
   * </p>
   * 
   * @param part one of the part constants
   * @param state the state of the part
   * @param x the desired x coordinate of the client area
   * @param y the desired y coordinate of the client area
   * @param width the desired width of the client area
   * @param height the desired height of the client area
   * @return the required bounds to produce the given client area
   * @see CTabFolderRenderer#computeSize(int, int, GC, int, int) valid part and state values
   * @since 3.6
   */
  @Override
  protected Rectangle computeTrim(int part, int state, int x, int y, int width, int height) {
    int borderLeft = parentborderVisible ? 1 : 0;
    int borderRight = borderLeft;
    int borderTop = parentonBottom ? borderLeft : 0;
    int borderBottom = parentonBottom ? 0 : borderLeft;
    int tabHeight = parenttabHeight();
    switch (part) {
      case PART_BODY:
        int style = parent.getStyle();
        int highlight_header = (style & SWT.FLAT) != 0 ? 1 : 3;
        int highlight_margin = (style & SWT.FLAT) != 0 ? 0 : 2;
        if (parentfixedTabHeight == 0 && (style & SWT.FLAT) != 0 && (style & SWT.BORDER) == 0) {
          highlight_header = 0;
        }
        int marginWidth = parent.marginWidth;
        int marginHeight = parent.marginHeight;
        x = x - marginWidth - highlight_margin - borderLeft;
        width = width + borderLeft + borderRight + 2 * marginWidth + 2 * highlight_margin;
        if (parentminimized()) {
          y = parentonBottom ? y - borderTop : y - highlight_header - tabHeight - borderTop;
          height = borderTop + borderBottom + tabHeight + highlight_header;
        } else {
          y = parentonBottom ? y - marginHeight - highlight_margin - borderTop : y - marginHeight
              - highlight_header - tabHeight - borderTop;
          height = height + borderTop + borderBottom + 2 * marginHeight + tabHeight
              + highlight_header + highlight_margin;
        }
        break;
      case PART_HEADER:
        //no trim 
        break;
      case PART_BORDER:
        x = x - borderLeft;
        width = width + borderLeft + borderRight;
        y = y - borderTop;
        height = height + borderTop + borderBottom;
        break;
      default:
        if (0 <= part && part < parent.getItemCount()) {
          updateCurves();
          x = x - ITEM_LEFT_MARGIN;
          width = width + ITEM_LEFT_MARGIN + ITEM_RIGHT_MARGIN;
          if (!parentsimple && !parentsingle && (state & SWT.SELECTED) != 0) {
            width += curveWidth - curveIndent;
          }
          y = y - ITEM_TOP_MARGIN;
          height = height + ITEM_TOP_MARGIN + ITEM_BOTTOM_MARGIN;
        }
        break;
    }
    return new Rectangle(x, y, width, height);
  }

  /**
   * Dispose of any operating system resources associated with the renderer. Called by the
   * CTabFolder parent upon receiving the dispose event or when changing the renderer.
   * 
   * @since 3.6
   */
  @Override
  protected void dispose() {
    disposeAntialiasColors();
    disposeSelectionHighlightGradientColors();
    if (fillColor != null) {
      releaseColor(parent.getDisplay(), fillColor);
      fillColor = null;
    }
  }

  /**
   * Draw a specified <code>part</code> of the CTabFolder using the provided <code>bounds</code> and
   * <code>GC</code>.
   * <p>
   * The valid CTabFolder <code>part</code> constants are:
   * <ul>
   * <li>PART_BODY - the entire body of the CTabFolder</li>
   * <li>PART_HEADER - the upper tab area of the CTabFolder</li>
   * <li>PART_BORDER - the border of the CTabFolder</li>
   * <li>PART_BACKGROUND - the background of the CTabFolder</li>
   * <li>PART_MAX_BUTTON</li>
   * <li>PART_MIN_BUTTON</li>
   * <li>PART_CHEVRON_BUTTON</li>
   * <li>PART_CLOSE_BUTTON</li>
   * <li>A positive integer which is the index of an item in the CTabFolder.</li>
   * </ul>
   * </p>
   * <p>
   * The <code>state</code> parameter may be a combination of:
   * <ul>
   * <li>SWT.BACKGROUND - whether the background should be drawn</li>
   * <li>SWT.FOREGROUND - whether the foreground should be drawn</li>
   * <li>SWT.SELECTED - whether the part is selected</li>
   * <li>SWT.HOT - whether the part is hot (i.e. mouse is over the part)</li>
   * </ul>
   * </p>
   * 
   * @param part part to draw
   * @param state state of the part
   * @param bounds the bounds of the part
   * @param gc the gc to draw the part on
   * @since 3.6
   */
  @Override
  protected void draw(int part, int state, Rectangle bounds, GC gc) {
    switch (part) {
      case PART_BACKGROUND:
        this.drawBackground(gc, bounds, state);
        break;
      case PART_BODY:
        drawBody(gc, bounds, state);
        break;
      case PART_HEADER:
        drawTabArea(gc, bounds, state);
        break;
      case PART_MAX_BUTTON:
        drawMaximize(gc, bounds, state);
        break;
      case PART_MIN_BUTTON:
        drawMinimize(gc, bounds, state);
        break;
      case PART_CHEVRON_BUTTON:
        drawChevron(gc, bounds, state);
        break;
      default:
        if (0 <= part && part < parent.getItemCount()) {
          if (bounds.width == 0 || bounds.height == 0) {
            return;
          }
          if ((state & SWT.SELECTED) != 0) {
            drawSelected(part, gc, bounds, state);
          } else {
            drawUnselected(part, gc, bounds, state);
          }
        }
        break;
    }
  }

  void antialias(int[] shape, Color innerColor, Color outerColor, GC gc) {
    // Don't perform anti-aliasing on Mac and WPF because the platform
    // already does it.  The simple style also does not require anti-aliasing.
    if (parentsimple) {
      return;
    }
    String platform = SWT.getPlatform();
    if ("cocoa".equals(platform)) {
      return; //$NON-NLS-1$
    }
    if ("carbon".equals(platform)) {
      return; //$NON-NLS-1$
    }
    if ("wpf".equals(platform)) {
      return; //$NON-NLS-1$
    }
    // Don't perform anti-aliasing on low resolution displays
    if (parent.getDisplay().getDepth() < 15) {
      return;
    }
    if (outerColor != null) {
      int index = 0;
      boolean left = true;
      int oldY = parentonBottom ? 0 : parent.getSize().y;
      int[] outer = new int[shape.length];
      for (int i = 0; i < shape.length / 2; i++) {
        if (left && (index + 3 < shape.length)) {
          left = parentonBottom ? oldY <= shape[index + 3] : oldY >= shape[index + 3];
          oldY = shape[index + 1];
        }
        outer[index] = shape[index++] + (left ? -1 : +1);
        outer[index] = shape[index++];
      }
      gc.setForeground(outerColor);
      gc.drawPolyline(outer);
    }
    if (innerColor != null) {
      int[] inner = new int[shape.length];
      int index = 0;
      boolean left = true;
      int oldY = parentonBottom ? 0 : parent.getSize().y;
      for (int i = 0; i < shape.length / 2; i++) {
        if (left && (index + 3 < shape.length)) {
          left = parentonBottom ? oldY <= shape[index + 3] : oldY >= shape[index + 3];
          oldY = shape[index + 1];
        }
        inner[index] = shape[index++] + (left ? +1 : -1);
        inner[index] = shape[index++];
      }
      gc.setForeground(innerColor);
      gc.drawPolyline(inner);
    }
  }

  void createAntialiasColors() {
    disposeAntialiasColors();
    Display display = parent.getDisplay();
    lastBorderColor = display.getSystemColor(BORDER1_COLOR);
    RGB lineRGB = lastBorderColor.getRGB();
    /* compute the selected color */
    RGB innerRGB = parentselectionBackground.getRGB();
    if (parentselectionBgImage != null
        || (parentselectionGradientColors != null && parentselectionGradientColors.length > 1)) {
      innerRGB = null;
    }
    RGB outerRGB = parent.getBackground().getRGB();
    if (parentgradientColors != null && parentgradientColors.length > 1) {
      outerRGB = null;
    }
    if (outerRGB != null) {
      RGB from = lineRGB;
      RGB to = outerRGB;
      int red = from.red + 2 * (to.red - from.red) / 3;
      int green = from.green + 2 * (to.green - from.green) / 3;
      int blue = from.blue + 2 * (to.blue - from.blue) / 3;
      selectedOuterColor = getColor(display, new RGB(red, green, blue));
    }
    if (innerRGB != null) {
      RGB from = lineRGB;
      RGB to = innerRGB;
      int red = from.red + 2 * (to.red - from.red) / 3;
      int green = from.green + 2 * (to.green - from.green) / 3;
      int blue = from.blue + 2 * (to.blue - from.blue) / 3;
      selectedInnerColor = getColor(display, new RGB(red, green, blue));
    }
    /* compute the tabArea color */
    outerRGB = parent.getParent().getBackground().getRGB();
    if (outerRGB != null) {
      RGB from = lineRGB;
      RGB to = outerRGB;
      int red = from.red + 2 * (to.red - from.red) / 3;
      int green = from.green + 2 * (to.green - from.green) / 3;
      int blue = from.blue + 2 * (to.blue - from.blue) / 3;
      tabAreaColor = getColor(display, new RGB(red, green, blue));
    }
  }

  /*
   * Allocate colors for the highlight line.
   * Colours will be a gradual blend ranging from to.
   * Blend length will be tab height.
   * Recompute this if tab height changes.
   * Could remain null if there'd be no gradient (start=end or low colour display)
   */
  void createSelectionHighlightGradientColors(Color start) {
    disposeSelectionHighlightGradientColors(); //dispose if existing

    if (start == null) {
      return;
    }

    //alloc colours for entire height to ensure it matches wherever we stop drawing
    int fadeGradientSize = parenttabHeight();

    RGB from = start.getRGB();
    RGB to = parentselectionBackground.getRGB();

    selectionHighlightGradientColorsCache = new Color[fadeGradientSize];
    int denom = fadeGradientSize - 1;

    Display display = parent.getDisplay();
    for (int i = 0; i < fadeGradientSize; i++) {
      int propFrom = denom - i;
      int propTo = i;
      int red = (to.red * propTo + from.red * propFrom) / denom;
      int green = (to.green * propTo + from.green * propFrom) / denom;
      int blue = (to.blue * propTo + from.blue * propFrom) / denom;
      selectionHighlightGradientColorsCache[i] = getColor(display, new RGB(red, green, blue));
    }
  }

  void disposeAntialiasColors() {
    Display display = parent.getDisplay();
    if (tabAreaColor != null) {
      releaseColor(display, tabAreaColor);
    }
    if (selectedInnerColor != null) {
      releaseColor(display, selectedInnerColor);
    }
    if (selectedOuterColor != null) {
      releaseColor(display, selectedOuterColor);
    }
    tabAreaColor = selectedInnerColor = selectedOuterColor = null;
  }

  void disposeSelectionHighlightGradientColors() {
    if (selectionHighlightGradientColorsCache == null) {
      return;
    }
    Display display = parent.getDisplay();
    for (int i = 0; i < selectionHighlightGradientColorsCache.length; i++) {
      releaseColor(display, selectionHighlightGradientColorsCache[i]);
    }
    selectionHighlightGradientColorsCache = null;
  }

  void drawBackground(GC gc, int[] shape, boolean selected) {
    Color defaultBackground = selected ? parentselectionBackground : parent.getBackground();
    Image image = selected ? parentselectionBgImage : null;
    Color[] colors = selected ? parentselectionGradientColors : parentgradientColors;
    int[] percents = selected ? parentselectionGradientPercents : parentgradientPercents;
    boolean vertical = selected ? parentselectionGradientVertical : parentgradientVertical;
    Point size = parent.getSize();
    int width = size.x;
    int height = parenttabHeight() + ((parent.getStyle() & SWT.FLAT) != 0 ? 1 : 3);
    int x = 0;

    int borderLeft = parentborderVisible ? 1 : 0;
    int borderTop = parentonBottom ? borderLeft : 0;
    int borderBottom = parentonBottom ? 0 : borderLeft;

    if (borderLeft > 0) {
      x += 1;
      width -= 2;
    }
    int y = parentonBottom ? size.y - borderBottom - height : borderTop;
    drawBackground(
        gc,
        shape,
        x,
        y,
        width,
        height,
        defaultBackground,
        image,
        colors,
        percents,
        vertical);
  }

  void drawBackground(GC gc, int[] shape, int x, int y, int width, int height,
      Color defaultBackground, Image image, Color[] colors, int[] percents, boolean vertical) {
    Region clipping = null, region = null;
    if (shape != null) {
      clipping = new Region();
      gc.getClipping(clipping);
      region = new Region();
      region.add(shape);
      region.intersect(clipping);
      gc.setClipping(region);
    }
    if (image != null) {
      // draw the background image in shape
      gc.setBackground(defaultBackground);
      gc.fillRectangle(x, y, width, height);
      Rectangle imageRect = image.getBounds();
      gc.drawImage(
          image,
          imageRect.x,
          imageRect.y,
          imageRect.width,
          imageRect.height,
          x,
          y,
          width,
          height);
    } else if (colors != null) {
      // draw gradient
      if (colors.length == 1) {
        Color background = colors[0] != null ? colors[0] : defaultBackground;
        gc.setBackground(background);
        gc.fillRectangle(x, y, width, height);
      } else {
        if (vertical) {
          if (parentonBottom) {
            int pos = 0;
            if (percents[percents.length - 1] < 100) {
              pos = (100 - percents[percents.length - 1]) * height / 100;
              gc.setBackground(defaultBackground);
              gc.fillRectangle(x, y, width, pos);
            }
            Color lastColor = colors[colors.length - 1];
            if (lastColor == null) {
              lastColor = defaultBackground;
            }
            for (int i = percents.length - 1; i >= 0; i--) {
              gc.setForeground(lastColor);
              lastColor = colors[i];
              if (lastColor == null) {
                lastColor = defaultBackground;
              }
              gc.setBackground(lastColor);
              int percentage = i > 0 ? percents[i] - percents[i - 1] : percents[i];
              int gradientHeight = percentage * height / 100;
              gc.fillGradientRectangle(x, y + pos, width, gradientHeight, true);
              pos += gradientHeight;
            }
          } else {
            Color lastColor = colors[0];
            if (lastColor == null) {
              lastColor = defaultBackground;
            }
            int pos = 0;
            for (int i = 0; i < percents.length; i++) {
              gc.setForeground(lastColor);
              lastColor = colors[i + 1];
              if (lastColor == null) {
                lastColor = defaultBackground;
              }
              gc.setBackground(lastColor);
              int percentage = i > 0 ? percents[i] - percents[i - 1] : percents[i];
              int gradientHeight = percentage * height / 100;
              gc.fillGradientRectangle(x, y + pos, width, gradientHeight, true);
              pos += gradientHeight;
            }
            if (pos < height) {
              gc.setBackground(defaultBackground);
              gc.fillRectangle(x, pos, width, height - pos + 1);
            }
          }
        } else { //horizontal gradient
          y = 0;
          height = parent.getSize().y;
          Color lastColor = colors[0];
          if (lastColor == null) {
            lastColor = defaultBackground;
          }
          int pos = 0;
          for (int i = 0; i < percents.length; ++i) {
            gc.setForeground(lastColor);
            lastColor = colors[i + 1];
            if (lastColor == null) {
              lastColor = defaultBackground;
            }
            gc.setBackground(lastColor);
            int gradientWidth = (percents[i] * width / 100) - pos;
            gc.fillGradientRectangle(x + pos, y, gradientWidth, height, false);
            pos += gradientWidth;
          }
          if (pos < width) {
            gc.setBackground(defaultBackground);
            gc.fillRectangle(x + pos, y, width - pos, height);
          }
        }
      }
    } else {
      // draw a solid background using default background in shape
      if ((parent.getStyle() & SWT.NO_BACKGROUND) != 0
          || !defaultBackground.equals(parent.getBackground())) {
        gc.setBackground(defaultBackground);
        gc.fillRectangle(x, y, width, height);
      }
    }
    if (shape != null) {
      gc.setClipping(clipping);
      clipping.dispose();
      region.dispose();
    }
  }

  void drawBackground(GC gc, Rectangle bounds, int state) {
    boolean selected = (state & SWT.SELECTED) != 0;
    Color defaultBackground = selected ? parentselectionBackground : parent.getBackground();
    Image image = selected ? parentselectionBgImage : null;
    Color[] colors = selected ? parentselectionGradientColors : parentgradientColors;
    int[] percents = selected ? parentselectionGradientPercents : parentgradientPercents;
    boolean vertical = selected ? parentselectionGradientVertical : parentgradientVertical;

    drawBackground(
        gc,
        null,
        bounds.x,
        bounds.y,
        bounds.width,
        bounds.height,
        defaultBackground,
        image,
        colors,
        percents,
        vertical);
  }

  void drawBody(GC gc, Rectangle bounds, int state) {
    Point size = new Point(bounds.width, bounds.height);
    int selectedIndex = parentselectedIndex();
    int tabHeight = parenttabHeight();

    int borderLeft = parentborderVisible ? 1 : 0;
    int borderRight = borderLeft;
    int borderTop = parentonBottom ? borderLeft : 0;
    int borderBottom = parentonBottom ? 0 : borderLeft;

    int style = parent.getStyle();
    int highlight_header = (style & SWT.FLAT) != 0 ? 1 : 3;
    int highlight_margin = (style & SWT.FLAT) != 0 ? 0 : 2;

    // fill in body
    if (!parentminimized()) {
      int width = size.x - borderLeft - borderRight - 2 * highlight_margin;
      int height = size.y - borderTop - borderBottom - tabHeight - highlight_header
          - highlight_margin;
      // Draw highlight margin
      if (highlight_margin > 0) {
        int[] shape = null;
        if (parentonBottom) {
          int x1 = borderLeft;
          int y1 = borderTop;
          int x2 = size.x - borderRight;
          int y2 = size.y - borderBottom - tabHeight - highlight_header;
          shape = new int[] {
              x1, y1, x2, y1, x2, y2, x2 - highlight_margin, y2, x2 - highlight_margin,
              y1 + highlight_margin, x1 + highlight_margin, y1 + highlight_margin,
              x1 + highlight_margin, y2, x1, y2};
        } else {
          int x1 = borderLeft;
          int y1 = borderTop + tabHeight + highlight_header;
          int x2 = size.x - borderRight;
          int y2 = size.y - borderBottom;
          shape = new int[] {
              x1, y1, x1 + highlight_margin, y1, x1 + highlight_margin, y2 - highlight_margin,
              x2 - highlight_margin, y2 - highlight_margin, x2 - highlight_margin, y1, x2, y1, x2,
              y2, x1, y2};
        }
        // If horizontal gradient, show gradient across the whole area
        if (selectedIndex != -1 && parentselectionGradientColors != null
            && parentselectionGradientColors.length > 1 && !parentselectionGradientVertical) {
          drawBackground(gc, shape, true);
        } else if (selectedIndex == -1 && parentgradientColors != null
            && parentgradientColors.length > 1 && !parentgradientVertical) {
          drawBackground(gc, shape, false);
        } else {
          gc.setBackground(selectedIndex == -1 ? parent.getBackground() : parentselectionBackground);
          gc.fillPolygon(shape);
        }
      }
      //Draw client area
      if ((parent.getStyle() & SWT.NO_BACKGROUND) != 0) {
        gc.setBackground(parent.getBackground());
        int marginWidth = parent.marginWidth;
        int marginHeight = parent.marginHeight;
        int xClient = borderLeft + marginWidth + highlight_margin, yClient;
        if (parentonBottom) {
          yClient = borderTop + highlight_margin + marginHeight;
        } else {
          yClient = borderTop + tabHeight + highlight_header + marginHeight;
        }
        gc.fillRectangle(xClient - marginWidth, yClient - marginHeight, width, height);
      }
    } else {
      if ((parent.getStyle() & SWT.NO_BACKGROUND) != 0) {
        int height = borderTop + tabHeight + highlight_header + borderBottom;
        if (size.y > height) {
          gc.setBackground(parent.getParent().getBackground());
          gc.fillRectangle(0, height, size.x, size.y - height);
        }
      }
    }

    //draw 1 pixel border around outside
    if (borderLeft > 0) {
      gc.setForeground(parent.getDisplay().getSystemColor(BORDER1_COLOR));
      int x1 = borderLeft - 1;
      int x2 = size.x - borderRight;
      int y1 = parentonBottom ? borderTop - 1 : borderTop + tabHeight;
      int y2 = parentonBottom ? size.y - tabHeight - borderBottom - 1 : size.y - borderBottom;
      gc.drawLine(x1, y1, x1, y2); // left
      gc.drawLine(x2, y1, x2, y2); // right
      if (parentonBottom) {
        gc.drawLine(x1, y1, x2, y1); // top
      } else {
        gc.drawLine(x1, y2, x2, y2); // bottom
      }
    }
  }

  /*
   * Draw the border of the tab
   * 
   * @param gc
   * @param shape
   */
  void drawBorder(GC gc, int[] shape) {

    gc.setForeground(parent.getDisplay().getSystemColor(BORDER1_COLOR));
    gc.drawPolyline(shape);
  }

  void drawChevron(GC gc, Rectangle chevronRect, int chevronImageState) {
    if (chevronRect.width == 0 || chevronRect.height == 0) {
      return;
    }
    int selectedIndex = parentselectedIndex();
    // draw chevron (10x7)
    Display display = parent.getDisplay();
    Point dpi = display.getDPI();
    int fontHeight = 72 * 10 / dpi.y;
    FontData fd = parent.getFont().getFontData()[0];
    fd.setHeight(fontHeight);
    Font f = new Font(display, fd);
    int fHeight = f.getFontData()[0].getHeight() * dpi.y / 72;
    int indent = Math.max(2, (chevronRect.height - fHeight - 4) / 2);
    int x = chevronRect.x + 2;
    int y = chevronRect.y + indent;
    int count;
    int itemCount = parent.getItemCount();
    int[] priority = parentpriority();
    CTabItem[] items = parentitems();
    if (parentsingle) {
      count = selectedIndex == -1 ? itemCount : itemCount - 1;
    } else {
      int showCount = 0;
      while (showCount < priority.length && itemshowing(items[priority[showCount]])) {
        showCount++;
      }
      count = itemCount - showCount;
    }
    String chevronString = count > 99 ? "99+" : String.valueOf(count); //$NON-NLS-1$
    switch (chevronImageState & (SWT.HOT | SWT.SELECTED)) {
      case SWT.NONE: {
        Color chevronBorder = parentsingle ? parent.getSelectionForeground()
            : parent.getForeground();
        gc.setForeground(chevronBorder);
        gc.setFont(f);
        gc.drawLine(x, y, x + 2, y + 2);
        gc.drawLine(x + 2, y + 2, x, y + 4);
        gc.drawLine(x + 1, y, x + 3, y + 2);
        gc.drawLine(x + 3, y + 2, x + 1, y + 4);
        gc.drawLine(x + 4, y, x + 6, y + 2);
        gc.drawLine(x + 6, y + 2, x + 5, y + 4);
        gc.drawLine(x + 5, y, x + 7, y + 2);
        gc.drawLine(x + 7, y + 2, x + 4, y + 4);
        gc.drawString(chevronString, x + 7, y + 3, true);
        break;
      }
      case SWT.HOT: {
        gc.setForeground(display.getSystemColor(BUTTON_BORDER));
        gc.setBackground(display.getSystemColor(BUTTON_FILL));
        gc.setFont(f);
        gc.fillRoundRectangle(
            chevronRect.x,
            chevronRect.y,
            chevronRect.width,
            chevronRect.height,
            6,
            6);
        gc.drawRoundRectangle(
            chevronRect.x,
            chevronRect.y,
            chevronRect.width - 1,
            chevronRect.height - 1,
            6,
            6);
        gc.drawLine(x, y, x + 2, y + 2);
        gc.drawLine(x + 2, y + 2, x, y + 4);
        gc.drawLine(x + 1, y, x + 3, y + 2);
        gc.drawLine(x + 3, y + 2, x + 1, y + 4);
        gc.drawLine(x + 4, y, x + 6, y + 2);
        gc.drawLine(x + 6, y + 2, x + 5, y + 4);
        gc.drawLine(x + 5, y, x + 7, y + 2);
        gc.drawLine(x + 7, y + 2, x + 4, y + 4);
        gc.drawString(chevronString, x + 7, y + 3, true);
        break;
      }
      case SWT.SELECTED: {
        gc.setForeground(display.getSystemColor(BUTTON_BORDER));
        gc.setBackground(display.getSystemColor(BUTTON_FILL));
        gc.setFont(f);
        gc.fillRoundRectangle(
            chevronRect.x,
            chevronRect.y,
            chevronRect.width,
            chevronRect.height,
            6,
            6);
        gc.drawRoundRectangle(
            chevronRect.x,
            chevronRect.y,
            chevronRect.width - 1,
            chevronRect.height - 1,
            6,
            6);
        gc.drawLine(x + 1, y + 1, x + 3, y + 3);
        gc.drawLine(x + 3, y + 3, x + 1, y + 5);
        gc.drawLine(x + 2, y + 1, x + 4, y + 3);
        gc.drawLine(x + 4, y + 3, x + 2, y + 5);
        gc.drawLine(x + 5, y + 1, x + 7, y + 3);
        gc.drawLine(x + 7, y + 3, x + 6, y + 5);
        gc.drawLine(x + 6, y + 1, x + 8, y + 3);
        gc.drawLine(x + 8, y + 3, x + 5, y + 5);
        gc.drawString(chevronString, x + 8, y + 4, true);
        break;
      }
    }
    f.dispose();
  }

  void drawClose(GC gc, Rectangle closeRect, int closeImageState) {
    if (closeRect.width == 0 || closeRect.height == 0) {
      return;
    }
    Display display = parent.getDisplay();

    // draw X 9x9
    int x = closeRect.x + Math.max(1, (closeRect.width - 9) / 2);
    int y = closeRect.y + Math.max(1, (closeRect.height - 9) / 2);
    y += parentonBottom ? -1 : 1;

    Color closeBorder = display.getSystemColor(BUTTON_BORDER);
    switch (closeImageState & (SWT.HOT | SWT.SELECTED | SWT.BACKGROUND)) {
      case SWT.NONE: {
        int[] shape = new int[] {
            x, y, x + 2, y, x + 4, y + 2, x + 5, y + 2, x + 7, y, x + 9, y, x + 9, y + 2, x + 7,
            y + 4, x + 7, y + 5, x + 9, y + 7, x + 9, y + 9, x + 7, y + 9, x + 5, y + 7, x + 4,
            y + 7, x + 2, y + 9, x, y + 9, x, y + 7, x + 2, y + 5, x + 2, y + 4, x, y + 2};
        gc.setBackground(display.getSystemColor(BUTTON_FILL));
        gc.fillPolygon(shape);
        gc.setForeground(closeBorder);
        gc.drawPolygon(shape);
        break;
      }
      case SWT.HOT: {
        int[] shape = new int[] {
            x, y, x + 2, y, x + 4, y + 2, x + 5, y + 2, x + 7, y, x + 9, y, x + 9, y + 2, x + 7,
            y + 4, x + 7, y + 5, x + 9, y + 7, x + 9, y + 9, x + 7, y + 9, x + 5, y + 7, x + 4,
            y + 7, x + 2, y + 9, x, y + 9, x, y + 7, x + 2, y + 5, x + 2, y + 4, x, y + 2};
        gc.setBackground(getFillColor());
        gc.fillPolygon(shape);
        gc.setForeground(closeBorder);
        gc.drawPolygon(shape);
        break;
      }
      case SWT.SELECTED: {
        int[] shape = new int[] {
            x + 1, y + 1, x + 3, y + 1, x + 5, y + 3, x + 6, y + 3, x + 8, y + 1, x + 10, y + 1,
            x + 10, y + 3, x + 8, y + 5, x + 8, y + 6, x + 10, y + 8, x + 10, y + 10, x + 8,
            y + 10, x + 6, y + 8, x + 5, y + 8, x + 3, y + 10, x + 1, y + 10, x + 1, y + 8, x + 3,
            y + 6, x + 3, y + 5, x + 1, y + 3};
        gc.setBackground(getFillColor());
        gc.fillPolygon(shape);
        gc.setForeground(closeBorder);
        gc.drawPolygon(shape);
        break;
      }
      case SWT.BACKGROUND: {
        int[] shape = new int[] {x, y, x + 10, y, x + 10, y + 10, x, y + 10};
        drawBackground(gc, shape, false);
        break;
      }
    }
  }

  /*
   * Draw a highlight effect along the left, top, and right edges of the tab.
   * Only for curved tabs, on top.
   * Do not draw if insufficient colors.
   */
  void drawHighlight(GC gc, Rectangle bounds, int state, int rightEdge) {
    //only draw for curvy tabs and only draw for top tabs
    if (parentsimple || parentonBottom) {
      return;
    }

    if (selectionHighlightGradientBegin == null) {
      return;
    }

    Color[] gradients = selectionHighlightGradientColorsCache;
    if (gradients == null) {
      return;
    }
    int gradientsSize = gradients.length;
    if (gradientsSize == 0) {
      return; //shouldn't happen but just to be tidy
    }

    int x = bounds.x;
    int y = bounds.y;

    gc.setForeground(gradients[0]);

    //draw top horizontal line
    gc.drawLine(TOP_LEFT_CORNER_HILITE[0] + x + 1, //rely on fact that first pair is top/right of curve
        1 + y,
        rightEdge - curveIndent,
        1 + y);

    int[] leftHighlightCurve = TOP_LEFT_CORNER_HILITE;

    int d = parenttabHeight() - topCurveHighlightEnd.length / 2;

    int lastX = 0;
    int lastY = 0;
    int lastColorIndex = 0;

    //draw upper left curve highlight
    for (int i = 0; i < leftHighlightCurve.length / 2; i++) {
      int rawX = leftHighlightCurve[i * 2];
      int rawY = leftHighlightCurve[i * 2 + 1];
      lastX = rawX + x;
      lastY = rawY + y;
      lastColorIndex = rawY - 1;
      gc.setForeground(gradients[lastColorIndex]);
      gc.drawPoint(lastX, lastY);
    }
    //draw left vertical line highlight
    for (int i = lastColorIndex; i < gradientsSize; i++) {
      gc.setForeground(gradients[i]);
      gc.drawPoint(lastX, 1 + lastY++);
    }

    int rightEdgeOffset = rightEdge - curveIndent;

    //draw right swoop highlight up to diagonal portion
    for (int i = 0; i < topCurveHighlightStart.length / 2; i++) {
      int rawX = topCurveHighlightStart[i * 2];
      int rawY = topCurveHighlightStart[i * 2 + 1];
      lastX = rawX + rightEdgeOffset;
      lastY = rawY + y;
      lastColorIndex = rawY - 1;
      if (lastColorIndex >= gradientsSize) {
        break; //can happen if tabs are unusually short and cut off the curve
      }
      gc.setForeground(gradients[lastColorIndex]);
      gc.drawPoint(lastX, lastY);
    }
    //draw right diagonal line highlight
    for (int i = lastColorIndex; i < lastColorIndex + d; i++) {
      if (i >= gradientsSize) {
        break; //can happen if tabs are unusually short and cut off the curve
      }
      gc.setForeground(gradients[i]);
      gc.drawPoint(1 + lastX++, 1 + lastY++);
    }

    //draw right swoop highlight from diagonal portion to end
    for (int i = 0; i < topCurveHighlightEnd.length / 2; i++) {
      int rawX = topCurveHighlightEnd[i * 2]; //d is already encoded in this value
      int rawY = topCurveHighlightEnd[i * 2 + 1]; //d already encoded
      lastX = rawX + rightEdgeOffset;
      lastY = rawY + y;
      lastColorIndex = rawY - 1;
      if (lastColorIndex >= gradientsSize) {
        break; //can happen if tabs are unusually short and cut off the curve
      }
      gc.setForeground(gradients[lastColorIndex]);
      gc.drawPoint(lastX, lastY);
    }
  }

  /*
   * Draw the unselected border for the receiver on the left.
   * 
   * @param gc
   */
  void drawLeftUnselectedBorder(GC gc, Rectangle bounds, int state) {
    int x = bounds.x;
    int y = bounds.y;
    int height = bounds.height;

    int[] shape = null;
    if (parentonBottom) {
      int[] left = parentsimple ? SIMPLE_UNSELECTED_INNER_CORNER : BOTTOM_LEFT_CORNER;

      shape = new int[left.length + 2];
      int index = 0;
      shape[index++] = x;
      shape[index++] = y - 1;
      for (int i = 0; i < left.length / 2; i++) {
        shape[index++] = x + left[2 * i];
        shape[index++] = y + height + left[2 * i + 1] - 1;
      }
    } else {
      int[] left = parentsimple ? SIMPLE_UNSELECTED_INNER_CORNER : TOP_LEFT_CORNER;

      shape = new int[left.length + 2];
      int index = 0;
      shape[index++] = x;
      shape[index++] = y + height;
      for (int i = 0; i < left.length / 2; i++) {
        shape[index++] = x + left[2 * i];
        shape[index++] = y + left[2 * i + 1];
      }

    }

    drawBorder(gc, shape);
  }

  void drawMaximize(GC gc, Rectangle maxRect, int maxImageState) {
    if (maxRect.width == 0 || maxRect.height == 0) {
      return;
    }
    Display display = parent.getDisplay();
    // 5x4 or 7x9
    int x = maxRect.x + (maxRect.width - 10) / 2;
    int y = maxRect.y + 3;

    gc.setForeground(display.getSystemColor(BUTTON_BORDER));
    gc.setBackground(display.getSystemColor(BUTTON_FILL));

    switch (maxImageState & (SWT.HOT | SWT.SELECTED)) {
      case SWT.NONE: {
        if (!parent.getMaximized()) {
          gc.fillRectangle(x, y, 9, 9);
          gc.drawRectangle(x, y, 9, 9);
          gc.drawLine(x + 1, y + 2, x + 8, y + 2);
        } else {
          gc.fillRectangle(x, y + 3, 5, 4);
          gc.fillRectangle(x + 2, y, 5, 4);
          gc.drawRectangle(x, y + 3, 5, 4);
          gc.drawRectangle(x + 2, y, 5, 4);
          gc.drawLine(x + 3, y + 1, x + 6, y + 1);
          gc.drawLine(x + 1, y + 4, x + 4, y + 4);
        }
        break;
      }
      case SWT.HOT: {
        gc.fillRoundRectangle(maxRect.x, maxRect.y, maxRect.width, maxRect.height, 6, 6);
        gc.drawRoundRectangle(maxRect.x, maxRect.y, maxRect.width - 1, maxRect.height - 1, 6, 6);
        if (!parent.getMaximized()) {
          gc.fillRectangle(x, y, 9, 9);
          gc.drawRectangle(x, y, 9, 9);
          gc.drawLine(x + 1, y + 2, x + 8, y + 2);
        } else {
          gc.fillRectangle(x, y + 3, 5, 4);
          gc.fillRectangle(x + 2, y, 5, 4);
          gc.drawRectangle(x, y + 3, 5, 4);
          gc.drawRectangle(x + 2, y, 5, 4);
          gc.drawLine(x + 3, y + 1, x + 6, y + 1);
          gc.drawLine(x + 1, y + 4, x + 4, y + 4);
        }
        break;
      }
      case SWT.SELECTED: {
        gc.fillRoundRectangle(maxRect.x, maxRect.y, maxRect.width, maxRect.height, 6, 6);
        gc.drawRoundRectangle(maxRect.x, maxRect.y, maxRect.width - 1, maxRect.height - 1, 6, 6);
        if (!parent.getMaximized()) {
          gc.fillRectangle(x + 1, y + 1, 9, 9);
          gc.drawRectangle(x + 1, y + 1, 9, 9);
          gc.drawLine(x + 2, y + 3, x + 9, y + 3);
        } else {
          gc.fillRectangle(x + 1, y + 4, 5, 4);
          gc.fillRectangle(x + 3, y + 1, 5, 4);
          gc.drawRectangle(x + 1, y + 4, 5, 4);
          gc.drawRectangle(x + 3, y + 1, 5, 4);
          gc.drawLine(x + 4, y + 2, x + 7, y + 2);
          gc.drawLine(x + 2, y + 5, x + 5, y + 5);
        }
        break;
      }
    }
  }

  void drawMinimize(GC gc, Rectangle minRect, int minImageState) {
    if (minRect.width == 0 || minRect.height == 0) {
      return;
    }
    Display display = parent.getDisplay();
    // 5x4 or 9x3
    int x = minRect.x + (minRect.width - 10) / 2;
    int y = minRect.y + 3;

    gc.setForeground(display.getSystemColor(BUTTON_BORDER));
    gc.setBackground(display.getSystemColor(BUTTON_FILL));

    switch (minImageState & (SWT.HOT | SWT.SELECTED)) {
      case SWT.NONE: {
        if (!parent.getMinimized()) {
          gc.fillRectangle(x, y, 9, 3);
          gc.drawRectangle(x, y, 9, 3);
        } else {
          gc.fillRectangle(x, y + 3, 5, 4);
          gc.fillRectangle(x + 2, y, 5, 4);
          gc.drawRectangle(x, y + 3, 5, 4);
          gc.drawRectangle(x + 2, y, 5, 4);
          gc.drawLine(x + 3, y + 1, x + 6, y + 1);
          gc.drawLine(x + 1, y + 4, x + 4, y + 4);
        }
        break;
      }
      case SWT.HOT: {
        gc.fillRoundRectangle(minRect.x, minRect.y, minRect.width, minRect.height, 6, 6);
        gc.drawRoundRectangle(minRect.x, minRect.y, minRect.width - 1, minRect.height - 1, 6, 6);
        if (!parent.getMinimized()) {
          gc.fillRectangle(x, y, 9, 3);
          gc.drawRectangle(x, y, 9, 3);
        } else {
          gc.fillRectangle(x, y + 3, 5, 4);
          gc.fillRectangle(x + 2, y, 5, 4);
          gc.drawRectangle(x, y + 3, 5, 4);
          gc.drawRectangle(x + 2, y, 5, 4);
          gc.drawLine(x + 3, y + 1, x + 6, y + 1);
          gc.drawLine(x + 1, y + 4, x + 4, y + 4);
        }
        break;
      }
      case SWT.SELECTED: {
        gc.fillRoundRectangle(minRect.x, minRect.y, minRect.width, minRect.height, 6, 6);
        gc.drawRoundRectangle(minRect.x, minRect.y, minRect.width - 1, minRect.height - 1, 6, 6);
        if (!parent.getMinimized()) {
          gc.fillRectangle(x + 1, y + 1, 9, 3);
          gc.drawRectangle(x + 1, y + 1, 9, 3);
        } else {
          gc.fillRectangle(x + 1, y + 4, 5, 4);
          gc.fillRectangle(x + 3, y + 1, 5, 4);
          gc.drawRectangle(x + 1, y + 4, 5, 4);
          gc.drawRectangle(x + 3, y + 1, 5, 4);
          gc.drawLine(x + 4, y + 2, x + 7, y + 2);
          gc.drawLine(x + 2, y + 5, x + 5, y + 5);
        }
        break;
      }
    }
  }

  /*
   * Draw the unselected border for the receiver on the right.
   * 
   * @param gc
   */
  void drawRightUnselectedBorder(GC gc, Rectangle bounds, int state) {
    int x = bounds.x;
    int y = bounds.y;
    int width = bounds.width;
    int height = bounds.height;

    int[] shape = null;
    int startX = x + width - 1;

    if (parentonBottom) {
      int[] right = parentsimple ? SIMPLE_UNSELECTED_INNER_CORNER : BOTTOM_RIGHT_CORNER;

      shape = new int[right.length + 2];
      int index = 0;

      for (int i = 0; i < right.length / 2; i++) {
        shape[index++] = startX + right[2 * i];
        shape[index++] = y + height + right[2 * i + 1] - 1;
      }
      shape[index++] = startX;
      shape[index++] = y - 1;
    } else {
      int[] right = parentsimple ? SIMPLE_UNSELECTED_INNER_CORNER : TOP_RIGHT_CORNER;

      shape = new int[right.length + 2];
      int index = 0;

      for (int i = 0; i < right.length / 2; i++) {
        shape[index++] = startX + right[2 * i];
        shape[index++] = y + right[2 * i + 1];
      }

      shape[index++] = startX;
      shape[index++] = y + height;

    }

    drawBorder(gc, shape);

  }

  void drawSelected(int itemIndex, GC gc, Rectangle bounds, int state) {
    CTabItem item = parentitems()[itemIndex];
    int x = bounds.x;
    int y = bounds.y;
    int height = bounds.height;
    int width = bounds.width;
    if (!parentsimple && !parentsingle) {
      width -= (curveWidth - curveIndent);
    }
    int borderLeft = parentborderVisible ? 1 : 0;
    int borderRight = borderLeft;
    int borderTop = parentonBottom ? borderLeft : 0;
    int borderBottom = parentonBottom ? 0 : borderLeft;

    Point size = parent.getSize();

    int rightEdge = Math.min(x + width, parentgetRightItemEdge(gc));
    //   Draw selection border across all tabs

    if ((state & SWT.BACKGROUND) != 0) {
      int highlight_header = (parent.getStyle() & SWT.FLAT) != 0 ? 1 : 3;
      int xx = borderLeft;
      int tabHeight = parenttabHeight();
      int yy = parentonBottom ? size.y - borderBottom - tabHeight - highlight_header : borderTop
          + tabHeight + 1;
      int ww = size.x - borderLeft - borderRight;
      int hh = highlight_header - 1;
      int[] shape = new int[] {xx, yy, xx + ww, yy, xx + ww, yy + hh, xx, yy + hh};
      if (parentselectionGradientColors != null && !parentselectionGradientVertical) {
        drawBackground(gc, shape, true);
      } else {
        gc.setBackground(parentselectionBackground);
        gc.fillRectangle(xx, yy, ww, hh);
      }

      if (parentsingle) {
        if (!itemshowing(item)) {
          return;
        }
      } else {
        // if selected tab scrolled out of view or partially out of view
        // just draw bottom line
        if (!itemshowing(item)) {
          int x1 = Math.max(0, borderLeft - 1);
          int y1 = (parentonBottom) ? y - 1 : y + height;
          int x2 = size.x - borderRight;
          gc.setForeground(parent.getDisplay().getSystemColor(BORDER1_COLOR));
          gc.drawLine(x1, y1, x2, y1);
          return;
        }

        // draw selected tab background and outline
        shape = null;
        if (parentonBottom) {
          int[] left = parentsimple ? SIMPLE_BOTTOM_LEFT_CORNER : BOTTOM_LEFT_CORNER;
          int[] right = parentsimple ? SIMPLE_BOTTOM_RIGHT_CORNER : curve;
          if (borderLeft == 0 && itemIndex == parentfirstIndex()) {
            left = new int[] {x, y + height};
          }
          shape = new int[left.length + right.length + 8];
          int index = 0;
          shape[index++] = x; // first point repeated here because below we reuse shape to draw outline
          shape[index++] = y - 1;
          shape[index++] = x;
          shape[index++] = y - 1;
          for (int i = 0; i < left.length / 2; i++) {
            shape[index++] = x + left[2 * i];
            shape[index++] = y + height + left[2 * i + 1] - 1;
          }
          for (int i = 0; i < right.length / 2; i++) {
            shape[index++] = parentsimple ? rightEdge - 1 + right[2 * i] : rightEdge - curveIndent
                + right[2 * i];
            shape[index++] = parentsimple ? y + height + right[2 * i + 1] - 1 : y
                + right[2 * i + 1] - 2;
          }
          shape[index++] = parentsimple ? rightEdge - 1 : rightEdge + curveWidth - curveIndent;
          shape[index++] = y - 1;
          shape[index++] = parentsimple ? rightEdge - 1 : rightEdge + curveWidth - curveIndent;
          shape[index++] = y - 1;
        } else {
          int[] left = parentsimple ? SIMPLE_TOP_LEFT_CORNER : TOP_LEFT_CORNER;
          int[] right = parentsimple ? SIMPLE_TOP_RIGHT_CORNER : curve;
          if (borderLeft == 0 && itemIndex == parentfirstIndex()) {
            left = new int[] {x, y};
          }
          shape = new int[left.length + right.length + 8];
          int index = 0;
          shape[index++] = x; // first point repeated here because below we reuse shape to draw outline
          shape[index++] = y + height + 1;
          shape[index++] = x;
          shape[index++] = y + height + 1;
          for (int i = 0; i < left.length / 2; i++) {
            shape[index++] = x + left[2 * i];
            shape[index++] = y + left[2 * i + 1];
          }
          for (int i = 0; i < right.length / 2; i++) {
            shape[index++] = parentsimple ? rightEdge - 1 + right[2 * i] : rightEdge - curveIndent
                + right[2 * i];
            shape[index++] = y + right[2 * i + 1];
          }
          shape[index++] = parentsimple ? rightEdge - 1 : rightEdge + curveWidth - curveIndent;
          shape[index++] = y + height + 1;
          shape[index++] = parentsimple ? rightEdge - 1 : rightEdge + curveWidth - curveIndent;
          shape[index++] = y + height + 1;
        }

        Rectangle clipping = gc.getClipping();
        Rectangle clipBounds = item.getBounds();
        clipBounds.height += 1;
        if (parentonBottom) {
          clipBounds.y -= 1;
        }
        boolean tabInPaint = clipping.intersects(clipBounds);

        if (tabInPaint) {
          // fill in tab background
          if (parentselectionGradientColors != null && !parentselectionGradientVertical) {
            drawBackground(gc, shape, true);
          } else {
            Color defaultBackground = parentselectionBackground;
            Image image = parentselectionBgImage;
            Color[] colors = parentselectionGradientColors;
            int[] percents = parentselectionGradientPercents;
            boolean vertical = parentselectionGradientVertical;
            xx = x;
            yy = parentonBottom ? y - 1 : y + 1;
            ww = width;
            hh = height;
            if (!parentsingle && !parentsimple) {
              ww += curveWidth - curveIndent;
            }
            drawBackground(
                gc,
                shape,
                xx,
                yy,
                ww,
                hh,
                defaultBackground,
                image,
                colors,
                percents,
                vertical);
          }
        }

        //Highlight MUST be drawn before the outline so that outline can cover it in the right spots (start of swoop)
        //otherwise the curve looks jagged
        drawHighlight(gc, bounds, state, rightEdge);

        // draw outline
        shape[0] = Math.max(0, borderLeft - 1);
        if (borderLeft == 0 && itemIndex == parentfirstIndex()) {
          shape[1] = parentonBottom ? y + height - 1 : y;
          shape[5] = shape[3] = shape[1];
        }
        shape[shape.length - 2] = size.x - borderRight + 1;
        for (int i = 0; i < shape.length / 2; i++) {
          if (shape[2 * i + 1] == y + height + 1) {
            shape[2 * i + 1] -= 1;
          }
        }
        Color borderColor = parent.getDisplay().getSystemColor(BORDER1_COLOR);
        if (!borderColor.equals(lastBorderColor)) {
          createAntialiasColors();
        }
        antialias(shape, selectedInnerColor, selectedOuterColor, gc);
        gc.setForeground(borderColor);
        gc.drawPolyline(shape);

        if (!tabInPaint) {
          return;
        }
      }
    }

    if ((state & SWT.FOREGROUND) != 0) {
      // draw Image
      Rectangle trim = computeTrim(itemIndex, SWT.NONE, 0, 0, 0, 0);
      int xDraw = x - trim.x;
      if (parentsingle && (parentshowClose || itemshowClose(item))) {
        xDraw += itemcloseRect(item).width;
      }
      Image image = item.getImage();
      if (image != null) {
        Rectangle imageBounds = image.getBounds();
        // only draw image if it won't overlap with close button
        int maxImageWidth = rightEdge - xDraw - (trim.width + trim.x);
        if (!parentsingle && itemcloseRect(item).width > 0) {
          maxImageWidth -= itemcloseRect(item).width + INTERNAL_SPACING;
        }
        if (imageBounds.width < maxImageWidth) {
          int imageX = xDraw;
          int imageY = y + (height - imageBounds.height) / 2;
          imageY += parentonBottom ? -1 : 1;
          gc.drawImage(image, imageX, imageY);
          xDraw += imageBounds.width + INTERNAL_SPACING;
        }
      }

      // draw Text
      int textWidth = rightEdge - xDraw - (trim.width + trim.x);
      if (!parentsingle && itemcloseRect(item).width > 0) {
        textWidth -= itemcloseRect(item).width + INTERNAL_SPACING;
      }
      if (textWidth > 0) {
        Font gcFont = gc.getFont();
        gc.setFont(itemfont(item) == null ? parent.getFont() : itemfont(item));

        if (itemshortenedText(item) == null || itemshortenedTextWidth(item) != textWidth) {
          itemshortenedText(item, shortenText(gc, item.getText(), textWidth));
          itemshortenedTextWidth(item, textWidth);
        }
        Point extent = gc.textExtent(itemshortenedText(item), FLAGS);
        int textY = y + (height - extent.y) / 2;
        textY += parentonBottom ? -1 : 1;

        gc.setForeground(parentselectionForeground);
        gc.drawText(itemshortenedText(item), xDraw, textY, FLAGS);
        gc.setFont(gcFont);

        // draw a Focus rectangle
        if (parent.isFocusControl()) {
          Display display = parent.getDisplay();
          if (parentsimple || parentsingle) {
            gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
            gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
            gc.drawFocus(xDraw - 1, textY - 1, extent.x + 2, extent.y + 2);
          } else {
            gc.setForeground(display.getSystemColor(BUTTON_BORDER));
            gc.drawLine(xDraw, textY + extent.y + 1, xDraw + extent.x + 1, textY + extent.y + 1);
          }
        }
      }
      if (parentshowClose || itemshowClose(item)) {
        drawClose(gc, itemcloseRect(item), itemcloseImageState(item));
      }
    }
  }

  void drawTabArea(GC gc, Rectangle bounds, int state) {
    Point size = parent.getSize();
    int[] shape = null;
    Color borderColor = parent.getDisplay().getSystemColor(BORDER1_COLOR);
    int tabHeight = parenttabHeight();
    int style = parent.getStyle();

    int borderLeft = parentborderVisible ? 1 : 0;
    int borderRight = borderLeft;
    int borderTop = parentonBottom ? borderLeft : 0;
    int borderBottom = parentonBottom ? 0 : borderLeft;

    int selectedIndex = parentselectedIndex();
    int highlight_header = (style & SWT.FLAT) != 0 ? 1 : 3;
    if (tabHeight == 0) {
      if ((style & SWT.FLAT) != 0 && (style & SWT.BORDER) == 0) {
        return;
      }
      int x1 = borderLeft - 1;
      int x2 = size.x - borderRight;
      int y1 = parentonBottom ? size.y - borderBottom - highlight_header - 1 : borderTop
          + highlight_header;
      int y2 = parentonBottom ? size.y - borderBottom : borderTop;
      if (borderLeft > 0 && parentonBottom) {
        y2 -= 1;
      }

      shape = new int[] {x1, y1, x1, y2, x2, y2, x2, y1};

      // If horizontal gradient, show gradient across the whole area
      if (selectedIndex != -1 && parentselectionGradientColors != null
          && parentselectionGradientColors.length > 1 && !parentselectionGradientVertical) {
        drawBackground(gc, shape, true);
      } else if (selectedIndex == -1 && parentgradientColors != null
          && parentgradientColors.length > 1 && !parentgradientVertical) {
        drawBackground(gc, shape, false);
      } else {
        gc.setBackground(selectedIndex == -1 ? parent.getBackground() : parentselectionBackground);
        gc.fillPolygon(shape);
      }

      //draw 1 pixel border
      if (borderLeft > 0) {
        gc.setForeground(borderColor);
        gc.drawPolyline(shape);
      }
      return;
    }

    int x = Math.max(0, borderLeft - 1);
    int y = parentonBottom ? size.y - borderBottom - tabHeight : borderTop;
    int width = size.x - borderLeft - borderRight + 1;
    int height = tabHeight - 1;
    boolean simple = parentsimple;
    // Draw Tab Header
    if (parentonBottom) {
      int[] left, right;
      if ((style & SWT.BORDER) != 0) {
        left = simple ? SIMPLE_BOTTOM_LEFT_CORNER : BOTTOM_LEFT_CORNER;
        right = simple ? SIMPLE_BOTTOM_RIGHT_CORNER : BOTTOM_RIGHT_CORNER;
      } else {
        left = simple ? SIMPLE_BOTTOM_LEFT_CORNER_BORDERLESS : BOTTOM_LEFT_CORNER_BORDERLESS;
        right = simple ? SIMPLE_BOTTOM_RIGHT_CORNER_BORDERLESS : BOTTOM_RIGHT_CORNER_BORDERLESS;
      }
      shape = new int[left.length + right.length + 4];
      int index = 0;
      shape[index++] = x;
      shape[index++] = y - highlight_header;
      for (int i = 0; i < left.length / 2; i++) {
        shape[index++] = x + left[2 * i];
        shape[index++] = y + height + left[2 * i + 1];
        if (borderLeft == 0) {
          shape[index - 1] += 1;
        }
      }
      for (int i = 0; i < right.length / 2; i++) {
        shape[index++] = x + width + right[2 * i];
        shape[index++] = y + height + right[2 * i + 1];
        if (borderLeft == 0) {
          shape[index - 1] += 1;
        }
      }
      shape[index++] = x + width;
      shape[index++] = y - highlight_header;
    } else {
      int[] left, right;
      if ((style & SWT.BORDER) != 0) {
        left = simple ? SIMPLE_TOP_LEFT_CORNER : TOP_LEFT_CORNER;
        right = simple ? SIMPLE_TOP_RIGHT_CORNER : TOP_RIGHT_CORNER;
      } else {
        left = simple ? SIMPLE_TOP_LEFT_CORNER_BORDERLESS : TOP_LEFT_CORNER_BORDERLESS;
        right = simple ? SIMPLE_TOP_RIGHT_CORNER_BORDERLESS : TOP_RIGHT_CORNER_BORDERLESS;
      }
      shape = new int[left.length + right.length + 4];
      int index = 0;
      shape[index++] = x;
      shape[index++] = y + height + highlight_header + 1;
      for (int i = 0; i < left.length / 2; i++) {
        shape[index++] = x + left[2 * i];
        shape[index++] = y + left[2 * i + 1];
      }
      for (int i = 0; i < right.length / 2; i++) {
        shape[index++] = x + width + right[2 * i];
        shape[index++] = y + right[2 * i + 1];
      }
      shape[index++] = x + width;
      shape[index++] = y + height + highlight_header + 1;
    }
    // Fill in background
    boolean single = parentsingle;
    boolean bkSelected = single && selectedIndex != -1;
    drawBackground(gc, shape, bkSelected);
    // Fill in parent background for non-rectangular shape
    Region r = new Region();
    r.add(new Rectangle(x, y, width + 1, height + 1));
    r.subtract(shape);
    gc.setBackground(parent.getParent().getBackground());
    fillRegion(gc, r);
    r.dispose();

    // Draw selected tab
    if (selectedIndex == -1) {
      // if no selected tab - draw line across bottom of all tabs
      int x1 = borderLeft;
      int y1 = (parentonBottom) ? size.y - borderBottom - tabHeight - 1 : borderTop + tabHeight;
      int x2 = size.x - borderRight;
      gc.setForeground(borderColor);
      gc.drawLine(x1, y1, x2, y1);
    }

    // Draw border line
    if (borderLeft > 0) {
      if (!borderColor.equals(lastBorderColor)) {
        createAntialiasColors();
      }
      antialias(shape, null, tabAreaColor, gc);
      gc.setForeground(borderColor);
      gc.drawPolyline(shape);
    }
  }

  void drawUnselected(int index, GC gc, Rectangle bounds, int state) {
    CTabItem item = parentitems()[index];
    int x = bounds.x;
    int y = bounds.y;
    int height = bounds.height;
    int width = bounds.width;

    // Do not draw partial items
    if (!itemshowing(item)) {
      return;
    }

    Rectangle clipping = gc.getClipping();
    if (!clipping.intersects(bounds)) {
      return;
    }

    if ((state & SWT.BACKGROUND) != 0) {
      if (index > 0 && index < parentselectedIndex()) {
        drawLeftUnselectedBorder(gc, bounds, state);
      }
      // If it is the last one then draw a line
      if (index > parentselectedIndex()) {
        drawRightUnselectedBorder(gc, bounds, state);
      }
    }

    if ((state & SWT.FOREGROUND) != 0) {
      // draw Image
      Rectangle trim = computeTrim(index, SWT.NONE, 0, 0, 0, 0);
      int xDraw = x - trim.x;
      Image image = item.getImage();
      if (image != null && parentshowUnselectedImage) {
        Rectangle imageBounds = image.getBounds();
        // only draw image if it won't overlap with close button
        int maxImageWidth = x + width - xDraw - (trim.width + trim.x);
        if (parentshowUnselectedClose && (parentshowClose || itemshowClose(item))) {
          maxImageWidth -= itemcloseRect(item).width + INTERNAL_SPACING;
        }
        if (imageBounds.width < maxImageWidth) {
          int imageX = xDraw;
          int imageHeight = imageBounds.height;
          int imageY = y + (height - imageHeight) / 2;
          imageY += parentonBottom ? -1 : 1;
          int imageWidth = imageBounds.width * imageHeight / imageBounds.height;
          gc.drawImage(
              image,
              imageBounds.x,
              imageBounds.y,
              imageBounds.width,
              imageBounds.height,
              imageX,
              imageY,
              imageWidth,
              imageHeight);
          xDraw += imageWidth + INTERNAL_SPACING;
        }
      }
      // draw Text
      int textWidth = x + width - xDraw - (trim.width + trim.x);
      if (parentshowUnselectedClose && (parentshowClose || itemshowClose(item))) {
        textWidth -= itemcloseRect(item).width + INTERNAL_SPACING;
      }
      if (textWidth > 0) {
        Font gcFont = gc.getFont();
        gc.setFont(itemfont(item) == null ? parent.getFont() : itemfont(item));
        if (itemshortenedText(item) == null || itemshortenedTextWidth(item) != textWidth) {
          itemshortenedText(item, shortenText(gc, item.getText(), textWidth));
          itemshortenedTextWidth(item, textWidth);
        }
        Point extent = gc.textExtent(itemshortenedText(item), FLAGS);
        int textY = y + (height - extent.y) / 2;
        textY += parentonBottom ? -1 : 1;
        gc.setForeground(parent.getForeground());
        gc.drawText(itemshortenedText(item), xDraw, textY, FLAGS);
        gc.setFont(gcFont);
      }
      // draw close
      if (parentshowUnselectedClose && (parentshowClose || itemshowClose(item))) {
        drawClose(gc, itemcloseRect(item), itemcloseImageState(item));
      }
    }
  }

  void fillRegion(GC gc, Region region) {
    // NOTE: region passed in to this function will be modified
    Region clipping = new Region();
    gc.getClipping(clipping);
    region.intersect(clipping);
    gc.setClipping(region);
    gc.fillRectangle(region.getBounds());
    gc.setClipping(clipping);
    clipping.dispose();
  }

  Color getFillColor() {
    if (fillColor == null) {
      fillColor = getColor(parent.getDisplay(), CLOSE_FILL);
    }
    return fillColor;
  }

  /*
   * Return true if given start color, the cache of highlight colors we have
   * would match the highlight colors we'd compute.
   */
  boolean isSelectionHighlightColorsCacheHit(Color start) {

    if (selectionHighlightGradientColorsCache == null) {
      return false;
    }

    //this case should never happen but check to be safe before accessing array indexes
    if (selectionHighlightGradientColorsCache.length < 2) {
      return false;
    }

    Color highlightBegin = selectionHighlightGradientColorsCache[0];
    Color highlightEnd = selectionHighlightGradientColorsCache[selectionHighlightGradientColorsCache.length - 1];

    if (!highlightBegin.equals(start)) {
      return false;
    }

    //Compare number of colours we have vs. we'd compute
    if (selectionHighlightGradientColorsCache.length != parenttabHeight()) {
      return false;
    }

    //Compare existing highlight end to what it would be (selectionBackground)
    if (!highlightEnd.equals(parentselectionBackground)) {
      return false;
    }

    return true;
  }

  void setSelectionHighlightGradientColor(Color start) {
    //
    //Set to null to match all the early return cases.
    //For early returns, don't realloc the cache, we may get a cache hit next time we're given the highlight
    selectionHighlightGradientBegin = null;

    if (start == null) {
      return;
    }

    //don't bother on low colour
    if (parent.getDisplay().getDepth() < 15) {
      return;
    }

    //don't bother if we don't have a background gradient
    if (parentselectionGradientColors.length < 2) {
      return;
    }

    //OK we know its a valid gradient now
    selectionHighlightGradientBegin = start;

    if (!isSelectionHighlightColorsCacheHit(start)) {
      createSelectionHighlightGradientColors(start); //if no cache hit then compute new ones
    }
  }

  String shortenText(GC gc, String text, int width) {
    return useEllipses() ? shortenText(gc, text, width, ELLIPSIS)
        : shortenText(gc, text, width, ""); //$NON-NLS-1$
  }

  String shortenText(GC gc, String text, int width, String ellipses) {
    if (gc.textExtent(text, FLAGS).x <= width) {
      return text;
    }
    int ellipseWidth = gc.textExtent(ellipses, FLAGS).x;
    int length = text.length();
    TextLayout layout = new TextLayout(parent.getDisplay());
    layout.setText(text);
    int end = layout.getPreviousOffset(length, SWT.MOVEMENT_CLUSTER);
    while (end > 0) {
      text = text.substring(0, end);
      int l = gc.textExtent(text, FLAGS).x;
      if (l + ellipseWidth <= width) {
        break;
      }
      end = layout.getPreviousOffset(end, SWT.MOVEMENT_CLUSTER);
    }
    layout.dispose();
    return end == 0 ? text.substring(0, 1) : text + ellipses;
  }

  void updateCurves() {
    int tabHeight = parenttabHeight();
    if (tabHeight == lastTabHeight) {
      return;
    }
    if (parentonBottom) {
      int d = tabHeight - 12;
      curve = new int[] {
          0, 13 + d, 0, 12 + d, 2, 12 + d, 3, 11 + d, 5, 11 + d, 6, 10 + d, 7, 10 + d, 9, 8 + d,
          10, 8 + d, 11, 7 + d, 11 + d, 7, 12 + d, 6, 13 + d, 6, 15 + d, 4, 16 + d, 4, 17 + d, 3,
          19 + d, 3, 20 + d, 2, 22 + d, 2, 23 + d, 1};
      curveWidth = 26 + d;
      curveIndent = curveWidth / 3;
    } else {
      int d = tabHeight - 12;
      curve = new int[] {
          0, 0, 0, 1, 2, 1, 3, 2, 5, 2, 6, 3, 7, 3, 9, 5, 10, 5, 11, 6, 11 + d, 6 + d, 12 + d,
          7 + d, 13 + d, 7 + d, 15 + d, 9 + d, 16 + d, 9 + d, 17 + d, 10 + d, 19 + d, 10 + d,
          20 + d, 11 + d, 22 + d, 11 + d, 23 + d, 12 + d};
      curveWidth = 26 + d;
      curveIndent = curveWidth / 3;

      //this could be static but since values depend on curve, better to keep in one place
      topCurveHighlightStart = new int[] {
          0, 2, 1, 2, 2, 2, 3, 3, 4, 3, 5, 3, 6, 4, 7, 4, 8, 5, 9, 6, 10, 6};

      //also, by adding in 'd' here we save some math cost when drawing the curve
      topCurveHighlightEnd = new int[] {
          10 + d, 6 + d, 11 + d, 7 + d, 12 + d, 8 + d, 13 + d, 8 + d, 14 + d, 9 + d, 15 + d,
          10 + d, 16 + d, 10 + d, 17 + d, 11 + d, 18 + d, 11 + d, 19 + d, 11 + d, 20 + d, 12 + d,
          21 + d, 12 + d, 22 + d, 12 + d};
    }
  }

  /*
   * Return whether to use ellipses or just truncate labels
   */
  boolean useEllipses() {
    return parentsimple;
  }

  private void initialize() {
    parentgradientPercents = ReflectionUtils.getFieldObject(parent, "gradientPercents");
    parentselectionGradientPercents = ReflectionUtils.getFieldObject(
        parent,
        "selectionGradientPercents");
    parentselectionBackground = ReflectionUtils.getFieldObject(parent, "selectionBackground");
    parentselectionForeground = ReflectionUtils.getFieldObject(parent, "selectionForeground");
    parentselectionGradientColors = ReflectionUtils.getFieldObject(
        parent,
        "selectionGradientColors");
    parentgradientColors = ReflectionUtils.getFieldObject(parent, "gradientColors");
    parentselectionBgImage = ReflectionUtils.getFieldObject(parent, "selectionBgImage");
  }

  private int itemcloseImageState(CTabItem item) {
    return ReflectionUtils.getFieldObject(item, "closeImageState");
  }

  private Rectangle itemcloseRect(CTabItem item) {
    return ReflectionUtils.getFieldObject(item, "closeRect");
  }

  private Font itemfont(CTabItem item) {
    return ReflectionUtils.getFieldObject(item, "font");
  }

  private String itemshortenedText(CTabItem item) {
    return ReflectionUtils.getFieldObject(item, "shortenedText");
  }

  private void itemshortenedText(CTabItem item, String value) {
    ReflectionUtils.setField(item, "shortenedText", value);
  }

  private int itemshortenedTextWidth(CTabItem item) {
    return ReflectionUtils.getFieldObject(item, "shortenedTextWidth");
  }

  private void itemshortenedTextWidth(CTabItem item, int value) {
    ReflectionUtils.setField(item, "shortenedTextWidth", value);
  }

  private boolean itemshowClose(CTabItem item) {
    return ReflectionUtils.getFieldObject(item, "showClose");
  }

  private boolean itemshowing(CTabItem item) {
    return ReflectionUtils.getFieldObject(item, "showing");
  }

  private int parentfirstIndex() {
    return ReflectionUtils.getFieldObject(parent, "firstIndex");
  }

  private int parentgetRightItemEdge(GC gc) {
    return ReflectionUtils.invokeMethod(parent, "getRightItemEdge(org.eclipse.swt.graphics.GC)", gc);
  }

  private CTabItem[] parentitems() {
    return ReflectionUtils.getFieldObject(parent, "items");
  }

  private boolean parentminimized() {
    return ReflectionUtils.getFieldObject(parent, "minimized");
  }

  private int[] parentpriority() {
    return ReflectionUtils.getFieldObject(parent, "priority");
  }

  private int parentselectedIndex() {
    return ReflectionUtils.getFieldObject(parent, "selectedIndex");
  }

  private int parenttabHeight() {
    return ReflectionUtils.getFieldObject(parent, "tabHeight");
  }
}
