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

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class DartSuperTypeAction extends DartLibrariesAction {

//  private static final int BUILD_PATH_PAGE_INDEX = 1;

  @Override
  public void run(IAction arg0) {
    Map<Object, Object> data = new HashMap<Object, Object>();
// TODO (pquitslund): if/when we have a build path property page, hook it up    
//    data.put(BuildPathsPropertyPage.DATA_PAGE_INDEX, new Integer(
//        BUILD_PATH_PAGE_INDEX));
    String ID = arg0.getId();
    String propertyPage = (String) PROPS_TO_IDS.get(ID);

    PreferencesUtil.createPropertyDialogOn(getShell(), project, propertyPage, null, data).open();
  }

}
