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
*    Kohsuke Kawaguchi, Daniel Dyer, Red Hat, Inc., Yahoo!, Inc.
 *     
 *
 *******************************************************************************/ 

package hudson.tasks.test;

import hudson.model.AbstractBuild;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResult;
import org.eclipse.hudson.stapler.export.Exported;
import org.eclipse.hudson.stapler.export.ExportedBean;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link AbstractTestResultAction} that aggregates all the test results
 * from the corresponding {@link AbstractBuild}s.
 *
 * <p>
 * (This has nothing to do with {@link AggregatedTestResultPublisher}, unfortunately)
 *
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public abstract class AggregatedTestResultAction extends AbstractTestResultAction {
    private int failCount,skipCount,totalCount;

    public static final class Child {
        /**
         * Name of the module. Could be relative to something.
         * The interpretation of this is done by
         * {@link AggregatedTestResultAction#getChildName(AbstractTestResultAction)} and
         * {@link AggregatedTestResultAction#resolveChild(Child)} and
         */
        public final String name;
        public final int build;

        public Child(String name, int build) {
            this.name = name;
            this.build = build;
        }
    }

    /**
     * child builds whose test results are used for aggregation.
     */
    //TODO: review and check whether we can do it private
    public final List<Child> children = new ArrayList<Child>();

    public List<Child> getChildren() {
        return children;
    }

    public AggregatedTestResultAction(AbstractBuild owner) {
        super(owner);
    }

    protected void update(List<? extends AbstractTestResultAction> children) {
        failCount = skipCount = totalCount = 0;
        this.children.clear();
        for (AbstractTestResultAction tr : children)
            add(tr);
    }

    protected void add(AbstractTestResultAction child) {
        failCount += child.getFailCount();
        skipCount += child.getSkipCount();
        totalCount += child.getTotalCount();
        this.children.add(new Child(getChildName(child),child.owner.number));
    }

    public int getFailCount() {
        return failCount;
    }

    @Override
    public int getSkipCount() {
        return skipCount;
    }

    public int getTotalCount() {
        return totalCount;
    }
   
    public List<ChildReport> getResult() {
        // I think this is a reasonable default.
        return getChildReports();
    }

    @Override
    public List<CaseResult> getFailedTests() {
        List<CaseResult> failedTests = new ArrayList<CaseResult>(failCount);
        for (ChildReport childReport : getChildReports()) {
            if (childReport.result instanceof TestResult) {
                failedTests.addAll(((TestResult) childReport.result).getFailedTests());
            }
        }
        return failedTests;
    }

    /**
     * Data-binding bean for the remote API.
     */
    @ExportedBean(defaultVisibility=2)
    public static final class ChildReport {
        //TODO: review and check whether we can do it private
        @Exported
        public final AbstractBuild<?,?> child;
        //TODO: review and check whether we can do it private
        @Exported
        public final Object result;

        public AbstractBuild getChild() {
            return child;
        }

        public Object getResult() {
            return result;
        }

        public ChildReport(AbstractBuild<?, ?> child, AbstractTestResultAction result) {
            this.child = child;
            this.result = result.getResult();
        }
    }

    /**
     * Mainly for the remote API. Expose results from children.
     */
    @Exported(inline=true)
    public List<ChildReport> getChildReports() {
        return new AbstractList<ChildReport>() {
            public ChildReport get(int index) {
                return new ChildReport(
                        resolveChild(children.get(index)),
                        getChildReport(children.get(index)));
            }

            public int size() {
                return children.size();
            }
        };
    }

    protected abstract String getChildName(AbstractTestResultAction tr);
    public abstract AbstractBuild<?,?> resolveChild(Child child);

    /**
     * Uses {@link #resolveChild(Child)} and obtain the
     * {@link AbstractTestResultAction} object for the given child.
     */
    protected AbstractTestResultAction getChildReport(Child child) {
        AbstractBuild<?,?> b = resolveChild(child);
        if(b==null) return null;
        return b.getAction(AbstractTestResultAction.class);
    }

    /**
     * Since there's no TestObject that points this action as the owner
     * (aggregated {@link TestObject}s point to their respective real owners, not 'this'),
     * so this method should be never invoked.
     *
     * @deprecated
     *      so that IDE warns you if you accidentally try to call it.
     */
    @Override
    protected final String getDescription(TestObject object) {
        throw new AssertionError();
    }

    /**
     * See {@link #getDescription(TestObject)}
     *
     * @deprecated
     *      so that IDE warns you if you accidentally try to call it.
     */
    @Override
    protected final void setDescription(TestObject object, String description) {
        throw new AssertionError();
    }
}
