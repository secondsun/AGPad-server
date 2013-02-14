package org.jboss.aerogear.agpad;

import com.gmongo.GMongo
import com.mongodb.DB


import java.security.MessageDigest

class LoginHandler {

    private final GMongo mongo = new GMongo()
    private final DB db = mongo.getDB("checkers")
    private final Map<String, User> sessions = [:];

    User login(String username, String password) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        digest.update(password.bytes);
        String hashedPW = new BigInteger(1, digest.digest()).toString(16).padLeft(32, '0')

        User user = db.users.find([username:username, password:hashedPW])[0].findAll { it.key != '_id' }

        startSession(user)
    }

    User getUser(String sessionId) {
        sessions[sessionId];
    }

    User enroll(Map<String, String> userData) {

        String username = userData['username']
        String password = userData['password']
        MessageDigest digest = MessageDigest.getInstance("MD5")
        digest.update(password.bytes);
        String hashedPW = new BigInteger(1, digest.digest()).toString(16).padLeft(32, '0')

        User user = db.users.find(username:username)[0]?.findAll { it.key != '_id' }
        if (user != null) {
            throw new RuntimeException("Duplicate User");
        }

        db.users.insert([username:username, password: hashedPW])
        user = db.users.find([username:username, password:hashedPW])[0].findAll { it.key != '_id' }
        startSession(user)
    }

    def logout(String sessionId) {
        sessions.remove(sessionId)
    }

    def getSessionId() {UUID.randomUUID().toString()}

    private User startSession(User user) {
        user.sessionId = getSessionId()
        sessions[user.sessionId] = user
        return user;
    }

}