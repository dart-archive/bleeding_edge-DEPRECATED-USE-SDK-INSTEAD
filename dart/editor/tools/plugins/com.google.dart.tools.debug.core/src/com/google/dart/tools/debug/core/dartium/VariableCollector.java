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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.webkit.WebkitCallback;
import com.google.dart.tools.debug.core.webkit.WebkitPropertyDescriptor;
import com.google.dart.tools.debug.core.webkit.WebkitRemoteObject;
import com.google.dart.tools.debug.core.webkit.WebkitResult;

import org.eclipse.debug.core.model.IVariable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * A helper class to asynchronously collect variable values for the DartiumDebugStackFrame class.
 */
class VariableCollector {

  public static VariableCollector createCollector(DartiumDebugTarget target,
      DartiumDebugVariable variable, List<WebkitRemoteObject> remoteObjects) {
    final VariableCollector collector = new VariableCollector(
        target,
        remoteObjects.size(),
        variable);

    for (WebkitRemoteObject obj : remoteObjects) {
      try {
        target.getConnection().getRuntime().getProperties(
            obj.getObjectId(),
            true,
            new WebkitCallback<WebkitPropertyDescriptor[]>() {
              @Override
              public void handleResult(WebkitResult<WebkitPropertyDescriptor[]> result) {
                collector.collect(result);
              }
            });
      } catch (IOException e) {
        collector.worked();
      }
    }

    return collector;
  }

  public static VariableCollector createCollector(DartiumDebugTarget target,
      List<WebkitRemoteObject> remoteObjects) {
    return createCollector(target, (WebkitRemoteObject) null, remoteObjects);
  }

  public static VariableCollector createCollector(DartiumDebugTarget target,
      WebkitRemoteObject thisObject, List<WebkitRemoteObject> remoteObjects) {
    final VariableCollector collector = new VariableCollector(target, remoteObjects.size());

    if (thisObject != null) {
      collector.createThisVariable(thisObject);
    }

    for (WebkitRemoteObject obj : remoteObjects) {
      try {
        target.getConnection().getRuntime().getProperties(
            obj.getObjectId(),
            true,
            new WebkitCallback<WebkitPropertyDescriptor[]>() {
              @Override
              public void handleResult(WebkitResult<WebkitPropertyDescriptor[]> result) {
                collector.collect(result);
              }
            });
      } catch (IOException e) {
        collector.worked();
      }
    }

    return collector;
  }

  public static VariableCollector empty() {
    return new VariableCollector(null, 0);
  }

  private DartiumDebugTarget target;
  private DartiumDebugVariable parentVariable;

  private CountDownLatch latch;
  private List<IVariable> variables = new ArrayList<IVariable>();

  private VariableCollector(DartiumDebugTarget target, int work) {
    this(target, work, null);
  }

  private VariableCollector(DartiumDebugTarget target, int work, DartiumDebugVariable parentVariable) {
    this.target = target;
    this.parentVariable = parentVariable;

    latch = new CountDownLatch(work);
  }

  public IVariable[] getVariables() throws InterruptedException {
    latch.await();

    return variables.toArray(new IVariable[variables.size()]);
  }

  private void collect(WebkitResult<WebkitPropertyDescriptor[]> results) {
    if (results.isError()) {
      DartDebugCorePlugin.logError("Error retrieving webkit properties: " + results);
    } else {
      for (WebkitPropertyDescriptor descriptor : results.getResult()) {
        if (descriptor.isEnumerable() && !shouldFilter(descriptor)) {
          DartiumDebugVariable variable = new DartiumDebugVariable(target, descriptor);

          if (parentVariable != null) {
            variable.setParent(parentVariable);
          }

          variables.add(variable);
        }
      }
    }

    latch.countDown();
  }

  private void createThisVariable(WebkitRemoteObject thisObject) {
    variables.add(new DartiumDebugVariable(
        target,
        WebkitPropertyDescriptor.createThisObjectDescriptor(thisObject),
        true));
  }

  private boolean isListLength(WebkitPropertyDescriptor descriptor) {
    if (parentVariable != null && parentVariable.isListValue()) {
      if ("length".equals(descriptor.getName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Some specific property filters, to make up for the fact that the enumerable property is not
   * always set correctly.
   * 
   * @param descriptor
   * @return
   */
  private boolean shouldFilter(WebkitPropertyDescriptor descriptor) {
    // array length
    if (isListLength(descriptor)) {
      return true;
    }

    // toString function
    if (descriptor.getValue() != null && descriptor.getValue().isFunction()) {
      if ("toString".equals(descriptor.getName())) {
        return true;
      }
    }

    return false;
  }

  private void worked() {
    latch.countDown();
  }

}
