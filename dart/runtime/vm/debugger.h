// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_DEBUGGER_H_
#define VM_DEBUGGER_H_

#include "include/dart_debugger_api.h"

#include "vm/object.h"
#include "vm/port.h"

namespace dart {

class ActiveVariables;
class CodeBreakpoint;
class Isolate;
class JSONArray;
class JSONStream;
class ObjectPointerVisitor;
class RemoteObjectCache;
class SourceBreakpoint;
class StackFrame;

// SourceBreakpoint represents a user-specified breakpoint location in
// Dart source. There may be more than one CodeBreakpoint object per
// SourceBreakpoint.
class SourceBreakpoint {
 public:
  SourceBreakpoint(intptr_t id, const Script& script, intptr_t token_pos);

  RawFunction* function() const { return function_; }
  intptr_t token_pos() const { return token_pos_; }
  intptr_t id() const { return id_; }

  RawScript* script() { return script_; }
  RawString* SourceUrl();
  intptr_t LineNumber();

  void GetCodeLocation(Library* lib, Script* script, intptr_t* token_pos);

  void Enable();
  void Disable();
  bool IsEnabled() const { return is_enabled_; }
  bool IsResolved() { return is_resolved_; }

  void PrintToJSONStream(JSONStream* stream);

 private:
  void VisitObjectPointers(ObjectPointerVisitor* visitor);

  void SetResolved(const Function& func, intptr_t token_pos);
  void set_next(SourceBreakpoint* value) { next_ = value; }
  SourceBreakpoint* next() const { return this->next_; }

  const intptr_t id_;
  RawScript* script_;
  intptr_t token_pos_;
  bool is_resolved_;
  bool is_enabled_;
  SourceBreakpoint* next_;

  // Valid for resolved breakpoints:
  RawFunction* function_;
  intptr_t line_number_;

  friend class Debugger;
  DISALLOW_COPY_AND_ASSIGN(SourceBreakpoint);
};


// CodeBreakpoint represents a location in compiled code. There may be
// more than one CodeBreakpoint for one SourceBreakpoint, e.g. when a
// function gets compiled as a regular function and as a closure.
class CodeBreakpoint {
 public:
  CodeBreakpoint(const Code& code, intptr_t pc_desc_index);
  ~CodeBreakpoint();

  RawFunction* function() const;
  uword pc() const { return pc_; }
  intptr_t token_pos() const { return token_pos_; }
  bool IsInternal() const { return src_bpt_ == NULL; }

  RawScript* SourceCode();
  RawString* SourceUrl();
  intptr_t LineNumber();

  void Enable();
  void Disable();
  bool IsEnabled() const { return is_enabled_; }

  uword OrigStubAddress() const;

 private:
  void VisitObjectPointers(ObjectPointerVisitor* visitor);

  SourceBreakpoint* src_bpt() const { return src_bpt_; }
  void set_src_bpt(SourceBreakpoint* value) { src_bpt_ = value; }

  void set_next(CodeBreakpoint* value) { next_ = value; }
  CodeBreakpoint* next() const { return this->next_; }
  intptr_t pc_desc_index() const { return pc_desc_index_; }

  void PatchCode();
  void RestoreCode();

  RawCode* code_;
  intptr_t pc_desc_index_;
  intptr_t token_pos_;
  uword pc_;
  intptr_t line_number_;
  bool is_enabled_;

  SourceBreakpoint* src_bpt_;
  CodeBreakpoint* next_;

  PcDescriptors::Kind breakpoint_kind_;
  uword saved_value_;

  friend class Debugger;
  DISALLOW_COPY_AND_ASSIGN(CodeBreakpoint);
};


// ActivationFrame represents one dart function activation frame
// on the call stack.
class ActivationFrame : public ZoneAllocated {
 public:
  ActivationFrame(uword pc, uword fp, uword sp, const Code& code,
                  const Array& deopt_frame, intptr_t deopt_frame_offset);

  uword pc() const { return pc_; }
  uword fp() const { return fp_; }
  uword sp() const { return sp_; }
  const Function& function() const {
    ASSERT(!function_.IsNull());
    return function_;
  }
  const Code& code() const {
    ASSERT(!code_.IsNull());
    return code_;
  }

  RawString* QualifiedFunctionName();
  RawString* SourceUrl();
  RawScript* SourceScript();
  RawLibrary* Library();
  intptr_t TokenPos();
  intptr_t LineNumber();
  intptr_t ColumnNumber();
  void SetContext(const Context& ctx) { ctx_ = ctx.raw(); }

  // Returns true if this frame is for a function that is visible
  // to the user and can be debugged.
  bool IsDebuggable() const;

  // The context level of a frame is the context level at the
  // PC/token index of the frame. It determines the depth of the context
  // chain that belongs to the function of this activation frame.
  intptr_t ContextLevel();

  const char* ToCString();

  intptr_t NumLocalVariables();

  void VariableAt(intptr_t i,
                  String* name,
                  intptr_t* token_pos,
                  intptr_t* end_pos,
                  Instance* value);

  RawArray* GetLocalVariables();
  RawContext* GetSavedEntryContext();
  RawContext* GetSavedCurrentContext();

  void PrintToJSONObject(JSONObject* jsobj);

 private:
  intptr_t PcDescIndex();
  intptr_t TryIndex();
  void GetPcDescriptors();
  void GetVarDescriptors();
  void GetDescIndices();

  RawObject* GetLocalVar(intptr_t slot_index);
  RawInstance* GetLocalInstanceVar(intptr_t slot_index);
  RawContext* GetLocalContextVar(intptr_t slot_index);

  RawInstance* GetLocalVarValue(intptr_t slot_index);
  RawInstance* GetInstanceCallReceiver(intptr_t num_actual_args);
  RawObject* GetClosureObject(intptr_t num_acatual_args);

  uword pc_;
  uword fp_;
  uword sp_;

  // The anchor of the context chain for this function.
  Context& ctx_;
  const Code& code_;
  const Function& function_;
  intptr_t token_pos_;
  intptr_t pc_desc_index_;
  intptr_t line_number_;
  intptr_t column_number_;
  intptr_t context_level_;

  // Some frames are deoptimized into a side array in order to inspect them.
  const Array& deopt_frame_;
  const intptr_t deopt_frame_offset_;

  bool vars_initialized_;
  LocalVarDescriptors& var_descriptors_;
  ZoneGrowableArray<intptr_t> desc_indices_;
  PcDescriptors& pc_desc_;

  friend class Debugger;
  friend class DebuggerStackTrace;
  DISALLOW_COPY_AND_ASSIGN(ActivationFrame);
};


// Array of function activations on the call stack.
class DebuggerStackTrace : public ZoneAllocated {
 public:
  explicit DebuggerStackTrace(int capacity)
      : trace_(capacity) { }

  intptr_t Length() const { return trace_.length(); }

  ActivationFrame* FrameAt(int i) const {
    return trace_[i];
  }

  ActivationFrame* GetHandlerFrame(const Instance& exc_obj) const;

 private:
  void AddActivation(ActivationFrame* frame);
  ZoneGrowableArray<ActivationFrame*> trace_;

  friend class Debugger;
  DISALLOW_COPY_AND_ASSIGN(DebuggerStackTrace);
};


typedef void BreakpointHandler(Dart_Port isolate_id,
                               SourceBreakpoint* bpt,
                               DebuggerStackTrace* stack);


class Debugger {
 public:
  enum EventType {
    kBreakpointReached = 1,
    kBreakpointResolved = 2,
    kExceptionThrown = 3,
    kIsolateCreated = 4,
    kIsolateShutdown = 5,
    kIsolateInterrupted = 6,
  };
  struct DebuggerEvent {
    explicit DebuggerEvent(EventType event_type)
        : type(event_type) {
      top_frame = NULL;
      breakpoint = NULL;
      exception = NULL;
      isolate_id = 0;
    }
    EventType type;
    // type == kBreakpointReached.
    ActivationFrame* top_frame;
    // type == kBreakpointResolved, kBreakpointReached.
    SourceBreakpoint* breakpoint;
    // type == kExceptionThrown.
    const Object* exception;
    // type == kIsolate(Created|Shutdown|Interrupted).
    Dart_Port isolate_id;
  };
  typedef void EventHandler(DebuggerEvent *event);

  Debugger();
  ~Debugger();

  void Initialize(Isolate* isolate);
  void Shutdown();

  void NotifyCompilation(const Function& func);

  RawFunction* ResolveFunction(const Library& library,
                               const String& class_name,
                               const String& function_name);

  // Set breakpoint at closest location to function entry.
  SourceBreakpoint* SetBreakpointAtEntry(const Function& target_function);
  SourceBreakpoint* SetBreakpointAtLine(const String& script_url,
                                        intptr_t line_number);
  void OneTimeBreakAtEntry(const Function& target_function);

  void RemoveBreakpoint(intptr_t bp_id);
  SourceBreakpoint* GetBreakpointById(intptr_t id);

  void SetStepOver();
  void SetSingleStep();
  void SetStepOut();
  bool IsStepping() const { return resume_action_ != kContinue; }

  bool IsPaused() const { return pause_event_ != NULL; }

  // Indicates why the debugger is currently paused.  If the debugger
  // is not paused, this returns NULL.  Note that the debugger can be
  // paused for breakpoints, isolate interruption, and (sometimes)
  // exceptions.
  const DebuggerEvent* PauseEvent() const { return pause_event_; }

  void SetExceptionPauseInfo(Dart_ExceptionPauseInfo pause_info);
  Dart_ExceptionPauseInfo GetExceptionPauseInfo();

  void VisitObjectPointers(ObjectPointerVisitor* visitor);

  // Called from Runtime when a breakpoint in Dart code is reached.
  void BreakpointCallback();

  // Returns true if there is at least one breakpoint set in func or code.
  // Checks for both user-defined and internal temporary breakpoints.
  bool HasBreakpoint(const Function& func);
  bool HasBreakpoint(const Code& code);

  // Returns true if the call at address pc is patched to point to
  // a debugger stub.
  bool HasActiveBreakpoint(uword pc);

  // Returns a stack trace with frames corresponding to invisible functions
  // omitted. CurrentStackTrace always returns a new trace on the current stack.
  // The trace returned by StackTrace may have been cached; it is suitable for
  // use when stepping, but otherwise may be out of sync with the current stack.
  DebuggerStackTrace* StackTrace();
  DebuggerStackTrace* CurrentStackTrace();

  // Returns a debugger stack trace corresponding to a dart.core.Stacktrace.
  // Frames corresponding to invisible functions are omitted. It is not valid
  // to query local variables in the returned stack.
  DebuggerStackTrace* StackTraceFrom(const Stacktrace& dart_stacktrace);

  RawArray* GetInstanceFields(const Instance& obj);
  RawArray* GetStaticFields(const Class& cls);
  RawArray* GetLibraryFields(const Library& lib);
  RawArray* GetGlobalFields(const Library& lib);

  intptr_t CacheObject(const Object& obj);
  RawObject* GetCachedObject(intptr_t obj_id);
  bool IsValidObjectId(intptr_t obj_id);

  Dart_Port GetIsolateId() { return isolate_id_; }

  static void SetEventHandler(EventHandler* handler);

  // Utility functions.
  static const char* QualifiedFunctionName(const Function& func);

  RawObject* GetInstanceField(const Class& cls,
                              const String& field_name,
                              const Instance& object);
  RawObject* GetStaticField(const Class& cls,
                            const String& field_name);

  void SignalBpReached();
  void DebuggerStepCallback();

  void SignalExceptionThrown(const Instance& exc);
  void SignalIsolateEvent(EventType type);
  static void SignalIsolateInterrupted();

  uword GetPatchedStubAddress(uword breakpoint_address);

  void PrintBreakpointsToJSONArray(JSONArray* jsarr) const;

  static bool IsDebuggable(const Function& func);

 private:
  enum ResumeAction {
    kContinue,
    kStepOver,
    kStepOut,
    kSingleStep
  };

  void FindCompiledFunctions(const Script& script,
                             intptr_t start_pos,
                             intptr_t end_pos,
                             GrowableObjectArray* function_list);
  RawFunction* FindBestFit(const Script& script, intptr_t token_pos);
  RawFunction* FindInnermostClosure(const Function& function,
                                    intptr_t token_pos);
  intptr_t ResolveBreakpointPos(const Function& func,
                                intptr_t requested_token_pos);
  void DeoptimizeWorld();
  void SetInternalBreakpoints(const Function& target_function);
  SourceBreakpoint* SetBreakpoint(const Script& script, intptr_t token_pos);
  SourceBreakpoint* SetBreakpoint(const Function& target_function,
                                  intptr_t first_token_pos,
                                  intptr_t last_token_pos);
  void RemoveInternalBreakpoints();
  void UnlinkCodeBreakpoints(SourceBreakpoint* src_bpt);
  void RegisterSourceBreakpoint(SourceBreakpoint* bpt);
  void RegisterCodeBreakpoint(CodeBreakpoint* bpt);
  SourceBreakpoint* GetSourceBreakpoint(const Script& script,
                                        intptr_t token_pos);
  void MakeCodeBreakpointsAt(const Function& func,
                             SourceBreakpoint* bpt);
  // Returns NULL if no breakpoint exists for the given address.
  CodeBreakpoint* GetCodeBreakpoint(uword breakpoint_address);

  void SyncBreakpoint(SourceBreakpoint* bpt);

  ActivationFrame* TopDartFrame() const;
  static ActivationFrame* CollectDartFrame(Isolate* isolate,
                                           uword pc,
                                           StackFrame* frame,
                                           const Code& code,
                                           const Array& deopt_frame,
                                           intptr_t deopt_frame_offset,
                                           ActivationFrame* callee_activation,
                                           const Context& entry_ctx);
  static RawArray* DeoptimizeToArray(Isolate* isolate,
                                     StackFrame* frame,
                                     const Code& code);
  static DebuggerStackTrace* CollectStackTrace();
  void SignalBpResolved(SourceBreakpoint *bpt);
  void SignalPausedEvent(ActivationFrame* top_frame,
                         SourceBreakpoint* bpt);

  intptr_t nextId() { return next_id_++; }

  bool ShouldPauseOnException(DebuggerStackTrace* stack_trace,
                              const Instance& exc);

  void CollectLibraryFields(const GrowableObjectArray& field_list,
                            const Library& lib,
                            const String& prefix,
                            bool include_private_fields);

  // Handles any events which pause vm execution.  Breakpoints,
  // interrupts, etc.
  void Pause(DebuggerEvent* event);

  void HandleSteppingRequest(DebuggerStackTrace* stack_trace);

  Isolate* isolate_;
  Dart_Port isolate_id_;  // A unique ID for the isolate in the debugger.
  bool initialized_;

  // ID number generator.
  intptr_t next_id_;


  SourceBreakpoint* src_breakpoints_;
  CodeBreakpoint* code_breakpoints_;

  // Tells debugger what to do when resuming execution after a breakpoint.
  ResumeAction resume_action_;

  // Do not call back to breakpoint handler if this flag is set.
  // Effectively this means ignoring breakpoints. Set when Dart code may
  // be run as a side effect of getting values of fields.
  bool ignore_breakpoints_;

  // Indicates why the debugger is currently paused.  If the debugger
  // is not paused, this is NULL.  Note that the debugger can be
  // paused for breakpoints, isolate interruption, and (sometimes)
  // exceptions.
  DebuggerEvent* pause_event_;

  // An id -> object map.  Valid only while IsPaused().
  RemoteObjectCache* obj_cache_;

  // Current stack trace. Valid only while IsPaused().
  DebuggerStackTrace* stack_trace_;

  // When stepping through code, only pause the program if the top
  // frame corresponds to this fp value, or if the top frame is
  // lower on the stack.
  uword stepping_fp_;

  Dart_ExceptionPauseInfo exc_pause_info_;

  static EventHandler* event_handler_;

  friend class Isolate;
  friend class SourceBreakpoint;
  DISALLOW_COPY_AND_ASSIGN(Debugger);
};


}  // namespace dart

#endif  // VM_DEBUGGER_H_
