/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.tabletree;

import java.util.HashMap;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

/**
 * Helper class to handle images provided by this plug-in. NOTE: For internal use only. For images
 * used externally, please use the Shared***ImageHelper class instead.
 * 
 * @author amywu
 */
public class XMLEditorPluginImageHelper {
  private final String PLUGINID = XMLUIPlugin.ID;
  private static XMLEditorPluginImageHelper instance = null;
  public static final String EDITOR_MENU = "editor.xml_tabletree.menu"; //$NON-NLS-1$

  // save a descriptor for each image
  private static HashMap fImageDescRegistry = null;

  /**
   * Gets the instance.
   * 
   * @return Returns a XMLEditorPluginImageHelper
   */
  public synchronized static XMLEditorPluginImageHelper getInstance() {
    if (instance == null) {
      instance = new XMLEditorPluginImageHelper();
      initializeRegistry();
    }
    return instance;
  }

  private static void initializeRegistry() {
    // Taken from org.eclipse.ui.internal.WorkbenchImages
    Display d = Display.getCurrent();

    Image viewMenu = new Image(d, 11, 16);
    Image viewMenuMask = new Image(d, 11, 16);

    GC gc = new GC(viewMenu);
    GC maskgc = new GC(viewMenuMask);
    drawViewMenu(gc, maskgc);
    gc.dispose();
    maskgc.dispose();

    ImageData data = viewMenu.getImageData();
    data.transparentPixel = data.getPixel(0, 0);

    Image vm2 = new Image(d, viewMenu.getImageData(), viewMenuMask.getImageData());
    viewMenu.dispose();
    viewMenuMask.dispose();

    getImageRegistry().put(EDITOR_MENU, vm2);
    getImageDescriptorRegistry().put(EDITOR_MENU, ImageDescriptor.createFromImage(vm2));
  }

  private static void drawViewMenu(GC gc, GC maskgc) {
    Display display = Display.getCurrent();

    gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
    gc.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));

    int[] shapeArray = new int[] {1, 1, 10, 1, 6, 5, 5, 5};
    gc.fillPolygon(shapeArray);
    gc.drawPolygon(shapeArray);

    Color black = display.getSystemColor(SWT.COLOR_BLACK);
    Color white = display.getSystemColor(SWT.COLOR_WHITE);

    maskgc.setBackground(black);
    maskgc.fillRectangle(0, 0, 12, 16);

    maskgc.setBackground(white);
    maskgc.setForeground(white);
    maskgc.fillPolygon(shapeArray);
    maskgc.drawPolygon(shapeArray);
  }

  /**
   * Retrieves the image associated with resource from the image registry. If the image cannot be
   * retrieved, attempt to find and load the image at the location specified in resource.
   * 
   * @param resource the image to retrieve
   * @return Image the image associated with resource or null if one could not be found
   */
  public Image getImage(String resource) {
    Image image = getImageRegistry().get(resource);
    if (image == null) {
      // create an image
      image = createImage(resource);
    }
    return image;
  }

  /**
   * Retrieves the image descriptor associated with resource from the image descriptor registry. If
   * the image descriptor cannot be retrieved, attempt to find and load the image descriptor at the
   * location specified in resource.
   * 
   * @param resource the image descriptor to retrieve
   * @return ImageDescriptor the image descriptor assocated with resource or the default "missing"
   *         image descriptor if one could not be found
   */
  public ImageDescriptor getImageDescriptor(String resource) {
    ImageDescriptor imageDescriptor = null;
    Object o = getImageDescriptorRegistry().get(resource);
    if (o == null) {
      // create a descriptor
      imageDescriptor = createImageDescriptor(resource);
    } else {
      imageDescriptor = (ImageDescriptor) o;
    }
    return imageDescriptor;
  }

  /**
   * Returns the image descriptor registry for this plugin.
   * 
   * @return HashMap - image descriptor registry for this plugin
   */
  private static HashMap getImageDescriptorRegistry() {
    if (fImageDescRegistry == null) {
      fImageDescRegistry = new HashMap();
    }
    return fImageDescRegistry;
  }

  /**
   * Returns the image registry for this plugin.
   * 
   * @return ImageRegistry - image registry for this plugin
   */
  private static ImageRegistry getImageRegistry() {
    return JFaceResources.getImageRegistry();
  }

  /**
   * Creates an image from the given resource and adds the image to the image registry.
   * 
   * @param resource
   * @return Image
   */
  private Image createImage(String resource) {
    ImageDescriptor desc = getImageDescriptor(resource);
    Image image = null;

    if (desc != null) {
      image = desc.createImage();
      // dont add the missing image descriptor image to the image
      // registry
      if (!desc.equals(ImageDescriptor.getMissingImageDescriptor())) {
        getImageRegistry().put(resource, image);
      }
    }
    return image;
  }

  /**
   * Creates an image descriptor from the given imageFilePath and adds the image descriptor to the
   * image descriptor registry. If an image descriptor could not be created, the default "missing"
   * image descriptor is returned but not added to the image descriptor registry.
   * 
   * @param imageFilePath
   * @return ImageDescriptor image descriptor for imageFilePath or default "missing" image
   *         descriptor if resource could not be found
   */
  private ImageDescriptor createImageDescriptor(String imageFilePath) {
    ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGINID,
        imageFilePath);
    if (imageDescriptor != null) {
      getImageDescriptorRegistry().put(imageFilePath, imageDescriptor);
    } else {
      imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
    }

    return imageDescriptor;
  }
}
