package org.jboss.aerogear.agpad.vo

import groovy.transform.EqualsAndHashCode
import org.bson.types.ObjectId

@EqualsAndHashCode
class Pad {
    ObjectId _id;
    String ownerName;
    String name;
    String content;
}
