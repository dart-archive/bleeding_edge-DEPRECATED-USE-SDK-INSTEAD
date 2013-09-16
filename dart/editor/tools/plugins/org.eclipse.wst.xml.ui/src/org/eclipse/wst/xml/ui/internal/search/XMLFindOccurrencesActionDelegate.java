/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.sse.ui.internal.search.FindOccurrencesActionDelegate;

/**
 * Sets up FindOccurrencesActionDelegate for xml find occurrences processors
 */
public class XMLFindOccurrencesActionDelegate extends FindOccurrencesActionDelegate {
  private List fProcessors;

  protected List getProcessors() {
    if (fProcessors == null) {
      fProcessors = new ArrayList();
      XMLFindOccurrencesProcessor htmlProcessor = new XMLFindOccurrencesProcessor();
      fProcessors.add(htmlProcessor);
    }
    return fProcessors;
  }
}
