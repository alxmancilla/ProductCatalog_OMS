# 🔒 Security Best Practices

## ⚠️ Critical: Never Commit Credentials!

This project has been configured to **prevent credential leakage**. Follow these guidelines:

---

## 🔐 Environment Variables (Required)

### Local Development

```bash
# Create .env file (not tracked in git)
cp .env.example .env

# Edit .env with your local MongoDB URI
export MONGODB_URI="mongodb://localhost:27017/product_catalog"
export MONGODB_DATABASE="product_catalog"

# Load environment variables
source .env

# Or load all at once
export $(cat .env | xargs)

# Start application
mvn spring-boot:run
```

### Production Deployment

**Never hardcode production credentials!**

```bash
# AWS EC2 / ECS
# Set via environment variables in task definition

# Kubernetes
# Use Secrets:
kubectl create secret generic mongodb-secret \
  --from-literal=uri='mongodb+srv://prod-user:SECRET@cluster.mongodb.net/'

# AWS Elastic Beanstalk
# Set via EB Console or eb setenv

# Heroku
heroku config:set MONGODB_URI=mongodb+srv://...
```

---

## 📁 Files Ignored by Git

The following files are in `.gitignore` and **will not be committed**:

```
✅ .env
✅ .env.local
✅ .env.*.local
✅ src/main/resources/application.properties
✅ src/main/resources/application-*.properties (with credentials)
```

---

## ✅ Safe to Commit

These files **can be committed** (no secrets):

```
✅ .env.example (template only)
✅ application.properties.template (template only)
✅ application-dev.properties (uses localhost default)
✅ application-prod.properties (requires env var)
```

---

## 🔒 MongoDB Security Checklist

### Database User Permissions

Create a dedicated database user with **minimum required permissions**:

```javascript
// MongoDB Shell
use admin

db.createUser({
  user: "product_catalog_app",
  pwd: "STRONG_PASSWORD_HERE",
  roles: [
    { role: "readWrite", db: "product_catalog" },
    { role: "read", db: "product_catalog" } // For analytics
  ]
})
```

**DO NOT** use admin or root user in application!

### Connection String Security

✅ **Good** (environment variable):
```bash
export MONGODB_URI="mongodb+srv://app-user:${SECRET}@cluster.mongodb.net/?w=majority"
```

❌ **Bad** (hardcoded):
```properties
spring.data.mongodb.uri=mongodb+srv://admin:password123@cluster.mongodb.net/
```

### Write Concern (Data Safety)

Always use `w=majority&journal=true` for production:

```
mongodb://host:27017/?w=majority&journal=true
```

This ensures:
- Data is replicated to majority of nodes
- Write is journaled to disk
- Survives primary failure

---

## 🛡️ Production Hardening

### 1. Network Security

- **MongoDB Atlas:** Enable IP whitelist
- **Self-hosted:** Use VPC/private network
- **Never** expose MongoDB to public internet

### 2. TLS/SSL

Always use TLS in production:

```
mongodb+srv://...  # Atlas always uses TLS
mongodb://host:27017/?tls=true&tlsCAFile=/path/to/ca.pem
```

### 3. Authentication

- **Atlas:** Uses SCRAM-SHA-256 (secure)
- **Self-hosted:** Enable authentication:

```yaml
# mongod.conf
security:
  authorization: enabled
```

### 4. Secrets Management

**Development:**
- Use `.env` file (not committed)

**Production:**
- AWS: Use Secrets Manager or Parameter Store
- GCP: Use Secret Manager
- Azure: Use Key Vault
- Kubernetes: Use Secrets
- HashiCorp Vault

---

## 🚨 If Credentials Are Leaked

### Immediate Actions

1. **Rotate credentials immediately**
   ```bash
   # MongoDB Atlas: Reset password in UI
   # Self-hosted:
   db.updateUser("username", { pwd: "NEW_PASSWORD" })
   ```

2. **Revoke compromised tokens**
3. **Review audit logs** for unauthorized access
4. **Update all deployment configurations**

### Git History Cleanup

If credentials were committed to git:

```bash
# DANGER: This rewrites git history!
# Backup first!

# Remove from history
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch src/main/resources/application.properties" \
  --prune-empty --tag-name-filter cat -- --all

# Force push (coordinate with team!)
git push --force --all
```

**Better:** Use [BFG Repo-Cleaner](https://rtyley.github.io/bfg-repo-cleaner/)

---

## 📚 Additional Resources

- [MongoDB Security Checklist](https://www.mongodb.com/docs/manual/administration/security-checklist/)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)

---

**Remember:** Security is not a feature, it's a requirement!
