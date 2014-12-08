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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.MessageConsole.MessageStream;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.actions.DeployConsolePatternMatcher;
import com.google.dart.tools.ui.actions.OpenIntroEditorAction;
import com.google.dart.tools.ui.internal.preferences.DartKeyBindingPersistence;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.theme.preferences.TemporaryProject;
import com.google.dart.tools.ui.watchdog.MonitoringUtil;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileTree;
import org.eclipse.core.internal.localstore.UnifiedTree;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEInternalPreferences;
import org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages;
import org.eclipse.ui.internal.ide.IDEWorkbenchActivityHelper;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.undo.WorkspaceUndoMonitor;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.statushandlers.AbstractStatusHandler;
import org.osgi.framework.Bundle;

import java.net.URL;

/**
 * IDE-specified workbench advisor which configures the workbench for use as an IDE.
 * <p>
 * Much of this code was copied out the Eclipses version of this class:
 * <code>IDEWorkbenchAdvistor</code> in <code>org.eclipse.ui.ide.application</code>.
 * </p>
 * <p>
 * Note: This class replaces <code>org.eclipse.ui.internal.Workbench</code>.
 * </p>
 */
@SuppressWarnings({"restriction", "deprecation"})
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      updateConsoleFont(event.getProperty());
    }
  }

  private static final String PERSPECTIVE_ID = "com.google.dart.tools.ui.DartPerspective"; //$NON-NLS-1$

  private static ApplicationWorkbenchAdvisor workbenchAdvisor = null;

  /**
   * Helper for managing activities in response to workspace changes.
   */
  private IDEWorkbenchActivityHelper activityHelper = null;

  /**
   * Helper for managing work that is performed when the system is otherwise idle.
   */
  private DartIdleHelper idleHelper;

  /**
   * Support class for monitoring workspace changes and periodically validating the undo history
   */
  private WorkspaceUndoMonitor workspaceUndoMonitor;

  /**
   * Helper class used to process delayed events.
   */
  private DelayedEventsProcessor delayedEventsProcessor;

  /**
   * The workbench error handler.
   */
  private AbstractStatusHandler workbenchErrorHandler;

  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();
  private MessageConsole console;
  private boolean uiMonitorStarted;

  /**
   * Creates a new workbench advisor instance.
   */
  public ApplicationWorkbenchAdvisor(DelayedEventsProcessor processor) {
    super();
    if (workbenchAdvisor != null) {
      throw new IllegalStateException();
    }
    workbenchAdvisor = this;
    this.delayedEventsProcessor = processor;
  }

  @Override
  public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
    return new ApplicationWorkbenchWindowAdvisor(workbenchAdvisor, configurer);
  }

  @Override
  public void eventLoopIdle(Display display) {
    if (delayedEventsProcessor != null) {
      delayedEventsProcessor.catchUp(display);
    }
    if (!uiMonitorStarted) {
      uiMonitorStarted = true;
      MonitoringUtil.start();
    }
    MonitoringUtil.idle();
    super.eventLoopIdle(display);
  }

  @Override
  public IAdaptable getDefaultPageInput() {
    // use workspace root
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  @Override
  public String getInitialWindowPerspectiveId() {
    return PERSPECTIVE_ID;
  }

  @Override
  public synchronized AbstractStatusHandler getWorkbenchErrorHandler() {
    if (workbenchErrorHandler == null) {
      workbenchErrorHandler = new DartWorkbenchErrorHandler();
    }
    return workbenchErrorHandler;
  }

  @Override
  public void initialize(IWorkbenchConfigurer configurer) {

    // make sure we always save and restore workspace state
    configurer.setSaveAndRestore(true);

    // check to see if a UI code change requires workbench reset
    checkForWorkbenchStateReset(configurer);

    // register workspace adapters
    IDE.registerAdapters();

    // set our preferred preference settings
    initializePreferenceSettings();

    // register shared images
    declareWorkbenchImages();

    // initialize the activity helper
    activityHelper = IDEWorkbenchActivityHelper.getInstance();

    // initialize idle handler
    idleHelper = new DartIdleHelper(configurer);

    // initialize the workspace undo monitor
    workspaceUndoMonitor = WorkspaceUndoMonitor.getInstance();
  }

  @Override
  public void postShutdown() {
    if (activityHelper != null) {
      activityHelper.shutdown();
      activityHelper = null;
    }
    if (idleHelper != null) {
      idleHelper.shutdown();
      idleHelper = null;
    }
    if (workspaceUndoMonitor != null) {
      workspaceUndoMonitor.shutdown();
      workspaceUndoMonitor = null;
    }
    if (IDEWorkbenchPlugin.getPluginWorkspace() != null) {
      disconnectFromWorkspace();
    }
  }

  @Override
  public void postStartup() {
    try {

      refreshFromLocal();

      // close all editors whose file contents have been deleted
      EditorUtility.closeOrphanedEditors();

      // TODO remove or comment in the following code: activate a proxy service?
      //activateProxyService();

      //((Workbench) PlatformUI.getWorkbench()).registerService(ISelectionConversionService.class,
      //    new IDESelectionConversionService());

      // TODO remove or comment in the following code: prompt user when certain settings are changed?
      //initializeSettingsChangeListener();

      //Display.getCurrent().addListener(SWT.Settings, settingsChangeListener);

      console = new MessageConsole("", null); // empty string hides title bar
      console.addPatternMatchListener(new DeployConsolePatternMatcher());
      ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] {console});
      final MessageConsoleStream stream = console.newMessageStream();
      stream.setActivateOnWrite(false);
      JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
      updateConsoleFont(PreferenceConstants.EDITOR_TEXT_FONT);

      DartCore.getConsole().addStream(new MessageStream() {
        @Override
        public void clear() {
          console.clearConsole();
        }

        @Override
        public void print(String s) {
          stream.print(s);
        }

        @Override
        public void println() {
          stream.println();
        }

        @Override
        public void println(String s) {
          stream.println(s);
        }
      });

      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          if (shouldShowWelcome()) {
            new OpenIntroEditorAction().run();
          }
        }
      });

      IWorkbench workbench = PlatformUI.getWorkbench();
      IActivityManager act = workbench.getActivitySupport().getActivityManager();
      IBindingService bind = (IBindingService) workbench.getService(IBindingService.class);
      ICommandService cmd = (ICommandService) workbench.getService(ICommandService.class);
      DartKeyBindingPersistence persist = new DartKeyBindingPersistence(act, bind, cmd);
      persist.restoreBindingPreferences();

      optionallyEnableSWTResourceProfiling();

    } finally {// Resume background jobs after we startup
      Job.getJobManager().resume();
    }
  }

  @Override
  public boolean preShutdown() {
    //Display.getCurrent().removeListener(SWT.Settings, settingsChangeListener);
    return super.preShutdown();
  }

  @Override
  public void preStartup() {

    // since there's no support for closed projects, we remove any that exist
    // and harvest any deleted projects along the way
    cleanupClosedOrNonExistentProjects();

    // Suspend background jobs while we startup
    Job.getJobManager().suspend();

    // Register the build actions
//    IProgressService service = PlatformUI.getWorkbench().getProgressService();
//    ImageDescriptor newImage = DartWorkbenchImages.getImageDescriptor(DartWorkbenchImages.IMG_ETOOL_BUILD_EXEC);
//    service.registerIconForFamily(newImage, ResourcesPlugin.FAMILY_MANUAL_BUILD);
//    service.registerIconForFamily(newImage, ResourcesPlugin.FAMILY_AUTO_BUILD);
  }

  protected boolean shouldShowWelcome() {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      return ResourcesPlugin.getWorkspace().getRoot().getProjects().length == 0;
    } else {
      return DartCore.getProjectManager().getProjects().length == 0;
    }
  }

  //checks to see if a flag has been set noting that workspace layout needs to be reset
  private void checkForWorkbenchStateReset(IWorkbenchConfigurer workbenchConfigurer) {
    //this particular reset is necessitated by a move of plugin control contributions
    final String NEEDS_RESET = "needsReset"; //$NON-NLS-1$

    if (!Activator.getDefault().getPreferenceStore().contains(NEEDS_RESET)) {
      Activator.getDefault().getPreferenceStore().putValue(NEEDS_RESET, IPreferenceStore.FALSE);

      //set to false to ensure old state does not get re-loaded; this will be reset to true in 
      //ApplicationWorkbenchWindowAdvisor.preWindowOpen()
      workbenchConfigurer.setSaveAndRestore(false);
    }
  }

  private void cleanupClosedOrNonExistentProjects() {
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (IProject project : projects) {
      if (!ResourceUtil.isExistingProject(project)) {
        try {
          boolean remove = MessageDialog.openConfirm(
              Display.getDefault().getActiveShell(),
              "Remove Project",
              "Removing closed or non-existent project '" + project.getName() + "'.");
          if (remove) {
            Activator.log("removing closed or non-existent project '" + project.getName()
                + "' pre startup");
            project.delete(false /* don't delete content */, true /* force */, null /* no monitor */);
          }
        } catch (CoreException e) {
          Activator.logError(e);
        }
      } else if (project.isHidden() && project.getName().equals(TemporaryProject.DEFAULT_NAME)) {
        try {
          project.delete(true /* delete content */, true /* force */, null /* no monitor */);
        } catch (CoreException e) {
          Activator.logError(e);
        }
      }
    }
  }

  /**
   * Code originally copied over from IDEWorkbenchAdvisor.declareWorkbenchImage(..) in
   * <code>org.eclipse.ui.ide.application</code>.
   * <p>
   * Declares an IDE-specific workbench image.
   * </p>
   * 
   * @param symbolicName the symbolic name of the image
   * @param path the path of the image file; this path is relative to the base of the IDE plug-in
   * @param shared <code>true</code> if this is a shared image, and <code>false</code> if this is
   *          not a shared image
   * @see IWorkbenchConfigurer#declareImage
   */
  private void declareWorkbenchImage(Bundle ideBundle, String symbolicName, String path,
      boolean shared) {
    URL url = FileLocator.find(ideBundle, new Path(path), null);
    ImageDescriptor desc = ImageDescriptor.createFromURL(url);
    getWorkbenchConfigurer().declareImage(symbolicName, desc, shared);
  }

  /**
   * Code originally copied over from IDEWorkbenchAdvisor.declareWorkbenchImages() in
   * <code>org.eclipse.ui.ide.application</code>.
   * <p>
   * Declares all IDE-specific workbench images. This includes both "shared" images (named in
   * {@link IDE.SharedImages}) and internal images (named in
   * {@link org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages}).
   * 
   * @see IWorkbenchConfigurer#declareImage
   */
  private void declareWorkbenchImages() {

    final String ICONS_PATH = "$nl$/icons/full/";//$NON-NLS-1$
    final String PATH_ELOCALTOOL = ICONS_PATH + "elcl16/"; // Enabled //$NON-NLS-1$

    // toolbar
    // icons.
    final String PATH_DLOCALTOOL = ICONS_PATH + "dlcl16/"; // Disabled //$NON-NLS-1$
    // //$NON-NLS-1$
    // toolbar
    // icons.
    final String PATH_ETOOL = ICONS_PATH + "etool16/"; // Enabled toolbar //$NON-NLS-1$
    // //$NON-NLS-1$
    // icons.
    final String PATH_DTOOL = ICONS_PATH + "dtool16/"; // Disabled toolbar //$NON-NLS-1$
    // //$NON-NLS-1$
    // icons.
    final String PATH_OBJECT = ICONS_PATH + "obj16/"; // Model object //$NON-NLS-1$
    // //$NON-NLS-1$
    // icons
    final String PATH_WIZBAN = ICONS_PATH + "wizban/"; // Wizard //$NON-NLS-1$
    // //$NON-NLS-1$
    // icons

    // View icons
    // Introduced in 3.7
    final String PATH_EVIEW = ICONS_PATH + "eview16/"; //$NON-NLS-1$

    Bundle ideBundle = Platform.getBundle(Activator.PLUGIN_ID);//(IDEWorkbenchPlugin.IDE_WORKBENCH);

    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_ETOOL_BUILD_EXEC, PATH_ETOOL
        + "build_exec.gif", false); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_ETOOL_BUILD_EXEC_HOVER, PATH_ETOOL
        + "build_exec.gif", false); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_ETOOL_BUILD_EXEC_DISABLED, PATH_DTOOL
        + "build_exec.gif", false); //$NON-NLS-1$

    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_ETOOL_SEARCH_SRC, PATH_ETOOL
        + "search_src.gif", false); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_ETOOL_SEARCH_SRC_HOVER, PATH_ETOOL
        + "search_src.gif", false); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_ETOOL_SEARCH_SRC_DISABLED, PATH_DTOOL
        + "search_src.gif", false); //$NON-NLS-1$

    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_ETOOL_NEXT_NAV, PATH_ETOOL
        + "next_nav.gif", false); //$NON-NLS-1$

    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_ETOOL_PREVIOUS_NAV, PATH_ETOOL
        + "prev_nav.gif", false); //$NON-NLS-1$

    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_WIZBAN_NEWPRJ_WIZ, PATH_WIZBAN
        + "newprj_wiz.png", false); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_WIZBAN_NEWFOLDER_WIZ, PATH_WIZBAN
        + "newfolder_wiz.png", false); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_WIZBAN_NEWFILE_WIZ, PATH_WIZBAN
        + "newfile_wiz.png", false); //$NON-NLS-1$

    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_WIZBAN_IMPORTDIR_WIZ, PATH_WIZBAN
        + "importdir_wiz.png", false); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_WIZBAN_IMPORTZIP_WIZ, PATH_WIZBAN
        + "importzip_wiz.png", false); //$NON-NLS-1$

    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_WIZBAN_EXPORTDIR_WIZ, PATH_WIZBAN
        + "exportdir_wiz.png", false); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_WIZBAN_EXPORTZIP_WIZ, PATH_WIZBAN
        + "exportzip_wiz.png", false); //$NON-NLS-1$

    declareWorkbenchImage(
        ideBundle,
        DartWorkbenchImages.IMG_WIZBAN_RESOURCEWORKINGSET_WIZ,
        PATH_WIZBAN + "workset_wiz.png", false); //$NON-NLS-1$

    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_DLGBAN_SAVEAS_DLG, PATH_WIZBAN
        + "saveas_wiz.png", false); //$NON-NLS-1$

    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_DLGBAN_QUICKFIX_DLG, PATH_WIZBAN
        + "quick_fix.png", false); //$NON-NLS-1$

    declareWorkbenchImage(
        ideBundle,
        IDE.SharedImages.IMG_OBJ_PROJECT,
        PATH_OBJECT + "prj_obj.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED, PATH_OBJECT
        + "cprj_obj.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OPEN_MARKER, PATH_ELOCALTOOL
        + "gotoobj_tsk.gif", true); //$NON-NLS-1$

    // Quick fix icons
    declareWorkbenchImage(
        ideBundle,
        DartWorkbenchImages.IMG_ELCL_QUICK_FIX_ENABLED,
        PATH_ELOCALTOOL + "smartmode_co.gif", true); //$NON-NLS-1$

    declareWorkbenchImage(
        ideBundle,
        DartWorkbenchImages.IMG_DLCL_QUICK_FIX_DISABLED,
        PATH_DLOCALTOOL + "smartmode_co.gif", true); //$NON-NLS-1$

    // Introduced in 3.7
    declareWorkbenchImage(
        ideBundle,
        IDEInternalWorkbenchImages.IMG_OBJS_FIXABLE_WARNING,
        PATH_OBJECT + "quickfix_warning_obj.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_FIXABLE_ERROR, PATH_OBJECT
        + "quickfix_error_obj.gif", true); //$NON-NLS-1$

    declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OBJS_TASK_TSK, PATH_OBJECT
        + "taskmrk_tsk.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OBJS_BKMRK_TSK, PATH_OBJECT
        + "bkmrk_tsk.gif", true); //$NON-NLS-1$

    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_OBJS_COMPLETE_TSK, PATH_OBJECT
        + "complete_tsk.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_OBJS_INCOMPLETE_TSK, PATH_OBJECT
        + "incomplete_tsk.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_OBJS_WELCOME_ITEM, PATH_OBJECT
        + "welcome_item.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_OBJS_WELCOME_BANNER, PATH_OBJECT
        + "welcome_banner.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_OBJS_ERROR_PATH, PATH_OBJECT
        + "error_tsk.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_OBJS_WARNING_PATH, PATH_OBJECT
        + "warn_tsk.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_OBJS_INFO_PATH, PATH_OBJECT
        + "info_tsk.gif", true); //$NON-NLS-1$

    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_LCL_FLAT_LAYOUT, PATH_ELOCALTOOL
        + "flatLayout.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(
        ideBundle,
        DartWorkbenchImages.IMG_LCL_HIERARCHICAL_LAYOUT,
        PATH_ELOCALTOOL + "hierarchicalLayout.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(ideBundle, DartWorkbenchImages.IMG_ETOOL_PROBLEM_CATEGORY, PATH_ETOOL
        + "problem_category.gif", true); //$NON-NLS-1$

    // Introduced in 3.7
    declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW, PATH_EVIEW
        + "problems_view.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(
        ideBundle,
        IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_ERROR,
        PATH_EVIEW + "problems_view_error.gif", true); //$NON-NLS-1$
    declareWorkbenchImage(
        ideBundle,
        IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_WARNING,
        PATH_EVIEW + "problems_view_warning.gif", true); //$NON-NLS-1$
  }

  /**
   * Disconnect from the core workspace.
   */
  private void disconnectFromWorkspace() {
    IStatus status;

    try {
      // Save the workspace.
      status = ResourcesPlugin.getWorkspace().save(true, new NullProgressMonitor());
    } catch (CoreException e) {
      status = e.getStatus();
    }

    if (status != null && !status.isOK()) {
      ErrorDialog.openError(
          null,
          IDEWorkbenchMessages.ProblemsSavingWorkspace,
          null,
          status,
          IStatus.ERROR | IStatus.WARNING);
      IDEWorkbenchPlugin.log(IDEWorkbenchMessages.ProblemsSavingWorkspace, status);
    }
  }

  private void initializePreferenceSettings() {
    // tab style setting
    PlatformUI.getPreferenceStore().setValue(
        IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS,
        false);
    // auto-refresh setting
    Preferences preferences = ResourcesPlugin.getPlugin().getPluginPreferences();
    preferences.setValue(ResourcesPlugin.PREF_AUTO_REFRESH, true);
  }

  private void optionallyEnableSWTResourceProfiling() {

    // Optionally enable SWT resource leak debugging
    //
    // requires:
    //   * the swt tools plugin (http://www.eclipse.org/swt/updates/3.6), and
    //   * the following tracing options:
    //         org.eclipse.ui/debug=true 
    //         org.eclipse.ui/trace/graphics=true
    if (DartCoreDebug.PERF_OS_RESOURCES) {

      try {

        String debugFlag = Platform.getDebugOption("org.eclipse.ui/debug");
        if (!"true".equals(debugFlag)) {
          System.err.println("the \"org.eclipse.ui/debug\" trace option must be set to true for resource profiling");
          return;
        }

        String graphicsTracingFlag = Platform.getDebugOption("org.eclipse.ui/trace/graphics");
        if (!"true".equals(graphicsTracingFlag)) {
          System.err.println("the \"org.eclipse.ui/trace/graphics\" trace option must be set to true for resource profiling");
          return;
        }

        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
            "org.eclipse.swt.tools.views.SleakView");

      } catch (PartInitException e) {
        System.err.println("OS resource profiling requires SWT tools to be installed");
        e.printStackTrace();
      }

    }
  }

  private void refreshFromLocal() {
    String[] commandLineArgs = Platform.getCommandLineArgs();
    IPreferenceStore store = IDEWorkbenchPlugin.getDefault().getPreferenceStore();
    boolean refresh = store.getBoolean(IDEInternalPreferences.REFRESH_WORKSPACE_ON_STARTUP);
    if (!refresh) {
      return;
    }

    // Do not refresh if it was already done by core on startup.
    for (int i = 0; i < commandLineArgs.length; i++) {
      if (commandLineArgs[i].equalsIgnoreCase("-refresh")) { //$NON-NLS-1$
        return;
      }
    }

    final IContainer root = ResourcesPlugin.getWorkspace().getRoot();
    Job job = new WorkspaceJob(IDEWorkbenchMessages.Workspace_refreshing) {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        refreshResources((IWorkspaceRoot) root, monitor);
        return Status.OK_STATUS;
      }
    };
    job.setRule(root);
    job.schedule();
  }

  private void refreshResources(IWorkspaceRoot root, IProgressMonitor monitor) throws CoreException {
    IProject[] projects = root.getProjects();
    for (IProject target : projects) {
      if (!target.isAccessible()) {
        continue;
      }

      RefreshLinkedVisitor visitor = new RefreshLinkedVisitor(monitor);
      IFileStore fileStore = ((Resource) target).getStore();
      //try to get all info in one shot, if file system supports it
      IFileTree fileTree = fileStore.getFileSystem().fetchFileTree(
          fileStore,
          new SubProgressMonitor(monitor, 0));
      UnifiedTree tree = fileTree == null ? new UnifiedTree(target) : new UnifiedTree(
          target,
          fileTree);
      tree.accept(visitor, IResource.DEPTH_INFINITE);
      IStatus result = visitor.getErrorStatus();
      if (!result.isOK()) {
        Activator.logError(new ResourceException(result));
      }
    }
  }

  private void updateConsoleFont(String name) {
    if (console != null) {
      if (name.equals(PreferenceConstants.EDITOR_TEXT_FONT)) {
        console.setFont(JFaceResources.getFont(name));
      }
    }
  }
}
