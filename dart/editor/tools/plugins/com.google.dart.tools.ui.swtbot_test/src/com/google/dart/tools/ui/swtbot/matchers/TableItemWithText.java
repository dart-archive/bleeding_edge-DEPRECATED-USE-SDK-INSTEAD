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
package com.google.dart.tools.ui.swtbot.matchers;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.regex.Pattern;

/**
 * Match a table item that contains text matching the specified regular expression.
 */
public final class TableItemWithText extends BaseMatcher<SWTBotTable> {
  private final String regex;
  private final Pattern pattern;

  public TableItemWithText(String regex) {
    if (regex == null || regex.length() == 0) {
      throw new IllegalArgumentException();
    }
    this.regex = regex;
    this.pattern = Pattern.compile(regex);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("table with item matching ").appendText(regex);
  }

  @Override
  public boolean matches(Object item) {
    SWTBotTable table = (SWTBotTable) item;
    int rowCount = table.rowCount();
    for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
      String text = table.cell(rowIndex, 0);
      if (text != null) {
        return pattern.matcher(text).matches();
      }
    }
    return false;
  }
}
