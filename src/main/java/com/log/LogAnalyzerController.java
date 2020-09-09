package com.log;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("report")
public class LogAnalyzerController {
    private LogAnalyzerService logAnalyzerService = new LogAnalyzerService();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReport() {
        Report report = logAnalyzerService.getReport();

        if (report == null) {
            return Response.ok().entity(new LogAnalyzerError(LogAnalyzerError.Code.NOT_FOUND)).build();
        } else {
            return Response.ok().entity(report).build();
        }
    }
}
