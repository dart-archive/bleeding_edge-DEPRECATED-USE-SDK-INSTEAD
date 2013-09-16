/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.util;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;

public class SharedXMLEditorPluginImageHelper {
  public static final String IMG_DTOOL_CONSTRAINOFF = XMLEditorPluginImages.IMG_DTOOL_CONSTRAINOFF;
  public static final String IMG_DTOOL_CONSTRAINON = XMLEditorPluginImages.IMG_DTOOL_CONSTRAINON;
  public static final String IMG_DTOOL_RLDGRMR = XMLEditorPluginImages.IMG_DTOOL_RLDGRMR;
  public static final String IMG_DTOOL_VALIDATE = XMLEditorPluginImages.IMG_DTOOL_VALIDATE;
  public static final String IMG_ETOOL_CONSTRAINOFF = XMLEditorPluginImages.IMG_ETOOL_CONSTRAINOFF;
  public static final String IMG_ETOOL_CONSTRAINON = XMLEditorPluginImages.IMG_ETOOL_CONSTRAINON;
  public static final String IMG_ETOOL_RLDGRMR = XMLEditorPluginImages.IMG_ETOOL_RLDGRMR;
  public static final String IMG_ETOOL_VALIDATE = XMLEditorPluginImages.IMG_ETOOL_VALIDATE;
  public static final String IMG_OBJ_ATTRIBUTE = XMLEditorPluginImages.IMG_OBJ_ATTRIBUTE;
  public static final String IMG_OBJ_CDATASECTION = XMLEditorPluginImages.IMG_OBJ_CDATASECTION;
  public static final String IMG_OBJ_COMMENT = XMLEditorPluginImages.IMG_OBJ_COMMENT;
  public static final String IMG_OBJ_DOCTYPE = XMLEditorPluginImages.IMG_OBJ_DOCTYPE;
  public static final String IMG_OBJ_ELEMENT = XMLEditorPluginImages.IMG_OBJ_ELEMENT;
  public static final String IMG_OBJ_ENTITY = XMLEditorPluginImages.IMG_OBJ_ENTITY;
  public static final String IMG_OBJ_ENTITY_REFERENCE = XMLEditorPluginImages.IMG_OBJ_ENTITY_REFERENCE;
  public static final String IMG_OBJ_NOTATION = XMLEditorPluginImages.IMG_OBJ_NOTATION;
  public static final String IMG_OBJ_PROCESSINGINSTRUCTION = XMLEditorPluginImages.IMG_OBJ_PROCESSINGINSTRUCTION;
  public static final String IMG_OBJ_TAG_GENERIC = XMLEditorPluginImages.IMG_OBJ_TAG_GENERIC;
  public static final String IMG_OBJ_TAG_MACRO = XMLEditorPluginImages.IMG_OBJ_TAG_MACRO;
  public static final String IMG_OBJ_TXTEXT = XMLEditorPluginImages.IMG_OBJ_TXTEXT;

  /**
   * Retrieves the specified image from the xml source editor plugin's image registry. Note: The
   * returned <code>Image</code> is managed by the workbench; clients must <b>not </b> dispose of
   * the returned image.
   * 
   * @param symbolicName the symbolic name of the image; there are constants declared in this class
   *          for build-in images that come with the xml source editor
   * @return the image, or <code>null</code> if not found
   */
  public static Image getImage(String symbolicName) {
    return XMLEditorPluginImageHelper.getInstance().getImage(symbolicName);
  }

  /**
   * Retrieves the image descriptor for specified image from the xml source editor plugin's image
   * registry. Unlike <code>Image</code>s, image descriptors themselves do not need to be disposed.
   * 
   * @param symbolicName the symbolic name of the image; there are constants declared in this
   *          interface for build-in images that come with the xml source editor
   * @return the image descriptor, or <code>null</code> if not found
   */
  public static ImageDescriptor getImageDescriptor(String symbolicName) {
    return XMLEditorPluginImageHelper.getInstance().getImageDescriptor(symbolicName);
  }
}
