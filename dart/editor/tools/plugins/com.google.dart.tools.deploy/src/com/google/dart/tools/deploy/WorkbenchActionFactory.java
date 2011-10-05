/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.deploy;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.handlers.IActionCommandMappingService;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

/**
 * A factory for creating workbench actions.
 */
@SuppressWarnings("restriction")
public class WorkbenchActionFactory {

  public final IWorkbenchWindow window;

  public WorkbenchActionFactory(IWorkbenchWindow window) {
    this.window = window;
  }

  public IContributionItem getBookmarkItem() {
    return getItem(IDEActionFactory.BOOKMARK.getId(), IDEActionFactory.BOOKMARK.getCommandId(),
        null, null, IDEWorkbenchMessages.Workbench_addBookmark,
        IDEWorkbenchMessages.Workbench_addBookmarkToolTip, null);
  }

  public IContributionItem getCloseProjectItem() {
    return getItem(IDEActionFactory.CLOSE_PROJECT.getId(),
        IDEActionFactory.CLOSE_PROJECT.getCommandId(), null, null,
        IDEWorkbenchMessages.CloseResourceAction_text,
        IDEWorkbenchMessages.CloseResourceAction_text, null);
  }

  public IContributionItem getCopyItem() {
    return getItem(ActionFactory.COPY.getId(), ActionFactory.COPY.getCommandId(),
        ISharedImages.IMG_TOOL_COPY, ISharedImages.IMG_TOOL_COPY_DISABLED,
        WorkbenchMessages.Workbench_copy, WorkbenchMessages.Workbench_copyToolTip, null);
  }

  public IContributionItem getCutItem() {
    return getItem(ActionFactory.CUT.getId(), ActionFactory.CUT.getCommandId(),
        ISharedImages.IMG_TOOL_CUT, ISharedImages.IMG_TOOL_CUT_DISABLED,
        WorkbenchMessages.Workbench_cut, WorkbenchMessages.Workbench_cutToolTip, null);
  }

  public IContributionItem getDeleteItem() {
    return getItem(ActionFactory.DELETE.getId(), ActionFactory.DELETE.getCommandId(),
        ISharedImages.IMG_TOOL_DELETE, ISharedImages.IMG_TOOL_DELETE_DISABLED,
        WorkbenchMessages.Workbench_delete, WorkbenchMessages.Workbench_deleteToolTip,
        IWorkbenchHelpContextIds.DELETE_RETARGET_ACTION);
  }

  public IContributionItem getFindItem() {
    return getItem(ActionFactory.FIND.getId(), ActionFactory.FIND.getCommandId(), null, null,
        WorkbenchMessages.Workbench_findReplace, WorkbenchMessages.Workbench_findReplaceToolTip,
        null);
  }

  public IContributionItem getItem(String actionId, String commandId, String image,
      String disabledImage, String label, String tooltip, String helpContextId) {
    ISharedImages sharedImages = getWindow().getWorkbench().getSharedImages();

    IActionCommandMappingService acms = (IActionCommandMappingService) getWindow().getService(
        IActionCommandMappingService.class);
    acms.map(actionId, commandId);

    CommandContributionItemParameter commandParm = new CommandContributionItemParameter(
        getWindow(), actionId, commandId, null, sharedImages.getImageDescriptor(image),
        sharedImages.getImageDescriptor(disabledImage), null, label, null, tooltip,
        CommandContributionItem.STYLE_PUSH, null, false);
    return new CommandContributionItem(commandParm);
  }

  public IContributionItem getMoveItem() {
    return getItem(ActionFactory.MOVE.getId(), ActionFactory.MOVE.getCommandId(), null, null,
        WorkbenchMessages.Workbench_move, WorkbenchMessages.Workbench_moveToolTip, null);
  }

  public IContributionItem getOpenProjectItem() {
    return getItem(IDEActionFactory.OPEN_PROJECT.getId(),
        IDEActionFactory.OPEN_PROJECT.getCommandId(), null, null,
        IDEWorkbenchMessages.OpenResourceAction_text,
        IDEWorkbenchMessages.OpenResourceAction_toolTip, null);
  }

  public IContributionItem getPasteItem() {
    return getItem(ActionFactory.PASTE.getId(), ActionFactory.PASTE.getCommandId(),
        ISharedImages.IMG_TOOL_PASTE, ISharedImages.IMG_TOOL_PASTE_DISABLED,
        WorkbenchMessages.Workbench_paste, WorkbenchMessages.Workbench_pasteToolTip, null);
  }

  public IContributionItem getPinEditorItem() {
    return ContributionItemFactory.PIN_EDITOR.create(window);
  }

  public IContributionItem getPrintItem() {
    return getItem(ActionFactory.PRINT.getId(), ActionFactory.PRINT.getCommandId(),
        ISharedImages.IMG_ETOOL_PRINT_EDIT, ISharedImages.IMG_ETOOL_PRINT_EDIT_DISABLED,
        WorkbenchMessages.Workbench_print, WorkbenchMessages.Workbench_printToolTip, null);
  }

  public IContributionItem getPropertiesItem() {
    return getItem(ActionFactory.PROPERTIES.getId(), ActionFactory.PROPERTIES.getCommandId(), null,
        null, WorkbenchMessages.Workbench_properties,
        WorkbenchMessages.Workbench_propertiesToolTip, null);
  }

  public IContributionItem getRefreshItem() {
    return getItem(ActionFactory.REFRESH.getId(), ActionFactory.REFRESH.getCommandId(), null, null,
        WorkbenchMessages.Workbench_refresh, WorkbenchMessages.Workbench_refreshToolTip, null);
  }

  public IContributionItem getRenameItem() {
    return getItem(ActionFactory.RENAME.getId(), ActionFactory.RENAME.getCommandId(), null, null,
        WorkbenchMessages.Workbench_rename, WorkbenchMessages.Workbench_renameToolTip, null);
  }

  public IContributionItem getRevertItem() {
    return getItem(ActionFactory.REVERT.getId(), ActionFactory.REVERT.getCommandId(), null, null,
        WorkbenchMessages.Workbench_revert, WorkbenchMessages.Workbench_revertToolTip, null);
  }

  public IContributionItem getSelectAllItem() {
    return getItem(ActionFactory.SELECT_ALL.getId(), ActionFactory.SELECT_ALL.getCommandId(), null,
        null, WorkbenchMessages.Workbench_selectAll, WorkbenchMessages.Workbench_selectAllToolTip,
        null);
  }

  public IContributionItem getTaskItem() {
    return getItem(IDEActionFactory.ADD_TASK.getId(), IDEActionFactory.ADD_TASK.getCommandId(),
        null, null, IDEWorkbenchMessages.Workbench_addTask,
        IDEWorkbenchMessages.Workbench_addTaskToolTip, null);
  }

  public IWorkbenchWindow getWindow() {
    return window;
  }

}
