package org.jboss.aerogear.agpad


import groovy.json.JsonBuilder
import groovy.util.logging.Log
import org.jboss.aerogear.agpad.handler.LoginHandler
import org.jboss.aerogear.agpad.handler.PadHandler
import org.jboss.aerogear.agpad.vo.Pad
import org.jboss.aerogear.agpad.vo.PadDiff
import org.vertx.groovy.core.buffer.Buffer
import org.vertx.groovy.core.http.ServerWebSocket
import org.vertx.groovy.platform.Verticle
import groovy.json.JsonSlurper
import org.vertx.groovy.core.http.RouteMatcher

import java.util.logging.Level
import java.util.logging.Logger

@Log
class DemoVerticle extends Verticle {

    def start() {
        def routeMatcher = new RouteMatcher()
        def server = vertx.createHttpServer()

        def eb = vertx.eventBus
        def padHandler = new PadHandler(eb);

        //Get all existing Pads
        routeMatcher.get("/pad", {req ->
            try {
                def pads = new JsonBuilder(padHandler.getPads("globalpad")).toString();
                req.response.with {
                    statusCode = 200
                    end(pads)
                }
            } catch (Exception e) {
                req.response.with {
                    log.severe(e.getMessage())
                    statusCode = 200
                    end("[]")
                }
            }

        })

        //Get an existing Pad
        routeMatcher.get("/pad/:id", {req ->
            try {
                def id = req.params.get("id")
                Pad pad = padHandler.getPad(id);
                req.response.with {
                    statusCode = 200
                    end(new JsonBuilder(pad).toString())
                }
            } catch (Exception e) {
                log.severe(e.getMessage())
                req.response.with {
                    statusCode = 200
                    end("[]")
                }
            }

        })

        //Create a Pad
        routeMatcher.post("/pad", {req ->

            req.dataHandler { buffer ->

                try {

                    def body = new Buffer(0)

                    body << buffer

                    body = body.toString()

                    Pad pad = new JsonSlurper().parseText(body);
                    pad.ownerName = 'globalpad';

                    pad = padHandler.createPad(new JsonBuilder(pad).toString());

                    req.response.statusCode = 200
                    req.response.end(new JsonBuilder(pad).toString())

                } catch (Exception e) {
                    req.response.statusCode = 500
                    log.log(Level.SEVERE, e.getMessage(), e)
                    req.response.end(e.getMessage())
                }

            }

        })

        //POst a diff.  Response will be sent on WebSocket
        routeMatcher.post("/padDiff/:sessionId", {req ->

            req.dataHandler { buffer ->


                def sessionId = UUID.fromString(req.params.get("sessionId"));


                def body = new Buffer(0)
                body << buffer
                body = body.toString()

                padHandler.handleDiff(body, sessionId);

                req.response.with {
                    statusCode = 200
                    end('{}')
                }
            }

        })

        //Echo a message to a session
        routeMatcher.post("/echo/:sessionId", {req ->

            req.dataHandler { buffer ->


                def sessionId = UUID.fromString(req.params.get("sessionId"));


                def body = new Buffer(0)
                body << buffer
                body = body.toString()

                vertx.eventBus.send("echo.$sessionId", body)
                req.response.with {
                    statusCode = 200
                    end('{}')
                }
            }

        })

        server.requestHandler(routeMatcher.asClosure())
                .websocketHandler({ ServerWebSocket ws ->
            String path = ws.path
            if (path.matches("/pad/[\\d-\\.\\w]+")) {
                String padId = path.split("/pad/")[1];
                UUID sessionId = padHandler.createSession(padId)
                log.severe("New Session: " + sessionId.toString())

                def diffHandler = {message ->
                    PadDiff newDiff = new JsonSlurper().parseText(message.body)
                    log.log(Level.SEVERE, "websocket sending ${message.body}")
                    if (!newDiff.diff.isEmpty()) {
                        ws.writeTextFrame(message.body)
                    }
                };

                def errorHandler = {message ->
                    log.log(Level.SEVERE, message.body)
                    ws.writeTextFrame('{"error":"'+message.body+'"}');
                };

                def echoHandler = {message ->
                    log.log(Level.SEVERE, message.body)
                    ws.writeTextFrame('{"message":"'+message.body+'"}');
                };

                log.log(Level.SEVERE, "register $sessionId")
                vertx.eventBus.with {
                    registerHandler("padDiff.$sessionId", diffHandler)
                    registerHandler("error.$sessionId", errorHandler)
                    registerHandler("echo.$sessionId", echoHandler)
                }
                ws.closeHandler {
                    log.log(Level.SEVERE, "closing $sessionId");

                    vertx.eventBus.with {
                        unregisterHandler("padDiff.$sessionId", diffHandler)
                        unregisterHandler("error.$sessionId", errorHandler)
                        unregisterHandler("echo.$sessionId", echoHandler)
                    }

                    padHandler.closeSession(sessionId)
                }

                ws.writeTextFrame('{"session_id":"'+sessionId+'"}');

            }   else {
                log.log(Level.SEVERE, "reject $path")
                ws.reject()
            }
        }).listen(8080, "localhost")


    }



}
