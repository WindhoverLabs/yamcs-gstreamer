package com.windhoverlabs.yamcs.media.utils;

import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for working with GStreamer elements and properties.
 *
 * <p>This class provides methods to serialize an element's properties to a string, as well as to
 * read and write properties using a path-like string to identify the property.
 */
public class GStreamerUtils {
  // Logger for logging warnings and errors.
  private static final Logger logger = LoggerFactory.getLogger(GStreamerUtils.class);

  /**
   * Serializes a GStreamer element's properties to a formatted string.
   *
   * <p>This method iterates over all property names of the provided element, attempts to retrieve
   * their values, and builds a string representation of the element and its properties. If an error
   * occurs while retrieving a property's value, an error message is appended instead.
   *
   * @param element the GStreamer element to serialize
   * @return a string representation of the element's properties
   */
  public static String serializeElement(Element element) {
    StringBuilder sb = new StringBuilder();
    // Append the element's name as the header.
    sb.append("Element: ").append(element.getName()).append("\n");

    // Loop through each property name available in the element.
    for (String propertyName : element.listPropertyNames()) {
      try {
        // Retrieve the property value.
        Object propertyValue = element.get(propertyName);
        sb.append(propertyName).append(": ").append(propertyValue).append("\n");
      } catch (Exception e) {
        // In case of an exception, indicate that there was an error retrieving the value.
        sb.append(propertyName).append(": ").append("Error retrieving value").append("\n");
      }
    }
    return sb.toString();
  }

  /**
   * Reads a property value from a GStreamer element using a path-like string.
   *
   * <p>The path should be in the format "elementname/propertyname[/subproperty...]" where the first
   * part identifies the element by name (within the pipeline) and the remaining parts indicate the
   * nested property structure to traverse.
   *
   * @param pipeline the GStreamer pipeline containing the element
   * @param path the path to the property (e.g., "elementname/propertyname")
   * @return the string representation of the property value, or an error message if not found
   */
  public static String readPropertyByPath(Pipeline pipeline, String path) {
    // Split the path into segments using "/" as the delimiter.
    String[] parts = path.split("/");
    if (parts.length < 2) {
      return "Invalid path: path must be in the format 'elementname/propertyname'.";
    }

    // Extract the element name and the remaining property path.
    String elementName = parts[0];
    String propertyPath =
        path.substring(elementName.length() + 1); // Remove the element name and the following "/"

    // Retrieve the element from the pipeline by its name.
    Element element = pipeline.getElementByName(elementName);
    if (element == null) {
      return "Element not found: " + elementName;
    }

    // Split the property path to handle nested properties.
    String[] propertyParts = propertyPath.split("/");
    if (propertyParts.length == 0) {
      return "Invalid property path: path is empty.";
    }

    // Start traversing from the element.
    Object current = element;

    // Iterate over each part of the property path.
    for (String part : propertyParts) {
      // If at any point current becomes null, the full path is not valid.
      if (current == null) {
        return "Path not found: " + path;
      }

      // If the current object is an Element, try to get its property.
      if (current instanceof Element) {
        Element el = (Element) current;
        try {
          current = el.get(part);
        } catch (Exception e) {
          // If property retrieval fails, return an error.
          return "Property not found: " + part;
        }
      }
      // If the current object is a Structure, try to get the value associated with the field.
      else if (current instanceof Structure) {
        Structure structure = (Structure) current;
        current = structure.getValue(part);
      }
      // If the current object is neither an Element nor a Structure, the path is invalid.
      else {
        return "Path not found: " + part + " is not a structure or element.";
      }
    }

    // Return the string representation of the final property value.
    return current != null ? current.toString() : "null";
  }

  /**
   * Writes a property value to a GStreamer element using a path-like string.
   *
   * <p>The path should be in the format "elementname/propertyname[/subproperty...]" where the first
   * part identifies the element by name (within the pipeline) and the remaining parts indicate the
   * nested property structure to traverse. Currently, only direct properties of an element are
   * supported for writing.
   *
   * @param pipeline the GStreamer pipeline containing the element
   * @param path the path to the property (e.g., "elementname/propertyname")
   * @param value the value to set for the property
   * @return the string representation of the final value, or an error message if the operation
   *     fails
   */
  public static String writePropertyByPath(Pipeline pipeline, String path, String value) {
    // Split the path into segments using "/" as the delimiter.
    String[] parts = path.split("/");
    if (parts.length < 2) {
      return "Invalid path: path must be in the format 'elementname/propertyname'.";
    }

    // Extract the element name and the remaining property path.
    String elementName = parts[0];
    String propertyPath =
        path.substring(elementName.length() + 1); // Remaining path after the element name

    // Retrieve the element from the pipeline by its name.
    Element element = pipeline.getElementByName(elementName);
    if (element == null) {
      return "Element not found: " + elementName;
    }

    // Split the property path to handle nested properties.
    String[] propertyParts = propertyPath.split("/");
    if (propertyParts.length == 0) {
      return "Invalid property path: path is empty.";
    }

    // Start traversing from the element.
    Object current = element;

    // Iterate over each part of the property path.
    for (String part : propertyParts) {
      if (current == null) {
        return "Path not found: " + path;
      }

      // Only Elements are currently supported for writing properties.
      if (current instanceof Element) {
        Element el = (Element) current;
        try {
          // Attempt to write the property value.
          writeProperty(value, el, part);
        } catch (Exception e) {
          // If property writing fails, return an error.
          return "Property not found: " + part;
        }
      } else {
        // If the current object is not an Element, the path is invalid.
        return "Path not found: " + part + " is not a structure or element.";
      }
    }

    // Return the string representation of the current value.
    return current != null ? current.toString() : "null";
  }

  /**
   * Writes a property to a GStreamer element.
   *
   * <p>This method determines the type of the current value of the specified property and attempts
   * to convert the provided string value to the appropriate type. Supported types include {@code
   * Integer}, {@code Long}, {@code Boolean}, {@code Float}, {@code Double}, {@code String}, and
   * {@code Character}.
   *
   * @param value the value to set, as a string
   * @param element the GStreamer element on which the property will be set
   * @param propertyName the name of the property to be set
   * @return {@code true} if the property was successfully set; {@code false} otherwise
   */
  public static boolean writeProperty(String value, Element element, String propertyName) {
    try {
      // Retrieve the current value of the property to determine its type.
      Object currentValue = element.get(propertyName);

      // Convert the provided string value to the appropriate type and set the property.
      if (currentValue instanceof Integer) {
        element.set(propertyName, Integer.parseInt(value));
      } else if (currentValue instanceof Long) {
        element.set(propertyName, Long.parseLong(value));
      } else if (currentValue instanceof Boolean) {
        element.set(propertyName, Boolean.parseBoolean(value));
      } else if (currentValue instanceof Float) {
        element.set(propertyName, Float.parseFloat(value));
      } else if (currentValue instanceof Double) {
        element.set(propertyName, Double.parseDouble(value));
      } else if (currentValue instanceof String) {
        element.set(propertyName, value);
      } else if (currentValue instanceof Character) {
        // Use the first character of the string as the property value.
        element.set(propertyName, value.charAt(0));
      } else {
        // Log a warning if the property type is unsupported.
        logger.warn("Unsupported property type: {}", currentValue.getClass().getSimpleName());
        return false;
      }
      return true;
    } catch (Exception e) {
      // Log the error if setting the property fails.
      logger.error("Failed to set property: {}", e.getMessage());
      return false;
    }
  }
}
