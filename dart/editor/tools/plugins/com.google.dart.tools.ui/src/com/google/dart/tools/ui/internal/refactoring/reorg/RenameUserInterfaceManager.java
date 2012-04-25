/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.refactoring.reorg;

import com.google.dart.tools.internal.corext.refactoring.rename.RenameFieldProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameFunctionProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameFunctionTypeAliasProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameGlobalVariableProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameLocalVariableProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameMethodProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameTypeParameterProcessor;
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
    put(
        RenameGlobalVariableProcessor.class,
        RenameUserInterfaceStarter.class,
        RenameGlobalVariableWizard.class);
    put(
        RenameFunctionTypeAliasProcessor.class,
        RenameUserInterfaceStarter.class,
        RenameFunctionTypeAliasWizard.class);
    put(RenameTypeProcessor.class, RenameUserInterfaceStarter.class, RenameTypeWizard.class);
    put(
        RenameTypeParameterProcessor.class,
        RenameUserInterfaceStarter.class,
        RenameTypeParameterWizard.class);
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
