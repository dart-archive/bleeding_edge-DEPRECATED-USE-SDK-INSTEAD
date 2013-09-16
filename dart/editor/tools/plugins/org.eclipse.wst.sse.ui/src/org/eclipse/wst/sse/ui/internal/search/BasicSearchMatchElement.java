/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.search;

import org.eclipse.core.resources.IFile;

/**
 * This is used as the element for a <code>Match</code> object by <code>BasicSearchQuery</code>. It
 * contains all the needed information for use by the query, result providers, and label providers.
 * 
 * @see org.eclipse.search.ui.text.Match
 * @see org.eclipse.wst.sse.ui.internal.search.BasicSearchQuery
 */
public class BasicSearchMatchElement {
  /**
   * The file this element is a match on
   */
  private IFile fFile;

  /**
   * The line number this element is a match on
   */
  private int fLineNumber;

  /**
   * The offset of the line in the file
   */
  private int fLineOffset;

  /**
   * The line this match is on
   */
  private String fLine;

  /**
   * Constructor
   * 
   * @param file The file this element is a match on
   * @param lineNumber The line number this element is a match on
   * @param lineOffset The offset of the line in the file
   * @param message The message associated with this element
   */
  public BasicSearchMatchElement(IFile file, int lineNumber, int lineOffset, String message) {
    this.fFile = file;
    this.fLineNumber = lineNumber;
    this.fLineOffset = lineOffset;
    this.fLine = message;
  }

  /**
   * @return The file this element is a match on
   */
  public IFile getFile() {
    return this.fFile;
  }

  /**
   * @return The line number this element is a match on
   */
  public int getLineNum() {
    return this.fLineNumber;
  }

  /**
   * @return the offset of the line the match is on in the file
   */
  public int geLineOffset() {
    return this.fLineOffset;
  }

  /**
   * @return The line this match is on
   */
  public String getLine() {
    return this.fLine;
  }

  /**
   * <p>
   * Two <code>BasicSearchMatchElement</code> are equal if they are both on the same line. This
   * helps with determining how many matches are all on the same line which is automatically taken
   * care of by internal Eclipse search APIs so long as this equal method is structured thusly.
   * </p>
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    boolean equal = false;

    if (obj instanceof BasicSearchMatchElement) {
      equal = getLineNum() == ((BasicSearchMatchElement) obj).getLineNum();
    }

    return equal;
  }

  /**
   * Like the <code>#equals</code> method this function is needed so that the internal Eclipse
   * search APIs can keep track of matches that are on the same line.
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return getLineNum() + getLine().hashCode();
  }
}
