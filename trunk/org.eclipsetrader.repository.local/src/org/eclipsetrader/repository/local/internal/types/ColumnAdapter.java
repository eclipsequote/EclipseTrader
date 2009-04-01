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

package org.eclipsetrader.repository.local.internal.types;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.eclipsetrader.core.views.IColumn;

public class ColumnAdapter extends XmlAdapter<ColumnType,IColumn> {

	public ColumnAdapter() {
	}

	/* (non-Javadoc)
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
     */
    @Override
    public ColumnType marshal(IColumn v) throws Exception {
	    return v != null ? new ColumnType(v) : null;
    }

	/* (non-Javadoc)
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
     */
    @Override
    public IColumn unmarshal(ColumnType v) throws Exception {
		return v != null ? v.getElement() : null;
    }
}