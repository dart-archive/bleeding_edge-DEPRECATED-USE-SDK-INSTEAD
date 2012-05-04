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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.HTMLFileImpl;
import com.google.dart.tools.core.internal.model.info.DartLibraryInfo;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.internal.util.StatusUtil;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.general.SourceUtilities;
import com.google.dart.tools.core.utilities.resource.IProjectUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;

/**
 * Instances of <code>FileGenerator</code> are used to create a new file in an existing Library
 * after validating the name of the new class.
 */
public class FileGenerator extends AbstractGenerator {

  private String fileName;

  private String fileLocation;

  private DartLibraryImpl library = null;

  private IFile iFile = null;

  public static final String DESCRIPTION = GeneratorMessages.FileGenerator_Description;

  public static String getStringLibraryPath(DartLibrary library) {
    try {
      return library.getCorrespondingResource().getLocation().makeAbsolute().removeLastSegments(1).toOSString();
    } catch (DartModelException e) {
      DartCore.logError(e);
    }
    return null;
  }

  /**
   * Construct a new instance.
   */
  public FileGenerator() {
  }

  /**
   * Generate the new file.
   * 
   * @param monitor the progress monitor (not <code>null</code>)
   * @throws CoreException
   */
  @Override
  public void execute(IProgressMonitor monitor) throws CoreException {
    // Sanity Checks
    // Fail fast for null elements
    Assert.isNotNull(fileName);
    Assert.isNotNull(fileLocation);

    IStatus status = validate();
    if (status.getSeverity() == IStatus.ERROR) {
      throw new IllegalStateException(status.getMessage());
    }

    // If the entered file name does not have a '.', then append ".dart" to the name, then this case
    // will fall through to the isDartLikeFileName case below
    SubMonitor subMonitor = SubMonitor.convert(
        monitor,
        GeneratorMessages.FileGenerator_message,
        100);

    subMonitor.newChild(5);
    fileName = appendIfNoExtension(fileName, Extensions.DOT_DART);
    final File systemFile = getSystemFile();

    // for non-library file creations, we need to compute which library configuration file we need 
    // to modify (to include the resource which is about to be created)

    // if some library was selected, and fileLocation is pointing to the location of the selected library,
    // then we make the assumption that we want to add it to this specific library in this file location,
    // otherwise the current library shouldn't be used
    if (library != null) {
      String directoryOfLibrary = null;
      String temp = getStringLibraryPath(library);
      if (temp != null && !temp.isEmpty()) {
        directoryOfLibrary = temp;
      }
      // this is the check to see if the fileLocation is pointing to the location of the selected library, as mentioned above
      if (directoryOfLibrary != null && directoryOfLibrary.equals(fileLocation)) {
        // do nothing, current library is correct.
      } else {
        library = null;
      }
    }
    // otherwise, we have the location of where the file will be written to, but we don't know which library we should modify
    // If we don't have a library yet, and the destination directory exists on disk, then find the
    // library in that directory, that is also loaded into the editor.
    File directory = new File(fileLocation);
    if (library == null && directory.exists()/* destination directory exists */) {
      library = (DartLibraryImpl) DartCore.findLibraryInDirectory(directory);
    }

    subMonitor.newChild(15);

    final HashMap<String, String> substitutions = new HashMap<String, String>();
    String nameOfSrcTxt;
    boolean isNewLibrary = false;
    boolean isSrc = false;

    if (DartCore.isDartLikeFileName(fileName)) {
      // DART file
      String className = fileName.substring(0, fileName.indexOf('.'));
      substitutions.put("className", className); //$NON-NLS-1$

      if (library != null) {
        substitutions.put("directives", ""); //$NON-NLS-1$ //$NON-NLS-2$
      } else {
        // need the #library directive in the following, otherwise it isn't a defining compilation unit!
        substitutions.put("directives", "#library('" + className
            + "');" + SourceUtilities.LINE_SEPARATOR + SourceUtilities.LINE_SEPARATOR); //$NON-NLS-1$ //$NON-NLS-2$

        isNewLibrary = true;
      }
      nameOfSrcTxt = "generated-dart-class-empty.txt";

      isSrc = true;

    } else if (DartCore.isHTMLLikeFileName(fileName)) {
      // HTML file
      String name = fileName.substring(0, fileName.indexOf('.'));
      substitutions.put("title", name); //$NON-NLS-1$
      String jsGeneratedFileName = name;
      if (library != null) {
        jsGeneratedFileName = library.getImplicitLibraryName();
      }
      substitutions.put("dartPath", jsGeneratedFileName + ".dart.app.js"); //$NON-NLS-1$ //$NON-NLS-2$

      nameOfSrcTxt = "generated-html.txt";

    } else if (DartCore.isCSSLikeFileName(fileName)) {
      // CSS file- same as empty file case, for now
      nameOfSrcTxt = "generated-empty-file.txt";

    } else {
      // empty file
      nameOfSrcTxt = "generated-empty-file.txt";
    }

    subMonitor.newChild(40);

    //
    // Finally, write the content to the file.
    //
    execute(nameOfSrcTxt, systemFile, substitutions, monitor); //$NON-NLS-1$

    if (isNewLibrary) {
      //
      // Call DartLibrary.openLibrary(..) to link the new file into the workspace.
      //
      DartCore.openLibrary(systemFile, monitor);
    } else if (library != null) {
      //
      // Call library.addSource(..) to append the additional #source tag onto the library
      //
      if (isSrc) {
        library.addSource(systemFile, monitor);
      } else {
        // else, add the #resource for all non-html resource files
        if (!DartCore.isHTMLLikeFileName(fileName)) {
          library.addResource(systemFile, monitor);
        } else {
          // Even though we aren't having the #resource(..) added for new HTML files, we do need the
          // new file linked into the project so that the resource change listener will be able to
          // trigger the DeltaProcessor.
          IFile iFile = IProjectUtilities.addLinkToProject(
              library.getDartProject().getProject(),
              systemFile,
              monitor);
          // Finally, create and add the new HTMLFile into the model
          HTMLFileImpl htmlFile = new HTMLFileImpl(library, iFile);
          DartLibraryInfo libraryInfo = (DartLibraryInfo) library.getElementInfo();
          libraryInfo.addChild(htmlFile);
        }
      }
      library.setTopLevel(true);
    } else {
      // TODO The validator should be updated to ensure that this case is never reached.
      // should never get here, validator should be updated if it does.
    }

    //
    // Find and reference the IFile so that getFile() will return the correct resource
    //
    IResource[] files = ResourceUtil.getResources(systemFile);
    if (files.length > 0 && files[0] instanceof IFile) {
      iFile = (IFile) files[0];
    }
    subMonitor.newChild(40);

    subMonitor.done();
  }

  public IFile getFile() {
    return iFile;
  }

  public String getFileLocation() {
    return fileLocation;
  }

  public String getFileName() {
    return fileName;
  }

  public DartLibrary getLibrary() {
    return library;
  }

  public File getSystemFile() {
    // Sanity Check. Fail fast for null elements.
    Assert.isNotNull(fileName);
    Assert.isNotNull(fileLocation);
    return new File(fileLocation + File.separator
        + appendIfNoExtension(fileName, Extensions.DOT_DART));
  }

  public void setFileLocation(String fileLocation) {
    this.fileLocation = fileLocation;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setLibrary(DartLibraryImpl library) {
    this.library = library;
  }

  @Override
  public IStatus validate() {
    IStatus status = StatusUtil.getMoreSevere(Status.OK_STATUS, validateFileLocation());
    status = StatusUtil.getMoreSevere(status, validateFileName());
    return status;
  }

  private IStatus validateFileLocation() {
    // Validate that:
    // 1) The file location is non-null and non-empty;
    // 2) The file location does not exist, and is a directory;
    // 3) The file location can be linked.
    if (fileLocation == null || fileLocation.isEmpty()) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, DESCRIPTION);
    }
//    File file = new File(fileLocation);
//    if (!file.isDirectory()) {
//      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, MessageFormat.format(
//          GeneratorMessages.FileGenerator_locationDoesNotExist, new Object[] {fileLocation}));
//    }
//    if ((new Path(fileLocation)).isPrefixOf(ResourcesPlugin.getWorkspace().getRoot().getLocation().makeAbsolute())) {
//      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, MessageFormat.format(
//          GeneratorMessages.FileGenerator_filesCannotBeGenerated, new Object[] {fileLocation})); //$NON-NLS-2$
//    }
    return Status.OK_STATUS;
  }

  private IStatus validateFileName() {
    // Validate that:
    // 1) The file is non-null and non-empty;
    // 2) The file does not contain any whitespace;
    // 3) The file exists already.
    // 4) Validation for Dart file type names, i.e. "-" not allowed in Dart type names
    if (fileName == null || fileName.isEmpty()) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, DESCRIPTION);
    } else if (containsWhitespace(fileName)) {
      return new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          GeneratorMessages.FileGenerator_whiteSpaceNotAllowed);
    } else if (getSystemFile().exists()) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, MessageFormat.format(
          GeneratorMessages.FileGenerator_fileExists,
          new Object[] {getSystemFile().getName()}));
    }
    String name = appendIfNoExtension(fileName, Extensions.DOT_DART);
    if (name.endsWith(Extensions.DOT_DART)) {
      String prefix = name.substring(0, name.lastIndexOf('.'));
      IStatus status = DartIdentifierUtil.validateIdentifier(prefix);
      if (status != Status.OK_STATUS) {
        return status;
      }
    }
    return Status.OK_STATUS;
  }
}
