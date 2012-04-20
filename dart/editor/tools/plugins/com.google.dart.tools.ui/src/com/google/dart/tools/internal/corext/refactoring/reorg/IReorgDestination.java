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
package com.google.dart.tools.internal.corext.refactoring.reorg;

import org.eclipse.jface.viewers.ViewerDropAdapter;

/**
 * {@link ReorgDestinationFactory} can create concrete instances
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public interface IReorgDestination {

  public static final int LOCATION_BEFORE = ViewerDropAdapter.LOCATION_BEFORE;
  public static final int LOCATION_AFTER = ViewerDropAdapter.LOCATION_AFTER;
  public static final int LOCATION_ON = ViewerDropAdapter.LOCATION_ON;

  public Object getDestination();

  public int getLocation();
}
