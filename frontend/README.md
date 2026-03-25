# ResumeAI — Smart Resume Analyzer

A full-stack AI-powered resume analysis platform that uses **RAG (Retrieval-Augmented Generation)**, **vector search**, and **LLM-based evaluation** to match candidates to job descriptions intelligently.

Built with **Spring Boot**, **React**, **PostgreSQL + pgvector**, and **OpenAI APIs**.

---

## What It Does

Upload resumes (PDF or text) → automatically chunk and embed them into a vector database → then search, match, analyze, and rank candidates against any job description using semantic similarity and AI-generated insights.

### Core Features

- **Resume Upload & Processing** — Upload PDF resumes or paste text. The system extracts content, splits it into sentence-aware overlapping chunks, generates embeddings via OpenAI, and stores them in PostgreSQL with pgvector.

- **Semantic Search** — Search across all resume chunks using natural language queries. Returns the most relevant sections ranked by cosine similarity with confidence scores.

- **Job Matching** — Paste a job description to find the best matching resumes. Uses a weighted scoring algorithm (70% best chunk score, 30% average top-K chunk score) to rank candidates.

- **RAG Analysis** — Retrieves the most relevant resume chunks for a job description, then feeds them as context to GPT-4o-mini for a detailed evaluation including strengths, skill gaps, and improvement suggestions.

- **Candidate Ranking** — Rank all candidates against a job description with medal-style leaderboard display (gold, silver, bronze).

---

## Architecture

```
┌─────────────────┐         ┌──────────────────┐        ┌─────────────────┐
│   React Frontend │ ──API──▶│  Spring Boot API  │──SQL──▶│  PostgreSQL +   │
│   (Port 3000)    │◀───────│  (Port 8080)      │◀──────│  pgvector       │
└─────────────────┘         └────────┬───────────┘        └─────────────────┘
                                     │
                            ┌────────▼───────────┐
                            │   OpenAI APIs       │
                            │  • Embeddings       │
                            │  • Chat Completions │
                            └─────────────────────┘
```

### How the RAG Pipeline Works

1. **Chunking** — Resume text is split into ~500-character chunks at sentence boundaries with ~100-character overlap to preserve context across chunk edges.

2. **Embedding** — Each chunk is embedded using OpenAI's `text-embedding-3-small` model (1536 dimensions).

3. **Storage** — Chunks and their vector embeddings are stored in PostgreSQL using the pgvector extension.

4. **Retrieval** — When a job description is provided, it's embedded and compared against all stored chunks using cosine distance. The top-K most similar chunks are retrieved.

5. **Generation** — Retrieved chunks are passed as context to GPT-4o-mini, which generates a structured evaluation of the candidate's fit.

### Scoring Algorithm

The matching system uses an exponential decay scoring function:

```
score = 100 × e^(-α × cosine_distance)
```

Where `α = 2.0` controls the penalty for distance. For job matching, the final score is a weighted combination:

```
final_score = 0.7 × best_chunk_score + 0.3 × avg_top_K_score
```

This balances peak relevance (best chunk) with overall consistency (average across top chunks).

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, Axios, React Dropzone, React Markdown, Lucide Icons |
| Backend | Java 21, Spring Boot 3.5, Spring Data JPA, Spring WebFlux |
| Database | PostgreSQL with pgvector extension |
| AI/ML | OpenAI text-embedding-3-small (embeddings), GPT-4o-mini (analysis) |
| PDF Processing | Apache PDFBox |
| Build | Maven (backend), npm (frontend) |

---

## Project Structure

```
resume-analyzer/
├── src/main/java/com/example/resume_analyzer/
│   ├── config/
│   │   ├── WebClientConfig.java        # WebClient bean
│   │   └── CorsConfig.java             # CORS for React frontend
│   ├── controller/
│   │   └── ResumeController.java       # REST API endpoints
│   ├── domain/
│   │   ├── Resume.java                 # Resume entity
│   │   └── ResumeChunk.java            # Chunk entity with vector ref
│   ├── dto/
│   │   ├── AnalyzeRequest.java
│   │   ├── AnalyzeResponse.java
│   │   ├── CandidateRankingResult.java
│   │   ├── JobMatchRequest.java
│   │   ├── JobMatchResult.java
│   │   ├── JobRankingRequest.java
│   │   ├── SearchRequest.java
│   │   └── SearchResult.java
│   ├── repository/
│   │   ├── ResumeRepository.java       # Basic CRUD
│   │   └── ResumeChunkRepository.java  # Vector search queries
│   └── service/
│       ├── EmbeddingService.java       # OpenAI embeddings
│       ├── OpenAIChatService.java      # GPT-4o-mini chat
│       ├── PdfService.java             # PDF text extraction
│       └── ResumeService.java          # Core business logic
├── frontend/
│   ├── src/
│   │   ├── api.js                      # Axios API client
│   │   ├── App.js                      # Main app with navigation
│   │   ├── index.css                   # Design system
│   │   ├── components/
│   │   │   └── ScoreBadge.js           # Score display component
│   │   └── pages/
│   │       ├── UploadPage.js           # PDF upload & text input
│   │       ├── SearchPage.js           # Semantic search
│   │       ├── MatchPage.js            # Job matching
│   │       ├── AnalyzePage.js          # RAG analysis
│   │       └── RankPage.js             # Candidate ranking
│   └── package.json
├── pom.xml
└── README.md
```

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- Node.js 18+
- Docker (for PostgreSQL + pgvector)
- OpenAI API key

### 1. Start the Database

```bash
docker run -d \
  --name resume-db \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=resumedb \
  -p 5432:5432 \
  ankane/pgvector
```

Then set up the schema:

```bash
docker exec -it resume-db psql -U admin -d resumedb -c "
  CREATE EXTENSION IF NOT EXISTS vector;
  CREATE TABLE resumes (
    id UUID PRIMARY KEY,
    content TEXT,
    created_at TIMESTAMP
  );
  CREATE TABLE resume_chunks (
    id UUID PRIMARY KEY,
    resume_id UUID REFERENCES resumes(id),
    chunk_index INTEGER,
    chunk_text TEXT,
    embedding vector(1536)
  );
"
```

### 2. Set Environment Variable

```bash
# Mac/Linux
export OPENAI_API_KEY=your-key-here

# Windows PowerShell
$env:OPENAI_API_KEY="your-key-here"
```

### 3. Run the Application

```bash
# Install frontend dependencies (first time only)
cd frontend
npm install
cd ..

# Start both backend and frontend
cd frontend
npm run dev
```

Backend runs on `http://localhost:8080`, frontend on `http://localhost:3000`.

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/resumes` | List all resumes |
| `POST` | `/api/resumes` | Save resume from text |
| `POST` | `/api/resumes/upload` | Upload PDF resume |
| `POST` | `/api/resumes/search` | Semantic search across chunks |
| `POST` | `/api/resumes/match` | Match resumes to job description |
| `POST` | `/api/resumes/analyze` | Analyze resume with skill extraction |
| `POST` | `/api/resumes/analyze-rag` | AI-powered RAG analysis |
| `POST` | `/api/resumes/jobs/rank` | Rank candidates for a job |

---

## Key Design Decisions

**Sentence-aware chunking with overlap** — Instead of fixed-size character splits that cut mid-word, the system splits on sentence boundaries with configurable overlap. This ensures each chunk contains coherent thoughts, producing higher-quality embeddings.

**Cosine distance over L2** — Cosine distance normalizes for vector magnitude, making it more suitable for comparing text embeddings where the direction of the vector matters more than its length. Scores stay in the 0-1 range.

**Weighted scoring for job matching** — A 70/30 split between best chunk score and average top-K score prevents a single lucky chunk match from dominating while still rewarding resumes with deep, focused relevance.

**Exponential decay scoring** — Converts raw cosine distance to a 0-100 score using exponential decay, which naturally penalizes poor matches more aggressively than linear conversion.

---

## Future Improvements

- [ ] Custom skill taxonomy with LLM-powered extraction
- [ ] Cross-encoder re-ranking for improved retrieval precision
- [ ] Evaluation pipeline with precision@k and MRR metrics
- [ ] Batch resume upload and processing
- [ ] User authentication and resume ownership
- [ ] Deployment with Docker Compose
