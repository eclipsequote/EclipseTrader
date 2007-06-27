/*
 * Copyright (c) 2004-2007 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package net.sourceforge.eclipsetrader.core.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sourceforge.eclipsetrader.core.CorePlugin;
import net.sourceforge.eclipsetrader.core.Repository;
import net.sourceforge.eclipsetrader.core.db.Account;
import net.sourceforge.eclipsetrader.core.db.AccountGroup;
import net.sourceforge.eclipsetrader.core.db.Alert;
import net.sourceforge.eclipsetrader.core.db.Bar;
import net.sourceforge.eclipsetrader.core.db.Chart;
import net.sourceforge.eclipsetrader.core.db.ChartIndicator;
import net.sourceforge.eclipsetrader.core.db.ChartObject;
import net.sourceforge.eclipsetrader.core.db.ChartRow;
import net.sourceforge.eclipsetrader.core.db.ChartTab;
import net.sourceforge.eclipsetrader.core.db.Dividend;
import net.sourceforge.eclipsetrader.core.db.Event;
import net.sourceforge.eclipsetrader.core.db.History;
import net.sourceforge.eclipsetrader.core.db.IntradayHistory;
import net.sourceforge.eclipsetrader.core.db.NewsItem;
import net.sourceforge.eclipsetrader.core.db.Order;
import net.sourceforge.eclipsetrader.core.db.OrderRoute;
import net.sourceforge.eclipsetrader.core.db.OrderSide;
import net.sourceforge.eclipsetrader.core.db.OrderStatus;
import net.sourceforge.eclipsetrader.core.db.OrderType;
import net.sourceforge.eclipsetrader.core.db.OrderValidity;
import net.sourceforge.eclipsetrader.core.db.PersistentObject;
import net.sourceforge.eclipsetrader.core.db.PersistentPreferenceStore;
import net.sourceforge.eclipsetrader.core.db.Security;
import net.sourceforge.eclipsetrader.core.db.SecurityGroup;
import net.sourceforge.eclipsetrader.core.db.Split;
import net.sourceforge.eclipsetrader.core.db.Transaction;
import net.sourceforge.eclipsetrader.core.db.Watchlist;
import net.sourceforge.eclipsetrader.core.db.WatchlistColumn;
import net.sourceforge.eclipsetrader.core.db.WatchlistItem;
import net.sourceforge.eclipsetrader.core.db.feed.FeedSource;
import net.sourceforge.eclipsetrader.core.db.feed.Quote;
import net.sourceforge.eclipsetrader.core.db.feed.TradeSource;
import net.sourceforge.eclipsetrader.core.db.trading.TradingSystem;
import net.sourceforge.eclipsetrader.core.db.trading.TradingSystemGroup;

import org.apache.commons.collections.map.ReferenceIdentityMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Platform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 */
public class XMLRepository extends Repository
{
    Integer securitiesNextId = new Integer(1);
    Integer securitiesGroupNextId = new Integer(1);
    Map securitiesMap = new HashMap();
    Map securityGroupsMap = new HashMap();
    Integer chartsNextId = new Integer(1);
    Map chartsMap = new HashMap();
    Integer watchlistsNextId = new Integer(1);
    Map watchlistsMap = new HashMap();
    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); //$NON-NLS-1$
    Integer accountGroupNextId = new Integer(1);
    Map accountGroupMap = new HashMap();
    Integer accountNextId = new Integer(1);
    Map accountMap = new HashMap();
    Integer eventNextId = new Integer(1);
    TradingSystemRepository tradingRepository;
    Integer orderNextId = new Integer(1);
    ReferenceIdentityMap historyMap = new ReferenceIdentityMap();
    ReferenceIdentityMap intradayHistoryMap = new ReferenceIdentityMap();
    private Log log = LogFactory.getLog(getClass());
    private ErrorHandler errorHandler = new ErrorHandler() {

        public void error(SAXParseException exception) throws SAXException
        {
            log.error(exception, exception);
        }

        public void fatalError(SAXParseException exception) throws SAXException
        {
            log.fatal(exception, exception);
        }

        public void warning(SAXParseException exception) throws SAXException
        {
            log.warn(exception, exception);
        }
    };

    public XMLRepository()
    {
        File file = new File(Platform.getLocation().toFile(), "securities.xml"); //$NON-NLS-1$
        if (file.exists() == true)
        {
            log.info("Loading securities"); //$NON-NLS-1$
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(errorHandler);
                Document document = builder.parse(file);

                Node firstNode = document.getFirstChild();
                securitiesNextId = new Integer(firstNode.getAttributes().getNamedItem("nextId").getNodeValue()); //$NON-NLS-1$
                if (firstNode.getAttributes().getNamedItem("nextGroupId") != null) //$NON-NLS-1$
                    securitiesGroupNextId = new Integer(firstNode.getAttributes().getNamedItem("nextGroupId").getNodeValue()); //$NON-NLS-1$

                NodeList childNodes = firstNode.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++)
                {
                    Node item = childNodes.item(i);
                    String nodeName = item.getNodeName();
                    if (nodeName.equalsIgnoreCase("security")) //$NON-NLS-1$
                    {
                        Security obj = loadSecurity(item.getChildNodes());
                        obj.setRepository(this);
                        securitiesMap.put(obj.getId(), obj);
                        allSecurities().add(obj);
                    }
                    else if (nodeName.equalsIgnoreCase("group")) //$NON-NLS-1$
                    {
                        SecurityGroup obj = loadSecurityGroup(item.getChildNodes());
                        obj.setRepository(this);
                        allSecurityGroups().add(obj);
                    }
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }

        file = new File(Platform.getLocation().toFile(), "watchlists.xml"); //$NON-NLS-1$
        if (file.exists() == true)
        {
            log.info("Loading watchlists"); //$NON-NLS-1$
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(errorHandler);
                Document document = builder.parse(file);

                Node firstNode = document.getFirstChild();
                watchlistsNextId = new Integer(firstNode.getAttributes().getNamedItem("nextId").getNodeValue()); //$NON-NLS-1$

                NodeList childNodes = firstNode.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++)
                {
                    Node item = childNodes.item(i);
                    String nodeName = item.getNodeName();
                    if (nodeName.equalsIgnoreCase("watchlist")) //$NON-NLS-1$
                    {
                        Watchlist obj = loadWatchlist(item.getChildNodes());
                        obj.setRepository(this);
                        watchlistsMap.put(obj.getId(), obj);
                        allWatchlists().add(obj);
                    }
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }

        file = new File(Platform.getLocation().toFile(), "charts.xml"); //$NON-NLS-1$
        if (file.exists() == true)
        {
            log.info("Loading charts"); //$NON-NLS-1$
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(errorHandler);
                Document document = builder.parse(file);

                Node firstNode = document.getFirstChild();
                chartsNextId = new Integer(firstNode.getAttributes().getNamedItem("nextId").getNodeValue()); //$NON-NLS-1$

                NodeList childNodes = firstNode.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++)
                {
                    Node item = childNodes.item(i);
                    String nodeName = item.getNodeName();
                    if (nodeName.equalsIgnoreCase("chart")) //$NON-NLS-1$
                    {
                        Chart obj = loadChart(item.getChildNodes());
                        if (obj.getSecurity() != null)
                        {
                            obj.setRepository(this);
                            chartsMap.put(obj.getId(), obj);
                            allCharts().add(obj);
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }

        boolean needToSave = false;
        for (Iterator iter = allSecurities().iterator(); iter.hasNext(); )
        {
            Security security = (Security)iter.next();
            file = new File(Platform.getLocation().toFile(), "charts/" + String.valueOf(security.getId()) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
            if (file.exists())
            {
                Chart obj = loadChart(security.getId());
                if (obj.getSecurity() != null)
                {
                    if (obj.getId().intValue() > chartsNextId.intValue())
                        chartsNextId = getNextId(obj.getId());
                    obj.setRepository(this);
                    chartsMap.put(obj.getId(), obj);
                    allCharts().add(obj);
                    file.delete();
                    needToSave = true;
                }
            }
        }
        if (needToSave)
            saveCharts();

        file = new File(Platform.getLocation().toFile(), "news.xml"); //$NON-NLS-1$
        if (file.exists() == true)
        {
            log.info("Loading news"); //$NON-NLS-1$
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(errorHandler);
                Document document = builder.parse(file);
                
                Calendar limit = Calendar.getInstance();
                limit.add(Calendar.DATE, - CorePlugin.getDefault().getPreferenceStore().getInt(CorePlugin.PREFS_NEWS_DATE_RANGE));

                Node firstNode = document.getFirstChild();

                NodeList childNodes = firstNode.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++)
                {
                    Node item = childNodes.item(i);
                    String nodeName = item.getNodeName();
                    if (nodeName.equalsIgnoreCase("news")) //$NON-NLS-1$
                    {
                        NewsItem obj = loadNews(item.getChildNodes());
                        if (obj.getDate().before(limit.getTime()))
                            continue;
                        
                        obj.setRepository(this);
                        allNews().add(obj);
                    }
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }

        file = new File(Platform.getLocation().toFile(), "accounts.xml"); //$NON-NLS-1$
        if (file.exists() == true)
        {
            log.info("Loading accounts"); //$NON-NLS-1$
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(errorHandler);
                Document document = builder.parse(file);

                Node firstNode = document.getFirstChild();
                accountNextId = new Integer(firstNode.getAttributes().getNamedItem("nextId").getNodeValue()); //$NON-NLS-1$
                accountGroupNextId = new Integer(firstNode.getAttributes().getNamedItem("nextGroupId").getNodeValue()); //$NON-NLS-1$

                NodeList childNodes = firstNode.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++)
                {
                    Node item = childNodes.item(i);
                    String nodeName = item.getNodeName();
                    if (nodeName.equalsIgnoreCase("account")) //$NON-NLS-1$
                    {
                        Account obj = loadAccount(item.getChildNodes(), null);
                        obj.setRepository(this);
                    }
                    else if (nodeName.equalsIgnoreCase("group")) //$NON-NLS-1$
                    {
                        AccountGroup obj = loadAccountGroup(item.getChildNodes(), null);
                        obj.setRepository(this);
                    }
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }

        file = new File(Platform.getLocation().toFile(), "events.xml"); //$NON-NLS-1$
        if (file.exists() == true)
        {
            log.info("Loading events"); //$NON-NLS-1$
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(errorHandler);
                Document document = builder.parse(file);
                
                Node firstNode = document.getFirstChild();

                NodeList childNodes = firstNode.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++)
                {
                    Node item = childNodes.item(i);
                    String nodeName = item.getNodeName();
                    if (nodeName.equalsIgnoreCase("event")) //$NON-NLS-1$
                    {
                        Event obj = loadEvent(item.getChildNodes());
                        obj.setRepository(this);
                        allEvents().add(obj);
                    }
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }

        file = new File(Platform.getLocation().toFile(), "orders.xml"); //$NON-NLS-1$
        if (file.exists() == true)
        {
            log.info("Loading orders"); //$NON-NLS-1$
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(errorHandler);
                Document document = builder.parse(file);
                
                Node firstNode = document.getFirstChild();
                orderNextId = new Integer(firstNode.getAttributes().getNamedItem("nextId").getNodeValue()); //$NON-NLS-1$

                NodeList childNodes = firstNode.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++)
                {
                    Node item = childNodes.item(i);
                    String nodeName = item.getNodeName();
                    if (nodeName.equalsIgnoreCase("order")) //$NON-NLS-1$
                    {
                        Order obj = loadOrder(item.getChildNodes());
                        obj.setRepository(this);
                        allOrders().add(obj);
                    }
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }
        
        eventNextId = new Integer(allEvents().size() + 1);
        
        tradingRepository = new TradingSystemRepository(this);
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.Repository#dispose()
     */
    public void dispose()
    {
    	saveSecurities();
        saveWatchlists();
        saveCharts();
        saveAccounts();
        saveNews();
        saveEvents();
        saveOrders();

        tradingRepository.saveTradingSystems();
        
        super.dispose();
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.Repository#clear()
     */
    public void clear()
    {
        File file = new File(Platform.getLocation().toFile(), "securities.xml"); //$NON-NLS-1$
        if (file.exists() == true)
            file.delete();
        file = new File(Platform.getLocation().toFile(), "watchlists.xml"); //$NON-NLS-1$
        if (file.exists() == true)
            file.delete();
        file = new File(Platform.getLocation().toFile(), "charts.xml"); //$NON-NLS-1$
        if (file.exists() == true)
            file.delete();
        file = new File(Platform.getLocation().toFile(), "news.xml"); //$NON-NLS-1$
        if (file.exists() == true)
            file.delete();
        file = new File(Platform.getLocation().toFile(), "accounts.xml"); //$NON-NLS-1$
        if (file.exists() == true)
            file.delete();
        file = new File(Platform.getLocation().toFile(), "events.xml"); //$NON-NLS-1$
        if (file.exists() == true)
            file.delete();
        file = new File(Platform.getLocation().toFile(), "orders.xml"); //$NON-NLS-1$
        if (file.exists() == true)
            file.delete();
        
        for (Iterator iter = allSecurities().iterator(); iter.hasNext(); )
        {
            Security security = (Security)iter.next();
            file = new File(Platform.getLocation().toFile(), "history/" + String.valueOf(security.getId()) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$  $NON-NLS-2$
            if (file.exists() == true)
                file.delete();
            file = new File(Platform.getLocation().toFile(), "intraday/" + String.valueOf(security.getId()) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$  $NON-NLS-2$
            if (file.exists() == true)
                file.delete();
        }
        
        securitiesNextId = new Integer(1);
        securitiesGroupNextId = new Integer(1);
        securitiesMap = new HashMap();
        securityGroupsMap = new HashMap();
        chartsNextId = new Integer(1);
        chartsMap = new HashMap();
        watchlistsNextId = new Integer(1);
        watchlistsMap = new HashMap();
        accountGroupNextId = new Integer(1);
        accountGroupMap = new HashMap();
        accountNextId = new Integer(1);
        accountMap = new HashMap();
        eventNextId = new Integer(1);
        orderNextId = new Integer(1);
        tradingRepository.clear();
        historyMap = new ReferenceIdentityMap();
        intradayHistoryMap = new ReferenceIdentityMap();

        super.clear();
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.Repository#load(java.lang.Class, java.lang.Integer)
     */
    public PersistentObject load(Class clazz, Integer id)
    {
        PersistentObject obj = null;
        
        if (clazz.equals(Security.class))
            obj = (PersistentObject)securitiesMap.get(id);
        else if (clazz.equals(SecurityGroup.class))
            obj = (PersistentObject)securityGroupsMap.get(id);
        else if (clazz.equals(IntradayHistory.class))
        {
            obj = (IntradayHistory)intradayHistoryMap.get(id);
            if (obj == null)
            {
                obj = loadIntradayHistory(id);
                intradayHistoryMap.put(id, obj);
            }
        }
        else if (clazz.equals(History.class))
        {
            obj = (History)historyMap.get(id);
            if (obj == null)
            {
                obj = loadHistory(id);
                historyMap.put(id, obj);
            }
        }
        else if (clazz.equals(Chart.class))
            obj = (PersistentObject)chartsMap.get(id);
        else if (clazz.equals(Watchlist.class))
            obj = (PersistentObject)watchlistsMap.get(id);
        else if (clazz.equals(Account.class))
            obj = (PersistentObject)accountMap.get(id);
        else if (clazz.equals(AccountGroup.class))
            obj = (PersistentObject)accountGroupMap.get(id);
        else if (clazz.equals(TradingSystem.class))
            obj = (PersistentObject)tradingRepository.tsMap.get(id);
        else if (clazz.equals(TradingSystemGroup.class))
            obj = (PersistentObject)tradingRepository.tsGroupMap.get(id);
        
        if (obj == null)
        {
            if (clazz.equals(Chart.class))
            {
                obj = loadChart(id);
                if (obj != null)
                    chartsMap.put(id, obj);
            }
        }
        
        if (obj != null && !clazz.isInstance(obj))
            return null;

        if (obj != null)
            obj.setRepository(this);
        
        return obj;
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.Repository#save(net.sourceforge.eclipsetrader.core.db.PersistentObject)
     */
    public void save(PersistentObject obj)
    {
        if (obj instanceof Event)
        {
            if (obj.getId() == null)
            {
                obj.setId(eventNextId);
                eventNextId = getNextId(eventNextId);
            }
        }
        else if (obj instanceof Security)
        {
            if (obj.getId() == null)
            {
                obj.setId(securitiesNextId);
                securitiesNextId = getNextId(securitiesNextId);
            }
            securitiesMap.put(obj.getId(), obj);
        }
        else if (obj instanceof SecurityGroup)
        {
            if (obj.getId() == null)
            {
                obj.setId(securitiesGroupNextId);
                securitiesGroupNextId = getNextId(securitiesGroupNextId);
            }
            securityGroupsMap.put(obj.getId(), obj);
        }
        else if (obj instanceof History)
            saveHistory((History)obj);
        else if (obj instanceof Chart)
        {
            if (obj.getId() == null)
            {
                obj.setId(chartsNextId);
                chartsNextId = getNextId(chartsNextId);
            }
            
            Chart chart = (Chart)obj;
            for (int r = 0; r < chart.getRows().size(); r++)
            {
                ChartRow row = (ChartRow)chart.getRows().get(r);
                row.setId(new Integer(r));
                row.setParent(chart);
                row.setRepository(this);

                for (int t = 0; t < row.getTabs().size(); t++)
                {
                    ChartTab tab = (ChartTab)row.getTabs().get(t);
                    tab.setId(new Integer(t));
                    tab.setParent(row);
                    tab.setRepository(this);

                    for (int i = 0; i < tab.getIndicators().size(); i++)
                    {
                        ChartIndicator indicator = (ChartIndicator)tab.getIndicators().get(i);
                        indicator.setId(new Integer(i));
                        indicator.setParent(tab);
                        indicator.setRepository(this);
                    }

                    for (int i = 0; i < tab.getObjects().size(); i++)
                    {
                        ChartObject object = (ChartObject)tab.getObjects().get(i);
                        object.setId(new Integer(i));
                        object.setParent(tab);
                        object.setRepository(this);
                    }
                }
            }
            
            chartsMap.put(obj.getId(), obj);
        }
        else if (obj instanceof Watchlist)
        {
            if (obj.getId() == null)
            {
                obj.setId(watchlistsNextId);
                watchlistsNextId = getNextId(watchlistsNextId);
            }
            watchlistsMap.put(obj.getId(), obj);
        }
        else if (obj instanceof Account)
        {
            if (obj.getId() == null)
            {
                obj.setId(accountNextId);
                accountNextId = getNextId(accountNextId);
            }
            accountMap.put(obj.getId(), obj);
        }
        else if (obj instanceof AccountGroup)
        {
            if (obj.getId() == null)
            {
                obj.setId(accountGroupNextId);
                accountGroupNextId = getNextId(accountGroupNextId);
            }
            accountGroupMap.put(obj.getId(), obj);
        }
        else if (obj instanceof TradingSystem)
            tradingRepository.save((TradingSystem) obj);
        else if (obj instanceof TradingSystemGroup)
            tradingRepository.save((TradingSystemGroup) obj);
        else if (obj instanceof Order)
        {
            if (obj.getId() == null)
            {
                obj.setId(orderNextId);
                orderNextId = getNextId(orderNextId);
            }
        }
        
        super.save(obj);
    }

    /* (non-Javadoc)
     * @see net.sourceforge.eclipsetrader.core.Repository#delete(net.sourceforge.eclipsetrader.core.db.PersistentObject)
     */
    public void delete(PersistentObject obj)
    {
        super.delete(obj);

        if (obj instanceof Security)
        {
            securitiesMap.remove(obj.getId());
            historyMap.remove(obj.getId());
            intradayHistoryMap.remove(obj.getId());
            File file = new File(Platform.getLocation().toFile(), "charts/" + String.valueOf(obj.getId()) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$  $NON-NLS-2$
            if (file.exists())
                file.delete();
            file = new File(Platform.getLocation().toFile(), "history/" + String.valueOf(obj.getId()) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$  $NON-NLS-2$
            if (file.exists())
                file.delete();
            file = new File(Platform.getLocation().toFile(), "intraday/" + String.valueOf(obj.getId()) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$  $NON-NLS-2$
            if (file.exists())
                file.delete();
        }
        else if (obj instanceof SecurityGroup) {
        	for (Iterator<PersistentObject> iter = ((SecurityGroup) obj).getChildrens().iterator(); iter.hasNext(); )
        		delete(iter.next());
        	securityGroupsMap.remove(obj.getId());
        }
        else if (obj instanceof History)
        {
            if (obj instanceof IntradayHistory)
            {
                intradayHistoryMap.remove(obj.getId());
                File file = new File(Platform.getLocation().toFile(), "intraday/" + String.valueOf(obj.getId()) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$  $NON-NLS-2$
                if (file.exists())
                    file.delete();
            }
            else
            {
                historyMap.remove(obj.getId());
                File file = new File(Platform.getLocation().toFile(), "history/" + String.valueOf(obj.getId()) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$  $NON-NLS-2$
                if (file.exists())
                    file.delete();
            }
        }
        else if (obj instanceof Watchlist)
            watchlistsMap.remove(obj.getId());
        else if (obj instanceof Chart)
            chartsMap.remove(obj.getId());
        else if (obj instanceof Account)
            accountMap.remove(obj.getId());
        else if (obj instanceof AccountGroup)
            accountGroupMap.remove(obj.getId());
        else if (obj instanceof TradingSystem)
        {
            TradingSystem system = (TradingSystem) obj;
            if (system.getGroup() != null)
                system.getGroup().getTradingSystems().remove(obj);
            getTradingSystems().remove(obj);
            tradingRepository.tsMap.remove(obj.getId());
        }
        else if (obj instanceof TradingSystemGroup)
        {
            TradingSystemGroup group = (TradingSystemGroup) obj;
            if (group.getParent() != null)
                group.getParent().getGroups().remove(obj);
            getTradingSystemGroups().remove(obj);
            
            Object[] members = group.getTradingSystems().toArray();
            for (int i = 0; i < members.length; i++)
                delete((PersistentObject) members[i]);
            
            members = group.getGroups().toArray();
            for (int i = 0; i < members.length; i++)
                delete((PersistentObject) members[i]);

            tradingRepository.tsGroupMap.remove(obj.getId());
        }
    }

    private Integer getNextId(Integer id)
    {
        return new Integer(id.intValue() + 1);
    }
    
    private History loadHistory(Integer id)
    {
        History barData = new History(id);
        
        File file = new File(Platform.getLocation().toFile(), "history/" + String.valueOf(id) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$  $NON-NLS-2$
        if (file.exists() == true)
        {
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(errorHandler);
                Document document = builder.parse(file);
                barData.addAll(decodeBarData(document.getFirstChild().getChildNodes()));
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }
        
        barData.clearChanged();
        
        return barData;
    }
    
    public IntradayHistory loadIntradayHistory(Integer id)
    {
        IntradayHistory barData = new IntradayHistory(id);
        
        File file = new File(Platform.getLocation().toFile(), "intraday/" + String.valueOf(id) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
        if (file.exists() == true)
        {
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(errorHandler);
                Document document = builder.parse(file);
                barData.addAll(decodeBarData(document.getFirstChild().getChildNodes()));
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }
        
        barData.clearChanged();
        
        return barData;
    }
    
    private List decodeBarData(NodeList node)
    {
        Integer id = new Integer(0);
        List barData = new ArrayList();
        
        for (int i = 0; i < node.getLength(); i++)
        {
            Node dataNode = node.item(i);
            if (dataNode.getNodeName().equalsIgnoreCase("data")) //$NON-NLS-1$
            {
                id = new Integer(id.intValue() + 1);
                Bar bar = new Bar(id);
                NodeList valuesNode = dataNode.getChildNodes();
                for (int ii = 0; ii < valuesNode.getLength(); ii++)
                {
                    Node item = valuesNode.item(ii);
                    Node value = item.getFirstChild();
                    if (value != null)
                    {
                        String nodeName = item.getNodeName();
                        if (nodeName.equalsIgnoreCase("open") == true) //$NON-NLS-1$
                            bar.setOpen(Double.parseDouble(value.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("high") == true) //$NON-NLS-1$
                            bar.setHigh(Double.parseDouble(value.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("low") == true) //$NON-NLS-1$
                            bar.setLow(Double.parseDouble(value.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("close") == true) //$NON-NLS-1$
                            bar.setClose(Double.parseDouble(value.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("volume") == true) //$NON-NLS-1$
                            bar.setVolume(Long.parseLong(value.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("date") == true) //$NON-NLS-1$
                        {
                            try {
                                bar.setDate(dateTimeFormat.parse(value.getNodeValue()));
                            } catch (Exception e) {
                                log.warn(e.toString());
                            }
                        }
                    }
                }
                barData.add(bar);
            }
        }
        
        return barData;
    }
    
    private void saveHistory(History list)
    {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setErrorHandler(errorHandler);
            Document document = builder.getDOMImplementation().createDocument(null, "history", null); //$NON-NLS-1$

            Element root = document.getDocumentElement();
            encodeBarData(list.getList(), root, document);

            if (list instanceof IntradayHistory)
                saveDocument(document, "intraday", String.valueOf(list.getId()) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
            else
                saveDocument(document, "history", String.valueOf(list.getId()) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }
    
    private void encodeBarData(List list, Element root, Document document)
    {
        for (Iterator iter = list.iterator(); iter.hasNext(); )
        {
            Bar bar = (Bar)iter.next(); 

            Element element = document.createElement("data"); //$NON-NLS-1$
            root.appendChild(element);
            
            Element node = document.createElement("open"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(bar.getOpen())));
            element.appendChild(node);
            node = document.createElement("high"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(bar.getHigh())));
            element.appendChild(node);
            node = document.createElement("low"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(bar.getLow())));
            element.appendChild(node);
            node = document.createElement("close"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(bar.getClose())));
            element.appendChild(node);
            node = document.createElement("volume"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(bar.getVolume())));
            element.appendChild(node);
            if (bar.getDate() != null)
            {
                node = document.createElement("date"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(dateTimeFormat.format(bar.getDate())));
                element.appendChild(node);
            }
        }
    }
    
    private void saveCharts()
    {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setErrorHandler(errorHandler);
            Document document = builder.getDOMImplementation().createDocument(null, "data", null); //$NON-NLS-1$

            Element root = document.getDocumentElement();
            root.setAttribute("nextId", String.valueOf(chartsNextId)); //$NON-NLS-1$
            
            for (Iterator iter = chartsMap.values().iterator(); iter.hasNext(); )
            {
                Chart chart = (Chart)iter.next();
                saveChart(chart, root, document);
            }

            saveDocument(document, "", "charts.xml"); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }
    
    private void saveChart(Chart chart, Element root, Document document)
    {
        Element element = document.createElement("chart"); //$NON-NLS-1$
        element.setAttribute("id", String.valueOf(chart.getId())); //$NON-NLS-1$
        root.appendChild(element);

        Element node = document.createElement("title"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(chart.getTitle()));
        element.appendChild(node);
        node = document.createElement("security"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(String.valueOf(chart.getSecurity().getId())));
        element.appendChild(node);
        node = document.createElement("compression"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(String.valueOf(chart.getCompression())));
        element.appendChild(node);
        node = document.createElement("period"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(String.valueOf(chart.getPeriod())));
        element.appendChild(node);
        node = document.createElement("autoScale"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(String.valueOf(chart.isAutoScale())));
        element.appendChild(node);
        if (chart.getBeginDate() != null)
        {
            node = document.createElement("begin"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(dateTimeFormat.format(chart.getBeginDate())));
            element.appendChild(node);
        }
        if (chart.getEndDate() != null)
        {
            node = document.createElement("end"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(dateTimeFormat.format(chart.getEndDate())));
            element.appendChild(node);
        }
        for (int r = 0; r < chart.getRows().size(); r++)
        {
            ChartRow row = (ChartRow)chart.getRows().get(r);
            row.setId(new Integer(r));
            row.setParent(chart);
            row.setRepository(this);

            Element rowNode = document.createElement("row"); //$NON-NLS-1$
            element.appendChild(rowNode);

            for (int t = 0; t < row.getTabs().size(); t++)
            {
                ChartTab tab = (ChartTab)row.getTabs().get(t);
                tab.setId(new Integer(t));
                tab.setParent(row);
                tab.setRepository(this);

                Element tabNode = document.createElement("tab"); //$NON-NLS-1$
                tabNode.setAttribute("label", tab.getLabel()); //$NON-NLS-1$
                rowNode.appendChild(tabNode);

                for (int i = 0; i < tab.getIndicators().size(); i++)
                {
                    ChartIndicator indicator = (ChartIndicator)tab.getIndicators().get(i);
                    indicator.setId(new Integer(i));
                    indicator.setParent(tab);
                    indicator.setRepository(this);

                    Element indicatorNode = document.createElement("indicator"); //$NON-NLS-1$
                    indicatorNode.setAttribute("pluginId", indicator.getPluginId()); //$NON-NLS-1$
                    tabNode.appendChild(indicatorNode);

                    for (Iterator iter = indicator.getParameters().keySet().iterator(); iter.hasNext(); )
                    {
                        String key = (String)iter.next();

                        node = document.createElement("param"); //$NON-NLS-1$
                        node.setAttribute("key", key); //$NON-NLS-1$
                        node.setAttribute("value", (String)indicator.getParameters().get(key)); //$NON-NLS-1$
                        indicatorNode.appendChild(node);
                    }
                }

                for (int i = 0; i < tab.getObjects().size(); i++)
                {
                    ChartObject object = (ChartObject)tab.getObjects().get(i);
                    object.setId(new Integer(i));
                    object.setParent(tab);
                    object.setRepository(this);

                    Element indicatorNode = document.createElement("object"); //$NON-NLS-1$
                    indicatorNode.setAttribute("pluginId", object.getPluginId()); //$NON-NLS-1$
                    tabNode.appendChild(indicatorNode);

                    for (Iterator iter = object.getParameters().keySet().iterator(); iter.hasNext(); )
                    {
                        String key = (String)iter.next();

                        node = document.createElement("param"); //$NON-NLS-1$
                        node.setAttribute("key", key); //$NON-NLS-1$
                        node.setAttribute("value", (String)object.getParameters().get(key)); //$NON-NLS-1$
                        indicatorNode.appendChild(node);
                    }
                }
            }
        }
    }
    
    private Chart loadChart(Integer id)
    {
        Chart chart = new Chart(id);
        chart.setRepository(this);
        chart.setSecurity((Security)load(Security.class, id));
        
        File file = new File(Platform.getLocation().toFile(), "charts/" + String.valueOf(id) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
        if (file.exists() == false)
            file = new File(Platform.getLocation().toFile(), "charts/default.xml"); //$NON-NLS-1$
        if (file.exists() == true)
        {
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setErrorHandler(errorHandler);
                Document document = builder.parse(file);

                NodeList firstNode = document.getFirstChild().getChildNodes();
                for (int r = 0; r < firstNode.getLength(); r++)
                {
                    Node item = firstNode.item(r);
                    Node valueNode = item.getFirstChild();
                    String nodeName = item.getNodeName();
                    
                    if (valueNode != null)
                    {
                        if (nodeName.equalsIgnoreCase("compression") == true) //$NON-NLS-1$
                            chart.setCompression(Integer.parseInt(valueNode.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("period") == true) //$NON-NLS-1$
                            chart.setPeriod(Integer.parseInt(valueNode.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("autoScale") == true) //$NON-NLS-1$
                            chart.setAutoScale(new Boolean(valueNode.getNodeValue()).booleanValue());
                        else if (nodeName.equalsIgnoreCase("begin") == true) //$NON-NLS-1$
                        {
                            try {
                                chart.setBeginDate(dateTimeFormat.parse(valueNode.getNodeValue()));
                            } catch (Exception e) {
                                log.warn(e.toString());
                            }
                        }
                        else if (nodeName.equalsIgnoreCase("end") == true) //$NON-NLS-1$
                        {
                            try {
                                chart.setEndDate(dateTimeFormat.parse(valueNode.getNodeValue()));
                            } catch (Exception e) {
                                log.warn(e.toString());
                            }
                        }
                    }
                    if (nodeName.equalsIgnoreCase("row")) //$NON-NLS-1$
                    {
                        ChartRow row = new ChartRow(new Integer(r));
                        row.setRepository(this);
                        row.setParent(chart);
                        
                        NodeList tabList = item.getChildNodes();
                        for (int t = 0; t < tabList.getLength(); t++)
                        {
                            item = tabList.item(t);
                            nodeName = item.getNodeName();
                            if (nodeName.equalsIgnoreCase("tab")) //$NON-NLS-1$
                            {
                                ChartTab tab = new ChartTab(new Integer(t));
                                tab.setRepository(this);
                                tab.setParent(row);
                                tab.setLabel(((Node)item).getAttributes().getNamedItem("label").getNodeValue()); //$NON-NLS-1$

                                NodeList indicatorList = item.getChildNodes();
                                for (int i = 0; i < indicatorList.getLength(); i++)
                                {
                                    item = indicatorList.item(i);
                                    nodeName = item.getNodeName();
                                    if (nodeName.equalsIgnoreCase("indicator")) //$NON-NLS-1$
                                    {
                                        ChartIndicator indicator = new ChartIndicator(new Integer(i));
                                        indicator.setRepository(this);
                                        indicator.setParent(tab);
                                        indicator.setPluginId(((Node)item).getAttributes().getNamedItem("pluginId").getNodeValue()); //$NON-NLS-1$

                                        NodeList parametersList = item.getChildNodes();
                                        for (int p = 0; p < parametersList.getLength(); p++)
                                        {
                                            item = parametersList.item(p);
                                            nodeName = item.getNodeName();
                                            if (nodeName.equalsIgnoreCase("param")) //$NON-NLS-1$
                                            {
                                                String key = ((Node)item).getAttributes().getNamedItem("key").getNodeValue();  //$NON-NLS-1$
                                                String value = ((Node)item).getAttributes().getNamedItem("value").getNodeValue(); //$NON-NLS-1$
                                                indicator.getParameters().put(key, value);
                                            }
                                        }
                                        
                                        tab.getIndicators().add(indicator);
                                    }
                                    else if (nodeName.equalsIgnoreCase("object")) //$NON-NLS-1$
                                    {
                                        ChartObject object = new ChartObject(new Integer(i));
                                        object.setRepository(this);
                                        object.setParent(tab);
                                        object.setPluginId(((Node)item).getAttributes().getNamedItem("pluginId").getNodeValue()); //$NON-NLS-1$

                                        NodeList parametersList = item.getChildNodes();
                                        for (int p = 0; p < parametersList.getLength(); p++)
                                        {
                                            item = parametersList.item(p);
                                            nodeName = item.getNodeName();
                                            if (nodeName.equalsIgnoreCase("param")) //$NON-NLS-1$
                                            {
                                                String key = ((Node)item).getAttributes().getNamedItem("key").getNodeValue();  //$NON-NLS-1$
                                                String value = ((Node)item).getAttributes().getNamedItem("value").getNodeValue(); //$NON-NLS-1$
                                                object.getParameters().put(key, value);
                                            }
                                        }
                                        
                                        tab.getObjects().add(object);
                                    }
                                }

                                row.getTabs().add(tab);
                            }
                        }
                        
                        chart.getRows().add(row);
                    }
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }
        
        if (chart.getTitle().length() == 0)
            chart.setTitle(chart.getSecurity().getDescription());
        chart.clearChanged();
        
        return chart;
    }
    
    private Chart loadChart(NodeList node)
    {
        Chart chart = new Chart(new Integer(Integer.parseInt(((Node)node).getAttributes().getNamedItem("id").getNodeValue()))); //$NON-NLS-1$
        chart.setRepository(this);
        
        for (int r = 0; r < node.getLength(); r++)
        {
            Node item = node.item(r);
            Node valueNode = item.getFirstChild();
            String nodeName = item.getNodeName();
            
            if (valueNode != null)
            {
                if (nodeName.equalsIgnoreCase("title") == true) //$NON-NLS-1$
                    chart.setTitle(valueNode.getNodeValue());
                else if (nodeName.equals("security")) //$NON-NLS-1$
                {
                    chart.setSecurity((Security)load(Security.class, new Integer(Integer.parseInt(valueNode.getNodeValue()))));
                    if (chart.getSecurity() == null)
                        log.warn("Cannot load security (id=" + valueNode.getNodeValue() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                else if (nodeName.equalsIgnoreCase("title") == true) //$NON-NLS-1$
                    chart.setTitle(valueNode.getNodeValue());
                else if (nodeName.equalsIgnoreCase("compression") == true) //$NON-NLS-1$
                    chart.setCompression(Integer.parseInt(valueNode.getNodeValue()));
                else if (nodeName.equalsIgnoreCase("period") == true) //$NON-NLS-1$
                    chart.setPeriod(Integer.parseInt(valueNode.getNodeValue()));
                else if (nodeName.equalsIgnoreCase("autoScale") == true) //$NON-NLS-1$
                    chart.setAutoScale(new Boolean(valueNode.getNodeValue()).booleanValue());
                else if (nodeName.equalsIgnoreCase("begin") == true) //$NON-NLS-1$
                {
                    try {
                        chart.setBeginDate(dateTimeFormat.parse(valueNode.getNodeValue()));
                    } catch (Exception e) {
                        log.warn(e.toString());
                    }
                }
                else if (nodeName.equalsIgnoreCase("end") == true) //$NON-NLS-1$
                {
                    try {
                        chart.setEndDate(dateTimeFormat.parse(valueNode.getNodeValue()));
                    } catch (Exception e) {
                        log.warn(e.toString());
                    }
                }
            }
            if (nodeName.equalsIgnoreCase("row")) //$NON-NLS-1$
            {
                ChartRow row = new ChartRow(new Integer(r));
                row.setRepository(this);
                row.setParent(chart);
                
                NodeList tabList = item.getChildNodes();
                for (int t = 0; t < tabList.getLength(); t++)
                {
                    item = tabList.item(t);
                    nodeName = item.getNodeName();
                    if (nodeName.equalsIgnoreCase("tab")) //$NON-NLS-1$
                    {
                        ChartTab tab = new ChartTab(new Integer(t));
                        tab.setRepository(this);
                        tab.setParent(row);
                        tab.setLabel(((Node)item).getAttributes().getNamedItem("label").getNodeValue()); //$NON-NLS-1$

                        NodeList indicatorList = item.getChildNodes();
                        for (int i = 0; i < indicatorList.getLength(); i++)
                        {
                            item = indicatorList.item(i);
                            nodeName = item.getNodeName();
                            if (nodeName.equalsIgnoreCase("indicator")) //$NON-NLS-1$
                            {
                                ChartIndicator indicator = new ChartIndicator(new Integer(i));
                                indicator.setRepository(this);
                                indicator.setParent(tab);
                                indicator.setPluginId(((Node)item).getAttributes().getNamedItem("pluginId").getNodeValue()); //$NON-NLS-1$

                                NodeList parametersList = item.getChildNodes();
                                for (int p = 0; p < parametersList.getLength(); p++)
                                {
                                    item = parametersList.item(p);
                                    nodeName = item.getNodeName();
                                    if (nodeName.equalsIgnoreCase("param")) //$NON-NLS-1$
                                    {
                                        String key = ((Node)item).getAttributes().getNamedItem("key").getNodeValue();  //$NON-NLS-1$
                                        String value = ((Node)item).getAttributes().getNamedItem("value").getNodeValue(); //$NON-NLS-1$
                                        indicator.getParameters().put(key, value);
                                    }
                                }
                                
                                tab.getIndicators().add(indicator);
                            }
                            else if (nodeName.equalsIgnoreCase("object")) //$NON-NLS-1$
                            {
                                ChartObject object = new ChartObject(new Integer(i));
                                object.setRepository(this);
                                object.setParent(tab);
                                object.setPluginId(((Node)item).getAttributes().getNamedItem("pluginId").getNodeValue()); //$NON-NLS-1$

                                NodeList parametersList = item.getChildNodes();
                                for (int p = 0; p < parametersList.getLength(); p++)
                                {
                                    item = parametersList.item(p);
                                    nodeName = item.getNodeName();
                                    if (nodeName.equalsIgnoreCase("param")) //$NON-NLS-1$
                                    {
                                        String key = ((Node)item).getAttributes().getNamedItem("key").getNodeValue();  //$NON-NLS-1$
                                        String value = ((Node)item).getAttributes().getNamedItem("value").getNodeValue(); //$NON-NLS-1$
                                        object.getParameters().put(key, value);
                                    }
                                }
                                
                                tab.getObjects().add(object);
                            }
                        }

                        row.getTabs().add(tab);
                    }
                }
                
                chart.getRows().add(row);
            }
        }

        chart.clearChanged();
        
        return chart;
    }

    private SecurityGroup loadSecurityGroup(NodeList node)
    {
        SecurityGroup group = new SecurityGroup(new Integer(Integer.parseInt(((Node)node).getAttributes().getNamedItem("id").getNodeValue()))); //$NON-NLS-1$
        
        for (int i = 0; i < node.getLength(); i++)
        {
            Node item = node.item(i);
            String nodeName = item.getNodeName();
            Node value = item.getFirstChild();
            if (value != null)
            {
                if (nodeName.equals("code")) //$NON-NLS-1$
                    group.setCode(value.getNodeValue());
                else if (nodeName.equals("description")) //$NON-NLS-1$
                    group.setDescription(value.getNodeValue());
                else if (nodeName.equals("currency")) //$NON-NLS-1$
                    group.setCurrency(Currency.getInstance(value.getNodeValue()));
            }
            if (nodeName.equals("security")) //$NON-NLS-1$
            {
                Security obj = loadSecurity(item.getChildNodes());
                obj.setGroup(group);
                obj.setRepository(this);
                obj.clearChanged();
                allSecurities().add(obj);
            }
            else if (nodeName.equals("group")) //$NON-NLS-1$
            {
                SecurityGroup obj = loadSecurityGroup(item.getChildNodes());
                obj.setParentGroup(group);
                obj.setRepository(this);
                obj.clearChanged();
                allSecurityGroups().add(obj);
            }
        }
        
        group.clearChanged();
        securityGroupsMap.put(group.getId(), group);
        
        return group;
    }

    private Security loadSecurity(NodeList node)
    {
        Security security = new Security(new Integer(Integer.parseInt(((Node)node).getAttributes().getNamedItem("id").getNodeValue()))); //$NON-NLS-1$
        
        for (int i = 0; i < node.getLength(); i++)
        {
            Node item = node.item(i);
            String nodeName = item.getNodeName();
            Node value = item.getFirstChild();
            if (value != null)
            {
                if (nodeName.equals("code")) //$NON-NLS-1$
                    security.setCode(value.getNodeValue());
                else if (nodeName.equals("description")) //$NON-NLS-1$
                    security.setDescription(value.getNodeValue());
                else if (nodeName.equals("currency")) //$NON-NLS-1$
                    security.setCurrency(Currency.getInstance(value.getNodeValue()));
                else if (nodeName.equals("comment")) //$NON-NLS-1$
                    security.setComment(value.getNodeValue());
            }
            if (nodeName.equals("dataCollector")) //$NON-NLS-1$
            {
                security.setEnableDataCollector(new Boolean(((Node)item).getAttributes().getNamedItem("enable").getNodeValue()).booleanValue()); //$NON-NLS-1$
                NodeList nodeList = item.getChildNodes();
                for (int q = 0; q < nodeList.getLength(); q++)
                {
                    item = nodeList.item(q);
                    nodeName = item.getNodeName();
                    value = item.getFirstChild();
                    if (nodeName.equals("begin")) //$NON-NLS-1$
                    {
                        String[] s = value.getNodeValue().split(":"); //$NON-NLS-1$
                        security.setBeginTime(Integer.parseInt(s[0]) * 60 + Integer.parseInt(s[1]));
                    }
                    else if (nodeName.equals("end")) //$NON-NLS-1$
                    {
                        String[] s = value.getNodeValue().split(":"); //$NON-NLS-1$
                        security.setEndTime(Integer.parseInt(s[0]) * 60 + Integer.parseInt(s[1]));
                    }
                    else if (nodeName.equals("weekdays")) //$NON-NLS-1$
                        security.setWeekDays(Integer.parseInt(value.getNodeValue()));
                    else if (nodeName.equals("keepdays")) //$NON-NLS-1$
                        security.setKeepDays(Integer.parseInt(value.getNodeValue()));
                }
            }
            else if (nodeName.equalsIgnoreCase("feeds")) //$NON-NLS-1$
            {
                NodeList nodeList = item.getChildNodes();
                for (int q = 0; q < nodeList.getLength(); q++)
                {
                    item = nodeList.item(q);
                    nodeName = item.getNodeName();
                    value = item.getFirstChild();
                    if (nodeName.equals("quote")) //$NON-NLS-1$
                    {
                        FeedSource feed = new FeedSource();
                        feed.setId(item.getAttributes().getNamedItem("id").getNodeValue()); //$NON-NLS-1$
                        Node attribute = item.getAttributes().getNamedItem("exchange"); //$NON-NLS-1$
                        if (attribute != null)
                            feed.setExchange(attribute.getNodeValue());
                        if (value != null)
                            feed.setSymbol(value.getNodeValue());
                        security.setQuoteFeed(feed);
                    }
                    else if (nodeName.equals("level2")) //$NON-NLS-1$
                    {
                        FeedSource feed = new FeedSource();
                        feed.setId(item.getAttributes().getNamedItem("id").getNodeValue()); //$NON-NLS-1$
                        Node attribute = item.getAttributes().getNamedItem("exchange"); //$NON-NLS-1$
                        if (attribute != null)
                            feed.setExchange(attribute.getNodeValue());
                        if (value != null)
                            feed.setSymbol(value.getNodeValue());
                        security.setLevel2Feed(feed);
                    }
                    else if (nodeName.equals("history")) //$NON-NLS-1$
                    {
                        FeedSource feed = new FeedSource();
                        feed.setId(item.getAttributes().getNamedItem("id").getNodeValue()); //$NON-NLS-1$
                        Node attribute = item.getAttributes().getNamedItem("exchange"); //$NON-NLS-1$
                        if (attribute != null)
                            feed.setExchange(attribute.getNodeValue());
                        if (value != null)
                            feed.setSymbol(value.getNodeValue());
                        security.setHistoryFeed(feed);
                    }
                }
            }
            else if (nodeName.equalsIgnoreCase("tradeSource")) //$NON-NLS-1$
            {
                TradeSource source = new TradeSource();
                source.setTradingProviderId(item.getAttributes().getNamedItem("id").getNodeValue()); //$NON-NLS-1$
                Node attribute = item.getAttributes().getNamedItem("exchange"); //$NON-NLS-1$
                if (attribute != null)
                    source.setExchange(attribute.getNodeValue());
                NodeList quoteList = item.getChildNodes();
                for (int q = 0; q < quoteList.getLength(); q++)
                {
                    item = quoteList.item(q);
                    nodeName = item.getNodeName();
                    value = item.getFirstChild();
                    if (value != null)
                    {
                        if (nodeName.equalsIgnoreCase("symbol")) //$NON-NLS-1$
                            source.setSymbol(value.getNodeValue());
                        else if (nodeName.equalsIgnoreCase("account")) //$NON-NLS-1$
                            source.setAccountId(new Integer(value.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("quantity")) //$NON-NLS-1$
                            source.setQuantity(Integer.parseInt(value.getNodeValue()));
                    }
                }
                security.setTradeSource(source);
            }
            else if (nodeName.equalsIgnoreCase("quote")) //$NON-NLS-1$
            {
                Quote quote = new Quote();
                NodeList quoteList = item.getChildNodes();
                for (int q = 0; q < quoteList.getLength(); q++)
                {
                    item = quoteList.item(q);
                    nodeName = item.getNodeName();
                    value = item.getFirstChild();
                    if (value != null)
                    {
                        if (nodeName.equalsIgnoreCase("date")) //$NON-NLS-1$
                        {
                            try {
                                quote.setDate(dateTimeFormat.parse(value.getNodeValue()));
                            } catch (Exception e) {
                                log.warn(e.toString());
                            }
                        }
                        else if (nodeName.equalsIgnoreCase("last")) //$NON-NLS-1$
                            quote.setLast(Double.parseDouble(value.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("bid")) //$NON-NLS-1$
                            quote.setBid(Double.parseDouble(value.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("ask")) //$NON-NLS-1$
                            quote.setAsk(Double.parseDouble(value.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("bidSize")) //$NON-NLS-1$
                            quote.setBidSize(Integer.parseInt(value.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("askSize")) //$NON-NLS-1$
                            quote.setAskSize(Integer.parseInt(value.getNodeValue()));
                        else if (nodeName.equalsIgnoreCase("volume")) //$NON-NLS-1$
                            quote.setVolume(Integer.parseInt(value.getNodeValue()));
                    }
                }
                security.setQuote(quote);
            }
            else if (nodeName.equalsIgnoreCase("data")) //$NON-NLS-1$
            {
                NodeList dataList = item.getChildNodes();
                for (int q = 0; q < dataList.getLength(); q++)
                {
                    item = dataList.item(q);
                    nodeName = item.getNodeName();
                    value = item.getFirstChild();
                    if (value != null)
                    {
                        if (nodeName.equalsIgnoreCase("open")) //$NON-NLS-1$
                            security.setOpen(new Double(Double.parseDouble(value.getNodeValue())));
                        else if (nodeName.equalsIgnoreCase("high")) //$NON-NLS-1$
                            security.setHigh(new Double(Double.parseDouble(value.getNodeValue())));
                        else if (nodeName.equalsIgnoreCase("low")) //$NON-NLS-1$
                            security.setLow(new Double(Double.parseDouble(value.getNodeValue())));
                        else if (nodeName.equalsIgnoreCase("close")) //$NON-NLS-1$
                            security.setClose(new Double(Double.parseDouble(value.getNodeValue())));
                    }
                }
            }
            else if (nodeName.equalsIgnoreCase("split")) //$NON-NLS-1$
            {
                NamedNodeMap attributes = item.getAttributes();
                Split split = new Split();
                try {
                    split.setDate(dateTimeFormat.parse(attributes.getNamedItem("date").getNodeValue())); //$NON-NLS-1$
                    split.setFromQuantity(Integer.parseInt(attributes.getNamedItem("fromQuantity").getNodeValue())); //$NON-NLS-1$
                    split.setToQuantity(Integer.parseInt(attributes.getNamedItem("toQuantity").getNodeValue())); //$NON-NLS-1$
                    security.getSplits().add(split);
                } catch (Exception e) {
                    log.error(e.toString());
                }
            }
            else if (nodeName.equalsIgnoreCase("dividend")) //$NON-NLS-1$
            {
                NamedNodeMap attributes = item.getAttributes();
                Dividend dividend = new Dividend();
                try {
                    dividend.setDate(dateTimeFormat.parse(attributes.getNamedItem("date").getNodeValue())); //$NON-NLS-1$
                    dividend.setValue(new Double(Double.parseDouble(attributes.getNamedItem("value").getNodeValue())).doubleValue()); //$NON-NLS-1$
                    security.getDividends().add(dividend);
                } catch (Exception e) {
                    log.error(e.toString());
                }
            }
        }
        
        security.clearChanged();
        security.getQuoteMonitor().clearChanged();
        security.getLevel2Monitor().clearChanged();
        securitiesMap.put(security.getId(), security);
        
        return security;
    }
    
    private void saveSecurityGroup(SecurityGroup group, Document document, Element root)
    {
        Element element = document.createElement("group"); //$NON-NLS-1$
        element.setAttribute("id", String.valueOf(group.getId())); //$NON-NLS-1$
        root.appendChild(element);

        Element node = document.createElement("code"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(group.getCode()));
        element.appendChild(node);
        node = document.createElement("description"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(group.getDescription()));
        element.appendChild(node);
        if (group.getCurrency() != null)
        {
            node = document.createElement("currency"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(group.getCurrency().getCurrencyCode()));
            element.appendChild(node);
        }
        
        for (PersistentObject child : group.getChildrens()) {
        	if (child instanceof Security)
        		saveSecurity((Security) child, document, element);
        	if (child instanceof SecurityGroup)
                saveSecurityGroup((SecurityGroup) child, document, element);
        }
    }
    
    private void saveSecurity(Security security, Document document, Element root)
    {
        Element element = document.createElement("security"); //$NON-NLS-1$
        element.setAttribute("id", String.valueOf(security.getId())); //$NON-NLS-1$
        root.appendChild(element);
        
        Element node = document.createElement("code"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(security.getCode()));
        element.appendChild(node);
        node = document.createElement("description"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(security.getDescription()));
        element.appendChild(node);
        if (security.getCurrency() != null)
        {
            node = document.createElement("currency"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(security.getCurrency().getCurrencyCode()));
            element.appendChild(node);
        }
        
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(false);
        nf.setMinimumIntegerDigits(2);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(0);
        
        Element collectorNode = document.createElement("dataCollector"); //$NON-NLS-1$
        collectorNode.setAttribute("enable", String.valueOf(security.isEnableDataCollector())); //$NON-NLS-1$
        element.appendChild(collectorNode);
        node = document.createElement("begin"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(nf.format(security.getBeginTime() / 60) + ":" + nf.format(security.getBeginTime() % 60))); //$NON-NLS-1$
        collectorNode.appendChild(node);
        node = document.createElement("end"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(nf.format(security.getEndTime() / 60) + ":" + nf.format(security.getEndTime() % 60))); //$NON-NLS-1$
        collectorNode.appendChild(node);
        node = document.createElement("weekdays"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(String.valueOf(security.getWeekDays())));
        collectorNode.appendChild(node);
        node = document.createElement("keepdays"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(String.valueOf(security.getKeepDays())));
        collectorNode.appendChild(node);

        if (security.getQuoteFeed() != null || security.getLevel2Feed() != null || security.getHistoryFeed() != null)
        {
            Node feedsNode = document.createElement("feeds"); //$NON-NLS-1$
            element.appendChild(feedsNode);
            if (security.getQuoteFeed() != null)
            {
                node = document.createElement("quote"); //$NON-NLS-1$
                node.setAttribute("id", security.getQuoteFeed().getId()); //$NON-NLS-1$
                if (security.getQuoteFeed().getExchange() != null)
                    node.setAttribute("exchange", security.getQuoteFeed().getExchange()); //$NON-NLS-1$
                node.appendChild(document.createTextNode(security.getQuoteFeed().getSymbol()));
                feedsNode.appendChild(node);
            }
            if (security.getLevel2Feed() != null)
            {
                node = document.createElement("level2"); //$NON-NLS-1$
                node.setAttribute("id", security.getLevel2Feed().getId()); //$NON-NLS-1$
                if (security.getLevel2Feed().getExchange() != null)
                    node.setAttribute("exchange", security.getLevel2Feed().getExchange()); //$NON-NLS-1$
                node.appendChild(document.createTextNode(security.getLevel2Feed().getSymbol()));
                feedsNode.appendChild(node);
            }
            if (security.getHistoryFeed() != null)
            {
                node = document.createElement("history"); //$NON-NLS-1$
                node.setAttribute("id", security.getHistoryFeed().getId()); //$NON-NLS-1$
                if (security.getHistoryFeed().getExchange() != null)
                    node.setAttribute("exchange", security.getHistoryFeed().getExchange()); //$NON-NLS-1$
                node.appendChild(document.createTextNode(security.getHistoryFeed().getSymbol()));
                feedsNode.appendChild(node);
            }
        }
        
        if (security.getTradeSource() != null)
        {
            TradeSource source = security.getTradeSource();
            
            Element feedsNode = document.createElement("tradeSource"); //$NON-NLS-1$
            feedsNode.setAttribute("id", source.getTradingProviderId()); //$NON-NLS-1$
            if (source.getExchange() != null)
                feedsNode.setAttribute("exchange", source.getExchange()); //$NON-NLS-1$
            element.appendChild(feedsNode);

            if (!source.getSymbol().equals("")) //$NON-NLS-1$
            {
                node = document.createElement("symbol"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(source.getSymbol()));
                feedsNode.appendChild(node);
            }
            if (source.getAccountId() != null)
            {
                node = document.createElement("account"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(String.valueOf(source.getAccountId())));
                feedsNode.appendChild(node);
            }
            node = document.createElement("quantity"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(source.getQuantity())));
            feedsNode.appendChild(node);
        }
        
        if (security.getQuote() != null)
        {
            Quote quote = security.getQuote();
            Node quoteNode = document.createElement("quote"); //$NON-NLS-1$

            if (quote.getDate() != null)
            {
                node = document.createElement("date"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(dateTimeFormat.format(quote.getDate())));
                quoteNode.appendChild(node);
            }
            node = document.createElement("last"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(quote.getLast())));
            quoteNode.appendChild(node);
            node = document.createElement("bid"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(quote.getBid())));
            quoteNode.appendChild(node);
            node = document.createElement("ask"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(quote.getAsk())));
            quoteNode.appendChild(node);
            node = document.createElement("bidSize"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(quote.getBidSize())));
            quoteNode.appendChild(node);
            node = document.createElement("askSize"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(quote.getAskSize())));
            quoteNode.appendChild(node);
            node = document.createElement("volume"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(quote.getVolume())));
            quoteNode.appendChild(node);
            
            element.appendChild(quoteNode);
        }

        Node dataNode = document.createElement("data"); //$NON-NLS-1$
        element.appendChild(dataNode);
        
        if (security.getOpen() != null)
        {
            node = document.createElement("open"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(security.getOpen())));
            dataNode.appendChild(node);
        }
        if (security.getHigh() != null)
        {
            node = document.createElement("high"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(security.getHigh())));
            dataNode.appendChild(node);
        }
        if (security.getLow() != null)
        {
            node = document.createElement("low"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(security.getLow())));
            dataNode.appendChild(node);
        }
        if (security.getClose() != null)
        {
            node = document.createElement("close"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(String.valueOf(security.getClose())));
            dataNode.appendChild(node);
        }

        node = document.createElement("comment"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(security.getComment()));
        element.appendChild(node);
        
        for (Iterator iter = security.getSplits().iterator(); iter.hasNext(); )
        {
            Split split = (Split)iter.next();
            node = document.createElement("split"); //$NON-NLS-1$
            node.setAttribute("date", dateTimeFormat.format(split.getDate())); //$NON-NLS-1$
            node.setAttribute("fromQuantity", String.valueOf(split.getFromQuantity())); //$NON-NLS-1$
            node.setAttribute("toQuantity", String.valueOf(split.getToQuantity())); //$NON-NLS-1$
            element.appendChild(node);
        }

        for (Iterator iter = security.getDividends().iterator(); iter.hasNext(); )
        {
            Dividend dividend = (Dividend)iter.next();
            node = document.createElement("dividend"); //$NON-NLS-1$
            node.setAttribute("date", dateTimeFormat.format(dividend.getDate())); //$NON-NLS-1$
            node.setAttribute("value", String.valueOf(dividend.getValue())); //$NON-NLS-1$
            element.appendChild(node);
        }
    }
    
    private void saveSecurities()
    {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setErrorHandler(errorHandler);
            Document document = builder.getDOMImplementation().createDocument(null, "data", null); //$NON-NLS-1$

            Element root = document.getDocumentElement();
            root.setAttribute("nextId", String.valueOf(securitiesNextId)); //$NON-NLS-1$
            root.setAttribute("nextGroupId", String.valueOf(securitiesGroupNextId)); //$NON-NLS-1$
            
            for (Iterator iter = allSecurityGroups().iterator(); iter.hasNext(); )
            {
                SecurityGroup group = (SecurityGroup)iter.next();
                if (group.getParentGroup() == null)
                    saveSecurityGroup(group, document, root);
            }
            
            for (Iterator iter = allSecurities().iterator(); iter.hasNext(); )
            {
                Security security = (Security)iter.next();
                if (security.getGroup() == null)
                    saveSecurity(security, document, root);
            }

            saveDocument(document, "", "securities.xml"); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }
    
    private Watchlist loadWatchlist(NodeList node)
    {
        int itemIndex = 1;
        Watchlist watchlist = new Watchlist(new Integer(Integer.parseInt(((Node)node).getAttributes().getNamedItem("id").getNodeValue()))); //$NON-NLS-1$
        
        for (int i = 0; i < node.getLength(); i++)
        {
            Node item = node.item(i);
            String nodeName = item.getNodeName();
            Node value = item.getFirstChild();
            if (value != null)
            {
                if (nodeName.equalsIgnoreCase("title")) //$NON-NLS-1$
                    watchlist.setDescription(value.getNodeValue());
                else if (nodeName.equalsIgnoreCase("style")) //$NON-NLS-1$
                    watchlist.setStyle(Integer.parseInt(value.getNodeValue()));
                else if (nodeName.equalsIgnoreCase("feed")) //$NON-NLS-1$
                    watchlist.setDefaultFeed(value.getNodeValue());
                else if (nodeName.equals("currency")) //$NON-NLS-1$
                    watchlist.setCurrency(Currency.getInstance(value.getNodeValue()));
            }
            if (nodeName.equalsIgnoreCase("columns")) //$NON-NLS-1$
            {
                List list = new ArrayList();
                NodeList columnList = item.getChildNodes();
                for (int c = 0; c < columnList.getLength(); c++)
                {
                    item = columnList.item(c);
                    nodeName = item.getNodeName();
                    if (nodeName.equalsIgnoreCase("column")) //$NON-NLS-1$
                    {
                        WatchlistColumn column = new WatchlistColumn();
                        if (item.getAttributes().getNamedItem("id") != null) //$NON-NLS-1$
                            column.setId(item.getAttributes().getNamedItem("id").getNodeValue()); //$NON-NLS-1$
                        else if (item.getAttributes().getNamedItem("class") != null) //$NON-NLS-1$
                        {
                            String id = item.getAttributes().getNamedItem("class").getNodeValue(); //$NON-NLS-1$
                            id = id.substring(id.lastIndexOf('.') + 1);
                            column.setId("watchlist." + id.substring(0, 1).toLowerCase() + id.substring(1)); //$NON-NLS-1$
                        }
                        list.add(column);
                    }
                }
                watchlist.setColumns(list);
            }
            else if (nodeName.equalsIgnoreCase("items")) //$NON-NLS-1$
            {
                List list = new ArrayList();
                NodeList itemList = item.getChildNodes();
                for (int c = 0; c < itemList.getLength(); c++)
                {
                    item = itemList.item(c);
                    nodeName = item.getNodeName();
                    if (nodeName.equalsIgnoreCase("security")) //$NON-NLS-1$
                    {
                        String id = ((Node)item).getAttributes().getNamedItem("id").getNodeValue(); //$NON-NLS-1$
                        Security security = (Security)load(Security.class, new Integer(id));
                        if (security == null)
                        {
                            log.warn("Cannot load security (id=" + id + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                            continue;
                        }
                        
                        WatchlistItem watchlistItem = new WatchlistItem(new Integer(itemIndex++));
                        watchlistItem.setParent(watchlist);
                        watchlistItem.setSecurity(security);

                        int alertIndex = 1;
                        NodeList quoteList = item.getChildNodes();
                        for (int q = 0; q < quoteList.getLength(); q++)
                        {
                            item = quoteList.item(q);
                            nodeName = item.getNodeName();
                            value = item.getFirstChild();
                            if (value != null)
                            {
                                if (nodeName.equalsIgnoreCase("position")) //$NON-NLS-1$
                                    watchlistItem.setPosition(Integer.parseInt(value.getNodeValue()));
                                else if (nodeName.equalsIgnoreCase("paid")) //$NON-NLS-1$
                                    watchlistItem.setPaidPrice(Double.parseDouble(value.getNodeValue()));
                            }
                            if (nodeName.equalsIgnoreCase("alert")) //$NON-NLS-1$
                            {
                                Alert alert = new Alert(new Integer(alertIndex++));
                                alert.setPluginId(item.getAttributes().getNamedItem("pluginId").getNodeValue()); //$NON-NLS-1$
                                if (item.getAttributes().getNamedItem("lastSeen") != null) //$NON-NLS-1$
                                {
                                    try {
                                        alert.setLastSeen(dateTimeFormat.parse(item.getAttributes().getNamedItem("lastSeen").getNodeValue())); //$NON-NLS-1$
                                    } catch(Exception e) {
                                        log.warn(e.toString());
                                    }
                                }
                                if (item.getAttributes().getNamedItem("popup") != null) //$NON-NLS-1$
                                    alert.setPopup(new Boolean(item.getAttributes().getNamedItem("popup").getNodeValue()).booleanValue()); //$NON-NLS-1$
                                if (item.getAttributes().getNamedItem("hilight") != null) //$NON-NLS-1$
                                    alert.setHilight(new Boolean(item.getAttributes().getNamedItem("hilight").getNodeValue()).booleanValue()); //$NON-NLS-1$

                                NodeList paramList = item.getChildNodes();
                                for (int p = 0; p < paramList.getLength(); p++)
                                {
                                    item = paramList.item(p);
                                    nodeName = item.getNodeName();
                                    value = item.getFirstChild();
                                    if (value != null)
                                    {
                                        if (nodeName.equalsIgnoreCase("param")) //$NON-NLS-1$
                                        {
                                            String key = ((Node)item).getAttributes().getNamedItem("key").getNodeValue(); //$NON-NLS-1$
                                            alert.getParameters().put(key, value.getNodeValue());
                                        }
                                    }
                                }
                                
                                watchlistItem.getAlerts().add(alert);
                            }
                        }

                        list.add(watchlistItem);
                    }
                }
                watchlist.setItems(list);
            }
        }
        
        watchlist.clearChanged();
        
        return watchlist;
    }
    
    private void saveWatchlists()
    {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setErrorHandler(errorHandler);
            Document document = builder.getDOMImplementation().createDocument(null, "data", null); //$NON-NLS-1$

            Element root = document.getDocumentElement();
            root.setAttribute("nextId", String.valueOf(watchlistsNextId)); //$NON-NLS-1$
            
            for (Iterator iter = watchlistsMap.values().iterator(); iter.hasNext(); )
            {
                Watchlist watchlist = (Watchlist)iter.next(); 

                Element element = document.createElement("watchlist"); //$NON-NLS-1$
                element.setAttribute("id", String.valueOf(watchlist.getId())); //$NON-NLS-1$
                root.appendChild(element);

                Element node = document.createElement("title"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(watchlist.getDescription()));
                element.appendChild(node);
                node = document.createElement("style"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(String.valueOf(watchlist.getStyle())));
                element.appendChild(node);
                if (watchlist.getCurrency() != null)
                {
                    node = document.createElement("currency"); //$NON-NLS-1$
                    node.appendChild(document.createTextNode(watchlist.getCurrency().getCurrencyCode()));
                    element.appendChild(node);
                }
                if (watchlist.getDefaultFeed() != null)
                {
                    node = document.createElement("feed"); //$NON-NLS-1$
                    node.appendChild(document.createTextNode(watchlist.getDefaultFeed()));
                    element.appendChild(node);
                }

                Element columnsNode = document.createElement("columns"); //$NON-NLS-1$
                element.appendChild(columnsNode);

                for (Iterator iter2 = watchlist.getColumns().iterator(); iter2.hasNext(); )
                {
                    WatchlistColumn column = (WatchlistColumn)iter2.next();

                    Element columnNode = document.createElement("column"); //$NON-NLS-1$
                    columnNode.setAttribute("id", column.getId()); //$NON-NLS-1$
                    columnsNode.appendChild(columnNode);
                }

                Element itemsNode = document.createElement("items"); //$NON-NLS-1$
                element.appendChild(itemsNode);

                int itemIndex = 1;
                for (Iterator itemIter = watchlist.getItems().iterator(); itemIter.hasNext(); )
                {
                    WatchlistItem item = (WatchlistItem)itemIter.next();
                    item.setId(new Integer(itemIndex++));
                    item.setParent(watchlist);
                    item.setRepository(this);

                    Element itemNode = document.createElement("security"); //$NON-NLS-1$
                    itemNode.setAttribute("id", String.valueOf(item.getSecurity().getId())); //$NON-NLS-1$
                    itemsNode.appendChild(itemNode);

                    if (item.getPosition() != null && item.getPosition().intValue() != 0)
                    {
                        node = document.createElement("position"); //$NON-NLS-1$
                        node.appendChild(document.createTextNode(String.valueOf(item.getPosition())));
                        itemNode.appendChild(node);
                    }
                    if (item.getPaidPrice() != null && item.getPaidPrice().doubleValue() != 0)
                    {
                        node = document.createElement("paid"); //$NON-NLS-1$
                        node.appendChild(document.createTextNode(String.valueOf(item.getPaidPrice())));
                        itemNode.appendChild(node);
                    }

                    int alertIndex = 1;
                    for (Iterator alertIter = item.getAlerts().iterator(); alertIter.hasNext(); )
                    {
                        Alert alert = (Alert)alertIter.next();
                        alert.setId(new Integer(alertIndex++));

                        Element alertNode = document.createElement("alert"); //$NON-NLS-1$
                        alertNode.setAttribute("pluginId", alert.getPluginId()); //$NON-NLS-1$
                        if (alert.getLastSeen() != null)
                            alertNode.setAttribute("lastSeen", dateTimeFormat.format(alert.getLastSeen())); //$NON-NLS-1$
                        alertNode.setAttribute("popup", String.valueOf(alert.isPopup())); //$NON-NLS-1$
                        alertNode.setAttribute("hilight", String.valueOf(alert.isHilight())); //$NON-NLS-1$
                        itemNode.appendChild(alertNode);

                        for (Iterator paramIter = alert.getParameters().keySet().iterator(); paramIter.hasNext(); )
                        {
                            String key = (String)paramIter.next();
                            
                            node = document.createElement("param"); //$NON-NLS-1$
                            node.setAttribute("key", key); //$NON-NLS-1$
                            node.appendChild(document.createTextNode((String)alert.getParameters().get(key)));
                            alertNode.appendChild(node);
                        }
                    }
                }
            }
            
            saveDocument(document, "", "watchlists.xml"); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    private NewsItem loadNews(NodeList node)
    {
        NewsItem news = new NewsItem();
        
        if (((Node)node).getAttributes().getNamedItem("security") != null) //$NON-NLS-1$
        {
            Security security = getSecurity(((Node)node).getAttributes().getNamedItem("security").getNodeValue()); //$NON-NLS-1$
            if (security != null)
                news.addSecurity(security);
        }
        if (((Node)node).getAttributes().getNamedItem("readed") != null) //$NON-NLS-1$
            news.setReaded(new Boolean(((Node)node).getAttributes().getNamedItem("readed").getNodeValue()).booleanValue()); //$NON-NLS-1$
        
        for (int i = 0; i < node.getLength(); i++)
        {
            Node item = node.item(i);
            String nodeName = item.getNodeName();
            Node value = item.getFirstChild();
            if (value != null)
            {
                if (nodeName.equalsIgnoreCase("date") == true) //$NON-NLS-1$
                {
                    try {
                        news.setDate(dateTimeFormat.parse(value.getNodeValue()));
                    } catch (Exception e) {
                        log.warn(e.toString());
                    }
                }
                else if (nodeName.equals("description")) //$NON-NLS-1$
                    news.setTitle(value.getNodeValue());
                else if (nodeName.equals("source")) //$NON-NLS-1$
                    news.setSource(value.getNodeValue());
                else if (nodeName.equals("url")) //$NON-NLS-1$
                    news.setUrl(value.getNodeValue());
                else if (nodeName.equals("security")) //$NON-NLS-1$
                {
                    Security security = getSecurity(value.getNodeValue());
                    if (security != null)
                        news.addSecurity(security);
                }
            }
        }
        
        news.clearChanged();
        
        return news;
    }
    
    private void saveNews()
    {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setErrorHandler(errorHandler);
            Document document = builder.getDOMImplementation().createDocument(null, "data", null); //$NON-NLS-1$

            Element root = document.getDocumentElement();
            
            for (Iterator iter = allNews().iterator(); iter.hasNext(); )
            {
                NewsItem news = (NewsItem)iter.next(); 

                Element element = document.createElement("news"); //$NON-NLS-1$
                element.setAttribute("readed", String.valueOf(news.isReaded())); //$NON-NLS-1$
                root.appendChild(element);

                Element node = document.createElement("date"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(dateTimeFormat.format(news.getDate())));
                element.appendChild(node);
                node = document.createElement("description"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(news.getTitle()));
                element.appendChild(node);
                node = document.createElement("source"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(news.getSource()));
                element.appendChild(node);
                node = document.createElement("url"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(news.getUrl()));
                element.appendChild(node);
                
                Object[] o = news.getSecurities().toArray();
                for (int i = 0; i < o.length; i++)
                {
                    node = document.createElement("security"); //$NON-NLS-1$
                    node.appendChild(document.createTextNode(String.valueOf(((Security)o[i]).getId())));
                    element.appendChild(node);
                }
            }
            
            saveDocument(document, "", "news.xml"); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    private AccountGroup loadAccountGroup(NodeList node, AccountGroup parent)
    {
        AccountGroup group = new AccountGroup(new Integer(Integer.parseInt(((Node)node).getAttributes().getNamedItem("id").getNodeValue()))); //$NON-NLS-1$
        if (parent != null)
        {
            group.setParent(parent);
            parent.getGroups().add(group);
        }
        
        for (int i = 0; i < node.getLength(); i++)
        {
            Node item = node.item(i);
            String nodeName = item.getNodeName();
            Node value = item.getFirstChild();
            if (value != null)
            {
                if (nodeName.equals("description")) //$NON-NLS-1$
                    group.setDescription(value.getNodeValue());
            }
            if (nodeName.equals("account")) //$NON-NLS-1$
                loadAccount(item.getChildNodes(), group);
        }
        
        group.clearChanged();
        accountGroupMap.put(group.getId(), group);
        allAccountGroups().add(group);
        
        return group;
    }

    private Account loadAccount(NodeList node, AccountGroup group)
    {
        Integer id = new Integer(Integer.parseInt(((Node)node).getAttributes().getNamedItem("id").getNodeValue())); //$NON-NLS-1$
        String pluginId = ""; //$NON-NLS-1$
        if (((Node)node).getAttributes().getNamedItem("pluginId") != null) //$NON-NLS-1$
            pluginId = ((Node)node).getAttributes().getNamedItem("pluginId").getNodeValue(); //$NON-NLS-1$
        if (pluginId.equals("")) //$NON-NLS-1$
            pluginId = "net.sourceforge.eclipsetrader.accounts.simple"; //$NON-NLS-1$
        PersistentPreferenceStore preferenceStore = new PersistentPreferenceStore();
        List transactions = new ArrayList();
        
        for (int i = 0; i < node.getLength(); i++)
        {
            Node item = node.item(i);
            String nodeName = item.getNodeName();
            Node value = item.getFirstChild();
            if (nodeName.equals("transaction")) //$NON-NLS-1$
            {
                Transaction transaction = new Transaction(new Integer(Integer.parseInt(item.getAttributes().getNamedItem("id").getNodeValue()))); //$NON-NLS-1$
                
                NodeList childs = item.getChildNodes();
                for (int ii = 0; ii < childs.getLength(); ii++)
                {
                    item = childs.item(ii);
                    nodeName = item.getNodeName();
                    value = item.getFirstChild();
                    if (value != null)
                    {
                        if (nodeName.equals("date")) //$NON-NLS-1$
                        {
                            try {
                                transaction.setDate(dateTimeFormat.parse(value.getNodeValue()));
                            } catch(Exception e) {
                                log.error(e.toString(), e);
                                break;
                            }
                        }
                        else if (nodeName.equals("security")) //$NON-NLS-1$
                        {
                            transaction.setSecurity((Security)load(Security.class, new Integer(Integer.parseInt(value.getNodeValue()))));
                            if (transaction.getSecurity() == null)
                                log.warn("Cannot load security (id=" + value.getNodeValue() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                        else if (nodeName.equals("price")) //$NON-NLS-1$
                            transaction.setPrice(Double.parseDouble(value.getNodeValue()));
                        else if (nodeName.equals("quantity")) //$NON-NLS-1$
                            transaction.setQuantity(Integer.parseInt(value.getNodeValue()));
                        else if (nodeName.equals("expenses")) //$NON-NLS-1$
                            transaction.setExpenses(Double.parseDouble(value.getNodeValue()));
                    }
                    if (nodeName.equalsIgnoreCase("param") == true) //$NON-NLS-1$
                    {
                        NamedNodeMap map = item.getAttributes();
                        transaction.getParams().put(map.getNamedItem("key").getNodeValue(), map.getNamedItem("value").getNodeValue()); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
                if (transaction.getSecurity() != null)
                    transactions.add(transaction);
            }
            else if (value != null)
            {
                if (nodeName.equals("description")) //$NON-NLS-1$
                    ;
                else if (nodeName.equals("currency")) //$NON-NLS-1$
                    ;
                else if (nodeName.equals("initialBalance")) //$NON-NLS-1$
                    ;
                else
                    preferenceStore.setValue(nodeName, value.getNodeValue());
            }
        }

        Collections.sort(transactions, new Comparator() {
            public int compare(Object arg0, Object arg1)
            {
                return ((Transaction)arg0).getDate().compareTo(((Transaction)arg1).getDate());
            }
        });
        
        Account account = CorePlugin.createAccount(pluginId, preferenceStore, transactions);
        account.setId(id);
        account.setPluginId(pluginId);
        account.setGroup(group);
        
        for (int i = 0; i < node.getLength(); i++)
        {
            Node item = node.item(i);
            String nodeName = item.getNodeName();
            Node value = item.getFirstChild();
            if (value != null)
            {
                if (nodeName.equals("description")) //$NON-NLS-1$
                    account.setDescription(value.getNodeValue());
                else if (nodeName.equals("currency")) //$NON-NLS-1$
                    account.setCurrency(Currency.getInstance(value.getNodeValue()));
                else if (nodeName.equals("initialBalance")) //$NON-NLS-1$
                    account.setInitialBalance(Double.parseDouble(value.getNodeValue()));
            }
        }
        
        account.clearChanged();
        accountMap.put(account.getId(), account);
        allAccounts().add(account);

        return account;
    }
    
    private void saveGroup(AccountGroup group, Document document, Element root)
    {
        Element element = document.createElement("group"); //$NON-NLS-1$
        element.setAttribute("id", String.valueOf(group.getId())); //$NON-NLS-1$
        root.appendChild(element);

        Element node = document.createElement("description"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(group.getDescription()));
        element.appendChild(node);

        for (Iterator iter = group.getGroups().iterator(); iter.hasNext(); )
        {
            AccountGroup grp = (AccountGroup)iter.next();
            saveGroup(grp, document, element);
        }

        for (Iterator iter = group.getAccounts().iterator(); iter.hasNext(); )
        {
            Account account = (Account)iter.next(); 
            saveAccount(account, document, element);
        }
    }

    private void saveAccount(Account account, Document document, Element root)
    {
        Element element = document.createElement("account"); //$NON-NLS-1$
        element.setAttribute("id", String.valueOf(account.getId())); //$NON-NLS-1$
        element.setAttribute("pluginId", String.valueOf(account.getPluginId())); //$NON-NLS-1$
        root.appendChild(element);
        
        Element node = document.createElement("description"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(account.getDescription()));
        element.appendChild(node);
        if (account.getCurrency() != null)
        {
            node = document.createElement("currency"); //$NON-NLS-1$
            node.appendChild(document.createTextNode(account.getCurrency().getCurrencyCode()));
            element.appendChild(node);
        }
        node = document.createElement("initialBalance"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(String.valueOf(account.getInitialBalance())));
        element.appendChild(node);

        String[] names = account.getPreferenceStore().preferenceNames();
        for (int i = 0; i < names.length; i++)
        {
            node = document.createElement(names[i]);
            node.appendChild(document.createTextNode(account.getPreferenceStore().getString(names[i])));
            element.appendChild(node);
        }

        int transactionId = 0;
        for (Iterator iter = account.getTransactions().iterator(); iter.hasNext(); )
        {
            Transaction transaction = (Transaction)iter.next();
            if (transaction.getId() != null)
                transactionId = Math.max(transactionId, transaction.getId().intValue());
        }
        for (Iterator iter = account.getTransactions().iterator(); iter.hasNext(); )
        {
            Transaction transaction = (Transaction)iter.next();
            if (transaction.getId() == null)
                transaction.setId(new Integer(++transactionId));
            saveTransaction(transaction, document, element);
        }
    }

    private void saveTransaction(Transaction transaction, Document document, Element root)
    {
        Element element = document.createElement("transaction"); //$NON-NLS-1$
        element.setAttribute("id", String.valueOf(transaction.getId())); //$NON-NLS-1$
        root.appendChild(element);
        
        Element node = document.createElement("date"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(dateTimeFormat.format(transaction.getDate())));
        element.appendChild(node);
        node = document.createElement("security"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(String.valueOf(transaction.getSecurity().getId())));
        element.appendChild(node);
        node = document.createElement("price"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(String.valueOf(transaction.getPrice())));
        element.appendChild(node);
        node = document.createElement("quantity"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(String.valueOf(transaction.getQuantity())));
        element.appendChild(node);
        node = document.createElement("expenses"); //$NON-NLS-1$
        node.appendChild(document.createTextNode(String.valueOf(transaction.getExpenses())));
        element.appendChild(node);

        for (Iterator paramIter = transaction.getParams().keySet().iterator(); paramIter.hasNext(); )
        {
            String key = (String)paramIter.next();
            node = document.createElement("param"); //$NON-NLS-1$
            node.setAttribute("key", key); //$NON-NLS-1$
            node.setAttribute("value", (String)transaction.getParams().get(key)); //$NON-NLS-1$
            element.appendChild(node);
        }
    }
    
    private void saveAccounts()
    {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setErrorHandler(errorHandler);
            Document document = builder.getDOMImplementation().createDocument(null, "data", null); //$NON-NLS-1$

            Element root = document.getDocumentElement();
            root.setAttribute("nextId", String.valueOf(accountNextId)); //$NON-NLS-1$
            root.setAttribute("nextGroupId", String.valueOf(accountGroupNextId)); //$NON-NLS-1$
            
            for (Iterator iter = accountGroupMap.values().iterator(); iter.hasNext(); )
            {
                AccountGroup group = (AccountGroup)iter.next();
                if (group.getParent() != null)
                    continue;
                saveGroup(group, document, root);
            }
            
            for (Iterator iter = accountMap.values().iterator(); iter.hasNext(); )
            {
                Account account = (Account)iter.next();
                if (account.getGroup() != null)
                    continue;
                saveAccount(account, document, root);
            }

            saveDocument(document, "", "accounts.xml"); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    private Event loadEvent(NodeList node)
    {
        Event event = new Event(new Integer(allEvents().size() + 1));
        
        if (((Node)node).getAttributes().getNamedItem("security") != null) //$NON-NLS-1$
        {
            Security security = getSecurity(((Node)node).getAttributes().getNamedItem("security").getNodeValue()); //$NON-NLS-1$
            event.setSecurity(security);
        }
        
        for (int i = 0; i < node.getLength(); i++)
        {
            Node item = node.item(i);
            String nodeName = item.getNodeName();
            Node value = item.getFirstChild();
            if (value != null)
            {
                if (nodeName.equalsIgnoreCase("date") == true) //$NON-NLS-1$
                {
                    try {
                        event.setDate(dateTimeFormat.parse(value.getNodeValue()));
                    } catch (Exception e) {
                        log.warn(e.toString());
                    }
                }
                else if (nodeName.equals("message")) //$NON-NLS-1$
                    event.setMessage(value.getNodeValue());
                else if (nodeName.equals("longMessage")) //$NON-NLS-1$
                    event.setLongMessage(value.getNodeValue());
            }
        }
        
        event.clearChanged();
        
        return event;
    }

    private Order loadOrder(NodeList node)
    {
        Order order = new Order(new Integer(Integer.parseInt(((Node)node).getAttributes().getNamedItem("id").getNodeValue()))); //$NON-NLS-1$

        order.setSecurity(getSecurity(((Node)node).getAttributes().getNamedItem("security").getNodeValue())); //$NON-NLS-1$
        if (order.getSecurity() == null)
            log.warn("Cannot load security (id=" + ((Node)node).getAttributes().getNamedItem("security").getNodeValue() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        
        String pluginId = ((Node)node).getAttributes().getNamedItem("pluginId").getNodeValue(); //$NON-NLS-1$
        order.setProvider(CorePlugin.createTradeSourcePlugin(pluginId));
        if (order.getProvider() == null)
            log.warn("Cannot load trade source '" + pluginId + "' for order (id=" + String.valueOf(order.getId()) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        order.setPluginId(pluginId);

        for (int i = 0; i < node.getLength(); i++)
        {
            Node item = node.item(i);
            String nodeName = item.getNodeName();
            Node value = item.getFirstChild();
            if (value != null)
            {
                if (nodeName.equalsIgnoreCase("date") == true) //$NON-NLS-1$
                {
                    try {
                        order.setDate(dateTimeFormat.parse(value.getNodeValue()));
                    } catch (Exception e) {
                        log.warn(e.toString());
                    }
                }
                else if (nodeName.equals("exchange")) //$NON-NLS-1$
                    order.setExchange(new OrderRoute(item.getAttributes().getNamedItem("id").getNodeValue(), value.getNodeValue())); //$NON-NLS-1$
                else if (nodeName.equals("orderId")) //$NON-NLS-1$
                    order.setOrderId(value.getNodeValue());
                else if (nodeName.equals("side")) //$NON-NLS-1$
                    order.setSide(new OrderSide(Integer.parseInt(value.getNodeValue())));
                else if (nodeName.equals("type")) //$NON-NLS-1$
                    order.setType(new OrderType(Integer.parseInt(value.getNodeValue())));
                else if (nodeName.equals("quantity")) //$NON-NLS-1$
                    order.setQuantity(Integer.parseInt(value.getNodeValue()));
                else if (nodeName.equals("price")) //$NON-NLS-1$
                    order.setPrice(new Double(value.getNodeValue()).doubleValue());
                else if (nodeName.equals("stopPrice")) //$NON-NLS-1$
                    order.setStopPrice(new Double(value.getNodeValue()).doubleValue());
                else if (nodeName.equals("filledQuantity")) //$NON-NLS-1$
                    order.setFilledQuantity(Integer.parseInt(value.getNodeValue()));
                else if (nodeName.equals("averagePrice")) //$NON-NLS-1$
                    order.setAveragePrice(new Double(value.getNodeValue()).doubleValue());
                else if (nodeName.equals("validity")) //$NON-NLS-1$
                    order.setValidity(new OrderValidity(Integer.parseInt(value.getNodeValue())));
                else if (nodeName.equals("status")) //$NON-NLS-1$
                    order.setStatus(new OrderStatus(Integer.parseInt(value.getNodeValue())));
                else if (nodeName.equals("account")) //$NON-NLS-1$
                {
                    order.setAccount((Account)this.accountMap.get(new Integer(value.getNodeValue())));
                    if (order.getAccount() != null)
                        log.warn("Cannot load account (id=" + value.getNodeValue() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                else if (nodeName.equals("text")) //$NON-NLS-1$
                    order.setText(value.getNodeValue());
                else if (nodeName.equals("message")) //$NON-NLS-1$
                    order.setMessage(value.getNodeValue());
            }
            if (nodeName.equalsIgnoreCase("param") == true) //$NON-NLS-1$
            {
                NamedNodeMap map = item.getAttributes();
                order.getParams().put(map.getNamedItem("key").getNodeValue(), map.getNamedItem("value").getNodeValue()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        
        order.clearChanged();
        
        return order;
    }
    
    private void saveOrders()
    {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setErrorHandler(errorHandler);
            Document document = builder.getDOMImplementation().createDocument(null, "data", null); //$NON-NLS-1$

            Element root = document.getDocumentElement();
            root.setAttribute("nextId", String.valueOf(orderNextId)); //$NON-NLS-1$
            
            for (Iterator iter = allOrders().iterator(); iter.hasNext(); )
            {
                Order order = (Order)iter.next(); 

                Element element = document.createElement("order"); //$NON-NLS-1$
                element.setAttribute("id", String.valueOf(order.getId())); //$NON-NLS-1$
                element.setAttribute("pluginId", order.getPluginId()); //$NON-NLS-1$
                element.setAttribute("security", String.valueOf(order.getSecurity().getId())); //$NON-NLS-1$
                root.appendChild(element);

                Element node = document.createElement("date"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(dateTimeFormat.format(order.getDate())));
                element.appendChild(node);
                if (order.getExchange() != null)
                {
                    node = document.createElement("exchange"); //$NON-NLS-1$
                    node.setAttribute("id", order.getExchange().getId()); //$NON-NLS-1$
                    node.appendChild(document.createTextNode(order.getExchange().toString()));
                    element.appendChild(node);
                }
                node = document.createElement("orderId"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(order.getOrderId()));
                element.appendChild(node);
                node = document.createElement("side"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(String.valueOf(order.getSide())));
                element.appendChild(node);
                node = document.createElement("type"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(String.valueOf(order.getType())));
                element.appendChild(node);
                node = document.createElement("quantity"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(String.valueOf(order.getQuantity())));
                element.appendChild(node);
                node = document.createElement("price"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(String.valueOf(order.getPrice())));
                element.appendChild(node);
                node = document.createElement("stopPrice"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(String.valueOf(order.getStopPrice())));
                element.appendChild(node);
                node = document.createElement("filledQuantity"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(String.valueOf(order.getFilledQuantity())));
                element.appendChild(node);
                node = document.createElement("averagePrice"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(String.valueOf(order.getAveragePrice())));
                element.appendChild(node);
                if (order.getValidity() != null)
                {
                    node = document.createElement("validity"); //$NON-NLS-1$
                    node.appendChild(document.createTextNode(String.valueOf(order.getValidity())));
                    element.appendChild(node);
                }
                node = document.createElement("status"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(String.valueOf(order.getStatus())));
                element.appendChild(node);
                if (order.getAccount() != null)
                {
                    node = document.createElement("account"); //$NON-NLS-1$
                    node.appendChild(document.createTextNode(String.valueOf(order.getAccount().getId())));
                    element.appendChild(node);
                }
                if (order.getText() != null)
                {
                    node = document.createElement("text"); //$NON-NLS-1$
                    node.appendChild(document.createTextNode(order.getText()));
                    element.appendChild(node);
                }
                if (order.getMessage() != null)
                {
                    node = document.createElement("message"); //$NON-NLS-1$
                    node.appendChild(document.createTextNode(order.getMessage()));
                    element.appendChild(node);
                }

                for (Iterator paramIter = order.getParams().keySet().iterator(); paramIter.hasNext(); )
                {
                    String key = (String)paramIter.next();
                    node = document.createElement("param"); //$NON-NLS-1$
                    node.setAttribute("key", key); //$NON-NLS-1$
                    node.setAttribute("value", (String)order.getParams().get(key)); //$NON-NLS-1$
                    element.appendChild(node);
                }
            }
            
            saveDocument(document, "", "orders.xml"); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }
    
    private void saveEvents()
    {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setErrorHandler(errorHandler);
            Document document = builder.getDOMImplementation().createDocument(null, "data", null); //$NON-NLS-1$

            Element root = document.getDocumentElement();
            
            for (Iterator iter = allEvents().iterator(); iter.hasNext(); )
            {
                Event event = (Event)iter.next(); 

                Element element = document.createElement("event"); //$NON-NLS-1$
                if (event.getSecurity() != null)
                    element.setAttribute("security", String.valueOf(event.getSecurity().getId())); //$NON-NLS-1$
                root.appendChild(element);

                Element node = document.createElement("date"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(dateTimeFormat.format(event.getDate())));
                element.appendChild(node);
                node = document.createElement("message"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(event.getMessage()));
                element.appendChild(node);
                node = document.createElement("longMessage"); //$NON-NLS-1$
                node.appendChild(document.createTextNode(event.getLongMessage()));
                element.appendChild(node);
            }
            
            saveDocument(document, "", "events.xml"); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    void saveDocument(Document document, String path, String name)
    {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try {
                factory.setAttribute("indent-number", new Integer(4)); //$NON-NLS-1$
            } catch(Exception e) {}
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
            transformer.setOutputProperty("{http\u003a//xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$
            DOMSource source = new DOMSource(document);
            
            File file = new File(Platform.getLocation().toFile(), path);
            file.mkdirs();
            file = new File(file, name);
            
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);
            out.flush();
            out.close();
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }
}
