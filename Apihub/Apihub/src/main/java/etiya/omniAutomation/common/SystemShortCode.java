package etiya.omniAutomation.common;

public enum SystemShortCode {
    DCE("DCE"),
    OM("OM"),
    NTFENGINE("NTFENGINE"),
    FRONT("FRONT");

    private String message;

    SystemShortCode(String message) {
        this.message = message;
    }

    @Override
    public String toString()
    {
        return this.message;
    }
}
