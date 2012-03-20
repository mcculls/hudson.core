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
*    Kohsuke Kawaguchi, Stephen Connolly
 *     
 *
 *******************************************************************************/ 

package hudson.slaves;

import hudson.EnvVars;
import hudson.Util;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.util.StreamCopyThread;
import hudson.util.FormValidation;
import hudson.util.ProcessTree;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.hudson.stapler.DataBoundConstructor;
import org.eclipse.hudson.stapler.QueryParameter;

/**
 * {@link ComputerLauncher} through a remote login mechanism like ssh/rsh.
 *
 * @author Stephen Connolly
 * @author Kohsuke Kawaguchi
*/
public class CommandLauncher extends ComputerLauncher {

    /**
     * Command line to launch the agent, like
     * "ssh myslave java -jar /path/to/hudson-remoting.jar"
     */
    private final String agentCommand;
    
    /**
     * Optional environment variables to add to the current environment. Can be null.
     */
    private final EnvVars env;

    @DataBoundConstructor
    public CommandLauncher(String command) {
        this(command, null);
    }
    
    public CommandLauncher(String command, EnvVars env) {
    	this.agentCommand = command;
    	this.env = env;
    }

    public String getCommand() {
        return agentCommand;
    }

    /**
     * Gets the formatted current time stamp.
     */
    private static String getTimestamp() {
        return String.format("[%1$tD %1$tT]", new Date());
    }

    @Override
    public void launch(SlaveComputer computer, final TaskListener listener) {
        EnvVars _cookie = null;
        Process _proc = null;
        try {
            listener.getLogger().println(hudson.model.Messages.Slave_Launching(getTimestamp()));
            if(getCommand().trim().length()==0) {
                listener.getLogger().println(Messages.CommandLauncher_NoLaunchCommand());
                return;
            }
            listener.getLogger().println("$ " + getCommand());

            ProcessBuilder pb = new ProcessBuilder(Util.tokenize(getCommand()));
            final EnvVars cookie = _cookie = EnvVars.createCookie();
            pb.environment().putAll(cookie);

            {// system defined variables
                String rootUrl = Hudson.getInstance().getRootUrl();
                if (rootUrl!=null) {
                    pb.environment().put("HUDSON_URL", rootUrl);
                    pb.environment().put("SLAVEJAR_URL", rootUrl+"/jnlpJars/slave.jar");
                }
            }

            if (env != null) {
            	pb.environment().putAll(env);
            }
            
            final Process proc = _proc = pb.start();

            // capture error information from stderr. this will terminate itself
            // when the process is killed.
            new StreamCopyThread("stderr copier for remote agent on " + computer.getDisplayName(),
                    proc.getErrorStream(), listener.getLogger()).start();

            computer.setChannel(proc.getInputStream(), proc.getOutputStream(), listener.getLogger(), new Channel.Listener() {
                @Override
                public void onClosed(Channel channel, IOException cause) {
                    try {
                        int exitCode = proc.exitValue();
                        if (exitCode!=0) {
                            listener.error("Process terminated with exit code "+exitCode);
                        }
                    } catch (IllegalThreadStateException e) {
                        // hasn't terminated yet
                    }

                    try {
                        ProcessTree.get().killAll(proc, cookie);
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.INFO, "interrupted", e);
                    }
                }
            });

            LOGGER.info("slave agent launched for " + computer.getDisplayName());
        } catch (InterruptedException e) {
            e.printStackTrace(listener.error(Messages.ComputerLauncher_abortedLaunch()));
        } catch (RuntimeException e) {
            e.printStackTrace(listener.error(Messages.ComputerLauncher_unexpectedError()));
        } catch (Error e) {
            e.printStackTrace(listener.error(Messages.ComputerLauncher_unexpectedError()));
        } catch (IOException e) {
            Util.displayIOException(e, listener);

            String msg = Util.getWin32ErrorMessage(e);
            if (msg == null) {
                msg = "";
            } else {
                msg = " : " + msg;
            }
            msg = hudson.model.Messages.Slave_UnableToLaunch(computer.getDisplayName(), msg);
            LOGGER.log(Level.SEVERE, msg, e);
            e.printStackTrace(listener.error(msg));

            if(_proc!=null)
                try {
                    ProcessTree.get().killAll(_proc, _cookie);
                } catch (InterruptedException x) {
                    x.printStackTrace(listener.error(Messages.ComputerLauncher_abortedLaunch()));
                }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(CommandLauncher.class.getName());

    @Extension
    public static class DescriptorImpl extends Descriptor<ComputerLauncher> {
        public String getDisplayName() {
            return Messages.CommandLauncher_displayName();
        }

        public FormValidation doCheckCommand(@QueryParameter String value) {
            if(Util.fixEmptyAndTrim(value)==null)
                return FormValidation.error("Command is empty");
            else
                return FormValidation.ok();
        }
    }
}
