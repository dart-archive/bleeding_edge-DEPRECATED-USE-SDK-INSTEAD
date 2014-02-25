/**
 * 
 */
package com.xored.glance.internal.ui;

import com.xored.glance.internal.ui.search.SearchManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * @author Yuri Strot
 */
public class OpenSearchPanelHandler extends AbstractHandler {

  /**
   * The constructor.
   */
  public OpenSearchPanelHandler() {
  }

  /**
   * the command has been executed, so extract extract the needed information from the application
   * context.
   */
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    SearchManager.getIntance().activate();
    return null;
  }
}
