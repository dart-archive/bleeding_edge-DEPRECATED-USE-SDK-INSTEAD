/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.core.resources.IResource;

/**
 * This class locates different resources which are related to an object
 */
public class ResourceLocator implements IResourceLocator {

  @Override
  public IResource getContainingResource(Object element) throws DartModelException {
    IResource resource = null;
    if (element instanceof IResource) {
      resource = (IResource) element;
    }
    return resource;
  }

  @Override
  public IResource getCorrespondingResource(Object element) throws DartModelException {
    return null;
  }

  @Override
  public IResource getUnderlyingResource(Object element) throws DartModelException {
    return null;
  }
}
