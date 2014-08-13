package com.google.dart.tools.tests.swtbot.model;

import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.hamcrest.Matcher;

import java.util.List;

/**
 * Model the debugger's context view.
 */
public class DebuggerContextBotView extends AbstractTreeBotView {

  private SWTBotTree treeBot;

  public DebuggerContextBotView(SWTWorkbenchBot bot, SWTBotTree treeBot) {
    super(bot);
    this.treeBot = treeBot;
  }

  /**
   * Get the SWTBotTree for this view.
   * 
   * @return the SWTBotTree
   */
  @Override
  public SWTBotTree tree() {
    return treeBot;
  }

  /**
   * Get all the widgets that are accessible from the Tree.
   * 
   * @return a list of widgets
   */
  public List<? extends Widget> widgets() {
    final Matcher<Widget> matcher = WidgetOfType.widgetOfType(Widget.class);
    return UIThreadRunnable.syncExec(new Result<List<? extends Widget>>() {
      @Override
      public List<? extends Widget> run() {
        return bot.widgets(matcher, treeBot.widget);
      }
    });
  }

  @Override
  protected String viewName() {
    return "Debugger";
  }

}
