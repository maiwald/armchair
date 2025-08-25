const test = require('assert');
const http = require('http');

// Simple test to verify our API endpoints work
function testAPI() {
  console.log('Testing API endpoints...');
  
  // Test health endpoint
  const healthReq = http.get('http://localhost:5300/api/health', (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
      const result = JSON.parse(data);
      test.strictEqual(result.status, 'ok', 'Health check should return ok');
      console.log('✓ Health endpoint works');
      
      // Test characters endpoint
      testCharacters();
    });
  });
  
  healthReq.on('error', (err) => {
    console.error('Health test failed:', err.message);
    process.exit(1);
  });
}

function testCharacters() {
  const charactersReq = http.get('http://localhost:5300/api/characters', (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
      const characters = JSON.parse(data);
      test.ok(Array.isArray(characters), 'Characters should be an array');
      test.ok(characters.length > 0, 'Should have at least one character');
      test.ok(characters[0]['entity/id'], 'Character should have entity/id');
      test.ok(characters[0]['display-name'], 'Character should have display-name');
      console.log('✓ Characters endpoint works');
      
      // Test locations endpoint
      testLocations();
    });
  });
  
  charactersReq.on('error', (err) => {
    console.error('Characters test failed:', err.message);
    process.exit(1);
  });
}

function testLocations() {
  const locationsReq = http.get('http://localhost:5300/api/locations', (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
      const locations = JSON.parse(data);
      test.ok(Array.isArray(locations), 'Locations should be an array');
      test.ok(locations.length > 0, 'Should have at least one location');
      test.ok(locations[0]['entity/id'], 'Location should have entity/id');
      test.ok(locations[0]['display-name'], 'Location should have display-name');
      console.log('✓ Locations endpoint works');
      
      console.log('All API tests passed! 🎉');
      process.exit(0);
    });
  });
  
  locationsReq.on('error', (err) => {
    console.error('Locations test failed:', err.message);
    process.exit(1);
  });
}

// Run tests
testAPI();