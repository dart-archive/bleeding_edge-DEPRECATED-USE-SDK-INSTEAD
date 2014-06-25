/*
 * Copyright 2013 Dart project authors.
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

import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFilterMatcherDescriptor;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.WorkspaceLock;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

public class MockWorkspace implements IWorkspace {

  private final MockWorkspaceRoot root = new MockWorkspaceRoot(this);
  private IResourceChangeListener resourceChangeListener;

  @Override
  public void addResourceChangeListener(IResourceChangeListener listener) {
    this.resourceChangeListener = listener;
  }

  @Override
  public void addResourceChangeListener(IResourceChangeListener listener, int eventMask) {
  }

  @Override
  public ISavedState addSaveParticipant(Plugin plugin, ISaveParticipant participant)
      throws CoreException {
    return null;
  }

  @Override
  public ISavedState addSaveParticipant(String pluginId, ISaveParticipant participant)
      throws CoreException {
    return null;
  }

  @Override
  public void build(IBuildConfiguration[] buildConfigs, int kind, boolean buildReferences,
      IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void build(int kind, IProgressMonitor monitor) throws CoreException {
  }

  @Override
  public void checkpoint(boolean build) {
  }

  @Override
  public IProject[][] computePrerequisiteOrder(IProject[] projects) {
    return null;
  }

  @Override
  public ProjectOrder computeProjectOrder(IProject[] projects) {
    return null;
  }

  @Override
  public IStatus copy(IResource[] resources, IPath destination, boolean force,
      IProgressMonitor monitor) throws CoreException {
    return null;
  }

  @Override
  public IStatus copy(IResource[] resources, IPath destination, int updateFlags,
      IProgressMonitor monitor) throws CoreException {
    return null;
  }

  @Override
  public IStatus delete(IResource[] resources, boolean force, IProgressMonitor monitor)
      throws CoreException {
    return null;
  }

  @Override
  public IStatus delete(IResource[] resources, int updateFlags, IProgressMonitor monitor)
      throws CoreException {
    return null;
  }

  @Override
  public void deleteMarkers(IMarker[] markers) throws CoreException {
  }

  @Override
  public void forgetSavedTree(String pluginId) {
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class adapter) {
    return null;
  }

  @Override
  public Map<IProject, IProject[]> getDanglingReferences() {
    return null;
  }

  @Override
  public IWorkspaceDescription getDescription() {
    return null;
  }

  @Override
  public IFilterMatcherDescriptor getFilterMatcherDescriptor(String filterMatcherId) {
    return null;
  }

  @Override
  public IFilterMatcherDescriptor[] getFilterMatcherDescriptors() {
    return null;
  }

  @Override
  public IProjectNatureDescriptor getNatureDescriptor(String natureId) {
    return null;
  }

  @Override
  public IProjectNatureDescriptor[] getNatureDescriptors() {
    return null;
  }

  @Override
  public IPathVariableManager getPathVariableManager() {
    return null;
  }

  @Override
  public MockWorkspaceRoot getRoot() {
    return root;
  }

  @Override
  public IResourceRuleFactory getRuleFactory() {
    return null;
  }

  @Override
  public ISynchronizer getSynchronizer() {
    return null;
  }

  @Override
  public boolean isAutoBuilding() {
    return false;
  }

  @Override
  public boolean isTreeLocked() {
    return false;
  }

  @Override
  public IProjectDescription loadProjectDescription(InputStream projectDescriptionFile)
      throws CoreException {
    return null;
  }

  @Override
  public IProjectDescription loadProjectDescription(IPath projectDescriptionFile)
      throws CoreException {
    return null;
  }

  @Override
  public IStatus move(IResource[] resources, IPath destination, boolean force,
      IProgressMonitor monitor) throws CoreException {
    return null;
  }

  @Override
  public IStatus move(IResource[] resources, IPath destination, int updateFlags,
      IProgressMonitor monitor) throws CoreException {
    return null;
  }

  @Override
  public IBuildConfiguration newBuildConfig(String projectName, String configName) {
    return null;
  }

  @SuppressWarnings("restriction")
  @Override
  public IProjectDescription newProjectDescription(String projectName) {
    ProjectDescription description = new ProjectDescription();
    description.setName(projectName);
    return description;
  }

  public void notifyResourceChange(MockDelta delta, int type) {
    if (resourceChangeListener != null) {
      resourceChangeListener.resourceChanged(new MockResourceChangeEvent(delta, type));
    }
  }

  @Override
  public void removeResourceChangeListener(IResourceChangeListener listener) {
  }

  @Override
  public void removeSaveParticipant(Plugin plugin) {
  }

  @Override
  public void removeSaveParticipant(String pluginId) {
  }

  @Override
  public void run(IWorkspaceRunnable action, IProgressMonitor monitor) throws CoreException {
    run(action, root, IWorkspace.AVOID_UPDATE, monitor);
  }

  @Override
  public void run(IWorkspaceRunnable action, ISchedulingRule rule, int flags,
      IProgressMonitor monitor) throws CoreException {
    action.run(monitor);
  }

  @Override
  public IStatus save(boolean full, IProgressMonitor monitor) throws CoreException {
    return null;
  }

  @Override
  public void setDescription(IWorkspaceDescription description) throws CoreException {
  }

  @Override
  public void setWorkspaceLock(WorkspaceLock lock) {
  }

  @Override
  public String[] sortNatureSet(String[] natureIds) {
    return null;
  }

  @Override
  public IStatus validateEdit(IFile[] files, Object context) {
    return null;
  }

  @Override
  public IStatus validateFiltered(IResource resource) {
    return null;
  }

  @Override
  public IStatus validateLinkLocation(IResource resource, IPath location) {
    return null;
  }

  @Override
  public IStatus validateLinkLocationURI(IResource resource, URI location) {
    return null;
  }

  @Override
  public IStatus validateName(String segment, int typeMask) {
    return null;
  }

  @Override
  public IStatus validateNatureSet(String[] natureIds) {
    return null;
  }

  @Override
  public IStatus validatePath(String path, int typeMask) {
    return null;
  }

  @Override
  public IStatus validateProjectLocation(IProject project, IPath location) {
    return null;
  }

  @Override
  public IStatus validateProjectLocationURI(IProject project, URI location) {
    return null;
  }
}
