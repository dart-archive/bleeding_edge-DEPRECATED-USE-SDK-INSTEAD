// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_CHA_H_
#define VM_CHA_H_

#include "vm/allocation.h"
#include "vm/growable_array.h"
#include "vm/thread.h"

namespace dart {

class Class;
class Function;
template <typename T> class ZoneGrowableArray;
class String;

class CHA : public StackResource {
 public:
  explicit CHA(Thread* thread)
      : StackResource(thread->isolate()),
        thread_(thread),
        leaf_classes_(thread->zone(), 1),
        previous_(thread->cha()) {
    thread->set_cha(this);
  }

  ~CHA() {
    ASSERT(thread_->cha() == this);
    thread_->set_cha(previous_);
  }

  // Returns true if the class has subclasses.
  // Updates set of leaf classes that we register optimized code with for lazy
  // deoptimization.
  bool HasSubclasses(const Class& cls);
  bool HasSubclasses(intptr_t cid);

  // Return true if the class is implemented by some other class.
  // Updates set of leaf classes that we register optimized code with for lazy
  // deoptimization.
  bool IsImplemented(const Class& cls);

  // Returns true if any subclass of 'cls' contains the function.
  // Updates set of leaf classes that we register optimized code with for lazy
  // deoptimization.
  bool HasOverride(const Class& cls, const String& function_name);

  const GrowableArray<Class*>& leaf_classes() const {
    return leaf_classes_;
  }

 private:
  void AddToLeafClasses(const Class& cls);

  Thread* thread_;
  GrowableArray<Class*> leaf_classes_;
  CHA* previous_;
};

}  // namespace dart

#endif  // VM_CHA_H_
