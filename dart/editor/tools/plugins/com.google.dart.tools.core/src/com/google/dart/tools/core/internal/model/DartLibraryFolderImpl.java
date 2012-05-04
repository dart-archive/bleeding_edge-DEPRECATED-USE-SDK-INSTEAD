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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartLibraryFolderInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibraryFolder;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class <code>DartLibraryFolderImpl</code> implement an object that represents a
 * folder within a library.
 */
public class DartLibraryFolderImpl extends OpenableElementImpl implements DartLibraryFolder,
    CompilationUnitContainer {
  /**
   * The folder being represented by this element.
   */
  private IFolder folder;

  /**
   * Initialize a newly created library folder to be contained in the given library folder.
   * 
   * @param parentFolder the library folder containing this library folder
   * @param folder the folder being represented by this element
   */
  public DartLibraryFolderImpl(DartLibraryFolderImpl parentFolder, IFolder folder) {
    super(parentFolder);
    this.folder = folder;
  }

  /**
   * Initialize a newly created library folder to be contained in the given library.
   * 
   * @param library the library containing this library folder
   * @param folder the folder being represented by this element
   */
  public DartLibraryFolderImpl(DartLibraryImpl library, IFolder folder) {
    super(library);
    this.folder = folder;
  }

  @Override
  public String getElementName() {
    return folder.getName();
  }

  @Override
  public int getElementType() {
    return DART_LIBRARY_FOLDER;
  }

  @Override
  public IResource resource() {
    return folder;
  }

  @Override
  protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm,
      Map<DartElement, DartElementInfo> newElements, IResource underlyingResource)
      throws DartModelException {
    DartLibraryFolderInfo folderInfo = (DartLibraryFolderInfo) info;
    final List<DartElement> children = new ArrayList<DartElement>();
    try {
      folder.accept(new IResourceProxyVisitor() {
        private DefaultWorkingCopyOwner workingCopyOwner = DefaultWorkingCopyOwner.getInstance();

        @Override
        public boolean visit(IResourceProxy proxy) throws CoreException {
          if (proxy.getType() == IResource.FOLDER) {
            // Create a child element for the nested folder even if the nested
            // folder doesn't contain any compilation units (directly or
            // indirectly) so that compilation units can be added to it.
            children.add(new DartLibraryFolderImpl(
                DartLibraryFolderImpl.this,
                (IFolder) proxy.requestResource()));
          } else if (proxy.getType() == IResource.FILE
              && DartCore.isDartLikeFileName(proxy.getName())) {
            DartCore.notYetImplemented();
            // TODO(brianwilkerson) This will include all .dart files, but it
            // probably ought to only include those that are contained in the
            // containing library.
            children.add(new CompilationUnitImpl(
                DartLibraryFolderImpl.this,
                (IFile) proxy.requestResource(),
                workingCopyOwner));
          }
          return false;
        }
      },
          0);
    } catch (CoreException exception) {
      throw new DartModelException(exception);
    }
    folderInfo.setChildren(children.toArray(new DartElementImpl[children.size()]));
    return false;
  }

  @Override
  protected DartElementInfo createElementInfo() {
    return new DartLibraryFolderInfo();
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    switch (token.charAt(0)) {
      case MEMENTO_DELIMITER_LIBRARY_FOLDER:
        if (!tokenizer.hasMoreTokens()) {
          return this;
        }
        String folderName = tokenizer.nextToken();
        IFolder parentFolder = folder.getFolder(folderName);
        DartLibraryFolderImpl folder = new DartLibraryFolderImpl(this, parentFolder);
        return folder.getHandleFromMemento(tokenizer, owner);
      case MEMENTO_DELIMITER_COMPILATION_UNIT:
        if (!tokenizer.hasMoreTokens()) {
          return this;
        }
        String unitPath = tokenizer.nextToken();
        CompilationUnitImpl unit = new CompilationUnitImpl(
            this,
            getResource().getProject().getFile(new Path(unitPath)),
            owner);
        return unit.getHandleFromMemento(tokenizer, owner);
    }
    return null;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return MEMENTO_DELIMITER_LIBRARY_FOLDER;
  }

  @Override
  protected IStatus validateExistence(IResource underlyingResource) {
    if (underlyingResource == null || !underlyingResource.exists()) {
      return newDoesNotExistStatus();
    }
    return DartModelStatusImpl.OK_STATUS;
  }
}
