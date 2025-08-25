const sqlite3 = require('sqlite3').verbose();
const path = require('path');

// Initialize database
const dbPath = path.join(__dirname, 'armchair.db');
const db = new sqlite3.Database(dbPath);

// Sample data for seeding
const sampleLocations = [
  {
    id: '121fb127-fbc8-44b9-ba62-2ca2517b6995',
    display_name: 'Village',
    bounds_x: 0,
    bounds_y: 0,
    bounds_w: 25,
    bounds_h: 14,
    background1_data: JSON.stringify({}), // Simplified for demo
    background2_data: JSON.stringify({}),
    foreground1_data: JSON.stringify({}),
    foreground2_data: JSON.stringify({}),
    blocked_positions: JSON.stringify([]),
    placements_data: JSON.stringify({}),
    connection_triggers_data: JSON.stringify({})
  },
  {
    id: 'e5b01f7e-d6d1-4e29-8623-f0500b1e933a',
    display_name: 'Tavern',
    bounds_x: -9,
    bounds_y: -4,
    bounds_w: 12,
    bounds_h: 13,
    background1_data: JSON.stringify({}),
    background2_data: JSON.stringify({}),
    foreground1_data: JSON.stringify({}),
    foreground2_data: JSON.stringify({}),
    blocked_positions: JSON.stringify([]),
    placements_data: JSON.stringify({}),
    connection_triggers_data: JSON.stringify({})
  }
];

const sampleCharacters = [
  {
    id: 'c05f9046-a148-4001-9549-b0a3ea22d205',
    display_name: 'Hugo',
    color: 'rgba(255, 0, 0, .6)',
    sprite_file: 'characters.png',
    sprite_x: 2,
    sprite_y: 23
  },
  {
    id: '455e8d64-2345-4014-af28-ba4eff184af4',
    display_name: 'Gustav',
    color: 'rgba(92, 154, 9, 0.8)',
    sprite_file: 'characters.png',
    sprite_x: 1,
    sprite_y: 21
  },
  {
    id: '3834c55d-ba4c-4211-91ee-3887dd620076',
    display_name: 'Conni',
    color: 'blue',
    sprite_file: 'characters.png',
    sprite_x: 3,
    sprite_y: 43
  }
];

// Create tables and seed the database
db.serialize(() => {
  console.log('Creating tables...');
  
  // Create locations table
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

  // Create characters table
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

  console.log('Seeding locations...');
  
  const locationStmt = db.prepare(`
    INSERT OR REPLACE INTO locations (
      id, display_name, bounds_x, bounds_y, bounds_w, bounds_h,
      background1_data, background2_data, foreground1_data, foreground2_data,
      blocked_positions, placements_data, connection_triggers_data
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);
  
  sampleLocations.forEach(location => {
    locationStmt.run([
      location.id,
      location.display_name,
      location.bounds_x,
      location.bounds_y,
      location.bounds_w,
      location.bounds_h,
      location.background1_data,
      location.background2_data,
      location.foreground1_data,
      location.foreground2_data,
      location.blocked_positions,
      location.placements_data,
      location.connection_triggers_data
    ]);
  });
  
  locationStmt.finalize();
  
  console.log('Seeding characters...');
  
  const characterStmt = db.prepare(`
    INSERT OR REPLACE INTO characters (
      id, display_name, color, sprite_file, sprite_x, sprite_y
    ) VALUES (?, ?, ?, ?, ?, ?)
  `);
  
  sampleCharacters.forEach(character => {
    characterStmt.run([
      character.id,
      character.display_name,
      character.color,
      character.sprite_file,
      character.sprite_x,
      character.sprite_y
    ]);
  });
  
  characterStmt.finalize();
  
  console.log('Database seeded successfully!');
});

db.close();