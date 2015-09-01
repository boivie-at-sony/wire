/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema;

import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okio.Okio;
import okio.Source;

/** Load proto files and their transitive dependencies, parse them, and link them together. */
public final class Loader {
  private final List<Path> dirs;
  private final Map<Path, ProtoFile> loaded = new LinkedHashMap<>();

  public Loader(List<Path> dirs) {
    this.dirs = dirs;
  }

  /** Returns a loader that loads all files from {@code paths}. */
  public static Loader forSearchPaths(final List<Path> paths) {
    return new Loader(paths);
  }

  public Schema load(Iterable<Path> sourceFiles) throws IOException {
    for (Path path : sourceFiles) {
      load(path);
    }
    return new Linker(loaded.values()).link();
  }

  private void load(Path path) throws IOException {
    if (loaded.containsKey(path)) {
      return;
    }

    ProtoFileElement element = null;
    for (Path dir : dirs) {
      if (path.isAbsolute() && !path.startsWith(dir)) {
        continue;
      }
      Path resolvedPath = dir.resolve(path);
      if (Files.exists(resolvedPath)) {
        Location location = Location.get(dir.toString(), path.toString());
        try (Source source = Okio.source(resolvedPath)) {
          String data = Okio.buffer(source).readUtf8();
          element = ProtoParser.parse(location, data);
        } catch (IOException e) {
          throw new IOException("Failed to load " + path, e);
        }
        break;
      }
    }
    if (element == null) {
      throw new FileNotFoundException("Failed to locate " + path + " in " + dirs);
    }

    ProtoFile protoFile = ProtoFile.get(element);
    loaded.put(path, protoFile);

    // Recursively load dependencies.
    FileSystem fs = path.getFileSystem();
    for (String importPath : element.imports()) {
      load(fs.getPath(importPath));
    }
  }
}
