# SimpleNPCs
Simple "NPC" interaction plugin for binding commands to entities

## Features

- Simple, composable entity interactions via command binding

## Config Overview
```yaml
# Timeout between binding command and NPC interaction
bindtimeout: PT10S # Accepts ISO-8601 duration format PnDTnHnMn.nS
```

For more information on the format, see [`java.time.Duration#parse`](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-)

## Commands

- `/bindnpc <command> [args...]`
  - Bind the given command and args to the next entity you interact with (right click)
- `/unbindnpc`
  - Unbind a command to the next entity you interact with
- `/cancelnpc`
  - Force cancel an awaiting NPC bind/unbind (don't bind or unbind on your next entity interaction)

## Permissions

- `simplenpc.*`
  - "Wildcard" for all below permissions
- `simplenpc.bind`
  - Allows binding NPC commands via `bindnpc`
- `simplenpc.unbind`
  - Allows unbinding (deleting) NPC commands via `unbindnpc`
- `simplenpc.interact`
  - Allows interaction with SimpleNPCs' NPCs
