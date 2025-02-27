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

package com.windhoverlabs.yamcs.media;

import com.windhoverlabs.yamcs.media.actions.DumpAllElementNamesToLog;
import com.windhoverlabs.yamcs.media.actions.DumpAllElementsToFile;
import com.windhoverlabs.yamcs.media.actions.DumpElementToLog;
import com.windhoverlabs.yamcs.media.actions.DumpPropertyToLog;
import com.windhoverlabs.yamcs.media.actions.SetActivePipelineAction;
import com.windhoverlabs.yamcs.media.actions.WritePropertyByPath;
import com.windhoverlabs.yamcs.media.utils.GStreamerUtils;
import java.util.ArrayList;
import java.util.List;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.GstException;
import org.freedesktop.gstreamer.GstObject;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.message.BufferingMessage;
import org.freedesktop.gstreamer.message.ErrorMessage;
import org.freedesktop.gstreamer.message.InfoMessage;
import org.freedesktop.gstreamer.message.Message;
import org.freedesktop.gstreamer.message.NeedContextMessage;
import org.freedesktop.gstreamer.message.TagMessage;
import org.freedesktop.gstreamer.message.WarningMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.ConfigurationException;
import org.yamcs.Spec;
import org.yamcs.Spec.OptionType;
import org.yamcs.YConfiguration;
import org.yamcs.parameter.ParameterValue;
import org.yamcs.parameter.SystemParametersProducer;
import org.yamcs.parameter.SystemParametersService;
import org.yamcs.protobuf.Yamcs.Value.Type;
import org.yamcs.tctm.AbstractLink;
import org.yamcs.xtce.Parameter;

/**
 * The main class for managing GStreamer pipelines in Yamcs.
 *
 * <p>This class extends {@link AbstractLink} and implements {@link SystemParametersProducer} to
 * manage GStreamer pipelines, handle telemetry parameters, and provide actions to interact with the
 * pipelines.
 */
public class GStreamerLink extends AbstractLink implements SystemParametersProducer {

  // Logger for internal logging.
  private static final Logger internalLogger = LoggerFactory.getLogger(GStreamerLink.class);
  // List of telemetry parameters configured for this link.
  private List<Parameter> telemetryParameters;
  // The currently active GStreamer pipeline.
  private Pipeline pipeline;
  // The name of the active pipeline.
  private String activePipeline;

  /**
   * Sets up system parameters for the GStreamer link.
   *
   * <p>This method reads the telemetry configuration from the link's configuration and creates
   * system parameters accordingly.
   *
   * @param sysParamService the system parameters service to register parameters with
   */
  @Override
  public void setupSystemParameters(SystemParametersService sysParamService) {
    super.setupSystemParameters(sysParamService);
    telemetryParameters = new ArrayList<>();

    // Retrieve telemetry configurations.
    List<YConfiguration> telemetry = config.getConfigList("telemetry");
    for (YConfiguration telemetryConfig : telemetry) {
      String name;
      try {
        // Attempt to retrieve the "name" field; if null, fallback to "path".
        name = telemetryConfig.getString("name");
        if (name == null) {
          name = telemetryConfig.getString("path");
        }
      } catch (Exception e) {
        // Fallback to using the "path" field if any exception occurs.
        name = telemetryConfig.getString("path");
      }
      // Create a system parameter using the telemetry path.
      Parameter parameter =
          sysParamService.createSystemParameter(
              "links/" + linkName + "/" + telemetryConfig.getString("path"), Type.STRING, name);
      // Use the telemetry "path" as the long description.
      parameter.setLongDescription(telemetryConfig.getString("path"));
      telemetryParameters.add(parameter);
    }
  }

  /**
   * Retrieves the current system parameter values.
   *
   * <p>This method iterates over configured telemetry parameters and reads their values from the
   * active GStreamer pipeline.
   *
   * @param time the timestamp to associate with the parameter values
   * @return a list of {@link ParameterValue} objects representing the current telemetry values
   */
  @Override
  public List<ParameterValue> getSystemParameters(long time) {
    ArrayList<ParameterValue> list = new ArrayList<>();

    // Iterate over each telemetry parameter and attempt to read its value from the pipeline.
    for (Parameter parameter : telemetryParameters) {
      if (pipeline != null) {
        String paramValue =
            GStreamerUtils.readPropertyByPath(pipeline, parameter.getLongDescription());
        if (paramValue != null) {
          list.add(SystemParametersService.getPV(parameter, time, paramValue));
        }
      }
    }

    try {
      // Collect additional system parameters from the superclass.
      super.collectSystemParameters(time, list);
    } catch (Exception e) {
      internalLogger.error("Exception caught when collecting link system parameters", e);
    }
    return list;
  }

  /**
   * Returns the specification for the GStreamer link.
   *
   * <p>The specification defines options for telemetry and pipelines as configured in Yamcs.
   *
   * @return the {@link Spec} object describing configuration options for this link
   */
  @Override
  public Spec getSpec() {
    // Define telemetry specification.
    Spec telemetrySpec = new Spec();
    telemetrySpec.addOption("path", OptionType.STRING).withRequired(true);
    telemetrySpec.addOption("name", OptionType.STRING).withRequired(false).withDefault(null);

    // Define pipeline specification.
    Spec pipelineSpec = new Spec();
    pipelineSpec.addOption("name", OptionType.STRING);
    pipelineSpec.addOption("description", OptionType.STRING);

    // Define the root specification for the link.
    Spec rootSpec = new Spec();
    rootSpec.addOption("name", OptionType.STRING).withRequired(true);
    rootSpec.addOption("class", OptionType.STRING).withRequired(true);
    rootSpec.addOption("activePipeline", OptionType.STRING).withDefault(null).withRequired(false);

    rootSpec
        .addOption("pipelines", OptionType.LIST)
        .withElementType(OptionType.MAP)
        .withSpec(pipelineSpec);

    rootSpec
        .addOption("telemetry", OptionType.LIST)
        .withElementType(OptionType.MAP)
        .withSpec(telemetrySpec);

    return rootSpec;
  }

  /**
   * Initializes the GStreamer link.
   *
   * <p>This method initializes the link configuration, sets up GStreamer, and enables or disables
   * the link based on the {@code activePipeline} configuration.
   *
   * @param yamcsInstance the Yamcs instance name
   * @param serviceName the service name for this link
   * @param config the configuration for this link
   * @throws ConfigurationException if GStreamer initialization fails
   */
  @Override
  public void init(String yamcsInstance, String serviceName, YConfiguration config)
      throws ConfigurationException {
    super.init(yamcsInstance, serviceName, config);
    this.config = config;
    // Get the active pipeline name from the configuration.
    activePipeline = config.getString("activePipeline");

    try {
      // Initialize GStreamer.
      Gst.init(serviceName, new String[] {});
    } catch (GstException e) {
      throw new ConfigurationException("Failed to initialize GStreamer", e);
    }

    // Disable or enable the link based on whether an active pipeline is set.
    if (activePipeline != null) {
      super.disable();
    } else {
      super.enable();
    }
  }

  /**
   * Determines the current connection status of the link.
   *
   * <p>This method returns a status based on the state of the active pipeline.
   *
   * @return the current {@link Status} of the link
   */
  @Override
  protected Status connectionStatus() {
    Status status = Status.OK;

    if (pipeline == null) {
      status = Status.DISABLED;
    } else {
      org.freedesktop.gstreamer.State state = pipeline.getState();
      // Determine the status based on the pipeline's state.
      switch (state) {
        case PLAYING:
          status = Status.OK;
          break;
        case PAUSED:
          status = Status.DISABLED;
          break;
        case NULL:
          status = Status.FAILED;
          break;
        default:
          status = Status.UNAVAIL;
      }
    }
    return status;
  }

  /**
   * Starts the GStreamer link.
   *
   * <p>This method registers various actions, enables the link, and notifies that the link has
   * started.
   */
  @Override
  protected void doStart() {
    internalLogger.info("GStreamer starting");

    // Register actions for interacting with GStreamer pipelines.
    addAction(new DumpAllElementNamesToLog(internalLogger));
    addAction(new DumpElementToLog(this, internalLogger));
    addAction(new DumpAllElementsToFile(internalLogger));
    addAction(new DumpPropertyToLog(this, internalLogger));
    addAction(new WritePropertyByPath(this, internalLogger));

    // Register actions to set active pipelines based on the configuration.
    List<YConfiguration> pipelines = config.getConfigList("pipelines");
    for (YConfiguration pipelineConfig : pipelines) {
      addAction(
          new SetActivePipelineAction(this, internalLogger, pipelineConfig.getString("name")));
    }

    // Notify that the link has started.
    notifyStarted();
    super.enable();
    internalLogger.info("GStreamer started");
  }

  /**
   * Returns the currently active GStreamer pipeline.
   *
   * @return the active {@link Pipeline} instance, or {@code null} if none is active
   */
  public Pipeline getActivePipeline() {
    return pipeline;
  }

  /**
   * Stops the GStreamer link.
   *
   * <p>This method stops the active pipeline, quits GStreamer, notifies that the link has stopped,
   * and logs the shutdown.
   */
  @Override
  protected void doStop() {
    internalLogger.info("GStreamer stopping");
    stopPipeline();
    Gst.quit();
    notifyStopped();
    internalLogger.info("GStreamer stopped");
  }

  /**
   * Enables the GStreamer pipeline.
   *
   * <p>This method starts the pipeline identified by {@code activePipeline}.
   */
  @Override
  protected void doEnable() {
    internalLogger.info("Enabling pipeline");
    startPipeline(activePipeline);
    internalLogger.info("Pipeline enabled");
  }

  /**
   * Disables the GStreamer pipeline.
   *
   * <p>This method stops the active pipeline.
   */
  @Override
  protected void doDisable() {
    internalLogger.info("Disabling pipeline");
    stopPipeline();
    internalLogger.info("Pipeline disabled");
  }

  /**
   * Retrieves a detailed status string for the link.
   *
   * <p>The status includes the active pipeline name and the current state of the pipeline.
   *
   * @return a detailed status string
   */
  @Override
  public String getDetailedStatus() {
    String status =
        (activePipeline == null) ? "<NO ACTIVE PIPELINE>" : "\"" + activePipeline + "\"";
    status += " : ";
    status += (pipeline == null) ? "NOT RUNNING" : pipeline.getState().toString();
    return status;
  }

  /**
   * Stops the active GStreamer pipeline.
   *
   * <p>This method stops the pipeline, waits (with timeout) for it to reach the {@code NULL} state,
   * disposes of it, and logs the shutdown process.
   */
  public void stopPipeline() {
    internalLogger.debug("stopPipeline initiated");
    if (pipeline != null) {
      internalLogger.info("Stopping pipeline");
      pipeline.stop();

      // Wait until the pipeline reaches the NULL state with a timeout of 5 seconds.
      long timeout = 5000; // 5 seconds
      long startTime = System.currentTimeMillis();
      while (pipeline.getState() != org.freedesktop.gstreamer.State.NULL) {
        internalLogger.debug("Waiting for pipeline to stop...");
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          internalLogger.error("Interrupted while waiting for pipeline to stop", e);
          break;
        }
        if (System.currentTimeMillis() - startTime > timeout) {
          internalLogger.warn("Timeout while waiting for pipeline to stop");
          break;
        }
      }

      pipeline.dispose();
      pipeline = null;
      internalLogger.info("Pipeline stopped");
    }
  }

  /**
   * Starts a GStreamer pipeline by name.
   *
   * <p>This method stops any currently running pipeline, retrieves the pipeline configuration by
   * name, creates a new pipeline using GStreamer, sets up a bus to handle messages, and starts the
   * pipeline.
   *
   * @param pipelineName the name of the pipeline to start
   */
  public void startPipeline(String pipelineName) {
    internalLogger.debug("startPipeline initiated for pipeline: {}", pipelineName);
    // Stop any currently running pipeline.
    if (pipeline != null) {
      stopPipeline();
    }

    // Retrieve the pipeline configuration by name using a stream.
    YConfiguration pipelineConfig = getPipelineConfigByName(pipelineName);
    if (pipelineConfig == null) {
      internalLogger.error("Pipeline not found: {}", pipelineName);
      return;
    }

    // Create the pipeline from its description.
    String pipelineDescription = pipelineConfig.getString("description");
    pipeline = (Pipeline) Gst.parseLaunch(pipelineDescription);
    if (pipeline == null) {
      throw new ConfigurationException(
          "Failed to create GStreamer pipeline with description: " + pipelineDescription);
    }
    activePipeline = pipelineName;
    pipeline.setName("my_pipeline");

    // Setup bus message handling.
    setupBus(pipeline.getBus());
    pipeline.play();
  }

  /**
   * Sets up the GStreamer bus for handling messages.
   *
   * <p>This method connects a lambda to the bus message signal and delegates handling of each
   * message to {@link #handleBusMessage(Message)}.
   *
   * @param bus the GStreamer bus from the active pipeline
   */
  private void setupBus(Bus bus) {
    bus.connect((Bus.MESSAGE) (b, message) -> handleBusMessage(message));
  }

  /**
   * Handles incoming messages from the GStreamer bus.
   *
   * <p>This method processes various message types (ERROR, WARNING, INFO, TAG, BUFFERING,
   * NEED_CONTEXT, etc.) and logs them appropriately.
   *
   * @param message the GStreamer message to handle
   */
  private void handleBusMessage(Message message) {
    GstObject source = message.getSource();
    switch (message.getType()) {
      case ERROR:
        ErrorMessage errorMessage = (ErrorMessage) message;
        internalLogger.error("{}: {}", source, errorMessage.toString());
        break;
      case WARNING:
        WarningMessage warningMessage = (WarningMessage) message;
        internalLogger.warn("{}: {}", source, warningMessage.toString());
        break;
      case STATE_CHANGED:
        // Optionally handle state changed messages.
        break;
      case INFO:
        InfoMessage infoMessage = (InfoMessage) message;
        internalLogger.info("{}: {}", source, infoMessage.toString());
        break;
      case TAG:
        TagMessage tagMessage = (TagMessage) message;
        internalLogger.info("TAG from {}: {}", source, tagMessage.getTagList().toString());
        break;
      case BUFFERING:
        BufferingMessage bufferingMessage = (BufferingMessage) message;
        int percent = bufferingMessage.getPercent();
        internalLogger.info("BUFFERING from {}: {}%", source, percent);
        break;
      case NEED_CONTEXT:
        NeedContextMessage needContextMessage = (NeedContextMessage) message;
        internalLogger.debug(
            "NEED_CONTEXT from {}: {}", source, needContextMessage.getContextType());
        break;
      default:
        internalLogger.debug("{} from {}: {}", message.getType().toString(), source, message);
        break;
    }
  }

  /**
   * Retrieves the configuration for a pipeline by its name.
   *
   * @param name the name of the pipeline
   * @return the {@link YConfiguration} for the specified pipeline, or {@code null} if not found
   */
  public YConfiguration getPipelineConfigByName(String name) {
    return config.getConfigList("pipelines").stream()
        .filter(pipelineConfig -> pipelineConfig.getString("name").equals(name))
        .findFirst()
        .orElse(null);
  }
}
