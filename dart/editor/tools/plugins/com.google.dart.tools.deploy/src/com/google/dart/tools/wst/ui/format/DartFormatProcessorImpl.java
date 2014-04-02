/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.wst.ui.format;

import com.google.dart.tools.ui.internal.formatter.DartFormatter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.w3c.dom.Node;

import java.io.IOException;

@SuppressWarnings("restriction")
public class DartFormatProcessorImpl implements IStructuredFormatProcessor {

  private IProgressMonitor progressMonitor;

  @Override
  public void formatDocument(IDocument doc, int start, int length) throws IOException,
      CoreException {
    // Unused
  }

  @Override
  public void formatFile(IFile file) throws IOException, CoreException {
    DartFormatter.format(file, progressMonitor);
  }

  @Override
  public void formatModel(IStructuredModel arg0) {
    // Unused
  }

  @Override
  public void formatModel(IStructuredModel arg0, int arg1, int arg2) {
    // Unused
  }

  @Override
  public void formatNode(Node arg0) {
    // Unused
  }

  @Override
  public void setProgressMonitor(IProgressMonitor pm) {
    this.progressMonitor = pm;
  }

}
