package org.jboss.aerogear.agpad;

import com.mongodb.BasicDBList
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import groovy.transform.EqualsAndHashCode
import org.bson.types.ObjectId

@EqualsAndHashCode
class User {
    String username
    String password
    String sessionId

    public Object asType(Class type) {
        if (type == DBObject) {
            new BasicDBObject([username:username]);
        }
    }
}
