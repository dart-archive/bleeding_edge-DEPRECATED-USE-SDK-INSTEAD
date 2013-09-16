/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.style;

import org.eclipse.wst.xml.ui.internal.style.IStyleConstantsXML;

/**
 * Contains the symbolic name of styles used by LineStyleProvider, ColorManager, and any others who
 * may be interested
 */
public interface IStyleConstantsHTML extends IStyleConstantsXML {
  public static final String SCRIPT_AREA_BORDER = "SCRIPT_AREA_BORDER";//$NON-NLS-1$
  public static final String SCRIPT_AREA = "SCRIPT_AREA";//$NON-NLS-1$
}
