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

package com.google.dart.tools.debug.core.logical;

import com.google.dart.tools.core.utilities.general.StringUtilities;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.util.IDartDebugValue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IIndexedValue;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This ILogicalStructureTypeDelegate handles displaying Dart types, like maps, in a more logical
 * view.
 */
public class MapStructureType implements ILogicalStructureTypeDelegate {
  private static final Set<String> MAP_TYPES = new HashSet<String>(Arrays.asList(
      "Map",
      "HashMap",
      "_HashMap",
      "LinkedHashMap",
      "_LinkedHashMap",
      "SplayTreeMap"));

  private static final Set<String> SET_TYPES = new HashSet<String>(Arrays.asList(
      "Set",
      "HashSet",
      "_LinkedHashSet"));

  private static final String MAP_EXPR = "keys.expand("
      + "(key) => [key is String ? '\\'${key}\\'' : '${key}', this[key]]).toList()";

  private static final String SET_EXPR = "expand("
      + "(key) => [key is String ? '\\'${key}\\'' : '${key}', key]).toList()";

  public MapStructureType() {

  }

  @Override
  public IValue getLogicalStructure(IValue value) throws CoreException {
    IDartDebugValue dartDebugValue = (IDartDebugValue) value;

    try {
      String refTypeName = value.getReferenceTypeName();

      if (MAP_TYPES.contains(refTypeName)) {
        return createMap(dartDebugValue, MAP_EXPR, true);
      }

      if (SET_TYPES.contains(refTypeName)) {
        return createMap(dartDebugValue, SET_EXPR, true);
      }
    } catch (Throwable t) {
      DartDebugCorePlugin.logError(t);
    }

    return value;
  }

  @Override
  public boolean providesLogicalStructure(IValue value) {
    if (!(value instanceof IDartDebugValue)) {
      return false;
    }

    try {
      String refTypeName = value.getReferenceTypeName();

      // Map types.
      if (MAP_TYPES.contains(refTypeName)) {
        return true;
      }

      // Set types.
      if (SET_TYPES.contains(refTypeName)) {
        return true;
      }
    } catch (Throwable t) {

    }

    return false;
  }

  private IValue createMap(IDartDebugValue value, String evalExpr, boolean convertToMap)
      throws DebugException {
    final CountDownLatch latch = new CountDownLatch(1);
    final IWatchExpressionResult[] results = new IWatchExpressionResult[1];

    value.evaluateExpression(evalExpr, new IWatchExpressionListener() {
      @Override
      public void watchEvaluationFinished(IWatchExpressionResult result) {
        results[0] = result;
        latch.countDown();
      }
    });

    try {
      latch.await(3000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new DebugException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "timeout from debug client"));
    }

    IWatchExpressionResult exResult = results[0];

    if (exResult.getException() != null) {
      throw exResult.getException();
    }

    IValue val = exResult.getValue();

    if (!(val instanceof IDartDebugValue)) {
      return val;
    }

    if (!convertToMap || !(val instanceof IIndexedValue)) {
      return val;
    }

    IIndexedValue resultValue = (IIndexedValue) val;

    IVariable[] variables = resultValue.getVariables(0, resultValue.getSize());
    List<IVariable> vars = new ArrayList<IVariable>();

    for (int i = 0; i < variables.length; i += 2) {
      String keyName = variables[i].getValue().getValueString();
      keyName = StringUtilities.stripQuotes(keyName);

      IValue keyValue = variables[i + 1].getValue();

      vars.add(new LogicalDebugVariable(keyName, keyValue));
    }

    return new LogicalDebugValue(value, vars.toArray(new IVariable[vars.size()]));
  }
}
