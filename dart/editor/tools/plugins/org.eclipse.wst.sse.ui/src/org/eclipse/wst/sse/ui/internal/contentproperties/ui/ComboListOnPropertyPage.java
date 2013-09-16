/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentproperties.ui;

import org.eclipse.swt.widgets.Composite;

/**
 * @deprecated People should manage their own combo/list
 */
public final class ComboListOnPropertyPage extends ComboList {

  private String currentApplyValue;

  public ComboListOnPropertyPage(Composite parent, int style) {
    super(parent, style);
  }

  public final String getApplyValue() {
    return currentApplyValue;
  }

  public final void setApplyValue(String currentApplyValue) {
    this.currentApplyValue = currentApplyValue;
  }

}
