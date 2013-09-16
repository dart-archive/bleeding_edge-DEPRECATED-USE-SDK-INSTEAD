/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.text.hover;

import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSPrimitiveValue;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.RGBColor;

public class CSSColorHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

  private IInformationControlCreator fInformationControlCreator;

  public CSSColorHover() {

  }

  public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
    final IDocument document = textViewer.getDocument();
    if (document instanceof IStructuredDocument) {
      IStructuredModel model = StructuredModelManager.getModelManager().getModelForRead(
          (IStructuredDocument) document);
      try {
        final IndexedRegion region = model.getIndexedRegion(hoverRegion.getOffset());
        if (region instanceof ICSSPrimitiveValue) {
          return getColorValue((ICSSPrimitiveValue) region);
        }
      } finally {
        if (model != null)
          model.releaseFromRead();
      }
    }
    return null;
  }

  public IInformationControlCreator getHoverControlCreator() {
    if (fInformationControlCreator == null) {
      fInformationControlCreator = new CSSColorInformationControlCreator();
    }
    return fInformationControlCreator;
  }

  public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
    Object result = getHoverInfo2(textViewer, hoverRegion);
    return result != null ? result.toString() : null;
  }

  /**
   * Gets an {@link RGB} color value from the primitive value
   * 
   * @param value the primitive CSS value
   * @return an RGB value if it can be extracted from the primitive
   */
  private RGB getColorValue(ICSSPrimitiveValue value) {
    if (value.getParentNode() instanceof RGBColor) {
      value = (ICSSPrimitiveValue) value.getParentNode();
    }
    RGB rgb = null;
    switch (value.getPrimitiveType()) {
      case CSSPrimitiveValue.CSS_RGBCOLOR:
        rgb = getRGB((RGBColor) value);
        break;
      case ICSSPrimitiveValue.CSS_HASH:
        rgb = getRGBFromHex(value.getStringValue());
        break;
      case CSSPrimitiveValue.CSS_IDENT:
        rgb = CSSColorNames.getInstance().getRGB(value.getStringValue());
        break;
    }
    return rgb;
  }

  /**
   * Converts a hex string (either 3-digit or 6-digit notation) into an {@link RGB}. Any invalid hex
   * color will result in a null RGB value.
   * 
   * @param hex rgb value in hexadecimal notation (3- or 6-digit)
   * @return an {@link RGB} based on the hexadecimal value. <code>null</code> if the rgb value is
   *         invalid
   */
  private RGB getRGBFromHex(String hex) {
    try {
      final int length = hex.length();
      if (length == 4) { // convert 3-digit notation
        final int r = Integer.parseInt(hex.substring(1, 2), 16);
        final int g = Integer.parseInt(hex.substring(2, 3), 16);
        final int b = Integer.parseInt(hex.substring(3, 4), 16);
        return new RGB((r << 4) | r, (g << 4) | g, (b << 4) | b);
      } else if (length == 7) { // convert 6-digit notation
        return new RGB(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(
            hex.substring(3, 5), 16), Integer.parseInt(hex.substring(5, 7), 16));
      }
    } catch (NumberFormatException e) {
    } // Invalid hexcode used
    return null;
  }

  /**
   * Provides an {@link RGB} based on an {@link RGBColor}
   * 
   * @param color The {@link RGBColor} to extra RGB information from
   * @return an {@link RGB} based on an {@link RGBColor}
   */
  private RGB getRGB(RGBColor color) {
    final int red = getColorInt(color.getRed());
    final int green = getColorInt(color.getGreen());
    final int blue = getColorInt(color.getBlue());
    if (red >= 0 && green >= 0 && blue >= 0)
      return new RGB(red, green, blue);
    return null;
  }

  private int getColorInt(CSSPrimitiveValue value) {
    if (value.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE) { // Percentage of 255
      final float percentage = value.getFloatValue(CSSPrimitiveValue.CSS_NUMBER);
      return capIntValue((int) (percentage / 100 * 255));
    } else if (value.getPrimitiveType() == ICSSPrimitiveValue.CSS_INTEGER) {
      return capIntValue((int) value.getFloatValue(CSSPrimitiveValue.CSS_NUMBER));
    }
    return -1;
  }

  /**
   * Caps the integer value between the ranges of 0 and 255, inclusive.
   * 
   * @param value integer value to cap
   * @return a value between 0 and 255, inclusive
   */
  private int capIntValue(int value) {
    if (value > 255)
      value = 255;
    else if (value < 0)
      value = 0;
    return value;
  }

  public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
    final IDocument document = textViewer.getDocument();
    if (document instanceof IStructuredDocument) {
      IStructuredModel model = StructuredModelManager.getModelManager().getModelForRead(
          (IStructuredDocument) document);
      try {
        IndexedRegion region = model.getIndexedRegion(offset);
        if (region instanceof ICSSPrimitiveValue) {
          // Shift the hover region to the rgb() function instead of the individual numbers
          if (((ICSSPrimitiveValue) region).getParentNode() instanceof RGBColor) {
            region = (IndexedRegion) ((ICSSPrimitiveValue) region).getParentNode();
          }
        }
        if (region != null) {
          return new Region(region.getStartOffset(), region.getLength());
        }
      } finally {
        if (model != null)
          model.releaseFromRead();
      }
    }
    return null;
  }

  private class CSSColorInformationControlCreator extends AbstractReusableInformationControlCreator {

    protected IInformationControl doCreateInformationControl(Shell parent) {
      return new CSSColorInformationControl(parent);
    }

  }

  /**
   * An information control that simply displays color information. The control takes an {@link RGB}
   * input value and sets its background to the corresponding color.
   */
  private class CSSColorInformationControl extends AbstractInformationControl implements
      IInformationControlExtension2 {

    private Color fColor;

    CSSColorInformationControl(Shell shell) {
      super(shell, false);
      create();
    }

    public Point computeSizeConstraints(int widthInChars, int heightInChars) {
      return new Point(50, 50);
    }

    public boolean hasContents() {
      return fColor != null;
    }

    protected void createContent(Composite parent) {
    }

    public void setInformation(String information) {
    }

    public void dispose() {
      if (fColor != null) {
        fColor.dispose();
        fColor = null;
      }
      super.dispose();
    }

    public void setInput(Object input) {
      if (input instanceof RGB) {
        RGB rgb = (RGB) input;
        if (fColor == null || !rgb.equals(fColor.getRGB())) {
          if (fColor != null) { // Cleanup any old color
            fColor.dispose();
          }
          fColor = new Color(getShell().getDisplay(), rgb);
          setBackgroundColor(fColor);
        }
      }
    }

    public IInformationControlCreator getInformationPresenterControlCreator() {
      return new CSSColorInformationControlCreator();
    }

  }
}
