package com.log;

import java.text.SimpleDateFormat;
import java.util.Date;

/*
CREATE TABLE `requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(100) NOT NULL,
  `datetime` bigint NOT NULL,
  `httpMethod` varchar(100) NOT NULL,
  `path` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
);
*/
public class Request {
    public static SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ssZ");

    private Long id;
    private String userId;
    private Date datetime;
    private String httpMethod;
    private String path;

    public Request(final String userId, final Date datetime, final String httpMethod, final String path) {
        this(null, userId, datetime, httpMethod, path);
    }

    public Request(final Long id, final String userId, final Date datetime, final String httpMethod, final String path) {
        this.id = id;
        this.userId = userId;
        this.datetime = datetime;
        this.httpMethod = httpMethod;
        this.path = path;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Date getDatetime() {
        return datetime;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }
}
