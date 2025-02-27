package com.windhoverlabs.yamcs.media.actions;

import com.google.gson.JsonObject;
import com.windhoverlabs.yamcs.media.GStreamerLink;
import com.windhoverlabs.yamcs.media.utils.GStreamerUtils;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Pipeline;
import org.slf4j.Logger;
import org.yamcs.ConfigurationException;
import org.yamcs.Spec;
import org.yamcs.Spec.OptionType;
import org.yamcs.actions.ActionResult;
import org.yamcs.tctm.Link;
import org.yamcs.tctm.LinkAction;

/**
 * Action to dump an element's details to the log.
 *
 * <p>This action retrieves a GStreamer element by its name from the active pipeline and logs its
 * details. The element's properties are serialized using {@link
 * GStreamerUtils#serializeElement(Element)}.
 */
public class DumpElementToLog extends LinkAction {
  // Logger for logging messages and errors.
  private Logger internalLogger;
  // GStreamerLink instance used to access the active pipeline.
  private GStreamerLink gStreamerLink;

  /**
   * Constructs a new DumpElementToLog action.
   *
   * @param gStreamerLink the GStreamerLink instance to access the active pipeline
   * @param logger the Logger instance for logging output
   */
  public DumpElementToLog(GStreamerLink gStreamerLink, Logger logger) {
    super("DumpElementToLog", "Dump an element to log");
    this.internalLogger = logger;
    this.gStreamerLink = gStreamerLink;
  }

  /**
   * Returns the specification for this action.
   *
   * <p>This action requires a single string parameter:
   *
   * <ul>
   *   <li>"name" - the name of the GStreamer element to be dumped
   * </ul>
   *
   * @return the action specification containing required options
   */
  @Override
  public Spec getSpec() {
    Spec rootSpec = new Spec();
    // Define a required string option "name" to specify the element's name.
    rootSpec.addOption("name", OptionType.STRING).withRequired(true);
    return rootSpec;
  }

  /**
   * Executes the DumpElementToLog action.
   *
   * <p>This method retrieves the element name from the JSON request, obtains the active pipeline
   * from the GStreamerLink, and then attempts to retrieve the element by name. If the element is
   * found, its details are serialized and logged. If the pipeline is not active, an informational
   * message is logged.
   *
   * @param link the link on which the action is executed (unused in this implementation)
   * @param request the JSON object containing the "name" parameter for the element
   * @param result the ActionResult to be completed after execution
   */
  @Override
  public void execute(Link link, JsonObject request, ActionResult result) {
    try {
      // Extract the element name from the JSON request.
      String name = request.get("name").getAsString();

      // Retrieve the active GStreamer pipeline.
      Pipeline pipeline = gStreamerLink.getActivePipeline();
      if (pipeline != null) {
        // Retrieve the element by name from the active pipeline.
        Element element = pipeline.getElementByName(name);
        if (element != null) {
          // Serialize the element's details and log the output.
          internalLogger.info("DumpElementToLog: {}", GStreamerUtils.serializeElement(element));
        }
        // Mark the action as successfully completed.
        result.complete();
      } else {
        // Log an informational message if the pipeline is not active.
        internalLogger.info("DumpElementToLog: Pipeline is not active");
        result.complete();
      }
    } catch (ConfigurationException e) {
      // Complete the action result exceptionally if a configuration error occurs.
      result.completeExceptionally(e);
    }
  }
}
