// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(zra): Remove when tests are ready to enable.
#include "platform/globals.h"
#if !defined(TARGET_ARCH_ARM64)

#include "platform/assert.h"
#include "vm/class_finalizer.h"
#include "vm/compiler.h"
#include "vm/dart_api_impl.h"
#include "vm/object.h"
#include "vm/symbols.h"
#include "vm/unit_test.h"

namespace dart {

TEST_CASE(CompileScript) {
  const char* kScriptChars =
      "class A {\n"
      "  static foo() { return 42; }\n"
      "}\n";
  String& url = String::Handle(String::New("dart-test:CompileScript"));
  String& source = String::Handle(String::New(kScriptChars));
  Script& script = Script::Handle(Script::New(url,
                                              source,
                                              RawScript::kScriptTag));
  Library& lib = Library::Handle(Library::CoreLibrary());
  EXPECT(CompilerTest::TestCompileScript(lib, script));
}


TEST_CASE(CompileFunction) {
  const char* kScriptChars =
            "class A {\n"
            "  static foo() { return 42; }\n"
            "  static moo() {\n"
            "    // A.foo();\n"
            "  }\n"
            "}\n";
  String& url = String::Handle(String::New("dart-test:CompileFunction"));
  String& source = String::Handle(String::New(kScriptChars));
  Script& script = Script::Handle(Script::New(url,
                                              source,
                                              RawScript::kScriptTag));
  Library& lib = Library::Handle(Library::CoreLibrary());
  EXPECT(CompilerTest::TestCompileScript(lib, script));
  EXPECT(ClassFinalizer::ProcessPendingClasses());
  Class& cls = Class::Handle(
      lib.LookupClass(String::Handle(Symbols::New("A"))));
  EXPECT(!cls.IsNull());
  String& function_foo_name = String::Handle(String::New("foo"));
  Function& function_foo =
      Function::Handle(cls.LookupStaticFunction(function_foo_name));
  EXPECT(!function_foo.IsNull());
  String& function_source = String::Handle(function_foo.GetSource());
  EXPECT_STREQ("static foo() { return 42; }\n  ", function_source.ToCString());
  EXPECT(CompilerTest::TestCompileFunction(function_foo));
  EXPECT(function_foo.HasCode());

  String& function_moo_name = String::Handle(String::New("moo"));
  Function& function_moo =
      Function::Handle(cls.LookupStaticFunction(function_moo_name));
  EXPECT(!function_moo.IsNull());

  EXPECT(CompilerTest::TestCompileFunction(function_moo));
  EXPECT(function_moo.HasCode());
  function_source = function_moo.GetSource();
  EXPECT_STREQ("static moo() {\n    // A.foo();\n  }\n",
               function_source.ToCString());
}


TEST_CASE(EvalExpression) {
  const char* kScriptChars =
      "int ten = 2 * 5;              \n"
      "get dot => '.';               \n"
      "class A {                     \n"
      "  var apa = 'Herr Nilsson';   \n"
      "  calc(x) => '${x*ten}';      \n"
      "}                             \n"
      "makeObj() => new A();         \n";

  Dart_Handle lib = TestCase::LoadTestScript(kScriptChars, NULL);
  Dart_Handle obj_handle =
      Dart_Invoke(lib, Dart_NewStringFromCString("makeObj"), 0,  NULL);
  EXPECT(!Dart_IsNull(obj_handle));
  EXPECT(!Dart_IsError(obj_handle));
  const Object& obj = Object::Handle(Api::UnwrapHandle(obj_handle));
  EXPECT(!obj.IsNull());
  EXPECT(obj.IsInstance());

  String& expr_text = String::Handle();
  expr_text = String::New("apa + ' ${calc(10)}' + dot");
  Object& val = Object::Handle();
  val = Instance::Cast(obj).Evaluate(expr_text);
  EXPECT(!val.IsNull());
  EXPECT(!val.IsError());
  EXPECT(val.IsString());
  EXPECT_STREQ("Herr Nilsson 100.", val.ToCString());
}

}  // namespace dart

#endif  // !defined(TARGET_ARCH_ARM64)
