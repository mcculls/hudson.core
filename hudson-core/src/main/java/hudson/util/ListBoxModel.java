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

package hudson.util;

import hudson.model.ModelObject;
import org.eclipse.hudson.stapler.HttpResponse;
import org.eclipse.hudson.stapler.StaplerRequest;
import org.eclipse.hudson.stapler.StaplerResponse;
import org.eclipse.hudson.stapler.export.Exported;
import org.eclipse.hudson.stapler.export.ExportedBean;
import org.eclipse.hudson.stapler.export.Flavor;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Model object of dynamically filled list box.
 *
 * <h2>Usage</h2>
 * <p>
 * The dynamic list box support allows the SELECT element to change its options dynamically
 * by using the values given by the server.
 *
 * <p>
 * To use this, HTML needs to declare the SELECT element:
 *
 * <pre><xmp>
 * <select id='foo'>
 *   <option>Fetching values...</optoin>
 * </select>
 * </xmp></pre>
 *
 * <p>
 * The SELECT element may have initial option values (in fact in most cases having initial
 * values are desirable to avoid the client from submitting the form before the AJAX call
 * updates the SELECT element.) It should also have an ID (although if you can get
 * to the DOM element by other means, that's fine, too.)
 *
 * <p>
 * Other parts of the HTML can initiate the SELECT element update by using the "updateListBox"
 * function, defined in <tt>hudson-behavior.js</tt>. The following example does it
 * when the value of the textbox changes:
 *
 * <pre><xmp>
 * <input type="textbox" onchange="updateListBox('list','optionValues?value='+encode(this.value))"/>
 * </xmp></pre>
 *
 * <p>
 * The first argument is the SELECT element or the ID of it (see Prototype.js <tt>$(...)</tt> function.)
 * The second argument is the URL that returns the options list.
 *
 * <p>
 * The URL usually maps to the <tt>doXXX</tt> method on the server, which uses {@link ListBoxModel}
 * for producing option values. See the following example:
 *
 * <pre>
 * public ListBoxModel doOptionValues(@QueryParameter("value") String value) throws IOException, ServletException {
 *   ListBoxModel m = new ListBoxModel();
 *   for(int i=0; i<5; i++)
 *     m.add(value+i,value+i);
 *   // make the third option selected initially
 *   m.get(3).selected = true;
 *   return m;
 * }
 * </pre>
 * @since 1.123
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public class ListBoxModel extends ArrayList<ListBoxModel.Option> implements HttpResponse {

    @ExportedBean(defaultVisibility=999)
    public static final class Option {
        /**
         * Text to be displayed to user.
         */
        //TODO: review and check whether we can do it private
        @Exported
        public String name;
        /**
         * The value that gets sent to the server when the form is submitted.
         */
        //TODO: review and check whether we can do it private
        @Exported
        public String value;

        /**
         * True to make this item selected.
         */
        //TODO: review and check whether we can do it private
        @Exported
        public boolean selected;

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public boolean isSelected() {
            return selected;
        }

        public Option(String name, String value) {
            this(name,value,false);
        }

        public Option(String name) {
            this(name,name,false);
        }

        public Option(String name, String value, boolean selected) {
            this.name = name;
            this.value = value;
            this.selected = selected;
        }
    }

    public ListBoxModel(int initialCapacity) {
        super(initialCapacity);
    }

    public ListBoxModel() {
    }

    public ListBoxModel(Collection<Option> c) {
        super(c);
    }

    public ListBoxModel(Option... data) {
        super(Arrays.asList(data));
    }

    public void add(String displayName, String value) {
        add(new Option(displayName,value));
    }

    public void add(ModelObject usedForDisplayName, String value) {
        add(usedForDisplayName.getDisplayName(), value);
    }

    /**
     * A version of the {@link #add(String, String)} method where the display name and the value are the same. 
     */
    public ListBoxModel add(String nameAndValue) {
        add(nameAndValue,nameAndValue);
        return this;
    }

    public void writeTo(StaplerRequest req,StaplerResponse rsp) throws IOException, ServletException {
        rsp.serveExposedBean(req,this,Flavor.JSON);
    }

    public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
        writeTo(req,rsp);
    }

    /**
     * @deprecated
     *      Exposed for stapler. Not meant for programatic consumption.
     */
    @Exported
    public Option[] values() {
        return toArray(new Option[size()]);
    }
}
