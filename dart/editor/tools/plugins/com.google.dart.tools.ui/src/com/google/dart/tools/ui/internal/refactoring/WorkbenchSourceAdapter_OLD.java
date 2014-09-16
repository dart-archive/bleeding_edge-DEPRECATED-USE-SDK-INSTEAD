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

package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.engine.source.Source;
import com.google.dart.tools.ui.DartPluginImages;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Adapter of {@link Source} to {@link IWorkbenchAdapter}.
 */
public class WorkbenchSourceAdapter_OLD implements IWorkbenchAdapter {
  private final Source source;

  public WorkbenchSourceAdapter_OLD(Source source) {
    this.source = source;
  }

  @Override
  public Object[] getChildren(Object o) {
    return ArrayUtils.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public ImageDescriptor getImageDescriptor(Object object) {
    return DartPluginImages.getDescriptor(DartPluginImages.IMG_OBJS_CUNIT);
  }

  @Override
  public String getLabel(Object o) {
    return source.getShortName();
  }

  @Override
  public Object getParent(Object o) {
    return null;
  }
}
