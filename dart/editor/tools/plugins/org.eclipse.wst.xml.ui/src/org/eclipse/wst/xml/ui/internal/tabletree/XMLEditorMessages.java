/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.tabletree;

import org.eclipse.osgi.util.NLS;

/**
 * Strings used by XML Editor
 * 
 * @plannedfor 1.0
 */
public class XMLEditorMessages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.wst.xml.ui.internal.tabletree.XMLEditorResources";//$NON-NLS-1$

  public static String XMLTableTreeViewer_0;
  public static String XMLTableTreeViewer_1;
  public static String XMLTableTreeViewer_2;
  public static String XMLMultiPageEditorPart_0;
  public static String XMLTreeExtension_0;
  public static String XMLTreeExtension_1;
  public static String XMLTreeExtension_3;
  public static String XMLTreeExtension_4;
  public static String XMLTableTreeActionBarContributor_0;
  public static String XMLTableTreeActionBarContributor_1;
  public static String XMLTableTreeActionBarContributor_2;
  public static String XMLTableTreeActionBarContributor_3;
  public static String XMLTableTreeActionBarContributor_4;
  public static String XMLTableTreeActionBarContributor_5;
  public static String XMLTableTreeActionBarContributor_6;
  public static String XMLTableTreeActionBarContributor_7;
  public static String XMLTableTreeActionBarContributor_8;
  public static String An_error_has_occurred_when1_ERROR_;

  public static String ConfigureColumns_label;
  public static String Resource__does_not_exist;
  public static String Editor_could_not_be_open;

  public static String EditorMenu_tooltip;
  public static String TreeExtension_0;

  static {
    // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, XMLEditorMessages.class);
  }

  private XMLEditorMessages() {
    // cannot create new instance
  }
}
