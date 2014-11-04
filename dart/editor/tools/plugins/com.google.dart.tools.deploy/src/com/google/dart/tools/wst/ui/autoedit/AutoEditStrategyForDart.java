/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.wst.ui.autoedit;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.internal.text.dart.DartAutoIndentStrategy_NEW;
import com.google.dart.tools.ui.internal.text.dart.DartAutoIndentStrategy_OLD;
import com.google.dart.tools.ui.internal.text.dart.SmartSemicolonAutoEditStrategy;
import com.google.dart.tools.ui.internal.text.dartdoc.DartDocAutoIndentStrategy;

import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.html.core.text.IHTMLPartitions;

public class AutoEditStrategyForDart implements IAutoEditStrategy {

  private IAutoEditStrategy[] strategies;

  @Override
  public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
    IAutoEditStrategy[] strats = getAutoEditStrategies(document);
    for (int i = 0; i < strats.length; i++) {
      strats[i].customizeDocumentCommand(document, command);
    }
  }

  public IAutoEditStrategy[] getAutoEditStrategies(IDocument document) {
    if (strategies == null) {
      String partitioning = IHTMLPartitions.SCRIPT;
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        strategies = new IAutoEditStrategy[] {
            new SmartSemicolonAutoEditStrategy(partitioning),
            new DartAutoIndentStrategy_NEW(partitioning, null),
            new DartDocAutoIndentStrategy(partitioning)};
      } else {
        strategies = new IAutoEditStrategy[] {
            new SmartSemicolonAutoEditStrategy(partitioning),
            new DartAutoIndentStrategy_OLD(partitioning, null),
            new DartDocAutoIndentStrategy(partitioning)};
      }
    }
    return strategies;
  }

}
