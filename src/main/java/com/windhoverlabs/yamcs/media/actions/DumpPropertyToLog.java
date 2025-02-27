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
 * YAMCS Action that dumps a GStreamer property to the log.
 *
 * <p>This action retrieves a property value from the active GStreamer pipeline using a provided
 * path and logs the value using the supplied logger. The action requires a JSON parameter "path"
 * which specifies the property path to retrieve.
 */
public class DumpPropertyToLog extends LinkAction {
  // Logger for logging information and errors.
  private final Logger internalLogger;
  // GStreamerLink instance used to access the active pipeline.
  private final GStreamerLink gStreamerLink;

  /**
   * Constructs a new DumpPropertyToLog action.
   *
   * @param gStreamerLink the GStreamerLink instance used to access the active pipeline
   * @param logger the Logger instance for logging messages and errors
   */
  public DumpPropertyToLog(GStreamerLink gStreamerLink, Logger logger) {
    super("DumpPropertyToLog", "Dump a property to log");
    this.internalLogger = logger;
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
    Spec spec = new Spec();
    spec.addOption("path", OptionType.STRING).withRequired(true);
    return spec;
  }

  /**
   * Executes the DumpPropertyToLog action.
   *
   * <p>This method extracts the "path" parameter from the JSON request, retrieves the active
   * GStreamer pipeline using the provided GStreamerLink, and reads the property value from the
   * pipeline using that path. The retrieved property value is then logged. If the pipeline is not
   * active or the property value cannot be retrieved, appropriate log messages are generated.
   *
   * @param link the link on which the action is executed (unused in this implementation)
   * @param request the JSON object containing the action parameters, including the "path" option
   * @param result the ActionResult to be completed after execution, indicating success or failure
   */
  @Override
  public void execute(Link link, JsonObject request, ActionResult result) {
    try {
      // Extract the property path from the JSON request.
      final String path = request.get("path").getAsString();

      // Retrieve the active GStreamer pipeline.
      Pipeline pipeline = gStreamerLink.getActivePipeline();
      if (pipeline == null) {
        internalLogger.info("DumpPropertyToLog: Pipeline is not active");
        result.complete();
        return;
      }

      // Read the property value using the provided path.
      String propertyValue = GStreamerUtils.readPropertyByPath(pipeline, path);
      if (propertyValue != null) {
        internalLogger.info("DumpPropertyToLog: {}", propertyValue);
      } else {
        internalLogger.error("DumpPropertyToLog: Failed to retrieve property at path '{}'", path);
      }

      result.complete();
    } catch (ConfigurationException e) {
      result.completeExceptionally(e);
    }
  }
}
