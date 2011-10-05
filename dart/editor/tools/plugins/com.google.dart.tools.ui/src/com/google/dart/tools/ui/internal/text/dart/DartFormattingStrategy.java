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

import com.google.dart.tools.core.formatter.CodeFormatter;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.internal.util.CodeFormatterUtil;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import java.util.LinkedList;
import java.util.Map;

/**
 * Formatting strategy for java source code.
 */
public class DartFormattingStrategy extends ContextBasedFormattingStrategy {

  /** Documents to be formatted by this strategy */
  private final LinkedList<IDocument> fDocuments = new LinkedList<IDocument>();
  /** Partitions to be formatted by this strategy */
  private final LinkedList<TypedPosition> fPartitions = new LinkedList<TypedPosition>();

  /**
   * Creates a new java formatting strategy.
   */
  public DartFormattingStrategy() {
    super();
  }

  /*
   * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#format()
   */
  @SuppressWarnings("unchecked")
  @Override
  public void format() {
    super.format();

    final IDocument document = fDocuments.removeFirst();
    final TypedPosition partition = fPartitions.removeFirst();

    if (document != null && partition != null) {
      Map<String, IDocumentPartitioner> partitioners = null;
      try {

        // TODO need to enable formatting the selection when it works
        DartX.todo();
        final TextEdit edit = CodeFormatterUtil.reformat(CodeFormatter.K_COMPILATION_UNIT,
            document.get(),
//            partition.getOffset(), partition.getLength(), 0,
            0, document.getLength(), 0, TextUtilities.getDefaultLineDelimiter(document),
            getPreferences());
        if (edit != null) {
          if (edit.getChildrenSize() > 20) {
            partitioners = TextUtilities.removeDocumentPartitioners(document);
          }

          edit.apply(document);
        }

      } catch (MalformedTreeException exception) {
        DartToolsPlugin.log(exception);
      } catch (BadLocationException exception) {
        // Can only happen on concurrent document modification - log and bail
        // out
        DartToolsPlugin.log(exception);
      } finally {
        if (partitioners != null) {
          TextUtilities.addDocumentPartitioners(document, partitioners);
        }
      }
    }
  }

  /*
   * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#formatterStarts
   * (org.eclipse.jface.text.formatter.IFormattingContext)
   */
  @Override
  public void formatterStarts(final IFormattingContext context) {
    super.formatterStarts(context);

    fPartitions.addLast((TypedPosition) context.getProperty(FormattingContextProperties.CONTEXT_PARTITION));
    fDocuments.addLast((IDocument) context.getProperty(FormattingContextProperties.CONTEXT_MEDIUM));
  }

  /*
   * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#formatterStops ()
   */
  @Override
  public void formatterStops() {
    super.formatterStops();

    fPartitions.clear();
    fDocuments.clear();
  }
}
