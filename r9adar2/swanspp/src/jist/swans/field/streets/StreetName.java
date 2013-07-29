/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StreetName.java
 * RCS:          $Id: StreetName.java,v 1.1 2007/04/09 18:49:42 drchoffnes Exp $
 * Description:  StreetName class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Dec 8, 2004
 * Language:     Java
 * Package:      jist.swans.field.streets
 * Status:       Alpha Release
 *
 * (C) Copyright 2005, Northwestern University, all rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package jist.swans.field.streets;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StreetName class stores information about a street name.
 */
public class StreetName {
	/** prefix (e.g, North, South, ...) */
    String prefix;
    /** street name */
    String name;
    /** street type */
    String type;
    /** suffix (e.g., St, Rd, Blvd, etc) */
    String suffix;
    
    
    /**
     * @param prefix
     * @param name
     * @param type
     * @param suffix
     */
    public StreetName(String prefix, String name, String type, String suffix) {
        super();
        this.prefix = prefix.trim();
        this.name = name.trim();
        this.type = type.trim();
        this.suffix = suffix.trim();
    }
    /**
     * 
     */
    public StreetName() {
        
        this.prefix = "";
        this.name = "No name";
        this.type = "";
        this.suffix = "";
    }
    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return Returns the prefix.
     */
    public String getPrefix() {
        return prefix;
    }
    /**
     * @param prefix The prefix to set.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    /**
     * @return Returns the suffix.
     */
    public String getSuffix() {
        return suffix;
    }
    /**
     * @param suffix The suffix to set.
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
    /**
     * @return Returns the type.
     */
    public String getType() {
        return type;
    }
    /**
     * @param type The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }
    
    public String toString()
    {
        return prefix.trim() + " " + name.trim() + " " + suffix.trim();
    }
}
