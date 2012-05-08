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
package com.google.dart.tools.search.internal.ui.text;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.Region;

/**
 * A {@link FileResourceMatch} backed by an {@link IFile} in the workspace.
 */
public class FileMatch extends FileResourceMatch {

  private Region originalLocation;

  public FileMatch(IFile element) {
    super(element, -1, -1);
    originalLocation = null;
  }

  public FileMatch(IFile element, int offset, int length, LineElement lineEntry) {
    super(element, offset, length, lineEntry);
  }

  public IFile getFile() {
    return (IFile) getElement();
  }

  public int getOriginalLength() {
    if (originalLocation != null) {
      return originalLocation.getLength();
    }
    return getLength();
  }

  public int getOriginalOffset() {
    if (originalLocation != null) {
      return originalLocation.getOffset();
    }
    return getOffset();
  }

  public boolean isFileSearch() {
    return getLineElement() == null;
  }

  @Override
  public void setLength(int length) {
    if (originalLocation == null) {
      // remember the original location before changing it
      originalLocation = new Region(getOffset(), getLength());
    }
    super.setLength(length);
  }

  @Override
  public void setOffset(int offset) {
    if (originalLocation == null) {
      // remember the original location before changing it
      originalLocation = new Region(getOffset(), getLength());
    }
    super.setOffset(offset);
  }
}
