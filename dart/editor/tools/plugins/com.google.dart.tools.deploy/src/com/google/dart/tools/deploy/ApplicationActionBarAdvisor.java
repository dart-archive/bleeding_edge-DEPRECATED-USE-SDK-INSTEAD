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
package com.google.dart.tools.deploy;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.debug.ui.internal.view.BreakpointsView;
import com.google.dart.tools.debug.ui.internal.view.DebuggerView;
import com.google.dart.tools.debug.ui.launch.DartRunAction;
import com.google.dart.tools.debug.ui.launch.StopPubServeAction;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.actions.AboutDartAction;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.actions.OpenAction_NEW;
import com.google.dart.tools.ui.actions.OpenAction_OLD;
import com.google.dart.tools.ui.actions.OpenApiDocsAction;
import com.google.dart.tools.ui.actions.OpenIntroEditorAction;
import com.google.dart.tools.ui.actions.OpenNewFolderWizardAction;
import com.google.dart.tools.ui.actions.OpenOnlineDocsAction;
import com.google.dart.tools.ui.actions.OpenTutorialAction;
import com.google.dart.tools.ui.actions.RunPubAction;
import com.google.dart.tools.ui.actions.RunPublishAction;
import com.google.dart.tools.ui.actions.ShowInFinderAction;
import com.google.dart.tools.ui.build.CleanLibrariesAction;
import com.google.dart.tools.ui.console.DartConsoleView;
import com.google.dart.tools.ui.internal.handlers.NewFileHandler;
import com.google.dart.tools.ui.internal.handlers.NewFileHandler.NewFileCommandAction;
import com.google.dart.tools.ui.internal.handlers.OpenFolderHandler;
import com.google.dart.tools.ui.internal.projects.OpenNewApplicationWizardAction;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.internal.provisional.action.IToolBarContributionItem;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.NewWizardMenu;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.ide.IIDEActionConstants;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.ShowViewAction;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.actions.QuickMenuAction;
import org.eclipse.ui.internal.provisional.application.IActionBarConfigurer2;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.ui.views.IViewDescriptor;

/**
 * Adds actions to a workbench window.
 */
@SuppressWarnings({"restriction", "deprecation"})
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {
  /**
   * Subclass of {@link ShowViewAction} to make protected constructor accessible.
   */
  private static class AccessibleShowViewAction extends ShowViewAction {
    protected AccessibleShowViewAction(IWorkbenchWindow window, IViewDescriptor desc,
        boolean makeFast) {
      super(window, desc, makeFast);
    }
  }

  private class ShowPropertiesAction extends Action implements IWorkbenchAction,
      ISelectionListener, ISelectionChangedListener, ISelectionProvider {

    private ISelection selection;

    private final PropertyDialogAction workbenchAction = new PropertyDialogAction(getWindow(), this);

    private ShowPropertiesAction() {
      setId(IWorkbenchCommandConstants.FILE_PROPERTIES);
      setText(org.eclipse.ui.internal.WorkbenchMessages.PropertyDialog_text);
      setDescription(org.eclipse.ui.internal.WorkbenchMessages.PropertyDialog_text);
      setToolTipText(org.eclipse.ui.internal.WorkbenchMessages.PropertyDialog_toolTip);
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
      // Ignore
    }

    @Override
    public void dispose() {
      workbenchAction.dispose();
    }

    @Override
    public ISelection getSelection() {
      return selection;
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
      // Ignore
    }

    @Override
    public void run() {
      workbenchAction.run();
    }

    @Override
    public void runWithEvent(Event event) {
      run();
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
      updateEnablement(selection);
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      ISelection selection = event.getSelection();
      updateEnablement(selection);
    }

    @Override
    public void setSelection(ISelection selection) {
      this.selection = selection;
    }

    private void updateEnablement(ISelection selection) {
      setSelection(selection);
      boolean enabled = false;
      if (selection instanceof IStructuredSelection) {
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        enabled = structuredSelection.size() == 1
            && structuredSelection.getFirstElement() instanceof IResource;
      }
      setEnabled(enabled);
    }
  }

  private final IWorkbenchWindow window;

  // generic actions
  private IWorkbenchAction closeAction;

  private IWorkbenchAction closeAllAction;

  private IWorkbenchAction closeOthersAction;

  private IWorkbenchAction closeAllSavedAction;

  private IWorkbenchAction saveAction;

  private IWorkbenchAction saveAllAction;

  private IWorkbenchAction newWindowAction;

  private IWorkbenchAction newEditorAction;

  private IWorkbenchAction helpContentsAction;

  private IWorkbenchAction helpSearchAction;

  private IWorkbenchAction aboutAction;

  //private IWorkbenchAction saveAsAction;

  private IWorkbenchAction openPreferencesAction;

  private IWorkbenchAction hideShowEditorAction;

  private IWorkbenchAction lockToolBarAction;

  private IWorkbenchAction showViewMenuAction;

  private IWorkbenchAction showPartPaneMenuAction;

  private IWorkbenchAction nextPartAction;

  private IWorkbenchAction prevPartAction;

  private IWorkbenchAction nextEditorAction;

  private IWorkbenchAction prevEditorAction;

  private IWorkbenchAction activateEditorAction;

  private IWorkbenchAction maximizePartAction;

  private IWorkbenchAction minimizePartAction;

  private IWorkbenchAction switchToEditorAction;

  private IWorkbenchAction workbookEditorsAction;

  private IWorkbenchAction quickAccessAction;

  private IWorkbenchAction backwardHistoryAction;

  private IWorkbenchAction forwardHistoryAction;

  // generic retarget actions
  private IWorkbenchAction undoAction;

  private IWorkbenchAction redoAction;

  private IWorkbenchAction copyAction;

  private IWorkbenchAction pasteAction;

  private IWorkbenchAction quitAction;

  private IWorkbenchAction goIntoAction;

  // IDE-specific actions
//  private IAction newWizardAction;

  private IWorkbenchAction upAction;

  private DartRunAction dartRunAction;

  private RunPubAction pubInstallAction;

  private RunPubAction pubUpdateAction;

//  private OrganizeImportsAction organizeImportsAction;

  private RunPubAction pubDeployAction;

  private IWorkbenchAction importResourcesAction;

  private IWorkbenchAction exportResourcesAction;

  private IWorkbenchAction cleanAllAction;

  private QuickMenuAction showInQuickMenu;

  private QuickMenuAction newQuickMenu;

  private IWorkbenchAction refreshAction;

  // IDE-specific retarget actions
  //private CommandContributionItem minimizeItem;

  //private CommandContributionItem zoomItem;

  //private CommandContributionItem arrangeWindowsItem;

  // contribution items
  // @issue should obtain from ContributionItemFactory

  private ShowInFinderAction showInFinderAction;

  // @issue class is workbench internal
  private StatusLineContributionItem statusLineItem;

  private Preferences.IPropertyChangeListener prefListener;

  // listener for the "close editors automatically"
  // preference change
  private IPropertyChangeListener propPrefListener;

  private IPageListener pageListener;

  private IResourceChangeListener resourceListener;

//  /**
//   * The coolbar context menu manager.
//   */
//  private MenuManager coolbarPopupMenuManager;

  /**
   * Indicates if the action builder has been disposed
   */
  private boolean isDisposed = false;

  private final WorkbenchActionFactory actionFactory;
  private IWorkbenchAction newApplicationWizardAction;
  private OpenOnlineDocsAction openOnlineDocsAction;
  private OpenTutorialAction openTutorialAction;

  private OpenApiDocsAction openApiDocsAction;

  private NewFileCommandAction newFileAction;

  private RunPublishAction pubPublishAction;

  private ShowPropertiesAction showPropertiesAction;

  private StopPubServeAction stopPubServeAction;

  /**
   * Constructs a new action builder which contributes actions to the given window.
   * 
   * @param configurer the action bar configurer for the window
   */
  public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
    super(configurer);
    window = configurer.getWindowConfigurer().getWindow();
    actionFactory = new WorkbenchActionFactory(window);
  }

  /**
   * Disposes any resources and unhooks any listeners that are no longer needed. Called when the
   * window is closed.
   */
  @Override
  public void dispose() {
    if (isDisposed) {
      return;
    }
    isDisposed = true;

//    IMenuService menuService = (IMenuService) window.getService(IMenuService.class);
//    menuService.releaseContributions(coolbarPopupMenuManager);
//    coolbarPopupMenuManager.dispose();

    getActionBarConfigurer().getStatusLineManager().remove(statusLineItem);
    if (pageListener != null) {
      window.removePageListener(pageListener);
      pageListener = null;
    }
    if (prefListener != null) {
      ResourcesPlugin.getPlugin().getPluginPreferences().removePropertyChangeListener(prefListener);
      prefListener = null;
    }
    if (propPrefListener != null) {
      WorkbenchPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(
          propPrefListener);
      propPrefListener = null;
    }
    if (resourceListener != null) {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
      resourceListener = null;
    }

    showInQuickMenu.dispose();
    newQuickMenu.dispose();

    // null out actions to make leak debugging easier
    closeAction = null;
    closeAllAction = null;
    closeAllSavedAction = null;
    closeOthersAction = null;
    saveAction = null;
    saveAllAction = null;
    newWindowAction = null;
    newEditorAction = null;
    helpContentsAction = null;
    helpSearchAction = null;
    aboutAction = null;
    openPreferencesAction = null;
    //saveAsAction = null;
    hideShowEditorAction = null;
    lockToolBarAction = null;
    showViewMenuAction = null;
    showPartPaneMenuAction = null;
    nextPartAction = null;
    prevPartAction = null;
    nextEditorAction = null;
    prevEditorAction = null;
    activateEditorAction = null;
    maximizePartAction = null;
    minimizePartAction = null;
    switchToEditorAction = null;
    quickAccessAction.dispose();
    quickAccessAction = null;
    backwardHistoryAction = null;
    forwardHistoryAction = null;
    undoAction = null;
    redoAction = null;
    quitAction = null;
    goIntoAction = null;
    upAction = null;
    //    newWizardAction = null;
//    newWizardDropDownAction = null;
    importResourcesAction = null;
    exportResourcesAction = null;
    cleanAllAction = null;
    showInQuickMenu = null;
    newQuickMenu = null;
    statusLineItem = null;
    prefListener = null;
    propPrefListener = null;
    openOnlineDocsAction = null;
    openTutorialAction = null;
    openApiDocsAction = null;
    refreshAction = null;
    showPropertiesAction = null;
    //minimizeItem = null;
    //zoomItem = null;
    //arrangeWindowsItem = null;
    super.dispose();
  }

  @Override
  public void fillActionBars(int flags) {
    super.fillActionBars(flags);
    updateProjectStateDependentActions(true);
    if ((flags & FILL_PROXY) == 0) {
      hookListeners();
    }
  }

  /**
   * Returns true if the menu with the given ID should be considered as an OLE container menu.
   * Container menus are preserved in OLE menu merging.
   */
  @Override
  public boolean isApplicationMenu(String menuId) {
    if (menuId.equals(IWorkbenchActionConstants.M_FILE)) {
      return true;
    }
    if (menuId.equals(IWorkbenchActionConstants.M_WINDOW)) {
      return true;
    }
    return false;
  }

  /**
   * Return whether or not given id matches the id of the coolitems that the workbench creates.
   */
  public boolean isWorkbenchCoolItemId(String id) {
    if (IWorkbenchActionConstants.TOOLBAR_FILE.equalsIgnoreCase(id)) {
      return true;
    }
    if (IWorkbenchActionConstants.TOOLBAR_NAVIGATE.equalsIgnoreCase(id)) {
      return true;
    }
    return false;
  }

  /**
   * Fills the coolbar with the workbench actions.
   */
  @Override
  protected void fillCoolBar(ICoolBarManager coolBar) {

    IActionBarConfigurer2 actionBarConfigurer = (IActionBarConfigurer2) getActionBarConfigurer();

//    {
//      // Set up the context Menu
//      coolbarPopupMenuManager = new MenuManager();
//      coolbarPopupMenuManager.add(new ActionContributionItem(lockToolBarAction));
//      coolBar.setContextMenuManager(coolbarPopupMenuManager);
//      IMenuService menuService = (IMenuService) window.getService(IMenuService.class);
//      menuService.populateContributionManager(coolbarPopupMenuManager,
//          "popup:windowCoolbarContextMenu"); //$NON-NLS-1$
//    }

    coolBar.add(new GroupMarker(IIDEActionConstants.GROUP_FILE));
    { // File Group
      IToolBarManager fileToolBar = actionBarConfigurer.createToolBarManager();
      fileToolBar.add(newApplicationWizardAction);
      fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
      fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.SAVE_GROUP));
      fileToolBar.add(saveAction);
      fileToolBar.add(saveAllAction);
      fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.GROUP_APP));
      fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.SAVE_EXT));
      fileToolBar.add(new Separator(IWorkbenchActionConstants.BUILD_GROUP));
      fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.BUILD_EXT));
      fileToolBar.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

      // Add to the cool bar manager
      coolBar.add(actionBarConfigurer.createToolBarContributionItem(
          fileToolBar,
          IWorkbenchActionConstants.TOOLBAR_FILE));
    }

    //   coolBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    coolBar.add(new GroupMarker(IIDEActionConstants.GROUP_NAV));
    { // Navigate group
      IToolBarManager navToolBar = actionBarConfigurer.createToolBarManager();
      navToolBar.add(new Separator(IWorkbenchActionConstants.HISTORY_GROUP));
      navToolBar.add(new GroupMarker(IWorkbenchActionConstants.GROUP_APP));
      navToolBar.add(backwardHistoryAction);
      navToolBar.add(forwardHistoryAction);
      navToolBar.add(new Separator(IWorkbenchActionConstants.PIN_GROUP));
      navToolBar.add(actionFactory.getPinEditorItem());

      // Add to the cool bar manager
      coolBar.add(actionBarConfigurer.createToolBarContributionItem(
          navToolBar,
          IWorkbenchActionConstants.TOOLBAR_NAVIGATE));
    }

    //   coolBar.add(new GroupMarker(IWorkbenchActionConstants.GROUP_EDITOR));

    coolBar.add(new GroupMarker(IWorkbenchActionConstants.GROUP_HELP));

    { // Help group
      IToolBarManager helpToolBar = actionBarConfigurer.createToolBarManager();
      helpToolBar.add(new Separator(IWorkbenchActionConstants.GROUP_HELP));
//            helpToolBar.add(searchComboItem);
      // Add the group for applications to contribute
      helpToolBar.add(new GroupMarker(IWorkbenchActionConstants.GROUP_APP));

      helpToolBar.add(dartRunAction);

      // Add to the cool bar manager
      coolBar.add(actionBarConfigurer.createToolBarContributionItem(
          helpToolBar,
          IWorkbenchActionConstants.TOOLBAR_HELP));
    }
  }

  /**
   * Fills the menu bar with the workbench actions.
   */
  @Override
  protected void fillMenuBar(IMenuManager menuBar) {
    menuBar.add(createFileMenu());
    menuBar.add(createEditMenu());
    menuBar.add(createNavigateMenu());
    //menuBar.add(createBuildMenu());
    menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    //menuBar.add(createViewMenu());
    menuBar.add(createToolsMenu());
    menuBar.add(createHelpMenu());
  }

  /**
   * Fills the status line with the workbench contribution items.
   */
  @Override
  protected void fillStatusLine(IStatusLineManager statusLine) {
    statusLine.add(statusLineItem);
  }

  /**
   * Creates actions (and contribution items) for the menu bar, toolbar and status line.
   */
  @Override
  protected void makeActions(final IWorkbenchWindow window) {
    // @issue should obtain from ConfigurationItemFactory
    statusLineItem = new StatusLineContributionItem("ModeContributionItem"); //$NON-NLS-1$

    dartRunAction = new DartRunAction(window);

    pubInstallAction = RunPubAction.createPubInstallAction(window);

    pubUpdateAction = RunPubAction.createPubUpdateAction(window);

    pubPublishAction = RunPublishAction.createPubPublishAction(window);

    pubDeployAction = RunPubAction.createPubDeployAction(window);

    stopPubServeAction = new StopPubServeAction();

//    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
//      // TODO(scheglov)
//    } else {
//      organizeImportsAction = new OrganizeImportsAction(window);
//    }

    newApplicationWizardAction = new OpenNewApplicationWizardAction();

    register(newApplicationWizardAction);

    importResourcesAction = ActionFactory.IMPORT.create(window);
    register(importResourcesAction);

    exportResourcesAction = ActionFactory.EXPORT.create(window);
    register(exportResourcesAction);

    cleanAllAction = new CleanLibrariesAction(window);
    register(cleanAllAction);

    saveAction = ActionFactory.SAVE.create(window);
    register(saveAction);

    //TODO (keertip) : re-enable when we have save as implemented
//    saveAsAction = ActionFactory.SAVE_AS.create(window);
//    register(saveAsAction);

    saveAllAction = ActionFactory.SAVE_ALL.create(window);
    register(saveAllAction);

    refreshAction = ActionFactory.REFRESH.create(window);
    register(refreshAction);

    showInFinderAction = ShowInFinderAction.getInstance(window);
    register(showInFinderAction);

    showPropertiesAction = new ShowPropertiesAction();
    register(showPropertiesAction);

    newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(getWindow());
    newWindowAction.setText(IDEWorkbenchMessages.Workbench_openNewWindow);
    register(newWindowAction);

    newEditorAction = ActionFactory.NEW_EDITOR.create(window);
    register(newEditorAction);

    copyAction = ActionFactory.COPY.create(window);
    register(copyAction);

    pasteAction = ActionFactory.PASTE.create(window);
    register(pasteAction);

    undoAction = ActionFactory.UNDO.create(window);
    register(undoAction);

    redoAction = ActionFactory.REDO.create(window);
    register(redoAction);

    closeAction = ActionFactory.CLOSE.create(window);
    register(closeAction);

    closeAllAction = ActionFactory.CLOSE_ALL.create(window);
    register(closeAllAction);

    closeOthersAction = ActionFactory.CLOSE_OTHERS.create(window);
    register(closeOthersAction);

    closeAllSavedAction = ActionFactory.CLOSE_ALL_SAVED.create(window);
    register(closeAllSavedAction);

    helpContentsAction = ActionFactory.HELP_CONTENTS.create(window);
    register(helpContentsAction);

    helpSearchAction = ActionFactory.HELP_SEARCH.create(window);
    register(helpSearchAction);

    aboutAction = new AboutDartAction(window);
    register(aboutAction);

    openPreferencesAction = ActionFactory.PREFERENCES.create(window);
    register(openPreferencesAction);

    makeFeatureDependentActions(window);

    // Actions for invisible accelerators
    showViewMenuAction = ActionFactory.SHOW_VIEW_MENU.create(window);
    register(showViewMenuAction);

    showPartPaneMenuAction = ActionFactory.SHOW_PART_PANE_MENU.create(window);
    register(showPartPaneMenuAction);

    activateEditorAction = ActionFactory.ACTIVATE_EDITOR.create(window);
    register(activateEditorAction);

    nextEditorAction = ActionFactory.NEXT_EDITOR.create(window);
    register(nextEditorAction);
    prevEditorAction = ActionFactory.PREVIOUS_EDITOR.create(window);
    register(prevEditorAction);
    ActionFactory.linkCycleActionPair(nextEditorAction, prevEditorAction);

    maximizePartAction = ActionFactory.MAXIMIZE.create(window);
    register(maximizePartAction);

    minimizePartAction = ActionFactory.MINIMIZE.create(window);
    register(minimizePartAction);

    switchToEditorAction = ActionFactory.SHOW_OPEN_EDITORS.create(window);
    register(switchToEditorAction);

    workbookEditorsAction = ActionFactory.SHOW_WORKBOOK_EDITORS.create(window);
    register(workbookEditorsAction);

    quickAccessAction = ActionFactory.SHOW_QUICK_ACCESS.create(window);

    hideShowEditorAction = ActionFactory.SHOW_EDITOR.create(window);
    register(hideShowEditorAction);
    lockToolBarAction = ActionFactory.LOCK_TOOL_BAR.create(window);
    register(lockToolBarAction);

    forwardHistoryAction = ActionFactory.FORWARD_HISTORY.create(window);
    register(forwardHistoryAction);

    backwardHistoryAction = ActionFactory.BACKWARD_HISTORY.create(window);
    register(backwardHistoryAction);

    quitAction = ActionFactory.QUIT.create(window);
    register(quitAction);

    goIntoAction = ActionFactory.GO_INTO.create(window);
    register(goIntoAction);

    upAction = ActionFactory.UP.create(window);
    register(upAction);

    String showInQuickMenuId = IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU;
    showInQuickMenu = new QuickMenuAction(showInQuickMenuId) {
      @Override
      protected void fillMenu(IMenuManager menu) {
        menu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(window));
      }
    };
    register(showInQuickMenu);

    final String newQuickMenuId = "org.eclipse.ui.file.newQuickMenu"; //$NON-NLS-1$
    newQuickMenu = new QuickMenuAction(newQuickMenuId) {
      @Override
      protected void fillMenu(IMenuManager menu) {
        menu.add(new NewWizardMenu(window));
      }
    };
    register(newQuickMenu);

    openOnlineDocsAction = new OpenOnlineDocsAction();
    register(openOnlineDocsAction);

    openTutorialAction = new OpenTutorialAction();
    register(openTutorialAction);

    openApiDocsAction = new OpenApiDocsAction();
    register(openApiDocsAction);

//    if (Util.isCocoa()) {
//
//      CommandContributionItemParameter minimizeParam = new CommandContributionItemParameter(window,
//          null, "org.eclipse.ui.cocoa.minimizeWindow", CommandContributionItem.STYLE_PUSH); //$NON-NLS-1$
//      minimizeItem = new CommandContributionItem(minimizeParam);
//      CommandContributionItemParameter zoomParam = new CommandContributionItemParameter(window,
//          null, "org.eclipse.ui.cocoa.zoomWindow", CommandContributionItem.STYLE_PUSH); //$NON-NLS-1$
//      zoomItem = new CommandContributionItem(zoomParam);
//      CommandContributionItemParameter arrangeWindowsParam = new CommandContributionItemParameter(
//          window, null,
//          "org.eclipse.ui.cocoa.arrangeWindowsInFront", CommandContributionItem.STYLE_PUSH); //$NON-NLS-1$
//      arrangeWindowsItem = new CommandContributionItem(arrangeWindowsParam);
//    }

  }

  void updateModeLine(final String text) {
    statusLineItem.setText(text);
  }

  /**
   * Update the pin action's tool bar
   */
  void updatePinActionToolbar() {

    ICoolBarManager coolBarManager = getActionBarConfigurer().getCoolBarManager();
    IContributionItem cbItem = coolBarManager.find(IWorkbenchActionConstants.TOOLBAR_NAVIGATE);
    if (!(cbItem instanceof IToolBarContributionItem)) {
      // This should not happen
      IDEWorkbenchPlugin.log("Navigation toolbar contribution item is missing"); //$NON-NLS-1$
      return;
    }
    IToolBarContributionItem toolBarItem = (IToolBarContributionItem) cbItem;
    IToolBarManager toolBarManager = toolBarItem.getToolBarManager();
    if (toolBarManager == null) {
      // error if this happens, navigation toolbar assumed to always exist
      IDEWorkbenchPlugin.log("Navigate toolbar is missing"); //$NON-NLS-1$
      return;
    }

    toolBarManager.update(false);
    toolBarItem.update(ICoolBarManager.SIZE);
  }

  /**
   * Update actions based on resource changes. Actions affected include the new file action, the
   * build actions on the toolbar and menu bar based on the current state of autobuild.
   * <p>
   * This method can be called from any thread.
   * 
   * @param immediately <code>true</code> to update the actions immediately, <code>false</code> to
   *          queue the update to be run in the event loop
   */
  void updateProjectStateDependentActions(boolean immediately) {
    Runnable update = new Runnable() {
      @Override
      public void run() {

        if (isDisposed) {
          return;
        }

        if (newFileAction != null) {
          newFileAction.updateEnablement();
        }

        //update the cool bar build button
        ICoolBarManager coolBarManager = getActionBarConfigurer().getCoolBarManager();
        IContributionItem cbItem = coolBarManager.find(IWorkbenchActionConstants.TOOLBAR_FILE);
        if (!(cbItem instanceof IToolBarContributionItem)) {
          // This should not happen
          IDEWorkbenchPlugin.log("File toolbar contribution item is missing"); //$NON-NLS-1$
          return;
        }
        IToolBarContributionItem toolBarItem = (IToolBarContributionItem) cbItem;
        IToolBarManager toolBarManager = toolBarItem.getToolBarManager();
        if (toolBarManager == null) {
          // error if this happens, file toolbar assumed to always exist
          IDEWorkbenchPlugin.log("File toolbar is missing"); //$NON-NLS-1$
          return;
        }
      }
    };
    if (immediately) {
      update.run();
    } else {
      // Dispatch the update to be run later in the UI thread.
      // This helps to reduce flicker if autobuild is being temporarily disabled programmatically.
      Shell shell = window.getShell();
      if (shell != null && !shell.isDisposed()) {
        shell.getDisplay().asyncExec(update);
      }
    }
  }

  /**
   * Adds the keyboard navigation submenu to the specified menu.
   */
  @SuppressWarnings("unused")
  private void addKeyboardShortcuts(MenuManager menu) {
    MenuManager subMenu = new MenuManager(IDEWorkbenchMessages.Workbench_shortcuts, "shortcuts"); //$NON-NLS-1$
    menu.add(subMenu);
    subMenu.add(showPartPaneMenuAction);
    subMenu.add(showViewMenuAction);
    subMenu.add(quickAccessAction);
    subMenu.add(new Separator());
    subMenu.add(maximizePartAction);
    subMenu.add(minimizePartAction);
    subMenu.add(new Separator());
    subMenu.add(activateEditorAction);
    subMenu.add(nextEditorAction);
    subMenu.add(prevEditorAction);
    subMenu.add(switchToEditorAction);
    subMenu.add(new Separator());
    subMenu.add(nextPartAction);
    subMenu.add(prevPartAction);
  }

  /**
   * Adds a <code>GroupMarker</code> or <code>Separator</code> to a menu. The test for whether a
   * separator should be added is done by checking for the existence of a preference matching the
   * string useSeparator.MENUID.GROUPID that is set to <code>true</code>.
   * 
   * @param menu the menu to add to
   * @param groupId the group id for the added separator or group marker
   */
  private void addSeparatorOrGroupMarker(MenuManager menu, String groupId) {
    String prefId = "useSeparator." + menu.getId() + "." + groupId; //$NON-NLS-1$ //$NON-NLS-2$
    boolean addExtraSeparators = IDEWorkbenchPlugin.getDefault().getPreferenceStore().getBoolean(
        prefId);
    if (addExtraSeparators) {
      menu.add(new Separator(groupId));
    } else {
      menu.add(new GroupMarker(groupId));
    }
  }

  private void addViewActions(MenuManager menu) {
    IViewDescriptor viewDesc;

    viewDesc = WorkbenchPlugin.getDefault().getViewRegistry().find(BreakpointsView.VIEW_ID);
    menu.add(new AccessibleShowViewAction(window, viewDesc, false));

    viewDesc = WorkbenchPlugin.getDefault().getViewRegistry().find(DebuggerView.ID);
    menu.add(new AccessibleShowViewAction(window, viewDesc, false));

    viewDesc = WorkbenchPlugin.getDefault().getViewRegistry().find(DartUI.ID_FILE_EXPLORER);
    menu.add(new AccessibleShowViewAction(window, viewDesc, false));

    viewDesc = WorkbenchPlugin.getDefault().getViewRegistry().find(IPageLayout.ID_OUTLINE);
    menu.add(new AccessibleShowViewAction(window, viewDesc, false));

    viewDesc = WorkbenchPlugin.getDefault().getViewRegistry().find(DartConsoleView.VIEW_ID);
    menu.add(new AccessibleShowViewAction(window, viewDesc, false));

    //optionally add AST view if it's available
    viewDesc = WorkbenchPlugin.getDefault().getViewRegistry().find(
        "com.google.dart.dev.util.ast.ASTExplorer"); //$NON-NLS-1$
    if (viewDesc != null) {
      menu.add(new AccessibleShowViewAction(window, viewDesc, false));
    }

    // optionally add Analysis view if it's available
    viewDesc = WorkbenchPlugin.getDefault().getViewRegistry().find(
        "com.google.dart.dev.util.analysis.AnalysisView"); //$NON-NLS-1$
    if (viewDesc != null) {
      menu.add(new AccessibleShowViewAction(window, viewDesc, false));
    }

    // optionally add SWT Leak view if it's available
    viewDesc = WorkbenchPlugin.getDefault().getViewRegistry().find(
        "org.eclipse.swt.tools.views.SleakView"); //$NON-NLS-1$
    if (viewDesc != null) {
      menu.add(new AccessibleShowViewAction(window, viewDesc, false));
    }

    viewDesc = WorkbenchPlugin.getDefault().getViewRegistry().find(DartUI.ID_PROBLEMS);
    menu.add(new AccessibleShowViewAction(window, viewDesc, false));

    if (DartCoreDebug.ENABLE_TESTS_VIEW) {
      viewDesc = WorkbenchPlugin.getDefault().getViewRegistry().find(DartUI.ID_DARTUNIT_VIEW);
      menu.add(new AccessibleShowViewAction(window, viewDesc, false));
    }
  }

  /**
   * Creates and returns the Build menu (roughly equivalent to the eclipse "Project" menu).
   */
  @SuppressWarnings("unused")
  private MenuManager createBuildMenu() {
    MenuManager menu = new MenuManager(
        WorkbenchMessages.build_menu,
        IWorkbenchActionConstants.M_PROJECT);
    menu.add(cleanAllAction);
    menu.add(new GroupMarker(IWorkbenchActionConstants.BUILD_EXT));
    menu.add(new Separator());
    menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    menu.add(new GroupMarker(IWorkbenchActionConstants.PROJ_END));
    return menu;
  }

  /**
   * Creates and returns the Edit menu.
   */
  private MenuManager createEditMenu() {
    MenuManager menu = new MenuManager(
        IDEWorkbenchMessages.Workbench_edit,
        IWorkbenchActionConstants.M_EDIT);
    menu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_START));

    menu.add(undoAction);
    menu.add(redoAction);
    //menu.add(new GroupMarker(IWorkbenchActionConstants.UNDO_EXT));
    menu.add(new Separator());

    menu.add(actionFactory.getCutItem());
    menu.add(copyAction);
    menu.add(pasteAction);

    //menu.add(new GroupMarker(IWorkbenchActionConstants.CUT_EXT));
    menu.add(new Separator());

    menu.add(actionFactory.getDeleteItem());
    menu.add(actionFactory.getSelectAllItem());
    menu.add(new Separator());

    menu.add(actionFactory.getFindItem());
    menu.add(new GroupMarker(IWorkbenchActionConstants.FIND_EXT));
    menu.add(new Separator());

//    if (organizeImportsAction != null) {
//      menu.add(organizeImportsAction);
//    }
    //menu.add(actionFactory.getBookmarkItem());
    //menu.add(actionFactory.getTaskItem());
    //menu.add(new GroupMarker(IWorkbenchActionConstants.ADD_EXT));

    menu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_END));
    return menu;
  }

  /**
   * Creates and returns the File menu.
   */
  private MenuManager createFileMenu() {
    MenuManager menu = new MenuManager(
        IDEWorkbenchMessages.Workbench_file,
        IWorkbenchActionConstants.M_FILE);
    menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));

    Action newApplicationAction = new OpenNewApplicationWizardAction();
    menu.add(newApplicationAction);
    newFileAction = NewFileHandler.createCommandAction(getWindow());
    menu.add(newFileAction);
    OpenNewFolderWizardAction newFolderAction = new OpenNewFolderWizardAction(getWindow());
    menu.add(newFolderAction);

    menu.add(new Separator());
    IAction openFolderAction = OpenFolderHandler.createCommandAction(getWindow());
    menu.add(openFolderAction);

    menu.add(new Separator());

    menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    menu.add(new Separator());

    menu.add(closeAction);
    menu.add(closeAllAction);

    //		menu.add(closeAllSavedAction);
    menu.add(new GroupMarker(IWorkbenchActionConstants.CLOSE_EXT));
    menu.add(new Separator());
    menu.add(saveAction);
    //TODO (keertip) : renable when we have save as implemented
    //  menu.add(saveAsAction);
    menu.add(saveAllAction);
    menu.add(actionFactory.getRevertItem());
    menu.add(new Separator());
    //TODO (pquitslund): re-enable when we have proper refactoring support implemented
//    menu.add(actionFactory.getMoveItem());
//    menu.add(actionFactory.getRenameItem());

//    menu.add(actionFactory.getRefreshItem());

    menu.add(new GroupMarker(IWorkbenchActionConstants.SAVE_EXT));
    //menu.add(new Separator());
    //menu.add(actionFactory.getPrintItem());
    //menu.add(new GroupMarker(IWorkbenchActionConstants.PRINT_EXT));
    //menu.add(new Separator());
    //menu.add(importResourcesAction);
    //menu.add(exportResourcesAction);
    //menu.add(new GroupMarker(IWorkbenchActionConstants.IMPORT_EXT));
    //menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

//    menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
//    menu.add(new Separator());
//    menu.add(actionFactory.getPropertiesItem());

    menu.add(new Separator());
    menu.add(showInFinderAction);
    menu.add(showPropertiesAction);

    menu.add(ContributionItemFactory.REOPEN_EDITORS.create(getWindow()));
    menu.add(new GroupMarker(IWorkbenchActionConstants.MRU));
    menu.add(new Separator());

    // If we're on OS X we shouldn't show this command in the File menu. It
    // should be invisible to the user. However, we should not remove it -
    // the carbon UI code will do a search through our menu structure
    // looking for it when Cmd-Q is invoked (or Quit is chosen from the
    // application menu.
    ActionContributionItem quitItem = new ActionContributionItem(quitAction);
    quitItem.setVisible(!Util.isMac());
    menu.add(quitItem);
    menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));

    menu.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager manager) {
        showPropertiesAction.updateEnablement(getWindow().getSelectionService().getSelection());
        showInFinderAction.updateEnablement();
      }
    });

    return menu;
  }

  /**
   * Creates and returns the Help menu.
   */
  private MenuManager createHelpMenu() {
    MenuManager menu = new MenuManager(
        IDEWorkbenchMessages.Workbench_help,
        IWorkbenchActionConstants.M_HELP);

    menu.add(new Separator("group.assist")); //$NON-NLS-1$
    menu.add(new Separator("group.main")); //$NON-NLS-1$
    menu.add(openOnlineDocsAction);
    menu.add(openTutorialAction);
    menu.add(openApiDocsAction);

    // HELP_START should really be the first item, but it was after
    // quickStartAction and tipsAndTricksAction in 2.1.
    menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));
    menu.add(new GroupMarker("group.main.ext")); //$NON-NLS-1$
    menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));
    addSeparatorOrGroupMarker(menu, IWorkbenchActionConstants.MB_ADDITIONS);

    // about should always be at the bottom
    menu.add(new Separator("group.about")); //$NON-NLS-1$

    ActionContributionItem aboutItem = new ActionContributionItem(aboutAction);
    aboutItem.setVisible(!Util.isMac());
    menu.add(aboutItem);
    menu.add(new GroupMarker("group.about.ext")); //$NON-NLS-1$
    return menu;
  }

  /**
   * Creates and returns the Navigate menu.
   */
  private MenuManager createNavigateMenu() {
    MenuManager menu = new MenuManager(
        IDEWorkbenchMessages.Workbench_navigate,
        IWorkbenchActionConstants.M_NAVIGATE);
    menu.add(new GroupMarker(IWorkbenchActionConstants.NAV_START));
    //menu.add(goIntoAction);

    //MenuManager goToSubMenu = new MenuManager(IDEWorkbenchMessages.Workbench_goTo,
    //    IWorkbenchActionConstants.GO_TO);
    //menu.add(goToSubMenu);
    //goToSubMenu.add(backAction);
    //goToSubMenu.add(forwardAction);
    //goToSubMenu.add(upAction);
    //goToSubMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

    menu.add(new Separator(IWorkbenchActionConstants.OPEN_EXT));
    for (int i = 2; i < 5; ++i) {
      menu.add(new GroupMarker(IWorkbenchActionConstants.OPEN_EXT + i));
    }
    menu.add(new Separator(IWorkbenchActionConstants.SHOW_EXT));
    //{
    //  MenuManager showInSubMenu = new MenuManager(IDEWorkbenchMessages.Workbench_showIn, "showIn"); //$NON-NLS-1$
    //  showInSubMenu.setActionDefinitionId(showInQuickMenu.getActionDefinitionId());
    //  showInSubMenu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(getWindow()));
    //  menu.add(showInSubMenu);
    // }
    for (int i = 2; i < 5; ++i) {
      menu.add(new Separator(IWorkbenchActionConstants.SHOW_EXT + i));
    }

    menu.add(new Separator());
    menu.add(activateEditorAction);
    menu.add(nextEditorAction);
    menu.add(prevEditorAction);
    menu.add(workbookEditorsAction);

    //menu.add(new Separator());
    //menu.add(nextAction);
    //menu.add(previousAction);
    menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    menu.add(new GroupMarker(IWorkbenchActionConstants.NAV_END));

    IMenuListener menuSelectionistener = new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager manager) {
        updateContextSensitiveMenuItems(manager);
      }
    };
    menu.addMenuListener(menuSelectionistener);
    //TBD: Location of this actions
    //menu.add(new Separator());
    //menu.add(backwardHistoryAction);
    //menu.add(forwardHistoryAction);
    return menu;
  }

  /**
   * Creates and returns the Window menu.
   */
  private MenuManager createToolsMenu() {
    MenuManager menu = new MenuManager(
        WorkbenchMessages.tools_menu,
        IWorkbenchActionConstants.M_WINDOW);

    menu.add(new OpenIntroEditorAction());

    menu.add(new Separator());

    addViewActions(menu);

    menu.add(new Separator());

    menu.add(pubInstallAction);
    menu.add(pubUpdateAction);
    menu.add(pubDeployAction);
    menu.add(pubPublishAction);

    menu.add(stopPubServeAction);
    menu.add(new Separator());

    menu.add(cleanAllAction);

    Separator sep = new Separator(IWorkbenchActionConstants.MB_ADDITIONS);
    sep.setVisible(!Util.isMac());
    menu.add(sep);

    //if (Util.isCocoa()) {
    //  menu.add(arrangeWindowsItem);
    //}

    // See the comment for quit in createFileMenu
    if (!Util.isMac()) {
      menu.add(new ActionContributionItem(openPreferencesAction));
    }

    //menu.add(ContributionItemFactory.OPEN_WINDOWS.create(getWindow()));
    return menu;
  }

  /**
   * Returns the window to which this action builder is contributing.
   */
  private IWorkbenchWindow getWindow() {
    return window;
  }

  /**
   * Hooks listeners on the preference store and the window's page, perspective and selection
   * services.
   */
  private void hookListeners() {

    pageListener = new IPageListener() {
      @Override
      public void pageActivated(IWorkbenchPage page) {
        // do nothing
      }

      @Override
      public void pageClosed(IWorkbenchPage page) {
        // do nothing
      }

      @Override
      public void pageOpened(IWorkbenchPage page) {
        // set default build handler -- can't be done until the shell is available
//                IAction buildHandler = new BuildAction(page.getWorkbenchWindow(), IncrementalProjectBuilder.INCREMENTAL_BUILD);
//            	((RetargetActionWithDefault)buildProjectAction).setDefaultHandler(buildHandler);
      }
    };
    getWindow().addPageListener(pageListener);

    prefListener = new Preferences.IPropertyChangeListener() {
      @Override
      public void propertyChange(Preferences.PropertyChangeEvent event) {
        if (event.getProperty().equals(ResourcesPlugin.PREF_AUTO_BUILDING)) {
          updateProjectStateDependentActions(false);
        }
      }
    };
    ResourcesPlugin.getPlugin().getPluginPreferences().addPropertyChangeListener(prefListener);

    // listener for the "close editors automatically"
    // preference change
    propPrefListener = new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(IPreferenceConstants.REUSE_EDITORS_BOOLEAN)) {
          if (window.getShell() != null && !window.getShell().isDisposed()) {
            // this property change notification could be from a non-ui thread
            window.getShell().getDisplay().syncExec(new Runnable() {
              @Override
              public void run() {
                updatePinActionToolbar();
              }
            });
          }
        }
      }
    };
    /*
     * In order to ensure that the pin action toolbar sets its size correctly, the pin action should
     * set its visiblity before we call updatePinActionToolbar().
     * 
     * In other words we always want the PinActionContributionItem to be notified before the
     * WorkbenchActionBuilder.
     */
    WorkbenchPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(propPrefListener);
    //listen for project description changes, which can affect enablement of build actions
    resourceListener = new IResourceChangeListener() {
      @Override
      public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        if (delta == null) {
          return;
        }
        IResourceDelta[] projectDeltas = delta.getAffectedChildren();
        for (int i = 0; i < projectDeltas.length; i++) {
          int kind = projectDeltas[i].getKind();
          //affected by projects being opened/closed or description changes
          boolean changed = (projectDeltas[i].getFlags() & (IResourceDelta.DESCRIPTION | IResourceDelta.OPEN)) != 0;
          if (kind != IResourceDelta.CHANGED || changed) {
            updateProjectStateDependentActions(false);
            return;
          }
        }
      }
    };
    ResourcesPlugin.getWorkspace().addResourceChangeListener(
        resourceListener,
        IResourceChangeEvent.POST_CHANGE);
  }

  /**
   * Creates the feature-dependent actions for the menu bar.
   */
  private void makeFeatureDependentActions(IWorkbenchWindow window) {

    IPreferenceStore prefs = IDEWorkbenchPlugin.getDefault().getPreferenceStore();

    // Optimization: avoid obtaining the about infos if the platform state is
    // unchanged from last time.  See bug 75130 for details.
    String stateKey = "platformState"; //$NON-NLS-1$
    String prevState = prefs.getString(stateKey);
    String currentState = String.valueOf(Platform.getStateStamp());
    boolean sameState = currentState.equals(prevState);
    if (!sameState) {
      prefs.putValue(stateKey, currentState);
    }
  }

  private void updateContextSensitiveMenuItems(IMenuManager manager) {
    try {
      SubContributionItem item = (SubContributionItem) manager.findUsingPath(DartEditorActionDefinitionIds.OPEN_EDITOR);
      ActionContributionItem inner = (ActionContributionItem) item.getInnerItem();
      RetargetTextEditorAction reaction = (RetargetTextEditorAction) inner.getAction();
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        OpenAction_NEW action = (OpenAction_NEW) ReflectionUtils.getFieldObject(reaction, "fAction");
        action.updateLabel();
      } else {
        OpenAction_OLD action = (OpenAction_OLD) ReflectionUtils.getFieldObject(reaction, "fAction");
        action.updateLabel();
      }
    } catch (Exception ex) {
      // initialization order can cause exception
    }
  }
}
