package org.ciyam.at.test;

import org.ciyam.at.AtLogger;

import java.util.function.Supplier;

public class QuietTestLogger implements AtLogger {

	@Override
	public void error(String message) {
		System.err.println("ERROR: " + message);
	}

	@Override
	public void error(Supplier<String> messageSupplier) {
		System.err.println("ERROR: " + messageSupplier.get());
	}

	@Override
	public void debug(String message) {
	}

	@Override
	public void debug(Supplier<String> messageSupplier) {
	}

	@Override
	public void echo(String message) {
		System.err.println("ECHO: " + message);
	}

	@Override
	public void echo(Supplier<String> messageSupplier) {
		System.err.println("ECHO: " + messageSupplier.get());
	}

}
