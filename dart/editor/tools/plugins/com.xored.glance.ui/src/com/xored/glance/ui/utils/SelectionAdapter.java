/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.ui.utils;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

/**
 * @author Yuri Strot
 */
public abstract class SelectionAdapter implements SelectionListener {

  public abstract void selected(SelectionEvent e);

  @Override
  public final void widgetDefaultSelected(SelectionEvent e) {
    selected(e);
  }

  @Override
  public final void widgetSelected(SelectionEvent e) {
    selected(e);
  }

}
