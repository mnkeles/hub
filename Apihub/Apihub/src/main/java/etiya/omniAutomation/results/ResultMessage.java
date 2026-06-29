package etiya.omniAutomation.results;

public enum ResultMessage {
    SUCCESS("success"),
    IS_ACTV_CHECK("is_actv column should be checked"),
    IS_PROCESS_FLOW_ID_CHECK("proc_flow_id column should be checked"),
    IS_PROCESS_FLOW_SHORT_CODE_CHECK("shrt_code column should be checked"),
    IS_PROCESS_FLOW_BODY_CHECK("RequestBody is not valid"),
    EMAIL_NOT_UNIQUE("Email used"),
    USER_NOT_ENABLED("User not enabled"),
    INVALID_PASSWORD("Invalid password"),
    ROLE_DEFINED("Role defined"),
    ALREADY_ADDED("Already added"),
    AUTHORIZATION_NOT_DEFINED("Authorization not defined");

    private String message;

    ResultMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString()
    {
        return this.message;
    }
}
