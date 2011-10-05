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

import org.eclipse.osgi.util.NLS;

public class SaveParticipantMessages extends NLS {
  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.internal.text.editor.saveparticipant.SaveParticipantMessages"; //$NON-NLS-1$

  public static String CleanUpSaveParticipantConfigurationModifyDialog_SelectAnAction_Error;

  public static String CleanUpSaveParticipantConfigurationModifyDialog_XofYSelected_Label;

  public static String CleanUpSaveParticipantPreferenceConfiguration_AdditionalActions_Checkbox;
  public static String CleanUpSaveParticipantPreferenceConfiguration_CleanUpActionsTopNodeName_Checkbox;
  public static String CleanUpSaveParticipantPreferenceConfiguration_CleanUpSaveParticipantConfiguration_Title;
  public static String CleanUpSaveParticipantPreferenceConfiguration_Configure_Button;
  public static String CleanUpSaveParticipantPreferenceConfiguration_ConfigureFormatter_Link;
  public static String CleanUpSaveParticipantPreferenceConfiguration_ConfigureImports_Link;

  public static String CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePaae_FormatAllLines_Radio;

  public static String CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_FormatOnlyChangedRegions_Radio;
  public static String CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_FormatSource_Checkbox;
  public static String CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_OrganizeImports_Checkbox;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, SaveParticipantMessages.class);
  }

  private SaveParticipantMessages() {
  }
}
