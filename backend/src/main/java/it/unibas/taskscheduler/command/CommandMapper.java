package it.unibas.taskscheduler.command;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class CommandMapper {

    public static final String SCRIPT = "SCRIPT";
    public static final String MATH = "MATH";
    public static final String FILE = "FILE";
    public static final String HTTP = "HTTP";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CommandMapper() {
    }

    public static Command from(String type, String payload) {
        return switch (type) {
            case SCRIPT -> new ScriptCommand(payload);
            case MATH -> new CalcoloCommand(leggiPayload(payload, CalcoloCommand.Params.class));
            case FILE -> new FileIOCommand(leggiPayload(payload, FileIOCommand.Params.class));
            case HTTP -> new HttpRequestCommand(leggiPayload(payload, HttpRequestCommand.Params.class));
            default -> throw new IllegalArgumentException("Tipo comando non supportato: " + type);
        };
    }

    public static String typeOf(Command command) {
        if (command instanceof ScriptCommand) {
            return SCRIPT;
        }
        if (command instanceof CalcoloCommand) {
            return MATH;
        }
        if (command instanceof FileIOCommand) {
            return FILE;
        }
        if (command instanceof HttpRequestCommand) {
            return HTTP;
        }
        throw new IllegalArgumentException("Comando non supportato: " + command.getClass().getName());
    }

    public static String payloadOf(Command command) {
        if (command instanceof ScriptCommand scriptCommand) {
            return scriptCommand.getComando();
        }
        if (command instanceof CalcoloCommand calcoloCommand) {
            return scriviPayload(calcoloCommand.getParams());
        }
        if (command instanceof FileIOCommand fileIOCommand) {
            return scriviPayload(fileIOCommand.getParams());
        }
        if (command instanceof HttpRequestCommand httpRequestCommand) {
            return scriviPayload(httpRequestCommand.getParams());
        }
        throw new IllegalArgumentException("Comando non supportato: " + command.getClass().getName());
    }

    private static <T> T leggiPayload(String payload, Class<T> tipo) {
        try {
            return MAPPER.readValue(payload, tipo);
        } catch (Exception e) {
            throw new RuntimeException("Errore lettura payload comando", e);
        }
    }

    private static String scriviPayload(Object params) {
        try {
            return MAPPER.writeValueAsString(params);
        } catch (Exception e) {
            throw new RuntimeException("Errore serializzazione payload comando", e);
        }
    }
}
