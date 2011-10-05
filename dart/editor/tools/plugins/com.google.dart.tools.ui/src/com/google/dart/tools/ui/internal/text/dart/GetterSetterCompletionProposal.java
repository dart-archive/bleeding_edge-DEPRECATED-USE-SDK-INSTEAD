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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.compiler.ast.Modifiers;
import com.google.dart.tools.core.formatter.CodeFormatter;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.CodeGenerationSettings;
import com.google.dart.tools.ui.GetterSetterUtil;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.JavaPreferencesSettings;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.util.CodeFormatterUtil;
import com.google.dart.tools.ui.internal.util.Strings;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;

import java.util.Collection;
import java.util.Set;

public class GetterSetterCompletionProposal extends DartTypeCompletionProposal implements
    ICompletionProposalExtension4 {

  public static void evaluateProposals(Type type, String prefix, int offset, int length,
      int relevance, Set<String> suggestedMethods, Collection<GetterSetterCompletionProposal> result)
      throws CoreException {
    if (prefix.length() == 0) {
      relevance--;
    }

    Field[] fields = type.getFields();
    Method[] methods = type.getMethods();
    for (int i = 0; i < fields.length; i++) {
      Field curr = fields[i];
      String getterName = GetterSetterUtil.getGetterName(curr, null);
      if (getterName.startsWith(prefix) && !hasMethod(methods, getterName)
          && suggestedMethods.add(getterName)) {
        result.add(new GetterSetterCompletionProposal(curr, offset, length, true, relevance));
      }

      String setterName = GetterSetterUtil.getSetterName(curr, null);
      if (setterName.startsWith(prefix) && !hasMethod(methods, setterName)
          && suggestedMethods.add(setterName)) {
        result.add(new GetterSetterCompletionProposal(curr, offset, length, false, relevance));
      }
    }
  }

  private static String getDisplayName(Field field, boolean isGetter) throws DartModelException {
    StringBuffer buf = new StringBuffer();
    if (isGetter) {
      buf.append(GetterSetterUtil.getGetterName(field, null));
      buf.append("()  "); //$NON-NLS-1$
      buf.append(field.getTypeName());
      buf.append(" - "); //$NON-NLS-1$
      buf.append(Messages.format(DartTextMessages.GetterSetterCompletionProposal_getter_label,
          field.getElementName()));
    } else {
      buf.append(GetterSetterUtil.getSetterName(field, null));
      buf.append('(').append(field.getTypeName()).append(')');
      buf.append("  "); //$NON-NLS-1$
      buf.append("void");
      buf.append(" - "); //$NON-NLS-1$
      buf.append(Messages.format(DartTextMessages.GetterSetterCompletionProposal_setter_label,
          field.getElementName()));
    }
    return buf.toString();
  }

  private static boolean hasMethod(Method[] methods, String name) {
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].getElementName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  private final Field fField;

  private final boolean fIsGetter;

  public GetterSetterCompletionProposal(Field field, int start, int length, boolean isGetter,
      int relevance) throws DartModelException {
    super(
        "", field.getCompilationUnit(), start, length, DartPluginImages.get(DartPluginImages.IMG_MISC_PUBLIC), getDisplayName(field, isGetter), relevance); //$NON-NLS-1$
    Assert.isNotNull(field);

    fField = field;
    fIsGetter = isGetter;
    setProposalInfo(new ProposalInfo(field));
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4# isAutoInsertable()
   */
  @Override
  public boolean isAutoInsertable() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see DartTypeCompletionProposal#updateReplacementString(IDocument, char, int, ImportRewrite)
   */
  @Override
  protected boolean updateReplacementString(IDocument document, char trigger, int offset,
      ImportRewrite impRewrite) throws CoreException, BadLocationException {

    CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(fField.getDartProject());
    boolean addComments = settings.createComments;
    Modifiers modifiers = Modifiers.NONE;
    if (fField.getModifiers().isStatic()) {
      modifiers.makeStatic();
    }

    String stub;
    if (fIsGetter) {
      String getterName = GetterSetterUtil.getGetterName(fField, null);
      stub = GetterSetterUtil.getGetterStub(fField, getterName, addComments, modifiers);
    } else {
      String setterName = GetterSetterUtil.getSetterName(fField, null);
      stub = GetterSetterUtil.getSetterStub(fField, setterName, addComments, modifiers);
    }

    // use the code formatter
    String lineDelim = TextUtilities.getDefaultLineDelimiter(document);

    IRegion region = document.getLineInformationOfOffset(getReplacementOffset());
    int lineStart = region.getOffset();
    int indent = Strings.computeIndentUnits(
        document.get(lineStart, getReplacementOffset() - lineStart), settings.tabWidth,
        settings.indentWidth);

    String replacement = CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, stub,
        indent, null, lineDelim, fField.getDartProject());

    if (replacement.endsWith(lineDelim)) {
      replacement = replacement.substring(0, replacement.length() - lineDelim.length());
    }

    setReplacementString(Strings.trimLeadingTabsAndSpaces(replacement));
    return true;
  }
}
