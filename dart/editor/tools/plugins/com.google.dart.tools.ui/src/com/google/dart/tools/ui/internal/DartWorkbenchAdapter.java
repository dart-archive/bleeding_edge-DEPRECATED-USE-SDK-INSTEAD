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
package com.google.dart.tools.ui.internal;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.ParentElement;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * An implementation of the {@link IWorkbenchAdapter} for {@link DartElement}s.
 */
public class DartWorkbenchAdapter implements IWorkbenchAdapter {

  protected static final Object[] NO_CHILDREN = new Object[0];

  private DartElementImageProvider fImageProvider;

  public DartWorkbenchAdapter() {
    fImageProvider = new DartElementImageProvider();
  }

  @Override
  public Object[] getChildren(Object element) {
    DartElement de = getDartElement(element);
    if (de instanceof ParentElement) {
      try {
        return ((ParentElement) de).getChildren();
      } catch (DartModelException e) {
        DartToolsPlugin.log(e);
      }
    }
    return NO_CHILDREN;
  }

  @Override
  public ImageDescriptor getImageDescriptor(Object element) {
    DartElement de = getDartElement(element);
    if (de != null) {
      return fImageProvider.getDartImageDescriptor(de, DartElementImageProvider.OVERLAY_ICONS
          | DartElementImageProvider.SMALL_ICONS);
    }
    return null;
  }

  @Override
  public String getLabel(Object element) {
    return DartElementLabels.getTextLabel(getDartElement(element), DartElementLabels.ALL_DEFAULT);
  }

  @Override
  public Object getParent(Object element) {
    DartElement de = getDartElement(element);
    return de != null ? de.getParent() : null;
  }

  private DartElement getDartElement(Object element) {
    if (element instanceof DartElement) {
      return (DartElement) element;
    }
    return null;
  }
}
