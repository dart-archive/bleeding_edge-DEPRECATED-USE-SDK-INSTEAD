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
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.internal.util.StatusUtil;
import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.core.model.DartElement;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Instances of <code>DartTypeGenerator</code> are used to create a new Dart type in an existing
 * Dart library or application after validating the name of the new class.
 */
public class DartClassGenerator extends DartFileGenerator {
  static {
    DartCore.notYetImplemented();
    // replace the following constants once Dart compiler constants are
    // available
  }
  /**
   * If true the generated class template will have no main method or constructor or run method
   */
  private boolean isEmptyClass = false;

  /**
   * Dart Element corresponding to the parent type of our new type
   */
  private DartElement superType;

  /**
   * Array of all the interfaces extended by the new interface to be created
   */
  private DartElement[] interfaceList = new DartElement[0];

  /**
   * Dart Element corresponding to the library we want to add this new class to
   */
  private String library;

  private boolean mustBuildLibrary;

  /**
   * Construct a new instance.
   * 
   * @param containerMustExist If <code>true</code> then the container must exist for the
   *          {@link #validateContainer()} method to return <code>true</code>.
   */
  public DartClassGenerator(boolean containerMustExist) {
    super(containerMustExist);
  }

  /**
   * Generate the new type
   * 
   * @param monitor the progress monitor (not <code>null</code>)
   */
  public void execute(IProgressMonitor monitor) throws CoreException {
    final HashMap<String, String> substitutions = new HashMap<String, String>();
    substitutions.put("extends", getExtendsString());
    substitutions.put("implements", getImplementsString());
    substitutions.put("className", getName());
    String templateFile;
    if (isEmptyClass) {
      templateFile = "generated-dart-class.txt";
    } else {
      templateFile = "generated-dart-class-main.txt";
    }

    SubMonitor subMonitor = SubMonitor.convert(monitor, "Generating class", 100);

    // Create the folder if it does not already exist
    createFolderIfNecessary(getContainerPath(), monitor);
    subMonitor.newChild(20);

    execute(templateFile, getFile(), substitutions, monitor);
    subMonitor.newChild(60);

    if (mustBuildLibrary) {
      DartLibraryGenerator libGen = new DartLibraryGenerator(false);
      Path libPath = new Path(library);
      libGen.setName(libPath.lastSegment());
      if (getFile() != null && getFile() instanceof IResource) {
        List<IResource> sources = new LinkedList<IResource>();
        sources.add(getFile());
        libGen.setSourcesList(sources);
        if (libPath.segmentCount() == 1) {
          libGen.setContainerPath(this.getContainerPath());
        } else {
          libGen.setContainerPath(libPath.removeLastSegments(1).toOSString());
        }
      }
      if (libGen.validate().getSeverity() != IStatus.ERROR) {
        try {
          libGen.execute(monitor);
          subMonitor.newChild(20);
        } catch (CoreException e) {
          //Do nothing bad lib path given
        }
      }
    } else if (library != null) {
      DartCore.notYetImplemented();
//      IResource lib = root.findMember(new Path(library));
//      if (lib != null) {//if mustBuildLibrary is false then lib should not legally be null
//        if (DartCore.create(lib) instanceof LibraryConfigurationFileImpl) {
//          LibraryConfigurationFileImpl library = (LibraryConfigurationFileImpl) DartCore.create(lib);
//          library.addSource(getName() + Extensions.DOT_DART);
//        }
//      }
    }
    monitor.done();
  }

  /**
   * Answer the file to be created
   * 
   * @return the file or <code>null</code> if a file cannot be created
   */
  public IFile getFile() {
    if (validate().getSeverity() == IStatus.ERROR) {
      return null;
    }
    return getContainer().getFile(new Path(getName() + Extensions.DOT_DART));
  }

  /**
   * Answer the suggested HTML file name based upon the current type name, by stripping off
   * characters up to and including the last dot '.' if one exists.
   * 
   * @param isApplication <code>true</code> if the name is for a Dart application file or
   *          <code>false</code> if the name is for a Dart library file
   * @return the HTML file name (not <code>null</code>, but may be empty)
   */
  public String getSuggestedHtmlFileName(boolean isApplication) {
    if (!isApplication) {
      return "";
    }
    String libName = stripFileExtension(getName());
    if (libName.length() == 0) {
      return "";
    }
    return libName + DartHtmlGenerator.HTML_FILENAME_EXTENSION;
  }

  /**
   * @return True if an Empty Class is desired
   */
  public boolean isEmptyClass() {
    return isEmptyClass;
  }

  /**
   * @param isEmptyClass-Set whether an Empty Class is desired
   */
  public void setEmptyClass(boolean isEmptyClass) {
    this.isEmptyClass = isEmptyClass;
  }

  /**
   * Set the list of interfaces to be implemented by our new type
   * 
   * @param newInterfaceList-array of DartElements that are Interfaces to be implemented in our new
   *          class
   */
  public void setInterfaceList(DartElement[] newInterfaceList) {
    this.interfaceList = newInterfaceList;
  }

  /**
   * Set the library that our new type will be added to
   * 
   * @param library-Instance of the library this class will be added as a source to
   */
  public void setLibrary(String library) {
    this.library = library;
  }

  /**
   * Set the parent type of the new Type
   * 
   * @param newParentType-Instance of the Parent type
   */
  public void setParentType(DartElement newParentType) {
    superType = newParentType;
  }

  /**
   * Checks that the new lib/app is valid
   * 
   * @return IStatus corresponding to any errors/warnings that would make the lib/app invalid
   */
  @Override
  public IStatus validate() {
    // add check to DartConventions.validateCompilationUnit(...) as well
    // when it becomes available.
    IStatus status = StatusUtil.getMoreSevere(Status.OK_STATUS, validateContainer());
    status = StatusUtil.getMoreSevere(status, DartConventions.validateTypeName(getName()));
    status = StatusUtil.getMoreSevere(status, validateLibrary());
    return status;
  }

  /**
   * Creates a valid string to inject the extends into the template
   */
  private String getExtendsString() {
    if (superType == null || superType.getElementName() == "") {
      return "";
    }
    return " extends " + superType.getElementName().replace(Extensions.DOT_DART, "");
  }

  /**
   * Creates a valid string to inject the interfaces into the template
   */
  private String getImplementsString() {
    if (interfaceList.length == 0) {
      return "";
    }
    String implementsCode = " implements ";
    for (int i = 0; i != interfaceList.length; ++i) {
      implementsCode += interfaceList[i].getElementName();
      if (i != interfaceList.length - 1) {
        implementsCode += ", ";
      }
    }
    return implementsCode;
  }

  private IStatus validateLibrary() {
    mustBuildLibrary = false;
    if (library == null || library.equals("")) {
      return new Status(
          IStatus.WARNING,
          DartCore.PLUGIN_ID,
          "You have not selected a Library to add this new Class to");
    }
    IResource file = root.findMember(new Path(this.library));
    DartElement libElement = DartCore.create(file);
    if (libElement == null) {
      mustBuildLibrary = true;
      return new Status(
          IStatus.WARNING,
          DartCore.PLUGIN_ID,
          "The library you have chosen does not exist, if you continue you will create a new Library named: "
              + library);
    }
    return Status.OK_STATUS;
  }
}
