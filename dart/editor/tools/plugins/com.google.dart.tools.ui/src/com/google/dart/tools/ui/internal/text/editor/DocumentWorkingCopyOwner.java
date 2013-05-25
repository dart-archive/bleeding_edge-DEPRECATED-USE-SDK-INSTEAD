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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.model.SourceFileElement;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

/**
 * A WorkingCopyOwner that knows about editor Documents.
 */
public class DocumentWorkingCopyOwner extends WorkingCopyOwner {

  @Override
  public Buffer createBuffer(SourceFileElement<?> workingCopy) {

    IResource resource = workingCopy.getResource();
    if (resource instanceof IFile) {
      return new DocumentAdapter(workingCopy, (IFile) resource);
    }
    return DocumentAdapter.NULL;
  }

}
