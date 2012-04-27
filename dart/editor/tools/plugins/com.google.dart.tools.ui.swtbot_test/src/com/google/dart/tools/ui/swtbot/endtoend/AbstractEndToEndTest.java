/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.ui.swtbot.endtoend;

import com.google.dart.tools.ui.swtbot.EndToEndUITest;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

/**
 * This <code>abstract</code> class is used by {@link EndToEndUITest} to avoid having a lot of
 * repeating code.
 */
public abstract class AbstractEndToEndTest {

  final SWTWorkbenchBot bot;

  public AbstractEndToEndTest(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  abstract public void afterTest();

  abstract public void runTest() throws Exception;
}
