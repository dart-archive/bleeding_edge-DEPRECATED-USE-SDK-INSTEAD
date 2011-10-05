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

import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;

/**
 * An adapter factory for DartProjects.
 */
public class DartProjectAdapterFactory implements IAdapterFactory {

  private static Class<?>[] PROPERTIES = new Class<?>[] {IProject.class,};

  @Override
  public Object getAdapter(Object element, @SuppressWarnings("rawtypes") Class key) {
    if (IProject.class.equals(key)) {
      DartProject dartProject = (DartProject) element;
      return dartProject.getProject();
    }
    return null;
  }

  @Override
  public Class<?>[] getAdapterList() {
    return PROPERTIES;
  }
}
