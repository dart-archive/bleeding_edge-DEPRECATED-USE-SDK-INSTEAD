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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.ui.internal.DartWorkbenchAdapter;
import com.google.dart.tools.ui.internal.IResourceLocator;
import com.google.dart.tools.ui.internal.Logger;
import com.google.dart.tools.ui.internal.ResourceLocator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IContainmentAdapter;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.ide.IContributorResourceAdapter2;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.properties.FilePropertySource;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.ResourcePropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.osgi.framework.Bundle;

/**
 * Implements basic UI support for {@link DartElement}s. Implements handle to persistent support for
 * Dart elements.
 */
public class DartElementAdapterFactory implements IAdapterFactory, IContributorResourceAdapter2 {

  private static Class<?>[] ADAPTER_LIST = new Class[] {
      IPropertySource.class, IResource.class, IWorkbenchAdapter.class, IResourceLocator.class,
      IPersistableElement.class, IContributorResourceAdapter.class,
      IContributorResourceAdapter2.class, ITaskListResourceAdapter.class, IContainmentAdapter.class};

  @SuppressWarnings("unused")
  private static void addClassToAdapterList(Class<?> clazz) {
    int oldSize = ADAPTER_LIST.length;
    Class<?>[] oldProperties = ADAPTER_LIST;
    ADAPTER_LIST = new Class[oldSize + 1];
    System.arraycopy(oldProperties, 0, ADAPTER_LIST, 0, oldSize);
    ADAPTER_LIST[oldSize] = clazz;
  }

//  private static JavaElementContainmentAdapter getJavaElementContainmentAdapter() {
//    if (fgJavaElementContainmentAdapter == null) {
//      fgJavaElementContainmentAdapter = new JavaElementContainmentAdapter();
//    }
//    return fgJavaElementContainmentAdapter;
//  }

  private static DartWorkbenchAdapter getDartWorkbenchAdapter() {
    if (fgDartWorkbenchAdapter == null) {
      fgDartWorkbenchAdapter = new DartWorkbenchAdapter();
    }
    return fgDartWorkbenchAdapter;
  }

  private static IResourceLocator getResourceLocator() {
    if (fgResourceLocator == null) {
      fgResourceLocator = new ResourceLocator();
    }
    return fgResourceLocator;
  }

//  private static ITaskListResourceAdapter getTaskListAdapter() {
//    if (fgTaskListAdapter == null) {
//      fgTaskListAdapter = new JavaTaskListAdapter();
//    }
//    return fgTaskListAdapter;
//  }

  @SuppressWarnings("unused")
  private static boolean isTeamUIPlugInActivated() {
    return Platform.getBundle("org.eclipse.team.ui").getState() == Bundle.ACTIVE; //$NON-NLS-1$
  }

  /*
   * Do not use real type since this would cause the Search plug-in to be loaded.
   */
  private Object fSearchPageScoreComputer;

  @SuppressWarnings("unused")
  private boolean fIsTeamUILoaded;

  private static IResourceLocator fgResourceLocator;

  private static DartWorkbenchAdapter fgDartWorkbenchAdapter;

  @SuppressWarnings("unused")
  private static ITaskListResourceAdapter fgTaskListAdapter;

  //private static JavaElementContainmentAdapter fgJavaElementContainmentAdapter;

  @Override
  public IResource getAdaptedResource(IAdaptable adaptable) {
    DartElement de = getDartElement(adaptable);
    if (de != null) {
      return getResource(de);
    }

    return null;
  }

  @Override
  public ResourceMapping getAdaptedResourceMapping(IAdaptable adaptable) {
//    DartElement de = getDartElement(adaptable);
    DartCore.notYetImplemented();
//    if (de != null) {
//      return JavaElementResourceMapping.create(de);
//    }

    return null;
  }

  @Override
  public Object getAdapter(Object element, @SuppressWarnings("rawtypes") Class key) {
    updateLazyLoadedAdapters();
    DartElement dart = getDartElement(element);

    if (IPropertySource.class.equals(key)) {
      return getProperties(dart);
    }
    if (IResource.class.equals(key)) {
      return getResource(dart);
    }
//    if (fSearchPageScoreComputer != null && ISearchPageScoreComputer.class.equals(key)) {
//      return fSearchPageScoreComputer;
//    }
    if (IWorkbenchAdapter.class.equals(key)) {
      return getDartWorkbenchAdapter();
    }
    if (IResourceLocator.class.equals(key)) {
      return getResourceLocator();
    }
    DartCore.notYetImplemented();
//    if (IPersistableElement.class.equals(key)) {
//      return new PersistableJavaElementFactory(java);
//    }
    if (IContributorResourceAdapter.class.equals(key)) {
      return this;
    }
    if (IContributorResourceAdapter2.class.equals(key)) {
      return this;
    }
    DartCore.notYetImplemented();
//    if (ITaskListResourceAdapter.class.equals(key)) {
//      return getTaskListAdapter();
//    }
//    if (IContainmentAdapter.class.equals(key)) {
//      return getJavaElementContainmentAdapter();
//    }
//    if (fIsTeamUILoaded && IHistoryPageSource.class.equals(key)
//        && JavaElementHistoryPageSource.hasEdition(java)) {
//      return JavaElementHistoryPageSource.getInstance();
//    }
    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Class[] getAdapterList() {
    updateLazyLoadedAdapters();
    return ADAPTER_LIST;
  }

  @SuppressWarnings("unused")
  private void createSearchPageScoreComputer() {
    DartCore.notYetImplemented();
    //fSearchPageScoreComputer = new JavaSearchPageScoreComputer();
    //addClassToAdapterList(ISearchPageScoreComputer.class);
  }

  private DartElement getDartElement(Object element) {
    if (element instanceof DartElement) {
      return (DartElement) element;
    }
    return null;
  }

  private IPropertySource getProperties(DartElement element) {
    IResource resource = getResource(element);
    if (resource == null) {
      DartCore.notYetImplemented();
      //return new JavaElementProperties(element);
      return new ResourcePropertySource(resource);
    }
    if (resource.getType() == IResource.FILE) {
      return new FilePropertySource((IFile) resource);
    }
    return new ResourcePropertySource(resource);
  }

  private IResource getResource(DartElement element) {
    if (element == null) {
      return null;
    }
    // We can't use DartElement.getResource directly as we are interested in the corresponding
    // resource.
    switch (element.getElementType()) {
      case DartElement.HTML_FILE:
        try {
          return ((HTMLFile) element).getUnderlyingResource();
        } catch (DartModelException e) {
          Logger.logException(e);
          return null;
        }
      case DartElement.TYPE:
        // top level types behave like the CU
        DartElement parent = element.getParent();
        if (parent instanceof CompilationUnit) {
          return ((CompilationUnit) parent).getPrimary().getResource();
        }
        return null;
      case DartElement.COMPILATION_UNIT:
        return ((CompilationUnit) element).getPrimary().getResource();
      case DartElement.DART_PROJECT:
      case DartElement.DART_MODEL:
        return element.getResource();
      default:
        return null;
    }
  }

  private void updateLazyLoadedAdapters() {
    DartCore.notYetImplemented();
//    if (fSearchPageScoreComputer == null
//        && SearchUtil.isSearchPlugInActivated()) {
//      createSearchPageScoreComputer();
//    }
//    if (!fIsTeamUILoaded && isTeamUIPlugInActivated()) {
//      addClassToAdapterList(IHistoryPageSource.class);
//      fIsTeamUILoaded = true;
//    }
  }
}
