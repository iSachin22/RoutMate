import { Button, Form } from 'react-bootstrap';
import { CITIES } from '../data/cities.js';

/**
 * Route waypoint editor with preset city dropdowns (no raw lat/lng typing).
 * value: array of city indices; onChange receives the new array.
 */
export default function WaypointEditor({ value, onChange }) {
  const setCity = (index, cityIndex) => {
    const next = [...value];
    next[index] = Number(cityIndex);
    onChange(next);
  };

  const addStop = () => {
    if (value.length >= 6) return;
    const next = [...value];
    next.splice(value.length - 1, 0, 4); // default stop: Lonavala
    onChange(next);
  };

  const removeStop = (index) => onChange(value.filter((_, i) => i !== index));

  return (
    <div>
      {value.map((cityIndex, i) => {
        const label = i === 0 ? 'Origin' : i === value.length - 1 ? 'Destination' : 'Stop';
        const isEnd = i === 0 || i === value.length - 1;
        return (
          <div className="waypoint-row" key={i}>
            <Form.Select
              aria-label={label}
              value={cityIndex}
              onChange={(e) => setCity(i, e.target.value)}
            >
              {CITIES.map((c, idx) => (
                <option key={c.name} value={idx}>
                  {c.name}
                </option>
              ))}
            </Form.Select>
            {isEnd ? (
              <span className="wp-tag">{label}</span>
            ) : (
              <Button variant="outline-danger" size="sm" className="wp-remove" onClick={() => removeStop(i)}>
                &times;
              </Button>
            )}
          </div>
        );
      })}
      <Button variant="outline-primary" size="sm" onClick={addStop} disabled={value.length >= 6}>
        + Add intermediate stop
      </Button>
      <div className="form-text">
        At least 2 points are required to form the route (PostGIS-style LineString).
      </div>
    </div>
  );
}
