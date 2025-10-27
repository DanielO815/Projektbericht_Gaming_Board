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

const port = process.env.PORT || 3000;
app.listen(port, () => console.log(`Server läuft auf http://localhost:${port}`));
