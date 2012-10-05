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
package com.google.dart.tools.ui.internal.handlers;

import com.google.dart.tools.core.DartCore;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to enable/disable the File New menu item
 */
public class NewFileCommandState extends AbstractSourceProvider {

  public static String NEW_FILE_STATE = "com.google.dart.tools.ui.newFile.active"; //$NON-NLS-N$
  public final static String ENABLED = "ENABLED"; //$NON-NLS-N$
  public final static String DISABLED = "DISABLED"; //$NON-NLS-N$

  public void checkState() {
    String currentState = isEmpty() ? DISABLED : ENABLED;
    fireSourceChanged(ISources.WORKBENCH, NEW_FILE_STATE, currentState);
  }

  @Override
  public void dispose() {

  }

  @Override
  public Map<String, String> getCurrentState() {
    Map<String, String> map = new HashMap<String, String>(1);
    String value = isEmpty() ? DISABLED : ENABLED;
    map.put(NEW_FILE_STATE, value);
    return map;
  }

  @Override
  public String[] getProvidedSourceNames() {
    return new String[] {NEW_FILE_STATE};
  }

  private boolean isEmpty() {
    return DartCore.getDirectorySetManager().getChildren().length == 0;
  }
}
