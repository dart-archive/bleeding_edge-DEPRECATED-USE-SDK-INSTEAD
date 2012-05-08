/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.search.internal.ui;

import java.net.URL;

import org.osgi.framework.Bundle;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;

/**
 * Bundle of all images used by the Search UI plugin.
 */
public class SearchPluginImages {

  // The plugin registry
  private final static ImageRegistry PLUGIN_REGISTRY = SearchPlugin.getDefault().getImageRegistry();

  private static final IPath ICONS_PATH = new Path("$nl$/icons/full"); //$NON-NLS-1$

  public static final String T_OBJ = "obj16/"; //$NON-NLS-1$
  public static final String T_WIZBAN = "wizban/"; //$NON-NLS-1$
  public static final String T_LCL = "lcl16/"; //$NON-NLS-1$
  public static final String T_TOOL = "tool16/"; //$NON-NLS-1$
  public static final String T_EVIEW = "eview16/"; //$NON-NLS-1$

  private static final String NAME_PREFIX = "com.google.dart.tools.search.ui."; //$NON-NLS-1$
  private static final int NAME_PREFIX_LENGTH = NAME_PREFIX.length();

  // Define image names
  public static final String IMG_TOOL_SEARCH = NAME_PREFIX + "search.gif"; //$NON-NLS-1$

  public static final String IMG_LCL_REFRESH = NAME_PREFIX + "refresh.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_PIN_VIEW = NAME_PREFIX + "pin_view.gif"; //$NON-NLS-1$

  public static final String IMG_LCL_SEARCH_REM = NAME_PREFIX + "search_rem.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_REM_ALL = NAME_PREFIX + "search_remall.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_NEXT = NAME_PREFIX + "search_next.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_PREV = NAME_PREFIX + "search_prev.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_GOTO = NAME_PREFIX + "search_goto.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_SORT = NAME_PREFIX + "search_sortmatch.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_HISTORY = NAME_PREFIX + "search_history.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_FLAT_LAYOUT = NAME_PREFIX + "flatLayout.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_HIERARCHICAL_LAYOUT = NAME_PREFIX
      + "hierarchicalLayout.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_HORIZONTAL_ORIENTATION = NAME_PREFIX
      + "horizontalOrientation.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_VERTICAL_ORIENTATION = NAME_PREFIX
      + "verticalOrientation.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_AUTOMATIC_ORIENTATION = NAME_PREFIX
      + "automaticOrientation.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_SINGLE_ORIENTATION = NAME_PREFIX
      + "singleOrientation.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_CANCEL = NAME_PREFIX + "stop.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_COLLAPSE_ALL = NAME_PREFIX + "collapseall.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_EXPAND_ALL = NAME_PREFIX + "expandall.gif"; //$NON-NLS-1$
  public static final String IMG_LCL_SEARCH_FILTER = NAME_PREFIX + "filter_ps.gif"; //$NON-NLS-1$
  public static final String IMG_VIEW_SEARCHRES = NAME_PREFIX + "searchres.gif"; //$NON-NLS-1$

  public static final String IMG_OBJ_TSEARCH_DPDN = NAME_PREFIX + "tsearch_dpdn_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJ_SEARCHMARKER = NAME_PREFIX + "searchm_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJ_TEXT_SEARCH_LINE = NAME_PREFIX + "line_match.gif"; //$NON-NLS-1$

  // Define images
  public static final ImageDescriptor DESC_OBJ_TSEARCH_DPDN = createManaged(
      T_OBJ,
      IMG_OBJ_TSEARCH_DPDN);
  public static final ImageDescriptor DESC_OBJ_SEARCHMARKER = createManaged(
      T_OBJ,
      IMG_OBJ_SEARCHMARKER);
  public static final ImageDescriptor DESC_OBJ_TEXT_SEARCH_LINE = createManaged(
      T_OBJ,
      IMG_OBJ_TEXT_SEARCH_LINE);
  public static final ImageDescriptor DESC_VIEW_SEARCHRES = createManaged(
      T_EVIEW,
      IMG_VIEW_SEARCHRES);

  public static Image get(String key) {
    return PLUGIN_REGISTRY.get(key);
  }

  private static ImageDescriptor createManaged(String prefix, String name) {
    ImageDescriptor result = create(prefix, name.substring(NAME_PREFIX_LENGTH), true);
    PLUGIN_REGISTRY.put(name, result);
    return result;
  }

  /*
   * Creates an image descriptor for the given prefix and name in the Search plugin bundle. The path
   * can contain variables like $NL$. If no image could be found,
   * <code>useMissingImageDescriptor</code> decides if either the 'missing image descriptor' is
   * returned or <code>null</code>. or <code>null</code>.
   */
  private static ImageDescriptor create(String prefix, String name,
      boolean useMissingImageDescriptor) {
    IPath path = ICONS_PATH.append(prefix).append(name);
    return createImageDescriptor(
        SearchPlugin.getDefault().getBundle(),
        path,
        useMissingImageDescriptor);
  }

  /*
   * Sets all available image descriptors for the given action.
   */
  public static void setImageDescriptors(IAction action, String type, String relPath) {
    relPath = relPath.substring(NAME_PREFIX_LENGTH);

    action.setDisabledImageDescriptor(create("d" + type, relPath, false)); //$NON-NLS-1$

    ImageDescriptor desc = create("e" + type, relPath, true); //$NON-NLS-1$
    action.setHoverImageDescriptor(desc);
    action.setImageDescriptor(desc);
  }

  /*
   * Creates an image descriptor for the given path in a bundle. The path can contain variables like
   * $NL$. If no image could be found, <code>useMissingImageDescriptor</code> decides if either the
   * 'missing image descriptor' is returned or <code>null</code>. Added for 3.1.1.
   */
  public static ImageDescriptor createImageDescriptor(Bundle bundle, IPath path,
      boolean useMissingImageDescriptor) {
    URL url = FileLocator.find(bundle, path, null);
    if (url != null) {
      return ImageDescriptor.createFromURL(url);
    }
    if (useMissingImageDescriptor) {
      return ImageDescriptor.getMissingImageDescriptor();
    }
    return null;
  }

}
