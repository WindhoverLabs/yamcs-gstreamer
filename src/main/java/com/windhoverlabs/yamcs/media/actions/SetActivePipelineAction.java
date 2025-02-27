package com.windhoverlabs.yamcs.media.actions;

import com.google.gson.JsonObject;
import com.windhoverlabs.yamcs.media.GStreamerLink;
import org.slf4j.Logger;
import org.yamcs.ConfigurationException;
import org.yamcs.actions.ActionResult;
import org.yamcs.tctm.Link;
import org.yamcs.tctm.LinkAction;

/**
 * YAMCS Action for setting the active GStreamer pipeline.
 *
 * <p>This action triggers the activation of a specified GStreamer pipeline based on the provided
 * pipeline name. It extends {@link LinkAction} and uses a push-button style action within YAMCS.
 */
public class SetActivePipelineAction extends LinkAction {
  // Logger for logging internal details of the action.
  private Logger internalLogger;
  // The GStreamerLink instance used to manage GStreamer pipeline operations.
  private GStreamerLink gstreamerLink;
  // The name of the pipeline to be activated.
  String pipelineName;

  /**
   * Constructs a new {@code SetActivePipelineAction}.
   *
   * @param gstreamerLink the GStreamerLink used to start pipelines
   * @param logger the Logger instance for logging messages and errors
   * @param name the name of the pipeline to be activated
   */
  public SetActivePipelineAction(GStreamerLink gstreamerLink, Logger logger, String name) {
    // Create a unique action name and description using the pipeline name, with a push-button
    // style.
    super("set-pipeline-" + name, "Activate " + name, ActionStyle.PUSH_BUTTON);
    internalLogger = logger;
    pipelineName = name;
    this.gstreamerLink = gstreamerLink;
  }

  /**
   * Executes the action to set the active GStreamer pipeline.
   *
   * <p>This method attempts to start the specified pipeline using the provided {@link
   * GStreamerLink}. Upon successful activation, the action result is marked as complete. If the
   * pipeline is not set or another configuration issue occurs, the result is completed
   * exceptionally with a {@link ConfigurationException}.
   *
   * @param link the link on which the action is executed (not used directly in this implementation)
   * @param request the JSON request object containing action parameters (not used in this
   *     implementation)
   * @param result the {@link ActionResult} to be completed after execution
   */
  @Override
  public void execute(Link link, JsonObject request, ActionResult result) {
    try {
      // Start the pipeline using the provided GStreamerLink and pipeline name.
      this.gstreamerLink.startPipeline(pipelineName);
      // Mark the action result as complete if the pipeline is successfully started.
      result.complete();
    } catch (ConfigurationException e) {
      // If an error occurs (e.g., pipeline not set), complete the result exceptionally.
      result.completeExceptionally(e);
    }
  }
}
