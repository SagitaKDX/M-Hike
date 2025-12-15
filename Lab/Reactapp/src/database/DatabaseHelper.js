import * as SQLite from 'expo-sqlite';

let db = null;

const getDatabase = async () => {
  if (db === null) {
    db = await SQLite.openDatabaseAsync('mhike.db');
  }
  return db;
};

export const initDatabase = async () => {
  try {
    const database = await getDatabase();
    
    await database.execAsync(`
      CREATE TABLE IF NOT EXISTS hikes (
        hikeId INTEGER PRIMARY KEY,
        name TEXT NOT NULL,
        location TEXT NOT NULL,
        date TEXT NOT NULL,
        parkingAvailable INTEGER NOT NULL,
        length REAL NOT NULL,
        difficulty TEXT NOT NULL,
        description TEXT,
        createdAt DATETIME DEFAULT CURRENT_TIMESTAMP
      );
    `);
    
    await database.execAsync(`
      CREATE TABLE IF NOT EXISTS observations (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        hikeId INTEGER NOT NULL,
        observation TEXT NOT NULL,
        time TEXT NOT NULL,
        comments TEXT,
        createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (hikeId) REFERENCES hikes(hikeId) ON DELETE CASCADE
      );
    `);
    
    console.log('Database initialized successfully');
    return true;
  } catch (error) {
    console.log('Error initializing database:', error);
    return false;
  }
};

export const insertHike = async (hike) => {
  try {
    const database = await getDatabase();
    const result = await database.runAsync(
      `INSERT INTO hikes (hikeId, name, location, date, parkingAvailable, length, difficulty, description) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        hike.hikeId,
        hike.name,
        hike.location,
        hike.date,
        hike.parkingAvailable ? 1 : 0,
        hike.length,
        hike.difficulty,
        hike.description || ''
      ]
    );
    return { success: true, result };
  } catch (error) {
    console.log('Error inserting hike:', error);
    return { success: false, error };
  }
};

export const getAllHikes = async () => {
  try {
    const database = await getDatabase();
    const hikes = await database.getAllAsync('SELECT * FROM hikes ORDER BY date DESC');
    return hikes;
  } catch (error) {
    console.log('Error fetching hikes:', error);
    return [];
  }
};

export const getHikeById = async (id) => {
  try {
    const database = await getDatabase();
    const hike = await database.getFirstAsync('SELECT * FROM hikes WHERE hikeId = ?', [id]);
    return hike;
  } catch (error) {
    console.log('Error fetching hike:', error);
    return null;
  }
};

export const updateHike = async (id, hike) => {
  try {
    const database = await getDatabase();
    const result = await database.runAsync(
      `UPDATE hikes 
       SET name = ?, location = ?, date = ?, parkingAvailable = ?, length = ?, 
           difficulty = ?, description = ?
       WHERE hikeId = ?`,
      [
        hike.name,
        hike.location,
        hike.date,
        hike.parkingAvailable ? 1 : 0,
        hike.length,
        hike.difficulty,
        hike.description || '',
        id
      ]
    );
    return { success: true, result };
  } catch (error) {
    console.log('Error updating hike:', error);
    return { success: false, error };
  }
};

export const deleteHike = async (id) => {
  try {
    const database = await getDatabase();
    await database.runAsync('DELETE FROM hikes WHERE hikeId = ?', [id]);
    return { success: true };
  } catch (error) {
    console.log('Error deleting hike:', error);
    return { success: false, error };
  }
};

export const deleteAllHikes = async () => {
  try {
    const database = await getDatabase();
    await database.runAsync('DELETE FROM hikes');
    return { success: true };
  } catch (error) {
    console.log('Error deleting all hikes:', error);
    return { success: false, error };
  }
};

export const searchHikesByName = async (searchTerm) => {
  try {
    const database = await getDatabase();
    const hikes = await database.getAllAsync(
      'SELECT * FROM hikes WHERE name LIKE ? ORDER BY date DESC',
      [`%${searchTerm}%`]
    );
    return hikes;
  } catch (error) {
    console.log('Error searching hikes:', error);
    return [];
  }
};

export const advancedSearchHikes = async (criteria) => {
  try {
    const database = await getDatabase();
    let query = 'SELECT * FROM hikes WHERE 1=1';
    const params = [];

    if (criteria.name) {
      query += ' AND name LIKE ?';
      params.push(`%${criteria.name}%`);
    }
    if (criteria.location) {
      query += ' AND location LIKE ?';
      params.push(`%${criteria.location}%`);
    }
    if (criteria.date) {
      query += ' AND date = ?';
      params.push(criteria.date);
    }
    if (criteria.length) {
      query += ' AND length = ?';
      params.push(criteria.length);
    }

    query += ' ORDER BY date DESC';

    const hikes = await database.getAllAsync(query, params);
    return hikes;
  } catch (error) {
    console.log('Error in advanced search:', error);
    return [];
  }
};

export const insertObservation = async (observation) => {
  try {
    const database = await getDatabase();
    const result = await database.runAsync(
      'INSERT INTO observations (hikeId, observation, time, comments) VALUES (?, ?, ?, ?)',
      [observation.hikeId, observation.observation, observation.time, observation.comments || '']
    );
    return { success: true, result };
  } catch (error) {
    console.log('Error inserting observation:', error);
    return { success: false, error };
  }
};

export const getObservationsByHikeId = async (hikeId) => {
  try {
    const database = await getDatabase();
    const observations = await database.getAllAsync(
      'SELECT * FROM observations WHERE hikeId = ? ORDER BY time DESC',
      [hikeId]
    );
    return observations;
  } catch (error) {
    console.log('Error fetching observations:', error);
    return [];
  }
};

export const updateObservation = async (id, observation) => {
  try {
    const database = await getDatabase();
    const result = await database.runAsync(
      'UPDATE observations SET observation = ?, time = ?, comments = ? WHERE id = ?',
      [observation.observation, observation.time, observation.comments || '', id]
    );
    return { success: true, result };
  } catch (error) {
    console.log('Error updating observation:', error);
    return { success: false, error };
  }
};

export const deleteObservation = async (id) => {
  try {
    const database = await getDatabase();
    await database.runAsync('DELETE FROM observations WHERE id = ?', [id]);
    return { success: true };
  } catch (error) {
    console.log('Error deleting observation:', error);
    return { success: false, error };
  }
};
