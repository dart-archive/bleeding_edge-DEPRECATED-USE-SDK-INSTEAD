/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.edit.util;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImageHelper;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImages;

public class SharedEditorPluginImageHelper {
  public static final String IMG_DLCL_COLLAPSEALL = EditorPluginImages.IMG_DLCL_COLLAPSEALL;
  public static final String IMG_DLCL_DELETE = EditorPluginImages.IMG_DLCL_DELETE;
  public static final String IMG_DLCL_SYNCED = EditorPluginImages.IMG_DLCL_SYNCED;
  public static final String IMG_ELCL_COLLAPSEALL = EditorPluginImages.IMG_ELCL_COLLAPSEALL;
  public static final String IMG_ELCL_DELETE = EditorPluginImages.IMG_ELCL_DELETE;
  public static final String IMG_ELCL_SYNCED = EditorPluginImages.IMG_ELCL_SYNCED;

  /**
   * Retrieves the specified image from the source editor plugin's image registry. Note: The
   * returned <code>Image</code> is managed by the workbench; clients must <b>not </b> dispose of
   * the returned image.
   * 
   * @param symbolicName the symbolic name of the image; there are constants declared in this class
   *          for build-in images that come with the source editor
   * @return the image, or <code>null</code> if not found
   */
  public static Image getImage(String symbolicName) {
    return EditorPluginImageHelper.getInstance().getImage(symbolicName);
  }

  /**
   * Retrieves the image descriptor for specified image from the source editor plugin's image
   * registry. Unlike <code>Image</code>s, image descriptors themselves do not need to be disposed.
   * 
   * @param symbolicName the symbolic name of the image; there are constants declared in this
   *          interface for build-in images that come with the source editor
   * @return the image descriptor, or <code>null</code> if not found
   */
  public static ImageDescriptor getImageDescriptor(String symbolicName) {
    return EditorPluginImageHelper.getInstance().getImageDescriptor(symbolicName);
  }
}
