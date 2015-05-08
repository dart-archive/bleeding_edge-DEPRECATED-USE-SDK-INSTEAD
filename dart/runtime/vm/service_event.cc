// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/service_event.h"

namespace dart {

// Translate from the legacy DebugEvent to a ServiceEvent.
static ServiceEvent::EventType TranslateEventType(
    DebuggerEvent::EventType type) {
    switch (type) {
      case DebuggerEvent::kIsolateCreated:
        return ServiceEvent::kIsolateStart;

      case DebuggerEvent::kIsolateShutdown:
        return ServiceEvent::kIsolateExit;

      case DebuggerEvent::kBreakpointReached:
        return ServiceEvent::kPauseBreakpoint;

      case DebuggerEvent::kIsolateInterrupted:
        return ServiceEvent::kPauseInterrupted;

      case DebuggerEvent::kExceptionThrown:
        return ServiceEvent::kPauseException;

      default:
        UNREACHABLE();
        return ServiceEvent::kIllegal;
    }
}

ServiceEvent::ServiceEvent(const DebuggerEvent* debugger_event)
    : isolate_(debugger_event->isolate()),
      type_(TranslateEventType(debugger_event->type())),
      breakpoint_(NULL),
      top_frame_(NULL),
      exception_(NULL),
      inspectee_(NULL),
      gc_stats_(NULL) {
  DebuggerEvent::EventType type = debugger_event->type();
  if (type == DebuggerEvent::kBreakpointReached) {
    set_breakpoint(debugger_event->breakpoint());
  }
  if (type == DebuggerEvent::kExceptionThrown) {
    set_exception(debugger_event->exception());
  }
  if (type == DebuggerEvent::kBreakpointReached ||
      type == DebuggerEvent::kIsolateInterrupted ||
      type == DebuggerEvent::kExceptionThrown) {
    set_top_frame(debugger_event->top_frame());
  }
}


const char* ServiceEvent::EventTypeToCString(EventType type) {
  switch (type) {
    case kIsolateStart:
      return "IsolateStart";
    case kIsolateExit:
      return "IsolateExit";
    case kIsolateUpdate:
      return "IsolateUpdate";
    case kPauseStart:
      return "PauseStart";
    case kPauseExit:
      return "PauseExit";
    case kPauseBreakpoint:
      return "PauseBreakpoint";
    case kPauseInterrupted:
      return "PauseInterrupted";
    case kPauseException:
      return "PauseException";
    case kResume:
      return "Resume";
    case kBreakpointAdded:
      return "BreakpointAdded";
    case kBreakpointResolved:
      return "BreakpointResolved";
    case kBreakpointRemoved:
      return "BreakpointRemoved";
    case kGC:
      return "GC";  // TODO(koda): Change to GarbageCollected.
    case kInspect:
      return "Inspect";
    default:
      UNREACHABLE();
      return "Unknown";
  }
}


void ServiceEvent::PrintJSON(JSONStream* js) const {
  JSONObject jsobj(js);
  jsobj.AddProperty("type", "ServiceEvent");
  jsobj.AddProperty("eventType", EventTypeToCString(type()));
  jsobj.AddProperty("isolate", isolate());
  if (breakpoint() != NULL) {
    jsobj.AddProperty("breakpoint", breakpoint());
  }
  if (top_frame() != NULL) {
    JSONObject jsFrame(&jsobj, "topFrame");
    top_frame()->PrintToJSONObject(&jsFrame);
  }
  if (exception() != NULL) {
    jsobj.AddProperty("exception", *(exception()));
  }
  if (inspectee() != NULL) {
    jsobj.AddProperty("inspectee", *(inspectee()));
  }
  if (gc_stats() != NULL) {
    jsobj.AddProperty("reason", Heap::GCReasonToString(gc_stats()->reason_));
    isolate()->heap()->PrintToJSONObject(Heap::kNew, &jsobj);
    isolate()->heap()->PrintToJSONObject(Heap::kOld, &jsobj);
  }
}

}  // namespace dart
