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

import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.mock.ui.CodeGenerationSettings;
import com.google.dart.tools.mock.ui.GetterSetterUtil;
import com.google.dart.tools.mock.ui.JavaPreferencesSettings;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.text.dart.DartTextMessages;
import com.google.dart.tools.ui.internal.text.dart.ImportRewrite;
import com.google.dart.tools.ui.internal.util.Strings;
import com.google.dart.tools.ui.internal.viewsupport.BasicElementLabels;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.viewers.StyledString;

import java.util.Collection;
import java.util.Set;

public class GetterSetterCompletionProposal extends DartTypeCompletionProposal implements
    ICompletionProposalExtension4 {

  public static void evaluateProposals(Type type, String prefix, int offset, int length,
      int lengthIdentifier, int relevance, Set<String> suggestedMethods,
      Collection<IDartCompletionProposal> result) throws CoreException {
    if (prefix.length() == 0) {
      relevance--;
    }

    Field[] fields = type.getFields();
    for (int i = 0; i < fields.length; i++) {
      Field curr = fields[i];
      String getterName = GetterSetterUtil.getGetterName(curr, null);
      if (Strings.startsWithIgnoreCase(getterName, prefix)) {
        suggestedMethods.add(getterName);
        int getterRelevance = relevance;
        if (curr.isStatic() && curr.isFinal()) {
          getterRelevance = relevance - 1;
        }
        result.add(new GetterSetterCompletionProposal(
            curr,
            offset,
            length,
            lengthIdentifier,
            true,
            getterRelevance));
      }

      if (!curr.isFinal()) {
        String setterName = GetterSetterUtil.getSetterName(curr, null);
        if (Strings.startsWithIgnoreCase(setterName, prefix)) {
          suggestedMethods.add(setterName);
          result.add(new GetterSetterCompletionProposal(
              curr,
              offset,
              length,
              lengthIdentifier,
              false,
              relevance));
        }
      }
    }
  }

  private static StyledString getDisplayName(Field field, boolean isGetter)
      throws DartModelException {
    StyledString buf = new StyledString();
    String fieldTypeName = Signature.toString(field.getTypeName());
    String fieldNameLabel = BasicElementLabels.getDartElementName(field.getElementName());
    if (isGetter) {
      buf.append(BasicElementLabels.getDartElementName(GetterSetterUtil.getGetterName(field, null)
          + "() : " + fieldTypeName)); //$NON-NLS-1$
      buf.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
      buf.append(Messages.format(
          DartTextMessages.GetterSetterCompletionProposal_getter_label,
          fieldNameLabel), StyledString.QUALIFIER_STYLER);
    } else {
      buf.append(BasicElementLabels.getDartElementName(GetterSetterUtil.getSetterName(field, null)
          + '(' + fieldTypeName + ") : void")); //$NON-NLS-1$
      buf.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
      buf.append(Messages.format(
          DartTextMessages.GetterSetterCompletionProposal_setter_label,
          fieldNameLabel), StyledString.QUALIFIER_STYLER);
    }
    return buf;
  }

  private final Field fField;

  private final boolean fIsGetter;

  public GetterSetterCompletionProposal(Field field, int start, int length, int lengthIdentifier,
      boolean isGetter, int relevance) throws DartModelException {
    super(
        "",
        field.getCompilationUnit(),
        start,
        length,
        lengthIdentifier,
        DartPluginImages.get(DartPluginImages.IMG_MISC_PUBLIC),
        getDisplayName(field, isGetter),
        relevance,
        null,
        null);
    Assert.isNotNull(field);

    fField = field;
    fIsGetter = isGetter;
    // TODO(scheglov) implement documentation comment
//    setProposalInfo(new ProposalInfo(field));
  }

  @Override
  public boolean isAutoInsertable() {
    return false;
  }

  @Override
  protected boolean updateReplacementString(IDocument document, char trigger, int offset,
      ImportRewrite impRewrite) throws CoreException, BadLocationException {

    CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(fField.getDartProject());
    boolean addComments = settings.createComments;

    String stub;
    if (fIsGetter) {
      String getterName = GetterSetterUtil.getGetterName(fField, null);
      stub = GetterSetterUtil.getGetterStub(fField, getterName, addComments);
    } else {
      String setterName = GetterSetterUtil.getSetterName(fField, null);
      stub = GetterSetterUtil.getSetterStub(fField, setterName, addComments);
    }

    // use the code formatter
    String lineDelim = TextUtilities.getDefaultLineDelimiter(document);

    //TODO (pquitslund): hook in formatting

//
//    IRegion region = document.getLineInformationOfOffset(getReplacementOffset());
//    int lineStart = region.getOffset();
//    int indent = Strings.computeIndentUnits(
//        document.get(lineStart, getReplacementOffset() - lineStart),
//        settings.tabWidth,
//        settings.indentWidth);

//    String replacement = CodeFormatterUtil.format(
//        CodeFormatter.K_CLASS_BODY_DECLARATIONS,
//        stub,
//        indent,
//        null,
//        lineDelim,
//        fField.getDartProject());

    String replacement = stub;

    if (replacement.endsWith(lineDelim)) {
      replacement = replacement.substring(0, replacement.length() - lineDelim.length());
    }

    setReplacementString(Strings.trimLeadingTabsAndSpaces(replacement));
    return true;
  }
}
