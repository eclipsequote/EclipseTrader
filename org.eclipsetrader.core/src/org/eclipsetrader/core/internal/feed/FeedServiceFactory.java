/*
 * Copyright (c) 2004-2008 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package org.eclipsetrader.core.internal.feed;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class FeedServiceFactory implements ServiceFactory {
	private FeedService serviceInstance;

	public FeedServiceFactory() {
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
	 */
	public Object getService(Bundle bundle, ServiceRegistration registration) {
		if (serviceInstance == null) {
			serviceInstance = new FeedService();
			serviceInstance.startUp();
		}
		return serviceInstance;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
	 */
	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
	}

	public void dispose() {
		if (serviceInstance != null)
			serviceInstance.shutDown();
	}
}