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
package com.google.dart.tools.ui.internal.util;

import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Empty implementation of {@link IPartListener}.
 */
public class PartListenerAdapter implements IPartListener {

  @Override
  public void partActivated(IWorkbenchPart part) {
  }

  @Override
  public void partBroughtToTop(IWorkbenchPart part) {
  }

  @Override
  public void partClosed(IWorkbenchPart part) {
  }

  @Override
  public void partDeactivated(IWorkbenchPart part) {
  }

  @Override
  public void partOpened(IWorkbenchPart part) {
  }
}
