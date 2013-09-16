/******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 ******************************************************************************/
package org.eclipse.wst.sse.ui;

/**
 * This interface defines extends {@link ISemanticHighlighting} to provide a preference key for
 * background styling when not using a <code>styleString</code> associated with the semantic
 * highlighting
 * 
 * @since 3.3
 */
public interface ISemanticHighlightingExtension2 {
  /**
   * The preference key that controls the text's background color attribute for the semantic
   * highlighting
   * 
   * @return the background color preference key
   */
  String getBackgroundColorPreferenceKey();
}
