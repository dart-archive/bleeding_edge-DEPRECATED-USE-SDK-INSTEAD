// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/isolate.h"

#include "include/dart_api.h"
#include "platform/assert.h"
#include "platform/json.h"
#include "lib/mirrors.h"
#include "vm/code_observers.h"
#include "vm/compiler_stats.h"
#include "vm/coverage.h"
#include "vm/dart_api_state.h"
#include "vm/dart_entry.h"
#include "vm/debugger.h"
#include "vm/deopt_instructions.h"
#include "vm/heap.h"
#include "vm/heap_histogram.h"
#include "vm/message_handler.h"
#include "vm/object_id_ring.h"
#include "vm/object_store.h"
#include "vm/parser.h"
#include "vm/port.h"
#include "vm/reusable_handles.h"
#include "vm/service.h"
#include "vm/simulator.h"
#include "vm/stack_frame.h"
#include "vm/stub_code.h"
#include "vm/symbols.h"
#include "vm/thread.h"
#include "vm/timer.h"
#include "vm/visitor.h"


namespace dart {

DEFINE_FLAG(bool, report_usage_count, false,
            "Track function usage and report.");
DEFINE_FLAG(bool, trace_isolates, false,
            "Trace isolate creation and shut down.");


void Isolate::RegisterClass(const Class& cls) {
  class_table()->Register(cls);
  if (object_histogram() != NULL) object_histogram()->RegisterClass(cls);
}


class IsolateMessageHandler : public MessageHandler {
 public:
  explicit IsolateMessageHandler(Isolate* isolate);
  ~IsolateMessageHandler();

  const char* name() const;
  void MessageNotify(Message::Priority priority);
  bool HandleMessage(Message* message);

#if defined(DEBUG)
  // Check that it is safe to access this handler.
  void CheckAccess();
#endif
  bool IsCurrentIsolate() const;
  virtual Isolate* GetIsolate() const { return isolate_; }
  bool UnhandledExceptionCallbackHandler(const Object& message,
                                         const UnhandledException& error);

 private:
  bool ProcessUnhandledException(const Object& message, const Error& result);
  RawFunction* ResolveCallbackFunction();
  Isolate* isolate_;
};


IsolateMessageHandler::IsolateMessageHandler(Isolate* isolate)
    : isolate_(isolate) {
}


IsolateMessageHandler::~IsolateMessageHandler() {
}

const char* IsolateMessageHandler::name() const {
  return isolate_->name();
}


void IsolateMessageHandler::MessageNotify(Message::Priority priority) {
  if (priority >= Message::kOOBPriority) {
    // Handle out of band messages even if the isolate is busy.
    isolate_->ScheduleInterrupts(Isolate::kMessageInterrupt);
  }
  Dart_MessageNotifyCallback callback = isolate_->message_notify_callback();
  if (callback) {
    // Allow the embedder to handle message notification.
    (*callback)(Api::CastIsolate(isolate_));
  }
}


bool IsolateMessageHandler::HandleMessage(Message* message) {
  StartIsolateScope start_scope(isolate_);
  StackZone zone(isolate_);
  HandleScope handle_scope(isolate_);

  // If the message is in band we lookup the receive port to dispatch to.  If
  // the receive port is closed, we drop the message without deserializing it.
  Object& receive_port = Object::Handle();
  if (!message->IsOOB()) {
    receive_port = DartLibraryCalls::LookupReceivePort(message->dest_port());
    if (receive_port.IsError()) {
      return ProcessUnhandledException(Object::null_instance(),
                                       Error::Cast(receive_port));
    }
    if (receive_port.IsNull()) {
      delete message;
      return true;
    }
  }

  // Parse the message.
  SnapshotReader reader(message->data(), message->len(),
                        Snapshot::kMessage, Isolate::Current());
  const Object& msg_obj = Object::Handle(reader.ReadObject());
  if (msg_obj.IsError()) {
    // An error occurred while reading the message.
    return ProcessUnhandledException(Object::null_instance(),
                                     Error::Cast(msg_obj));
  }
  if (!msg_obj.IsNull() && !msg_obj.IsInstance()) {
    // TODO(turnidge): We need to decide what an isolate does with
    // malformed messages.  If they (eventually) come from a remote
    // machine, then it might make sense to drop the message entirely.
    // In the case that the message originated locally, which is
    // always true for now, then this should never occur.
    UNREACHABLE();
  }

  Instance& msg = Instance::Handle();
  msg ^= msg_obj.raw();  // Can't use Instance::Cast because may be null.

  bool success = true;
  if (message->IsOOB()) {
    Service::HandleServiceMessage(isolate_, message->reply_port(), msg);
  } else {
    const Object& result = Object::Handle(
        DartLibraryCalls::HandleMessage(
            receive_port, message->reply_port(), msg));
    if (result.IsError()) {
      success = ProcessUnhandledException(msg, Error::Cast(result));
    } else {
      ASSERT(result.IsNull());
    }
  }
  delete message;
  return success;
}


RawFunction* IsolateMessageHandler::ResolveCallbackFunction() {
  ASSERT(isolate_->object_store()->unhandled_exception_handler() != NULL);
  String& callback_name = String::Handle(isolate_);
  if (isolate_->object_store()->unhandled_exception_handler() !=
      String::null()) {
    callback_name = isolate_->object_store()->unhandled_exception_handler();
  } else {
    callback_name = String::New("_unhandledExceptionCallback");
  }
  Library& lib =
      Library::Handle(isolate_, isolate_->object_store()->isolate_library());
  Function& func =
      Function::Handle(isolate_, lib.LookupLocalFunction(callback_name));
  if (func.IsNull()) {
    lib = isolate_->object_store()->root_library();
    func = lib.LookupLocalFunction(callback_name);
  }
  return func.raw();
}


bool IsolateMessageHandler::UnhandledExceptionCallbackHandler(
    const Object& message, const UnhandledException& error) {
  const Instance& cause = Instance::Handle(isolate_, error.exception());
  const Instance& stacktrace =
      Instance::Handle(isolate_, error.stacktrace());

  // Wrap these args into an IsolateUncaughtException object.
  const Array& exception_args = Array::Handle(Array::New(3));
  exception_args.SetAt(0, message);
  exception_args.SetAt(1, cause);
  exception_args.SetAt(2, stacktrace);
  const Object& exception =
      Object::Handle(isolate_,
                     Exceptions::Create(Exceptions::kIsolateUnhandledException,
                                        exception_args));
  if (exception.IsError()) {
    return false;
  }
  ASSERT(exception.IsInstance());

  // Invoke script's callback function.
  Object& function = Object::Handle(isolate_, ResolveCallbackFunction());
  if (function.IsNull() || function.IsError()) {
    return false;
  }
  const Array& callback_args = Array::Handle(Array::New(1));
  callback_args.SetAt(0, exception);
  const Object& result =
      Object::Handle(DartEntry::InvokeFunction(Function::Cast(function),
                                               callback_args));
  if (result.IsError()) {
    const Error& err = Error::Cast(result);
    OS::PrintErr("failed calling unhandled exception callback: %s\n",
                 err.ToErrorCString());
    return false;
  }

  ASSERT(result.IsBool());
  bool continue_from_exception = Bool::Cast(result).value();
  if (continue_from_exception) {
    isolate_->object_store()->clear_sticky_error();
  }
  return continue_from_exception;
}

#if defined(DEBUG)
void IsolateMessageHandler::CheckAccess() {
  ASSERT(IsCurrentIsolate());
}
#endif


bool IsolateMessageHandler::IsCurrentIsolate() const {
  return (isolate_ == Isolate::Current());
}


bool IsolateMessageHandler::ProcessUnhandledException(
    const Object& message, const Error& result) {
  if (result.IsUnhandledException()) {
    // Invoke the isolate's uncaught exception handler, if it exists.
    const UnhandledException& error = UnhandledException::Cast(result);
    RawInstance* exception = error.exception();
    if ((exception != isolate_->object_store()->out_of_memory()) &&
        (exception != isolate_->object_store()->stack_overflow())) {
      if (UnhandledExceptionCallbackHandler(message, error)) {
        return true;
      }
    }
  }

  // Invoke the isolate's unhandled exception callback if there is one.
  if (Isolate::UnhandledExceptionCallback() != NULL) {
    Dart_EnterScope();
    Dart_Handle error = Api::NewHandle(isolate_, result.raw());
    (Isolate::UnhandledExceptionCallback())(error);
    Dart_ExitScope();
  }

  isolate_->object_store()->set_sticky_error(result);
  return false;
}


#if defined(DEBUG)
// static
void BaseIsolate::AssertCurrent(BaseIsolate* isolate) {
  ASSERT(isolate == Isolate::Current());
}
#endif


#define REUSABLE_HANDLE_INITIALIZERS(object)                                   \
  object##_handle_(NULL),

Isolate::Isolate()
    : store_buffer_(),
      message_notify_callback_(NULL),
      name_(NULL),
      start_time_(OS::GetCurrentTimeMicros()),
      main_port_(0),
      heap_(NULL),
      object_store_(NULL),
      top_context_(Context::null()),
      top_exit_frame_info_(0),
      init_callback_data_(NULL),
      environment_callback_(NULL),
      library_tag_handler_(NULL),
      api_state_(NULL),
      stub_code_(NULL),
      debugger_(NULL),
      single_step_(false),
      simulator_(NULL),
      long_jump_base_(NULL),
      timer_list_(),
      deopt_id_(0),
      mutex_(new Mutex()),
      stack_limit_(0),
      saved_stack_limit_(0),
      message_handler_(NULL),
      spawn_data_(0),
      is_runnable_(false),
      gc_prologue_callbacks_(),
      gc_epilogue_callbacks_(),
      defer_finalization_count_(0),
      deopt_context_(NULL),
      stacktrace_(NULL),
      stack_frame_index_(-1),
      object_histogram_(NULL),
      object_id_ring_(NULL),
      REUSABLE_HANDLE_LIST(REUSABLE_HANDLE_INITIALIZERS)
      reusable_handles_() {
  if (FLAG_print_object_histogram && (Dart::vm_isolate() != NULL)) {
    object_histogram_ = new ObjectHistogram(this);
  }
}
#undef REUSABLE_HANDLE_INITIALIZERS


Isolate::~Isolate() {
  delete [] name_;
  delete heap_;
  delete object_store_;
  delete api_state_;
  delete stub_code_;
  delete debugger_;
#if defined(USING_SIMULATOR)
  delete simulator_;
#endif
  delete mutex_;
  mutex_ = NULL;  // Fail fast if interrupts are scheduled on a dead isolate.
  delete message_handler_;
  message_handler_ = NULL;  // Fail fast if we send messages to a dead isolate.
  ASSERT(deopt_context_ == NULL);  // No deopt in progress when isolate deleted.
  delete object_histogram_;
}

void Isolate::SetCurrent(Isolate* current) {
  Thread::SetThreadLocal(isolate_key, reinterpret_cast<uword>(current));
}


// The single thread local key which stores all the thread local data
// for a thread. Since an Isolate is the central repository for
// storing all isolate specific information a single thread local key
// is sufficient.
ThreadLocalKey Isolate::isolate_key = Thread::kUnsetThreadLocalKey;


void Isolate::InitOnce() {
  ASSERT(isolate_key == Thread::kUnsetThreadLocalKey);
  isolate_key = Thread::CreateThreadLocal();
  ASSERT(isolate_key != Thread::kUnsetThreadLocalKey);
  create_callback_ = NULL;
}


Isolate* Isolate::Init(const char* name_prefix) {
  Isolate* result = new Isolate();
  ASSERT(result != NULL);

  // TODO(5411455): For now just set the recently created isolate as
  // the current isolate.
  SetCurrent(result);

  // Setup the isolate specific resuable handles.
#define REUSABLE_HANDLE_ALLOCATION(object)                                     \
  result->object##_handle_ = result->AllocateReusableHandle<object>();         \

  REUSABLE_HANDLE_LIST(REUSABLE_HANDLE_ALLOCATION)
#undef REUSABLE_HANDLE_ALLOCATION

  // Setup the isolate message handler.
  MessageHandler* handler = new IsolateMessageHandler(result);
  ASSERT(handler != NULL);
  result->set_message_handler(handler);

  // Setup the Dart API state.
  ApiState* state = new ApiState();
  ASSERT(state != NULL);
  result->set_api_state(state);

  // Initialize stack top and limit in case we are running the isolate in the
  // main thread.
  // TODO(5411455): Need to figure out how to set the stack limit for the
  // main thread.
  result->SetStackLimitFromCurrentTOS(reinterpret_cast<uword>(&result));
  result->set_main_port(PortMap::CreatePort(result->message_handler()));
  result->BuildName(name_prefix);

  result->debugger_ = new Debugger();
  result->debugger_->Initialize(result);
  if (FLAG_trace_isolates) {
    if (name_prefix == NULL || strcmp(name_prefix, "vm-isolate") != 0) {
      OS::Print("[+] Starting isolate:\n"
                "\tisolate:    %s\n", result->name());
    }
  }
  return result;
}


void Isolate::BuildName(const char* name_prefix) {
  ASSERT(name_ == NULL);
  if (name_prefix == NULL) {
    name_prefix = "isolate";
  }
  const char* kFormat = "%s-%lld";
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, name_prefix, main_port()) + 1;
  name_ = new char[len];
  OS::SNPrint(name_, len, kFormat, name_prefix, main_port());
}


// TODO(5411455): Use flag to override default value and Validate the
// stack size by querying OS.
uword Isolate::GetSpecifiedStackSize() {
  ASSERT(Isolate::kStackSizeBuffer < Thread::GetMaxStackSize());
  uword stack_size = Thread::GetMaxStackSize() - Isolate::kStackSizeBuffer;
  return stack_size;
}


void Isolate::SetStackLimitFromCurrentTOS(uword stack_top_value) {
#if defined(USING_SIMULATOR)
  // Ignore passed-in native stack top and use Simulator stack top.
  Simulator* sim = Simulator::Current();  // May allocate a simulator.
  ASSERT(simulator() == sim);  // This isolate's simulator is the current one.
  stack_top_value = sim->StackTop();
  // The overflow area is accounted for by the simulator.
#endif
  SetStackLimit(stack_top_value - GetSpecifiedStackSize());
}


void Isolate::SetStackLimit(uword limit) {
  MutexLocker ml(mutex_);
  if (stack_limit_ == saved_stack_limit_) {
    // No interrupt pending, set stack_limit_ too.
    stack_limit_ = limit;
  }
  saved_stack_limit_ = limit;
}


void Isolate::ScheduleInterrupts(uword interrupt_bits) {
  // TODO(turnidge): Can't use MutexLocker here because MutexLocker is
  // a StackResource, which requires a current isolate.  Should
  // MutexLocker really be a StackResource?
  mutex_->Lock();
  ASSERT((interrupt_bits & ~kInterruptsMask) == 0);  // Must fit in mask.
  if (stack_limit_ == saved_stack_limit_) {
    stack_limit_ = (~static_cast<uword>(0)) & ~kInterruptsMask;
  }
  stack_limit_ |= interrupt_bits;
  mutex_->Unlock();
}


bool Isolate::MakeRunnable() {
  ASSERT(Isolate::Current() == NULL);
  // Can't use MutexLocker here because MutexLocker is
  // a StackResource, which requires a current isolate.
  mutex_->Lock();
  // Check if we are in a valid state to make the isolate runnable.
  if (is_runnable_ == true) {
    mutex_->Unlock();
    return false;  // Already runnable.
  }
  // Set the isolate as runnable and if we are being spawned schedule
  // isolate on thread pool for execution.
  is_runnable_ = true;
  IsolateSpawnState* state = reinterpret_cast<IsolateSpawnState*>(spawn_data());
  if (state != NULL) {
    ASSERT(this == state->isolate());
    Run();
  }
  mutex_->Unlock();
  return true;
}


static void StoreError(Isolate* isolate, const Object& obj) {
  ASSERT(obj.IsError());
  isolate->object_store()->set_sticky_error(Error::Cast(obj));
}


static bool RunIsolate(uword parameter) {
  Isolate* isolate = reinterpret_cast<Isolate*>(parameter);
  IsolateSpawnState* state = NULL;
  {
    MutexLocker ml(isolate->mutex());
    state = reinterpret_cast<IsolateSpawnState*>(isolate->spawn_data());
    isolate->set_spawn_data(0);
  }
  {
    StartIsolateScope start_scope(isolate);
    StackZone zone(isolate);
    HandleScope handle_scope(isolate);
    if (!ClassFinalizer::FinalizePendingClasses()) {
      // Error is in sticky error already.
      return false;
    }

    // Set up specific unhandled exception handler.
    const String& callback_name = String::Handle(
        isolate, String::New(state->exception_callback_name()));
    isolate->object_store()->
        set_unhandled_exception_handler(callback_name);

    Object& result = Object::Handle();
    result = state->ResolveFunction();
    bool is_spawn_uri = state->is_spawn_uri();
    delete state;
    state = NULL;
    if (result.IsError()) {
      StoreError(isolate, result);
      return false;
    }
    ASSERT(result.IsFunction());
    Function& func = Function::Handle(isolate);
    func ^= result.raw();
    func = func.ImplicitClosureFunction();

    // Instead of directly invoking the entry point we call '_startIsolate' with
    // the entry point as argument. The '_startIsolate' function will
    // communicate with the spawner to receive the initial message before it
    // executes the real entry point.
    // Since this function ("RunIsolate") is used for both Isolate.spawn and
    // Isolate.spawnUri we also send a boolean flag as argument so that the
    // "_startIsolate" function can act corresponding to how the isolate was
    // created.
    const Array& args = Array::Handle(Array::New(2));
    args.SetAt(0, Instance::Handle(func.ImplicitStaticClosure()));
    args.SetAt(1, is_spawn_uri ? Bool::True() : Bool::False());

    const Library& lib = Library::Handle(Library::IsolateLibrary());
    const String& entry_name = String::Handle(String::New("_startIsolate"));
    const Function& entry_point =
        Function::Handle(lib.LookupLocalFunction(entry_name));
    ASSERT(entry_point.IsFunction() && !entry_point.IsNull());

    result = DartEntry::InvokeFunction(entry_point, args);
    if (result.IsError()) {
      StoreError(isolate, result);
      return false;
    }
  }
  return true;
}


static void ShutdownIsolate(uword parameter) {
  Isolate* isolate = reinterpret_cast<Isolate*>(parameter);
  {
    // Print the error if there is one.  This may execute dart code to
    // print the exception object, so we need to use a StartIsolateScope.
    StartIsolateScope start_scope(isolate);
    StackZone zone(isolate);
    HandleScope handle_scope(isolate);
    Error& error = Error::Handle();
    error = isolate->object_store()->sticky_error();
    if (!error.IsNull()) {
      OS::PrintErr("in ShutdownIsolate: %s\n", error.ToErrorCString());
    }
    Dart::RunShutdownCallback();
  }
  {
    // Shut the isolate down.
    SwitchIsolateScope switch_scope(isolate);
    Dart::ShutdownIsolate();
  }
}


void Isolate::Run() {
  message_handler()->Run(Dart::thread_pool(),
                         RunIsolate,
                         ShutdownIsolate,
                         reinterpret_cast<uword>(this));
}


uword Isolate::GetAndClearInterrupts() {
  MutexLocker ml(mutex_);
  if (stack_limit_ == saved_stack_limit_) {
    return 0;  // No interrupt was requested.
  }
  uword interrupt_bits = stack_limit_ & kInterruptsMask;
  stack_limit_ = saved_stack_limit_;
  return interrupt_bits;
}


static int MostUsedFunctionFirst(const Function* const* a,
                                 const Function* const* b) {
  if ((*a)->usage_counter() > (*b)->usage_counter()) {
    return -1;
  } else if ((*a)->usage_counter() < (*b)->usage_counter()) {
    return 1;
  } else {
    return 0;
  }
}


static void AddFunctionsFromClass(const Class& cls,
                                  GrowableArray<const Function*>* functions) {
  const Array& class_functions = Array::Handle(cls.functions());
  // Class 'dynamic' is allocated/initialized in a special way, leaving
  // the functions field NULL instead of empty.
  const int func_len = class_functions.IsNull() ? 0 : class_functions.Length();
  for (int j = 0; j < func_len; j++) {
    Function& function = Function::Handle();
    function ^= class_functions.At(j);
    if (function.usage_counter() > 0) {
      functions->Add(&function);
    }
  }
}


void Isolate::PrintInvokedFunctions() {
  ASSERT(this == Isolate::Current());
  const GrowableObjectArray& libraries =
      GrowableObjectArray::Handle(object_store()->libraries());
  Library& library = Library::Handle();
  GrowableArray<const Function*> invoked_functions;
  for (int i = 0; i < libraries.Length(); i++) {
    library ^= libraries.At(i);
    Class& cls = Class::Handle();
    ClassDictionaryIterator iter(library,
                                 ClassDictionaryIterator::kIteratePrivate);
    while (iter.HasNext()) {
      cls = iter.GetNextClass();
      AddFunctionsFromClass(cls, &invoked_functions);
    }
  }
  invoked_functions.Sort(MostUsedFunctionFirst);
  for (int i = 0; i < invoked_functions.length(); i++) {
    OS::Print("%10" Pd " x %s\n",
        invoked_functions[i]->usage_counter(),
        invoked_functions[i]->ToFullyQualifiedCString());
  }
}


class FinalizeWeakPersistentHandlesVisitor : public HandleVisitor {
 public:
  FinalizeWeakPersistentHandlesVisitor() {
  }

  void VisitHandle(uword addr) {
    FinalizablePersistentHandle* handle =
        reinterpret_cast<FinalizablePersistentHandle*>(addr);
    FinalizablePersistentHandle::Finalize(handle);
  }

 private:
  DISALLOW_COPY_AND_ASSIGN(FinalizeWeakPersistentHandlesVisitor);
};


void Isolate::Shutdown() {
  ASSERT(this == Isolate::Current());
  ASSERT(top_resource() == NULL);
  ASSERT((heap_ == NULL) || heap_->Verify());

  // Create an area where we do have a zone and a handle scope so that we can
  // call VM functions while tearing this isolate down.
  {
    StackZone stack_zone(this);
    HandleScope handle_scope(this);

    if (FLAG_print_object_histogram) {
      heap()->CollectAllGarbage();
      object_histogram()->Print();
    }

    // Clean up debugger resources.
    debugger()->Shutdown();

    // Close all the ports owned by this isolate.
    PortMap::ClosePorts(message_handler());

    // Fail fast if anybody tries to post any more messsages to this isolate.
    delete message_handler();
    set_message_handler(NULL);

    // Dump all accumalated timer data for the isolate.
    timer_list_.ReportTimers();
    if (FLAG_report_usage_count) {
      PrintInvokedFunctions();
    }

    // Write out the coverage data if collection has been enabled.
    CodeCoverage::Write(this);

    // Finalize any weak persistent handles with a non-null referent.
    FinalizeWeakPersistentHandlesVisitor visitor;
    api_state()->weak_persistent_handles().VisitHandles(&visitor);

    CompilerStats::Print();
    if (FLAG_trace_isolates) {
      heap()->PrintSizes();
      megamorphic_cache_table()->PrintSizes();
      Symbols::DumpStats();
      OS::Print("[-] Stopping isolate:\n"
                "\tisolate:    %s\n", name());
    }
  }

  // TODO(5411455): For now just make sure there are no current isolates
  // as we are shutting down the isolate.
  SetCurrent(NULL);
}


Dart_IsolateCreateCallback Isolate::create_callback_ = NULL;
Dart_IsolateInterruptCallback Isolate::interrupt_callback_ = NULL;
Dart_IsolateUnhandledExceptionCallback
    Isolate::unhandled_exception_callback_ = NULL;
Dart_IsolateShutdownCallback Isolate::shutdown_callback_ = NULL;
Dart_FileOpenCallback Isolate::file_open_callback_ = NULL;
Dart_FileReadCallback Isolate::file_read_callback_ = NULL;
Dart_FileWriteCallback Isolate::file_write_callback_ = NULL;
Dart_FileCloseCallback Isolate::file_close_callback_ = NULL;
Dart_IsolateInterruptCallback Isolate::vmstats_callback_ = NULL;


void Isolate::VisitObjectPointers(ObjectPointerVisitor* visitor,
                                  bool visit_prologue_weak_handles,
                                  bool validate_frames) {
  ASSERT(visitor != NULL);

  // Visit objects in the object store.
  object_store()->VisitObjectPointers(visitor);

  // Visit objects in the class table.
  class_table()->VisitObjectPointers(visitor);

  // Visit objects in the megamorphic cache.
  megamorphic_cache_table()->VisitObjectPointers(visitor);

  // Visit objects in per isolate stubs.
  StubCode::VisitObjectPointers(visitor);

  // Visit objects in zones.
  current_zone()->VisitObjectPointers(visitor);

  // Visit objects in isolate specific handles area.
  reusable_handles_.VisitObjectPointers(visitor);

  // Iterate over all the stack frames and visit objects on the stack.
  StackFrameIterator frames_iterator(validate_frames);
  StackFrame* frame = frames_iterator.NextFrame();
  while (frame != NULL) {
    frame->VisitObjectPointers(visitor);
    frame = frames_iterator.NextFrame();
  }

  // Visit the dart api state for all local and persistent handles.
  if (api_state() != NULL) {
    api_state()->VisitObjectPointers(visitor, visit_prologue_weak_handles);
  }

  // Visit the top context which is stored in the isolate.
  visitor->VisitPointer(reinterpret_cast<RawObject**>(&top_context_));

  // Visit objects in the debugger.
  debugger()->VisitObjectPointers(visitor);

  // Visit objects that are being used for deoptimization.
  if (deopt_context() != NULL) {
    deopt_context()->VisitObjectPointers(visitor);
  }
}


void Isolate::VisitWeakPersistentHandles(HandleVisitor* visitor,
                                         bool visit_prologue_weak_handles) {
  if (api_state() != NULL) {
    api_state()->VisitWeakHandles(visitor, visit_prologue_weak_handles);
  }
}


static Monitor* status_sync = NULL;


bool Isolate::FetchStacktrace() {
  Isolate* isolate = Isolate::Current();
  MonitorLocker ml(status_sync);
  DebuggerStackTrace* stack = Debugger::CollectStackTrace();
  TextBuffer buffer(256);
  buffer.Printf("{ \"handle\": \"0x%" Px64 "\", \"stacktrace\": [ ",
                reinterpret_cast<int64_t>(isolate));
  intptr_t n_frames = stack->Length();
  String& url = String::Handle();
  String& function = String::Handle();
  for (int i = 0; i < n_frames; i++) {
    if (i > 0) {
      buffer.Printf(", ");
    }
    ActivationFrame* frame = stack->FrameAt(i);
    url ^= frame->SourceUrl();
    function ^= frame->function().UserVisibleName();
    buffer.Printf("{ \"url\": \"%s\", ", url.ToCString());
    buffer.Printf("\"line\": %" Pd ", ", frame->LineNumber());
    buffer.Printf("\"function\": \"%s\", ", function.ToCString());

    const Code& code = frame->code();
    buffer.Printf("\"code\": { ");
    buffer.Printf("\"alive\": %s, ", code.is_alive() ? "false" : "true");
    buffer.Printf("\"optimized\": %s }}",
        code.is_optimized() ? "false" : "true");
  }
  buffer.Printf("]}");
  isolate->stacktrace_ = OS::StrNDup(buffer.buf(), buffer.length());
  ml.Notify();
  return true;
}


bool Isolate::FetchStackFrameDetails() {
  Isolate* isolate = Isolate::Current();
  ASSERT(isolate->stack_frame_index_ >= 0);
  MonitorLocker ml(status_sync);
  DebuggerStackTrace* stack = Debugger::CollectStackTrace();
  intptr_t frame_index = isolate->stack_frame_index_;
  if (frame_index >= stack->Length()) {
    // Frame no longer available.
    return false;
  }
  ActivationFrame* frame = stack->FrameAt(frame_index);
  TextBuffer buffer(256);
  buffer.Printf("{ \"handle\": \"0x%" Px64 "\", \"frame_index\": %" Pd ", ",
       reinterpret_cast<int64_t>(isolate), frame_index);

  const Code& code = frame->code();
  buffer.Printf("\"code\": { \"size\": %" Pd ", ", code.Size());
  buffer.Printf("\"alive\": %s, ", code.is_alive() ? "false" : "true");
  buffer.Printf("\"optimized\": %s }, ",
      code.is_optimized() ? "false" : "true");
  // TODO(tball): add compilation stats (time, etc.), when available.

  buffer.Printf("\"local_vars\": [ ");
  intptr_t n_local_vars = frame->NumLocalVariables();
  String& var_name = String::Handle();
  Instance& value = Instance::Handle();
  for (int i = 0; i < n_local_vars; i++) {
    if (i > 0) {
      buffer.Printf(", ");
    }
    intptr_t token_pos, end_pos;
    frame->VariableAt(i, &var_name, &token_pos, &end_pos, &value);
    buffer.Printf(
        "{ \"name\": \"%s\", \"pos\": %" Pd ", \"end_pos\": %" Pd ", "
        "\"value\": \"%s\" }",
        var_name.ToCString(), token_pos, end_pos, value.ToCString());
  }
  buffer.Printf("]}");
  isolate->stacktrace_ = OS::StrNDup(buffer.buf(), buffer.length());
  ml.Notify();
  return true;
}


char* Isolate::DoStacktraceInterrupt(Dart_IsolateInterruptCallback cb) {
  ASSERT(stacktrace_ == NULL);
  SetVmStatsCallback(cb);
  if (status_sync == NULL) {
    status_sync = new Monitor();
  }
  if (is_runnable()) {
    ScheduleInterrupts(Isolate::kVmStatusInterrupt);
    {
      MonitorLocker ml(status_sync);
      if (stacktrace_ == NULL) {  // It may already be available.
        ml.Wait(1000);
      }
    }
    SetVmStatsCallback(NULL);
  }
  char* result = stacktrace_;
  stacktrace_ = NULL;
  if (result == NULL) {
    // Return empty stack.
    TextBuffer buffer(256);
    buffer.Printf("{ \"handle\": \"0x%" Px64 "\", \"stacktrace\": []}",
                  reinterpret_cast<int64_t>(this));

    result = OS::StrNDup(buffer.buf(), buffer.length());
  }
  ASSERT(result != NULL);
  // result is freed by VmStats::WebServer().
  return result;
}


char* Isolate::GetStatusStacktrace() {
  return DoStacktraceInterrupt(&FetchStacktrace);
}

char* Isolate::GetStatusStackFrame(intptr_t index) {
  ASSERT(index >= 0);
  stack_frame_index_ = index;
  char* result = DoStacktraceInterrupt(&FetchStackFrameDetails);
  stack_frame_index_ = -1;
  return result;
}


// Returns the isolate's general detail information.
char* Isolate::GetStatusDetails() {
  const char* format = "{\n"
      "  \"handle\": \"0x%" Px64 "\",\n"
      "  \"name\": \"%s\",\n"
      "  \"port\": %" Pd ",\n"
      "  \"starttime\": %" Pd ",\n"
      "  \"stacklimit\": %" Pd ",\n"
      "  \"newspace\": {\n"
      "    \"used\": %" Pd ",\n"
      "    \"capacity\": %" Pd "\n"
      "  },\n"
      "  \"oldspace\": {\n"
      "    \"used\": %" Pd ",\n"
      "    \"capacity\": %" Pd "\n"
      "  }\n"
      "}";
  char buffer[300];
  int64_t address = reinterpret_cast<int64_t>(this);
  int n = OS::SNPrint(buffer, 300, format, address, name(), main_port(),
                      (start_time() / 1000L), saved_stack_limit(),
                      heap()->Used(Heap::kNew) / KB,
                      heap()->Capacity(Heap::kNew) / KB,
                      heap()->Used(Heap::kOld) / KB,
                      heap()->Capacity(Heap::kOld) / KB);
  ASSERT(n < 300);
  return strdup(buffer);
}


char* Isolate::GetStatus(const char* request) {
  char* p = const_cast<char*>(request);
  const char* service_type = "/isolate/";
  ASSERT(!strncmp(p, service_type, strlen(service_type)));
  p += strlen(service_type);

  // Extract isolate handle.
  int64_t addr;
  OS::StringToInt64(p, &addr);
  // TODO(tball): add validity check when issue 9600 is fixed.
  Isolate* isolate = reinterpret_cast<Isolate*>(addr);
  p += strcspn(p, "/");

  // Query "/isolate/<handle>".
  if (strlen(p) == 0) {
    return isolate->GetStatusDetails();
  }

  // Query "/isolate/<handle>/stacktrace"
  if (!strcmp(p, "/stacktrace")) {
    return isolate->GetStatusStacktrace();
  }

  // Query "/isolate/<handle>/stacktrace/<frame-index>"
  const char* stacktrace_query = "/stacktrace/";
  int64_t frame_index = -1;
  if (!strncmp(p, stacktrace_query, strlen(stacktrace_query))) {
    p += strlen(stacktrace_query);
    OS::StringToInt64(p, &frame_index);
    if (frame_index >= 0) {
      return isolate->GetStatusStackFrame(frame_index);
    }
  }

  // TODO(tball): "/isolate/<handle>/stacktrace/<frame-index>"/disassemble"

  return NULL;  // Unimplemented query.
}


template<class T>
T* Isolate::AllocateReusableHandle() {
  T* handle = reinterpret_cast<T*>(reusable_handles_.AllocateScopedHandle());
  T::initializeHandle(handle, T::null());
  return handle;
}


IsolateSpawnState::IsolateSpawnState(const Function& func)
    : isolate_(NULL),
      script_url_(NULL),
      library_url_(NULL),
      function_name_(NULL),
      exception_callback_name_(NULL) {
  script_url_ = NULL;
  const Class& cls = Class::Handle(func.Owner());
  ASSERT(cls.IsTopLevel());
  const Library& lib = Library::Handle(cls.library());
  const String& lib_url = String::Handle(lib.url());
  library_url_ = strdup(lib_url.ToCString());

  const String& func_name = String::Handle(func.name());
  function_name_ = strdup(func_name.ToCString());
  exception_callback_name_ = strdup("_unhandledExceptionCallback");
}


IsolateSpawnState::IsolateSpawnState(const char* script_url)
    : isolate_(NULL),
      library_url_(NULL),
      function_name_(NULL),
      exception_callback_name_(NULL) {
  script_url_ = strdup(script_url);
  library_url_ = NULL;
  function_name_ = strdup("main");
  exception_callback_name_ = strdup("_unhandledExceptionCallback");
}


IsolateSpawnState::~IsolateSpawnState() {
  free(script_url_);
  free(library_url_);
  free(function_name_);
  free(exception_callback_name_);
}


RawObject* IsolateSpawnState::ResolveFunction() {
  // Resolve the library.
  Library& lib = Library::Handle();
  if (library_url()) {
    const String& lib_url = String::Handle(String::New(library_url()));
    lib = Library::LookupLibrary(lib_url);
    if (lib.IsNull() || lib.IsError()) {
      const String& msg = String::Handle(String::NewFormatted(
          "Unable to find library '%s'.", library_url()));
      return LanguageError::New(msg);
    }
  } else {
    lib = isolate()->object_store()->root_library();
  }
  ASSERT(!lib.IsNull());

  // Resolve the function.
  const String& func_name =
      String::Handle(String::New(function_name()));
  const Function& func = Function::Handle(lib.LookupLocalFunction(func_name));
  if (func.IsNull()) {
    const String& msg = String::Handle(String::NewFormatted(
        "Unable to resolve function '%s' in library '%s'.",
        function_name(), (library_url() ? library_url() : script_url())));
    return LanguageError::New(msg);
  }
  return func.raw();
}


void IsolateSpawnState::Cleanup() {
  SwitchIsolateScope switch_scope(isolate());
  Dart::ShutdownIsolate();
}

}  // namespace dart
