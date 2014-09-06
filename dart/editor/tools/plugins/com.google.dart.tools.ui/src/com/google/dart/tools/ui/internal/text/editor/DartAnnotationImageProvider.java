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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;

/**
 * Image provider for annotations based on Java problem markers.
 */
public class DartAnnotationImageProvider implements IAnnotationImageProvider {

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
    return null;
  }
}
