package com.example.orderhub.orders;

import jakarta.validation.constraints.NotNull;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "statusChange")
public record StatusChange(@NotNull OrderStatus status) {
}
