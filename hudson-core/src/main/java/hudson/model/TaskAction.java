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
 *    Kohsuke Kawaguchi, Jorg Heymans
 *
 *
 *******************************************************************************/ 

package hudson.model;

import hudson.console.AnnotatedLargeText;
import org.kohsuke.stapler.framework.io.LargeText;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.lang.ref.WeakReference;
import java.io.IOException;

import hudson.security.Permission;
import hudson.security.ACL;

/**
 * Partial {@link Action} implementation for those who kick some processing
 * asynchronously (such as SCM tagging.)
 *
 * <p> The class offers the basic set of functionality to do it.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.191
 * @see TaskThread
 */
public abstract class TaskAction extends AbstractModelObject implements Action {

    /**
     * If non-null, that means either the activitiy is in progress
     * asynchronously, or it failed unexpectedly and the thread is dead.
     */
    protected transient volatile TaskThread workerThread;
    /**
     * Hold the log of the tagging operation.
     */
    protected transient WeakReference<AnnotatedLargeText> log;

    /**
     * Gets the permission object that represents the permission to perform this
     * task.
     */
    protected abstract Permission getPermission();

    /**
     * Gets the {@link ACL} against which the permissions are checked.
     */
    protected abstract ACL getACL();

    /**
     * @deprecated as of 1.350 Use {@link #obtainLog()}, which returns the same
     * object in a more type-safe signature.
     */
    public LargeText getLog() {
        return obtainLog();
    }

    /**
     * Obtains the log file.
     *
     * <p> The default implementation get this from {@link #workerThread}, so
     * when it's complete, the log could be gone any time.
     *
     * <p> Derived classes that persist the text should override this method so
     * that it fetches the file from disk.
     */
    public AnnotatedLargeText obtainLog() {
        WeakReference<AnnotatedLargeText> l = log;
        if (l == null) {
            return null;
        }
        return l.get();
    }

    public String getSearchUrl() {
        return getUrlName();
    }

    public TaskThread getWorkerThread() {
        return workerThread;
    }

    /**
     * Handles incremental log output.
     */
    public void doProgressiveLog(StaplerRequest req, StaplerResponse rsp) throws IOException {
        AnnotatedLargeText text = obtainLog();
        if (text != null) {
            text.doProgressText(req, rsp);
            return;
        }
        rsp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Handles incremental log output.
     */
    public void doProgressiveHtml(StaplerRequest req, StaplerResponse rsp) throws IOException {
        AnnotatedLargeText text = obtainLog();
        if (text != null) {
            text.doProgressiveHtml(req, rsp);
            return;
        }
        rsp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Clears the error status.
     */
    public synchronized void doClearError(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        getACL().checkPermission(getPermission());

        if (workerThread != null && !workerThread.isRunning()) {
            workerThread = null;
        }
        rsp.sendRedirect(".");
    }
}
