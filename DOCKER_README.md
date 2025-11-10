# Docker Deployment Guide for IHDRS Backend

This guide explains how to deploy the IHDRS (Intelligent Handwritten Digit Recognition System) backend services using Docker.

## Architecture Overview

The system consists of the following services:
- **PostgreSQL**: Database for storing user data, recognition records, and model metadata
- **Redis**: Cache for improving performance
- **Model Service**: Python Flask service for ML model inference and training
- **Backend**: Java Spring Boot service for API gateway and business logic

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 4GB RAM available
- At least 10GB disk space

## Quick Start

### 1. Build and Start All Services

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f backend
docker-compose logs -f model-service
```

### 2. Check Service Health

```bash
# Check all containers are running
docker-compose ps

# Check backend health
curl http://localhost:8080/api/health

# Check model service health
curl http://localhost:5000/health

# Check model service ping
curl http://localhost:5000/ping
```

### 3. Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: This deletes all data)
docker-compose down -v
```

## Service Endpoints

### Backend Service (Port 8080)
- Base URL: `http://localhost:8080/api`
- Health Check: `GET /api/health`
- Recognition: `POST /api/recognition/recognize`
- Models: `GET /api/models/list`
- Authentication: `POST /api/auth/login`

### Model Service (Port 5000)
- Base URL: `http://localhost:5000`
- Health Check: `GET /health`
- Ping: `GET /ping`
- Recognition: `POST /api/recognize`
- Training: `POST /api/train`
- Training Status: `GET /api/train/status`

### Database
- PostgreSQL: `localhost:5432`
- Database: `ihdrs_db`
- Username: `ihdrs_user`
- Password: `ihdrs_password`

### Cache
- Redis: `localhost:6379`

## Configuration

### Environment Variables

You can customize the deployment by setting environment variables in `docker-compose.yml`:

#### Backend Service
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ihdrs_db
  - SPRING_DATASOURCE_USERNAME=ihdrs_user
  - SPRING_DATASOURCE_PASSWORD=ihdrs_password
  - MODEL_SERVICE_BASE_URL=http://model-service:5000
  - JWT_SECRET=your_secret_key_here
  - JAVA_OPTS=-Xms512m -Xmx1024m
```

#### Model Service
```yaml
environment:
  - FLASK_ENV=production
  - FLASK_HOST=0.0.0.0
  - FLASK_PORT=5000
```

### Volume Mounts

The following directories are mounted as volumes:

#### Backend
- `./logs:/app/logs` - Application logs
- `./uploads:/app/uploads` - Uploaded files
- `./models:/app/models` - Model files

#### Model Service
- `./ihdrs-model-service/models:/app/models` - ML model files
- `./ihdrs-model-service/logs:/app/logs` - Service logs
- `./ihdrs-model-service/data:/app/data` - Training data

## Building Individual Services

### Build Backend Only
```bash
docker build -t ihdrs-backend:latest .
```

### Build Model Service Only
```bash
docker build -t ihdrs-model-service:latest ./ihdrs-model-service
```

## Troubleshooting

### Service Won't Start

1. Check logs:
```bash
docker-compose logs [service-name]
```

2. Check if ports are already in use:
```bash
# Linux/Mac
lsof -i :8080
lsof -i :5000
lsof -i :5432
lsof -i :6379

# Windows
netstat -ano | findstr :8080
```

3. Restart services:
```bash
docker-compose restart [service-name]
```

### Database Connection Issues

1. Ensure PostgreSQL is healthy:
```bash
docker-compose ps postgres
```

2. Check database logs:
```bash
docker-compose logs postgres
```

3. Connect to database manually:
```bash
docker-compose exec postgres psql -U ihdrs_user -d ihdrs_db
```

### Model Service Issues

1. Check if models directory exists and contains model files:
```bash
ls -la ihdrs-model-service/models/
```

2. Check Python dependencies:
```bash
docker-compose exec model-service pip list
```

3. Test model service directly:
```bash
curl http://localhost:5000/ping
```

### Memory Issues

If services are crashing due to memory:

1. Increase Docker memory limit in Docker Desktop settings
2. Adjust Java heap size in docker-compose.yml:
```yaml
environment:
  - JAVA_OPTS=-Xms256m -Xmx512m
```

## Production Deployment

### Security Considerations

1. **Change default passwords** in docker-compose.yml:
   - PostgreSQL password
   - JWT secret

2. **Use environment files** instead of hardcoding secrets:
```bash
# Create .env file
echo "POSTGRES_PASSWORD=your_secure_password" > .env
echo "JWT_SECRET=your_jwt_secret" >> .env

# Reference in docker-compose.yml
environment:
  - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
```

3. **Enable HTTPS** using a reverse proxy (nginx/traefik)

4. **Restrict network access** using Docker networks

5. **Regular backups** of PostgreSQL data:
```bash
docker-compose exec postgres pg_dump -U ihdrs_user ihdrs_db > backup.sql
```

### Performance Optimization

1. **Increase connection pools** in application-prod.yml
2. **Enable Redis persistence** if needed
3. **Use production-grade database** settings
4. **Monitor resource usage**:
```bash
docker stats
```

## Monitoring

### View Resource Usage
```bash
docker stats
```

### View Container Logs
```bash
# All services
docker-compose logs -f

# Specific service with timestamps
docker-compose logs -f --timestamps backend
```

### Access Container Shell
```bash
# Backend
docker-compose exec backend sh

# Model Service
docker-compose exec model-service bash

# Database
docker-compose exec postgres psql -U ihdrs_user -d ihdrs_db
```

## Maintenance

### Update Services
```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose up -d --build
```

### Clean Up
```bash
# Remove stopped containers
docker-compose rm

# Remove unused images
docker image prune

# Remove unused volumes (WARNING: Data loss)
docker volume prune
```

### Backup Database
```bash
# Create backup
docker-compose exec postgres pg_dump -U ihdrs_user ihdrs_db > backup_$(date +%Y%m%d).sql

# Restore backup
docker-compose exec -T postgres psql -U ihdrs_user -d ihdrs_db < backup_20250127.sql
```

## Support

For issues and questions:
1. Check logs: `docker-compose logs`
2. Verify health endpoints
3. Review this documentation
4. Check GitHub issues

## License

[Your License Here]
