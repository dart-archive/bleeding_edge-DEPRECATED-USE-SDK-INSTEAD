/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.preferences;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.DartUiException;
import com.google.dart.tools.ui.internal.DartUiStatus;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.commands.contexts.ContextManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.BindingManager;
import org.eclipse.jface.bindings.Scheme;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Defines persistence operations for Dart Editor key bindings.
 * <p>
 * The key binding file is an XML file with the following schema:
 * 
 * <pre>
 * dartKeyBindings - Root element containing a list of keyBinding
 * keyBinding - Key binding element with attributes:
 *   commandName - The user-readable name of the command (as it appears in the UI)
 *   customKeySequence - The user-customized key sequence (default to dartKeySequence)
 *   dartKeySequence - The standard key sequence defined by Dart Editor
 *   platform - The platform name for platform-specific bindings (optional)
 * </pre>
 * TODO allow empty command name to unbind a key sequence<br>
 * TODO allow empty keys to remove all bindings for a command
 */
public class DartKeyBindingPersistence {

  private class KeyBindingHandler extends DefaultHandler {

    private List<Map<String, String>> bindings;
    private int version;
    private Map<String, String> attribs;

    @Override
    public void endElement(String uri, String localName, String qName) {
      if (qName.equals(XML_NODE_BINDING)) {
        bindings.add(attribs);
        attribs = null;
      }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {

      if (qName.equals(XML_NODE_BINDING)) {

        if (version != 1) {
          // only check version if it has content
          throw new SAXException();
        }
        attribs = new HashMap<String, String>();
        String commandName = attributes.getValue(XML_ATTRIBUTE_COMMANDID);
        attribs.put(XML_ATTRIBUTE_COMMANDID, commandName);
        String context = attributes.getValue(XML_ATTRIBUTE_CONTEXTID);
        attribs.put(XML_ATTRIBUTE_CONTEXTID, context);
        String dartkeys = attributes.getValue(XML_ATTRIBUTE_KEYS);
        attribs.put(XML_ATTRIBUTE_KEYS, dartkeys);
        String platform = attributes.getValue(XML_ATTRIBUTE_PLATFORM);
        attribs.put(XML_ATTRIBUTE_PLATFORM, platform);

      } else if (qName.equals(XML_NODE_ROOT)) {

        bindings = new ArrayList<Map<String, String>>();
        try {
          String vers = attributes.getValue(XML_ATTRIBUTE_VERSION);
          if (vers != null) {
            version = Integer.parseInt(vers);
          }
        } catch (NumberFormatException ex) {
          throw new SAXException(ex);
        }

      }
    }

    List<Map<String, String>> getBindings() {
      return bindings;
    }
  }

  public static final String CUSTOM_KEY_BINDING_STRING = DartToolsPlugin.PLUGIN_ID + ".keyBindings";

  private static final String SERIALIZATION_PROBLEM = "Problems serializing key bindings to XML."; //$NON-NLS-1$
  private static final String DESERIALIZATION_PROBLEM = "Problems reading key bindings from XML."; //$NON-NLS-1$
  private static final String DART_BINDING_SCHEME = "com.google.dart.tools.dartAcceleratorConfiguration"; //$NON-NLS-1$
  private static final String XML_NODE_ROOT = "dartKeyBindings"; //$NON-NLS-1$
  private static final String XML_NODE_BINDING = "keyBinding"; //$NON-NLS-1$
  private static final String XML_ATTRIBUTE_VERSION = "version"; //$NON-NLS-1$
  // the key sequence names are chosen to be adjacent after lexically sorting attribute names
  private static final String XML_ATTRIBUTE_KEYS = "keySequence"; //$NON-NLS-1$
  // the command name is first in a lexical sort of attribute names
  private static final String XML_ATTRIBUTE_COMMANDID = "commandName"; //$NON-NLS-1$
  private static final String XML_ATTRIBUTE_CONTEXTID = "context"; //$NON-NLS-1$
  private static final String XML_ATTRIBUTE_PLATFORM = "platform"; //$NON-NLS-1$// optional attribute
  private static final String XML_UNKNOWN = ""; //$NON-NLS-1$ // should never be used
  private static final String DESCR_FORMAT = "The format is straightforward, consisting of three attributes plus one that is optional.\n"
      + "The required attributes are the command name, which is the same as it appears in\n"
      + "menus, and the key sequence, which is all uppercase. The context is an internal identifier\n"
      + "used to indicate in which portion of the UI the binding is active. The optional attribute\n"
      + "is the name of the platform to which the binding applies if it is not universal.";

  private static DartUiException createException(Throwable ex, String message) {
    return new DartUiException(DartUiStatus.createError(IStatus.ERROR, message, ex));
  }

  /**
   * The workbench's activity manager. This activity manager is used to see if certain commands
   * should be filtered from the user interface.
   */
  private IActivityManager activityManager;

  /**
   * The workbench's binding service. This binding service is used to access the current set of
   * bindings, and to persist changes.
   */
  private IBindingService bindingService;

  /**
   * A local copy of the workbench's binding manager. Changes are made locally while processing the
   * new bindings. When everything is complete and error-free the changes are persisted.
   */
  private BindingManager bindingManager;

  /**
   * A map of binding elements that are being written.
   */
  private Map<String, Element> knownBindings;

  private ICommandService commandService;

  public DartKeyBindingPersistence(IActivityManager activityManager,
      IBindingService bindingService, ICommandService commandService) {
    this.activityManager = activityManager;
    this.bindingService = bindingService;
    this.commandService = commandService;
  }

  public Binding findBinding(String commandName, String platform, String context)
      throws NotDefinedException {
    Binding[] bindings = bindingService.getBindings();
    if (bindings != null) {
      for (Binding binding : bindings) {
        if (binding.getSchemeId().equals(DART_BINDING_SCHEME)
            && (context == null || context.equals(binding.getContextId()))) {
          if ((platform != null && platform.equals(binding.getPlatform()))
              || binding.getPlatform() == null) {
            ParameterizedCommand pc = binding.getParameterizedCommand();
            if (pc != null) {
              Command cmd = pc.getCommand();
              if (cmd != null) {
                try {
                  if (commandName.equals(pc.getName())) {
                    return binding;
                  }
                } catch (NotDefinedException e) {
                  DartCore.logError("Dropping key binding for " + commandName);
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Read a key binding file in the format created by {@code writeFile()}.
   * 
   * @param file The File of key bindings
   * @throws CoreException if there is a problem reading or parsing the file
   */
  public void readFile(File file, String encoding) throws CoreException {
    Reader reader = null;
    try {
      reader = new FileReader(file);
      readFrom(reader);
    } catch (IOException ex) {
      throw createException(ex, ex.getMessage());
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException ex) {
          DartToolsPlugin.log(ex);
        }
      }
    }
    try {
      String bindString = FileUtilities.getContents(file, encoding);
      IPreferenceStore prefs = DartToolsPlugin.getDefault().getPreferenceStore();
      prefs.setValue(CUSTOM_KEY_BINDING_STRING, bindString);
    } catch (IOException ex) {
      DartToolsPlugin.log(ex);
    }
  }

  /**
   * Read key bindings in the format created by {@code writeFile()}.
   * 
   * @param reader The Reader used to read the input
   * @throws CoreException if there is a problem reading or parsing the file
   */
  public void readFrom(Reader reader) throws CoreException {
    initBindingManager();
    bindingManager.setBindings(new Binding[0]);

    List<Map<String, String>> newBindings;
    newBindings = readKeyBindingsFromStream(new InputSource(reader));
    for (Map<String, String> map : newBindings) {
      updateKeyBinding(map);
    }

    saveKeyBindingPreferences();
  }

  /**
   * Remove all custom key bindings and restore default bindings.
   */
  public void resetBindings() throws CoreException {
    IPreferenceStore prefs = DartToolsPlugin.getDefault().getPreferenceStore();
    prefs.setValue(CUSTOM_KEY_BINDING_STRING, "");
    bindingService.readRegistryAndPreferences(commandService);
    initBindingManager(); // deletes all USER bindings
    saveKeyBindingPreferences();
  }

  /**
   * Called at start up; restore custom bindings saved in a previous session, if any.
   */
  public void restoreBindingPreferences() {
    IPreferenceStore prefs = DartToolsPlugin.getDefault().getPreferenceStore();
    String prefString = prefs.getString(CUSTOM_KEY_BINDING_STRING);
    if (prefString == null || prefString.isEmpty()) {
      return;
    }
    Reader reader = new StringReader(prefString);
    try {
      readFrom(reader);
    } catch (CoreException ex) {
      DartToolsPlugin.log(ex);
    }
  }

  /**
   * Write the currently-defined key bindings to a file. This file is intended to be edited by users
   * to create custom key bindings. For usability, ID strings are not written. For readability, an
   * XML format is used so that each value has a label indicating its purpose. The current key
   * binding is written twice, once as a reference that is used to identify the binding when the
   * file is read, and another time as a template for the user to edit. The command name is written
   * to help the user identify which command is being modified. A platform identifier is included if
   * it exists; this is used to define platform-specific bindings, which are common on Mac OSX.
   * 
   * @param file The File to write
   * @param encoding The file encoding to use
   * @throws CoreException if there are any problem writing the file
   */
  public void writeFile(File file, String encoding) throws CoreException {
    try {
      OutputStream stream = new FileOutputStream(file);
      try {
        writeKeyBindingsToStream(stream, encoding);
      } finally {
        try {
          stream.close();
        } catch (IOException ex) {
          DartToolsPlugin.log(ex);
        }
      }
    } catch (IOException e) {
      throw createException(e, SERIALIZATION_PROBLEM);
    }
  }

  private Element createBindingElement(Binding binding, Document document) {
    // binding is known to have a ParameterizedCommand whose command ID matches a registered Command
    String keys = binding.getTriggerSequence().toString();
    String platform = binding.getPlatform();
    String context = binding.getContextId();
    System.out.println(context);
    String commandName;
    try {
      commandName = binding.getParameterizedCommand().getName();
    } catch (NotDefinedException ex) {
      return null;
    }
    String id = keys + commandName + (platform == null ? "" : platform) + context;
    if (knownBindings.containsKey(id)) {
      if (binding.getType() == Binding.USER) {
        // A SYSTEM binding has already been created
        return null; // do not add it again
      } else {
        // A USER binding has already been created; update its standard key binding
        Element element = knownBindings.get(id);
        element.setAttribute(XML_ATTRIBUTE_KEYS, binding.getTriggerSequence().toString());
        return null;
      }
    }
    Element element = document.createElement(XML_NODE_BINDING);
    element.setAttribute(XML_ATTRIBUTE_KEYS, keys);
    element.setAttribute(XML_ATTRIBUTE_COMMANDID, commandName);
    element.setAttribute(XML_ATTRIBUTE_CONTEXTID, context);
    if (platform != null) {
      element.setAttribute(XML_ATTRIBUTE_PLATFORM, platform);
    }
    knownBindings.put(id, element);
    return element;
  }

  private void initBindingManager() {
    bindingManager = new BindingManager(new ContextManager(), new CommandManager());
    Scheme[] definedSchemes = bindingService.getDefinedSchemes();
    try {
      for (int i = 0; i < definedSchemes.length; i++) {
        Scheme scheme = definedSchemes[i];
        Scheme copy = bindingManager.getScheme(scheme.getId());
        copy.define(scheme.getName(), scheme.getDescription(), scheme.getParentId());
      }
      bindingManager.setActiveScheme(bindingService.getActiveScheme());
    } catch (NotDefinedException e) {
      throw new Error("Internal error in DartKeyBindingPersistence"); //$NON-NLS-1$
    }
    bindingManager.setLocale(bindingService.getLocale());
    bindingManager.setPlatform(bindingService.getPlatform());

    Binding[] currentBindings = bindingService.getBindings();
    Set<Binding> trimmedBindings = new HashSet<Binding>();
    if (currentBindings != null) {
      for (Binding binding : currentBindings) {
        if (binding.getType() != Binding.USER) {
          trimmedBindings.add(binding);
        }
      }
    }
    Binding[] trimmedBindingArray = trimmedBindings.toArray(new Binding[trimmedBindings.size()]);
    bindingManager.setBindings(trimmedBindingArray);
  }

  private boolean isActive(Command command) {
    return activityManager.getIdentifier(command.getId()).isEnabled();
  }

  private List<Map<String, String>> readKeyBindingsFromStream(InputSource inputSource)
      throws CoreException {

    KeyBindingHandler handler = new KeyBindingHandler();
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser parser = factory.newSAXParser();
      parser.parse(inputSource, handler);
    } catch (SAXException e) {
      throw createException(e, DESERIALIZATION_PROBLEM);
    } catch (IOException e) {
      throw createException(e, DESERIALIZATION_PROBLEM);
    } catch (ParserConfigurationException e) {
      throw createException(e, DESERIALIZATION_PROBLEM);
    }
    return handler.getBindings();

  }

  /**
   * Serialize the current key bindings and store them in the preference for next startup.
   */
  private void saveKeyBindingPreferences() throws DartUiException {
    try {
      bindingService.savePreferences(bindingManager.getActiveScheme(), bindingManager.getBindings());
    } catch (IOException e) {
      throw createException(e, DESERIALIZATION_PROBLEM);
    }
  }

  private Binding[] sort(Binding[] bindings) {
    Comparator<Binding> comp = new Comparator<Binding>() {
      @Override
      public int compare(Binding b0, Binding b1) {
        ParameterizedCommand c0 = b0.getParameterizedCommand();
        ParameterizedCommand c1 = b1.getParameterizedCommand();
        int k;
        if (c0 == null || c1 == null) {
          if (c0 != c1) {
            k = c0 == null ? -1 : 1;
          } else {
            k = 0;
          }
        } else {
          try {
            k = c0.getCommand().getName().compareTo(c1.getCommand().getName());
          } catch (NotDefinedException ex) {
            k = 0;
          }
        }
        if (k == 0) {
          String p0 = b0.getPlatform();
          if (p0 == null) {
            p0 = XML_UNKNOWN;
          }
          String p1 = b1.getPlatform();
          if (p1 == null) {
            p1 = XML_UNKNOWN;
          }
          k = p0.compareTo(p1);
        }
        if (k == 0) {
          k = b0.getTriggerSequence().toString().compareTo(b1.getTriggerSequence().toString());
        }
        return k;
      }
    };
    Arrays.sort(bindings, comp);
    return bindings;
  }

  private void updateKeyBinding(Map<String, String> map) throws CoreException {
    try {
      String platform = map.get(XML_ATTRIBUTE_PLATFORM);
      String commandName = map.get(XML_ATTRIBUTE_COMMANDID);
      String context = map.get(XML_ATTRIBUTE_CONTEXTID);
      String stdKeys = map.get(XML_ATTRIBUTE_KEYS);
      Binding binding = findBinding(commandName, platform, context);
      if (binding == null) {
        return;
      }
      Command command = binding.getParameterizedCommand().getCommand();
      ParameterizedCommand cmd = new ParameterizedCommand(command, null);
      String schemeId = binding.getSchemeId();
      String contextId = binding.getContextId();
      String locale = binding.getLocale();
      String wm = null;
      int type = Binding.USER;
      KeySequence stdSeq = KeySequence.getInstance(stdKeys);
      Binding newBind = new KeyBinding(stdSeq, cmd, schemeId, contextId, locale, platform, wm, type);
      bindingManager.removeBindings(stdSeq, schemeId, contextId, null, platform, null, type);
      bindingManager.addBinding(newBind);
    } catch (NotDefinedException ex) {
      throw createException(ex, ex.getMessage());
    } catch (ParseException ex) {
      throw createException(ex, ex.getMessage());
    }
  }

  private void writeKeyBindingsToStream(OutputStream stream, String encoding) throws CoreException {
    try {
      knownBindings = new HashMap<String, Element>();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.newDocument();
      Element rootElement = document.createElement(XML_NODE_ROOT);
      rootElement.setAttribute(XML_ATTRIBUTE_VERSION, Integer.toString(1));
      document.appendChild(rootElement);
      Comment comment = document.createComment(DESCR_FORMAT);
      document.getElementsByTagName(XML_NODE_ROOT).item(0).appendChild(comment);

      Binding[] bindings = bindingService.getBindings();
      if (bindings != null) {
        bindings = sort(bindings);
        for (Binding binding : bindings) {
          if (binding.getSchemeId().equals(DART_BINDING_SCHEME)) {
            ParameterizedCommand pc = binding.getParameterizedCommand();
            if (pc != null) {
              Command cmd = pc.getCommand();
              if (cmd != null && isActive(cmd)) {
                Element bindingElement = createBindingElement(binding, document);
                if (bindingElement != null) {
                  rootElement.appendChild(bindingElement);
                }
              }
            }
          }
        }
      }

      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
      transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
      transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
      transformer.transform(new DOMSource(document), new StreamResult(stream));
    } catch (TransformerException e) {
      throw createException(e, SERIALIZATION_PROBLEM);
    } catch (ParserConfigurationException e) {
      throw createException(e, SERIALIZATION_PROBLEM);
    }
  }

}
