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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartLibraryInfo;
import com.google.dart.tools.core.internal.model.info.DartProjectInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.internal.util.LibraryReferenceFinder;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.core.utilities.resource.IFileUtilities;
import com.google.dart.tools.core.utilities.resource.IResourceUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class <code>DartProjectImpl</code> implement a project that has a Dart nature.
 */
public class DartProjectImpl extends OpenableElementImpl implements DartProject {
  /**
   * Listen for changes to our preferences and reset any cached information.
   */
  private IEclipsePreferences.IPreferenceChangeListener preferencesChangeListener;

  /**
   * Preferences listeners
   */
  private IEclipsePreferences.INodeChangeListener preferencesNodeListener;

  /**
   * The project being represented by this object.
   */
  private IProject project;

  /**
   * The name of the file that contains the project-relative paths of the children of the project.
   */
  private static final String CHILDREN_FILE_NAME = ".children";

  /**
   * Initialize a newly created Dart project to represent the given project.
   * 
   * @param parent the parent of this project
   * @param project the project being represented by this object
   */
  public DartProjectImpl(DartModelImpl parent, IProject project) {
    super(parent);
    this.project = project;
  }

  /**
   * This should be called when a project is created or opened and immediately before it is closed
   * or deleted to clear any cached {@link DartLibraryInfo} instances as they change as a result of
   * either being mapped into the workspace or not.
   */
  public void clearLibraryInfo() {
    DartModelManager manager = DartModelManager.getInstance();
    manager.removeLibraryInfoAndChildren(project.getLocationURI());
    IResource[] members;
    try {
      members = project.members();
    } catch (CoreException exception) {
      if (project.isOpen()) {
        DartCore.logError("Failed to get project members: " + project, exception); //$NON-NLS-1$
      }
      return;
    }
    for (IResource child : members) {
      if (child instanceof IFolder) {
        if (((IFolder) child).isLinked()) {
          manager.removeLibraryInfoAndChildren(child.getLocationURI());
        }
      }
    }
  }

  /**
   * If the project is open or just prior to the project being closed, this method removes the
   * preference change listener and calls {@link #clearLibraryInfo()} to clear any cached
   * {@link DartLibraryInfo} instances.
   */
  @Override
  public void close() throws DartModelException {
    if (DartProjectNature.hasDartNature(project)) {
      // Get cached preferences if exist
      PerProjectInfo perProjectInfo = DartModelManager.getInstance().getPerProjectInfo(
          project,
          false);
      if (perProjectInfo != null && perProjectInfo.getPreferences() != null) {
        try {
          perProjectInfo.getPreferences().flush();
        } catch (BackingStoreException exception) {
          DartCore.logError(exception);
        }

        IEclipsePreferences eclipseParentPreferences = (IEclipsePreferences) perProjectInfo.getPreferences().parent();
        if (preferencesNodeListener != null) {
          eclipseParentPreferences.removeNodeChangeListener(preferencesNodeListener);
          preferencesNodeListener = null;
        }
        if (preferencesChangeListener != null) {
          perProjectInfo.getPreferences().removePreferenceChangeListener(preferencesChangeListener);
          preferencesChangeListener = null;
        }
      }
      clearLibraryInfo();
    }
    super.close();
  }

  /**
   * Return <code>true</code> if the given resource is accessible through the children or the
   * non-Dart resources of this project. Assumes that the resource is a folder or a file.
   * 
   * @param resource the resource being tested for
   * @return <code>true</code> if the resource is contained in this project
   */
  public boolean contains(IResource resource) {
    // IClasspathEntry[] classpath;
    // IPath output;
    // try {
    // classpath = getResolvedClasspath();
    // output = getOutputLocation();
    // } catch (JavaModelException e) {
    // return false;
    // }
    //
    // IPath fullPath = resource.getFullPath();
    // IPath innerMostOutput = output.isPrefixOf(fullPath) ? output : null;
    // IClasspathEntry innerMostEntry = null;
    // ExternalFoldersManager foldersManager =
    // JavaModelManager.getExternalManager();
    // for (int j = 0, cpLength = classpath.length; j < cpLength; j++) {
    // IClasspathEntry entry = classpath[j];
    //
    // IPath entryPath = entry.getPath();
    // if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
    // IResource linkedFolder = foldersManager.getFolder(entryPath);
    // if (linkedFolder != null)
    // entryPath = linkedFolder.getFullPath();
    // }
    // if ((innerMostEntry == null || innerMostEntry.getPath().isPrefixOf(
    // entryPath))
    // && entryPath.isPrefixOf(fullPath)) {
    // innerMostEntry = entry;
    // }
    // IPath entryOutput = classpath[j].getOutputLocation();
    // if (entryOutput != null && entryOutput.isPrefixOf(fullPath)) {
    // innerMostOutput = entryOutput;
    // }
    // }
    // if (innerMostEntry != null) {
    // // special case prj==src and nested output location
    // if (innerMostOutput != null && innerMostOutput.segmentCount() > 1 //
    // output
    // // isn't
    // // project
    // && innerMostEntry.getPath().segmentCount() == 1) { // 1 segment must
    // // be project name
    // return false;
    // }
    // if (resource instanceof IFolder) {
    // // folders are always included in src/lib entries
    // return true;
    // }
    // switch (innerMostEntry.getEntryKind()) {
    // case IClasspathEntry.CPE_SOURCE:
    // // .class files are not visible in source folders
    // return
    // !org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(fullPath.lastSegment());
    // case IClasspathEntry.CPE_LIBRARY:
    // // .java files are not visible in library folders
    // return
    // !DartCore.isJavaLikeFileName(fullPath.lastSegment());
    // }
    // }
    // if (innerMostOutput != null) {
    // return false;
    // }
    // return true;
    DartCore.notYetImplemented();
    // TODO This does not take linked resources into account.
    return resource.getProject().equals(project);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DartProjectImpl)) {
      return false;
    }
    DartProjectImpl other = (DartProjectImpl) o;
    return project.equals(other.getProject());
  }

  @Override
  @Deprecated
  public Type findType(String qualifiedTypeName) throws DartModelException {
    // TODO(devoncarew): remove this method - Dart projects don't have a notion of a qualified type name
    return null;
  }

  @Override
  @Deprecated
  public Type findType(String qualifiedTypeName, WorkingCopyOwner owner) throws DartModelException {
    // TODO(devoncarew): remove this method - Dart projects don't have a notion of a qualified type name
    return null;
  }

  @Override
  public Type[] findTypes(String typeName) throws DartModelException {
    ArrayList<Type> types = new ArrayList<Type>();
    for (DartLibrary library : getChildrenOfType(DartLibrary.class)) {
      Type type = library.findType(typeName);
      if (type != null) {
        types.add(type);
      }
    }
    return types.toArray(new Type[types.size()]);
  }

  @Override
  public IPath getArtifactLocation() {
    return getDefaultOutputFullPath();
  }

  @Override
  public DartLibrary[] getDartLibraries() throws DartModelException {
    List<DartLibrary> libraryList = getChildrenOfType(DartLibrary.class);
    return libraryList.toArray(new DartLibrary[libraryList.size()]);
  }

  @Override
  public DartLibrary getDartLibrary(IResource resource) throws DartModelException {
    for (DartLibrary library : getDartLibraries()) {
      IResource libraryResource = library.getCorrespondingResource();
      if (libraryResource != null && libraryResource.equals(resource)) {
        return library;
      }
    }
    return null;
  }

  @Override
  public DartLibrary getDartLibrary(String resourcePath) throws DartModelException {
    for (DartLibrary library : getDartLibraries()) {
      IResource libraryResource = library.getCorrespondingResource();
      if (libraryResource != null && libraryResource.getFullPath().equals(resourcePath)) {
        return library;
      }
    }
    return null;
  }

  @Override
  public DartProject getDartProject() {
    return this;
  }

  @Override
  public IPath getDefaultOutputFullPath() {
    return this.project.getFullPath().append("out"); //$NON-NLS-1$
  }

  /**
   * Return the project custom preference pool. Project preferences may include custom encoding.
   * 
   * @return IEclipsePreferences or <code>null</code> if the project does not have a java nature.
   */
  public IEclipsePreferences getEclipsePreferences() {
    if (!DartProjectNature.hasDartNature(project)) {
      return null;
    }
    // Get cached preferences if exist
    PerProjectInfo perProjectInfo = DartModelManager.getInstance().getPerProjectInfo(project, true);
    if (perProjectInfo.getPreferences() != null) {
      return perProjectInfo.getPreferences();
    }

    // Init project preferences
    ProjectScope scope = new ProjectScope(project);
    final IEclipsePreferences eclipsePreferences = scope.getNode(DartCore.PLUGIN_ID);

    //IScopeContext context = new ProjectScope(getProject());
    //final IEclipsePreferences eclipsePreferences = context.getNode(DartCore.PLUGIN_ID);
    perProjectInfo.setPreferences(eclipsePreferences);

    // Listen to new preferences node
    final IEclipsePreferences eclipseParentPreferences = (IEclipsePreferences) eclipsePreferences.parent();
    if (eclipseParentPreferences != null) {
      if (preferencesNodeListener != null) {
        eclipseParentPreferences.removeNodeChangeListener(preferencesNodeListener);
      }
      preferencesNodeListener = new IEclipsePreferences.INodeChangeListener() {
        @Override
        public void added(IEclipsePreferences.NodeChangeEvent event) {
          // do nothing
        }

        @Override
        public void removed(IEclipsePreferences.NodeChangeEvent event) {
          if (event.getChild() == eclipsePreferences) {
            DartModelManager.getInstance().resetProjectPreferences(DartProjectImpl.this);
          }
        }
      };
      eclipseParentPreferences.addNodeChangeListener(preferencesNodeListener);
    }

    // Listen to preferences changes
    if (preferencesChangeListener != null) {
      eclipsePreferences.removePreferenceChangeListener(preferencesChangeListener);
    }
    preferencesChangeListener = new IEclipsePreferences.IPreferenceChangeListener() {
      @Override
      public void preferenceChange(IEclipsePreferences.PreferenceChangeEvent event) {
        String propertyName = event.getKey();
        DartModelManager manager = DartModelManager.getInstance();
        if (propertyName.startsWith(DartCore.PLUGIN_ID)) {
          // if
          // (propertyName.equals(DartCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER)
          // ||
          // propertyName.equals(DartCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER)
          // || propertyName.equals(DartCore.CORE_JAVA_BUILD_DUPLICATE_RESOURCE)
          // ||
          // propertyName.equals(DartCore.CORE_JAVA_BUILD_RECREATE_MODIFIED_CLASS_FILES_IN_OUTPUT_FOLDER)
          // || propertyName.equals(DartCore.CORE_JAVA_BUILD_INVALID_CLASSPATH)
          // ||
          // propertyName.equals(DartCore.CORE_ENABLE_CLASSPATH_EXCLUSION_PATTERNS)
          // ||
          // propertyName.equals(DartCore.CORE_ENABLE_CLASSPATH_MULTIPLE_OUTPUT_LOCATIONS)
          // || propertyName.equals(DartCore.CORE_INCOMPLETE_CLASSPATH)
          // || propertyName.equals(DartCore.CORE_CIRCULAR_CLASSPATH)
          // || propertyName.equals(DartCore.CORE_INCOMPATIBLE_JDK_LEVEL)) {
          // manager.deltaState.addClasspathValidation(DartProjectImpl.this);
          // }
          manager.resetProjectOptions(DartProjectImpl.this);
          DartProjectImpl.this.resetCaches();
          // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=233568
        }
      }
    };
    eclipsePreferences.addPreferenceChangeListener(preferencesChangeListener);
    return eclipsePreferences;
  }

  @Override
  public String getElementName() {
    return project.getName();
  }

  @Override
  public int getElementType() {
    return DartElement.DART_PROJECT;
  }

  /**
   * Return the mapping for the html files contained in this project. If the mapping has not been
   * created, it will do so and return the result.
   * 
   * @return the table with the html file to library mapping html file location string, list of
   *         library resource location string
   * @throws CoreException
   */
  @Override
  public HashMap<String, List<String>> getHtmlMapping() throws CoreException {

    HashMap<String, List<String>> htmlMapping = ((DartProjectInfo) getElementInfo()).getHtmlMapping();

    if (htmlMapping != null) {
      return htmlMapping;
    }

    final HashMap<String, List<String>> mapping = new HashMap<String, List<String>>();

    getProject().accept(new IResourceProxyVisitor() {
      @Override
      public boolean visit(IResourceProxy proxy) throws CoreException {
        if (proxy.getType() != IResource.FILE || !DartCore.isHTMLLikeFileName(proxy.getName())) {
          return true;
        }
        IResource resource = proxy.requestResource();
        if (resource.isAccessible()) {
          try {
            List<String> libraryNames = LibraryReferenceFinder.findInHTML(IFileUtilities.getContents((IFile) resource));
            if (!libraryNames.isEmpty()) {
              List<String> libraryPaths = IResourceUtilities.getResolvedFilePaths(
                  resource,
                  libraryNames);
              mapping.put(resource.getLocation().toPortableString(), libraryPaths);
            }
          } catch (IOException exception) {
            DartCore.logInformation(
                "Could not get contents of " + resource.getLocation(),
                exception);
          }
        }

        return true;
      }
    },
        0);

    ((DartProjectInfo) getElementInfo()).setHtmlMapping(mapping);

    return mapping;
  }

  @Override
  public IResource[] getNonDartResources() throws DartModelException {
    return ((DartProjectInfo) getElementInfo()).getNonDartResources(this);
  }

  @Override
  public String getOption(String optionName, boolean inheritDartCoreOptions) {
    if (DartModelManager.getInstance().getOptionNames().contains(optionName)) {
      IEclipsePreferences projectPreferences = getEclipsePreferences();
      String coreDefault = inheritDartCoreOptions ? DartCore.getOption(optionName) : null;
      if (projectPreferences == null) {
        return coreDefault;
      }
      String value = projectPreferences.get(optionName, coreDefault);
      return value == null ? null : value.trim();
    }
    return null;
  }

  @Override
  public Hashtable<String, String> getOptions(boolean inheritDartCoreOptions) {
    DartCore.notYetImplemented();
    return new Hashtable<String, String>();
  }

  @Override
  public IPath getOutputLocation() throws DartModelException {
    if (!DartProjectNature.hasDartNature(project)) {
      return getDefaultOutputFullPath();
    } else {
      PerProjectInfo perProjectInfo = getPerProjectInfo();

      IPath outputLocation = perProjectInfo.getOutputLocation();

      if (outputLocation != null) {
        return outputLocation;
      } else {
        return getDefaultOutputFullPath();
      }
    }
  }

  @Override
  public IPath getPath() {
    return project.getFullPath();
  }

  public PerProjectInfo getPerProjectInfo() throws DartModelException {
    PerProjectInfo projectInfo = DartModelManager.getInstance().getPerProjectInfoCheckExistence(
        project);

    if (projectInfo.getPreferences() == null) {
      // TODO (danrubel): investigate better approach
      // getEclipsePreferences() will initialize the Eclipse preferences for this project.
      // It's a getter with side effects - not ideal.
      getEclipsePreferences();
    }

    return projectInfo;
  }

  @Override
  public IProject getProject() {
    return project;
  }

  @Override
  public IResource getUnderlyingResource() throws DartModelException {
    if (!exists()) {
      throw newNotPresentException();
    }
    return project;
  }

  @Override
  public boolean hasBuildState() {
    DartCore.notYetImplemented();
    return false;
    // return DartModelManager.getInstance().getLastBuiltState(project, null) !=
    // null;
  }

  @Override
  public int hashCode() {
    return project.hashCode();
  }

  /**
   * Recompute the set of libraries that are part of this Dart project.
   */
  public void recomputeLibrarySet() {
    try {
      DartProjectInfo projectInfo = (DartProjectInfo) getElementInfo();
      if (project == null || !project.exists() || !project.isOpen()) {
        projectInfo.setChildren(DartElement.EMPTY_ARRAY);
        return;
      }
      List<String> childPaths = getChildPaths(projectInfo, true);
      setChildPaths(projectInfo, childPaths);

      if (childPaths.size() > 0) {
        IProject project = getProject();
        ArrayList<DartElementImpl> children = new ArrayList<DartElementImpl>();
        for (String path : childPaths) {
          IFile resource = project.getFile(new Path(path));
          if (resource != null) {
            children.add(new DartLibraryImpl(DartProjectImpl.this, resource));
          }
        }
        projectInfo.setChildren(children.toArray(new DartElementImpl[children.size()]));
        return;
      }
    } catch (DartModelException exception) {
      DartCore.logError("Unable to recompute the set of top-level libraries in the project:" //$NON-NLS-1$
          + getElementName(), exception);
    }
  }

  @Override
  public boolean removeLibraryFile(IFile file) {
    boolean foundAndRemoved = false;
    try {
      // Get the element info
      DartProjectInfo projectInfo = (DartProjectInfo) getElementInfo();

      // Read the set of child paths from the .children file for the project
      List<String> childPaths = getChildPaths(projectInfo, false);

      // Remove the new file path from the list
      foundAndRemoved = childPaths.remove(file.getProjectRelativePath().toPortableString());

      // Only update the .children file if the specified was in the .children file to be removed,
      // or if the children file doesn't exist yet.
      File childrenFile = getChildrenFile();
      if (foundAndRemoved || childrenFile == null || !childrenFile.exists()) {
        // Write out the new contents to the .children file
        setChildPaths(projectInfo, childPaths);

        // Update the project info object to include the new file as a DartLibraryImpl
        IProject project = file.getProject();
        ArrayList<DartElementImpl> children = new ArrayList<DartElementImpl>();
        for (String path : childPaths) {
          IFile resource = project.getFile(new Path(path));
          if (resource != null) {
            children.add(new DartLibraryImpl(DartProjectImpl.this, resource));
          }
        }
        projectInfo.setChildren(children.toArray(new DartElementImpl[children.size()]));
      }
    } catch (DartModelException exception) {
      DartCore.logError("Failed to remove the new libray file " + file.getName() //$NON-NLS-1$
          + " to the project " + getElementName()); //$NON-NLS-1$
    }
    return foundAndRemoved;
  }

  public void resetCaches() {
    clearLibraryInfo();
    DartProjectInfo info = (DartProjectInfo) DartModelManager.getInstance().peekAtInfo(this);
    if (info != null) {
      info.resetCaches();
    }
  }

  @Override
  public IResource resource() {
    return project;
  }

  @Override
  public void setOption(String optionName, String optionValue) {
    DartCore.notYetImplemented();
    // if (!DartModelManager.getInstance().optionNames.contains(optionName))
    // return; // unrecognized option
    IEclipsePreferences projectPreferences = getEclipsePreferences();
    if (optionValue == null) {
      // remove preference
      projectPreferences.remove(optionName);
    } else {
      projectPreferences.put(optionName, optionValue);
    }

    // Dump changes
    try {
      projectPreferences.flush();
    } catch (BackingStoreException e) {
      // problem with pref store - quietly ignore
    }
  }

  @Override
  public void setOptions(Map<String, String> newOptions) {
    IEclipsePreferences projectPreferences = getEclipsePreferences();
    if (projectPreferences == null) {
      return;
    }
    try {
      if (newOptions == null) {
        projectPreferences.clear();
      } else {
        for (Map.Entry<String, String> entry : newOptions.entrySet()) {
          String key = entry.getKey();
          DartCore.notYetImplemented();
          // if (!DartModelManager.getInstance().optionNames.contains(key))
          // continue; // unrecognized option
          // no filtering for encoding (custom encoding for project is allowed)
          projectPreferences.put(key, entry.getValue());
        }

        // reset to default all options not in new map
        // @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=26255
        // @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=49691
        String[] pNames = projectPreferences.keys();
        int ln = pNames.length;
        for (int i = 0; i < ln; i++) {
          String key = pNames[i];
          if (!newOptions.containsKey(key)) {
            // old preferences => remove from preferences table
            projectPreferences.remove(key);
          }
        }
      }

      // persist options
      projectPreferences.flush();

      // flush cache immediately
      try {
        getPerProjectInfo().setOptions(null);
      } catch (DartModelException e) {
        // do nothing
      }
    } catch (BackingStoreException e) {
      // problem with pref store - quietly ignore
    }
  }

  @Override
  public void setOutputLocation(IPath path, IProgressMonitor monitor) throws DartModelException {
    // Null implies reset to the default output location.
    if (path == null) {
      path = getDefaultOutputFullPath();
    }

    // Don't change the setting if the values are the same.
    if (path.equals(getOutputLocation())) {
      return;
    }

    PerProjectInfo perProjectInfo = getPerProjectInfo();

    perProjectInfo.setOutputLocation(path);
  }

  /**
   * Update the html mapping table
   * 
   * @param htmlFileName the name of the html file
   * @param libraries the list of libraries referenced in the html file
   * @throws DartModelException
   */
  @Override
  public void updateHtmlMapping(String htmlFileName, List<String> libraries, boolean add)
      throws DartModelException {
    ((DartProjectInfo) getElementInfo()).updateHtmlMapping(htmlFileName, libraries, add);
  }

  @Override
  protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm,
      Map<DartElement, DartElementInfo> newElements, IResource underlyingResource)
      throws DartModelException {
    DartProjectInfo projectInfo = (DartProjectInfo) info;
    if (project == null || !project.exists() || !project.isOpen()) {
      projectInfo.setChildren(DartElement.EMPTY_ARRAY);
      return true;
    }
    List<String> childPaths = getChildPaths(projectInfo, false);
    projectInfo.setChildPaths(childPaths);
    if (childPaths.size() > 0) {
      IProject project = getProject();
      ArrayList<DartElementImpl> children = new ArrayList<DartElementImpl>();
      for (String path : childPaths) {
        IFile resource = project.getFile(new Path(path));
        if (resource != null) {
          children.add(new DartLibraryImpl(DartProjectImpl.this, resource));
        }
      }
      projectInfo.setChildren(children.toArray(new DartElementImpl[children.size()]));
      return true;
    }
    // TODO(brianwilkerson) Remove the code below. It only exists for backward compatibility.
//    try {
//      final ArrayList<DartElementImpl> children = new ArrayList<DartElementImpl>();
//      project.accept(new IResourceProxyVisitor() {
//        @Override
//        public boolean visit(IResourceProxy proxy) throws CoreException {
//          if (proxy.getType() == IResource.FILE) {
//            String fileName = proxy.getName();
//            if (DartCore.isDartLibraryFile(fileName)) {
//              IFile resource = (IFile) proxy.requestResource();
//              children.add(new DartLibraryImpl(DartProjectImpl.this, resource));
//            } else if (DartCore.isHTMLLikeFileName(fileName)) {
//              IFile resource = (IFile) proxy.requestResource();
//              children.add(new HTMLFileImpl(DartProjectImpl.this, resource));
//            }
//          }
//          return true;
//        }
//      }, 0);
//      projectInfo.setChildren(children.toArray(new DartElementImpl[children.size()]));
//    } catch (CoreException exception) {
//      projectInfo.setChildren(DartElement.EMPTY_ARRAY);
//    }
//    return true;
    return false;
  }

  @Override
  protected DartElementInfo createElementInfo() {
    return new DartProjectInfo();
  }

  /**
   * Return a list containing the project-relative paths to all children in the project.
   * 
   * @param recomputeLibrarySet if <code>true</code>, recompute the set of libraries, even if a set
   *          is cached in the .children file
   * @return the project-relative paths of the project's children
   */
  protected List<String> getChildPaths(DartProjectInfo info, boolean recomputeLibrarySet) {
    List<String> childPaths = info.getChildPaths();
    if (childPaths != null && !recomputeLibrarySet) {
      return childPaths;
    }
    childPaths = new ArrayList<String>();
    File file = getChildrenFile();
    if (recomputeLibrarySet || file == null || !file.exists()) {
      computeChildPaths(childPaths);
      return childPaths;
    }
    FileReader fileReader = null;
    try {
      fileReader = new FileReader(file);
      BufferedReader reader = new BufferedReader(fileReader);
      String line = reader.readLine();
      while (line != null) {
        if (line.length() > 0) {
          childPaths.add(line);
        }
        line = reader.readLine();
      }
    } catch (Exception exception) {
      DartCore.logError("Could not read children file", exception);
    } finally {
      if (fileReader != null) {
        try {
          fileReader.close();
        } catch (IOException exception) {
          // Ignored
        }
      }
    }
    info.setChildPaths(childPaths);
    return childPaths;
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    if (!tokenizer.hasMoreTokens()) {
      return this;
    }

    if (token.charAt(0) != MEMENTO_DELIMITER_LIBRARY) {
      return null;
    }
    String libraryUri = tokenizer.nextToken();

    URI uri;
    try {
      uri = new URI(libraryUri);
    } catch (URISyntaxException exception) {
      DartCore.logError(exception);
      return null;
    }
    IFile file = findFileForUri(uri);

    PackageLibraryManager libMgr = PackageLibraryManagerProvider.getSystemLibraryManager();
    LibrarySource librarySource = new UrlLibrarySource(uri, libMgr);
    DartLibraryImpl library;
    if (file != null) {
      // If found, then build a library reference with the Eclipse file
      DartProjectImpl dartProject = !file.getProject().equals(project)
          ? DartModelManager.getInstance().create(file.getProject()) : this;
      library = new DartLibraryImpl(dartProject, file, librarySource);
    } else {
      // Otherwise build an external library reference
      library = new DartLibraryImpl(librarySource);
    }
    return library.getHandleFromMemento(tokenizer, owner);
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return MEMENTO_DELIMITER_PROJECT;
  }

  /**
   * Set the list of projects that are open to those with the given project-relative paths.
   * 
   * @param paths the project-relative paths of the open libraries
   */
  protected void setChildPaths(DartProjectInfo info, List<String> paths) {
    info.setChildPaths(paths);
    File file = getChildrenFile();
    if (file == null) {
      return;
    }
    FileWriter fileWriter = null;
    try {
      File parentDirectory = file.getParentFile();
      if (!parentDirectory.exists()) {
        parentDirectory.mkdir();
      }
      fileWriter = new FileWriter(file);
      PrintWriter writer = new PrintWriter(new BufferedWriter(fileWriter));
      for (String path : paths) {
        writer.println(path);
      }
      writer.flush();
    } catch (Exception exception) {
      DartCore.logError("Could not write children file", exception);
    } finally {
      if (fileWriter != null) {
        try {
          fileWriter.close();
        } catch (IOException exception) {
          // Ignore
        }
      }
    }
  }

  @Override
  protected IStatus validateExistence(IResource underlyingResource) {
    // check whether the Dart project can be opened
    try {
      if (!((IProject) underlyingResource).hasNature(DartCore.DART_PROJECT_NATURE)) {
        return newDoesNotExistStatus();
      }
    } catch (CoreException e) {
      return newDoesNotExistStatus();
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  /**
   * Assuming that this is the first time the project has been opened, compute the set of child
   * paths that should be represented as libraries.
   * 
   * @param childPaths the array to which child paths should be added
   */
  private void computeChildPaths(List<String> childPaths) {
    final ArrayList<IFile> dartFiles = new ArrayList<IFile>();
    try {
      project.accept(new IResourceProxyVisitor() {
        @Override
        public boolean visit(IResourceProxy proxy) throws CoreException {
          if (proxy.getType() == IResource.FILE && DartCore.isDartLikeFileName(proxy.getName())) {
            dartFiles.add((IFile) proxy.requestResource());
          } else if (proxy.getType() == IResource.FOLDER && proxy.getName().startsWith(".")) {
            return false;
          }
          return true;
        }
      }, 0);
    } catch (CoreException exception) {
      // This should never happen.
      DartCore.logError(
          "Could not traverse resource structure in project " + project.getLocation(),
          exception);
    }
    HashSet<IFile> libraryFiles = new HashSet<IFile>(dartFiles);
    for (IFile dartFile : dartFiles) {
      DartUnit dartUnit = parseDartFile(dartFile);
      if (dartUnit != null) {
        LibrarySource librarySource = new UrlLibrarySource(dartFile.getLocation().toFile());
        for (DartDirective directive : dartUnit.getDirectives()) {
          if (directive instanceof DartSourceDirective) {
            DartSourceDirective sourceDirective = (DartSourceDirective) directive;
            String relativePath = getRelativePath(sourceDirective.getSourceUri());
            if (relativePath != null && relativePath.length() > 0) {
              DartSource source = librarySource.getSourceFor(relativePath);
              if (source != null) {
                IResource[] compilationUnitFiles = ResourceUtil.getResources(source);
                if (compilationUnitFiles != null && compilationUnitFiles.length == 1) {
                  libraryFiles.remove(compilationUnitFiles[0]);
                }
              }
            }
          }
        }
      }
    }
    for (IFile dartFile : libraryFiles) {
      childPaths.add(dartFile.getProjectRelativePath().toString());
    }
  }

  /**
   * Find the file within this project that is associated with the given URI.
   * 
   * @param uri the URI used to identify the file to be returned
   * @return the file that was found, or <code>null</code> if no file could be found
   */
  private IFile findFileForUri(URI uri) {
    IResource[] resources = ResourceUtil.getResources(uri);
    if (resources != null) {
      for (IResource resource : resources) {
        if (resource instanceof IFile && resource.getProject().equals(project)) {
          return (IFile) resource;
        }
      }
    }
    return null;
  }

  /**
   * Return the file that contains the project-relative paths of the children of the project, or
   * <code>null</code> if the location of the file cannot be determined, such as when the project no
   * longer exists. Even if non-<code>null</code>, the returned file might not exist.
   * 
   * @return the file that contains the paths to the project's children
   */
  private File getChildrenFile() {
    return new File(DartCore.getPlugin().getStateLocation().append(getProject().getName()).append(
        CHILDREN_FILE_NAME).toOSString());
//    IProject project = getProject();
//    if (project == null) {
//      return null;
//    }
//    IPath location = project.getLocation();
//    if (location == null) {
//      return null;
//    }
//    return new File(location.append(CHILDREN_FILE_NAME).toOSString());
  }

  private String getRelativePath(DartStringLiteral literal) {
    if (literal == null) {
      return null;
    }
    String relativePath = literal.getValue();
    if (relativePath == null || relativePath.length() == 0) {
      return null;
    }
    return relativePath;
  }

  /**
   * Return the result of parsing the file that defines this library, or <code>null</code> if the
   * contents of the file cannot be accessed for some reason.
   * 
   * @return the result of parsing the file that defines this library
   */
  private DartUnit parseDartFile(IFile dartFile) {
    String fileName = null;
    try {
      fileName = dartFile.getName();
      return DartCompilerUtilities.parseSource(
          fileName,
          FileUtilities.getDartContents(dartFile.getLocation().toFile()),
          null);
    } catch (Exception exception) {
      DartCore.logInformation("Could not read and parse the file " + fileName, exception);
      // Fall through to return null.
    }
    return null;
  }

  // /**
  // * Return the model element within the given list of elements that
  // corresponds
  // * to the given resource, or <code>null</code> if there is no such element.
  // *
  // * @param elements the elements to be searched
  // * @param resource the resource corresponding to the element to be returned
  // *
  // * @return the element that corresponds to the given resource
  // */
  // private DartElement findElement(DartElement[] elements, IResource resource)
  // {
  // for (DartElement element : elements) {
  // try {
  // if (resource.equals(element.getCorrespondingResource())) {
  // return element;
  // }
  // } catch (DartModelException exception) {
  // // Ignored
  // }
  // }
  // return null;
  // }
}
