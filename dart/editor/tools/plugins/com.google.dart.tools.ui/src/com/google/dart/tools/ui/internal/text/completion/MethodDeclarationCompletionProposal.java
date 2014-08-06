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

import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.mock.ui.CodeGenerationSettings;
import com.google.dart.tools.mock.ui.JavaPreferencesSettings;
import com.google.dart.tools.ui.CodeGeneration;
import com.google.dart.tools.ui.DartElementImageDescriptor;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.dart.DartTextMessages;
import com.google.dart.tools.ui.internal.text.dart.ImportRewrite;
import com.google.dart.tools.ui.internal.util.Strings;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.viewers.StyledString;

import java.util.Collection;
import java.util.Set;

/**
 * Method declaration proposal.
 */
public class MethodDeclarationCompletionProposal extends DartTypeCompletionProposal implements
    ICompletionProposalExtension4 {

  public static void evaluateProposals(Type type, String prefix, int offset, int length,
      int lengthIdentifier, int relevance, Set<String> suggestedMethods,
      Collection<IDartCompletionProposal> result) throws CoreException {
    String constructorName = type.getElementName();
    if (constructorName.length() > 0 && constructorName.startsWith(prefix)
        && suggestedMethods.add(constructorName)) {
      result.add(new MethodDeclarationCompletionProposal(
          type,
          constructorName,
          null,
          offset,
          length,
          lengthIdentifier,
          relevance + 500));
    }

    if (prefix.length() > 0 && !"main".equals(prefix) && suggestedMethods.add(prefix)) { //$NON-NLS-1$
      if (!DartConventions.validateMethodName(prefix).matches(IStatus.ERROR)) {
        result.add(new MethodDeclarationCompletionProposal(
            type,
            prefix,
            Signature.SIG_VOID,
            offset,
            length,
            lengthIdentifier,
            relevance));
      }
    }
  }

  private static StyledString getDisplayName(String methodName, String returnTypeSig) {
    StyledString buf = new StyledString();
    buf.append(methodName);
    buf.append('(');
    buf.append(')');
    if (returnTypeSig != null) {
      buf.append(" : "); //$NON-NLS-1$
      buf.append(Signature.toString(returnTypeSig));
      buf.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
      buf.append(
          DartTextMessages.MethodCompletionProposal_method_label,
          StyledString.QUALIFIER_STYLER);
    } else {
      buf.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
      buf.append(
          DartTextMessages.MethodCompletionProposal_constructor_label,
          StyledString.QUALIFIER_STYLER);
    }
    return buf;
  }

  private final Type fType;
  private final String fReturnTypeSig;

  private final String fMethodName;

  public MethodDeclarationCompletionProposal(Type type, String methodName, String returnTypeSig,
      int start, int length, int lengthIdentifier, int relevance) {
    super("", type.getCompilationUnit(), start, length, lengthIdentifier, null, getDisplayName(
        methodName,
        returnTypeSig), relevance, null, null);
    Assert.isNotNull(type);
    Assert.isNotNull(methodName);

    fType = type;
    fMethodName = methodName;
    fReturnTypeSig = returnTypeSig;

    if (returnTypeSig == null) {
      // TODO(scheglov) implement documentation comment
//      setProposalInfo(new ProposalInfo(type));

      ImageDescriptor desc = new DartElementImageDescriptor(
          DartPluginImages.DESC_MISC_PUBLIC,
          DartElementImageDescriptor.CONSTRUCTOR,
          DartElementImageProvider.SMALL_SIZE);
      setImage(DartToolsPlugin.getImageDescriptorRegistry().get(desc));
    } else {
      setImage(DartPluginImages.get(DartPluginImages.IMG_MISC_PRIVATE));
    }
  }

  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    return new String(); // don't let method stub proposals complete incrementally
  }

  @Override
  public boolean isAutoInsertable() {
    return false;
  }

  @Override
  protected boolean updateReplacementString(IDocument document, char trigger, int offset,
      ImportRewrite impRewrite) throws CoreException, BadLocationException {

    CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(fType.getDartProject());
    boolean addComments = settings.createComments;

    String[] empty = new String[0];
    String lineDelim = TextUtilities.getDefaultLineDelimiter(document);
    @SuppressWarnings("deprecation")
    String declTypeName = fType.getTypeQualifiedName('.');
    boolean isInterface = fType.isInterface();

    StringBuffer buf = new StringBuffer();
    if (addComments) {
      String comment = CodeGeneration.getMethodComment(
          fType.getCompilationUnit(),
          declTypeName,
          fMethodName,
          empty,
          empty,
          fReturnTypeSig,
          lineDelim);
      if (comment != null) {
        buf.append(comment);
        buf.append(lineDelim);
      }
    }

    if (fReturnTypeSig != null) {
      buf.append(Signature.toString(fReturnTypeSig));
    }
    buf.append(' ');
    buf.append(fMethodName);
    if (isInterface) {
      buf.append("();"); //$NON-NLS-1$
      buf.append(lineDelim);
    } else {
      buf.append("() {"); //$NON-NLS-1$
      buf.append(lineDelim);

      String body = CodeGeneration.getMethodBodyContent(
          fType.getCompilationUnit(),
          declTypeName,
          fMethodName,
          fReturnTypeSig == null,
          "", lineDelim); //$NON-NLS-1$
      if (body != null) {
        buf.append(body);
        buf.append(lineDelim);
      }
      buf.append("}"); //$NON-NLS-1$
      buf.append(lineDelim);
    }
    String stub = buf.toString();

    // use the code formatter
//    IRegion region = document.getLineInformationOfOffset(getReplacementOffset());
//    int lineStart = region.getOffset();
//    int indent = Strings.computeIndentUnits(
//        document.get(lineStart, getReplacementOffset() - lineStart),
//        settings.tabWidth,
//        settings.indentWidth);
//
//    String replacement = CodeFormatterUtil.format(
//        CodeFormatter.K_CLASS_BODY_DECLARATIONS,
//        stub,
//        indent,
//        null,
//        lineDelim,
//        fType.getDartProject());

    //TODO (pquitslund): hook in formatting

    String replacement = stub;

    if (replacement.endsWith(lineDelim)) {
      replacement = replacement.substring(0, replacement.length() - lineDelim.length());
    }

    setReplacementString(Strings.trimLeadingTabsAndSpaces(replacement));
    return true;
  }

}
