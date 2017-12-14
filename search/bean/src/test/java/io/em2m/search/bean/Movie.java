/**
 * ELASTIC M2M Inc. CONFIDENTIAL
 * __________________
 *
 * Copyright (c) 2013-2016 Elastic M2M Incorporated, All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elastic M2M Incorporated
 *
 * The intellectual and technical concepts contained
 * herein are proprietary to Elastic M2M Incorporated
 * and may be covered by U.S. and Foreign Patents,  patents in
 * process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elastic M2M Incorporated.
 */
package io.em2m.search.bean;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Movie {
    private String id;
    private String type;
    private Map<String, Object> fields;

    static Map<String, Movie> load() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, Movie.class);
        List<Movie> movies = mapper.readValue(Movie.class.getResourceAsStream("/moviedata2.json"), listType);
        Map<String, Movie> result = new HashMap<>();
        movies.forEach(movie -> {
            result.put(movie.id, movie);
        });
        return result;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }
}
