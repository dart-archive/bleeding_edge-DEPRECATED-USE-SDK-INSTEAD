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

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentTypeMatcher;

import java.net.URI;
import java.util.Map;

@SuppressWarnings("deprecation")
public class MockProject extends MockContainer implements IProject {
  private IProjectDescription description;

  public MockProject() {
    this(null);
  }

  public MockProject(String name) {
    super(null, name);
  }

  // Eclipse 3.7 specific method
  @Override
  public void build(IBuildConfiguration config, int kind, IProgressMonitor monitor)
      throws CoreException {
    // TODO Auto-generated method stub

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
  }

  @Override
  public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor)
      throws CoreException {
  }

  // Eclipse 3.7 specific method
  @Override
  public IBuildConfiguration getActiveBuildConfig() throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  // Eclipse 3.7 specific method
  @Override
  public IBuildConfiguration getBuildConfig(String configName) throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  // Eclipse 3.7 specific method
  @Override
  public IBuildConfiguration[] getBuildConfigs() throws CoreException {
    // TODO Auto-generated method stub
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
    }
    return description;
  }

  @Override
  public IFile getFile(String name) {
    return null;
  }

  @Override
  public IFolder getFolder(String name) {
    return null;
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
    // TODO Auto-generated method stub
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
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean hasNature(String natureId) throws CoreException {
    return false;
  }

  @Override
  public boolean isNatureEnabled(String natureId) throws CoreException {
    return false;
  }

  @Override
  public boolean isOpen() {
    return false;
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
}
