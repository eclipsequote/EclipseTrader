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

package org.eclipsetrader.core.feed;

/**
 * Implementation of this interface receives notifications from
 * a feed connector when a quote is updated.
 *
 * @since 1.0
 */
public interface ISubscriptionListener {

	/**
	 * Notify the receiver that a quote was updated.
	 *
	 * @param event the event that lists the updated quotes.
	 */
	public void quoteUpdate(QuoteEvent event);
}