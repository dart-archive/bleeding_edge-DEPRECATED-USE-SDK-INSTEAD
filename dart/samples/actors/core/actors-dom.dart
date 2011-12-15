
class _WindowActor extends Actor {
  
  _WindowActor() : super() {
    
    /**
     * Shows a message using window.alert() function.
     */
    on["alert"] = (String m) {
      window.alert("${m}");
    };
    
    /**
     * Prints a message to console. 
     */
    on["print"] = (String m) {
      print("${m}");
    };
    
    /**
     * Sets styles for a number of elements selected by 'selector'.
     * Properties to be set are represented by propertyNames list and 
     * the values for those properties are a list of list of strings 
     * for each element.
     * For example, if one wants to change the 'top' and 'left'
     * properties of two div elements with 'div1-id' and 'div2-id' ids,
     * then selector must be "#div1-id, #div2-id", propertyNames must be 
     * ['top', 'left'] and values must be 
     * [['100px', '100px'], ['200px', '200px']].
     */
    on["setStyles"] = (String selector, 
        List<String> propertyNames, List<List<String>> values) {
      final elements = document.queryAll(selector);
      int i = 0;
      for (final element in elements) {
        int j = 0;
        if (element != null) {
          for (final propertyName in propertyNames) {
            element.style.setProperty(propertyName, values[i][j], "");
            j++;
          }
        }
        i++;
      }
    };
    
    /**
     * Sets attributes for a number of elements selected by 'selector'.
     * Attributes to be set are represented by attrNames list and 
     * the values for those attributes are a list of list of strings 
     * for each element.
     * For example, if one wants to change the 'class' attribute of two 
     * div elements with 'div1-id' and 'div2-id' ids, then selector must 
     * be "#div1-id, #div2-id", propertyNames must be ['class'] and values 
     * must be [['css-class1'], ['css-class2']].
     */
    on["setAttributes"] = (String selector,
        List<String> attrNames, List<List<String>> values) {
      final elements = document.queryAll(selector);
      int i = 0;
      for (final element in elements) {
        int j = 0;
        if (element != null) {
          for (final attrName in attrNames) {
            element.attributes[attrName] = values[i][j];
            j++;
          }
        }
        i++;
      }
    };
    
    /**
     * Retrieves the value of an attribute, 'attrName', of the element
     * selected by 'selector'. Selector must select one unique element.
     */
    on["getAttribute"] = (String selector, String attrName, Reply r) {
      final element = document.query(selector);
      if (element != null)
        r.respond([element.attributes[attrName]]);
      else r.respond([null]);
    };
    
    /**
     * Retrieves the value of an element selected by 'selector' and sends a 
     * response back with the retreived value.
     */
    on["getValue"] = (String selector, Reply r) {
      final element = document.query(selector);
      if (element != null)
        r.respond([element.value]);
      else 
        r.respond([null]);
    };
    
    /**
     * Removes an attribute , 'attrName', from an element selected 
     * by 'selector'.
     */
    on["removeAttribute"] = (String selector, String attrName) {
      final element = document.query(selector);
      if (element != null)
        element.attributes.remove(attrName);
    };
    
    /**
     * Sets the innerHTML of an element selected by 'selector' to the value
     * passed by 'value'.
     */
    on["innerHTML"] = (String selector, String value) {
      final element = document.query(selector);
      if (element != null)
        element.innerHTML = value;
    };
    
    /**
     * Creates a number of elements and set their id attributes.
     * The tag name is passed by 'elemName' and the ids is the list 
     * of identifiers to be set for each element. 
     */
    on["create"] = (String elemName, List<String> ids) {
      for (String id in ids) {
        final element = new Element.tag(elemName);
        document.body.elements.add(element);
        element.attributes["id"] = id;
      }
    };
    
    /**
     * Creates a number of elements inside another element selected by 
     * 'selector' and set their id attributes.
     * The tag name is passed by 'elemName' and the ids is the list 
     * of identifiers to be set for each element. 
     */
    on["createIn"] = (String elemName, List<String> ids, String selector) {
      final inElem = document.query(selector);
      for (String id in ids) {
        final element = new Element.tag(elemName);
        document.body.elements.add(element);
        element.attributes["id"] = id;
        inElem.elements.add(element);
      }
    };
    
    /**
     * Sends a message back using the 'reply' parameter after specified number
     * of milliseconds specified by 'timeout' parameter.
     */
    on["setTimeout"] = (Reply reply, int timeout) {
      window.setTimeout(() {reply.respond();}, timeout);
    };
    
    /**
     * Installs an event listener for onclick event of an element selected by
     * 'selector'. This event listener sends a response back using the 'reply'
     * parameter.
     */
    on["onclick"] = (String selector, Reply reply, [bool useCapture=false]) {
      final element = document.query(selector);
      if (element != null) {
        element.on.click.add((Event e) {
          if (e is MouseEvent)
            reply.respond([new MouseEventWrapper.fromMouseEvent(e)]);
        });
      }
      else {
        reply.respond([null]);
      }
    };
    
    /**
     * Installs an event listener for keyPress event. This event listener sends 
     * a response back using the 'reply' parameter. 
     */
    on["onKeyPress"] = (Reply reply) {
      document.on.keyPress.add((KeyboardEvent e) {
        reply.respond([e.keyCode]);
      });
    };
    
    /**
     * Sets the image data of a canvas sepecified by a selector, 'selector', 
     * to the data passed by 'data' parameter. The starting point is (x, y)
     * and the width and height of the region are represented by 'w' and 'h'
     * parameters.
     */
    on["setCanvas2DImage"] = 
        (String selector, int x, int y, int w, int h, var data) {
      CanvasElement canvas = document.query(selector);
      final ctx = canvas.getContext("2d");
      final imgdata = ctx.getImageData(x, y, w, h);
      var hh = 0;
      for (int i = x; i < h; i++) {
        var k = 0;
        for (int j = y; j < w; j++) {
          int indx = hh*w*4+k*4;
          imgdata.data[indx]   = data[indx];
          imgdata.data[indx+1] = data[indx+1];
          imgdata.data[indx+2] = data[indx+2];
          imgdata.data[indx+3] = data[indx+3];
          k++;
        }
        hh++;
      }
      ctx.putImageData(imgdata, 0, 0);
    };
    
    /**
     * Installs an event listener for onMouseDown event of an element selected 
     * by 'selector'. This event listener sends a response back using the 
     * 'reply' parameter.
     */
    on["onMouseDown"] = (String selector, Reply reply) {
      final element = document.query(selector);
      element.on.mouseDown.add((MouseEvent e) {
        e.preventDefault();
        reply.respond([new MouseEventWrapper.fromMouseEvent(e)]);        
      });
    };

    /**
     * Installs an event listener for onMouseUp event of an element selected 
     * by 'selector'. This event listener sends a response back using the 
     * 'reply' parameter.
     */
    on["onMouseUp"] = (String selector, Reply reply) {
      final element = document.query(selector);
      element.on.mouseUp.add((MouseEvent e) {
        reply.respond([new MouseEventWrapper.fromMouseEvent(e)]);
      });
    };
    
    /**
     * Installs an event listener for onMouseMove event of an element selected 
     * by 'selector'. This event listener sends a response back using the 
     * 'reply' parameter.
     */
    on["onMouseMove"] = (String selector, Reply reply) {
      final element = document.query(selector);
      element.on.mouseMove.add((MouseEvent e) {
        e.preventDefault();
        reply.respond([new MouseEventWrapper.fromMouseEvent(e)]);
      });
    };
    
    /**
     * Installs an event listener for rect future of an element selected 
     * by 'selector'. This event listener sends a response back using the 
     * 'reply' parameter.
     */
    on["onRectReady"] = (String selector, Reply reply) {
      final element = document.query(selector);
      element.rect.then((ElementRect rect) {
        reply.respond([rect.offset]);
      });
    };
    
    /**
     * Removes a node identified by selectorWhat from the node identified by 
     * selectorFrom.
     */
    on["remove"] = (String selectorFrom, String selectorWhat) {
      document.query(selectorFrom).query(selectorWhat).remove();
    };

    /**
     * Creates a Text node and append it to the element identified by selector
     */
    on["createTextNode"] = (String selector, String text) {
      final element = document.query(selector);
      element.nodes.add(new Text(text));
    };

    /**
     * Clones nodes identified by selector and assign their ids and add them
     * to the node identified by inSelector
     */
    on["clone"] = (String selector, List<String> ids, bool deep) {
      final elements = document.queryAll(selector);
      int i = 0;
      for (final element in elements) {
        if (element != null) {
          final cloned = element.clone(deep);
          cloned.attributes["id"] = ids[i];
          document.body.elements.add(cloned);
        }
        i++;
      }
    };

    /**
     * Clones nodes identified by selector and assign their ids and add them
     * to the node identified by inSelector
     */
    on["cloneIn"] = (String selector, List<String> ids, String inSelector, 
        bool deep) {
      final inWhat = document.query(inSelector);
      final elements = document.queryAll(selector);
      int i = 0;
      for (final element in elements) {
        if (element != null) {
          final cloned = element.clone(deep);
          cloned.attributes["id"] = ids[i];
          inWhat.elements.add(cloned);
        }
        i++;
      }
    };
    
    /**
     * Queries an element specified by 'selector' and sends response back with 
     * a wrapper of the queried element.
     */
    on["query"] = (String selector, Reply reply) {
      final element = document.query(selector);
      if (element != null) reply.respond([new ElementWrapper(element)]);
      else reply.respond([null]);
    };
    
    /**
     * Queries all the elements specified by 'selector' and sends response back 
     * with wrappers of the queried elements.
     */
    on["queryAll"] = (String selector, Reply reply) {
      final elements = document.queryAll(selector);
      final elementWrappers = new List<ElementWrapper>();
      for (final e in elements) {
        if (e != null) elementWrappers.add(new ElementWrapper(e));
        else elementWrappers.add(null);
      }
      reply.respond([elementWrappers]);
    };
  }
}

class MouseEventWrapper {
  int pageX;
  int pageY;
  int button;
  int clientX;
  int clientY;
  bool ctrlKey;
  bool metaKey;
  int offsetX;
  int offsetY;
  int screenX;
  int screenY;
  bool shiftKey;
  int x;
  int y;
  
  MouseEventWrapper.fromMouseEvent(MouseEvent e) {
    pageX = e.pageX;
    pageY = e.pageY; 
    button = e.button; 
    clientX = e.clientX; 
    clientY = e.clientY;
    ctrlKey = e.ctrlKey;
    metaKey = e.metaKey;
    offsetX = e.offsetX;
    offsetY = e.offsetY;
    screenX = e.screenX;
    screenY = e.screenY;
    shiftKey = e.shiftKey;
    x = e.x;
    y = e.y;
  }
}

class ElementWrapper {
  String dir;
  String id;
  String innerHTML;
  String outerHTML;
  int tabIndex;
  String tagName;
  
  ElementWrapper(Element e) {
    id = e.id;
    dir = e.dir;
    innerHTML = e.innerHTML;
    outerHTML = e.outerHTML;
    tabIndex = e.tabIndex;
    tagName = e.tagName;
  }
}

class _WindowProxyImpl implements WindowProxy {
  
  ActorId _window;
  
  _WindowProxyImpl(this._window);
  
  void alert(String m) => _window.send("alert", [m]);
  
  void print(String m) => _window.send("print", [m]);
  
  void setStyles(String selector, 
      List<String> propertyNames, List<List<String>> values) => 
      _window.send("setStyles", [selector, propertyNames, values]);
  
  void setAttributes(String selector,
      List<String> attrNames, List<List<String>> values) =>
      _window.send("setAttributes", [selector, attrNames, values]);
  
  void removeAttribute(String selector, String attrName) =>
      _window.send("removeAttribute", [selector, attrName]);
  
  void innerHTML(String selector, String value) => 
      _window.send("innerHTML", [selector, value]);
      
  void create(String elemName, List<String> ids) => 
      _window.send("create", [elemName, ids]);
      
  void createIn(String elemName, List<String> ids, String selector) =>
      _window.send("createIn", [elemName, ids, selector]);
      
  void setTimeout(Reply t, int timeout) =>
      _window.send("setTimeout", [t, timeout]);
      
  void onclick(String selector, Reply reply, [bool useCapture=false]) => 
      _window.send("onclick", [selector, reply, useCapture]);
  
  void setCanvas2DImage(String selector, int x, int y, int w, int h, var data) 
      => _window.send("setCanvas2DImage", [selector, x, y, w, h, data]);
      
  void getAttribute(String selector, String attrName, Reply r) =>
      _window.send("getAttribute", [selector, attrName, r]);
      
  void getValue(String selector, Reply r) => 
      _window.send("getValue", [selector, r]);
  
  void onMouseDown(String selector, Reply reply) => 
      _window.send("onMouseDown", [selector, reply]);

  void onMouseUp(String selector, Reply reply) => 
      _window.send("onMouseUp", [selector, reply]);

  void onMouseMove(String selector, Reply reply) => 
      _window.send("onMouseMove", [selector, reply]);

  void onRectReady(String selector, Reply reply)
      => _window.send("onRectReady", [selector, reply]);
  
  void remove(String selectorFrom, String selectorWhat) =>
      _window.send("remove", [selectorFrom, selectorWhat]);
      
  void onKeyPress(Reply reply) => _window.send("onKeyPress", [reply]);
  
  void createTextNode(String selector, String text) => 
      _window.send("createTextNode", [selector, text]);
      
  void clone(String selector, List<String> ids, bool deep) =>
      _window.send("clone", [selector, ids, deep]);

  void cloneIn(String selector, List<String> ids, String inSelector, bool deep) 
      => _window.send("cloneIn", [selector, ids, inSelector, deep]);

  void query(String selector, Reply reply)
      => _window.send("query", [selector, reply]);
    
  void queryAll(String selector, Reply reply)
      => _window.send("queryAll", [selector, reply]);
}
