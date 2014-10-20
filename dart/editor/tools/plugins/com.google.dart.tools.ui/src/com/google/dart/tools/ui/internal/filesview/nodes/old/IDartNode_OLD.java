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

package com.google.dart.tools.ui.internal.filesview.nodes.old;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * An interface used to represent Dart SDK nodes in the Files view.
 */
public interface IDartNode_OLD {

  public IFileStore getFileStore();

  public ImageDescriptor getImageDescriptor();

  public String getLabel();

  public Object getParent();

}
