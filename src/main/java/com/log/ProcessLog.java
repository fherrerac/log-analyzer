package com.log;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.Date;
import java.util.List;

public class ProcessLog {
    static String LOGS_DIR = "resources";
    static long MAX_SESSION_DURATION = 600000; // 10 mins = 10 x 60 x 1000 = 600000 millis

    private LogAnalyzerDao logAnalyzerDaoMySql = new LogAnalyzerDaoMySql();

    public static void main(String[] args) {
        ProcessLog pl = new ProcessLog();
        pl.processLogFiles();
    }

    private void processLogFiles() {
        processRequests();
        processSessions();
    }

    private void processRequests() {
        long start = System.currentTimeMillis();
        File logsDir = new File(LOGS_DIR);
        BufferedReader br = null;

        try {
            boolean keepDbConnection = true;

            for (File logFile : logsDir.listFiles()) {
                System.out.println("Processing requests from file: " + logFile.getName());

                String line = "";
                br = new BufferedReader(new FileReader(logFile));
                while ((line = br.readLine()) != null) {
                    String[] requestTokens = line.split(" ", -1);

                    if (requestTokens.length < 7) {
                        // log error processing line and continue with next
                        continue;
                    }

                    String requestDatetimeStr = requestTokens[3];
                    String requestDatetimeTimeZoneStr = requestTokens[4];

                    String httpMethod = requestTokens[5].replace("\"", "");
                    String path = requestTokens[6];
                    String[] pathSegments = path.split("/", -1);

                    if (StringUtils.isEmpty(requestDatetimeStr) ||
                        StringUtils.isEmpty(requestDatetimeTimeZoneStr) ||
                        StringUtils.isEmpty(httpMethod) ||
                        StringUtils.isEmpty(path) ||
                        pathSegments.length < 4 ||
                        StringUtils.isEmpty(path)) {
                        // log error processing line and continue with next
                        continue;
                    }
                    String userId = pathSegments[3];

                    Date requestDatetime = null;
                    try {
                        requestDatetime = Request.DATETIME_FORMAT.parse(requestDatetimeStr + requestDatetimeTimeZoneStr);
                    }
                    catch (ParseException e) {
                        e.printStackTrace();
                        // log error processing line and continue with next
                        continue;
                    }

                    Request request = new Request(userId, requestDatetime, httpMethod, path);
                    logAnalyzerDaoMySql.addRequest(request, keepDbConnection);
                }
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("Error processing log file");
        }
        catch (IOException e) {
            throw new RuntimeException("Error processing log file");
        }
        finally {
            logAnalyzerDaoMySql.closeDbConnection();

            if (br != null) {
                try {
                    br.close();
                }
                catch (IOException e) {
                    throw new RuntimeException("Error processing log file");
                }
            }
        }

        long end = System.currentTimeMillis();

        Duration duration = Duration.ofMillis(end - start);
        long minutesPart = duration.toMinutes();
        long secondsPart = duration.minusMinutes(minutesPart).getSeconds();
        System.out.println(String.format("Time to process logs: %d mins %d secs", minutesPart, secondsPart));
    }

    private void processSessions() {
        long start = System.currentTimeMillis();
        boolean keepDbConnection = true;

        // get all users
        List<String> userIds = logAnalyzerDaoMySql.getUsers(keepDbConnection);

        // loop over users
        for (String userId : userIds) {
            System.out.println("Processing sessions for user: " + userId);

            // get all userId requests sorted by time
            List<Request> requests = logAnalyzerDaoMySql.getUserRequests(userId, keepDbConnection);

            // loop over userId requests
            for (Request request : requests) {
                Session lastUserSession = logAnalyzerDaoMySql.getLastUserSession(userId, keepDbConnection);
                if (lastUserSession == null) {
                    // if there is no session yet create one with duration as zero
                    Session session = new Session(userId, request.getDatetime());
                    logAnalyzerDaoMySql.addSession(session, keepDbConnection);
                }
                else {
                    // calculate the delta with the last request
                    Long requestDelta = request.getDatetime().getTime() -
                                        (lastUserSession.getStartDatetime().getTime() + lastUserSession.getDuration());
                    if (requestDelta <= MAX_SESSION_DURATION) {
                        // the last request is less than MAX_SESSION_DURATION  apart (10 minutes)
                        // then increase session duration with delta
                        lastUserSession.setDuration(lastUserSession.getDuration() + requestDelta);
                        logAnalyzerDaoMySql.updateSession(lastUserSession, keepDbConnection);
                    }
                    else {
                        // the last request is more than MAX_SESSION_DURATION  apart (10 minutes)
                        // then create a new session
                        Session session = new Session(userId, request.getDatetime());
                        logAnalyzerDaoMySql.addSession(session, keepDbConnection);
                    }
                }
            }
        }
        logAnalyzerDaoMySql.closeDbConnection();

        long end = System.currentTimeMillis();

        Duration duration = Duration.ofMillis(end - start);
        long minutesPart = duration.toMinutes();
        long secondsPart = duration.minusMinutes(minutesPart).getSeconds();
        System.out.println(String.format("Time to process sessions: %d mins %d secs", minutesPart, secondsPart));
    }
}
