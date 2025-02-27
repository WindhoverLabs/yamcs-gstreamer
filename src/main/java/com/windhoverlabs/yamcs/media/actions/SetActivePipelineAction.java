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
  private final Logger internalLogger;
  // The GStreamerLink instance used to manage GStreamer pipeline operations.
  private final GStreamerLink gstreamerLink;
  // The name of the pipeline to be activated.
  private final String pipelineName;

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
    this.internalLogger = logger;
    this.pipelineName = name;
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
      gstreamerLink.startPipeline(pipelineName);
      // Mark the action result as complete if the pipeline is successfully started.
      result.complete();
    } catch (ConfigurationException e) {
      // Complete the action result exceptionally if an error occurs (e.g., pipeline not set).
      result.completeExceptionally(e);
    }
  }
}
