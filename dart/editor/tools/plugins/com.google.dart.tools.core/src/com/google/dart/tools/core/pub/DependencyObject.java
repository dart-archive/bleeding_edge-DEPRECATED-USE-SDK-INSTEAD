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
package com.google.dart.tools.core.pub;


/**
 * Object representing a dependency package
 */
public class DependencyObject {

  private PubspecModel model;

  private String name;
  private String version = "any";
  private String path;
  private String ref;
  private boolean isGit = false;

  public DependencyObject(String name) {
    this.name = name;
  }

  public String getGitRef() {
    return ref;
  }

  public PubspecModel getModel() {
    return model;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public String getVersion() {
    return version;
  }

  public boolean isGitDependency() {
    return isGit;
  }

  public void setGitDependency(boolean isGit) {
    this.isGit = isGit;
  }

  public void setGitRef(String gitRef) {
    this.ref = gitRef;
  }

  public void setModel(PubspecModel model) {
    this.model = model;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return getName();
  }
}
