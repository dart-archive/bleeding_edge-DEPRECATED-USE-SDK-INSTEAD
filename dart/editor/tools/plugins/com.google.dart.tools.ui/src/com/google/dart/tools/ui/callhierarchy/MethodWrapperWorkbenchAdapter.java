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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class MethodWrapperWorkbenchAdapter implements IWorkbenchAdapter {

  private final MethodWrapper methodWrapper;

  public MethodWrapperWorkbenchAdapter(MethodWrapper methodWrapper) {
    Assert.isNotNull(methodWrapper);
    this.methodWrapper = methodWrapper;
  }

  @Override
  public boolean equals(Object obj) {
    //Note: A MethodWrapperWorkbenchAdapter is equal to its MethodWrapper and vice versa (bug 101677).
    return methodWrapper.equals(obj);
  }

  @Override
  public Object[] getChildren(Object o) { //should not be called
    return new Object[0];
  }

  @Override
  public ImageDescriptor getImageDescriptor(Object object) {
    return null;
  }

  @Override
  public String getLabel(Object o) {
    return methodWrapper.getMember().getElementName();
  }

  public MethodWrapper getMethodWrapper() {
    return methodWrapper;
  }

  @Override
  public Object getParent(Object o) {
    return methodWrapper.getParent();
  }

  @Override
  public int hashCode() {
    //Note: A MethodWrapperWorkbenchAdapter is equal to its MethodWrapper and vice versa (bug 101677).
    return methodWrapper.hashCode();
  }
}
