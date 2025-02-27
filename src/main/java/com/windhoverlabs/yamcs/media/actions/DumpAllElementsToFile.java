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

      // List to hold the documentation for each element.
      List<ElementDocs> elementDocsList = new ArrayList<>();
      // Iterate over each element factory.
      for (ElementFactory elementFactory : elements) {
        // Retrieve metadata for the element.
        String name = elementFactory.getName();
        String longName = elementFactory.getLongName();
        String description = elementFactory.getDescription();

        // Create an instance of the element to access its properties.
        Element element =
            elementFactory.create(name + "-instance"); // Provide a unique name for the instance.
        List<PropertyDocs> propertyDocsList = new ArrayList<>();

        // Retrieve and document properties of the element if the element instance is not null.
        if (element != null) {
          for (String propertyName : element.listPropertyNames()) {
            if (propertyName != null) {
              Object propertyValue = null;
              try {
                propertyValue = element.get(propertyName);

                String propertyType;
                List<String> enumValues = null;
                if (propertyValue != null) {
                  // Determine the property type and handle enums.
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
                  // If property value is null, mark type as unknown.
                  propertyType = "Unknown";
                }

                // Add property documentation for the current property.
                propertyDocsList.add(
                    new PropertyDocs(
                        propertyName,
                        propertyType, // Property type
                        true, // Assume property is readable
                        true, // Assume property is writable
                        enumValues // List of enum values if applicable
                        ));
              } catch (IllegalArgumentException e) {
                // Log a warning if the property cannot be retrieved.
                internalLogger.warn(
                    "Failed to get property '{}' for element '{}'", propertyName, name, e);
                continue;
              } catch (Exception e) {
                // Log any other exception encountered while retrieving the property.
                internalLogger.warn(
                    "Failed to get property '{}' for element '{}'", propertyName, name, e);
                continue;
              }
            }
          }
        }

        // Create an ElementDocs object containing the element metadata and its properties.
        elementDocsList.add(new ElementDocs(name, longName, description, propertyDocsList));
      }

      // Write the collected element documentation to the YAML file.
      try (FileWriter writer = new FileWriter(fileName)) {
        yaml.dump(elementDocsList, writer);
      } catch (IOException e) {
        // Print stack trace and complete the result exceptionally if an I/O error occurs.
        e.printStackTrace();
        result.completeExceptionally(e); // Propagate the exception to the action result.
        return; // Exit to avoid completing the result twice.
      }

      // Mark the action as complete.
      result.complete();
    } catch (Exception e) {
      // Complete the action result exceptionally if any error occurs.
      result.completeExceptionally(e);
    }
  }
}
