/*
 * Copyright (c) 2004-2014 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package org.eclipsetrader.yahoo.internal.core.connector;

import java.util.Map;

import junit.framework.TestCase;

public class StreamingConnectorTest extends TestCase {

    public void testParseUnixTime() throws Exception {
        StreamingConnector connector = new StreamingConnector();

        String s = "parent.yfs_mktmcb({\"unixtime\":1246097383,\"open\":0,\"close\":0});";
        Map<String, String> map = connector.parseScript(s);

        assertEquals("1246097383", map.get("unixtime"));
    }

    public void testParseLastPrice() throws Exception {
        StreamingConnector connector = new StreamingConnector();

        String s = "parent.yfs_u1f({\"MSFT\":{l10:\"23.35\",c10:\"-0.44\",p20:\"-1.85\"}});";
        Map<String, String> map = connector.parseScript(s);

        assertEquals("MSFT", map.get(StreamingConnector.K_SYMBOL));
        assertEquals("23.35", map.get(StreamingConnector.K_LAST));
    }

    public void testParseBidPrice() throws Exception {
        StreamingConnector connector = new StreamingConnector();

        String s = "parent.yfs_u1f({\"MSFT\":{b00:\"42.17\"}});";
        Map<String, String> map = connector.parseScript(s);

        assertEquals("MSFT", map.get(StreamingConnector.K_SYMBOL));
        assertEquals("42.17", map.get(StreamingConnector.K_BID_PRICE));
    }

    public void testParseFullScript() throws Exception {
        StreamingConnector connector = new StreamingConnector();

        String s = "try{parent.yfs_u1f({\"MSFT\":{l10:\"42.45\",v00:\"28,758,961\",a00:\"42.80\",a50:\"1600\",b00:\"42.15\",b60:\"200\",g00:\"42.03\",h00:\"42.47\",j10:\"350.65B\"}});}catch(e){}";
        Map<String, String> map = connector.parseScript(s);

        assertEquals("MSFT", map.get(StreamingConnector.K_SYMBOL));
        assertEquals("42.45", map.get(StreamingConnector.K_LAST));
        assertEquals("42.15", map.get(StreamingConnector.K_BID_PRICE));
    }
}
