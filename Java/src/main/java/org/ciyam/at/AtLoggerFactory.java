package org.ciyam.at;

@FunctionalInterface
public interface AtLoggerFactory {

	AtLogger create(final Class<?> loggerName);

}
