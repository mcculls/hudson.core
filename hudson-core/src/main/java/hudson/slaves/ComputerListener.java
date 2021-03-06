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

package hudson.slaves;

import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.ExtensionPoint;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.remoting.Channel;

import java.io.IOException;

/**
 * Receives notifications about status changes of {@link Computer}s.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.246
 */
public abstract class ComputerListener implements ExtensionPoint {

    /**
     * Called before a {@link Computer} is marked online.
     *
     * <p> This enables you to do some work on all the slaves as they get
     * connected. Unlike {@link #onOnline(Computer, TaskListener)}, a failure to
     * carry out this function normally will prevent a computer from marked as
     * online.
     *
     * @param channel This is the channel object to talk to the slave. (This is
     * the same object returned by {@link Computer#getChannel()} once it's
     * connected.
     * @param root The directory where this slave stores files. The same as
     * {@link Node#getRootPath()}, except that method returns null until the
     * slave is connected. So this parameter is passed explicitly instead.
     * @param listener This is connected to the launch log of the computer.
     * Since this method is called synchronously from the thread that launches a
     * computer, if this method performs a time-consuming operation, this
     * listener should be notified of the progress. This is also a good listener
     * for reporting problems.
     *
     * @throws IOException Exceptions will be recorded to the listener, and the
     * computer will not become online.
     * @throws InterruptedException Exceptions will be recorded to the listener,
     * and the computer will not become online.
     *
     * @since 1.295
     * @see #onOnline(Computer, TaskListener)
     */
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener) throws IOException, InterruptedException {
    }

    /**
     * Called right after a {@link Computer} comes online.
     *
     * @deprecated as of 1.292 Use {@link #onOnline(Computer, TaskListener)}
     */
    public void onOnline(Computer c) {
    }

    /**
     * Called right after a {@link Computer} comes online.
     *
     * <p> This enables you to do some work on all the slaves as they get
     * connected.
     *
     * <p> Starting Hudson 1.312, this method is also invoked for the master,
     * not just for slaves.
     *
     * @param listener This is connected to the launch log of the computer.
     * Since this method is called synchronously from the thread that launches a
     * computer, if this method performs a time-consuming operation, this
     * listener should be notified of the progress. This is also a good listener
     * for reporting problems.
     *
     * @throws IOException Exceptions will be recorded to the listener. Note
     * that throwing an exception doesn't put the computer offline.
     * @throws InterruptedException Exceptions will be recorded to the listener.
     * Note that throwing an exception doesn't put the computer offline.
     *
     * @see #preOnline(Computer, Channel, FilePath, TaskListener)
     */
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
        // compatibility
        onOnline(c);
    }

    /**
     * Called right after a {@link Computer} went offline.
     */
    public void onOffline(Computer c) {
    }

    /**
     * Called when configuration of the node was changed, a node is
     * added/removed, etc.
     *
     * @since 1.377
     */
    public void onConfigurationChange() {
    }

    /**
     * Registers this {@link ComputerListener} so that it will start receiving
     * events.
     *
     * @deprecated as of 1.286 put {@link Extension} on your class to have it
     * auto-registered.
     */
    public final void register() {
        all().add(this);
    }

    /**
     * Unregisters this {@link ComputerListener} so that it will never receive
     * further events.
     *
     * <p> Unless {@link ComputerListener} is unregistered, it will never be a
     * subject of GC.
     */
    public final boolean unregister() {
        return all().remove(this);
    }

    /**
     * All the registered {@link ComputerListener}s.
     */
    public static ExtensionList<ComputerListener> all() {
        return Hudson.getInstance().getExtensionList(ComputerListener.class);
    }
}
