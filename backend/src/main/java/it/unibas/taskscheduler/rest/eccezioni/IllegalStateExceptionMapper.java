package it.unibas.taskscheduler.rest.eccezioni;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class IllegalStateExceptionMapper implements ExceptionMapper<IllegalStateException> {

    @Override
    public Response toResponse(IllegalStateException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("errore", exception.getMessage()))
                .build();
    }
}
