// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/service.h"

#include "include/dart_api.h"
#include "platform/globals.h"

#include "vm/compiler.h"
#include "vm/coverage.h"
#include "vm/cpu.h"
#include "vm/dart_api_impl.h"
#include "vm/dart_entry.h"
#include "vm/debugger.h"
#include "vm/isolate.h"
#include "vm/message.h"
#include "vm/message_handler.h"
#include "vm/native_entry.h"
#include "vm/native_arguments.h"
#include "vm/object.h"
#include "vm/object_id_ring.h"
#include "vm/object_store.h"
#include "vm/port.h"
#include "vm/profiler.h"
#include "vm/stack_frame.h"
#include "vm/symbols.h"
#include "vm/version.h"


namespace dart {

DEFINE_FLAG(bool, trace_service, false, "Trace VM service requests.");

struct ResourcesEntry {
  const char* path_;
  const char* resource_;
  int length_;
};

extern ResourcesEntry __service_resources_[];

class Resources {
 public:
  static const int kNoSuchInstance = -1;
  static int ResourceLookup(const char* path, const char** resource) {
    ResourcesEntry* table = ResourceTable();
    for (int i = 0; table[i].path_ != NULL; i++) {
      const ResourcesEntry& entry = table[i];
      if (strcmp(path, entry.path_) == 0) {
        *resource = entry.resource_;
        ASSERT(entry.length_ > 0);
        return entry.length_;
      }
    }
    return kNoSuchInstance;
  }

  static const char* Path(int idx) {
    ASSERT(idx >= 0);
    ResourcesEntry* entry = At(idx);
    if (entry == NULL) {
      return NULL;
    }
    ASSERT(entry->path_ != NULL);
    return entry->path_;
  }

  static int Length(int idx) {
    ASSERT(idx >= 0);
    ResourcesEntry* entry = At(idx);
    if (entry == NULL) {
      return kNoSuchInstance;
    }
    ASSERT(entry->path_ != NULL);
    return entry->length_;
  }

  static const uint8_t* Resource(int idx) {
    ASSERT(idx >= 0);
    ResourcesEntry* entry = At(idx);
    if (entry == NULL) {
      return NULL;
    }
    return reinterpret_cast<const uint8_t*>(entry->resource_);
  }

 private:
  static ResourcesEntry* At(int idx) {
    ASSERT(idx >= 0);
    ResourcesEntry* table = ResourceTable();
    for (int i = 0; table[i].path_ != NULL; i++) {
      if (idx == i) {
        return &table[i];
      }
    }
    return NULL;
  }

  static ResourcesEntry* ResourceTable() {
    return &__service_resources_[0];
  }

  DISALLOW_ALLOCATION();
  DISALLOW_IMPLICIT_CONSTRUCTORS(Resources);
};


class EmbedderServiceHandler {
 public:
  explicit EmbedderServiceHandler(const char* name) : name_(NULL),
                                                      callback_(NULL),
                                                      user_data_(NULL),
                                                      next_(NULL) {
    ASSERT(name != NULL);
    name_ = strdup(name);
  }

  ~EmbedderServiceHandler() {
    free(name_);
  }

  const char* name() const { return name_; }

  Dart_ServiceRequestCallback callback() const { return callback_; }
  void set_callback(Dart_ServiceRequestCallback callback) {
    callback_ = callback;
  }

  void* user_data() const { return user_data_; }
  void set_user_data(void* user_data) {
    user_data_ = user_data;
  }

  EmbedderServiceHandler* next() const { return next_; }
  void set_next(EmbedderServiceHandler* next) {
    next_ = next;
  }

 private:
  char* name_;
  Dart_ServiceRequestCallback callback_;
  void* user_data_;
  EmbedderServiceHandler* next_;
};


static uint8_t* allocator(uint8_t* ptr, intptr_t old_size, intptr_t new_size) {
  void* new_ptr = realloc(reinterpret_cast<void*>(ptr), new_size);
  return reinterpret_cast<uint8_t*>(new_ptr);
}


static void SendIsolateServiceMessage(Dart_NativeArguments args) {
  NativeArguments* arguments = reinterpret_cast<NativeArguments*>(args);
  Isolate* isolate = arguments->isolate();
  StackZone zone(isolate);
  HANDLESCOPE(isolate);
  GET_NON_NULL_NATIVE_ARGUMENT(Instance, sp, arguments->NativeArgAt(0));
  GET_NON_NULL_NATIVE_ARGUMENT(Instance, message, arguments->NativeArgAt(1));

  // Extract SendPort port id.
  const Object& sp_id_obj = Object::Handle(DartLibraryCalls::PortGetId(sp));
  if (sp_id_obj.IsError()) {
    Exceptions::PropagateError(Error::Cast(sp_id_obj));
  }
  Integer& id = Integer::Handle(isolate);
  id ^= sp_id_obj.raw();
  Dart_Port sp_id = static_cast<Dart_Port>(id.AsInt64Value());
  ASSERT(sp_id != ILLEGAL_PORT);

  // Serialize message.
  uint8_t* data = NULL;
  MessageWriter writer(&data, &allocator);
  writer.WriteMessage(message);

  // TODO(turnidge): Throw an exception when the return value is false?
  PortMap::PostMessage(new Message(sp_id, data, writer.BytesWritten(),
                                   Message::kOOBPriority));
}


static void SendRootServiceMessage(Dart_NativeArguments args) {
  NativeArguments* arguments = reinterpret_cast<NativeArguments*>(args);
  Isolate* isolate = arguments->isolate();
  StackZone zone(isolate);
  HANDLESCOPE(isolate);
  GET_NON_NULL_NATIVE_ARGUMENT(Instance, message, arguments->NativeArgAt(0));
  Service::HandleRootMessage(message);
}


struct VmServiceNativeEntry {
  const char* name;
  int num_arguments;
  Dart_NativeFunction function;
};


static VmServiceNativeEntry _VmServiceNativeEntries[] = {
  {"VMService_SendIsolateServiceMessage", 2, SendIsolateServiceMessage},
  {"VMService_SendRootServiceMessage", 1, SendRootServiceMessage}
};


static Dart_NativeFunction VmServiceNativeResolver(Dart_Handle name,
                                                   int num_arguments,
                                                   bool* auto_setup_scope) {
  const Object& obj = Object::Handle(Api::UnwrapHandle(name));
  if (!obj.IsString()) {
    return NULL;
  }
  const char* function_name = obj.ToCString();
  ASSERT(function_name != NULL);
  ASSERT(auto_setup_scope != NULL);
  *auto_setup_scope = true;
  intptr_t n =
      sizeof(_VmServiceNativeEntries) / sizeof(_VmServiceNativeEntries[0]);
  for (intptr_t i = 0; i < n; i++) {
    VmServiceNativeEntry entry = _VmServiceNativeEntries[i];
    if ((strcmp(function_name, entry.name) == 0) &&
        (num_arguments == entry.num_arguments)) {
      return entry.function;
    }
  }
  return NULL;
}


EmbedderServiceHandler* Service::isolate_service_handler_head_ = NULL;
EmbedderServiceHandler* Service::root_service_handler_head_ = NULL;
Isolate* Service::service_isolate_ = NULL;
Dart_LibraryTagHandler Service::default_handler_ = NULL;
Dart_Port Service::port_ = ILLEGAL_PORT;


static Dart_Port ExtractPort(Dart_Handle receivePort) {
  HANDLESCOPE(Isolate::Current());
  const Object& unwrapped_rp = Object::Handle(Api::UnwrapHandle(receivePort));
  const Instance& rp = Instance::Cast(unwrapped_rp);
  // Extract RawReceivePort port id.
  const Object& rp_id_obj = Object::Handle(DartLibraryCalls::PortGetId(rp));
  if (rp_id_obj.IsError()) {
    return ILLEGAL_PORT;
  }
  ASSERT(rp_id_obj.IsSmi() || rp_id_obj.IsMint());
  const Integer& id = Integer::Cast(rp_id_obj);
  return static_cast<Dart_Port>(id.AsInt64Value());
}


Isolate* Service::GetServiceIsolate(void* callback_data) {
  if (service_isolate_ != NULL) {
    // Already initialized, return service isolate.
    return service_isolate_;
  }
  Dart_ServiceIsolateCreateCalback create_callback =
    Isolate::ServiceCreateCallback();
  if (create_callback == NULL) {
    return NULL;
  }
  Isolate::SetCurrent(NULL);
  char* error = NULL;
  Isolate* isolate = reinterpret_cast<Isolate*>(
      create_callback(callback_data, &error));
  if (isolate == NULL) {
    return NULL;
  }
  // We don't want to pause the service isolate.
  isolate->message_handler()->set_pause_on_start(false);
  isolate->message_handler()->set_pause_on_exit(false);
  Isolate::SetCurrent(isolate);
  {
    // Install the dart:vmservice library.
    StackZone zone(isolate);
    HANDLESCOPE(isolate);
    Library& library =
        Library::Handle(isolate, isolate->object_store()->root_library());
    // Isolate is empty.
    ASSERT(library.IsNull());
    // Grab embedder tag handler.
    default_handler_ = isolate->library_tag_handler();
    ASSERT(default_handler_ != NULL);
    // Temporarily install our own.
    isolate->set_library_tag_handler(LibraryTagHandler);
    // Get script resource.
    const char* resource = NULL;
    const char* path = "/vmservice.dart";
    intptr_t r = Resources::ResourceLookup(path, &resource);
    ASSERT(r != Resources::kNoSuchInstance);
    ASSERT(resource != NULL);
    const String& source_str = String::Handle(
        String::FromUTF8(reinterpret_cast<const uint8_t*>(resource), r));
    ASSERT(!source_str.IsNull());
    const String& url_str = String::Handle(Symbols::DartVMService().raw());
    library ^= Library::LookupLibrary(url_str);
    ASSERT(library.IsNull());
    // Setup library.
    library = Library::New(url_str);
    library.Register();
    const Script& script = Script::Handle(
      isolate, Script::New(url_str, source_str, RawScript::kLibraryTag));
    library.SetLoadInProgress();
    Dart_EnterScope();  // Need to enter scope for tag handler.
    const Error& error = Error::Handle(isolate,
                                       Compiler::Compile(library, script));
    ASSERT(error.IsNull());
    Dart_ExitScope();
    library.SetLoaded();
    // Install embedder default library tag handler again.
    isolate->set_library_tag_handler(default_handler_);
    default_handler_ = NULL;
    library.set_native_entry_resolver(VmServiceNativeResolver);
  }
  {
    // Boot the dart:vmservice library.
    Dart_EnterScope();
    Dart_Handle result;
    Dart_Handle url_str =
        Dart_NewStringFromCString(Symbols::Name(Symbols::kDartVMServiceId));
    Dart_Handle library = Dart_LookupLibrary(url_str);
    ASSERT(Dart_IsLibrary(library));
    result = Dart_Invoke(library, Dart_NewStringFromCString("boot"), 0, NULL);
    ASSERT(!Dart_IsError(result));
    port_ = ExtractPort(result);
    ASSERT(port_ != ILLEGAL_PORT);
    Dart_ExitScope();
  }
  Isolate::SetCurrent(NULL);
  service_isolate_ = reinterpret_cast<Isolate*>(isolate);
  return service_isolate_;
}


// These must be kept in sync with service/constants.dart
#define VM_SERVICE_ISOLATE_STARTUP_MESSAGE_ID 1
#define VM_SERVICE_ISOLATE_SHUTDOWN_MESSAGE_ID 2


static RawArray* MakeServiceControlMessage(Dart_Port port_id, intptr_t code,
                                           const String& name) {
  const Array& list = Array::Handle(Array::New(4));
  ASSERT(!list.IsNull());
  const Integer& code_int = Integer::Handle(Integer::New(code));
  const Integer& port_int = Integer::Handle(Integer::New(port_id));
  const Object& send_port = Object::Handle(
      DartLibraryCalls::NewSendPort(port_id));
  ASSERT(!send_port.IsNull());
  list.SetAt(0, code_int);
  list.SetAt(1, port_int);
  list.SetAt(2, send_port);
  list.SetAt(3, name);
  return list.raw();
}


bool Service::SendIsolateStartupMessage() {
  if (!IsRunning()) {
    return false;
  }
  Isolate* isolate = Isolate::Current();
  ASSERT(isolate != NULL);
  HANDLESCOPE(isolate);
  const String& name = String::Handle(String::New(isolate->name()));
  ASSERT(!name.IsNull());
  const Array& list = Array::Handle(
      MakeServiceControlMessage(Dart_GetMainPortId(),
                                VM_SERVICE_ISOLATE_STARTUP_MESSAGE_ID,
                                name));
  ASSERT(!list.IsNull());
  uint8_t* data = NULL;
  MessageWriter writer(&data, &allocator);
  writer.WriteMessage(list);
  intptr_t len = writer.BytesWritten();
  if (FLAG_trace_service) {
    OS::Print("Isolate %s %" Pd64 " registered with service \n",
              name.ToCString(),
              Dart_GetMainPortId());
  }
  return PortMap::PostMessage(
      new Message(port_, data, len, Message::kNormalPriority));
}


bool Service::SendIsolateShutdownMessage() {
  if (!IsRunning()) {
    return false;
  }
  Isolate* isolate = Isolate::Current();
  ASSERT(isolate != NULL);
  HANDLESCOPE(isolate);
  const String& name = String::Handle(String::New(isolate->name()));
  ASSERT(!name.IsNull());
  const Array& list = Array::Handle(
      MakeServiceControlMessage(Dart_GetMainPortId(),
                                VM_SERVICE_ISOLATE_SHUTDOWN_MESSAGE_ID,
                                name));
  ASSERT(!list.IsNull());
  uint8_t* data = NULL;
  MessageWriter writer(&data, &allocator);
  writer.WriteMessage(list);
  intptr_t len = writer.BytesWritten();
  if (FLAG_trace_service) {
    OS::Print("Isolate %s %" Pd64 " deregistered with service \n",
              name.ToCString(),
              Dart_GetMainPortId());
  }
  return PortMap::PostMessage(
      new Message(port_, data, len, Message::kNormalPriority));
}


bool Service::IsRunning() {
  return port_ != ILLEGAL_PORT;
}


Dart_Handle Service::GetSource(const char* name) {
  ASSERT(name != NULL);
  int i = 0;
  while (true) {
    const char* path = Resources::Path(i);
    if (path == NULL) {
      break;
    }
    ASSERT(*path != '\0');
    // Skip the '/'.
    path++;
    if (strcmp(name, path) == 0) {
      const uint8_t* str = Resources::Resource(i);
      intptr_t length = Resources::Length(i);
      return Dart_NewStringFromUTF8(str, length);
    }
    i++;
  }
  return Dart_Null();
}


Dart_Handle Service::LibraryTagHandler(Dart_LibraryTag tag, Dart_Handle library,
                                       Dart_Handle url) {
  if (!Dart_IsLibrary(library)) {
    return Dart_NewApiError("not a library");
  }
  if (!Dart_IsString(url)) {
    return Dart_NewApiError("url is not a string");
  }
  const char* url_string = NULL;
  Dart_Handle result = Dart_StringToCString(url, &url_string);
  if (Dart_IsError(result)) {
    return result;
  }
  if (tag == Dart_kImportTag) {
    // Embedder handles all requests for external libraries.
    ASSERT(default_handler_ != NULL);
    return default_handler_(tag, library, url);
  }
  ASSERT((tag == Dart_kSourceTag) || (tag == Dart_kCanonicalizeUrl));
  if (tag == Dart_kCanonicalizeUrl) {
    // url is already canonicalized.
    return url;
  }
  Dart_Handle source = GetSource(url_string);
  if (Dart_IsError(source)) {
    return source;
  }
  return Dart_LoadSource(library, url, source);
}


// A handler for a per-isolate request.
//
// If a handler returns true, the reply is complete and ready to be
// posted.  If a handler returns false, then it is responsible for
// posting the reply (this can be used for asynchronous delegation of
// the response handling).
typedef bool (*IsolateMessageHandler)(Isolate* isolate, JSONStream* stream);

struct IsolateMessageHandlerEntry {
  const char* command;
  IsolateMessageHandler handler;
};

static IsolateMessageHandler FindIsolateMessageHandler(const char* command);


// A handler for a root (vm-global) request.
//
// If a handler returns true, the reply is complete and ready to be
// posted.  If a handler returns false, then it is responsible for
// posting the reply (this can be used for asynchronous delegation of
// the response handling).
typedef bool (*RootMessageHandler)(JSONStream* stream);

struct RootMessageHandlerEntry {
  const char* command;
  RootMessageHandler handler;
};

static RootMessageHandler FindRootMessageHandler(const char* command);


static void PrintArgumentsAndOptions(const JSONObject& obj, JSONStream* js) {
  JSONObject jsobj(&obj, "request");
  {
    JSONArray jsarr(&jsobj, "arguments");
    for (intptr_t i = 0; i < js->num_arguments(); i++) {
      jsarr.AddValue(js->GetArgument(i));
    }
  }
  {
    JSONArray jsarr(&jsobj, "option_keys");
    for (intptr_t i = 0; i < js->num_options(); i++) {
      jsarr.AddValue(js->GetOptionKey(i));
    }
  }
  {
    JSONArray jsarr(&jsobj, "option_values");
    for (intptr_t i = 0; i < js->num_options(); i++) {
      jsarr.AddValue(js->GetOptionValue(i));
    }
  }
}


static void PrintError(JSONStream* js,
                       const char* format, ...) {
  Isolate* isolate = Isolate::Current();

  va_list args;
  va_start(args, format);
  intptr_t len = OS::VSNPrint(NULL, 0, format, args);
  va_end(args);

  char* buffer = isolate->current_zone()->Alloc<char>(len + 1);
  va_list args2;
  va_start(args2, format);
  OS::VSNPrint(buffer, (len + 1), format, args2);
  va_end(args2);

  JSONObject jsobj(js);
  jsobj.AddProperty("type", "Error");
  jsobj.AddProperty("id", "");
  jsobj.AddProperty("message", buffer);
  PrintArgumentsAndOptions(jsobj, js);
}


static void PrintErrorWithKind(JSONStream* js,
                               const char* kind,
                               const char* format, ...) {
  Isolate* isolate = Isolate::Current();

  va_list args;
  va_start(args, format);
  intptr_t len = OS::VSNPrint(NULL, 0, format, args);
  va_end(args);

  char* buffer = isolate->current_zone()->Alloc<char>(len + 1);
  va_list args2;
  va_start(args2, format);
  OS::VSNPrint(buffer, (len + 1), format, args2);
  va_end(args2);

  JSONObject jsobj(js);
  jsobj.AddProperty("type", "Error");
  jsobj.AddProperty("id", "");
  jsobj.AddProperty("kind", kind);
  jsobj.AddProperty("message", buffer);
  PrintArgumentsAndOptions(jsobj, js);
}


void Service::HandleIsolateMessage(Isolate* isolate, const Instance& msg) {
  ASSERT(isolate != NULL);
  ASSERT(!msg.IsNull());
  ASSERT(msg.IsGrowableObjectArray());

  {
    StackZone zone(isolate);
    HANDLESCOPE(isolate);

    const GrowableObjectArray& message = GrowableObjectArray::Cast(msg);
    // Message is a list with four entries.
    ASSERT(message.Length() == 4);

    Instance& reply_port = Instance::Handle(isolate);
    GrowableObjectArray& path = GrowableObjectArray::Handle(isolate);
    GrowableObjectArray& option_keys = GrowableObjectArray::Handle(isolate);
    GrowableObjectArray& option_values = GrowableObjectArray::Handle(isolate);
    reply_port ^= message.At(0);
    path ^= message.At(1);
    option_keys ^= message.At(2);
    option_values ^= message.At(3);

    ASSERT(!path.IsNull());
    ASSERT(!option_keys.IsNull());
    ASSERT(!option_values.IsNull());
    // Same number of option keys as values.
    ASSERT(option_keys.Length() == option_values.Length());

    String& path_segment = String::Handle();
    if (path.Length() > 0) {
      path_segment ^= path.At(0);
    } else {
      path_segment ^= Symbols::Empty().raw();
    }
    ASSERT(!path_segment.IsNull());
    const char* path_segment_c = path_segment.ToCString();

    IsolateMessageHandler handler =
        FindIsolateMessageHandler(path_segment_c);
    {
      JSONStream js;
      js.Setup(zone.GetZone(), reply_port, path, option_keys, option_values);
      if (handler == NULL) {
        // Check for an embedder handler.
        EmbedderServiceHandler* e_handler =
            FindIsolateEmbedderHandler(path_segment_c);
        if (e_handler != NULL) {
          EmbedderHandleMessage(e_handler, &js);
        } else {
          PrintError(&js, "Unrecognized path");
        }
        js.PostReply();
      } else {
        if (handler(isolate, &js)) {
          // Handler returns true if the reply is ready to be posted.
          // TODO(johnmccutchan): Support asynchronous replies.
          js.PostReply();
        }
      }
    }
  }
}


static bool HandleIsolate(Isolate* isolate, JSONStream* js) {
  isolate->PrintToJSONStream(js, false);
  return true;
}


static bool HandleStackTrace(Isolate* isolate, JSONStream* js) {
  if (js->num_arguments() > 1) {
    PrintError(js, "Command too long");
    return true;
  }
  DebuggerStackTrace* stack = isolate->debugger()->StackTrace();
  JSONObject jsobj(js);
  jsobj.AddProperty("type", "StackTrace");
  jsobj.AddProperty("id", "stacktrace");
  JSONArray jsarr(&jsobj, "members");
  intptr_t num_frames = stack->Length();
  for (intptr_t i = 0; i < num_frames; i++) {
    ActivationFrame* frame = stack->FrameAt(i);
    JSONObject jsobj(&jsarr);
    frame->PrintToJSONObject(&jsobj);
    // TODO(turnidge): Implement depth differently -- differentiate
    // inlined frames.
    jsobj.AddProperty("depth", i);
  }
  return true;
}


static bool HandleIsolateEcho(Isolate* isolate, JSONStream* js) {
  JSONObject jsobj(js);
  jsobj.AddProperty("type", "message");
  PrintArgumentsAndOptions(jsobj, js);
  return true;
}


// Print an error message if there is no ID argument.
#define REQUIRE_COLLECTION_ID(collection)                                      \
  if (js->num_arguments() == 1) {                                              \
    PrintError(js, "Must specify collection object id: /%s/id", collection);   \
    return true;                                                               \
  }


#define CHECK_COLLECTION_ID_BOUNDS(collection, length, arg, id, js)            \
  if (!GetIntegerId(arg, &id)) {                                               \
    PrintError(js, "Must specify collection object id: %s/id", collection);    \
    return true;                                                               \
  }                                                                            \
  if ((id < 0) || (id >= length)) {                                            \
    PrintError(js, "%s id (%" Pd ") must be in [0, %" Pd ").", collection, id, \
                                                               length);        \
    return true;                                                               \
  }


static bool GetIntegerId(const char* s, intptr_t* id, int base = 10) {
  if ((s == NULL) || (*s == '\0')) {
    // Empty string.
    return false;
  }
  if (id == NULL) {
    // No id pointer.
    return false;
  }
  intptr_t r = 0;
  char* end_ptr = NULL;
  r = strtol(s, &end_ptr, base);
  if (end_ptr == s) {
    // String was not advanced at all, cannot be valid.
    return false;
  }
  *id = r;
  return true;
}


static bool GetUnsignedIntegerId(const char* s, uintptr_t* id, int base = 10) {
  if ((s == NULL) || (*s == '\0')) {
    // Empty string.
    return false;
  }
  if (id == NULL) {
    // No id pointer.
    return false;
  }
  uintptr_t r = 0;
  char* end_ptr = NULL;
  r = strtoul(s, &end_ptr, base);
  if (end_ptr == s) {
    // String was not advanced at all, cannot be valid.
    return false;
  }
  *id = r;
  return true;
}


static bool GetInteger64Id(const char* s, int64_t* id, int base = 10) {
  if ((s == NULL) || (*s == '\0')) {
    // Empty string.
    return false;
  }
  if (id == NULL) {
    // No id pointer.
    return false;
  }
  int64_t r = 0;
  char* end_ptr = NULL;
  r = strtoll(s, &end_ptr, base);
  if (end_ptr == s) {
    // String was not advanced at all, cannot be valid.
    return false;
  }
  *id = r;
  return true;
}

// Scans the string until the '-' character. Returns pointer to string
// at '-' character. Returns NULL if not found.
const char* ScanUntilDash(const char* s) {
  if ((s == NULL) || (*s == '\0')) {
    // Empty string.
    return NULL;
  }
  while (*s != '\0') {
    if (*s == '-') {
      return s;
    }
    s++;
  }
  return NULL;
}


static bool GetCodeId(const char* s, int64_t* timestamp, uword* address) {
  if ((s == NULL) || (*s == '\0')) {
    // Empty string.
    return false;
  }
  if ((timestamp == NULL) || (address == NULL)) {
    // Bad arguments.
    return false;
  }
  // Extract the timestamp.
  if (!GetInteger64Id(s, timestamp, 16) || (*timestamp < 0)) {
    return false;
  }
  s = ScanUntilDash(s);
  if (s == NULL) {
    return false;
  }
  // Skip the dash.
  s++;
  // Extract the PC.
  if (!GetUnsignedIntegerId(s, address, 16)) {
    return false;
  }
  return true;
}


static bool HandleInstanceCommands(Isolate* isolate,
                                   const Object& obj,
                                   JSONStream* js,
                                   intptr_t arg_pos) {
  ASSERT(js->num_arguments() > arg_pos);
  const char* action = js->GetArgument(arg_pos);
  if (strcmp(action, "eval") == 0) {
    if (js->num_arguments() > (arg_pos + 1)) {
      PrintError(js, "expected at most %" Pd " arguments but found %" Pd "\n",
                 arg_pos + 1,
                 js->num_arguments());
      return true;
    }
    if (obj.IsNull()) {
      PrintErrorWithKind(js, "EvalCollected",
                         "attempt to evaluate against collected object\n",
                         js->num_arguments());
      return true;
    }
    if (obj.raw() == Object::sentinel().raw()) {
      PrintErrorWithKind(js, "EvalExpired",
                         "attempt to evaluate against expired object\n",
                         js->num_arguments());
      return true;
    }
    const char* expr = js->LookupOption("expr");
    if (expr == NULL) {
      PrintError(js, "eval expects an 'expr' option\n",
                 js->num_arguments());
      return true;
    }
    const String& expr_str = String::Handle(isolate, String::New(expr));
    ASSERT(obj.IsInstance());
    const Instance& instance = Instance::Cast(obj);
    const Object& result = Object::Handle(instance.Evaluate(expr_str));
    if (result.IsNull()) {
      Object::null_instance().PrintToJSONStream(js, true);
    } else {
      result.PrintToJSONStream(js, true);
    }
    return true;
  }

  PrintError(js, "unrecognized action '%s'\n", action);
  return true;
}


static bool HandleClassesClosures(Isolate* isolate, const Class& cls,
                                  JSONStream* js) {
  intptr_t id;
  if (js->num_arguments() > 4) {
    PrintError(js, "Command too long");
    return true;
  }
  if (!GetIntegerId(js->GetArgument(3), &id)) {
    PrintError(js, "Must specify collection object id: closures/id");
    return true;
  }
  Function& func = Function::Handle();
  func ^= cls.ClosureFunctionFromIndex(id);
  if (func.IsNull()) {
    PrintError(js, "Closure function %" Pd " not found", id);
    return true;
  }
  func.PrintToJSONStream(js, false);
  return true;
}


static bool HandleClassesEval(Isolate* isolate, const Class& cls,
                              JSONStream* js) {
  if (js->num_arguments() > 3) {
    PrintError(js, "Command too long");
    return true;
  }
  const char* expr = js->LookupOption("expr");
  if (expr == NULL) {
    PrintError(js, "eval expects an 'expr' option\n",
               js->num_arguments());
    return true;
  }
  const String& expr_str = String::Handle(isolate, String::New(expr));
  const Object& result = Object::Handle(cls.Evaluate(expr_str));
  if (result.IsNull()) {
    Object::null_instance().PrintToJSONStream(js, true);
  } else {
    result.PrintToJSONStream(js, true);
  }
  return true;
}


static bool HandleClassesDispatchers(Isolate* isolate, const Class& cls,
                                     JSONStream* js) {
  intptr_t id;
  if (js->num_arguments() > 4) {
    PrintError(js, "Command too long");
    return true;
  }
  if (!GetIntegerId(js->GetArgument(3), &id)) {
    PrintError(js, "Must specify collection object id: dispatchers/id");
    return true;
  }
  Function& func = Function::Handle();
  func ^= cls.InvocationDispatcherFunctionFromIndex(id);
  if (func.IsNull()) {
    PrintError(js, "Dispatcher %" Pd " not found", id);
    return true;
  }
  func.PrintToJSONStream(js, false);
  return true;
}


static bool HandleClassesFunctions(Isolate* isolate, const Class& cls,
                                   JSONStream* js) {
  intptr_t id;
  if (js->num_arguments() > 4) {
    PrintError(js, "Command too long");
    return true;
  }
  if (!GetIntegerId(js->GetArgument(3), &id)) {
    PrintError(js, "Must specify collection object id: functions/id");
    return true;
  }
  Function& func = Function::Handle();
  func ^= cls.FunctionFromIndex(id);
  if (func.IsNull()) {
    PrintError(js, "Function %" Pd " not found", id);
    return true;
  }
  func.PrintToJSONStream(js, false);
  return true;
}


static bool HandleClassesImplicitClosures(Isolate* isolate, const Class& cls,
                                          JSONStream* js) {
  intptr_t id;
  if (js->num_arguments() > 4) {
    PrintError(js, "Command too long");
    return true;
  }
  if (!GetIntegerId(js->GetArgument(3), &id)) {
    PrintError(js, "Must specify collection object id: implicit_closures/id");
    return true;
  }
  Function& func = Function::Handle();
  func ^= cls.ImplicitClosureFunctionFromIndex(id);
  if (func.IsNull()) {
    PrintError(js, "Implicit closure function %" Pd " not found", id);
    return true;
  }
  func.PrintToJSONStream(js, false);
  return true;
}


static bool HandleClassesFields(Isolate* isolate, const Class& cls,
                                JSONStream* js) {
  intptr_t id;
  if (js->num_arguments() > 4) {
    PrintError(js, "Command too long");
    return true;
  }
  if (!GetIntegerId(js->GetArgument(3), &id)) {
    PrintError(js, "Must specify collection object id: fields/id");
    return true;
  }
  Field& field = Field::Handle(cls.FieldFromIndex(id));
  if (field.IsNull()) {
    PrintError(js, "Field %" Pd " not found", id);
    return true;
  }
  field.PrintToJSONStream(js, false);
  return true;
}


static bool HandleClassesTypes(Isolate* isolate, const Class& cls,
                               JSONStream* js) {
  if (js->num_arguments() == 3) {
    JSONObject jsobj(js);
    jsobj.AddProperty("type", "TypeList");
    JSONArray members(&jsobj, "members");
    const intptr_t num_types = cls.NumCanonicalTypes();
    Type& type = Type::Handle();
    for (intptr_t i = 0; i < num_types; i++) {
      type = cls.CanonicalTypeFromIndex(i);
      members.AddValue(type);
    }
    return true;
  }
  ASSERT(js->num_arguments() >= 4);
  intptr_t id;
  if (!GetIntegerId(js->GetArgument(3), &id)) {
    PrintError(js, "Must specify collection object id: types/id");
    return true;
  }
  Type& type = Type::Handle();
  type ^= cls.CanonicalTypeFromIndex(id);
  if (type.IsNull()) {
    PrintError(js, "Canonical type %" Pd " not found", id);
    return true;
  }
  if (js->num_arguments() == 4) {
    type.PrintToJSONStream(js, false);
    return true;
  }
  return HandleInstanceCommands(isolate, type, js, 4);
}


static bool HandleClasses(Isolate* isolate, JSONStream* js) {
  if (js->num_arguments() == 1) {
    ClassTable* table = isolate->class_table();
    JSONObject jsobj(js);
    table->PrintToJSONObject(&jsobj);
    return true;
  }
  ASSERT(js->num_arguments() >= 2);
  intptr_t id;
  if (!GetIntegerId(js->GetArgument(1), &id)) {
    PrintError(js, "Must specify collection object id: /classes/id");
    return true;
  }
  ClassTable* table = isolate->class_table();
  if (!table->IsValidIndex(id)) {
    PrintError(js, "%" Pd " is not a valid class id.", id);
    return true;
  }
  Class& cls = Class::Handle(table->At(id));
  if (js->num_arguments() == 2) {
    cls.PrintToJSONStream(js, false);
    return true;
  } else if (js->num_arguments() >= 3) {
    const char* second = js->GetArgument(2);
    if (strcmp(second, "eval") == 0) {
      return HandleClassesEval(isolate, cls, js);
    } else if (strcmp(second, "closures") == 0) {
      return HandleClassesClosures(isolate, cls, js);
    } else if (strcmp(second, "fields") == 0) {
      return HandleClassesFields(isolate, cls, js);
    } else if (strcmp(second, "functions") == 0) {
      return HandleClassesFunctions(isolate, cls, js);
    } else if (strcmp(second, "implicit_closures") == 0) {
      return HandleClassesImplicitClosures(isolate, cls, js);
    } else if (strcmp(second, "dispatchers") == 0) {
      return HandleClassesDispatchers(isolate, cls, js);
    } else if (!strcmp(second, "types")) {
      return HandleClassesTypes(isolate, cls, js);
    } else {
      PrintError(js, "Invalid sub collection %s", second);
      return true;
    }
  }
  UNREACHABLE();
  return true;
}


static bool HandleLibrariesEval(Isolate* isolate, const Library& lib,
                                JSONStream* js) {
  if (js->num_arguments() > 3) {
    PrintError(js, "Command too long");
    return true;
  }
  const char* expr = js->LookupOption("expr");
  if (expr == NULL) {
    PrintError(js, "eval expects an 'expr' option\n",
               js->num_arguments());
    return true;
  }
  const String& expr_str = String::Handle(isolate, String::New(expr));
  const Object& result = Object::Handle(lib.Evaluate(expr_str));
  if (result.IsNull()) {
    Object::null_instance().PrintToJSONStream(js, true);
  } else {
    result.PrintToJSONStream(js, true);
  }
  return true;
}


static bool HandleLibraries(Isolate* isolate, JSONStream* js) {
  // TODO(johnmccutchan): Support fields and functions on libraries.
  REQUIRE_COLLECTION_ID("libraries");
  const GrowableObjectArray& libs =
      GrowableObjectArray::Handle(isolate->object_store()->libraries());
  ASSERT(!libs.IsNull());
  intptr_t id = 0;
  CHECK_COLLECTION_ID_BOUNDS("libraries", libs.Length(), js->GetArgument(1),
                             id, js);
  Library& lib = Library::Handle();
  lib ^= libs.At(id);
  ASSERT(!lib.IsNull());
  if (js->num_arguments() == 2) {
    lib.PrintToJSONStream(js, false);
    return true;
  } else if (js->num_arguments() >= 3) {
    const char* second = js->GetArgument(2);
    if (strcmp(second, "eval") == 0) {
      return HandleLibrariesEval(isolate, lib, js);
    } else {
      PrintError(js, "Invalid sub collection %s", second);
      return true;
    }
  }
  UNREACHABLE();
  return true;
}


static void PrintPseudoNull(JSONStream* js,
                            const char* id,
                            const char* preview) {
  JSONObject jsobj(js);
  jsobj.AddProperty("type", "Null");
  jsobj.AddProperty("id", id);
  jsobj.AddProperty("valueAsString", preview);
}


static RawObject* LookupObjectId(Isolate* isolate,
                                 const char* arg,
                                 bool* error) {
  *error = false;
  if (strncmp(arg, "int-", 4) == 0) {
    arg += 4;
    int64_t value = 0;
    if (!OS::StringToInt64(arg, &value) ||
        !Smi::IsValid64(value)) {
      *error = true;
      return Object::null();
    }
    const Integer& obj =
        Integer::Handle(isolate, Smi::New(static_cast<intptr_t>(value)));
    return obj.raw();

  } else if (strcmp(arg, "bool-true") == 0) {
    return Bool::True().raw();

  } else if (strcmp(arg, "bool-false") == 0) {
    return Bool::False().raw();
  }

  ObjectIdRing* ring = isolate->object_id_ring();
  ASSERT(ring != NULL);
  intptr_t id = -1;
  if (!GetIntegerId(arg, &id)) {
    *error = true;
    return Instance::null();
  }
  return ring->GetObjectForId(id);
}


static bool HandleObjects(Isolate* isolate, JSONStream* js) {
  REQUIRE_COLLECTION_ID("objects");
  if (js->num_arguments() < 2) {
    PrintError(js, "expected at least 2 arguments but found %" Pd "\n",
               js->num_arguments());
    return true;
  }
  const char* arg = js->GetArgument(1);

  // Handle special objects first.
  if (strcmp(arg, "null") == 0) {
    if (js->num_arguments() > 2) {
      PrintError(js, "expected at most 2 arguments but found %" Pd "\n",
                 js->num_arguments());
    } else {
      Instance::null_instance().PrintToJSONStream(js, false);
    }
    return true;

  } else if (strcmp(arg, "not-initialized") == 0) {
    if (js->num_arguments() > 2) {
      PrintError(js, "expected at most 2 arguments but found %" Pd "\n",
                 js->num_arguments());
    } else {
      Object::sentinel().PrintToJSONStream(js, false);
    }
    return true;

  } else if (strcmp(arg, "being-initialized") == 0) {
    if (js->num_arguments() > 2) {
      PrintError(js, "expected at most 2 arguments but found %" Pd "\n",
                 js->num_arguments());
    } else {
      Object::transition_sentinel().PrintToJSONStream(js, false);
    }
    return true;

  } else if (strcmp(arg, "optimized-out") == 0) {
    if (js->num_arguments() > 2) {
      PrintError(js, "expected at most 2 arguments but found %" Pd "\n",
                 js->num_arguments());
    } else {
      Symbols::OptimizedOut().PrintToJSONStream(js, false);
    }
    return true;

  } else if (strcmp(arg, "collected") == 0) {
    if (js->num_arguments() > 2) {
      PrintError(js, "expected at most 2 arguments but found %" Pd "\n",
                 js->num_arguments());
    } else {
      PrintPseudoNull(js, "objects/collected", "<collected>");
    }
    return true;

  } else if (strcmp(arg, "expired") == 0) {
    if (js->num_arguments() > 2) {
      PrintError(js, "expected at most 2 arguments but found %" Pd "\n",
                 js->num_arguments());
    } else {
      PrintPseudoNull(js, "objects/expired", "<expired>");
    }
    return true;
  }

  // Lookup the object.
  Object& obj = Object::Handle(isolate);
  bool error = false;
  obj = LookupObjectId(isolate, arg, &error);
  if (error) {
    PrintError(js, "unrecognized object id '%s'", arg);
    return true;
  }
  if (js->num_arguments() == 2) {
    // Print.
    if (obj.IsNull()) {
      // The object has been collected by the gc.
      PrintPseudoNull(js, "objects/collected", "<collected>");
      return true;
    } else if (obj.raw() == Object::sentinel().raw()) {
      // The object id has expired.
      PrintPseudoNull(js, "objects/expired", "<expired>");
      return true;
    }
    obj.PrintToJSONStream(js, false);
    return true;
  }
  return HandleInstanceCommands(isolate, obj, js, 2);
}


static bool HandleScriptsEnumerate(Isolate* isolate, JSONStream* js) {
  JSONObject jsobj(js);
  jsobj.AddProperty("type", "ScriptList");
  JSONArray members(&jsobj, "members");
  const GrowableObjectArray& libs =
      GrowableObjectArray::Handle(isolate->object_store()->libraries());
  int num_libs = libs.Length();
  Library &lib = Library::Handle();
  Script& script = Script::Handle();
  for (intptr_t i = 0; i < num_libs; i++) {
    lib ^= libs.At(i);
    ASSERT(!lib.IsNull());
    ASSERT(Smi::IsValid(lib.index()));
    const Array& loaded_scripts = Array::Handle(lib.LoadedScripts());
    ASSERT(!loaded_scripts.IsNull());
    intptr_t num_scripts = loaded_scripts.Length();
    for (intptr_t i = 0; i < num_scripts; i++) {
      script ^= loaded_scripts.At(i);
      members.AddValue(script);
    }
  }
  return true;
}


static bool HandleScriptsFetch(Isolate* isolate, JSONStream* js) {
  const GrowableObjectArray& libs =
    GrowableObjectArray::Handle(isolate->object_store()->libraries());
  int num_libs = libs.Length();
  Library &lib = Library::Handle();
  Script& script = Script::Handle();
  String& url = String::Handle();
  const String& id = String::Handle(String::New(js->GetArgument(1)));
  ASSERT(!id.IsNull());
  // The id is the url of the script % encoded, decode it.
  String& requested_url = String::Handle(String::DecodeURI(id));
  for (intptr_t i = 0; i < num_libs; i++) {
    lib ^= libs.At(i);
    ASSERT(!lib.IsNull());
    ASSERT(Smi::IsValid(lib.index()));
    const Array& loaded_scripts = Array::Handle(lib.LoadedScripts());
    ASSERT(!loaded_scripts.IsNull());
    intptr_t num_scripts = loaded_scripts.Length();
    for (intptr_t i = 0; i < num_scripts; i++) {
      script ^= loaded_scripts.At(i);
      ASSERT(!script.IsNull());
      url ^= script.url();
      if (url.Equals(requested_url)) {
        script.PrintToJSONStream(js, false);
        return true;
      }
    }
  }
  PrintErrorWithKind(js, "NotFoundError", "Cannot find script %s",
                     requested_url.ToCString());
  return true;
}


static bool HandleScripts(Isolate* isolate, JSONStream* js) {
  if (js->num_arguments() == 1) {
    // Enumerate all scripts.
    return HandleScriptsEnumerate(isolate, js);
  } else if (js->num_arguments() == 2) {
    // Fetch specific script.
    return HandleScriptsFetch(isolate, js);
  } else {
    PrintError(js, "Command too long");
    return true;
  }
}


static bool HandleDebug(Isolate* isolate, JSONStream* js) {
  if (js->num_arguments() == 1) {
    PrintError(js, "Must specify a subcommand");
    return true;
  }
  const char* command = js->GetArgument(1);
  if (strcmp(command, "breakpoints") == 0) {
    if (js->num_arguments() == 2) {
      // Print breakpoint list.
      JSONObject jsobj(js);
      jsobj.AddProperty("type", "BreakpointList");
      JSONArray jsarr(&jsobj, "breakpoints");
      isolate->debugger()->PrintBreakpointsToJSONArray(&jsarr);
      return true;
    } else if (js->num_arguments() == 3) {
      // Print individual breakpoint.
      intptr_t id = 0;
      SourceBreakpoint* bpt = NULL;
      if (GetIntegerId(js->GetArgument(2), &id)) {
        bpt = isolate->debugger()->GetBreakpointById(id);
      }
      if (bpt != NULL) {
        bpt->PrintToJSONStream(js);
        return true;
      } else {
        PrintError(js, "Unrecognized breakpoint id %s", js->GetArgument(2));
        return true;
      }
    } else {
      PrintError(js, "Command too long");
      return true;
    }
  } else {
    PrintError(js, "Unrecognized subcommand '%s'", js->GetArgument(1));
    return true;
  }
}


static bool HandleNullCode(uintptr_t pc, JSONStream* js) {
  // TODO(turnidge): Consider adding/using Object::null_code() for
  // consistent "type".
  Object::null_object().PrintToJSONStream(js, false);
  return true;
}


static bool HandleCode(Isolate* isolate, JSONStream* js) {
  REQUIRE_COLLECTION_ID("code");
  uword pc;
  if (js->num_arguments() > 2) {
    PrintError(js, "Command too long");
    return true;
  }
  ASSERT(js->num_arguments() == 2);
  static const char* kCollectedPrefix = "collected-";
  static intptr_t kCollectedPrefixLen = strlen(kCollectedPrefix);
  static const char* kNativePrefix = "native-";
  static intptr_t kNativePrefixLen = strlen(kNativePrefix);
  static const char* kReusedPrefix = "reused-";
  static intptr_t kReusedPrefixLen = strlen(kReusedPrefix);
  const char* command = js->GetArgument(1);
  if (strncmp(kCollectedPrefix, command, kCollectedPrefixLen) == 0) {
    if (!GetUnsignedIntegerId(&command[kCollectedPrefixLen], &pc, 16)) {
      PrintError(js, "Must specify code address: code/%sc0deadd0.",
                 kCollectedPrefix);
      return true;
    }
    return HandleNullCode(pc, js);
  }
  if (strncmp(kNativePrefix, command, kNativePrefixLen) == 0) {
    if (!GetUnsignedIntegerId(&command[kNativePrefixLen], &pc, 16)) {
      PrintError(js, "Must specify code address: code/%sc0deadd0.",
                 kNativePrefix);
      return true;
    }
    // TODO(johnmccutchan): Support native Code.
    return HandleNullCode(pc, js);
  }
  if (strncmp(kReusedPrefix, command, kReusedPrefixLen) == 0) {
    if (!GetUnsignedIntegerId(&command[kReusedPrefixLen], &pc, 16)) {
      PrintError(js, "Must specify code address: code/%sc0deadd0.",
                 kReusedPrefix);
      return true;
    }
    return HandleNullCode(pc, js);
  }
  int64_t timestamp = 0;
  if (!GetCodeId(command, &timestamp, &pc) || (timestamp < 0)) {
    PrintError(js, "Malformed code id: %s", command);
    return true;
  }
  Code& code = Code::Handle(Code::FindCode(pc, timestamp));
  if (!code.IsNull()) {
    code.PrintToJSONStream(js, false);
    return true;
  }
  PrintError(js, "Could not find code with id: %s", command);
  return true;
}


static bool HandleProfile(Isolate* isolate, JSONStream* js) {
  if (js->num_arguments() == 2) {
    const char* sub_command = js->GetArgument(1);
    if (!strcmp(sub_command, "tag")) {
      {
        JSONObject miniProfile(js);
        miniProfile.AddProperty("type", "TagProfile");
        miniProfile.AddProperty("id", "profile/tag");
        isolate->vm_tag_counters()->PrintToJSONObject(&miniProfile);
      }
      return true;
    } else {
      PrintError(js, "Unrecognized subcommand '%s'", sub_command);
      return true;
    }
  }
  // A full profile includes disassembly of all Dart code objects.
  // TODO(johnmccutchan): Add sub command to trigger full code dump.
  bool full_profile = false;
  const char* tags_option = js->LookupOption("tags");
  bool use_tags = true;
  if (tags_option != NULL) {
    if (strcmp("hide", tags_option) != 0) {
      PrintError(js, "Invalid tags option value: %s\n", tags_option);
      return true;
    }
    use_tags = false;
  }
  Profiler::PrintToJSONStream(isolate, js, full_profile, use_tags);
  return true;
}

static bool HandleCoverage(Isolate* isolate, JSONStream* js) {
  CodeCoverage::PrintToJSONStream(isolate, js);
  return true;
}


static bool HandleAllocationProfile(Isolate* isolate, JSONStream* js) {
  bool should_reset_accumulator = false;
  bool should_collect = false;
  if (js->num_arguments() != 1) {
    PrintError(js, "Command too long");
    return true;
  }
  if (js->HasOption("reset")) {
    if (js->OptionIs("reset", "true")) {
      should_reset_accumulator = true;
    } else {
      PrintError(js, "Unrecognized reset option '%s'",
                 js->LookupOption("reset"));
      return true;
    }
  }
  if (js->HasOption("gc")) {
    if (js->OptionIs("gc", "full")) {
      should_collect = true;
    } else {
      PrintError(js, "Unrecognized gc option '%s'", js->LookupOption("gc"));
      return true;
    }
  }
  if (should_reset_accumulator) {
    isolate->class_table()->ResetAllocationAccumulators();
  }
  if (should_collect) {
    isolate->heap()->CollectAllGarbage();
  }
  isolate->class_table()->AllocationProfilePrintToJSONStream(js);
  return true;
}


static bool HandleResume(Isolate* isolate, JSONStream* js) {
  if (isolate->message_handler()->pause_on_start()) {
    isolate->message_handler()->set_pause_on_start(false);
    JSONObject jsobj(js);
    jsobj.AddProperty("type", "Success");
    jsobj.AddProperty("id", "");
    return true;
  }
  if (isolate->message_handler()->pause_on_exit()) {
    isolate->message_handler()->set_pause_on_exit(false);
    JSONObject jsobj(js);
    jsobj.AddProperty("type", "Success");
    jsobj.AddProperty("id", "");
    return true;
  }

  PrintError(js, "VM was not paused");
  return true;
}


static bool HandleTypeArguments(Isolate* isolate, JSONStream* js) {
  ObjectStore* object_store = isolate->object_store();
  const Array& table = Array::Handle(object_store->canonical_type_arguments());
  ASSERT(table.Length() > 0);
  TypeArguments& type_args = TypeArguments::Handle();
  const intptr_t table_size = table.Length() - 1;
  const intptr_t table_used = Smi::Value(Smi::RawCast(table.At(table_size)));
  bool only_with_instantiations = false;
  if (js->num_arguments() >= 2) {
    const char* second = js->GetArgument(1);
    if (strcmp(second, "withinstantiations") == 0) {
      only_with_instantiations = true;
      if (js->num_arguments() > 2) {
        PrintError(js, "Command too long");
        return true;
      }
    }
  }
  if ((js->num_arguments() == 1) || only_with_instantiations) {
    JSONObject jsobj(js);
    jsobj.AddProperty("type", "TypeArgumentsList");
    jsobj.AddProperty("table_size", table_size);
    jsobj.AddProperty("table_used", table_used);
    JSONArray members(&jsobj, "members");
    for (intptr_t i = 0; i < table_size; i++) {
      type_args ^= table.At(i);
      if (!type_args.IsNull()) {
        if (!only_with_instantiations || type_args.HasInstantiations()) {
          members.AddValue(type_args);
        }
      }
    }
    return true;
  }
  ASSERT((js->num_arguments() >= 2) && !only_with_instantiations);
  intptr_t id;
  if (!GetIntegerId(js->GetArgument(1), &id)) {
    // Note that the table index of the canonical type arguments will change
    // when the table grows. Should we not support this access at all?
    PrintError(js, "Must specify collection object id: /typearguments/id");
    return true;
  }
  if ((id < 0) || (id >= table_size) || (table.At(id) == Object::null())) {
    PrintError(js, "%" Pd " is not a valid typearguments id.", id);
    return true;
  }
  type_args ^= table.At(id);
  type_args.PrintToJSONStream(js, false);
  return true;
}


static bool HandleHeapMap(Isolate* isolate, JSONStream* js) {
  isolate->heap()->PrintHeapMapToJSONStream(isolate, js);
  return true;
}


class ContainsAddressVisitor : public FindObjectVisitor {
 public:
  ContainsAddressVisitor(Isolate* isolate, uword addr)
      : FindObjectVisitor(isolate), addr_(addr) { }
  virtual ~ContainsAddressVisitor() { }

  virtual uword filter_addr() const { return addr_; }

  virtual bool FindObject(RawObject* obj) const {
    uword obj_begin = RawObject::ToAddr(obj);
    uword obj_end = obj_begin + obj->Size();
    return obj_begin <= addr_ && addr_ < obj_end;
  }
 private:
  uword addr_;
};


static bool HandleAddress(Isolate* isolate, JSONStream* js) {
  uword addr = 0;
  if (js->num_arguments() != 2 ||
      !GetUnsignedIntegerId(js->GetArgument(1), &addr, 16)) {
    static const uword kExampleAddr = static_cast<uword>(kIntptrMax / 7);
    PrintError(js, "Must specify address: address/" Px ".", kExampleAddr);
    return true;
  }
  Object& object = Object::Handle(isolate);
  {
    NoGCScope no_gc;
    ContainsAddressVisitor visitor(isolate, addr);
    object = isolate->heap()->FindObject(&visitor);
  }
  object.PrintToJSONStream(js, true);
  return true;
}


static bool HandleMalformedJson(Isolate* isolate, JSONStream* js) {
  JSONObject jsobj(js);
  jsobj.AddProperty("a", "a");
  JSONObject jsobj1(js);
  jsobj1.AddProperty("a", "a");
  JSONObject jsobj2(js);
  jsobj2.AddProperty("a", "a");
  JSONObject jsobj3(js);
  jsobj3.AddProperty("a", "a");
  return true;
}


static bool HandleMalformedObject(Isolate* isolate, JSONStream* js) {
  JSONObject jsobj(js);
  jsobj.AddProperty("bart", "simpson");
  return true;
}


static IsolateMessageHandlerEntry isolate_handlers[] = {
  { "_malformedjson", HandleMalformedJson },
  { "_malformedobject", HandleMalformedObject },
  { "_echo", HandleIsolateEcho },
  { "", HandleIsolate },
  { "address", HandleAddress },
  { "allocationprofile", HandleAllocationProfile },
  { "classes", HandleClasses },
  { "code", HandleCode },
  { "coverage", HandleCoverage },
  { "debug", HandleDebug },
  { "heapmap", HandleHeapMap },
  { "libraries", HandleLibraries },
  { "objects", HandleObjects },
  { "profile", HandleProfile },
  { "resume", HandleResume },
  { "scripts", HandleScripts },
  { "stacktrace", HandleStackTrace },
  { "typearguments", HandleTypeArguments },
};


static IsolateMessageHandler FindIsolateMessageHandler(const char* command) {
  intptr_t num_message_handlers = sizeof(isolate_handlers) /
                                  sizeof(isolate_handlers[0]);
  for (intptr_t i = 0; i < num_message_handlers; i++) {
    const IsolateMessageHandlerEntry& entry = isolate_handlers[i];
    if (strcmp(command, entry.command) == 0) {
      return entry.handler;
    }
  }
  if (FLAG_trace_service) {
    OS::Print("Service has no isolate message handler for <%s>\n", command);
  }
  return NULL;
}


void Service::HandleRootMessage(const Instance& msg) {
  Isolate* isolate = Isolate::Current();
  ASSERT(!msg.IsNull());
  ASSERT(msg.IsGrowableObjectArray());

  {
    StackZone zone(isolate);
    HANDLESCOPE(isolate);

    const GrowableObjectArray& message = GrowableObjectArray::Cast(msg);
    // Message is a list with four entries.
    ASSERT(message.Length() == 4);

    Instance& reply_port = Instance::Handle(isolate);
    GrowableObjectArray& path = GrowableObjectArray::Handle(isolate);
    GrowableObjectArray& option_keys = GrowableObjectArray::Handle(isolate);
    GrowableObjectArray& option_values = GrowableObjectArray::Handle(isolate);
    reply_port ^= message.At(0);
    path ^= message.At(1);
    option_keys ^= message.At(2);
    option_values ^= message.At(3);

    ASSERT(!path.IsNull());
    ASSERT(!option_keys.IsNull());
    ASSERT(!option_values.IsNull());
    // Path always has at least one entry in it.
    ASSERT(path.Length() > 0);
    // Same number of option keys as values.
    ASSERT(option_keys.Length() == option_values.Length());

    String& path_segment = String::Handle();
    if (path.Length() > 0) {
      path_segment ^= path.At(0);
    } else {
      path_segment ^= Symbols::Empty().raw();
    }
    ASSERT(!path_segment.IsNull());
    const char* path_segment_c = path_segment.ToCString();

    RootMessageHandler handler =
        FindRootMessageHandler(path_segment_c);
    {
      JSONStream js;
      js.Setup(zone.GetZone(), reply_port, path, option_keys, option_values);
      if (handler == NULL) {
        // Check for an embedder handler.
        EmbedderServiceHandler* e_handler =
            FindRootEmbedderHandler(path_segment_c);
        if (e_handler != NULL) {
          EmbedderHandleMessage(e_handler, &js);
        } else {
          PrintError(&js, "Unrecognized path");
        }
        js.PostReply();
      } else {
        if (handler(&js)) {
          // Handler returns true if the reply is ready to be posted.
          // TODO(johnmccutchan): Support asynchronous replies.
          js.PostReply();
        }
      }
    }
  }
}


static bool HandleRootEcho(JSONStream* js) {
  JSONObject jsobj(js);
  jsobj.AddProperty("type", "message");
  PrintArgumentsAndOptions(jsobj, js);
  return true;
}


class ServiceIsolateVisitor : public IsolateVisitor {
 public:
  explicit ServiceIsolateVisitor(JSONArray* jsarr)
      : jsarr_(jsarr) {
  }

  virtual ~ServiceIsolateVisitor() {}

  void VisitIsolate(Isolate* isolate) {
    if (isolate != Dart::vm_isolate() && !Service::IsServiceIsolate(isolate)) {
      jsarr_->AddValue(isolate);
    }
  }

 private:
  JSONArray* jsarr_;
};


static bool HandleVM(JSONStream* js) {
  JSONObject jsobj(js);
  jsobj.AddProperty("type", "VM");
  jsobj.AddProperty("id", "vm");
  jsobj.AddProperty("architecture", CPU::Id());
  jsobj.AddProperty("version", Version::String());

  int64_t start_time_micros = Dart::vm_isolate()->start_time();
  int64_t uptime_micros = (OS::GetCurrentTimeMicros() - start_time_micros);
  double seconds = (static_cast<double>(uptime_micros) /
                    static_cast<double>(kMicrosecondsPerSecond));
  jsobj.AddProperty("uptime", seconds);

  // Construct the isolate list.
  {
    JSONArray jsarr(&jsobj, "isolates");
    ServiceIsolateVisitor visitor(&jsarr);
    Isolate::VisitIsolates(&visitor);
  }
  return true;
}


static RootMessageHandlerEntry root_handlers[] = {
  { "_echo", HandleRootEcho },
  { "vm", HandleVM },
};


static RootMessageHandler FindRootMessageHandler(const char* command) {
  intptr_t num_message_handlers = sizeof(root_handlers) /
                                  sizeof(root_handlers[0]);
  for (intptr_t i = 0; i < num_message_handlers; i++) {
    const RootMessageHandlerEntry& entry = root_handlers[i];
    if (strcmp(command, entry.command) == 0) {
      return entry.handler;
    }
  }
  if (FLAG_trace_service) {
    OS::Print("Service has no root message handler for <%s>\n", command);
  }
  return NULL;
}


void Service::EmbedderHandleMessage(EmbedderServiceHandler* handler,
                                    JSONStream* js) {
  ASSERT(handler != NULL);
  Dart_ServiceRequestCallback callback = handler->callback();
  ASSERT(callback != NULL);
  const char* r = NULL;
  const char* name = js->command();
  const char** arguments = js->arguments();
  const char** keys = js->option_keys();
  const char** values = js->option_values();
  r = callback(name, arguments, js->num_arguments(), keys, values,
               js->num_options(), handler->user_data());
  ASSERT(r != NULL);
  // TODO(johnmccutchan): Allow for NULL returns?
  TextBuffer* buffer = js->buffer();
  buffer->AddString(r);
  free(const_cast<char*>(r));
}


void Service::RegisterIsolateEmbedderCallback(
    const char* name,
    Dart_ServiceRequestCallback callback,
    void* user_data) {
  if (name == NULL) {
    return;
  }
  EmbedderServiceHandler* handler = FindIsolateEmbedderHandler(name);
  if (handler != NULL) {
    // Update existing handler entry.
    handler->set_callback(callback);
    handler->set_user_data(user_data);
    return;
  }
  // Create a new handler.
  handler = new EmbedderServiceHandler(name);
  handler->set_callback(callback);
  handler->set_user_data(user_data);

  // Insert into isolate_service_handler_head_ list.
  handler->set_next(isolate_service_handler_head_);
  isolate_service_handler_head_ = handler;
}


EmbedderServiceHandler* Service::FindIsolateEmbedderHandler(
    const char* name) {
  EmbedderServiceHandler* current = isolate_service_handler_head_;
  while (current != NULL) {
    if (strcmp(name, current->name()) == 0) {
      return current;
    }
    current = current->next();
  }
  return NULL;
}


void Service::RegisterRootEmbedderCallback(
    const char* name,
    Dart_ServiceRequestCallback callback,
    void* user_data) {
  if (name == NULL) {
    return;
  }
  EmbedderServiceHandler* handler = FindRootEmbedderHandler(name);
  if (handler != NULL) {
    // Update existing handler entry.
    handler->set_callback(callback);
    handler->set_user_data(user_data);
    return;
  }
  // Create a new handler.
  handler = new EmbedderServiceHandler(name);
  handler->set_callback(callback);
  handler->set_user_data(user_data);

  // Insert into root_service_handler_head_ list.
  handler->set_next(root_service_handler_head_);
  root_service_handler_head_ = handler;
}


EmbedderServiceHandler* Service::FindRootEmbedderHandler(
    const char* name) {
  EmbedderServiceHandler* current = root_service_handler_head_;
  while (current != NULL) {
    if (strcmp(name, current->name()) == 0) {
      return current;
    }
    current = current->next();
  }
  return NULL;
}

}  // namespace dart
