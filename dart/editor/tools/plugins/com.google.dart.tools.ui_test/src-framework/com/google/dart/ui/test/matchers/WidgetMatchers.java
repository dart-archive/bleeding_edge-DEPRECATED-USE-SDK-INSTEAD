/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.ui.test.matchers;

import org.eclipse.swt.widgets.Widget;

/**
 * The factory of {@link WidgetMatcher}s.
 */
public class WidgetMatchers {
  /**
   * @return the composite {@link WidgetMatcher} that is matches iff all the given
   *         {@link WidgetMatcher} do match.
   */
  public static WidgetMatcher and(WidgetMatcher... matchers) {
    return new AndWidgetMatcher(matchers);
  }

  /**
   * @return the {@link WidgetMatcher} that matches any {@link Widget}s of the given class.
   */
  public static WidgetMatcher ofClass(Class<?> cls) {
    return new IsClassWidgetMatcher(cls);
  }

  /**
   * @return the {@link WidgetMatcher} that matches any widget with the same (equals) or matching
   *         (regular expression) text.
   */
  public static WidgetMatcher withText(String textOrPattern) {
    return new HasTextWidgetMatcher(textOrPattern);
  }
}
