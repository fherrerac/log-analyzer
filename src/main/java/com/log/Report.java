package com.log;

import java.util.ArrayList;
import java.util.List;

public class Report {
    private Integer uniqueUsers;
    private List<UserReport> userReports = new ArrayList<>();

    public Integer getUniqueUsers() {
        return uniqueUsers;
    }

    public void setUniqueUsers(final Integer uniqueUsers) {
        this.uniqueUsers = uniqueUsers;
    }

    public List<UserReport> getUserReports() {
        return userReports;
    }

    public void setUserReports(final List<UserReport> userReports) {
        this.userReports = userReports;
    }

    public static class UserReport {
        private String userId;
        private Integer pagesVisited;
        private Integer numberOfSessions;
        private Long longestSession;
        private Long shortestSession;

        public String getUserId() {
            return userId;
        }

        public void setUserId(final String userId) {
            this.userId = userId;
        }

        public Integer getPagesVisited() {
            return pagesVisited;
        }

        public void setPagesVisited(final Integer pagesVisited) {
            this.pagesVisited = pagesVisited;
        }

        public Integer getNumberOfSessions() {
            return numberOfSessions;
        }

        public void setNumberOfSessions(final Integer numberOfSessions) {
            this.numberOfSessions = numberOfSessions;
        }

        public Long getLongestSession() {
            return longestSession;
        }

        public void setLongestSession(final Long longestSession) {
            this.longestSession = longestSession;
        }

        public Long getShortestSession() {
            return shortestSession;
        }

        public void setShortestSession(final Long shortestSession) {
            this.shortestSession = shortestSession;
        }
    }
}
