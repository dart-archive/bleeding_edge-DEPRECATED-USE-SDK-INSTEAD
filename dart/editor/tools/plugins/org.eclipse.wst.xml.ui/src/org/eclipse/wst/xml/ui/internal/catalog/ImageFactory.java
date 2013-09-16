/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.catalog;

import java.util.Hashtable;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

public class ImageFactory {
  public static final int TOP_LEFT = 1;
  public static final int TOP_RIGHT = 2;
  public static final int BOTTOM_LEFT = 3;
  public static final int BOTTOM_RIGHT = 4;

  protected static final int[][] OVERLAY_COORDINATE = { {0, 0}, {0, 2}, {2, 0}, {2, 2}};

  protected static ImageFactory INSTANCE = new ImageFactory();

  protected Hashtable compositeImageTable = new Hashtable();

  public ImageFactory() {
    super();
  }

  public Image getImage(String iconName) {
    ImageRegistry imageRegistry = XMLUIPlugin.getDefault().getImageRegistry();

    if (imageRegistry.get(iconName) != null) {
      return imageRegistry.get(iconName);
    } else {
      imageRegistry.put(iconName, ImageDescriptor.createFromFile(getClass(), iconName));
      return imageRegistry.get(iconName);
    }
  }

  public Image createCompositeImage(Image base, Image overlay, int overlayPosition) {
    String key = base + "*" + overlay + "*" + overlayPosition; //$NON-NLS-1$ //$NON-NLS-2$
    Image result = (Image) compositeImageTable.get(key);
    if (result == null) {
      ImageDescriptor overlays[][] = new ImageDescriptor[3][3];
      int[] coord = OVERLAY_COORDINATE[overlayPosition];
      overlays[coord[1]][coord[0]] = new ImageBasedImageDescriptor(overlay);
      OverlayIcon icon = new OverlayIcon(new ImageBasedImageDescriptor(base), overlays, new Point(
          16, 16));
      result = icon.createImage();
      compositeImageTable.put(key, result);
    }
    return result;
  }

  public static ImageDescriptor createImageDescriptorWrapper(Image image) {
    return new ImageBasedImageDescriptor(image);
  }

  /**
   * An OverlayIcon consists of a main icon and several adornments.
   */
  class OverlayIcon extends CompositeImageDescriptor {

    static final int DEFAULT_WIDTH = 22;

    static final int DEFAULT_HEIGHT = 16;

    private Point fSize = null;

    private ImageDescriptor fBase;

    private ImageDescriptor fOverlays[][];

    public OverlayIcon(ImageDescriptor base, ImageDescriptor[][] overlays, Point size) {
      fBase = base;
      fOverlays = overlays;
      fSize = size;
    }

    protected void drawBottomLeft(ImageDescriptor[] overlays) {
      if (overlays == null) {
        return;
      }
      int length = overlays.length;
      int x = 0;
      for (int i = 0; i < 3; i++) {
        if ((i < length) && (overlays[i] != null)) {
          ImageData id = overlays[i].getImageData();
          drawImage(id, x, getSize().y - id.height);
          x += id.width;
        }
      }
    }

    protected void drawBottomRight(ImageDescriptor[] overlays) {
      if (overlays == null) {
        return;
      }
      int length = overlays.length;
      int x = getSize().x;
      for (int i = 2; i >= 0; i--) {
        if ((i < length) && (overlays[i] != null)) {
          ImageData id = overlays[i].getImageData();
          x -= id.width;
          drawImage(id, x, getSize().y - id.height);
        }
      }
    }

    /**
     * @see CompositeImageDescriptor#drawCompositeImage(int, int)
     */
    protected void drawCompositeImage(int width, int height) {
      ImageData bg;
      if ((fBase == null) || ((bg = fBase.getImageData()) == null)) {
        bg = DEFAULT_IMAGE_DATA;
      }
      drawImage(bg, 0, 0);

      if (fOverlays != null) {
        if (fOverlays.length > 0) {
          drawTopRight(fOverlays[0]);
        }

        if (fOverlays.length > 1) {
          drawBottomRight(fOverlays[1]);
        }

        if (fOverlays.length > 2) {
          drawBottomLeft(fOverlays[2]);
        }

        if (fOverlays.length > 3) {
          drawTopLeft(fOverlays[3]);
        }
      }
    }

    protected void drawTopLeft(ImageDescriptor[] overlays) {
      if (overlays == null) {
        return;
      }
      int length = overlays.length;
      int x = 0;
      for (int i = 0; i < 3; i++) {
        if ((i < length) && (overlays[i] != null)) {
          ImageData id = overlays[i].getImageData();
          drawImage(id, x, 0);
          x += id.width;
        }
      }
    }

    protected void drawTopRight(ImageDescriptor[] overlays) {
      if (overlays == null) {
        return;
      }
      int length = overlays.length;
      int x = getSize().x;
      for (int i = 2; i >= 0; i--) {
        if ((i < length) && (overlays[i] != null)) {
          ImageData id = overlays[i].getImageData();
          x -= id.width;
          drawImage(id, x, 0);
        }
      }
    }

    /**
     * @see CompositeImageDescriptor#getSize()
     */
    protected Point getSize() {
      return fSize;
    }
  }

  static class ImageBasedImageDescriptor extends ImageDescriptor {
    protected Image image;

    public ImageBasedImageDescriptor(Image image) {
      this.image = image;
    }

    public ImageData getImageData() {
      return image.getImageData();
    }
  }
}
