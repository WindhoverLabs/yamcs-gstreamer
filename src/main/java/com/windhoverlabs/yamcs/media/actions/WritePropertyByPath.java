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
 * utility to write the specified value to the property. The action logs the outcome of the
 * operation.
 */
public class WritePropertyByPath extends LinkAction {
  // Logger for logging action details.
  private final Logger internalLogger;
  // GStreamerLink instance for accessing the active GStreamer pipeline.
  private final GStreamerLink gStreamerLink;

  /**
   * Constructs a new WritePropertyByPath action.
   *
   * @param gStreamerLink the GStreamer link used to obtain the active pipeline
   * @param logger the logger to use for logging messages
   */
  public WritePropertyByPath(GStreamerLink gStreamerLink, Logger logger) {
    super("WritePropertyByPath", "Write value to property");
    this.internalLogger = logger;
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
   * @return the action specification containing required options
   */
  @Override
  public Spec getSpec() {
    Spec spec = new Spec();
    spec.addOption("path", OptionType.STRING).withRequired(true);
    spec.addOption("value", OptionType.STRING).withRequired(true);
    return spec;
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
      final String path = request.get("path").getAsString();
      final String value = request.get("value").getAsString();

      // Retrieve the active GStreamer pipeline from the GStreamerLink.
      Pipeline pipeline = gStreamerLink.getActivePipeline();
      if (pipeline == null) {
        throw new ConfigurationException("Pipeline not set");
      }

      // Attempt to write the property and retrieve the updated value.
      String newValue = GStreamerUtils.writePropertyByPath(pipeline, path, value);
      if (newValue != null) {
        internalLogger.info("WritePropertyByPath: {} = {}", path, newValue);
      } else {
        internalLogger.info("WritePropertyByPath failed");
      }

      // Mark the action result as complete.
      result.complete();
    } catch (ConfigurationException e) {
      // Complete the action result exceptionally in case of errors.
      result.completeExceptionally(e);
    }
  }
}
