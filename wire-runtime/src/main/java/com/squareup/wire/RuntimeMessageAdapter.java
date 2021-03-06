/*
 * Copyright 2013 Square Inc.
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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.squareup.wire.Message.Builder;

final class RuntimeMessageAdapter<M extends Message<M>, B extends Builder<M, B>>
    extends WireAdapter<M> {
  static <M extends Message<M>, B extends Builder<M, B>> RuntimeMessageAdapter<M, B> create(
      Wire wire, Class<M> messageType) {
    Class<B> builderType = getBuilderType(messageType);
    Constructor<B> builderCopyConstructor = getBuilderCopyConstructor(builderType, messageType);
    Map<Integer, FieldBinding<M, B>> fieldBindings = new LinkedHashMap<>();

    // Create tag bindings for fields annotated with '@ProtoField'
    for (Field messageField : messageType.getDeclaredFields()) {
      ProtoField protoField = messageField.getAnnotation(ProtoField.class);
      if (protoField != null) {
        WireAdapter<?> singleAdapter = singleAdapter(wire, messageField, protoField);
        fieldBindings.put(protoField.tag(),
            new FieldBinding<>(protoField, singleAdapter, messageField, builderType));
      }
    }

    Map<Integer, RegisteredExtension> extensions = Collections.emptyMap();
    return new RuntimeMessageAdapter<>(wire, messageType, builderType, builderCopyConstructor,
        Collections.unmodifiableMap(fieldBindings), extensions);
  }

  @SuppressWarnings("unchecked")
  private static WireAdapter<?> singleAdapter(
      Wire wire, Field messageField, ProtoField protoField) {
    Class<?> singleType = messageField.getType();
    if (List.class.isAssignableFrom(singleType)) {
      ParameterizedType listType = (ParameterizedType) messageField.getGenericType();
      singleType = (Class<?>) listType.getActualTypeArguments()[0];
    }

    if (Message.class.isAssignableFrom(singleType)) {
      return wire.adapter((Class<? extends Message>) singleType);
    } else if (ProtoEnum.class.isAssignableFrom(singleType)) {
      return wire.enumAdapter((Class<? extends ProtoEnum>) singleType);
    } else {
      return WireAdapter.get(wire, WireType.get(protoField.type()), null, null);
    }
  }

  private final Wire wire;
  private final Class<M> messageType;
  private final Class<B> builderType;
  private final Constructor<B> builderCopyConstructor;
  private final Map<Integer, FieldBinding<M, B>> fieldBindings;
  private final Map<Integer, RegisteredExtension> extensions;

  RuntimeMessageAdapter(Wire wire, Class<M> messageType, Class<B> builderType,
      Constructor<B> builderCopyConstructor, Map<Integer, FieldBinding<M, B>> fieldBindings,
      Map<Integer, RegisteredExtension> extensions) {
    super(FieldEncoding.LENGTH_DELIMITED, messageType);
    this.wire = wire;
    this.messageType = messageType;
    this.builderType = builderType;
    this.builderCopyConstructor = builderCopyConstructor;
    this.fieldBindings = fieldBindings;
    this.extensions = extensions;
  }

  @Override public RuntimeMessageAdapter<M, B> withExtensions(ExtensionRegistry extensionRegistry) {
    Map<Integer, RegisteredExtension> extensions = new LinkedHashMap<>(this.extensions);

    for (Extension<?, ?> extension : extensionRegistry.extensions(messageType)) {
      WireAdapter<?> singleAdapter = WireAdapter.get(wire, extension.getType(),
          extension.getMessageType(), extension.getEnumType());
      extensions.put(extension.getTag(), new RegisteredExtension(extension, singleAdapter));
    }

    return new RuntimeMessageAdapter<>(wire, messageType, builderType, builderCopyConstructor,
        fieldBindings, Collections.unmodifiableMap(extensions));
  }

  Map<Integer, FieldBinding<M, B>> fieldBindings() {
    return fieldBindings;
  }

  Map<Integer, RegisteredExtension> extensions() {
    return extensions;
  }

  B newBuilder() {
    try {
      return builderType.newInstance();
    } catch (IllegalAccessException | InstantiationException e) {
      throw new AssertionError(e);
    }
  }

  B newBuilder(M value) {
    try {
      return builderCopyConstructor.newInstance(value);
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <M extends Message<M>, B extends Builder<M, B>> Class<B> getBuilderType(
      Class<M> messageType) {
    try {
      return (Class<B>) Class.forName(messageType.getName() + "$Builder");
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("No builder class found for message type "
          + messageType.getName());
    }
  }

  private static <M extends Message<M>, B extends Builder<M, B>> Constructor<B>
  getBuilderCopyConstructor(Class<B> builderType, Class<M> messageType) {
    try {
      return builderType.getConstructor(messageType);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  // Writing

  @Override public int encodedSize(M message) {
    int cachedSerializedSize = message.cachedSerializedSize;
    if (cachedSerializedSize != 0) return cachedSerializedSize;

    int size = 0;
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      Object value = fieldBinding.get(message);
      if (value == null) continue;
      size += ((WireAdapter<Object>) fieldBinding.adapter).encodedSize(fieldBinding.tag, value);
    }

    size += message.tagMapEncodedSize();
    message.cachedSerializedSize = size;
    return size;
  }

  @Override public void encode(ProtoWriter writer, M message) throws IOException {
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      Object value = fieldBinding.get(message);
      if (value == null) continue;
      ((WireAdapter<Object>) fieldBinding.adapter).encodeTagged(writer, fieldBinding.tag, value);
    }
    if (message.tagMap != null) {
      message.tagMap.encode(writer);
    }
  }

  @Override public M redact(M message) {
    B builder = newBuilder(message);
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      if (!fieldBinding.redacted && (fieldBinding.type.isScalar()
          || fieldBinding.getFromBuilder(builder) instanceof ProtoEnum)) {
        continue;
      }
      if (fieldBinding.redacted && fieldBinding.label == Message.Label.REQUIRED) {
        throw new IllegalArgumentException(String.format(
            "Field %s.%s is REQUIRED and cannot be redacted.",
            javaType.getName(), fieldBinding.name));
      }
      Object builderValue = fieldBinding.getFromBuilder(builder);
      if (builderValue != null) {
        Object redactedValue = ((WireAdapter<Object>) fieldBinding.adapter).redact(builderValue);
        fieldBinding.set(builder, redactedValue);
      }
    }
    builder.tagMap = builder.tagMap != null
        ? builder.tagMap.redact()
        : null;
    return builder.build();
  }

  @Override public boolean equals(Object o) {
    return o instanceof RuntimeMessageAdapter
        && ((RuntimeMessageAdapter) o).messageType == messageType;
  }

  @Override public int hashCode() {
    return messageType.hashCode();
  }

  /**
   * Returns a human-readable version of the given {@link Message}.
   */
  public String toString(M message) {
    StringBuilder sb = new StringBuilder();
    sb.append(messageType.getSimpleName());
    sb.append('{');
    boolean seenValue = false;
    for (FieldBinding<M, B> fieldBinding : fieldBindings.values()) {
      Object value = fieldBinding.get(message);
      if (value == null) continue;
      if (seenValue) sb.append(", ");
      sb.append(fieldBinding.name)
          .append('=')
          .append(fieldBinding.redacted ? "██" : value);
      seenValue = true;
    }
    if (message.tagMap != null) {
      for (Extension<?, ?> extension : message.tagMap.extensions(true)) {
        if (seenValue) sb.append(", ");
        if (!extension.isUnknown()) {
          sb.append(extension.getName());
        } else {
          sb.append(extension.getTag());
        }
        sb.append('=').append(message.tagMap.get(extension));
        seenValue = true;
      }
    }
    sb.append('}');
    return sb.toString();
  }

  // Reading

  @Override public M decode(ProtoReader reader) throws IOException {
    B builder = newBuilder();
    long token = reader.beginMessage();
    for (int tag; (tag = reader.nextTag()) != -1;) {
      FieldBinding<M, B> fieldBinding = fieldBindings.get(tag);
      if (fieldBinding != null) {
        try {
          Object value = fieldBinding.singleAdapter.decode(reader);
          fieldBinding.value(builder, value);
        } catch (RuntimeEnumAdapter.EnumConstantNotFoundException e) {
          // An unknown Enum value was encountered, store it as an unknown field
          builder.addVarint(tag, e.value);
        }
        continue;
      }

      RegisteredExtension registeredExtension = extensions.get(tag);
      if (registeredExtension != null) {
        try {
          Object value = registeredExtension.adapter.decode(reader);
          builder.ensureTagMap().add(registeredExtension.extension, value);
        } catch (RuntimeEnumAdapter.EnumConstantNotFoundException e) {
          // An unknown Enum value was encountered, store it as an unknown field
          builder.addVarint(tag, e.value);
        }
        continue;
      }

      FieldEncoding fieldEncoding = reader.peekFieldEncoding();
      Object value = fieldEncoding.rawWireAdapter().decode(reader);
      builder.ensureTagMap().add(tag, fieldEncoding, value);
    }
    reader.endMessage(token);

    return builder.build();
  }
}
