/*
 * Copyright (c) 2013, the Dart project authors.
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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.util.Map;

public class MockMarker implements IMarker {

  private final MockResource resource;
  private final String type;

  public MockMarker(MockResource resource, String type) {
    this.resource = resource;
    this.type = type;
  }

  @Override
  public void delete() throws CoreException {
  }

  @Override
  public boolean exists() {
    return false;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class adapter) {
    return null;
  }

  @Override
  public Object getAttribute(String attributeName) throws CoreException {
    return null;
  }

  @Override
  public boolean getAttribute(String attributeName, boolean defaultValue) {
    return false;
  }

  @Override
  public int getAttribute(String attributeName, int defaultValue) {
    return 0;
  }

  @Override
  public String getAttribute(String attributeName, String defaultValue) {
    return null;
  }

  @Override
  public Map<String, Object> getAttributes() throws CoreException {
    return null;
  }

  @Override
  public Object[] getAttributes(String[] attributeNames) throws CoreException {
    return null;
  }

  @Override
  public long getCreationTime() throws CoreException {
    return 0;
  }

  @Override
  public long getId() {
    return 0;
  }

  @Override
  public IResource getResource() {
    return resource;
  }

  @Override
  public String getType() throws CoreException {
    return type;
  }

  @Override
  public boolean isSubtypeOf(String superType) throws CoreException {
    return false;
  }

  @Override
  public void setAttribute(String attributeName, boolean value) throws CoreException {
  }

  @Override
  public void setAttribute(String attributeName, int value) throws CoreException {
  }

  @Override
  public void setAttribute(String attributeName, Object value) throws CoreException {
  }

  @Override
  public void setAttributes(Map<String, ? extends Object> attributes) throws CoreException {
  }

  @Override
  public void setAttributes(String[] attributeNames, Object[] values) throws CoreException {
  }

}
