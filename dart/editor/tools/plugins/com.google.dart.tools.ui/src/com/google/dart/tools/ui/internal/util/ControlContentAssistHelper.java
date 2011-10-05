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
package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.ui.DartX;

import org.eclipse.jface.contentassist.SubjectControlContentAssistant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.contentassist.ContentAssistHandler;

/**
 * 
 */
@SuppressWarnings("deprecation")
public class ControlContentAssistHelper {

  /**
   * @param combo the text field to install ContentAssist
   * @param processor the <code>IContentAssistProcessor</code>
   */
  public static void createComboContentAssistant(final Combo combo,
      IContentAssistProcessor processor) {
    DartX.todo("this is part of refactoring");
    ContentAssistHandler.createHandlerForCombo(combo, createJavaContentAssistant(processor));
  }

  public static SubjectControlContentAssistant createJavaContentAssistant(
      IContentAssistProcessor processor) {
    final SubjectControlContentAssistant contentAssistant = new SubjectControlContentAssistant();

    contentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);

    DartX.todo();
//    ContentAssistPreference.configure(contentAssistant,
//        JavaScriptPlugin.getDefault().getPreferenceStore());
//    contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
//    contentAssistant.setInformationControlCreator(new IInformationControlCreator() {
//      public IInformationControl createInformationControl(Shell parent) {
//        return new DefaultInformationControl(parent, SWT.NONE,
//            new HTMLTextPresenter(true));
//      }
//    });

    return contentAssistant;
  }

  /**
   * @param text the text field to install ContentAssist
   * @param processor the <code>IContentAssistProcessor</code>
   */
  public static void createTextContentAssistant(final Text text, IContentAssistProcessor processor) {
    ContentAssistHandler.createHandlerForText(text, createJavaContentAssistant(processor));
  }

}
