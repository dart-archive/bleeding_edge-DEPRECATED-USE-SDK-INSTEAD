/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * A {@link DartElementImageDescriptor} consists of a base image and several adornments. The
 * adornments are computed according to the flags either passed during creation or set via the
 * method {@link #setAdornments(int)}.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class DartElementImageDescriptor extends CompositeImageDescriptor {

  /** Flag to render the abstract adornment. */
  public final static int ABSTRACT = 0x001;

  /** Flag to render the final adornment. */
  public final static int FINAL = 0x002;

  /** Flag to render the synchronized adornment. */
  public final static int SYNCHRONIZED = 0x004;

  /** Flag to render the static adornment. */
  public final static int STATIC = 0x008;

  /** Flag to render the runnable adornment. */
  public final static int RUNNABLE = 0x010;

  /** Flag to render the warning adornment. */
  public final static int WARNING = 0x020;

  /** Flag to render the error adornment. */
  public final static int ERROR = 0x040;

  /** Flag to render the 'override' adornment. */
  public final static int OVERRIDES = 0x080;

  /** Flag to render the 'implements' adornment. */
  public final static int IMPLEMENTS = 0x100;

  /** Flag to render the 'constructor' adornment. */
  public final static int CONSTRUCTOR = 0x200;

  /**
   * Flag to render the 'volatile' adornment.
   */
  public final static int VOLATILE = 0x800;

  /**
   * Flag to render the 'transient' adornment.
   */
  public final static int TRANSIENT = 0x1000;

  /**
   * Flag to render the 'const' adornment.
   */
  public final static int CONST = 0x2000;

  /**
   * Flag to render the 'setter' adornment
   */
  public final static int SETTER = 0x4000;

  /**
   * Flag to render the 'getter' adornment
   */
  public final static int GETTER = 0x10000;

  /**
   * Flag to render the 'linked' adornment
   */
  public final static int LINKED = 0x20000;

  private ImageDescriptor fBaseImage;
  private int fFlags;
  private Point fSize;

  /**
   * Creates a new DartElementImageDescriptor.
   * 
   * @param baseImage an image descriptor used as the base image
   * @param flags flags indicating which adornments are to be rendered. See
   *          {@link #setAdornments(int)} for valid values.
   * @param size the size of the resulting image
   */
  public DartElementImageDescriptor(ImageDescriptor baseImage, int flags, Point size) {
    fBaseImage = baseImage;
    Assert.isNotNull(fBaseImage);
    fFlags = flags;
    Assert.isTrue(fFlags >= 0);
    fSize = size;
    Assert.isNotNull(fSize);
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || !DartElementImageDescriptor.class.equals(object.getClass())) {
      return false;
    }

    DartElementImageDescriptor other = (DartElementImageDescriptor) object;
    return (fBaseImage.equals(other.fBaseImage) && fFlags == other.fFlags && fSize.equals(other.fSize));
  }

  /**
   * Returns the current adornments.
   * 
   * @return the current adornments
   */
  public int getAdronments() {
    return fFlags;
  }

  /**
   * Returns the size of the image created by calling {@link #createImage()}.
   * 
   * @return the size of the image created by calling {@link #createImage()}
   */
  public Point getImageSize() {
    return new Point(fSize.x, fSize.y);
  }

  @Override
  public int hashCode() {
    return fBaseImage.hashCode() | fFlags | fSize.hashCode();
  }

  /**
   * Sets the descriptors adornments. Valid values are: {@link #ABSTRACT}, {@link #FINAL},
   * {@link #SYNCHRONIZED}, {@link #STATIC}, {@link #RUNNABLE}, {@link #WARNING}, {@link #ERROR},
   * {@link #OVERRIDES}, {@link #IMPLEMENTS}, {@link #CONSTRUCTOR}, {@link #VOLATILE},
   * {@link #TRANSIENT}, {@link #CONST}, {@link #GETTER}, {@link #SETTER} or any combination of
   * those.
   * 
   * @param adornments the image descriptors adornments
   */
  public void setAdornments(int adornments) {
    Assert.isTrue(adornments >= 0);
    fFlags = adornments;
  }

  /**
   * Sets the size of the image created by calling {@link #createImage()}.
   * 
   * @param size the size of the image returned from calling {@link #createImage()}
   */
  public void setImageSize(Point size) {
    Assert.isNotNull(size);
    Assert.isTrue(size.x >= 0 && size.y >= 0);
    fSize = size;
  }

  @Override
  protected void drawCompositeImage(int width, int height) {
    ImageData bg = getImageData(fBaseImage);

    drawImage(bg, 0, 0);

    drawTopRight();
    drawBottomRight();
    drawBottomLeft();

  }

  @Override
  protected Point getSize() {
    return fSize;
  }

  private void addBottomLeftImage(ImageDescriptor desc, Point pos) {
    ImageData data = getImageData(desc);
    int x = pos.x;
    int y = pos.y - data.height;
    if (x + data.width < getSize().x && y >= 0) {
      drawImage(data, x, y);
      pos.x = x + data.width;
    }
  }

  private void addBottomRightImage(ImageDescriptor desc, Point pos) {
    ImageData data = getImageData(desc);
    int x = pos.x - data.width;
    int y = pos.y - data.height;
    if (x >= 0 && y >= 0) {
      drawImage(data, x, y);
      pos.x = x;
    }
  }

  private void addTopRightImage(ImageDescriptor desc, Point pos) {
    ImageData data = getImageData(desc);
    int x = pos.x - data.width;
    if (x >= 0) {
      drawImage(data, x, pos.y);
      pos.x = x;
    }
  }

  private void drawBottomLeft() {
    Point pos = new Point(0, getSize().y);

    if ((fFlags & ERROR) != 0) {
      addBottomLeftImage(DartPluginImages.DESC_OVR_ERROR, pos);
    }
    if ((fFlags & WARNING) != 0) {
      addBottomLeftImage(DartPluginImages.DESC_OVR_WARNING, pos);
    }
    if ((fFlags & LINKED) != 0) {
      addBottomLeftImage(DartPluginImages.DESC_OVR_LINKED, pos);
    }
  }

  private void drawBottomRight() {
    Point size = getSize();
    Point pos = new Point(size.x, size.y);

    int flags = fFlags;

    int syncAndOver = SYNCHRONIZED | OVERRIDES;
    int syncAndImpl = SYNCHRONIZED | IMPLEMENTS;

    if ((flags & syncAndOver) == syncAndOver) { // both flags set: merged
// overlay image
      addBottomRightImage(DartPluginImages.DESC_OVR_SYNCH_AND_OVERRIDES, pos);
      flags &= ~syncAndOver; // clear to not render again
    } else if ((flags & syncAndImpl) == syncAndImpl) { // both flags set: merged
// overlay image
      addBottomRightImage(DartPluginImages.DESC_OVR_SYNCH_AND_IMPLEMENTS, pos);
      flags &= ~syncAndImpl; // clear to not render again
    }
    if ((flags & OVERRIDES) != 0) {
      addBottomRightImage(DartPluginImages.DESC_OVR_OVERRIDES, pos);
    }
    if ((flags & IMPLEMENTS) != 0) {
      addBottomRightImage(DartPluginImages.DESC_OVR_IMPLEMENTS, pos);
    }
    if ((flags & SYNCHRONIZED) != 0) {
      addBottomRightImage(DartPluginImages.DESC_OVR_SYNCH, pos);
    }
    if ((flags & RUNNABLE) != 0) {
      addBottomRightImage(DartPluginImages.DESC_OVR_RUN, pos);
    }
//    if ((flags & TRANSIENT) != 0) {
//      addBottomRightImage(DartPluginImages.DESC_OVR_TRANSIENT, pos);
//    }

    if ((flags & GETTER) != 0) {
      addBottomRightImage(DartPluginImages.DESC_OVR_GETTER, pos);
    }
    if ((flags & SETTER) != 0) {
      addBottomRightImage(DartPluginImages.DESC_OVR_SETTER, pos);
    }
  }

  private void drawTopRight() {
    Point pos = new Point(getSize().x, 0);
    if ((fFlags & ABSTRACT) != 0) {
      addTopRightImage(DartPluginImages.DESC_OVR_ABSTRACT, pos);
    }
    if ((fFlags & CONSTRUCTOR) != 0) {
      addTopRightImage(DartPluginImages.DESC_OVR_CONSTRUCTOR, pos);
    }
    if ((fFlags & FINAL) != 0) {
      addTopRightImage(DartPluginImages.DESC_OVR_FINAL, pos);
    }
    if ((fFlags & VOLATILE) != 0) {
      addTopRightImage(DartPluginImages.DESC_OVR_VOLATILE, pos);
    }
    if ((fFlags & STATIC) != 0) {
      addTopRightImage(DartPluginImages.DESC_OVR_STATIC, pos);
    }
    if ((fFlags & CONST) != 0) {
      addTopRightImage(DartPluginImages.DESC_OVR_FINAL, pos);
    }

  }

  private ImageData getImageData(ImageDescriptor descriptor) {
    ImageData data = descriptor.getImageData(); // see bug 51965: getImageData
// can return null
    if (data == null) {
      data = DEFAULT_IMAGE_DATA;
      DartToolsPlugin.logErrorMessage("Image data not available: " + descriptor.toString()); //$NON-NLS-1$
    }
    return data;
  }
}
