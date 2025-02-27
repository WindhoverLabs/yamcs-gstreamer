package com.windhoverlabs.yamcs.media.utils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Custom YAML representer for GStreamer elements and properties.
 *
 * <p>This representer customizes the default behavior provided by SnakeYAML's {@link Representer}
 * to skip properties with null values and to ensure that the property named "name" is represented
 * first in the YAML output.
 */
public class CustomRepresenter extends Representer {

  /**
   * Constructs a new {@code CustomRepresenter} with the specified {@link DumperOptions}.
   *
   * @param options the dumper options for customizing YAML output
   */
  public CustomRepresenter(DumperOptions options) {
    super(options);
  }

  /**
   * Represents a JavaBean property as a YAML node tuple.
   *
   * <p>This method overrides the default behavior to skip properties whose value is {@code null}.
   *
   * @param javaBean the Java bean instance
   * @param property the property to represent
   * @param propertyValue the value of the property
   * @param customTag a custom tag to be applied, if any
   * @return a {@link NodeTuple} representing the property, or {@code null} if the property value is
   *     {@code null}
   */
  @Override
  protected NodeTuple representJavaBeanProperty(
      Object javaBean, Property property, Object propertyValue, Tag customTag) {
    // Skip properties with null values to avoid including them in the YAML output.
    if (propertyValue == null) {
      return null;
    }
    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
  }

  /**
   * Retrieves and orders the properties of the given class for YAML representation.
   *
   * <p>This method overrides the default behavior to ensure that the property named "name" appears
   * first in the ordered set of properties.
   *
   * @param type the class whose properties are to be retrieved
   * @return an ordered {@link Set} of {@link Property} objects, with "name" as the first element
   */
  @Override
  protected Set<Property> getProperties(Class<?> type) {
    Set<Property> properties = super.getProperties(type);
    // Sort properties so that "name" comes first, followed by the rest in natural order.
    return properties.stream()
        .sorted(
            (p1, p2) -> {
              if (p1.getName().equals("name")) {
                return -1;
              } else if (p2.getName().equals("name")) {
                return 1;
              } else {
                return p1.compareTo(p2);
              }
            })
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
