# SQLite Database Integration for Armchair

This project now includes a Node.js backend with SQLite database integration for managing locations and characters.

## Architecture

- **Frontend**: ClojureScript/React application using Re-frame for state management
- **Backend**: Node.js Express server with SQLite database
- **Database**: SQLite with tables for locations and characters

## Backend API

The server provides REST endpoints for:

### Locations
- `GET /api/locations` - Retrieve all locations
- `GET /api/locations/:id` - Retrieve a specific location by ID

### Characters
- `GET /api/characters` - Retrieve all characters
- `GET /api/characters/:id` - Retrieve a specific character by ID

### Health Check
- `GET /api/health` - Server health status

## Database Schema

### Locations Table
- `id` (TEXT, PRIMARY KEY) - UUID identifier
- `display_name` (TEXT) - Human-readable name
- `bounds_x`, `bounds_y`, `bounds_w`, `bounds_h` (INTEGER) - Bounding box
- `background1_data`, `background2_data`, `foreground1_data`, `foreground2_data` (TEXT) - Sprite layer data (JSON)
- `blocked_positions` (TEXT) - Array of blocked positions (JSON)
- `placements_data` (TEXT) - Character placements (JSON)
- `connection_triggers_data` (TEXT) - Location connections (JSON)
- `created_at`, `updated_at` (DATETIME) - Timestamps

### Characters Table
- `id` (TEXT, PRIMARY KEY) - UUID identifier
- `display_name` (TEXT) - Character name
- `color` (TEXT) - Character color
- `sprite_file` (TEXT) - Sprite sheet filename
- `sprite_x`, `sprite_y` (INTEGER) - Sprite coordinates
- `created_at`, `updated_at` (DATETIME) - Timestamps

## Running the Application

### Development Mode
```bash
npm run dev
```

This starts:
- ClojureScript compilation and hot-reload
- Sass compilation
- Tailwind CSS compilation
- Backend API server on port 5300

### Backend Only
```bash
npm run server
```

### Database Setup
```bash
npm run seed
```

### Testing
```bash
npm run test:api
```

## Frontend Integration

The frontend includes:
- API client (`src/cljs/armchair/api.cljs`) for backend communication
- New events for loading data from API
- React components for displaying database data
- Menu item "Database View" to access the new interface

The application automatically loads locations and characters from the database on startup while preserving the existing editor functionality.