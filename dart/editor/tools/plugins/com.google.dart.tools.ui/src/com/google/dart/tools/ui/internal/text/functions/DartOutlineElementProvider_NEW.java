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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.ui.IEditorPart;

/**
 * Provides a Dart element to be displayed in outline.
 */
public class DartOutlineElementProvider_NEW implements IInformationProvider,
    IInformationProviderExtension {
  private final DartEditor fEditor;

  public DartOutlineElementProvider_NEW(IEditorPart editor) {
    fEditor = editor instanceof DartEditor ? (DartEditor) editor : null;
  }

  @Override
  public String getInformation(ITextViewer textViewer, IRegion subject) {
    return getInformation2(textViewer, subject).toString();
  }

  @Override
  public Object getInformation2(ITextViewer textViewer, IRegion subject) {
    return fEditor.getOutline();
  }

  @Override
  public IRegion getSubject(ITextViewer textViewer, int offset) {
    return new Region(offset, 0);
  }
}
