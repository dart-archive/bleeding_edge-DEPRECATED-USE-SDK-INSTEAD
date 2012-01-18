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

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

import java.io.File;

/**
 * Condition that waits for a specified file to exist
 */
public class FileExists implements ICondition {
  private final File file;

  public FileExists(File file) {
    this.file = file;
  }

  public FileExists(File dir, String name) {
    this(new File(dir, name));
  }

  public FileExists(String path) {
    this(new File(path));
  }

  public FileExists(String dir, String name) {
    this(new File(dir, name));
  }

  @Override
  public String getFailureMessage() {
    return "File does not exist: " + file;
  }

  @Override
  public void init(SWTBot bot) {
  }

  @Override
  public boolean test() throws Exception {
    return file.exists();
  }
}
