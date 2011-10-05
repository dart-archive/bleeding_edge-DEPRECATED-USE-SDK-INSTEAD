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
package com.google.dart.indexer.pagedstorage.exceptions;

public class PagedStorageException extends Exception {
  private static final long serialVersionUID = 1L;

  public PagedStorageException() {
    super();
  }

  public PagedStorageException(String message) {
    super(message);
  }

  public PagedStorageException(String message, Throwable cause) {
    super(message, cause);
  }

  public PagedStorageException(Throwable cause) {
    super(cause);
  }
}
