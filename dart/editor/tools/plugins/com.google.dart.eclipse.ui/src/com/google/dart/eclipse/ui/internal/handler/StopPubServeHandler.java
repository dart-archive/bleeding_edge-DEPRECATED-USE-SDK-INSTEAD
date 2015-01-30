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

import com.google.dart.tools.core.pub.IPubServeListener;
import com.google.dart.tools.debug.core.pubserve.PubServeManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Stop pub serve
 */
public class StopPubServeHandler extends AbstractHandler implements IPubServeListener {

  PubServeManager manager = PubServeManager.getManager();

  public StopPubServeHandler() {
    manager.addListener(this);
    setBaseEnabled(manager.isServing());
  }

  @Override
  public void dispose() {
    manager.removeListener(this);
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    manager.terminatePubServe();
    return null;
  }

  @Override
  public void pubServeStatus(boolean isServing) {
    super.setBaseEnabled(isServing);
  }
}
