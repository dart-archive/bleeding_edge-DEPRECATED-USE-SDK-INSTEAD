// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "platform/globals.h"

#include "include/dart_debugger_api.h"
#include "vm/dart_api_impl.h"
#include "vm/dart_entry.h"
#include "vm/debugger.h"
#include "vm/globals.h"
#include "vm/message_handler.h"
#include "vm/object_id_ring.h"
#include "vm/os.h"
#include "vm/port.h"
#include "vm/service.h"
#include "vm/unit_test.h"

namespace dart {

// This flag is used in the Service_Flags test below.
DEFINE_FLAG(bool, service_testing_flag, false, "Comment");

class ServiceTestMessageHandler : public MessageHandler {
 public:
  ServiceTestMessageHandler() : _msg(NULL) {}

  ~ServiceTestMessageHandler() {
    free(_msg);
  }

  bool HandleMessage(Message* message) {
    if (_msg != NULL) {
      free(_msg);
    }

    // Parse the message.
    SnapshotReader reader(message->data(), message->len(), Snapshot::kMessage,
                          Isolate::Current(), Thread::Current()->zone());
    const Object& response_obj = Object::Handle(reader.ReadObject());
    String& response = String::Handle();
    response ^= response_obj.raw();
    _msg = strdup(response.ToCString());
    return true;
  }

  const char* msg() const { return _msg; }

 private:
  char* _msg;
};


static RawArray* Eval(Dart_Handle lib, const char* expr) {
  const String& dummy_isolate_id = String::Handle(String::New("isolateId"));
  Dart_Handle expr_val = Dart_EvaluateExpr(lib, NewString(expr));
  EXPECT_VALID(expr_val);
  Isolate* isolate = Isolate::Current();
  const GrowableObjectArray& value =
      Api::UnwrapGrowableObjectArrayHandle(isolate, expr_val);
  const Array& result = Array::Handle(Array::MakeArray(value));
  GrowableObjectArray& growable = GrowableObjectArray::Handle();
  growable ^= result.At(4);
  // Append dummy isolate id to parameter values.
  growable.Add(dummy_isolate_id);
  Array& array = Array::Handle(Array::MakeArray(growable));
  result.SetAt(4, array);
  growable ^= result.At(5);
  // Append dummy isolate id to parameter values.
  growable.Add(dummy_isolate_id);
  array = Array::MakeArray(growable);
  result.SetAt(5, array);
  return result.raw();
}


static RawArray* EvalF(Dart_Handle lib, const char* fmt, ...) {
  Isolate* isolate = Isolate::Current();

  va_list args;
  va_start(args, fmt);
  intptr_t len = OS::VSNPrint(NULL, 0, fmt, args);
  va_end(args);

  char* buffer = isolate->current_zone()->Alloc<char>(len + 1);
  va_list args2;
  va_start(args2, fmt);
  OS::VSNPrint(buffer, (len + 1), fmt, args2);
  va_end(args2);

  return Eval(lib, buffer);
}


static RawFunction* GetFunction(const Class& cls, const char* name) {
  const Function& result = Function::Handle(cls.LookupDynamicFunction(
      String::Handle(String::New(name))));
  EXPECT(!result.IsNull());
  return result.raw();
}


static RawClass* GetClass(const Library& lib, const char* name) {
  const Class& cls = Class::Handle(
      lib.LookupClass(String::Handle(Symbols::New(name))));
  EXPECT(!cls.IsNull());  // No ambiguity error expected.
  return cls.raw();
}


TEST_CASE(Service_Code) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "class A {\n"
      "  var a;\n"
      "  dynamic b() {}\n"
      "  dynamic c() {\n"
      "    var d = () { b(); };\n"
      "    return d;\n"
      "  }\n"
      "}\n"
      "main() {\n"
      "  var z = new A();\n"
      "  var x = z.c();\n"
      "  x();\n"
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(lib);
  Library& vmlib = Library::Handle();
  vmlib ^= Api::UnwrapHandle(lib);
  EXPECT(!vmlib.IsNull());
  Dart_Handle result = Dart_Invoke(lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);
  const Class& class_a = Class::Handle(GetClass(vmlib, "A"));
  EXPECT(!class_a.IsNull());
  const Function& function_c = Function::Handle(GetFunction(class_a, "c"));
  EXPECT(!function_c.IsNull());
  const Code& code_c = Code::Handle(function_c.CurrentCode());
  EXPECT(!code_c.IsNull());
  // Use the entry of the code object as it's reference.
  uword entry = code_c.EntryPoint();
  int64_t compile_timestamp = code_c.compile_timestamp();
  EXPECT_GT(code_c.Size(), 16);
  uword last = entry + code_c.Size();

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port = Api::NewHandle(isolate, SendPort::New(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Array& service_msg = Array::Handle();

  // Request an invalid code object.
  service_msg =
      Eval(lib, "[0, port, '0', 'getObject', ['objectId'], ['code/0']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Error\"", handler.msg());

  // The following test checks that a code object can be found only
  // at compile_timestamp()-code.EntryPoint().
  service_msg = EvalF(lib, "[0, port, '0', 'getObject', "
                      "['objectId'], ['code/%" Px64"-%" Px "']]",
                      compile_timestamp,
                      entry);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  {
    // Only perform a partial match.
    const intptr_t kBufferSize = 512;
    char buffer[kBufferSize];
    OS::SNPrint(buffer, kBufferSize-1,
                "{\"type\":\"Code\",\"id\":\"code\\/%" Px64 "-%" Px "\",",
                compile_timestamp,
                entry);
    EXPECT_SUBSTRING(buffer, handler.msg());
  }

  // Request code object at compile_timestamp-code.EntryPoint() + 16
  // Expect this to fail because the address is not the entry point.
  uintptr_t address = entry + 16;
  service_msg = EvalF(lib, "[0, port, '0', 'getObject', "
                      "['objectId'], ['code/%" Px64"-%" Px "']]",
                      compile_timestamp,
                      address);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Error\"", handler.msg());

  // Request code object at (compile_timestamp - 1)-code.EntryPoint()
  // Expect this to fail because the timestamp is wrong.
  address = entry;
  service_msg = EvalF(lib, "[0, port, '0', 'getObject', "
                      "['objectId'], ['code/%" Px64"-%" Px "']]",
                      compile_timestamp - 1,
                      address);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Error\"", handler.msg());

  // Request native code at address. Expect the null code object back.
  address = last;
  service_msg = EvalF(lib, "[0, port, '0', 'getObject', "
                      "['objectId'], ['code/native-%" Px "']]",
                      address);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("{\"type\":\"null\",\"id\":\"objects\\/null\","
                   "\"valueAsString\":\"null\"",
                   handler.msg());

  // Request malformed native code.
  service_msg = EvalF(lib, "[0, port, '0', 'getObject', ['objectId'], "
                      "['code/native%" Px "']]",
                      address);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Error\"", handler.msg());
}


TEST_CASE(Service_TokenStream) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "main() {\n"
      "}";

  Isolate* isolate = Isolate::Current();

  Dart_Handle lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(lib);
  Library& vmlib = Library::Handle();
  vmlib ^= Api::UnwrapHandle(lib);
  EXPECT(!vmlib.IsNull());

  const String& script_name = String::Handle(String::New("test-lib"));
  EXPECT(!script_name.IsNull());
  const Script& script = Script::Handle(vmlib.LookupScript(script_name));
  EXPECT(!script.IsNull());

  const TokenStream& token_stream = TokenStream::Handle(script.tokens());
  EXPECT(!token_stream.IsNull());
  ObjectIdRing* ring = isolate->object_id_ring();
  intptr_t id = ring->GetIdForObject(token_stream.raw());

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port = Api::NewHandle(isolate, SendPort::New(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Array& service_msg = Array::Handle();

  // Fetch object.
  service_msg = EvalF(lib, "[0, port, '0', 'getObject', "
                      "['objectId'], ['objects/%" Pd "']]", id);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();

  // Check type.
  EXPECT_SUBSTRING("\"type\":\"Object\"", handler.msg());
  EXPECT_SUBSTRING("\"_vmType\":\"TokenStream\"", handler.msg());
  // Check for members array.
  EXPECT_SUBSTRING("\"members\":[", handler.msg());
}


TEST_CASE(Service_PcDescriptors) {
  const char* kScript =
    "var port;\n"  // Set to our mock port by C++.
    "\n"
    "class A {\n"
    "  var a;\n"
    "  dynamic b() {}\n"
    "  dynamic c() {\n"
    "    var d = () { b(); };\n"
    "    return d;\n"
    "  }\n"
    "}\n"
    "main() {\n"
    "  var z = new A();\n"
    "  var x = z.c();\n"
    "  x();\n"
    "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(lib);
  Library& vmlib = Library::Handle();
  vmlib ^= Api::UnwrapHandle(lib);
  EXPECT(!vmlib.IsNull());
  Dart_Handle result = Dart_Invoke(lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);
  const Class& class_a = Class::Handle(GetClass(vmlib, "A"));
  EXPECT(!class_a.IsNull());
  const Function& function_c = Function::Handle(GetFunction(class_a, "c"));
  EXPECT(!function_c.IsNull());
  const Code& code_c = Code::Handle(function_c.CurrentCode());
  EXPECT(!code_c.IsNull());

  const PcDescriptors& descriptors =
      PcDescriptors::Handle(code_c.pc_descriptors());
  EXPECT(!descriptors.IsNull());
  ObjectIdRing* ring = isolate->object_id_ring();
  intptr_t id = ring->GetIdForObject(descriptors.raw());

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port = Api::NewHandle(isolate, SendPort::New(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Array& service_msg = Array::Handle();

  // Fetch object.
  service_msg = EvalF(lib, "[0, port, '0', 'getObject', "
                      "['objectId'], ['objects/%" Pd "']]", id);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  // Check type.
  EXPECT_SUBSTRING("\"type\":\"Object\"", handler.msg());
  EXPECT_SUBSTRING("\"_vmType\":\"PcDescriptors\"", handler.msg());
  // Check for members array.
  EXPECT_SUBSTRING("\"members\":[", handler.msg());
}


TEST_CASE(Service_LocalVarDescriptors) {
  const char* kScript =
    "var port;\n"  // Set to our mock port by C++.
    "\n"
    "class A {\n"
    "  var a;\n"
    "  dynamic b() {}\n"
    "  dynamic c() {\n"
    "    var d = () { b(); };\n"
    "    return d;\n"
    "  }\n"
    "}\n"
    "main() {\n"
    "  var z = new A();\n"
    "  var x = z.c();\n"
    "  x();\n"
    "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(lib);
  Library& vmlib = Library::Handle();
  vmlib ^= Api::UnwrapHandle(lib);
  EXPECT(!vmlib.IsNull());
  Dart_Handle result = Dart_Invoke(lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);
  const Class& class_a = Class::Handle(GetClass(vmlib, "A"));
  EXPECT(!class_a.IsNull());
  const Function& function_c = Function::Handle(GetFunction(class_a, "c"));
  EXPECT(!function_c.IsNull());
  const Code& code_c = Code::Handle(function_c.CurrentCode());
  EXPECT(!code_c.IsNull());

  const LocalVarDescriptors& descriptors =
      LocalVarDescriptors::Handle(code_c.var_descriptors());
  // Generate an ID for this object.
  ObjectIdRing* ring = isolate->object_id_ring();
  intptr_t id = ring->GetIdForObject(descriptors.raw());

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port = Api::NewHandle(isolate, SendPort::New(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Array& service_msg = Array::Handle();

  // Fetch object.
  service_msg = EvalF(lib, "[0, port, '0', 'getObject', "
                      "['objectId'], ['objects/%" Pd "']]", id);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  // Check type.
  EXPECT_SUBSTRING("\"type\":\"Object\"", handler.msg());
  EXPECT_SUBSTRING("\"_vmType\":\"LocalVarDescriptors\"", handler.msg());
  // Check for members array.
  EXPECT_SUBSTRING("\"members\":[", handler.msg());
}


TEST_CASE(Service_Address) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "main() {\n"
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(lib);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port = Api::NewHandle(isolate, SendPort::New(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  const String& str = String::Handle(String::New("foobar", Heap::kOld));
  Array& service_msg = Array::Handle();
  // Note: If we ever introduce old space compaction, this test might fail.
  uword start_addr = RawObject::ToAddr(str.raw());
  // Expect to find 'str', also from internal addresses.
  for (int offset = 0; offset < kObjectAlignment; ++offset) {
    uword addr = start_addr + offset;
    char buf[1024];
    bool ref = offset % 2 == 0;
    OS::SNPrint(buf, sizeof(buf),
                (ref
                 ? "[0, port, '0', 'getObjectByAddress', "
                   "['address', 'ref'], ['%" Px "', 'true']]"
                 : "[0, port, '0', 'getObjectByAddress', "
                   "['address'], ['%" Px "']]"),
                addr);
    service_msg = Eval(lib, buf);
    Service::HandleIsolateMessage(isolate, service_msg);
    handler.HandleNextMessage();
    EXPECT_SUBSTRING(ref ? "\"type\":\"@String\"" :
                           "\"type\":\"String\"",
                     handler.msg());
    EXPECT_SUBSTRING("foobar", handler.msg());
  }
  // Expect null when no object is found.
  service_msg = Eval(lib, "[0, port, '0', 'getObjectByAddress', "
                     "['address'], ['7']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  // TODO(turnidge): Should this be a ServiceException instead?
  EXPECT_SUBSTRING("{\"type\":\"Sentinel\",\"id\":\"objects\\/free\","
                   "\"valueAsString\":\"<free>\"",
               handler.msg());
}


static const char* alpha_callback(
    const char* name,
    const char** option_keys,
    const char** option_values,
    intptr_t num_options,
    void* user_data) {
  return strdup("alpha");
}


static const char* beta_callback(
    const char* name,
    const char** option_keys,
    const char** option_values,
    intptr_t num_options,
    void* user_data) {
  return strdup("beta");
}


TEST_CASE(Service_EmbedderRootHandler) {
  const char* kScript =
    "var port;\n"  // Set to our mock port by C++.
    "\n"
    "var x = 7;\n"
    "main() {\n"
    "  x = x * x;\n"
    "  x = x / 13;\n"
    "}";

  Dart_RegisterRootServiceRequestCallback("alpha", alpha_callback, NULL);
  Dart_RegisterRootServiceRequestCallback("beta", beta_callback, NULL);

  Isolate* isolate = Isolate::Current();
  Dart_Handle lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(lib);
  Dart_Handle result = Dart_Invoke(lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port = Api::NewHandle(isolate, SendPort::New(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));


  Array& service_msg = Array::Handle();
  service_msg = Eval(lib, "[0, port, '0', 'alpha', [], []]");
  Service::HandleRootMessage(service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("{\"result\":alpha, \"id\":\"0\"}", handler.msg());
  service_msg = Eval(lib, "[0, port, '0', 'beta', [], []]");
  Service::HandleRootMessage(service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("{\"result\":beta, \"id\":\"0\"}", handler.msg());
}

TEST_CASE(Service_EmbedderIsolateHandler) {
  const char* kScript =
    "var port;\n"  // Set to our mock port by C++.
    "\n"
    "var x = 7;\n"
    "main() {\n"
    "  x = x * x;\n"
    "  x = x / 13;\n"
    "}";

  Dart_RegisterIsolateServiceRequestCallback("alpha", alpha_callback, NULL);
  Dart_RegisterIsolateServiceRequestCallback("beta", beta_callback, NULL);

  Isolate* isolate = Isolate::Current();
  Dart_Handle lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(lib);
  Dart_Handle result = Dart_Invoke(lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port = Api::NewHandle(isolate, SendPort::New(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Array& service_msg = Array::Handle();
  service_msg = Eval(lib, "[0, port, '0', 'alpha', [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("{\"result\":alpha, \"id\":\"0\"}", handler.msg());
  service_msg = Eval(lib, "[0, port, '0', 'beta', [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("{\"result\":beta, \"id\":\"0\"}", handler.msg());
}


// TODO(zra): Remove when tests are ready to enable.
#if !defined(TARGET_ARCH_ARM64)

TEST_CASE(Service_Profile) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "var x = 7;\n"
      "main() {\n"
      "  x = x * x;\n"
      "  x = x / 13;\n"
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(lib);
  Dart_Handle result = Dart_Invoke(lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port = Api::NewHandle(isolate, SendPort::New(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Array& service_msg = Array::Handle();
  service_msg = Eval(lib, "[0, port, '0', 'getCpuProfile', [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  // Expect error (tags required).
  EXPECT_SUBSTRING("\"type\":\"Error\"", handler.msg());

  service_msg =
      Eval(lib, "[0, port, '0', 'getCpuProfile', ['tags'], ['None']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  // Expect profile
  EXPECT_SUBSTRING("\"type\":\"_CpuProfile\"", handler.msg());

  service_msg =
      Eval(lib, "[0, port, '0', 'getCpuProfile', ['tags'], ['Bogus']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  // Expect error.
  EXPECT_SUBSTRING("\"type\":\"Error\"", handler.msg());
}

#endif  // !defined(TARGET_ARCH_ARM64)

}  // namespace dart
