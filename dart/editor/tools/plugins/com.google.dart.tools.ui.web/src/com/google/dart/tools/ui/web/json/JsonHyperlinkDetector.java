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

package com.google.dart.tools.ui.web.json;

import com.google.dart.tools.ui.web.utils.FileHyperlink;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

/**
 * Detects IFile links in JSON files.
 */
class JsonHyperlinkDetector extends AbstractHyperlinkDetector {
  private JsonEditor editor;

  public JsonHyperlinkDetector(JsonEditor editor) {
    this.editor = editor;
  }

  @Override
  public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
      boolean canShowMultipleHyperlinks) {
    if (region == null || textViewer == null || editor == null) {
      return null;
    }

    IDocument document = textViewer.getDocument();

    try {
      IRegion strRegion = getRegionForQuotedText(document, region.getOffset());

      if (strRegion == null) {
        return null;
      }

      // Validate that this path is an existing file.
      IFile file = getFileFor(
          textViewer,
          document.get(strRegion.getOffset(), strRegion.getLength()));

      if (file != null) {
        return new IHyperlink[] {new FileHyperlink(strRegion, file)};
      }
    } catch (BadLocationException e) {

    }

    return null;
  }

  private IFile getFileFor(ITextViewer textViewer, String path) {
    IFile baseFile = (IFile) editor.getEditorInput().getAdapter(IFile.class);

    if (baseFile != null) {
      IResource resource = baseFile.getParent().findMember(new Path(path));

      if (resource instanceof IFile && resource.exists()) {
        return (IFile) resource;
      }
    }

    return null;
  }

  private IRegion getRegionForQuotedText(IDocument document, int offset)
      throws BadLocationException {
    IRegion border = document.getLineInformationOfOffset(offset);

    int start = offset;
    int end = offset;

    char c = document.getChar(start);

    while (c != '"' && c != '\'') {
      start--;

      if (start < border.getOffset()) {
        return null;
      }

      c = document.getChar(start);
    }

    c = document.getChar(end);

    while (c != '"' && c != '\'') {
      end++;

      if (end > (border.getOffset() + border.getLength())) {
        return null;
      }

      c = document.getChar(end);
    }

    return new Region(start + 1, end - start - 1);
  }

}
