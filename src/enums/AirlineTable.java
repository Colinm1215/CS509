package enums;

public enum AirlineTable {
    DELTAS("deltas"),
    SOUTHWESTS("southwests");

    private final String tableName;

    AirlineTable(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return this.tableName;
    }
}

