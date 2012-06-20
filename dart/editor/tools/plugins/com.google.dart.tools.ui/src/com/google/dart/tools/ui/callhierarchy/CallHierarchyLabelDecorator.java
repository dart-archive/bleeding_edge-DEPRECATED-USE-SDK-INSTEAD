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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;
import com.google.dart.tools.ui.internal.viewsupport.ImageImageDescriptor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Label decorator that decorates an method's image with recursion overlays. The viewer using this
 * decorator is responsible for updating the images on element changes.
 */
public class CallHierarchyLabelDecorator implements ILabelDecorator {

  /**
   * Creates a decorator. The decorator creates an own image registry to cache images.
   */
  public CallHierarchyLabelDecorator() {
    // Do nothing
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    // Do nothing
  }

  @Override
  public Image decorateImage(Image image, Object element) {
    if (image == null) {
      return null;
    }

    int adornmentFlags = computeAdornmentFlags(element);
    if (adornmentFlags != 0) {
      ImageDescriptor baseImage = new ImageImageDescriptor(image);
      Rectangle bounds = image.getBounds();
      return DartToolsPlugin.getImageDescriptorRegistry().get(
          new CallHierarchyImageDescriptor(baseImage, adornmentFlags, new Point(
              bounds.width,
              bounds.height)));
    }
    return image;
  }

  @Override
  public String decorateText(String text, Object element) {
    return text;
  }

  @Override
  public void dispose() {
    // Nothing to dispose
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return true;
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    // Do nothing
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   * 
   * @param element the element for which to compute the flags
   * @return the flags
   */
  private int computeAdornmentFlags(Object element) {
    int flags = 0;
    if (element instanceof MethodWrapper) {
      MethodWrapper methodWrapper = (MethodWrapper) element;
      if (methodWrapper.isRecursive()) {
        flags = CallHierarchyImageDescriptor.RECURSIVE;
      }
      if (isMaxCallDepthExceeded(methodWrapper)) {
        flags |= CallHierarchyImageDescriptor.MAX_LEVEL;
      }
    }
    return flags;
  }

  private boolean isMaxCallDepthExceeded(MethodWrapper methodWrapper) {
    return methodWrapper.getLevel() > CallHierarchyUI.getDefault().getMaxCallDepth();
  }
}
