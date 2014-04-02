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
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.pub.DependencyObject;
import com.google.dart.tools.core.pub.IModelListener;
import com.google.dart.tools.core.pub.PubspecModel;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.text.correction.ICommandAccess;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import java.io.File;

/**
 * Correction proposal for adding new dependency into pubspec.
 * 
 * @coverage dart.editor.ui.correction
 */
public class AddDependencyCorrectionProposal implements IDartCompletionProposal, ICommandAccess {
  private final int relevance;
  private final String label;
  private final File file;
  private final String packageName;

  public AddDependencyCorrectionProposal(int relevance, String label, File file, String packageName) {
    this.relevance = relevance;
    this.label = label;
    this.file = file;
    this.packageName = packageName;
  }

  @Override
  public void apply(IDocument document) {
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        // prepare IFile
        IResource resource;
        {
          Source source = new FileBasedSource(file);
          resource = DartCore.getProjectManager().getResource(source);
          if (!(resource instanceof IFile)) {
            return;
          }
        }
        // prepare PubFolder 
        PubFolder pubFolder = DartCore.getProjectManager().getPubFolder(resource);
        if (pubFolder == null) {
          return;
        }
        // update PubspecModel
        PubspecModel pubspec = pubFolder.getPubspec();
        pubspec.add(
            new DependencyObject[] {new DependencyObject(packageName)},
            IModelListener.ADDED);
        try {
          pubspec.save();
        } catch (Throwable e) {
          String msg = "Unable to update " + resource;
          DartCore.logError(msg, e);
        }
      }
    });
  }

  @Override
  public String getAdditionalProposalInfo() {
    return null;
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
