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

  public enum Type {
    HOSTED,
    GIT,
    PATH;
  }

  private PubspecModel model;

  private String name;
  private String version = PubspecConstants.ANY;
  private String path;
  private String ref;
  private Type type = Type.HOSTED;
  private boolean forDevelopment = false;

  public DependencyObject(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DependencyObject other = (DependencyObject) obj;
    if (forDevelopment != other.forDevelopment) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (path == null) {
      if (other.path != null) {
        return false;
      }
    } else if (!path.equals(other.path)) {
      return false;
    }
    if (ref == null) {
      if (other.ref != null) {
        return false;
      }
    } else if (!ref.equals(other.ref)) {
      return false;
    }
    if (type != other.type) {
      return false;
    }
    if (version == null) {
      if (other.version != null) {
        return false;
      }
    } else if (!version.equals(other.version)) {
      return false;
    }
    return true;
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

  public Type getType() {
    return type;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (forDevelopment ? 1231 : 1237);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    result = prime * result + ((ref == null) ? 0 : ref.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    return result;
  }

  public boolean isForDevelopment() {
    return forDevelopment;
  }

  public void setForDevelopment(boolean forDevelopment) {
    this.forDevelopment = forDevelopment;
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

  public void setType(Type type) {
    this.type = type;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return getName();
  }

}
