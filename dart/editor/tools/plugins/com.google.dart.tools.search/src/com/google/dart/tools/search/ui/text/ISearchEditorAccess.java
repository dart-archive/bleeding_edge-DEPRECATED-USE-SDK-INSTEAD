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
package com.google.dart.tools.search.ui.text;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;

/**
 * <p>
 * This interface allows editors to provide customized access to editor internals for the search
 * implementation to highlight matches. The search system will use the document to do line/character
 * offset conversion if needed and it will add annotations to the annotation model.
 * </p>
 * <p>
 * The search system will ask an editor for an adapter of this class whenever it needs access to the
 * document or the annotation model of the editor. Since an editor might use multiple documents
 * and/or annotation models, the match is passed in when asking the editor. The editor is then
 * expected to return the proper annotation model or document for the given match.
 * </p>
 * <p>
 * This interface is intended to be implemented by clients.
 * </p>
 */
public interface ISearchEditorAccess {
  /**
   * Finds the document displaying the match.
   * 
   * @param match the match
   * @return the document displaying the given match.
   */
  IDocument getDocument(Match match);

  /**
   * Finds the annotation model for the given match
   * 
   * @param match the match
   * @return the annotation model displaying the given match.
   */
  IAnnotationModel getAnnotationModel(Match match);
}
