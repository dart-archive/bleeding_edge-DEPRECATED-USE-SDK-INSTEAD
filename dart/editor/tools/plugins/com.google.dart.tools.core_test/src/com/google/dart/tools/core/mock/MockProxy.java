/*
 * Copyright 2012 Dart project authors.
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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;

public class MockProxy implements IResourceProxy {

  private final MockResource resource;

  MockProxy(MockResource resource) {
    this.resource = resource;
  }

  @Override
  public long getModificationStamp() {
    return resource.getModificationStamp();
  }

  @Override
  public String getName() {
    return resource.getName();
  }

  @Override
  public Object getSessionProperty(QualifiedName key) {
    try {
      return resource.getSessionProperty(key);
    } catch (CoreException e) {
      return null;
    }
  }

  @Override
  public int getType() {
    return resource.getType();
  }

  @Override
  public boolean isAccessible() {
    return resource.isAccessible();
  }

  @Override
  public boolean isDerived() {
    return resource.isDerived();
  }

  @Override
  public boolean isHidden() {
    return resource.isHidden();
  }

  @Override
  public boolean isLinked() {
    return resource.isLinked();
  }

  @Override
  public boolean isPhantom() {
    return resource.isPhantom();
  }

  @Override
  public boolean isTeamPrivateMember() {
    return resource.isTeamPrivateMember();
  }

  @Override
  public IPath requestFullPath() {
    return resource.getFullPath();
  }

  @Override
  public IResource requestResource() {
    return resource;
  }
}
