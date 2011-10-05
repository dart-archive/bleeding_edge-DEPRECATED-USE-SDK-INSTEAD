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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.SignatureUtil;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import java.util.HashMap;
import java.util.Map;

/**
 * Proposal info that computes the javadoc lazily when it is queried.
 */
public final class MethodProposalInfo extends MemberProposalInfo {

  /**
   * Fallback in case we can't match a generic method. The fall back is only based on method name
   * and number of parameters.
   */
  private Method fFallbackMatch;

  /**
   * Creates a new proposal info.
   * 
   * @param project the java project to reference when resolving types
   * @param proposal the proposal to generate information for
   */
  public MethodProposalInfo(DartProject project, CompletionProposal proposal) {
    super(project, proposal);
  }

  /**
   * Resolves the member described by the receiver and returns it if found. Returns
   * <code>null</code> if no corresponding member can be found.
   * 
   * @return the resolved member or <code>null</code> if none is found
   * @throws DartModelException if accessing the java model fails
   */
  @Override
  protected TypeMember resolveMember() throws DartModelException {
    char[] declarationSignature = fProposal.getDeclarationSignature();
    Method func = null;
    if (declarationSignature != null) {
      String typeName = SignatureUtil.stripSignatureToFQN(String.valueOf(declarationSignature));
      String name = String.valueOf(fProposal.getName());
      String[] parameters = Signature.getParameterTypes(String.valueOf(fProposal.getSignature()));
      // search all the possible types until a match is found
      Type[] types = fJavaProject.findTypes(typeName);
      if (types != null && types.length > 0) {
        for (int i = 0; i < types.length && func == null; ++i) {
          Type type = types[i];
          if (type != null) {
            boolean isConstructor = fProposal.isConstructor();
            try {
              func = findMethod(name, parameters, isConstructor, type);
            } catch (DartModelException e) {
              // ignore, could not find method
            }
          }
        }
      } else {
//        ITypeRoot typeRoot = fJavaProject.findTypeRoot(typeName);
//        if (typeRoot != null) {
//          func = typeRoot.getFunction(name, parameters);
//        }
      }
    }
    return func;
  }

  /* adapted from DartModelUtil */

  /**
   * Returns the simple erased name for a given type signature, possibly replacing type variables.
   * 
   * @param signature the type signature
   * @param typeVariables the Map&lt;SimpleName, VariableName>
   * @return the simple erased name for signature
   */
  private String computeSimpleTypeName(String signature, Map<String, char[]> typeVariables) {
    // method equality uses erased types
    String erasure = signature;
    erasure = erasure.replaceAll("/", "."); //$NON-NLS-1$//$NON-NLS-2$
    String simpleName = Signature.getSimpleName(Signature.toString(erasure));
    char[] typeVar = typeVariables.get(simpleName);
    if (typeVar != null) {
      simpleName = String.valueOf(Signature.getSignatureSimpleName(typeVar));
    }
    return simpleName;
  }

  /**
   * The type and method signatures received in <code>CompletionProposals</code> of type
   * <code>FUNCTION_REF</code> contain concrete type bounds. When comparing parameters of the
   * signature with an <code>IFunction</code>, we have to make sure that we match the case where the
   * formal method declaration uses a type variable which in the signature is already substituted
   * with a concrete type (bound).
   * <p>
   * This method creates a map from type variable names to type signatures based on the position
   * they appear in the type declaration. The type signatures are filtered through
   * {@link SignatureUtil#getLowerBound(char[])}.
   * </p>
   * 
   * @param type the type to get the variables from
   * @return a map from type variables to concrete type signatures
   * @throws DartModelException if accessing the java model fails
   */
  private Map<String, char[]> computeTypeVariables(Type type) throws DartModelException {
    Map<String, char[]> map = new HashMap<String, char[]>();
    char[] declarationSignature = fProposal.getDeclarationSignature();
    if (declarationSignature == null) {
      // declaration signature
      return map;
    }

    return map;
  }

  /**
   * Finds a method by name. This searches for a method with a name and signature. Parameter types
   * are only compared by the simple name, no resolving for the fully qualified type name is done.
   * Constructors are only compared by parameters, not the name.
   * 
   * @param name The name of the method to find
   * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
   * @param isConstructor If the method is a constructor
   * @param methods The methods to search in
   * @param typeVariables a map from type variables to concretely used types
   * @return The found method or <code>null</code>, if nothing found
   */
  private Method findMethod(String name, String[] paramTypes, boolean isConstructor,
      Method[] methods, Map<String, char[]> typeVariables) throws DartModelException {
    for (int i = methods.length - 1; i >= 0; i--) {
      if (isSameMethodSignature(name, paramTypes, isConstructor, methods[i], typeVariables)) {
        return methods[i];
      }
    }
    return fFallbackMatch;
  }

  /**
   * Finds a method in a type. This searches for a method with the same name and signature.
   * Parameter types are only compared by the simple name, no resolving for the fully qualified type
   * name is done. Constructors are only compared by parameters, not the name.
   * 
   * @param name The name of the method to find
   * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
   * @param isConstructor If the method is a constructor
   * @return The first found method or <code>null</code>, if nothing found
   */
  private Method findMethod(String name, String[] paramTypes, boolean isConstructor, Type type)
      throws DartModelException {
    Map<String, char[]> typeVariables = computeTypeVariables(type);
    return findMethod(name, paramTypes, isConstructor, type.getMethods(), typeVariables);
  }

  /**
   * Tests if a method equals to the given signature. Parameter types are only compared by the
   * simple name, no resolving for the fully qualified type name is done. Constructors are only
   * compared by parameters, not the name.
   * 
   * @param name Name of the method
   * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
   * @param isConstructor Specifies if the method is a constructor
   * @param method the method to be compared with this info's method
   * @param typeVariables a map from type variables to types
   * @return Returns <code>true</code> if the method has the given name and parameter types and
   *         constructor state.
   */
  private boolean isSameMethodSignature(String name, String[] paramTypes, boolean isConstructor,
      Method method, Map<String, char[]> typeVariables) throws DartModelException {
    //TODO (pquitslund): since methods can't be overloaded in dart, mots of these parameters should be removed
    return name.equals(method.getElementName());

//    if (isConstructor || name.equals(method.getElementName())) {
//      if (isConstructor == method.isConstructor()) {
//        String[] otherParams = method.getParameterTypeNames(); // types may be type
//// variables
//        if (paramTypes.length == otherParams.length) {
//          fFallbackMatch = method;
//          String signature = method.getSignature();
//          String[] otherParamsFromSignature = Signature.getParameterTypes(signature); // types
//// are resolved / upper-bounded
//          // no need to check method type variables since these are
//          // not yet bound when proposing a method
//          for (int i = 0; i < paramTypes.length; i++) {
//            String ourParamName = computeSimpleTypeName(paramTypes[i],
//                typeVariables);
//            String otherParamName1 = computeSimpleTypeName(otherParams[i],
//                typeVariables);
//            String otherParamName2 = computeSimpleTypeName(
//                otherParamsFromSignature[i], typeVariables);
//
//            if (!ourParamName.equals(otherParamName1)
//                && !ourParamName.equals(otherParamName2)) {
//              return false;
//            }
//          }
//          return true;
//        }
//      }
//    }
//    return false;
  }
}
