#include "jni/vm_glue.h"

#include <math.h>
#include <unistd.h>

#include "bin/eventhandler.h"
#include "bin/isolate_data.h"
#include "bin/log.h"
#include "bin/platform.h"
#include "bin/process.h"
#include "vm/flags.h"

// snapshot_buffer points to a snapshot if we link in a snapshot otherwise
// it is initialized to NULL.
extern const uint8_t* snapshot_buffer;

VMGlue::VMGlue(Graphics* graphics)
    : graphics_(graphics),
      isolate_(NULL),
      initialized_vm_(false),
      initialized_script_(false) {
  Log::Print("Creating VMGlue");
}

int VMGlue::ErrorExit(const char* format, ...) {
  va_list arguments;
  va_start(arguments, format);
  Log::VPrintErr(format, arguments);
  va_end(arguments);
  Dart_ExitScope();
  Dart_ShutdownIsolate();
  Log::PrintErr("Shutdown isolate");
  return -1;
}

Dart_Handle VMGlue::SetupRuntimeOptions(CommandLineOptions* options,
                                           const char* executable_name,
                                           const char* script_name) {
  int options_count = 0;
  Dart_Handle dart_executable = DartUtils::NewString(executable_name);
  if (Dart_IsError(dart_executable)) {
    return dart_executable;
  }
  Dart_Handle dart_script = DartUtils::NewString(script_name);
  if (Dart_IsError(dart_script)) {
    return dart_script;
  }
  Dart_Handle dart_arguments = Dart_NewList(0);
  if (Dart_IsError(dart_arguments)) {
    return dart_arguments;
  }
  Dart_Handle core_lib_url = DartUtils::NewString("dart:core");
  if (Dart_IsError(core_lib_url)) {
    return core_lib_url;
  }
  Dart_Handle core_lib = Dart_LookupLibrary(core_lib_url);
  if (Dart_IsError(core_lib)) {
    return core_lib;
  }
  Dart_Handle runtime_options_class_name =
      DartUtils::NewString("_OptionsImpl");
  if (Dart_IsError(runtime_options_class_name)) {
    return runtime_options_class_name;
  }
  Dart_Handle runtime_options_class = Dart_GetClass(
      core_lib, runtime_options_class_name);
  if (Dart_IsError(runtime_options_class)) {
    return runtime_options_class;
  }
  Dart_Handle executable_name_name = DartUtils::NewString("_nativeExecutable");
  if (Dart_IsError(executable_name_name)) {
    return executable_name_name;
  }
  Dart_Handle set_executable_name =
      Dart_SetField(runtime_options_class,
                    executable_name_name,
                    dart_executable);
  if (Dart_IsError(set_executable_name)) {
    return set_executable_name;
  }
  Dart_Handle script_name_name = DartUtils::NewString("_nativeScript");
  if (Dart_IsError(script_name_name)) {
    return script_name_name;
  }
  Dart_Handle set_script_name =
      Dart_SetField(runtime_options_class, script_name_name, dart_script);
  if (Dart_IsError(set_script_name)) {
    return set_script_name;
  }
  Dart_Handle native_name = DartUtils::NewString("_nativeArguments");
  if (Dart_IsError(native_name)) {
    return native_name;
  }
  return Dart_SetField(runtime_options_class, native_name, dart_arguments);
}

#define CHECK_RESULT(result)                                                   \
  if (Dart_IsError(result)) {                                                  \
    *error = strdup(Dart_GetError(result));                                    \
    Log::PrintErr(*error);                                                     \
    Dart_ExitScope();                                                          \
    Dart_ShutdownIsolate();                                                    \
    return false;                                                              \
  }

// Returns true on success, false on failure.
bool VMGlue::CreateIsolateAndSetupHelper(const char* script_uri,
                                           const char* main,
                                           void* data,
                                           char** error) {
  Log::Print("Creating isolate %s, %s", script_uri, main);
  Dart_Isolate isolate =
      Dart_CreateIsolate(script_uri, main, snapshot_buffer, data, error);
  if (isolate == NULL) {
    Log::PrintErr("Couldn't create isolate: %s", error);
    return false;
  }

  Log::Print("Entering scope");
  Dart_EnterScope();

  if (snapshot_buffer != NULL) {
    // Setup the native resolver as the snapshot does not carry it.
    Builtin::SetNativeResolver(Builtin::kBuiltinLibrary);
    Builtin::SetNativeResolver(Builtin::kIOLibrary);
  }

  // Set up the library tag handler for this isolate.
  Log::Print("Setting up library tag handler");
  Dart_Handle result = Dart_SetLibraryTagHandler(DartUtils::LibraryTagHandler);
  CHECK_RESULT(result);

  // Load the specified application script into the newly created isolate.
  Dart_Handle library;

  // Prepare builtin and its dependent libraries for use to resolve URIs.
  Log::Print("Preparing uriLibrary");
  Dart_Handle uri_lib = Builtin::LoadAndCheckLibrary(Builtin::kUriLibrary);
  CHECK_RESULT(uri_lib);
  Log::Print("Preparing builtinLibrary");
  Dart_Handle builtin_lib =
      Builtin::LoadAndCheckLibrary(Builtin::kBuiltinLibrary);
  CHECK_RESULT(builtin_lib);

  // Prepare for script loading by setting up the 'print' and 'timer'
  // closures and setting up 'package root' for URI resolution.
  char* package_root = NULL;
  Log::Print("Preparing for script loading");
  result = DartUtils::PrepareForScriptLoading(package_root, builtin_lib);
  CHECK_RESULT(result);

  Log::Print("Loading script %s", script_uri);
  library = DartUtils::LoadScript(script_uri, builtin_lib);

  CHECK_RESULT(library);
  if (!Dart_IsLibrary(library)) {
    Log::PrintErr("Expected a library when loading script: %s",
             script_uri);
    Dart_ExitScope();
    Dart_ShutdownIsolate();
    return false;
  }
  Dart_ExitScope();
  return true;
}

bool VMGlue::CreateIsolateAndSetup(const char* script_uri,
                                  const char* main,
                                  void* data, char** error) {
  return CreateIsolateAndSetupHelper(script_uri,
                                     main,
                                     new IsolateData(),
                                     error);
}

#define VMHOSTNAME  "android_dart_host"
#define MAINSCRIPT  "/data/data/com.google.dartndk/app_dart/main.dart"

void VMGlue::ShutdownIsolate(void* callback_data) {
  IsolateData* isolate_data = reinterpret_cast<IsolateData*>(callback_data);
  EventHandler* handler = isolate_data->event_handler;
  if (handler != NULL) handler->Shutdown();
  delete isolate_data;
}

int VMGlue::InitializeVM() {
  // Perform platform specific initialization.
  Log::Print("Initializing platform");
  if (!Platform::Initialize()) {
    Log::PrintErr("Initialization failed\n");
    return -1;
  }

  // We need the next call to get Datrt_Initialize not to bail early.
  Log::Print("Processing command line flags");
  dart::Flags::ProcessCommandLineFlags(0, NULL);

  // Initialize the Dart VM, providing the callbacks to use for
  // creating and shutting down isolates.
  Log::Print("Initializing Dart");
  if (!Dart_Initialize(CreateIsolateAndSetup,
                       NULL,
                       NULL,
                       ShutdownIsolate)) {
    Log::PrintErr("VM initialization failed\n");
    return -1;
  }
  DartUtils::SetOriginalWorkingDirectory();
  initialized_vm_ = true;
  return 0;
}

int VMGlue::StartMainIsolate() {
  if (!initialized_vm_) {
    int rtn = InitializeVM();
    if (rtn != 0) return rtn;
  }

  // Create an isolate and loads up the application script.
  char* error = NULL;
  if (!CreateIsolateAndSetup(MAINSCRIPT, "main", NULL, &error)) {
    Log::PrintErr("CreateIsolateAndSetup: %s\n", error);
    free(error);
    return -1;
  }

  Log::Print("Created isolate");

  isolate_ = Dart_CurrentIsolate();
  Dart_ExitIsolate();
  return 0;
}

int VMGlue::CallSetup() {
  if (!initialized_script_) {
    initialized_script_ = true;
    Log::Print("Invoking setup");
    Dart_EnterIsolate(isolate_);
    Dart_EnterScope();
    Dart_Handle args[2];
    args[0] = Dart_NewInteger(graphics_->width());
    args[1] = Dart_NewInteger(graphics_->height());
    int rtn = Invoke("setup", 2, args);
    Dart_ExitScope();
    Dart_ExitIsolate();
    Log::Print("Done setup");
    return rtn;
  }
  return 0;
}

int VMGlue::CallUpdate() {
  if (initialized_script_) {
    Log::Print("Invoking update");
    Dart_EnterIsolate(isolate_);
    Dart_EnterScope();
    int rtn = Invoke("update", 0, 0);
    Dart_ExitScope();
    Dart_ExitIsolate();
    Log::Print("Done update");
    return rtn;
  }
  return -1;
}

int VMGlue::OnMotionEvent(const char* pFunction, int64_t pWhen,
                  float pMoveX, float pMoveY) {
  if (initialized_script_) {
    Log::Print("Invoking %s", pFunction);
    Dart_EnterIsolate(isolate_);
    Dart_EnterScope();
    Dart_Handle args[3];
    args[0] = Dart_NewInteger(pWhen);
    args[1] = Dart_NewDouble(pMoveX);
    args[2] = Dart_NewDouble(pMoveY);
    int rtn = Invoke(pFunction, 3, args);
    Dart_ExitScope();
    Dart_ExitIsolate();
    Log::Print("Done %s", pFunction);
    return rtn;
  }
  return -1;
}

int VMGlue::OnKeyEvent(const char* function, int64_t when, int32_t flags,
           int32_t key_code, int32_t meta_state, int32_t repeat) {
  if (initialized_script_) {
    Log::Print("Invoking %s", function);
    Dart_EnterIsolate(isolate_);
    Dart_EnterScope();
    Dart_Handle args[5];
    args[0] = Dart_NewInteger(when);
    args[1] = Dart_NewInteger(flags);
    args[2] = Dart_NewInteger(key_code);
    args[3] = Dart_NewInteger(meta_state);
    args[4] = Dart_NewInteger(repeat);
    int rtn = Invoke(function, 5, args);
    Dart_ExitScope();
    Dart_ExitIsolate();
    Log::Print("Done %s", function);
    return rtn;
  }
  return -1;
}

int VMGlue::Invoke(const char* function, int argc, Dart_Handle* args) {
  Dart_Handle result;

  Log::Print("in invoke(%s)", function);

  // Create a dart options object that can be accessed from dart code.
  Log::Print("setting up runtime options");
  CommandLineOptions dart_options(0);
  Dart_Handle options_result =
      SetupRuntimeOptions(&dart_options, VMHOSTNAME, MAINSCRIPT);
  if (Dart_IsError(options_result)) {
    return ErrorExit("%s\n", Dart_GetError(options_result));
  }

  // Lookup the library of the root script.
  Log::Print("looking up the root library");
  Dart_Handle library = Dart_RootLibrary();
  if (Dart_IsNull(library)) {
     return ErrorExit("Unable to find root library\n");
  }

  // Lookup and invoke the appropriate function.
  result = Dart_Invoke(library, DartUtils::NewString(function), argc, args);

  if (Dart_IsError(result)) {
    return ErrorExit("%s\n", Dart_GetError(result));
  }

  // Keep handling messages until the last active receive port is closed.
  Log::Print("Entering Dart message loop");
  result = Dart_RunLoop();
  if (Dart_IsError(result)) {
    return ErrorExit("%s\n", Dart_GetError(result));
  }

  Log::Print("out invoke");
  return 0;
}

void VMGlue::FinishMainIsolate() {
  Log::Print("Finish main isolate");
  Dart_EnterIsolate(isolate_);
  // Shutdown the isolate.
  Dart_ShutdownIsolate();
  // Terminate process exit-code handler.
  Process::TerminateExitCodeHandler();
  isolate_ = NULL;
  initialized_script_ = false;
}

