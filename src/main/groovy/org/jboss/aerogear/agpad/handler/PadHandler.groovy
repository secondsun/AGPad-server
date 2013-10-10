package org.jboss.aerogear.agpad.handler

import com.gmongo.GMongo
import com.mongodb.DB
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.jboss.aerogear.agpad.User
import org.jboss.aerogear.agpad.vo.Pad
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

    PadHandler(EventBus bus) {
        this.bus = bus
        bus.registerHandler("pad.create", { Message message ->
            def pad = new JsonSlurper().parseText(message.body.getString(0, message.body.length()));
            def reply = [:]
            try {
                Pad test = db.pads.find(name:pad.name, ownerName:pad.ownerName)[0]?.findAll { it.key != '_id' }
                if (test != null) {
                    throw new RuntimeException("Duplicate Pad");
                }

                db.pads.insert(pad)

                pad = db.pads.find(name:pad.name, ownerName:pad.ownerName)[0] as Pad;
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


}
