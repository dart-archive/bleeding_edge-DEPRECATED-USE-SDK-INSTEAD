/*
 * Copyright (c) 2014, the Dart project authors.
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
 *
 * This file has been automatically generated.  Please do not edit it manually.
 * To regenerate the file, use the script "pkg/analysis_server/spec/generate_files".
 */
package com.google.dart.server.generated.types;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.google.common.collect.Lists;
import com.google.dart.server.utilities.general.JsonUtilities;
import com.google.dart.server.utilities.general.ObjectUtilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.commons.lang3.StringUtils;

/**
 * A description of an executable file.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class ExecutableFile {

  public static final ExecutableFile[] EMPTY_ARRAY = new ExecutableFile[0];

  public static final List<ExecutableFile> EMPTY_LIST = Lists.newArrayList();

  /**
   * The path of the executable file.
   */
  private final String file;

  /**
   * The offset of the region to be highlighted.
   */
  private final String offset;

  /**
   * Constructor for {@link ExecutableFile}.
   */
  public ExecutableFile(String file, String offset) {
    this.file = file;
    this.offset = offset;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ExecutableFile) {
      ExecutableFile other = (ExecutableFile) obj;
      return
        ObjectUtilities.equals(other.file, file) &&
        ObjectUtilities.equals(other.offset, offset);
    }
    return false;
  }

  public static ExecutableFile fromJson(JsonObject jsonObject) {
    String file = jsonObject.get("file").getAsString();
    String offset = jsonObject.get("offset").getAsString();
    return new ExecutableFile(file, offset);
  }

  public static List<ExecutableFile> fromJsonArray(JsonArray jsonArray) {
    if (jsonArray == null) {
      return EMPTY_LIST;
    }
    ArrayList<ExecutableFile> list = new ArrayList<ExecutableFile>(jsonArray.size());
    Iterator<JsonElement> iterator = jsonArray.iterator();
    while (iterator.hasNext()) {
      list.add(fromJson(iterator.next().getAsJsonObject()));
    }
    return list;
  }

  /**
   * The path of the executable file.
   */
  public String getFile() {
    return file;
  }

  /**
   * The offset of the region to be highlighted.
   */
  public String getOffset() {
    return offset;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("file", file);
    jsonObject.addProperty("offset", offset);
    return jsonObject;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("file=");
    builder.append(file + ", ");
    builder.append("offset=");
    builder.append(offset);
    builder.append("]");
    return builder.toString();
  }

}
