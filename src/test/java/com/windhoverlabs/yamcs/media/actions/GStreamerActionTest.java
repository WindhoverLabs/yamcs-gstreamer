package com.windhoverlabs.yamcs.media.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.windhoverlabs.yamcs.media.GStreamerLink;
import com.windhoverlabs.yamcs.media.utils.GStreamerUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.yamcs.actions.ActionResult;
import org.yamcs.tctm.Link;

public class GStreamerActionTest {

  @Test
  public void setActivePipelineActionStartsRequestedPipeline() {
    // Covers AR-002.
    GStreamerLink link = mock(GStreamerLink.class);
    Logger logger = mock(Logger.class);
    SetActivePipelineAction action = new SetActivePipelineAction(link, logger, "ball");
    ActionResult result = new ActionResult();

    action.execute(mock(Link.class), new JsonObject(), result);

    verify(link).startPipeline("ball");
    assertTrue(result.future().isDone());
  }

  @Test
  public void dumpPropertyToLogLogsResolvedValue() throws Exception {
    // Covers AR-005.
    GStreamerLink link = mock(GStreamerLink.class);
    Logger logger = mock(Logger.class);
    Pipeline pipeline = mock(Pipeline.class);
    when(link.getActivePipeline()).thenReturn(pipeline);
    DumpPropertyToLog action = new DumpPropertyToLog(link, logger);
    JsonObject request = new JsonObject();
    request.addProperty("path", "encoder0/bitrate");
    ActionResult result = new ActionResult();

    try (MockedStatic<GStreamerUtils> utils = Mockito.mockStatic(GStreamerUtils.class)) {
      utils.when(() -> GStreamerUtils.readPropertyByPath(pipeline, "encoder0/bitrate"))
          .thenReturn("2048");

      action.execute(mock(Link.class), request, result);
    }

    verify(logger).info("DumpPropertyToLog: {}", "2048");
    assertTrue(result.future().isDone());
  }

  @Test
  public void writePropertyByPathFailsWhenNoPipelineIsActive() {
    // Covers AR-006.
    GStreamerLink link = mock(GStreamerLink.class);
    Logger logger = mock(Logger.class);
    when(link.getActivePipeline()).thenReturn(null);
    WritePropertyByPath action = new WritePropertyByPath(link, logger);
    JsonObject request = new JsonObject();
    request.addProperty("path", "encoder0/bitrate");
    request.addProperty("value", "2048");
    ActionResult result = new ActionResult();

    action.execute(mock(Link.class), request, result);

    try {
      result.future().get();
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Pipeline not set"));
      return;
    } catch (Exception e) {
      throw new AssertionError("Unexpected exception type", e);
    }
    throw new AssertionError("Expected the action to fail when no pipeline is active");
  }

  @Test
  public void dumpAllElementNamesToLogLogsEveryFactoryName() {
    // Covers AR-003.
    Logger logger = mock(Logger.class);
    DumpAllElementNamesToLog action = new DumpAllElementNamesToLog(logger);
    ElementFactory first = mock(ElementFactory.class);
    ElementFactory second = mock(ElementFactory.class);
    when(first.getName()).thenReturn("videotestsrc");
    when(second.getName()).thenReturn("fakesink");

    try (MockedStatic<ElementFactory> factories = Mockito.mockStatic(ElementFactory.class)) {
      factories
          .when(() -> ElementFactory.listGetElements(any(), any()))
          .thenReturn(java.util.Arrays.asList(first, second));

      action.execute(mock(Link.class), new JsonObject(), new ActionResult());
    }

    verify(logger).info("DumpAllElementNamesToLog: {}", "videotestsrc");
    verify(logger).info("DumpAllElementNamesToLog: {}", "fakesink");
  }

  @Test
  public void dumpAllElementsToFileWritesYamlDocumentation() throws Exception {
    // Covers AR-007, YR-001, YR-002, YR-003.
    Logger logger = mock(Logger.class);
    DumpAllElementsToFile action = new DumpAllElementsToFile(logger);
    ElementFactory factory = mock(ElementFactory.class);
    Element element = mock(Element.class);
    Path output = Files.createTempFile("elements", ".yaml");
    JsonObject request = new JsonObject();
    request.addProperty("file", output.toString());
    ActionResult result = new ActionResult();

    when(factory.getName()).thenReturn("videotestsrc");
    when(factory.getLongName()).thenReturn("Video Test Source");
    when(factory.getDescription()).thenReturn("Generates test video");
    when(factory.create("videotestsrc-instance")).thenReturn(element);
    when(element.listPropertyNames()).thenReturn(Collections.singletonList("pattern"));
    when(element.get("pattern")).thenReturn(TestPattern.SMPTE);

    try (MockedStatic<Gst> gst = Mockito.mockStatic(Gst.class);
        MockedStatic<ElementFactory> factories = Mockito.mockStatic(ElementFactory.class)) {
      gst.when(Gst::isInitialized).thenReturn(true);
      factories
          .when(() -> ElementFactory.listGetElements(any(), any()))
          .thenReturn(Collections.singletonList(factory));

      action.execute(mock(Link.class), request, result);
    }

    assertTrue(result.future().isDone());
    String yaml = Files.readString(output, StandardCharsets.UTF_8);
    assertTrue(yaml.contains("name: videotestsrc"));
    assertTrue(yaml.contains("longName: Video Test Source"));
    assertTrue(yaml.contains("description: Generates test video"));
    assertTrue(yaml.contains("type: Enum<TestPattern>"));
    assertTrue(yaml.contains("- SMPTE"));
  }

  @Test
  public void writePropertyByPathLogsUtilityResult() {
    // Covers AR-006.
    GStreamerLink link = mock(GStreamerLink.class);
    Logger logger = mock(Logger.class);
    Pipeline pipeline = mock(Pipeline.class);
    when(link.getActivePipeline()).thenReturn(pipeline);
    WritePropertyByPath action = new WritePropertyByPath(link, logger);
    JsonObject request = new JsonObject();
    request.addProperty("path", "encoder0/bitrate");
    request.addProperty("value", "4096");
    ActionResult result = new ActionResult();

    try (MockedStatic<GStreamerUtils> utils = Mockito.mockStatic(GStreamerUtils.class)) {
      utils.when(() -> GStreamerUtils.writePropertyByPath(pipeline, "encoder0/bitrate", "4096"))
          .thenReturn("encoder0");

      action.execute(mock(Link.class), request, result);
    }

    verify(logger).info("WritePropertyByPath: {} = {}", "encoder0/bitrate", "encoder0");
    assertTrue(result.future().isDone());
  }

  private enum TestPattern {
    SMPTE
  }
}
