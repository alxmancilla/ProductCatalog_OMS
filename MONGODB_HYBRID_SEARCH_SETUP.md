# MongoDB Hybrid Search Setup Guide

This guide explains how to set up **MongoDB Hybrid Search** for the Product Catalog using **Voyage AI embeddings** and **MongoDB Atlas**.

---

## 🎯 What is Hybrid Search?

**Hybrid Search** combines two powerful search methods:

1. **Vector Search** (Semantic) - Understands meaning and context
   - "running shoes" matches "athletic sneakers"
   - "gift for developer" matches "MongoDB Guide" book
   
2. **Text Search** (Keyword) - Exact and partial word matching
   - "MongoDB" matches "MongoDB Guide"
   - "laptop" matches "Laptop Pro 15"

**Result:** Best of both worlds - semantic understanding + keyword precision!

---

## 📋 Prerequisites

1. ✅ **MongoDB Atlas M10+ Cluster** (Vector Search requires M10 or higher)
2. ✅ **Voyage AI API Key** (Get from https://www.voyageai.com/)
3. ✅ **Products with embeddings** (Generated using `EmbeddingGenerator`)

---

## 🚀 Step 1: Configure Voyage AI

### 1.1 Get Voyage AI API Key

1. Go to https://www.voyageai.com/
2. Sign up or log in
3. Navigate to API Keys section
4. Create a new API key
5. Copy the key

### 1.2 Update `application.properties`

```properties
# Voyage AI Configuration
voyage.ai.key=YOUR_VOYAGE_AI_KEY_HERE
voyage.ai.url=https://api.voyageai.com/v1
voyage.ai.model=voyage-4-large
voyage.ai.input-type=document
voyage.ai.timeout-seconds=30
```

---

## 🤖 Step 2: Generate Embeddings for Products

### 2.1 Enable Embedding Generation

Edit `application.properties`:

```properties
# Enable embedding generation (set to true ONCE, then back to false)
generate.embeddings.enabled=true
```

### 2.2 Start the Application

```bash
mvn spring-boot:run
```

The `EmbeddingGenerator` will automatically:
- Find all products without embeddings
- Generate embeddings using Voyage AI (voyage-4-large)
- Save embeddings to the `descriptionEmbedding` field
- Process in batches of 10 products

**Expected Output:**
```
🤖 Starting embedding generation for products...
Found 110 products needing embeddings (out of 110 total)
Processing batch 1/11 (10 products)...
✅ Batch 1/11 complete (10/110 products processed)
...
🎉 Embedding generation complete! Processed 110 products
💡 Remember to disable embedding generation: generate.embeddings.enabled=false
```

### 2.3 Disable Embedding Generation

After embeddings are generated, edit `application.properties`:

```properties
# Disable embedding generation
generate.embeddings.enabled=false
```

Restart the application.

---

## 🔍 Step 3: Create Vector Search Index in MongoDB Atlas

### 3.1 Open Atlas UI

1. Go to https://cloud.mongodb.com/
2. Select your cluster
3. Click **"Search"** tab
4. Click **"Create Search Index"**

### 3.2 Choose Index Type

- Select **"Atlas Vector Search"**
- Click **"Next"**

### 3.3 Configure Vector Index

**Index Name:** `vector_index`

**Database:** `demo_pc_oms`

**Collection:** `products`

**Index Definition (JSON):**

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "descriptionEmbedding",
      "numDimensions": 1024,
      "similarity": "cosine"
    }
  ]
}
```

**Explanation:**
- `path`: Field containing the embedding vector
- `numDimensions`: 1024 (voyage-4-large output size)
- `similarity`: "cosine" (recommended for Voyage AI embeddings)

### 3.4 Create Index

- Click **"Create Search Index"**
- Wait for index to build (usually 1-2 minutes)
- Status should show **"Active"**

---

## 📝 Step 4: Create Text Search Index (Optional - for Full Hybrid)

### 4.1 Create Text Index

1. In Atlas UI, go to **"Search"** tab
2. Click **"Create Search Index"**
3. Select **"Atlas Search"** (not Vector Search)

### 4.2 Configure Text Index

**Index Name:** `text_index`

**Database:** `demo_pc_oms`

**Collection:** `products`

**Index Definition (JSON):**

```json
{
  "mappings": {
    "dynamic": false,
    "fields": {
      "name": {
        "type": "string",
        "analyzer": "lucene.standard"
      },
      "description": {
        "type": "string",
        "analyzer": "lucene.standard"
      },
      "category": {
        "type": "string",
        "analyzer": "lucene.keyword"
      },
      "sku": {
        "type": "string",
        "analyzer": "lucene.keyword"
      }
    }
  }
}
```

### 4.3 Create Index

- Click **"Create Search Index"**
- Wait for index to build
- Status should show **"Active"**

---

## ✅ Step 5: Verify Hybrid Search is Working

### 5.1 Check Application Logs

When the application starts, you should see:

```
🚀 Hybrid Search ENABLED (110/110 products have embeddings)
```

If you see this instead:
```
⚠️  Hybrid Search DISABLED (no products have embeddings yet)
```

Go back to Step 2 and generate embeddings.

### 5.2 Test with AI Order Assistant

**Test Query 1 (Semantic):**
```
POST http://localhost:8080/ai/order
{
  "message": "I want something comfortable for running"
}
```

**Expected:** Should find "Athletic Sneakers" or "Running Shoes" even though you didn't use those exact words!

**Test Query 2 (Exact Match):**
```
POST http://localhost:8080/ai/order
{
  "message": "I want a MongoDB book"
}
```

**Expected:** Should find "MongoDB Guide" book

---

## 🎯 How It Works

### Architecture

```
User Query: "comfortable running shoes"
         ↓
1. Generate Query Embedding (Voyage AI)
   [0.234, -0.567, 0.891, ..., 0.123] (1024 dimensions)
         ↓
2. MongoDB Vector Search ($vectorSearch)
   - Finds products with similar embeddings
   - Uses cosine similarity
   - Returns top 10 candidates
         ↓
3. Rank Results
   - Sort by vector search score
   - Return best match
         ↓
Result: "Athletic Sneakers" (score: 0.95)
```

### Code Flow

1. **ProductMatcherService.matchProducts()** - Entry point
2. **matchSingleProductHybrid()** - Uses hybrid search
3. **hybridSearch()** - Executes MongoDB aggregation
4. **buildHybridSearchPipeline()** - Builds $vectorSearch pipeline
5. **VoyageAiService.generateQueryEmbedding()** - Gets query embedding

---

## 📊 Performance & Cost

### Embedding Generation (One-Time)

- **110 products** × ~50 tokens/description = ~5,500 tokens
- **Cost:** ~$0.01 (Voyage AI pricing)
- **Time:** ~30 seconds (batched)

### Query Embeddings (Per Search)

- **1 query** × ~10 tokens = 10 tokens
- **Cost:** ~$0.0001 per search
- **Time:** ~100ms

### MongoDB Atlas

- **Vector Search:** Included in M10+ clusters
- **Storage:** Minimal (~1MB for 110 products with 1024-dim embeddings)

---

## 🐛 Troubleshooting

### Error: "Hybrid search not available"

**Cause:** Products don't have embeddings yet

**Solution:** Run Step 2 to generate embeddings

### Error: "Failed to generate embeddings"

**Cause:** Invalid Voyage AI API key or network issue

**Solution:** 
1. Check API key in `application.properties`
2. Verify network connectivity
3. Check Voyage AI service status

### Error: "Index not found: vector_index"

**Cause:** Vector search index not created in Atlas

**Solution:** Complete Step 3 to create the index

### Low Search Quality

**Possible Causes:**
1. Index not fully built (wait a few minutes)
2. Wrong similarity metric (use "cosine" for Voyage AI)
3. Wrong number of dimensions (must be 1024 for voyage-4-large)

**Solution:** Verify index configuration in Atlas UI

---

## 🎉 Success!

You now have MongoDB Hybrid Search working with Voyage AI embeddings!

**Benefits:**
- ✅ Semantic search - understands meaning, not just keywords
- ✅ Better product matching - "running shoes" finds "athletic sneakers"
- ✅ Improved AI agent - more accurate order processing
- ✅ Production-ready - scales with MongoDB Atlas

**Next Steps:**
- Test with various queries
- Monitor search quality
- Tune similarity thresholds
- Add more products and regenerate embeddings

---

## 📚 Additional Resources

- [MongoDB Atlas Vector Search Docs](https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-overview/)
- [Voyage AI Documentation](https://docs.voyageai.com/)
- [Voyage AI voyage-4-large Model](https://docs.voyageai.com/docs/embeddings)
- [Reciprocal Rank Fusion (RRF)](https://www.mongodb.com/docs/atlas/atlas-vector-search/tutorials/reciprocal-rank-fusion/)

