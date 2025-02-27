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
import java.util.List;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.ElementFactory.ListType;
import org.freedesktop.gstreamer.PluginFeature.Rank;
import org.slf4j.Logger;
import org.yamcs.ConfigurationException;
import org.yamcs.actions.ActionResult;
import org.yamcs.tctm.Link;
import org.yamcs.tctm.LinkAction;

/**
 * Action to dump all GStreamer element factory names to the log.
 *
 * <p>This action retrieves all available GStreamer element factories using the {@code
 * listGetElements} method and logs each factory's name using the provided logger.
 */
public class DumpAllElementNamesToLog extends LinkAction {
  // Logger for logging messages and errors.
  private static Logger internalLogger;

  /**
   * Constructs a new DumpAllElementNamesToLog action.
   *
   * @param logger the Logger instance to use for logging element factory names
   */
  public DumpAllElementNamesToLog(Logger logger) {
    super("DumpAllElementNamesToLog", "Dump all element factory names to log");
    internalLogger = logger;
  }

  /**
   * Executes the DumpAllElementNamesToLog action.
   *
   * <p>This method retrieves a list of all element factories with any type and rank NONE, then logs
   * each element factory's name.
   *
   * @param link the link on which the action is executed (unused in this implementation)
   * @param request the JSON request object (not used by this action)
   * @param result the ActionResult to be completed after execution
   */
  @Override
  public void execute(Link link, JsonObject request, ActionResult result) {
    try {
      // Retrieve all available element factories.
      List<ElementFactory> elements = ElementFactory.listGetElements(ListType.ANY, Rank.NONE);

      // Log each element factory's name using a lambda expression.
      elements.forEach(
          elementFactory ->
              internalLogger.info("DumpAllElementNamesToLog: {}", elementFactory.getName()));

      // Mark the action result as complete.
      result.complete();
    } catch (ConfigurationException e) {
      // Complete the action result exceptionally if a configuration error occurs.
      result.completeExceptionally(e);
    }
  }
}
