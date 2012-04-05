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

package com.google.dart.tools.debug.ui.internal.util;

import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.ide.FileStoreEditorInput;

/**
 * A custom FileStoreEditorInput used to represent scripts loaded from a remote debug server.
 */
public class DebuggerEditorInput extends FileStoreEditorInput {

  public DebuggerEditorInput(IFileStore fileStore) {
    super(fileStore);
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return DartDebugUIPlugin.getImageDescriptor("obj16/remoteDartFile.png");
  }

  @Override
  public String getName() {
    // Convert dart_foo$123456.dart to dart:foo.
    String name = super.getName().replaceFirst("_", ":");

    int index = name.indexOf('$');

    if (index != -1) {
      name = name.substring(0, index);
    }

    return name;
  }

  @Override
  public IPersistableElement getPersistable() {
    return null;
  }

  @Override
  public String getToolTipText() {
    return getName();
  }

  @Override
  public void saveState(IMemento memento) {

  }

}
