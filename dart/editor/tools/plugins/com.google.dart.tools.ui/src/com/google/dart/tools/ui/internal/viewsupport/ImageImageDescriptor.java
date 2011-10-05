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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

/**
  */
public class ImageImageDescriptor extends ImageDescriptor {

  private Image fImage;

  /**
   * Constructor for ImagImageDescriptor.
   */
  public ImageImageDescriptor(Image image) {
    super();
    fImage = image;
  }

  /*
   * (non-Javadoc)
   * 
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(Object obj) {
    return (obj != null) && getClass().equals(obj.getClass())
        && fImage.equals(((ImageImageDescriptor) obj).fImage);
  }

  /*
   * (non-Javadoc)
   * 
   * @see ImageDescriptor#getImageData()
   */
  @Override
  public ImageData getImageData() {
    return fImage.getImageData();
  }

  /*
   * (non-Javadoc)
   * 
   * @see Object#hashCode()
   */
  @Override
  public int hashCode() {
    return fImage.hashCode();
  }

}
