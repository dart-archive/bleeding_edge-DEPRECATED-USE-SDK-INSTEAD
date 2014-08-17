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
package com.google.dart.tools.ui.internal.text.completion;

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.text.dart.CompletionProposalCollector;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Completion proposal collector which creates proposals with filled in argument names.
 * <p>
 * This collector is used when {@link PreferenceConstants#CODEASSIST_FILL_ARGUMENT_NAMES} is
 * enabled.
 * <p/>
 */
public final class FillArgumentNamesCompletionProposalCollector extends CompletionProposalCollector {

  private final boolean fIsGuessArguments;

  public FillArgumentNamesCompletionProposalCollector(DartContentAssistInvocationContext context) {
    super(false);
    setInvocationContext(context);
    IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
    fIsGuessArguments = preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS);
    if (preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES)) {
      setRequireExtendedContext(true);
    }
  }

  @Override
  protected IDartCompletionProposal createDartCompletionProposal(CompletionProposal proposal) {
    switch (proposal.getKind()) {
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.ARGUMENT_LIST:
      case CompletionProposal.CONSTRUCTOR_INVOCATION:
//      case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER:
        return createMethodReferenceProposal(proposal);
      default:
        return super.createDartCompletionProposal(proposal);
    }
  }

  @SuppressWarnings("deprecation")
  private IDartCompletionProposal createMethodReferenceProposal(CompletionProposal methodProposal) {
    String completion = String.valueOf(methodProposal.getCompletion());
    // super class' behavior if this is not a normal completion or has no parameters
    if ((completion.length() == 0) || ((completion.length() == 1) && completion.charAt(0) == ')')
        || methodProposal.getParameterNames().length == 0 || getContext().isInJavadoc()) {
      return super.createDartCompletionProposal(methodProposal);
    }

    LazyDartCompletionProposal proposal = null;
    proposal = ParameterGuessingProposal.createProposal(
        methodProposal,
        getInvocationContext(),
        fIsGuessArguments);
    if (proposal == null) {
      proposal = new FilledArgumentNamesMethodProposal(methodProposal, getInvocationContext());
    }
    return proposal;
  }
}
