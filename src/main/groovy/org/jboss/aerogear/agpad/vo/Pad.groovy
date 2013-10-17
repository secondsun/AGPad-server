package org.jboss.aerogear.agpad.vo

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.EqualsAndHashCode
import org.bson.types.ObjectId


class Pad {
    String _id;
    String ownerName;
    String name;
    String content;

    public Object asType(Class type) {
        if (type == DBObject) {
            BasicDBObject object = new BasicDBObject();
            object['_id'] = new ObjectId(_id);
            object['ownerName'] = ownerName
            object['name'] = name
            object['content'] = content
            return object
        }
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Pad pad = (Pad) o

        if (_id != pad._id) return false
        if (name != pad.name) return false
        if (ownerName != pad.ownerName) return false

        return true
    }

    int hashCode() {
        int result
        result = (_id != null ? _id.hashCode() : 0)
        result = 31 * result + (ownerName != null ? ownerName.hashCode() : 0)
        result = 31 * result + (name != null ? name.hashCode() : 0)
        return result
    }
}
