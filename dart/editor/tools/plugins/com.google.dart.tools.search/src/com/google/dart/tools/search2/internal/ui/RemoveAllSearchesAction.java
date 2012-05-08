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
package com.google.dart.tools.search2.internal.ui;

import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.NewSearchUI;

import org.eclipse.jface.action.Action;

class RemoveAllSearchesAction extends Action {

  public RemoveAllSearchesAction() {
    super(SearchMessages.RemoveAllSearchesAction_label);
    setToolTipText(SearchMessages.RemoveAllSearchesAction_tooltip);
  }

  public void run() {
    ISearchQuery[] queries = NewSearchUI.getQueries();
    for (int i = 0; i < queries.length; i++) {
      if (!NewSearchUI.isQueryRunning(queries[i]))
        InternalSearchUI.getInstance().removeQuery(queries[i]);
    }
  }
}
