package com.log;

import java.util.Date;

/*
CREATE TABLE `sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(100) NOT NULL,
  `start_datetime` bigint NOT NULL,
  `duration` bigint NOT NULL,
  PRIMARY KEY (`id`)
);
 */
public class Session {
    private Long id;
    private String userId;
    private Date startDatetime;
    private Long duration;

    public Session(final String userId, final Date startDatetime) {
        this(null, userId, startDatetime, 0L);
    }

    public Session(final String userId, final Date startDatetime, final Long duration) {
        this(null, userId, startDatetime, duration);
    }

    public Session(final Long id, final String userId, final Date startDatetime, final Long duration) {
        this.id = id;
        this.userId = userId;
        this.startDatetime = startDatetime;
        this.duration = duration;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Date getStartDatetime() {
        return startDatetime;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(final Long duration) {
        this.duration = duration;
    }
}
