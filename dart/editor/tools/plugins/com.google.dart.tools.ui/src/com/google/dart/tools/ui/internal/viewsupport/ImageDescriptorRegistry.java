/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.util.HashMap;
import java.util.Iterator;

/**
 * A registry that maps <code>ImageDescriptors</code> to <code>Image</code>.
 */
public class ImageDescriptorRegistry {

  private HashMap<ImageDescriptor, Image> fRegistry = new HashMap<ImageDescriptor, Image>(10);
  private Display fDisplay;

  /**
   * Creates a new image descriptor registry for the current or default display, respectively.
   */
  public ImageDescriptorRegistry() {
    this(SWTUtil.getStandardDisplay());
  }

  /**
   * Creates a new image descriptor registry for the given display. All images managed by this
   * registry will be disposed when the display gets disposed.
   * 
   * @param display the display the images managed by this registry are allocated for
   */
  public ImageDescriptorRegistry(Display display) {
    fDisplay = display;
    Assert.isNotNull(fDisplay);
    hookDisplay();
  }

  /**
   * Disposes all images managed by this registry.
   */
  public void dispose() {
    for (Iterator<Image> iter = fRegistry.values().iterator(); iter.hasNext();) {
      Image image = iter.next();
      image.dispose();
    }
    fRegistry.clear();
  }

  /**
   * Returns the image associated with the given image descriptor.
   * 
   * @param descriptor the image descriptor for which the registry manages an image, or
   *          <code>null</code> for a missing image descriptor
   * @return the image associated with the image descriptor or <code>null</code> if the image
   *         descriptor can't create the requested image.
   */
  public Image get(ImageDescriptor descriptor) {
    if (descriptor == null) {
      descriptor = ImageDescriptor.getMissingImageDescriptor();
    }

    Image result = fRegistry.get(descriptor);
    if (result != null) {
      return result;
    }

    Assert.isTrue(fDisplay == SWTUtil.getStandardDisplay(), "Allocating image for wrong display."); //$NON-NLS-1$
    result = descriptor.createImage();
    if (result != null) {
      fRegistry.put(descriptor, result);
    }
    return result;
  }

  private void hookDisplay() {
    fDisplay.disposeExec(new Runnable() {
      @Override
      public void run() {
        dispose();
      }
    });
  }
}
