package com.google.dart.tools.tests.swtbot.model;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.waits.Conditions;

public class InlineMethodBotView extends AbstractBotView {

  public InlineMethodBotView(SWTWorkbenchBot bot) {
    super(bot);
    bot.waitUntilWidgetAppears(Conditions.shellIsActive(viewName()));
  }

  public void clickAllInvocations() {
    inlineMethodComposite().radio("All invocations").click();
  }

  public void close() {
    inlineMethodShell().button("OK").click();
    waitForAnalysis();
  }

  @Override
  protected String viewName() {
    return "Inline Method";
  }

  private SWTBot inlineMethodComposite() {
    return UIThreadRunnable.syncExec(new Result<SWTBot>() {
      @Override
      public SWTBot run() {
        return new SWTBot(bot.radio("Only the selected invocation").widget.getParent());
      }
    });
  }

  private SWTBot inlineMethodShell() {
    return UIThreadRunnable.syncExec(new Result<SWTBot>() {
      @Override
      public SWTBot run() {
        return new SWTBot(
            bot.radio("Only the selected invocation").widget.getParent().getParent().getParent());
      }
    });
  }
}
