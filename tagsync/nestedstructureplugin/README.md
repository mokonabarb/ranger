# tagsync-mapper
Tag-resource mapper for Atlas-Ranger tag sync for nestedstructure Ranger plugin

## License
Licensed under the Apache License, Version 2.0. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

## Tag-based access control
One of the most powerful features of Apache Ranger is tag-based access control (TBAC).  
With TBAC, objects or attributes can be tagged with categories relevant to access control (e.g., "sales region 1").
Users can then be assigned to a "sales region 1" role that represents those with access to data from that region.
A tag-based policy can be written that grants access on objects or attributes tagged with "sales region 1" only to users in the "sales region 1" role.

This enables a single policy to be written to control access to relevant data of any type (relational table, S3 bucket, hive column, 
attribute within the JSON response object of an API, etc).

## Syncing tag/metadata mapping from a metadata repository to Ranger
Tagging of objects/attributes as described is typically stored in a metadata repository.  
The mapping from tag to metadata is then synced with Ranger to make that mapping available for use in policies. 

## Apache Atlas
Apache Atlas is an extensible metadata repository with a richly nested data model. Atlas and Ranger have long been used together for TBAC.
The tagsync-mapper specifies how to map from Atlas objects to the resources of the Ranger plugin service.
In this case, the mapping is between json objects and fields and Ranger nestedstructure plugin schemas and fields.  

In future, we are planning to create nestedstructure_schema and nestedstructure_field supersets, from which json, avro, etc objects can inherit, 
and use those as the basis for tagging.

In creating the fields, the schema's structure is flattened, and field names are represented in "dot notation". For example:

```
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "person": {
      "type": "object",
      "properties": {
        "firstName": {
          "type": "string"
        },
        "lastName": {
          "type": "string"
        },
        "aliases": {
          "type": "array",
          "items": [
            {
              "type": "string"
            }
          ]
        },
        "address": {
          "type": "object",
          "properties": {
            "line1": {
              "type": "string"
            },
            "city": {
              "type": "string"
            },
            "state": {
              "type": "string"
            },
            "zipCode": {
              "type": "string"
            },
            "country": {
              "type": "string"
            }
          }
        }
      }
    }
  }
}


```
has these fields in the Atlas representation:

```
person.firstName,
person.lastName,
person.aliases,
person.address.city,
person.address.country,
person.address.line1,
person.address.state,
person.address.zipCode
```

This is the native Atlas description of the json object and field types:

**json_object**
```
{
  "category": "ENTITY",
  "version": 1,
  "name": "json_object",
  "description": "Atlas Type representing Abstract JSON Schema",
  "typeVersion": "1.0",
  "attributeDefs": [],
  "superTypes": [
    "schema",
    "json_type"
  ],
  "subTypes": [],
  "relationshipAttributeDefs": [
    {
      "name": "fields",
      "typeName": "array<json_field>",
      "isOptional": true,
      "cardinality": "SET",
      "valuesMinCount": -1,
      "valuesMaxCount": -1,
      "isUnique": false,
      "isIndexable": false,
      "includeInNotification": false,
      "searchWeight": -1,
      "relationshipTypeName": "json_object_fields",
      "isLegacyAttribute": true
    }
  ]
}
```
**json_field**
```
{
  "category": "ENTITY",
  "version": 1,
  "name": "json_field",
  "description": "Atlas Type representing an JSON Field",
  "typeVersion": "1.0",
  "attributeDefs": [
    {
      "name": "required",
      "typeName": "boolean",
      "isOptional": true,
      "cardinality": "SINGLE",
      "valuesMinCount": 0,
      "valuesMaxCount": 1,
      "isUnique": false,
      "isIndexable": false,
      "includeInNotification": false,
      "searchWeight": -1
    }
  ],
  "superTypes": [
    "json_type",
    "schema_field"
  ],
  "subTypes": [],
  "relationshipAttributeDefs": [
    {
      "name": "associatedSchema",
      "typeName": "json_object",
      "isOptional": true,
      "cardinality": "SINGLE",
      "valuesMinCount": -1,
      "valuesMaxCount": -1,
      "isUnique": false,
      "isIndexable": false,
      "includeInNotification": false,
      "searchWeight": -1,
      "relationshipTypeName": "json_object_fields",
      "isLegacyAttribute": false
    }
  ]
}
```
