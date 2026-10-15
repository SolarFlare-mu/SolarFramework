# Excel

`org.solarframework.mu:excel` — turns an Excel workbook into database tables, one table per sheet, over Apache POI.

```java
new ExcelConverter("input.xlsx", "jdbc:sqlite:output.db")
        .withDrop(true)      // drop the tables named after the sheets
        .withCreate(true)    // create them from the sheets' headers
        .withRows(true)      // insert the rows
        .Convert();
```

`GUI` is a small window over the same converter.
