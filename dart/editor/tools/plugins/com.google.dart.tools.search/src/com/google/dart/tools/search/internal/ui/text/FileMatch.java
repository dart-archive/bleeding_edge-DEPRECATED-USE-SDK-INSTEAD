/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.Region;


public class FileMatch extends Match {
  private LineElement fLineElement;
  private Region fOriginalLocation;

  public FileMatch(IFile element) {
    super(element, -1, -1);
    fLineElement = null;
    fOriginalLocation = null;
  }

  public FileMatch(IFile element, int offset, int length, LineElement lineEntry) {
    super(element, offset, length);
    Assert.isLegal(lineEntry != null);
    fLineElement = lineEntry;
  }

  public void setOffset(int offset) {
    if (fOriginalLocation == null) {
      // remember the original location before changing it
      fOriginalLocation = new Region(getOffset(), getLength());
    }
    super.setOffset(offset);
  }

  public void setLength(int length) {
    if (fOriginalLocation == null) {
      // remember the original location before changing it
      fOriginalLocation = new Region(getOffset(), getLength());
    }
    super.setLength(length);
  }

  public int getOriginalOffset() {
    if (fOriginalLocation != null) {
      return fOriginalLocation.getOffset();
    }
    return getOffset();
  }

  public int getOriginalLength() {
    if (fOriginalLocation != null) {
      return fOriginalLocation.getLength();
    }
    return getLength();
  }

  public LineElement getLineElement() {
    return fLineElement;
  }

  public IFile getFile() {
    return (IFile) getElement();
  }

  public boolean isFileSearch() {
    return fLineElement == null;
  }
}
