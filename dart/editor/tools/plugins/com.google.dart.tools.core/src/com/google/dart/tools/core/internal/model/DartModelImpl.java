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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartModelInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class <code>DartModelImpl</code> implement the root Dart element corresponding
 * to the workspace.
 */
public class DartModelImpl extends OpenableElementImpl implements DartModel {
  private DartProjectImpl externalProject;

  /**
   * Initialize a newly created model for the root of the Dart element structure.
   */
  public DartModelImpl() {
    super(null);
  }

  @Override
  public boolean contains(IResource resource) {
    switch (resource.getType()) {
      case IResource.ROOT:
      case IResource.PROJECT:
        return true;
    }
    // file or folder
    DartProject[] projects;
    try {
      projects = getDartProjects();
    } catch (DartModelException e) {
      return false;
    }
    for (int i = 0, length = projects.length; i < length; i++) {
      DartProjectImpl project = (DartProjectImpl) projects[i];
      if (!project.contains(resource)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void copy(DartElement[] elements, DartElement[] containers, DartElement[] siblings,
      String[] renamings, boolean replace, IProgressMonitor monitor) throws DartModelException {
    DartCore.notYetImplemented();
  }

  @Override
  public void delete(DartElement[] elements, boolean force, IProgressMonitor monitor)
      throws DartModelException {
    DartCore.notYetImplemented();
  }

  /**
   * Return the bundled compilation unit with the given URI, or <code>null</code> if there is no
   * such bundled compilation unit. If the returned value is not <code>null</code>, it will always
   * be the case that
   * <code>getBundledCompilationUnit(uri).getSourceRef().getUri().equals(uri)</code> returns
   * <code>true</code>.
   * 
   * @param uri the URI of the compilation unit to be returned
   * @return the external compilation unit with the given URI
   */
  public ExternalCompilationUnitImpl getBundledCompilationUnit(URI uri) throws DartModelException {
    for (DartLibrary library : getBundledLibraries()) {
      for (CompilationUnit unit : library.getCompilationUnits()) {
        URI unitUri = unit.getSourceRef().getUri();
        if (uri.equals(unitUri)) {
          return (ExternalCompilationUnitImpl) unit;
        }
      }
    }
    return null;
  }

  @Override
  public DartLibrary[] getBundledLibraries() throws DartModelException {
    return ((DartModelInfo) getElementInfo()).getBundledLibraries();
  }

  @Override
  public DartLibrary getCoreLibrary() throws DartModelException {
    return ((DartModelInfo) getElementInfo()).getCoreLibrary();
  }

  @Override
  public IResource getCorrespondingResource() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  /**
   * Return an array containing all of the libraries that are currently defined in the workspace.
   * 
   * @return all of the libraries that are currently defined in the workspace
   * @throws DartModelException if the libraries cannot be determined for some reason
   */
  public List<DartLibrary> getDartLibraries() throws DartModelException {
    List<DartLibrary> libraries = new ArrayList<DartLibrary>();
    for (DartProject project : getDartProjects()) {
      for (DartLibrary library : project.getDartLibraries()) {
        libraries.add(library);
      }
    }
    return libraries;
  }

  @Override
  public DartModel getDartModel() {
    return this;
  }

  @Override
  public DartProject getDartProject(IResource resource) {
    if (resource != null) {
      IProject project = resource.getProject();
      if (project != null) {
        return new DartProjectImpl(this, project);
      }
    }
    throw new IllegalArgumentException("Cannot create dart project from an instance of "
        + resource.getClass().getName());
  }

  @Override
  public DartProject getDartProject(String projectName) {
    if (ExternalDartProject.EXTERNAL_PROJECT_NAME.equals(projectName)) {
      return getExternalProject();
    }
    return new DartProjectImpl(this, ResourcesPlugin.getWorkspace().getRoot().getProject(
        projectName));
  }

  @Override
  public DartProject[] getDartProjects() throws DartModelException {
    List<DartProject> children = getChildrenOfType(DartProject.class);
    return children.toArray(new DartProject[children.size()]);
  }

  @Override
  public String getElementName() {
    return "";
  }

  @Override
  public int getElementType() {
    return DartElement.DART_MODEL;
  }

  /**
   * Answer a Dart project to contain Dart libraries on disk but not mapped into the Eclipse
   * workspace.
   */
  public DartProjectImpl getExternalProject() {
    if (externalProject == null) {
      externalProject = new ExternalDartProject();
    }
    return externalProject;
  }

  @Override
  public IResource[] getNonDartResources() throws DartModelException {
    return ((DartModelInfo) getElementInfo()).getNonDartResources();
  }

  @Override
  public IResource getUnderlyingResource() {
    return null;
  }

  @Override
  public List<DartLibrary> getUnreferencedLibraries() throws DartModelException {
    // The libraries that have not yet been proven to be referenced.
    List<DartLibrary> unreferenced = new ArrayList<DartLibrary>();
    // The libraries that are referenced but whose imports have not yet been examined.
    List<DartLibrary> referenced = new ArrayList<DartLibrary>();
    for (DartProject project : getChildrenOfType(DartProject.class)) {
      for (DartLibrary library : project.getDartLibraries()) {
        if (library.isTopLevel()) {
          referenced.add(library);
        } else {
          unreferenced.add(library);
        }
      }
    }
    // Process the libraries until there are no more libraries to be processed or until it becomes
    // pointless because all of the libraries have already been proven to be unreferenced.
    while (!referenced.isEmpty() && !unreferenced.isEmpty()) {
      DartLibrary referencedLibrary = referenced.remove(0);
      for (DartLibrary importedLibrary : referencedLibrary.getImportedLibraries()) {
        if (unreferenced.contains(importedLibrary)) {
          unreferenced.remove(importedLibrary);
          referenced.add(importedLibrary);
        }
      }
    }
    return unreferenced;
  }

  @Override
  public IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  @Override
  public void move(DartElement[] elements, DartElement[] containers, DartElement[] siblings,
      String[] renamings, boolean replace, IProgressMonitor monitor) throws DartModelException {
    DartCore.notYetImplemented();
  }

  @Override
  public void rename(DartElement[] elements, DartElement[] destinations, String[] names,
      boolean replace, IProgressMonitor monitor) throws DartModelException {
    DartCore.notYetImplemented();
  }

  @Override
  public IResource resource() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  @Override
  protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm,
      Map<DartElement, DartElementInfo> newElements, IResource underlyingResource)
      throws DartModelException {
    // determine my children
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    int length = projects.length;
    DartElement[] children = new DartElement[length];
    int index = 0;
    for (int i = 0; i < length; i++) {
      IProject project = projects[i];
      if (DartProjectNature.hasDartNature(project)) {
        children[index++] = getDartProject(project);
      }
    }
    if (index < length) {
      System.arraycopy(children, 0, children = new DartElement[index], 0, index);
    }
    info.setChildren(children);
    newElements.put(this, info);
    return true;
  }

  @Override
  protected DartElementInfo createElementInfo() {
    return new DartModelInfo();
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    if (token.charAt(0) == MEMENTO_DELIMITER_PROJECT) {
      if (!tokenizer.hasMoreTokens()) {
        return this;
      }
      DartProjectImpl project = (DartProjectImpl) getDartProject(tokenizer.nextToken());
      return project.getHandleFromMemento(tokenizer, owner);
    }
    return null;
  }

  @Override
  protected void getHandleMemento(StringBuilder builder) {
    // Because there is only one model and it is always at the root of the
    // hierarchy, it does not need to be represented in a handle memento.
    return;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected IStatus validateExistence(IResource underlyingResource) {
    // Dart model always exists
    return DartModelStatusImpl.VERIFIED_OK;
  }
}
