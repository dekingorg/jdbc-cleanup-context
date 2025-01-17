/*
 * Copyright (c) 2010-2014 Monits S.A. <http://www.monits.com/>
 */
package com.monits.listener;

import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A context listener for cleaning up at application shutdown
 *
 * @author Juan Martín Sotuyo Dodero <jmsotuyo@monits.com>
 */
public class JDBCCleanupContextListener implements ServletContextListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(JDBCCleanupContextListener.class);

	public JDBCCleanupContextListener() {
	}

	@Override
	public void contextInitialized(final ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
		// Deregister all JDBC drivers to avoid memory leaks
		final Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			final Driver d = drivers.nextElement();

			try {
				DriverManager.deregisterDriver(d);
				LOGGER.warn(String.format("Driver %s deregistered", d));
			} catch (final SQLException ex) {
				LOGGER.warn(String.format("Error deregistering driver %s", d), ex);
			}
		}

		// Shutdown the MySQL threads to avoid memory leaks
		try {
			Class<?>  cleanupThreadClass;
			String methodName;
			try {
				cleanupThreadClass= Class.forName("com.mysql.jdbc.AbandonedConnectionCleanupThread");
				methodName="shutdown";
			}catch(ClassNotFoundException e) {
				cleanupThreadClass= Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
				methodName="checkedShutdown";
			}
			final Method method = (cleanupThreadClass == null ? null : cleanupThreadClass.getMethod(methodName));
			if (method != null) {
				method.invoke(null);
			}

		} catch (final Throwable e) {
			LOGGER.warn("Error shutting down MySQL cleanup thread.", e);
		}
	}
}
