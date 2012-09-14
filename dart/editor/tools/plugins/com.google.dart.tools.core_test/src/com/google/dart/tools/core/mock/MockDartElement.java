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

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementVisitor;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.OpenableElement;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import java.util.List;

public class MockDartElement implements DartElement {
  private String elementName;

  public MockDartElement(String elementName) {
    this.elementName = elementName;
  }

  @Override
  public void accept(DartElementVisitor visitor) throws DartModelException {
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
  public <E extends DartElement> E getAncestor(Class<E> ancestorClass) {
    return null;
  }

  @Override
  public DartElement[] getChildren() throws DartModelException {
    return null;
  }

  @Override
  public <E extends DartElement> List<E> getChildrenOfType(Class<E> elementClass)
      throws DartModelException {
    return null;
  }

  @Override
  public IResource getCorrespondingResource() throws DartModelException {
    return null;
  }

  @Override
  public DartModel getDartModel() {
    return null;
  }

  @Override
  public DartProject getDartProject() {
    return null;
  }

  @Override
  public String getElementName() {
    return elementName;
  }

  @Override
  public int getElementType() {
    return 0;
  }

  @Override
  public String getHandleIdentifier() {
    return null;
  }

  @Override
  public OpenableElement getOpenable() {
    return null;
  }

  @Override
  public DartElement getParent() {
    return null;
  }

  @Override
  public IPath getPath() {
    return null;
  }

  @Override
  public DartElement getPrimaryElement() {
    return null;
  }

  @Override
  public IResource getResource() {
    return null;
  }

  @Override
  public ISchedulingRule getSchedulingRule() {
    return null;
  }

  @Override
  public IResource getUnderlyingResource() throws DartModelException {
    return null;
  }

  @Override
  public boolean isInSdk() {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public boolean isStructureKnown() throws DartModelException {
    return false;
  }
}
