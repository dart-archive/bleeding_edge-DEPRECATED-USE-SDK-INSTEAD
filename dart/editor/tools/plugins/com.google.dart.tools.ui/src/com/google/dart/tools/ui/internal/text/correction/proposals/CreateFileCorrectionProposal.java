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
package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.text.correction.ICommandAccess;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.CoreUtility;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Correction proposal for creating new {@link File}.
 * 
 * @coverage dart.editor.ui.correction
 */
public class CreateFileCorrectionProposal implements IDartCompletionProposal, ICommandAccess {

  private final int relevance;
  private final String label;
  private final File file;
  private final String content;

  public CreateFileCorrectionProposal(int relevance, String label, File file, String content) {
    this.relevance = relevance;
    this.label = label;
    this.file = file;
    this.content = content;
  }

  @Override
  public void apply(IDocument document) {
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        // prepare IFile
        IFile newFile;
        {
          Source source = new FileBasedSource(file);
          IResource resource = DartCore.getProjectManager().getResource(source);
          if (!(resource instanceof IFile)) {
            return;
          }
          newFile = (IFile) resource;
        }
        // ensure that folder with 'newFile' exists
        {
          IContainer container = newFile.getParent();
          if (container instanceof IFolder && !container.exists()) {
            CoreUtility.createFolder((IFolder) container, true, true, null);
          }
        }
        // do create
        newFile.create(new ByteArrayInputStream(content.getBytes("UTF-8")), true, null);
        newFile.setCharset("UTF-8", null);
        // open editor
        EditorUtility.openInEditor(newFile);
      }
    });
  }

  @Override
  public String getAdditionalProposalInfo() {
    return content;
  }

  @Override
  public String getCommandId() {
    return null;
  }

  @Override
  public IContextInformation getContextInformation() {
    return null;
  }

  @Override
  public String getDisplayString() {
    return label;
  }

  @Override
  public Image getImage() {
    return DartPluginImages.get(DartPluginImages.IMG_CORRECTION_ADD);
  }

  @Override
  public int getRelevance() {
    return relevance;
  }

  @Override
  public Point getSelection(IDocument document) {
    return null;
  }
}
