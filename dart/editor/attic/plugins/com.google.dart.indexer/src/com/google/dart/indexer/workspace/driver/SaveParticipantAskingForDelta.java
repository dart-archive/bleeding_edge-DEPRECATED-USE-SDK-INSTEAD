/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.workspace.driver;

import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.runtime.CoreException;

final class SaveParticipantAskingForDelta implements ISaveParticipant {
  @Override
  public void doneSaving(ISaveContext context) {
  }

  @Override
  public void prepareToSave(ISaveContext context) throws CoreException {
  }

  @Override
  public void rollback(ISaveContext context) {
  }

  @Override
  public void saving(ISaveContext context) throws CoreException {
    // has an effect only when doing a full save (i.e. exiting Eclipse)
    context.needDelta();
  }
}
