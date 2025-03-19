package enums;

import lombok.Getter;

@Getter
public enum AirlineTable {
    DELTAS("deltas"),
    SOUTHWESTS("southwests");

    private final String tableName;

    AirlineTable(String tableName) {
        this.tableName = tableName;
    }

}

