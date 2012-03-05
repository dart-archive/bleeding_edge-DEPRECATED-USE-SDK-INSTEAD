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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

public class DeferredMethodWrapper extends MethodWrapperWorkbenchAdapter implements
    IDeferredWorkbenchAdapter {

  /**
   * A simple job scheduling rule for serializing jobs that shouldn't be run concurrently.
   */
  private class BatchSimilarSchedulingRule implements ISchedulingRule {
    public String id;

    public BatchSimilarSchedulingRule(String id) {
      this.id = id;
    }

    @Override
    public boolean contains(ISchedulingRule rule) {
      return this == rule;
    }

    @Override
    public boolean isConflicting(ISchedulingRule rule) {
      if (rule instanceof BatchSimilarSchedulingRule) {
        return ((BatchSimilarSchedulingRule) rule).id.equals(id);
      }
      return false;
    }
  }

  private final CallHierarchyContentProvider provider;

  DeferredMethodWrapper(CallHierarchyContentProvider provider, MethodWrapper methodWrapper) {
    super(methodWrapper);
    this.provider = provider;
  }

  @Override
  public void fetchDeferredChildren(Object object, IElementCollector collector,
      IProgressMonitor monitor) {
    final DeferredMethodWrapper deferredMethodWrapper = (DeferredMethodWrapper) object;
    try {
      provider.startFetching();
      collector.add((Object[]) deferredMethodWrapper.getCalls(monitor), monitor);
      collector.done();
    } catch (OperationCanceledException e) {
      final MethodWrapper methodWrapper = deferredMethodWrapper.getMethodWrapper();
      if (!CallHierarchyContentProvider.isExpandWithConstructors(methodWrapper)) {
        Display.getDefault().asyncExec(new Runnable() {
          @Override
          public void run() {
            CallHierarchyViewPart viewPart = provider.getViewPart();
            if (viewPart != null && !viewPart.getViewer().getControl().isDisposed()) {
              provider.collapseAndRefresh(methodWrapper);
            }
          }
        });
      }
    } catch (Exception e) {
      DartToolsPlugin.log(e);
    } finally {
      provider.doneFetching();
    }
  }

  @Override
  public Object[] getChildren(Object o) {
    return this.provider.fetchChildren(((DeferredMethodWrapper) o).getMethodWrapper());
  }

  @Override
  public ISchedulingRule getRule(Object o) {
    return new BatchSimilarSchedulingRule("com.google.dart.tools.ui.callhierarchy.methodwrapper"); //$NON-NLS-1$
  }

  @Override
  public boolean isContainer() {
    return true;
  }

  private Object getCalls(IProgressMonitor monitor) {
    return getMethodWrapper().getCalls(monitor);
  }

}
