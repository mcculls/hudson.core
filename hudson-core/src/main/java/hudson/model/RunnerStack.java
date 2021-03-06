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

package hudson.model;

import hudson.model.Run.Runner;

import java.util.Stack;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps track of {@link Runner}s that are currently executing on the given
 * thread (that can be either an {@link Executor} or threads that are
 * impersonating one.)
 *
 * @author Kohsuke Kawaguchi
 * @since 1.319
 */
final class RunnerStack {

    private final Map<Executor, Stack<Runner>> stack = new WeakHashMap<Executor, Stack<Runner>>();

    synchronized void push(Runner r) {
        Executor e = Executor.currentExecutor();
        Stack<Runner> s = stack.get(e);
        if (s == null) {
            stack.put(e, s = new Stack<Runner>());
        }
        s.push(r);
    }

    synchronized void pop() {
        Executor e = Executor.currentExecutor();
        Stack<Runner> s = stack.get(e);
        s.pop();
        if (s.isEmpty()) {
            stack.remove(e);
        }
    }

    synchronized Runner peek() {
        return stack.get(Executor.currentExecutor()).peek();
    }
    static final RunnerStack INSTANCE = new RunnerStack();
}
