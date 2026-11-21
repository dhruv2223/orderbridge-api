package com.example.orderhub.catalog;

import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "importReport")
public record ImportReport(String supplier, int created, int updated) {
}
