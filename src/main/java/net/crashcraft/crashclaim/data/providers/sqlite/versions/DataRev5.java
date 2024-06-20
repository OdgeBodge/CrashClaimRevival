package net.crashcraft.crashclaim.data.providers.sqlite.versions;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import net.crashcraft.crashclaim.data.providers.sqlite.DataType;
import net.crashcraft.crashclaim.data.providers.sqlite.DataVersion;

import java.sql.SQLException;
import java.util.List;

public class DataRev5 implements DataVersion {
    @Override
    public int getVersion() {
        return 5;
    }

    @Override
    public void executeUpgrade(int fromRevision) throws SQLException {
        DB.executeUpdate("PRAGMA foreign_keys = OFF"); // Turn foreign keys off

        DB.executeUpdate("DROP TABLE IF EXISTS \"claim_data_backup\"");

        DB.executeUpdate("CREATE TABLE \"claim_data_backup\" (\n" +
                "\t\"id\"\tINTEGER,\n" +
                "\t\"type\"\tINTEGER,\n" +
                "\t\"minX\"\tINTEGER NOT NULL,\n" +
                "\t\"minZ\"\tINTEGER NOT NULL,\n" +
                "\t\"maxX\"\tINTEGER NOT NULL,\n" +
                "\t\"maxZ\"\tINTEGER NOT NULL,\n" +
                "\t\"world\"\tINTEGER NOT NULL,\n" +
                "\t\"name\"\tTEXT,\n" +
                "\t\"entryMessage\"\tTEXT,\n" +
                "\t\"exitMessage\"\tTEXT,\n" +
                "\tUNIQUE(\"minX\",\"minZ\",\"maxX\",\"maxZ\",\"world\"),\n" +
                "\tPRIMARY KEY(\"id\" AUTOINCREMENT),\n" +
                "\tFOREIGN KEY(\"world\") REFERENCES \"claimworlds\"(\"id\") ON DELETE CASCADE\n" +
                ")");

        DB.executeInsert("INSERT INTO claim_data_backup SELECT id, type, minX, minZ, maxX, maxZ, world, name, entryMessage, exitMessage FROM claim_data");

        DB.executeUpdate("DROP TABLE claim_data");

        DB.executeUpdate("CREATE TABLE \"claim_data\" (\n" +
                "\t\"id\"\tINTEGER,\n" +
                "\t\"type\"\tINTEGER NOT NULL,\n" +
                "\t\"minX\"\tINTEGER NOT NULL,\n" +
                "\t\"minZ\"\tINTEGER NOT NULL,\n" +
                "\t\"maxX\"\tINTEGER NOT NULL,\n" +
                "\t\"maxZ\"\tINTEGER NOT NULL,\n" +
                "\t\"world\"\tINTEGER NOT NULL,\n" +
                "\t\"name\"\tTEXT,\n" +
                "\t\"entryMessage\"\tTEXT,\n" +
                "\t\"exitMessage\"\tTEXT,\n" +
                "\t\"minY\"\tINTEGER NOT NULL DEFAULT -500,\n" +
                "\t\"maxY\"\tINTEGER NOT NULL DEFAULT -500,\n" +
                "\tFOREIGN KEY(\"world\") REFERENCES \"claimworlds\"(\"id\") ON DELETE CASCADE,\n" +
                "\tPRIMARY KEY(\"id\" AUTOINCREMENT),\n" +
                "\tUNIQUE(\"minX\",\"minZ\",\"maxX\",\"maxZ\",\"world\",\"type\",\"minY\",\"maxY\")\n" +
                ")");

        // Insert data from backup to new table without specifying minY and maxY
        DB.executeInsert("INSERT INTO claim_data (id, type, minX, minZ, maxX, maxZ, world, name, entryMessage, exitMessage) " +
                "SELECT id, type, minX, minZ, maxX, maxZ, world, name, entryMessage, exitMessage FROM claim_data_backup");

        DB.executeUpdate("PRAGMA foreign_keys = ON");  // Turn foreign keys back on
    }
}
