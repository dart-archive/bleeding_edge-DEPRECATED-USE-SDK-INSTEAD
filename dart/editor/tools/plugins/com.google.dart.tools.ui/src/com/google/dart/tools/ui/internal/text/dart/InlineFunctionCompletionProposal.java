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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.util.Strings;
import com.google.dart.tools.ui.internal.util.TypeLabelUtil;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.viewers.StyledString;

import java.util.Collection;
import java.util.Set;

public class InlineFunctionCompletionProposal extends DartTypeCompletionProposal implements
    ICompletionProposalExtension4 {

  public static void evaluateProposals(CompilationUnit cu, String prefix, char[][] paramNames,
      char[][] paramTypes, int offset, int length, int relevance, Set<String> suggestedMethods,
      Collection<IDartCompletionProposal> result) {
    suggestedMethods.add(prefix);
    result.add(new InlineFunctionCompletionProposal(cu, paramNames, paramTypes, prefix, offset,
        length, relevance));
  }

  private static StringBuffer fillDisplayName(char[][] paramNames, char[][] paramTypes,
      String typeName, StringBuffer buf) {
    buf.append('(');
    if (paramTypes.length > 0) {
      buf.append(paramTypes[0]);
      buf.append(' ');
      buf.append(paramNames[0]);
      for (int i = 1; i < paramTypes.length; i++) {
        buf.append(", "); //$NON-NLS-1$
        buf.append(paramTypes[i]);
        buf.append(' ');
        buf.append(paramNames[i]);
      }
    }
    buf.append(')');
    buf.append(" => "); //$NON-NLS-1$
    return buf;
  }

  private static StyledString getDisplayName(char[][] paramNames, char[][] paramTypes,
      String typeName) {
    StringBuffer buf = new StringBuffer();
    fillDisplayName(paramNames, paramTypes, typeName, buf);
    if (!typeName.isEmpty()) {
      buf.append(" : "); //$NON-NLS-1$
      TypeLabelUtil.insertTypeLabel(typeName, buf);
    }
    return new StyledString(buf.toString());
  }

  private String typeName;
  private char[][] paramNames;
  private char[][] paramTypes;

  public InlineFunctionCompletionProposal(CompilationUnit cu, char[][] paramNames,
      char[][] paramTypes, String typeName, int start, int length, int relevance) {
    super("", cu, start, length, null, getDisplayName(paramNames, paramTypes, typeName), relevance); //$NON-NLS-1$
    this.typeName = typeName;
    this.paramNames = paramNames;
    this.paramTypes = paramTypes;
    setImage(DartPluginImages.get(DartPluginImages.IMG_MISC_PRIVATE));
  }

  @Override
  public boolean isAutoInsertable() {
    return false;
  }

  @Override
  protected boolean updateReplacementString(IDocument document, char trigger, int offset,
      ImportRewrite impRewrite) throws CoreException, BadLocationException {
    String lineDelim = TextUtilities.getDefaultLineDelimiter(document);
    StringBuffer buf = new StringBuffer();

    fillDisplayName(paramNames, paramTypes, typeName, buf);

    String replacement = buf.toString();
    if (replacement.endsWith(lineDelim)) {
      replacement = replacement.substring(0, replacement.length() - lineDelim.length());
    }
    setReplacementString(Strings.trimLeadingTabsAndSpaces(replacement));
    return true;
  }
}
