/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.catalog;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogElement;

public abstract class ElementNodePage {

  Control fControl;

  public ElementNodePage() {
    super();

  }

  public abstract Control createControl(Composite parent);

  public Control getControl() {
    return fControl;
  }

  public abstract void saveData();

  public abstract ICatalogElement getData();
}
