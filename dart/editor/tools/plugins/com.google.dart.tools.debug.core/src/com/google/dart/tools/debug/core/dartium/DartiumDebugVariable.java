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
package com.google.dart.tools.debug.core.dartium;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.util.DebuggerUtils;
import com.google.dart.tools.debug.core.util.IDartDebugVariable;
import com.google.dart.tools.debug.core.webkit.WebkitPropertyDescriptor;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * The IVariable implementation of the Dartium Debug Element.
 */
public class DartiumDebugVariable extends DartiumDebugElement implements IDartDebugVariable {
  private WebkitPropertyDescriptor descriptor;

  private DartiumDebugVariable parent;
  private DartiumDebugValue value;

  private boolean isSpecialObject;
  private boolean isLocal;
  private boolean isStatic;
  private boolean isLibraryObject;

  /**
   * Create a new Dartium Debug Variable
   * 
   * @param target
   * @param descriptor
   */
  public DartiumDebugVariable(DartiumDebugTarget target, WebkitPropertyDescriptor descriptor) {
    this(target, descriptor, false);
  }

  /**
   * Create a new Dartium Debug Variable
   * 
   * @param target
   * @param descriptor
   * @param isThisObject
   */
  public DartiumDebugVariable(DartiumDebugTarget target, WebkitPropertyDescriptor descriptor,
      boolean isSpecialObject) {
    super(target);

    this.descriptor = descriptor;
    this.isSpecialObject = isSpecialObject;
  }

  public DartElement coerceToDartElement() {
    // TODO(devoncarew): implement this

    return null;
  }

  /**
   * @return a user-consumable string for the variable name
   */
  public String getDisplayName() throws DebugException {
    if (isListMember()) {
      return "[" + getName() + "]";
    }

    if (isLibraryRef()) {
      return convertLibraryRefName(descriptor.getName());
    }

    // The names of private fields are mangled by the VM.
    // _foo@652376 ==> _foo
    return getName();
  }

  @Override
  public String getName() throws DebugException {
    try {
      return DebuggerUtils.demangleVmName(descriptor.getName());
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    try {
      return getValue().getReferenceTypeName();
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public IValue getValue() throws DebugException {
    try {
      if (value == null) {
        value = DartiumDebugValue.create(getTarget(), this, descriptor.getValue());
      }

      return value;
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public boolean hasValueChanged() throws DebugException {
    // TODO(keertip): check to see if value has changed

    return false;
  }

  @Override
  public boolean isLibraryObject() {
    return isLibraryObject;
  }

  public boolean isListValue() {
    try {
      return ((DartiumDebugValue) getValue()).isListValue();
    } catch (DebugException e) {
      return false;
    }
  }

  @Override
  public boolean isLocal() {
    return isLocal;
  }

  public boolean isPrimitiveValue() {
    try {
      return ((DartiumDebugValue) getValue()).isPrimitive();
    } catch (DebugException e) {
      return false;
    }
  }

  @Override
  public boolean isStatic() {
    return isStatic || "[[class]]".equals(descriptor.getName());
  }

  @Override
  public boolean isThisObject() {
    return isSpecialObject && descriptor.getName().equals("this");
  }

  @Override
  public boolean isThrownException() {
    return isSpecialObject && descriptor.getName().equals("exception");
  }

  @Override
  public void setValue(IValue value) throws DebugException {
    setValue(value.getValueString());
  }

  @Override
  public void setValue(String expression) throws DebugException {
    if (DartDebugCorePlugin.VM_SUPPORTS_VALUE_MODIFICATION) {
      // TODO(devoncarew):

      System.out.println("change: " + expression);
    }
  }

  @Override
  public boolean supportsValueModification() {
    if (DartDebugCorePlugin.VM_SUPPORTS_VALUE_MODIFICATION) {
      return descriptor.isWritable() && descriptor.getValue().isPrimitive();
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return descriptor.toString();
  }

  @Override
  public boolean verifyValue(IValue value) throws DebugException {
    return verifyValue(value.getValueString());
  }

  @Override
  public boolean verifyValue(String expression) throws DebugException {
    // TODO(devoncarew): do verification for numbers

    return true;
  }

  protected boolean isClassDescriptor() {
    return descriptor.isClassDescriptor();
  }

  protected void setIsLibraryObject(boolean value) {
    this.isLibraryObject = value;
  }

  protected void setIsLocal(boolean value) {
    isLocal = value;
  }

  protected void setIsStatic(boolean value) {
    isStatic = value;
  }

  protected void setParent(DartiumDebugVariable parent) {
    this.parent = parent;
  }

  private String convertLibraryRefName(String name) {
    if (name.startsWith("file:") && name.indexOf('/') != -1) {
      return "file:" + name.substring(name.lastIndexOf('/') + 1);
    }

    if (name.startsWith("http:")) {
      try {
        URL url = new URL(name);
        return url.getFile();
      } catch (MalformedURLException e) {
      }
    }

    return name;
  }

  private boolean isLibraryRef() {
    if (descriptor.getValue() == null) {
      return false;
    }

    return descriptor.getValue().isLibraryRef();
  }

  private boolean isListMember() {
    if (parent != null && parent.isListValue()) {
      return true;
    } else {
      return false;
    }
  }
}
