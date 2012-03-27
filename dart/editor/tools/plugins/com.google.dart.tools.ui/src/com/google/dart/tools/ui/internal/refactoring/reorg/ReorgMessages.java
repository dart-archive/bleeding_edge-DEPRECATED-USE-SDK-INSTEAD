package com.google.dart.tools.ui.internal.refactoring.reorg;

import org.eclipse.osgi.util.NLS;

public final class ReorgMessages extends NLS {

  private static final String BUNDLE_NAME = ReorgMessages.class.getName();

  public static String CutAction_text;

  public static String PasteAction_projectName;
  public static String RenameInformationPopup_delayJobName;
  public static String RenameInformationPopup_EnterNewName;
  public static String RenameInformationPopup_menu;
  public static String RenameInformationPopup_OpenDialog;
  public static String RenameInformationPopup_preferences;
  public static String RenameInformationPopup_Preview;
  public static String RenameInformationPopup_RenameInWorkspace;
  public static String RenameInformationPopup_snap_bottom_right;
  public static String RenameInformationPopup_snap_over_left;
  public static String RenameInformationPopup_snap_over_right;
  public static String RenameInformationPopup_snap_under_left;
  public static String RenameInformationPopup_snap_under_right;
  public static String RenameInformationPopup_SnapTo;
  public static String RenameLinkedMode_error_saving_editor;
  public static String JdtMoveAction_update_references_singular;
  public static String JdtMoveAction_update_references_plural;
  /**
   * DO NOT REMOVE, used in a product.
   * 
   * @deprecated As of 3.6
   */
  @Deprecated
  public static String JdtMoveAction_update_references;

  public static String ReorgQueries_enterNewNameQuestion;

  public static String ReorgQueries_nameConflictMessage;
  public static String ReorgQueries_resourceWithThisNameAlreadyExists;
  public static String ReorgQueries_invalidNameMessage;
  public static String ReorgQueries_packagewithThatNameexistsMassage;
  public static String ReorgQueries_resourceExistsWithDifferentCaseMassage;
  public static String ReorgQueries_skip_all;
  /**
   * DO NOT REMOVE, used in a product, see https://bugs.eclipse.org/297392.
   * 
   * @deprecated As of 3.6
   */
  @Deprecated
  public static String ReorgGroup_paste;

  /**
   * DO NOT REMOVE, used in a product, see https://bugs.eclipse.org/297392 .
   * 
   * @deprecated As of 3.6
   */
  @Deprecated
  public static String ReorgGroup_delete;

  /**
   * DO NOT REMOVE, used in a product, see https://bugs.eclipse.org/297392 .
   * 
   * @deprecated As of 3.6
   */
  @Deprecated
  public static String CutSourceReferencesToClipboardAction_cut;

  public static String CopyToClipboardAction_text;

  public static String CopyToClipboardAction_description;
  public static String CopyToClipboardAction_2;
  public static String CopyToClipboardAction_3;
  public static String CopyToClipboardAction_4;
  public static String CopyToClipboardAction_5;
  public static String DeleteAction_3;
  public static String DeleteAction_4;
  public static String DeleteWorkingSet_Hide;
  public static String DeleteWorkingSet_removeorhideworkingset_single;
  public static String DeleteWorkingSet_removeorhideworkingset_multiple;
  public static String DeleteWorkingSet_Remove;
  public static String DeleteWorkingSet_single;
  public static String DeleteWorkingSet_multiple;
  public static String ReorgCopyAction_3;
  public static String ReorgCopyAction_4;
  public static String ReorgCopyWizard_1;
  public static String ReorgMoveAction_3;
  public static String ReorgMoveAction_4;
  public static String ReorgMoveWizard_3;
  public static String ReorgMoveWizard_textual_move;
  public static String ReorgMoveWizard_newPackage;
  public static String ReorgUserInputPage_choose_destination_single;
  public static String ReorgUserInputPage_choose_destination_multi;
  public static String RenameMethodUserInterfaceStarter_name;
  public static String RenameMethodUserInterfaceStarter_message;
  public static String PasteAction_4;
  public static String PasteAction_5;
  public static String PasteAction_change_name;
  public static String PasteAction_edit_name;
  public static String PasteAction_element_doesnot_exist;
  public static String PasteAction_invalid_destination;
  public static String PasteAction_name;
  public static String PasteAction_wrong_destination;
  public static String PasteAction_TextPaster_exists;
  public static String PasteAction_TextPaster_confirmOverwriting;
  public static String PasteAction_cannot_selection;
  public static String PasteAction_cannot_no_selection;
  public static String PasteAction_snippet_default_package_name;
  public static String PasteAction_snippet_default_type_name;
  static {
    NLS.initializeMessages(BUNDLE_NAME, ReorgMessages.class);
  }

  private ReorgMessages() {
    // Do not instantiate
  }
}
