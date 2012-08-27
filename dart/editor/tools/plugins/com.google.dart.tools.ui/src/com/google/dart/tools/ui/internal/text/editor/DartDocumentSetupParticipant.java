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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartTextTools;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;

/**
 * The document setup participant for Dart.
 */
public class DartDocumentSetupParticipant implements IDocumentSetupParticipant {

  public DartDocumentSetupParticipant() {
  }

  /*
   * @see org.eclipse.core.filebuffers.IDocumentSetupParticipant#setup(org.eclipse
   * .jface.text.IDocument)
   */
  @Override
  public void setup(IDocument document) {
    DartTextTools tools = DartToolsPlugin.getDefault().getDartTextTools();
    tools.setupDartDocumentPartitioner(document, DartPartitions.DART_PARTITIONING);
  }
}
