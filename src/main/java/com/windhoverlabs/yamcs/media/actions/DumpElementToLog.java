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
  private final Logger internalLogger;
  // GStreamerLink instance used to access the active pipeline.
  private final GStreamerLink gStreamerLink;

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
   * found, its details are serialized and logged. If the pipeline is not active or the element is
   * not found, an informational or warning message is logged.
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
      if (pipeline == null) {
        internalLogger.info("DumpElementToLog: Pipeline is not active");
        result.complete();
        return;
      }

      // Retrieve the element by name from the active pipeline.
      Element element = pipeline.getElementByName(name);
      if (element != null) {
        // Serialize the element's details and log the output.
        String elementDetails = GStreamerUtils.serializeElement(element);
        internalLogger.info("DumpElementToLog: {}", elementDetails);
      } else {
        internalLogger.warn("DumpElementToLog: Element with name '{}' not found", name);
      }

      // Mark the action as successfully completed.
      result.complete();
    } catch (ConfigurationException e) {
      // Complete the action result exceptionally if a configuration error occurs.
      result.completeExceptionally(e);
    }
  }
}
