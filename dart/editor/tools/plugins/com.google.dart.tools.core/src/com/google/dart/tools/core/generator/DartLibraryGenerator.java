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
package com.google.dart.tools.core.generator;

import com.google.dart.compiler.DartCompiler;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.util.Paths;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.internal.util.StatusUtil;
import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.core.model.DartLibrary;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Instances of <code>DartLibraryGenerator</code> are used to create a new Dart library in an
 * existing Dart project after validating the name of the new library.
 */
@Deprecated
//TODO(pquitslund): remove me!
public class DartLibraryGenerator extends DartFileGenerator {

  static {
    DartCore.notYetImplemented();
    // replace the following constants once Dart compiler constants are
    // available
  }

  /**
   * Generate source declaring a library. If an entryPoint is provided a main() method will be
   * synthesized which wraps a call to the provided entryPoint method.
   * 
   * @param name the name of the application or library
   * @param baseFile the application or library file that will contain this source or any file in
   *          that same directory (not <code>null</code>, but does not need to exist)
   * @param importFiles a collection of library files imported by this application or library
   * @param sourceFiles a collection of dart source files included in this application or library
   * @param entryPoint The name of the static method to call to invoke the library. A synthetic
   *          main() method will be generated which wraps a call to this method. Pass
   *          <code>null</code> to use the default main() method lookup.
   * @return the source (not <code>null</code>)
   */
  public static String generateSource(String name, File baseFile, List<File> importFiles,
      List<File> sourceFiles, String entryPoint) {
    // Copied from DefaultLibrarySource
    StringWriter sw = new StringWriter(200);
    PrintWriter pw = new PrintWriter(sw);
    pw.println("#library(\"" + name + "\");");
    if (importFiles != null) {
      for (File file : importFiles) {
        String relPath = file.getPath();
        if (!relPath.startsWith("dart:")) {
          relPath = Paths.relativePathFor(baseFile, file);
        }
        if (relPath != null) {
          pw.println("#import(\"" + relPath + "\");");
        }
      }
    }
    if (sourceFiles != null) {
      for (File file : sourceFiles) {
        String relPath = Paths.relativePathFor(baseFile, file);
        if (relPath != null) {
          pw.println("#source(\"" + relPath + "\");");
        }
      }
    }
    if (entryPoint != null) {
      // synthesize a main method, which wraps the entryPoint method call
      pw.println();
      pw.println(DartCompiler.MAIN_ENTRY_POINT_NAME + "() {");
      pw.println("  " + entryPoint + "();");
      pw.println("}");
    }
    return sw.toString();
  }

  /**
   * The class name to be referenced in the application or library
   */
  private String className = "";

  /**
   * The list of imports of this library
   */
  private List<DartLibrary> imports = new ArrayList<DartLibrary>();

  /**
   * The list of sources of this library
   */
  private List<IResource> sources = new ArrayList<IResource>();

  private IStatus importValidation;

  /**
   * Construct a new instance.
   * 
   * @param containerMustExist If <code>true</code> then the container must exist for the
   *          {@link #validateContainer()} method to return <code>true</code>.
   */
  public DartLibraryGenerator(boolean containerMustExist) {
    super(containerMustExist);
  }

  /**
   * Create the folder to contain the library and the library declaration file
   * 
   * @param monitor the monitor to which activity is reported
   */
  public void execute(IProgressMonitor monitor) throws CoreException {
    if (validate().getSeverity() == IStatus.ERROR) {
      throw new IllegalStateException(validate().getMessage());
    }

    workspace.run(new IWorkspaceRunnable() {

      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        monitor.beginTask("", 300); //$NON-NLS-1$

        // Create the folder if it does not already exist
        createFolderIfNecessary(getContainerPath(), monitor);
        File libFile = getFile().getLocation().toFile();
        monitor.worked(100);

        // Create the library file
        List<File> sourceFiles = new ArrayList<File>();
        String entryPoint = null;
        //TODO: (mmay) vv used only in the temporary NewDartProjectWizard (to be removed)
        if (className.length() > 0) {
          sourceFiles.add(Paths.relativePathToFile(libFile, className + Extensions.DOT_DART));
        }
        //^^used only in the temporary NewDartProjectWizard (to be removed)
        for (IResource source : sources) {
          sourceFiles.add(Paths.relativePathToFile(libFile, source.getName()));
        }
        List<File> importedLibs = new ArrayList<File>(imports.size());

        for (DartLibrary lib : imports) {
          LibrarySource libSource = ((DartLibraryImpl) lib).getLibrarySourceFile();
          File file = ResourceUtil.getFile(libSource);
          if (file != null) {
            importedLibs.add(file);
          }
        }

        String source = generateSource( // was DefaultLibrarySource.generateSource(
            getSimpleLibraryName(),
            libFile,
            importedLibs,
            sourceFiles,
            entryPoint);

        InputStream input = new ByteArrayInputStream(source.getBytes());
        if (getFile() != null) {
          getFile().create(input, false, monitor);
        }
        monitor.done();
      }
    }, monitor);
  }

  /**
   * Return the class name to be referenced in the application or library
   * 
   * @return the className (not <code>null</code>)
   * @deprecated because with this method you can only add one class to a library this way, the
   *             get/setSources is more expandable
   */
  @Deprecated
  public String getClassName() {
    return className;
  }

  /**
   * Answer the file to be created
   * 
   * @return the file or <code>null</code> if a file cannot be created
   */
  public IFile getFile() {
    if (validateContainer().getSeverity() == IStatus.ERROR) {
      return null;
    }
    return workspace.getRoot().getFile(getLibraryPath());
  }

  /**
   * Answer the container for the library
   * 
   * @return the container or <code>null</code> if unspecified.
   */
  public IContainer getLibraryContainer() {
    if (validateContainer().matches(IStatus.ERROR) || validateName().matches(IStatus.ERROR)) {
      return null;
    }
    IPath path = getLibraryPath().removeLastSegments(1);
    if (path.segmentCount() == 1) {
      return workspace.getRoot().getProject(path.lastSegment());
    }
    return workspace.getRoot().getFolder(path);
  }

  /**
   * Answer a suggested class name based upon the current application name.
   * 
   * @return the suggested class name (not <code>null</code>)
   */
  public String getSuggestedClassName() {
    String name = getSimpleLibraryName();
    if (name.length() > 0) {
      int index = 0;
      while (index < name.length()) {
        if (Character.isWhitespace(name.charAt(index))) {
          name = name.substring(0, index) + name.substring(index + 1, name.length());
        } else {
          index++;
        }
      }
    }
    if (name.length() > 0 && Character.isLowerCase(name.charAt(0))) {
      name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    return name;
  }

  /**
   * Set the class name to be referenced in the application or library
   * 
   * @param className the class name
   */
  @Deprecated
  public void setClassName(String className) {
    this.className = className != null ? className.trim() : "";
  }

  /**
   * Set the import list relative to the generated library file.
   * 
   * @param importsRelativeToWorkspace the imports {@link List} relative to the Eclipse workspace
   */
  public void setImportList(List<DartLibrary> importedLibraries) {
    this.imports = importedLibraries != null ? importedLibraries : new ArrayList<DartLibrary>(0);
  }

  public void setImportValidation(IStatus status) {
    this.importValidation = status;
  }

  /**
   * Set the sources list relative to the generated library file.
   * 
   * @param sourcesRelativeToWorkspace the sources {@link List} relative to the Eclipse workspace
   */
  public void setSourcesList(List<IResource> importedSources) {
    this.sources = importedSources != null ? importedSources : new ArrayList<IResource>(0);

  }

  /**
   * Checks that the new lib/app is valid
   * 
   * @return IStatus corresponding to any errors/warnings that would make the lib/app invalid
   */
  @Override
  public IStatus validate() {
    IStatus status = StatusUtil.getMoreSevere(Status.OK_STATUS, validateContainer());
    status = StatusUtil.getMoreSevere(status, validateName());
    status = StatusUtil.getMoreSevere(status, validateImports());

    if (getClassName().length() > 0 || sources.size() > 0) {
      if (sources.size() == 0) {
        status = StatusUtil.getMoreSevere(status, DartConventions.validateTypeName(getClassName()));
      }
    }
    if (status.getSeverity() != IStatus.ERROR) {
      IFile file = getFile();
      if (file.exists()) {
        status = StatusUtil.getMoreSevere(status, error("A file with that name already exists"));
      }
    }
    return status;
  }

  /**
   * Answer the simple library name without the file extension or leading path segments
   * 
   * @return the simple name (not <code>null</code>)
   */
  protected String getSimpleLibraryName() {
    String name = new Path(getName()).lastSegment();
    if (name == null) {
      return "";
    }
    String fileExtension = Extensions.DOT_DART;
    if (name.endsWith(fileExtension)) {
      name = name.substring(0, name.length() - fileExtension.length());
    }
    return name;
  }

  /**
   * Answer the full path for the library
   * 
   * @return the path (not <code>null</code>)
   */
  private IPath getLibraryPath() {
    String name = getName();
    String fileExtension = Extensions.DOT_DART;
    if (!name.endsWith(fileExtension)) {
      name += fileExtension;
    }
    return getContainerPath().append(name);
  }

  /**
   * Validate the import paths
   * 
   * @return {@link Status#OK_STATUS} if the imports are valid, or a status indicating an error or
   *         warning.
   */
  private IStatus validateImports() {
    if (importValidation != null) {
      return importValidation;
    }
    return Status.OK_STATUS;
  }

  /**
   * Validate the library name
   * 
   * @return {@link Status#OK_STATUS} if the name is valid, or a status indicating an error or
   *         warning.
   */
  private IStatus validateName() {
    String name = getName();
    IPath path = new Path(name);
    if (name.length() == 0 || path.segmentCount() == 0) {
      return error("Enter Library name");
    }
    if (path.isAbsolute()) {
      return error("Library name must be relative.");
    }
    if (path.hasTrailingSeparator()) {
      return error("Library name cannot end with '" + name.charAt(name.length() - 1) + "'");
    }
    int index = path.segmentCount() - 1;
    IStatus status = workspace.validateName(path.lastSegment(), IResource.FILE);
    if (!status.isOK()) {
      return status;
    }
    while (--index >= 0) {
      status = workspace.validateName(path.segment(index), IResource.FOLDER);
      if (!status.isOK()) {
        return status;
      }
    }
    return Status.OK_STATUS;
  }
}
