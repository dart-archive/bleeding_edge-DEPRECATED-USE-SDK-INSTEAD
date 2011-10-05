/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.ContextSensitiveImportRewriteContext;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.QualifiedTypeNameHistory;
import com.google.dart.tools.ui.StubUtility;
import com.google.dart.tools.ui.internal.text.editor.ASTProvider;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;

/**
 * If passed compilation unit is not null, the replacement string will be seen as a qualified type
 * name.
 */
public class LazyDartTypeCompletionProposal extends LazyDartCompletionProposal {
  /** Triggers for types. Do not modify. */
  protected static final char[] TYPE_TRIGGERS = new char[] {'\t', '[', '(', ' '};
  /** Triggers for types in javadoc. Do not modify. */
  protected static final char[] JDOC_TYPE_TRIGGERS = new char[] {'#', '}', ' ', '.'};

  /** The compilation unit, or <code>null</code> if none is available. */
  protected final CompilationUnit fCompilationUnit;

  private String fQualifiedName;
  private String fSimpleName;
  private ImportRewrite fImportRewrite;
  private ContextSensitiveImportRewriteContext fImportContext;

  public LazyDartTypeCompletionProposal(CompletionProposal proposal,
      DartContentAssistInvocationContext context) {
    super(proposal, context);
    fCompilationUnit = context.getCompilationUnit();
    fQualifiedName = null;
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.LazyDartCompletionProposal# apply(org
   * .eclipse.jface.text.IDocument, char, int)
   */
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

      //TODO (pquitslund): implement import rewriting
//      if (fImportRewrite != null && fImportRewrite.hasRecordedChanges()) {
//        int oldLen = document.getLength();
//        fImportRewrite.rewriteImports(new NullProgressMonitor()).apply(
//            document, TextEdit.UPDATE_REGIONS);
//        setReplacementOffset(getReplacementOffset() + document.getLength()
//            - oldLen);
//      }

      if (insertClosingParenthesis) {
        setUpLinkedMode(document, ')');
      }

      rememberSelection();
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    }
//    catch (BadLocationException e) {
//      DartToolsPlugin.log(e);
//    }
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.DartCompletionProposal# getCompletionText ()
   */
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
      fQualifiedName = String.valueOf(Signature.toCharArray(fProposal.getSignature()));
    }
    return fQualifiedName;
  }

  public boolean isValidTypePrefix(String prefix) {
    return isValidPrefix(prefix);
  }

  /**
   * Returns <code>true</code> if imports may be added. The return value depends on the context and
   * preferences only and does not take into account the contents of the compilation unit or the
   * kind of proposal. Even if <code>true</code> is returned, there may be cases where no imports
   * are added for the proposal. For example:
   * <ul>
   * <li>when completing within the import section</li>
   * <li>when completing informal javadoc references (e.g. within <code>&lt;code&gt;</code> tags)</li>
   * <li>when completing a type that conflicts with an existing import</li>
   * <li>when completing an implicitly imported type (same package, <code>java.lang</code> types)</li>
   * </ul>
   * <p>
   * The decision whether a qualified type or the simple type name should be inserted must take into
   * account these different scenarios.
   * </p>
   * <p>
   * Subclasses may extend.
   * </p>
   * 
   * @return <code>true</code> if imports may be added, <code>false</code> if not
   */
  protected boolean allowAddingImports() {
    if (isInJavadoc()) {
      // TODO fix
//			if (!fContext.isInJavadocFormalReference())
//				return false;
      if (fProposal.getKind() == CompletionProposal.TYPE_REF
          && fInvocationContext.getCoreContext().isInJavadocText()) {
        return false;
      }

      if (!isJavadocProcessingEnabled()) {
        return false;
      }
    }

    IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
    return preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_ADDIMPORT);
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.LazyDartCompletionProposal#
   * computeProposalInfo()
   */
  @Override
  protected ProposalInfo computeProposalInfo() {
    if (fCompilationUnit != null) {
      DartProject project = fCompilationUnit.getDartProject();
      if (project != null) {
        return new TypeProposalInfo(project, fProposal);
      }
    }
    return super.computeProposalInfo();
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.LazyDartCompletionProposal# computeRelevance
   * ()
   */
  @Override
  protected int computeRelevance() {
    /*
     * There are two histories: the RHS history remembers types used for the current expected type
     * (left hand side), while the type history remembers recently used types in general). The
     * presence of an RHS ranking is a much more precise sign for relevance as it proves the subtype
     * relationship between the proposed type and the expected type. The "recently used" factor (of
     * either the RHS or general history) is less important, it should not override other relevance
     * factors such as if the type is already imported etc.
     */
    float rhsHistoryRank = fInvocationContext.getHistoryRelevance(getQualifiedTypeName());
    // TODO Implement history
    float typeHistoryRank = 0;//QualifiedTypeNameHistory.getDefault().getNormalizedPosition(
//        getQualifiedTypeName());

    int recencyBoost = Math.round((rhsHistoryRank + typeHistoryRank) * 5);
    int rhsBoost = rhsHistoryRank > 0.0f ? 50 : 0;
    int baseRelevance = super.computeRelevance();

    return baseRelevance + rhsBoost + recencyBoost;
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.LazyDartCompletionProposal#
   * computeReplacementString()
   */
  @Override
  protected String computeReplacementString() {
    String replacement = super.computeReplacementString();

//		/* No import rewriting ever from within the import section. */
//		if (isImportCompletion())
//	        return replacement;

    /* Always use the simple name for non-formal javadoc references to types. */
    // TODO fix
    if (fProposal.getKind() == CompletionProposal.TYPE_REF
        && fInvocationContext.getCoreContext().isInJavadocText()) {
      return getSimpleTypeName();
    }

    String qualifiedTypeName = replacement;
// 		if (qualifiedTypeName.indexOf('.') == -1)
// 			// default package - no imports needed 
// 			return qualifiedTypeName;

    /*
     * If the user types in the qualification, don't force import rewriting on him - insert the
     * qualified name.
     */
    IDocument document = fInvocationContext.getDocument();
    if (document != null) {
      String prefix = getPrefix(document, getReplacementOffset() + getReplacementLength());
      int dotIndex = prefix.lastIndexOf('.');
      // match up to the last dot in order to make higher level matching still
// work (camel case...)
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
      if (isInJavadoc()) {
        return getSimpleTypeName(); // don't use the braces added for javadoc
      }
// link proposals
      return replacement;
    }

    /* Add imports if the preference is on. */
    fImportRewrite = createImportRewrite();

//		if (fImportRewrite != null) {
//			String packageName=null;
//			try {
//				DartElement javaElement = this.getProposalInfo().getJavaElement();
//				 packageName=DartModelUtil.getFilePackage(javaElement);
//			} catch (DartModelException e) {
//				DartToolsPlugin.log(e);
//			}
//			return fImportRewrite.addImport(qualifiedTypeName,packageName, fImportContext);
//		}

    // fall back for the case we don't have an import rewrite (see
// allowAddingImports)

    /* No imports for implicit imports. */
    if (fCompilationUnit != null
        && DartModelUtil.isImplicitImport(Signature.getQualifier(qualifiedTypeName),
            fCompilationUnit)) {
      return Signature.getSimpleName(qualifiedTypeName);
    }

    /* Default: use the fully qualified type name. */
    return qualifiedTypeName;
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.LazyDartCompletionProposal# computeSortString
   * ()
   */
  @Override
  protected String computeSortString() {
    // try fast sort string to avoid display string creation
    return getQualifiedTypeName();
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.LazyDartCompletionProposal#
   * computeTriggerCharacters()
   */
  @Override
  protected char[] computeTriggerCharacters() {
    return isInJavadoc() ? JDOC_TYPE_TRIGGERS : TYPE_TRIGGERS;
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

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.LazyDartCompletionProposal# isValidPrefix
   * (java.lang.String)
   */
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
    Type lhs = fInvocationContext.getExpectedType();
    Type rhs = (Type) getJavaElement();
    if (lhs != null && rhs != null) {
      DartToolsPlugin.getDefault().getContentAssistHistory().remember(lhs, rhs);
    }

    QualifiedTypeNameHistory.remember(getQualifiedTypeName());
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

  private ImportRewrite createImportRewrite() {
    if (fCompilationUnit != null && allowAddingImports()) {
      try {
        DartUnit cu = getASTRoot(fCompilationUnit);
        if (cu == null) {
          ImportRewrite rewrite = StubUtility.createImportRewrite(fCompilationUnit, true);
          fImportContext = null;
          return rewrite;
        } else {
          ImportRewrite rewrite = StubUtility.createImportRewrite(cu, true);
          fImportContext = new ContextSensitiveImportRewriteContext(cu,
              fInvocationContext.getInvocationOffset(), rewrite);
          return rewrite;
        }
      } catch (CoreException x) {
        DartToolsPlugin.log(x);
      }
    }
    return null;
  }

  private DartUnit getASTRoot(CompilationUnit compilationUnit) {
    return DartToolsPlugin.getDefault().getASTProvider().getAST(compilationUnit,
        ASTProvider.WAIT_NO, new NullProgressMonitor());
  }

  private boolean isJavadocProcessingEnabled() {
    DartProject project = fCompilationUnit.getDartProject();
    boolean processJavadoc;
    if (project == null) {
      processJavadoc = JavaScriptCore.ENABLED.equals(DartCore.getOption(JavaScriptCore.COMPILER_DOC_COMMENT_SUPPORT));
    } else {
      processJavadoc = JavaScriptCore.ENABLED.equals(project.getOption(
          JavaScriptCore.COMPILER_DOC_COMMENT_SUPPORT, true));
    }
    return processJavadoc;
  }
}
