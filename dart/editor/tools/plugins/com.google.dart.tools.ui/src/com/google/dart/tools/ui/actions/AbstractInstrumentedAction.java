/*
 * Copyright (c) 2011, the Dart project authors.
 */
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import java.util.HashMap;

public class AbstractInstrumentedAction extends Action {

  public AbstractInstrumentedAction() {
  }

  protected void EmitInstrumentationCommand() {
    try {

      //It doesn't matter which window this command is fired from
      ICommandService cmd = (ICommandService) PlatformUI.getWorkbench().getWorkbenchWindows()[0].getService(ICommandService.class);
      Command ic = cmd.getCommand("com.google.dart.tools.ui.BroadcastCommand");

      HashMap<String, String> props = new HashMap<String, String>();
      props.put("ActionType", this.getClass().toString());

      ExecutionEvent ev = new ExecutionEvent(ic, props, this, PlatformUI.getWorkbench());

      ic.executeWithChecks(ev);
    } catch (Exception e) {
      DartCore.logError(e);
    }

  }

}
