/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.spelling;

import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;

/**
 * <p>
 * Defines an interface for deciding if an offset in an <code>IStructuredModel</code> should be
 * spell checked. Created so that content type specific decisions could be made as to whether a
 * region should be spell-checked or not. This is done using the <code>IAdapterFactory</code>
 * framework.
 * </p>
 */
public interface ISpellcheckDelegate {

  /**
   * Decide if the <code>offset</code> in <code>model</code> should be spell checked or not.
   * 
   * @param offset decide if this offset in the given <code>model</code> should be spell-checked
   * @param model used to decide if the given <code>offset</code> should be spell-checked
   * @return <code>true</code> if the given <code>offset</code> in the given <code>model</code>
   *         should be spell checked, <code>false</code> otherwise.
   */
  public boolean shouldSpellcheck(int offset, IStructuredModel model);
}
