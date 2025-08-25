const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 5300;

// Middleware
app.use(cors());
app.use(express.json());

// Initialize SQLite database
const dbPath = path.join(__dirname, 'armchair.db');
const db = new sqlite3.Database(dbPath);

// Initialize database tables
db.serialize(() => {
  // Locations table
  db.run(`
    CREATE TABLE IF NOT EXISTS locations (
      id TEXT PRIMARY KEY,
      display_name TEXT NOT NULL,
      bounds_x INTEGER,
      bounds_y INTEGER,
      bounds_w INTEGER,
      bounds_h INTEGER,
      background1_data TEXT,
      background2_data TEXT,
      foreground1_data TEXT,
      foreground2_data TEXT,
      blocked_positions TEXT,
      placements_data TEXT,
      connection_triggers_data TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);

  // Characters table
  db.run(`
    CREATE TABLE IF NOT EXISTS characters (
      id TEXT PRIMARY KEY,
      display_name TEXT NOT NULL,
      color TEXT NOT NULL,
      sprite_file TEXT,
      sprite_x INTEGER,
      sprite_y INTEGER,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);
});

// Locations API endpoints
app.get('/api/locations', (req, res) => {
  db.all('SELECT * FROM locations ORDER BY display_name', (err, rows) => {
    if (err) {
      res.status(500).json({ error: err.message });
      return;
    }
    
    // Transform rows to match ClojureScript data format
    const locations = rows.map(row => ({
      'entity/id': row.id,
      'entity/type': 'location',
      'display-name': row.display_name,
      bounds: {
        x: row.bounds_x,
        y: row.bounds_y,
        w: row.bounds_w,
        h: row.bounds_h
      },
      background1: row.background1_data ? JSON.parse(row.background1_data) : {},
      background2: row.background2_data ? JSON.parse(row.background2_data) : {},
      foreground1: row.foreground1_data ? JSON.parse(row.foreground1_data) : {},
      foreground2: row.foreground2_data ? JSON.parse(row.foreground2_data) : {},
      blocked: row.blocked_positions ? JSON.parse(row.blocked_positions) : [],
      placements: row.placements_data ? JSON.parse(row.placements_data) : {},
      'connection-triggers': row.connection_triggers_data ? JSON.parse(row.connection_triggers_data) : {}
    }));
    
    res.json(locations);
  });
});

app.get('/api/locations/:id', (req, res) => {
  const { id } = req.params;
  
  db.get('SELECT * FROM locations WHERE id = ?', [id], (err, row) => {
    if (err) {
      res.status(500).json({ error: err.message });
      return;
    }
    
    if (!row) {
      res.status(404).json({ error: 'Location not found' });
      return;
    }
    
    const location = {
      'entity/id': row.id,
      'entity/type': 'location',
      'display-name': row.display_name,
      bounds: {
        x: row.bounds_x,
        y: row.bounds_y,
        w: row.bounds_w,
        h: row.bounds_h
      },
      background1: row.background1_data ? JSON.parse(row.background1_data) : {},
      background2: row.background2_data ? JSON.parse(row.background2_data) : {},
      foreground1: row.foreground1_data ? JSON.parse(row.foreground1_data) : {},
      foreground2: row.foreground2_data ? JSON.parse(row.foreground2_data) : {},
      blocked: row.blocked_positions ? JSON.parse(row.blocked_positions) : [],
      placements: row.placements_data ? JSON.parse(row.placements_data) : {},
      'connection-triggers': row.connection_triggers_data ? JSON.parse(row.connection_triggers_data) : {}
    };
    
    res.json(location);
  });
});

// Characters API endpoints
app.get('/api/characters', (req, res) => {
  db.all('SELECT * FROM characters ORDER BY display_name', (err, rows) => {
    if (err) {
      res.status(500).json({ error: err.message });
      return;
    }
    
    // Transform rows to match ClojureScript data format
    const characters = rows.map(row => ({
      'entity/id': row.id,
      'entity/type': 'character',
      'display-name': row.display_name,
      color: row.color,
      sprite: [row.sprite_file, { x: row.sprite_x, y: row.sprite_y }]
    }));
    
    res.json(characters);
  });
});

app.get('/api/characters/:id', (req, res) => {
  const { id } = req.params;
  
  db.get('SELECT * FROM characters WHERE id = ?', [id], (err, row) => {
    if (err) {
      res.status(500).json({ error: err.message });
      return;
    }
    
    if (!row) {
      res.status(404).json({ error: 'Character not found' });
      return;
    }
    
    const character = {
      'entity/id': row.id,
      'entity/type': 'character',
      'display-name': row.display_name,
      color: row.color,
      sprite: [row.sprite_file, { x: row.sprite_x, y: row.sprite_y }]
    };
    
    res.json(character);
  });
});

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Armchair API server is running' });
});

// Start server
app.listen(PORT, () => {
  console.log(`Armchair API server running on http://localhost:${PORT}`);
});

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\nShutting down server...');
  db.close();
  process.exit(0);
});