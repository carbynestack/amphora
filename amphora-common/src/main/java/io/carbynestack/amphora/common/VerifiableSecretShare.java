/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * {@link VerifiableSecretShare} is the combination of {@link OutputDeliveryObject} and {@link
 * Metadata} in one object used by the Amphora services to send their individual {@link SecretShare}
 * to the client. This bundles the describing attributes from {@link Metadata} with the information
 * from {@link OutputDeliveryObject} used to verify the integrity of the data.
 */
@AllArgsConstructor(staticName = "of")
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EqualsAndHashCode
@JsonDeserialize(using = VerifiableSecretShare.VSSDeserializer.class)
@JsonSerialize(using = VerifiableSecretShare.VSSSerializer.class)
public class VerifiableSecretShare {
  private Metadata metadata;
  private OutputDeliveryObject outputDeliveryObject;

  static class VSSDeserializer extends StdDeserializer<VerifiableSecretShare> {

    public VSSDeserializer() {
      this(null);
    }

    public VSSDeserializer(Class<VerifiableSecretShare> vc) {
      super(vc);
    }

    @Override
    public VerifiableSecretShare deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      String jsonString = jp.readValueAsTree().toString();
      ObjectReader reader =
          ((ObjectMapper) jp.getCodec())
              .reader()
              .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      Metadata metadata = reader.readValue(jsonString, Metadata.class);
      OutputDeliveryObject outputDeliveryObject =
          reader.readValue(jsonString, OutputDeliveryObject.class);
      return VerifiableSecretShare.of(metadata, outputDeliveryObject);
    }
  }

  static class VSSSerializer extends StdSerializer<VerifiableSecretShare> {

    public VSSSerializer() {
      this(null);
    }

    public VSSSerializer(Class<VerifiableSecretShare> vc) {
      super(vc);
    }

    @Override
    public void serialize(
        VerifiableSecretShare vss, JsonGenerator jsonGenerator, SerializerProvider provider)
        throws IOException {
      ObjectMapper objectMapper = (ObjectMapper) jsonGenerator.getCodec();
      ObjectNode vssNode = objectMapper.createObjectNode();
      JsonNode metaNode = objectMapper.valueToTree(vss.metadata);
      metaNode.fields().forEachRemaining(jn -> vssNode.set(jn.getKey(), jn.getValue()));
      JsonNode odoNode = objectMapper.valueToTree(vss.outputDeliveryObject);
      odoNode.fields().forEachRemaining(jn -> vssNode.set(jn.getKey(), jn.getValue()));
      jsonGenerator.writeObject(vssNode);
    }
  }
}
