// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Class for intrinsifying functions.

#include "vm/assembler.h"
#include "vm/intrinsifier.h"
#include "vm/flags.h"
#include "vm/object.h"
#include "vm/symbols.h"

namespace dart {

DEFINE_FLAG(bool, intrinsify, true, "Instrinsify when possible");
DECLARE_FLAG(bool, throw_on_javascript_int_overflow);

static bool CompareNames(const Library& lib,
                         const char* test_name,
                         const char* name) {
  static const char* kPrivateGetterPrefix = "get:_";
  static const char* kPrivateSetterPrefix = "set:_";

  if (test_name[0] == '_') {
    if (name[0] != '_') {
      return false;
    }
  } else if (strncmp(test_name,
                     kPrivateGetterPrefix,
                     strlen(kPrivateGetterPrefix)) == 0) {
    if (strncmp(name,
                kPrivateGetterPrefix,
                strlen(kPrivateGetterPrefix)) != 0) {
      return false;
    }
  } else if (strncmp(test_name,
                     kPrivateSetterPrefix,
                     strlen(kPrivateSetterPrefix)) == 0) {
    if (strncmp(name,
                kPrivateSetterPrefix,
                strlen(kPrivateSetterPrefix)) != 0) {
      return false;
    }
  } else {
    return (strcmp(test_name, name) == 0);
  }

  // Check if the private class is member of the library and matches
  // the test_class_name.
  const String& test_str = String::Handle(String::New(test_name));
  const String& test_str_with_key = String::Handle(
      String::Concat(test_str, String::Handle(lib.private_key())));
  if (strcmp(test_str_with_key.ToCString(), name) == 0) {
    return true;
  }

  return false;
}


// Returns true if the function matches function_name and class_name, with
// special recognition of corelib private classes.
static bool TestFunction(const Library& lib,
                         const Function& function,
                         const char* function_class_name,
                         const char* function_name,
                         const char* test_class_name,
                         const char* test_function_name) {
  // If test_function_name starts with a '.' we use that to indicate
  // that it is a named constructor in the class. Therefore, if
  // the class matches and the rest of the method name starting with
  // the dot matches, we have found a match.
  // We do not store the entire factory constructor name with the class
  // (e.g: _GrowableList.withData) because the actual function name
  //  that we see here includes the private key.
  if (test_function_name[0] == '.') {
    function_name = strstr(function_name, ".");
    if (function_name == NULL) {
      return false;
    }
  }
  return CompareNames(lib, test_class_name, function_class_name) &&
         CompareNames(lib, test_function_name, function_name);
}


bool Intrinsifier::CanIntrinsify(const Function& function) {
  if (!FLAG_intrinsify) return false;
  if (function.IsClosureFunction()) return false;
  // Can occur because of compile-all flag.
  if (function.is_external()) return false;
  return function.is_intrinsic();
}


void Intrinsifier::InitializeState() {
  Isolate* isolate = Isolate::Current();
  Library& lib = Library::Handle(isolate);
  Class& cls = Class::Handle(isolate);
  Function& func = Function::Handle(isolate);
  String& str = String::Handle(isolate);
  Error& error = Error::Handle(isolate);

#define SETUP_FUNCTION(class_name, function_name, destination, fp)             \
  if (strcmp(#class_name, "::") == 0) {                                        \
    str = String::New(#function_name);                                         \
    func = lib.LookupFunctionAllowPrivate(str);                                \
  } else {                                                                     \
    str = String::New(#class_name);                                            \
    cls = lib.LookupClassAllowPrivate(str);                                    \
    ASSERT(!cls.IsNull());                                                     \
    error = cls.EnsureIsFinalized(isolate);                                    \
    if (!error.IsNull()) {                                                     \
      OS::PrintErr("%s\n", error.ToErrorCString());                            \
    }                                                                          \
    ASSERT(error.IsNull());                                                    \
    if (#function_name[0] == '.') {                                            \
      str = String::New(#class_name#function_name);                            \
    } else {                                                                   \
      str = String::New(#function_name);                                       \
    }                                                                          \
    func = cls.LookupFunctionAllowPrivate(str);                                \
  }                                                                            \
  ASSERT(!func.IsNull());                                                      \
  func.set_is_intrinsic(true);

  // Set up all core lib functions that can be intrisified.
  lib = Library::CoreLibrary();
  CORE_LIB_INTRINSIC_LIST(SETUP_FUNCTION);

  // Integer intrinsics are in the core library, but we don't want to intrinsify
  // when Smi > 32 bits if we are looking for javascript integer overflow.
  if (!FLAG_throw_on_javascript_int_overflow || (Smi::kBits < 32)) {
    CORE_INTEGER_LIB_INTRINSIC_LIST(SETUP_FUNCTION);
  }

  // Set up all math lib functions that can be intrisified.
  lib = Library::MathLibrary();
  MATH_LIB_INTRINSIC_LIST(SETUP_FUNCTION);

  // Set up all dart:typed_data lib functions that can be intrisified.
  lib = Library::TypedDataLibrary();
  TYPED_DATA_LIB_INTRINSIC_LIST(SETUP_FUNCTION);

#undef SETUP_FUNCTION
}


void Intrinsifier::Intrinsify(const Function& function, Assembler* assembler) {
  if (!CanIntrinsify(function)) return;

  const char* function_name = String::Handle(function.name()).ToCString();
  const Class& function_class = Class::Handle(function.Owner());
  const char* class_name = String::Handle(function_class.Name()).ToCString();
  const Library& lib = Library::Handle(function_class.library());

#define FIND_INTRINSICS(test_class_name, test_function_name, destination, fp)  \
  if (TestFunction(lib, function,                                              \
                   class_name, function_name,                                  \
                   #test_class_name, #test_function_name)) {                   \
    ASSERT(function.CheckSourceFingerprint(fp));                               \
    assembler->Comment("Intrinsic");                                           \
    destination(assembler);                                                    \
    return;                                                                    \
  }                                                                            \

  if (lib.raw() == Library::CoreLibrary()) {
    CORE_LIB_INTRINSIC_LIST(FIND_INTRINSICS);
    if (!FLAG_throw_on_javascript_int_overflow || (Smi::kBits < 32)) {
      CORE_INTEGER_LIB_INTRINSIC_LIST(FIND_INTRINSICS);
    }
  } else if (lib.raw() == Library::TypedDataLibrary()) {
    TYPED_DATA_LIB_INTRINSIC_LIST(FIND_INTRINSICS);
  } else if (lib.raw() == Library::MathLibrary()) {
    MATH_LIB_INTRINSIC_LIST(FIND_INTRINSICS);
  }
#undef FIND_INTRINSICS
}

}  // namespace dart
