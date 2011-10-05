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
package com.google.dart.tools.ui.internal.text.editor.saveparticipant;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Preference UI to configure details of a save participant on the the Java &gt; Editor &gt; Save
 * Participants preference page.
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface ISaveParticipantPreferenceConfiguration {

  /**
   * Creates a control that will be displayed on the Java &gt; Editor &gt; Save Participants
   * preference page to edit the details of a save participant.
   * 
   * @param parent the parent composite to which to add the preferences control
   * @param container the container in which this preference configuration is displayed
   * @return the control that was added to the <code>parent</code>
   */
  Control createControl(Composite parent, IPreferencePageContainer container);

  /**
   * Called when project specific settings have been disabled
   */
  void disableProjectSettings();

  /**
   * Called when the preference page is being disposed.
   * <p>
   * Implementations should free any resources they are holding on to.
   * </p>
   */
  void dispose();

  /**
   * Called when project specific settings have been enabled
   */
  void enableProjectSettings();

  /**
   * Called when the property page is opened to check whether this has enabled settings in the given
   * context.
   * 
   * @param context the context to check
   * @return true if this has settings in context
   */
  boolean hasSettingsInScope(IScopeContext context);

  /**
   * Called after creating the control.
   * <p>
   * Implementations should load the preferences values and update the controls accordingly.
   * </p>
   * 
   * @param context the context from which to load the preference values from
   * @param element the element to configure, or null if this configures the workspace settings
   */
  void initialize(IScopeContext context, IAdaptable element);

  /**
   * Called when a compilation unit is saved.
   * <p>
   * 
   * @param context the context in which the compilation unit is saved
   * @return true if the corresponding {@link IPostSaveListener} needs to be informed
   */
  boolean isEnabled(IScopeContext context);

  /**
   * Called when the <code>Defaults</code> button is pressed on the preference page.
   * <p>
   * Implementation should reset any preference settings to their default values and adjust the
   * controls accordingly.
   * </p>
   */
  void performDefaults();

  /**
   * Called when the <code>OK</code> button is pressed on the preference page.
   * <p>
   * Implementations should commit the configured preference settings into their form of preference
   * storage.
   * </p>
   */
  void performOk();

}
