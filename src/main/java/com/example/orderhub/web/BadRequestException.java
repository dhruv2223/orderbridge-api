package com.example.orderhub.web;

public class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}

}
