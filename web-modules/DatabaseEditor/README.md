# DatabaseEditor

`org.solarframework.mu:database-editor` — an admin page to edit any entity's rows, over [`DatabaseImpl`](../../core-modules/DatabaseImpl/README.md) and [`WebUtils`](../WebUtils/README.md).

`DatabaseEditorController` answers under `/admin/db/v1`: `GET /{item}/edit` and `/{item}/edit/{id}` show the form, `POST /{objectName}/update[/{id}]` saves it, `GET /list/{item}` returns the rows as JSON. Every page checks `WebUtils.canAccessPage` first. Templates are in `src/main/resources/templates/dbeditor`.
