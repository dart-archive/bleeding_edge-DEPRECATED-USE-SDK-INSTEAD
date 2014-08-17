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
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.internal.corext.util.QualifiedTypeNameHistory;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.dart.ProposalContextInformation;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

/**
 * If passed compilation unit is not null, the replacement string will be seen as a qualified type
 * name.
 */
public class LazyDartTypeCompletionProposal extends LazyDartCompletionProposal {
  /** Triggers for types. Do not modify. */
  protected static final char[] TYPE_TRIGGERS = new char[] {'.', '\t', '[', '(', ' '};
  /** Triggers for types Dart doc. Do not modify. */
  protected static final char[] JDOC_TYPE_TRIGGERS = new char[] {'#', '}', ' ', '.'};

  private String fQualifiedName;
  private String fSimpleName;

  public LazyDartTypeCompletionProposal(CompletionProposal proposal,
      DartContentAssistInvocationContext context) {
    super(proposal, context);
    fQualifiedName = null;
  }

  @Override
  public void apply(IDocument document, char trigger, int offset) {
    try {
      boolean insertClosingParenthesis = trigger == '(' && autocloseBrackets();
      if (insertClosingParenthesis) {
        StringBuffer replacement = new StringBuffer(getReplacementString());
        updateReplacementWithParentheses(replacement);
        setReplacementString(replacement.toString());
        trigger = '\0';
      }

      super.apply(document, trigger, offset);

      if (insertClosingParenthesis) {
        setUpLinkedMode(document, ')');
      }

      rememberSelection();
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    }
  }

  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    String prefix = getPrefix(document, completionOffset);

    String completion;
    // return the qualified name if the prefix is already qualified
    if (prefix.indexOf('.') != -1) {
      completion = getQualifiedTypeName();
    } else {
      completion = getSimpleTypeName();
    }

    if (isCamelCaseMatching()) {
      return getCamelCaseCompound(prefix, completion);
    }

    return completion;
  }

  public final String getQualifiedTypeName() {
    if (fQualifiedName == null) {
      fQualifiedName = String.valueOf(fProposal.getSignature());
    }
    return fQualifiedName;
  }

  @Override
  protected IContextInformation computeContextInformation() {
    char[][] typeParameters = fProposal.getParameterNames();
    if (typeParameters == null || typeParameters.length == 0) {
      return super.computeContextInformation();
    }

    ProposalContextInformation contextInformation = new ProposalContextInformation(fProposal);
    if (fContextInformationPosition != 0 && fProposal.getCompletion().length == 0) {
      contextInformation.setContextInformationPosition(fContextInformationPosition);
    }
    return contextInformation;

  }

  @Override
  protected ProposalInfo computeProposalInfo() {
    // TODO(scheglov) implement documentation comment
//    if (fCompilationUnit != null) {
//      DartProject project = fCompilationUnit.getDartProject();
//      if (project != null) {
//        return new TypeProposalInfo(project, fProposal);
//      }
//    }
    return super.computeProposalInfo();
  }

  @Override
  protected int computeRelevance() {
    /*
     * There are two histories: the RHS history remembers types used for the current expected type
     * (left hand side), while the type history remembers recently used types in general).
     * 
     * The presence of an RHS ranking is a much more precise sign for relevance as it proves the
     * subtype relationship between the proposed type and the expected type.
     * 
     * The "recently used" factor (of either the RHS or general history) is less important, it
     * should not override other relevance factors such as if the type is already imported etc.
     */
    float rhsHistoryRank = fInvocationContext.getHistoryRelevance(getQualifiedTypeName());
    float typeHistoryRank = QualifiedTypeNameHistory.getDefault().getNormalizedPosition(
        getQualifiedTypeName());

    int recencyBoost = Math.round((rhsHistoryRank + typeHistoryRank) * 5);
    int rhsBoost = rhsHistoryRank > 0.0f ? 50 : 0;
    int baseRelevance = super.computeRelevance();

    return baseRelevance + rhsBoost + recencyBoost;
  }

  @SuppressWarnings("deprecation")
  @Override
  protected String computeReplacementString() {
    String replacement = super.computeReplacementString();

    /* No import rewriting ever from within the import section. */
    if (isImportCompletion()) {
      return replacement;
    }

    /* Always use the simple name for non-formal Dart doc references to types. */
    // TODO fix
    if (fProposal.getKind() == CompletionProposal.TYPE_REF
        && fInvocationContext.getCoreContext().isInJavadocText()) {
      return getSimpleTypeName();
    }

    String qualifiedTypeName = getQualifiedTypeName();

    if (qualifiedTypeName.indexOf('.') == -1) {
      // default package - no imports needed
      return qualifiedTypeName;
    }

    /*
     * If the user types in the qualification, don't force import rewriting on him - insert the
     * qualified name.
     */
    IDocument document = fInvocationContext.getDocument();
    if (document != null) {
      String prefix = getPrefix(document, getReplacementOffset() + getReplacementLength());
      int dotIndex = prefix.lastIndexOf('.');
      // match up to the last dot in order to make higher level matching still work (camel case...)
      if (dotIndex != -1
          && qualifiedTypeName.toLowerCase().startsWith(
              prefix.substring(0, dotIndex + 1).toLowerCase())) {
        return qualifiedTypeName;
      }
    }

    /*
     * The replacement does not contain a qualification (e.g. an inner type qualified by its parent)
     * - use the replacement directly.
     */
    if (replacement.indexOf('.') == -1) {
      if (isInDartDoc()) {
        return getSimpleTypeName(); // don't use the braces added for Dart doc link proposals
      }
      return replacement;
    }

    // fall back for the case we don't have an import rewrite (see allowAddingImports)

    /* Default: use the fully qualified type name. */
    return qualifiedTypeName;
  }

  @Override
  protected String computeSortString() {
    // try fast sort string to avoid display string creation
    return getSimpleTypeName() + Character.MIN_VALUE + getQualifiedTypeName();
  }

  @Override
  protected char[] computeTriggerCharacters() {
    return isInDartDoc() ? JDOC_TYPE_TRIGGERS : TYPE_TRIGGERS;
  }

  protected final String getSimpleTypeName() {
    if (fSimpleName == null) {
      fSimpleName = Signature.getSimpleName(getQualifiedTypeName());
    }
    return fSimpleName;
  }

  protected final boolean isImportCompletion() {
    char[] completion = fProposal.getCompletion();
    if (completion.length == 0) {
      return false;
    }

    char last = completion[completion.length - 1];
    /*
     * Proposals end in a semicolon when completing types in normal imports or when completing
     * static members, in a period when completing types in static imports.
     */
    return last == ';' || last == '.';
  }

  @Override
  protected boolean isValidPrefix(String prefix) {
    return isPrefix(prefix, getSimpleTypeName()) || isPrefix(prefix, getQualifiedTypeName());
  }

  /**
   * Remembers the selection in the content assist history.
   * 
   * @throws DartModelException if anything goes wrong
   */
  protected final void rememberSelection() throws DartModelException {
//    Type lhs = fInvocationContext.getExpectedType();
//    Type rhs = (Type) getDartElement();
//    if (lhs != null && rhs != null) {
//      DartToolsPlugin.getDefault().getContentAssistHistory().remember(lhs, rhs);
//    }
//
//    QualifiedTypeNameHistory.remember(getQualifiedTypeName());
  }

  protected void updateReplacementWithParentheses(StringBuffer replacement) {
    FormatterPrefs prefs = getFormatterPrefs();

    if (prefs.beforeOpeningParen) {
      replacement.append(SPACE);
    }
    replacement.append(LPAREN);

    if (prefs.afterOpeningParen) {
      replacement.append(SPACE);
    }

    setCursorPosition(replacement.length());

    if (prefs.afterOpeningParen) {
      replacement.append(SPACE);
    }

    replacement.append(RPAREN);
  }
}
