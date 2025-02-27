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

package com.windhoverlabs.yamcs.media.actions;

import com.google.gson.JsonObject;
import com.windhoverlabs.yamcs.media.model.ElementDocs;
import com.windhoverlabs.yamcs.media.model.PropertyDocs;
import com.windhoverlabs.yamcs.media.utils.CustomRepresenter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Gst;
import org.slf4j.Logger;
import org.yamcs.Spec;
import org.yamcs.Spec.OptionType;
import org.yamcs.actions.ActionResult;
import org.yamcs.tctm.Link;
import org.yamcs.tctm.LinkAction;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * Action to dump all GStreamer element factories to a YAML file.
 *
 * <p>This action retrieves all available GStreamer element factories, extracts their metadata and
 * properties, serializes them into a list of {@link ElementDocs} objects, and writes the data to a
 * YAML file using SnakeYAML.
 */
public class DumpAllElementsToFile extends LinkAction {
  // Internal logger for logging messages and errors.
  private static Logger internalLogger;

  /**
   * Constructs a new DumpAllElementsToFile action.
   *
   * @param logger the Logger instance for logging output and errors
   */
  public DumpAllElementsToFile(Logger logger) {
    super("DumpAllElementsToFile", "Dump all element factories to file");
    internalLogger = logger;
  }

  /**
   * Returns the specification for this action.
   *
   * <p>This action requires a single option:
   *
   * <ul>
   *   <li>"file" - The file path where the YAML output will be written. Defaults to
   *       "elements.yaml".
   * </ul>
   *
   * @return the action specification with required options
   */
  @Override
  public Spec getSpec() {
    Spec rootSpec = new Spec();
    // Define a required string option "file" with a default value.
    rootSpec.addOption("file", OptionType.STRING).withRequired(true).withDefault("elements.yaml");
    return rootSpec;
  }

  /**
   * Executes the DumpAllElementsToFile action.
   *
   * <p>This method performs the following steps:
   *
   * <ol>
   *   <li>Retrieves the output file name from the JSON request.
   *   <li>Initializes GStreamer if it is not already initialized.
   *   <li>Retrieves a list of all element factories.
   *   <li>Configures a YAML dumper with a custom representer for {@link ElementDocs} and {@link
   *       PropertyDocs}.
   *   <li>Iterates over each element factory, creates an instance of the element, and extracts its
   *       metadata and properties.
   *   <li>Serializes the collected data to the specified YAML file.
   * </ol>
   *
   * @param link the link on which the action is executed (unused in this implementation)
   * @param request the JSON object containing the "file" parameter
   * @param result the ActionResult to be completed after execution
   */
  @Override
  public void execute(Link link, JsonObject request, ActionResult result) {
    try {
      // Extract the file name from the JSON request.
      String fileName = request.get("file").getAsString();

      // Initialize GStreamer if it has not been initialized yet.
      if (!Gst.isInitialized()) {
        Gst.init("GStreamerLink", new String[] {});
      }

      // Retrieve all available element factories.
      List<ElementFactory> elements =
          ElementFactory.listGetElements(ElementFactory.ListType.ANY, ElementFactory.Rank.NONE);

      // Configure YAML dumper options.
      DumperOptions options = new DumperOptions();
      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

      // Set up a custom representer for YAML serialization.
      CustomRepresenter representer = new CustomRepresenter(options);
      representer.addClassTag(ElementDocs.class, Tag.MAP);
      representer.addClassTag(PropertyDocs.class, Tag.MAP);

      // Create a Yaml instance with the custom representer and options.
      Yaml yaml = new Yaml(representer, options);

      // Build documentation for each element factory.
      List<ElementDocs> elementDocsList = new ArrayList<>();
      for (ElementFactory elementFactory : elements) {
        ElementDocs docs = createElementDocs(elementFactory);
        elementDocsList.add(docs);
      }

      // Write the collected element documentation to the YAML file.
      try (FileWriter writer = new FileWriter(fileName)) {
        yaml.dump(elementDocsList, writer);
      } catch (IOException e) {
        internalLogger.error("Failed to write YAML file", e);
        result.completeExceptionally(e);
        return;
      }

      result.complete();
    } catch (Exception e) {
      result.completeExceptionally(e);
    }
  }

  /**
   * Creates an ElementDocs object by extracting metadata and properties from the given element
   * factory.
   *
   * @param elementFactory the element factory to process
   * @return an ElementDocs object containing the element's metadata and properties
   */
  private ElementDocs createElementDocs(ElementFactory elementFactory) {
    String name = elementFactory.getName();
    String longName = elementFactory.getLongName();
    String description = elementFactory.getDescription();

    // Create an instance of the element to access its properties. A unique instance name is used.
    Element element = elementFactory.create(name + "-instance");
    List<PropertyDocs> propertyDocsList = new ArrayList<>();

    if (element != null) {
      propertyDocsList = extractPropertyDocs(element, name);
    }

    return new ElementDocs(name, longName, description, propertyDocsList);
  }

  /**
   * Extracts property documentation from the given element.
   *
   * <p>This method iterates over all property names of the element, retrieves the property value,
   * determines its type (including handling enum types), and creates a list of PropertyDocs.
   *
   * @param element the GStreamer element from which to extract properties
   * @param elementName the name of the element (used for logging)
   * @return a list of PropertyDocs for the element
   */
  private List<PropertyDocs> extractPropertyDocs(Element element, String elementName) {
    List<PropertyDocs> propertyDocsList = new ArrayList<>();

    for (String propertyName : element.listPropertyNames()) {
      if (propertyName == null) {
        continue;
      }
      try {
        Object propertyValue = element.get(propertyName);
        String propertyType;
        List<String> enumValues = null;
        if (propertyValue != null) {
          Class<?> propertyClass = propertyValue.getClass();
          if (propertyClass.isEnum()) {
            propertyType = "Enum<" + propertyClass.getSimpleName() + ">";
            enumValues = new ArrayList<>();
            // Retrieve all enum constants.
            for (Object enumConstant : propertyClass.getEnumConstants()) {
              enumValues.add(enumConstant.toString());
            }
          } else {
            propertyType = propertyClass.getSimpleName();
          }
        } else {
          propertyType = "Unknown";
        }
        propertyDocsList.add(new PropertyDocs(propertyName, propertyType, true, true, enumValues));
      } catch (IllegalArgumentException e) {
        internalLogger.warn(
            "Failed to get property '{}' for element '{}'", propertyName, elementName, e);
      } catch (Exception e) {
        internalLogger.warn(
            "Failed to get property '{}' for element '{}'", propertyName, elementName, e);
      }
    }
    return propertyDocsList;
  }
}
