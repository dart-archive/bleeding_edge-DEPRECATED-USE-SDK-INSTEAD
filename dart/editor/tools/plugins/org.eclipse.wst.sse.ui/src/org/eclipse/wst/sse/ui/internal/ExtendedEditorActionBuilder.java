/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.wst.sse.ui.internal.extension.ActionDescriptor;
import org.eclipse.wst.sse.ui.internal.extension.IExtendedEditorActionProxyForDelayLoading;
import org.eclipse.wst.sse.ui.internal.extension.RegistryReader;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ISourceEditingTextTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class reads the registry for extensions that plug into 'editorActions' extension point.
 */

public class ExtendedEditorActionBuilder extends RegistryReader {

  public class ExtendedContributor implements IExtendedContributor, IMenuListener {
    private IExtendedSimpleEditor activeExtendedEditor = null;

    private List cache;
    private Map map = new HashMap();
    private IMenuManager menuBar = null;

    private Set menus = new HashSet();

    public ExtendedContributor(List cache) {
      this.cache = cache;
    }

    private IExtendedSimpleEditor computeExtendedEditor(final IEditorPart editor) {
      IExtendedSimpleEditor simpleEditor = null;
      if (editor instanceof IExtendedSimpleEditor) {
        simpleEditor = (IExtendedSimpleEditor) editor;
      }
      if (editor != null && simpleEditor == null) {
        final ISourceEditingTextTools tools = (ISourceEditingTextTools) editor.getAdapter(ISourceEditingTextTools.class);
        if (tools != null) {
          simpleEditor = new IExtendedSimpleEditor() {
            public int getCaretPosition() {
              return tools.getCaretOffset();
            }

            public IDocument getDocument() {
              return tools.getDocument();
            }

            public IEditorPart getEditorPart() {
              return tools.getEditorPart();
            }

            public Point getSelectionRange() {
              ITextSelection selection = tools.getSelection();
              return new Point(selection.getOffset(), selection.getOffset() + selection.getLength());
            }

          };
        }
      }
      return simpleEditor;
    }

    public void contributeToMenu(IMenuManager menu) {
      menuBar = menu;
      long time0 = System.currentTimeMillis();
      for (int i = 0; i < cache.size(); i++) {
        Object obj = cache.get(i);
        if (obj instanceof IConfigurationElement) {
          IConfigurationElement menuElement = (IConfigurationElement) obj;
          if ((menuElement.getName()).equals(TAG_MENU)) {
            contributeMenu(menuElement, menu, true);
            if (debugMenu)
              System.out.println(getClass().getName()
                  + "#contributeToMenu() added: " + menuElement.getAttribute(ATT_ID)); //$NON-NLS-1$
          }
        } else if (obj instanceof ActionDescriptor) {
          try {
            ActionDescriptor ad = (ActionDescriptor) obj;
            IMenuManager mm = contributeMenuAction(ad, menu, true, false);
            if (mm != null) {
              map.put(ad.getContributionItem(), mm);
              mm.addMenuListener(this);
              menus.add(mm);
              if (debugMenu)
                System.out.println(getClass().getName()
                    + "#contributeToMenu() added: " + ad.getId()); //$NON-NLS-1$
            }
          } catch (Exception e) {
            Logger.logException("contributing to menu", e); //$NON-NLS-1$
          }
        }
      }
      if (debugContributeTime)
        System.out.println(getClass().getName()
            + "#contributeToMenu(): ran in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void contributeToPopupMenu(IMenuManager menu) {
      long time0 = System.currentTimeMillis();
      for (int i = 0; i < cache.size(); i++) {
        Object obj = cache.get(i);
        if (obj instanceof IConfigurationElement) {
          IConfigurationElement menuElement = (IConfigurationElement) obj;
          if ((menuElement.getName()).equals(TAG_POPUPMENU)) {
            contributeMenu(menuElement, menu, true);
          }
        } else if (obj instanceof ActionDescriptor) {
          try {
            ActionDescriptor ad = (ActionDescriptor) obj;
            IAction a = ad.getAction();
            if (a instanceof IExtendedEditorAction) {
              // uncaught exceptions could cause the menu to not
              // be shown
              try {
                if (((ad.getPopupMenuPath() != null) || (ad.getPopupMenuGroup() != null))
                    && (a instanceof IExtendedEditorActionProxyForDelayLoading)) {
                  ((IExtendedEditorActionProxyForDelayLoading) a).realize();
                }

                IExtendedEditorAction eea = (IExtendedEditorAction) a;
                eea.setActiveExtendedEditor(activeExtendedEditor);
                eea.update();
                if (eea.isVisible()) {
                  IMenuManager parent = contributeMenuAction(ad, menu, true, true);
                  if (debugPopup && parent != null)
                    System.out.println(getClass().getName()
                        + "#contributeToPopupMenu() added: " + ad.getId()); //$NON-NLS-1$
                }
              } catch (Exception e) {
                Logger.logException(e);
              }

            } else {
              IMenuManager parent = contributeMenuAction(ad, menu, true, true);
              if (debugPopup && parent != null)
                System.out.println(getClass().getName()
                    + "#contributeToPopupMenu() added: " + ad.getId()); //$NON-NLS-1$
            }
          } catch (Exception e) {
            Logger.logException("contributing to popup", e); //$NON-NLS-1$
          }
        }
      }
      if (debugContributeTime)
        System.out.println(getClass().getName()
            + "#contributeToPopupMenu(): ran in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void contributeToStatusLine(IStatusLineManager manager) {
      // nothing from here
    }

    public void contributeToToolBar(IToolBarManager manager) {
      long time0 = System.currentTimeMillis();
      for (int i = 0; i < cache.size(); i++) {
        Object obj = cache.get(i);
        if (obj instanceof ActionDescriptor) {
          try {
            ActionDescriptor ad = (ActionDescriptor) obj;
            IAction a = ad.getAction();
            if (a instanceof IExtendedEditorAction) {
              if (((ad.getToolbarPath() != null) || (ad.getToolbarGroup() != null))
                  && (a instanceof IExtendedEditorActionProxyForDelayLoading)) {
                ((IExtendedEditorActionProxyForDelayLoading) a).realize();
              }
              IExtendedEditorAction eea = (IExtendedEditorAction) a;
              eea.setActiveExtendedEditor(activeExtendedEditor);
              eea.update();
              if (eea.isVisible()) {
                boolean contributed = contributeToolbarAction(ad, manager, true);
                if (debugToolbar && contributed)
                  System.out.println(getClass().getName()
                      + "#contributeToToolBar() added: " + ad.getId()); //$NON-NLS-1$
              } else {
                if (debugToolbar)
                  System.out.println(getClass().getName()
                      + "#contributeToToolBar(): [skipped] " + ad.getId()); //$NON-NLS-1$
              }
            } else {
              boolean contributed = contributeToolbarAction(ad, manager, true);
              if (debugToolbar && contributed)
                System.out.println(getClass().getName()
                    + "#contributeToToolBar() added: " + ad.getId()); //$NON-NLS-1$
            }
          } catch (Exception e) {
            Logger.logException("contributing to toolbar", e); //$NON-NLS-1$
          }
        }
      }
      if (debugContributeTime)
        System.out.println(getClass().getName()
            + "#contributeToToolBar(): ran in " + (System.currentTimeMillis() - time0) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void dispose() {
      Iterator it = menus.iterator();
      while (it.hasNext()) {
        Object o = it.next();
        if (o instanceof IMenuManager) {
          ((IMenuManager) o).removeMenuListener(this);
        }
      }
    }

    public void init(IActionBars bars, IWorkbenchPage page) {
      // nothing from here
    }

    public void menuAboutToShow(IMenuManager menu) {
      // slows down the menu and interferes with it for other editors;
      // optimize on visibility
      IEditorSite site = null;
      boolean activeEditorIsVisible = false;

      if (activeExtendedEditor != null && activeExtendedEditor.getEditorPart() != null)
        site = activeExtendedEditor.getEditorPart().getEditorSite();
      if (site == null)
        return;

      // Eclipse bug 48784 - [MPE] ClassCast exception Workbench page
      // isPartVisiable for MultiPageSite
      if (site instanceof MultiPageEditorSite) {
        Object multiPageEditor = ((MultiPageEditorSite) site).getMultiPageEditor();
        activeEditorIsVisible = multiPageEditor.equals(site.getPage().getActiveEditor())
            || multiPageEditor.equals(site.getPage().getActivePart());
      } else {
        activeEditorIsVisible = site.getWorkbenchWindow().getPartService().getActivePart().equals(
            activeExtendedEditor.getEditorPart());
      }
      // due to a delay class loading, don't return now
//			if (!activeEditorIsVisible)
//				return;

      IContributionItem[] items = menu.getItems();
      if (items == null || items.length == 0)
        return;

      for (int i = 0; i < items.length; ++i) {
        // add menu listener to submenu
        if (items[i] instanceof IMenuManager) {
          ((IMenuManager) items[i]).addMenuListener(this);
          menus.add(items[i]);
        }
      }

      Set keys = map.keySet();
      Iterator it = keys.iterator();
      boolean needActionContributionItemUpdate = false;
      while (it.hasNext()) {
        IContributionItem item = (IContributionItem) it.next();
        IMenuManager mm = (IMenuManager) map.get(item);
        if (menu.getId() != null && menu.getId().equals(mm.getId())
            && item instanceof ActionContributionItem) {
          try {
            IAction action = ((ActionContributionItem) item).getAction();

            if (action instanceof IExtendedEditorActionProxyForDelayLoading) {
              IExtendedEditorActionProxyForDelayLoading eea = (IExtendedEditorActionProxyForDelayLoading) action;
              if (eea.isBundleActive() == true && eea.isRealized() == false) {
                eea.realize();
                needActionContributionItemUpdate = true;
              }
            }

            if (activeEditorIsVisible || needActionContributionItemUpdate) {
              if (action instanceof IUpdate) {
                ((IUpdate) action).update();
              }
            }

            if (activeEditorIsVisible || needActionContributionItemUpdate) {
              boolean visible = true;
              if (action instanceof IExtendedEditorAction) {
                visible = ((IExtendedEditorAction) action).isVisible();
              }
              item.setVisible(visible);
            }

            if (needActionContributionItemUpdate) {
              ((ActionContributionItem) item).update();
            }

          } catch (Exception e) {
            Logger.logException("updating actions", e); //$NON-NLS-1$
          }
        }
      }
      if (activeEditorIsVisible || needActionContributionItemUpdate) {
        if (needActionContributionItemUpdate) {
          // the action is realized so that need to update the menu w/
          // force set to true
          menu.update(true);
        } else {
          menu.update(false);
        }
      }
    }

    public void setActiveEditor(IEditorPart editor) {
      activeExtendedEditor = computeExtendedEditor(editor);
      IKeyBindingService svc = (editor != null) ? editor.getEditorSite().getKeyBindingService()
          : null;
      for (int i = 0; i < cache.size(); i++) {
        Object obj = cache.get(i);
        if (obj instanceof ActionDescriptor) {
          ActionDescriptor ad = (ActionDescriptor) obj;
          try {
            IAction action = ad.getAction();
            if (action instanceof IExtendedEditorAction) {
              ((IExtendedEditorAction) action).setActiveExtendedEditor(activeExtendedEditor);
              ((IExtendedEditorAction) action).update();
              // update visibility right now so that the menu
              // will show/hide properly
              if (!((IExtendedEditorAction) action).isVisible() && ad.getContributionItem() != null)
                ad.getContributionItem().setVisible(false);
              if (svc != null && action.getActionDefinitionId() != null) {
                svc.registerAction(action);
              }
            }
          } catch (Exception e) {
            Logger.logException("setting active editor on actions", e); //$NON-NLS-1$
          }
        }
      }

      if (menuBar != null && editor != null) {
        // Class clz = editor.getClass();
        // while (clz != null) {
        // if (clz.getName().equals(targetID)) {
        // contributeToMenu(menuBar);
        // break;
        // }
        // clz = clz.getSuperclass();
        // }
        if (targetIDs.contains(editor.getEditorSite().getId())) {
          contributeToMenu(menuBar);
        }
      }

      updateToolbarActions();
    }

    public void updateToolbarActions() {
      for (int i = 0; i < cache.size(); i++) {
        Object obj = cache.get(i);
        if (obj instanceof ActionDescriptor) {
          try {
            ActionDescriptor ad = (ActionDescriptor) obj;
            if (ad.getToolbarPath() != null) {
              IAction action = ad.getAction();
              if (action instanceof IUpdate) {
                ((IUpdate) action).update();
              }
            }
          } catch (Exception e) {
            Logger.logException("updating toolbar actions", e); //$NON-NLS-1$
          }
        }
      }
    }
  }

  public static final String ATT_ID = "id"; //$NON-NLS-1$
  public static final String ATT_LABEL = "label"; //$NON-NLS-1$
  public static final String ATT_NAME = "name"; //$NON-NLS-1$
  public static final String ATT_PATH = "path"; //$NON-NLS-1$

  public static final String ATT_TARGET_ID = "targetID"; //$NON-NLS-1$
  protected final static boolean debugContributeTime = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.ui/extendededitoractionbuilder/contributetime")); //$NON-NLS-1$  //$NON-NLS-2$

  protected final static boolean debugMenu = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.ui/extendededitoractionbuilder/debugmenu")); //$NON-NLS-1$  //$NON-NLS-2$;
  protected final static boolean debugPopup = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.ui/extendededitoractionbuilder/debugpopup")); //$NON-NLS-1$  //$NON-NLS-2$;
  protected final static boolean debugReadTime = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.ui/extendededitoractionbuilder/readtime")); //$NON-NLS-1$  //$NON-NLS-2$
  protected final static boolean debugToolbar = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.ui/extendededitoractionbuilder/debugtoolbar")); //$NON-NLS-1$  //$NON-NLS-2$;

  private static final String EXTENDED_EDITOR = "extendedEditor"; //$NON-NLS-1$

  public static final String PL_EXTENDED_EDITOR_ACTIONS = "extendedEditorActions"; //$NON-NLS-1$

  public static final String PLUGIN_ID = "org.eclipse.wst.sse.ui"; //$NON-NLS-1$
  public static final String TAG_ACTION = "action"; //$NON-NLS-1$

  public static final String TAG_CONTRIBUTION_TYPE = "editorContribution"; //$NON-NLS-1$

  public static final String TAG_MENU = "menu"; //$NON-NLS-1$
  public static final String TAG_POPUPMENU = "popupmenu"; //$NON-NLS-1$
  public static final String TAG_RULERMENU = "rulermenu"; //$NON-NLS-1$
  public static final String TAG_SEPARATOR = "separator"; //$NON-NLS-1$

  protected List readingCache;

  protected String targetContributionTag;
  protected List targetIDs;

  /**
   * The constructor.
   */
  public ExtendedEditorActionBuilder() {
    super();
  }

  /**
   * Creates a menu from the information in the menu configuration element and adds it into the
   * provided menu manager. If 'appendIfMissing' is true, and menu path slot is not found, it will
   * be created and menu will be added into it. Otherwise, add operation will fail.
   */
  protected void contributeMenu(IConfigurationElement menuElement, IMenuManager mng,
      boolean appendIfMissing) {
    // Get config data.
    String id = menuElement.getAttribute(ATT_ID);
    String label = menuElement.getAttribute(ATT_LABEL);
    String path = menuElement.getAttribute(ATT_PATH);
    if (label == null) {
      Logger.log(Logger.ERROR, "Invalid Menu Extension (label == null): " + id); //$NON-NLS-1$
      return;
    }

    // Calculate menu path and group.
    String group = null;
    if (path != null) {
      int loc = path.lastIndexOf('/');
      if (loc != -1) {
        group = path.substring(loc + 1);
        path = path.substring(0, loc);
      } else {
        // assume that path represents a slot
        // so actual path portion should be null
        group = path;
        path = null;
      }
    }

    // Find parent menu.
    IMenuManager parent = mng;
    if (path != null) {
      parent = mng.findMenuUsingPath(path);
      if (parent == null) {
        // Logger.log("Invalid Menu Extension (Path is invalid): " +
        // id);//$NON-NLS-1$
        return;
      }
      // IMenuManager.findMenuUsingPath() returns invisible menu item if
      // the manager can't find
      // the specified path and create new MenuManager for it.
      // I don't know this is a specification or bug.
      // Anyway, to ensure the menu can be visible, setVisible(true)
      // needs to be called.
      parent.setVisible(true);
    }

    // Find reference group.
    if (group == null)
      group = IWorkbenchActionConstants.MB_ADDITIONS;
    IContributionItem sep = parent.find(group);
    if (sep == null) {
      if (appendIfMissing)
        parent.add(new Separator(group));
      else {
        Logger.log(Logger.ERROR, "Invalid Menu Extension (Group is invalid): " + id); //$NON-NLS-1$
        return;
      }
    }

    // If the menu does not exist create it.
    IMenuManager newMenu = parent.findMenuUsingPath(id);
    if (newMenu == null)
      newMenu = new MenuManager(label, id);

    // Create separators.
    IConfigurationElement[] children = menuElement.getChildren(TAG_SEPARATOR);
    for (int i = 0; i < children.length; i++) {
      contributeSeparator(newMenu, children[i]);
    }

    // Add new menu
    try {
      parent.insertAfter(group, newMenu);
    } catch (IllegalArgumentException e) {
      Logger.log(Logger.ERROR, "Invalid Menu Extension (Group is missing): " + id); //$NON-NLS-1$
    }
  }

  /**
   * Contributes action from action descriptor into the provided menu manager.
   */
  protected IMenuManager contributeMenuAction(ActionDescriptor ad, IMenuManager menu,
      boolean appendIfMissing, boolean popupmenu) {
    if (ad.getContributionItem() == null || ad.getAction() == null)
      return null;

    // Get config data.
    String mpath = popupmenu ? ad.getPopupMenuPath() : ad.getMenuPath();
    String mgroup = popupmenu ? ad.getPopupMenuGroup() : ad.getMenuGroup();
    if (mpath == null && mgroup == null)
      return null;

    // Find parent menu.
    IMenuManager parent = menu;
    if (mpath != null) {
      parent = parent.findMenuUsingPath(mpath);
      if (parent == null) {
        // Logger.log("Invalid Menu Extension (Path is invalid): " +
        // ad.getId()); //$NON-NLS-1$
        return null;
      }
      // IMenuManager.findMenuUsingPath() returns invisible menu item if
      // the manager can't find
      // the specified path and create new MenuManager for it.
      // I don't know this is a specification or bug.
      // Anyway, to ensure the menu can be visible, setVisible(true)
      // needs to be called.
      parent.setVisible(true);
    }

    // First remove existing menu item
    IContributionItem item = parent.find(ad.getId());
    if (item != null) {
      parent.remove(ad.getId());
    }

    // Find reference group.
    if (mgroup == null)
      mgroup = IWorkbenchActionConstants.MB_ADDITIONS;
    IContributionItem sep = parent.find(mgroup);
    if (sep == null) {
      if (appendIfMissing)
        parent.add(sep = new Separator(mgroup));
      else {
        Logger.log(Logger.ERROR, "Invalid Menu Extension (Group is invalid): " + ad.getId()); //$NON-NLS-1$
        return null;
      }
    }

    // Add action.
    try {
      if (popupmenu) {
        // Context menu need a newly created contribution item
        if (sep != null && sep.isGroupMarker())
          parent.appendToGroup(sep.getId(), ad.getAction());
        else
          parent.insertAfter(mgroup, ad.getAction());
      } else {
        // Normal menu need to add existing contribution item to
        // remove it from menu listener
        if (sep != null && sep.isGroupMarker())
          parent.appendToGroup(sep.getId(), ad.getContributionItem());
        else
          parent.insertAfter(mgroup, ad.getContributionItem());
      }
    } catch (IllegalArgumentException e) {
      Logger.log(Logger.ERROR, "Invalid Menu Extension (Group is missing): " + ad.getId()); //$NON-NLS-1$
      parent = null;
    }

    return parent;
  }

  /**
   * Creates a named menu separator from the information in the configuration element. If the
   * separator already exists do not create a second.
   */
  protected boolean contributeSeparator(IMenuManager menu, IConfigurationElement element) {
    String id = element.getAttribute(ATT_NAME);
    if (id == null || id.length() <= 0)
      return false;
    IContributionItem sep = menu.find(id);
    if (sep != null)
      return false;
    menu.add(new Separator(id));
    return true;
  }

  /**
   * Contributes action from the action descriptor into the provided tool bar manager.
   */
  protected boolean contributeToolbarAction(ActionDescriptor ad, IToolBarManager toolbar,
      boolean appendIfMissing) {
    if (ad.getContributionItem() == null || ad.getAction() == null)
      return false;

    // Get config data.
    String tpath = ad.getToolbarPath();
    String tgroup = ad.getToolbarGroup();
    if (tpath == null && tgroup == null)
      return false;

    // First remove existing toolbar item
    IContributionItem item = toolbar.find(ad.getId());
    if (item != null) {
      toolbar.remove(ad.getId());
    }

    // Find reference group.
    if (tgroup == null)
      tgroup = IWorkbenchActionConstants.MB_ADDITIONS;
    IContributionItem sep = toolbar.find(tgroup);
    if (sep == null) {
      if (appendIfMissing)
        toolbar.add(new Separator(tgroup));
      else {
        Logger.log(Logger.ERROR, "Invalid Toolbar Extension (Group is invalid): " + ad.getId()); //$NON-NLS-1$
        return false;
      }
    }

    // Add action to tool bar.
    try {
      if (sep != null && sep.isGroupMarker())
        toolbar.appendToGroup(sep.getId(), ad.getAction());
      else
        toolbar.insertAfter(tgroup, ad.getAction());
    } catch (IllegalArgumentException e) {
      Logger.log(Logger.ERROR, "Invalid Toolbar Extension (Group is missing): " + ad.getId()); //$NON-NLS-1$
      return false;
    }
    return true;
  }

  /**
   * This factory method returns a new ActionDescriptor for the configuration element. It should be
   * implemented by subclasses.
   */
  protected ActionDescriptor createActionDescriptor(IConfigurationElement element) {
    ActionDescriptor ad = null;
    try {
      ad = new ActionDescriptor(element);
      // these cases like "class not found" are handled
      // at lower level, so no action if formed. In that
      // case, we also don't want to form an action descriptor.
      if ((ad != null) && (ad.getAction() == null)) {
        ad = null;
      }
    } catch (Exception e) {
      Logger.traceException(EXTENDED_EDITOR, e);
      ad = null;
    }
    return ad;
  }

  /**
   * Returns the name of the part ID attribute that is expected in the target extension.
   */
  protected String getTargetID(IConfigurationElement element) {
    String value = element.getAttribute(ATT_TARGET_ID);
    return value != null ? value : "???"; //$NON-NLS-1$
  }

  /**
   * Reads editor contributor if specified directly in the 'editor' extension point, and all
   * external contributions for this editor's ID registered in 'editorActions' extension point.
   */
  public IExtendedContributor readActionExtensions(String editorId) {
    return readActionExtensions(new String[] {editorId});
  }

  /**
   * Reads editor contributor if specified directly in the 'editor' extension point, and all
   * external contributions for this editor's ID registered in 'editorActions' extension point.
   */
  public IExtendedContributor readActionExtensions(String[] ids) {
    long time0 = System.currentTimeMillis();
    ExtendedContributor ext = null;
    readContributions(ids, TAG_CONTRIBUTION_TYPE, PL_EXTENDED_EDITOR_ACTIONS);
    if (debugReadTime) {
      String idlist = ""; //$NON-NLS-1$
      if (ids.length > 0) {
        for (int i = 0; i < ids.length; i++) {
          idlist += ids[i];
          if (i < ids.length - 1)
            idlist += ","; //$NON-NLS-1$
        }
      }
      System.out.println(getClass().getName()
          + "#readActionExtensions(" + idlist + "): read in " + (System.currentTimeMillis() - time0) + "ms [" + (readingCache != null ? readingCache.size() : 0) + " contributions]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
    if (readingCache != null) {
      ext = new ExtendedContributor(readingCache);
      readingCache = null;
    }
    return ext;
  }

  /**
   * Reads the contributions from the registry for the provided workbench part and the provided
   * extension point IDs.
   */
  protected void readContributions(String[] ids, String tag, String extensionPoint) {
    readingCache = null;
    targetIDs = Arrays.asList(ids);
    targetContributionTag = tag;
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    readRegistry(registry, PLUGIN_ID, extensionPoint);
  }

  /**
   * Implements abstract method to handle the provided XML element in the registry.
   */
  protected boolean readElement(IConfigurationElement element) {
    String tag = element.getName();
    if (tag.equals(targetContributionTag)) {
      String id = getTargetID(element);
      if (id == null || !targetIDs.contains(id)) {
        // This is not of interest to us - don't go deeper
        return true;
      }
    } else if (tag.equals(TAG_MENU)) {
      if (readingCache == null)
        readingCache = new ArrayList();
      readingCache.add(element);
      return true; // just cache the element - don't go into it
    } else if (tag.equals(TAG_POPUPMENU)) {
      if (readingCache == null)
        readingCache = new ArrayList();
      readingCache.add(element);
      return true; // just cache the element - don't go into it
    } else if (tag.equals(TAG_RULERMENU)) {
      if (readingCache == null)
        readingCache = new ArrayList();
      readingCache.add(element);
      return true; // just cache the element - don't go into it
    } else if (tag.equals(TAG_ACTION)) {
      if (readingCache == null)
        readingCache = new ArrayList();
      ActionDescriptor ad = createActionDescriptor(element);
      if (ad != null)
        readingCache.add(ad);
      return true; // just cache the action - don't go into
    } else {
      return false;
    }

    readElementChildren(element);
    return true;
  }
}
