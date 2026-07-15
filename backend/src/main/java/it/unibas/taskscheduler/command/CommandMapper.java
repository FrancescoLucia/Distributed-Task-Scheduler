package it.unibas.taskscheduler.command;

public final class CommandMapper {

    public static final String SCRIPT = "SCRIPT";

    private CommandMapper() {
    }

    public static Command from(String type, String payload) {
        if (SCRIPT.equals(type)) {
            return new ScriptCommand(payload);
        }
        throw new IllegalArgumentException("Tipo comando non supportato: " + type);
    }

    public static String typeOf(Command command) {
        if (command instanceof ScriptCommand) {
            return SCRIPT;
        }
        throw new IllegalArgumentException("Comando non supportato: " + command.getClass().getName());
    }

    public static String payloadOf(Command command) {
        if (command instanceof ScriptCommand scriptCommand) {
            return scriptCommand.getComando();
        }
        throw new IllegalArgumentException("Comando non supportato: " + command.getClass().getName());
    }
}
