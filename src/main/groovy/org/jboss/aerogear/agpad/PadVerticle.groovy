/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 *
 */

package org.jboss.aerogear.agpad

import groovy.json.JsonBuilder
import groovy.util.logging.Log
import org.jboss.aerogear.agpad.handler.LoginHandler
import org.jboss.aerogear.agpad.handler.PadHandler
import org.jboss.aerogear.agpad.vo.Pad
import org.vertx.groovy.core.buffer.Buffer
import org.vertx.groovy.core.http.ServerWebSocket
import org.vertx.groovy.platform.Verticle
import groovy.json.JsonSlurper
import org.vertx.groovy.core.http.RouteMatcher

import java.util.logging.Level
import java.util.logging.Logger

/*
 * This is a simple compiled Groovy verticle which receives `ping` messages on the event bus and sends back `pong`
 * replies
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@Log
class PadVerticle extends Verticle {

  def start() {

    def loginHandler = new LoginHandler()
    def routeMatcher = new RouteMatcher()
    def server = vertx.createHttpServer()

    Logger log = Logger.getLogger("server")
    def eb = vertx.eventBus
    def padHandler = new PadHandler(eb);

      routeMatcher.get("/auth/login", { req ->
          req.bodyHandler {body ->

              try {
                  req.response.with {
                          Date d = new Date()
                          d.time = d.time + 30 * 60 * 1000
                          headers.set('Set-Cookie', "session_id=5; Expires=${d.toGMTString()};Path=/;".toString())
                          statusCode = 200
                  }
              } catch (Throwable t) {
                  log.log(Level.SEVERE, t.getMessage(), t)
                  req.response.statusCode = 401
              }
              req.response.end()
          }
      })

      routeMatcher.post("/auth/login", {req ->

          req.bodyHandler {body ->

              try {

                  log.log(Level.INFO, body.toString());

                  def auth = new JsonSlurper().parseText(body.toString())



                  assert auth.username != null
                  assert auth.password != null

                  def user = loginHandler.login(auth.username, auth.password)
                  assert user != null;
                  req.response.with {
                      if (user == null) {
                          statusCode = 401
                      }  else {
                          Date d = new Date()
                          d.time = d.time + 30 * 60 * 1000
                          headers.set('Set-Cookie', "session_id=$user.sessionId; Expires=${d.toGMTString()};Path=/;")
                          statusCode = 200
                      }
                  }
              } catch (Throwable t) {
                  log.log(Level.SEVERE, t.getMessage(), t)
                  req.response.statusCode = 401
              }
              req.response.end()
          }

      }
      )

      routeMatcher.post("/auth/logout", {req ->
          try {
          loginHandler.logout(getSessionId(req))
          req.response.statusCode = 200;
          req.response.end()
          } catch (Exception e) {
              req.response.statusCode = 500;
              req.response.end(e.getMessage())
          }
      }
      )

      routeMatcher.post("/auth/enroll", {req ->
          req.bodyHandler {body ->

              try {

                  log.log(Level.INFO, body.toString());

                  def auth = new JsonSlurper().parseText(body.toString())

                  assert auth.username != null
                  assert auth.password != null

                  def user = loginHandler.enroll(auth)
                  assert user != null;
                  req.response.with {
                      if (user == null) {
                          statusCode = 401
                      }  else {
                          Date d = new Date()
                          d.time = d.time + 30 * 60 * 1000
                          headers.set('Set-Cookie', "session_id=$user.sessionId; Expires=${d.toGMTString()};Path=/;")
                          statusCode = 200
                      }
                  }
              } catch (Throwable t) {
                  log.log(Level.SEVERE, t.getMessage(), t)
                  req.response.statusCode = 401
              }
              req.response.end()
          }
      }
      )

      routeMatcher.get("/pad", {req ->
          User user = loginHandler.getUser(getSessionId(req))
          if (user == null) {
              req.response.with {
                  statusCode = 403
                  end('')
              }
          }  else {
              try {
              def pads = new JsonBuilder(padHandler.getPads(user.username));
                req.response.with {
                  statusCode = 200
                  end(pads)
                }
              } catch (Exception e) {
                  req.response.with {
                      statusCode = 200
                      end("[]")
                  }
              }
          }


      })

      routeMatcher.put("/pad/:id", {req ->
          User user = loginHandler.getUser(getSessionId(req))
          if (user == null) {
              req.response.with {
                  statusCode = 403
                  end('')
              }
          }  else {
              def id = req.params.get("id");

              req.dataHandler { buffer ->

                  try {

                      def body = new Buffer(0)

                      if (id == null) {
                          throw new RuntimeException("No id supplied");
                      }

                      body << buffer

                      body = body.toString()

                      Pad pad = new JsonSlurper().parseText(body);

                      pad = padHandler.savePad(new JsonBuilder(pad).toString(), id);

                      req.response.statusCode = 200
                      req.response.end(new JsonBuilder(pad).toString())

                  } catch (Exception e) {
                      req.response.statusCode = 500
                      req.response.end(e.getMessage())
                  }

              }
          }
      })

      routeMatcher.post("/pad", {req ->
          User user = loginHandler.getUser(getSessionId(req))
          if (user == null) {
              req.response.with {
                  statusCode = 403
                  end('')
              }
          }  else {
              def id = req.params.get("id");


              req.dataHandler { buffer ->

                  try {

                  def body = new Buffer(0)

                  body << buffer

                  body = body.toString()

                  Pad pad = new JsonSlurper().parseText(body);
                  pad.ownerName = user.username;

                  pad = padHandler.createPad(new JsonBuilder(pad).toString());

                  req.response.statusCode = 200
                  req.response.end(new JsonBuilder(pad).toString())

                  } catch (Exception e) {
                      req.response.statusCode = 500
                      log.log(Level.SEVERE, e.getMessage(), e)
                      req.response.end(e.getMessage())
                  }

              }
          }
      })

      routeMatcher.post("/padDiff/:id", {req ->
          User user = loginHandler.getUser(getSessionId(req))
          if (user == null) {
              req.response.with {
                  statusCode = 403
                  end('')
              }
          }  else {
              req.bodyHandler { body ->
                  def sessionId = req.params.get("id");

                  vertx.eventBus.send("padDiff.$id", body)
                  req.response.with {
                      statusCode = 200
                      end('{}')
                  }
              }
          }


      })

      server.requestHandler(routeMatcher.asClosure())
            .websocketHandler({ ServerWebSocket ws ->
          String path = ws.path
          if (path.matches("/pad/[\\d-\\.]+")) {
              String id = path.split("/pad/")[1];
              def handler = {message ->
                  log.log(Level.SEVERE, "websocket sending $id")
                  ws.writeTextFrame("posted! $id")
              };

              log.log(Level.SEVERE, "register $id")
              vertx.eventBus.registerHandler("pad.$id", handler)
              ws.closeHandler {
                    log.log(Level.SEVERE, "unregister $id");
                    vertx.eventBus.unregisterHandler("pad.$id", handler)}
          }   else {
              log.log(Level.SEVERE, "reject $path")
              ws.reject()
          }
      }).listen(8080, "localhost")

  }

    def getSessionId( req) {
        def cookieHeader = req.headers['cookie'];
        String cookieValue = null;
        log.log(Level.SEVERE, cookieHeader)
        if (cookieHeader == null) {
            return null;
        }

        List<HttpCookie> cookies = HttpCookie.parse(cookieHeader)
        cookies.each{ cookie ->
            if (cookie.name.matches('session_id')) {
                log.log(Level.INFO, 'found session')
                return cookieValue = cookie.value
            }
        }

        return  cookieValue;

    }

}
