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

package com.google.dart.tools.debug.core.server;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.debug.core.util.DebuggerUtils;
import com.google.dart.tools.debug.core.util.IDartDebugVariable;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * An IVariable implementation for VM debugging.
 */
public class ServerDebugVariable extends ServerDebugElement implements IDartDebugVariable {
  public static interface IValueRetriever {
    public String getDisplayName();

    public List<IVariable> getVariables();
  }

  public static ServerDebugVariable createLibraryVariable(final ServerDebugTarget target,
      final VmIsolate isolate, final int libraryId) {
    return new ServerDebugVariable(target, DebuggerUtils.TOP_LEVEL_NAME, new IValueRetriever() {
      @Override
      public String getDisplayName() {
        return "";
      }

      @Override
      public List<IVariable> getVariables() {
        return createLibraryVariables(target, isolate, libraryId);
      }
    });
  }

  protected static List<IVariable> createLibraryVariables(final ServerDebugTarget target,
      final VmIsolate isolate, int libraryId) {
    final List<IVariable> variables = new ArrayList<IVariable>();

    final CountDownLatch latch = new CountDownLatch(1);

    try {
      target.getConnection().getGlobalVariables(
          isolate,
          libraryId,
          new VmCallback<List<VmVariable>>() {
            @Override
            public void handleResult(VmResult<List<VmVariable>> result) {
              if (!result.isError()) {
                List<VmVariable> globals = result.getResult();

                for (VmVariable variable : globals) {
                  variables.add(new ServerDebugVariable(target, variable));
                }
              }

              latch.countDown();
            }
          });
    } catch (IOException e) {
      latch.countDown();
    }

    try {
      latch.await();
    } catch (InterruptedException e) {

    }

    return variables;
  }

  private VmVariable vmVariable;

  private ServerDebugValue value;
  private String name;

  private boolean isStatic;

  public ServerDebugVariable(IDebugTarget target, String name, IValueRetriever valueRetriever) {
    super(target);

    this.name = name;

    this.value = new ServerDebugValue(target, valueRetriever);
  }

  public ServerDebugVariable(IDebugTarget target, VmVariable vmVariable) {
    super(target);

    this.vmVariable = vmVariable;
    this.value = new ServerDebugValue(target, vmVariable.getValue());

    this.name = vmVariable.getName();
  }

  public DartElement coerceToDartElement() {
    if (isLibraryObject()) {
      return null;
    }

    if (isThisObject()) {
      return null;
    }

    if (isListElement()) {
      return null;
    }

    // TODO(devoncarew): top-level

    // TODO(devoncarew): instance or static vars

    // TODO(devoncarew): params or locals

    return null;
  }

  public String getDisplayName() {
    return getName();
  }

  @Override
  public String getName() {
    // The names of private fields are mangled by the VM.
    // _foo@652376 ==> _foo
    return DebuggerUtils.demanglePrivateName(name);
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return getValue().getReferenceTypeName();
  }

  @Override
  public IValue getValue() {
    return value;
  }

  @Override
  public boolean hasValueChanged() throws DebugException {
    // TODO(devoncarew):

    return false;
  }

  @Override
  public boolean isLibraryObject() {
    return value.isValueRetriever()
        && (DebuggerUtils.LIBRARY_NAME.equals(getName()) || DebuggerUtils.TOP_LEVEL_NAME.equals(getName()));
  }

  public boolean isListValue() {
    return value.isListValue();
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public boolean isThisObject() {
    return "this".equals(getName());
  }

  @Override
  public boolean isThrownException() {
    return vmVariable != null && vmVariable.getIsException();
  }

  @Override
  public void setValue(IValue value) throws DebugException {
    // Not supported.

  }

  @Override
  public void setValue(String expression) throws DebugException {
    // Not supported.

  }

  @Override
  public boolean supportsValueModification() {
    return false;
  }

  @Override
  public boolean verifyValue(IValue value) throws DebugException {
    // Not supported.

    return false;
  }

  @Override
  public boolean verifyValue(String expression) throws DebugException {
    // Not supported.

    return false;
  }

  protected void setIsStatic(boolean value) {
    this.isStatic = value;
  }

  private boolean isListElement() {
    return getName().startsWith("[");
  }

}
