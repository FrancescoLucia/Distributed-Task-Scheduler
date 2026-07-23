package it.unibas.taskscheduler.rest.eccezioni;

import it.unibas.taskscheduler.service.WorkflowNonValidoException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class WorkflowNonValidoExceptionMapper implements ExceptionMapper<WorkflowNonValidoException> {

    @Override
    public Response toResponse(WorkflowNonValidoException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("errori", exception.getErrori()))
                .build();
    }
}
