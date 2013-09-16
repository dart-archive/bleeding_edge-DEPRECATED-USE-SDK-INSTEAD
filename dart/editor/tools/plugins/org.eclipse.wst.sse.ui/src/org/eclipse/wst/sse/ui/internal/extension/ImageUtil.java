/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.extension;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.Bundle;

import java.net.MalformedURLException;
import java.net.URL;

public class ImageUtil {

  /**
   * Convenience Method. Returns an ImageDescriptor whose path, relative to the plugin containing
   * the <code>extension</code> is <code>subdirectoryAndFilename</code>. If there isn't any value
   * associated with the name then <code>null
   * </code> is returned. This method is convenience and only intended for use by the workbench
   * because it explicitly uses the workbench's registry for caching/retrieving images from other
   * extensions -- other plugins must user their own registry. This convenience method is subject to
   * removal. Note: subdirectoryAndFilename must not have any leading "." or path separators / or \
   * ISV's should use icons/mysample.gif and not ./icons/mysample.gif Note: This consults the plugin
   * for extension and obtains its installation location. all requested images are assumed to be in
   * a directory below and relative to that plugins installation directory.
   */
  public static ImageDescriptor getImageDescriptorFromExtension(IExtension extension,
      String subdirectoryAndFilename) {
    String pluginId = extension.getNamespace();
    Bundle bundle = Platform.getBundle(pluginId);
    return getImageDescriptorFromBundle(bundle, subdirectoryAndFilename);
  }

  /**
   * Convenience Method. Return an ImageDescriptor whose path relative to the plugin described by
   * <code>bundle</code> is <code>subdirectoryAndFilename</code>. Returns <code>null</code> if no
   * image could be found. This method is convenience and only intended for use by the workbench
   * because it explicitly uses the workbench's registry for caching/retrieving images from other
   * extensions -- other plugins must user their own registry. This convenience method is subject to
   * removal. Note: subdirectoryAndFilename must not have any leading "." or path separators / or \
   * ISV's should use icons/mysample.gif and not ./icons/mysample.gif Note: This consults the plugin
   * for extension and obtains its installation location. all requested images are assumed to be in
   * a directory below and relative to that plugins installation directory.
   */
  public static ImageDescriptor getImageDescriptorFromBundle(Bundle bundle,
      String subdirectoryAndFilename) {

    URL path = bundle.getEntry("/"); //$NON-NLS-1$
    URL fullPathString = null;
    try {
      fullPathString = new URL(path, subdirectoryAndFilename);
      return ImageDescriptor.createFromURL(fullPathString);
    } catch (MalformedURLException e) {
    }
    return null;
  }
}
