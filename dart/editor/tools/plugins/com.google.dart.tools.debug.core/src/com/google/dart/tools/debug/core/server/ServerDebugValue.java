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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * An IValue implementation for VM debugging.
 */
public class ServerDebugValue extends ServerDebugElement implements IValue {
  private VmValue value;
  private List<ServerDebugVariable> fields;

  public ServerDebugValue(IDebugTarget target, VmValue value) {
    super(target);

    this.value = value;
  }

  public String getDisplayString() {
    fillInFields();

    if (value.isString()) {
      return "\"" + getValueString() + "\"";
    } else if (value.isObject()) {
      // TODO(devoncarew): show the type of object here

      return getValueString();
    } else {
      return getValueString();
    }
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return value.getKind();
  }

  @Override
  public String getValueString() {
    return value.getText();
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    fillInFields();

    return fields.toArray(new ServerDebugVariable[fields.size()]);
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return getVariables().length > 0;
  }

  @Override
  public boolean isAllocated() throws DebugException {
    return true;
  }

  public boolean isListValue() {
    // TODO(devoncarew): we need more plumbing in the wire protocol

    return false;
  }

  protected void fillInFieldsAsync(final CountDownLatch latch) {
    try {
      getConnection().getObjectProperties(value.getObjectId(), new VmCallback<VmObject>() {
        @Override
        public void handleResult(VmResult<VmObject> result) {
          fields = convert(result);

          latch.countDown();
        }
      });

      latch.await();
    } catch (Exception e) {
      fields = Collections.emptyList();

      latch.countDown();
    }
  }

  synchronized void fillInFields() {
    if (value.isObject()) {
      if (fields == null) {
        CountDownLatch latch = new CountDownLatch(1);;

        fillInFieldsAsync(latch);

        try {
          latch.await();
        } catch (InterruptedException e) {

        }
      }
    } else {
      if (fields == null) {
        fields = Collections.emptyList();
      }
    }
  }

  private List<ServerDebugVariable> convert(VmResult<VmObject> result) {
    if (result.isError()) {
      return Collections.emptyList();
    }

    List<ServerDebugVariable> vars = new ArrayList<ServerDebugVariable>();

    VmObject vmObject = result.getResult();

    // TODO(devoncarew): figure out a good strategy for populating these objects
//    VmClass vmClass = vmObject.getClassObject();
//
//    // Add static fields.
//    if (vmClass != null) {
//      for (VmVariable vmVariable : vmClass.getFields()) {
//        vars.add(new ServerDebugVariable(getTarget(), vmVariable));
//      }
//    }

    // Add instance fields.
    for (VmVariable vmVariable : vmObject.getFields()) {
      vars.add(new ServerDebugVariable(getTarget(), vmVariable));
    }

    return vars;
  }

}
