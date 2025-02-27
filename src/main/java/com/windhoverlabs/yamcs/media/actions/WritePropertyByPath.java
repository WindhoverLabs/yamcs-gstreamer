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
 * YAMCS Action for writing a value to a GStreamer property identified by a path.
 *
 * <p>This action extracts a "path" and "value" from the incoming JSON request, retrieves the active
 * GStreamer pipeline from the provided {@link GStreamerLink}, and uses the {@link GStreamerUtils}
 * utility to write the specified value to the property. The action logs the result of the
 * operation.
 */
public class WritePropertyByPath extends LinkAction {
  // Internal logger for logging action details.
  private static Logger internalLogger;
  // GStreamerLink instance for accessing the active GStreamer pipeline.
  private GStreamerLink gStreamerLink;

  /**
   * Constructs a new WritePropertyByPath action.
   *
   * @param gStreamerLink the GStreamer link used to obtain the active pipeline
   * @param logger the logger to use for logging messages
   */
  public WritePropertyByPath(GStreamerLink gStreamerLink, Logger logger) {
    super("WritePropertyByPath", "Write value to property");
    internalLogger = logger;
    this.gStreamerLink = gStreamerLink;
  }

  /**
   * Returns the specification for this action.
   *
   * <p>The specification defines two required options:
   *
   * <ul>
   *   <li>"path" - A string representing the path to the property.
   *   <li>"value" - A string representing the value to be set for the property.
   * </ul>
   *
   * @return the action specification
   */
  @Override
  public Spec getSpec() {
    Spec rootSpec = new Spec();
    // Define a required string option for the property path.
    rootSpec.addOption("path", OptionType.STRING).withRequired(true);
    // Define a required string option for the property value.
    rootSpec.addOption("value", OptionType.STRING).withRequired(true);
    return rootSpec;
  }

  /**
   * Executes the WritePropertyByPath action.
   *
   * <p>This method extracts the "path" and "value" from the provided JSON request, retrieves the
   * active GStreamer pipeline from the {@link GStreamerLink}, and attempts to write the new value
   * to the property identified by the path using {@link
   * GStreamerUtils#writePropertyByPath(Pipeline, String, String)}. It logs the outcome of the
   * operation and completes the action result accordingly.
   *
   * @param link the link on which the action is executed
   * @param request the JSON object containing action parameters ("path" and "value")
   * @param result the ActionResult to be completed after execution
   */
  @Override
  public void execute(Link link, JsonObject request, ActionResult result) {
    try {
      // Extract the "path" and "value" parameters from the JSON request.
      String path = request.get("path").getAsString();
      String value = request.get("value").getAsString();

      // Retrieve the active GStreamer pipeline from the GStreamerLink.
      Pipeline pipeline = gStreamerLink.getActivePipeline();

      // If the pipeline is not set, throw a configuration exception.
      if (pipeline == null) {
        throw new ConfigurationException("Pipeline not set");
      }

      // Attempt to write the property and retrieve the new value.
      String newValue = GStreamerUtils.writePropertyByPath(pipeline, path, value);
      if (newValue != null) {
        // Log the successful write operation.
        internalLogger.info("WritePropertyByPath: {} = {}", path, newValue);
      } else {
        // Log failure if the property could not be updated.
        internalLogger.info("WritePropertyByPath failed");
      }

      // Complete the action result successfully.
      result.complete();
    } catch (ConfigurationException e) {
      // Complete the action result exceptionally if an error occurs.
      result.completeExceptionally(e);
    }
  }
}
