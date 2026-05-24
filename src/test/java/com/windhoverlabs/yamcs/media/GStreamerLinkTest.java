package com.windhoverlabs.yamcs.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.State;
import org.junit.Test;
import org.yamcs.Spec;
import org.yamcs.YConfiguration;
import org.yamcs.parameter.SystemParametersService;
import org.yamcs.protobuf.Yamcs.Value.Type;
import org.yamcs.tctm.Link.Status;
import org.yamcs.xtce.EnumeratedParameterType;
import org.yamcs.xtce.SystemParameter;
import org.yamcs.xtce.ValueEnumeration;

public class GStreamerLinkTest {

  @Test
  public void specValidatesConfiguredLinkShape() throws Exception {
    // Covers CR-001, CR-003, CR-004.
    GStreamerLink link = new GStreamerLink();
    Spec spec = link.getSpec();

    Map<String, Object> config = new LinkedHashMap<>();
    config.put("name", "video-link");
    config.put("class", "com.windhoverlabs.yamcs.media.GStreamerLink");
    config.put(
        "pipelines",
        Collections.singletonList(
            mapOf("name", "p1", "description", "videotestsrc name=src0 ! fakesink")));
    config.put("telemetry", Collections.singletonList(mapOf("path", "src0/pattern")));

    YConfiguration validated = spec.validate(YConfiguration.wrap(config));

    assertEquals("video-link", validated.getString("name"));
    assertEquals("com.windhoverlabs.yamcs.media.GStreamerLink", validated.getString("class"));
    assertEquals(1, validated.getConfigList("pipelines").size());
    assertEquals(1, validated.getConfigList("telemetry").size());
  }

  @Test
  public void setupSystemParametersRegistersConfiguredTelemetry() throws Exception {
    // Covers TR-001.
    GStreamerLink link = new GStreamerLink();
    setField(link, "config", YConfiguration.wrap(configWithTelemetry()));
    setField(link, "linkName", "video-link");

    SystemParametersService service = mock(SystemParametersService.class);
    SystemParameter linkStatus = mock(SystemParameter.class);
    EnumeratedParameterType statusType = mock(EnumeratedParameterType.class);
    ValueEnumeration valueEnumeration = mock(ValueEnumeration.class);
    when(service.createEnumeratedSystemParameter(anyString(), eq(Status.class), anyString()))
        .thenReturn(linkStatus);
    when(linkStatus.getParameterType()).thenReturn(statusType);
    when(statusType.enumValue(anyString())).thenReturn(valueEnumeration);
    when(service.createSystemParameter(any(), eq(Type.UINT64), any()))
        .thenReturn(mock(SystemParameter.class));
    when(service.createSystemParameter(any(), eq(Type.DOUBLE), any(), any()))
        .thenReturn(mock(SystemParameter.class));
    SystemParameter parameter = mock(SystemParameter.class);
    when(service.createSystemParameter(
            eq("links/video-link/udpsink0/host"), eq(Type.STRING), eq("UDP Host")))
        .thenReturn(parameter);

    link.setupSystemParameters(service);

    verify(service).createSystemParameter("links/video-link/udpsink0/host", Type.STRING, "UDP Host");
    verify(parameter).setLongDescription("udpsink0/host");
  }

  @Test
  public void getPipelineConfigByNameFindsConfiguredPipeline() throws Exception {
    // Covers PR-001.
    GStreamerLink link = new GStreamerLink();
    setField(link, "config", YConfiguration.wrap(configWithPipelines()));

    YConfiguration pipelineConfig = link.getPipelineConfigByName("ball");

    assertNotNull(pipelineConfig);
    assertEquals("videotestsrc name=src0 pattern=ball ! fakesink", pipelineConfig.getString("description"));
  }

  @Test
  public void connectionStatusReturnsDisabledWhenNoPipelineIsActive() {
    // Covers PR-005.
    TestableGStreamerLink link = new TestableGStreamerLink();

    assertEquals(Status.DISABLED, link.exposedConnectionStatus());
  }

  @Test
  public void connectionStatusMapsPipelineStates() throws Exception {
    // Covers PR-005.
    TestableGStreamerLink link = new TestableGStreamerLink();
    Pipeline pipeline = mock(Pipeline.class);
    setField(link, "pipeline", pipeline);

    when(pipeline.getState()).thenReturn(State.PLAYING);
    assertEquals(Status.OK, link.exposedConnectionStatus());

    when(pipeline.getState()).thenReturn(State.PAUSED);
    assertEquals(Status.DISABLED, link.exposedConnectionStatus());

    when(pipeline.getState()).thenReturn(State.NULL);
    assertEquals(Status.FAILED, link.exposedConnectionStatus());

    when(pipeline.getState()).thenReturn(State.READY);
    assertEquals(Status.UNAVAIL, link.exposedConnectionStatus());
  }

  @Test
  public void detailedStatusShowsSelectedPipelineAndState() throws Exception {
    // Covers PR-006.
    GStreamerLink link = new GStreamerLink();
    Pipeline pipeline = mock(Pipeline.class);
    setField(link, "activePipeline", "ball");
    setField(link, "pipeline", pipeline);
    when(pipeline.getState()).thenReturn(State.PLAYING);

    assertEquals("\"ball\" : PLAYING", link.getDetailedStatus());
  }

  @Test
  public void detailedStatusShowsNoActivePipelineWhenUnset() {
    // Covers PR-006.
    GStreamerLink link = new GStreamerLink();

    assertEquals("<NO ACTIVE PIPELINE> : NOT RUNNING", link.getDetailedStatus());
  }

  private static Map<String, Object> configWithTelemetry() {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("telemetry", Collections.singletonList(mapOf("path", "udpsink0/host", "name", "UDP Host")));
    root.put("pipelines", Collections.emptyList());
    return root;
  }

  private static Map<String, Object> configWithPipelines() {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put(
        "pipelines",
        Arrays.asList(
            mapOf("name", "smpte", "description", "videotestsrc name=src0 pattern=smpte ! fakesink"),
            mapOf("name", "ball", "description", "videotestsrc name=src0 pattern=ball ! fakesink")));
    root.put("telemetry", Collections.emptyList());
    return root;
  }

  private static Map<String, Object> mapOf(Object... keyValues) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      map.put((String) keyValues[i], keyValues[i + 1]);
    }
    return map;
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = findField(target.getClass(), fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Field findField(Class<?> type, String fieldName) throws Exception {
    Class<?> current = type;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }

  private static class TestableGStreamerLink extends GStreamerLink {
    Status exposedConnectionStatus() {
      return super.connectionStatus();
    }
  }
}
