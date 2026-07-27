# OpenCode Session Export / Import

Export a session to transfer between machines:

```bash
# Interactive (select from list)
opencode export > session-backup.json

# Or by session ID
opencode session list
opencode export <sessionID> > session-backup.json
```

Import on the new machine:

```bash
opencode import session-backup.json
```

Also copy config and agent files:

```bash
# User-level config
~/.config/opencode/

# Project-level config
opencode.json
AGENTS.md
```

Session data is stored in `~/.local/share/opencode/opencode.db` (SQLite) — you can copy the entire database instead of exporting individual sessions.