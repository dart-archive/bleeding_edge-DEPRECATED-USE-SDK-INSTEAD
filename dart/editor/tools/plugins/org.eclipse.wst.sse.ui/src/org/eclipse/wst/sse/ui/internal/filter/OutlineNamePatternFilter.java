/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.filter;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * The NamePatternFilter selects the elements which match the given string patterns.
 * <p>
 * The following characters have special meaning: ? => any character * => any string
 * </p>
 */
public class OutlineNamePatternFilter extends ViewerFilter {
  private String[] fPatterns;
  private StringMatcher[] fMatchers;

  /**
   * Gets the patterns for the receiver.
   * 
   * @return returns the patterns to be filtered for
   */
  public String[] getPatterns() {
    return fPatterns;
  }

  /*
   * (non-Javadoc) Method declared on ViewerFilter.
   */
  public boolean select(Viewer viewer, Object parentElement, Object element) {
    if (getPatterns().length == 0) {
      return true;
    }
    String matchName = null;
    if (viewer instanceof ContentViewer) {
      final IBaseLabelProvider labelProvider = ((ContentViewer) viewer).getLabelProvider();
      if (labelProvider instanceof ILabelProvider) {
        matchName = ((ILabelProvider) labelProvider).getText(element);
      }
    }
    if (matchName != null && matchName.length() > 0) {
      String[] fPatterns = getPatterns();
      for (int i = 0; i < fPatterns.length; i++) {
        if (new StringMatcher(fPatterns[i], true, false).match(matchName))
          return false;
      }
      return true;
    }
    return true;
  }

  /**
   * Sets the patterns to filter out for the receiver.
   * <p>
   * The following characters have special meaning: ? => any character * => any string
   * </p>
   * 
   * @param newPatterns the new patterns
   */
  public void setPatterns(String[] newPatterns) {
    fPatterns = newPatterns;
    fMatchers = new StringMatcher[newPatterns.length];
    for (int i = 0; i < newPatterns.length; i++) {
      //Reset the matchers to prevent constructor overhead
      fMatchers[i] = new StringMatcher(newPatterns[i], true, false);
    }
  }
}
