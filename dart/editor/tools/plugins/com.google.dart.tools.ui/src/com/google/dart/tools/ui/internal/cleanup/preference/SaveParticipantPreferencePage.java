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
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IScopeContext;

/**
 * Configures Dart Editor save participant preferences.
 */
public final class SaveParticipantPreferencePage extends
    AbstractConfigurationBlockPreferenceAndPropertyPage {

  public static final String PROPERTY_PAGE_ID = "com.google.dart.tools.ui.internal.cleanup.preference.CleanUpPreferencePage"; //$NON-NLS-1$
  public static final String PREFERENCE_PAGE_ID = "com.google.dart.tools.ui.internal.cleanup.preference.CleanUpPreferencePage"; //$NON-NLS-1$

  /**
   * {@inheritDoc}
   */
  @Override
  protected IPreferenceAndPropertyConfigurationBlock createConfigurationBlock(IScopeContext context) {
    return new SaveParticipantConfigurationBlock(context, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getHelpId() {
    return DartHelpContextIds.DART_EDITOR_PREFERENCE_PAGE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getPreferencePageID() {
    return PREFERENCE_PAGE_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getPropertyPageID() {
    return null;//PROPERTY_PAGE_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean hasProjectSpecificOptions(IProject project) {
    return DartToolsPlugin.getDefault().getSaveParticipantRegistry().hasSettingsInScope(
        new ProjectScope(project));
  }
}
