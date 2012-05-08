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

import com.google.dart.tools.search.internal.core.text.TextSearchEngineRegistry;
import com.google.dart.tools.search.internal.ui.util.ExceptionHandler;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search2.internal.ui.InternalSearchUI;
import com.google.dart.tools.search2.internal.ui.text2.TextSearchQueryProviderRegistry;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The plug-in runtime class for Search plug-in
 */
public class SearchPlugin extends AbstractUIPlugin {

  private static class WindowRef {
    public IWorkbenchWindow window;
  }

  public static final String SEARCH_PAGE_EXTENSION_POINT = "searchPages"; //$NON-NLS-1$

  public static final String SORTER_EXTENSION_POINT = "searchResultSorters"; //$NON-NLS-1$

  /**
   * Filtered search marker type (value
   * <code>"com.google.dart.tools.search.filteredsearchmarker"</code>).
   * 
   * @see org.eclipse.core.resources.IMarker
   */
  public static final String FILTERED_SEARCH_MARKER = NewSearchUI.PLUGIN_ID
      + ".filteredsearchmarker"; //$NON-NLS-1$

  /**
   * Search annotation type (value <code>"com.google.dart.tools.search.results"</code>).
   */
  public static final String SEARCH_ANNOTATION_TYPE = NewSearchUI.PLUGIN_ID + ".results"; //$NON-NLS-1$

  /**
   * Filtered search annotation type (value
   * <code>"com.google.dart.tools.search.filteredResults"</code>).
   */
  public static final String FILTERED_SEARCH_ANNOTATION_TYPE = NewSearchUI.PLUGIN_ID
      + ".filteredResults"; //$NON-NLS-1$

  /** Status code describing an internal error */
  public static final int INTERNAL_ERROR = 1;

  private static SearchPlugin fgSearchPlugin;

  /**
   * Beeps using the display of the active workbench window.
   */
  public static void beep() {
    getActiveWorkbenchShell().getDisplay().beep();
  }

  /**
   * @return Returns the active workbench window's currrent page.
   */
  public static IWorkbenchPage getActivePage() {
    return getActiveWorkbenchWindow().getActivePage();
  }

  /**
   * @return Returns the shell of the active workbench window.
   */
  public static Shell getActiveWorkbenchShell() {
    IWorkbenchWindow window = getActiveWorkbenchWindow();
    if (window != null) {
      return window.getShell();
    }
    return null;
  }

  /**
   * Returns the active workbench window.
   * 
   * @return returns <code>null</code> if the active window is not a workbench window
   */
  public static IWorkbenchWindow getActiveWorkbenchWindow() {
    IWorkbenchWindow window = fgSearchPlugin.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      final WindowRef windowRef = new WindowRef();
      Display.getDefault().syncExec(new Runnable() {
        @Override
        public void run() {
          setActiveWorkbenchWindow(windowRef);
        }
      });
      return windowRef.window;
    }
    return window;
  }

  /**
   * @return Returns the search plugin instance.
   */
  public static SearchPlugin getDefault() {
    return fgSearchPlugin;
  }

  public static String getID() {
    return NewSearchUI.PLUGIN_ID;
  }

  /**
   * @return Returns the workbench from which this plugin has been loaded.
   */
  public static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  /**
   * Log status to platform log
   * 
   * @param status the status to log
   */
  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  public static void log(Throwable e) {
    log(new Status(
        IStatus.ERROR,
        NewSearchUI.PLUGIN_ID,
        INTERNAL_ERROR,
        SearchMessages.SearchPlugin_internal_error,
        e));
  }

  static boolean setAutoBuilding(boolean state) {
    IWorkspaceDescription workspaceDesc = getWorkspace().getDescription();
    boolean isAutobuilding = workspaceDesc.isAutoBuilding();

    if (isAutobuilding != state) {
      workspaceDesc.setAutoBuilding(state);
      try {
        getWorkspace().setDescription(workspaceDesc);
      } catch (CoreException ex) {
        ExceptionHandler.handle(
            ex,
            SearchMessages.Search_Error_setDescription_title,
            SearchMessages.Search_Error_setDescription_message);
      }
    }
    return isAutobuilding;
  }

  private static void setActiveWorkbenchWindow(WindowRef windowRef) {
    windowRef.window = null;
    Display display = Display.getCurrent();
    if (display == null) {
      return;
    }
    Control shell = display.getActiveShell();
    while (shell != null) {
      Object data = shell.getData();
      if (data instanceof IWorkbenchWindow) {
        windowRef.window = (IWorkbenchWindow) data;
        return;
      }
      shell = shell.getParent();
    }
    Shell shells[] = display.getShells();
    for (int i = 0; i < shells.length; i++) {
      Object data = shells[i].getData();
      if (data instanceof IWorkbenchWindow) {
        windowRef.window = (IWorkbenchWindow) data;
        return;
      }
    }
  }

  private List<SearchPageDescriptor> fPageDescriptors;

  private List<SorterDescriptor> fSorterDescriptors;

  private TextSearchEngineRegistry fTextSearchEngineRegistry;

  private TextSearchQueryProviderRegistry fTextSearchQueryProviderRegistry;

  public SearchPlugin() {
    super();
    Assert.isTrue(fgSearchPlugin == null);
    fgSearchPlugin = this;
    fTextSearchEngineRegistry = null;
    fTextSearchQueryProviderRegistry = null;
  }

  public IDialogSettings getDialogSettingsSection(String name) {
    IDialogSettings dialogSettings = getDialogSettings();
    IDialogSettings section = dialogSettings.getSection(name);
    if (section == null) {
      section = dialogSettings.addNewSection(name);
    }
    return section;
  }

  /**
   * @param pageId the page id
   * @return Returns all search pages contributed to the workbench.
   */
  public List<SearchPageDescriptor> getEnabledSearchPageDescriptors(String pageId) {
    Iterator<SearchPageDescriptor> iter = getSearchPageDescriptors().iterator();
    List<SearchPageDescriptor> enabledDescriptors = new ArrayList<SearchPageDescriptor>(5);
    while (iter.hasNext()) {
      SearchPageDescriptor desc = iter.next();
      if (desc.isEnabled() || desc.getId().equals(pageId)) {
        enabledDescriptors.add(desc);
      }
    }
    return enabledDescriptors;
  }

  /**
   * @return Returns all search pages contributed to the workbench.
   */
  public List<SearchPageDescriptor> getSearchPageDescriptors() {
    if (fPageDescriptors == null) {
      IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
          NewSearchUI.PLUGIN_ID,
          SEARCH_PAGE_EXTENSION_POINT);
      fPageDescriptors = createSearchPageDescriptors(elements);
    }
    return fPageDescriptors;
  }

  /**
   * @return Returns all sorters contributed to the workbench.
   */
  public List<SorterDescriptor> getSorterDescriptors() {
    if (fSorterDescriptors == null) {
      IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
          NewSearchUI.PLUGIN_ID,
          SORTER_EXTENSION_POINT);
      fSorterDescriptors = createSorterDescriptors(elements);
    }
    return fSorterDescriptors;
  }

  public TextSearchEngineRegistry getTextSearchEngineRegistry() {
    if (fTextSearchEngineRegistry == null) {
      fTextSearchEngineRegistry = new TextSearchEngineRegistry();
    }
    return fTextSearchEngineRegistry;
  }

  public TextSearchQueryProviderRegistry getTextSearchQueryProviderRegistry() {
    if (fTextSearchQueryProviderRegistry == null) {
      fTextSearchQueryProviderRegistry = new TextSearchQueryProviderRegistry();
    }
    return fTextSearchQueryProviderRegistry;
  }

  /*
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
  }

  /*
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    InternalSearchUI.shutdown();
    super.stop(context);
    fgSearchPlugin = null;
  }

  /**
   * Creates all necessary search page nodes.
   * 
   * @param elements the configuration elements
   * @return the created SearchPageDescriptor
   */
  @SuppressWarnings("unchecked")
  private List<SearchPageDescriptor> createSearchPageDescriptors(IConfigurationElement[] elements) {
    List<SearchPageDescriptor> result = new ArrayList<SearchPageDescriptor>(5);
    for (int i = 0; i < elements.length; i++) {
      IConfigurationElement element = elements[i];
      if (SearchPageDescriptor.PAGE_TAG.equals(element.getName())) {
        SearchPageDescriptor desc = new SearchPageDescriptor(element);
        result.add(desc);
      }
    }
    Collections.sort(result);
    return result;
  }

  /**
   * Creates all necessary sorter description nodes.
   * 
   * @param elements the configuration elements
   * @return the created SorterDescriptor
   */
  private List<SorterDescriptor> createSorterDescriptors(IConfigurationElement[] elements) {
    List<SorterDescriptor> result = new ArrayList<SorterDescriptor>(5);
    for (int i = 0; i < elements.length; i++) {
      IConfigurationElement element = elements[i];
      if (SorterDescriptor.SORTER_TAG.equals(element.getName())) {
        result.add(new SorterDescriptor(element));
      }
    }
    return result;
  }
}
