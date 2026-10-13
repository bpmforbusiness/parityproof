# Bob MCP context (ParityProof)

Status: **PLANNED — no MCP server is wired yet.** This file is honest about that: nothing
below is configured anywhere; it is the plan for what MCP context we intend to make
available to Bob, for when the repo work starts and for the judges' record.

## What MCP is for here
Bob connects to MCP servers (Model Context Protocol, JSON-RPC 2.0) to reach external
tools/data beyond the repo. For ParityProof the useful context is:

| Candidate server | What it would give Bob | When / why |
|---|---|---|
| **Local filesystem / repo context** | Read `cobol/`, `catalog/`, `parity/copybook.txt` precisely | Always — helps Bob cite source lines |
| **CardDemo data (a database)** | Card transaction sample data for the posting program | Only after Task-5 program is chosen + CardDemo license confirmed |

## Not configured — deliberately
- No `mcp.json` / settings entry exists in this repo (nothing to commit).
- No credentials, tokens or db connection strings are stored anywhere in the repo.

## Security rule (from IBM Bob docs — https://bob.ibm.com/docs/ide/configuration/mcp)
- Never hardcode API keys / tokens in MCP config.
- Use environment variables or a vault; keep the config file out of version control.
- Any MCP config file containing secrets must be `.gitignore`d.

## How to wire it (when Abdul configures on the Oracle VM)
1. Add the MCP server via Bob Settings → MCP (see Understanding MCP + MCP in Bob docs).
2. Point secrets at env vars, not literals.
3. Record the server name + transport here and mark status WIRED.
4. Update this file's Status line from PLANNED to WIRED only when it is actually running,
   so the repo never overclaims.