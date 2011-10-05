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
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.IndentAction;
import com.google.dart.tools.ui.cleanup.ICleanUpConfigurationUI;
import com.google.dart.tools.ui.internal.cleanup.MultiFixMessages;
import com.google.dart.tools.ui.internal.text.DartStatusConstants;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatterExtension;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.swt.widgets.Composite;

import java.util.Map;

public class CleanUpPreview extends DartPreview {

  private ICleanUpConfigurationUI fPage;
  private boolean fFormat;
  private boolean fCorrectIndentation;

  public CleanUpPreview(Composite parent, ICleanUpConfigurationUI page) {
    super(DartCore.getDefaultOptions(), parent);
    fPage = page;
    fFormat = false;
  }

  public void setCorrectIndentation(boolean enabled) {
    fCorrectIndentation = enabled;
  }

  public void setFormat(boolean enable) {
    fFormat = enable;
  }

  @Override
  public void setWorkingValues(Map<String, String> workingValues) {
    //Don't change the formatter settings
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doFormatPreview() {
    format(fPage.getPreview());
  }

  private void format(String text) {
    if (text == null) {
      fPreviewDocument.set(""); //$NON-NLS-1$
      return;
    }
    fPreviewDocument.set(text);

    if (!fFormat) {
      if (!fCorrectIndentation) {
        return;
      }

      fSourceViewer.setRedraw(false);
      try {
        IndentAction.indent(fPreviewDocument, null);
      } catch (BadLocationException e) {
        DartToolsPlugin.log(e);
      } finally {
        fSourceViewer.setRedraw(true);
      }

      return;
    }

    fSourceViewer.setRedraw(false);
    final IFormattingContext context = new DartFormattingContext();
    try {
      final IContentFormatter formatter = fViewerConfiguration.getContentFormatter(fSourceViewer);
      if (formatter instanceof IContentFormatterExtension) {
        final IContentFormatterExtension extension = (IContentFormatterExtension) formatter;
        context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, DartCore.getOptions());
        context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.valueOf(true));
        extension.format(fPreviewDocument, context);
      } else {
        formatter.format(fPreviewDocument, new Region(0, fPreviewDocument.getLength()));
      }
    } catch (Exception e) {
      final IStatus status = new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(),
          DartStatusConstants.INTERNAL_ERROR,
          MultiFixMessages.CleanUpRefactoringWizard_formatterException_errorMessage, e);
      DartToolsPlugin.log(status);
    } finally {
      context.dispose();
      fSourceViewer.setRedraw(true);
    }
  }

}
