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
package com.google.dart.tools.core.mock;

import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.CallList.Call;
import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.net.URI;

@SuppressWarnings("deprecation")
public class MockFile extends MockResource implements IFile {
  private String contents = "";

  public MockFile(IContainer parent) {
    super(parent, null);
  }

  public MockFile(IContainer parent, String name) {
    super(parent, name);
  }

  public MockFile(IContainer parent, String name, boolean exists) {
    super(parent, name, exists);
  }

  public MockFile(IContainer parent, String name, String contents) {
    super(parent, name);
    this.contents = contents;
  }

  @Override
  public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
    visitor.visit(new MockProxy(this));
  }

  public void accept(IResourceProxyVisitor visitor, int depth, int memberFlags)
      throws CoreException {
    accept(visitor, memberFlags);
  }

  @Override
  public void accept(IResourceVisitor visitor) throws CoreException {
    visitor.visit(this);
  }

  @Override
  public void appendContents(InputStream source, boolean force, boolean keepHistory,
      IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void appendContents(InputStream source, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
  }

  /**
   * Assert that {@link #deleteMarkers(String, boolean, int)} was called.
   */
  public void assertMarkersDeleted() {
    getMarkerCallList().assertOneOrMoreCalls(newDeleteMarkerCall());
  }

  /**
   * Assert that {@link #deleteMarkers(String, boolean, int)} was NOT called.
   */
  public void assertMarkersNotDeleted() {
    getMarkerCallList().assertNoCall(newDeleteMarkerCall());
  }

  /**
   * Answer a source representing this file
   * 
   * @return a source, not {@code null}
   */
  public Source asSource() {
    return new FileBasedSource(toFile()) {
      @Override
      public boolean equals(Object object) {
        return object instanceof Source && getFullName().equals(((Source) object).getFullName());
      }

      @Override
      public boolean exists() {
        return MockFile.this.exists();
      }

      @Override
      public TimestampedData<CharSequence> getContents() throws Exception {
        return new TimestampedData<CharSequence>(
            getModificationStamp(),
            MockFile.this.getContentsAsString());
      }
    };
  }

  @Override
  public void create(InputStream source, boolean force, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void create(InputStream source, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void createLink(URI location, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public String getCharset() throws CoreException {
    return "UTF-8";
  }

  @Override
  public String getCharset(boolean checkImplicit) throws CoreException {
    return null;
  }

  @Override
  public String getCharsetFor(Reader reader) throws CoreException {
    return null;
  }

  @Override
  public IContentDescription getContentDescription() throws CoreException {
    return null;
  }

  @Override
  public InputStream getContents() throws CoreException {
    return getContents(true);
  }

  @Override
  public InputStream getContents(boolean force) throws CoreException {
    return new StringBufferInputStream(contents);
  }

  public String getContentsAsString() {
    return contents;
  }

  @Override
  public int getEncoding() throws CoreException {
    return 0;
  }

  @Override
  public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
    return null;
  }

  @Override
  public int getType() {
    return IResource.FILE;
  }

  @Override
  public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void setCharset(String newCharset) throws CoreException {
  }

  @Override
  public void setCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void setContents(IFileState source, boolean force, boolean keepHistory,
      IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void setContents(IFileState source, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void setContents(InputStream source, boolean force, boolean keepHistory,
      IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
  }

  public void setContents(String contents) {
    this.contents = contents;
  }

  private Call newDeleteMarkerCall() {
    return new Call(
        this,
        MockFile.DELETE_MARKERS,
        DartCore.DART_PROBLEM_MARKER_TYPE,
        true,
        IResource.DEPTH_ZERO);
  }
}
