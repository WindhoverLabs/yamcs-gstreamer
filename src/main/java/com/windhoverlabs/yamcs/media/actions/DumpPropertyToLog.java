package com.windhoverlabs.yamcs.media.actions;

import com.google.gson.JsonObject;
import com.windhoverlabs.yamcs.media.GStreamerLink;
import com.windhoverlabs.yamcs.media.utils.GStreamerUtils;
import org.freedesktop.gstreamer.Pipeline;
import org.slf4j.Logger;
import org.yamcs.ConfigurationException;
import org.yamcs.Spec;
import org.yamcs.Spec.OptionType;
import org.yamcs.actions.ActionResult;
import org.yamcs.tctm.Link;
import org.yamcs.tctm.LinkAction;

/**
 * YAMCS Action that dumps a GStreamer property to the log.
 *
 * <p>This action retrieves a property value from the active GStreamer pipeline using a provided
 * path and logs the value using the supplied logger. The action requires a JSON parameter "path"
 * which specifies the property path to retrieve.
 */
public class DumpPropertyToLog extends LinkAction {
  // Logger for logging information and errors.
  private static Logger internalLogger;
  // GStreamerLink instance to access the active pipeline.
  private GStreamerLink gStreamerLink;

  /**
   * Constructs a new DumpPropertyToLog action.
   *
   * @param gStreamerLink the GStreamerLink instance used to access the active pipeline
   * @param logger the Logger instance for logging messages and errors
   */
  public DumpPropertyToLog(GStreamerLink gStreamerLink, Logger logger) {
    super("DumpPropertyToLog", "Dump a property to log");
    internalLogger = logger;
    this.gStreamerLink = gStreamerLink;
  }

  /**
   * Returns the specification for this action.
   *
   * <p>The specification defines a single required option:
   *
   * <ul>
   *   <li>"path" - A string representing the path to the property to be dumped.
   * </ul>
   *
   * @return the action specification containing required options
   */
  @Override
  public Spec getSpec() {
    Spec rootSpec = new Spec();
    // Define a required string option for the property path.
    rootSpec.addOption("path", OptionType.STRING).withRequired(true);
    return rootSpec;
  }

  /**
   * Executes the DumpPropertyToLog action.
   *
   * <p>This method extracts the "path" parameter from the JSON request, retrieves the active
   * GStreamer pipeline using the provided GStreamerLink, and reads the property value from the
   * pipeline using that path. The retrieved property value is then logged. If the pipeline is not
   * active or the property value cannot be retrieved, appropriate log messages are generated.
   *
   * @param link the link on which the action is executed (not used directly in this implementation)
   * @param request the JSON object containing the action parameters, including the "path" option
   * @param result the ActionResult to be completed after execution, indicating success or failure
   */
  @Override
  public void execute(Link link, JsonObject request, ActionResult result) {
    try {
      // Extract the property path from the JSON request.
      String path = request.get("path").getAsString();

      // Retrieve the active GStreamer pipeline from the GStreamerLink.
      Pipeline pipeline = gStreamerLink.getActivePipeline();

      // Check if the pipeline is active.
      if (pipeline == null) {
        internalLogger.info("DumpPropertyToLog: Pipeline is not active");
        result.complete();
        return;
      }

      // Read the property value using the provided path.
      String propertyValue = GStreamerUtils.readPropertyByPath(pipeline, path);
      if (propertyValue != null) {
        // Log the retrieved property value.
        internalLogger.info("DumpPropertyToLog: {}", propertyValue);
      } else {
        // Log an error if the property could not be read.
        internalLogger.error("DumpPropertyToLog failed");
      }

      // Mark the action result as complete.
      result.complete();
    } catch (ConfigurationException e) {
      // Complete the action result exceptionally in case of configuration errors.
      result.completeExceptionally(e);
    }
  }
}
