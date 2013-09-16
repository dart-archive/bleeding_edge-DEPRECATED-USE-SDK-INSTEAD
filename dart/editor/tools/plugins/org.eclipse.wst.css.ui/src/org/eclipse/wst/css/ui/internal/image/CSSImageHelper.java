/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.css.ui.internal.CSSUIPlugin;
import org.eclipse.wst.css.ui.internal.editor.CSSEditorPluginImages;

import java.util.HashMap;
import java.util.Map;

public class CSSImageHelper {
  private static CSSImageHelper fInstance = null;

  /**
   * singleton
   */
  public synchronized static CSSImageHelper getInstance() {
    if (fInstance == null) {
      fInstance = new CSSImageHelper();
    }
    return fInstance;
  }

  private HashMap fImageDescRegistry = null; // save a descriptor for each image
  private Map fTypeMap = null;

  /**
	 * 
	 */
  private CSSImageHelper() {
    super();
  }

  /**
	 * 
	 */
  private Image createImage(String resource) {
    ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(CSSUIPlugin.ID, resource);
    Image image = null;

    if (desc == null) {
      desc = ImageDescriptor.getMissingImageDescriptor();
      image = desc.createImage();
    } else {
      image = desc.createImage();
      getImageRegistry().put(resource, image);
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
    ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(CSSUIPlugin.ID,
        imageFilePath);
    if (imageDescriptor != null) {
      getImageDescriptorRegistry().put(imageFilePath, imageDescriptor);
    } else {
      imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
    }

    return imageDescriptor;
  }

  /**
   * by type
   */
  public Image getImage(CSSImageType type) {
    if (fTypeMap == null) {
      fTypeMap = new HashMap();
      fTypeMap.put(CSSImageType.STYLESHEET, CSSEditorPluginImages.IMG_OBJ_STYLESHEET);

      fTypeMap.put(CSSImageType.RULE_CHARSET, CSSEditorPluginImages.IMG_OBJ_RULE_CHARSET);
      fTypeMap.put(CSSImageType.RULE_FONTFACE, CSSEditorPluginImages.IMG_OBJ_RULE_FONTFACE);
      fTypeMap.put(CSSImageType.RULE_IMPORT, CSSEditorPluginImages.IMG_OBJ_RULE_IMPORT);
      fTypeMap.put(CSSImageType.RULE_MEDIA, CSSEditorPluginImages.IMG_OBJ_RULE_MEDIA);
      fTypeMap.put(CSSImageType.RULE_PAGE, CSSEditorPluginImages.IMG_OBJ_RULE_PAGE);
      fTypeMap.put(CSSImageType.RULE_STYLE, CSSEditorPluginImages.IMG_OBJ_RULE_STYLE);
      fTypeMap.put(CSSImageType.RULE_UNKNOWN, CSSEditorPluginImages.IMG_OBJ_RULE_UNKNOWN);

      fTypeMap.put(CSSImageType.SELECTOR_CLASS, CSSEditorPluginImages.IMG_OBJ_SELECTOR_CLASS);
      fTypeMap.put(CSSImageType.SELECTOR_ID, CSSEditorPluginImages.IMG_OBJ_SELECTOR_ID);
      fTypeMap.put(CSSImageType.SELECTOR_PSEUDO, CSSEditorPluginImages.IMG_OBJ_SELECTOR_PSEUDO);
      fTypeMap.put(CSSImageType.SELECTOR_TAG, CSSEditorPluginImages.IMG_OBJ_SELECTOR_TAG);
      fTypeMap.put(CSSImageType.SELECTOR_LINK, CSSEditorPluginImages.IMG_OBJ_SELECTOR_LINK);
      fTypeMap.put(CSSImageType.SELECTOR_DEFAULT, CSSEditorPluginImages.IMG_OBJ_SELECTOR_DEFAULT);

      fTypeMap.put(CSSImageType.VALUE_FUNCTION, CSSEditorPluginImages.IMG_OBJ_VALUE_FUNCTION);
      fTypeMap.put(CSSImageType.VALUE_NUMBER, CSSEditorPluginImages.IMG_OBJ_VALUE_NUMBER);
      fTypeMap.put(CSSImageType.VALUE_STRING, CSSEditorPluginImages.IMG_OBJ_VALUE_STRING);

      fTypeMap.put(CSSImageType.CATEGORY_AURAL, CSSEditorPluginImages.IMG_OBJ_CATEGORY_AURAL);
      fTypeMap.put(CSSImageType.CATEGORY_BOX, CSSEditorPluginImages.IMG_OBJ_CATEGORY_BOX);
      fTypeMap.put(CSSImageType.CATEGORY_COLORANDBACKGROUND,
          CSSEditorPluginImages.IMG_OBJ_CATEGORY_COLORANDBACKGROUND);
      fTypeMap.put(CSSImageType.CATEGORY_CONTENT, CSSEditorPluginImages.IMG_OBJ_CATEGORY_CONTENT);
      fTypeMap.put(CSSImageType.CATEGORY_FONT, CSSEditorPluginImages.IMG_OBJ_CATEGORY_FONT);
      fTypeMap.put(CSSImageType.CATEGORY_PAGE, CSSEditorPluginImages.IMG_OBJ_CATEGORY_PAGE);
      fTypeMap.put(CSSImageType.CATEGORY_TABLES, CSSEditorPluginImages.IMG_OBJ_CATEGORY_TABLES);
      fTypeMap.put(CSSImageType.CATEGORY_TEXT, CSSEditorPluginImages.IMG_OBJ_CATEGORY_TEXT);
      fTypeMap.put(CSSImageType.CATEGORY_UI, CSSEditorPluginImages.IMG_OBJ_CATEGORY_UI);
      fTypeMap.put(CSSImageType.CATEGORY_VISUAL, CSSEditorPluginImages.IMG_OBJ_CATEGORY_VISUAL);
      fTypeMap.put(CSSImageType.CATEGORY_DEFAULT, CSSEditorPluginImages.IMG_OBJ_CATEGORY_DEFAULT);
    }
    return getImage((String) fTypeMap.get(type));
  }

  /**
   * by relative path(from here)
   */
  public Image getImage(String resource) {
    if (resource == null) {
      return null;
    }
    Image image = getImageRegistry().get(resource);
    if (image == null) {
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
  private HashMap getImageDescriptorRegistry() {
    if (fImageDescRegistry == null)
      fImageDescRegistry = new HashMap();
    return fImageDescRegistry;
  }

  private ImageRegistry getImageRegistry() {
    return JFaceResources.getImageRegistry();
  }
}
