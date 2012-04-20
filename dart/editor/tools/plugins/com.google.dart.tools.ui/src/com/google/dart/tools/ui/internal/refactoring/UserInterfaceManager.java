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
package com.google.dart.tools.ui.internal.refactoring;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class UserInterfaceManager {

  private static class Tuple {
    private Class<? extends UserInterfaceStarter> starter;
    private Class<? extends RefactoringWizard> wizard;

    public Tuple(Class<? extends UserInterfaceStarter> s, Class<? extends RefactoringWizard> w) {
      starter = s;
      wizard = w;
    }
  }

  private Map<Class<? extends RefactoringProcessor>, Tuple> fMap = new HashMap<Class<? extends RefactoringProcessor>, Tuple>();

  public UserInterfaceStarter getStarter(Refactoring refactoring) {
    RefactoringProcessor processor = (RefactoringProcessor) refactoring.getAdapter(RefactoringProcessor.class);
    if (processor == null) {
      return null;
    }
    Tuple tuple = fMap.get(processor.getClass());
    if (tuple == null) {
      return null;
    }
    try {
      UserInterfaceStarter starter = tuple.starter.newInstance();
      Class<? extends RefactoringWizard> wizardClass = tuple.wizard;
      Constructor<? extends RefactoringWizard> constructor = wizardClass.getConstructor(new Class[] {Refactoring.class});
      RefactoringWizard wizard = constructor.newInstance(new Object[] {refactoring});
      starter.initialize(wizard);
      return starter;
    } catch (NoSuchMethodException e) {
      return null;
    } catch (IllegalAccessException e) {
      return null;
    } catch (InstantiationException e) {
      return null;
    } catch (IllegalArgumentException e) {
      return null;
    } catch (InvocationTargetException e) {
      return null;
    }
  }

  protected void put(Class<? extends RefactoringProcessor> processor,
      Class<? extends UserInterfaceStarter> starter, Class<? extends RefactoringWizard> wizard) {
    fMap.put(processor, new Tuple(starter, wizard));
  }
}
