/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.internal.ui.panels;

import com.xored.glance.internal.ui.GlancePlugin;
import com.xored.glance.ui.utils.UIUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import java.net.URL;

public abstract class ImageAnimation extends Thread {

  private final ImageLoader loader = new ImageLoader();

  private ImageData[] imageDataArray;

  private final Color bg;

  public ImageAnimation(URL url, Color bg) throws CoreException {
    setDaemon(true);
    this.bg = bg;
    try {
      imageDataArray = loader.load(url.openStream());
    } catch (Exception e) {
      throw new CoreException(GlancePlugin.createStatus(
          "Can't read image '" + url.toString() + "'",
          e));
    }
    if (imageDataArray == null || imageDataArray.length <= 1) {
      throw new CoreException(GlancePlugin.createStatus("Image '" + url.toString()
          + "' is not an animated image"));
    }
  }

  @Override
  public void run() {
    Display display = PlatformUI.getWorkbench().getDisplay();
    /*
     * Create an off-screen image to draw on, and fill it with the shell background.
     */
    final Image offScreenImage = new Image(
        display,
        loader.logicalScreenWidth,
        loader.logicalScreenHeight);
    GC offScreenImageGC = new GC(offScreenImage);
    offScreenImageGC.setBackground(bg);
    offScreenImageGC.fillRectangle(0, 0, loader.logicalScreenWidth, loader.logicalScreenHeight);

    Image image = null;

    try {
      /* Create the first image and draw it on the off-screen image. */
      int imageDataIndex = 0;
      ImageData imageData = imageDataArray[imageDataIndex];
      image = new Image(display, imageData);
      offScreenImageGC.drawImage(
          image,
          0,
          0,
          imageData.width,
          imageData.height,
          imageData.x,
          imageData.y,
          imageData.width,
          imageData.height);

      /*
       * Now loop through the images, creating and drawing each one on the off-screen image before
       * drawing it on the shell.
       */
      int repeatCount = loader.repeatCount;
      while ((loader.repeatCount == 0 || repeatCount > 0) && !isTerminated()) {
        switch (imageData.disposalMethod) {
          case SWT.DM_FILL_BACKGROUND:
            /* Fill with the background color before drawing. */
            offScreenImageGC.setBackground(bg);
            offScreenImageGC.fillRectangle(
                imageData.x,
                imageData.y,
                imageData.width,
                imageData.height);
            break;
          case SWT.DM_FILL_PREVIOUS:
            /* Restore the previous image before drawing. */
            offScreenImageGC.drawImage(
                image,
                0,
                0,
                imageData.width,
                imageData.height,
                imageData.x,
                imageData.y,
                imageData.width,
                imageData.height);
            break;
        }

        imageDataIndex = (imageDataIndex + 1) % imageDataArray.length;
        imageData = imageDataArray[imageDataIndex];
        image.dispose();
        image = new Image(display, imageData);
        offScreenImageGC.drawImage(
            image,
            0,
            0,
            imageData.width,
            imageData.height,
            imageData.x,
            imageData.y,
            imageData.width,
            imageData.height);
        final Image newImage = image;

        UIUtils.syncExec(display, new Runnable() {
          @Override
          public void run() {
            updateImage(newImage);
          }
        });

        /*
         * Sleep for the specified delay time (adding commonly-used slow-down fudge factors).
         */
        try {
          int ms = imageData.delayTime * 10;
          if (ms < 20) {
            ms += 30;
          }
          if (ms < 30) {
            ms += 10;
          }
          Thread.sleep(ms);
        } catch (InterruptedException e) {
        }

        /*
         * If we have just drawn the last image, decrement the repeat count and start again.
         */
        if (imageDataIndex == imageDataArray.length - 1) {
          repeatCount--;
        }
      }
    } catch (SWTException ex) {
      GlancePlugin.log("There was an error animating the GIF", ex);
    } finally {
      if (offScreenImage != null && !offScreenImage.isDisposed()) {
        offScreenImage.dispose();
      }
      if (offScreenImageGC != null && !offScreenImageGC.isDisposed()) {
        offScreenImageGC.dispose();
      }
      if (image != null && !image.isDisposed()) {
        image.dispose();
      }
    }
  }

  protected abstract boolean isTerminated();

  protected abstract void updateImage(Image image);

}
