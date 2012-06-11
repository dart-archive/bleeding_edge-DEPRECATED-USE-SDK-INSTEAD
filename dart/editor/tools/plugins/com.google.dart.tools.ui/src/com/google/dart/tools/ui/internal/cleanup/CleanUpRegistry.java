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
package com.google.dart.tools.ui.internal.cleanup;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.cleanup.CleanUpOptions;
import com.google.dart.tools.ui.cleanup.ICleanUp;
import com.google.dart.tools.ui.cleanup.ICleanUpConfigurationUI;
import com.google.dart.tools.ui.cleanup.ICleanUpOptionsInitializer;
import com.google.dart.tools.ui.internal.cleanup.preference.CleanUpTabPage;
import com.google.dart.tools.ui.internal.cleanup.preference.ContributedCleanUpTabPage;

import com.ibm.icu.text.Collator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

/**
 * The clean up registry provides a set of clean ups and there corresponding UI representatives.
 * 
 * @since 3.4
 */
public class CleanUpRegistry {

  public static class CleanUpTabPageDescriptor {

    private static final String ATTRIBUTE_ID_CLASS = "class"; //$NON-NLS-1$
    private static final String ATTRIBUTE_ID_NAME = "name"; //$NON-NLS-1$
    private static final String ATTRIBUTE_NAME_KIND = "cleanUpKind"; //$NON-NLS-1$

    private final String fName;
    private final IConfigurationElement fElement;
    private int fKind;

    /**
     * @param element the configuration element
     */
    public CleanUpTabPageDescriptor(IConfigurationElement element) {
      fElement = element;
      fName = element.getAttribute(ATTRIBUTE_ID_NAME);
      String kind = fElement.getAttribute(ATTRIBUTE_NAME_KIND);
      fKind = getCleanUpKind(kind);
      if (fKind == -1) {
        DartToolsPlugin.logErrorMessage(Messages.format(
            FixMessages.CleanUpRegistry_WrongKindForConfigurationUI_error, new String[] {
                fName, element.getContributor().getName(), kind}));
      }
    }

    /**
     * @return new instance of a tab page
     */
    public CleanUpTabPage createTabPage() {
      try {
        ICleanUpConfigurationUI page = (ICleanUpConfigurationUI) fElement.createExecutableExtension(ATTRIBUTE_ID_CLASS);
        if (page instanceof CleanUpTabPage) {
          return (CleanUpTabPage) page;
        }

        return new ContributedCleanUpTabPage(page);
      } catch (final CoreException e) {
        DartToolsPlugin.log(e);
        return new ContributedCleanUpTabPage(new ErrorPage(e));
      }
    }

    /**
     * @return the kind of clean up
     */
    public int getKind() {
      return fKind;
    }

    /**
     * @return the name of the tab
     */
    public String getName() {
      return fName;
    }
  }

  private static class CleanUpDescriptor {

    private static final String ATTRIBUTE_ID_CLASS = "class"; //$NON-NLS-1$
    private static final String ATTRIBURE_ID_RUNAFTER = "runAfter"; //$NON-NLS-1$
    private static final String ATTRIBUTE_ID_ID = "id"; //$NON-NLS-1$

    private final IConfigurationElement fElement;
    private final String fId;
    private final String fRunAfter;

    /**
     * @param element the configuration element
     */
    public CleanUpDescriptor(IConfigurationElement element) {
      fElement = element;
      fId = element.getAttribute(ATTRIBUTE_ID_ID);
      fRunAfter = element.getAttribute(ATTRIBURE_ID_RUNAFTER);
    }

    /**
     * @return the clean up or <code>null</code> if the clean up could not be instantiated
     */
    public ICleanUp createCleanUp() {
      try {
        return (ICleanUp) fElement.createExecutableExtension(ATTRIBUTE_ID_CLASS);
      } catch (CoreException e) {
        String msg = Messages.format(FixMessages.CleanUpRegistry_cleanUpCreation_error,
            new String[] {
                fElement.getAttribute(ATTRIBUTE_ID_ID), fElement.getContributor().getName()});
        DartToolsPlugin.logErrorStatus(msg, e.getStatus());
        return null;
      }
    }

    /**
     * @return the unique id of this clean up
     */
    public String getId() {
      return fId;
    }

    /**
     * @return the id of the clean up which must run before this clean up or <strong>null</strong>
     *         if none specified
     */
    public String getRunAfter() {
      return fRunAfter;
    }
  }

  private static final class CleanUpInitializerDescriptor {

    private static final String ATTRIBUTE_NAME_CLASS = "class"; //$NON-NLS-1$
    private static final String ATTRIBUTE_NAME_KIND = "cleanUpKind"; //$NON-NLS-1$

    private final IConfigurationElement fElement;

    private final int fKind;

    private ICleanUpOptionsInitializer fOptionsProvider;

    public CleanUpInitializerDescriptor(IConfigurationElement element) {
      fElement = element;
      String kind = fElement.getAttribute(ATTRIBUTE_NAME_KIND);
      fKind = getCleanUpKind(kind);
      if (fKind == -1) {
        DartToolsPlugin.logErrorMessage(Messages.format(
            FixMessages.CleanUpRegistry_UnknownInitializerKind_errorMessage, new String[] {
                element.getContributor().getName(), kind}));
      }
    }

    @SuppressWarnings("unused")
    public int getKind() {
      return fKind;
    }

    public ICleanUpOptionsInitializer getOptionsInitializer() {
      if (fOptionsProvider == null) {
        try {
          fOptionsProvider = (ICleanUpOptionsInitializer) fElement.createExecutableExtension(ATTRIBUTE_NAME_CLASS);
        } catch (CoreException e) {
          DartToolsPlugin.log(e);
          fOptionsProvider = new DefaultCleanUpOptionsInitializer();
        }
      }
      return fOptionsProvider;
    }
  }

  private static final class ErrorPage implements ICleanUpConfigurationUI {

    private final Exception fException;

    private ErrorPage(Exception e) {
      fException = e;
    }

    @Override
    public Composite createContents(Composite parent) {
      Composite result = new Composite(parent, SWT.NONE);
      result.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      result.setLayout(new GridLayout(1, false));

      Text text = new Text(result, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
      text.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
      text.setText(Messages.format(FixMessages.CleanUpRegistry_ErrorTabPage_description,
          fException.getLocalizedMessage()));

      return result;
    }

    @Override
    public int getCleanUpCount() {
      return 0;
    }

    @Override
    public String getPreview() {
      return FixMessages.CleanUpRegistry_ErrorTabPage_preview;
    }

    @Override
    public int getSelectedCleanUpCount() {
      return 0;
    }

    @Override
    public void setOptions(CleanUpOptions options) {
    }
  }

  private static final String EXTENSION_POINT_NAME = "cleanUps"; //$NON-NLS-1$
  private static final String CLEAN_UP_CONFIGURATION_ELEMENT_NAME = "cleanUp"; //$NON-NLS-1$
  private static final String TABPAGE_CONFIGURATION_ELEMENT_NAME = "cleanUpConfigurationUI"; //$NON-NLS-1$

  private static final String CLEAN_UP_INITIALIZER_CONFIGURATION_ELEMENT_NAME = "cleanUpOptionsInitializer"; //$NON-NLS-1$
  private static final String ATTRIBUTE_KIND_TYPE_SAVE_ACTION = "saveAction"; //$NON-NLS-1$
  private static final String ATTRIBUTE_KIND_TYPE_CLEAN_UP = "cleanUp"; //$NON-NLS-1$

  private static int getCleanUpKind(String kind) {
    if (kind.equals(ATTRIBUTE_KIND_TYPE_CLEAN_UP)) {
      return CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS;
    } else if (kind.equals(ATTRIBUTE_KIND_TYPE_SAVE_ACTION)) {
      return CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS;
    } else {
      return -1;
    }
  }

  private static void sort(CleanUpDescriptor[] data) {
    int lastSwapI = -1;
    int lastSwapJ = -1;
    mainLoop : for (int i = 0; i < data.length; i++) {
      String runAfter = data[i].getRunAfter();
      if (runAfter == null) {
        continue;
      }
      int jStart = i + 1;
      for (int j = jStart; j < data.length; j++) {
        String jID = data[j].getId();
        if (runAfter.equals(jID)) {
          if (lastSwapI == i && j >= lastSwapJ) {
            DartToolsPlugin.logErrorMessage("Problem reading cleanUps extensions: cannot satisfy rule for '" + data[i].getId() + "' to runAfter '" + runAfter + "'"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            continue mainLoop;
          }
          lastSwapI = i;
          lastSwapJ = j;
          swap(data, i, j);
          i--;
          continue mainLoop;
        }
      }
      for (int j = 0; j < jStart; j++) {
        String jID = data[j].getId();
        if (runAfter.equals(jID)) {
          continue mainLoop;
        }
      }
      DartToolsPlugin.logErrorMessage("Problem reading cleanUps extensions: cannot satisfy rule for '" + data[i].getId() + "' to runAfter '" + runAfter + "' because the runAfter clean up does not exist."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
  }

  private static void swap(CleanUpDescriptor[] data, int i, int j) {
    CleanUpDescriptor o = data[i];
    data[i] = data[j];
    data[j] = o;
  }

  private CleanUpDescriptor[] fCleanUpDescriptors;

  private CleanUpTabPageDescriptor[] fPageDescriptors;

  private CleanUpInitializerDescriptor[] fCleanUpInitializerDescriptors;

  /**
   * Creates and returns the registered clean ups that don't fail upon creation.
   * 
   * @return an array of clean ups
   */
  public synchronized ICleanUp[] createCleanUps() {
    return createCleanUps(null);
  }

  /**
   * Creates and returns the registered clean ups that don't fail upon creation.
   * 
   * @param ids the ids of the clean ups to create, or <code>null</code> to create all
   * @return an array of clean ups
   * @since 3.5
   */
  public synchronized ICleanUp[] createCleanUps(Set<String> ids) {
    ensureCleanUpsRegistered();
    ArrayList<ICleanUp> result = new ArrayList<ICleanUp>(fCleanUpDescriptors.length);
    for (int i = 0; i < fCleanUpDescriptors.length; i++) {
      if (ids == null || ids.contains(fCleanUpDescriptors[i].getId())) {
        ICleanUp cleanUp = fCleanUpDescriptors[i].createCleanUp();
        if (cleanUp != null) {
          result.add(cleanUp);
        }
      }
    }
    return result.toArray(new ICleanUp[result.size()]);
  }

  /**
   * @param kind the kind of clean up for which to retrieve the configuratin pages
   * @return set of clean up tab page descriptors
   * @see CleanUpConstants#DEFAULT_CLEAN_UP_OPTIONS
   * @see CleanUpConstants#DEFAULT_SAVE_ACTION_OPTIONS
   */
  public synchronized CleanUpTabPageDescriptor[] getCleanUpTabPageDescriptors(int kind) {
    ensurePagesRegistered();

    ArrayList<CleanUpTabPageDescriptor> result = new ArrayList<CleanUpTabPageDescriptor>();
    for (int i = 0; i < fPageDescriptors.length; i++) {
      if (fPageDescriptors[i].getKind() == kind) {
        result.add(fPageDescriptors[i]);
      }
    }
    return result.toArray(new CleanUpTabPageDescriptor[result.size()]);
  }

  /**
   * Returns the default options for the specified clean up kind.
   * 
   * @param kind the kind of clean up for which to retrieve the options
   * @return the default options
   * @see CleanUpConstants#DEFAULT_CLEAN_UP_OPTIONS
   * @see CleanUpConstants#DEFAULT_SAVE_ACTION_OPTIONS
   */
  public MapCleanUpOptions getDefaultOptions(int kind) {
    ensureCleanUpInitializersRegistered();

    CleanUpOptions options = new CleanUpOptions();
    for (int i = 0; i < fCleanUpInitializerDescriptors.length; i++) {
      CleanUpInitializerDescriptor descriptor = fCleanUpInitializerDescriptors[i];
      descriptor.getOptionsInitializer().setDefaultOptions(options);
    }
    MapCleanUpOptions mapCleanUpOptions = new MapCleanUpOptions();
    mapCleanUpOptions.addAll(options);
    return mapCleanUpOptions;
  }

  private synchronized void ensureCleanUpInitializersRegistered() {
    if (fCleanUpInitializerDescriptors != null) {
      return;
    }

    ArrayList<CleanUpInitializerDescriptor> result = new ArrayList<CleanUpInitializerDescriptor>();

    IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(
        DartToolsPlugin.getPluginId(), EXTENSION_POINT_NAME);
    IConfigurationElement[] elements = point.getConfigurationElements();
    for (int i = 0; i < elements.length; i++) {
      IConfigurationElement element = elements[i];
      if (CLEAN_UP_INITIALIZER_CONFIGURATION_ELEMENT_NAME.equals(element.getName())) {
        result.add(new CleanUpInitializerDescriptor(element));
      }
    }

    fCleanUpInitializerDescriptors = result.toArray(new CleanUpInitializerDescriptor[result.size()]);
  }

  private synchronized void ensureCleanUpsRegistered() {
    if (fCleanUpDescriptors != null) {
      return;
    }

    final ArrayList<CleanUpDescriptor> descriptors = new ArrayList<CleanUpDescriptor>();

    IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(
        DartToolsPlugin.getPluginId(), EXTENSION_POINT_NAME);
    IConfigurationElement[] elements = point.getConfigurationElements();
    for (int i = 0; i < elements.length; i++) {
      IConfigurationElement element = elements[i];

      if (CLEAN_UP_CONFIGURATION_ELEMENT_NAME.equals(element.getName())) {
        descriptors.add(new CleanUpDescriptor(element));
      }
    }

    // Make sure we filter those who fail or misbehave
    for (int i = 0; i < descriptors.size(); i++) {
      final CleanUpDescriptor cleanUpDescriptor = descriptors.get(i);
      final boolean disable[] = new boolean[1];
      ISafeRunnable runnable = new SafeRunnable() {

        @Override
        public void handleException(Throwable t) {
          disable[0] = true;
          String message = Messages.format(FixMessages.CleanUpRegistry_cleanUpCreation_error,
              new String[] {
                  cleanUpDescriptor.getId(), cleanUpDescriptor.fElement.getContributor().getName()});
          IStatus status = new Status(IStatus.ERROR, DartUI.ID_PLUGIN, IStatus.ERROR, message, t);
          DartToolsPlugin.log(status);
        }

        @Override
        public void run() throws Exception {
          ICleanUp cleanUp = cleanUpDescriptor.createCleanUp();
          if (cleanUp == null) {
            disable[0] = true;
          } else {
            cleanUp.setOptions(new CleanUpOptions());
            String[] enbledSteps = cleanUp.getStepDescriptions();
            if (enbledSteps != null && enbledSteps.length > 0) {
              DartToolsPlugin.logErrorMessage(Messages.format(
                  FixMessages.CleanUpRegistry_cleanUpAlwaysEnabled_error, new String[] {
                      cleanUpDescriptor.getId(),
                      cleanUpDescriptor.fElement.getContributor().getName()}));
              disable[0] = true;
            }
          }
        }

      };
      SafeRunner.run(runnable);
      if (disable[0]) {
        descriptors.remove(i--);
      }
    }

    fCleanUpDescriptors = descriptors.toArray(new CleanUpDescriptor[descriptors.size()]);
    sort(fCleanUpDescriptors);

  }

  private synchronized void ensurePagesRegistered() {
    if (fPageDescriptors != null) {
      return;
    }

    ArrayList<CleanUpTabPageDescriptor> result = new ArrayList<CleanUpTabPageDescriptor>();

    IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(DartUI.ID_PLUGIN,
        EXTENSION_POINT_NAME);
    IConfigurationElement[] elements = point.getConfigurationElements();
    for (int i = 0; i < elements.length; i++) {
      IConfigurationElement element = elements[i];

      if (TABPAGE_CONFIGURATION_ELEMENT_NAME.equals(element.getName())) {
        result.add(new CleanUpTabPageDescriptor(element));
      }
    }

    fPageDescriptors = result.toArray(new CleanUpTabPageDescriptor[result.size()]);
    Arrays.sort(fPageDescriptors, new Comparator<CleanUpTabPageDescriptor>() {
      @Override
      public int compare(CleanUpTabPageDescriptor o1, CleanUpTabPageDescriptor o2) {
        String name1 = o1.getName();
        String name2 = o2.getName();
        return Collator.getInstance().compare(name1.replaceAll("&", ""), name2.replaceAll("&", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
    });
  }

}
