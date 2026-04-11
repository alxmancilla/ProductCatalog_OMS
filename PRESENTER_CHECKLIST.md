# Webinar Presenter Checklist

## Pre-Webinar Setup (30 minutes before)

### 1. Environment Setup
- [ ] Java 21 installed and verified: `java -version`
- [ ] Maven installed and verified: `mvn -version`
- [ ] Docker installed and running: `docker --version`
- [ ] `jq` installed for JSON formatting: `jq --version`

### 2. Start MongoDB
```bash
# Option 1: Using docker-compose (recommended)
docker-compose up -d

# Option 2: Using docker directly
docker run -d -p 27017:27017 --name mongodb mongo:8

# Verify MongoDB is running
docker ps
```

### 3. Build the Project
```bash
mvn clean install
```

Expected output: `BUILD SUCCESS`

### 4. Test Run the Application
```bash
mvn spring-boot:run
```

Wait for: `Started DemoApplication in X.XXX seconds`

Press `Ctrl+C` to stop (you'll restart during the demo)

### 5. Prepare Your IDE
- [ ] Open project in IntelliJ IDEA / VS Code / Eclipse
- [ ] Have these files ready to show:
  - `Product.java`
  - `Order.java`
  - `OrderItem.java`
  - `ProductController.java`
  - `OrderController.java`

### 6. Prepare Terminals
Set up 3 terminal windows/tabs:

**Terminal 1: MongoDB Shell**
```bash
mongosh
```

**Terminal 2: Application**
```bash
# Ready to run:
mvn spring-boot:run
```

**Terminal 3: curl Commands**
```bash
# Have demo-commands.sh ready
chmod +x demo-commands.sh
```

### 7. Import the Product Dataset
```bash
# Make script executable (first time only)
chmod +x import-products.sh

# Load all 222 products — idempotent, safe to re-run
./import-products.sh

# Expected response:
# { "imported": 222, "skipped": 0, "errors": [] }

# Or via the web UI: Products → Import Dataset → upload products-dataset.json
```

### 8. Test the Complete Flow
```bash
# In Terminal 2: Start app
mvn spring-boot:run

# In Terminal 3: Import dataset, then run demo script
./import-products.sh
./demo-commands.sh

# In Terminal 1: Verify in MongoDB
use product_catalog_oms
db.products.find().pretty()
db.orders.find().pretty()
```

### 9. Clean the Database (for fresh demo)
```bash
# In mongosh
use product_catalog_oms
db.products.deleteMany({})
db.orders.deleteMany({})
```

### 10. Screen Sharing Setup
- [ ] Close unnecessary applications
- [ ] Increase terminal font size (16-18pt recommended)
- [ ] Increase IDE font size (14-16pt recommended)
- [ ] Test screen sharing with a colleague
- [ ] Have presentation slides ready (if any)

### 11. Backup Plan
- [ ] Have product IDs pre-copied in a text file
- [ ] Have curl commands ready (see DEMO_GUIDE.md or use the Web Interface)
- [ ] Know how to restart MongoDB if needed
- [ ] Have the demo-commands.sh script tested

---

## During Webinar Flow

### Opening (2 min)
1. Welcome attendees
2. Share screen
3. Show the agenda (WEBINAR_OUTLINE.md)

### Demo Part 1: Show the Model (3 min)
1. Open `Product.java` - explain `@Document`
2. Open `OrderItem.java` - highlight NO `@Document`
3. Open `Order.java` - show `List<OrderItem>`

### Demo Part 2: Live Code or Show Controller (5 min)
1. Open `ProductController.java`
2. Explain `@RestController`, `@RequestMapping`
3. Show `@PostMapping` and `@GetMapping`
4. Highlight `productRepository.save()` and `findAll()`

### Demo Part 3: Test the API (5 min)
1. **Terminal 2**: Start application
   ```bash
   mvn spring-boot:run
   ```

2. **Terminal 3**: Create products
   ```bash
   ./demo-commands.sh
   ```
   OR manually:
   ```bash
   curl -X POST http://localhost:8080/products \
     -H "Content-Type: application/json" \
     -d '{"name":"Laptop","price":1299.99,"category":"Electronics","inventory":50}'
   ```

3. **Terminal 1**: Show in MongoDB
   ```bash
   use product_catalog_oms
   db.products.find().pretty()
   ```

4. **Terminal 3**: Create an order (use actual product ID)

5. **Terminal 1**: Show order with embedded items
   ```bash
   db.orders.find().pretty()
   ```

### Wrap Up (2 min)
1. Summarize key points
2. Share resources
3. Q&A

---

## Troubleshooting During Webinar

### MongoDB Connection Failed
```bash
# Check if MongoDB is running
docker ps

# Restart MongoDB
docker-compose restart

# Or restart Docker container
docker restart mongodb
```

### Port 8080 Already in Use
```bash
# Find and kill process
lsof -ti:8080 | xargs kill -9

# Or change port in application.properties
server.port=8081
```

### Application Won't Start
```bash
# Check Java version
java -version  # Should be 21+

# Clean and rebuild
mvn clean install

# Check for errors in pom.xml
mvn validate
```

### curl Command Not Working
- Check application is running: `curl http://localhost:8080/products`
- Verify JSON syntax (use a JSON validator)
- Check Content-Type header is set
- Use the demo-commands.sh script as backup

---

## Post-Webinar

### Share Resources
- [ ] GitHub repository link
- [ ] Slides (if any)
- [ ] Recording link
- [ ] Additional resources from DEMO_GUIDE.md

### Cleanup
```bash
# Stop application (Ctrl+C in Terminal 2)

# Stop MongoDB
docker-compose down

# Or
docker stop mongodb
docker rm mongodb
```

---

## Quick Reference Commands

### Start Everything
```bash
docker-compose up -d
mvn spring-boot:run
```

### Import Product Dataset
```bash
./import-products.sh
```

### Test Everything
```bash
./demo-commands.sh
```

### View in MongoDB
```bash
mongosh
use product_catalog_oms
db.products.find().pretty()
db.orders.find().pretty()
```

### Clean Database
```bash
mongosh
use product_catalog_oms
db.products.deleteMany({})
db.orders.deleteMany({})
```

### Stop Everything
```bash
# Ctrl+C in application terminal
docker-compose down
```

---

## Emergency Contacts
- Technical support: [Your contact]
- Webinar platform support: [Platform support]

---

## Notes Section
Use this space for last-minute notes or changes:

```
[Your notes here]
```

