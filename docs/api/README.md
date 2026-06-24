# GoTrip OpenAPI

The OpenAPI source is organized as:

- `gotrip-openapi.yaml` is the main entry point.
- `paths/` contains endpoint groups.
- Reusable schemas, parameters, responses, and security schemes live in `gotrip-openapi.yaml`.

Components are kept in the main file so Swagger UI shows clean model names instead of file-path-based names for externally referenced schemas.

## Notifications

Notification endpoints are defined in `paths/notifications.yaml`.

- `GET /notification-preferences` and `PUT /notification-preferences` manage the current user's notification preference.
- `GET /notifications` returns all notifications for the current user.

## Preview With Swagger UI Watcher

Run Swagger UI locally:

```bash
npx swagger-ui-watcher docs/api/gotrip-openapi.yaml
```

The command prints a local URL. Open it in a browser to view the API docs.
