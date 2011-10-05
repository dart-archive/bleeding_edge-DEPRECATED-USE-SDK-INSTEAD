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
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.preferences.OverlayPreferenceStore;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * Abstract preference page which is used to wrap a
 * {@link org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock}.
 * 
 * @since 3.0
 */
public abstract class AbstractConfigurationBlockPreferencePage extends PreferencePage implements
    IWorkbenchPreferencePage {

  private IPreferenceConfigurationBlock fConfigurationBlock;
  private OverlayPreferenceStore fOverlayStore;

  /**
   * Creates a new preference page.
   */
  public AbstractConfigurationBlockPreferencePage() {
    setDescription();
    setPreferenceStore();
    fOverlayStore = new OverlayPreferenceStore(getPreferenceStore(),
        new OverlayPreferenceStore.OverlayKey[] {});
    fConfigurationBlock = createConfigurationBlock(fOverlayStore);
  }

  /*
   * @see PreferencePage#createControl(Composite)
   */
  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpId());
  }

  /*
   * @see DialogPage#dispose()
   */
  @Override
  public void dispose() {

    fConfigurationBlock.dispose();

    if (fOverlayStore != null) {
      fOverlayStore.stop();
      fOverlayStore = null;
    }

    super.dispose();
  }

  /*
   * @see IWorkbenchPreferencePage#init()
   */
  @Override
  public void init(IWorkbench workbench) {
  }

  /*
   * @see PreferencePage#performDefaults()
   */
  @Override
  public void performDefaults() {

    fOverlayStore.loadDefaults();
    fConfigurationBlock.performDefaults();

    super.performDefaults();
  }

  /*
   * @see PreferencePage#performOk()
   */
  @Override
  public boolean performOk() {

    fConfigurationBlock.performOk();

    fOverlayStore.propagate();

    DartToolsPlugin.flushInstanceScope();

    return true;
  }

  protected abstract IPreferenceConfigurationBlock createConfigurationBlock(
      OverlayPreferenceStore overlayPreferenceStore);

  /*
   * @see PreferencePage#createContents(Composite)
   */
  @Override
  protected Control createContents(Composite parent) {

    fOverlayStore.load();
    fOverlayStore.start();

    Control content = fConfigurationBlock.createControl(parent);

    initialize();

    Dialog.applyDialogFont(content);
    return content;
  }

  protected abstract String getHelpId();

  protected abstract void setDescription();

  protected abstract void setPreferenceStore();

  private void initialize() {
    fConfigurationBlock.initialize();
  }
}
