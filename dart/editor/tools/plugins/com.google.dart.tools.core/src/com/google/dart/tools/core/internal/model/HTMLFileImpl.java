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

import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.HTMLFileInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.internal.util.LibraryReferenceFinder;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.core.utilities.resource.IFileUtilities;
import com.google.dart.tools.core.utilities.resource.IResourceUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class <code>HTMLFileImpl</code> implement an HTML file.
 */
public class HTMLFileImpl extends OpenableElementImpl implements HTMLFile {
  /**
   * The file being represented by this element.
   */
  private IFile file;

  /**
   * Initialize a newly created HTML file element to be a child of the given project.
   * 
   * @param project the project containing the element
   * @param file the file being represented by the element
   */
  public HTMLFileImpl(DartLibraryImpl library, IFile file) {
    super(library);
    this.file = file;
  }

  @Override
  public String getElementName() {
    return file.getName();
  }

  @Override
  public int getElementType() {
    return HTML_FILE;
  }

  @Override
  public DartLibrary[] getReferencedLibraries() throws DartModelException {
    return ((HTMLFileInfo) getElementInfo()).getReferencedLibraries();
  }

  @Override
  public IResource getUnderlyingResource() throws DartModelException {
    return file;
  }

  @Override
  public IResource resource() {
    return file;
  }

  @Override
  protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm,
      Map<DartElement, DartElementInfo> newElements, IResource underlyingResource)
      throws DartModelException {
    // TODO : remove referenced libraries. They are no longer needed, as html files are now part of 
    // library. 
    HTMLFileInfo fileInfo = (HTMLFileInfo) info;
    fileInfo.setChildren(DartElementImpl.EMPTY_ARRAY);
    try {
      List<String> libraryNames = LibraryReferenceFinder.findInHTML(IFileUtilities.getContents(file));
      List<String> libraryPaths = IResourceUtilities.getResolvedFilePaths(
          getUnderlyingResource(),
          libraryNames);
      List<DartLibrary> referencedLibraries = new ArrayList<DartLibrary>(libraryNames.size());
      List<DartLibrary> libraries = DartModelManager.getInstance().getDartModel().getDartLibraries();
      for (DartLibrary library : libraries) {
        if (library.getDartProject().getProject().equals(file.getProject())) {
          String elementName = library.getElementName();
          for (String libraryPath : libraryPaths) {
            if (elementName.equals(libraryPath) || elementName.contains(libraryPath)) {
              referencedLibraries.add(library);
              break;
            }
          }
        }
      }

      fileInfo.setReferencedLibraries(referencedLibraries.toArray(new DartLibrary[referencedLibraries.size()]));
      fileInfo.setIsStructureKnown(true);
      return true;
    } catch (Exception exception) {
      fileInfo.setReferencedLibraries(DartLibrary.EMPTY_LIBRARY_ARRAY);
      fileInfo.setIsStructureKnown(false);
      return false;
    }
  }

  @Override
  protected DartElementInfo createElementInfo() {
    return new HTMLFileInfo();
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    // HTML files do not have any children
    return this;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return MEMENTO_DELIMITER_HTML_FILE;
  }

  @Override
  protected String getHandleMementoName() {
    // Because the HTML file can be anywhere relative to the project that contains it we need to
    // specify the full path.
    return file.getProjectRelativePath().toPortableString();
  }

  @Override
  protected IStatus validateExistence(IResource underlyingResource) {
    if (!underlyingResource.exists()) {
      return newDoesNotExistStatus();
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }
}
