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

package com.google.dart.tools.ui.internal.testing;

import org.eclipse.core.resources.IFile;

/**
 * An element in the Tests view - a test or test group.
 */
public abstract class DartUnitElement {
  private IFile dartFile;
  private DartUnitGroup parent;
  private String name;

  public DartUnitElement(IFile dartFile, String name) {
    this.dartFile = dartFile;
    this.name = name;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof DartUnitElement)) {
      return false;
    }

    if (getClass() != other.getClass()) {
      return false;
    }

    DartUnitElement element = (DartUnitElement) other;

    return getName().equals(element.getName());
  }

  public IFile getDartFile() {
    return dartFile;
  }

  public String getName() {
    return name;
  }

  public DartUnitElement getParent() {
    return parent;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() ^ getName().hashCode();
  }

  @Override
  public String toString() {
    return getName();
  }

  protected void setParent(DartUnitGroup parent) {
    this.parent = parent;
  }

}
