# ElasticM2M Core Search

## SearchRequest

Examples:

```json

{
  "offset" : 0,
  "limit" : 10,
  "query" : {
    "op" : "and",
    "of" : [ {
      "op" : "native",
      "value" : "{\"properties.vin\": \"3C4PDCAB4GT233400}"
    }, {
      "op" : "native",
      "value" : "select * from derp group by date"
    }, {
      "op" : "native",
      "value" : {
        "properties.vin" : "3C4PDCAB4GT233400"
      }
    }, {
      "op" : "range",
      "field" : "voltage",
      "lt" : 10
    }, {
      "op" : "not",
      "of" : [ {
        "op" : "term",
        "field" : "make",
        "value" : "Ford"
      }, {
        "op" : "regex",
        "field" : "make",
        "value" : "^Chev.*"
      }, {
        "op" : "prefix",
        "field" : "make",
        "value" : "Chev"
      }, {
        "op" : "phrase",
        "field" : "dealer",
        "value" : [ "Jim", "Bob" ]
      } ]
    }, {
      "op" : "all"
    } ]
  },
  "headers" : {
    "echo" : true
  },
  "fieldSet" : "test",
  "fields" : [ "id", "title" ],
  "sorts" : [ {
    "field" : "timestamp",
    "direction" : "Descending"
  } ],
  "aggs" : [ {
    "op" : "terms",
    "field" : "year",
    "size" : 10,
    "key" : "year",
    "sort" : null,
    "aggs" : [ ]
  }, {
    "op" : "terms",
    "field" : "make",
    "size" : 10,
    "key" : "make",
    "sort" : null,
    "aggs" : [ ]
  }, {
    "op" : "terms",
    "field" : "model",
    "size" : 10,
    "key" : "model",
    "sort" : null,
    "aggs" : [ ]
  } ],
  "countTotal" : true
}

```


## SearchResult

Example:

```json

{
  "headers": {},
  "totalItems": 378,
  "fields": [],
  "aggs": {
    "type": {
      "key": "type",
      "buckets": [
        {
          "key": "consumer",
          "count": 275
        },
        {
          "key": "dealer",
          "count": 97
        },
        {
          "key": "inventory",
          "count": 7
        }
      ]
    }
  },
  "items": [
    {
      "id": "01e6acf1-6128-4c71-9b4a-1ed4f1493097",
      "name": "Bulk Device Transfer - Target",
      "description": null,
      "type": "dealer",
      "brand": "theftpatrol",
      "organization": "inventory",
      "orgPath": [
        "root",
        "8af74c8e-edf9-4e33-8ac5-d2b833f7927d",
        "inventory",
        "01e6acf1-6128-4c71-9b4a-1ed4f1493097"
      ]
    }
  ]
}




```