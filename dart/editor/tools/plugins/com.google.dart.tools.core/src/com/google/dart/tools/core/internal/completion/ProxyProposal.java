/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.engine.services.completion.ProposalKind;
import com.google.dart.tools.core.completion.CompletionProposal;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Wrap String-based completion proposals for use in legacy char[]-based client code.
 */
public class ProxyProposal extends CompletionProposal {

  private com.google.dart.engine.services.completion.CompletionProposal proposal;

  public ProxyProposal(com.google.dart.engine.services.completion.CompletionProposal proposal) {
    this.proposal = proposal;
  }

  @Override
  public char[][] findParameterNames(IProgressMonitor monitor) {
    return getParameterNames();
  }

  @Override
  public char[] getCompletion() {
    return proposal.getCompletion().toCharArray();
  }

  @Override
  public int getCompletionLocation() {
    return proposal.getLocation() - 1;
  }

  @Override
  public char[] getDeclarationSignature() {
    return proposal.getDeclaringType().toCharArray();
  }

  @Override
  public int getKind() {
    switch (proposal.getKind()) {
      case CLASS:
        return CompletionProposal.TYPE_REF;
      case CLASS_ALIAS:
        return CompletionProposal.TYPE_REF;
      case CONSTRUCTOR:
        return CompletionProposal.METHOD_REF;
      case FIELD:
        return CompletionProposal.FIELD_REF;
      case FUNCTION:
        return CompletionProposal.METHOD_REF;
      case FUNCTION_ALIAS:
        return CompletionProposal.TYPE_REF;
      case GETTER:
        return CompletionProposal.FIELD_REF;
      case IMPORT:
        return CompletionProposal.TYPE_IMPORT;
      case LIBRARY_PREFIX:
        return CompletionProposal.LIBRARY_PREFIX;
      case METHOD:
        return CompletionProposal.METHOD_REF;
      case PARAMETER:
        return CompletionProposal.LOCAL_VARIABLE_REF;
      case SETTER:
        return CompletionProposal.FIELD_REF;
      case TYPE_VARIABLE:
        return CompletionProposal.TYPE_REF;
      case VARIABLE:
        return CompletionProposal.LOCAL_VARIABLE_REF;
      default:
        return 0;
    }
  }

  @Override
  public char[] getName() {
    return proposal.getCompletion().toCharArray();
  }

  @Override
  public char[][] getParameterNames() {
    return copyStrings(proposal.getParameterNames());
  }

  @Override
  public char[][] getParameterTypeNames() {
    return copyStrings(proposal.getParameterTypes());
  }

  @Override
  public int getPositionalParameterCount() {
    return proposal.getPositionalParameterCount();
  }

  @Override
  public int getReplaceEnd() {
    return proposal.getLocation() + proposal.getReplacementLength();
  }

  @Override
  public int getReplaceStart() {
    return proposal.getLocation();
  }

  @Override
  public char[] getReturnTypeName() {
    return proposal.getReturnType().toCharArray();
  }

  @Override
  public char[] getSignature() {
    return proposal.getCompletion().toCharArray();
  }

  @Override
  public boolean hasNamedParameters() {
    return proposal.hasNamed();
  }

  @Override
  public boolean hasOptionalParameters() {
    return proposal.hasPositional() || proposal.hasNamed();
  }

  @Override
  public boolean isConstructor() {
    return proposal.getKind() == ProposalKind.CONSTRUCTOR;
  }

  public boolean isGetter() {
    return proposal.getKind() == ProposalKind.GETTER;
  }

  public boolean isSetter() {
    return proposal.getKind() == ProposalKind.SETTER;
  }

  com.google.dart.engine.services.completion.CompletionProposal getProposal() {
    return proposal;
  }

  private char[][] copyStrings(String[] strings) {
    char[][] chars = new char[strings.length][];
    for (int i = 0; i < strings.length; i++) {
      chars[i] = strings[i].toCharArray();
    }
    return chars;
  }
}
