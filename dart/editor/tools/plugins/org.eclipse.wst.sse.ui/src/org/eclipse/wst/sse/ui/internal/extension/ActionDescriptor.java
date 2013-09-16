/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.extension;

import com.ibm.icu.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.ui.internal.IActionValidator;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.osgi.framework.Bundle;

/**
 * When 'action' tag is found in the registry, an object of this class is created. It creates the
 * appropriate action object and captures information that is later used to add this action object
 * into menu/tool bar. This class is reused for global (workbench) menu/tool bar, popup menu
 * actions, as well as view's pulldown and local tool bar.
 */
public class ActionDescriptor {
  public static final String ATT_ACCELERATOR = "accelerator"; //$NON-NLS-1$
  public static final String ATT_CLASS = "class"; //$NON-NLS-1$
  public static final String ATT_DEFINITION_ID = "definitionId"; //$NON-NLS-1$
  public static final String ATT_DESCRIPTION = "description"; //$NON-NLS-1$
  public static final String ATT_DISABLEDICON = "disabledIcon"; //$NON-NLS-1$
  public static final String ATT_HELP_CONTEXT_ID = "helpContextId"; //$NON-NLS-1$
  public static final String ATT_HOVERICON = "hoverIcon"; //$NON-NLS-1$
  public static final String ATT_ICON = "icon"; //$NON-NLS-1$

  public static final String ATT_ID = "id"; //$NON-NLS-1$
  public static final String ATT_LABEL = "label"; //$NON-NLS-1$
  public static final String ATT_MENUBAR_PATH = "menubarPath"; //$NON-NLS-1$
  public static final String ATT_POPUPMENU_PATH = "popupmenuPath"; //$NON-NLS-1$
  public static final String ATT_STATE = "state"; //$NON-NLS-1$
  public static final String ATT_TOOLBAR_PATH = "toolbarPath"; //$NON-NLS-1$
  public static final String ATT_TOOLTIP = "tooltip"; //$NON-NLS-1$

  /**
   * Creates an extension. If the extension plugin has not been loaded a busy cursor will be
   * activated during the duration of the load.
   * 
   * @param element the config element defining the extension
   * @param classAttribute the name of the attribute carrying the class
   * @returns the extension object if successful. If an error occurs when createing executable
   *          extension, the exception is logged, and null returned.
   */
  public static Object createExtension(final IConfigurationElement element,
      final String classAttribute) {
    final Object[] result = new Object[1];
    // If plugin has been loaded create extension.
    // Otherwise, show busy cursor then create extension.
    String pluginId = element.getDeclaringExtension().getNamespace();
    Bundle bundle = Platform.getBundle(pluginId);
    if (bundle.getState() == Bundle.ACTIVE) {
      try {
        result[0] = element.createExecutableExtension(classAttribute);
      } catch (Exception e) {
        // catch and log ANY exception from extension point
        handleCreateExecutableException(result, e);
      }
    } else {
      BusyIndicator.showWhile(null, new Runnable() {
        public void run() {
          try {
            result[0] = element.createExecutableExtension(classAttribute);
          } catch (Exception e) {
            // catch and log ANY exception from extension point
            handleCreateExecutableException(result, e);
          }
        }
      });
    }
    return result[0];
  }

  private static void handleCreateExecutableException(final Object[] result, Throwable e) {
    Logger.logException(e);
    result[0] = null;
  }

  private String id;

  private ActionContributionItem item;
  private String menuGroup;
  private String menuPath;
  private String popupmenuGroup;
  private String popupmenuPath;
  private String toolbarGroup;
  private String toolbarPath;

  /**
   * Creates a new descriptor with the targetType
   */
  public ActionDescriptor(IConfigurationElement actionElement) throws CoreException {

    // Calculate menu and toolbar paths.
    String mpath = actionElement.getAttribute(ATT_MENUBAR_PATH);
    String mgroup = null;
    if (mpath != null) {
      int loc = mpath.lastIndexOf('/');
      if (loc != -1) {
        mgroup = mpath.substring(loc + 1);
        mpath = mpath.substring(0, loc);
      } else {
        mgroup = mpath;
        mpath = null;
      }
    }
    menuPath = mpath;
    menuGroup = mgroup;

    String ppath = actionElement.getAttribute(ATT_POPUPMENU_PATH);
    String pgroup = null;
    if (ppath != null) {
      int loc = ppath.lastIndexOf('/');
      if (loc != -1) {
        pgroup = ppath.substring(loc + 1);
        ppath = ppath.substring(0, loc);
      } else {
        pgroup = ppath;
        ppath = null;
      }
    }
    popupmenuPath = ppath;
    popupmenuGroup = pgroup;

    String tpath = actionElement.getAttribute(ATT_TOOLBAR_PATH);
    String tgroup = null;
    if (tpath != null) {
      int loc = tpath.lastIndexOf('/');
      if (loc != -1) {
        tgroup = tpath.substring(loc + 1);
        tpath = tpath.substring(0, loc);
      } else {
        tgroup = tpath;
        tpath = null;
      }
    }
    toolbarPath = tpath;
    toolbarGroup = tgroup;

    // Create action.
    IAction action = createAction(actionElement);
    if (action == null)
      return;

    String label = actionElement.getAttribute(ATT_LABEL);
    if (label != null)
      action.setText(label);

    id = actionElement.getAttribute(ATT_ID);
    if (id == null) {
      id = actionElement.getAttribute(ATT_CLASS);
    }
    if (id != null)
      action.setId(id);

    String defId = actionElement.getAttribute(ATT_DEFINITION_ID);
    if (defId != null && defId.length() != 0) {
      action.setActionDefinitionId(defId);
    }

    String tooltip = actionElement.getAttribute(ATT_TOOLTIP);
    if (tooltip != null)
      action.setToolTipText(tooltip);

    String helpContextId = actionElement.getAttribute(ATT_HELP_CONTEXT_ID);
    if (helpContextId != null) {
      String fullID = helpContextId;
      if (helpContextId.indexOf(".") == -1) //$NON-NLS-1$
        // For backward compatibility we auto qualify the id if it is
        // not qualified)
        fullID = actionElement.getDeclaringExtension().getNamespace() + "." + helpContextId; //$NON-NLS-1$
      PlatformUI.getWorkbench().getHelpSystem().setHelp(action, fullID);
    }

    String description = actionElement.getAttribute(ATT_DESCRIPTION);
    if (description != null)
      action.setDescription(description);

    String state = actionElement.getAttribute(ATT_STATE);
    if (state != null) {
      action.setChecked(state.equals("true")); //$NON-NLS-1$
    }

    String icon = actionElement.getAttribute(ATT_ICON);
    if (icon != null) {
      action.setImageDescriptor(ImageUtil.getImageDescriptorFromExtension(
          actionElement.getDeclaringExtension(), icon));
    }

    String hoverIcon = actionElement.getAttribute(ATT_HOVERICON);
    if (hoverIcon != null) {
      action.setHoverImageDescriptor(ImageUtil.getImageDescriptorFromExtension(
          actionElement.getDeclaringExtension(), hoverIcon));
    }

    String disabledIcon = actionElement.getAttribute(ATT_DISABLEDICON);
    if (disabledIcon != null) {
      action.setDisabledImageDescriptor(ImageUtil.getImageDescriptorFromExtension(
          actionElement.getDeclaringExtension(), disabledIcon));
    }

    String accelerator = actionElement.getAttribute(ATT_ACCELERATOR);
    if (accelerator != null)
      processAccelerator(action, accelerator);

    item = new ActionContributionItem(action);
  }

  /**
   * Parses the given accelerator text, and converts it to an accelerator key code.
   * 
   * @param acceleratorText the accelerator text
   * @result the SWT key code, or 0 if there is no accelerator
   */
  private int convertAccelerator(String acceleratorText) {
    int accelerator = 0;
    StringTokenizer stok = new StringTokenizer(acceleratorText, "+"); //$NON-NLS-1$

    int keyCode = -1;

    boolean hasMoreTokens = stok.hasMoreTokens();
    while (hasMoreTokens) {
      String token = stok.nextToken();
      hasMoreTokens = stok.hasMoreTokens();
      // Every token except the last must be one of the modifiers
      // Ctrl, Shift, or Alt.
      if (hasMoreTokens) {
        int modifier = Action.findModifier(token);
        if (modifier != 0) {
          accelerator |= modifier;
        } else { //Leave if there are none
          return 0;
        }
      } else {
        keyCode = Action.findKeyCode(token);
      }
    }
    if (keyCode != -1) {
      accelerator |= keyCode;
    }
    return accelerator;
  }

  /**
	 */
  private IAction createAction(IConfigurationElement actionElement) {
    Object action = new ExtendedEditorActionProxyForDelayLoading(actionElement, ATT_CLASS);
    if (action == null)
      return null;
    if (action instanceof IActionValidator) {
      if (!((IActionValidator) action).isValidAction())
        return null;
    }
    return (action instanceof IAction ? (IAction) ExtendedEditorActionProxy.newInstance(action)
        : null);
  }

  /**
   * Returns the action object held in this descriptor.
   */
  public IAction getAction() {
    return (item != null ? item.getAction() : null);
  }

  /**
   * Returns the IContributionItem object held in this descriptor.
   */
  public IContributionItem getContributionItem() {
    return item;
  }

  /**
   * Returns action's id as defined in the registry.
   */
  public String getId() {
    return id;
  }

  /**
   * Returns named slot (group) in the menu where this action should be added.
   */
  public String getMenuGroup() {
    return menuGroup;
  }

  /**
   * Returns menu path where this action should be added. If null, the action will not be added into
   * the menu.
   */

  public String getMenuPath() {
    return menuPath;
  }

  /**
   * Returns named slot (group) in the popup menu where this action should be added.
   */
  public String getPopupMenuGroup() {
    return popupmenuGroup;
  }

  /**
   * Returns popup menu path where this action should be added. If null, the action will not be
   * added into the popup menu.
   */

  public String getPopupMenuPath() {
    return popupmenuPath;
  }

  /**
   * Returns the named slot (group) in the tool bar where this action should be added.
   */

  public String getToolbarGroup() {
    return toolbarGroup;
  }

  /**
   * Returns path in the tool bar where this action should be added. If null, action will not be
   * added to the tool bar.
   */
  public String getToolbarPath() {
    return toolbarPath;
  }

  /**
   * Process the accelerator definition. If it is a number then process the code directly - if not
   * then parse it and create the code
   */
  private void processAccelerator(IAction action, String acceleratorText) {

    if (acceleratorText.length() == 0)
      return;

    //Is it a numeric definition?
    if (Character.isDigit(acceleratorText.charAt(0))) {
      try {
        action.setAccelerator(Integer.valueOf(acceleratorText).intValue());
      } catch (NumberFormatException exception) {
        Logger.log(Logger.ERROR, "Invalid accelerator declaration: " + id); //$NON-NLS-1$
      }
    } else
      action.setAccelerator(convertAccelerator(acceleratorText));
  }

  /**
   * For debugging only.
   */
  public String toString() {
    return "ActionDescriptor(" + id + ")"; //$NON-NLS-2$//$NON-NLS-1$
  }
}
