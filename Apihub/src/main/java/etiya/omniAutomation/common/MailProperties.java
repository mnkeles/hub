package etiya.omniAutomation.common;

public enum MailProperties {
    TO("gizem.taskin@etiya.com,berkan.unsal@etiya.com"),
    FROM("fizz_automation@etiya.com"),
    SUBJECT("Fizz Test Automation"),
    CONTENT("Automation çalıştı.");

    private String mailProperty;

    MailProperties(String mailProperty) {
        this.mailProperty = mailProperty;
    }

    @Override
    public String toString()
    {
        return this.mailProperty;
    }
}