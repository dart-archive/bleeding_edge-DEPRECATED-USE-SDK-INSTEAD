/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Position;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;

/**
 * This interface defines preferences and consumability for semantic highlighting. The preference
 * keys provided by the implementation are stored within the preference store provided by the
 * implementation. When a region is consumable by the semantic highlighting, the implementation
 * provides an array of positions to style.
 * 
 * @since 3.1
 */
public interface ISemanticHighlighting {

  /**
   * The preference key that controls the text's bold attribute for the semantic highlighting
   * 
   * @return the bold preference key
   */
  public String getBoldPreferenceKey();

  /**
   * The preference key that controls the text's underline attribute for the semantic highlighting
   * 
   * @return the underline preference key
   */
  public String getUnderlinePreferenceKey();

  /**
   * The preference key that controls the text's strikethrough attribute for the semantic
   * highlighting
   * 
   * @return the strikethrough preference key
   */
  public String getStrikethroughPreferenceKey();

  /**
   * The preference key that controls the text's italic attribute for the semantic highlighting
   * 
   * @return the italic preference key
   */
  public String getItalicPreferenceKey();

  /**
   * The preference key that controls the text's color attribute for the semantic highlighting
   * 
   * @return the foreground color preference key
   */
  public String getColorPreferenceKey();

  /**
   * The preference store that maintains the preferences for the semantic highlighting
   * 
   * @return the preference store for the semantic highlighting
   */
  public IPreferenceStore getPreferenceStore();

  /**
   * The preference key that controls if the semantic highlighting is enabled
   * 
   * @return the enabled state preference key
   */
  public String getEnabledPreferenceKey();

  /**
   * @return the display name
   */
  public String getDisplayName();

  /**
   * Returns an array of positions iff the semantic highlighting consumes any part of the structured
   * document region.
   * <p>
   * NOTE: Implementors are not allowed to keep a reference on the region or on any object retrieved
   * from the region.
   * </p>
   * 
   * @param region the structured document region
   * @return an array of positions to consume iff the semantic highlighting consumes any part of the
   *         structured document region, otherwise null
   */
  public Position[] consumes(IStructuredDocumentRegion region);
}
