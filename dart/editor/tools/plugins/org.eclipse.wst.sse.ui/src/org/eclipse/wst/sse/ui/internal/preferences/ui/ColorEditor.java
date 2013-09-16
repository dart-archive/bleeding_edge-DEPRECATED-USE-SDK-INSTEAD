/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.preferences.ui;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * A "button" of a certain color determined by the color picker.
 */
public class ColorEditor {
  Button fButton;
  Color fColor;
  RGB fColorValue;

  private Point fExtent;
  Image fImage;

  public ColorEditor(Composite parent) {

    fButton = new Button(parent, SWT.PUSH);
    fExtent = computeImageSize(parent);
    fImage = new Image(parent.getDisplay(), fExtent.x, fExtent.y);

    GC gc = new GC(fImage);
    gc.setBackground(fButton.getBackground());
    gc.fillRectangle(0, 0, fExtent.x, fExtent.y);
    gc.dispose();

    fButton.setImage(fImage);

    // bug2541 - associate color value to button's value field
    fButton.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
      /**
       * @see org.eclipse.swt.accessibility.AccessibleControlAdapter#getValue(AccessibleControlEvent)
       */
      public void getValue(AccessibleControlEvent e) {
        if (e.childID == ACC.CHILDID_SELF) {
          if (getColorValue() != null)
            e.result = getColorValue().toString();
          else
            e.result = null;
        }
      }
    });

    fButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        ColorDialog colorDialog = new ColorDialog(fButton.getShell());
        colorDialog.setRGB(fColorValue);
        RGB newColor = colorDialog.open();
        if (newColor != null) {
          fColorValue = newColor;
          updateColorImage();
        }
      }
    });

    fButton.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent event) {
        if (fImage != null) {
          fImage.dispose();
          fImage = null;
        }
        if (fColor != null) {
          fColor.dispose();
          fColor = null;
        }
      }
    });
  }

  protected Point computeImageSize(Control window) {
    GC gc = new GC(window);
    Font f = JFaceResources.getFontRegistry().get(JFaceResources.DEFAULT_FONT);
    gc.setFont(f);
    int height = gc.getFontMetrics().getHeight();
    gc.dispose();
    Point p = new Point(height * 3 - 6, height);
    return p;
  }

  public Button getButton() {
    return fButton;
  }

  public RGB getColorValue() {
    return fColorValue;
  }

  public void setColorValue(RGB rgb) {
    fColorValue = rgb;
    updateColorImage();
  }

  protected void updateColorImage() {

    Display display = fButton.getDisplay();

    GC gc = new GC(fImage);
    gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
    gc.drawRectangle(0, 2, fExtent.x - 1, fExtent.y - 4);

    if (fColor != null)
      fColor.dispose();

    fColor = new Color(display, fColorValue);
    gc.setBackground(fColor);
    gc.fillRectangle(1, 3, fExtent.x - 2, fExtent.y - 5);
    gc.dispose();

    fButton.setImage(fImage);
  }
}
