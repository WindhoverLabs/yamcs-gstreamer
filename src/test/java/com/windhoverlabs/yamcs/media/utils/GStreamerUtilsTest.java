package com.windhoverlabs.yamcs.media.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Structure;
import org.junit.Test;

public class GStreamerUtilsTest {

  @Test
  public void readPropertyByPathRejectsInvalidPath() {
    // Covers TP-001, TP-003.
    Pipeline pipeline = mock(Pipeline.class);

    String result = GStreamerUtils.readPropertyByPath(pipeline, "elementOnly");

    assertEquals("Invalid path: path must be in the format 'elementname/propertyname'.", result);
  }

  @Test
  public void readPropertyByPathReadsDirectProperty() {
    // Covers TP-001, TP-002.
    Pipeline pipeline = mock(Pipeline.class);
    Element element = mock(Element.class);
    when(pipeline.getElementByName("udpsink0")).thenReturn(element);
    when(element.get("host")).thenReturn("127.0.0.1");

    String result = GStreamerUtils.readPropertyByPath(pipeline, "udpsink0/host");

    assertEquals("127.0.0.1", result);
  }

  @Test
  public void readPropertyByPathReadsNestedStructureValue() {
    // Covers TP-002.
    Pipeline pipeline = mock(Pipeline.class);
    Element element = mock(Element.class);
    Structure structure = mock(Structure.class);
    when(pipeline.getElementByName("sink")).thenReturn(element);
    when(element.get("stats")).thenReturn(structure);
    when(structure.getValue("dropped")).thenReturn(5);

    String result = GStreamerUtils.readPropertyByPath(pipeline, "sink/stats/dropped");

    assertEquals("5", result);
  }

  @Test
  public void readPropertyByPathReportsMissingElement() {
    // Covers TP-003.
    Pipeline pipeline = mock(Pipeline.class);
    when(pipeline.getElementByName("missing")).thenReturn(null);

    String result = GStreamerUtils.readPropertyByPath(pipeline, "missing/port");

    assertEquals("Element not found: missing", result);
  }

  @Test
  public void readPropertyByPathReportsMissingProperty() {
    // Covers TP-003.
    Pipeline pipeline = mock(Pipeline.class);
    Element element = mock(Element.class);
    when(pipeline.getElementByName("source")).thenReturn(element);
    when(element.get("missing")).thenThrow(new IllegalArgumentException("missing"));

    String result = GStreamerUtils.readPropertyByPath(pipeline, "source/missing");

    assertEquals("Property not found: missing", result);
  }

  @Test
  public void writePropertySetsIntegerValue() {
    // Covers TP-005.
    Element element = mock(Element.class);
    when(element.get("port")).thenReturn(5000);

    boolean result = GStreamerUtils.writeProperty("6000", element, "port");

    assertTrue(result);
    verify(element).set("port", 6000);
  }

  @Test
  public void writePropertySetsBooleanValue() {
    // Covers TP-005.
    Element element = mock(Element.class);
    when(element.get("async")).thenReturn(Boolean.TRUE);

    boolean result = GStreamerUtils.writeProperty("false", element, "async");

    assertTrue(result);
    verify(element).set("async", false);
  }

  @Test
  public void writePropertyRejectsUnsupportedValueType() {
    // Covers TP-006.
    Element element = mock(Element.class);
    when(element.get("stats")).thenReturn(Collections.singletonMap("a", "b"));

    boolean result = GStreamerUtils.writeProperty("ignored", element, "stats");

    assertFalse(result);
  }

  @Test
  public void writePropertyByPathWritesDirectElementProperty() {
    // Covers TP-004, TP-005.
    Pipeline pipeline = mock(Pipeline.class);
    Element element = mock(Element.class);
    when(pipeline.getElementByName("encoder0")).thenReturn(element);
    when(element.get("bitrate")).thenReturn(2048);

    String result = GStreamerUtils.writePropertyByPath(pipeline, "encoder0/bitrate", "4096");

    verify(element).set("bitrate", 4096);
    assertTrue(result != null && !result.isEmpty());
  }

  @Test
  public void writePropertyByPathReportsMissingElement() {
    // Covers TP-004, TP-006.
    Pipeline pipeline = mock(Pipeline.class);
    when(pipeline.getElementByName("missing")).thenReturn(null);

    String result = GStreamerUtils.writePropertyByPath(pipeline, "missing/bitrate", "10");

    assertEquals("Element not found: missing", result);
  }

  @Test
  public void serializeElementIncludesPropertiesAndReadErrors() {
    // Covers AR-004 helper behavior and DR-002.
    Element element = mock(Element.class);
    when(element.getName()).thenReturn("videotestsrc0");
    when(element.listPropertyNames()).thenReturn(Arrays.asList("pattern", "broken"));
    when(element.get("pattern")).thenReturn("smpte");
    doThrow(new IllegalArgumentException("boom")).when(element).get(eq("broken"));

    String result = GStreamerUtils.serializeElement(element);

    assertTrue(result.contains("Element: videotestsrc0"));
    assertTrue(result.contains("pattern: smpte"));
    assertTrue(result.contains("broken: Error retrieving value"));
  }
}
