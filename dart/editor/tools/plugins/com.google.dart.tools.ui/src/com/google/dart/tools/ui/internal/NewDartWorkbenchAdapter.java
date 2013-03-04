/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.internal;

import com.google.dart.engine.element.Element;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.NewDartElementLabels;
import com.google.dart.tools.ui.NewStandardDartElementContentProvider;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;
import com.google.dart.tools.ui.internal.viewsupport.NewDartElementImageProvider;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A {@link IWorkbenchAdapter} for Dart {@link Element}s.
 */
public class NewDartWorkbenchAdapter implements IWorkbenchAdapter {

  private final NewDartElementImageProvider imageProvider;
  private final NewStandardDartElementContentProvider contentProvider;

  public NewDartWorkbenchAdapter() {
    imageProvider = new NewDartElementImageProvider();
    contentProvider = new NewStandardDartElementContentProvider(true);
  }

  @Override
  public Object[] getChildren(Object element) {
    return contentProvider.getChildren(element);
  }

  @Override
  public ImageDescriptor getImageDescriptor(Object element) {
    Element de = getDartElement(element);
    if (de != null) {
      return imageProvider.getDartImageDescriptor(de, DartElementImageProvider.OVERLAY_ICONS
          | DartElementImageProvider.SMALL_ICONS);
    }
    return null;
  }

  @Override
  public String getLabel(Object element) {
    return NewDartElementLabels.getTextLabel(getDartElement(element), DartElementLabels.ALL_DEFAULT);
  }

  @Override
  public Object getParent(Object element) {
    return contentProvider.getParent(element);
  }

  private Element getDartElement(Object element) {
    if (element instanceof Element) {
      return (Element) element;
    }
    return null;
  }
}
