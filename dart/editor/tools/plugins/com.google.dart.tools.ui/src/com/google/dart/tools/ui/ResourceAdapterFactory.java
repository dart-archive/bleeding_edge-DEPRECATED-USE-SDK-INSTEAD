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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartElement;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.part.FileEditorInput;

public class ResourceAdapterFactory implements IAdapterFactory {

  private static Class<?>[] PROPERTIES = new Class<?>[] {DartElement.class};

  @Override
  public Object getAdapter(Object element, @SuppressWarnings("rawtypes") Class key) {
    if (DartElement.class.equals(key)) {

      // Performance optimization, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=133141
      if (element instanceof IFile) {
        DartElement de = DartToolsPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(
            new FileEditorInput((IFile) element));
        if (de != null) {
          return de;
        }
      }

      return DartCore.create((IResource) element);
    }
    return null;
  }

  @Override
  public Class<?>[] getAdapterList() {
    return PROPERTIES;
  }
}
