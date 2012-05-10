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
package com.google.dart.tools.ui.swtbot.conditions;

import com.google.dart.tools.core.utilities.compiler.DartCompilerWarmup;
import com.google.dart.tools.ui.swtbot.performance.SwtBotPerformance;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

/**
 * Condition that waits until the Dart Compiler is warmed up
 * 
 * @see #waitUntilWarmedUp(SWTWorkbenchBot)
 */
public class CompilerWarmedUp implements ICondition {
  public static void waitUntilWarmedUp(SWTWorkbenchBot bot) {
    SwtBotPerformance.COMPILER_WARMUP.log(bot, new CompilerWarmedUp());
  }

  @Override
  public String getFailureMessage() {
    return "Gave up waiting for compiler warmup";
  }

  @Override
  public void init(SWTBot bot) {
  }

  @Override
  public boolean test() throws Exception {
    return DartCompilerWarmup.isComplete();
  }
}
