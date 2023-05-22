package com.practice.gateway.domain;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class DynamicRoute {

    @Id
    private String id;

    @Column(columnDefinition = "TEXT")
    private String uri;

    @Column(columnDefinition = "TEXT")
    private String path;

    @Column(columnDefinition = "TEXT")
    private String rewritePath;

    @Column(columnDefinition = "INTEGER")
    private String stripPrefix;

    @Column(columnDefinition = "TEXT")
    private String filterPath;

}
