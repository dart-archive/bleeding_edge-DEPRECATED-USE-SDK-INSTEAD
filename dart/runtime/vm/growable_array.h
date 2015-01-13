// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Defines growable array classes, that differ where they are allocated:
// - GrowableArray: allocated on stack.
// - ZoneGrowableArray: allocated in the zone.
// - MallocGrowableArray: allocates using malloc/realloc; free is only called
//   at destruction.

#ifndef VM_GROWABLE_ARRAY_H_
#define VM_GROWABLE_ARRAY_H_

#include "platform/utils.h"
#include "vm/allocation.h"
#include "vm/isolate.h"
#include "vm/zone.h"

namespace dart {

template<typename T, typename B, typename Allocator = Zone>
class BaseGrowableArray : public B {
 public:
  explicit BaseGrowableArray(Allocator* allocator)
      : length_(0), capacity_(0), data_(NULL), allocator_(allocator) {}

  BaseGrowableArray(intptr_t initial_capacity, Allocator* allocator)
      : length_(0), capacity_(0), data_(NULL), allocator_(allocator) {
    if (initial_capacity > 0) {
      capacity_ = Utils::RoundUpToPowerOfTwo(initial_capacity);
      data_ = allocator_->template Alloc<T>(capacity_);
    }
  }

  ~BaseGrowableArray() {
    allocator_->template Free<T>(data_, capacity_);
  }

  intptr_t length() const { return length_; }
  T* data() const { return data_; }
  bool is_empty() const { return length_ == 0; }

  void TruncateTo(intptr_t length) {
    ASSERT(length_ >= length);
    length_ = length;
  }

  void Add(const T& value) {
    Resize(length() + 1);
    Last() = value;
  }

  T& RemoveLast() {
    ASSERT(length_ > 0);
    T& result = operator[](length_ - 1);
    length_--;
    return result;
  }

  T& operator[](intptr_t index) const {
    ASSERT(0 <= index);
    ASSERT(index < length_);
    ASSERT(length_ <= capacity_);
    return data_[index];
  }

  const T& At(intptr_t index) const {
    return operator[](index);
  }

  T& Last() const {
    ASSERT(length_ > 0);
    return operator[](length_ - 1);
  }

  void AddArray(const BaseGrowableArray<T, B>& src) {
    for (intptr_t i = 0; i < src.length(); i++) {
      Add(src[i]);
    }
  }

  void Clear() {
    length_ = 0;
  }

  void InsertAt(intptr_t idx, const T& value) {
    Resize(length() + 1);
    for (intptr_t i = length_ - 2; i >= idx; i--) {
      data_[i + 1] = data_[i];
    }
    data_[idx] = value;
  }

  // The content is uninitialized after calling it.
  void SetLength(intptr_t new_length);

  // Sort the array in place.
  inline void Sort(int compare(const T*, const T*));

 private:
  intptr_t length_;
  intptr_t capacity_;
  T* data_;
  Allocator* allocator_;  // Used to (re)allocate the array.

  // Used for growing the array.
  void Resize(intptr_t new_length);

  DISALLOW_COPY_AND_ASSIGN(BaseGrowableArray);
};


template<typename T, typename B, typename Allocator>
inline void BaseGrowableArray<T, B, Allocator>::Sort(
    int compare(const T*, const T*)) {
  typedef int (*CompareFunction)(const void*, const void*);
  qsort(data_, length_, sizeof(T), reinterpret_cast<CompareFunction>(compare));
}


template<typename T, typename B, typename Allocator>
void BaseGrowableArray<T, B, Allocator>::Resize(intptr_t new_length) {
  if (new_length > capacity_) {
    intptr_t new_capacity = Utils::RoundUpToPowerOfTwo(new_length);
    T* new_data =
        allocator_->template Realloc<T>(data_, capacity_, new_capacity);
    ASSERT(new_data != NULL);
    data_ = new_data;
    capacity_ = new_capacity;
  }
  length_ = new_length;
}


template<typename T, typename B, typename Allocator>
void BaseGrowableArray<T, B, Allocator>::SetLength(intptr_t new_length) {
  if (new_length > capacity_) {
    T* new_data = allocator_->template Alloc<T>(new_length);
    ASSERT(new_data != NULL);
    data_ = new_data;
    capacity_ = new_length;
  }
  length_ = new_length;
}


template<typename T>
class GrowableArray : public BaseGrowableArray<T, ValueObject> {
 public:
  GrowableArray(Isolate* isolate, intptr_t initial_capacity)
      : BaseGrowableArray<T, ValueObject>(
          initial_capacity, ASSERT_NOTNULL(isolate->current_zone())) {}
  explicit GrowableArray(intptr_t initial_capacity)
      : BaseGrowableArray<T, ValueObject>(
          initial_capacity,
          ASSERT_NOTNULL(Isolate::Current()->current_zone())) {}
  GrowableArray()
      : BaseGrowableArray<T, ValueObject>(
          ASSERT_NOTNULL(Isolate::Current()->current_zone())) {}
};


template<typename T>
class ZoneGrowableArray : public BaseGrowableArray<T, ZoneAllocated> {
 public:
  ZoneGrowableArray(Isolate* isolate, intptr_t initial_capacity)
      : BaseGrowableArray<T, ZoneAllocated>(
          initial_capacity, ASSERT_NOTNULL(isolate->current_zone())) {}
  explicit ZoneGrowableArray(intptr_t initial_capacity)
      : BaseGrowableArray<T, ZoneAllocated>(
          initial_capacity,
          ASSERT_NOTNULL(Isolate::Current()->current_zone())) {}
  ZoneGrowableArray()
      : BaseGrowableArray<T, ZoneAllocated>(
          ASSERT_NOTNULL(Isolate::Current()->current_zone())) {}
};


class Malloc : public AllStatic {
 public:
  template <class T>
  static inline T* Alloc(intptr_t len) {
    return reinterpret_cast<T*>(malloc(len * sizeof(T)));
  }

  template <class T>
  static inline T* Realloc(T* old_array, intptr_t old_len, intptr_t new_len) {
    return reinterpret_cast<T*>(realloc(old_array, new_len * sizeof(T)));
  }

  template <class T>
  static inline void Free(T* old_array, intptr_t old_len) {
    free(old_array);
  }
};


class EmptyBase {};


template<typename T>
class MallocGrowableArray : public BaseGrowableArray<T, EmptyBase, Malloc> {
 public:
  explicit MallocGrowableArray(intptr_t initial_capacity)
      : BaseGrowableArray<T, EmptyBase, Malloc>(initial_capacity, NULL) {}
  MallocGrowableArray()
      : BaseGrowableArray<T, EmptyBase, Malloc>(NULL) {}
};

}  // namespace dart

#endif  // VM_GROWABLE_ARRAY_H_
