require('dotenv').config();
const express = require('express');
const sql = require('mssql');

const app = express();
app.use(express.json());

// MSSQL Konfiguration
const config = {
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  server: process.env.DB_SERVER,
  database: process.env.DB_NAME,
  options: {
    encrypt: true,
    trustServerCertificate: false
  },
  pool: {
    max: 10,
    min: 0,
    idleTimeoutMillis: 30000
  }
};

// Einfaches CORS für Entwicklung (prod. einschränken)
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept');
  next();
});

app.get('/getColumn', async (req, res) => {
  const filterCol = req.query.filterCol || '';
  const table = req.query.table || '';
  const selectCol = req.query.selectCol || '';
  const filterVal = req.query.filterVal

  const queryText = `SELECT ${selectCol} FROM dbo.${table} WHERE ${filterCol} = @filterVal`;

    try {
      await sql.connect(config);
      const request = new sql.Request();
      request.input('filterVal', sql.NVarChar, filterVal);
      const result = await request.query(queryText);

      // Rückgabe: array von Objekten mit dem selectCol-Key
      // Beispiel: [{ "Spiele": "Skat" }, ...]
      return res.json(result.recordset);
    } catch (err) {
      console.error('DB-Error:', err);
      return res.status(500).json({ error: err.message });
    } finally {
      try { await sql.close(); } catch (e) {}
    }


});

// Helper: sichere Query-Ausführung (öffnet und schließt Verbindung)
async function executeQuery(query, inputs = []) {
  try {
    const pool = await sql.connect(config);
    const req = pool.request();
    for (const inp of inputs) req.input(inp.name, inp.type, inp.value);
    const result = await req.query(query);
    return result.recordset;
  } finally {
    try { await sql.close(); } catch (e) {}
  }
}

app.get('/completeTable', async (req, res) => {
  const table = req.query.table || '';
  const query = `SELECT * FROM dbo.${table}`;
  try {
    const rows = await executeQuery(query);
    return res.json(rows);
  } catch (err) {
    console.error('DB-Error:', err);
    return res.status(500).json({ error: err.message });
  }
});

// Insert: POST /insert
// Body: { table: "YourTable", values: { Col1: "val1", Col2: 123, Col3: "val3" } }
app.post('/insert', async (req, res) => {
  const table = req.body.table;
  const values = req.body.values;

  if (!table || !values || typeof values !== 'object' || Object.keys(values).length === 0) {
    return res.status(400).json({ error: 'table und values erforderlich' });
  }

  const cols = Object.keys(values);
  const params = cols.map((c, i) => `@p${i}`);
  const query = `INSERT INTO dbo.${table} (${cols.join(',')}) VALUES (${params.join(',')}); SELECT SCOPE_IDENTITY() AS id;`;

  const inputs = cols.map((c, i) => {
    const val = values[c];
    const type = typeof val === 'number' ? sql.Int : sql.NVarChar;
    return { name: `p${i}`, type, value: val };
  });

  try {
    const result = await executeQuery(query, inputs);
    return res.json({ insertedId: result && result[0] ? result[0].id : null });
  } catch (err) {
    console.error('DB-Error:', err);
    return res.status(500).json({ error: err.message });
  }
});

// Update: PUT /update
// Body: { table: "YourTable", values: { Col1: "new", Col2: 5 }, where: { key: "Id", value: 7 } }
app.put('/update', async (req, res) => {
  const table = req.body.table;
  const values = req.body.values;
  const where = req.body.where;

  if (!table || !values || typeof values !== 'object' || Object.keys(values).length === 0 || !where || !where.key) {
    return res.status(400).json({ error: 'table, values und where.key erforderlich' });
  }

  const cols = Object.keys(values);
  const setStatements = cols.map((c, i) => `${c} = @p${i}`);
  const query = `UPDATE dbo.${table} SET ${setStatements.join(', ')} WHERE ${where.key} = @whereVal; SELECT @@ROWCOUNT AS affected;`;

  const inputs = cols.map((c, i) => {
    const val = values[c];
    const type = typeof val === 'number' ? sql.Int : sql.NVarChar;
    return { name: `p${i}`, type, value: val };
  });

  // where param
  inputs.push({ name: 'whereVal', type: typeof where.value === 'number' ? sql.Int : sql.NVarChar, value: where.value });

  try {
    const result = await executeQuery(query, inputs);
    const affected = result && result[0] ? result[0].affected : 0;
    return res.json({ affected });
  } catch (err) {
    console.error('DB-Error:', err);
    return res.status(500).json({ error: err.message });
  }
});


const port = process.env.PORT || 3000;
app.listen(port, () => console.log(`Server läuft auf http://localhost:${port}`));
