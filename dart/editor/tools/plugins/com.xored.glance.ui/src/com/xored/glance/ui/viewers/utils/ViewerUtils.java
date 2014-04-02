/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.viewers.utils;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TypedListener;

import java.lang.reflect.Field;

public class ViewerUtils {

  public static ITextViewer getTextViewer(StyledText text) {
    return getViewer(ITextViewer.class, text, SWT.Selection);
  }

  public static TreeViewer getTreeViewer(Tree tree) {
    return getViewer(TreeViewer.class, tree, SWT.Expand);
  }

  private static <T> T getViewer(Class<T> clazz, Control control, int event) {
    Listener[] listeners = control.getListeners(event);
    for (Listener listener : listeners) {
      Object lookFor = listener;
      if (listener instanceof TypedListener) {
        lookFor = ((TypedListener) listener).getEventListener();
      }
      try {
        Field this$0 = lookFor.getClass().getDeclaredField("this$0");
        this$0.setAccessible(true);
        Object viewer = this$0.get(lookFor);
        if (clazz.isInstance(viewer)) {
          return clazz.cast(viewer);
        }
      } catch (Exception e) {
        // ignore exceptions
      }
    }
    return null;
  }

}
