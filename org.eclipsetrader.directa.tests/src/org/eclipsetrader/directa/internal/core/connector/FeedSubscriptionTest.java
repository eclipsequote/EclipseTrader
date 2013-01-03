/*
 * Copyright (c) 2004-2013 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package org.eclipsetrader.directa.internal.core.connector;

import java.util.Date;

import junit.framework.TestCase;

import org.eclipsetrader.directa.internal.core.repository.IdentifierType;

public class FeedSubscriptionTest extends TestCase {

    public void testIncrementInstanceCount() throws Exception {
        FeedSubscription subscription = new FeedSubscription(null, new IdentifierType("ID"));
        assertEquals(0, subscription.getInstanceCount());
        subscription.incrementInstanceCount();
        assertEquals(1, subscription.getInstanceCount());
    }

    public void testDecrementInstanceCount() throws Exception {
        FeedSubscription subscription = new FeedSubscription(null, new IdentifierType("ID"));
        subscription.incrementInstanceCount();
        assertEquals(1, subscription.getInstanceCount());
        int count = subscription.decrementInstanceCount();
        assertEquals(0, count);
        assertEquals(0, subscription.getInstanceCount());
    }

    public void testSetTrade() throws Exception {
        Date time = new Date();
        FeedSubscription subscription = new FeedSubscription(null, new IdentifierType("ID"));
        assertEquals(0, subscription.deltaList.size());
        subscription.setTrade(time, 10.0, null, 100L);
        assertEquals(1, subscription.deltaList.size());
        subscription.setTrade(time, 10.0, null, 100L);
        assertEquals(1, subscription.deltaList.size());
        subscription.setTrade(time, 11.0, null, 100L);
        assertEquals(2, subscription.deltaList.size());
        subscription.setTrade(time, 11.0, null, 200L);
        assertEquals(3, subscription.deltaList.size());
    }

    public void testSetQuote() throws Exception {
        FeedSubscription subscription = new FeedSubscription(null, new IdentifierType("ID"));
        assertEquals(0, subscription.deltaList.size());
        subscription.setQuote(100.0, 110.0, null, null);
        assertEquals(1, subscription.deltaList.size());
        subscription.setQuote(100.0, 110.0, null, null);
        assertEquals(1, subscription.deltaList.size());
        subscription.setQuote(105.0, 110.0, null, null);
        assertEquals(2, subscription.deltaList.size());
        subscription.setQuote(105.0, 115.0, null, null);
        assertEquals(3, subscription.deltaList.size());
        subscription.setQuote(105.0, 115.0, 1000L, 2000L);
        assertEquals(4, subscription.deltaList.size());
        subscription.setQuote(105.0, 115.0, 1100L, 2000L);
        assertEquals(5, subscription.deltaList.size());
        subscription.setQuote(105.0, 115.0, 1100L, 2100L);
        assertEquals(6, subscription.deltaList.size());
    }

    public void testTodayOHL() throws Exception {
        FeedSubscription subscription = new FeedSubscription(null, new IdentifierType("ID"));
        assertEquals(0, subscription.deltaList.size());
        subscription.setTodayOHL(10.0, 12.0, 9.0);
        assertEquals(1, subscription.deltaList.size());
        subscription.setTodayOHL(10.0, 12.0, 9.0);
        assertEquals(1, subscription.deltaList.size());
        subscription.setTodayOHL(12.0, 14.0, 8.0);
        assertEquals(2, subscription.deltaList.size());
    }

    public void testLastClose() throws Exception {
        FeedSubscription subscription = new FeedSubscription(null, new IdentifierType("ID"));
        assertEquals(0, subscription.deltaList.size());
        subscription.setLastClose(10.0, null);
        assertEquals(1, subscription.deltaList.size());
        subscription.setLastClose(10.0, null);
        assertEquals(1, subscription.deltaList.size());
        subscription.setLastClose(12.0, null);
        assertEquals(2, subscription.deltaList.size());
    }
}
