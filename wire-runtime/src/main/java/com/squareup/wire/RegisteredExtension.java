/*
 * Copyright 2015 Square Inc.
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
package com.squareup.wire;

/** An extension and its type adapter. */
final class RegisteredExtension {
  final Extension<?, ?> extension;
  final Message.Label label;
  final String name;
  final int tag;
  final WireAdapter<?> adapter;

  public RegisteredExtension(Extension<?, ?> extension, WireAdapter<?> singleAdapter) {
    this.label = extension.getLabel();
    this.name = extension.getName();
    this.tag = extension.getTag();
    this.adapter = singleAdapter;
    this.extension = extension;
  }
}
