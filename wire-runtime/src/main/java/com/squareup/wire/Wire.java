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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encode and decode Wire protocol buffers.
 */
public final class Wire {
  private final ThreadLocal<List<DeferredAdapter<?>>> reentrantCalls = new ThreadLocal<>();
  private final LinkedHashMap<Class<? extends Message<?>>,
      RuntimeMessageAdapter<? extends Message<?>, ? extends Message.Builder<?, ?>>> messageAdapters
      = new LinkedHashMap<>();
  private final Map<Class<? extends ProtoEnum>, RuntimeEnumAdapter<? extends ProtoEnum>>
      enumAdapters = new LinkedHashMap<>();
  private final ExtensionRegistry registry;

  public Wire() {
    this(null);
  }

  public Wire(ExtensionRegistry registry) {
    this.registry = registry;
  }

  /** Returns an adapter for reading and writing {@code type}, creating it if necessary. */
  public <M extends Message> WireAdapter<M> adapter(Class<M> type) {
    List<DeferredAdapter<?>> deferredAdapters = reentrantCalls.get();
    if (deferredAdapters == null) {
      deferredAdapters = new ArrayList<>();
      reentrantCalls.set(deferredAdapters);
    } else {
      // Check that this isn't a reentrant call.
      for (DeferredAdapter<?> deferredAdapter : deferredAdapters) {
        if (deferredAdapter.javaType.equals(type)) {
          //noinspection unchecked
          return (WireAdapter<M>) deferredAdapter;
        }
      }
    }

    DeferredAdapter<M> deferredAdapter = new DeferredAdapter<>(type);
    deferredAdapters.add(deferredAdapter);
    try {
      WireAdapter<M> adapter = messageAdapter(type);
      deferredAdapter.ready(adapter);
      return adapter;
    } finally {
      deferredAdapters.remove(deferredAdapters.size() - 1);
    }
  }

  /**
   * Returns a message adapter for {@code messageType}.
   */
  @SuppressWarnings("unchecked")
  synchronized <M extends Message<M>, B extends Message.Builder<M, B>> RuntimeMessageAdapter<M, B>
  messageAdapter(Class<M> messageType) {
    RuntimeMessageAdapter<M, B> adapter = (RuntimeMessageAdapter<M, B>) messageAdapters.get(
        messageType);
    if (adapter == null) {
      adapter = RuntimeMessageAdapter.create(this, messageType);
      if (registry != null) {
        adapter = adapter.withExtensions(registry);
      }
      messageAdapters.put(messageType, adapter);
    }
    return adapter;
  }

  /**
   * Returns an enum adapter for {@code enumClass}.
   */
  @SuppressWarnings("unchecked")
  synchronized <E extends ProtoEnum> RuntimeEnumAdapter<E> enumAdapter(Class<E> enumClass) {
    RuntimeEnumAdapter<E> adapter = (RuntimeEnumAdapter<E>) enumAdapters.get(enumClass);
    if (adapter == null) {
      adapter = new RuntimeEnumAdapter<>(enumClass);
      enumAdapters.put(enumClass, adapter);
    }
    return adapter;
  }

  /**
   * Returns {@code value} if it is not null; {@code defaultValue} otherwise.
   * This is used to conveniently return a default value when a value is null.
   * For example,
   *
   * <pre>
   * MyProto myProto = ...
   * MyField field = Wire.get(myProto.f, MyProto.f_default);
   * </pre>
   *
   * will attempt to retrieve the value of the field 'f' defined by MyProto.
   * If the field is null (i.e., unset), <code>get</code> will return its
   * second argument, which in this case is the default value for the field
   * 'f'.
   */
  public static <T> T get(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }

  /**
   * Sometimes a type adapter factory depends on its own product; either directly or indirectly.
   * To make this work, we offer this type adapter stub while the final adapter is being computed.
   * When it is ready, we wire this to delegate to that finished adapter.
   *
   * <p>Typically this is necessary in self-referential object models, such as an {@code Employee}
   * class that has a {@code List<Employee>} field for an organization's management hierarchy.
   */
  private static class DeferredAdapter<M extends Message> extends WireAdapter<M> {
    private WireAdapter<M> delegate;

    DeferredAdapter(Class<M> type) {
      super(FieldEncoding.LENGTH_DELIMITED, type);
    }

    public void ready(WireAdapter<M> delegate) {
      this.delegate = delegate;
    }

    @Override public M redact(M message) {
      if (delegate == null) throw new IllegalStateException("Type adapter isn't ready");
      return delegate.redact(message);
    }

    @Override public String toString(M value) {
      if (delegate == null) throw new IllegalStateException("Type adapter isn't ready");
      return delegate.toString(value);
    }

    @Override public int encodedSize(M value) {
      if (delegate == null) throw new IllegalStateException("Type adapter isn't ready");
      return delegate.encodedSize(value);
    }

    @Override public void encode(ProtoWriter writer, M value) throws IOException {
      if (delegate == null) throw new IllegalStateException("Type adapter isn't ready");
      delegate.encode(writer, value);
    }

    @Override public M decode(ProtoReader reader) throws IOException {
      if (delegate == null) throw new IllegalStateException("Type adapter isn't ready");
      return delegate.decode(reader);
    }
  }
}
