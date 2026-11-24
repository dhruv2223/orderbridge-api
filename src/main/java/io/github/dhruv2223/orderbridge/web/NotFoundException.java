package io.github.dhruv2223.orderbridge.web;

public class NotFoundException extends RuntimeException {

	public NotFoundException(String resource, Object id) {
		super(resource + " '" + id + "' was not found");
	}

}
