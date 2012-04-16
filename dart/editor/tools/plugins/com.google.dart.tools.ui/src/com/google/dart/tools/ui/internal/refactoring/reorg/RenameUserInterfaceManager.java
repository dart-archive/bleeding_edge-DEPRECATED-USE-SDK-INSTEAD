package com.google.dart.tools.ui.internal.refactoring.reorg;

import com.google.dart.tools.internal.corext.refactoring.rename.RenameFieldProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameFunctionProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameLocalVariableProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameMethodProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameTypeProcessor;
import com.google.dart.tools.ui.internal.refactoring.UserInterfaceManager;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameUserInterfaceManager extends UserInterfaceManager {
  private static final UserInterfaceManager fgInstance = new RenameUserInterfaceManager();

  public static UserInterfaceManager getDefault() {
    return fgInstance;
  }

  private RenameUserInterfaceManager() {
//    put(RenameDartProjectProcessor.class, RenameUserInterfaceStarter.class,
//        RenameDartProjectWizard.class);
//    put(RenameSourceFolderProcessor.class, RenameUserInterfaceStarter.class,
//        RenameSourceFolderWizard.class);
//    put(RenamePackageProcessor.class, RenameUserInterfaceStarter.class, RenamePackageWizard.class);
//    put(RenameCompilationUnitProcessor.class, RenameUserInterfaceStarter.class,
//        RenameCuWizard.class);
//    put(RenameTypeProcessor.class, RenameUserInterfaceStarter.class, RenameTypeWizard.class);
    put(RenameFunctionProcessor.class, RenameUserInterfaceStarter.class, RenameFunctionWizard.class);
    put(RenameTypeProcessor.class, RenameUserInterfaceStarter.class, RenameTypeWizard.class);
    put(RenameFieldProcessor.class, RenameUserInterfaceStarter.class, RenameFieldWizard.class);
    put(RenameMethodProcessor.class, RenameUserInterfaceStarter.class, RenameMethodWizard.class);
//    put(RenameEnumConstProcessor.class, RenameUserInterfaceStarter.class,
//        RenameEnumConstWizard.class);
//    put(RenameTypeParameterProcessor.class, RenameUserInterfaceStarter.class,
//        RenameTypeParameterWizard.class);
//    put(RenameNonVirtualMethodProcessor.class, RenameMethodUserInterfaceStarter.class,
//        RenameMethodWizard.class);
//    put(RenameVirtualMethodProcessor.class, RenameMethodUserInterfaceStarter.class,
//        RenameMethodWizard.class);
    put(
        RenameLocalVariableProcessor.class,
        RenameUserInterfaceStarter.class,
        RenameLocalVariableWizard.class);
  }
}
