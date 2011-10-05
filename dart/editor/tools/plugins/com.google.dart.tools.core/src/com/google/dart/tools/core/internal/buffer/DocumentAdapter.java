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
package com.google.dart.tools.core.internal.buffer;

import com.google.dart.tools.core.buffer.Buffer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

/**
 * Instances of the class <code>DocumentAdapter</code> adapt a {@link Buffer} to a {@link Document}.
 */
public class DocumentAdapter extends Document {
  /**
   * The buffer being adapted.
   */
  private Buffer buffer;

  /**
   * Initialize a newly created adaptor to adapt the given buffer to a document.
   * 
   * @param buffer the buffer being adapted.
   */
  public DocumentAdapter(Buffer buffer) {
    super(buffer.getContents());
    this.buffer = buffer;
  }

  @Override
  public void replace(int offset, int length, String text) throws BadLocationException {
    super.replace(offset, length, text);
    buffer.replace(offset, length, text);
  }

  @Override
  public void set(String text) {
    super.set(text);
    buffer.setContents(text);
  }
}
