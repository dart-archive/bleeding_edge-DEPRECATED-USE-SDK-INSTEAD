// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(zra): Remove when tests are ready to enable.
#include "platform/globals.h"
#if !defined(TARGET_ARCH_ARM64)

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
    SnapshotReader reader(message->data(), message->len(),
                          Snapshot::kMessage, Isolate::Current());
    const Object& response_obj = Object::Handle(reader.ReadObject());
    String& response = String::Handle();
    response ^= response_obj.raw();
    _msg = strdup(response.ToCString());
    return true;
  }

  // Removes a given json key:value from _msg.
  void filterMsg(const char* key) {
    int key_len = strlen(key);
    int old_len = strlen(_msg);
    char* new_msg = reinterpret_cast<char*>(malloc(old_len + 1));
    int old_pos = 0;
    int new_pos = 0;
    while (_msg[old_pos] != '\0') {
      if (_msg[old_pos] == '\"') {
        old_pos++;
        if ((old_len - old_pos) > key_len &&
            strncmp(&_msg[old_pos], key, key_len) == 0 &&
            _msg[old_pos + key_len] == '\"') {
          old_pos += (key_len + 2);
          // Skip until next , or }.
          while (_msg[old_pos] != '\0' &&
                 _msg[old_pos] != ',' &&
                 _msg[old_pos] != '}') {
            old_pos++;
          }
          if (_msg[old_pos] == ',') {
            old_pos++;
          }
        } else {
          new_msg[new_pos] = '\"';;
          new_pos++;
        }
      } else {
        new_msg[new_pos] = _msg[old_pos];
        new_pos++;
        old_pos++;
      }
    }
    new_msg[new_pos] = '\0';
    free(_msg);
    _msg = new_msg;
  }

  const char* msg() const { return _msg; }

 private:
  char* _msg;
};


static RawInstance* Eval(Dart_Handle lib, const char* expr) {
  Dart_Handle result = Dart_EvaluateExpr(lib, NewString(expr));
  EXPECT_VALID(result);
  Isolate* isolate = Isolate::Current();
  const Instance& instance = Api::UnwrapInstanceHandle(isolate, result);
  return instance.raw();
}


static RawInstance* EvalF(Dart_Handle lib, const char* fmt, ...) {
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


// Search for the formatted string in buff.
//
// TODO(turnidge): This function obscures the line number of failing
// EXPECTs.  Rework this.
static void ExpectSubstringF(const char* buff, const char* fmt, ...) {
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

  EXPECT_SUBSTRING(buffer, buff);
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


TEST_CASE(Service_Isolate) {
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
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();

  // Get the isolate summary.
  service_msg = Eval(lib, "[port, [], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();

  JSONReader reader(handler.msg());

  const int kBufferSize = 128;
  char buffer[kBufferSize];

  // Check that the response string is somewhat sane.

  // type
  EXPECT(reader.Seek("type"));
  EXPECT_EQ(reader.Type(), JSONReader::kString);
  reader.GetDecodedValueChars(buffer, kBufferSize);
  EXPECT_STREQ("Isolate", buffer);

  // id
  EXPECT(reader.Seek("id"));
  EXPECT_EQ(reader.Type(), JSONReader::kString);
  reader.GetDecodedValueChars(buffer, kBufferSize);
  EXPECT_SUBSTRING("isolates/", buffer);

  // heap
  EXPECT(reader.Seek("heap"));
  EXPECT_EQ(reader.Type(), JSONReader::kObject);

  // timers
  EXPECT(reader.Seek("timers"));
  EXPECT_EQ(reader.Type(), JSONReader::kArray);
}


TEST_CASE(Service_StackTrace) {
  // TODO(turnidge): Extend this test to cover a non-trivial stack trace.
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
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();

  // Get the stacktrace.
  service_msg = Eval(lib, "[port, ['stacktrace'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
      "{\"type\":\"StackTrace\",\"id\":\"stacktrace\",\"members\":[]}",
      handler.msg());

  // Malformed request.
  service_msg = Eval(lib, "[port, ['stacktrace', 'jamboree'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
      "{\"type\":\"Error\",\"id\":\"\",\"message\":\"Command too long\","
      "\"request\":{\"arguments\":[\"stacktrace\",\"jamboree\"],"
      "\"option_keys\":[],\"option_values\":[]}}",
      handler.msg());
}


TEST_CASE(Service_DebugBreakpoints) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "main() {\n"   // We set breakpoint here.
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(lib);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();

  // Add a breakpoint.
  const String& url = String::Handle(String::New(TestCase::url()));
  isolate->debugger()->SetBreakpointAtLine(url, 3);

  // Get the breakpoint list.
  service_msg = Eval(lib, "[port, ['debug', 'breakpoints'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
      "{\"type\":\"BreakpointList\",\"breakpoints\":[{"
          "\"type\":\"Breakpoint\",\"id\":1,\"enabled\":true,"
          "\"resolved\":false,"
          "\"location\":{\"type\":\"Location\","
                        "\"script\":\"dart:test-lib\",\"tokenPos\":5}}]}",
      handler.msg());

  // Individual breakpoint.
  service_msg = Eval(lib, "[port, ['debug', 'breakpoints', '1'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
      "{\"type\":\"Breakpoint\",\"id\":1,\"enabled\":true,"
       "\"resolved\":false,"
       "\"location\":{\"type\":\"Location\","
                     "\"script\":\"dart:test-lib\",\"tokenPos\":5}}",
      handler.msg());

  // Missing sub-command.
  service_msg = Eval(lib, "[port, ['debug'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
      "{\"type\":\"Error\",\"id\":\"\","
       "\"message\":\"Must specify a subcommand\","
       "\"request\":{\"arguments\":[\"debug\"],\"option_keys\":[],"
                    "\"option_values\":[]}}",
      handler.msg());

  // Unrecognized breakpoint.
  service_msg = Eval(lib, "[port, ['debug', 'breakpoints', '1111'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("{\"type\":\"Error\",\"id\":\"\","
                "\"message\":\"Unrecognized breakpoint id 1111\","
                "\"request\":{"
                    "\"arguments\":[\"debug\",\"breakpoints\",\"1111\"],"
                    "\"option_keys\":[],\"option_values\":[]}}",
               handler.msg());

  // Command too long.
  service_msg =
      Eval(lib, "[port, ['debug', 'breakpoints', '1111', 'green'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("{\"type\":\"Error\",\"id\":\"\","
                "\"message\":\"Command too long\","
                "\"request\":{\"arguments\":[\"debug\",\"breakpoints\","
                                            "\"1111\",\"green\"],"
                             "\"option_keys\":[],\"option_values\":[]}}",
               handler.msg());

  // Unrecognized subcommand.
  service_msg = Eval(lib, "[port, ['debug', 'nosferatu'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("{\"type\":\"Error\",\"id\":\"\","
                "\"message\":\"Unrecognized subcommand 'nosferatu'\","
                "\"request\":{\"arguments\":[\"debug\",\"nosferatu\"],"
                             "\"option_keys\":[],\"option_values\":[]}}",
               handler.msg());
}


TEST_CASE(Service_Objects) {
  // TODO(turnidge): Extend this test to cover a non-trivial stack trace.
  const char* kScript =
      "var port;\n"     // Set to our mock port by C++.
      "var validId;\n"  // Set to a valid object id by C++.
      "\n"
      "main() {\n"
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(lib);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  ObjectIdRing* ring = isolate->object_id_ring();
  const String& str = String::Handle(String::New("value"));
  intptr_t str_id = ring->GetIdForObject(str.raw());
  Dart_Handle valid_id = Dart_NewInteger(str_id);
  EXPECT_VALID(valid_id);
  EXPECT_VALID(Dart_SetField(lib, NewString("validId"), valid_id));

  Instance& service_msg = Instance::Handle();

  // null
  service_msg = Eval(lib, "[port, ['objects', 'null'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Null\",\"id\":\"objects\\/null\","
      "\"valueAsString\":\"null\"}",
      handler.msg());

  // not initialized
  service_msg = Eval(lib, "[port, ['objects', 'not-initialized'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Null\",\"id\":\"objects\\/not-initialized\","
      "\"valueAsString\":\"<not initialized>\"}",
      handler.msg());

  // being initialized
  service_msg = Eval(lib, "[port, ['objects', 'being-initialized'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Null\",\"id\":\"objects\\/being-initialized\","
      "\"valueAsString\":\"<being initialized>\"}",
      handler.msg());

  // optimized out
  service_msg = Eval(lib, "[port, ['objects', 'optimized-out'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Null\",\"id\":\"objects\\/optimized-out\","
      "\"valueAsString\":\"<optimized out>\"}",
      handler.msg());

  // collected
  service_msg = Eval(lib, "[port, ['objects', 'collected'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Null\",\"id\":\"objects\\/collected\","
      "\"valueAsString\":\"<collected>\"}",
      handler.msg());

  // expired
  service_msg = Eval(lib, "[port, ['objects', 'expired'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Null\",\"id\":\"objects\\/expired\","
      "\"valueAsString\":\"<expired>\"}",
      handler.msg());

  // bool
  service_msg = Eval(lib, "[port, ['objects', 'bool-true'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Bool\",\"id\":\"objects\\/bool-true\","
      "\"class\":{\"type\":\"@Class\",\"id\":\"classes\\/46\","
      "\"user_name\":\"bool\"},\"valueAsString\":\"true\"}",
      handler.msg());

  // int
  service_msg = Eval(lib, "[port, ['objects', 'int-123'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Smi\","
      "\"class\":{\"type\":\"@Class\",\"id\":\"classes\\/42\","
      "\"user_name\":\"int\"},"
      "\"fields\":[],"
      "\"id\":\"objects\\/int-123\","
      "\"valueAsString\":\"123\"}",
      handler.msg());

  // object id ring / valid
  service_msg = Eval(lib, "[port, ['objects', '$validId'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  handler.filterMsg("size");
  handler.filterMsg("id");
  EXPECT_STREQ(
      "{\"type\":\"String\","
      "\"class\":{\"type\":\"@Class\","
      "\"user_name\":\"String\"},\"fields\":[],"
      "\"valueAsString\":\"\\\"value\\\"\"}",
      handler.msg());

  // object id ring / invalid => expired
  service_msg = Eval(lib, "[port, ['objects', '99999999'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Null\",\"id\":\"objects\\/expired\","
      "\"valueAsString\":\"<expired>\"}",
      handler.msg());

  // expired/eval => error
  service_msg = Eval(lib, "[port, ['objects', 'expired', 'eval'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Error\",\"id\":\"\","
      "\"message\":\"expected at most 2 arguments but found 3\\n\","
      "\"request\":{\"arguments\":[\"objects\",\"expired\",\"eval\"],"
      "\"option_keys\":[],\"option_values\":[]}}",
      handler.msg());

  // int/eval => good
  service_msg = Eval(lib,
                     "[port, ['objects', 'int-123', 'eval'], "
                     "['expr'], ['this+99']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"@Smi\","
      "\"class\":{\"type\":\"@Class\",\"id\":\"classes\\/42\","
      "\"user_name\":\"int\"},"
      "\"id\":\"objects\\/int-222\","
      "\"valueAsString\":\"222\"}",
      handler.msg());

  // eval returning null works
  service_msg = Eval(lib,
                     "[port, ['objects', 'int-123', 'eval'], "
                     "['expr'], ['null']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"@Null\",\"id\":\"objects\\/null\","
      "\"valueAsString\":\"null\"}",
      handler.msg());

  // object id ring / invalid => expired
  service_msg = Eval(lib, "[port, ['objects', '99999999', 'eval'], "
                     "['expr'], ['this']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Error\",\"id\":\"\",\"kind\":\"EvalExpired\","
      "\"message\":\"attempt to evaluate against expired object\\n\","
      "\"request\":{\"arguments\":[\"objects\",\"99999999\",\"eval\"],"
      "\"option_keys\":[\"expr\"],\"option_values\":[\"this\"]}}",
      handler.msg());

  // Extra arg to eval.
  service_msg = Eval(lib,
                     "[port, ['objects', 'int-123', 'eval', 'foo'], "
                     "['expr'], ['this+99']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"Error\",\"id\":\"\","
      "\"message\":\"expected at most 3 arguments but found 4\\n\","
      "\"request\":{\"arguments\":[\"objects\",\"int-123\",\"eval\",\"foo\"],"
      "\"option_keys\":[\"expr\"],\"option_values\":[\"this+99\"]}}",
      handler.msg());
}


TEST_CASE(Service_Libraries) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "var libVar = 54321;\n"
      "\n"
      "main() {\n"
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle h_lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(h_lib);
  Library& lib = Library::Handle();
  lib ^= Api::UnwrapHandle(h_lib);
  EXPECT(!lib.IsNull());

  // Find the current library.
  intptr_t lib_id = -1;
  const GrowableObjectArray& libs =
      GrowableObjectArray::Handle(isolate->object_store()->libraries());
  for (intptr_t i = 0; i < libs.Length(); i++) {
    if (libs.At(i) == lib.raw()) {
      lib_id = i;
    }
  }
  ASSERT(lib_id > 0);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(h_lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();

  // Request library.
  service_msg = EvalF(h_lib,
                      "[port, ['libraries', '%" Pd "'], [], []]", lib_id);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Library\"", handler.msg());
  EXPECT_SUBSTRING("\"url\":\"dart:test-lib\"", handler.msg());

  // Evaluate an expression from a library.
  service_msg = EvalF(h_lib,
                      "[port, ['libraries', '%" Pd "', 'eval'], "
                      "['expr'], ['libVar - 1']]", lib_id);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"@Smi\","
      "\"class\":{\"type\":\"@Class\",\"id\":\"classes\\/42\","
      "\"user_name\":\"int\"},\"id\":\"objects\\/int-54320\","
      "\"valueAsString\":\"54320\"}",
      handler.msg());
}


TEST_CASE(Service_Classes) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "class A {\n"
      "  var a;\n"
      "  static var cobra = 11235;\n"
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
  Dart_Handle h_lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(h_lib);
  Library& lib = Library::Handle();
  lib ^= Api::UnwrapHandle(h_lib);
  EXPECT(!lib.IsNull());
  Dart_Handle result = Dart_Invoke(h_lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);
  const Class& class_a = Class::Handle(GetClass(lib, "A"));
  EXPECT(!class_a.IsNull());
  intptr_t cid = class_a.id();

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(h_lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();

  // Request an invalid class id.
  service_msg = Eval(h_lib, "[port, ['classes', '999999'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
    "{\"type\":\"Error\",\"id\":\"\","
    "\"message\":\"999999 is not a valid class id.\","
    "\"request\":{\"arguments\":[\"classes\",\"999999\"],"
    "\"option_keys\":[],\"option_values\":[]}}", handler.msg());

  // Request the class A over the service.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "'], [], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Class\"", handler.msg());
  ExpectSubstringF(handler.msg(),
                   "\"id\":\"classes\\/%" Pd "\",\"name\":\"A\",", cid);

  // Evaluate an expression from class A.
  service_msg = EvalF(h_lib,
                      "[port, ['classes', '%" Pd "', 'eval'], "
                      "['expr'], ['cobra + 100000']]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  handler.filterMsg("name");
  EXPECT_STREQ(
      "{\"type\":\"@Smi\","
      "\"class\":{\"type\":\"@Class\",\"id\":\"classes\\/42\","
      "\"user_name\":\"int\"},"
      "\"id\":\"objects\\/int-111235\","
      "\"valueAsString\":\"111235\"}",
      handler.msg());

  // Request function 0 from class A.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'functions', '0'],"
                              "[], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Function\"", handler.msg());
  ExpectSubstringF(handler.msg(),
                   "\"id\":\"classes\\/%" Pd "\\/functions\\/0\","
                   "\"name\":\"get:a\",", cid);

  // Request field 0 from class A.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'fields', '0'],"
                              "[], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Field\"", handler.msg());
  ExpectSubstringF(handler.msg(),
                   "\"id\":\"classes\\/%" Pd "\\/fields\\/0\","
                   "\"name\":\"a\",", cid);

  // Invalid sub command.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'huh', '0'],"
                              "[], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  ExpectSubstringF(handler.msg(),
    "{\"type\":\"Error\",\"id\":\"\",\"message\":\"Invalid sub collection huh\""
    ",\"request\":"
    "{\"arguments\":[\"classes\",\"%" Pd "\",\"huh\",\"0\"],\"option_keys\":[],"
    "\"option_values\":[]}}", cid);

  // Invalid field request.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'fields', '9'],"
                              "[], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  ExpectSubstringF(handler.msg(),
    "{\"type\":\"Error\",\"id\":\"\",\"message\":\"Field 9 not found\","
    "\"request\":{\"arguments\":[\"classes\",\"%" Pd "\",\"fields\",\"9\"],"
    "\"option_keys\":[],\"option_values\":[]}}", cid);

  // Invalid function request.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'functions', '9'],"
                              "[], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  ExpectSubstringF(handler.msg(),
    "{\"type\":\"Error\",\"id\":\"\",\"message\":\"Function 9 not found\","
    "\"request\":{\"arguments\":[\"classes\",\"%" Pd "\",\"functions\",\"9\"],"
    "\"option_keys\":[],\"option_values\":[]}}", cid);


  // Invalid field subcommand.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'fields', '9', 'x']"
                             ",[], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  ExpectSubstringF(handler.msg(),
    "{\"type\":\"Error\",\"id\":\"\",\"message\":\"Command too long\","
    "\"request\":"
    "{\"arguments\":[\"classes\",\"%" Pd "\",\"fields\",\"9\",\"x\"],"
    "\"option_keys\":[],\"option_values\":[]}}", cid);

  // Invalid function request.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'functions', '9',"
                             "'x'], [], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  ExpectSubstringF(handler.msg(),
    "{\"type\":\"Error\",\"id\":\"\",\"message\":\"Command too long\","
    "\"request\":"
    "{\"arguments\":[\"classes\",\"%" Pd "\",\"functions\",\"9\",\"x\"],"
    "\"option_keys\":[],\"option_values\":[]}}", cid);
}


TEST_CASE(Service_Types) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "class A<T> { }\n"
      "\n"
      "main() {\n"
      "  new A<A<bool>>();\n"
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle h_lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(h_lib);
  Library& lib = Library::Handle();
  lib ^= Api::UnwrapHandle(h_lib);
  EXPECT(!lib.IsNull());
  Dart_Handle result = Dart_Invoke(h_lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);
  const Class& class_a = Class::Handle(GetClass(lib, "A"));
  EXPECT(!class_a.IsNull());
  intptr_t cid = class_a.id();

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(h_lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();

  // Request the class A over the service.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "'], [], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Class\"", handler.msg());
  EXPECT_SUBSTRING("\"name\":\"A\"", handler.msg());
  ExpectSubstringF(handler.msg(),
                   "\"id\":\"classes\\/%" Pd "\"", cid);

  // Request canonical type 0 from class A.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'types', '0'],"
                              "[], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Type\"", handler.msg());
  EXPECT_SUBSTRING("\"name\":\"A<bool>\"", handler.msg());
  ExpectSubstringF(handler.msg(),
                   "\"id\":\"classes\\/%" Pd "\\/types\\/0\"", cid);

  // Request canonical type 1 from class A.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'types', '1'],"
                              "[], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Type\"", handler.msg());
  EXPECT_SUBSTRING("\"name\":\"A<A<bool>>\"", handler.msg());
  ExpectSubstringF(handler.msg(),
                   "\"id\":\"classes\\/%" Pd "\\/types\\/1\"", cid);

  // Request for non-existent canonical type from class A.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'types', '42'],"
                              "[], []]", cid);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  ExpectSubstringF(handler.msg(),
    "{\"type\":\"Error\",\"id\":\"\","
    "\"message\":\"Canonical type 42 not found\""
    ",\"request\":"
    "{\"arguments\":[\"classes\",\"%" Pd "\",\"types\",\"42\"],"
    "\"option_keys\":[],\"option_values\":[]}}", cid);

  // Request canonical type arguments. Expect <A<bool>> to be listed.
  service_msg = EvalF(h_lib, "[port, ['typearguments'],"
                              "[], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"TypeArgumentsList\"", handler.msg());
  ExpectSubstringF(handler.msg(), "\"name\":\"<A<bool>>\",");

  // Request canonical type arguments with instantiations.
  service_msg = EvalF(h_lib, "[port, ['typearguments', 'withinstantiations'],"
                              "[], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"TypeArgumentsList\"", handler.msg());
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
  Dart_Handle h_lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(h_lib);
  Library& lib = Library::Handle();
  lib ^= Api::UnwrapHandle(h_lib);
  EXPECT(!lib.IsNull());
  Dart_Handle result = Dart_Invoke(h_lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);
  const Class& class_a = Class::Handle(GetClass(lib, "A"));
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
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(h_lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();

  // Request an invalid code object.
  service_msg = Eval(h_lib, "[port, ['code', '0'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
    "{\"type\":\"Error\",\"id\":\"\",\"message\":\"Malformed code id: 0\","
    "\"request\":{\"arguments\":[\"code\",\"0\"],"
    "\"option_keys\":[],\"option_values\":[]}}", handler.msg());

  // The following test checks that a code object can be found only
  // at compile_timestamp()-code.EntryPoint().
  service_msg = EvalF(h_lib, "[port, ['code', '%" Px64"-%" Px "'], [], []]",
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
  service_msg = EvalF(h_lib, "[port, ['code', '%" Px64"-%" Px "'], [], []]",
                      compile_timestamp,
                      address);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  {
    // Only perform a partial match.
    const intptr_t kBufferSize = 512;
    char buffer[kBufferSize];
    OS::SNPrint(buffer, kBufferSize-1,
                "Could not find code with id: %" Px64 "-%" Px "",
                compile_timestamp,
                address);
    EXPECT_SUBSTRING(buffer, handler.msg());
  }

  // Request code object at (compile_timestamp - 1)-code.EntryPoint()
  // Expect this to fail because the timestamp is wrong.
  address = entry;
  service_msg = EvalF(h_lib, "[port, ['code', '%" Px64"-%" Px "'], [], []]",
                      compile_timestamp - 1,
                      address);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  {
    // Only perform a partial match.
    const intptr_t kBufferSize = 512;
    char buffer[kBufferSize];
    OS::SNPrint(buffer, kBufferSize-1,
                "Could not find code with id: %" Px64 "-%" Px "",
                compile_timestamp - 1,
                address);
    EXPECT_SUBSTRING(buffer, handler.msg());
  }

  // Request native code at address. Expect the null code object back.
  address = last;
  service_msg = EvalF(h_lib, "[port, ['code', 'native-%" Px "'], [], []]",
                      address);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("{\"type\":\"null\"}", handler.msg());

  // Request malformed native code.
  service_msg = EvalF(h_lib, "[port, ['code', 'native%" Px "'], [], []]",
                      address);
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"message\":\"Malformed code id:", handler.msg());
}


TEST_CASE(Service_VM) {
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
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();
  service_msg = Eval(lib, "[port, ['vm'], [], []]");

  Service::HandleRootMessage(service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"VM\",\"id\":\"vm\"", handler.msg());
  EXPECT_SUBSTRING("\"architecture\"", handler.msg());
  EXPECT_SUBSTRING("\"version\"", handler.msg());
  EXPECT_SUBSTRING("\"uptime\"", handler.msg());
  EXPECT_SUBSTRING("\"isolates\"", handler.msg());
}


TEST_CASE(Service_Scripts) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "main() {\n"
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle h_lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(h_lib);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(h_lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();
  service_msg = Eval(h_lib, "[port, ['scripts', 'dart:test-lib'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
      "{\"type\":\"Script\",\"id\":\"scripts\\/dart%3Atest-lib\","
       "\"name\":\"dart:test-lib\",\"user_name\":\"dart:test-lib\","
       "\"kind\":\"script\","
       "\"source\":\"var port;\\n\\nmain() {\\n}\","
       "\"tokenPosTable\":[[1,0,1,1,5,2,9],[3,5,1,6,5,7,6,8,8],[4,10,1]]}",
      handler.msg());
}


TEST_CASE(Service_Coverage) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "var x = 7;\n"
      "main() {\n"
      "  x = x * x;\n"
      "  x = x / 13;\n"
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle h_lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(h_lib);
  Library& lib = Library::Handle();
  lib ^= Api::UnwrapHandle(h_lib);
  EXPECT(!lib.IsNull());
  Dart_Handle result = Dart_Invoke(h_lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(h_lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();
  service_msg = Eval(h_lib, "[port, ['coverage'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING(
      "{\"source\":\"dart:test-lib\",\"script\":{"
      "\"type\":\"@Script\",\"id\":\"scripts\\/dart%3Atest-lib\","
      "\"name\":\"dart:test-lib\",\"user_name\":\"dart:test-lib\","
      "\"kind\":\"script\"},\"hits\":"
      "[5,1,6,1]}", handler.msg());
}


TEST_CASE(Service_AllocationProfile) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "var x = 7;\n"
      "main() {\n"
      "  x = x * x;\n"
      "  x = x / 13;\n"
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle h_lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(h_lib);
  Library& lib = Library::Handle();
  lib ^= Api::UnwrapHandle(h_lib);
  EXPECT(!lib.IsNull());
  Dart_Handle result = Dart_Invoke(h_lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(h_lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();
  service_msg = Eval(h_lib, "[port, ['allocationprofile'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"AllocationProfile\"", handler.msg());

  // Too long.
  service_msg = Eval(h_lib, "[port, ['allocationprofile', 'foo'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Error\"", handler.msg());

  // Bad gc option.
  service_msg = Eval(h_lib, "[port, ['allocationprofile'], ['gc'], ['cat']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Error\"", handler.msg());

  // Bad reset option.
  service_msg = Eval(h_lib, "[port, ['allocationprofile'], ['reset'], ['ff']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"Error\"", handler.msg());

  // Good reset.
  service_msg =
      Eval(h_lib, "[port, ['allocationprofile'], ['reset'], ['true']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"AllocationProfile\"", handler.msg());

  // Good GC.
  service_msg =
      Eval(h_lib, "[port, ['allocationprofile'], ['gc'], ['full']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"AllocationProfile\"", handler.msg());

  // Good GC and reset.
  service_msg = Eval(h_lib,
      "[port, ['allocationprofile'], ['gc', 'reset'], ['full', 'true']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"AllocationProfile\"", handler.msg());
}


TEST_CASE(Service_HeapMap) {
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
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();
  service_msg = Eval(lib, "[port, ['heapmap'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"HeapMap\"", handler.msg());
  EXPECT_SUBSTRING("\"pages\":[", handler.msg());
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
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  const String& str = String::Handle(String::New("foobar", Heap::kOld));
  Instance& service_msg = Instance::Handle();
  // Note: If we ever introduce old space compaction, this test might fail.
  uword start_addr = RawObject::ToAddr(str.raw());
  // Expect to find 'str', also from internal addresses.
  for (int offset = 0; offset < kObjectAlignment; ++offset) {
    uword addr = start_addr + offset;
    char buf[1024];
    OS::SNPrint(buf, sizeof(buf),
                "[port, ['address', '%" Px "'], [], []]", addr);
    service_msg = Eval(lib, buf);
    Service::HandleIsolateMessage(isolate, service_msg);
    handler.HandleNextMessage();
    EXPECT_SUBSTRING("\"type\":\"@String\"", handler.msg());
    EXPECT_SUBSTRING("foobar", handler.msg());
  }
  // Expect null when no object is found.
  service_msg = Eval(lib, "[port, ['address', '7'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_SUBSTRING("\"type\":\"null\"", handler.msg());
}


static const char* alpha_callback(
    const char* name,
    const char** arguments,
    intptr_t num_arguments,
    const char** option_keys,
    const char** option_values,
    intptr_t num_options,
    void* user_data) {
  return strdup("alpha");
}


static const char* beta_callback(
    const char* name,
    const char** arguments,
    intptr_t num_arguments,
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
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));


  Instance& service_msg = Instance::Handle();
  service_msg = Eval(lib, "[port, ['alpha'], [], []]");
  Service::HandleRootMessage(service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("alpha", handler.msg());
  service_msg = Eval(lib, "[port, ['beta'], [], []]");
  Service::HandleRootMessage(service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("beta", handler.msg());
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
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();
  service_msg = Eval(lib, "[port, ['alpha'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("alpha", handler.msg());
  service_msg = Eval(lib, "[port, ['beta'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("beta", handler.msg());
}

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
  Dart_Handle h_lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(h_lib);
  Library& lib = Library::Handle();
  lib ^= Api::UnwrapHandle(h_lib);
  EXPECT(!lib.IsNull());
  Dart_Handle result = Dart_Invoke(h_lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(h_lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();
  service_msg = Eval(h_lib, "[port, ['profile'], [], []]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  // Expect profile
  EXPECT_SUBSTRING("\"type\":\"Profile\"", handler.msg());

  service_msg = Eval(h_lib, "[port, ['profile'], ['tags'], ['hide']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  // Expect profile
  EXPECT_SUBSTRING("\"type\":\"Profile\"", handler.msg());

  service_msg = Eval(h_lib, "[port, ['profile'], ['tags'], ['hidden']]");
  Service::HandleIsolateMessage(isolate, service_msg);
  handler.HandleNextMessage();
  // Expect error.
  EXPECT_SUBSTRING("\"type\":\"Error\"", handler.msg());
}

}  // namespace dart

#endif  // !defined(TARGET_ARCH_ARM64)
