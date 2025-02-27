package com.windhoverlabs.yamcs.media.model;

import java.util.List;

/**
 * Represents documentation for a GStreamer element.
 *
 * <p>This class encapsulates details about a GStreamer element, including its short name, long
 * descriptive name, a textual description, and a list of property documentation objects that
 * describe its properties.
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
}
