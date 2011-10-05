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
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.net.URI;
import java.net.URISyntaxException;

public class MockProjectDescription implements IProjectDescription {
  private String projectName;
  private String comment;
  private ICommand[] buildSpec = new ICommand[0];
  private IProject[] dynamicReferences = new IProject[0];
  private URI location;
  private String[] natureIds = new String[0];
  private IProject[] referencedProjects = new IProject[0];

  // Eclipse 3.7 specific method
  public IBuildConfiguration[] getBuildConfigReferences(String configName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ICommand[] getBuildSpec() {
    return buildSpec;
  }

  @Override
  public String getComment() {
    return comment;
  }

  @Override
  public IProject[] getDynamicReferences() {
    return dynamicReferences;
  }

  @Override
  public IPath getLocation() {
    return new Path(location.getSchemeSpecificPart());
  }

  @Override
  public URI getLocationURI() {
    return location;
  }

  @Override
  public String getName() {
    return projectName;
  }

  @Override
  public String[] getNatureIds() {
    return natureIds;
  }

  @Override
  public IProject[] getReferencedProjects() {
    return referencedProjects;
  }

  @Override
  public boolean hasNature(String natureId) {
    for (String nature : natureIds) {
      if (nature.equals(natureId)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public ICommand newCommand() {
    return new MockCommand();
  }

  // Eclipse 3.7 specific method
  public void setActiveBuildConfig(String configName) {
    // TODO Auto-generated method stub

  }

  // Eclipse 3.7 specific method
  public void setBuildConfigReferences(String configName, IBuildConfiguration[] references) {
    // TODO Auto-generated method stub

  }

  // Eclipse 3.7 specific method
  public void setBuildConfigs(String[] configNames) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setBuildSpec(ICommand[] buildSpec) {
    this.buildSpec = buildSpec;
  }

  @Override
  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public void setDynamicReferences(IProject[] projects) {
    dynamicReferences = projects;
  }

  @Override
  public void setLocation(IPath location) {
    try {
      this.location = new URI("file", location.toString(), null);
    } catch (URISyntaxException exception) {
      // This should never happen
    }
  }

  @Override
  public void setLocationURI(URI location) {
    this.location = location;
  }

  @Override
  public void setName(String projectName) {
    this.projectName = projectName;
  }

  @Override
  public void setNatureIds(String[] natures) {
    natureIds = natures;
  }

  @Override
  public void setReferencedProjects(IProject[] projects) {
    referencedProjects = projects;
  }
}
