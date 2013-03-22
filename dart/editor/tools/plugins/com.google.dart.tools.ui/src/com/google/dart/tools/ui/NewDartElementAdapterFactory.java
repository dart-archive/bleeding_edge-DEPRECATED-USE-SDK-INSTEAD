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
package com.google.dart.tools.ui;

import com.google.dart.engine.element.Element;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.internal.IResourceLocator;
import com.google.dart.tools.ui.internal.NewDartWorkbenchAdapter;
import com.google.dart.tools.ui.internal.ResourceLocator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IContainmentAdapter;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.ide.IContributorResourceAdapter2;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.properties.FilePropertySource;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.ResourcePropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;

/**
 * Implements basic UI adapter support for {@link Element}s.
 * <p>
 * To replace {@link DartElementAdapterFactory}.
 */
public class NewDartElementAdapterFactory implements IAdapterFactory, IContributorResourceAdapter2 {

  private static Class<?>[] ADAPTER_LIST = new Class[] {
      IPropertySource.class, IResource.class, IWorkbenchAdapter.class, IResourceLocator.class,
      IPersistableElement.class, IContributorResourceAdapter.class,
      IContributorResourceAdapter2.class, ITaskListResourceAdapter.class, IContainmentAdapter.class};

  private static IResourceLocator resourceLocator;
  private static IWorkbenchAdapter dartWorkbenchAdapter;

  private static IWorkbenchAdapter getDartWorkbenchAdapter() {
    if (dartWorkbenchAdapter == null) {
      dartWorkbenchAdapter = new NewDartWorkbenchAdapter();
    }
    return dartWorkbenchAdapter;
  }

  private static IResourceLocator getResourceLocator() {
    if (resourceLocator == null) {
      resourceLocator = new ResourceLocator();
    }
    return resourceLocator;
  }

  @Override
  public IResource getAdaptedResource(IAdaptable adaptable) {

    Element de = getDartElement(adaptable);
    if (de != null) {
      return getResource(de);
    }

    return null;
  }

  @Override
  public ResourceMapping getAdaptedResourceMapping(IAdaptable adaptable) {
    return null;
  }

  @Override
  public Object getAdapter(Object elem, @SuppressWarnings("rawtypes") Class key) {

    Element element = getDartElement(elem);

    if (IPropertySource.class.equals(key)) {
      return getProperties(element);
    }
    if (IResource.class.equals(key)) {
      return getResource(element);
    }
    if (IWorkbenchAdapter.class.equals(key)) {
      return getDartWorkbenchAdapter();
    }
    if (IResourceLocator.class.equals(key)) {
      return getResourceLocator();
    }
    if (IContributorResourceAdapter.class.equals(key)) {
      return this;
    }
    if (IContributorResourceAdapter2.class.equals(key)) {
      return this;
    }

    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Class[] getAdapterList() {
    return ADAPTER_LIST;
  }

  private Element getDartElement(Object element) {
    if (element instanceof Element) {
      return (Element) element;
    }
    return null;
  }

  private IPropertySource getProperties(Element element) {
    IResource resource = getResource(element);
    if (resource == null) {
      return null;
    }
    if (resource.getType() == IResource.FILE) {
      return new FilePropertySource((IFile) resource);
    }
    return new ResourcePropertySource(resource);
  }

  private IResource getResource(Element element) {
    return DartCore.getProjectManager().getResource(element.getSource());
  }

}
