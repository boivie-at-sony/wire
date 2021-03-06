// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/person.proto at 21:1
package com.squareup.wire.protos.person;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoEnum;
import com.squareup.wire.ProtoField;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;

public final class Person extends Message<Person> {
  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_NAME = "";

  public static final Integer DEFAULT_ID = 0;

  public static final String DEFAULT_EMAIL = "";

  /**
   * The customer's full name.
   */
  @ProtoField(
      tag = 1,
      type = "string",
      label = Message.Label.REQUIRED
  )
  public final String name;

  /**
   * The customer's ID number.
   */
  @ProtoField(
      tag = 2,
      type = "int32",
      label = Message.Label.REQUIRED
  )
  public final Integer id;

  /**
   * Email address for the customer.
   */
  @ProtoField(
      tag = 3,
      type = "string"
  )
  public final String email;

  /**
   * A list of the customer's phone numbers.
   */
  @ProtoField(
      tag = 4,
      type = "squareup.protos.person.Person.PhoneNumber",
      label = Message.Label.REPEATED
  )
  public final List<PhoneNumber> phone;

  public Person(String name, Integer id, String email, List<PhoneNumber> phone) {
    this.name = name;
    this.id = id;
    this.email = email;
    this.phone = immutableCopyOf(phone);
  }

  private Person(Builder builder) {
    this(builder.name, builder.id, builder.email, builder.phone);
    setBuilder(builder);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Person)) return false;
    Person o = (Person) other;
    return equals(name, o.name)
        && equals(id, o.id)
        && equals(email, o.email)
        && equals(phone, o.phone);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    if (result == 0) {
      result = name != null ? name.hashCode() : 0;
      result = result * 37 + (id != null ? id.hashCode() : 0);
      result = result * 37 + (email != null ? email.hashCode() : 0);
      result = result * 37 + (phone != null ? phone.hashCode() : 1);
      hashCode = result;
    }
    return result;
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<Person, Builder> {
    public String name;

    public Integer id;

    public String email;

    public List<PhoneNumber> phone = Collections.emptyList();

    public Builder() {
    }

    public Builder(Person message) {
      super(message);
      if (message == null) return;
      this.name = message.name;
      this.id = message.id;
      this.email = message.email;
      this.phone = copyOf(message.phone);
    }

    /**
     * The customer's full name.
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * The customer's ID number.
     */
    public Builder id(Integer id) {
      this.id = id;
      return this;
    }

    /**
     * Email address for the customer.
     */
    public Builder email(String email) {
      this.email = email;
      return this;
    }

    /**
     * A list of the customer's phone numbers.
     */
    public Builder phone(List<PhoneNumber> phone) {
      this.phone = canonicalizeList(phone);
      return this;
    }

    @Override
    public Person build() {
      if (name == null
          || id == null) {
        throw missingRequiredFields(name, "name",
            id, "id");
      }
      return new Person(this);
    }
  }

  public enum PhoneType implements ProtoEnum {
    MOBILE(0),

    HOME(1),

    /**
     * Could be phone or fax.
     */
    WORK(2);

    private final int value;

    PhoneType(int value) {
      this.value = value;
    }

    @Override
    public int getValue() {
      return value;
    }
  }

  public static final class PhoneNumber extends Message<PhoneNumber> {
    private static final long serialVersionUID = 0L;

    public static final String DEFAULT_NUMBER = "";

    public static final PhoneType DEFAULT_TYPE = PhoneType.HOME;

    /**
     * The customer's phone number.
     */
    @ProtoField(
        tag = 1,
        type = "string",
        label = Message.Label.REQUIRED
    )
    public final String number;

    /**
     * The type of phone stored here.
     */
    @ProtoField(
        tag = 2,
        type = "squareup.protos.person.Person.PhoneType"
    )
    public final PhoneType type;

    public PhoneNumber(String number, PhoneType type) {
      this.number = number;
      this.type = type;
    }

    private PhoneNumber(Builder builder) {
      this(builder.number, builder.type);
      setBuilder(builder);
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) return true;
      if (!(other instanceof PhoneNumber)) return false;
      PhoneNumber o = (PhoneNumber) other;
      return equals(number, o.number)
          && equals(type, o.type);
    }

    @Override
    public int hashCode() {
      int result = hashCode;
      if (result == 0) {
        result = number != null ? number.hashCode() : 0;
        result = result * 37 + (type != null ? type.hashCode() : 0);
        hashCode = result;
      }
      return result;
    }

    public static final class Builder extends com.squareup.wire.Message.Builder<PhoneNumber, Builder> {
      public String number;

      public PhoneType type;

      public Builder() {
      }

      public Builder(PhoneNumber message) {
        super(message);
        if (message == null) return;
        this.number = message.number;
        this.type = message.type;
      }

      /**
       * The customer's phone number.
       */
      public Builder number(String number) {
        this.number = number;
        return this;
      }

      /**
       * The type of phone stored here.
       */
      public Builder type(PhoneType type) {
        this.type = type;
        return this;
      }

      @Override
      public PhoneNumber build() {
        if (number == null) {
          throw missingRequiredFields(number, "number");
        }
        return new PhoneNumber(this);
      }
    }
  }
}
