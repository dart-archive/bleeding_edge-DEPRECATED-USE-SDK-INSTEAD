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

  /**
   * This IValueRetriever implementation retrieves all the top-level variables visible from the
   * given library.
   */
  static class LibraryTopLevelRetriever implements IValueRetriever {
    private final ServerDebugTarget target;
    private final int libraryId;
    private final VmIsolate isolate;

    LibraryTopLevelRetriever(ServerDebugTarget target, int libraryId, VmIsolate isolate) {
      this.target = target;
      this.libraryId = libraryId;
      this.isolate = isolate;
    }

    @Override
    public String getDisplayName() {
      return "";
    }

    @Override
    public List<IVariable> getVariables() {
      return createVisibleLibraryVariables(target, isolate, libraryId);
    }

    @Override
    public boolean hasVariables() {
      return true;
    }
  }

  public static ServerDebugVariable createLibraryVariable(ServerDebugTarget target,
      VmIsolate isolate, int libraryId) {
    ServerDebugVariable variable = new ServerDebugVariable(
        target,
        "globals",
        new LibraryTopLevelRetriever(target, libraryId, isolate));
    variable.setIsLibraryObject(true);
    return variable;
  }

  protected static List<IVariable> createVisibleLibraryVariables(final ServerDebugTarget target,
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

  private boolean isLibraryObject;

  public ServerDebugVariable(IDebugTarget target, String name, IValueRetriever valueRetriever) {
    super(target);

    this.name = name;
    this.value = new ServerDebugValue(target, valueRetriever);
  }

  public ServerDebugVariable(IDebugTarget target, VmVariable vmVariable) {
    super(target);

    this.vmVariable = vmVariable;
    this.value = ServerDebugValue.createValue(target, vmVariable.getValue());
    this.name = vmVariable.getName();
  }

  public String getDisplayName() throws DebugException {
    return getName();
  }

  @Override
  public String getName() throws DebugException {
    try {
      // The names of private fields are mangled by the VM.
      // _foo@652376 ==> _foo
      return DebuggerUtils.demangleVmName(name);
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
    return isLibraryObject;
  }

  public boolean isListValue() {
    return value.isListValue();
  }

  @Override
  public boolean isLocal() {
    return vmVariable.isLocal();
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public boolean isThisObject() {
    return "this".equals(name);
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
  public String toString() {
    return name;
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

  protected void setIsLibraryObject(boolean value) {
    this.isLibraryObject = value;
  }

  protected void setIsStatic(boolean value) {
    this.isStatic = value;
  }

  private boolean isListElement() {
    return name.startsWith("[");
  }

}
