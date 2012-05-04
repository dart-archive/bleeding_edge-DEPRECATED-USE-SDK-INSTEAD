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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartResourceDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.dom.visitor.SafeDartNodeTraverser;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaImpl;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartLibraryInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.internal.util.LibraryReferenceFinder;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartResource;
import com.google.dart.tools.core.model.ElementChangedEvent;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.utilities.general.SourceUtilities;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.core.utilities.net.URIUtilities;
import com.google.dart.tools.core.utilities.resource.IFileUtilities;
import com.google.dart.tools.core.utilities.resource.IProjectUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Instances of the class <code>DartLibraryImpl</code> implement an object that represents a Dart
 * library.
 */
public class DartLibraryImpl extends OpenableElementImpl implements DartLibrary,
    CompilationUnitContainer {
  public static final DartLibraryImpl[] EMPTY_LIBRARY_ARRAY = new DartLibraryImpl[0];

  /**
   * Answer a library source for the specified file
   * 
   * @param libraryFile the *.dart library configuration file
   * @return the source file or <code>null</code> if it could not be determined
   */
  private static LibrarySource newLibrarySourceFile(File libraryFile) throws AssertionError {
    if (libraryFile == null) {
      return null;
    }

    URI uri = libraryFile.toURI().normalize();

    SystemLibraryManager libMgr = SystemLibraryManagerProvider.getSystemLibraryManager();

    return new UrlLibrarySource(uri, libMgr);
  }

  /**
   * Answer a library source for the specified file
   * 
   * @param libraryFile the *.dart library configuration file
   * @return the source file or <code>null</code> if it could not be determined
   */
  private static LibrarySource newLibrarySourceFile(IFile libraryFile) {
    if (libraryFile == null) {
      return null;
    }
    IPath location = libraryFile.getLocation();
    if (location == null) {
      return null;
    }
    return newLibrarySourceFile(location.toFile());
  }

  /**
   * The file containing the definition of this library.
   */
  private IFile libraryFile;

  /**
   * The structured representation of the library file that defines this library.
   */
  private LibrarySource sourceFile;

  /**
   * An empty array of libraries.
   */
  public static final DartLibraryImpl[] EMPTY_ARRAY = new DartLibraryImpl[0];

  /**
   * The qualified name used to access the persistent property indicating whether the file
   * associated with the defining compilation unit defines a top-level library.
   */
  private static final QualifiedName TOP_LEVEL_PROPERTY_NAME = new QualifiedName(
      DartCore.PLUGIN_ID,
      "topLevel");

  /**
   * Initialize a newly created library to be contained in the given project.
   * 
   * @param project the project containing this library
   * @param libraryFile the *.lib library configuration file
   */
  public DartLibraryImpl(DartProjectImpl project, IFile libraryFile) {
    this(project, libraryFile, newLibrarySourceFile(libraryFile));
  }

  /**
   * Initialize a newly created library to be contained in the given project.
   * 
   * @param project the project containing this library (not <code>null</code>)
   * @param libraryFile the file containing the children of this library or <code>null</code> if
   *          this is not part of the workspace
   * @param sourceFile the library source file
   */
  public DartLibraryImpl(DartProjectImpl project, IFile libraryFile, LibrarySource sourceFile) {
    super(project);
    this.libraryFile = libraryFile;
    this.sourceFile = sourceFile;
  }

  /**
   * Initialize a new created library that is not mapped into the workspace
   * 
   * @param libraryFile the library specification file (*.lib file)
   */
  public DartLibraryImpl(File libraryFile) {
    this(
        DartModelManager.getInstance().getDartModel().getExternalProject(),
        null,
        newLibrarySourceFile(libraryFile));
  }

  /**
   * Initialize a new created library that is not mapped into the workspace
   * 
   * @param libraryFile the library specification file (*.lib file)
   */
  public DartLibraryImpl(LibrarySource source) {
    this(DartModelManager.getInstance().getDartModel().getExternalProject(), null, source);
  }

  @Override
  public DartResource addResource(File file, IProgressMonitor monitor) throws DartModelException {
    //
    // Create a link to the file.
    //
    IFile resourceFile;
    try {
      resourceFile = IProjectUtilities.addLinkToProject(
          getDartProject().getProject(),
          file,
          monitor);
    } catch (CoreException exception) {
      throw new DartModelException(exception);
    }
    DartResourceImpl resource = new DartResourceImpl(this, resourceFile);
    //
    // Add the text of the directive to this library's defining file.
    //
    if (addDirective(SourceUtilities.RESOURCE_DIRECTIVE, file, monitor)) {
      //
      // Add the resource to the list of resources associated with the library.
      //
      DartLibraryInfo info = (DartLibraryInfo) getElementInfo();
      info.addChild(resource);
    }
    return resource;
  }

  @Override
  public CompilationUnit addSource(File file, IProgressMonitor monitor) throws DartModelException {
    //
    // Create a link to the file.
    //
    IFile unitFile;
    try {
      unitFile = IProjectUtilities.addLinkToProject(getDartProject().getProject(), file, monitor);
    } catch (CoreException exception) {
      throw new DartModelException(exception);
    }
    CompilationUnitImpl unit = new CompilationUnitImpl(
        this,
        unitFile,
        DefaultWorkingCopyOwner.getInstance());
    //
    // Add the text of the directive to this library's defining file.
    //
    if (addDirective(SourceUtilities.SOURCE_DIRECTIVE, file, monitor)) {
      //
      // Add the newly created compilation unit to the list of children.
      //
      DartLibraryInfo info = (DartLibraryInfo) getElementInfo();
      info.addChild(unit);
    }
    return unit;
  }

  @Override
  public void delete(IProgressMonitor monitor) throws DartModelException {
    if (DartCoreDebug.ANALYSIS_SERVER) {
      if (libraryFile != null) {
        IPath location = libraryFile.getLocation();
        if (location != null) {
          SystemLibraryManagerProvider.getDefaultAnalysisServer().discard(location.toFile());
        }
      }
    }
    DartProject project = getDartProject();
    project.close();
    try {
      project.getProject().delete(true, true, monitor);
    } catch (CoreException exception) {
      throw new DartModelException(exception);
    }
  }

  /**
   * Override the superclass implementation to make two libraries equal based upon their
   * {@link #getElementName()} regardless of their parent
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof DartLibraryImpl
        && getElementName().equals(((DartLibraryImpl) o).getElementName());
  }

  @Override
  public Type findType(String typeName) throws DartModelException {
    for (CompilationUnit unit : getCompilationUnits()) {
      for (Type type : unit.getTypes()) {
        if (type.getElementName().equals(typeName)) {
          return type;
        }
      }
    }
    return null;
  }

  @Override
  public Type findTypeInScope(String typeName) throws DartModelException {
    // try to find in this library
    Type type = findType(typeName);
    if (type != null) {
      return type;
    }
    // try to find in imported libraries, non-transitively
    if (!typeName.startsWith("_")) {
      for (DartLibrary library : getImportedLibraries()) {
        type = library.findType(typeName);
        if (type != null) {
          return type;
        }
      }
    }
    // not found
    return null;
  }

  @Override
  public CompilationUnit getCompilationUnit(String name) {
    if (!DartCore.isDartLikeFileName(name)) {
      // TODO move the following exception name into some messages.properties file
      throw new IllegalArgumentException(
          "Compilation unit name must end with .dart, or one of the registered Dart-like extensions");
    }
    //
    // Search through existing compilation units.
    //
    try {
      for (CompilationUnit unit : getCompilationUnits()) {
        if (unit.getElementName().equals(name)) {
          return unit;
        }
      }
    } catch (DartModelException exception) {
      // Ignored
    }
    //
    // If not found, but there is a resource behind the library, use the resource to create the file
    // that would represent the compilation unit.
    //
    if (libraryFile != null) {
      IFile file = libraryFile.getParent().getFile(new Path(name));
      return new CompilationUnitImpl(this, file, DefaultWorkingCopyOwner.getInstance());
    }
    //
    // Otherwise fail. We cannot currently create a placeholder for external compilation units that
    // do not actually exist.
    //
    return null;
  }

  @Override
  public CompilationUnit getCompilationUnit(URI uri) {
    // TODO(jwren) revisit this assertion, this was thrown from it: "Illegal dart file name: file:/Users/user/dart/HelloWorld/B.dart"
//    Assert.isTrue(
//        SystemLibraryManager.isDartUri(uri),
//        "Compilation unit name must end with .dart, or one of the registered Dart-like extensions. Illegal dart file name: "
//            + uri.toString());
    return new CompilationUnitImpl(this, uri, DefaultWorkingCopyOwner.getInstance());
  }

  @Override
  public CompilationUnit[] getCompilationUnits() throws DartModelException {
    List<CompilationUnit> compilationUnits = getChildrenOfType(CompilationUnit.class);
    return compilationUnits.toArray(new CompilationUnit[compilationUnits.size()]);
  }

  @Override
  public List<CompilationUnit> getCompilationUnitsInScope() throws DartModelException {
    List<CompilationUnit> units = Lists.newArrayList();
    // add units from imported libraries
    for (DartLibrary library : getImportedLibraries()) {
      Collections.addAll(units, library.getCompilationUnits());
    }
    // add units of this library
    Collections.addAll(units, getCompilationUnits());
    return units;
  }

  @Override
  public IResource getCorrespondingResource() {
    return libraryFile;
  }

  @Override
  public CompilationUnit getDefiningCompilationUnit() throws DartModelException {
    DartLibraryInfo info = (DartLibraryInfo) getElementInfo();
    return info.getDefiningCompilationUnit();
  }

  @Override
  public String getDisplayName() {
    // If this is a bundled library, then show "dart:<libname>" to the user
    if (sourceFile != null) {
      EditorLibraryManager libMgr = SystemLibraryManagerProvider.getSystemLibraryManager();
      URI uri = libMgr.getShortUri(sourceFile.getUri());
      if (uri != null) {
        return uri.toString();
      }
    }
    try {
      DartLibraryInfo info = (DartLibraryInfo) getElementInfo();
      String name = info.getName();
      if (name != null) {
        return name;
      }
    } catch (DartModelException exception) {
      // If we cannot access the info we compute the name from the file name, just like we will if
      // the library directive does not contain a literal.
    }
    return getImplicitLibraryName();
  }

  /**
   * Answer a unique name for this library. This name must be unique across the entire model because
   * a project containing a library might be closed, but that library may still be referenced by
   * other applications and/or libraries in projects that are still open. In addition, a library may
   * be referenced but not part of the workspace and then later imported/mapped into a project in
   * the workspace.
   */
  @Override
  public String getElementName() {
    if (sourceFile != null) {
      EditorLibraryManager libMgr = SystemLibraryManagerProvider.getSystemLibraryManager();
      URI shortUri = libMgr.getShortUri(sourceFile.getUri());
      if (shortUri != null) {
        return shortUri.toString();
      }
      return sourceFile.getName();
    }
    return libraryFile.getName();
  }

  @Override
  public int getElementType() {
    return DartElement.LIBRARY;
  }

  /**
   * This method is called when there is no Library directive to specify the name of a library.
   * <p>
   * The implicit library name is the name of the defining {@link CompilationUnit} without the
   * appended <code>.dart</code> extension.
   */
  public String getImplicitLibraryName() {
    String name = new Path(getElementName()).lastSegment();
    if (name.endsWith(Extensions.DOT_DART)) {
      name = name.substring(0, name.length() - Extensions.DOT_DART.length());
    }
    return name;
  }

  @Override
  public DartLibrary[] getImportedLibraries() throws DartModelException {
    DartLibraryInfo elementInfo = (DartLibraryInfo) getElementInfo();
    if (elementInfo != null) {
      Set<DartLibrary> libraries = Sets.newHashSet();
      for (DartImport imprt : getImports()) {
        libraries.add(imprt.getLibrary());
      }
      return libraries.toArray(new DartLibrary[libraries.size()]);
    } else {
      return DartLibrary.EMPTY_LIBRARY_ARRAY;
    }
  }

  @Override
  public DartImport[] getImports() throws DartModelException {
    DartLibraryInfo info = (DartLibraryInfo) getElementInfo();
    if (info != null) {
      return info.getImports();
    } else {
      return DartImport.EMPTY_ARRAY;
    }
  }

  /**
   * Return the structured representation of the library file that defines this library.
   * 
   * @return the structured representation of the library file that defines this library
   */
  public LibrarySource getLibrarySourceFile() {
    // TODO (danrubel): rename this getLibrarySource()
    return sourceFile;
  }

  @Override
  public List<DartLibrary> getReferencingLibraries() throws DartModelException {
    List<DartLibrary> libraries = new ArrayList<DartLibrary>();
    for (DartProject project : DartModelManager.getInstance().getDartModel().getDartProjects()) {
      for (DartLibrary candidate : project.getDartLibraries()) {
        if (!equals(candidate)) {
          for (DartLibrary importedLibrary : candidate.getImportedLibraries()) {
            if (equals(importedLibrary)) {
              libraries.add(candidate);
              break;
            }
          }
        }
      }
    }
    return libraries;
  }

  @Override
  public DartResource getResource(URI uri) {
    return new DartResourceImpl(this, uri);
  }

  @Override
  public DartResource[] getResources() throws DartModelException {
    List<DartResource> resources = getChildrenOfType(DartResource.class);
    return resources.toArray(new DartResource[resources.size()]);
  }

  @Override
  public IResource getUnderlyingResource() throws DartModelException {
    return null;
  }

  /**
   * Override the superclass to generate a hash code based solely on the element name and ignoring
   * the parent
   */
  @Override
  public int hashCode() {
    return getElementName().hashCode();
  }

  /**
   * Answer <code>true</code> if this library has a <code>main()</code> method
   */
  public boolean hasMain() {
    DartElement[] children;
    try {
      children = getChildren();
    } catch (DartModelException e) {
      DartCore.logError("Could not determine whether " + getDisplayName()
          + " contains a main() method", e);
      return false;
    }
    for (DartElement child : children) {
      if (child instanceof CompilationUnitImpl) {
        CompilationUnitImpl unit = (CompilationUnitImpl) child;
        List<DartFunction> functions;
        try {
          functions = unit.getChildrenOfType(DartFunction.class);
        } catch (DartModelException e) {
          DartCore.logError("Could not determine whether " + unit.getElementName() + " in "
              + getDisplayName() + " contains a main() method", e);
          continue;
        }
        for (DartFunction funct : functions) {
          if (funct.isMain()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Answer <code>true</code> if this library is an application that can be run in the browser.
   */
  public boolean isBrowserApplication() {
    return hasMain() && (hasReferencingHtmlFile() || isOrImportsBrowserLibrary());
  }

  /**
   * Determine if the receiver is contained in an open project or maps to a library contained within
   * an open project.
   */
  @Override
  public boolean isLocal() {
    // Check the most common case
    IProject project = ((DartProjectImpl) getParent()).getProject();
    if (project.isOpen()) {
      return true;
    }

    // Check for external reference to file in open project
    IResource resource = ResourceUtil.getResource(sourceFile);
    return resource != null && resource.getProject().isOpen();
  }

  /**
   * Answer <code>true</code> if the receiver directly or indirectly imports the dart:dom or
   * dart:html libraries
   */
  public boolean isOrImportsBrowserLibrary() {
    List<DartLibrary> visited = new ArrayList<DartLibrary>(10);
    visited.add(this);
    for (int index = 0; index < visited.size(); index++) {
      DartLibrary library = visited.get(index);
      String libraryName = library.getElementName();
      if ("dart:html".equals(libraryName) || "dart:dom".equals(libraryName)) {
        return true;
      }
      try {
        for (DartLibrary importedLibrary : library.getImportedLibraries()) {
          if (!visited.contains(importedLibrary)) {
            visited.add(importedLibrary);
          }
        }
      } catch (DartModelException exception) {
        DartCore.logError(
            "Could not get the libraries imported by " + library.getDisplayName(),
            exception);
        continue;
      }
    }
    return false;
  }

  /**
   * Returns whether this library is a server application. This is distinct from
   * !isBrowserApplication() because a server application that is erroneously referenced from a html
   * should still be considered a server application.
   * 
   * @return whether this library is a server application.
   */
  public boolean isServerApplication() {
    return hasMain() && !isOrImportsBrowserLibrary();
  }

  @Override
  public boolean isTopLevel() {
    try {
      IFile definingResource = getDefiningResource();
      if (definingResource != null) {
        return definingResource.getPersistentProperty(TOP_LEVEL_PROPERTY_NAME) != null;
      }
    } catch (CoreException exception) {
      // Fall through to return the default value.
    }
    return false;
  }

  @Override
  public boolean isUnreferenced() throws DartModelException {
    return getDartModel().getUnreferencedLibraries().contains(this);
  }

  @Override
  public IResource resource() {
    return null;
  }

  @Override
  public void setTopLevel(boolean topLevel) {
    if (topLevel != isTopLevel()) {
      try {
        getDefiningResource().setPersistentProperty(
            TOP_LEVEL_PROPERTY_NAME,
            topLevel ? "true" : null);
        DartElementDeltaImpl delta = new DartElementDeltaImpl(this);
        delta.changed(DartElementDelta.F_TOP_LEVEL);
        DartModelManager.getInstance().getDeltaProcessor().fire(
            delta,
            ElementChangedEvent.POST_CHANGE);
      } catch (CoreException exception) {
        // Ignore
      }
    }
  }

  @Override
  protected boolean buildStructure(OpenableElementInfo info, final IProgressMonitor monitor,
      final Map<DartElement, DartElementInfo> newElements, final IResource underlyingResource)
      throws DartModelException {
    final DartLibraryInfo libraryInfo = (DartLibraryInfo) info;
    if (sourceFile == null) {
      libraryInfo.setChildren(DartElementImpl.EMPTY_ARRAY);
      return true;
    }
    final ArrayList<DartElementImpl> children = new ArrayList<DartElementImpl>();
    final CompilationUnitImpl definingUnit;
    if (libraryFile == null) {
      String relativePath = sourceFile.getName();
      definingUnit = new ExternalCompilationUnitImpl(
          this,
          relativePath,
          sourceFile.getSourceFor(relativePath));
      libraryInfo.setDefiningCompilationUnit(definingUnit);
      children.add(definingUnit);
    } else {
      definingUnit = new CompilationUnitImpl(
          DartLibraryImpl.this,
          libraryFile,
          DefaultWorkingCopyOwner.getInstance());
      libraryInfo.setDefiningCompilationUnit(definingUnit);
      children.add(definingUnit);
    }
    DartUnit unit = parseLibraryFile();
    if (unit == null) {
      libraryInfo.setChildren(children.toArray(new DartElementImpl[children.size()]));
      return true;
    }
    final ArrayList<IResource> resourceList = new ArrayList<IResource>();
    final DartModelManager modelManager = DartModelManager.getInstance();
    unit.accept(new SafeDartNodeTraverser<Void>() {
      @Override
      public Void visitImportDirective(DartImportDirective node) {
        // prepare "path"
        DartStringLiteral uriNode = node.getLibraryUri();
        String relativePath = getRelativePath(uriNode);
        if (relativePath == null) {
          return null;
        }
        // prepare SourceRanges
        SourceRange sourceRange = new SourceRangeImpl(
            node.getSourceInfo().getOffset(),
            node.getSourceInfo().getLength());
        SourceRange uriRange = sourceRange;
        if (uriNode != null) {
          uriRange = new SourceRangeImpl(
              uriNode.getSourceInfo().getOffset(),
              uriNode.getSourceInfo().getLength());
        }
        // prepare "prefix"
        String prefix = null;
        SourceRange nameRange = null;
        {
          DartStringLiteral prefixLiteral = node.getPrefix();
          if (prefixLiteral != null) {
            prefix = prefixLiteral.getValue();
            SourceInfo prefixSourceInfo = prefixLiteral.getSourceInfo();
            nameRange = new SourceRangeImpl(
                prefixSourceInfo.getOffset(),
                prefixSourceInfo.getLength());
          }
        }
        // prepare LibrarySource
        LibrarySource librarySource;
        try {
          librarySource = sourceFile.getImportFor(relativePath);
        } catch (Exception exception) {
          DartCore.logError(
              "Failed to resolve import " + relativePath + " in " + sourceFile.getUri(),
              exception);
          return null;
        }
        if (librarySource == null) {
          DartCore.logError("Failed to resolve import " + relativePath + " in "
              + sourceFile.getUri());
          return null;
        } else if (SystemLibraryManager.isDartUri(librarySource.getUri())) {
          // It is a bundled library.
          try {
            if (librarySource.exists()) {
              DartLibraryImpl library = new DartLibraryImpl(librarySource);
              libraryInfo.addImport(new DartImportImpl(
                  definingUnit,
                  sourceRange,
                  uriRange,
                  library,
                  prefix,
                  nameRange));
            }
          } catch (Exception exception) {
            // The library is not valid, so we don't add it.
          }
          return null;
        } else if (!librarySource.exists()) {
          // Don't add non-existent libraries.
          return null;
        }

        // Find a resource in the workspace corresponding to the imported library.
        IResource[] libraryFiles = ResourceUtil.getResources(librarySource);
        if (libraryFiles != null && libraryFiles.length == 1 && libraryFiles[0] instanceof IFile) {
          IFile libFile = (IFile) libraryFiles[0];
          DartProjectImpl dartProject = modelManager.create(libFile.getProject());
          DartLibraryImpl library = new DartLibraryImpl(dartProject, libFile, librarySource);
          libraryInfo.addImport(new DartImportImpl(
              definingUnit,
              sourceRange,
              uriRange,
              library,
              prefix,
              nameRange));
          return null;
        }

        // Find an external library on disk.
        File libFile = ResourceUtil.getFile(librarySource);
        if (libFile != null) {
//          DartLibraryImpl library = null;
//          try {
//            library = (DartLibraryImpl) modelManager.openLibrary(libFile, monitor);
//          } catch (DartModelException exception) {
//            // I believe that this should not happen, but I'm leaving it in until I can confirm this.
//            DartCore.logInformation("Failed to open imported library " + relativePath + " in "
//                + sourceFile.getUri(), exception);
//          }
//          if (library == null) {
          DartLibraryImpl library = new DartLibraryImpl(libFile);
          libraryInfo.addImport(new DartImportImpl(
              definingUnit,
              sourceRange,
              uriRange,
              library,
              prefix,
              nameRange));
//          } else {
//            importedLibraries.add(library);
//          }
          return null;
        }

        // Otherwise, the library could not be resolved.
        DartCore.logError("Failed to resolve import " + relativePath + " in " + sourceFile);
        return null;
      }

      @Override
      public Void visitLibraryDirective(DartLibraryDirective node) {
        DartStringLiteral literal = node.getName();
        if (literal == null) {
          return null;
        }
        libraryInfo.setName(literal.getValue());
        return null;
      }

      @Override
      public Void visitResourceDirective(DartResourceDirective node) {
        String relativePath = getRelativePath(node.getResourceUri());
        if (relativePath == null) {
          return null;
        }
        DartSource source = sourceFile.getSourceFor(relativePath);
        if (source == null) {
          return null;
        }
        URI uri = URIUtilities.makeAbsolute(source.getUri());
        children.add(new DartResourceImpl(DartLibraryImpl.this, uri));
        return null;
      }

      @Override
      public Void visitSourceDirective(DartSourceDirective node) {
        String relativePath = getRelativePath(node.getSourceUri());
        if (relativePath == null || relativePath.length() == 0) {
          return null;
        }
        DartSource source = sourceFile.getSourceFor(relativePath);
        if (source == null) {
          return null;
        }
        IResource[] compilationUnitFiles = ResourceUtil.getResources(source);
        if (compilationUnitFiles != null && compilationUnitFiles.length == 1
            && compilationUnitFiles[0] instanceof IFile) {
          IFile unitFile = (IFile) compilationUnitFiles[0];
          if (unitFile.isAccessible()) {
            children.add(new CompilationUnitImpl(
                DartLibraryImpl.this,
                unitFile,
                DefaultWorkingCopyOwner.getInstance()));
            return null;
          }
        }
        children.add(new ExternalCompilationUnitImpl(DartLibraryImpl.this, relativePath, source));
        return null;
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
    });

    // find html files for library
    try {
      if (getDartProject().exists()) { // will not look into ExternalDartProject
        final String elementName = getElementName();
        getDartProject().getProject().accept(new IResourceProxyVisitor() {
          @Override
          public boolean visit(IResourceProxy proxy) throws CoreException {
            if (proxy.getType() != IResource.FILE || !DartCore.isHTMLLikeFileName(proxy.getName())) {
              return true;
            }
            IResource resource = proxy.requestResource();
            if (!resourceList.contains(resource)) {
              if (resource.isAccessible()) {
                try {
                  List<String> libraryNames = LibraryReferenceFinder.findInHTML(IFileUtilities.getContents((IFile) resource));
                  for (String libraryName : libraryNames) {
                    if (elementName.equals(libraryName) || elementName.endsWith("/" + libraryName)) {
                      children.add(new HTMLFileImpl(DartLibraryImpl.this, (IFile) resource));
                      break;
                    }
                  }
                } catch (IOException exception) {
                  DartCore.logInformation(
                      "Could not get contents of " + resource.getLocation(),
                      exception);
                }
              }
            }
            return true;
          }
        },
            0);
      }
    } catch (CoreException exception) {
      DartCore.logError(exception);
    }

    if (!children.isEmpty()) {
      libraryInfo.setChildren(children.toArray(new DartElementImpl[children.size()]));
    }
    return true;
  }

  @Override
  protected DartElementInfo createElementInfo() {
    return new DartLibraryInfo();
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    switch (token.charAt(0)) {
      case MEMENTO_DELIMITER_COMPILATION_UNIT:
        if (!tokenizer.hasMoreTokens()) {
          return this;
        }
        String path = tokenizer.nextToken();
        CompilationUnitImpl unit;
        if (getDartProject().exists()) {
          unit = new CompilationUnitImpl(
              this,
              libraryFile.getProject().getFile(new Path(path)),
              owner);
        } else {
          unit = new ExternalCompilationUnitImpl(this, path);
        }
        return unit.getHandleFromMemento(tokenizer, owner);
      case MEMENTO_DELIMITER_LIBRARY_FOLDER:
        if (!tokenizer.hasMoreTokens()) {
          return this;
        }
        String folderName = tokenizer.nextToken();
        IFolder parentFolder = libraryFile.getParent().getFolder(new Path(folderName));
        DartLibraryFolderImpl folder = new DartLibraryFolderImpl(this, parentFolder);
        return folder.getHandleFromMemento(tokenizer, owner);
      case MEMENTO_DELIMITER_HTML_FILE:
        if (!tokenizer.hasMoreTokens()) {
          return this;
        }
        String htmlPath = tokenizer.nextToken();
        HTMLFileImpl file = new HTMLFileImpl(this, libraryFile.getProject().getFile(
            new Path(htmlPath)));
        return file.getHandleFromMemento(tokenizer, owner);
      case MEMENTO_DELIMITER_RESOURCE:
        if (!tokenizer.hasMoreTokens()) {
          return this;
        }
        String resourceUri = tokenizer.nextToken();
        try {
          DartResourceImpl resource = new DartResourceImpl(this, new URI(resourceUri));
          return resource.getHandleFromMemento(tokenizer, owner);
        } catch (URISyntaxException exception) {
          DartCore.logError(
              "Illegal URI found in memento for a resource: \"" + resourceUri + "\"",
              exception);
          return null;
        }
    }
    return null;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return MEMENTO_DELIMITER_LIBRARY;
  }

  @Override
  protected String getHandleMementoName() {
    URI uri = getUri();
    URI shortUri = SystemLibraryManagerProvider.getSystemLibraryManager().getShortUri(uri);
    if (shortUri != null) {
      return shortUri.toString();
    }
    if (uri == null) {
      // If the library location in the workspace directory hierarchy
      // then return a path relative to the workspace root
      String absPath = getElementName();
      IPath libPath = new Path(absPath);
      IPath rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
      if (rootPath.isPrefixOf(libPath)) {
        return libPath.removeFirstSegments(rootPath.segmentCount()).toString();
      }
      // otherwise return an absolute path.
      return absPath;
    }
    return uri.toString();
  }

  /**
   * Return the URI of the library file that defines this library, or <code>null</code> if there is
   * no such file or if the URI for the file cannot be determined for some reason.
   * 
   * @return the URI of the library file that defines this library
   */
  protected URI getUri() {
    if (sourceFile != null) {
      return sourceFile.getUri();
    }
    return libraryFile.getLocationURI();
  }

  /**
   * The parent of an external Dart library is a hidden project that cannot be opened. Override to
   * prevent superclass method from trying to open the parent of an external library.
   */
  @Override
  protected void openAncestors(HashMap<DartElement, DartElementInfo> newElements,
      IProgressMonitor monitor) throws DartModelException {
    if (getParent().exists()) {
      super.openAncestors(newElements, monitor);
    }
  }

  @Override
  protected IStatus validateExistence(IResource underlyingResource) {
    DartCore.notYetImplemented();
    return DartModelStatusImpl.OK_STATUS;
  }

  /**
   * Add a directive to the compilation unit that defines this library that will include the given
   * file as a part of this library.
   * 
   * @param directiveName the name of the directive (with the leading pound sign)
   * @param file the file to reference in the directive
   * @param monitor the progress monitor used to provide feedback to the user, or <code>null</code>
   *          if no feedback is desired
   * @return <code>true</code> if the change was saved to disk, requiring the model to be updated
   * @throws DartModelException if the directive cannot be added
   */
  private boolean addDirective(String directiveName, File file, IProgressMonitor monitor)
      throws DartModelException {
    CompilationUnit libraryUnit = getDefiningCompilationUnit();
    CompilationUnit workingCopy = libraryUnit.getWorkingCopy(
        DefaultWorkingCopyOwner.getInstance(),
        monitor);
    boolean hadUnsavedChanges = workingCopy.hasUnsavedChanges();
    Buffer buffer = workingCopy.getBuffer();
    String relativePath = libraryFile.getLocation().removeLastSegments(1).toFile().toURI().relativize(
        file.toURI()).getPath();
    int insertionPoint = SourceUtilities.findInsertionPointForSource(
        buffer.getContents(),
        directiveName,
        relativePath);
    // TODO(brianwilkerson) This won't add a blank line if this is the first directive of its kind.
    buffer.replace(insertionPoint, 0, directiveName + "('" + relativePath + "');"
        + SourceUtilities.LINE_SEPARATOR);
    workingCopy.makeConsistent(monitor);

    if (!hadUnsavedChanges) {
      // Save the changes we just made.
      workingCopy.commitWorkingCopy(true, monitor);
      return true;
    }
    return false;
  }

  /**
   * Return the resource associated with the defining compilation unit.
   * 
   * @return the resource associated with the defining compilation unit
   */
  private IFile getDefiningResource() {
    try {
      CompilationUnit compilationUnit = getDefiningCompilationUnit();
      if (compilationUnit == null) {
        return null;
      }
      return (IFile) compilationUnit.getUnderlyingResource();
    } catch (DartModelException exception) {
      return null;
    }
  }

  private boolean hasReferencingHtmlFile() {
    DartElement[] children;
    try {
      children = getChildren();
    } catch (DartModelException e) {
      DartCore.logError("Could not determine if " + getDisplayName()
          + " has an HTML file referencing it", e);
      return false;
    }
    for (DartElement child : children) {
      if (child instanceof HTMLFileImpl) {
        HTMLFileImpl htmlFile = (HTMLFileImpl) child;
        DartLibrary[] referencedLibraries;
        try {
          referencedLibraries = htmlFile.getReferencedLibraries();
        } catch (DartModelException e) {
          DartCore.logError("Could not determine if " + htmlFile.getElementName() + " references "
              + getDisplayName(), e);
          continue;
        }
        for (DartLibrary lib : referencedLibraries) {
          if (this.equals(lib)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Return the result of parsing the file that defines this library, or <code>null</code> if the
   * contents of the file cannot be accessed for some reason.
   * 
   * @return the result of parsing the file that defines this library
   */
  private DartUnit parseLibraryFile() {
    String fileName = null;
    try {
      if (sourceFile != null) {
        fileName = sourceFile.getName();
        return DartCompilerUtilities.parseSource(
            fileName,
            FileUtilities.getContents(sourceFile.getSourceReader()),
            null);
      }
      if (libraryFile != null && libraryFile.exists()) {
        fileName = libraryFile.getName();
        return DartCompilerUtilities.parseSource(
            fileName,
            IFileUtilities.getContents(libraryFile),
            null);
      }
    } catch (Exception exception) {
      DartCore.logInformation("Could not read and parse the file " + fileName, exception);
      // Fall through to return null.
    }
    return null;
  }
}
