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
package com.google.dart.tools.core.mock;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import java.net.URI;
import java.util.Map;

public abstract class MockResource implements IResource {
  private IContainer parent;
  private String name;
  private boolean exists;

  public MockResource(IContainer parent, String name) {
    this(parent, name, true);
  }

  public MockResource(IContainer parent, String name, boolean exists) {
    this.parent = parent;
    this.name = name;
    this.exists = exists;
  }

  @Override
  public void accept(IResourceVisitor visitor) throws CoreException {
  }

  @Override
  public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms)
      throws CoreException {
  }

  @Override
  public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
  }

  @Override
  public void clearHistory(IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public boolean contains(ISchedulingRule rule) {
    return this == rule;
  }

  @Override
  public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void copy(IPath destination, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public IMarker createMarker(String type) throws CoreException {
    return null;
  }

  @Override
  public IResourceProxy createProxy() {
    return null;
  }

  @Override
  public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
  }

  @Override
  public boolean exists() {
    return exists;
  }

  @Override
  public IMarker findMarker(long id) throws CoreException {
    return null;
  }

  @Override
  public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth)
      throws CoreException {
    return null;
  }

  @Override
  public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth)
      throws CoreException {
    return 0;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class adapter) {
    return null;
  }

  @Override
  public String getFileExtension() {
    return null;
  }

  @Override
  public IPath getFullPath() {

    IPath path = null;
    if (name != null) {
      path = new Path(name);
    }
    if (parent != null && parent.getFullPath() != null) {
      path = parent.getFullPath().append(path);
    }
    return path;
  }

  @Override
  public long getLocalTimeStamp() {
    return 0;
  }

  @Override
  public IPath getLocation() {
    return ResourcesPlugin.getWorkspace().getRoot().getLocation().append(getFullPath());
  }

  @Override
  public URI getLocationURI() {
    return null;
  }

  @Override
  public IMarker getMarker(long id) {
    return null;
  }

  @Override
  public long getModificationStamp() {
    return 0;
  }

  @Override
  public final String getName() {
    return name;
  }

  @Override
  public IContainer getParent() {
    return parent;
  }

  @Override
  public IPathVariableManager getPathVariableManager() {
    return null;
  }

  @Override
  public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
    return null;
  }

  @Override
  public String getPersistentProperty(QualifiedName key) throws CoreException {
    return null;
  }

  @Override
  public IProject getProject() {
    if (parent == null) {
      return null;
    }
    return parent.getProject();
  }

  @Override
  public IPath getProjectRelativePath() {
    return null;
  }

  @Override
  public IPath getRawLocation() {
    return null;
  }

  @Override
  public URI getRawLocationURI() {
    return null;
  }

  @Override
  public ResourceAttributes getResourceAttributes() {
    return null;
  }

  @Override
  public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
    return null;
  }

  @Override
  public Object getSessionProperty(QualifiedName key) throws CoreException {
    return null;
  }

  @Override
  public IWorkspace getWorkspace() {
    return null;
  }

  @Override
  public boolean isAccessible() {
    return false;
  }

  @Override
  public boolean isConflicting(ISchedulingRule rule) {
    return this == rule;
  }

  @Override
  public boolean isDerived() {
    return false;
  }

  @Override
  public boolean isDerived(int options) {
    return false;
  }

  @Override
  public boolean isHidden() {
    return false;
  }

  @Override
  public boolean isHidden(int options) {
    return false;
  }

  @Override
  public boolean isLinked() {
    return false;
  }

  @Override
  public boolean isLinked(int options) {
    return false;
  }

  @Override
  public boolean isLocal(int depth) {
    return false;
  }

  @Override
  public boolean isPhantom() {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public boolean isSynchronized(int depth) {
    return false;
  }

  @Override
  public boolean isTeamPrivateMember() {
    return false;
  }

  @Override
  public boolean isTeamPrivateMember(int options) {
    return false;
  }

  @Override
  public boolean isVirtual() {
    return false;
  }

  @Override
  public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void move(IPath destination, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void move(IProjectDescription description, boolean force, boolean keepHistory,
      IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void revertModificationStamp(long value) throws CoreException {
  }

  @Override
  public void setDerived(boolean isDerived) throws CoreException {
  }

  @Override
  public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void setHidden(boolean isHidden) throws CoreException {
  }

  @Override
  public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public long setLocalTimeStamp(long value) throws CoreException {
    return 0;
  }

  @Override
  public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
  }

  @Override
  public void setReadOnly(boolean readOnly) {
  }

  @Override
  public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
  }

  @Override
  public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
  }

  @Override
  public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getFullPath() + "]";
  }

  @Override
  public void touch(IProgressMonitor monitor) throws CoreException {
  }
}
