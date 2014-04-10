package liquibase.change.core;

import liquibase.change.ChangeFactory;
import liquibase.change.StandardChangeTest;
import liquibase.change.ChangeMetaData;
import liquibase.database.Database;
import liquibase.database.core.*
import liquibase.snapshot.MockSnapshotGeneratorFactory
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.AddAutoIncrementStatement;
import liquibase.statement.core.AddDefaultValueStatement;
import liquibase.statement.core.CreateSequenceStatement;
import liquibase.statement.core.SetNullableStatement
import liquibase.structure.core.Column
import liquibase.structure.core.Table
import spock.lang.Unroll;

import static org.junit.Assert.*;
import org.junit.Test;

public class AddAutoIncrementChangeTest extends StandardChangeTest {

    def getAppliesTo() {
        expect:
        def change = new AddAutoIncrementChange();
        ChangeFactory.getInstance().getChangeMetaData(change).getAppliesTo().iterator().next() == "column"
    }


    def getConfirmationMessage() throws Exception {
        when:
        def change = new AddAutoIncrementChange();
        change.setSchemaName("SCHEMA_NAME");
        change.setTableName("TABLE_NAME");
        change.setColumnName("COLUMN_NAME");
        change.setColumnDataType("DATATYPE(255)");

        then:
        change.getConfirmationMessage() == "Auto-increment added to TABLE_NAME.COLUMN_NAME"
    }

    def "check change metadata"() {
        expect:
        def change = new AddAutoIncrementChange();
        def metaData = ChangeFactory.getInstance().getChangeMetaData(change);
        metaData.getName() == "addAutoIncrement"

    }

    @Unroll("verifyUpdate with #columnStartWith / #columnIncrementBy vs #changeStartWith / #changeIncrementBy")
    def "verifyUpdate"() {
        when:
        def database = new MockDatabase()
        def snapshotFactory = new MockSnapshotGeneratorFactory()
        SnapshotGeneratorFactory.instance = snapshotFactory

        def table = new Table(null, null, "test_table")
        def testColumn = new Column(Table.class, null, null, table.name, "test_col")
        table.getColumns().add(new Column(Table.class, null, null, table.name, "other_col"))
        table.getColumns().add(testColumn)

        def change = new AddAutoIncrementChange()
        change.tableName = table.name
        change.columnName = testColumn.name

        then:
        assert !change.verifyUpdate(database).verified

        when: "Objects exist but not auto-increment"
        snapshotFactory.addObjects(table)
        then:
        assert !change.verifyUpdate(database).verifiedPassed

        when: "Column is auto-increment"
        testColumn.autoIncrementInformation = new Column.AutoIncrementInformation(columnStartWith, columnIncrementBy)
        change.startWith = changeStartWith
        change.incrementBy = changeIncrementBy
        then:
        change.verifyUpdate(database).verifiedPassed == expectedResult

        where:
        columnStartWith | columnIncrementBy | changeStartWith | changeIncrementBy | expectedResult
        null | null | null | null | true
        2    | 4    | null | null | true
        2    | 4    | 2    | null | true
        2    | 4    | null | 4    | true
        2    | 4    | 2    | 4    | true
        3    | 5    | 1    | 5    | false
        3    | 5    | 3    | 2    | false

    }

    def "verifyRollback"() {
        when:
        def database = new MockDatabase()
        def snapshotFactory = new MockSnapshotGeneratorFactory()
        SnapshotGeneratorFactory.instance = snapshotFactory

        def table = new Table(null, null, "test_table")
        def testColumn = new Column(Table.class, null, null, table.name, "test_col")
        table.getColumns().add(new Column(Table.class, null, null, table.name, "other_col"))
        table.getColumns().add(testColumn)

        def change = new AddAutoIncrementChange()
        change.tableName = table.name
        change.columnName = testColumn.name

        then:
        assert !change.verifyRollback(database).verified

        when: "Objects exist but not auto-increment"
        snapshotFactory.addObjects(table)
        then:
        assert change.verifyRollback(database).verifiedPassed

        when: "Column is auto-increment"
        testColumn.autoIncrementInformation = new Column.AutoIncrementInformation()
        then:
        assert !change.verifyRollback(database).verifiedPassed
    }
}
