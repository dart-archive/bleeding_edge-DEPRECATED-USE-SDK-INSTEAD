// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_JSON_STREAM_H_
#define VM_JSON_STREAM_H_

#include "include/dart_api.h"  // for Dart_Port
#include "platform/json.h"
#include "vm/allocation.h"
#include "vm/service.h"

namespace dart {

class Array;
class Field;
class GrowableObjectArray;
class Instance;
class JSONArray;
class JSONObject;
class MessageQueue;
class Metric;
class Object;
class ServiceEvent;
class SourceBreakpoint;
class String;
class Zone;

class JSONStream : ValueObject {
 public:
  explicit JSONStream(intptr_t buf_size = 256);
  ~JSONStream();

  void Setup(Zone* zone,
             Dart_Port reply_port,
             const String& seq,
             const String& method,
             const Array& param_keys,
             const Array& param_values);
  void SetupError();

  void PostReply();

  void set_id_zone(ServiceIdZone* id_zone) {
    id_zone_ = id_zone;
  }
  ServiceIdZone* id_zone() {
    return id_zone_;
  }

  TextBuffer* buffer() { return &buffer_; }
  const char* ToCString() { return buffer_.buf(); }

  void set_reply_port(Dart_Port port);

  void SetParams(const char** param_keys, const char** param_values,
                 intptr_t num_params);

  Dart_Port reply_port() const { return reply_port_; }

  intptr_t num_params() const { return num_params_; }
  const char* GetParamKey(intptr_t i) const {
    return param_keys_[i];
  }
  const char* GetParamValue(intptr_t i) const {
    return param_values_[i];
  }

  const char* LookupParam(const char* key) const;

  bool HasParam(const char* key) const;

  // Returns true if there is an param with key and value, false
  // otherwise.
  bool ParamIs(const char* key, const char* value) const;

  const char* seq() const { return seq_; }
  const char* method() const { return method_; }
  const char** param_keys() const { return param_keys_; }
  const char** param_values() const { return param_values_; }

 private:
  void Clear();

  void OpenObject(const char* property_name = NULL);
  void CloseObject();

  void OpenArray(const char* property_name = NULL);
  void CloseArray();

  void PrintValueBool(bool b);
  void PrintValue(intptr_t i);
  void PrintValue64(int64_t i);
  void PrintValue(double d);
  void PrintValue(const char* s);
  void PrintValue(const char* s, intptr_t len);
  void PrintValueNoEscape(const char* s);
  void PrintfValue(const char* format, ...) PRINTF_ATTRIBUTE(2, 3);
  void PrintValue(const Object& o, bool ref = true);
  void PrintValue(SourceBreakpoint* bpt);
  void PrintValue(const ServiceEvent* event);
  void PrintValue(Metric* metric);
  void PrintValue(MessageQueue* queue);
  void PrintValue(Isolate* isolate, bool ref = true);
  bool PrintValueStr(const String& s, intptr_t limit);

  void PrintServiceId(const char* name, const Object& o);
  void PrintPropertyBool(const char* name, bool b);
  void PrintProperty(const char* name, intptr_t i);
  void PrintProperty64(const char* name, int64_t i);
  void PrintProperty(const char* name, double d);
  void PrintProperty(const char* name, const char* s);
  bool PrintPropertyStr(const char* name, const String& s, intptr_t limit);
  void PrintPropertyNoEscape(const char* name, const char* s);
  void PrintfProperty(const char* name, const char* format, ...)
  PRINTF_ATTRIBUTE(3, 4);
  void PrintProperty(const char* name, const Object& o, bool ref = true);

  void PrintProperty(const char* name, const ServiceEvent* event);
  void PrintProperty(const char* name, SourceBreakpoint* bpt);
  void PrintProperty(const char* name, Metric* metric);
  void PrintProperty(const char* name, MessageQueue* queue);
  void PrintProperty(const char* name, Isolate* isolate);
  void PrintPropertyName(const char* name);
  void PrintCommaIfNeeded();
  bool NeedComma();

  bool AddDartString(const String& s, intptr_t limit);
  void AddEscapedUTF8String(const char* s);
  void AddEscapedUTF8String(const char* s, intptr_t len);

  intptr_t nesting_level() const { return open_objects_; }

  intptr_t open_objects_;
  TextBuffer buffer_;
  // Default service id zone.
  RingServiceIdZone default_id_zone_;
  ServiceIdZone* id_zone_;
  Dart_Port reply_port_;
  const char* seq_;
  const char* method_;
  const char** param_keys_;
  const char** param_values_;
  intptr_t num_params_;
  int64_t setup_time_micros_;

  friend class JSONObject;
  friend class JSONArray;
};


class JSONObject : public ValueObject {
 public:
  explicit JSONObject(JSONStream* stream) : stream_(stream) {
    stream_->OpenObject();
  }
  JSONObject(const JSONObject* obj, const char* name) : stream_(obj->stream_) {
    stream_->OpenObject(name);
  }
  explicit JSONObject(const JSONArray* arr);

  ~JSONObject() {
    stream_->CloseObject();
  }

  void AddServiceId(const char* name, const Object& o) const {
    stream_->PrintServiceId(name, o);
  }

  void AddProperty(const char* name, bool b) const {
    stream_->PrintPropertyBool(name, b);
  }
  void AddProperty(const char* name, intptr_t i) const {
    stream_->PrintProperty(name, i);
  }
  void AddProperty64(const char* name, int64_t i) const {
    stream_->PrintProperty64(name, i);
  }
  void AddProperty(const char* name, double d) const {
    stream_->PrintProperty(name, d);
  }
  void AddProperty(const char* name, const char* s) const {
    stream_->PrintProperty(name, s);
  }
  bool AddPropertyStr(const char* name,
                      const String& s,
                      intptr_t limit = -1) const {
    return stream_->PrintPropertyStr(name, s, limit);
  }
  void AddPropertyNoEscape(const char* name, const char* s) const {
    stream_->PrintPropertyNoEscape(name, s);
  }
  void AddProperty(const char* name, const Object& obj, bool ref = true) const {
    stream_->PrintProperty(name, obj, ref);
  }
  void AddProperty(const char* name, const ServiceEvent* event) const {
    stream_->PrintProperty(name, event);
  }
  void AddProperty(const char* name, SourceBreakpoint* bpt) const {
    stream_->PrintProperty(name, bpt);
  }
  void AddProperty(const char* name, Metric* metric) const {
    stream_->PrintProperty(name, metric);
  }
  void AddProperty(const char* name, MessageQueue* queue) const {
    stream_->PrintProperty(name, queue);
  }
  void AddProperty(const char* name, Isolate* isolate) const {
    stream_->PrintProperty(name, isolate);
  }
  void AddPropertyF(const char* name, const char* format, ...) const
      PRINTF_ATTRIBUTE(3, 4);

 private:
  JSONStream* stream_;

  friend class JSONArray;

  DISALLOW_ALLOCATION();
  DISALLOW_COPY_AND_ASSIGN(JSONObject);
};


class JSONArray : public ValueObject {
 public:
  explicit JSONArray(JSONStream* stream) : stream_(stream) {
    stream_->OpenArray();
  }
  JSONArray(const JSONObject* obj, const char* name) : stream_(obj->stream_) {
    stream_->OpenArray(name);
  }
  explicit JSONArray(const JSONArray* arr) : stream_(arr->stream_) {
    stream_->OpenArray();
  }
  ~JSONArray() {
    stream_->CloseArray();
  }

  void AddValue(bool b) const { stream_->PrintValueBool(b); }
  void AddValue(intptr_t i) const { stream_->PrintValue(i); }
  void AddValue64(int64_t i) const { stream_->PrintValue64(i); }
  void AddValue(double d) const { stream_->PrintValue(d); }
  void AddValue(const char* s) const { stream_->PrintValue(s); }
  void AddValue(const Object& obj, bool ref = true) const {
    stream_->PrintValue(obj, ref);
  }
  void AddValue(Isolate* isolate, bool ref = true) const {
    stream_->PrintValue(isolate, ref);
  }
  void AddValue(SourceBreakpoint* bpt) const {
    stream_->PrintValue(bpt);
  }
  void AddValue(const ServiceEvent* event) const {
    stream_->PrintValue(event);
  }
  void AddValue(Metric* metric) const {
    stream_->PrintValue(metric);
  }
  void AddValue(MessageQueue* queue) const {
    stream_->PrintValue(queue);
  }
  void AddValueF(const char* format, ...) const PRINTF_ATTRIBUTE(2, 3);

 private:
  JSONStream* stream_;

  friend class JSONObject;

  DISALLOW_ALLOCATION();
  DISALLOW_COPY_AND_ASSIGN(JSONArray);
};

}  // namespace dart

#endif  // VM_JSON_STREAM_H_
