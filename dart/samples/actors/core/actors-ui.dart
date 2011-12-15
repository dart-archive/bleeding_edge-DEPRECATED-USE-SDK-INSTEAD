
interface WindowProxy {
  void alert(String m);
  
  void print(String m);
  
  void setStyles(String selector, 
      List<String> propertyNames, List<List<String>> values);
  
  void setAttributes(String selector,
      List<String> attrNames, List<List<String>> values);
  
  void removeAttribute(String selector, String attrName);
  
  void innerHTML(String selector, String value);
      
  void create(String elemName, List<String> ids);
      
  void createIn(String elemName, List<String> ids, String selector);
      
  void setTimeout(Reply t, int timeout);
      
  void onclick(String selector, Reply reply, bool useCapture);
  
  void setCanvas2DImage(String selector, int x, int y, int w, int h, var data);
      
  void getAttribute(String selector, String attrName, Reply r);
      
  void getValue(String selector, Reply r);
  
  void onMouseDown(String selector, Reply reply);

  void onMouseUp(String selector, Reply reply);

  void onMouseMove(String selector, Reply reply);

  void onRectReady(String selector, Reply reply);
  
  void remove(String selectorFrom, String selectorWhat);
      
  void onKeyPress(Reply reply);
  
  void createTextNode(String selector, String text);
      
  void clone(String selector, List<String> ids, bool deep);

  void cloneIn(String selector, List<String> ids, String inSelector, bool deep);

  void query(String selector, Reply reply);
    
  void queryAll(String selector, Reply reply);
}

class _NullWindowProxy implements WindowProxy {
  void alert(String m){}
  
  void print(String m){}
  
  void setStyles(String selector, 
      List<String> propertyNames, List<List<String>> values){}
  
  void setAttributes(String selector,
      List<String> attrNames, List<List<String>> values){}
  
  void removeAttribute(String selector, String attrName){}
  
  void innerHTML(String selector, String value){}
      
  void create(String elemName, List<String> ids){}
      
  void createIn(String elemName, List<String> ids, String selector){}
      
  void setTimeout(Reply t, int timeout){}
      
  void onclick(String selector, Reply reply, bool useCapture){}
  
  void setCanvas2DImage(String selector, int x, int y, int w, int h, var data){}
      
  void getAttribute(String selector, String attrName, Reply r){}
      
  void getValue(String selector, Reply r){}
  
  void onMouseDown(String selector, Reply reply){}

  void onMouseUp(String selector, Reply reply){}

  void onMouseMove(String selector, Reply reply){}

  void onRectReady(String selector, Reply reply){}
  
  void remove(String selectorFrom, String selectorWhat){}
      
  void onKeyPress(Reply reply){}
  
  void createTextNode(String selector, String text){}
      
  void clone(String selector, List<String> ids, bool deep){}

  void cloneIn(String selector, List<String> ids, String inSelector, bool deep){}

  void query(String selector, Reply reply){}
    
  void queryAll(String selector, Reply reply){}
}
