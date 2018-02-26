/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.sse;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.api.transport.PropertyScope.INBOUND;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

public class HttpEventStreamPublisherTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort serverPort = new DynamicPort("serverPort");
//    @Rule
//    public DynamicPort otherServerPort = new DynamicPort("otherServerPort");
    
    private ExecutorService executor = newSingleThreadExecutor();

    @Override
    protected String getConfigFile()
    {
        return "http-event-stream-publisher-config.xml";
    }
    
    private String getSseEndpointUrl()
    {
        return format("http://localhost:%s/sse", serverPort.getValue());
    }
    
    private String fetchEventStream() throws ClientProtocolException, IOException
    {
        final Response response = Request.Get(getSseEndpointUrl()).connectTimeout(1000).socketTimeout(30000).execute();
        final HttpResponse httpResponse = response.returnResponse();
        return IOUtils.toString(httpResponse.getEntity().getContent());
    }
    
    @Test
    public void cleanState() throws Exception
    {
        Future<String> submitted = executor.submit(new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                return fetchEventStream();
            }
        });
        
        Thread.sleep(5000);
        assertThat(submitted.get(), is("retry: 10000" + lineSeparator() +
                lineSeparator()));
    }

    @Test
    public void publishDefault() throws Exception
    {
        Future<String> submitted = executor.submit(new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                return fetchEventStream();
            }
        });
        
        Thread.sleep(5000);
        runFlow("publishDefault", getTestEvent(TEST_MESSAGE));

        assertThat(submitted.get(), is("retry: 10000" + lineSeparator() + 
                "id: 0" + lineSeparator() +
                "data: " + TEST_MESSAGE + lineSeparator() +
                lineSeparator()));
    }
    
    @Test
    public void publishDefaultMultiline() throws Exception
    {
        Future<String> submitted = executor.submit(new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                return fetchEventStream();
            }
        });
        
        Thread.sleep(5000);
        runFlow("publishDefault", getTestEvent(TEST_MESSAGE + lineSeparator() + TEST_MESSAGE));
        
        assertThat(submitted.get(), is("retry: 10000" + lineSeparator() + 
                "id: 0" + lineSeparator() +
                "data: " + TEST_MESSAGE + lineSeparator() +
                "data: " + TEST_MESSAGE + lineSeparator() +
                lineSeparator()));
    }
    
    @Test
    public void publishCustom() throws Exception
    {
        System.out.println(serverPort.getNumber());
        
        Future<String> submitted = executor.submit(new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                return fetchEventStream();
            }
        });
        
        Thread.sleep(5000);
        MuleEvent event = getTestEvent("");
        Map<String, Object> props = new HashMap<>();
        props.put("data", TEST_MESSAGE);
        props.put("event", "push");
        event.getMessage().addProperties(props, INBOUND);
        
        runFlow("publishCustom", event);

        assertThat(submitted.get(), is("retry: 10000" + lineSeparator() + 
                "id: 0" + lineSeparator() +
                "event: push" + lineSeparator() +
                "data: " + TEST_MESSAGE + lineSeparator() +
                lineSeparator()));
        assertThat(fetchEventStream(), is(""));
    }
    
}
