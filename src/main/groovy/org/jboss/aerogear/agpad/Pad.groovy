package org.jboss.aerogear.agpad


import com.mongodb.BasicDBList
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import groovy.transform.EqualsAndHashCode
import org.bson.types.ObjectId

@EqualsAndHashCode
class Pad {
    String username
    String name
    String content

    public Object asType(Class type) {
        if (type == DBObject) {
            new BasicDBObject([username:username, name:name, content:content]);
        }
    }
}

