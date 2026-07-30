# Security Policy

## Supported Version

This repository is a student project and currently supports only the latest code on the `main` branch.

| Version | Supported |
| ------- | --------- |
| main    | yes       |
| older branches | no |

## Reporting a Vulnerability

Please do not publish real credentials, tokens, mail passwords, database dumps, or private mailbox content in public issues.

If you find a vulnerability, report it privately to the repository maintainers through GitHub or the agreed project communication channel.

Please include:

- A short description of the problem
- Steps to reproduce
- Affected component, for example frontend, backend, Docker, mail, database, or attachment storage
- Expected impact
- Whether real credentials or private data may be involved

## Secrets and Configuration

The application must not commit real secrets. Runtime-specific values must be configured through environment variables.

Examples of sensitive values:

- Database passwords
- JWT secrets
- SMTP and IMAP credentials
- S3 or SeaweedFS access keys
- Real mailbox addresses if they are private

Use `.env.example` only with placeholders. The real `.env` file must stay local and must not be committed.

## Scope

Security-relevant areas include:

- Authentication and JWT handling
- REST API access through `/api`
- Docker and Caddy exposure
- PostgreSQL configuration
- SMTP/IMAP mailbox access
- SeaweedFS/S3 attachment storage
- File upload limits and attachment downloads
