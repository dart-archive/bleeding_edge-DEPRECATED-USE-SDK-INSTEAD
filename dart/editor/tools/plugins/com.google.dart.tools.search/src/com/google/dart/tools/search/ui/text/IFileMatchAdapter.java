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

import org.eclipse.core.resources.IFile;

/**
 * This interface serves to map matches to <code>IFile</code> instances. Changes to those files are
 * then tracked (via the platforms file buffer mechanism) and matches updated when changes are
 * saved. Clients who want their match positions automatically updated should return an
 * implementation of <code>IFileMatchAdapter</code> from the <code>getFileMatchAdapter()</code>
 * method in their search result implementation. It is assumed that the match adapters are
 * stateless, and no lifecycle management is provided.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @see com.google.dart.tools.search.ui.text.AbstractTextSearchResult
 */
public interface IFileMatchAdapter {
  /**
   * Returns an array with all matches contained in the given file in the given search result. If
   * the matches are not contained within an <code>IFile</code>, this method must return an empty
   * array.
   * 
   * @param result the search result to find matches in
   * @param file the file to find matches in
   * @return an array of matches (possibly empty)
   */
  public abstract Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file);

  /**
   * Returns the file associated with the given element (usually the file the element is contained
   * in). If the element is not associated with a file, this method should return <code>null</code>.
   * 
   * @param element an element associated with a match
   * @return the file associated with the element or <code>null</code>
   */
  public abstract IFile getFile(Object element);
}
