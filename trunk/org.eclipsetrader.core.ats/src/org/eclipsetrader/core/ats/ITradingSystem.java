/*
 * Copyright (c) 2004-2011 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package org.eclipsetrader.core.ats;

import org.eclipse.core.runtime.IAdaptable;

public interface ITradingSystem extends IAdaptable {

    public static final String PROPERTY_STATUS = "status";

    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_STARTING = 1;
    public static final int STATUS_STARTED = 2;
    public static final int STATUS_STOPPING = 3;
    public static final int STATUS_STOPPED = 4;

    public IStrategy getStrategy();

    public ITradingSystemInstrument[] getInstruments();

    public int getStatus();

    public void start(ITradingSystemContext context) throws Exception;

    public void stop();
}
