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
package com.google.dart.tools.ui.internal.text.dart;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class DartTextMessages extends NLS {

  private static final String BUNDLE_NAME = DartTextMessages.class.getName();

  public static String CompletionProcessor_error_accessing_title;
  public static String CompletionProcessor_error_accessing_message;
  public static String CompletionProcessor_error_notOnBuildPath_title;
  public static String CompletionProcessor_error_notOnBuildPath_message;
  public static String CompletionProcessor_error_javaCompletion_took_too_long_message;
  public static String CompletionProposalComputerRegistry_messageAvoidanceHint;
  public static String CompletionProposalComputerRegistry_messageAvoidanceHintWithWarning;
  public static String ContentAssistProcessor_all_disabled_message;
  public static String ContentAssistProcessor_all_disabled_preference_link;
  public static String ContentAssistProcessor_all_disabled_title;
  public static String FilledArgumentNamesMethodProposal_error_msg;
  public static String ParameterGuessingProposal_error_msg;
  public static String GetterSetterCompletionProposal_getter_label;
  public static String GetterSetterCompletionProposal_setter_label;
  public static String MethodCompletionProposal_constructor_label;
  public static String MethodCompletionProposal_method_label;
  public static String ContentAssistProcessor_computing_proposals;
  public static String ContentAssistProcessor_collecting_proposals;
  public static String ContentAssistProcessor_sorting_proposals;
  public static String ContentAssistProcessor_computing_contexts;
  public static String ContentAssistProcessor_collecting_contexts;
  public static String ContentAssistProcessor_sorting_contexts;
  public static String CompletionProposalComputerDescriptor_illegal_attribute_message;
  public static String CompletionProposalComputerDescriptor_reason_invalid;
  public static String CompletionProposalComputerDescriptor_reason_instantiation;
  public static String CompletionProposalComputerDescriptor_reason_runtime_ex;
  public static String CompletionProposalComputerDescriptor_reason_API;
  public static String CompletionProposalComputerDescriptor_reason_performance;
  public static String CompletionProposalComputerDescriptor_blame_message;
  public static String CompletionProposalComputerRegistry_invalid_message;
  public static String CompletionProposalComputerRegistry_error_dialog_title;
  public static String ContentAssistProcessor_defaultProposalCategory;
  public static String ContentAssistProcessor_toggle_affordance_press_gesture;
  public static String ContentAssistProcessor_toggle_affordance_click_gesture;
  public static String ContentAssistProcessor_toggle_affordance_update_message;
  public static String ContentAssistProcessor_empty_message;
  public static String ContentAssistHistory_serialize_error;
  public static String ContentAssistHistory_deserialize_error;
  public static String ProposalSorterHandle_blame;

  static {
    NLS.initializeMessages(BUNDLE_NAME, DartTextMessages.class);
  }

  private DartTextMessages() {
    // Do not instantiate
  }
}
