/******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 ******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMNode;
import org.eclipse.wst.css.ui.internal.CSSUIPlugin;
import org.eclipse.wst.css.ui.internal.Logger;

/**
 * Generates proposal information for {@link CSSMMNode}s. If the Proposal Information instance
 * implements {@link IProposalInfo} the node will be set as the input element.
 */
class ProposalInfoFactory {

  private static IConfigurationElement fElement = null;
  private static boolean fIsInitialized = false;

  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  private ProposalInfoFactory() {
  }

  /**
   * Returns additional proposal information for <code>node</code>
   * 
   * @param node the CSS metamodel node
   * @return additional proposal information about <code>node></code>
   */
  public static synchronized Object getProposalInfo(CSSMMNode node) {
    Object info = null;
    if (!fIsInitialized) {
      // Only attempt to initialize this once. If there are no proposal information providers, we'll always return null
      IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
          CSSUIPlugin.ID, "proposalInfo"); //$NON-NLS-1$
      if (elements.length > 0) {
        // Get the first proposal info that's declared
        fElement = elements[0];
      }

      fIsInitialized = true;
    }

    if (fElement != null) {
      try {
        info = fElement.createExecutableExtension(ATTR_CLASS); //$NON-NLS-1$
        if (info instanceof IProposalInfo) {
          ((IProposalInfo) info).setInputElement(node);
        }
      } catch (CoreException e) {
        Logger.log(Logger.ERROR,
            "Could not create instance for proposalInfo [" + fElement.getAttribute("class") + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
    }
    return info;
  }

}
