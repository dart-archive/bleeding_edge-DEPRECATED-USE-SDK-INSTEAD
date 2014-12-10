/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.eclipse.ui.internal.handler;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.internal.search.ui.FindReferencesAction;
import com.google.dart.tools.internal.search.ui.FindReferencesAction_NEW;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Find uses
 */
public class FindUsesHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ISelection selection = HandlerUtil.getCurrentSelection(event);
    String searchText = ((DartSelection) selection).getText();;
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      FindReferencesAction_NEW.searchNameUses(searchText);
    } else {
      FindReferencesAction.searchNameUses(searchText);
    }
    return null;
  }
}
