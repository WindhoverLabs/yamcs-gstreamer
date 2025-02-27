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
 * Represents documentation for a GStreamer element.
 *
 * <p>This class encapsulates details about a GStreamer element, including its short name, long
 * descriptive name, a textual description, and a list of property documentation objects that
 * describe its properties.
 *
 * <p>If the element documentation is not expected to change after creation, consider making this
 * class immutable by removing setters and declaring fields as final.
 */
public class ElementDocs {
  // The short name of the element.
  private String name;
  // The long, descriptive name of the element.
  private String longName;
  // A description of the element.
  private String description;
  // A list of property documentation objects associated with the element.
  private List<PropertyDocs> properties;

  /** No-argument constructor for serialization frameworks. */
  public ElementDocs() {}

  /**
   * Constructs a new {@code ElementDocs} instance with the specified details.
   *
   * @param name the short name of the element
   * @param longName the long descriptive name of the element
   * @param description a description of the element
   * @param properties a list of {@link PropertyDocs} objects representing the element's properties
   */
  public ElementDocs(
      String name, String longName, String description, List<PropertyDocs> properties) {
    this.name = name;
    this.longName = longName;
    this.description = description;
    this.properties = properties;
  }

  /**
   * Returns the short name of the element.
   *
   * @return the element's short name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the short name of the element.
   *
   * @param name the element's short name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the long descriptive name of the element.
   *
   * @return the element's long name
   */
  public String getLongName() {
    return longName;
  }

  /**
   * Sets the long descriptive name of the element.
   *
   * @param longName the element's long name to set
   */
  public void setLongName(String longName) {
    this.longName = longName;
  }

  /**
   * Returns the description of the element.
   *
   * @return the element's description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of the element.
   *
   * @param description the element's description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the list of property documentation associated with the element.
   *
   * @return a list of {@link PropertyDocs} objects representing the element's properties
   */
  public List<PropertyDocs> getProperties() {
    return properties;
  }

  /**
   * Sets the list of property documentation associated with the element.
   *
   * @param properties a list of {@link PropertyDocs} objects to set
   */
  public void setProperties(List<PropertyDocs> properties) {
    this.properties = properties;
  }

  @Override
  public String toString() {
    return "ElementDocs{"
        + "name='"
        + name
        + '\''
        + ", longName='"
        + longName
        + '\''
        + ", description='"
        + description
        + '\''
        + ", properties="
        + properties
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ElementDocs)) return false;
    ElementDocs that = (ElementDocs) o;
    return Objects.equals(name, that.name)
        && Objects.equals(longName, that.longName)
        && Objects.equals(description, that.description)
        && Objects.equals(properties, that.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, longName, description, properties);
  }
}
