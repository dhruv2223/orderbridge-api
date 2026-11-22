package com.example.orderhub.orders;

import java.util.List;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "orders")
public record OrderList(
		@JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "order") List<OrderResponse> items) {
}
