package com.xored.glance.internal.ui;

import com.xored.glance.internal.ui.search.SearchManager;

import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.BindingManager;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.keys.BindingService;
import org.eclipse.ui.internal.keys.WorkbenchKeyboard;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;

import java.util.List;
import java.util.Map;

@SuppressWarnings("restriction")
public class GlanceEventDispatcher {

  public static final String GLANCE_CTX = "com.xored.glance.ui.context";
  public static final String TEXT_EDITOR_CTX = "org.eclipse.ui.textEditorScope";

  public static final String NEXT_COMMAND = "com.xored.glance.ui.nextResult";
  public static final String PREV_COMMAND = "com.xored.glance.ui.prevResult";
  public static final String FOCUS_COMMAND = "com.xored.glance.commands.focus";
  public static final String CLOSE_COMMAND = "com.xored.glance.commands.close";
  public static final String CLEAR_COMMAND = "com.xored.glance.commands.clearHistory";

  public static GlanceEventDispatcher INSTANCE = new GlanceEventDispatcher();

  private final BindingManager bindingManager;

  private GlanceEventDispatcher() {
    bindingManager = ((BindingService) PlatformUI.getWorkbench().getService(IBindingService.class)).getBindingManager();
  }

  public void dispatchKeyPressed(Event event) {
    @SuppressWarnings("unchecked")
    List<Object> potentialKeyStrokes = WorkbenchKeyboard.generatePossibleKeyStrokes(event);
    if (potentialKeyStrokes.isEmpty()) {
      return;
    }

    String commandID = getBindCommand(KeySequence.getInstance((KeyStroke) potentialKeyStrokes.get(0)));
    if (commandID == null) {
      return;
    } else if (FOCUS_COMMAND.equals(commandID)) {
//      SearchManager.getIntance().sourceFocus(); // in case someone has a key binding already
    } else if (NEXT_COMMAND.equals(commandID)
        || IWorkbenchActionDefinitionIds.FIND_NEXT.equals(commandID)) {
      SearchManager.getIntance().findNext();
      event.doit = false;
    } else if (PREV_COMMAND.equals(commandID)
        || IWorkbenchActionDefinitionIds.FIND_PREVIOUS.equals(commandID)) {
      SearchManager.getIntance().findPrevious();
      event.doit = false;
    } else if (CLOSE_COMMAND.equals(commandID)) {
      SearchManager.getIntance().close();
      event.doit = false;
    } else if (CLEAR_COMMAND.equals(commandID)) {
      SearchManager.getIntance().clearHistory();
      event.doit = false;
    } else if (IWorkbenchCommandConstants.EDIT_SELECT_ALL.equals(commandID)) {
      SearchManager.getIntance().selectAll();
      event.doit = false;
    } else if (ITextEditorActionDefinitionIds.SMART_ENTER.equals(commandID)) {
      // on linux we have to deal with this
      SearchManager.getIntance().findPrevious();
      event.doit = false;
    }
  }

  public String getBindCommand(KeySequence keySequence) {
    Map<?, ?> map = bindingManager.getActiveBindingsDisregardingContext();
    List<?> bindings = (List<?>) map.get(keySequence);
    if (bindings != null) {
      for (Object obj : bindings) {
        Binding binding = (Binding) obj;
        String ctx = binding.getContextId();
        if (GLANCE_CTX.equals(ctx) || TEXT_EDITOR_CTX.equals(ctx)) {
          return binding.getParameterizedCommand().getId();
        }
      }
    }
    return null;
  }

}
