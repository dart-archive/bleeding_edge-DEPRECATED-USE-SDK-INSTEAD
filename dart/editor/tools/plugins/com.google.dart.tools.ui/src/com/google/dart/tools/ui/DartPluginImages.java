/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.osgi.framework.Bundle;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Bundle of most images used by the Java plug-in.
 */
public class DartPluginImages {

  private static final class CachedImageDescriptor extends ImageDescriptor {
    private ImageDescriptor fDescriptor;
    private ImageData fData;

    public CachedImageDescriptor(ImageDescriptor descriptor) {
      fDescriptor = descriptor;
    }

    @Override
    public ImageData getImageData() {
      if (fData == null) {
        fData = fDescriptor.getImageData();
      }
      return fData;
    }
  }

  public static final IPath ICONS_PATH = new Path("$nl$/icons/full"); //$NON-NLS-1$
  private static final String NAME_PREFIX = "com.google.dart.tools.ui."; //$NON-NLS-1$

  private static final int NAME_PREFIX_LENGTH = NAME_PREFIX.length();
  // The plug-in registry
  private static ImageRegistry fgImageRegistry = null;

  private static HashMap<String, ImageDescriptor> fgAvoidSWTErrorMap = null;
  private static final String T_DART = "dart16"; //$NON-NLS-1$
  private static final String T_OBJ = "obj16"; //$NON-NLS-1$
  private static final String T_OVR = "ovr16"; //$NON-NLS-1$
  private static final String T_WIZBAN = "wizban"; //$NON-NLS-1$
  private static final String T_ELCL = "elcl16"; //$NON-NLS-1$
  private static final String T_DLCL = "dlcl16"; //$NON-NLS-1$
  private static final String T_ETOOL = "etool16"; //$NON-NLS-1$

  private static final String T_EVIEW = "eview16"; //$NON-NLS-1$
  /*
   * Keys for images available from the Java-UI plug-in image registry.
   */
  public static final String IMG_MISC_PUBLIC = NAME_PREFIX + "methpub_obj.gif"; //$NON-NLS-1$
  public static final String IMG_MISC_PROTECTED = NAME_PREFIX + "methpro_obj.gif"; //$NON-NLS-1$
  public static final String IMG_MISC_PRIVATE = NAME_PREFIX + "methpri_obj.gif"; //$NON-NLS-1$

  public static final String IMG_MISC_DEFAULT = NAME_PREFIX + "methdef_obj.gif"; //$NON-NLS-1$
  public static final String IMG_FIELD_PUBLIC = NAME_PREFIX + "field_public_obj.gif"; //$NON-NLS-1$
  public static final String IMG_FIELD_PROTECTED = NAME_PREFIX + "field_protected_obj.gif"; //$NON-NLS-1$
  public static final String IMG_FIELD_PRIVATE = NAME_PREFIX + "field_private_obj.gif"; //$NON-NLS-1$

  public static final String IMG_FIELD_DEFAULT = NAME_PREFIX + "field_default_obj.gif"; //$NON-NLS-1$
  public static final String IMG_ELCL_VIEW_MENU = NAME_PREFIX + T_ELCL + "view_menu.gif"; //$NON-NLS-1$

  public static final String IMG_DLCL_VIEW_MENU = NAME_PREFIX + T_DLCL + "view_menu.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_GHOST = NAME_PREFIX + "ghost.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_SEARCH_TSK = NAME_PREFIX + "search_tsk.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_PACKDECL = NAME_PREFIX + "packd_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_IMPDECL = NAME_PREFIX + "imp_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_IMPCONT = NAME_PREFIX + "impc_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_JSEARCH = NAME_PREFIX + "jsearch_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_SEARCH_DECL = NAME_PREFIX + "search_decl_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_SEARCH_REF = NAME_PREFIX + "search_ref_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_CLASS = NAME_PREFIX + "class_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_CLASSALT = NAME_PREFIX + "classfo_obj.gif"; //$NON-NLS-1$	

  public static final String IMG_OBJS_CLASS_DEFAULT = NAME_PREFIX + "class_default_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_INNER_CLASS_PUBLIC = NAME_PREFIX
      + "innerclass_public_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_INNER_CLASS_DEFAULT = NAME_PREFIX
      + "innerclass_default_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_INNER_CLASS_PROTECTED = NAME_PREFIX
      + "innerclass_protected_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_INNER_CLASS_PRIVATE = NAME_PREFIX
      + "innerclass_private_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_INTERFACE = NAME_PREFIX + "int_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_INTERFACEALT = NAME_PREFIX + "intf_obj.gif"; //$NON-NLS-1$	

  public static final String IMG_OBJS_INTERFACE_DEFAULT = NAME_PREFIX + "int_default_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_INNER_INTERFACE_PUBLIC = NAME_PREFIX
      + "innerinterface_public_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_INNER_INTERFACE_DEFAULT = NAME_PREFIX
      + "innerinterface_default_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_INNER_INTERFACE_PROTECTED = NAME_PREFIX
      + "innerinterface_protected_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_INNER_INTERFACE_PRIVATE = NAME_PREFIX
      + "innerinterface_private_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_ANNOTATION = NAME_PREFIX + "annotation_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_ANNOTATION_DEFAULT = NAME_PREFIX
      + "annotation_default_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_ANNOTATION_PROTECTED = NAME_PREFIX
      + "annotation_protected_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_ANNOTATION_PRIVATE = NAME_PREFIX
      + "annotation_private_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_ANNOTATION_ALT = NAME_PREFIX + "annotation_alt_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_ENUM = NAME_PREFIX + "enum_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_ENUM_DEFAULT = NAME_PREFIX + "enum_default_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_ENUM_PROTECTED = NAME_PREFIX + "enum_protected_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_ENUM_PRIVATE = NAME_PREFIX + "enum_private_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_ENUM_ALT = NAME_PREFIX + "enum_alt_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_FUNCTIONTYPE = NAME_PREFIX + "functiontype_obj.png"; //$NON-NLS-1$
  public static final String IMG_OBJS_FUNCTIONTYPE_PRIVATE = NAME_PREFIX
      + "functiontype_private_obj.png"; //$NON-NLS-1$
  public static final String IMG_OBJS_CUNIT = NAME_PREFIX + "jcu_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_CUNIT_RESOURCE = NAME_PREFIX + "jcu_resource_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_CFILE = NAME_PREFIX + "classf_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_CFILECLASS = NAME_PREFIX + "class_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_CFILEINT = NAME_PREFIX + "int_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_LOGICAL_PACKAGE = NAME_PREFIX + "logical_package_obj.gif";//$NON-NLS-1$
  public static final String IMG_OBJS_EMPTY_LOGICAL_PACKAGE = NAME_PREFIX
      + "empty_logical_package_obj.gif";//$NON-NLS-1$
  public static final String IMG_OBJS_PACKAGE = NAME_PREFIX + "package_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_EMPTY_PACK_RESOURCE = NAME_PREFIX + "empty_pack_fldr_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_EMPTY_PACKAGE = NAME_PREFIX + "empty_pack_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_PACKFRAG_ROOT = NAME_PREFIX + "packagefolder_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_JAR = NAME_PREFIX + "jar_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_EXTJAR = NAME_PREFIX + "jar_l_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_FILE = NAME_PREFIX + "file_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_JAR_WSRC = NAME_PREFIX + "jar_src_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_EXTJAR_WSRC = NAME_PREFIX + "jar_lsrc_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_ENV_VAR = NAME_PREFIX + "envvar_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_JAVA_MODEL = NAME_PREFIX + "java_model_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_UNKNOWN = NAME_PREFIX + "unknown_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_LOCAL_VARIABLE = NAME_PREFIX + "localvariable_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_PROJECT_SETTINGS = NAME_PREFIX + "settings_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_LIBRARY = NAME_PREFIX + "library_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_JAVADOCTAG = NAME_PREFIX + "jdoc_tag_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_HTMLTAG = NAME_PREFIX + "html_tag_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_TEMPLATE = NAME_PREFIX + "template_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_EXCEPTION = NAME_PREFIX + "jexception_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_ERROR = NAME_PREFIX + "jrtexception_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_BREAKPOINT_INSTALLED = NAME_PREFIX + "brkpi_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_QUICK_ASSIST = NAME_PREFIX + "quickassist_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_FIXABLE_PROBLEM = NAME_PREFIX + "quickfix_warning_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_FIXABLE_ERROR = NAME_PREFIX + "quickfix_error_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_ACCESSRULES_ATTRIB = NAME_PREFIX
      + "access_restriction_attrib.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_REFACTORING_FATAL = NAME_PREFIX + "fatalerror_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_REFACTORING_ERROR = NAME_PREFIX + "error_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_REFACTORING_WARNING = NAME_PREFIX + "warning_obj.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_REFACTORING_INFO = NAME_PREFIX + "info_obj.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_NLS_TRANSLATE = NAME_PREFIX + "translate.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_NLS_NEVER_TRANSLATE = NAME_PREFIX + "never_translate.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_NLS_SKIP = NAME_PREFIX + "skip.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_SEARCH_READACCESS = NAME_PREFIX + "occ_read.gif"; //$NON-NLS-1$
  public static final String IMG_OBJS_SEARCH_WRITEACCESS = NAME_PREFIX + "occ_write.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_SEARCH_OCCURRENCE = NAME_PREFIX + "occ_match.gif"; //$NON-NLS-1$

  public static final String IMG_OBJS_HELP = NAME_PREFIX + "help.gif"; //$NON-NLS-1$

  /*
   * Set of predefined Angular Image Descriptors.
   */
  public static final ImageDescriptor DESC_ANGULAR_BLUE = createUnManaged(
      T_DART,
      "angular_16_blue.png");
  public static final ImageDescriptor DESC_ANGULAR_RED = createUnManaged(
      T_DART,
      "angular_16_red.png");
  public static final ImageDescriptor DESC_ANGULAR_YELLOW = createUnManaged(
      T_DART,
      "angular_16_yellow.png");

  public static final ImageDescriptor DESC_BLANK = createUnManaged(T_DART, "blank.png"); //$NON-NLS-1$

  /*
   * Set of predefined Image Descriptors.
   */
  public static final ImageDescriptor DESC_DART_IMPORT_PREFIX = createUnManaged(
      T_DART,
      "import_prefix.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_LIB_FILE = createUnManaged(
      T_DART,
      "package_obj.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_COMP_UNIT = createUnManaged(T_DART, "dart.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_HTML_FILE = createUnManaged(T_DART, "html.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_CLASS_TYPE_ALIAS = createUnManaged(
      T_DART,
      "typevariable_obj.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_DART_FUNCTIONTYPE_PUBLIC = createUnManaged(
      T_DART,
      "functiontype_public.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_FUNCTIONTYPE_PRIVATE = createUnManaged(
      T_DART,
      "functiontype_private.png"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_DART_CLASS_PUBLIC = createUnManaged(
      T_DART,
      "class_public.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_CLASS_PRIVATE = createUnManaged(
      T_DART,
      "class_private.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_INTERFACE = createUnManaged(
      T_DART,
      "interface_public.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_INNER_INTERFACE_PRIVATE = createUnManaged(
      T_DART,
      "interface_private.png"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_DART_LABEL = createUnManaged(T_DART, "label.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_LOCAL_VARIABLE = createUnManaged(
      T_DART,
      "localvariable_obj.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_METHOD_PUBLIC = createUnManaged(
      T_DART,
      "method_public.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_FIELD_PUBLIC = createUnManaged(
      T_DART,
      "field_public.png"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_DART_METHOD_PRIVATE = createUnManaged(
      T_DART,
      "method_private.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_FIELD_PRIVATE = createUnManaged(
      T_DART,
      "field_private.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_KEYWORD = DESC_BLANK;

  public static final ImageDescriptor DESC_DART_TEST_CASE = createUnManaged(T_DART, "test_case.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DART_TEST_GROUP = createUnManaged(
      T_DART,
      "test_group.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_VIEW_ERRORWARNING_TAB = createUnManaged(
      T_EVIEW,
      "errorwarning_tab.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_VIEW_CLASSFILEGENERATION_TAB = createUnManaged(
      T_EVIEW,
      "classfilegeneration_tab.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_VIEW_JDKCOMPLIANCE_TAB = createUnManaged(
      T_EVIEW,
      "jdkcompliance_tab.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_ELCL_FILTER = createUnManaged(T_ELCL, "filter_ps.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_DLCL_FILTER = createUnManaged(T_DLCL, "filter_ps.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_ELCL_CODE_ASSIST = createUnManaged(
      T_ELCL,
      "metharg_obj.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_DLCL_CODE_ASSIST = createUnManaged(
      T_DLCL,
      "metharg_obj.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_ELCL_VIEW_MENU = createManaged(
      T_ELCL,
      "view_menu.gif", IMG_ELCL_VIEW_MENU); //$NON-NLS-1$

  public static final ImageDescriptor DESC_DLCL_VIEW_MENU = createManaged(
      T_DLCL,
      "view_menu.gif", IMG_DLCL_VIEW_MENU); //$NON-NLS-1$
  public static final ImageDescriptor DESC_MISC_PUBLIC = createManagedFromKey(
      T_OBJ,
      IMG_MISC_PUBLIC);
  public static final ImageDescriptor DESC_MISC_PROTECTED = createManagedFromKey(
      T_OBJ,
      IMG_MISC_PROTECTED);
  public static final ImageDescriptor DESC_MISC_PRIVATE = createManagedFromKey(
      T_OBJ,
      IMG_MISC_PRIVATE);

  public static final ImageDescriptor DESC_MISC_DEFAULT = createManagedFromKey(
      T_OBJ,
      IMG_MISC_DEFAULT);
  public static final ImageDescriptor DESC_FIELD_PUBLIC = createManagedFromKey(
      T_OBJ,
      IMG_FIELD_PUBLIC);
  public static final ImageDescriptor DESC_FIELD_PROTECTED = createManagedFromKey(
      T_OBJ,
      IMG_FIELD_PROTECTED);
  public static final ImageDescriptor DESC_FIELD_PRIVATE = createManagedFromKey(
      T_OBJ,
      IMG_FIELD_PRIVATE);

  public static final ImageDescriptor DESC_FIELD_DEFAULT = createManagedFromKey(
      T_OBJ,
      IMG_FIELD_DEFAULT);
  public static final ImageDescriptor DESC_MENU_SHIFT_RIGHT = createUnManaged(
      T_ETOOL,
      "shift_r_edit.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_MENU_SHIFT_LEFT = createUnManaged(
      T_ETOOL,
      "shift_l_edit.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OBJS_GHOST = createManagedFromKey(T_OBJ, IMG_OBJS_GHOST);
  public static final ImageDescriptor DESC_OBJS_PACKDECL = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_PACKDECL);
  public static final ImageDescriptor DESC_OBJS_IMPDECL = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_IMPDECL);
  public static final ImageDescriptor DESC_OBJS_IMPCONT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_IMPCONT);
  public static final ImageDescriptor DESC_OBJS_JSEARCH = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_JSEARCH);
  public static final ImageDescriptor DESC_OBJS_SEARCH_DECL = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_SEARCH_DECL);
  public static final ImageDescriptor DESC_OBJS_SEARCH_REF = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_SEARCH_REF);
  public static final ImageDescriptor DESC_OBJS_FUNCTIONTYPE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_FUNCTIONTYPE);
  public static final ImageDescriptor DESC_OBJS_FUNCTIONTYPE_PRIVATE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_FUNCTIONTYPE_PRIVATE);
  public static final ImageDescriptor DESC_OBJS_CUNIT = createManagedFromKey(T_OBJ, IMG_OBJS_CUNIT);
  public static final ImageDescriptor DESC_OBJS_CUNIT_RESOURCE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_CUNIT_RESOURCE);
  public static final ImageDescriptor DESC_OBJS_CFILE = createManagedFromKey(T_OBJ, IMG_OBJS_CFILE);
  public static final ImageDescriptor DESC_OBJS_CFILECLASS = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_CFILECLASS);
  public static final ImageDescriptor DESC_ELCL_CLEAR = createUnManaged(T_ELCL, "clear_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_CLEAR = createUnManaged(T_DLCL, "clear_co.gif"); //$NON-NLS-1$ 
  public static final ImageDescriptor DESC_OBJS_CFILEINT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_CFILEINT);
  public static final ImageDescriptor DESC_OBJS_PACKAGE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_PACKAGE);
  public static final ImageDescriptor DESC_OBJS_EMPTY_LOGICAL_PACKAGE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_EMPTY_LOGICAL_PACKAGE);
  public static final ImageDescriptor DESC_OBJS_LOGICAL_PACKAGE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_LOGICAL_PACKAGE);
  public static final ImageDescriptor DESC_OBJS_EMPTY_PACKAGE_RESOURCES = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_EMPTY_PACK_RESOURCE);
  public static final ImageDescriptor DESC_OBJS_EMPTY_PACKAGE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_EMPTY_PACKAGE);
  public static final ImageDescriptor DESC_OBJS_PACKFRAG_ROOT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_PACKFRAG_ROOT);

  public static final ImageDescriptor DESC_OBJS_PROJECT_SETTINGS = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_PROJECT_SETTINGS);

  public static final ImageDescriptor DESC_OBJS_JAVA_MODEL = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_JAVA_MODEL);
  public static final ImageDescriptor DESC_OBJS_CLASS = createManagedFromKey(T_OBJ, IMG_OBJS_CLASS);

  public static final ImageDescriptor DESC_OBJS_CLASS_DEFAULT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_CLASS_DEFAULT);
  public static final ImageDescriptor DESC_OBJS_INNER_CLASS_PUBLIC = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_INNER_CLASS_PUBLIC);
  public static final ImageDescriptor DESC_OBJS_INNER_CLASS_DEFAULT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_INNER_CLASS_DEFAULT);
  public static final ImageDescriptor DESC_OBJS_INNER_CLASS_PROTECTED = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_INNER_CLASS_PROTECTED);

  public static final ImageDescriptor DESC_OBJS_INNER_CLASS_PRIVATE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_INNER_CLASS_PRIVATE);

  public static final ImageDescriptor DESC_OBJS_CLASSALT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_CLASSALT);
  public static final ImageDescriptor DESC_OBJS_INTERFACE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_INTERFACE);

  public static final ImageDescriptor DESC_OBJS_INTERFACE_DEFAULT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_INTERFACE_DEFAULT);
  public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_PUBLIC = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_INNER_INTERFACE_PUBLIC);
  public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_DEFAULT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_INNER_INTERFACE_DEFAULT);
  public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_PROTECTED = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_INNER_INTERFACE_PROTECTED);

  public static final ImageDescriptor DESC_OBJS_INNER_INTERFACE_PRIVATE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_INNER_INTERFACE_PRIVATE);

  public static final ImageDescriptor DESC_OBJS_INTERFACEALT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_INTERFACEALT);
  public static final ImageDescriptor DESC_OBJS_ANNOTATION = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_ANNOTATION);
  public static final ImageDescriptor DESC_OBJS_ANNOTATION_DEFAULT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_ANNOTATION_DEFAULT);
  public static final ImageDescriptor DESC_OBJS_ANNOTATION_PROTECTED = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_ANNOTATION_PROTECTED);
  public static final ImageDescriptor DESC_OBJS_ANNOTATION_PRIVATE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_ANNOTATION_PRIVATE);

  public static final ImageDescriptor DESC_OBJS_ANNOTATION_ALT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_ANNOTATION_ALT);
  public static final ImageDescriptor DESC_OBJS_ENUM = createManagedFromKey(T_OBJ, IMG_OBJS_ENUM);
  public static final ImageDescriptor DESC_OBJS_ENUM_DEFAULT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_ENUM_DEFAULT);
  public static final ImageDescriptor DESC_OBJS_ENUM_PROTECTED = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_ENUM_PROTECTED);
  public static final ImageDescriptor DESC_OBJS_ENUM_PRIVATE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_ENUM_PRIVATE);

  public static final ImageDescriptor DESC_OBJS_ENUM_ALT = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_ENUM_ALT);
  public static final ImageDescriptor DESC_OBJS_JAR = createManagedFromKey(T_OBJ, IMG_OBJS_JAR);
  public static final ImageDescriptor DESC_OBJS_EXTJAR = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_EXTJAR);
  public static final ImageDescriptor DESC_OBJS_FILE = createManagedFromKey(T_OBJ, IMG_OBJS_FILE);
  public static final ImageDescriptor DESC_OBJS_JAR_WSRC = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_JAR_WSRC);
  public static final ImageDescriptor DESC_OBJS_EXTJAR_WSRC = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_EXTJAR_WSRC);

  public static final ImageDescriptor DESC_OBJS_ENV_VAR = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_ENV_VAR);

  public static final ImageDescriptor DESC_OBJS_LIBRARY = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_LIBRARY);
  public static final ImageDescriptor DESC_OBJS_JAVADOCTAG = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_JAVADOCTAG);

  public static final ImageDescriptor DESC_OBJS_HTMLTAG = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_HTMLTAG);

  public static final ImageDescriptor DESC_OBJS_TEMPLATE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_TEMPLATE);
  public static final ImageDescriptor DESC_OBJS_EXCEPTION = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_EXCEPTION);
  public static final ImageDescriptor DESC_OBJS_BREAKPOINT_INSTALLED = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_BREAKPOINT_INSTALLED);
  public static final ImageDescriptor DESC_OBJS_ERROR = createManagedFromKey(T_OBJ, IMG_OBJS_ERROR);
  public static final ImageDescriptor DESC_OBJS_QUICK_ASSIST = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_QUICK_ASSIST);
  public static final ImageDescriptor DESC_OBJS_FIXABLE_PROBLEM = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_FIXABLE_PROBLEM);

  // public static final ImageDescriptor DESC_OBJS_SNIPPET_EVALUATING=
  // createManaged(T_OBJ, IMG_OBJS_SNIPPET_EVALUATING);

  public static final ImageDescriptor DESC_OBJS_FIXABLE_ERROR = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_FIXABLE_ERROR);

  public static final ImageDescriptor DESC_OBJS_DEFAULT_CHANGE = createUnManaged(
      T_OBJ,
      "change.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OBJS_EXCLUSION_FILTER_ATTRIB = createUnManaged(
      T_OBJ,
      "exclusion_filter_attrib.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OBJS_INCLUSION_FILTER_ATTRIB = createUnManaged(
      T_OBJ,
      "inclusion_filter_attrib.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OBJS_OUTPUT_FOLDER_ATTRIB = createUnManaged(
      T_OBJ,
      "output_folder_attrib.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OBJS_SOURCE_ATTACH_ATTRIB = createUnManaged(
      T_OBJ,
      "source_attach_attrib.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_OBJS_JAVADOC_LOCATION_ATTRIB = createUnManaged(
      T_OBJ,
      "javadoc_location_attrib.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_OBJS_ACCESSRULES_ATTRIB = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_ACCESSRULES_ATTRIB);
  public static final ImageDescriptor DESC_OBJS_REFACTORING_FATAL = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_REFACTORING_FATAL);
  public static final ImageDescriptor DESC_OBJS_REFACTORING_ERROR = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_REFACTORING_ERROR);
  public static final ImageDescriptor DESC_OBJS_REFACTORING_WARNING = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_REFACTORING_WARNING);

  public static final ImageDescriptor DESC_OBJS_REFACTORING_INFO = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_REFACTORING_INFO);
  public static final ImageDescriptor DESC_OBJS_NLS_TRANSLATE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_NLS_TRANSLATE);
  public static final ImageDescriptor DESC_OBJS_NLS_NEVER_TRANSLATE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_NLS_NEVER_TRANSLATE);

  public static final ImageDescriptor DESC_OBJS_NLS_SKIP = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_NLS_SKIP);

  public static final ImageDescriptor DESC_OBJS_UNKNOWN = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_UNKNOWN);

  public static final ImageDescriptor DESC_OBJS_TYPE_SEPARATOR = createUnManaged(
      T_OBJ,
      "type_separator.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OBJS_SEARCH_READACCESS = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_SEARCH_READACCESS);
  public static final ImageDescriptor DESC_OBJS_SEARCH_WRITEACCESS = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_SEARCH_WRITEACCESS);

  public static final ImageDescriptor DESC_OBJS_SEARCH_OCCURRENCE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_SEARCH_OCCURRENCE);

  public static final ImageDescriptor DESC_OBJS_LOCAL_VARIABLE = createManagedFromKey(
      T_OBJ,
      IMG_OBJS_LOCAL_VARIABLE);
  public static final ImageDescriptor DESC_OBJS_TOP_LEVEL_VARIABLE = DESC_DART_CLASS_TYPE_ALIAS;

  public static final ImageDescriptor DESC_OBJS_HELP = createManagedFromKey(T_ELCL, IMG_OBJS_HELP);
  public static final ImageDescriptor DESC_ELCL_ADD_TO_BP = createUnManaged(
      T_ELCL,
      "add_to_buildpath.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_ELCL_REMOVE_FROM_BP = createUnManaged(
      T_ELCL,
      "remove_from_buildpath.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_ELCL_INCLUSION = createUnManaged(
      T_ELCL,
      "inclusion_filter_attrib.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_ELCL_EXCLUSION = createUnManaged(
      T_ELCL,
      "exclusion_filter_attrib.gif"); //$NON-NLS-1$
  //    public static final ImageDescriptor DESC_ELCL_INCLUSION_UNDO= createUnManaged(T_ELCL, "inclusion_filter_attrib_undo.gif"); //$NON-NLS-1$
  //    public static final ImageDescriptor DESC_ELCL_EXCLUSION_UNDO= createUnManaged(T_ELCL, "exclusion_filter_attrib_undo.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_ADD_TO_BP = createUnManaged(
      T_DLCL,
      "add_to_buildpath.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_REMOVE_FROM_BP = createUnManaged(
      T_DLCL,
      "remove_from_buildpath.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_INCLUSION = createUnManaged(
      T_DLCL,
      "inclusion_filter_attrib.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_EXCLUSION = createUnManaged(
      T_DLCL,
      "exclusion_filter_attrib.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_DLCL_OUTPUT_FOLDER_ATTRIB = createUnManaged(
      T_DLCL,
      "output_folder_attrib.gif"); //$NON-NLS-1$
  //    public static final ImageDescriptor DESC_DLCL_INCLUSION_UNDO= createUnManaged(T_DLCL, "inclusion_filter_attrib_undo.gif"); //$NON-NLS-1$
  //    public static final ImageDescriptor DESC_DLCL_EXCLUSION_UNDO= createUnManaged(T_DLCL, "exclusion_filter_attrib_undo.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_ADD_LINKED_SOURCE_TO_BUILDPATH = createUnManaged(
      T_DLCL,
      "add_linked_source_to_buildpath.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_ELCL_ADD_LINKED_SOURCE_TO_BUILDPATH = createUnManaged(
      T_ELCL,
      "add_linked_source_to_buildpath.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_CONFIGURE_BUILDPATH = createUnManaged(
      T_DLCL,
      "configure_build_path.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_ELCL_CONFIGURE_BUILDPATH = createUnManaged(
      T_ELCL,
      "configure_build_path.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_CONFIGURE_BUILDPATH_FILTERS = createUnManaged(
      T_DLCL,
      "configure_buildpath_filters.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_ELCL_CONFIGURE_BUILDPATH_FILTERS = createUnManaged(
      T_ELCL,
      "configure_buildpath_filters.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_CONFIGURE_OUTPUT_FOLDER = createUnManaged(
      T_DLCL,
      "configure_output_folder.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_ELCL_CONFIGURE_OUTPUT_FOLDER = createUnManaged(
      T_ELCL,
      "configure_output_folder.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_EXCLUDE_FROM_BUILDPATH = createUnManaged(
      T_DLCL,
      "exclude_from_buildpath.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_ELCL_EXCLUDE_FROM_BUILDPATH = createUnManaged(
      T_ELCL,
      "exclude_from_buildpath.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_INCLUDE_ON_BUILDPATH = createUnManaged(
      T_DLCL,
      "include_on_buildpath.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_ELCL_INCLUDE_ON_BUILDPATH = createUnManaged(
      T_ELCL,
      "include_on_buildpath.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_ADD_AS_SOURCE_FOLDER = createUnManaged(
      T_DLCL,
      "add_as_source_folder.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_ELCL_ADD_AS_SOURCE_FOLDER = createUnManaged(
      T_ELCL,
      "add_as_source_folder.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_REMOVE_AS_SOURCE_FOLDER = createUnManaged(
      T_DLCL,
      "remove_as_source_folder.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_ELCL_REMOVE_AS_SOURCE_FOLDER = createUnManaged(
      T_ELCL,
      "remove_as_source_folder.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_DLCL_COPY_QUALIFIED_NAME = createUnManaged(
      T_DLCL,
      "cpyqual_menu.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_ELCL_COPY_QUALIFIED_NAME = createUnManaged(
      T_ELCL,
      "cpyqual_menu.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OBJ_OVERRIDES = createUnManaged(T_OBJ, "over_co.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_OBJ_IMPLEMENTS = createUnManaged(T_OBJ, "implm_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_STATIC = createUnManagedCached(
      T_OVR,
      "static_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_FINAL = createUnManagedCached(T_OVR, "final_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_ABSTRACT = createUnManagedCached(
      T_OVR,
      "abstract_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_SYNCH = createUnManagedCached(T_OVR, "synch_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_VOLATILE = createUnManagedCached(
      T_OVR,
      "volatile_co.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_OVR_GETTER = createUnManagedCached(T_OVR, "exit_ovr.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_SETTER = createUnManagedCached(
      T_OVR,
      "entry_ovr.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_OVR_TRANSIENT = createUnManagedCached(
      T_OVR,
      "transient_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_RUN = createUnManagedCached(T_OVR, "run_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_WARNING = createUnManagedCached(
      T_OVR,
      "warning_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_ERROR = createUnManagedCached(T_OVR, "error_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_OVERRIDES = createUnManagedCached(
      T_OVR,
      "over_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_IMPLEMENTS = createUnManagedCached(
      T_OVR,
      "implm_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_LINKED = createUnManagedCached(T_OVR, "link_ovr.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_SYNCH_AND_OVERRIDES = createUnManagedCached(
      T_OVR,
      "sync_over.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_SYNCH_AND_IMPLEMENTS = createUnManagedCached(
      T_OVR,
      "sync_impl.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_CONSTRUCTOR = createUnManagedCached(
      T_OVR,
      "constr_ovr.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_DEPRECATED = createUnManagedCached(
      T_OVR,
      "deprecated.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_FOCUS = createUnManagedCached(T_OVR, "focus_ovr.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_ANNOTATION = createUnManagedCached(
      T_OVR,
      "annotation_tsk.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_ENUM = createUnManagedCached(T_OVR, "enum_tsk.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_INTERFACE = createUnManagedCached(
      T_OVR,
      "interface_tsk.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_OVR_CLASS = createUnManagedCached(T_OVR, "class_tsk.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_OVR_ABSTRACT_CLASS = createUnManagedCached(
      T_OVR,
      "class_abs_tsk.gif"); //$NON-NLS-1$
  // Call Hierarchy
  public static final ImageDescriptor DESC_OVR_RECURSIVE = createUnManaged(
      T_OVR,
      "recursive_co.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_OVR_MAX_LEVEL = createUnManaged(T_OVR, "maxlevel_co.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_NEWCLASS = createUnManaged(
      T_WIZBAN,
      "newclass_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_NEWJSFILE = createUnManaged(
      T_WIZBAN,
      "newjscriptfile_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_NEWINT = createUnManaged(
      T_WIZBAN,
      "newint_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_NEWENUM = createUnManaged(
      T_WIZBAN,
      "newenum_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_NEWANNOT = createUnManaged(
      T_WIZBAN,
      "newannotation_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_NEWJPRJ = createUnManaged(
      T_WIZBAN,
      "newjprj_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_NEWSRCFOLDR = createUnManaged(
      T_WIZBAN,
      "newsrcfldr_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_NEWMETH = createUnManaged(
      T_WIZBAN,
      "newmeth_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_NEWPACK = createUnManaged(
      T_WIZBAN,
      "newpack_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_NEWSCRAPPAGE = createUnManaged(
      T_WIZBAN,
      "newsbook_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_JAVA_LAUNCH = createUnManaged(
      T_WIZBAN,
      "java_app_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_JAVA_ATTACH = createUnManaged(
      T_WIZBAN,
      "java_attach_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_REFACTOR = createUnManaged(
      T_WIZBAN,
      "refactor_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_REFACTOR_FIELD = createUnManaged(
      T_WIZBAN,
      "fieldrefact_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_REFACTOR_METHOD = createUnManaged(
      T_WIZBAN,
      "methrefact_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_REFACTOR_TYPE = createUnManaged(
      T_WIZBAN,
      "typerefact_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_REFACTOR_PACKAGE = createUnManaged(
      T_WIZBAN,
      "packrefact_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_REFACTOR_CODE = createUnManaged(
      T_WIZBAN,
      "coderefact_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_REFACTOR_CU = createUnManaged(
      T_WIZBAN,
      "compunitrefact_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_REFACTOR_PULL_UP = createUnManaged(
      T_WIZBAN,
      "pullup_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_REFACTOR_FIX_DEPRECATION = createUnManaged(
      T_WIZBAN,
      "fixdepr_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_JAR_PACKAGER = createUnManaged(
      T_WIZBAN,
      "jar_pack_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_REFACTOR_EXTRACT_SUPERTYPE = createUnManaged(
      T_WIZBAN,
      "extractsupertype_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_REPLACE_JAR = createUnManaged(
      T_WIZBAN,
      "replacejar_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_JAVA_WORKINGSET = createUnManaged(
      T_WIZBAN,
      "java_workingset_wiz.png");//$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_EXPORT_JAVADOC = createUnManaged(
      T_WIZBAN,
      "export_javadoc_wiz.png");//$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_EXTERNALIZE_STRINGS = createUnManaged(
      T_WIZBAN,
      "extstr_wiz.png");//$NON-NLS-1$
  public static final ImageDescriptor DESC_WIZBAN_ADD_LIBRARY = createUnManaged(
      T_WIZBAN,
      "addlibrary_wiz.png");//$NON-NLS-1$

  public static final ImageDescriptor DESC_WIZBAN_CLEAN_UP = createUnManaged(
      T_WIZBAN,
      "cleanup_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_TOOL_SHOW_EMPTY_PKG = createUnManaged(
      T_ETOOL,
      "show_empty_pkg.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_TOOL_NEW_FILE = createUnManaged(
      T_ETOOL,
      "newfile_wiz.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_TOOL_SHOW_SEGMENTS = createUnManaged(
      T_ETOOL,
      "segment_edit.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_TOOL_OPENTYPE = createUnManaged(T_ETOOL, "opentype.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_TOOL_NEWPROJECT = createUnManaged(
      T_ETOOL,
      "newjprj_wiz.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_TOOL_NEWPACKAGE = createUnManaged(
      T_ETOOL,
      "newpack_wiz.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_TOOL_NEWCLASS = createUnManaged(
      T_ETOOL,
      "newclass_wiz.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_TOOL_NEWINTERFACE = createUnManaged(
      T_ETOOL,
      "newint_wiz.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_TOOL_NEWSNIPPET = createUnManaged(
      T_ETOOL,
      "newsbook_wiz.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_TOOL_NEWPACKROOT = createUnManaged(
      T_ETOOL,
      "newpackfolder_wiz.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_TOOL_RUN = createUnManaged(T_ETOOL, "run_exc.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DESC_DLCL_NEWPACKROOT = createUnManaged(
      T_DLCL,
      "newpackfolder_wiz.gif"); //$NON-NLS-1$
  public static final ImageDescriptor DESC_TOOL_CLASSPATH_ORDER = createUnManaged(
      T_OBJ,
      "cp_order_obj.gif"); //$NON-NLS-1$

  // Keys for correction proposal. We have to put the image into the registry
  // since "code assist" doesn't
  // have a life cycle. So no change to dispose icons.

  public static final ImageDescriptor DESC_ELCL_COLLAPSEALL = createUnManaged(
      T_ELCL,
      "collapseall.gif"); //$NON-NLS-1$
  public static final String IMG_CORRECTION_CHANGE = NAME_PREFIX + "correction_change.gif"; //$NON-NLS-1$
  public static final String IMG_CORRECTION_MOVE = NAME_PREFIX + "correction_move.gif"; //$NON-NLS-1$
  public static final String IMG_CORRECTION_RENAME = NAME_PREFIX + "correction_rename.gif"; //$NON-NLS-1$
  public static final String IMG_CORRECTION_LINKED_RENAME = NAME_PREFIX
      + "correction_linked_rename.gif"; //$NON-NLS-1$
  public static final String IMG_CORRECTION_DELETE_IMPORT = NAME_PREFIX
      + "correction_delete_import.gif"; //$NON-NLS-1$
  public static final String IMG_CORRECTION_LOCAL = NAME_PREFIX + "localvariable_obj.gif"; //$NON-NLS-1$
  public static final String IMG_CORRECTION_REMOVE = NAME_PREFIX + "remove_correction.gif"; //$NON-NLS-1$
  public static final String IMG_CORRECTION_ADD = NAME_PREFIX + "add_correction.gif"; //$NON-NLS-1$

  public static final String IMG_CORRECTION_CAST = NAME_PREFIX + "correction_cast.gif"; //$NON-NLS-1$

  static {
    createManagedFromKey(T_OBJ, IMG_CORRECTION_CHANGE);
    createManagedFromKey(T_OBJ, IMG_CORRECTION_MOVE);
    createManagedFromKey(T_OBJ, IMG_CORRECTION_RENAME);
    createManagedFromKey(T_OBJ, IMG_CORRECTION_LINKED_RENAME);
    createManagedFromKey(T_OBJ, IMG_CORRECTION_DELETE_IMPORT);
    createManagedFromKey(T_OBJ, IMG_CORRECTION_LOCAL);
    createManagedFromKey(T_OBJ, IMG_CORRECTION_REMOVE);
    createManagedFromKey(T_OBJ, IMG_CORRECTION_ADD);
    createManagedFromKey(T_OBJ, IMG_CORRECTION_CAST);
  }

  /*
   * Creates an image descriptor for the given path in a bundle. The path can contain variables like
   * $NL$. If no image could be found, <code>useMissingImageDescriptor</code> decides if either the
   * 'missing image descriptor' is returned or <code>null</code>. Added for 3.1.1.
   */
  public static ImageDescriptor createImageDescriptor(Bundle bundle, IPath path,
      boolean useMissingImageDescriptor) {
    URL url = FileLocator.find(bundle, path, null);
    if (url != null) {
      return ImageDescriptor.createFromURL(url);
    }
    if (useMissingImageDescriptor) {
      return ImageDescriptor.getMissingImageDescriptor();
    }
    return null;
  }

  /**
   * Returns the image managed under the given key in this registry.
   * 
   * @param key the image's key
   * @return the image managed under the given key
   */
  public static Image get(String key) {
    return getImageRegistry().get(key);
  }

  /**
   * Returns the image descriptor for the given key in this registry. Might be called in a non-UI
   * thread.
   * 
   * @param key the image's key
   * @return the image descriptor for the given key
   */
  public static ImageDescriptor getDescriptor(String key) {
    if (fgImageRegistry == null) {
      return fgAvoidSWTErrorMap.get(key);
    }
    return getImageRegistry().getDescriptor(key);
  }

  /**
   * Sets the three image descriptors for enabled, disabled, and hovered to an action. The actions
   * are retrieved from the *lcl16 folders.
   * 
   * @param action the action
   * @param iconName the icon name
   */
  public static void setLocalImageDescriptors(IAction action, String iconName) {
    setImageDescriptors(action, "lcl16", iconName); //$NON-NLS-1$
  }

  /**
   * Sets the three image descriptors for enabled, disabled, and hovered to an action. The actions
   * are retrieved from the *tool16 folders.
   * 
   * @param action the action
   * @param iconName the icon name
   */
  public static void setToolImageDescriptors(IAction action, String iconName) {
    setImageDescriptors(action, "tool16", iconName); //$NON-NLS-1$
  }

  // ---- Helper methods to access icons on the file system
  // --------------------------------------

  /*
   * Helper method to access the image registry from the DartToolsPlugin class.
   */
  /* package */static ImageRegistry getImageRegistry() {
    if (fgImageRegistry == null) {
      fgImageRegistry = new ImageRegistry();
      for (Iterator<String> iter = fgAvoidSWTErrorMap.keySet().iterator(); iter.hasNext();) {
        String key = iter.next();
        fgImageRegistry.put(key, fgAvoidSWTErrorMap.get(key));
      }
      fgAvoidSWTErrorMap = null;
    }
    return fgImageRegistry;
  }

  /*
   * Creates an image descriptor for the given prefix and name in the JDT UI bundle. The path can
   * contain variables like $NL$. If no image could be found, <code>useMissingImageDescriptor</code>
   * decides if either the 'missing image descriptor' is returned or <code>null</code>. or
   * <code>null</code>.
   */
  private static ImageDescriptor create(String prefix, String name,
      boolean useMissingImageDescriptor) {
    IPath path = ICONS_PATH.append(prefix).append(name);
    return createImageDescriptor(
        DartToolsPlugin.getDefault().getBundle(),
        path,
        useMissingImageDescriptor);
  }

  private static ImageDescriptor createManaged(String prefix, String name, String key) {
    ImageDescriptor result = create(prefix, name, true);

    if (fgAvoidSWTErrorMap == null) {
      fgAvoidSWTErrorMap = new HashMap<String, ImageDescriptor>();
    }
    fgAvoidSWTErrorMap.put(key, result);
    if (fgImageRegistry != null) {
      DartToolsPlugin.logErrorMessage("Image registry already defined"); //$NON-NLS-1$
    }
    return result;
  }

  private static ImageDescriptor createManagedFromKey(String prefix, String key) {
    return createManaged(prefix, key.substring(NAME_PREFIX_LENGTH), key);
  }

  /*
   * Creates an image descriptor for the given prefix and name in the JDT UI bundle. The path can
   * contain variables like $NL$. If no image could be found, the 'missing image descriptor' is
   * returned.
   */
  private static ImageDescriptor createUnManaged(String prefix, String name) {
    return create(prefix, name, true);
  }

  /*
   * Creates an image descriptor for the given prefix and name in the JDT UI bundle and let tye
   * descriptor cache the image data. If no image could be found, the 'missing image descriptor' is
   * returned.
   */
  private static ImageDescriptor createUnManagedCached(String prefix, String name) {
    return new CachedImageDescriptor(create(prefix, name, true));
  }

  private static void setImageDescriptors(IAction action, String type, String relPath) {
    ImageDescriptor id = create("d" + type, relPath, false); //$NON-NLS-1$
    if (id != null) {
      action.setDisabledImageDescriptor(id);
    }

    /*
     * id= create("c" + type, relPath, false); //$NON-NLS-1$ if (id != null)
     * action.setHoverImageDescriptor(id);
     */

    ImageDescriptor descriptor = create("e" + type, relPath, true); //$NON-NLS-1$
    action.setHoverImageDescriptor(descriptor);
    action.setImageDescriptor(descriptor);
  }
}
