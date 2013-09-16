/******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 ******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.wst.css.core.internal.metamodel.CSSMMNode;

/**
 * Interface for additional proposal information for CSS Content Assist
 */
public interface IProposalInfo {
  /**
   * Sets the element for additional proposal information
   * 
   * @param node the element for additional proposal information
   */
  void setInputElement(CSSMMNode node);
}
