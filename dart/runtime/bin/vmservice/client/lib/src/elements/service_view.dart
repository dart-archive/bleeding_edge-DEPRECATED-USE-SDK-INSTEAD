// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library service_object_view_element;

import 'dart:html';
import 'package:logging/logging.dart';
import 'package:observatory/service.dart';
import 'package:observatory/elements.dart';
import 'package:polymer/polymer.dart';
import 'observatory_element.dart';

@CustomTag('service-view')
class ServiceObjectViewElement extends ObservatoryElement {
  @published ServiceObject object;

  ServiceObjectViewElement.created() : super.created();

  ObservatoryElement _constructElementForObject() {
    var type = object.serviceType;
    switch (type) {
      case 'AllocationProfile':
        HeapProfileElement element = new Element.tag('heap-profile');
        element.profile = object;
        return element;
      case 'BreakpointList':
        BreakpointListElement element = new Element.tag('breakpoint-list');
        element.msg = object;
        return element;
      case 'Class':
        ClassViewElement element = new Element.tag('class-view');
        element.cls = object;
        return element;
      case 'Code':
        CodeViewElement element = new Element.tag('code-view');
        element.code = object;
        return element;
      case 'Error':
        ErrorViewElement element = new Element.tag('error-view');
        element.error = object;
        return element;
      case 'Field':
        FieldViewElement element = new Element.tag('field-view');
        element.field = object;
        return element;
      case 'Function':
        FunctionViewElement element = new Element.tag('function-view');
        element.function = object;
        return element;
      case 'HeapMap':
        HeapMapElement element = new Element.tag('heap-map');
        element.fragmentation = object;
        return element;
      case 'Array':
      case 'Bool':
      case 'Closure':
      case 'Double':
      case 'GrowableObjectArray':
      case 'Instance':
      case 'Smi':
      case 'String':
      case 'Type':
        InstanceViewElement element = new Element.tag('instance-view');
        element.instance = object;
        return element;
      case 'Isolate':
        IsolateViewElement element = new Element.tag('isolate-view');
        element.isolate = object;
        return element;
      case 'Library':
        LibraryViewElement element = new Element.tag('library-view');
        element.library = object;
        return element;
      case 'Profile':
        IsolateProfileElement element = new Element.tag('isolate-profile');
        element.profile = object;
        return element;
      case 'ServiceError':
        ServiceErrorViewElement element =
            new Element.tag('service-error-view');
        element.error = object;
        return element;
      case 'ServiceException':
        ServiceExceptionViewElement element =
                    new Element.tag('service-exception-view');
        element.exception = object;
        return element;
      case 'Script':
        ScriptViewElement element = new Element.tag('script-view');
        element.script = object;
        return element;
      case 'StackTrace':
        StackTraceElement element = new Element.tag('stack-trace');
        element.trace = object;
        return element;
      case 'VM':
        VMViewElement element = new Element.tag('vm-view');
        element.vm = object;
        return element;
      default:
        JsonViewElement element = new Element.tag('json-view');
        element.map = object;
        return element;
    }
  }

  objectChanged(oldValue) {
    // Remove the current view.
    children.clear();
    if (object == null) {
      Logger.root.info('Viewing null object.');
      return;
    }
    var type = object.serviceType;
    var element = _constructElementForObject();
    if (element == null) {
      Logger.root.info('Unable to find a view element for \'${type}\'');
      return;
    }
    children.add(element);
    Logger.root.info('Viewing object of \'${type}\'');
  }
}
