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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import junit.framework.TestCase;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.HashMap;

public class DartElementImplTest extends TestCase {
  private class MockDartElement extends DartElementImpl {
    public MockDartElement(DartElement parent) {
      super(parent);
    }

    @Override
    public DartElement[] getChildren() {
      return DartElement.EMPTY_ARRAY;
    }

    @Override
    public String getElementName() {
      return "";
    }

    @Override
    public int getElementType() {
      return 0;
    }

    // @Override
    // public IResource[] getNonDartResources() throws DartModelException {
    // return NO_RESOURCES;
    // }

    @Override
    public IResource getUnderlyingResource() throws DartModelException {
      return null;
    }

    @Override
    public IResource resource() {
      return null;
    }

    @Override
    protected DartElementInfo createElementInfo() {
      return null;
    }

    @Override
    protected void generateInfos(DartElementInfo info,
        HashMap<DartElement, DartElementInfo> newElements, IProgressMonitor pm)
        throws DartModelException {
    }

    @Override
    protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
        WorkingCopyOwner owner) {
      return null;
    }

    @Override
    protected char getHandleMementoDelimiter() {
      return 0;
    }
  }

  public void test_DartElementImpl_getAncestor_none() {
    DartElementImpl element = new MockDartElement(null);
    DartElement ancestor = element.getAncestor(DartModel.class);
    assertNull(ancestor);
  }

  public void test_DartElementImpl_getAncestor_self() {
    DartElementImpl element = new MockDartElement(null);
    DartElement ancestor = element.getAncestor(DartElement.class);
    assertEquals(element, ancestor);
  }

  public void test_DartElementImpl_getChildren_none() throws DartModelException {
    DartElementImpl element = new MockDartElement(null);
    DartElement[] children = element.getChildren();
    assertNotNull(children);
    assertEquals(0, children.length);
  }

  public void test_DartElementImpl_getDartModel_child() {
    DartModelImpl model = new DartModelImpl();
    DartProjectImpl project = new DartProjectImpl(model, new MockProject("testProject"));
    DartElementImpl element = new MockDartElement(project);
    assertEquals(model, element.getDartModel());
  }

  public void test_DartElementImpl_getDartProject_child() {
    DartProjectImpl project = new DartProjectImpl(new DartModelImpl(), new MockProject(
        "testProject"));
    DartElementImpl element = new MockDartElement(project);
    assertEquals(project, element.getDartProject());
  }

  public void test_DartElementImpl_getDartProject_grandchild() {
    DartProjectImpl project = new DartProjectImpl(new DartModelImpl(), new MockProject(
        "testProject"));
    DartElementImpl element = new MockDartElement(new MockDartElement(project));
    assertEquals(project, element.getDartProject());
  }

  public void test_DartElementImpl_getDartProject_null() {
    DartProjectImpl project = null;
    DartElementImpl element = new MockDartElement(project);
    assertEquals(project, element.getDartProject());
  }

  public void test_DartElementImpl_getHandleIdentifier() {
    DartElementImpl element = new MockDartElement(null);
    assertNotNull(element.getHandleIdentifier());
  }

  public void test_DartElementImpl_getParent_notNull() {
    DartElement parent = new MockDartElement(null);
    DartElementImpl element = new MockDartElement(parent);
    assertEquals(parent, element.getParent());
  }

  public void test_DartElementImpl_getParent_null() {
    DartElement parent = null;
    DartElementImpl element = new MockDartElement(parent);
    assertEquals(parent, element.getParent());
  }
}
