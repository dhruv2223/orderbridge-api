package io.github.dhruv2223.orderbridge.orders;

import jakarta.validation.constraints.NotNull;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "statusChange")
public record StatusChange(@NotNull OrderStatus status) {
}
