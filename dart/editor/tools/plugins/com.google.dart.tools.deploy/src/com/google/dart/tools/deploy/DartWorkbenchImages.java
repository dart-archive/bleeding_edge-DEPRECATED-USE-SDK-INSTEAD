/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.deploy;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;

// TODO Clean up /icons/full to remove unused images. It contains a bunch of unneeded icons.
public class DartWorkbenchImages {

  /*** Constants for Images ***/

  // other toolbar buttons
  public final static String IMG_ETOOL_BUILD_EXEC = "IMG_ETOOL_BUILD_EXEC"; //$NON-NLS-1$

  public final static String IMG_ETOOL_BUILD_EXEC_HOVER = "IMG_ETOOL_BUILD_EXEC_HOVER"; //$NON-NLS-1$

  public final static String IMG_ETOOL_BUILD_EXEC_DISABLED = "IMG_ETOOL_BUILD_EXEC_DISABLED"; //$NON-NLS-1$

  public final static String IMG_ETOOL_SEARCH_SRC = "IMG_ETOOL_SEARCH_SRC"; //$NON-NLS-1$

  public final static String IMG_ETOOL_SEARCH_SRC_HOVER = "IMG_ETOOL_SEARCH_SRC_HOVER"; //$NON-NLS-1$

  public final static String IMG_ETOOL_SEARCH_SRC_DISABLED = "IMG_ETOOL_SEARCH_SRC_DISABLED"; //$NON-NLS-1$

  public final static String IMG_ETOOL_NEXT_NAV = "IMG_ETOOL_NEXT_NAV"; //$NON-NLS-1$

  public final static String IMG_ETOOL_PREVIOUS_NAV = "IMG_ETOOL_PREVIOUS_NAV"; //$NON-NLS-1$

  public final static String IMG_ETOOL_PROBLEM_CATEGORY = "IMG_ETOOL_PROBLEM_CATEGORY"; //$NON-NLS-1$

  public final static String IMG_ETOOL_PROBLEMS_VIEW = "IMG_ETOOL_PROBLEMS_VIEW"; //$NON-NLS-1$

  public final static String IMG_ETOOL_PROBLEMS_VIEW_ERROR = "IMG_ETOOL_PROBLEMS_VIEW_ERROR"; //$NON-NLS-1$

  public final static String IMG_ETOOL_PROBLEMS_VIEW_WARNING = "IMG_ETOOL_PROBLEMS_VIEW_WARNING"; //$NON-NLS-1$

  public final static String IMG_LCL_FLAT_LAYOUT = "IMG_LCL_FLAT_LAYOUT"; //$NON-NLS-1$

  public final static String IMG_LCL_HIERARCHICAL_LAYOUT = "IMG_LCL_HIERARCHICAL_LAYOUT"; //$NON-NLS-1$

  //wizard images
  public final static String IMG_WIZBAN_NEWPRJ_WIZ = "IMG_WIZBAN_NEWPRJ_WIZ"; //$NON-NLS-1$

  public final static String IMG_WIZBAN_NEWFOLDER_WIZ = "IMG_WIZBAN_NEWFOLDER_WIZ"; //$NON-NLS-1$

  public final static String IMG_WIZBAN_NEWFILE_WIZ = "IMG_WIZBAN_NEWFILE_WIZ"; //$NON-NLS-1$

  public final static String IMG_WIZBAN_IMPORTDIR_WIZ = "IMG_WIZBAN_IMPORTDIR_WIZ"; //$NON-NLS-1$

  public final static String IMG_WIZBAN_IMPORTZIP_WIZ = "IMG_WIZBAN_IMPORTZIP_WIZ"; //$NON-NLS-1$

  public final static String IMG_WIZBAN_EXPORTDIR_WIZ = "IMG_WIZBAN_EXPORTDIR_WIZ"; //$NON-NLS-1$

  public final static String IMG_WIZBAN_EXPORTZIP_WIZ = "IMG_WIZBAN_EXPORTZIP_WIZ"; //$NON-NLS-1$

  public final static String IMG_WIZBAN_RESOURCEWORKINGSET_WIZ = "IMG_WIZBAN_EXPORTZIP_WIZ"; //$NON-NLS-1$

  public final static String IMG_DLGBAN_SAVEAS_DLG = "IMG_DLGBAN_SAVEAS_DLG"; //$NON-NLS-1$

  public final static String IMG_DLGBAN_QUICKFIX_DLG = "IMG_DLGBAN_QUICKFIX_DLG"; //$NON-NLS-1$

  // task objects
  public final static String IMG_OBJS_COMPLETE_TSK = "IMG_OBJS_COMPLETE_TSK"; //$NON-NLS-1$

  public final static String IMG_OBJS_INCOMPLETE_TSK = "IMG_OBJS_INCOMPLETE_TSK"; //$NON-NLS-1$

  //problems images
  public static final String IMG_OBJS_ERROR_PATH = "IMG_OBJS_ERROR_PATH"; //$NON-NLS-1$

  public static final String IMG_OBJS_WARNING_PATH = "IMG_OBJS_WARNING_PATH"; //$NON-NLS-1$

  public static final String IMG_OBJS_INFO_PATH = "IMG_OBJS_INFO_PATH"; //$NON-NLS-1$

  // product
  public final static String IMG_OBJS_DEFAULT_PROD = "IMG_OBJS_DEFAULT_PROD"; //$NON-NLS-1$

  // welcome
  public final static String IMG_OBJS_WELCOME_ITEM = "IMG_OBJS_WELCOME_ITEM"; //$NON-NLS-1$

  public final static String IMG_OBJS_WELCOME_BANNER = "IMG_OBJS_WELCOME_BANNER"; //$NON-NLS-1$

  //Quick fix images
  public static final String IMG_DLCL_QUICK_FIX_DISABLED = "IMG_DLCL_QUICK_FIX_DISABLED";//$NON-NLS-1$

  public static final String IMG_ELCL_QUICK_FIX_ENABLED = "IMG_ELCL_QUICK_FIX_ENABLED"; //$NON-NLS-1$
  public static final String IMG_OBJS_FIXABLE_WARNING = "IMG_OBJS_FIXABLE_WARNING"; //$NON-NLS-1$
  public static final String IMG_OBJS_FIXABLE_ERROR = "IMG_OBJS_FIXABLE_ERROR"; //$NON-NLS-1$

  /**
   * Returns the image descriptor for the workbench image with the given symbolic name. Use this
   * method to retrieve image descriptors for any of the images named in this class.
   * 
   * @param symbolicName the symbolic name of the image
   * @return the image descriptor, or <code>null</code> if none
   */
  public static ImageDescriptor getImageDescriptor(String symbolicName) {
    return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(symbolicName);
  }

  /** Block instantiation. */
  private DartWorkbenchImages() {
  }
}
