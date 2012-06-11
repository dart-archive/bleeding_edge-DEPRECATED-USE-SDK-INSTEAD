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
package com.google.dart.tools.ui.internal.text.editor.saveparticipant;

import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.dialogs.fields.DialogField;
import com.google.dart.tools.ui.internal.dialogs.fields.IDialogFieldListener;
import com.google.dart.tools.ui.internal.dialogs.fields.SelectionButtonDialogField;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public abstract class AbstractSaveParticipantPreferenceConfiguration implements
    ISaveParticipantPreferenceConfiguration {

  /**
   * Preference prefix that is appended to the id of {@link SaveParticipantDescriptor save
   * participants}.
   * <p>
   * Value is of type <code>Boolean</code>.
   * </p>
   * 
   * @see SaveParticipantDescriptor
   */
  private static final String EDITOR_SAVE_PARTICIPANT_PREFIX = "editor_save_participant_"; //$NON-NLS-1$

  private SelectionButtonDialogField fEnableField;
  private Control fConfigControl;
  private IScopeContext fContext;
  private ControlEnableState fConfigControlEnabledState;

  /**
   * {@inheritDoc}
   */
  @Override
  public Control createControl(Composite parent, IPreferencePageContainer container) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
    composite.setLayoutData(gridData);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);

    fEnableField = new SelectionButtonDialogField(SWT.CHECK);
    fEnableField.setLabelText(getPostSaveListenerName());
    fEnableField.doFillIntoGrid(composite, 1);

    createConfigControl(composite, container);

    return composite;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disableProjectSettings() {
    fContext.getNode(DartUI.ID_PLUGIN).remove(getPreferenceKey());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void enableProjectSettings() {
    fContext.getNode(DartUI.ID_PLUGIN).putBoolean(getPreferenceKey(), fEnableField.isSelected());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasSettingsInScope(IScopeContext context) {
    return context.getNode(DartUI.ID_PLUGIN).get(getPreferenceKey(), null) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(final IScopeContext context, IAdaptable element) {
    boolean enabled = isEnabled(context);
    fEnableField.setSelection(enabled);

    if (fConfigControl != null && !enabled) {
      fConfigControlEnabledState = ControlEnableState.disable(fConfigControl);
    }

    fEnableField.setDialogFieldListener(new IDialogFieldListener() {
      @Override
      public void dialogFieldChanged(DialogField field) {
        enableConfigControl(fEnableField.isSelected());
      }
    });

    fContext = context;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("deprecation")
  @Override
  public boolean isEnabled(IScopeContext context) {
    IEclipsePreferences node;
    if (hasSettingsInScope(context)) {
      node = context.getNode(DartUI.ID_PLUGIN);
    } else {
      node = new InstanceScope().getNode(DartUI.ID_PLUGIN);
    }
    IEclipsePreferences defaultNode = new DefaultScope().getNode(DartUI.ID_PLUGIN);

    String key = getPreferenceKey();
    return node.getBoolean(key, defaultNode.getBoolean(key, false));
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("deprecation")
  @Override
  public void performDefaults() {
    String key = getPreferenceKey();
    boolean defaultEnabled = new DefaultScope().getNode(DartUI.ID_PLUGIN).getBoolean(key, false);
    fContext.getNode(DartUI.ID_PLUGIN).putBoolean(key, defaultEnabled);
    fEnableField.setSelection(defaultEnabled);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void performOk() {
  }

  protected void createConfigControl(Composite composite, IPreferencePageContainer container) {
    // Default has no specific controls
  }

  protected void enableConfigControl(boolean isEnabled) {
    fContext.getNode(DartUI.ID_PLUGIN).putBoolean(getPreferenceKey(), isEnabled);
    if (fConfigControl != null) {
      if (fConfigControlEnabledState != null) {
        fConfigControlEnabledState.restore();
        fConfigControlEnabledState = null;
      } else {
        fConfigControlEnabledState = ControlEnableState.disable(fConfigControl);
      }
    }
  }

  /**
   * @param enabled true if this save action has been enabled by user, false otherwise
   */
  protected void enabled(boolean enabled) {
  }

  /**
   * The id of the post save listener managed by this configuration block, not null
   */
  protected abstract String getPostSaveListenerId();

  /**
   * The name of the post save listener managed by this configuration block, not null
   */
  protected abstract String getPostSaveListenerName();

  private String getPreferenceKey() {
    return EDITOR_SAVE_PARTICIPANT_PREFIX + getPostSaveListenerId();
  }
}
