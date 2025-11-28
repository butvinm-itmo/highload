# Tarology Config Repository

This repository contains centralized configuration for all Tarology microservices.

## Structure

- `application.yml` - Shared configuration (database, JPA, Flyway, SpringDoc)
- `user-service.yml` - User service specific (port 8081, Flyway table)
- `tarot-service.yml` - Tarot service specific (port 8082, Flyway table)
- `divination-service.yml` - Divination service specific (port 8083, Feign client URLs, Flyway table)

## Environment Variables

All configs support environment variable substitution:

### Database Configuration
- `DB_HOST` - Database host (default: localhost)
- `DB_PORT` - Database port (default: 5432)
- `DB_NAME` - Database name (default: tarot_db)
- `DB_USER` - Database username (default: tarot_user)
- `DB_PASSWORD` - Database password

### Service URLs (divination-service only)
- `USER_SERVICE_URL` - User service URL (default: http://localhost:8081)
- `TAROT_SERVICE_URL` - Tarot service URL (default: http://localhost:8082)

## Usage

Configs are served by config-server (port 8888). Services fetch configuration on startup using:
```yaml
spring:
  config:
    import: optional:configserver:http://config-server:8888
```

## Future Profiles

Currently using default profile. Future profiles can be added:
- `application-dev.yml` - Development environment
- `application-prod.yml` - Production environment
- `{service}-dev.yml` - Service-specific dev configs
- `{service}-prod.yml` - Service-specific prod configs

## Updating Configuration

1. Clone or update the repository:
   ```bash
   git clone https://github.com/butvinm-itmo/highload-config.git
   cd highload-config
   ```

2. Make changes to the configuration files
3. Commit and push to this repository:
   ```bash
   git add .
   git commit -m "Update configuration"
   git push origin main
   ```
3. Restart config-server to pull latest changes:
   ```bash
   docker compose restart config-server
   ```
4. Restart affected services to pick up new configuration:
   ```bash
   docker compose restart user-service tarot-service divination-service
   ```

## Security Notes

- Sensitive values (passwords, tokens) use environment variables
- For production, consider using encrypted properties with Spring Cloud Config encryption
- Restrict repository access to authorized team members only
