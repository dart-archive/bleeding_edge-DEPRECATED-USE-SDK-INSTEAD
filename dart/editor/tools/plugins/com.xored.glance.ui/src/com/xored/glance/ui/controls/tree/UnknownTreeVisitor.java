/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.tree;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class UnknownTreeVisitor {

  public void visit(IProgressMonitor monitor) {
    monitor.beginTask("Tree indexing", 100);
    try {
      Object[] roots = getRoots();
      monitor.worked(1);
      breadthVisit(roots, monitor, 1);
    } finally {
      monitor.done();
    }
  }

  protected abstract Object[] getChildren(Object element);

  protected abstract Object[] getRoots();

  private void breadthVisit(Object[] elements, IProgressMonitor monitor, int level) {
    List<Object> next = new ArrayList<Object>();
    for (Object element : elements) {
      if (monitor.isCanceled()) {
        return;
      }
      next.addAll(Arrays.asList(getChildren(element)));
    }
    if (monitor.isCanceled()) {
      return;
    }
    int work = (int) Math.pow(2, level);
    monitor.worked(work);
    int totalWork = 2 * work - 1;
    int size = next.size();
    if (size == 0) {
      return;
    }
    int remains = 100 - totalWork;
    if (remains > size) {
      breadthVisit(next.toArray(), monitor, level + 1);
    } else {
      SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, remains);
      try {
        subMonitor.beginTask("", size);
        for (Object element : next) {
          depthVisit(element, monitor, level + 1);
          subMonitor.worked(1);
        }
      } finally {
        subMonitor.done();
      }
    }
  }

  private void depthVisit(Object element, IProgressMonitor monitor, int level) {
    if (level > 4) {
      return;
    }
    for (Object child : getChildren(element)) {
      if (monitor.isCanceled()) {
        return;
      }
      depthVisit(child, monitor, level + 1);
    }
  }

}
