/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.internal.cleanup.CleanUpRegistry;
import com.google.dart.tools.ui.internal.cleanup.preference.PreferencesAccess;
import com.google.dart.tools.ui.internal.preferences.MembersOrderPreferenceCache;
import com.google.dart.tools.ui.internal.text.DartStatusConstants;
import com.google.dart.tools.ui.internal.text.dart.ContentAssistHistory;
import com.google.dart.tools.ui.internal.text.editor.ASTProvider;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitDocumentProvider;
import com.google.dart.tools.ui.internal.text.editor.ICompilationUnitDocumentProvider;
import com.google.dart.tools.ui.internal.text.editor.WorkingCopyManager;
import com.google.dart.tools.ui.internal.text.editor.saveparticipant.SaveParticipantRegistry;
import com.google.dart.tools.ui.internal.text.folding.JavaFoldingStructureProviderRegistry;
import com.google.dart.tools.ui.internal.text.functions.PreferencesAdapter;
import com.google.dart.tools.ui.internal.util.TypeFilter;
import com.google.dart.tools.ui.internal.viewsupport.ImageDescriptorRegistry;
import com.google.dart.tools.ui.internal.viewsupport.ProblemMarkerManager;
import com.google.dart.tools.ui.text.DartTextTools;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.prefs.BackingStoreException;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The activator class controls the plug-in life cycle
 */
public class DartToolsPlugin extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.dart.tools.ui"; //$NON-NLS-1$

  // The shared instance
  private static DartToolsPlugin plugin;

  /** The key to store customized templates. */
  private static final String TEMPLATES_KEY = "com.google.dart.tools.ui.text.custom_templates"; //$NON-NLS-1$

  /** The key to store customized code templates. */
  private static final String CODE_TEMPLATES_KEY = "com.google.dart.tools.ui.text.custom_code_templates"; //$NON-NLS-1$

  /** The key to store whether the legacy templates have been migrated */
  @SuppressWarnings("unused")
  private static final String TEMPLATES_MIGRATION_KEY = "com.google.dart.tools.ui.text.templates_migrated"; //$NON-NLS-1$

  /** The key to store whether the legacy code templates have been migrated */
  @SuppressWarnings("unused")
  private static final String CODE_TEMPLATES_MIGRATION_KEY = "com.google.dart.tools.ui.text.code_templates_migrated"; //$NON-NLS-1$

  private static LinkedHashMap<String, Long> fgRepeatedMessages = new LinkedHashMap<String, Long>(
      20, 0.75f, true) {
    private static final long serialVersionUID = 1L;

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<String, Long> eldest) {
      return size() >= 20;
    }
  };

  private static Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>();

  /**
   * Create an error Status object with the given message and this plugin's ID.
   * 
   * @param message the error message
   * @return the created error status object
   */
  public static Status createErrorStatus(String message) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message);
  }

//  @Deprecated
//  private static final String DEPRECATED_EDITOR_TAB_WIDTH = PreferenceConstants.EDITOR_TAB_WIDTH;
//
//  @Deprecated
//  private static final String DEPRECATED_REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD = PreferenceConstants.REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD;
//
//  @Deprecated
//  private static final String DEPRECATED_CODEASSIST_ORDER_PROPOSALS = PreferenceConstants.CODEASSIST_ORDER_PROPOSALS;

  /**
   * Creates the Java plug-in's standard groups for view context menus.
   * 
   * @param menu the menu manager to be populated
   */
  public static void createStandardGroups(IMenuManager menu) {
    if (!menu.isEmpty()) {
      return;
    }

    menu.add(new Separator(IContextMenuConstants.GROUP_NEW));
    menu.add(new GroupMarker(IContextMenuConstants.GROUP_GOTO));
    menu.add(new Separator(IContextMenuConstants.GROUP_OPEN));
    menu.add(new GroupMarker(IContextMenuConstants.GROUP_SHOW));
    menu.add(new Separator(IContextMenuConstants.GROUP_EDIT));
    menu.add(new Separator(IContextMenuConstants.GROUP_REORGANIZE));
    menu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
    menu.add(new Separator(IContextMenuConstants.GROUP_SEARCH));
    menu.add(new Separator(IContextMenuConstants.GROUP_BUILD));
    menu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
    menu.add(new Separator(IContextMenuConstants.GROUP_VIEWER_SETUP));
    menu.add(new Separator(IContextMenuConstants.GROUP_PROPERTIES));
  }

  /**
   * Respects images residing in any plug-in. If path is relative, then this bundle is looked up for
   * the image, otherwise, for absolute path, first segment is taken as id of plug-in with image
   * 
   * @param path the path to image, either absolute (with plug-in id as first segment), or relative
   *          for bundled images
   * @return the image descriptor
   */
  public static ImageDescriptor findImageDescriptor(String path) {
    final IPath p = new Path(path);

    if (p.isAbsolute() && p.segmentCount() > 1) {
      return AbstractUIPlugin.imageDescriptorFromPlugin(p.segment(0),
          p.removeFirstSegments(1).makeAbsolute().toString());
    } else {
      return getBundledImageDescriptor(p.makeAbsolute().toString());
    }
  }

  /**
   * Flushes the instance scope of this plug-in.
   * 
   * @since 3.7
   */
  public static void flushInstanceScope() {
    try {
      PreferencesAccess.INSTANCE_SCOPE.getNode(DartUI.ID_PLUGIN).flush();
    } catch (BackingStoreException e) {
      log(e);
    }
  }

  public static IWorkbenchPage getActivePage() {
    return getDefault().internalGetActivePage();
  }

  public static Shell getActiveWorkbenchShell() {
    IWorkbenchWindow window = getActiveWorkbenchWindow();
    if (window != null) {
      return window.getShell();
    }
    return null;
  }

  public static IWorkbenchWindow getActiveWorkbenchWindow() {
    return getDefault().getWorkbench().getActiveWorkbenchWindow();
  }

  /**
   * Returns the SVN revision number as a String suitable for display.
   */
  public static String getBuildId() {
    return DartCore.getBuildId();
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in relative path.
   * 
   * @param path the path
   * @return the image descriptor
   */
  public static ImageDescriptor getBundledImageDescriptor(String path) {
    return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static DartToolsPlugin getDefault() {
    return plugin;
  }

  /**
   * Get an image given an ImageDescriptor.
   * 
   * @param imageDescriptor
   * @return an image
   */
  public static Image getImage(ImageDescriptor imageDescriptor) {
    Image image = imageCache.get(imageDescriptor);

    if (image == null) {
      image = imageDescriptor.createImage();

      imageCache.put(imageDescriptor, image);
    }

    return image;
  }

  /**
   * Get an image given a path relative to this plugin.
   * 
   * @param path
   * @return an image
   */
  public static Image getImage(String path) {
    if (getDefault().getImageRegistry().get(path) != null) {
      return getDefault().getImageRegistry().get(path);
    }

    ImageDescriptor descriptor = findImageDescriptor(path);

    if (descriptor != null) {
      getDefault().getImageRegistry().put(path, descriptor);

      return getDefault().getImageRegistry().get(path);
    }

    return null;
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in relative path
   * 
   * @param path the path
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  public static ImageDescriptorRegistry getImageDescriptorRegistry() {
    return getDefault().internalGetImageDescriptorRegistry();
  }

  public static String getPluginId() {
    return DartUI.ID_PLUGIN;
  }

  /**
   * Get the associated bundle's version.
   * 
   * @return the version string
   */
  public static String getVersionString() {
    Bundle bundle = getDefault().getBundle();
    if (bundle == null) {
      return null;
    }
    return bundle.getHeaders().get(Constants.BUNDLE_VERSION);
  }

  public static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  public static boolean isDebug() {
    return getDefault().isDebugging();
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  public static void log(String message) {
    getDefault().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
  }

  public static void log(String message, Throwable e) {
    log(new Status(IStatus.ERROR, getPluginId(), DartStatusConstants.INTERNAL_ERROR, message, e));
  }

  public static void log(Throwable e) {
    log(DartUIMessages.JavaPlugin_internal_error, e);
  }

  public static void logErrorMessage(String message) {
    log(new Status(IStatus.ERROR, getPluginId(), DartStatusConstants.INTERNAL_ERROR, message, null));
  }

  public static void logErrorStatus(String message, IStatus status) {
    if (status == null) {
      logErrorMessage(message);
      return;
    }
    MultiStatus multi = new MultiStatus(getPluginId(), DartStatusConstants.INTERNAL_ERROR, message,
        null);
    multi.add(status);
    log(multi);
  }

  /**
   * Log a message that is potentially repeated after a very short time. The first time this method
   * is called with a given message, the message is written to the log along with the detail message
   * and a stack trace.
   * <p>
   * Only intended for use in debug statements.
   * 
   * @param message the (generic) message
   * @param detail the detail message
   */
  public static void logRepeatedMessage(String message, String detail) {
    long now = System.currentTimeMillis();
    boolean writeToLog = true;
    if (fgRepeatedMessages.containsKey(message)) {
      long last = fgRepeatedMessages.get(message).longValue();
      writeToLog = now - last > 5000;
    }
    fgRepeatedMessages.put(message, new Long(now));
    if (writeToLog) {
      log(new Exception(message + detail).fillInStackTrace());
    }
  }

  /* package */static void initializeAfterLoad(IProgressMonitor monitor) {
    DartX.notYet();
    // OpenTypeHistory.getInstance().checkConsistency(monitor);
  }

  /**
   * The template context type registry for the java editor.
   */
  @SuppressWarnings("unused")
  private ContextTypeRegistry contextTypeRegistry;

  /**
   * The code template context type registry for the java editor.
   */
  private ContextTypeRegistry codeTemplateContextTypeRegistry;

//  @Deprecated
//  private static IPreferenceStore getDeprecatedWorkbenchPreferenceStore() {
//    return PlatformUI.getWorkbench().getPreferenceStore();
//  }

  /**
   * The template store for the java editor.
   */
  private TemplateStore templateStore;

  /**
   * The coded template store for the java editor.
   */
  private TemplateStore codeTemplateStore;

  /**
   * Default instance of the appearance type filters.
   */
  static {
    DartX.todo();
  }

  private TypeFilter typeFilter;

  private WorkingCopyManager workingCopyManager;
  @Deprecated
  private ICompilationUnitDocumentProvider compilationUnitDocumentProvider;

  static {
    DartX.todo();
  }

  private DartTextTools dartTextTools;

  private ProblemMarkerManager problemMarkerManager;

  private ImageDescriptorRegistry imageDescriptorRegistry;

  static {
    DartX.todo();
  }

  private MembersOrderPreferenceCache membersOrderPreferenceCache;

  /**
   * Property change listener on this plugin's preference store.
   */
  private IPropertyChangeListener propertyChangeListener;
  static {
    DartX.todo("hover");
  }

  //private IPropertyChangeListener fFontPropertyChangeListener;

  @SuppressWarnings("unused")
  private/* JavaEditorTextHoverDescriptor */Object[] dartEditorTextHoverDescriptors;

  /**
   * The AST provider.
   */
  private ASTProvider astProvider;
  /**
   * The combined preference store.
   */
  private IPreferenceStore combinedPreferenceStore;

  /**
   * The extension point registry for the
   * <code>org.eclipse.wst.jsdt.ui.javaFoldingStructureProvider</code> extension point.
   */
  private JavaFoldingStructureProviderRegistry foldingStructureProviderRegistry;

  /**
   * Content assist history.
   */
  private ContentAssistHistory contentAssistHistory;

  /**
   * The save participant registry.
   */
  private SaveParticipantRegistry saveParticipantRegistry;

  /**
   * Theme listener.
   */
  //private IPropertyChangeListener fThemeListener;

  private FormToolkit dialogsFormToolkit;

  /**
   * The clean up registry
   */
  private CleanUpRegistry cleanUpRegistry;

  /**
   * The constructor
   */
  public DartToolsPlugin() {
    plugin = this;
  }

  /**
   * Returns the AST provider.
   * 
   * @return the AST provider
   */
  public synchronized ASTProvider getASTProvider() {
    if (astProvider == null) {
      astProvider = new ASTProvider();
    }

    return astProvider;
  }

  /** @deprecated there are no class files */
  @Deprecated
  public synchronized Object getClassFileDocumentProvider() {
    DartX.notYet();
    return null;
  }

  public synchronized CleanUpRegistry getCleanUpRegistry() {
    if (cleanUpRegistry == null) {
      cleanUpRegistry = new CleanUpRegistry();
    }

    return cleanUpRegistry;
  }

  /**
   * Returns the template context type registry for the code generation templates.
   * 
   * @return the template context type registry for the code generation templates
   */
  public ContextTypeRegistry getCodeTemplateContextRegistry() {
    if (codeTemplateContextTypeRegistry == null) {
      codeTemplateContextTypeRegistry = new ContributionContextTypeRegistry();
      DartX.todo();
      // CodeTemplateContextType.registerContextTypes(fCodeTemplateContextTypeRegistry);
    }

    return codeTemplateContextTypeRegistry;
  }

  /**
   * Returns the template store for the code generation templates.
   * 
   * @return the template store for the code generation templates
   */
  public TemplateStore getCodeTemplateStore() {
    if (codeTemplateStore == null) {
      IPreferenceStore store = getPreferenceStore();
      codeTemplateStore = new ContributionTemplateStore(getCodeTemplateContextRegistry(), store,
          CODE_TEMPLATES_KEY);
      try {
        codeTemplateStore.load();
      } catch (IOException e) {
        log(e);
      }
      codeTemplateStore.startListeningForPreferenceChanges();
    }
    return codeTemplateStore;
  }

  /**
   * Returns a combined preference store, this store is read-only.
   * 
   * @return the combined preference store
   */
  @SuppressWarnings("deprecation")
  public IPreferenceStore getCombinedPreferenceStore() {
    if (combinedPreferenceStore == null) {
      IPreferenceStore generalTextStore = EditorsUI.getPreferenceStore();
      combinedPreferenceStore = new ChainedPreferenceStore(new IPreferenceStore[] {
          getPreferenceStore(),
          new PreferencesAdapter(DartCore.getPlugin().getPluginPreferences()), generalTextStore});
    }
    return combinedPreferenceStore;
  }

  public synchronized ICompilationUnitDocumentProvider getCompilationUnitDocumentProvider() {
    if (compilationUnitDocumentProvider == null) {
      compilationUnitDocumentProvider = new CompilationUnitDocumentProvider();
    }
    return compilationUnitDocumentProvider;
  }

  /**
   * Returns the Dart content assist history.
   * 
   * @return the Dart content assist history
   */
  @SuppressWarnings("deprecation")
  public ContentAssistHistory getContentAssistHistory() {
    if (contentAssistHistory == null) {
      try {
        contentAssistHistory = ContentAssistHistory.load(getPluginPreferences(),
            PreferenceConstants.CODEASSIST_LRU_HISTORY);
      } catch (CoreException x) {
        log(x);
      }
      if (contentAssistHistory == null) {
        contentAssistHistory = new ContentAssistHistory();
      }
    }

    return contentAssistHistory;
  }

  /**
   * Returns a section in the Java plugin's dialog settings. If the section doesn't exist yet, it is
   * created.
   * 
   * @param name the name of the section
   * @return the section of the given name
   */
  public IDialogSettings getDialogSettingsSection(String name) {
    IDialogSettings dialogSettings = getDialogSettings();
    IDialogSettings section = dialogSettings.getSection(name);
    if (section == null) {
      section = dialogSettings.addNewSection(name);
    }
    return section;
  }

  public FormToolkit getDialogsFormToolkit() {
    if (dialogsFormToolkit == null) {
      FormColors colors = new FormColors(Display.getCurrent());
      colors.setBackground(null);
      colors.setForeground(null);
      dialogsFormToolkit = new FormToolkit(colors);
    }
    return dialogsFormToolkit;
  }

  /**
   * Returns the registry of the extensions to the
   * <code>org.eclipse.wst.jsdt.ui.javaFoldingStructureProvider</code> extension point.
   * 
   * @return the registry of contributed <code>IJavaFoldingStructureProvider</code>
   */
  public synchronized JavaFoldingStructureProviderRegistry getFoldingStructureProviderRegistry() {
    if (foldingStructureProviderRegistry == null) {
      foldingStructureProviderRegistry = new JavaFoldingStructureProviderRegistry();
    }
    return foldingStructureProviderRegistry;
  }

  /**
   * Returns all Java editor text hovers contributed to the workbench.
   * 
   * @return an array of JavaEditorTextHoverDescriptor
   */
  public synchronized/* JavaEditorTextHoverDescriptor */Object[] getJavaEditorTextHoverDescriptors() {
    DartX.todo("hover");
    return null;
    // if (fJavaEditorTextHoverDescriptors == null) {
    // fJavaEditorTextHoverDescriptors =
    // JavaEditorTextHoverDescriptor.getContributedHovers();
    // ConfigurationElementSorter sorter = new ConfigurationElementSorter() {
    // /*
    // * @see org.eclipse.ui.texteditor.ConfigurationElementSorter#
    // * getConfigurationElement(java.lang.Object)
    // */
    // public IConfigurationElement getConfigurationElement(Object object) {
    // return ((JavaEditorTextHoverDescriptor)
    // object).getConfigurationElement();
    // }
    // };
    // sorter.sort(fJavaEditorTextHoverDescriptors);
    //
    // // Move Best Match hover to front
    // for (int i = 0; i < fJavaEditorTextHoverDescriptors.length - 1; i++) {
    // if
    // (PreferenceConstants.ID_BESTMATCH_HOVER.equals(fJavaEditorTextHoverDescriptors[i].getId()))
    // {
    // JavaEditorTextHoverDescriptor hoverDescriptor =
    // fJavaEditorTextHoverDescriptors[i];
    // for (int j = i; j > 0; j--)
    // fJavaEditorTextHoverDescriptors[j] = fJavaEditorTextHoverDescriptors[j -
    // 1];
    // fJavaEditorTextHoverDescriptors[0] = hoverDescriptor;
    // break;
    // }
    //
    // }
    // }
    //
    // return fJavaEditorTextHoverDescriptors;
  }

  @SuppressWarnings("deprecation")
  public synchronized DartTextTools getJavaTextTools() {
    if (dartTextTools == null) {
      dartTextTools = new DartTextTools(getPreferenceStore(),
          DartCore.getPlugin().getPluginPreferences());
    }
    return dartTextTools;
  }

  public synchronized MembersOrderPreferenceCache getMemberOrderPreferenceCache() {
    if (membersOrderPreferenceCache == null) {
      membersOrderPreferenceCache = new MembersOrderPreferenceCache();
      membersOrderPreferenceCache.install(getPreferenceStore());
    }
    return membersOrderPreferenceCache;
  }

  public synchronized ProblemMarkerManager getProblemMarkerManager() {
    if (problemMarkerManager == null) {
      problemMarkerManager = new ProblemMarkerManager();
    }
    return problemMarkerManager;
  }

  /**
   * Returns the save participant registry.
   * 
   * @return the save participant registry, not null
   */
  public synchronized SaveParticipantRegistry getSaveParticipantRegistry() {
    if (saveParticipantRegistry == null) {
      saveParticipantRegistry = new SaveParticipantRegistry();
    }
    return saveParticipantRegistry;
  }

  /**
   * Returns the template context type registry for the java plug-in.
   * 
   * @return the template context type registry for the java plug-in
   */
  public ContextTypeRegistry getTemplateContextRegistry() {
    DartX.todo();
    return null;
    // if (fContextTypeRegistry == null) {
    // ContributionContextTypeRegistry registry = new
    // ContributionContextTypeRegistry();
    // registry.addContextType(JavaContextType.NAME);
    // registry.addContextType(JavaDocContextType.NAME);
    //
    // fContextTypeRegistry = registry;
    // }
    //
    // return fContextTypeRegistry;
  }

  /**
   * Returns the template store for the java editor templates.
   * 
   * @return the template store for the java editor templates
   */
  public TemplateStore getTemplateStore() {
    if (templateStore == null) {
      final IPreferenceStore store = getPreferenceStore();
      templateStore = new ContributionTemplateStore(getTemplateContextRegistry(), store,
          TEMPLATES_KEY);

      try {
        templateStore.load();
      } catch (IOException e) {
        log(e);
      }
      templateStore.startListeningForPreferenceChanges();
    }

    return templateStore;
  }

  public synchronized TypeFilter getTypeFilter() {
    if (typeFilter == null) {
      typeFilter = new TypeFilter();
    }
    return typeFilter;
  }

  public synchronized WorkingCopyManager getWorkingCopyManager() {
    if (workingCopyManager == null) {
      ICompilationUnitDocumentProvider provider = getCompilationUnitDocumentProvider();
      workingCopyManager = new WorkingCopyManager(provider);
    }
    return workingCopyManager;
  }

  /**
   * Resets the Java editor text hovers contributed to the workbench.
   * <p>
   * This will force a rebuild of the descriptors the next time a client asks for them.
   * </p>
   */
  public synchronized void resetJavaEditorTextHoverDescriptors() {
    dartEditorTextHoverDescriptors = null;
  }

  /**
   * Start the Dart Tools UI plugin. This method is called automatically by the OSGi framework.
   * <p>
   * We want to be careful about what code we call from this start() method and what code we call
   * from the activator's class and instance initializers. In order to have very snappy launch
   * times, we don't want to initialize plugins unnecessarily. Specifically, we're trying not to
   * call into the Dart Tools Core plugin until code in that plugin is actually referenced / needed.
   * After this start() method has run, and everywhere else in the UI plugin, calling into Core code
   * is fair game.
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    //ensurePreferenceStoreBackwardsCompatibility();

    DartX.todo();
    // new InitializeAfterLoadJob().schedule();

    // make sure is loaded too for org.eclipse.wst.jsdt.core.manipulation
    // can be removed if JavaElementPropertyTester is moved down to jdt.core
    DartX.todo();
    // JavaScriptManipulation.class.toString();

//    fThemeListener = new IPropertyChangeListener() {
//      @Override
//      public void propertyChange(PropertyChangeEvent event) {
//        if (IThemeManager.CHANGE_CURRENT_THEME.equals(event.getProperty())) {
//          DartX.todo();
//          // new JavaUIPreferenceInitializer().initializeDefaultPreferences();
//        }
//      }
//    };
//    PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fThemeListener);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void stop(BundleContext context) throws Exception {
    try {
      DartUIStartup.cancelStartup();

      if (imageDescriptorRegistry != null) {
        imageDescriptorRegistry.dispose();
      }

      if (astProvider != null) {
        astProvider.dispose();
        astProvider = null;
      }

      if (workingCopyManager != null) {
        workingCopyManager.shutdown();
        workingCopyManager = null;
      }

      if (compilationUnitDocumentProvider != null) {
        compilationUnitDocumentProvider.shutdown();
        compilationUnitDocumentProvider = null;
      }

      if (dartTextTools != null) {
        dartTextTools.dispose();
        dartTextTools = null;
      }

      if (typeFilter != null) {
        typeFilter.dispose();
        typeFilter = null;
      }

      if (contentAssistHistory != null) {
        ContentAssistHistory.store(contentAssistHistory, getPluginPreferences(),
            PreferenceConstants.CODEASSIST_LRU_HISTORY);
        contentAssistHistory = null;
      }

      uninstallPreferenceStoreBackwardsCompatibility();

      if (templateStore != null) {
        templateStore.stopListeningForPreferenceChanges();
        templateStore = null;
      }

      if (codeTemplateStore != null) {
        codeTemplateStore.stopListeningForPreferenceChanges();
        codeTemplateStore = null;
      }

      if (membersOrderPreferenceCache != null) {
        membersOrderPreferenceCache.dispose();
        membersOrderPreferenceCache = null;
      }

      if (saveParticipantRegistry != null) {
        saveParticipantRegistry.dispose();
        saveParticipantRegistry = null;
      }

      if (dialogsFormToolkit != null) {
        dialogsFormToolkit.dispose();
        dialogsFormToolkit = null;
      }

//      if (fThemeListener != null) {
//        PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fThemeListener);
//        fThemeListener = null;
//      }

      DartX.todo();
      // QualifiedTypeNameHistory.getDefault().save();

      // must add here to guarantee that it is the first in the listener list
      DartX.todo();
      // OpenTypeHistory.shutdown();
    } finally {
      super.stop(context);
    }
  }

  /**
   * @deprecated Indirection added to avoid deprecated warning on file
   */
  // private org.eclipse.wst.jsdt.internal.corext.template.java.CodeTemplates
  // getOldCodeTemplateStoreInstance() {
  // return
  // org.eclipse.wst.jsdt.internal.corext.template.java.CodeTemplates.getInstance();
  // }

  @Deprecated
  @Override
  protected ImageRegistry createImageRegistry() {
    return DartPluginImages.getImageRegistry();
  }

//  /**
//   * Installs backwards compatibility for the preference store.
//   */
//  @Deprecated
//  private void ensurePreferenceStoreBackwardsCompatibility() {
//
//    IPreferenceStore store = getPreferenceStore();
//
  /*
   * Installs backwards compatibility: propagate the Java editor font from a pre-2.1 plug-in to the
   * Platform UI's preference store to preserve the Java editor font from a pre-2.1 workspace. This
   * is done only once.
   */
  //    String fontPropagatedKey = "fontPropagated"; //$NON-NLS-1$
  // if (store.contains(JFaceResources.TEXT_FONT)
  // && !store.isDefault(JFaceResources.TEXT_FONT)) {
  // if (!store.getBoolean(fontPropagatedKey))
  // PreferenceConverter.setValue(getDeprecatedWorkbenchPreferenceStore(),
  // PreferenceConstants.EDITOR_TEXT_FONT,
  // PreferenceConverter.getFontDataArray(store,
  // JFaceResources.TEXT_FONT));
  // }
  // store.setValue(fontPropagatedKey, true);

  /*
   * Backwards compatibility: set the Java editor font in this plug-in's preference store to let
   * older versions access it. Since 2.1 the Java editor font is managed by the workbench font
   * preference page.
   */
  // PreferenceConverter.putValue(
  // store,
  // JFaceResources.TEXT_FONT,
  // JFaceResources.getFontRegistry().getFontData(
  // PreferenceConstants.EDITOR_TEXT_FONT));

//    fFontPropertyChangeListener = new IPropertyChangeListener() {
//      @Override
//      public void propertyChange(PropertyChangeEvent event) {
//        if (PreferenceConstants.EDITOR_TEXT_FONT.equals(event.getProperty())) {
//          PreferenceConverter.putValue(getPreferenceStore(), JFaceResources.TEXT_FONT,
//              JFaceResources.getFontRegistry().getFontData(PreferenceConstants.EDITOR_TEXT_FONT));
//        }
//      }
//    };
//    JFaceResources.getFontRegistry().addListener(fFontPropertyChangeListener);

  /*
   * Backwards compatibility: propagate the Java editor tab width from a pre-3.0 plug-in to the new
   * preference key. This is done only once.
   */
  // final String oldTabWidthKey = DEPRECATED_EDITOR_TAB_WIDTH;
  // final String newTabWidthKey =
  // AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH;
  //    String tabWidthPropagatedKey = "tabWidthPropagated"; //$NON-NLS-1$
  // if (store.contains(oldTabWidthKey) && !store.isDefault(oldTabWidthKey)) {
  // if (!store.getBoolean(tabWidthPropagatedKey))
  // store.setValue(newTabWidthKey, store.getInt(oldTabWidthKey));
  // }
  // store.setValue(tabWidthPropagatedKey, true);

  /*
   * Backwards compatibility: set the Java editor tab width in this plug-in's preference store with
   * the old key to let older versions access it. Since 3.0 the tab width is managed by the extended
   * text editor and uses a new key.
   */
  // store.putValue(oldTabWidthKey, store.getString(newTabWidthKey));

  // fPropertyChangeListener = new IPropertyChangeListener() {
  // public void propertyChange(PropertyChangeEvent event) {
  // if (newTabWidthKey.equals(event.getProperty())) {
  // IPreferenceStore prefStore = getPreferenceStore();
  // prefStore.putValue(oldTabWidthKey,
  // prefStore.getString(newTabWidthKey));
  // }
  // }
  // };
  // store.addPropertyChangeListener(fPropertyChangeListener);

  /*
   * Backward compatibility for the refactoring preference key.
   */
  // store.setValue(
  // PreferenceConstants.REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD,
  // RefactoringCore.getConditionCheckingFailedSeverity());

  // The commented call above triggers the eager loading of the LTK core
  // plug-in
  // Since the condition checking failed severity is guaranteed to be of
  // RefactoringStatus.SEVERITY_WARNING,
  // we directly insert the inlined value of this constant
  // store.setToDefault(DEPRECATED_REFACTOR_ERROR_PAGE_SEVERITY_THRESHOLD);

  // if (!store.getBoolean(JavaDocLocations.PREF_JAVADOCLOCATIONS_MIGRATED)) {
  // JavaDocLocations.migrateToClasspathAttributes();
  // }

  // FormatterProfileStore.checkCurrentOptionsVersion();

  /*
   * Backward compatibility: migrate "alphabetic ordering" preference to point the sorter preference
   * to the alphabetic sorter.
   */
  //    String proposalOrderMigrated = "proposalOrderMigrated"; //$NON-NLS-1$
  // if (store.contains(DEPRECATED_CODEASSIST_ORDER_PROPOSALS)) {
  // if (!store.getBoolean(proposalOrderMigrated)) {
  // boolean alphabetic =
  // store.getBoolean(DEPRECATED_CODEASSIST_ORDER_PROPOSALS);
  // if (alphabetic)
  // store.setValue(PreferenceConstants.CODEASSIST_SORTER,
  //              "org.eclipse.wst.jsdt.ui.AlphabeticSorter"); //$NON-NLS-1$
  // }
  // }
  // store.setValue(proposalOrderMigrated, true);
//  }

  /**
   * @deprecated Indirection added to avoid deprecated warning on file
   */
  // private org.eclipse.wst.jsdt.internal.corext.template.java.Templates
  // getOldTemplateStoreInstance() {
  // return
  // org.eclipse.wst.jsdt.internal.corext.template.java.Templates.getInstance();
  // }

  @Deprecated
  private IWorkbenchPage internalGetActivePage() {
    IWorkbenchWindow window = getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      return null;
    }
    return window.getActivePage();
  }

  private synchronized ImageDescriptorRegistry internalGetImageDescriptorRegistry() {
    if (imageDescriptorRegistry == null) {
      imageDescriptorRegistry = new ImageDescriptorRegistry();
    }
    return imageDescriptorRegistry;
  }

  /**
   * Uninstalls backwards compatibility for the preference store.
   */
  private void uninstallPreferenceStoreBackwardsCompatibility() {
    //JFaceResources.getFontRegistry().removeListener(fFontPropertyChangeListener);

    if (propertyChangeListener != null) {
      getPreferenceStore().removePropertyChangeListener(propertyChangeListener);
    }
  }

}
