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
package com.google.dart.engine.formatter.edit;

/**
 * An {@code EditOperation} applies an edit (or sequence of edits) to a document, optionally
 * returning a result.
 * 
 * @param <D> the document type
 * @param <R> an optional result
 */
public interface EditOperation<D, R> {

  /**
   * Apply this operation to the given document. Concrete implementations may optionally return a
   * result (such as an undo edit).
   * 
   * @param document the document to which to apply the edit(s)
   * @return an optional result (such as undo operation)
   */
  R applyTo(D document);

}
