/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.contentassist;

public abstract class AutoActivationDelegate {

  public char[] getCompletionProposalAutoActivationCharacters() {
    return null;
  }

  public char[] getContextInformationAutoActivationCharacters() {
    return null;
  }

  public abstract void dispose();
}
