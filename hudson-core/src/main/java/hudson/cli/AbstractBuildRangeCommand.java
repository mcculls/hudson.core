/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *
 *******************************************************************************/ 

package hudson.cli;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Fingerprint.RangeSet;
import org.kohsuke.args4j.Argument;

import java.io.IOException;
import java.util.List;

/**
 * {@link CLICommand} that acts on a series of {@link AbstractBuild}s.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractBuildRangeCommand extends CLICommand {

    @Argument(metaVar = "JOB", usage = "Name of the job to build. ", required = true, index = 0)
    public AbstractProject<?, ?> job;
    @Argument(metaVar = "RANGE", usage = "Range of the build records to delete. 'N-M', 'N,M', or 'N'", required = true, index = 1)
    public String range;

    protected int run() throws Exception {
        RangeSet rs = RangeSet.fromString(range, false);

        return act((List) job.getBuilds(rs));
    }

    protected abstract int act(List<AbstractBuild<?, ?>> builds) throws IOException;
}
