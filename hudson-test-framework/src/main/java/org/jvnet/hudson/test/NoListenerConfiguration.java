/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
 *    Kohsuke Kawaguchi
 *
 *
 *******************************************************************************/ 

package org.jvnet.hudson.test;

import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.WebAppContext;

import javax.servlet.ServletContextListener;

import hudson.model.Hudson;

/**
 * Kills off {@link ServletContextListener}s loaded from web.xml.
 *
 * <p> This is so that the harness can create the {@link Hudson} object. with
 * the home directory of our choice.
 *
 * @author Kohsuke Kawaguchi
 */
final class NoListenerConfiguration implements Configuration {

    private WebAppContext context;

    public void setWebAppContext(WebAppContext context) {
        this.context = context;
    }

    public WebAppContext getWebAppContext() {
        return context;
    }

    public void configureClassLoader() throws Exception {
    }

    public void configureDefaults() throws Exception {
    }

    public void configureWebApp() throws Exception {
        context.setEventListeners(null);
    }

    public void deconfigureWebApp() throws Exception {
    }
}
