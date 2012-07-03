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
public class ServerDebugVariable extends ServerDebugElement implements IVariable {
  public static interface IValueRetriever {
    public String getDisplayName();

    public List<IVariable> getVariables();
  }

  public static ServerDebugVariable createLibraryVariable(final ServerDebugTarget target,
      final int libraryId) {
    return new ServerDebugVariable(target, "library", new IValueRetriever() {
      private String name = "";

      @Override
      public String getDisplayName() {
        return name;
      }

      @Override
      public List<IVariable> getVariables() {
        String[] nameResult = new String[1];

        List<IVariable> result = createLibraryVariables(target, libraryId, nameResult);

        name = nameResult[0];

        return result;
      }
    });
  }

  protected static List<IVariable> createLibraryVariables(final ServerDebugTarget target,
      int libraryId, final String[] nameResult) {
    final List<IVariable> variables = new ArrayList<IVariable>();

    final CountDownLatch latch = new CountDownLatch(1);

    try {
      target.getConnection().getLibraryProperties(libraryId, new VmCallback<VmLibrary>() {
        @Override
        public void handleResult(VmResult<VmLibrary> result) {
          if (!result.isError()) {
            VmLibrary library = result.getResult();

            nameResult[0] = library.getUrl();

            for (VmVariable variable : library.getGlobals()) {
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

  public String getDisplayName() {
    // The names of private fields are mangled by the VM.
    // _foo@652376 ==> _foo
    String name = getName();

    if (name.indexOf('@') != -1) {
      name = name.substring(0, name.indexOf('@'));
    }

    return name;
  }

  @Override
  public String getName() {
    return name;
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

  public boolean isLibraryObject() {
    return value.isValueRetriever() && "library".equals(getName());
  }

  public boolean isListValue() {
    return value.isListValue();
  }

  public boolean isThisObject() {
    return "this".equals(getName());
  }

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

}
