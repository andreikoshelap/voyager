export interface Harbour {
  id: number;
  name: string;
  lat: number;
  lon: number;
  vhfChannel: string | null;
  phone: string | null;
  depthM: number | null;
  priceNote: string | null;
  hasHost: boolean;
}

/** Matches ee.voyagelog.api.GeoJson.FeatureCollection on the backend. */
export interface HarbourFeatureCollection {
  type: 'FeatureCollection';
  features: HarbourFeature[];
}

export interface HarbourFeature {
  type: 'Feature';
  geometry: { type: 'Point'; coordinates: [number, number] };
  properties: {
    id: number;
    name: string;
    vhfChannel: string | null;
    phone: string | null;
    depthM: number | null;
    priceNote: string | null;
    hasHost: boolean;
  };
}

export function harbourFromFeature(f: HarbourFeature): Harbour {
  const [lon, lat] = f.geometry.coordinates;
  return {
    id: f.properties.id,
    name: f.properties.name,
    lat,
    lon,
    vhfChannel: f.properties.vhfChannel,
    phone: f.properties.phone,
    depthM: f.properties.depthM,
    priceNote: f.properties.priceNote,
    hasHost: f.properties.hasHost,
  };
}
