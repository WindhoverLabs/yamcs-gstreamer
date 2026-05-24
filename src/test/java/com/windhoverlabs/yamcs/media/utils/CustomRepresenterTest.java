package com.windhoverlabs.yamcs.media.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.windhoverlabs.yamcs.media.model.ElementDocs;
import com.windhoverlabs.yamcs.media.model.PropertyDocs;
import java.util.Collections;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

public class CustomRepresenterTest {

  @Test
  public void yamlExportOmitsNullFields() {
    // Covers YR-004.
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    CustomRepresenter representer = new CustomRepresenter(options);
    representer.addClassTag(ElementDocs.class, Tag.MAP);
    representer.addClassTag(PropertyDocs.class, Tag.MAP);
    Yaml yaml = new Yaml(representer, options);

    PropertyDocs propertyDocs = new PropertyDocs("pattern", "String", true, true, null);
    String dumped = yaml.dump(propertyDocs);

    assertTrue(dumped.contains("name: pattern"));
    assertFalse(dumped.contains("enumValues:"));
    assertFalse(dumped.contains("null"));
  }

  @Test
  public void yamlExportKeepsNameFieldFirst() {
    // Covers YR-005.
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    CustomRepresenter representer = new CustomRepresenter(options);
    representer.addClassTag(ElementDocs.class, Tag.MAP);
    representer.addClassTag(PropertyDocs.class, Tag.MAP);
    Yaml yaml = new Yaml(representer, options);

    ElementDocs elementDocs =
        new ElementDocs(
            "videotestsrc",
            "Video Test Source",
            "Generates test frames",
            Collections.singletonList(new PropertyDocs("pattern", "String", true, true, null)));
    String dumped = yaml.dump(elementDocs);

    assertTrue(
        dumped.indexOf("name: videotestsrc") < dumped.indexOf("longName: Video Test Source"));
  }
}
