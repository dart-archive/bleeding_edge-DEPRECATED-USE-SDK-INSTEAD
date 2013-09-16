/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search;

public interface ISearchConstants {

  /**
   * The search result is a declaration. Can be used in conjunction with any of the nature of
   * searched elements so as to better narrow down the search.
   */
  public static int DECLARATIONS = 0;

  /**
   * The search result is a reference. Can be used in conjunction with any of the nature of searched
   * elements so as to better narrow down the search. References can contain implementers since they
   * are more generic kind of matches.
   */
  public static int REFERENCES = 1;

  /**
   * The search result is a declaration, or a reference. Can be used in conjunction with any of the
   * nature of searched elements so as to better narrow down the search.
   */
  public static int ALL_OCCURRENCES = 2;
}
