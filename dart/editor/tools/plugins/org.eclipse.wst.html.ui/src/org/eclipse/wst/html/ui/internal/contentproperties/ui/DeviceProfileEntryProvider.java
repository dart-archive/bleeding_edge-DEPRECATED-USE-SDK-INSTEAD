/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentproperties.ui;

import java.util.Iterator;

/**
 * <p>
 * This interface is not intended to be implemented by clients directly. Instead, please use
 * abstract class(AbstractDeviceProfileEntryProvider) instead.
 * </p>
 * 
 * @deprecated Not needed. See BUG118359
 */
public interface DeviceProfileEntryProvider {
  public Iterator getDeviceProfileEntries();

  public void release();
}
