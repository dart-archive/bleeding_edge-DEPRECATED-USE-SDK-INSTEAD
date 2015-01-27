// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_SERVICE_H_
#define VM_SERVICE_H_

#include "include/dart_api.h"

#include "vm/allocation.h"

namespace dart {

class Array;
class DebuggerEvent;
class EmbedderServiceHandler;
class GCEvent;
class Instance;
class Isolate;
class JSONStream;
class Object;
class RawInstance;
class String;

class Service : public AllStatic {
 public:
  // Handles a message which is not directed to an isolate.
  static void HandleRootMessage(const Instance& message);

  // Handles a message which is directed to a particular isolate.
  static void HandleIsolateMessage(Isolate* isolate, const Array& message);

  static Isolate* GetServiceIsolate(void* callback_data);
  static bool SendIsolateStartupMessage();
  static bool SendIsolateShutdownMessage();

  static bool IsRunning() {
    return port_ != ILLEGAL_PORT;
  }

  static void set_port(Dart_Port port) {
    port_ = port;
  }

  static void SetEventMask(uint32_t mask);

  // Is the service interested in debugger events?
  static bool NeedsDebuggerEvents() {
    return IsRunning() && ((event_mask_ & kEventFamilyDebugMask) != 0);
  }
  // Is the service interested in garbage collection events?
  static bool NeedsGCEvents() {
    return IsRunning() && ((event_mask_ & kEventFamilyGCMask) != 0);
  }

  static void HandleDebuggerEvent(DebuggerEvent* event);
  static void HandleGCEvent(GCEvent* event);

  static void RegisterIsolateEmbedderCallback(
      const char* name,
      Dart_ServiceRequestCallback callback,
      void* user_data);

  static void RegisterRootEmbedderCallback(
      const char* name,
      Dart_ServiceRequestCallback callback,
      void* user_data);

  static bool IsServiceIsolate(Isolate* isolate) {
    return isolate == service_isolate_;
  }

  static void SendEchoEvent(Isolate* isolate);
  static void SendGraphEvent(Isolate* isolate);

 private:
  // These must be kept in sync with service/constants.dart
  static const int kEventFamilyDebug = 0;
  static const int kEventFamilyGC = 1;
  static const uint32_t kEventFamilyDebugMask = (1 << kEventFamilyDebug);
  static const uint32_t kEventFamilyGCMask = (1 << kEventFamilyGC);

  static void EmbedderHandleMessage(EmbedderServiceHandler* handler,
                                    JSONStream* js);
  static EmbedderServiceHandler* FindIsolateEmbedderHandler(const char* name);
  static EmbedderServiceHandler* FindRootEmbedderHandler(const char* name);
  static Dart_Handle GetSource(const char* name);
  static Dart_Handle LibraryTagHandler(Dart_LibraryTag tag, Dart_Handle library,
                                       Dart_Handle url);
  static void SendEvent(intptr_t eventId, const Object& eventMessage);
  // Does not take ownership of 'data'.
  static void SendEvent(intptr_t eventId,
                        const String& meta,
                        const uint8_t* data,
                        intptr_t size);

  static EmbedderServiceHandler* isolate_service_handler_head_;
  static EmbedderServiceHandler* root_service_handler_head_;

  static Isolate* service_isolate_;
  static Dart_LibraryTagHandler embedder_provided_handler_;
  static Dart_Port port_;
  static uint32_t event_mask_;
};

}  // namespace dart

#endif  // VM_SERVICE_H_
