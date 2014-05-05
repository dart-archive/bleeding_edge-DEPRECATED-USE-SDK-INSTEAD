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
package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.core.pub.IPubServeListener;
import com.google.dart.tools.debug.core.pubserve.PubServeManager;
import com.google.dart.tools.ui.actions.InstrumentedAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;

import org.eclipse.swt.widgets.Event;

/**
 * Action that stops the currently running pub serve process.
 */
public class StopPubServeAction extends InstrumentedAction implements IPubServeListener {

  public StopPubServeAction() {
    super("Pub Serve [stopped]");
    setEnabled(PubServeManager.getManager().isServing());
    PubServeManager.getManager().addListener(this);
  }

  public void dispose() {
    PubServeManager.getManager().removeListener(this);
  }

  @Override
  public void pubServeStatus(boolean isServing) {
    if (isServing) {
      setText("Stop Pub Serve");
    } else {
      setText("Pub Serve [stopped]");
    }
    super.setEnabled(isServing);
  }

  @Override
  protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {
    PubServeManager.getManager().terminatePubServe();
  }
}
