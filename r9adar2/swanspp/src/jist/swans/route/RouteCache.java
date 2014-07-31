/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         RouteCache.java
 * RCS:          $Id: RouteCache.java,v 1.1 2007/04/09 18:49:30 drchoffnes Exp $
 * Description:  RouteCache class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jul 19, 2006 at 2:55:00 PM
 * Language:     Java
 * Package:      jist.swans.route
 * Status:       Alpha Release
 *
 * (C) Copyright 2006, Northwestern University, all rights reserved.
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

/*
 * routecache.cc
 * Copyright (C) 2000 by the University of Southern California
 * $Id: RouteCache.java,v 1.1 2007/04/09 18:49:30 drchoffnes Exp $
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License,
 * version 2, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 *
 * The copyright of this module includes the following
 * linking-with-specific-other-licenses addition:
 *
 * In addition, as a special exception, the copyright holders of
 * this module give you permission to combine (via static or
 * dynamic linking) this module with free software programs or
 * libraries that are released under the GNU LGPL and with code
 * included in the standard release of ns-2 under the Apache 2.0
 * license or under otherwise-compatible licenses with advertising
 * requirements (or modified versions of such code, with unchanged
 * license).  You may copy and distribute such a system following the
 * terms of the GNU GPL for this module and the licenses of the
 * other code concerned, provided that you include the source code of
 * that other code when and as the GNU GPL requires distribution of
 * source code.
 *
 * Note that people who make modified versions of this module
 * are not obligated to grant this special exception for their
 * modified versions; it is their choice whether to do so.  The GNU
 * General Public License gives permission to release a modified
 * version without this exception; this exception also makes it
 * possible to release a modified version which carries forward this
 * exception.
 *
 */

/*
 * Ported from CMU/Monarch's code, appropriate copyright applies.  
 * routecache.cc
 *   handles routes
 */
package jist.swans.route;

import java.util.HashSet;

import jist.swans.net.NetAddress;
import jist.swans.route.RouteDsr_Ns2.ID;
import jist.swans.route.RouteDsr_Ns2.ID.Link_Type;
import jist.swans.route.RouteDsr_Ns2.ID.Log_Status;
import jist.swans.route.RouteDsr_Ns2.Path;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The RouteCache class ...
 */
public abstract class RouteCache  extends HashSet {

    ID MAC_id, net_id;
    NetAddress invalid_addr = new NetAddress(0);
//  abstract void dump(FILE *out);
  
  boolean cache_ignore_hints = false;
  boolean cache_use_overheard_routes = true;

  static final int STOP_PROCESSING = 0;
  static final int CONT_PROCESSING = 1;
    
    /**
     * 
     */
    public RouteCache() {
        super();
        MAC_id = null;
        net_id = null;

    }
    
    abstract void noticeDeadLink(final ID from, final ID to, long t);
    // the link from->to isn't working anymore, purge routes containing
    // it from the cache

    abstract void noticeRouteUsed(final Path route, long t, 
                   final ID who_from);
    // tell the cache about a route we saw being used
    // if first tested is set, then we assume the first link was recently
    // known to work

    abstract void addRoute(final Path route, long t, final ID who_from);
    // add this route to the cache (presumably we did a route request
    // to find this route and don't want to lose it)

    abstract boolean findRoute(ID dest, Path route, boolean for_use);
    // if there is a cached path from us to dest returns true and fills in
    // the route accordingly. returns false otherwise
    // if for_use, then we assume that the node really wants to keep 
    // the returned route

//    abstract int command(int argc, final char*final* argv);
//    void trace(char* fmt, ...);

    // *******************************************************



//  #ifdef DSR_CACHE_STATS
//    MobiHandler mh;
//    struct cache_stats stat;
//
//    abstract void periodic_checkCache() = 0;
//    int checkRoute_logall(Path p, int action, int start);
//  #endif


    //**********************************************************


    // returns 1 if the Route should be added to the cache, 0 otherwise.
    int
    pre_addRoute(final Path route, Path rt,
                 long t, final ID who_from)
    {
      assert(!(net_id.equals(invalid_addr)));
      
      if (route.length() < 1) // T-ODO check why this was set to 2 instead of 1
        return STOP_PROCESSING; // we laugh in your face

//      if(verbose_debug)
//        trace("SRC %.9f _%s_ adding rt %s from %s",
//          Scheduler::instance().clock(), net_id.dump(),
//          route.dump(), who_from.dump());

      if (!route.get(0).equals(net_id) && !route.get(0).equals(MAC_id)) 
        {
//          fprintf(stderr,"%.9f _%s_ adding bad route to cache %s %s\n",
//              t, net_id.dump(), who_from.dump(), route.dump());
          return STOP_PROCESSING;
        }
             
      ((Path) route).copyInto(rt);   // cast away final Path
      rt.set_owner(who_from);

      int kind = Link_Type.LT_TESTED;
      for (int c = 0; c < rt.length(); c++)
        {
          rt.get(c).log_stat = Log_Status.LS_UNLOGGED;
          if (rt.get(c) == who_from) kind = Link_Type.LT_UNTESTED; // remaining ids came from $
          rt.get(c).link_type = kind;
          rt.get(c).t = t;
        }

      return CONT_PROCESSING;
    }

    // returns 1 if the Route should be added to the cache, 0 otherwise.
    int
    pre_noticeRouteUsed(final Path p, Path stub,
                    long t, final ID who_from)
    {
      int c;
      boolean first_tested = true;

      if (p.length() < 2)
          return STOP_PROCESSING;
      if (cache_ignore_hints == true)
          return STOP_PROCESSING;

      for (c = 0; c < p.length() ; c++) {
          if (p.get(c).equals(net_id) || p.get(c).equals(MAC_id)) break;
      }

      if (c == p.length() - 1)
          return STOP_PROCESSING; // path contains only us

      if (c == p.length()) { // we aren't in the path...
          if (cache_use_overheard_routes) {
              // assume a link from us to the person who
              // transmitted the packet
              if (p.index() == 0) {
                   /* must be a route request */
                  return STOP_PROCESSING;
              }

              stub.reset();
              stub.appendToPath(net_id);
              int i = p.index() - 1;
              for ( ; i < p.length() && !stub.full() ; i++) {
                  stub.appendToPath(p.get(i));
              }
              // link to xmiter might be unidirectional
              first_tested = false;
          }
          else {
              return STOP_PROCESSING;
          }
      }
      else { // we are in the path, extract the subpath
          Path.CopyIntoPath(stub, p, c, p.length() - 1);
      }

      int kind = Link_Type.LT_TESTED;
      for (c = 0; c < stub.length(); c++) {
          stub.get(c).log_stat = Log_Status.LS_UNLOGGED;

           // remaining ids came from $
          if (stub.get(c) == who_from)
              kind = Link_Type.LT_UNTESTED;
          stub.get(c).link_type = kind;
          stub.get(c).t = t;
      }
      if (first_tested == false)
          stub.get(0).link_type = Link_Type.LT_UNTESTED;

      return CONT_PROCESSING;
    }


////////////////////////////////////////////////////////////////////    //


//
//    void
//    MobiHandler::handle(Event *) {
//            cache->periodic_checkCache();
//            Scheduler::instance().schedule(this, intr, interval);
//    }
//
//    int
//    checkRoute_logall(Path p, int action, int start)
//    {
//      int c;
//      int subroute_bad_count = 0;
//
//      if(p->length() == 0)
//        return 0;
//      assert(p->length() >= 2);
//
//      assert(action == ACTION_DEAD_LINK ||
//             action == ACTION_EVICT ||
//             action == ACTION_FIND_ROUTE);
//
//      for (c = start; c < p->length() - 1; c++)
//        {
//          if (God::instance()->hops((*p).get(c).getNSAddr_t(), (*p)[c+1].getNSAddr_t()) != 1)
//        {
//              trace("SRC %.9f _%s_ %s [%d %d] %s->%s dead %d %.9f",
//                    Scheduler::instance().clock(), net_id.dump(),
//                    action_name[action], p->length(), c,
//                    (*p).get(c).dump(), (*p)[c+1].dump(),
//                    (*p).get(c).link_type, (*p).get(c).t);
//
//              if(subroute_bad_count == 0)
//                subroute_bad_count = p->length() - c - 1;
//        }
//        }
//      return subroute_bad_count;
//    }


}
