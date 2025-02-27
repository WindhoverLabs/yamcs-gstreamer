/****************************************************************************
 *
 *   Copyright (c) 2025 Windhover Labs, L.L.C. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name Windhover Labs nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *****************************************************************************/

package com.windhoverlabs.yamcs.media.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents documentation for a GStreamer property.
 *
 * <p>This class encapsulates information about a GStreamer property's details, including its name,
 * type, access permissions, and any supported enumerated values.
 *
 * <p>Consider making this class immutable if its properties are not intended to change after
 * creation. If immutability is desired, remove the setters and mark the fields as final.
 */
public class PropertyDocs {
  // The name of the property.
  private String name;
  // The data type of the property.
  private String type;
  // Flag indicating whether the property is readable.
  private boolean readable;
  // Flag indicating whether the property is writable.
  private boolean writable;
  // List of possible enumeration values for the property (if applicable).
  private List<String> enumValues;

  /**
   * Constructs a new {@code PropertyDocs} instance with the specified details.
   *
   * @param name the name of the property
   * @param type the data type of the property
   * @param readable {@code true} if the property is readable; {@code false} otherwise
   * @param writable {@code true} if the property is writable; {@code false} otherwise
   * @param enumValues a list of enumerated values for the property, or {@code null} if none exist
   */
  public PropertyDocs(
      String name, String type, boolean readable, boolean writable, List<String> enumValues) {
    this.name = name;
    this.type = type;
    this.readable = readable;
    this.writable = writable;
    this.enumValues = enumValues;
  }

  /** No-argument constructor for serialization frameworks. */
  public PropertyDocs() {}

  /**
   * Returns the name of the property.
   *
   * @return the property name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the property.
   *
   * @param name the property name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the data type of the property.
   *
   * @return the property type as a string
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the data type of the property.
   *
   * @param type the property type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Checks whether the property is readable.
   *
   * @return {@code true} if the property is readable; {@code false} otherwise
   */
  public boolean isReadable() {
    return readable;
  }

  /**
   * Sets the readability of the property.
   *
   * @param readable {@code true} if the property should be marked as readable; {@code false}
   *     otherwise
   */
  public void setReadable(boolean readable) {
    this.readable = readable;
  }

  /**
   * Checks whether the property is writable.
   *
   * @return {@code true} if the property is writable; {@code false} otherwise
   */
  public boolean isWritable() {
    return writable;
  }

  /**
   * Sets the writability of the property.
   *
   * @param writable {@code true} if the property should be marked as writable; {@code false}
   *     otherwise
   */
  public void setWritable(boolean writable) {
    this.writable = writable;
  }

  /**
   * Returns the list of enumeration values for the property.
   *
   * @return a list of enumeration values, or {@code null} if none exist
   */
  public List<String> getEnumValues() {
    return enumValues;
  }

  /**
   * Sets the list of enumeration values for the property.
   *
   * @param enumValues a list of enumeration values to set
   */
  public void setEnumValues(List<String> enumValues) {
    this.enumValues = enumValues;
  }

  @Override
  public String toString() {
    return "PropertyDocs{"
        + "name='"
        + name
        + '\''
        + ", type='"
        + type
        + '\''
        + ", readable="
        + readable
        + ", writable="
        + writable
        + ", enumValues="
        + enumValues
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PropertyDocs)) return false;
    PropertyDocs that = (PropertyDocs) o;
    return readable == that.readable
        && writable == that.writable
        && Objects.equals(name, that.name)
        && Objects.equals(type, that.type)
        && Objects.equals(enumValues, that.enumValues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, readable, writable, enumValues);
  }
}
