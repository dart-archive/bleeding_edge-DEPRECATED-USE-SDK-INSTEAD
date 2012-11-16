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
 * Implementers translate sequences of {@link Edit} descriptions into concrete {@link EditOperation}
 * s that can apply them.
 * 
 * @param <D> the document type
 * @param <R> an (optional) return result type
 */
public interface EditBuilder<D, R> {

  /**
   * Build an edit operation that can apply a given sequence of edit descriptions.
   * 
   * @param edits the sequence of edits to apply
   * @return the resulting edit operation
   */
  EditOperation<D, R> buildEdit(Iterable<Edit> edits);

}
