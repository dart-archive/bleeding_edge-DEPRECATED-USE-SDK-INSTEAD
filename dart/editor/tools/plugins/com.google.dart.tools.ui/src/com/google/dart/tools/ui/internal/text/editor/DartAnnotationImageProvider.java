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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;

/**
 * Image provider for annotations based on Java problem markers.
 */
public class DartAnnotationImageProvider implements IAnnotationImageProvider {

  private final static int NO_IMAGE = 0;
  private final static int GRAY_IMAGE = 1;
  private final static int OVERLAY_IMAGE = 2;
  private final static int QUICKFIX_IMAGE = 3;
  private final static int QUICKFIX_ERROR_IMAGE = 4;

  private static Image fgQuickFixImage;
  private static Image fgQuickFixErrorImage;
  private static ImageRegistry fgImageRegistry;

//  private boolean fShowQuickFixIcon;
  private int fCachedImageType;
  private Image fCachedImage;

  public DartAnnotationImageProvider() {
//    fShowQuickFixIcon = PreferenceConstants.getPreferenceStore().getBoolean(
//        PreferenceConstants.EDITOR_CORRECTION_INDICATION);
  }

  /*
   * @see org.eclipse.jface.text.source.IAnnotationImageProvider#getImageDescriptor
   * (java.lang.String)
   */
  @Override
  public ImageDescriptor getImageDescriptor(String symbolicName) {
    // unmanaged images are not supported
    return null;
  }

  /*
   * @see org.eclipse.jface.text.source.IAnnotationImageProvider#getImageDescriptorId
   * (org.eclipse.jface.text.source.Annotation)
   */
  @Override
  public String getImageDescriptorId(Annotation annotation) {
    // unmanaged images are not supported
    return null;
  }

  /*
   * @see org.eclipse.jface.text.source.IAnnotationImageProvider#getManagedImage(
   * org.eclipse.jface.text.source.Annotation)
   */
  @Override
  public Image getManagedImage(Annotation annotation) {
    if (annotation instanceof IJavaAnnotation) {
      IJavaAnnotation javaAnnotation = (IJavaAnnotation) annotation;
      int imageType = getImageType(javaAnnotation);
      return getImage(javaAnnotation, imageType, Display.getCurrent());
    }
    return null;
  }

  private Image getImage(IJavaAnnotation annotation, int imageType, Display display) {
    if ((imageType == QUICKFIX_IMAGE || imageType == QUICKFIX_ERROR_IMAGE)
        && fCachedImageType == imageType) {
      return fCachedImage;
    }

    Image image = null;
    switch (imageType) {
      case OVERLAY_IMAGE:
        IJavaAnnotation overlay = annotation.getOverlay();
        image = getManagedImage((Annotation) overlay);
        fCachedImageType = -1;
        break;
      case QUICKFIX_IMAGE:
        image = getQuickFixImage();
        fCachedImageType = imageType;
        fCachedImage = image;
        break;
      case QUICKFIX_ERROR_IMAGE:
        image = getQuickFixErrorImage();
        fCachedImageType = imageType;
        fCachedImage = image;
        break;
      case GRAY_IMAGE: {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        String annotationType = annotation.getType();
        if (DartMarkerAnnotation.ERROR_ANNOTATION_TYPE.equals(annotationType)) {
          image = sharedImages.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
        } else if (DartMarkerAnnotation.WARNING_ANNOTATION_TYPE.equals(annotationType)) {
          image = sharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
        } else if (DartMarkerAnnotation.INFO_ANNOTATION_TYPE.equals(annotationType)) {
          image = DartToolsPlugin.getImage("icons/full/misc/info2.png");
        }
        if (image != null) {
          ImageRegistry registry = getImageRegistry(display);
          String key = Integer.toString(image.hashCode());
          Image grayImage = registry.get(key);
          if (grayImage == null) {
            grayImage = new Image(display, image, SWT.IMAGE_GRAY);
            registry.put(key, grayImage);
          }
          image = grayImage;
        }
        fCachedImageType = -1;
        break;
      }
    }

    return image;
  }

  private ImageRegistry getImageRegistry(Display display) {
    if (fgImageRegistry == null) {
      fgImageRegistry = new ImageRegistry(display);
    }
    return fgImageRegistry;
  }

  private int getImageType(IJavaAnnotation annotation) {
    int imageType = NO_IMAGE;
    if (annotation.hasOverlay()) {
      imageType = OVERLAY_IMAGE;
    } else if (!annotation.isMarkedDeleted()) {
      if (showQuickFix(annotation)) {
        imageType = DartMarkerAnnotation.ERROR_ANNOTATION_TYPE.equals(annotation.getType())
            ? QUICKFIX_ERROR_IMAGE : QUICKFIX_IMAGE;
      }
    } else {
      imageType = GRAY_IMAGE;
    }
    return imageType;
  }

  private Image getQuickFixErrorImage() {
    if (fgQuickFixErrorImage == null) {
      fgQuickFixErrorImage = DartPluginImages.get(DartPluginImages.IMG_OBJS_FIXABLE_ERROR);
    }
    return fgQuickFixErrorImage;
  }

  private Image getQuickFixImage() {
    if (fgQuickFixImage == null) {
      fgQuickFixImage = DartPluginImages.get(DartPluginImages.IMG_OBJS_FIXABLE_PROBLEM);
    }
    return fgQuickFixImage;
  }

  private boolean showQuickFix(IJavaAnnotation annotation) {
    DartX.todo();
    return false;
//    return fShowQuickFixIcon && annotation.isProblem()
//        && JavaCorrectionProcessor.hasCorrections((Annotation) annotation);
  }
}
