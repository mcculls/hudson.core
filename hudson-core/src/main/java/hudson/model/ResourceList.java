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

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;

/**
 * List of {@link Resource}s that an activity needs.
 *
 * <p> There are two ways to access resources. Read and write. As with usual
 * reader/writer pattern, multiple read accesses can co-exist concurrently, but
 * write access requires exclusive access (the number of allowed concurrent
 * write activity is determined by {@link Resource#numConcurrentWrite}.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.121
 */
public final class ResourceList {

    /**
     * All resources (R/W.)
     */
    private final Set<Resource> all = new HashSet<Resource>();
    /**
     * Write accesses. Values are the # of write counts that this list uses.
     *
     * The values are mostly supposed to be 1, but when
     * {@link ResourceController} uses a list to keep track of the union of all
     * the activities, it can get larger.
     */
    private final Map<Resource, Integer> write = new HashMap<Resource, Integer>();
    private static final Integer MAX_INT = Integer.MAX_VALUE;

    /**
     * Creates union of all resources.
     */
    public static ResourceList union(ResourceList... lists) {
        return union(Arrays.asList(lists));
    }

    /**
     * Creates union of all resources.
     */
    public static ResourceList union(Collection<ResourceList> lists) {
        switch (lists.size()) {
            case 0:
                return EMPTY;
            case 1:
                return lists.iterator().next();
            default:
                ResourceList r = new ResourceList();
                for (ResourceList l : lists) {
                    r.all.addAll(l.all);
                    for (Entry<Resource, Integer> e : l.write.entrySet()) {
                        r.write.put(e.getKey(), unbox(r.write.get(e.getKey())) + e.getValue());
                    }
                }
                return r;
        }
    }

    /**
     * Adds a resource for read access.
     */
    public ResourceList r(Resource r) {
        all.add(r);
        return this;
    }

    /**
     * Adds a resource for write access.
     */
    public ResourceList w(Resource r) {
        all.add(r);
        write.put(r, unbox(write.get(r)) + 1);
        return this;
    }

    /**
     * Checks if this resource list and that resource list has any conflicting
     * resource access.
     */
    public boolean isCollidingWith(ResourceList that) {
        return getConflict(that) != null;
    }

    /**
     * Returns the resource in this list that's colliding with the given
     * resource list.
     */
    public Resource getConflict(ResourceList that) {
        Resource r = _getConflict(this, that);
        if (r != null) {
            return r;
        }
        return _getConflict(that, this);
    }

    private Resource _getConflict(ResourceList lhs, ResourceList rhs) {
        for (Entry<Resource, Integer> r : lhs.write.entrySet()) {
            for (Resource l : rhs.all) {
                Integer v = rhs.write.get(l);
                if (v != null) // this is write/write conflict.
                {
                    v += r.getValue();
                } else // Otherwise set it to a very large value, since it's read/write conflict
                {
                    v = MAX_INT;
                }
                if (r.getKey().isCollidingWith(l, unbox(v))) {
                    return r.getKey();
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        Map<Resource, String> m = new HashMap<Resource, String>();
        for (Resource r : all) {
            m.put(r, "R");
        }
        for (Entry<Resource, Integer> e : write.entrySet()) {
            m.put(e.getKey(), "W" + e.getValue());
        }
        return m.toString();
    }

    /**
     * {@link Integer} unbox operation that treats null as 0.
     */
    private static int unbox(Integer x) {
        return x == null ? 0 : x;
    }
    /**
     * Empty resource list.
     */
    public static final ResourceList EMPTY = new ResourceList();
}
