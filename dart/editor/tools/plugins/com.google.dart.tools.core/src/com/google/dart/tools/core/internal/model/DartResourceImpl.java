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

import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartResourceInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartResource;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import java.net.URI;
import java.util.Map;

/**
 * Instances of the class <code>DartResourceImpl</code> implement a file contained within a project
 * that is included because of a #resource directive.
 */
public class DartResourceImpl extends OpenableElementImpl implements DartResource {
  /**
   * The file represented by this element, or <code>null</code> if the resource is included in a
   * library that is not open.
   */
  private IFile file;

  /**
   * The URI of the resource represented by this element.
   */
  private URI uri;

  /**
   * Initialize a newly created resource element to be a child of the given library.
   * 
   * @param library the library containing the element
   * @param file the file represented by the element
   */
  protected DartResourceImpl(DartLibraryImpl library, IFile file) {
    super(library);
    this.file = file;
    this.uri = file.getLocationURI();
  }

  /**
   * Initialize a newly created resource element to be a child of the given library.
   * 
   * @param library the library containing the element
   * @param uri the URI represented by the element
   */
  protected DartResourceImpl(DartLibraryImpl library, URI uri) {
    super(library);
    this.file = getResource(uri);
    this.uri = uri;
  }

  @Override
  public String getElementName() {
    if (file == null) {
      return new Path(uri.getPath()).lastSegment();
    }
    return file.getName();
  }

  @Override
  public int getElementType() {
    return DART_RESOURCE;
  }

  @Override
  public IResource getUnderlyingResource() throws DartModelException {
    return file;
  }

  @Override
  public URI getUri() {
    return uri;
  }

  @Override
  public IResource resource() {
    return file;
  }

  @Override
  protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm,
      Map<DartElement, DartElementInfo> newElements, IResource underlyingResource)
      throws DartModelException {
    // There is no structure to build at this point.
    return true;
  }

  @Override
  protected DartElementInfo createElementInfo() {
    return new DartResourceInfo();
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    // Resources do not have any children.
    return this;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return MEMENTO_DELIMITER_VARIABLE;
  }

  @Override
  protected String getHandleMementoName() {
    return uri.toString();
  }

  @Override
  protected IStatus validateExistence(IResource underlyingResource) {
    if (underlyingResource == null || !underlyingResource.exists()) {
      return newDoesNotExistStatus();
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  /**
   * Return the resource associated with the given URI, or <code>null</code> if the URI does not
   * correspond to an existing resource.
   * 
   * @param uri the URI representing the resource to be returned
   * @return the resource associated with the given URI
   */
  private IFile getResource(URI uri) {
    try {
      IFile[] resourceFiles = ResourceUtil.getResources(uri);
      if (resourceFiles != null && resourceFiles.length == 1) {
        IFile resource = resourceFiles[0];
        if (resource.exists()) {
          return resource;
        }
      }
    } catch (Exception exception) {
      return null;
    }
    return null;
  }
}
