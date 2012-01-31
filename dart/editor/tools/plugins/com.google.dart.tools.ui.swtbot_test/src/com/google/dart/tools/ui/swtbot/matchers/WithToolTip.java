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

import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

/**
 * Matches a widget having tool tip matching the specified regular expression.
 */
public class WithToolTip extends BaseMatcher<Widget> {

  /**
   * Gets the text of the object using the getText method. If the object doesn't contain a get text
   * method an exception is thrown.
   * 
   * @param obj any object to get the text from.
   * @return the return value of obj#getText()
   * @throws NoSuchMethodException if the method "getToolTipText" does not exist on the object.
   * @throws IllegalAccessException if the java access control does not allow invocation.
   * @throws InvocationTargetException if the method "getToolTipText" throws an exception.
   * @see Method#invoke(Object, Object[])
   */
  private static String getToolTip(Object obj) throws NoSuchMethodException,
      IllegalAccessException, InvocationTargetException {
    return ((String) SWTUtils.invokeMethod(obj, "getToolTipText")).replaceAll(Text.DELIMITER, "\n"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private final String regex;
  private final Pattern pattern;

  public WithToolTip(String regex) {
    if (regex == null || regex.length() == 0) {
      throw new IllegalArgumentException();
    }
    this.regex = regex;
    this.pattern = Pattern.compile(regex);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("tool tip matching ").appendText(regex);
  }

  @Override
  public boolean matches(Object item) {
    try {
      String text = getToolTip(item);
      if (text != null) {
        return pattern.matcher(text).matches();
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

}
