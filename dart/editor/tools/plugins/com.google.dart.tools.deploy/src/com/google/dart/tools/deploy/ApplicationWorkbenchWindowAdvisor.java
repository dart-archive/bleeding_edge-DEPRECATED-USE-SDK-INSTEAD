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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * The WorkbenchWindowAdvisor for the Dart Editor.
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

  /**
   * Preference nodes to filter (in Regexp form). Dependent plugins contribute a host of preference
   * nodes that have no business in our preference pages. This list specifies ids to filter. NOTE:
   * "org.eclipse.ui.preferencePages.Workbench" is a root node; filtering it removes ALL of its
   * children if we want to selectively add some children back in, this is the node to start with.
   */
  private static String[] PREF_BLACKLIST = {
      "org.eclipse.team.*", "org.eclipse.help.ui.*", "org.eclipse.update.*",
      "org.eclipse.equinox.internal.p2.*",
      "org.eclipse.ui.externaltools.ExternalToolsPreferencePage",
      "org.eclipse.debug.ui.DebugPreferencePage", "org.eclipse.ui.preferencePages.Perspectives",
      "org.eclipse.search.preferences.SearchPreferencePage", "org.eclipse.equinox.security.ui.*",
      "org.eclipse.compare.internal.ComparePreferencePage", "org.eclipse.ui.net.NetPreferences",
      "org.eclipse.ui.preferencePages.Keys", "org.eclipse.ui.preferencePages.ContentTypes",
      "org.eclipse.ui.preferencePages.Editors", "org.eclipse.ui.preferencePages.Views",
      "org.eclipse.ui.preferencePages.Workspace", "org.eclipse.ui.preferencePages.Workbench"};

  private static Object callReflectMethod(Object obj, String methodName) throws Exception {
    Method method = obj.getClass().getMethod(methodName);
    method.setAccessible(true);
    return method.invoke(obj);
  }

  private static void callReflectMethod(Object obj, String methodName, int param) throws Exception {
    Method method = obj.getClass().getDeclaredMethod(methodName, int.class);
    method.setAccessible(true);
    method.invoke(obj, param);
  }

  private static void callReflectMethod(Object obj, String methodName, long param) throws Exception {
    Method method = obj.getClass().getDeclaredMethod(methodName, long.class);
    method.setAccessible(true);
    method.invoke(obj, param);
  }

  private static Object getReflectField(Object obj, String fieldName) throws Exception {
    Field field = obj.getClass().getField(fieldName);
    field.setAccessible(true);
    return field.get(obj);
  }

  public ApplicationWorkbenchWindowAdvisor(ApplicationWorkbenchAdvisor wbAdvisor,
      IWorkbenchWindowConfigurer configurer) {
    super(configurer);
  }

  @Override
  public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
    return new ApplicationActionBarAdvisor(configurer);
  }

  @Override
  public void createWindowContents(Shell shell) {
    if (DartCore.isMac()) {
      enableFullScreenMode(shell);
    }

    super.createWindowContents(shell);
  }

  @Override
  public void postWindowOpen() {
    filterUnwantedPreferenceNodes();

    setDebugPreferences();

    super.postWindowOpen();

    // Turn off the ability to move the toolbars around.
    getWindowConfigurer().getActionBarConfigurer().getCoolBarManager().setLockLayout(true);
  }

  @Override
  public void preWindowOpen() {
    IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
    configurer.setInitialSize(new Point(1200, 900));
    configurer.setShowCoolBar(true);
    configurer.setShowStatusLine(true);
    configurer.setShowProgressIndicator(true);
    configurer.setTitle("Dart Editor"); //$NON-NLS-1$

    // make sure we always save and restore workspace state
    configurer.getWorkbenchConfigurer().setSaveAndRestore(true);
  }

  /**
   * This method should only be called under macos.
   * <p>
   * It makes a best effort to turn on fullscreen mode; if it fails it does not complain to the
   * user. We do this work reflectively so that the code can continue to compile for other
   * architectures.
   * 
   * @param shell the main application shell
   */
  private void enableFullScreenMode(Shell shell) {
    final int FULL_SCREEN_MODE = 1 << 7;

    try {
      // NSView nsView = shell.view;
      // NSWindow nsWindow = nsView.window();
      // nsWindow.setCollectionBehavior(behavior);

      if (DartCore.is32Bit()) {
        Object nsView = getReflectField(shell, "view");
        Object nsWindow = callReflectMethod(nsView, "window");
        callReflectMethod(nsWindow, "setCollectionBehavior", FULL_SCREEN_MODE);
      } else {
        Object nsView = getReflectField(shell, "view");
        Object nsWindow = callReflectMethod(nsView, "window");
        callReflectMethod(nsWindow, "setCollectionBehavior", (long) FULL_SCREEN_MODE);
      }
    } catch (Throwable t) {
      Activator.logError(t);
    }
  }

  private void filterUnwantedPreferenceNodes() {
    PreferenceManager preferenceManager = PlatformUI.getWorkbench().getPreferenceManager();
    for (Object elem : preferenceManager.getElements(PreferenceManager.POST_ORDER)) {
      if (elem instanceof IPreferenceNode) {
        IPreferenceNode node = (IPreferenceNode) elem;
        if (isBlacklisted(node)) {
          if (!preferenceManager.remove(node)) {
            for (IPreferenceNode rootNode : preferenceManager.getRootSubNodes()) {
              if (rootNode.findSubNode(node.getId()) != null) {
                rootNode.remove(node);
              }
            }
          }
        }
      }
    }
  }

  private boolean isBlacklisted(IPreferenceNode node) {
    String nodeId = node.getId();
    if (nodeId.matches("com.google.dart.tools.ui.theme.preferences.ThemePreferencePage")
        && !DartCoreDebug.ENABLE_THEMES) {
      return true;
    }
    for (String blacklistedId : PREF_BLACKLIST) {
      if (nodeId.matches(blacklistedId)) {
        return true;
      }
    }
    return false;
  }

  private void setDebugPreferences() {
    IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode("org.eclipse.debug.ui"); //$NON-NLS-N$
    if (prefs != null) {
      prefs.put("org.eclipse.debug.ui.switch_to_perspective", "never"); //$NON-NLS-N$
      prefs.put("org.eclipse.debug.ui.switch_to_perspective_on_suspend", "never"); //$NON-NLS-N$
      prefs.put("org.eclipse.debug.ui.switch_perspective_on_suspend", "never"); //$NON-NLS-N$
      prefs.putBoolean("org.eclipse.debug.ui.activate_debug_view", false); //$NON-NLS-N$
    }
  }

}
