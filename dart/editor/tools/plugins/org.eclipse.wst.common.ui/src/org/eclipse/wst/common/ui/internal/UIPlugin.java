/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - Initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * The main plugin class to be used in the desktop.
 */
public class UIPlugin extends AbstractUIPlugin {
  //The shared instance.
  private static UIPlugin plugin;

  /**
   * The constructor.
   */
  public UIPlugin() {
    super();
    plugin = this;
  }

  /**
   * Returns the shared instance.
   */
  public static UIPlugin getDefault() {
    return plugin;
  }

  /**
   * Returns the workspace instance.
   */
  public static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not found.
   */
  public static String getResourceString(String key) {
    ResourceBundle aResourceBundle = getDefault().getResourceBundle();
    try {
      return (aResourceBundle != null) ? aResourceBundle.getString(key) : key;
    } catch (MissingResourceException e) {
      return key;
    }
  }

  public static String getString(String key) {
    return getResourceString(key);
  }

  /**
   * This gets the string resource and does one substitution.
   */
  public static String getString(String key, Object s1) {
    return MessageFormat.format(getResourceString(key), new Object[] {s1});
  }

  /**
   * This gets the string resource and does two substitutions.
   */
  public static String getString(String key, Object s1, Object s2) {
    return MessageFormat.format(getResourceString(key), new Object[] {s1, s2});
  }

  /**
   * Returns the plugin's resource bundle,
   */
  public ResourceBundle getResourceBundle() {
    try {
      return Platform.getResourceBundle(plugin.getBundle());
    } catch (MissingResourceException x) {
      log(x);
    }
    return null;
  }

  public ImageDescriptor getImageDescriptor(String name) {
    try {
      URL url = new URL(getBundle().getEntry("/"), name);
      return ImageDescriptor.createFromURL(url);
    } catch (MalformedURLException e) {
      return ImageDescriptor.getMissingImageDescriptor();
    }
  }

  public Image getImage(String iconName) {
    ImageRegistry imageRegistry = getImageRegistry();

    if (imageRegistry.get(iconName) != null) {
      return imageRegistry.get(iconName);
    } else {
      imageRegistry.put(iconName, ImageDescriptor.createFromFile(getClass(), iconName));
      return imageRegistry.get(iconName);
    }
  }

  public static String getPluginId() {
    return getDefault().getBundle().getSymbolicName();
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  public static void log(String message, Throwable e) {
    log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, e));
  }

  public static void log(String message) {
    log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null));
  }

  public static void log(Throwable e) {
    log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, e.getLocalizedMessage(), e));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    // TODO Auto-generated method stub
    super.start(context);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    // TODO Auto-generated method stub
    super.stop(context);
  }

  public static IWorkbenchPage getActivePage() {
    return getDefault().internalGetActivePage();
  }

  private IWorkbenchPage internalGetActivePage() {
    IWorkbenchWindow window = getWorkbench().getActiveWorkbenchWindow();
    if (window == null)
      return null;
    return getWorkbench().getActiveWorkbenchWindow().getActivePage();
  }
}
