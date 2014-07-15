/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.sse.ui.internal.rules;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.wst.sse.ui.internal.Logger;

import java.util.AbstractCollection;
import java.util.Iterator;

/**
 * Passed to LineStyleProviders as the Collection for which StyleRanges are to be added. This class
 * provides additional sanity checks on the incoming StyleRanges so that we do not rely upon SWT to
 * report Errors with no record of the StyleRange or contributing LineStyleProvider, as well as a
 * slight performance increase by not allocating a redundant collection for the StyleRanges. This
 * class intentionally violates the contract for java.util.Collection.
 */

class PresentationCollector extends AbstractCollection {
  private final TextPresentation fPresentation;
  private int lastOffset;

  /**
   * @param presentation - the Presentation being added to
   * @param applyOnAdd
   */
  PresentationCollector(TextPresentation presentation) {
    super();
    Assert.isNotNull(presentation);
    fPresentation = presentation;
    lastOffset = 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Collection#add(java.lang.Object)
   */
  public boolean add(Object o) {
    StyleRange range = (StyleRange) o;
    if (lastOffset > range.start) {
//      Logger.log(Logger.ERROR,
//          "Overlapping start in StyleRange " + range.start + ":" + range.length); //$NON-NLS-1$ //$NON-NLS-2$
      return false;
    } else if (range.length < 0) {
      Logger.log(Logger.ERROR, "StyleRange with negative length" + range.start + ":" + range.length); //$NON-NLS-1$ //$NON-NLS-2$
      return false;
    }
    lastOffset = range.start + range.length;
    fPresentation.addStyleRange(range);
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.AbstractCollection#iterator()
   */
  public Iterator iterator() {
    return fPresentation.getNonDefaultStyleRangeIterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.AbstractCollection#size()
   */
  public int size() {
    throw new UnsupportedOperationException();
  }
}
