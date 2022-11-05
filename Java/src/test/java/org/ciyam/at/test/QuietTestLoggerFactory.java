package org.ciyam.at.test;

import org.ciyam.at.AtLogger;
import org.ciyam.at.AtLoggerFactory;

public class QuietTestLoggerFactory implements AtLoggerFactory {

	@Override
	public AtLogger create(Class<?> loggerName) {
		return new QuietTestLogger();
	}

}
