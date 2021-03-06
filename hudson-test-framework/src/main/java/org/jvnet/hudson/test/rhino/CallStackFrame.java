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

package org.jvnet.hudson.test.rhino;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebugFrame;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebuggableScript;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Stack frame.
 *
 * @author Kohsuke Kawaguchi
 */
public class CallStackFrame implements DebugFrame {

    /**
     * {@link JavaScriptDebugger} that this stack frame lives in.
     */
    public final JavaScriptDebugger owner;
    /**
     * The function being executed.
     */
    public final DebuggableScript fnOrScript;
    private Scriptable activation;
    private Scriptable thisObj;
    private Object[] args;
    private int line;

    public CallStackFrame(JavaScriptDebugger owner, DebuggableScript fnOrScript) {
        this.owner = owner;
        this.fnOrScript = fnOrScript;
    }

    public void onEnter(Context cx, Scriptable activation, Scriptable thisObj, Object[] args) {
        this.activation = activation;
        this.thisObj = thisObj;
        this.args = args;
        owner.callStack.add(this);
    }

    public void onExit(Context cx, boolean byThrow, Object resultOrException) {
        // can't simply call removeFirst, because due to tail call elimination,
        // intermediate frames can be dropped at any time
        // TODO: shouldn't it be suffice to just check the end?
        for (int i = owner.callStack.size() - 1; i >= 0; i--) {
            if (owner.callStack.get(i) == this) {
                owner.callStack.remove(i);
                break;
            }
        }

        activation = null;
        thisObj = null;
        args = null;
        line = -1;
    }

    public void onLineChange(Context cx, int lineNumber) {
        this.line = lineNumber;
    }

    public void onExceptionThrown(Context cx, Throwable ex) {
    }

    public void onDebuggerStatement(Context cx) {
    }

    /**
     * In-scope variables.
     */
    public SortedMap<String, Object> getVariables() {
        SortedMap<String, Object> r = new TreeMap<String, Object>();
        for (int i = fnOrScript.getParamAndVarCount() - 1; i >= 0; i--) {
            String name = fnOrScript.getParamOrVarName(i);
            r.put(name, activation.get(name, activation));
        }
        return r;
    }

    /**
     * Formats this call stack, arguments, and its local variables as a human
     * readable string.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(fnOrScript.getFunctionName());
        buf.append('(');
        for (int i = 0; i < args.length; i++) {
            if (i != 0) {
                buf.append(',');
            }
            buf.append(args[i]);
        }
        buf.append(')');
        buf.append("\n  at ").append(fnOrScript.getSourceName()).append('#').append(line);

        buf.append("\n  variables=").append(getVariables());

        return buf.toString();
    }
}
