/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.wst.sse.ui.internal.reconcile.TemporaryAnnotation;

/**
 * This is overridden to get around the problem of being registered as a
 * org.eclipse.wst.validation.core.problemmarker rather than a
 * org.eclipse.core.resource.problemmarker causing all problems to be skipped in the OverviewRuler
 */
public class StructuredMarkerAnnotation extends MarkerAnnotation implements IAnnotationPresentation {
  // controls if icon should be painted gray
  private boolean fIsGrayed = false;
  String fAnnotationType = null;

  StructuredMarkerAnnotation(IMarker marker) {
    super(marker);
  }

  public final String getAnnotationType() {
    if (fAnnotationType == null) {
      initAnnotationType();
    }
    return fAnnotationType;
  }

  /**
   * Eventually will have to use IAnnotationPresentation & IAnnotationExtension
   * 
   * @see org.eclipse.ui.texteditor.MarkerAnnotation#getImage(org.eclipse.swt.widgets.Display)
   */
  protected Image getImage(Display display) {
    Image image = null;
    String annotationType = getAnnotationType();
    if (annotationType == TemporaryAnnotation.ANNOT_ERROR) {
      image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
    } else if (annotationType == TemporaryAnnotation.ANNOT_WARNING) {
      image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
    } else if (annotationType == TemporaryAnnotation.ANNOT_INFO) {
      image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);
    }

    if (image != null && isGrayed())
      setImage(getGrayImage(display, image));
    else
      setImage(image);

    return super.getImage(display);
  }

  private Image getGrayImage(Display display, Image image) {
    if (image != null) {
      String key = Integer.toString(image.hashCode());
      // make sure we cache the gray image
      Image grayImage = JFaceResources.getImageRegistry().get(key);
      if (grayImage == null) {
        grayImage = new Image(display, image, SWT.IMAGE_GRAY);
        JFaceResources.getImageRegistry().put(key, grayImage);
      }
      image = grayImage;
    }
    return image;
  }

  public final boolean isGrayed() {
    return fIsGrayed;
  }

  public final void setGrayed(boolean grayed) {
    fIsGrayed = grayed;
  }

  /**
   * Initializes the annotation's icon representation and its drawing layer based upon the
   * properties of the underlying marker.
   */
  protected void initAnnotationType() {

    IMarker marker = getMarker();
    fAnnotationType = TemporaryAnnotation.ANNOT_UNKNOWN;
    try {
      if (marker.exists() && marker.isSubtypeOf(IMarker.PROBLEM)) {
        int severity = marker.getAttribute(IMarker.SEVERITY, -1);
        switch (severity) {
          case IMarker.SEVERITY_ERROR:
            fAnnotationType = TemporaryAnnotation.ANNOT_ERROR;
            break;
          case IMarker.SEVERITY_WARNING:
            fAnnotationType = TemporaryAnnotation.ANNOT_WARNING;
            break;
          case IMarker.SEVERITY_INFO:
            fAnnotationType = TemporaryAnnotation.ANNOT_INFO;
            break;
        }
      }

    } catch (CoreException e) {
      Logger.logException(e);
    }
  }
}
