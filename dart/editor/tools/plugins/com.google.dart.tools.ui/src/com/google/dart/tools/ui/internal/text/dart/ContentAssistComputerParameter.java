/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import org.eclipse.core.commands.IParameterValues;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Map of parameters for the specific content assist command.
 */
public final class ContentAssistComputerParameter implements IParameterValues {
  /*
   * @see org.eclipse.core.commands.IParameterValues#getParameterValues()
   */
  @Override
  public Map getParameterValues() {
    Collection descriptors = CompletionProposalComputerRegistry.getDefault().getProposalCategories();
    Map map = new HashMap(descriptors.size());
    for (Iterator it = descriptors.iterator(); it.hasNext();) {
      CompletionProposalCategory category = (CompletionProposalCategory) it.next();
      map.put(category.getDisplayName(), category.getId());
    }
    return map;
  }
}
