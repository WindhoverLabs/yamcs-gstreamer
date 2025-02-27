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
  public static String serializeElement(final Element element) {
    final StringBuilder sb = new StringBuilder();
    // Append the element's name as the header.
    sb.append("Element: ").append(element.getName()).append("\n");

    // Loop through each property name available in the element.
    for (final String propertyName : element.listPropertyNames()) {
      try {
        final Object propertyValue = element.get(propertyName);
        sb.append(propertyName).append(": ").append(propertyValue).append("\n");
      } catch (Exception e) {
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
  public static String readPropertyByPath(final Pipeline pipeline, final String path) {
    final String[] parts = path.split("/");
    if (parts.length < 2) {
      return "Invalid path: path must be in the format 'elementname/propertyname'.";
    }

    final String elementName = parts[0];
    final String propertyPath =
        path.substring(elementName.length() + 1); // Remove element name and "/"
    final Element element = pipeline.getElementByName(elementName);
    if (element == null) {
      return "Element not found: " + elementName;
    }

    final String[] propertyParts = propertyPath.split("/");
    if (propertyParts.length == 0) {
      return "Invalid property path: path is empty.";
    }

    Object current = element;
    for (final String part : propertyParts) {
      if (current == null) {
        return "Path not found: " + path;
      }

      if (current instanceof Element) {
        final Element el = (Element) current;
        try {
          current = el.get(part);
        } catch (Exception e) {
          return "Property not found: " + part;
        }
      } else if (current instanceof Structure) {
        final Structure structure = (Structure) current;
        current = structure.getValue(part);
      } else {
        return "Path not found: " + part + " is not a structure or element.";
      }
    }

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
  public static String writePropertyByPath(
      final Pipeline pipeline, final String path, final String value) {
    final String[] parts = path.split("/");
    if (parts.length < 2) {
      return "Invalid path: path must be in the format 'elementname/propertyname'.";
    }

    final String elementName = parts[0];
    final String propertyPath = path.substring(elementName.length() + 1);
    final Element element = pipeline.getElementByName(elementName);
    if (element == null) {
      return "Element not found: " + elementName;
    }

    final String[] propertyParts = propertyPath.split("/");
    if (propertyParts.length == 0) {
      return "Invalid property path: path is empty.";
    }

    Object current = element;
    for (final String part : propertyParts) {
      if (current == null) {
        return "Path not found: " + path;
      }
      if (current instanceof Element) {
        final Element el = (Element) current;
        try {
          // Attempt to write the property value.
          writeProperty(value, el, part);
        } catch (Exception e) {
          return "Property not found: " + part;
        }
      } else {
        return "Path not found: " + part + " is not a structure or element.";
      }
    }

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
  public static boolean writeProperty(
      final String value, final Element element, final String propertyName) {
    try {
      final Object currentValue = element.get(propertyName);
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
        element.set(propertyName, value.charAt(0));
      } else {
        logger.warn("Unsupported property type: {}", currentValue.getClass().getSimpleName());
        return false;
      }
      return true;
    } catch (Exception e) {
      logger.error("Failed to set property: {}", e.getMessage());
      return false;
    }
  }
}
