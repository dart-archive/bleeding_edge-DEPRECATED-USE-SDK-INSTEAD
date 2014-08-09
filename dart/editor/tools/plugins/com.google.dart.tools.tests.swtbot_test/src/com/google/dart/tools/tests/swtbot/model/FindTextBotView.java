/*
 * Copyright 2014 Dart project authors.
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
package com.google.dart.tools.tests.swtbot.model;

import com.xored.glance.internal.ui.GlancePlugin;
import com.xored.glance.internal.ui.panels.SearchPanelManager;
import com.xored.glance.internal.ui.preferences.IPreferenceConstants;
import com.xored.glance.ui.panels.ISearchPanel;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.ControlFinder;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.results.StringResult;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCLabel;
import org.hamcrest.Matcher;

import java.util.List;

public class FindTextBotView extends AbstractBotView {

  private static final int WAIT_TIME = 300;

  public FindTextBotView(SWTWorkbenchBot bot) {
    super(bot);
  }

  /**
   * Close the search panel.
   */
  public void dismiss() {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        getPanel().closePanel();
      }
    });
  }

  /**
   * Select the next match. Does not re-index but does update search status.
   */
  public void findNext() {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        getPanel().findNext();
      }
    });
    waitForAsyncDrain();
  }

  /**
   * Select the previous match. Does not re-index but does update search status.
   */
  public void findPrevious() {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        getPanel().findPrevious();
      }
    });
    waitForAsyncDrain();
  }

  /**
   * Search for the given <code>text</code>. This causes re-indexing, which will update the search
   * status.
   * 
   * @param text the string the find
   */
  public void findText(final String text) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        getCombo().setText(text);
      }
    });
    // There's no way to tell that the search has finished
    waitMillis(WAIT_TIME);
    waitForAsyncDrain();
  }

  /**
   * Get the search status string, which shows the number of matches and which one is currently
   * selected, if any.
   * 
   * @return the search status string
   */
  public String getSearchStatus() {
    return UIThreadRunnable.syncExec(new StringResult() {
      @Override
      public String run() {
        return getSearchStatusLabel().getText();
      }
    });
  }

  /**
   * Set the preference that determines if searching is case-sensitive or not to the given
   * <code>value</code>. This causes re-indexing, which will update the search status.
   * 
   * @param value <code>true</code> if searching should be case sensitive.
   */
  public void setCaseSensitiveSearch(final boolean value) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        IPreferenceStore preferences = GlancePlugin.getDefault().getPreferenceStore();
        preferences.setValue(IPreferenceConstants.SEARCH_CASE_SENSITIVE, value);
      }
    });
    waitMillis(WAIT_TIME);
    waitForAsyncDrain();
  }

  /**
   * Set the preference that determines if the search string should be interpreted as a regular
   * expression or not to the given <code>value</code>. This causes re-indexing, which will update
   * the search status.
   * 
   * @param value <code>true</code> if searching should use regular expression rules.
   */
  public void setRegexpSearch(final boolean value) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        IPreferenceStore preferences = GlancePlugin.getDefault().getPreferenceStore();
        preferences.setValue(IPreferenceConstants.SEARCH_REGEXP, value);
      }
    });
    waitMillis(WAIT_TIME);
    waitForAsyncDrain();
  }

  /**
   * Set the preference that determines if searching only matches the string at the beginning of a
   * word or not to the given <code>value</code>. This causes re-indexing, which will update the
   * search status.
   * 
   * @param value <code>true</code> if searching should match word-beginnings only.
   */
  public void setWordPrefixSearch(final boolean value) {
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        IPreferenceStore preferences = GlancePlugin.getDefault().getPreferenceStore();
        preferences.setValue(IPreferenceConstants.SEARCH_WORD_PREFIX, value);
      }
    });
    waitMillis(WAIT_TIME);
    waitForAsyncDrain();
  }

  @Override
  protected String viewName() {
    return "FindText";
  }

  private Combo getCombo() {
    // Must run on UI thread.
    Composite parent = (Composite) getPanel().getControl();
    Matcher<Combo> matcher = WidgetOfType.widgetOfType(Combo.class);
    List<Combo> list = new ControlFinder().findControls(parent, matcher, true);
    if (list.isEmpty()) {
      return null;
    }
    return list.get(0);
  }

  private ISearchPanel getPanel() {
    // Must run on UI thread.
    return SearchPanelManager.getInstance().getPanel(bot.getFocusedWidget());
  }

  private SWTBotCLabel getSearchStatusLabel() {
    // Must run on UI thread.
    return bot.clabelWithId("name", "searchStatus");
  }
}
