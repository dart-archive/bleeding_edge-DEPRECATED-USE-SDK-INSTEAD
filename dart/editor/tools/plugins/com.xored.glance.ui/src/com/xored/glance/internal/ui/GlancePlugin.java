/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.internal.ui;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.net.URL;

/**
 * The activator class controls the plug-in life cycle
 */
public class GlancePlugin extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.xored.glance.ui";

  // IMAGES

  private static final String IMG_PREFIX = "icons/glance/";

  public static final String IMG_WAIT = IMG_PREFIX + "wait.gif";
  public static final String IMG_NEXT = IMG_PREFIX + "next.gif";
  public static final String IMG_PREV = IMG_PREFIX + "prev.gif";
  public static final String IMG_SEARCH = IMG_PREFIX + "search.png";
  public static final String IMG_START_INDEXING = IMG_PREFIX + "update_1.gif";
  public static final String IMG_END_INDEXING = IMG_PREFIX + "update_2.gif";
  public static final String IMG_INDEXING_FINISHED = IMG_PREFIX + "update_done.gif";
  public static final String[] IMG_INDEXING_LOOP = new String[] {
      IMG_START_INDEXING, IMG_END_INDEXING};

  // The shared instance
  private static GlancePlugin plugin;

  public static IStatus createStatus(String message) {
    return createStatus(message, null);
  }

  public static IStatus createStatus(String message, Throwable t) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message, t);
  }

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static GlancePlugin getDefault() {
    return plugin;
  }

  public static Image getImage(String path) {
    Image image = getDefault().getImageRegistry().get(path);
    if (image == null) {
      ImageDescriptor imageDescriptor = getImageDescriptor(path, false);
      image = imageDescriptor.createImage();
      getDefault().getImageRegistry().put(path, image);
    }
    return image;
  }

  public static ImageDescriptor getImageDescriptor(String path) {
    return getImageDescriptor(path, true);
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  public static void log(String message) {
    log(message, null);
  }

  public static void log(String message, Throwable t) {
    log(createStatus(message, t));
  }

  public static void log(Throwable t) {
    log(t.getMessage(), t);
  }

  private static ImageDescriptor getImageDescriptor(String path, boolean addToRegistry) {
    ImageDescriptor imageDescriptor = getDefault().getImageRegistry().getDescriptor(path);
    if (imageDescriptor == null) {
      imageDescriptor = ImageDescriptor.createFromURL(makeImageURL(path));
      if (addToRegistry) {
        getDefault().getImageRegistry().put(path, imageDescriptor);
      }
    }
    return imageDescriptor;
  }

  private static URL makeImageURL(String path) {
    return FileLocator.find(getDefault().getBundle(), new Path(path), null);
  }

  /**
   * The constructor
   */
  public GlancePlugin() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext )
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext )
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }
}
