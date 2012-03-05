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
package com.google.dart.tools.ui.internal.callhierarchy;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

class CalleeMethodWrapper extends MethodWrapper {

  private static class MethodWrapperComparator implements Comparator<MethodWrapper> {

    @Override
    public int compare(MethodWrapper m1, MethodWrapper m2) {
      CallLocation callLocation1 = m1.getMethodCall().getFirstCallLocation();
      CallLocation callLocation2 = m2.getMethodCall().getFirstCallLocation();

      if ((callLocation1 != null) && (callLocation2 != null)) {
        if (callLocation1.getStartPosition() == callLocation2.getStartPosition()) {
          return callLocation1.getEndPosition() - callLocation2.getEndPosition();
        }

        return callLocation1.getStartPosition() - callLocation2.getStartPosition();
      }

      return 0;
    }
  }

  private Comparator<MethodWrapper> methodWrapperComparator = new MethodWrapperComparator();

  public CalleeMethodWrapper(MethodWrapper parent, MethodCall methodCall) {
    super(parent, methodCall);
  }

  @Override
  public boolean canHaveChildren() {
    return true;
  }

  /**
   * Returns the calls sorted after the call location
   */
  @Override
  public MethodWrapper[] getCalls(IProgressMonitor progressMonitor) {
    MethodWrapper[] result = super.getCalls(progressMonitor);
    Arrays.sort(result, methodWrapperComparator);

    return result;
  }

  @Override
  protected MethodWrapper createMethodWrapper(MethodCall methodCall) {
    return new CalleeMethodWrapper(this, methodCall);
  }

  /**
   * Find callees called from the current method.
   */
  @Override
  protected Map<String, MethodCall> findChildren(IProgressMonitor progressMonitor) {
    DartElement member = getMember();
    if (member.exists()) {
      DartUnit cu = CallHierarchy.getCompilationUnitNode((CompilationUnitElement) member, true);
      if (progressMonitor != null) {
        progressMonitor.worked(5);
      }

      if (cu != null) {
        CalleeAnalyzerVisitor visitor = new CalleeAnalyzerVisitor(member, progressMonitor);

        cu.accept(visitor);
        return visitor.getCallees();
      }
    }
    return new HashMap<String, MethodCall>(0);
  }

  @Override
  protected String getTaskName() {
    return CallHierarchyMessages.CalleeMethodWrapper_taskname;
  }
}
