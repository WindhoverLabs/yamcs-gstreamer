package com.windhoverlabs.yamcs.media.model;

import java.util.List;

/**
 * Represents documentation for a GStreamer property.
 *
 * <p>This class encapsulates information about a GStreamer property's details, including its name,
 * type, access permissions, and any supported enumerated values.
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
}
