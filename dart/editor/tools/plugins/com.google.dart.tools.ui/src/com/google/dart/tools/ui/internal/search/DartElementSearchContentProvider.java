/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.search;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.ParentElement;
import com.google.dart.tools.ui.ImportedDartLibrary;
import com.google.dart.tools.ui.ImportedDartLibraryContainer;
import com.google.dart.tools.ui.StandardDartElementContentProvider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.WeakHashMap;

/**
 * A base content provider for Dart elements tailored to presenting elements for search (e.g., in
 * search results).
 * <p>
 * Based on {@link StandardDartElementContentProvider} and modified to suit.
 * <p>
 * TODO(pquitslund): cleanup and decruft
 * <p>
 * TODO(pquitslund): update library children to be files (rather than dart elements)
 */
public class DartElementSearchContentProvider implements ITreeContentProvider {

  /**
   * This boolean is set to <code>true</code> if we want the "Imported Libraries" element to be
   * under each library, and <code>false</code> if we instead just want the libraries listed out,
   * aka: a flatter tree.
   * <p>
   * This boolean was introduced to make it easy to switch between behaviors if we change our mind
   * in the future.
   * 
   * @see ImportedDartLibraryContainer
   */
  private static final boolean INCLUDE_IMPORT_LIBS_ELEMENT = false;

  protected static final Object[] NO_CHILDREN = new Object[0];

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   */
  protected static Object[] concatenate(Object[] a1, Object[] a2) {
    int a1Len = a1.length;
    int a2Len = a2.length;
    if (a1Len == 0) {
      return a2;
    }
    if (a2Len == 0) {
      return a1;
    }
    Object[] res = new Object[a1Len + a2Len];
    System.arraycopy(a1, 0, res, 0, a1Len);
    System.arraycopy(a2, 0, res, a1Len, a2Len);
    return res;
  }

  protected boolean provideMembers;

  protected final boolean libsTopLevel;

  /**
   * A table being used to implement a weak set containing all of the library containers that are
   * currently known.
   */
  private final WeakHashMap<ImportedDartLibraryContainer, Boolean> libraryContainers = new WeakHashMap<ImportedDartLibraryContainer, Boolean>();

  /**
   * Creates a new content provider. The content provider does not provide members of compilation
   * units or class files.
   */
  public DartElementSearchContentProvider() {
    this(true);
  }

  /**
   * Creates a new <code>StandardDartElementContentProvider</code>.
   * 
   * @param provideMembers if <code>true</code>, members below compilation units are provided
   */
  public DartElementSearchContentProvider(boolean provideMembers) {
    this(provideMembers, true);
  }

  /**
   * Creates a new <code>StandardDartElementContentProvider</code>.
   * 
   * @param provideMembers if <code>true</code>, members below compilation units are provided
   * @param libsTopLevel of <code>true</code>, then the children of a workspace will be the
   *          libraries in any contained project, not the set of projects
   */
  public DartElementSearchContentProvider(boolean provideMembers, boolean libsTopLevel) {
    this.provideMembers = provideMembers;
    this.libsTopLevel = libsTopLevel;
  }

  @Override
  public void dispose() {
  }

  @Override
  public Object[] getChildren(Object element) {
    if (!exists(element)) {
      return NO_CHILDREN;
    }
    try {
      // if libraries are our top level element, libsTopLevel is true, handle this case first
      if (element instanceof DartModel) {
        if (libsTopLevel) {
          DartModel model = (DartModel) element;
          ArrayList<DartElement> childrenList = new ArrayList<DartElement>();
          Object[] projects = getDartProjects(model);
          for (Object obProject : projects) {
            DartProject dartProject = (DartProject) obProject;
            DartLibrary[] libraries = dartProject.getDartLibraries();
            for (DartLibrary dartLibrary : libraries) {
              if (dartLibrary.isTopLevel()) {
                childrenList.add(dartLibrary);
              }
            }
          }
          return childrenList.toArray(new DartElement[childrenList.size()]);
        } else {
          return getDartProjects((DartModel) element);
        }
      }
      if (element instanceof IProject) {
        IProject project = (IProject) element;
        if (project.hasNature(DartCore.DART_PROJECT_NATURE)) {
          DartProject dartProject = DartCore.create(project);
          DartLibrary[] libraries = dartProject.getDartLibraries();
          return libraries;
        }
      }
      if (element instanceof ImportedDartLibraryContainer) {
        return ((ImportedDartLibraryContainer) element).getDartLibraries();
      }
      if (element instanceof ImportedDartLibrary) {
        DartLibrary dartLibrary = ((ImportedDartLibrary) element).getDartLibrary();
        // if this DartLibrary is local, then include its' children, otherwise
        // return no children, this prevents locally defined libraries from
        // being listed out unnecessarily.
        if (!dartLibrary.isTopLevel()) {
          return getDartLibraryChildren(element, dartLibrary);
        }
      }
      if (element instanceof DartLibrary) {
        return getDartLibraryChildren(element, (DartLibrary) element);
      }
      if (element instanceof DartFunction) {
        return NO_CHILDREN;
      }
      if (element instanceof ParentElement) {
        ParentElement parent = (ParentElement) element;
        DartElement[] children = parent.getChildren();
        //filter out default constructors
        return Collections2.filter(Lists.newArrayList(children), new Predicate<DartElement>() {
          @Override
          public boolean apply(DartElement elem) {
            if (elem instanceof Method) {
              return !((Method) elem).isImplicit();
            }
            return true;
          }
        }).toArray();
      }
      if (element instanceof IFolder) {
        return getFolderContent((IFolder) element);
      }
      if (element instanceof IFile) {
        return getFileContent((IFile) element);
      }
    } catch (CoreException ce) {
      return NO_CHILDREN;
    }
    return NO_CHILDREN;
  }

  @Override
  public Object[] getElements(Object parent) {
    return getChildren(parent);
  }

  @Override
  public Object getParent(Object element) {
    if (!exists(element)) {
      return null;
    }
    Object parent = internalGetParent(element);
    if (parent instanceof DartProject) {
      return ((DartProject) parent).getProject();
    }
    return parent;
  }

  /**
   * Returns whether members are provided when asking for a compilation units or class file for its
   * children.
   * 
   * @return <code>true</code> if the content provider provides members; otherwise
   *         <code>false</code> is returned
   */
  public boolean getProvideMembers() {
    return provideMembers;
  }

  @Override
  public boolean hasChildren(Object element) {
    if (getProvideMembers()) {
      // assume CUs and class files are never empty
      if (element instanceof CompilationUnit) {
        try {
          if (element instanceof CompilationUnit) {
            CompilationUnit cu = (CompilationUnit) element;
            return cu.getTypes().length > 0;
          }
        } catch (DartModelException ex) {
          return false;
        }

        return true;
      }
    } else {
      // don't allow to drill down into a compilation unit
      if (element instanceof CompilationUnit || element instanceof IFile) {
        return false;
      }
    }

    if (element instanceof DartProject) {
      DartProject project = (DartProject) element;
      if (!project.getProject().isOpen()) {
        return false;
      }
    }

    if (element instanceof IProject) {
      IProject p = (IProject) element;
      if (!p.isOpen()) {
        return false;
      }
    }

    // never return the children of a function (or method)
    if (element instanceof DartFunction) {
      return false;
    }

    if (element instanceof ParentElement) {
      try {
        // when we have children, return true, else we fetch all the children
        if (((ParentElement) element).hasChildren()) {
          return true;
        }
      } catch (DartModelException e) {
        return true;
      }
    }
    Object[] children = getChildren(element);
    return children != null && children.length > 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }

  /**
   * Sets whether the content provider is supposed to return members when asking a compilation unit
   * for its children.
   * 
   * @param provideMembers if <code>true</code> then members are provided. If <code>false</code>
   *          compilation units and class files are the leaves provided by this content provider.
   */
  public void setProvideMembers(boolean provideMembers) {
    this.provideMembers = provideMembers;
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   */
  protected boolean exists(Object element) {
    if (element == null) {
      return false;
    }
    if (element instanceof IResource) {
      return ((IResource) element).exists();
    }
    if (element instanceof DartElement) {
      return ((DartElement) element).exists();
    }
    return true;
  }

  protected DartElement[] filter(DartElement[] children) {
    boolean initializers = false;
    for (int i = 0; i < children.length; i++) {
      if (matches(children[i])) {
        initializers = true;
        break;
      }
    }

    if (!initializers) {
      return children;
    }

    Vector<DartElement> v = new Vector<DartElement>();
    for (int i = 0; i < children.length; i++) {
      if (matches(children[i])) {
        continue;
      }
      v.addElement(children[i]);
    }

    DartElement[] result = new DartElement[v.size()];
    v.copyInto(result);
    return result;
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   */
  protected Object[] getDartProjects(DartModel dm) throws DartModelException {
    return dm.getDartProjects();
  }

  /**
   * Evaluates all children of a given {@link IFolder}. Clients can override this method.
   * 
   * @param directory The folder to evaluate the children for.
   * @return The children of the given package fragment.
   * @exception CoreException if the folder does not exist.
   */
  protected Object[] getFileContent(IFile file) throws CoreException {
    DartElement element = DartCore.create(file);
    if (element instanceof ParentElement) {
      return ((ParentElement) element).getChildren();
    }
    return NO_CHILDREN;
  }

  /**
   * Evaluates all children of a given {@link IFolder}. Clients can override this method.
   * <p>
   * This version only returns resources as children if they <b>aren't</b> in an app or lib.
   * 
   * @param folder The folder to evaluate the children for.
   * @return The children of the given package fragment.
   * @exception CoreException if the folder does not exist.
   */
  protected Object[] getFolderContent(IFolder folder) throws CoreException {
    IResource[] folderMembers = folder.members();
    List<Object> resultMembers = new ArrayList<Object>();
    DartProject dartProject = DartCore.create(folder.getProject());
    if (dartProject == null || !dartProject.exists()) {
      return folderMembers;
    } else {
      // Add each resource under this IFolder if and only if there is not a
      // DartElement associated with the resource, this way only resources not
      // in an app or lib will be returned.
      for (IResource resource : folderMembers) {
        DartElement dartElement = DartCore.create(resource);
        if (dartElement == null) {
          resultMembers.add(resource);
        }
      }
    }
    return resultMembers.toArray(new Object[resultMembers.size()]);
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   */
  protected Object internalGetParent(Object element) {
    // try to map resources to the containing package fragment
    if (element instanceof IResource) {
      IResource parent = ((IResource) element).getParent();
      DartElement dParent = DartCore.create(parent);
      // http://bugs.eclipse.org/bugs/show_bug.cgi?id=31374
      if (dParent != null && dParent.exists()) {
        return dParent;
      }
      return parent;
    } else if (element instanceof ExternalCompilationUnitImpl) {
      DartLibrary targetLibrary = ((ExternalCompilationUnitImpl) element).getLibrary();
      return getParentLibrary(targetLibrary);

    } else if (element instanceof CompilationUnit) {
      DartLibrary targetLibrary = ((CompilationUnitImpl) element).getLibrary();
      if (targetLibrary.isTopLevel()) {
        return targetLibrary;
      } else {
        return getParentLibrary(targetLibrary);
      }

    } else if (element instanceof DartLibrary) {
      return null;
    } else if (element instanceof DartElement) {
      return ((DartElement) element).getParent();
    } else if (element instanceof ImportedDartLibraryContainer) {
      return ((ImportedDartLibraryContainer) element).getParent();
    }

    return null;
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   */
  protected boolean isClassPathChange(DartElementDelta delta) {

    // need to test the flags only for package fragment roots
    // if (delta.getElement().getElementType() !=
    // DartElement.PACKAGE_FRAGMENT_ROOT)
    // return false;

    int flags = delta.getFlags();
    return delta.getKind() == DartElementDelta.CHANGED && (flags & DartElementDelta.F_REORDER) != 0;
  }

  /*
   * Anonymous -- matches anonymous types on the top level
   */
  protected boolean matches(DartElement element) {

    // if (element.getElementType() == DartElement.TYPE
    // && (element.getParent().getElementType() ==
    // DartElement.COMPILATION_UNIT)) {
    //
    // Type type = (Type) element;
    // try {
    // return type.isAnonymous();
    // } catch (DartModelException e) {
    // e.printStackTrace();
    // }
    // }

    return false;
  }

  /**
   * Given some {@link DartLibrary}, this method returns its children elements.
   * <p>
   * The children are the union of the elements returned from <code>dartLibrary.getChildren()</code>
   * and an {@link ImportedDartLibraryContainer} object to list out the imported Dart libraries.
   */
  private Object[] getDartLibraryChildren(Object parent, DartLibrary dartLibrary)
      throws DartModelException {
    DartElement[] dartLibraryChildren = dartLibrary.getChildren();
    List<Object> children = new ArrayList<Object>(dartLibraryChildren.length + 1);
    ImportedDartLibraryContainer importedLibsContainer = new ImportedDartLibraryContainer(
        parent,
        dartLibrary);
    // Only add this LibrariesContainer if it has children.
    if (importedLibsContainer.hasChildren()) {
      if (INCLUDE_IMPORT_LIBS_ELEMENT) {
        children.add(importedLibsContainer);
      } else {
        ImportedDartLibrary[] libs = importedLibsContainer.getDartLibraries();
        for (ImportedDartLibrary importedDartLibrary : libs) {
          children.add(importedDartLibrary);
        }
      }
      libraryContainers.put(importedLibsContainer, Boolean.TRUE);
    }
    for (DartElement dartElement : dartLibraryChildren) {
      children.add(dartElement);
    }
    return children.toArray(new Object[children.size()]);
  }

  private ImportedDartLibrary getParentLibrary(DartLibrary targetLibrary) {
    URI targetUri = ImportedDartLibraryContainer.getUri(targetLibrary);
    for (ImportedDartLibraryContainer container : libraryContainers.keySet()) {
      for (ImportedDartLibrary importedLibrary : container.getDartLibraries()) {
        URI importedUri = ImportedDartLibraryContainer.getUri(importedLibrary.getDartLibrary());
        if (targetUri.equals(importedUri)) {
          return importedLibrary;
        }
      }
    }
    DartModel model = DartCore.create(ResourcesPlugin.getWorkspace().getRoot());
    try {
      DartProject[] projects = model.getDartProjects();
      // Try to find import of "targetLibrary" in one of the top-level libraries.
      for (DartProject project : projects) {
        for (DartLibrary library : project.getDartLibraries()) {
          if (library.isTopLevel()) {
            for (DartLibrary importedLibrary : library.getImportedLibraries()) {
              URI importedUri = ImportedDartLibraryContainer.getUri(importedLibrary);
              if (targetUri.equals(importedUri)) {
                return new ImportedDartLibrary(importedLibrary, new ImportedDartLibraryContainer(
                    library,
                    library));
              }
            }
          }
        }
      }
      // Try to find "targetLibrary" as one of the libraries bundled with Editor: core, dom, etc.
      if (projects.length > 0) {
        DartLibrary[] libraries = projects[0].getDartLibraries();
        if (libraries.length > 0) {
          DartLibrary[] bundledLibraries = model.getBundledLibraries();
          for (DartLibrary bundledLibrary : bundledLibraries) {
            URI bundledUri = ImportedDartLibraryContainer.getUri(bundledLibrary);
            if (targetUri.equals(bundledUri)) {
              return new ImportedDartLibrary(bundledLibrary, new ImportedDartLibraryContainer(
                  libraries[0],
                  libraries[0]));
            }
          }
        }
      }
    } catch (DartModelException exception) {
      // Ignored. Simply fall through to return null.
    }
    return null;
  }

}
