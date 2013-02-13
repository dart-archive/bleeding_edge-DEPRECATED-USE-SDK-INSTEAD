package com.google.dart.tools.core.internal.completion;

import com.google.dart.tools.core.completion.CompletionProposal;

/**
 * Wrap String-based completion proposals for use in legacy char[]-based client code.
 */
public class ProxyProposal extends CompletionProposal {

  private com.google.dart.engine.services.completion.CompletionProposal proposal;

  public ProxyProposal(com.google.dart.engine.services.completion.CompletionProposal proposal) {
    this.proposal = proposal;
  }

  @Override
  public char[] getCompletion() {
    return proposal.getCompletion().toCharArray();
  }

  @Override
  public int getKind() {
    switch (proposal.getKind()) {
      case CLASS:
        return CompletionProposal.TYPE_REF;
      case CLASS_ALIAS:
        return CompletionProposal.TYPE_REF;
      case CONSTRUCTOR:
        return CompletionProposal.CONSTRUCTOR_INVOCATION;
      case FIELD:
        return CompletionProposal.FIELD_REF;
      case FUNCTION:
        return CompletionProposal.METHOD_REF;
      case FUNCTION_ALIAS:
        return CompletionProposal.TYPE_REF;
      case GETTER:
        return CompletionProposal.METHOD_REF;
      case IMPORT:
        return CompletionProposal.TYPE_IMPORT;
      case LIBRARY_PREFIX:
        return CompletionProposal.LIBRARY_PREFIX;
      case METHOD:
        return CompletionProposal.METHOD_REF;
      case PARAMETER:
        return CompletionProposal.LOCAL_VARIABLE_REF;
      case SETTER:
        return CompletionProposal.METHOD_REF;
      case TYPE_VARIABLE:
        return CompletionProposal.TYPE_REF;
      case VARIABLE:
        return CompletionProposal.LOCAL_VARIABLE_REF;
      default:
        return 0;
    }
  }

  @Override
  public char[][] getParameterNames() {
    return copyStrings(proposal.getParameterNames());
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
