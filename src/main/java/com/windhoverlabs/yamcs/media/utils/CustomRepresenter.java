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

package com.windhoverlabs.yamcs.media.utils;

import java.util.Comparator;
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
  public CustomRepresenter(final DumperOptions options) {
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
      final Object javaBean,
      final Property property,
      final Object propertyValue,
      final Tag customTag) {
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
  protected Set<Property> getProperties(final Class<?> type) {
    Set<Property> properties = super.getProperties(type);
    return properties.stream()
        .sorted(
            Comparator.comparing((Property p) -> !p.getName().equals("name"))
                .thenComparing(Property::compareTo))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
