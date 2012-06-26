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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;

/**
 * Image provider for
 * {@link com.google.dart.tools.ui.editor.OverrideIndicatorManager.OverrideIndicator} annotations.
 */
public class OverrideIndicatorImageProvider implements IAnnotationImageProvider {

  private static final String OVERRIDE_IMG_DESC_ID = "JavaPluginImages.DESC_OBJ_OVERRIDES"; //$NON-NLS-1$
  private static final String OVERWRITE_IMG_DESC_ID = "JavaPluginImages.DESC_OBJ_IMPLEMENTS"; //$NON-NLS-1$

  @Override
  public ImageDescriptor getImageDescriptor(String imageDescritporId) {
    if (OVERWRITE_IMG_DESC_ID.equals(imageDescritporId)) {
      return DartPluginImages.DESC_OBJ_IMPLEMENTS;
    } else if (OVERRIDE_IMG_DESC_ID.equals(imageDescritporId)) {
      return DartPluginImages.DESC_OBJ_OVERRIDES;
    }

    return null;
  }

  @Override
  public String getImageDescriptorId(Annotation annotation) {
    if (!isImageProviderFor(annotation)) {
      return null;
    }

    if (isOverride(annotation)) {
      return OVERRIDE_IMG_DESC_ID;
    } else {
      return OVERWRITE_IMG_DESC_ID;
    }
  }

  @Override
  public Image getManagedImage(Annotation annotation) {
    return null;
  }

  private boolean isImageProviderFor(Annotation annotation) {
    return annotation != null
        && OverrideIndicatorManager.ANNOTATION_TYPE.equals(annotation.getType());
  }

  private boolean isOverride(Annotation annotation) {
    return ((OverrideIndicatorManager.OverrideIndicator) annotation).isOverride();
  }
}
