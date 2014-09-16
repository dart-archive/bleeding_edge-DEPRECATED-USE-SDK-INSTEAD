/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.tools.core.utilities.io.FilenameUtils;
import com.google.dart.tools.ui.DartPluginImages;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

import java.io.File;

/**
 * Adapter of {@link File} to {@link IWorkbenchAdapter}.
 */
public class WorkbenchSourceAdapter_NEW implements IWorkbenchAdapter {
  private final String file;

  public WorkbenchSourceAdapter_NEW(String file) {
    this.file = file;
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
    return FilenameUtils.getName(file);
  }

  @Override
  public Object getParent(Object o) {
    return null;
  }
}
