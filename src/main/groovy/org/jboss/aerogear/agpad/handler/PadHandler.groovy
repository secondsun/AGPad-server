package org.jboss.aerogear.agpad.handler

import com.gmongo.GMongo
import com.mongodb.DB
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.jboss.aerogear.agpad.User
import org.jboss.aerogear.agpad.vo.Pad
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
class PadHandler {
    private final GMongo mongo = new GMongo()
    private final DB db = mongo.getDB("pad")
    final EventBus bus;
    Map<UUID, Pair<Pad, Pad>> sessionToPadShadows = new HashMap<>();
    Map<Pad, Set<Pad>> padToShadows =  [:]
    Map<Pair<String, String>, Pad> openPads = new HashMap();

    PadHandler() {}

    PadHandler(EventBus bus) {
        this.bus = bus
        bus.registerHandler("pad.create", { Message message ->

            def reply = [:]
            try {
                def pad = createPad(message.body.getString(0, message.body.length()));
                reply['body'] = new JsonBuilder(pad).toString();
                reply['statusCode'] = 200
            } catch (Throwable t) {
                reply['body'] = t.getMessage()
                reply['statusCode'] = 500
            }
            message.reply reply
        })

        bus.registerHandler("pad.update", { Message message ->
            def padDiff = new JsonSlurper().parseText(message.body.getString(0, message.body.length()));
            def reply = [:]

            reply['body'] = "not implemented"
            reply['statusCode'] = 500

            message.reply reply
        })

    }

    Pad createPad(String padJSON) {
        def pad = new JsonSlurper().parseText(padJSON);
        Pad test = db.pads.find(name:pad.name, ownerName:pad.ownerName)[0]
        if (test != null) {
            throw new RuntimeException("Duplicate Pad");
        }

        db.pads.insert(pad)

        return db.pads.find(name:pad.name, ownerName:pad.ownerName)[0] as Pad;

    }

    Pad getPad(String padName, String userName) {
        Pair p = new Pair(first: padName, second: userName);
        if (openPads.get(p) == null) {
            Pad pad = db.pads.find(name:padName, ownerName:userName)[0]
            if (pad == null) {
                throw new RuntimeException("Pad not found $padName:$userName");
            }
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
        sessionToPadShadows.put(id, new Pair<Pad, Pad>(first: pad, second: shadow ))
        if (padToShadows.get(pad) == null) {
            padToShadows.put(pad, new HashSet())
        }
        padToShadows.get(pad).add(shadow);
        return id;
    }

    void handleDiff(String padDiffJSON) {

    }

    void closeSession(UUID sessionId) {
        def padShadow = sessionToPadShadows.get(sessionId);
        padToShadows.get(padShadow.first).remove(padShadow.second)
        sessionToPadShadows.remove(sessionId);

    }
}
