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
package com.google.dart.tools.ui;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.internal.text.dart.ImportRewrite;

import java.util.regex.Pattern;

/**
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class CodeStyleConfiguration {

  @SuppressWarnings("unused")
  private static final Pattern SEMICOLON_PATTERN = Pattern.compile(";"); //$NON-NLS-1$

  /**
   * Returns a {@link ImportRewrite} using {@link ImportRewrite#create(CompilationUnit, boolean)}
   * and configures the rewriter with the settings as specified in the JDT UI preferences.
   * <p>
   * 
   * @param cu the compilation unit to create the rewriter on
   * @param restoreExistingImports specifies if the existing imports should be kept or removed.
   * @return the new rewriter configured with the settings as specified in the JDT UI preferences.
   * @throws DartModelException thrown when the compilation unit could not be accessed.
   * @see ImportRewrite#create(CompilationUnit, boolean)
   */
  public static ImportRewrite createImportRewrite(CompilationUnit cu, boolean restoreExistingImports)
      throws DartModelException {
    DartX.todo();
    return null;
//    return configureImportRewrite(ImportRewrite.create(cu,
//        restoreExistingImports));
  }

  /**
   * Returns a {@link ImportRewrite} using {@link ImportRewrite#create(DartUnit, boolean)} and
   * configures the rewriter with the settings as specified in the JDT UI preferences.
   * 
   * @param astRoot the AST root to create the rewriter on
   * @param restoreExistingImports specifies if the existing imports should be kept or removed.
   * @return the new rewriter configured with the settings as specified in the JDT UI preferences.
   * @see ImportRewrite#create(DartUnit, boolean)
   */
  public static ImportRewrite createImportRewrite(DartUnit astRoot, boolean restoreExistingImports) {
    DartX.todo();
    return null;
//    return configureImportRewrite(ImportRewrite.create(astRoot,
//        restoreExistingImports));
  }

  @SuppressWarnings("unused")
  private static ImportRewrite configureImportRewrite(ImportRewrite rewrite) {
    DartX.todo();
//    DartProject project = rewrite.getCompilationUnit().getDartProject();
//    String order = PreferenceConstants.getPreference(
//        PreferenceConstants.ORGIMPORTS_IMPORTORDER, project);
//    rewrite.setImportOrder(SEMICOLON_PATTERN.split(order, 0));
//
//    String thres = PreferenceConstants.getPreference(
//        PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD, project);
//    try {
//      int num = Integer.parseInt(thres);
//      if (num == 0)
//        num = 1;
//      rewrite.setOnDemandImportThreshold(num);
//    } catch (NumberFormatException e) {
//      // ignore
//    }
//    String thresStatic = PreferenceConstants.getPreference(
//        PreferenceConstants.ORGIMPORTS_STATIC_ONDEMANDTHRESHOLD, project);
//    try {
//      int num = Integer.parseInt(thresStatic);
//      if (num == 0)
//        num = 1;
//      rewrite.setStaticOnDemandImportThreshold(num);
//    } catch (NumberFormatException e) {
//      // ignore
//    }
    return rewrite;
  }

  private CodeStyleConfiguration() {
    // do not instantiate and subclass
  }

}
