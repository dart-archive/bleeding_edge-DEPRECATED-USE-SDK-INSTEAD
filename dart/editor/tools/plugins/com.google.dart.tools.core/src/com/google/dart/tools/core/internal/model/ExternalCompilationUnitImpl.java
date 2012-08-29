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

import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.internal.util.CharOperation;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.net.URIUtilities;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import java.io.Reader;
import java.net.URI;
import java.util.Map;

/**
 * Representation for Dart source files that exist on disk but are not mapped into the Eclipse
 * workspace.
 */
public class ExternalCompilationUnitImpl extends CompilationUnitImpl {
  private final String relPath;
  private DartSource source;
  private String elementName;

  public ExternalCompilationUnitImpl(DartLibraryImpl library, String relPath) {
    this(library, relPath, null);
  }

  public ExternalCompilationUnitImpl(DartLibraryImpl library, String relPath, DartSource source) {
    super(library, (IFile) null, DefaultWorkingCopyOwner.getInstance());
    this.relPath = relPath;
    this.source = source;
  }

  @Override
  public CompilationUnitImpl cloneCachingContents() {
    return new ExternalCompilationUnitImpl((DartLibraryImpl) getParent(), relPath, source) {
      private char[] cachedContents;

      @Override
      public char[] getContents() {
        if (this.cachedContents == null) {
          this.cachedContents = ExternalCompilationUnitImpl.this.getContents();
        }
        return this.cachedContents;
      }
    };
  }

  @Override
  public boolean exists() {
    DartSource source = getDartSource();
    return source != null && source.exists();
  }

  @Override
  public String getElementName() {
    if (elementName == null) {
      elementName = getElementName0();
    }
    return elementName;
  }

  @Override
  public long getModificationStamp() {
    // TODO(brianwilkerson) Figure out how to tell when the compilation unit
    // has changed.
    return IResource.NULL_STAMP;
  }

  @Override
  public IPath getPath() {
    return URIUtil.toPath(URIUtilities.safelyResolveDartUri(source.getUri()));
  }

  /**
   * Override to return <code>null</code> because external Dart source files have no corresponding
   * resource.
   */
  @Override
  public IResource getResource() {
    return null;
  }

  @Override
  public DartSource getSourceRef() {
    return getDartSource();
  }

  /**
   * Return the {@link URI} to this file.
   */
  public URI getUri() {
    DartSource source = getDartSource();
    if (source == null) {
      return null;
    }
    return source.getUri();
  }

  /**
   * External source files cannot be edited and will always return <code>true</code>.
   */
  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm,
      Map<DartElement, DartElementInfo> newElements, IResource underlyingResource)
      throws DartModelException {
    if (!exists()) {
      return false;
    }
    return super.buildStructure(info, pm, newElements, underlyingResource);
  }

  @Override
  protected String getHandleMementoName() {
    return relPath;
  }

  @Override
  protected void readBuffer(Buffer buffer, boolean isWorkingCopy) throws DartModelException {
    DartSource source = getDartSource();
    if (source == null) {
      buffer.setContents(CharOperation.NO_CHAR);
      return;
    }
    try {
      Reader reader = source.getSourceReader();
      try {
        buffer.setContents(CharStreams.toString(reader));
      } finally {
        Closeables.closeQuietly(reader);
      }
    } catch (Exception e) {
      buffer.setContents(CharOperation.NO_CHAR);
      throw new DartModelException(e, 0);
    }
  }

  /**
   * Override to always return OK because there are no resources associated with external Dart
   * sources.
   */
  @Override
  protected IStatus validateExistence(IResource underlyingResource) {
    // allow opening of external compilation unit
    return DartModelStatusImpl.VERIFIED_OK;
  }

  private DartSource getDartSource() {
    if (source == null) {
      LibrarySource libSrc = ((DartLibraryImpl) getLibrary()).getLibrarySourceFile();
      source = libSrc.getSourceFor(relPath);
    }
    return source;
  }

  private String getElementName0() {
    DartSource source = getDartSource();
    if (source == null) {
      // This can happen if the element does not exist. Normal compilation units have a file, even
      // when that file doesn't exist, but apparently we don't create a non-existent source object.
      return "";
    }
    URI uri = source.getUri();
    String path = uri.getPath();

    // getPath() returns null for "jar:/..." URIs, so get scheme specific part instead
    if (path == null) {
      path = uri.getSchemeSpecificPart();
    }

    return new Path(path).lastSegment();
  }
}
