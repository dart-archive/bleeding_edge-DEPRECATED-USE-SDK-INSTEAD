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
 * Instances of <code>DartInterfaceGenerator</code> are used to create a new Dart type in an
 * existing Dart library or application after validating the name of the new class.
 */
public class DartInterfaceGenerator extends DartFileGenerator {
  static {
    DartCore.notYetImplemented();
    // replace the following constants once Dart compiler constants are
    // available
  }

  /**
   * Array of all the interfaces extended by the new interface to be created
   */
  private DartElement[] interfaceList = new DartElement[0];

  /**
   * Library that the new interface will be added to
   */
  private String library;

  private boolean mustBuildLibrary;

  /**
   * Construct a new instance.
   * 
   * @param containerMustExist If <code>true</code> then the container must exist for the
   *          {@link #validateContainer()} method to return <code>true</code>.
   */
  public DartInterfaceGenerator(boolean containerMustExist) {
    super(containerMustExist);
  }

  /**
   * Generate the new type
   * 
   * @param monitor the progress monitor (not <code>null</code>)
   * @throws CoreException
   */
  public void execute(IProgressMonitor monitor) throws CoreException {
    final HashMap<String, String> substitutions = new HashMap<String, String>();
    substitutions.put("implements", createInterfaceString());
    substitutions.put("className", getName());

    SubMonitor subMonitor = SubMonitor.convert(monitor, "Generating Interface", 100);

    // Create the folder if it does not already exist
    createFolderIfNecessary(getContainerPath(), monitor);
    subMonitor.newChild(20);

    execute("generated-dart-interface.txt", getFile(), substitutions, monitor);
    subMonitor.newChild(60);

    if (mustBuildLibrary) {
      DartLibraryGenerator libGen = new DartLibraryGenerator(false);
      Path libPath = new Path(library);
      libGen.setName(libPath.lastSegment());
      if (getFile() != null && getFile() instanceof IResource) {
        List<IResource> sources = new LinkedList<IResource>();
        sources.add(getFile());
        libGen.setSourcesList(sources);
        libGen.setContainerPath(libPath.removeLastSegments(1).toOSString());
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
//        LibraryConfigurationFileImpl library = (LibraryConfigurationFileImpl) DartCore.create(lib);
//        library.addSource(getName() + Extensions.DOT_DART);
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

  public void setInterfaceList(DartElement[] newInterfaceList) {
    this.interfaceList = newInterfaceList;
  }

  public void setLibrary(String library) {
    this.library = library;
  }

  @Override
  public IStatus validate() {
    // add check to DartConventions.validateCompilationUnit(...) as well
    // when it becomes available.
    DartCore.notYetImplemented();
    //TODO(mmay):  if different DartConventions.validateInterfaceName(String name)
    IStatus status = StatusUtil.getMoreSevere(Status.OK_STATUS, validateContainer());
    status = StatusUtil.getMoreSevere(status, DartConventions.validateTypeName(getName()));
    status = StatusUtil.getMoreSevere(status, validateLibrary());
    return status;
  }

  private String createInterfaceString() {
    if (interfaceList.length == 0) {
      return "";
    }
    String rv = "implements ";
    for (int i = 0; i != interfaceList.length; ++i) {
      rv += interfaceList[i].getElementName();
      if (i != interfaceList.length - 1) {
        rv += ", ";
      }
    }
    return rv;
  }

  private IStatus validateLibrary() {
    mustBuildLibrary = false;
    if (library == null || library.equals("")) {
      return new Status(IStatus.WARNING, DartCore.PLUGIN_ID,
          "You have not selected a Library to add this new Interface to");
    }
    IResource file = root.findMember(new Path(this.library));
    DartElement libElement = DartCore.create(file);
    if (libElement == null) {
      mustBuildLibrary = true;
      return new Status(IStatus.WARNING, DartCore.PLUGIN_ID,
          "The library you have chosen does not exist, if you continue"
              + " you will attempt to create a new Library named: " + library);
    }
    return Status.OK_STATUS;
  }
}
