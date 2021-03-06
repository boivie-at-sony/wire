// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/samebasename/single_level.proto at 20:1
package com.squareup.wire.protos.single_level;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;

public final class Bar extends Message<Bar> {
  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_BAZ = 0;

  @ProtoField(
      tag = 1,
      type = "int32"
  )
  public final Integer baz;

  public Bar(Integer baz) {
    this.baz = baz;
  }

  private Bar(Builder builder) {
    this(builder.baz);
    setBuilder(builder);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Bar)) return false;
    return equals(baz, ((Bar) other).baz);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    return result != 0 ? result : (hashCode = baz != null ? baz.hashCode() : 0);
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<Bar, Builder> {
    public Integer baz;

    public Builder() {
    }

    public Builder(Bar message) {
      super(message);
      if (message == null) return;
      this.baz = message.baz;
    }

    public Builder baz(Integer baz) {
      this.baz = baz;
      return this;
    }

    @Override
    public Bar build() {
      return new Bar(this);
    }
  }
}
