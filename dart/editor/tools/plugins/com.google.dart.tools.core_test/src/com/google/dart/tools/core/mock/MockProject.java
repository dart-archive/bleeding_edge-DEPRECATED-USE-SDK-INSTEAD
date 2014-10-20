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

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentTypeMatcher;

import java.net.URI;
import java.util.Map;

@SuppressWarnings("deprecation")
public class MockProject extends MockContainer implements IProject {
  private IProjectDescription description;

  private IPath location;

  public MockProject() {
    this(null);
  }

  public MockProject(MockWorkspaceRoot root, String name) {
    super(root, name);
  }

  public MockProject(MockWorkspaceRoot root, String name, boolean exists) {
    super(root, name, exists);
  }

  public MockProject(String name) {
    super(null, name);
  }

  // Eclipse 3.7 specific method
  @Override
  public void build(IBuildConfiguration config, int kind, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void build(int kind, IProgressMonitor monitor) throws CoreException {
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void build(int kind, String builderName, Map args, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void close(IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void create(IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void create(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void create(IProjectDescription description, IProgressMonitor monitor)
      throws CoreException {
    if (description != null) {
      IPath location = description.getLocation();
      if (location != null) {
        this.location = location;
      }
    }
    ((MockContainer) getParent()).add(this);
  }

  @Override
  public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor)
      throws CoreException {
  }

  // Eclipse 3.7 specific method
  @Override
  public IBuildConfiguration getActiveBuildConfig() throws CoreException {
    return null;
  }

  // Eclipse 3.7 specific method
  @Override
  public IBuildConfiguration getBuildConfig(String configName) throws CoreException {
    return null;
  }

  // Eclipse 3.7 specific method
  @Override
  public IBuildConfiguration[] getBuildConfigs() throws CoreException {
    return null;
  }

  @Override
  public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
    return null;
  }

  @Override
  public IProjectDescription getDescription() throws CoreException {
    if (description == null) {
      description = new MockProjectDescription();
      description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
    }
    return description;
  }

  @Override
  public IFile getFile(String name) {
    return getFile(new Path(name));
  }

  @Override
  public IFolder getFolder(String name) {
    return getFolder(new Path(name));
  }

  @Override
  public IPath getLocation() {
    if (location != null) {
      return location;
    }
    if (getParent() == null) {
      return ResourcesPlugin.getWorkspace().getRoot().getLocation().append(getName());
    }
    return super.getLocation();
  }

  @Override
  public IProjectNature getNature(String natureId) throws CoreException {
    return null;
  }

  @Override
  public IPath getPluginWorkingLocation(IPluginDescriptor plugin) {
    return null;
  }

  @Override
  public IProject getProject() {
    return this;
  }

  // Eclipse 3.7 specific method
  @Override
  public IBuildConfiguration[] getReferencedBuildConfigs(String configName, boolean includeMissing)
      throws CoreException {
    return null;
  }

  @Override
  public IProject[] getReferencedProjects() throws CoreException {
    return null;
  }

  @Override
  public IProject[] getReferencingProjects() {
    return null;
  }

  @Override
  public int getType() {
    return IResource.PROJECT;
  }

  @Override
  public IPath getWorkingLocation(String id) {
    return null;
  }

  // Eclipse 3.7 specific method
  @Override
  public boolean hasBuildConfig(String configName) throws CoreException {
    return false;
  }

  @Override
  public boolean hasNature(String natureId) throws CoreException {
    return getDescription().hasNature(natureId);
  }

  @Override
  public boolean isNatureEnabled(String natureId) throws CoreException {
    return false;
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public void loadSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void move(IProjectDescription description, boolean force, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void open(int updateFlags, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void open(IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void saveSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor)
      throws CoreException {
  }

  @Override
  public void setDescription(IProjectDescription description, int updateFlags,
      IProgressMonitor monitor) throws CoreException {
    this.description = description;
  }

  @Override
  public void setDescription(IProjectDescription description, IProgressMonitor monitor)
      throws CoreException {
    this.description = description;
  }

  public void setLocation(IPath location) {
    this.location = location;
  }
}
