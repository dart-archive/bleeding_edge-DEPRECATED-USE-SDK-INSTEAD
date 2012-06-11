/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

public class CallHierarchyImageDescriptor extends CompositeImageDescriptor {

  /** Flag to render the recursive adornment */
  public final static int RECURSIVE = 0x001;

  /** Flag to render the callee adornment */
  public final static int MAX_LEVEL = 0x002;

  private ImageDescriptor baseImage;
  private int flags;
  private Point size;

  /**
   * Creates a new CallHierarchyImageDescriptor.
   * 
   * @param baseImage an image descriptor used as the base image
   * @param flags flags indicating which adornments are to be rendered. See
   *          <code>setAdornments</code> for valid values.
   * @param size the size of the resulting image
   * @see #setAdornments(int)
   */
  public CallHierarchyImageDescriptor(ImageDescriptor baseImage, int flags, Point size) {
    this.baseImage = baseImage;
    Assert.isNotNull(baseImage);
    this.flags = flags;
    Assert.isTrue(flags >= 0);
    this.size = size;
    Assert.isNotNull(size);
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || !CallHierarchyImageDescriptor.class.equals(object.getClass())) {
      return false;
    }

    CallHierarchyImageDescriptor other = (CallHierarchyImageDescriptor) object;
    return (baseImage.equals(other.baseImage) && flags == other.flags && size.equals(other.size));
  }

  /**
   * Returns the current adornments.
   * 
   * @return the current adornments
   */
  public int getAdronments() {
    return flags;
  }

  /**
   * Returns the size of the image created by calling <code>createImage()</code>.
   * 
   * @return the size of the image created by calling <code>createImage()</code>
   * @see ImageDescriptor#createImage()
   */
  public Point getImageSize() {
    return new Point(size.x, size.y);
  }

  @Override
  public int hashCode() {
    return baseImage.hashCode() | flags | size.hashCode();
  }

  /**
   * Sets the descriptors adornments. Valid values are: <code>RECURSIVE</code>, <code>CALLER</code>,
   * <code>CALLEE</code>, <code>MAX_LEVEL</code>, or any combination of those.
   * 
   * @param adornments the image descriptor's adornments
   */
  public void setAdornments(int adornments) {
    Assert.isTrue(adornments >= 0);
    flags = adornments;
  }

  /**
   * Sets the size of the image created by calling <code>createImage()</code>.
   * 
   * @param size the size of the image returned from calling <code>createImage()</code>
   * @see ImageDescriptor#createImage()
   */
  public void setImageSize(Point size) {
    Assert.isNotNull(size);
    Assert.isTrue(size.x >= 0 && size.y >= 0);
    this.size = size;
  }

  @Override
  protected void drawCompositeImage(int width, int height) {
    ImageData bg = getImageData(baseImage);

    drawImage(bg, 0, 0);
    drawBottomLeft();
  }

  @Override
  protected Point getSize() {
    return size;
  }

  private void drawBottomLeft() {
    Point size = getSize();
    int x = 0;
    ImageData data = null;
    if ((flags & RECURSIVE) != 0) {
      data = getImageData(DartPluginImages.DESC_OVR_RECURSIVE);
      drawImage(data, x, size.y - data.height);
      x += data.width;
    }
    if ((flags & MAX_LEVEL) != 0) {
      data = getImageData(DartPluginImages.DESC_OVR_MAX_LEVEL);
      drawImage(data, x, size.y - data.height);
      x += data.width;
    }
  }

  private ImageData getImageData(ImageDescriptor descriptor) {
    ImageData data = descriptor.getImageData(); // getImageData can return null
    if (data == null) {
      data = DEFAULT_IMAGE_DATA;
      DartToolsPlugin.logErrorMessage("Image data not available: " + descriptor.toString()); //$NON-NLS-1$
    }
    return data;
  }
}
