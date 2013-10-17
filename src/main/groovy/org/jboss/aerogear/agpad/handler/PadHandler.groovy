package org.jboss.aerogear.agpad.handler

import com.gmongo.GMongo
import com.mongodb.DB
import com.mongodb.DBObject
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import org.bson.types.ObjectId
import org.jboss.aerogear.agpad.PadMergeUtil
import org.jboss.aerogear.agpad.User
import org.jboss.aerogear.agpad.vo.Pad
import org.jboss.aerogear.agpad.vo.PadDiff
import org.jboss.aerogear.agpad.vo.Pair
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.groovy.core.eventbus.Message

/**
 * Created with IntelliJ IDEA.
 * User: summers
 * Date: 10/10/13
 * Time: 3:03 PM
 * To change this template use File | Settings | File Templates.
 */
@Log
class PadHandler {
    private final GMongo mongo = new GMongo()
    private final DB db = mongo.getDB("pad")
    final EventBus bus;
    static Map<UUID, Pair<Pad, Pad>> sessionToPadShadows = new HashMap<>();
    static Map<Pad, Set<Pad>> padToShadows =  [:]
    static Map<Pad, Set<Pair<Pad, UUID>>> padToShadowSessions =  [:]
    static Map<Pair<String, String>, Pad> openPads = new HashMap();

    PadHandler() {}

    PadHandler(EventBus bus) {
        log.severe("Pad Handler made")
        this.bus = bus

    }

    Pad createPad(String padJSON) {
        def pad = new JsonSlurper().parseText(padJSON);
        Pad test = db.pads.find(name:pad.name, ownerName:pad.ownerName)[0]
        if (test != null) {
            throw new RuntimeException("Duplicate Pad");
        }

        db.pads.insert(pad)
        pad = db.pads.find(name:pad.name, ownerName:pad.ownerName)[0] as Pad;
        pad._id = pad._id.toString()
        return pad

    }

    List<Pad> getPads(String userName) {
        def pads = new ArrayList()
            db.pads.find(ownerName:userName).each {it._id = it._id.toString(); pads.add(it)}
        if (pads == null) {
            throw new RuntimeException("Pad not found $userName");
        }
        return pads;
    }

    Pad getPad(String id) {
        def pad = db.pads.findOne(_id:new ObjectId(id)) as Pad
        if (pad == null) {
            log.severe("Pad not found $id")
            throw new RuntimeException("Pad not found $id");
        }

        pad._id = pad._id.toString()

        return pad;
    }

    Pad getPad(String padName, String userName) {
        Pair p = new Pair(first: padName, second: userName);
        if (openPads.get(p) == null) {
            Pad pad = db.pads.find(name:padName, ownerName:userName)[0]
            if (pad == null) {
                throw new RuntimeException("Pad not found $padName:$userName");
            }
            pad._id = pad._id.toString()
            openPads.put(p, pad);
            return pad
        } else {
            return openPads.get(p)
        }
    }

    UUID createSession(String padName, String userName) {
        def id = UUID.randomUUID();
        Pad pad = getPad(padName, userName)
        Pad shadow = new Pad(content: pad.content);
        shadow._id = id;
        sessionToPadShadows.put(id, new Pair<Pad, Pad>(first: pad, second: shadow ))
        if (padToShadows.get(pad) == null) {
            padToShadows.put(pad, new HashSet())
            padToShadowSessions.put(pad, new HashSet())
        }
        padToShadows.get(pad).add(shadow);
        padToShadowSessions.get(pad).add(new Pair(first: shadow, second: id))
        return id;
    }

    UUID createSession(String padId) {
        def id = UUID.randomUUID();
        Pad pad = getPad(padId)
        Pad shadow = new Pad(content: pad.content);
        shadow._id = id;
        shadow.name = pad.name
        sessionToPadShadows.put(id, new Pair<Pad, Pad>(first: pad, second: shadow ))
        if (padToShadows.get(pad) == null) {
            padToShadows.put(pad, new HashSet())
            padToShadowSessions.put(pad, new HashSet())
        }
        padToShadows.get(pad).add(shadow);
        padToShadowSessions.get(pad).add(new Pair(first: shadow, second: id))
        return id;
    }

    void handleDiff(String padDiffJSON, UUID sessionId) {

        Pad pad = sessionToPadShadows[sessionId].first;
        Pad padShadow = sessionToPadShadows[sessionId].second;
        PadDiff padDiff = new JsonSlurper().parseText(padDiffJSON)

        try {
            PadDiff newDiff = PadMergeUtil.updateAndDiffShadow(pad, padShadow, padDiff);
            db.pads.save (pad as DBObject)
            bus.send("padDiff.$sessionId", new JsonBuilder(newDiff).toString())
            log.severe("other sessions looking at pad:" + padToShadowSessions.get(pad).toString())
            for (Pair<Pad, UUID> shadowSession : padToShadowSessions.get(pad)) {
                try {
                    if (shadowSession.second != sessionId) {
                        log.severe("Broadcasting diff " + newDiff.diff);
                        handleDiffNoBroadcast(pad, shadowSession.second)
                    }

                } catch (Throwable t) {
                    bus.send("error.$shadowSession.second", t.getMessage())
                }
            }
        } catch (Throwable t) {
            bus.send("error.$sessionId", t.getMessage())
        }

    }

    void handleDiffNoBroadcast(Pad newPad, UUID sessionId) {
        Pad pad = sessionToPadShadows[sessionId].first = newPad;
        Pad padShadow = sessionToPadShadows[sessionId].second;
        try {
            log.severe("Sending Session updates: " + sessionId)
            PadDiff newDiff = PadMergeUtil.updateAndDiffShadow(pad, padShadow);
            bus.send("padDiff.$sessionId", new JsonBuilder(newDiff).toString())
        } catch (Throwable t) {
            bus.send("error.$sessionId", t.getMessage())
        }

    }



    void closeSession(UUID sessionId) {

        def padShadow = sessionToPadShadows.get(sessionId);
        log.severe("Closing Session " + sessionId + " with padShadow pad" + padShadow.first)
        log.severe("padToShadows" + padToShadows)
        padToShadows.get(padShadow.first).remove(padShadow.second)
        padToShadowSessions.get(padShadow.first).remove(new Pair(first: padShadow, second: sessionId))
        sessionToPadShadows.remove(sessionId);
        log.severe("Closing second " + sessionToPadShadows.toString())
    }

    Pad savePad(String padJSON, String id) {
        def pad = new JsonSlurper().parseText(padJSON);
        Pad test = db.pads.find(_id:new ObjectId(pad.id))[0]
        if (test == null) {
            throw new RuntimeException("No pad with id:$id");
        }

        pad._id = id;

        db.pads.save pad

        return getPad(id);
    }
}
