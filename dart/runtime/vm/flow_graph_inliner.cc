// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/flow_graph_inliner.h"

#include "vm/block_scheduler.h"
#include "vm/compiler.h"
#include "vm/flags.h"
#include "vm/flow_graph.h"
#include "vm/flow_graph_builder.h"
#include "vm/flow_graph_compiler.h"
#include "vm/flow_graph_optimizer.h"
#include "vm/il_printer.h"
#include "vm/intrinsifier.h"
#include "vm/longjump.h"
#include "vm/object.h"
#include "vm/object_store.h"
#include "vm/timer.h"

namespace dart {

DEFINE_FLAG(int, deoptimization_counter_inlining_threshold, 12,
    "How many times we allow deoptimization before we stop inlining.");
DEFINE_FLAG(bool, trace_inlining, false, "Trace inlining");
DEFINE_FLAG(charp, inlining_filter, NULL, "Inline only in named function");

// Flags for inlining heuristics.
DEFINE_FLAG(int, inlining_depth_threshold, 3,
    "Inline function calls up to threshold nesting depth");
DEFINE_FLAG(int, inlining_size_threshold, 25,
    "Always inline functions that have threshold or fewer instructions");
DEFINE_FLAG(int, inlining_callee_call_sites_threshold, 1,
    "Always inline functions containing threshold or fewer calls.");
DEFINE_FLAG(int, inlining_caller_size_threshold, 50000,
    "Stop inlining once caller reaches the threshold.");
DEFINE_FLAG(int, inlining_constant_arguments_count, 1,
    "Inline function calls with sufficient constant arguments "
    "and up to the increased threshold on instructions");
DEFINE_FLAG(int, inlining_constant_arguments_size_threshold, 60,
    "Inline function calls with sufficient constant arguments "
    "and up to the increased threshold on instructions");
DEFINE_FLAG(int, inlining_hotness, 10,
    "Inline only hotter calls, in percents (0 .. 100); "
    "default 10%: calls above-equal 10% of max-count are inlined.");
DEFINE_FLAG(bool, inline_recursive, true,
    "Inline recursive calls.");

DECLARE_FLAG(bool, print_flow_graph);
DECLARE_FLAG(bool, print_flow_graph_optimized);
DECLARE_FLAG(int, deoptimization_counter_threshold);
DECLARE_FLAG(bool, verify_compiler);
DECLARE_FLAG(bool, compiler_stats);

#define TRACE_INLINING(statement)                                              \
  do {                                                                         \
    if (FLAG_trace_inlining) statement;                                        \
  } while (false)


// Test if a call is recursive by looking in the deoptimization environment.
static bool IsCallRecursive(const Function& function, Definition* call) {
  Environment* env = call->env();
  while (env != NULL) {
    if (function.raw() == env->function().raw()) return true;
    env = env->outer();
  }
  return false;
}


// TODO(zerny): Remove the ChildrenVisitor and SourceLabelResetter once we have
// moved the label/join map for control flow out of the AST and into the flow
// graph builder.

// Default visitor to traverse child nodes.
class ChildrenVisitor : public AstNodeVisitor {
 public:
  ChildrenVisitor() { }
#define DEFINE_VISIT(BaseName)                                                 \
  virtual void Visit##BaseName##Node(BaseName##Node* node) {                   \
    node->VisitChildren(this);                                                 \
  }

  FOR_EACH_NODE(DEFINE_VISIT);
#undef DEFINE_VISIT
};


// Visitor to clear each AST node containing source labels.
class SourceLabelResetter : public ChildrenVisitor {
 public:
  SourceLabelResetter() { }
  virtual void VisitSequenceNode(SequenceNode* node) {
    Reset(node, node->label());
  }
  virtual void VisitCaseNode(CaseNode* node) {
    Reset(node, node->label());
  }
  virtual void VisitSwitchNode(SwitchNode* node) {
    Reset(node, node->label());
  }
  virtual void VisitWhileNode(WhileNode* node) {
    Reset(node, node->label());
  }
  virtual void VisitDoWhileNode(DoWhileNode* node) {
    Reset(node, node->label());
  }
  virtual void VisitForNode(ForNode* node) {
    Reset(node, node->label());
  }
  virtual void VisitJumpNode(JumpNode* node) {
    Reset(node, node->label());
  }
  void Reset(AstNode* node, SourceLabel* lbl) {
    node->VisitChildren(this);
    if (lbl == NULL) return;
    lbl->join_for_break_ = NULL;
    lbl->join_for_continue_ = NULL;
  }
};


// Helper to create a parameter stub from an actual argument.
static Definition* CreateParameterStub(intptr_t i,
                                       Value* argument,
                                       FlowGraph* graph) {
  ConstantInstr* constant = argument->definition()->AsConstant();
  if (constant != NULL) {
    return new ConstantInstr(constant->value());
  } else {
    return new ParameterInstr(i, graph->graph_entry());
  }
}


// Helper to get the default value of a formal parameter.
static ConstantInstr* GetDefaultValue(intptr_t i,
                                      const ParsedFunction& parsed_function) {
  return new ConstantInstr(Object::ZoneHandle(
      parsed_function.default_parameter_values().At(i)));
}


// Pair of an argument name and its value.
struct NamedArgument {
  String* name;
  Value* value;
  NamedArgument(String* name, Value* value)
    : name(name), value(value) { }
};


// Helper to collect information about a callee graph when considering it for
// inlining.
class GraphInfoCollector : public ValueObject {
 public:
  GraphInfoCollector()
      : call_site_count_(0),
        instruction_count_(0) { }

  void Collect(const FlowGraph& graph) {
    call_site_count_ = 0;
    instruction_count_ = 0;
    for (BlockIterator block_it = graph.postorder_iterator();
         !block_it.Done();
         block_it.Advance()) {
      for (ForwardInstructionIterator it(block_it.Current());
           !it.Done();
           it.Advance()) {
        ++instruction_count_;
        Instruction* current = it.Current();
        if (current->IsStaticCall() ||
            current->IsClosureCall()) {
          ++call_site_count_;
          continue;
        }
        if (current->IsPolymorphicInstanceCall()) {
          PolymorphicInstanceCallInstr* call =
              current->AsPolymorphicInstanceCall();
          // These checks make sure that the number of call-sites counted does
          // not change relative to the time when the current set of inlining
          // parameters was fixed.
          // TODO(fschneider): Determine new heuristic parameters that avoid
          // these checks entirely.
          if (!call->HasRecognizedTarget() &&
              (call->instance_call()->token_kind() != Token::kEQ)) {
            ++call_site_count_;
          }
        }
      }
    }
  }

  intptr_t call_site_count() const { return call_site_count_; }
  intptr_t instruction_count() const { return instruction_count_; }

 private:
  intptr_t call_site_count_;
  intptr_t instruction_count_;
};


// A collection of call sites to consider for inlining.
class CallSites : public ValueObject {
 public:
  explicit CallSites(FlowGraph* flow_graph)
      : static_calls_(),
        closure_calls_(),
        instance_calls_() { }

  const GrowableArray<ClosureCallInstr*>& closure_calls() const {
    return closure_calls_;
  }

  struct InstanceCallInfo {
    PolymorphicInstanceCallInstr* call;
    double ratio;
    explicit InstanceCallInfo(PolymorphicInstanceCallInstr* call_arg)
        : call(call_arg), ratio(0.0) {}
  };

  struct StaticCallInfo {
    StaticCallInstr* call;
    double ratio;
    explicit StaticCallInfo(StaticCallInstr* value)
        : call(value), ratio(0.0) {}
  };

  const GrowableArray<InstanceCallInfo>& instance_calls() const {
    return instance_calls_;
  }

  const GrowableArray<StaticCallInfo>& static_calls() const {
    return static_calls_;
  }

  bool HasCalls() const {
    return !(static_calls_.is_empty() &&
             closure_calls_.is_empty() &&
             instance_calls_.is_empty());
  }

  void Clear() {
    static_calls_.Clear();
    closure_calls_.Clear();
    instance_calls_.Clear();
  }

  void ComputeCallSiteRatio(intptr_t static_call_start_ix,
                            intptr_t instance_call_start_ix) {
    const intptr_t num_static_calls =
        static_calls_.length() - static_call_start_ix;
    const intptr_t num_instance_calls =
        instance_calls_.length() - instance_call_start_ix;

    intptr_t max_count = 0;
    GrowableArray<intptr_t> instance_call_counts(num_instance_calls);
    for (intptr_t i = 0; i < num_instance_calls; ++i) {
      const intptr_t aggregate_count =
          instance_calls_[i + instance_call_start_ix].
              call->ic_data().AggregateCount();
      instance_call_counts.Add(aggregate_count);
      if (aggregate_count > max_count) max_count = aggregate_count;
    }

    GrowableArray<intptr_t> static_call_counts(num_static_calls);
    for (intptr_t i = 0; i < num_static_calls; ++i) {
      const intptr_t aggregate_count =
          static_calls_[i + static_call_start_ix].
              call->ic_data()->AggregateCount();
      static_call_counts.Add(aggregate_count);
      if (aggregate_count > max_count) max_count = aggregate_count;
    }

    // max_count can be 0 if none of the calls was executed.
    for (intptr_t i = 0; i < num_instance_calls; ++i) {
      const double ratio = (max_count == 0) ?
          0.0 : static_cast<double>(instance_call_counts[i]) / max_count;
      instance_calls_[i + instance_call_start_ix].ratio = ratio;
    }
    for (intptr_t i = 0; i < num_static_calls; ++i) {
      const double ratio = (max_count == 0) ?
          0.0 : static_cast<double>(static_call_counts[i]) / max_count;
      static_calls_[i + static_call_start_ix].ratio = ratio;
    }
  }

  void FindCallSites(FlowGraph* graph, intptr_t depth) {
    ASSERT(graph != NULL);
    // If depth is less than the threshold recursively add call sites.
    if (depth > FLAG_inlining_depth_threshold) return;

    // Recognized methods are not treated as normal calls. They don't have
    // calls in themselves, so we keep adding those even when at the threshold.
    const bool only_recognized_methods =
        (depth == FLAG_inlining_depth_threshold);

    const intptr_t instance_call_start_ix = instance_calls_.length();
    const intptr_t static_call_start_ix = static_calls_.length();
    for (BlockIterator block_it = graph->postorder_iterator();
         !block_it.Done();
         block_it.Advance()) {
      for (ForwardInstructionIterator it(block_it.Current());
           !it.Done();
           it.Advance()) {
        Instruction* current = it.Current();
        if (only_recognized_methods) {
          PolymorphicInstanceCallInstr* instance_call =
              current->AsPolymorphicInstanceCall();
          if ((instance_call != NULL) && instance_call->HasRecognizedTarget()) {
            instance_calls_.Add(InstanceCallInfo(instance_call));
          }
          continue;
        }
        // Collect all call sites (!only_recognized_methods).
        ClosureCallInstr* closure_call = current->AsClosureCall();
        if (closure_call != NULL) {
          closure_calls_.Add(closure_call);
          continue;
        }
        StaticCallInstr* static_call = current->AsStaticCall();
        if (static_call != NULL) {
          if (static_call->function().IsInlineable()) {
            static_calls_.Add(StaticCallInfo(static_call));
          }
          continue;
        }
        PolymorphicInstanceCallInstr* instance_call =
            current->AsPolymorphicInstanceCall();
        if (instance_call != NULL) {
          instance_calls_.Add(InstanceCallInfo(instance_call));
          continue;
        }
      }
    }
    ComputeCallSiteRatio(static_call_start_ix, instance_call_start_ix);
  }

 private:
  GrowableArray<StaticCallInfo> static_calls_;
  GrowableArray<ClosureCallInstr*> closure_calls_;
  GrowableArray<InstanceCallInfo> instance_calls_;

  DISALLOW_COPY_AND_ASSIGN(CallSites);
};


struct InlinedCallData {
  InlinedCallData(Definition* call, GrowableArray<Value*>* arguments)
      : call(call),
        arguments(arguments),
        callee_graph(NULL),
        parameter_stubs(NULL),
        exit_collector(NULL) { }

  Definition* call;
  GrowableArray<Value*>* arguments;
  FlowGraph* callee_graph;
  ZoneGrowableArray<Definition*>* parameter_stubs;
  InlineExitCollector* exit_collector;
};


class CallSiteInliner;

class PolymorphicInliner : public ValueObject {
 public:
  PolymorphicInliner(CallSiteInliner* owner,
                     PolymorphicInstanceCallInstr* call);

  void Inline();

 private:
  bool CheckInlinedDuplicate(const Function& target);
  bool CheckNonInlinedDuplicate(const Function& target);

  bool TryInlining(intptr_t receiver_cid, const Function& target);
  bool TryInlineRecognizedMethod(intptr_t receiver_cid, const Function& target);

  TargetEntryInstr* BuildDecisionGraph();

  CallSiteInliner* const owner_;
  PolymorphicInstanceCallInstr* const call_;
  const intptr_t num_variants_;
  GrowableArray<CidTarget> variants_;

  GrowableArray<CidTarget> inlined_variants_;
  GrowableArray<CidTarget> non_inlined_variants_;
  GrowableArray<BlockEntryInstr*> inlined_entries_;
  InlineExitCollector* exit_collector_;
};


class CallSiteInliner : public ValueObject {
 public:
  explicit CallSiteInliner(FlowGraph* flow_graph)
      : caller_graph_(flow_graph),
        inlined_(false),
        initial_size_(flow_graph->InstructionCount()),
        inlined_size_(0),
        inlining_depth_(1),
        collected_call_sites_(NULL),
        inlining_call_sites_(NULL),
        function_cache_() { }

  FlowGraph* caller_graph() const { return caller_graph_; }

  // Inlining heuristics based on Cooper et al. 2008.
  bool ShouldWeInline(const Function& callee,
                      intptr_t instr_count,
                      intptr_t call_site_count,
                      intptr_t const_arg_count) {
    if (inlined_size_ > FLAG_inlining_caller_size_threshold) {
      // Prevent methods becoming humongous and thus slow to compile.
      return false;
    }
    if (instr_count <= FLAG_inlining_size_threshold) {
      return true;
    }
    if (call_site_count <= FLAG_inlining_callee_call_sites_threshold) {
      return true;
    }
    if ((const_arg_count >= FLAG_inlining_constant_arguments_count) &&
        (instr_count <= FLAG_inlining_constant_arguments_size_threshold)) {
      return true;
    }
    if (MethodRecognizer::AlwaysInline(callee)) {
      return true;
    }
    return false;
  }

  void InlineCalls() {
    // If inlining depth is less then one abort.
    if (FLAG_inlining_depth_threshold < 1) return;
    if (caller_graph_->parsed_function().function().deoptimization_counter() >=
        FLAG_deoptimization_counter_inlining_threshold) {
      return;
    }
    // Create two call site collections to swap between.
    CallSites sites1(caller_graph_);
    CallSites sites2(caller_graph_);
    CallSites* call_sites_temp = NULL;
    collected_call_sites_ = &sites1;
    inlining_call_sites_ = &sites2;
    // Collect initial call sites.
    collected_call_sites_->FindCallSites(caller_graph_, inlining_depth_);
    while (collected_call_sites_->HasCalls()) {
      TRACE_INLINING(OS::Print("  Depth %" Pd " ----------\n",
                               inlining_depth_));
      // Swap collected and inlining arrays and clear the new collecting array.
      call_sites_temp = collected_call_sites_;
      collected_call_sites_ = inlining_call_sites_;
      inlining_call_sites_ = call_sites_temp;
      collected_call_sites_->Clear();
      // Inline call sites at the current depth.
      InlineStaticCalls();
      InlineClosureCalls();
      InlineInstanceCalls();
      // Increment the inlining depth. Checked before recursive inlining.
      ++inlining_depth_;
    }
    collected_call_sites_ = NULL;
    inlining_call_sites_ = NULL;
  }

  bool inlined() const { return inlined_; }

  double GrowthFactor() const {
    return static_cast<double>(inlined_size_) /
        static_cast<double>(initial_size_);
  }

  bool TryInlining(const Function& function,
                   const Array& argument_names,
                   InlinedCallData* call_data) {
    TRACE_INLINING(OS::Print("  => %s (deopt count %d)\n",
                             function.ToCString(),
                             function.deoptimization_counter()));

    // TODO(fschneider): Enable inlining inside try-blocks.
    if (call_data->call->GetBlock()->try_index() !=
        CatchClauseNode::kInvalidTryIndex) {
      TRACE_INLINING(OS::Print("     Bailout: inside try-block\n"));
      return false;
    }

    // Abort if the inlinable bit on the function is low.
    if (!function.IsInlineable()) {
      TRACE_INLINING(OS::Print("     Bailout: not inlinable\n"));
      return false;
    }

    // Abort if this function has deoptimized too much.
    if (function.deoptimization_counter() >=
        FLAG_deoptimization_counter_threshold) {
      function.set_is_inlinable(false);
      TRACE_INLINING(OS::Print("     Bailout: deoptimization threshold\n"));
      return false;
    }

    GrowableArray<Value*>* arguments = call_data->arguments;
    const intptr_t constant_arguments = CountConstants(*arguments);
    if (!ShouldWeInline(function,
                        function.optimized_instruction_count(),
                        function.optimized_call_site_count(),
                        constant_arguments)) {
      TRACE_INLINING(OS::Print("     Bailout: early heuristics with "
                               "code size:  %" Pd ", "
                               "call sites: %" Pd ", "
                               "const args: %" Pd "\n",
                               function.optimized_instruction_count(),
                               function.optimized_call_site_count(),
                               constant_arguments));
      return false;
    }

    // Abort if this is a recursive occurrence.
    Definition* call = call_data->call;
    if (!FLAG_inline_recursive && IsCallRecursive(function, call)) {
      function.set_is_inlinable(false);
      TRACE_INLINING(OS::Print("     Bailout: recursive function\n"));
      return false;
    }

    Isolate* isolate = Isolate::Current();
    // Save and clear deopt id.
    const intptr_t prev_deopt_id = isolate->deopt_id();
    isolate->set_deopt_id(0);
    // Install bailout jump.
    LongJump* base = isolate->long_jump_base();
    LongJump jump;
    isolate->set_long_jump_base(&jump);
    if (setjmp(*jump.Set()) == 0) {
      // Parse the callee function.
      bool in_cache;
      ParsedFunction* parsed_function;
      {
        TimerScope timer(FLAG_compiler_stats,
                         &CompilerStats::graphinliner_parse_timer,
                         isolate);
        parsed_function = GetParsedFunction(function, &in_cache);
      }

      // Load IC data for the callee.
      Array& ic_data_array = Array::Handle();
      if (function.HasCode()) {
        const Code& unoptimized_code =
            Code::Handle(function.unoptimized_code());
        ic_data_array = unoptimized_code.ExtractTypeFeedbackArray();
      }

      // Build the callee graph.
      InlineExitCollector* exit_collector =
          new InlineExitCollector(caller_graph_, call);
      FlowGraphBuilder builder(parsed_function,
                               ic_data_array,
                               exit_collector,
                               Isolate::kNoDeoptId);
      builder.SetInitialBlockId(caller_graph_->max_block_id());
      FlowGraph* callee_graph;
      {
        TimerScope timer(FLAG_compiler_stats,
                         &CompilerStats::graphinliner_build_timer,
                         isolate);
        callee_graph = builder.BuildGraph();
      }

      // The parameter stubs are a copy of the actual arguments providing
      // concrete information about the values, for example constant values,
      // without linking between the caller and callee graphs.
      // TODO(zerny): Put more information in the stubs, eg, type information.
      ZoneGrowableArray<Definition*>* param_stubs =
          new ZoneGrowableArray<Definition*>(function.NumParameters());

      // Create a parameter stub for each fixed positional parameter.
      for (intptr_t i = 0; i < function.num_fixed_parameters(); ++i) {
        param_stubs->Add(CreateParameterStub(i, (*arguments)[i], callee_graph));
      }

      // If the callee has optional parameters, rebuild the argument and stub
      // arrays so that actual arguments are in one-to-one with the formal
      // parameters.
      if (function.HasOptionalParameters()) {
        TRACE_INLINING(OS::Print("     adjusting for optional parameters\n"));
        if (!AdjustForOptionalParameters(*parsed_function,
                                         argument_names,
                                         arguments,
                                         param_stubs,
                                         callee_graph)) {
          function.set_is_inlinable(false);
          TRACE_INLINING(OS::Print("     Bailout: optional arg mismatch\n"));
          return false;
        }
      }

      // After treating optional parameters the actual/formal count must match.
      ASSERT(arguments->length() == function.NumParameters());
      ASSERT(param_stubs->length() == callee_graph->parameter_count());

      BlockScheduler block_scheduler(callee_graph);
      block_scheduler.AssignEdgeWeights();

      {
        TimerScope timer(FLAG_compiler_stats,
                         &CompilerStats::graphinliner_ssa_timer,
                         isolate);
        // Compute SSA on the callee graph, catching bailouts.
        callee_graph->ComputeSSA(caller_graph_->max_virtual_register_number(),
                                 param_stubs);
        DEBUG_ASSERT(callee_graph->VerifyUseLists());
      }

      {
        TimerScope timer(FLAG_compiler_stats,
                         &CompilerStats::graphinliner_opt_timer,
                         isolate);
        // TODO(zerny): Do more optimization passes on the callee graph.
        FlowGraphOptimizer optimizer(callee_graph);
        optimizer.ApplyICData();
        DEBUG_ASSERT(callee_graph->VerifyUseLists());
      }

      if (FLAG_trace_inlining &&
          (FLAG_print_flow_graph || FLAG_print_flow_graph_optimized)) {
        OS::Print("Callee graph for inlining %s\n",
                  function.ToFullyQualifiedCString());
        FlowGraphPrinter printer(*callee_graph);
        printer.PrintBlocks();
      }

      // Collect information about the call site and caller graph.
      // TODO(zerny): Do this after CP and dead code elimination.
      intptr_t constants_count = 0;
      for (intptr_t i = 0; i < param_stubs->length(); ++i) {
        if ((*param_stubs)[i]->IsConstant()) ++constants_count;
      }
      GraphInfoCollector info;
      info.Collect(*callee_graph);
      const intptr_t size = info.instruction_count();
      const intptr_t call_site_count = info.call_site_count();

      function.set_optimized_instruction_count(size);
      function.set_optimized_call_site_count(call_site_count);

      // Use heuristics do decide if this call should be inlined.
      if (!ShouldWeInline(function, size, call_site_count, constants_count)) {
        // If size is larger than all thresholds, don't consider it again.
        if ((size > FLAG_inlining_size_threshold) &&
            (call_site_count > FLAG_inlining_callee_call_sites_threshold) &&
            (size > FLAG_inlining_constant_arguments_size_threshold)) {
          function.set_is_inlinable(false);
        }
        isolate->set_long_jump_base(base);
        isolate->set_deopt_id(prev_deopt_id);
        TRACE_INLINING(OS::Print("     Bailout: heuristics with "
                                 "code size:  %" Pd ", "
                                 "call sites: %" Pd ", "
                                 "const args: %" Pd "\n",
                                 size,
                                 call_site_count,
                                 constants_count));
        return false;
      }

      collected_call_sites_->FindCallSites(callee_graph, inlining_depth_);

      // Add the function to the cache.
      if (!in_cache) function_cache_.Add(parsed_function);

      // Build succeeded so we restore the bailout jump.
      inlined_ = true;
      inlined_size_ += size;
      isolate->set_long_jump_base(base);
      isolate->set_deopt_id(prev_deopt_id);

      call_data->callee_graph = callee_graph;
      call_data->parameter_stubs = param_stubs;
      call_data->exit_collector = exit_collector;

      // When inlined, we add the guarded fields of the callee to the caller's
      // list of guarded fields.
      for (intptr_t i = 0; i < callee_graph->guarded_fields()->length(); ++i) {
        FlowGraph::AddToGuardedFields(caller_graph_->guarded_fields(),
                                      (*callee_graph->guarded_fields())[i]);
      }

      TRACE_INLINING(OS::Print("     Success\n"));
      return true;
    } else {
      Error& error = Error::Handle();
      error = isolate->object_store()->sticky_error();
      isolate->object_store()->clear_sticky_error();
      isolate->set_long_jump_base(base);
      isolate->set_deopt_id(prev_deopt_id);
      TRACE_INLINING(OS::Print("     Bailout: %s\n", error.ToErrorCString()));
      return false;
    }
  }

 private:
  friend class PolymorphicInliner;

  void InlineCall(InlinedCallData* call_data) {
    TimerScope timer(FLAG_compiler_stats,
                     &CompilerStats::graphinliner_subst_timer,
                     Isolate::Current());

    // For closure calls: Store context value.
    FlowGraph* callee_graph = call_data->callee_graph;
    TargetEntryInstr* callee_entry =
        callee_graph->graph_entry()->normal_entry();
    ClosureCallInstr* closure_call = call_data->call->AsClosureCall();
    if (closure_call != NULL) {
      // TODO(fschneider): Avoid setting the context, if not needed.
      Definition* closure =
          closure_call->PushArgumentAt(0)->value()->definition();
      LoadFieldInstr* context =
          new LoadFieldInstr(new Value(closure),
                             Closure::context_offset(),
                             Type::ZoneHandle());
      AllocateObjectInstr* alloc =
          closure_call->ArgumentAt(0)->AsAllocateObject();
      if ((alloc != NULL) && !alloc->closure_function().IsNull()) {
        ASSERT(!alloc->context_field().IsNull());
        context->set_field(&alloc->context_field());
      }

      context->set_ssa_temp_index(caller_graph()->alloc_ssa_temp_index());
      context->InsertAfter(callee_entry);
      StoreContextInstr* set_context =
          new StoreContextInstr(new Value(context));
      set_context->InsertAfter(context);
    }

    // Plug result in the caller graph.
    InlineExitCollector* exit_collector = call_data->exit_collector;
    exit_collector->PrepareGraphs(callee_graph);
    exit_collector->ReplaceCall(callee_entry);

    // Replace each stub with the actual argument or the caller's constant.
    // Nulls denote optional parameters for which no actual was given.
    GrowableArray<Value*>* arguments = call_data->arguments;
    for (intptr_t i = 0; i < arguments->length(); ++i) {
      Definition* stub = (*call_data->parameter_stubs)[i];
      Value* actual = (*arguments)[i];
      if (actual != NULL) stub->ReplaceUsesWith(actual->definition());
    }

    // Remove push arguments of the call.
    Definition* call = call_data->call;
    for (intptr_t i = 0; i < call->ArgumentCount(); ++i) {
      PushArgumentInstr* push = call->PushArgumentAt(i);
      push->ReplaceUsesWith(push->value()->definition());
      push->RemoveFromGraph();
    }

    // Replace remaining constants with uses by constants in the caller's
    // initial definitions.
    GrowableArray<Definition*>* defns =
        callee_graph->graph_entry()->initial_definitions();
    for (intptr_t i = 0; i < defns->length(); ++i) {
      ConstantInstr* constant = (*defns)[i]->AsConstant();
      if ((constant != NULL) && constant->HasUses()) {
        constant->ReplaceUsesWith(
            caller_graph_->GetConstant(constant->value()));
      }
    }

    // Check that inlining maintains use lists.
    DEBUG_ASSERT(!FLAG_verify_compiler || caller_graph_->VerifyUseLists());
  }

  static intptr_t CountConstants(const GrowableArray<Value*>& arguments) {
    intptr_t count = 0;
    for (intptr_t i = 0; i < arguments.length(); i++) {
      if (arguments[i]->BindsToConstant()) count++;
    }
    return count;
  }

  // Parse a function reusing the cache if possible.
  ParsedFunction* GetParsedFunction(const Function& function, bool* in_cache) {
    // TODO(zerny): Use a hash map for the cache.
    for (intptr_t i = 0; i < function_cache_.length(); ++i) {
      ParsedFunction* parsed_function = function_cache_[i];
      if (parsed_function->function().raw() == function.raw()) {
        *in_cache = true;
        SourceLabelResetter reset;
        parsed_function->node_sequence()->Visit(&reset);
        return parsed_function;
      }
    }
    *in_cache = false;
    ParsedFunction* parsed_function = new ParsedFunction(function);
    Parser::ParseFunction(parsed_function);
    parsed_function->AllocateVariables();
    return parsed_function;
  }

  // Include special handling for List. factory: inlining it is not helpful
  // if the incoming argument is a non-constant value.
  // TODO(srdjan): Fix inlining of List. factory.
  void InlineStaticCalls() {
    const GrowableArray<CallSites::StaticCallInfo>& call_info =
        inlining_call_sites_->static_calls();
    TRACE_INLINING(OS::Print("  Static Calls (%" Pd ")\n", call_info.length()));
    for (intptr_t call_idx = 0; call_idx < call_info.length(); ++call_idx) {
      StaticCallInstr* call = call_info[call_idx].call;
      if (call->function().name() == Symbols::ListFactory().raw()) {
        // Inline only if no arguments or a constant was passed.
        ASSERT(call->function().NumImplicitParameters() == 1);
        ASSERT(call->ArgumentCount() <= 2);
        // Arg 0: Instantiator type arguments.
        // Arg 1: Length (optional).
        if ((call->ArgumentCount() == 2) &&
            (!call->PushArgumentAt(1)->value()->BindsToConstant())) {
          // Do not inline since a non-constant argument was passed.
          continue;
        }
      }
      const Function& target = call->function();
      if (!MethodRecognizer::AlwaysInline(target) &&
          (call_info[call_idx].ratio * 100) < FLAG_inlining_hotness) {
        TRACE_INLINING(OS::Print(
            "  => %s (deopt count %d)\n     Bailout: cold %f\n",
            target.ToCString(),
            target.deoptimization_counter(),
            call_info[call_idx].ratio));
        continue;
      }
      GrowableArray<Value*> arguments(call->ArgumentCount());
      for (int i = 0; i < call->ArgumentCount(); ++i) {
        arguments.Add(call->PushArgumentAt(i)->value());
      }
      InlinedCallData call_data(call, &arguments);
      if (TryInlining(call->function(), call->argument_names(), &call_data)) {
        InlineCall(&call_data);
      }
    }
  }

  void InlineClosureCalls() {
    const GrowableArray<ClosureCallInstr*>& calls =
        inlining_call_sites_->closure_calls();
    TRACE_INLINING(OS::Print("  Closure Calls (%" Pd ")\n", calls.length()));
    for (intptr_t i = 0; i < calls.length(); ++i) {
      ClosureCallInstr* call = calls[i];
      // Find the closure of the callee.
      ASSERT(call->ArgumentCount() > 0);
      Function& target = Function::ZoneHandle();
      CreateClosureInstr* closure =
          call->ArgumentAt(0)->AsCreateClosure();
      if (closure != NULL) {
        target ^= closure->function().raw();
      }
      AllocateObjectInstr* alloc =
          call->ArgumentAt(0)->AsAllocateObject();
      if ((alloc != NULL) && !alloc->closure_function().IsNull()) {
        target ^= alloc->closure_function().raw();
        ASSERT(target.signature_class() == alloc->cls().raw());
      }
      if (target.IsNull()) {
        TRACE_INLINING(OS::Print("     Bailout: non-closure operator\n"));
        continue;
      }
      GrowableArray<Value*> arguments(call->ArgumentCount());
      for (int i = 0; i < call->ArgumentCount(); ++i) {
        arguments.Add(call->PushArgumentAt(i)->value());
      }
      InlinedCallData call_data(call, &arguments);
      if (TryInlining(target,
                      call->argument_names(),
                      &call_data)) {
        InlineCall(&call_data);
      }
    }
  }

  void InlineInstanceCalls() {
    const GrowableArray<CallSites::InstanceCallInfo>& call_info =
        inlining_call_sites_->instance_calls();
    TRACE_INLINING(OS::Print("  Polymorphic Instance Calls (%" Pd ")\n",
                             call_info.length()));
    for (intptr_t call_idx = 0; call_idx < call_info.length(); ++call_idx) {
      PolymorphicInstanceCallInstr* call = call_info[call_idx].call;
      if (call->with_checks()) {
        PolymorphicInliner inliner(this, call);
        inliner.Inline();
        continue;
      }

      const ICData& ic_data = call->ic_data();
      const Function& target = Function::ZoneHandle(ic_data.GetTargetAt(0));
      if (!MethodRecognizer::AlwaysInline(target) &&
          (call_info[call_idx].ratio * 100) < FLAG_inlining_hotness) {
        TRACE_INLINING(OS::Print(
            "  => %s (deopt count %d)\n     Bailout: cold %f\n",
            target.ToCString(),
            target.deoptimization_counter(),
            call_info[call_idx].ratio));
        continue;
      }
      GrowableArray<Value*> arguments(call->ArgumentCount());
      for (int arg_i = 0; arg_i < call->ArgumentCount(); ++arg_i) {
        arguments.Add(call->PushArgumentAt(arg_i)->value());
      }
      InlinedCallData call_data(call, &arguments);
      if (TryInlining(target,
                      call->instance_call()->argument_names(),
                      &call_data)) {
        InlineCall(&call_data);
      }
    }
  }

  bool AdjustForOptionalParameters(const ParsedFunction& parsed_function,
                                   const Array& argument_names,
                                   GrowableArray<Value*>* arguments,
                                   ZoneGrowableArray<Definition*>* param_stubs,
                                   FlowGraph* callee_graph) {
    const Function& function = parsed_function.function();
    // The language and this code does not support both optional positional
    // and optional named parameters for the same function.
    ASSERT(!function.HasOptionalPositionalParameters() ||
           !function.HasOptionalNamedParameters());

    intptr_t arg_count = arguments->length();
    intptr_t param_count = function.NumParameters();
    intptr_t fixed_param_count = function.num_fixed_parameters();
    ASSERT(fixed_param_count <= arg_count);
    ASSERT(arg_count <= param_count);

    if (function.HasOptionalPositionalParameters()) {
      // Create a stub for each optional positional parameters with an actual.
      for (intptr_t i = fixed_param_count; i < arg_count; ++i) {
        param_stubs->Add(CreateParameterStub(i, (*arguments)[i], callee_graph));
      }
      ASSERT(function.NumOptionalPositionalParameters() ==
             (param_count - fixed_param_count));
      // For each optional positional parameter without an actual, add its
      // default value.
      for (intptr_t i = arg_count; i < param_count; ++i) {
        const Object& object =
            Object::ZoneHandle(
                parsed_function.default_parameter_values().At(
                    i - fixed_param_count));
        ConstantInstr* constant = new ConstantInstr(object);
        arguments->Add(NULL);
        param_stubs->Add(constant);
      }
      return true;
    }

    ASSERT(function.HasOptionalNamedParameters());

    // Passed arguments must match fixed parameters plus named arguments.
    intptr_t argument_names_count =
        (argument_names.IsNull()) ? 0 : argument_names.Length();
    ASSERT(arg_count == (fixed_param_count + argument_names_count));

    // Fast path when no optional named parameters are given.
    if (argument_names_count == 0) {
      for (intptr_t i = 0; i < param_count - fixed_param_count; ++i) {
        arguments->Add(NULL);
        param_stubs->Add(GetDefaultValue(i, parsed_function));
      }
      return true;
    }

    // Otherwise, build a collection of name/argument pairs.
    GrowableArray<NamedArgument> named_args(argument_names_count);
    for (intptr_t i = 0; i < argument_names.Length(); ++i) {
      String& arg_name = String::Handle(Isolate::Current());
      arg_name ^= argument_names.At(i);
      named_args.Add(
          NamedArgument(&arg_name, (*arguments)[i + fixed_param_count]));
    }

    // Truncate the arguments array to just fixed parameters.
    arguments->TruncateTo(fixed_param_count);

    // For each optional named parameter, add the actual argument or its
    // default if no argument is passed.
    intptr_t match_count = 0;
    for (intptr_t i = fixed_param_count; i < param_count; ++i) {
      String& param_name = String::Handle(function.ParameterNameAt(i));
      // Search for and add the named argument.
      Value* arg = NULL;
      for (intptr_t j = 0; j < named_args.length(); ++j) {
        if (param_name.Equals(*named_args[j].name)) {
          arg = named_args[j].value;
          match_count++;
          break;
        }
      }
      arguments->Add(arg);
      // Create a stub for the argument or use the parameter's default value.
      if (arg != NULL) {
        param_stubs->Add(CreateParameterStub(i, arg, callee_graph));
      } else {
        param_stubs->Add(
            GetDefaultValue(i - fixed_param_count, parsed_function));
      }
    }
    return argument_names_count == match_count;
  }


  FlowGraph* caller_graph_;
  bool inlined_;
  intptr_t initial_size_;
  intptr_t inlined_size_;
  intptr_t inlining_depth_;
  CallSites* collected_call_sites_;
  CallSites* inlining_call_sites_;
  GrowableArray<ParsedFunction*> function_cache_;

  DISALLOW_COPY_AND_ASSIGN(CallSiteInliner);
};


PolymorphicInliner::PolymorphicInliner(CallSiteInliner* owner,
                                       PolymorphicInstanceCallInstr* call)
    : owner_(owner),
      call_(call),
      num_variants_(call->ic_data().NumberOfChecks()),
      variants_(num_variants_),
      inlined_variants_(num_variants_),
      non_inlined_variants_(num_variants_),
      inlined_entries_(num_variants_),
      exit_collector_(new InlineExitCollector(owner->caller_graph(), call)) {
}


// Inlined bodies are shared if two different class ids have the same
// inlined target.  This sharing is represented by using three different
// types of entries in the inlined_entries_ array:
//
//   * GraphEntry: the inlined body is not shared.
//
//   * TargetEntry: the inlined body is shared and this is the first variant.
//
//   * JoinEntry: the inlined body is shared and this is a subsequent variant.
bool PolymorphicInliner::CheckInlinedDuplicate(const Function& target) {
  for (intptr_t i = 0; i < inlined_variants_.length(); ++i) {
    if ((target.raw() == inlined_variants_[i].target->raw()) &&
        !MethodRecognizer::PolymorphicTarget(target)) {
      // The call target is shared with a previous inlined variant.  Share
      // the graph.  This requires a join block at the entry, and edge-split
      // form requires a target for each branch.
      //
      // Represent the sharing by recording a fresh target for the first
      // variant and the shared join for all later variants.
      if (inlined_entries_[i]->IsGraphEntry()) {
        // Convert the old target entry to a new join entry.
        TargetEntryInstr* old_target =
            inlined_entries_[i]->AsGraphEntry()->normal_entry();
        JoinEntryInstr* new_join = BranchSimplifier::ToJoinEntry(old_target);
        old_target->ReplaceAsPredecessorWith(new_join);
        for (intptr_t j = 0; j < old_target->dominated_blocks().length(); ++j) {
          BlockEntryInstr* block = old_target->dominated_blocks()[j];
          new_join->AddDominatedBlock(block);
        }
        // Create a new target with the join as unconditional successor.
        TargetEntryInstr* new_target =
            new TargetEntryInstr(owner_->caller_graph()->allocate_block_id(),
                                 old_target->try_index());
        new_target->InheritDeoptTarget(new_join);
        GotoInstr* new_goto = new GotoInstr(new_join);
        new_goto->InheritDeoptTarget(new_join);
        new_target->LinkTo(new_goto);
        new_target->set_last_instruction(new_goto);
        new_join->predecessors_.Add(new_target);

        // Record the new target for the first variant.
        inlined_entries_[i] = new_target;
      }
      ASSERT(inlined_entries_[i]->IsTargetEntry());
      // Record the shared join for this variant.
      BlockEntryInstr* join =
          inlined_entries_[i]->last_instruction()->SuccessorAt(0);
      ASSERT(join->IsJoinEntry());
      inlined_entries_.Add(join);
      return true;
    }
  }

  return false;
}


bool PolymorphicInliner::CheckNonInlinedDuplicate(const Function& target) {
  for (intptr_t i = 0; i < non_inlined_variants_.length(); ++i) {
    if (target.raw() == non_inlined_variants_[i].target->raw()) {
      return true;
    }
  }

  return false;
}


bool PolymorphicInliner::TryInlining(intptr_t receiver_cid,
                                     const Function& target) {
  if (!target.is_optimizable()) {
    if (TryInlineRecognizedMethod(receiver_cid, target)) {
      owner_->inlined_ = true;
      return true;
    }
    return false;
  }

  GrowableArray<Value*> arguments(call_->ArgumentCount());
  for (int i = 0; i < call_->ArgumentCount(); ++i) {
    arguments.Add(call_->PushArgumentAt(i)->value());
  }
  InlinedCallData call_data(call_, &arguments);
  if (!owner_->TryInlining(target,
                           call_->instance_call()->argument_names(),
                           &call_data)) {
    return false;
  }

  FlowGraph* callee_graph = call_data.callee_graph;
  call_data.exit_collector->PrepareGraphs(callee_graph);
  inlined_entries_.Add(callee_graph->graph_entry());
  exit_collector_->Union(call_data.exit_collector);

  // Replace parameter stubs and constants.  Replace the receiver argument
  // with a redefinition to prevent code from the inlined body from being
  // hoisted above the inlined entry.
  ASSERT(arguments.length() > 0);
  Value* actual = arguments[0];
  RedefinitionInstr* redefinition = new RedefinitionInstr(actual->Copy());
  redefinition->set_ssa_temp_index(
      owner_->caller_graph()->alloc_ssa_temp_index());
  redefinition->InsertAfter(callee_graph->graph_entry()->normal_entry());
  Definition* stub = (*call_data.parameter_stubs)[0];
  stub->ReplaceUsesWith(redefinition);

  for (intptr_t i = 1; i < arguments.length(); ++i) {
    actual = arguments[i];
    if (actual != NULL) {
      stub = (*call_data.parameter_stubs)[i];
      stub->ReplaceUsesWith(actual->definition());
    }
  }
  GrowableArray<Definition*>* defns =
      callee_graph->graph_entry()->initial_definitions();
  for (intptr_t i = 0; i < defns->length(); ++i) {
    ConstantInstr* constant = (*defns)[i]->AsConstant();
    if ((constant != NULL) && constant->HasUses()) {
      constant->ReplaceUsesWith(
          owner_->caller_graph()->GetConstant(constant->value()));
    }
  }
  return true;
}


static Instruction* AppendInstruction(Instruction* first,
                                      Instruction* second) {
  for (intptr_t i = second->InputCount() - 1; i >= 0; --i) {
    Value* input = second->InputAt(i);
    input->definition()->AddInputUse(input);
  }
  first->LinkTo(second);
  return second;
}


bool PolymorphicInliner::TryInlineRecognizedMethod(intptr_t receiver_cid,
                                                   const Function& target) {
  FlowGraphOptimizer optimizer(owner_->caller_graph());
  TargetEntryInstr* entry;
  Definition* last;
  // Replace the receiver argument with a redefinition to prevent code from
  // the inlined body from being hoisted above the inlined entry.
  GrowableArray<Definition*> arguments(call_->ArgumentCount());
  Definition* receiver = call_->ArgumentAt(0);
    RedefinitionInstr* redefinition =
        new RedefinitionInstr(new Value(receiver));
    redefinition->set_ssa_temp_index(
        owner_->caller_graph()->alloc_ssa_temp_index());
  if (optimizer.TryInlineRecognizedMethod(receiver_cid,
                                          target,
                                          call_,
                                          redefinition,
                                          call_->instance_call()->token_pos(),
                                          *call_->instance_call()->ic_data(),
                                          &entry, &last)) {
    // Create a graph fragment.
    redefinition->InsertAfter(entry);
    InlineExitCollector* exit_collector =
        new InlineExitCollector(owner_->caller_graph(), call_);

    ReturnInstr* result =
        new ReturnInstr(call_->instance_call()->token_pos(),
                        new Value(last));
    owner_->caller_graph()->AppendTo(
        last,
        result,
        call_->env(),  // Return can become deoptimization target.
        Definition::kEffect);
    entry->set_last_instruction(result);
    exit_collector->AddExit(result);
    GraphEntryInstr* graph_entry =
        new GraphEntryInstr(NULL,  // No parsed function.
                            entry,
                            Isolate::kNoDeoptId);  // No OSR id.
    // Update polymorphic inliner state.
    inlined_entries_.Add(graph_entry);
    exit_collector_->Union(exit_collector);
    return true;
  }
  return false;
}


// Build a DAG to dispatch to the inlined function bodies.  Load the class
// id of the receiver and make explicit comparisons for each inlined body,
// in frequency order.  If all variants are inlined, the entry to the last
// inlined body is guarded by a CheckClassId instruction which can deopt.
// If not all variants are inlined, we add a PolymorphicInstanceCall
// instruction to handle the non-inlined variants.
TargetEntryInstr* PolymorphicInliner::BuildDecisionGraph() {
  // Start with a fresh target entry.
  TargetEntryInstr* entry =
      new TargetEntryInstr(owner_->caller_graph()->allocate_block_id(),
                           call_->GetBlock()->try_index());
  entry->InheritDeoptTarget(call_);

  // This function uses a cursor (a pointer to the 'current' instruction) to
  // build the graph.  The next instruction will be inserted after the
  // cursor.
  TargetEntryInstr* current_block = entry;
  Instruction* cursor = entry;

  Definition* receiver = call_->ArgumentAt(0);
  // There are at least two variants including non-inlined ones, so we have
  // at least one branch on the class id.
  LoadClassIdInstr* load_cid = new LoadClassIdInstr(new Value(receiver));
  load_cid->set_ssa_temp_index(owner_->caller_graph()->alloc_ssa_temp_index());
  cursor = AppendInstruction(cursor, load_cid);
  for (intptr_t i = 0; i < inlined_variants_.length(); ++i) {
    // 1. Guard the body with a class id check.
    if ((i == (inlined_variants_.length() - 1)) &&
        non_inlined_variants_.is_empty()) {
      // If it is the last variant use a check class or check smi
      // instruction which can deoptimize, followed unconditionally by the
      // body.  Check a redefinition of the receiver, to prevent the check
      // from being hoisted.
      RedefinitionInstr* redefinition =
          new RedefinitionInstr(new Value(receiver));
      redefinition->set_ssa_temp_index(
          owner_->caller_graph()->alloc_ssa_temp_index());
      cursor = AppendInstruction(cursor, redefinition);
      if (inlined_variants_[i].cid == kSmiCid) {
        CheckSmiInstr* check_smi =
            new CheckSmiInstr(new Value(redefinition), call_->deopt_id());
        check_smi->InheritDeoptTarget(call_);
        cursor = AppendInstruction(cursor, check_smi);
      } else {
        const ICData& old_checks = call_->ic_data();
        const ICData& new_checks = ICData::ZoneHandle(
            ICData::New(Function::Handle(old_checks.function()),
                        String::Handle(old_checks.target_name()),
                        Array::Handle(old_checks.arguments_descriptor()),
                        old_checks.deopt_id(),
                        1));  // Number of args tested.
        new_checks.AddReceiverCheck(inlined_variants_[i].cid,
                                    *inlined_variants_[i].target);
        CheckClassInstr* check_class =
            new CheckClassInstr(new Value(redefinition),
                                call_->deopt_id(),
                                new_checks);
        check_class->InheritDeoptTarget(call_);
        cursor = AppendInstruction(cursor, check_class);
      }
      // The next instruction is the first instruction of the inlined body.
      // Handle the two possible cases (unshared and shared subsequent
      // predecessors) separately.
      BlockEntryInstr* callee_entry = inlined_entries_[i];
      if (callee_entry->IsGraphEntry()) {
        // Unshared.  Graft the normal entry on after the check class
        // instruction.
        TargetEntryInstr* target =
            callee_entry->AsGraphEntry()->normal_entry();
        cursor->LinkTo(target->next());
        target->ReplaceAsPredecessorWith(current_block);
        // Unuse all inputs of the graph entry and the normal entry. They are
        // not in the graph anymore.
        callee_entry->UnuseAllInputs();
        target->UnuseAllInputs();
        // All blocks that were dominated by the normal entry are now
        // dominated by the current block.
        for (intptr_t j = 0;
             j < target->dominated_blocks().length();
             ++j) {
          BlockEntryInstr* block = target->dominated_blocks()[j];
          current_block->AddDominatedBlock(block);
        }
      } else if (callee_entry->IsJoinEntry()) {
        // Shared inlined body and this is a subsequent entry.  We have
        // already constructed a join and set its dominator.  Add a jump to
        // the join.
        JoinEntryInstr* join = callee_entry->AsJoinEntry();
        ASSERT(join->dominator() != NULL);
        GotoInstr* goto_join = new GotoInstr(join);
        goto_join->InheritDeoptTarget(join);
        cursor->LinkTo(goto_join);
        current_block->set_last_instruction(goto_join);
      } else {
        // There is no possibility of a TargetEntry (the first entry to a
        // shared inlined body) because this is the last inlined entry.
        UNREACHABLE();
      }
      cursor = NULL;
    } else {
      // For all variants except the last, use a branch on the loaded class
      // id.
      const Smi& cid = Smi::ZoneHandle(Smi::New(inlined_variants_[i].cid));
      ConstantInstr* cid_constant = new ConstantInstr(cid);
      cid_constant->set_ssa_temp_index(
          owner_->caller_graph()->alloc_ssa_temp_index());
      StrictCompareInstr* compare =
          new StrictCompareInstr(call_->instance_call()->token_pos(),
                                 Token::kEQ_STRICT,
                                 new Value(load_cid),
                                 new Value(cid_constant));
      BranchInstr* branch = new BranchInstr(compare);
      branch->InheritDeoptTarget(call_);
      AppendInstruction(AppendInstruction(cursor, cid_constant), branch);
      current_block->set_last_instruction(branch);
      cursor = NULL;

      // 2. Handle a match by linking to the inlined body.  There are three
      // cases (unshared, shared first predecessor, and shared subsequent
      // predecessors).
      BlockEntryInstr* callee_entry = inlined_entries_[i];
      TargetEntryInstr* true_target = NULL;
      if (callee_entry->IsGraphEntry()) {
        // Unshared.
        true_target = callee_entry->AsGraphEntry()->normal_entry();
        // Unuse all inputs of the graph entry. It is not in the graph anymore.
        callee_entry->UnuseAllInputs();
      } else if (callee_entry->IsTargetEntry()) {
        // Shared inlined body and this is the first entry.  We have already
        // constructed a join and this target jumps to it.
        true_target = callee_entry->AsTargetEntry();
        BlockEntryInstr* join =
            true_target->last_instruction()->SuccessorAt(0);
        current_block->AddDominatedBlock(join);
      } else {
        // Shared inlined body and this is a subsequent entry.  We have
        // already constructed a join.  We need a fresh target that jumps to
        // the join.
        JoinEntryInstr* join = callee_entry->AsJoinEntry();
        ASSERT(join != NULL);
        ASSERT(join->dominator() != NULL);
        true_target =
            new TargetEntryInstr(owner_->caller_graph()->allocate_block_id(),
                                 call_->GetBlock()->try_index());
        true_target->InheritDeoptTarget(join);
        GotoInstr* goto_join = new GotoInstr(join);
        goto_join->InheritDeoptTarget(join);
        true_target->LinkTo(goto_join);
        true_target->set_last_instruction(goto_join);
      }
      *branch->true_successor_address() = true_target;
      current_block->AddDominatedBlock(true_target);

      // 3. Prepare to handle a match failure on the next iteration or the
      // fall-through code below for non-inlined variants.
      TargetEntryInstr* false_target =
          new TargetEntryInstr(owner_->caller_graph()->allocate_block_id(),
                               call_->GetBlock()->try_index());
      false_target->InheritDeoptTarget(call_);
      *branch->false_successor_address() = false_target;
      current_block->AddDominatedBlock(false_target);
      cursor = current_block = false_target;
    }
  }

  // Handle any non-inlined variants.
  if (!non_inlined_variants_.is_empty()) {
    // Move push arguments of the call.
    for (intptr_t i = 0; i < call_->ArgumentCount(); ++i) {
      PushArgumentInstr* push = call_->PushArgumentAt(i);
      push->ReplaceUsesWith(push->value()->definition());
      push->previous()->LinkTo(push->next());
      cursor->LinkTo(push);
      cursor = push;
    }
    const ICData& old_checks = call_->ic_data();
    const ICData& new_checks = ICData::ZoneHandle(
        ICData::New(Function::Handle(old_checks.function()),
                    String::Handle(old_checks.target_name()),
                    Array::Handle(old_checks.arguments_descriptor()),
                    old_checks.deopt_id(),
                    1));  // Number of args tested.
    for (intptr_t i = 0; i < non_inlined_variants_.length(); ++i) {
      new_checks.AddReceiverCheck(non_inlined_variants_[i].cid,
                                  *non_inlined_variants_[i].target,
                                  non_inlined_variants_[i].count);
    }
    PolymorphicInstanceCallInstr* fallback_call =
        new PolymorphicInstanceCallInstr(call_->instance_call(),
                                         new_checks,
                                         true);  // With checks.
    fallback_call->set_ssa_temp_index(
        owner_->caller_graph()->alloc_ssa_temp_index());
    fallback_call->InheritDeoptTarget(call_);
    ReturnInstr* fallback_return =
        new ReturnInstr(call_->instance_call()->token_pos(),
                        new Value(fallback_call));
    fallback_return->InheritDeoptTargetAfter(call_);
    AppendInstruction(AppendInstruction(cursor, fallback_call),
                      fallback_return);
    exit_collector_->AddExit(fallback_return);
    cursor = NULL;
  } else {
    // Remove push arguments of the call.
    for (intptr_t i = 0; i < call_->ArgumentCount(); ++i) {
      PushArgumentInstr* push = call_->PushArgumentAt(i);
      push->ReplaceUsesWith(push->value()->definition());
      push->RemoveFromGraph();
    }
  }
  return entry;
}


void PolymorphicInliner::Inline() {
  // Consider the polymorphic variants in order by frequency.
  FlowGraphCompiler::SortICDataByCount(call_->ic_data(), &variants_);
  for (intptr_t var_idx = 0; var_idx < variants_.length(); ++var_idx) {
    const Function& target = *variants_[var_idx].target;
    const intptr_t receiver_cid = variants_[var_idx].cid;

    // First check if this is the same target as an earlier inlined variant.
    if (CheckInlinedDuplicate(target)) {
      inlined_variants_.Add(variants_[var_idx]);
      continue;
    }

    // Also check if this is the same target as an earlier non-inlined
    // variant.  If so and since inlining decisions are costly, do not try
    // to inline this variant.
    if (CheckNonInlinedDuplicate(target)) {
      non_inlined_variants_.Add(variants_[var_idx]);
      continue;
    }

    // Make an inlining decision.
    if (TryInlining(receiver_cid, target)) {
      inlined_variants_.Add(variants_[var_idx]);
    } else {
      non_inlined_variants_.Add(variants_[var_idx]);
    }
  }

  // If there are no inlined variants, leave the call in place.
  if (inlined_variants_.is_empty()) return;

  // Now build a decision tree (a DAG because of shared inline variants) and
  // inline it at the call site.
  TargetEntryInstr* entry = BuildDecisionGraph();
  exit_collector_->ReplaceCall(entry);
}


static uint16_t ClampUint16(intptr_t v) {
  return (v > 0xFFFF) ? 0xFFFF : static_cast<uint16_t>(v);
}


void FlowGraphInliner::CollectGraphInfo(FlowGraph* flow_graph) {
  GraphInfoCollector info;
  info.Collect(*flow_graph);
  const Function& function = flow_graph->parsed_function().function();
  function.set_optimized_instruction_count(
      ClampUint16(info.instruction_count()));
  function.set_optimized_call_site_count(ClampUint16(info.call_site_count()));
}


void FlowGraphInliner::Inline() {
  // Collect graph info and store it on the function.
  // We might later use it for an early bailout from the inlining.
  CollectGraphInfo(flow_graph_);

  if ((FLAG_inlining_filter != NULL) &&
      (strstr(flow_graph_->
              parsed_function().function().ToFullyQualifiedCString(),
              FLAG_inlining_filter) == NULL)) {
    return;
  }

  TRACE_INLINING(OS::Print(
      "Inlining calls in %s\n",
      flow_graph_->parsed_function().function().ToCString()));

  if (FLAG_trace_inlining &&
      (FLAG_print_flow_graph || FLAG_print_flow_graph_optimized)) {
    OS::Print("Before Inlining of %s\n", flow_graph_->
              parsed_function().function().ToFullyQualifiedCString());
    FlowGraphPrinter printer(*flow_graph_);
    printer.PrintBlocks();
  }

  CallSiteInliner inliner(flow_graph_);
  inliner.InlineCalls();

  if (inliner.inlined()) {
    flow_graph_->DiscoverBlocks();
    if (FLAG_trace_inlining) {
      OS::Print("Inlining growth factor: %f\n", inliner.GrowthFactor());
      if (FLAG_print_flow_graph || FLAG_print_flow_graph_optimized) {
        OS::Print("After Inlining of %s\n", flow_graph_->
                  parsed_function().function().ToFullyQualifiedCString());
        FlowGraphPrinter printer(*flow_graph_);
        printer.PrintBlocks();
      }
    }
  }
}

}  // namespace dart
