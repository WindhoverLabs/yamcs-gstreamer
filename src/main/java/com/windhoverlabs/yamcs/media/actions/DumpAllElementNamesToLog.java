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
 * <p>This action retrieves all available GStreamer element factories and logs their names using the
 * provided logger.
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
   * <p>This method retrieves a list of all element factories using GStreamer's {@code
   * listGetElements} method and logs each element factory's name.
   *
   * @param link the link on which the action is executed (unused in this implementation)
   * @param request the JSON request object (not used by this action)
   * @param result the ActionResult to be completed after execution
   */
  @Override
  public void execute(Link link, JsonObject request, ActionResult result) {
    try {
      // Retrieve all available element factories with any type and rank NONE.
      List<ElementFactory> elements = ElementFactory.listGetElements(ListType.ANY, Rank.NONE);

      // Iterate over each element factory and log its name.
      for (ElementFactory elementFactory : elements) {
        internalLogger.info("DumpAllElementNamesToLog: {}", elementFactory.getName());
      }

      // Mark the action result as complete.
      result.complete();
    } catch (ConfigurationException e) {
      // Complete the action result exceptionally if a configuration error occurs.
      result.completeExceptionally(e);
    }
  }
}
